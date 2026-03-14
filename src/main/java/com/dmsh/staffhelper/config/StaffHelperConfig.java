package com.dmsh.staffhelper.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StaffHelperConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("staffhelper.json");

    public boolean nickSearchEnabled = true;
    public int nickWidgetX = 8;
    public int nickWidgetY = 8;
    public List<String> nickPatterns = new ArrayList<>();
    public List<String> nickIgnoreNicks = new ArrayList<>();

    public int vanishWidgetX = 8;
    public int vanishWidgetY = 40;

    public boolean statsEnabled = true;
    public int statsWidgetX = 8;
    public int statsWidgetY = 8;
    public boolean statsShowRole = true;
    public boolean statsShowPing = true;
    public boolean statsShowTps = true;
    public boolean statsHorizontal = true;
    public boolean statsShowTpsNow = true;
    public boolean statsShowTps5m = true;
    public boolean statsShowTps10m = false;
    public boolean statsShowTps15m = false;
    public float statsBoxScale = 1.0f;
    public float nickBoxScale = 1.0f;
    public float vanishBoxScale = 1.0f;
    public float afkBoxScale = 1.0f;

    public int autoBoxSelection = 0;
    public String autoBoxCommandBox1 = "/move boxsmp";
    public String autoBoxCommandBox2 = "/move box2";

    public String uiTheme = "BLUE";
    public int uiCustomColor1 = 0x2D4A73;
    public int uiCustomColor2 = 0x5F8FD6;
    public List<UiGradientStop> uiCustomGradientStops = defaultCustomGradientStops(uiCustomColor1, uiCustomColor2);
    public float uiCustomGradientAngle = 90.0f;

    public boolean afkZoneEnabled = true;
    public boolean afkOutlineEnabled = true;
    public boolean afkFillEnabled = false;

    public int afkX1 = 0;
    public int afkY1 = 64;
    public int afkZ1 = 0;
    public int afkX2 = 0;
    public int afkY2 = 64;
    public int afkZ2 = 0;

    public int afkListX = 8;
    public int afkListY = 90;

    public List<String> afkIgnoreNicks = new ArrayList<>();
    public List<CommandBuilderEntry> commandBuilders = new ArrayList<>();

    public static StaffHelperConfig load() {
        try {
            if (Files.exists(FILE)) {
                String json = Files.readString(FILE);
                StaffHelperConfig cfg = GSON.fromJson(json, StaffHelperConfig.class);
                if (cfg != null) {
                    cfg.normalize();
                    return cfg;
                }
            }
        } catch (Exception ignored) {}
        return new StaffHelperConfig();
    }

    public void save() {
        try {
            normalize();
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(this));
        } catch (IOException ignored) {}
    }

    private void normalize() {
        if (nickPatterns == null) nickPatterns = new ArrayList<>();
        if (nickIgnoreNicks == null) nickIgnoreNicks = new ArrayList<>();
        if (afkIgnoreNicks == null) afkIgnoreNicks = new ArrayList<>();
        if (commandBuilders == null) commandBuilders = new ArrayList<>();

        uiTheme = normalizeTheme(uiTheme);
        uiCustomColor1 = clampRgb(uiCustomColor1, 0x2D4A73);
        uiCustomColor2 = clampRgb(uiCustomColor2, 0x5F8FD6);
        uiCustomGradientStops = normalizeCustomGradientStops(uiCustomGradientStops, uiCustomColor1, uiCustomColor2);
        uiCustomGradientAngle = normalizeAngle(uiCustomGradientAngle);
        if (!uiCustomGradientStops.isEmpty()) {
            uiCustomColor1 = uiCustomGradientStops.get(0).color;
            uiCustomColor2 = uiCustomGradientStops.get(uiCustomGradientStops.size() - 1).color;
        }

        autoBoxSelection = clampAutoBoxSelection(autoBoxSelection);
        autoBoxCommandBox1 = safe(autoBoxCommandBox1);
        autoBoxCommandBox2 = safe(autoBoxCommandBox2);

        for (CommandBuilderEntry entry : commandBuilders) {
            if (entry == null) continue;
            if (entry.name == null) entry.name = "";
            if (entry.alias == null) entry.alias = "";
            if (entry.execute == null) entry.execute = "";
            if (entry.timeOptions == null) entry.timeOptions = "";
            if (entry.reasonOptions == null) entry.reasonOptions = "";
        }
    }

    private static String normalizeTheme(String value) {
        String v = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        return switch (v) {
            case "BLUE", "RED", "PURPLE", "ORANGE", "GREEN", "BRIGHT_PURPLE", "PINK", "CUSTOM" -> v;
            default -> "BLUE";
        };
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static int clampAutoBoxSelection(int value) {
        if (value < 0) return 0;
        if (value > 2) return 0;
        return value;
    }

    private static int clampRgb(int value, int fallback) {
        if (value < 0 || value > 0xFFFFFF) return fallback;
        return value;
    }

    private static List<UiGradientStop> defaultCustomGradientStops(int color1, int color2) {
        List<UiGradientStop> out = new ArrayList<>();
        out.add(new UiGradientStop(0.0f, clampRgb(color1, 0x2D4A73)));
        out.add(new UiGradientStop(1.0f, clampRgb(color2, 0x5F8FD6)));
        return out;
    }

    private static List<UiGradientStop> normalizeCustomGradientStops(List<UiGradientStop> source, int fallback1, int fallback2) {
        int c1 = clampRgb(fallback1, 0x2D4A73);
        int c2 = clampRgb(fallback2, 0x5F8FD6);

        if ((c1 != 0x2D4A73 || c2 != 0x5F8FD6) && looksLikeDefaultStops(source)) {
            return defaultCustomGradientStops(c1, c2);
        }

        List<UiGradientStop> out = new ArrayList<>();
        if (source != null) {
            for (UiGradientStop stop : source) {
                if (stop == null) continue;
                UiGradientStop clean = new UiGradientStop();
                clean.position = clamp01(stop.position);
                clean.color = clampRgb(stop.color, clean.position <= 0.5f ? c1 : c2);
                out.add(clean);
                if (out.size() >= 10) break;
            }
        }

        if (out.isEmpty()) {
            return defaultCustomGradientStops(c1, c2);
        }

        out.sort((a, b) -> Float.compare(a.position, b.position));
        if (out.size() == 1) {
            UiGradientStop only = out.get(0);
            float secondPos = only.position < 0.5f ? 1.0f : 0.0f;
            int secondColor = only.position < 0.5f ? c2 : c1;
            out.add(new UiGradientStop(secondPos, secondColor));
            out.sort((a, b) -> Float.compare(a.position, b.position));
        }
        return out;
    }

    private static boolean looksLikeDefaultStops(List<UiGradientStop> source) {
        if (source == null || source.size() != 2) return false;
        UiGradientStop a = source.get(0);
        UiGradientStop b = source.get(1);
        if (a == null || b == null) return false;

        float p0 = clamp01(a.position);
        float p1 = clamp01(b.position);
        int c0 = clampRgb(a.color, 0x2D4A73);
        int c1 = clampRgb(b.color, 0x5F8FD6);
        return Math.abs(p0 - 0.0f) < 0.0001f
                && Math.abs(p1 - 1.0f) < 0.0001f
                && c0 == 0x2D4A73
                && c1 == 0x5F8FD6;
    }

    private static float clamp01(float value) {
        if (Float.isNaN(value)) return 0.0f;
        if (value < 0.0f) return 0.0f;
        return Math.min(1.0f, value);
    }

    private static float normalizeAngle(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) return 90.0f;
        float out = value % 360.0f;
        if (out < 0.0f) out += 360.0f;
        return out;
    }

    public static class CommandBuilderEntry {
        public String name = "";
        public String alias = "";
        public String execute = "";
        public String timeOptions = "";
        public String reasonOptions = "";
        public boolean expanded = true;

        public boolean hasExecuteToken(String token) {
            if (token == null || token.isBlank()) return false;
            return execute != null && execute.toLowerCase(Locale.ROOT).contains(token.toLowerCase(Locale.ROOT));
        }
    }

    public static class UiGradientStop {
        public float position = 0.0f;
        public int color = 0xFFFFFF;

        public UiGradientStop() {}

        public UiGradientStop(float position, int color) {
            this.position = position;
            this.color = color;
        }
    }
}
