package com.dmsh.staffhelper.feature;

import com.dmsh.staffhelper.StaffHelperState;
import com.dmsh.staffhelper.config.StaffHelperConfig;
import com.dmsh.staffhelper.util.AllowedUsersAccessGate;
import com.dmsh.staffhelper.util.DebugLogStore;
import com.dmsh.staffhelper.util.SupabaseApi;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UpdateNotifyFeature {
    private UpdateNotifyFeature() {}

    private static final String LEGACY_UPDATES_URL = "https://raw.githubusercontent.com/DontiMonti/staffhelper-bd/refs/heads/main/staffhelper_updates.json";
    private static final String RELEASES_URL = "https://github.com/DontiMonti/staffhelper/releases";
    private static final String MSG_UPDATE_REQUIRED_PREFIX = "[ESH] Вышла новая мода, скачайте её здесь ";
    private static final String TITLE_UPDATE_REQUIRED = "Нужно обновить ElytraStaffhelper";
    private static final String MSG_LOCAL_ABOVE_REMOTE =
            "[ESH] Ты где взял эту версию чумба? Ты че дофига разработчик?";
    private static final String DEBUG_OLD_LOCAL_VERSION = "0.0.1";
    private static final String DEBUG_FUTURE_LOCAL_VERSION = "999.0.0";
    private static final String DEBUG_REMOTE_FALLBACK_VERSION = "1.0.0";

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final ScheduledExecutorService EXEC = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "staffhelper-update-check");
        t.setDaemon(true);
        return t;
    });
    private static volatile String lastNoticeKey = "";

    public static void init() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            lastNoticeKey = "";
            EXEC.execute(() -> checkAndNotify(client));
            EXEC.schedule(() -> checkAndNotify(client), 5, TimeUnit.SECONDS);
            EXEC.schedule(() -> checkAndNotify(client), 15, TimeUnit.SECONDS);
        });
    }

    public static void forceCheckNow() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;
        EXEC.execute(() -> checkAndNotify(mc));
    }

    public static void simulateOldVersionNotice() {
        forceCheckNowWithLocalOverride(DEBUG_OLD_LOCAL_VERSION);
    }

    public static void simulateFutureVersionNotice() {
        forceCheckNowWithLocalOverride(DEBUG_FUTURE_LOCAL_VERSION);
    }

    private static void forceCheckNowWithLocalOverride(String simulatedLocalVersion) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;
        String version = simulatedLocalVersion == null ? "" : simulatedLocalVersion.trim();
        if (version.isEmpty()) return;
        EXEC.execute(() -> checkAndNotify(mc, version, true, true));
    }

    private static void checkAndNotify(MinecraftClient client) {
        checkAndNotify(client, null, false, false);
    }

    private static void checkAndNotify(
            MinecraftClient client,
            String localVersionOverride,
            boolean bypassNoticeDedup,
            boolean allowDebugRemoteFallback
    ) {
        if (!AllowedUsersAccessGate.isModAllowed()) return;
        try {
            String localVersion = localVersionOverride;
            if (localVersion == null || localVersion.isBlank()) {
                localVersion = FabricLoader.getInstance()
                        .getModContainer("staffhelper")
                        .map(c -> c.getMetadata().getVersion().getFriendlyString())
                        .orElse(null);
            }
            if (localVersion == null || localVersion.isBlank()) return;

            StaffHelperConfig cfg = StaffHelperState.CONFIG;
            String remoteVersion = fetchRemoteVersion(cfg);
            if ((remoteVersion == null || remoteVersion.isBlank()) && allowDebugRemoteFallback) {
                remoteVersion = DEBUG_REMOTE_FALLBACK_VERSION;
                DebugLogStore.add("[UPDATE][DEBUG] remote fallback version=" + remoteVersion);
            }
            if (remoteVersion == null || remoteVersion.isBlank()) return;
            if (localVersionOverride != null && !localVersionOverride.isBlank()) {
                DebugLogStore.add("[UPDATE][DEBUG] local override version=" + localVersionOverride + ", remote=" + remoteVersion);
            }

            int cmp = compareVersions(localVersion, remoteVersion);
            if (cmp < 0) {
                if (!bypassNoticeDedup && !markNoticeIfFirst("LOWER|" + localVersion + "|" + remoteVersion)) return;
                client.execute(() -> {
                    if (client.player != null) {
                        client.player.sendMessage(buildUpdateRequiredMessage(), false);
                        if (client.inGameHud != null) {
                            client.inGameHud.setTitleTicks(0, 60, 0);
                            client.inGameHud.setSubtitle(Text.empty());
                            client.inGameHud.setTitle(Text.literal(TITLE_UPDATE_REQUIRED).formatted(Formatting.RED));
                        }
                    }
                });
            } else if (cmp > 0) {
                if (!bypassNoticeDedup && !markNoticeIfFirst("HIGHER|" + localVersion + "|" + remoteVersion)) return;
                client.execute(() -> {
                    if (client.player != null) {
                        client.player.sendMessage(Text.literal(MSG_LOCAL_ABOVE_REMOTE).formatted(Formatting.YELLOW), false);
                    }
                });
            }
        } catch (Exception ignored) {
        }
    }

    private static Text buildUpdateRequiredMessage() {
        Text clickable = Text.literal("ТЫК")
                .styled(style -> style
                        .withColor(Formatting.AQUA)
                        .withUnderline(true)
                        .withClickEvent(new ClickEvent.OpenUrl(URI.create(RELEASES_URL))));
        return Text.literal(MSG_UPDATE_REQUIRED_PREFIX).append(clickable);
    }

    private static String fetchRemoteVersion(StaffHelperConfig cfg) throws Exception {
        if (SupabaseApi.isReadConfigured(cfg)) {
            for (String table : SupabaseApi.candidateTables(cfg.supabaseUpdatesTable, "updates", "staffhelper_updates")) {
                String url = SupabaseApi.updatesSelectUrl(cfg, table, false);
                if (url == null || url.isBlank()) continue;

                DebugLogStore.add("[UPDATE][SUPA] GET " + url);
                HttpRequest req = SupabaseApi.buildReadRequest(cfg, url, false).build();
                HttpResponse<String> resp = SupabaseApi.send(req);
                String body = resp.body();
                DebugLogStore.add("[UPDATE][SUPA] HTTP " + resp.statusCode() + " body=" + SupabaseApi.shortBody(body, 240));

                if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                    String version = parseRemoteVersion(body);
                    if (version != null && !version.isBlank()) return version;
                } else if (SupabaseApi.isMissingTable(resp.statusCode(), body)) {
                    continue;
                }
            }
        }

        String fallback = cfg != null && cfg.remoteUpdatesUrl != null && !cfg.remoteUpdatesUrl.isBlank()
                ? cfg.remoteUpdatesUrl
                : LEGACY_UPDATES_URL;
        fallback = normalizeGithubRawUrl(fallback);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(fallback))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        DebugLogStore.add("[UPDATE] GET " + fallback);
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        DebugLogStore.add("[UPDATE] HTTP " + resp.statusCode() + " body=" + SupabaseApi.shortBody(resp.body(), 240));
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) return null;
        String body = resp.body();
        if (body == null || body.isBlank()) return null;
        return parseRemoteVersion(body);
    }

    private static String parseRemoteVersion(String json) {
        try {
            JsonElement rootEl = JsonParser.parseString(json);
            if (rootEl.isJsonArray()) {
                JsonArray arr = rootEl.getAsJsonArray();
                if (arr.isEmpty() || !arr.get(0).isJsonObject()) return null;
                return parseVersionFromObject(arr.get(0).getAsJsonObject());
            }
            if (!rootEl.isJsonObject()) return null;
            JsonObject o = rootEl.getAsJsonObject();
            return parseVersionFromObject(o);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String parseVersionFromObject(JsonObject o) {
        for (String key : List.of(
                "latest_version",
                "mod-version",
                "modVersion",
                "version",
                "latest",
                "latestVersion")) {
            if (!o.has(key)) continue;
            JsonElement el = o.get(key);
            if (el != null && el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
                String v = el.getAsString();
                if (v != null && !v.isBlank()) return v.trim();
            }
        }
        return null;
    }

    private static int compareVersions(String a, String b) {
        List<Integer> va = numericTokens(extractComparableVersion(a));
        List<Integer> vb = numericTokens(extractComparableVersion(b));
        int n = Math.max(va.size(), vb.size());
        for (int i = 0; i < n; i++) {
            int ai = i < va.size() ? va.get(i) : 0;
            int bi = i < vb.size() ? vb.get(i) : 0;
            if (ai != bi) return Integer.compare(ai, bi);
        }
        String sa = normalizeVersionString(a);
        String sb = normalizeVersionString(b);
        if (sa.equals(sb)) return 0;
        return 0;
    }

    private static List<Integer> numericTokens(String version) {
        List<Integer> out = new ArrayList<>();
        if (version == null) return out;
        Matcher m = Pattern.compile("(\\d+)").matcher(version);
        while (m.find()) {
            try {
                out.add(Integer.parseInt(m.group(1)));
            } catch (Exception ignored) {
            }
        }
        return out;
    }

    private static String extractComparableVersion(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        Matcher mv = Pattern.compile("(?i)v\\s*(\\d+(?:\\.\\d+){1,})").matcher(s);
        String lastV = null;
        while (mv.find()) {
            lastV = mv.group(1);
        }
        if (lastV != null && !lastV.isBlank()) return lastV;

        Matcher md = Pattern.compile("(\\d+(?:\\.\\d+){1,})").matcher(s);
        String lastDots = null;
        while (md.find()) {
            lastDots = md.group(1);
        }
        if (lastDots != null && !lastDots.isBlank()) return lastDots;

        return s;
    }

    private static boolean markNoticeIfFirst(String key) {
        if (key == null || key.isBlank()) return false;
        if (key.equals(lastNoticeKey)) return false;
        lastNoticeKey = key;
        return true;
    }

    private static String normalizeVersionString(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeGithubRawUrl(String url) {
        if (url == null) return null;
        return url.replace("/refs/heads/main/", "/main/")
                .replace("/refs/heads/master/", "/master/");
    }
}
