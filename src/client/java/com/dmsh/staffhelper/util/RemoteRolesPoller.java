package com.dmsh.staffhelper.util;

import com.dmsh.staffhelper.StaffHelper;
import com.dmsh.staffhelper.config.StaffHelperConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Polls staff roles from Supabase REST (preferred) or old GitHub raw JSON fallback.
 */
public final class RemoteRolesPoller {
    private RemoteRolesPoller() {}

    private static final Logger LOGGER = LoggerFactory.getLogger(StaffHelper.MOD_ID);
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final ScheduledExecutorService EXEC = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "staffhelper-remote-roles");
        t.setDaemon(true);
        return t;
    });

    private static volatile boolean started = false;
    private static volatile String lastEtag = null;

    public static void start(StaffHelperConfig cfg) {
        if (started) return;
        started = true;
        if (cfg == null) return;

        long intervalSeconds = Math.max(5L, cfg.remoteRolesIntervalSeconds);
        if (SupabaseApi.isReadConfigured(cfg)) {
            LOGGER.info("[StaffHelper] Remote roles poller started (Supabase). interval={}s", intervalSeconds);
        } else {
            String url = normalizeGithubRawUrl(cfg.remoteRolesUrl);
            if (url == null || url.isBlank()) {
                LOGGER.warn("[StaffHelper] remoteRolesUrl is empty. Roles will not be loaded.");
                return;
            }
            LOGGER.info("[StaffHelper] Remote roles poller started (GitHub). interval={}s url={}", intervalSeconds, url);
        }

        EXEC.scheduleAtFixedRate(() -> safePoll(cfg), 0, intervalSeconds, TimeUnit.SECONDS);
    }

    public static void forcePollNow(StaffHelperConfig cfg) {
        if (cfg == null) return;
        DebugLogStore.add("[ROLES] force poll requested");
        EXEC.execute(() -> safePoll(cfg, true));
    }

    private static String normalizeGithubRawUrl(String url) {
        if (url == null) return null;
        return url.replace("/refs/heads/main/", "/main/")
                .replace("/refs/heads/master/", "/master/");
    }

    private static void safePoll(StaffHelperConfig cfg) {
        safePoll(cfg, false);
    }

    private static void safePoll(StaffHelperConfig cfg, boolean force) {
        if (cfg == null) return;
        if (!AllowedUsersAccessGate.isModAllowed()) {
            RolesStore.clear();
            return;
        }
        try {
            if (SupabaseApi.isReadConfigured(cfg)) {
                pollSupabase(cfg, force);
            } else {
                String url = normalizeGithubRawUrl(cfg.remoteRolesUrl);
                if (url == null || url.isBlank()) return;
                pollGithub(url, force);
            }
        } catch (Exception e) {
            DebugLogStore.add("[ROLES] poll error: " + e);
            LOGGER.debug("[StaffHelper] Failed to poll remote roles: {}", e.toString());
        }
    }

    private static void pollGithub(String url, boolean force) throws Exception {
        String requestUrl = force ? appendCacheBust(url) : url;
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(requestUrl))
                .timeout(Duration.ofSeconds(15))
                .GET();

        String et = lastEtag;
        if (!force && et != null && !et.isBlank()) b.header("If-None-Match", et);
        if (force) {
            b.header("Cache-Control", "no-cache");
            b.header("Pragma", "no-cache");
        }

        DebugLogStore.add("[ROLES] GET " + requestUrl + ((!force && et != null && !et.isBlank()) ? " If-None-Match=" + et : "") + (force ? " [force]" : ""));
        HttpResponse<String> resp = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
        String body = resp.body();
        String bodyPreview = (body == null) ? "" : body.replace("\n", "\\n");
        if (bodyPreview.length() > 260) bodyPreview = bodyPreview.substring(0, 260) + "...";
        DebugLogStore.add("[ROLES] HTTP " + resp.statusCode() + " etag=" + resp.headers().firstValue("etag").orElse("-") + " body=" + bodyPreview);

        if (resp.statusCode() == 304) return;
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) return;

        String newEtag = resp.headers().firstValue("etag").orElse(null);
        if (newEtag != null) lastEtag = newEtag;

        if (body == null || body.isBlank()) return;
        RolesStore.setRoleEntries(parse(body));
    }

    private static void pollSupabase(StaffHelperConfig cfg, boolean force) throws Exception {
        for (String table : SupabaseApi.candidateTables(cfg.supabaseRolesTable, "staffhelper_roles", "roles")) {
            boolean[] modes = cfg.supabaseUseActiveFilter ? new boolean[]{true, false} : new boolean[]{false};
            for (boolean withActive : modes) {
                String requestUrl = SupabaseApi.rolesSelectUrl(cfg, table, force, withActive);
                if (requestUrl == null || requestUrl.isBlank()) continue;

                HttpRequest req = SupabaseApi.buildReadRequest(cfg, requestUrl, force).build();
                DebugLogStore.add("[ROLES][SUPA] GET " + requestUrl + (withActive ? " [active]" : " [no-active]") + (force ? " [force]" : ""));
                HttpResponse<String> resp = SupabaseApi.send(req);

                String body = resp.body();
                DebugLogStore.add("[ROLES][SUPA] HTTP " + resp.statusCode() + " body=" + SupabaseApi.shortBody(body, 260));

                if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                    if (body != null && !body.isBlank()) {
                        RolesStore.setRoleEntries(parseSupabaseRows(body));
                    }
                    return;
                }

                if (SupabaseApi.isMissingTable(resp.statusCode(), body)) {
                    break; // try next table candidate
                }
                if (SupabaseApi.isMissingActiveColumn(resp.statusCode(), body) && withActive) {
                    if (cfg.supabaseUseActiveFilter) {
                        cfg.supabaseUseActiveFilter = false;
                        cfg.save();
                        DebugLogStore.add("[ROLES][SUPA] active-filter disabled (column missing).");
                    }
                    continue; // retry same table without active
                }
            }
        }
    }

    private static Map<String, RolesStore.RoleInfo> parse(String json) {
        JsonElement rootEl;
        try {
            rootEl = JsonParser.parseString(json);
        } catch (Exception ignored) {
            return Map.of();
        }
        if (rootEl.isJsonArray()) {
            return parseSupabaseRows(json);
        }
        if (!rootEl.isJsonObject()) return Map.of();

        JsonObject root = rootEl.getAsJsonObject();
        Map<String, RolesStore.RoleInfo> out = new HashMap<>();

        for (String key : List.of("roles", "players")) {
            if (root.has(key) && root.get(key).isJsonObject()) {
                JsonObject o = root.getAsJsonObject(key);
                for (Map.Entry<String, JsonElement> e : o.entrySet()) {
                    String nick = norm(e.getKey());
                    RolesStore.RoleInfo roleInfo = parseRoleInfo(e.getValue());
                    if (nick != null && !nick.isBlank() && roleInfo != null && roleInfo.role() != null && !roleInfo.role().isBlank()) {
                        out.put(nick.toLowerCase(Locale.ROOT), roleInfo);
                    }
                }
            }
        }

        if (root.has("entries") && root.get("entries").isJsonArray()) {
            for (JsonElement el : root.getAsJsonArray("entries")) {
                if (!el.isJsonObject()) continue;
                JsonObject o = el.getAsJsonObject();
                String nick = norm(optStringCI(o, "nick", "name", "player"));
                RolesStore.RoleInfo roleInfo = parseRoleInfo(o);
                if (nick != null && !nick.isBlank() && roleInfo != null && roleInfo.role() != null && !roleInfo.role().isBlank()) {
                    out.put(nick.toLowerCase(Locale.ROOT), roleInfo);
                }
            }
        }

        if (out.isEmpty()) {
            for (Map.Entry<String, JsonElement> e : root.entrySet()) {
                String k = e.getKey();
                if (k == null) continue;
                if (k.equalsIgnoreCase("entries") || k.equalsIgnoreCase("roles") || k.equalsIgnoreCase("players")) continue;
                String nick = norm(k);
                RolesStore.RoleInfo roleInfo = parseRoleInfo(e.getValue());
                if (nick != null && !nick.isBlank() && roleInfo != null && roleInfo.role() != null && !roleInfo.role().isBlank()) {
                    out.put(nick.toLowerCase(Locale.ROOT), roleInfo);
                }
            }
        }

        return out;
    }

    private static Map<String, RolesStore.RoleInfo> parseSupabaseRows(String json) {
        JsonElement rootEl;
        try {
            rootEl = JsonParser.parseString(json);
        } catch (Exception ignored) {
            return Map.of();
        }
        if (!rootEl.isJsonArray()) return Map.of();

        Map<String, RolesStore.RoleInfo> out = new HashMap<>();
        JsonArray arr = rootEl.getAsJsonArray();
        for (JsonElement el : arr) {
            if (!el.isJsonObject()) continue;
            JsonObject o = el.getAsJsonObject();
            String nick = norm(optStringCI(o, "nick", "name", "player"));
            RolesStore.RoleInfo roleInfo = parseRoleInfo(o);
            if (nick != null && !nick.isBlank() && roleInfo != null && roleInfo.role() != null && !roleInfo.role().isBlank()) {
                out.put(nick.toLowerCase(Locale.ROOT), roleInfo);
            }
        }
        return out;
    }

    private static RolesStore.RoleInfo parseRoleInfo(JsonElement el) {
        if (el == null || el.isJsonNull()) return null;
        if (el.isJsonObject()) {
            return parseRoleInfo(el.getAsJsonObject());
        }
        String role = norm(asString(el));
        if (role == null || role.isBlank()) return null;
        return new RolesStore.RoleInfo(role, RolesStore.DEFAULT_ROLE_COLOR);
    }

    private static RolesStore.RoleInfo parseRoleInfo(JsonObject o) {
        if (o == null) return null;
        String role = norm(optStringCI(o, "role", "rank", "title"));
        if (role == null || role.isBlank()) return null;

        String colorRaw = norm(optStringCI(o, "color", "role_color", "rgb"));
        Integer parsedColor = parseColor(colorRaw);
        int color = parsedColor == null ? RolesStore.DEFAULT_ROLE_COLOR : parsedColor;
        return new RolesStore.RoleInfo(role, color);
    }

    private static String asString(JsonElement el) {
        if (el == null || el.isJsonNull()) return null;
        if (!el.isJsonPrimitive()) return null;
        JsonPrimitive p = el.getAsJsonPrimitive();
        if (p.isString()) return p.getAsString();
        if (p.isNumber()) return p.getAsNumber().toString();
        if (p.isBoolean()) return Boolean.toString(p.getAsBoolean());
        return null;
    }

    private static String optStringCI(JsonObject o, String... keys) {
        if (o == null || keys == null) return null;
        for (String key : keys) {
            if (key == null) continue;
            for (Map.Entry<String, JsonElement> e : o.entrySet()) {
                String ek = e.getKey();
                if (ek != null && ek.equalsIgnoreCase(key)) {
                    return asString(e.getValue());
                }
            }
        }
        return null;
    }

    private static String norm(String s) {
        return s == null ? null : s.trim();
    }

    private static Integer parseColor(String s) {
        if (s == null || s.isBlank()) return null;
        String v = s.trim();
        try {
            if (v.startsWith("#")) v = v.substring(1);
            if (v.matches("^[0-9a-fA-F]{6}$")) return Integer.parseInt(v, 16);
            if (v.matches("^[0-9]{1,10}$")) return Integer.parseInt(v);
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String appendCacheBust(String url) {
        String sep = url.contains("?") ? "&" : "?";
        return url + sep + "_force=" + System.currentTimeMillis();
    }
}
