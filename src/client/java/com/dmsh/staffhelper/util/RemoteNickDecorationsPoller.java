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
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Polls nick decorations from Supabase REST (preferred) or old GitHub raw JSON fallback.
 */
public final class RemoteNickDecorationsPoller {
    private RemoteNickDecorationsPoller() {}

    private static final Logger LOGGER = LoggerFactory.getLogger(StaffHelper.MOD_ID);
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final ScheduledExecutorService EXEC = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "staffhelper-remote-decorations");
        t.setDaemon(true);
        return t;
    });

    private static volatile boolean started = false;
    private static volatile String lastEtag = null;

    public static void start(StaffHelperConfig cfg) {
        if (started) return;
        started = true;

        if (cfg == null || !cfg.remoteDecorationsEnabled) {
            LOGGER.info("[StaffHelper] Remote decorations disabled in config.");
            return;
        }

        long interval = Math.max(5, cfg.remoteDecorationsIntervalSeconds);
        if (SupabaseApi.isReadConfigured(cfg)) {
            LOGGER.info("[StaffHelper] Remote decorations poller started (Supabase). interval={}s", interval);
        } else {
            String url = normalizeGithubRawUrl(cfg.remoteDecorationsUrl);
            if (url == null || url.isBlank()) {
                LOGGER.warn("[StaffHelper] remoteDecorationsUrl is empty. Remote decorations will not be loaded.");
                return;
            }
            LOGGER.info("[StaffHelper] Remote decorations poller started (GitHub). interval={}s url={}", interval, url);
        }

        EXEC.scheduleAtFixedRate(() -> safePoll(cfg), 0, interval, TimeUnit.SECONDS);
    }

    public static void forcePollNow(StaffHelperConfig cfg) {
        if (cfg == null) return;
        DebugLogStore.add("[DECOR] force poll requested");
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
        if (cfg == null || !cfg.remoteDecorationsEnabled) return;
        if (!AllowedUsersAccessGate.isModAllowed()) {
            NickDecorationsStore.set(Map.of());
            return;
        }
        try {
            if (SupabaseApi.isReadConfigured(cfg)) {
                pollSupabase(cfg, force);
            } else {
                String url = normalizeGithubRawUrl(cfg.remoteDecorationsUrl);
                if (url == null || url.isBlank()) return;
                pollGithub(url, force);
            }
        } catch (Exception e) {
            DebugLogStore.add("[DECOR] poll error: " + e);
            LOGGER.debug("[StaffHelper] Failed to poll remote decorations: {}", e.toString());
        }
    }

    private static void pollGithub(String url, boolean force) throws Exception {
        String requestUrl = force ? appendCacheBust(url) : url;
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(requestUrl))
                .timeout(Duration.ofSeconds(15))
                .GET();

        String et = lastEtag;
        if (!force && et != null && !et.isBlank()) {
            b.header("If-None-Match", et);
        }
        if (force) {
            b.header("Cache-Control", "no-cache");
            b.header("Pragma", "no-cache");
        }

        DebugLogStore.add("[DECOR] GET " + requestUrl + ((!force && et != null && !et.isBlank()) ? " If-None-Match=" + et : "") + (force ? " [force]" : ""));
        HttpResponse<String> resp = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
        String body = resp.body();
        String bodyPreview = (body == null) ? "" : body.replace("\n", "\\n");
        if (bodyPreview.length() > 260) bodyPreview = bodyPreview.substring(0, 260) + "...";
        DebugLogStore.add("[DECOR] HTTP " + resp.statusCode() + " etag=" + resp.headers().firstValue("etag").orElse("-") + " body=" + bodyPreview);

        if (resp.statusCode() == 304) return;
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) return;

        String newEtag = resp.headers().firstValue("etag").orElse(null);
        if (newEtag != null) lastEtag = newEtag;

        if (body == null || body.isBlank()) return;
        NickDecorationsStore.set(parse(body));
    }

    private static void pollSupabase(StaffHelperConfig cfg, boolean force) throws Exception {
        for (String table : SupabaseApi.candidateTables(cfg.supabaseDecorationsTable, "decorations", "staffhelper_decorations")) {
            boolean[] modes = cfg.supabaseUseActiveFilter ? new boolean[]{true, false} : new boolean[]{false};
            for (boolean withActive : modes) {
                String requestUrl = SupabaseApi.decorationsSelectUrl(cfg, table, force, withActive);
                if (requestUrl == null || requestUrl.isBlank()) continue;

                HttpRequest req = SupabaseApi.buildReadRequest(cfg, requestUrl, force).build();
                DebugLogStore.add("[DECOR][SUPA] GET " + requestUrl + (withActive ? " [active]" : " [no-active]") + (force ? " [force]" : ""));
                HttpResponse<String> resp = SupabaseApi.send(req);

                String body = resp.body();
                DebugLogStore.add("[DECOR][SUPA] HTTP " + resp.statusCode() + " body=" + SupabaseApi.shortBody(body, 260));

                if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                    if (body != null && !body.isBlank()) {
                        NickDecorationsStore.set(parseSupabaseRows(body));
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
                        DebugLogStore.add("[DECOR][SUPA] active-filter disabled (column missing).");
                    }
                    continue; // retry same table without active
                }
            }
        }
    }

    private static Map<String, NickDecoration> parse(String json) {
        JsonElement rootEl = JsonParser.parseString(json);
        if (rootEl.isJsonArray()) {
            return parseSupabaseRows(json);
        }
        if (!rootEl.isJsonObject()) return Map.of();
        JsonObject root = rootEl.getAsJsonObject();

        Map<String, NickDecoration> out = new HashMap<>();

        if (root.has("players") && root.get("players").isJsonObject()) {
            JsonObject players = root.getAsJsonObject("players");
            for (Map.Entry<String, JsonElement> e : players.entrySet()) {
                String nick = e.getKey();
                if (!e.getValue().isJsonObject()) continue;
                NickDecoration d = parseDecoration(e.getValue().getAsJsonObject());
                if (d != null && nick != null && !nick.isBlank()) {
                    out.put(nick.toLowerCase(Locale.ROOT), d);
                }
            }
        }

        if (root.has("entries") && root.get("entries").isJsonArray()) {
            JsonArray arr = root.getAsJsonArray("entries");
            for (JsonElement el : arr) {
                if (!el.isJsonObject()) continue;
                JsonObject o = el.getAsJsonObject();
                String nick = optString(o, "nick");
                if (nick == null || nick.isBlank()) continue;
                NickDecoration d = parseDecoration(o);
                if (d != null) {
                    out.put(nick.toLowerCase(Locale.ROOT), d);
                }
            }
        }

        return out;
    }

    private static Map<String, NickDecoration> parseSupabaseRows(String json) {
        JsonElement rootEl;
        try {
            rootEl = JsonParser.parseString(json);
        } catch (Exception ignored) {
            return Map.of();
        }
        if (!rootEl.isJsonArray()) return Map.of();

        Map<String, NickDecoration> out = new HashMap<>();
        JsonArray arr = rootEl.getAsJsonArray();
        for (JsonElement el : arr) {
            if (!el.isJsonObject()) continue;
            JsonObject o = el.getAsJsonObject();
            String nick = optStringCI(o, "nick", "name", "player");
            if (nick == null || nick.isBlank()) continue;
            NickDecoration d = parseDecoration(o);
            if (d != null) {
                out.put(nick.toLowerCase(Locale.ROOT), d);
            }
        }
        return out;
    }

    private static NickDecoration parseDecoration(JsonObject o) {
        String symbol = optStringCI(o, "symbol");
        if (symbol == null || symbol.isBlank()) symbol = "*";

        String color = optStringCI(o, "color", "rgb");
        Integer rgb = parseColor(color);
        if (rgb == null) rgb = 0xFFFFFF;

        try {
            return new NickDecoration(symbol, rgb);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String optString(JsonObject o, String k) {
        if (o == null || k == null) return null;
        JsonElement el = o.get(k);
        if (el == null || el.isJsonNull()) return null;
        if (!el.isJsonPrimitive()) return null;
        JsonPrimitive p = el.getAsJsonPrimitive();
        return p.isString() ? p.getAsString() : Objects.toString(p.getAsNumber(), null);
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

    private static String asString(JsonElement el) {
        if (el == null || el.isJsonNull()) return null;
        if (!el.isJsonPrimitive()) return null;
        JsonPrimitive p = el.getAsJsonPrimitive();
        if (p.isString()) return p.getAsString();
        if (p.isNumber()) return p.getAsNumber().toString();
        if (p.isBoolean()) return Boolean.toString(p.getAsBoolean());
        return null;
    }

    private static Integer parseColor(String s) {
        if (s == null || s.isBlank()) return null;
        s = s.trim();
        try {
            if (s.startsWith("#")) s = s.substring(1);
            if (s.matches("^[0-9a-fA-F]{6}$")) {
                return Integer.parseInt(s, 16);
            }
            if (s.matches("^[0-9]{1,10}$")) {
                return Integer.parseInt(s);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String appendCacheBust(String url) {
        String sep = url.contains("?") ? "&" : "?";
        return url + sep + "_force=" + System.currentTimeMillis();
    }
}
