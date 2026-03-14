package com.dmsh.staffhelper.util;

import java.util.Locale;
import java.util.Map;

public final class RolesStore {
    private RolesStore() {}

    public static final int DEFAULT_ROLE_COLOR = 0x7ED3FF;

    private static final Map<String, RoleInfo> ROLES = Map.of(
            "dontimonti", new RoleInfo("DEV", 0xFF4A4A, RoleTextStyle.DEV),
            "werkuk", new RoleInfo("LOVE", 0xFF6FB5, RoleTextStyle.LOVE)
    );

    public static RoleInfo getRoleInfoFor(String nick) {
        String key = normalizeNick(nick);
        if (key == null || key.isBlank()) return null;
        return ROLES.get(key);
    }

    public static int size() {
        return ROLES.size();
    }

    private static String normalizeNick(String value) {
        if (value == null || value.isBlank()) return null;

        String clean = value.replaceAll("\u00A7.", "").replaceAll("[^A-Za-z0-9_]", "");
        if (clean.isBlank()) return null;
        return clean.toLowerCase(Locale.ROOT);
    }

    public enum RoleTextStyle {
        NONE,
        DEV,
        LOVE
    }

    public record RoleInfo(String role, int color, RoleTextStyle style) {}
}
