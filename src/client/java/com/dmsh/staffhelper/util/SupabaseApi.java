package com.dmsh.staffhelper.util;

import com.dmsh.staffhelper.config.StaffHelperConfig;
import net.minecraft.util.StringHelper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Small helper for Supabase REST URL/header building.
 */
public final class SupabaseApi {
    private SupabaseApi() {}

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public static boolean isReadConfigured(StaffHelperConfig cfg) {
        return cfg != null
                && !StringHelper.isBlank(cfg.supabaseProjectUrl)
                && !StringHelper.isBlank(cfg.supabaseAnonKey);
    }

    public static boolean isWriteConfigured(StaffHelperConfig cfg) {
        if (!isReadConfigured(cfg)) return false;
        return !StringHelper.isBlank(writeAuthKey(cfg));
    }

    public static String decorationsSelectUrl(StaffHelperConfig cfg, boolean force) {
        return decorationsSelectUrl(cfg, cfg.supabaseDecorationsTable, force, cfg != null && cfg.supabaseUseActiveFilter);
    }

    public static String decorationsSelectUrl(StaffHelperConfig cfg, String table, boolean force, boolean withActiveFilter) {
        if (!isReadConfigured(cfg)) return null;
        StringBuilder sb = new StringBuilder()
                .append(restTableUrl(cfg, table))
                .append("?select=nick,symbol,color")
                .append("&order=nick.asc");
        if (withActiveFilter) {
            sb.append("&active=eq.true");
        }
        String url = sb.toString();
        return force ? appendCacheBust(url) : url;
    }

    public static String rolesSelectUrl(StaffHelperConfig cfg, boolean force) {
        return rolesSelectUrl(cfg, cfg.supabaseRolesTable, force, cfg != null && cfg.supabaseUseActiveFilter);
    }

    public static String rolesSelectUrl(StaffHelperConfig cfg, String table, boolean force, boolean withActiveFilter) {
        if (!isReadConfigured(cfg)) return null;
        StringBuilder sb = new StringBuilder()
                .append(restTableUrl(cfg, table))
                .append("?select=*")
                .append("&order=nick.asc");
        if (withActiveFilter) {
            sb.append("&active=eq.true");
        }
        String url = sb.toString();
        return force ? appendCacheBust(url) : url;
    }

    public static String updatesSelectUrl(StaffHelperConfig cfg, boolean force) {
        return updatesSelectUrl(cfg, cfg.supabaseUpdatesTable, force);
    }

    public static String updatesSelectUrl(StaffHelperConfig cfg, String table, boolean force) {
        if (!isReadConfigured(cfg)) return null;
        String url = restTableUrl(cfg, table) + "?select=*&limit=1";
        return force ? appendCacheBust(url) : url;
    }

    public static String allowedUsersSelectUrl(StaffHelperConfig cfg, String table, String nick, boolean force) {
        if (!isReadConfigured(cfg)) return null;
        String normalizedNick = nick == null ? "" : nick.trim();
        // Use case-insensitive lookup because Minecraft nicknames are often compared without case.
        String url = restTableUrl(cfg, table) + "?select=nick&nick=ilike." + encode(normalizedNick) + "&limit=1";
        return force ? appendCacheBust(url) : url;
    }

    public static HttpRequest.Builder buildReadRequest(StaffHelperConfig cfg, String url, boolean force) {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .GET();
        String key = cfg.supabaseAnonKey;
        b.header("apikey", key);
        b.header("Authorization", "Bearer " + key);
        b.header("Accept", "application/json");
        if (force) {
            b.header("Cache-Control", "no-cache");
            b.header("Pragma", "no-cache");
        }
        return b;
    }

    public static HttpRequest.Builder buildWriteRequest(StaffHelperConfig cfg, String url, String method, String body) {
        String payload = body == null ? "" : body;
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");

        String key = writeAuthKey(cfg);
        if (!StringHelper.isBlank(key)) {
            b.header("apikey", key);
            b.header("Authorization", "Bearer " + key);
        }

        if ("POST".equalsIgnoreCase(method)) {
            b.POST(HttpRequest.BodyPublishers.ofString(payload));
        } else if ("PATCH".equalsIgnoreCase(method)) {
            b.method("PATCH", HttpRequest.BodyPublishers.ofString(payload));
        } else if ("DELETE".equalsIgnoreCase(method)) {
            b.DELETE();
        } else {
            b.method(method, HttpRequest.BodyPublishers.ofString(payload));
        }

        return b;
    }

    public static HttpResponse<String> send(HttpRequest request) throws Exception {
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public static String restTableUrl(StaffHelperConfig cfg, String table) {
        String base = cfg.supabaseProjectUrl;
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base + "/rest/v1/" + sanitizeTableName(table, "staffhelper_data");
    }

    public static String withEqFilter(String baseUrl, String column, String value) {
        String sep = baseUrl.contains("?") ? "&" : "?";
        return baseUrl + sep + sanitizeColumnName(column, "id") + "=eq." + encode(value);
    }

    public static String appendCacheBust(String url) {
        // PostgREST treats unknown query parameters as filters and may return 400 (PGRST100).
        // For Supabase REST endpoints we rely on no-cache headers instead of query cache-busting.
        return url;
    }

    public static String shortBody(String body, int max) {
        if (body == null) return "";
        String oneLine = body.replace("\n", "\\n");
        if (oneLine.length() <= max) return oneLine;
        return oneLine.substring(0, Math.max(0, max - 3)) + "...";
    }

    public static List<String> candidateTables(String preferred, String... fallbacks) {
        Set<String> out = new LinkedHashSet<>();
        addTableCandidate(out, preferred);
        if (fallbacks != null) {
            for (String fallback : fallbacks) {
                addTableCandidate(out, fallback);
            }
        }
        return new ArrayList<>(out);
    }

    public static boolean isMissingTable(int statusCode, String body) {
        if (statusCode != 404) return false;
        return hasErrorCode(body, "PGRST205") || bodyContains(body, "could not find the table");
    }

    public static boolean isMissingActiveColumn(int statusCode, String body) {
        if (statusCode != 400) return false;
        if (!hasErrorCode(body, "42703")) return false;
        return bodyContains(body, ".active") || bodyContains(body, "column active");
    }

    private static boolean hasErrorCode(String body, String code) {
        if (body == null || code == null || code.isBlank()) return false;
        String compact = body.replace(" ", "");
        return compact.contains("\"code\":\"" + code + "\"");
    }

    private static boolean bodyContains(String body, String fragment) {
        if (body == null || fragment == null) return false;
        return body.toLowerCase().contains(fragment.toLowerCase());
    }

    private static String writeAuthKey(StaffHelperConfig cfg) {
        if (cfg == null) return "";
        if (!StringHelper.isBlank(cfg.supabaseWriteKey)) return cfg.supabaseWriteKey.trim();
        return cfg.supabaseAnonKey == null ? "" : cfg.supabaseAnonKey.trim();
    }

    private static String sanitizeTableName(String value, String fallback) {
        if (StringHelper.isBlank(value)) return fallback;
        String s = value.trim();
        return s.matches("^[A-Za-z0-9_]+$") ? s : fallback;
    }

    private static void addTableCandidate(Set<String> out, String table) {
        if (out == null || StringHelper.isBlank(table)) return;
        String t = table.trim();
        if (!t.matches("^[A-Za-z0-9_]+$")) return;
        out.add(t);
    }

    private static String sanitizeColumnName(String value, String fallback) {
        if (StringHelper.isBlank(value)) return fallback;
        String s = value.trim();
        return s.matches("^[A-Za-z0-9_]+$") ? s : fallback;
    }

    private static String encode(String value) {
        String v = value == null ? "" : value;
        return URLEncoder.encode(v, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
