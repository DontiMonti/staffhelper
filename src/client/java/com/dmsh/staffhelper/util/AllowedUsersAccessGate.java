package com.dmsh.staffhelper.util;

import com.dmsh.staffhelper.StaffHelperState;
import com.dmsh.staffhelper.config.StaffHelperConfig;
import com.dmsh.staffhelper.feature.AfkZoneFeature;
import com.dmsh.staffhelper.feature.StaffStatsFeature;
import com.dmsh.staffhelper.feature.VanishFeature;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.StringHelper;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Runtime access control based on Supabase table "allowed_users".
 */
public final class AllowedUsersAccessGate {
    private AllowedUsersAccessGate() {}

    private enum State { PENDING, ALLOWED, DENIED }

    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "staffhelper-allowed-users-check");
        t.setDaemon(true);
        return t;
    });

    private static final long RECHECK_INTERVAL_MS = 15_000L;
    private static final long UNKNOWN_RETRY_INTERVAL_MS = 5_000L;
    private static final long DENIED_MESSAGE_COOLDOWN_MS = 2_500L;
    private static final String DENIED_TEXT = "[StaffHelper] \u0423 \u0432\u0430\u0441 \u043d\u0435\u0442\u0443 \u0434\u043e\u0441\u0442\u0443\u043f\u0430 \u043a \u0434\u0430\u043d\u043d\u043e\u043c\u0443 \u043c\u043e\u0434\u0443, \u043e\u0431\u0440\u0430\u0442\u0438\u0442\u0435\u0441\u044c \u043a DontiMonti.";

    private static volatile State state = State.PENDING;
    private static volatile State stateBeforeCheck = State.PENDING;
    private static volatile boolean checkInFlight = false;
    private static volatile long nextCheckAtMs = 0L;
    private static volatile long lastDeniedMessageAtMs = 0L;
    private static volatile String checkedNick = "";

    public static void init() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> requestCheck(client, true));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> resetForDisconnect());
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick(client));
    }

    public static boolean isModAllowed() {
        return state == State.ALLOWED;
    }

    public static boolean ensureAccessOrNotify(MinecraftClient client) {
        if (isModAllowed()) return true;
        if (state == State.DENIED) {
            maybeSendDeniedMessage(client);
        }
        return false;
    }

    private static void tick(MinecraftClient client) {
        if (client == null || client.player == null) return;
        if (state == State.ALLOWED && System.currentTimeMillis() < nextCheckAtMs) return;
        requestCheck(client, false);
    }

    private static void requestCheck(MinecraftClient client, boolean force) {
        if (client == null || client.player == null) return;
        StaffHelperConfig cfg = StaffHelperState.CONFIG;
        if (cfg == null) return;

        String nick = client.player.getGameProfile().getName();
        if (StringHelper.isBlank(nick)) return;
        nick = nick.trim();

        long now = System.currentTimeMillis();
        if (!force && now < nextCheckAtMs) return;
        if (!force && nick.equalsIgnoreCase(checkedNick) && state == State.ALLOWED) return;
        if (checkInFlight) return;

        checkInFlight = true;
        stateBeforeCheck = state;
        state = State.PENDING;
        String targetNick = nick;
        StaffHelperConfig snapshotCfg = cfg;
        EXEC.execute(() -> {
            CheckResult result = checkAllowed(snapshotCfg, targetNick);
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc == null) {
                checkInFlight = false;
                return;
            }
            mc.execute(() -> applyResult(mc, targetNick, result));
        });
    }

    private static CheckResult checkAllowed(StaffHelperConfig cfg, String nick) {
        if (!SupabaseApi.isReadConfigured(cfg)) {
            return CheckResult.deny("supabase read config missing");
        }

        boolean hadSuccessfulResponse = false;
        List<String> tables = SupabaseApi.candidateTables(cfg.supabaseAllowedUsersTable, "allowed_users");
        for (String table : tables) {
            try {
                String url = SupabaseApi.allowedUsersSelectUrl(cfg, table, nick, true);
                if (StringHelper.isBlank(url)) continue;

                HttpRequest req = SupabaseApi.buildReadRequest(cfg, url, true).build();
                DebugLogStore.add("[ACCESS][SUPA] GET " + url + " [force]");
                HttpResponse<String> resp = SupabaseApi.send(req);
                String body = resp.body();
                DebugLogStore.add("[ACCESS][SUPA] HTTP " + resp.statusCode() + " body=" + SupabaseApi.shortBody(body, 220));

                if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                    hadSuccessfulResponse = true;
                    return parseAllowed(body, nick)
                            ? CheckResult.allow()
                            : CheckResult.deny("nick not found");
                }

                if (SupabaseApi.isMissingTable(resp.statusCode(), body)) {
                    continue;
                }
            } catch (Exception e) {
                DebugLogStore.add("[ACCESS][SUPA] check error: " + e);
            }
        }
        if (hadSuccessfulResponse) {
            return CheckResult.deny("nick not found");
        }
        return CheckResult.unknown("allowed_users read failed (network/temporary)");
    }

    private static boolean parseAllowed(String body, String nick) {
        if (StringHelper.isBlank(body) || StringHelper.isBlank(nick)) return false;
        try {
            JsonElement root = JsonParser.parseString(body);
            if (!root.isJsonArray()) return false;
            JsonArray arr = root.getAsJsonArray();
            String target = nick.trim().toLowerCase(Locale.ROOT);
            for (JsonElement el : arr) {
                if (!el.isJsonObject()) continue;
                JsonObject o = el.getAsJsonObject();
                String rowNick = optStringCI(o, "nick", "name", "player");
                if (rowNick == null) continue;
                if (target.equals(rowNick.trim().toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
            return false;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String optStringCI(JsonObject o, String... keys) {
        if (o == null || keys == null) return null;
        for (String key : keys) {
            if (key == null) continue;
            for (Map.Entry<String, JsonElement> e : o.entrySet()) {
                String ek = e.getKey();
                if (ek != null && ek.equalsIgnoreCase(key)) {
                    JsonElement v = e.getValue();
                    if (v == null || v.isJsonNull()) return null;
                    if (!v.isJsonPrimitive()) return null;
                    if (v.getAsJsonPrimitive().isString()) return v.getAsString();
                    return v.getAsString();
                }
            }
        }
        return null;
    }

    private static void applyResult(MinecraftClient client, String nick, CheckResult result) {
        checkInFlight = false;
        checkedNick = nick == null ? "" : nick;

        if (result.allowed) {
            boolean becameAllowed = stateBeforeCheck != State.ALLOWED;
            state = State.ALLOWED;
            nextCheckAtMs = System.currentTimeMillis() + RECHECK_INTERVAL_MS;
            if (becameAllowed) {
                RemoteNickDecorationsPoller.forcePollNow(StaffHelperState.CONFIG);
                RemoteRolesPoller.forcePollNow(StaffHelperState.CONFIG);
            }
            System.out.println("[StaffHelper][ACCESS] ALLOWED nick=" + checkedNick + " reason=" + result.reason);
            return;
        }

        if (result.unknown) {
            state = stateBeforeCheck == State.ALLOWED ? State.ALLOWED : State.PENDING;
            nextCheckAtMs = System.currentTimeMillis() + UNKNOWN_RETRY_INTERVAL_MS;
            System.out.println("[StaffHelper][ACCESS] UNKNOWN nick=" + checkedNick + " reason=" + result.reason);
            return;
        }

        state = State.DENIED;
        nextCheckAtMs = System.currentTimeMillis() + RECHECK_INTERVAL_MS;
        disableAllRuntimeData();
        System.out.println("[StaffHelper][ACCESS] DENIED nick=" + checkedNick + " reason=" + result.reason);
    }

    private static void disableAllRuntimeData() {
        RolesStore.clear();
        NickDecorationsStore.set(Map.of());
        VanishFeature.clear();
        StaffStatsFeature.reset();
        AfkZoneFeature.clearRuntimeState();
    }

    private static void resetForDisconnect() {
        state = State.PENDING;
        checkInFlight = false;
        nextCheckAtMs = 0L;
        checkedNick = "";
        disableAllRuntimeData();
    }

    private static void maybeSendDeniedMessage(MinecraftClient client) {
        if (client == null || client.player == null) return;
        long now = System.currentTimeMillis();
        if (now - lastDeniedMessageAtMs < DENIED_MESSAGE_COOLDOWN_MS) return;
        lastDeniedMessageAtMs = now;
        client.player.sendMessage(net.minecraft.text.Text.literal(DENIED_TEXT), false);
    }

    private static final class CheckResult {
        final boolean allowed;
        final boolean unknown;
        final String reason;

        private CheckResult(boolean allowed, boolean unknown, String reason) {
            this.allowed = allowed;
            this.unknown = unknown;
            this.reason = reason;
        }

        static CheckResult allow() {
            return new CheckResult(true, false, "ok");
        }

        static CheckResult deny(String reason) {
            return new CheckResult(false, false, reason == null ? "" : reason);
        }

        static CheckResult unknown(String reason) {
            return new CheckResult(false, true, reason == null ? "" : reason);
        }
    }
}

