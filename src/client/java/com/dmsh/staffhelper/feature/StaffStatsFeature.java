package com.dmsh.staffhelper.feature;

import com.dmsh.staffhelper.config.ConfigSecretCodec;
import com.dmsh.staffhelper.util.AllowedUsersAccessGate;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StaffStatsFeature {
    private StaffStatsFeature() {}

    private static final Gson GSON = new GsonBuilder().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("staffhelper-staff-stats.dat");
    private static final Object LOCK = new Object();
    private static final long MAX_TRACKED_DELTA_MS = 5_000L;
    private static final long SAVE_INTERVAL_MS = 30_000L;
    private static final long MESSAGE_DEDUPE_WINDOW_MS = 4_000L;
    private static final long DATA_RETENTION_DAYS = 548L;
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,16}$");
    private static final Pattern MUTE_PATTERN = Pattern.compile("^ⓘ\\s+([A-Za-z0-9_]{3,16})\\s+заблокировал чат\\s+(.+)$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern BAN_PATTERN = Pattern.compile("^ⓘ\\s+([A-Za-z0-9_]{3,16})\\s+заблокировал\\s+(.+?)\\s+\\(ɪᴅ\\s+[^)]+\\)$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern KICK_PATTERN = Pattern.compile("^ⓘ\\s+([A-Za-z0-9_]{3,16})\\s+удалил из игры\\s+(.+)$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static StatsFileData data = loadStore();
    private static final LinkedHashMap<String, Long> recentMessages = new LinkedHashMap<>();
    private static boolean initialized = false;
    private static boolean dirty = false;
    private static long lastTickAtMs = -1L;
    private static long lastSaveAtMs = 0L;

    public static void init() {
        synchronized (LOCK) {
            if (initialized) return;
            initialized = true;
        }

        ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, receptionTimestamp) -> {
            onMessage(message.getString());
        });
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            onMessage(message.getString());
        });
        ClientTickEvents.END_CLIENT_TICK.register(StaffStatsFeature::onEndTick);
    }

    public static MonthOverview getCurrentMonthOverview() {
        synchronized (LOCK) {
            return buildCurrentMonthOverview(currentUsername(MinecraftClient.getInstance()));
        }
    }

    public static String getCurrentUserName() {
        synchronized (LOCK) {
            return currentUsername(MinecraftClient.getInstance());
        }
    }

    public static int getBans() {
        synchronized (LOCK) {
            return buildCurrentMonthOverview(currentUsername(MinecraftClient.getInstance())).totalBans();
        }
    }

    public static int getMutes() {
        synchronized (LOCK) {
            return buildCurrentMonthOverview(currentUsername(MinecraftClient.getInstance())).totalMutes();
        }
    }

    public static void reset() {
        synchronized (LOCK) {
            saveNowLocked();
            recentMessages.clear();
            lastTickAtMs = -1L;
        }
    }

    private static void onEndTick(MinecraftClient client) {
        synchronized (LOCK) {
            long now = System.currentTimeMillis();
            if (lastTickAtMs < 0L) {
                lastTickAtMs = now;
                return;
            }

            long deltaMs = now - lastTickAtMs;
            lastTickAtMs = now;
            if (deltaMs < 0L) deltaMs = 0L;
            if (deltaMs > MAX_TRACKED_DELTA_MS) deltaMs = MAX_TRACKED_DELTA_MS;

            boolean trackSession = shouldTrackSession(client);
            String username = currentUsername(client);
            if (trackSession && !username.isEmpty() && deltaMs > 0L) {
                DayStats dayStats = dayStats(username, LocalDate.now());
                dayStats.playMillis = safeAdd(dayStats.playMillis, deltaMs);
                dirty = true;
                pruneOldDays(profile(username));
            }

            if (dirty && (!trackSession || now - lastSaveAtMs >= SAVE_INTERVAL_MS)) {
                saveNowLocked();
            }
        }
    }

    private static void onMessage(String message) {
        synchronized (LOCK) {
            if (!AllowedUsersAccessGate.isModAllowed()) return;

            String username = currentUsername(MinecraftClient.getInstance());
            if (username.isEmpty()) return;

            ModerationAction action = parseModerationAction(message, username);
            if (action == null) return;

            long now = System.currentTimeMillis();
            purgeRecentMessages(now);
            String dedupeKey = action.type.name() + "|" + action.dedupeKey.toLowerCase(Locale.ROOT);
            Long previousAt = recentMessages.get(dedupeKey);
            if (previousAt != null && now - previousAt <= MESSAGE_DEDUPE_WINDOW_MS) {
                return;
            }
            recentMessages.put(dedupeKey, now);

            DayStats dayStats = dayStats(username, LocalDate.now());
            switch (action.type) {
                case BAN -> dayStats.bans++;
                case MUTE -> dayStats.mutes++;
                case KICK -> dayStats.kicks++;
            }
            dirty = true;
            pruneOldDays(profile(username));
            saveNowLocked();
        }
    }

    private static ModerationAction parseModerationAction(String rawMessage, String currentUser) {
        if (rawMessage == null || rawMessage.isBlank()) return null;
        String normalized = rawMessage
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace('\u00A0', ' ');

        String[] lines = normalized.split("\n");
        for (String line : lines) {
            String clean = normalizeLine(line);
            if (clean.isEmpty()) continue;

            ModerationAction action = matchAction(clean, currentUser, MUTE_PATTERN, ActionType.MUTE);
            if (action != null) return action;

            action = matchAction(clean, currentUser, BAN_PATTERN, ActionType.BAN);
            if (action != null) return action;

            action = matchAction(clean, currentUser, KICK_PATTERN, ActionType.KICK);
            if (action != null) return action;
        }
        return null;
    }

    private static ModerationAction matchAction(String line, String currentUser, Pattern pattern, ActionType type) {
        Matcher matcher = pattern.matcher(line);
        if (!matcher.matches()) return null;
        String moderator = matcher.group(1);
        if (!moderator.equalsIgnoreCase(currentUser)) return null;
        return new ModerationAction(type, line);
    }

    private static String normalizeLine(String line) {
        if (line == null) return "";
        String clean = line.replaceAll("§.", "");
        clean = clean.replace('\u00A0', ' ').trim();
        clean = clean.replaceAll("\\s+", " ");
        return clean;
    }

    private static boolean shouldTrackSession(MinecraftClient client) {
        return client != null
                && client.player != null
                && client.world != null
                && client.getNetworkHandler() != null
                && AllowedUsersAccessGate.isModAllowed();
    }

    private static String currentUsername(MinecraftClient client) {
        String user = "";
        if (client != null && client.player != null && client.player.getGameProfile() != null) {
            user = safe(client.player.getGameProfile().getName());
        }
        if (user.isEmpty() && client != null && client.getSession() != null) {
            user = safe(client.getSession().getUsername());
        }
        if (!USERNAME_PATTERN.matcher(user).matches()) return "";
        return user;
    }

    private static MonthOverview buildCurrentMonthOverview(String username) {
        YearMonth month = YearMonth.now();
        LocalDate today = LocalDate.now();
        ProfileStats profile = username.isEmpty() ? null : data.profiles.get(profileKey(username));

        List<DaySnapshot> days = new ArrayList<>();
        long totalPlayMillis = 0L;
        int totalBans = 0;
        int totalMutes = 0;
        int totalKicks = 0;
        DaySnapshot todaySnapshot = null;

        for (int dayOfMonth = 1; dayOfMonth <= month.lengthOfMonth(); dayOfMonth++) {
            LocalDate date = month.atDay(dayOfMonth);
            DayStats stored = profile != null ? profile.days.get(date.toString()) : null;
            long playMillis = stored != null ? Math.max(0L, stored.playMillis) : 0L;
            int bans = stored != null ? Math.max(0, stored.bans) : 0;
            int mutes = stored != null ? Math.max(0, stored.mutes) : 0;
            int kicks = stored != null ? Math.max(0, stored.kicks) : 0;

            DaySnapshot snapshot = new DaySnapshot(date, playMillis, bans, mutes, kicks);
            if (date.equals(today)) {
                todaySnapshot = snapshot;
            }
            totalPlayMillis = safeAdd(totalPlayMillis, playMillis);
            totalBans += bans;
            totalMutes += mutes;
            totalKicks += kicks;
            days.add(snapshot);
        }

        if (todaySnapshot == null) {
            todaySnapshot = new DaySnapshot(today, 0L, 0, 0, 0);
        }

        return new MonthOverview(month, List.copyOf(days), todaySnapshot, totalPlayMillis, totalBans, totalMutes, totalKicks);
    }

    private static ProfileStats profile(String username) {
        return data.profiles.computeIfAbsent(profileKey(username), ignored -> new ProfileStats());
    }

    private static DayStats dayStats(String username, LocalDate date) {
        ProfileStats profile = profile(username);
        return profile.days.computeIfAbsent(date.toString(), ignored -> new DayStats());
    }

    private static String profileKey(String username) {
        return safe(username).toLowerCase(Locale.ROOT);
    }

    private static void pruneOldDays(ProfileStats profile) {
        if (profile == null || profile.days.isEmpty()) return;
        LocalDate cutoff = LocalDate.now().minusDays(DATA_RETENTION_DAYS);
        Iterator<Map.Entry<String, DayStats>> iterator = profile.days.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, DayStats> entry = iterator.next();
            LocalDate date = parseDate(entry.getKey());
            if (date == null || date.isBefore(cutoff)) {
                iterator.remove();
            }
        }
    }

    private static void purgeRecentMessages(long now) {
        Iterator<Map.Entry<String, Long>> iterator = recentMessages.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (now - entry.getValue() > MESSAGE_DEDUPE_WINDOW_MS) {
                iterator.remove();
            }
        }
    }

    private static StatsFileData loadStore() {
        try {
            if (!Files.exists(FILE)) {
                return new StatsFileData();
            }
            String encoded = Files.readString(FILE).trim();
            if (!ConfigSecretCodec.isEncrypted(encoded)) {
                return new StatsFileData();
            }
            String json = ConfigSecretCodec.decrypt(encoded);
            if (json.isBlank()) {
                return new StatsFileData();
            }
            StatsFileData loaded = GSON.fromJson(json, StatsFileData.class);
            return normalizeLoadedData(loaded);
        } catch (Exception ignored) {
            return new StatsFileData();
        }
    }

    private static StatsFileData normalizeLoadedData(StatsFileData source) {
        StatsFileData normalized = new StatsFileData();
        if (source == null || source.profiles == null) return normalized;

        for (Map.Entry<String, ProfileStats> profileEntry : source.profiles.entrySet()) {
            String key = profileKey(profileEntry.getKey());
            if (!USERNAME_PATTERN.matcher(key).matches()) continue;

            ProfileStats cleanProfile = new ProfileStats();
            ProfileStats rawProfile = profileEntry.getValue();
            if (rawProfile != null && rawProfile.days != null) {
                for (Map.Entry<String, DayStats> dayEntry : rawProfile.days.entrySet()) {
                    LocalDate date = parseDate(dayEntry.getKey());
                    if (date == null) continue;

                    DayStats rawDay = dayEntry.getValue();
                    DayStats cleanDay = new DayStats();
                    cleanDay.playMillis = rawDay != null ? Math.max(0L, rawDay.playMillis) : 0L;
                    cleanDay.bans = rawDay != null ? Math.max(0, rawDay.bans) : 0;
                    cleanDay.mutes = rawDay != null ? Math.max(0, rawDay.mutes) : 0;
                    cleanDay.kicks = rawDay != null ? Math.max(0, rawDay.kicks) : 0;
                    cleanProfile.days.put(date.toString(), cleanDay);
                }
            }
            pruneOldDays(cleanProfile);
            normalized.profiles.put(key, cleanProfile);
        }
        return normalized;
    }

    private static void saveNowLocked() {
        if (!dirty) return;
        try {
            String json = GSON.toJson(data);
            String encoded = ConfigSecretCodec.encrypt(json);
            if (!ConfigSecretCodec.isEncrypted(encoded)) return;
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, encoded);
            dirty = false;
            lastSaveAtMs = System.currentTimeMillis();
        } catch (Exception ignored) {
        }
    }

    private static LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(safe(value));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static long safeAdd(long a, long b) {
        if (b <= 0L) return a;
        if (Long.MAX_VALUE - a < b) return Long.MAX_VALUE;
        return a + b;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public record DaySnapshot(LocalDate date, long playMillis, int bans, int mutes, int kicks) {}

    public record MonthOverview(
            YearMonth month,
            List<DaySnapshot> days,
            DaySnapshot today,
            long totalPlayMillis,
            int totalBans,
            int totalMutes,
            int totalKicks
    ) {}

    private enum ActionType {
        BAN,
        MUTE,
        KICK
    }

    private record ModerationAction(ActionType type, String dedupeKey) {}

    private static final class StatsFileData {
        private Map<String, ProfileStats> profiles = new LinkedHashMap<>();
    }

    private static final class ProfileStats {
        private Map<String, DayStats> days = new LinkedHashMap<>();
    }

    private static final class DayStats {
        private long playMillis;
        private int bans;
        private int mutes;
        private int kicks;
    }
}
