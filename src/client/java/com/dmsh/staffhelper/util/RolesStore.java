package com.dmsh.staffhelper.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class RolesStore {
    private RolesStore() {}

    public static final int DEFAULT_ROLE_COLOR = 0x7ED3FF;

    private static volatile Map<String, RoleInfo> roles = Collections.emptyMap();

    public static void setRoleEntries(Map<String, RoleInfo> newRoles) {
        if (newRoles == null) return;
        Map<String, RoleInfo> normalized = new HashMap<>();
        for (Map.Entry<String, RoleInfo> e : newRoles.entrySet()) {
            if (e.getKey() == null || e.getValue() == null) continue;
            String key = normalizeNick(e.getKey());
            if (key == null || key.isBlank()) continue;

            String role = e.getValue().role();
            if (role == null || role.isBlank()) continue;

            normalized.put(key, new RoleInfo(role.trim(), normalizeColor(e.getValue().color())));
        }
        roles = Collections.unmodifiableMap(normalized);
    }

    public static String getRoleFor(String nick) {
        RoleInfo info = getRoleInfoFor(nick);
        return info == null ? null : info.role();
    }

    public static int getRoleColorFor(String nick) {
        RoleInfo info = getRoleInfoFor(nick);
        return info == null ? DEFAULT_ROLE_COLOR : normalizeColor(info.color());
    }

    public static RoleInfo getRoleInfoFor(String nick) {
        String key = normalizeNick(nick);
        if (key == null || key.isBlank()) return null;
        return roles.get(key);
    }

    public static int size() {
        return roles.size();
    }

    public static Map<String, RoleInfo> snapshot() {
        return roles;
    }

    public static void clear() {
        roles = Collections.emptyMap();
    }

    private static String normalizeNick(String s) {
        if (s == null || s.isBlank()) return null;

        String clean = s.replaceAll("§.", "").replaceAll("[^A-Za-z0-9_]", "");
        if (clean.isBlank()) return null;
        return clean.toLowerCase(Locale.ROOT);
    }

    private static int normalizeColor(int color) {
        if (color < 0 || color > 0xFFFFFF) return DEFAULT_ROLE_COLOR;
        return color;
    }

    public record RoleInfo(String role, int color) {}
}
