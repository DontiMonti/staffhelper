package com.dmsh.staffhelper.gui.util;

import com.dmsh.staffhelper.StaffHelperState;
import com.dmsh.staffhelper.config.StaffHelperConfig;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public final class UiChrome {
    private UiChrome() {}
    private static final Identifier UI_FONT = Identifier.of("staffhelper", "inter");

    public static Text uiText(Text text) {
        if (text == null) {
            return uiLiteral("");
        }
        return text.copy().styled(style -> style.withFont(UI_FONT));
    }

    public static MutableText uiLiteral(String value) {
        return Text.literal(value == null ? "" : value).setStyle(Style.EMPTY.withFont(UI_FONT));
    }

    public static void drawText(DrawContext ctx, TextRenderer renderer, Text text, int x, int y, int color, boolean shadow) {
        if (ctx == null || renderer == null) return;
        ctx.drawText(renderer, uiText(text), x, y, color, shadow);
    }

    public static void drawPanel(DrawContext ctx, int x, int y, int w, int h, int radius, long nowMs) {
        drawPanel(ctx, x, y, w, h, radius, nowMs, 0.0f, true, true);
    }

    public static void drawPanel(
            DrawContext ctx,
            int x,
            int y,
            int w,
            int h,
            int radius,
            long nowMs,
            float accentBoost,
            boolean shadow
    ) {
        drawPanel(ctx, x, y, w, h, radius, nowMs, accentBoost, shadow, true);
    }

    public static void drawPanel(
            DrawContext ctx,
            int x,
            int y,
            int w,
            int h,
            int radius,
            long nowMs,
            float accentBoost,
            boolean shadow,
            boolean sheen
    ) {
        if (w <= 0 || h <= 0) return;
        boolean animated = isUiSheenAnimationEnabled();
        float pulse = animated ? (float) ((Math.sin(nowMs / 600.0) + 1.0) * 0.5) : 0.5f;
        float accent = clamp01(0.40f + (accentBoost * 0.65f) + (pulse * 0.12f));

        ThemePalette palette = getThemePalette();

        int top = ModernGui.argb(228, palette.topR, palette.topG, palette.topB);
        int bottom = ModernGui.argb(232, palette.bottomR, palette.bottomG, palette.bottomB);

        int neutralOutline = ModernGui.argb(170, 58, 58, 66);
        int accentOutline = ModernGui.argb(185, palette.accentR, palette.accentG, palette.accentB);
        int border = ModernGui.lerpColor(neutralOutline, accentOutline, accent);
        int inner = ModernGui.argb(24 + (int) (18 * (0.35f + pulse * 0.65f)), 255, 255, 255);

        if (shadow) {
            ModernGui.shadow(ctx, x, y, w, h, radius, 1, ModernGui.argb(98, 0, 0, 0));
        }

        ModernGui.roundedVerticalGradient(ctx, x, y, w, h, radius, top, bottom);
        ModernGui.roundedOutline(ctx, x, y, w, h, radius, border);
        ModernGui.roundedOutline(ctx, x + 1, y + 1, w - 2, h - 2, Math.max(0, radius - 1), inner);
        ModernGui.topHighlight(ctx, x, y, w, radius, ModernGui.argb(46, 255, 255, 255));

        if (h >= 56) {
            int divider = ModernGui.lerpColor(outlineColor(84), accentColor(84), accent * 0.45f);
            ctx.fill(x + 2, y + 28, x + w - 2, y + 29, divider);
        }

        if (sheen && animated) {
            drawSheen(ctx, x, y, w, h, nowMs);
        }
    }

    public static void drawHudPanel(
            DrawContext ctx,
            int x,
            int y,
            int w,
            int h,
            int radius,
            int headerHeight,
            long nowMs
    ) {
        drawHudPanel(ctx, x, y, w, h, radius, headerHeight, nowMs, 0.0f, true);
    }

    public static void drawHudPanel(
            DrawContext ctx,
            int x,
            int y,
            int w,
            int h,
            int radius,
            int headerHeight,
            long nowMs,
            float accentBoost,
            boolean shadow
    ) {
        drawPanel(ctx, x, y, w, h, radius, nowMs, accentBoost, shadow, false);

        int innerW = w - 2;
        int maxHeader = h - 2;
        int hh = Math.max(0, Math.min(headerHeight, maxHeader));
        if (innerW <= 0 || hh <= 0) return;

        ThemePalette palette = getThemePalette();
        float tint = clamp01(0.08f + ((accentBoost + 0.45f) * 0.22f));
        int baseHeader = ModernGui.argb(162, 17, 19, 25);
        int accentHeader = ModernGui.argb(162, palette.accentR, palette.accentG, palette.accentB);
        int headerColor = ModernGui.lerpColor(baseHeader, accentHeader, tint);
        int bottomShade = ModernGui.argb(56, 0, 0, 0);
        int divider = ModernGui.lerpColor(outlineColor(116), accentColor(132), 0.22f + (tint * 0.24f));

        GuiRenderUtils.roundedRectTop(ctx, x + 1, y + 1, x + w - 1, y + 1 + hh, Math.max(0, radius - 1), headerColor);
        if (hh >= 4) {
            ctx.fill(x + 1, y + hh - 3, x + w - 1, y + hh, bottomShade);
        }
        ctx.fill(x + 2, y + hh, x + w - 2, y + hh + 1, divider);
    }

    public static void drawHudHeaderBadge(DrawContext ctx, int x, int y, int size, boolean active) {
        if (size <= 1) return;
        int radius = Math.max(2, size / 3);
        int fill = active ? accentColor(70) : outlineColor(96);
        int border = active ? accentColor(190) : outlineColor(168);
        int dot = active ? accentColor(255) : mainTextColor(220);
        int dotSize = Math.max(2, size / 3);
        int dotX = x + ((size - dotSize) / 2);
        int dotY = y + ((size - dotSize) / 2);

        ModernGui.roundedRect(ctx, x, y, size, size, radius, fill);
        ModernGui.roundedOutline(ctx, x, y, size, size, radius, border);
        ctx.fill(dotX, dotY, dotX + dotSize, dotY + dotSize, dot);
    }

    private static void drawSheen(DrawContext ctx, int x, int y, int w, int h, long nowMs) {
        int inset = 2;
        int ix = x + inset;
        int iy = y + inset;
        int iw = w - inset * 2;
        int ih = h - inset * 2;
        if (iw <= 0 || ih <= 0) return;

        ctx.enableScissor(ix, iy, ix + iw, iy + ih);
        int sweep = ix - 20 + (int) (((nowMs % 2300L) / 2300.0) * (iw + 40));
        int c1 = ModernGui.argb(8, 255, 255, 255);
        int c2 = ModernGui.argb(16, 255, 255, 255);
        for (int i = 0; i < ih; i++) {
            int yy = iy + i;
            int xx = sweep + (i / 2);
            ctx.fill(xx, yy, xx + 1, yy + 1, c1);
            ctx.fill(xx + 1, yy, xx + 2, yy + 1, c2);
            ctx.fill(xx + 2, yy, xx + 3, yy + 1, c1);
        }
        ctx.disableScissor();
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : Math.min(v, 1f);
    }

    private static float normalizeAngle(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) return 90.0f;
        float out = value % 360.0f;
        if (out < 0.0f) out += 360.0f;
        return out;
    }

    private static int clamp255(int value) {
        if (value < 0) return 0;
        return Math.min(255, value);
    }

    public static int accentColor(int alpha) {
        ThemePalette palette = getThemePalette();
        return ModernGui.argb(clamp255(alpha), palette.accentR, palette.accentG, palette.accentB);
    }

    public static int outlineColor(int alpha) {
        return ModernGui.argb(clamp255(alpha), 58, 58, 66);
    }

    public static int mutedTextColor(int alpha) {
        return ModernGui.argb(clamp255(alpha), 152, 152, 164);
    }

    public static int mainTextColor(int alpha) {
        return ModernGui.argb(clamp255(alpha), 217, 217, 226);
    }

    private static ThemePalette getThemePalette() {
        String theme = (StaffHelperState.CONFIG != null && StaffHelperState.CONFIG.uiTheme != null)
                ? StaffHelperState.CONFIG.uiTheme.trim().toUpperCase()
                : "BLUE";

        return switch (theme) {
            case "RED" -> tintedPalette(228, 121, 133);
            case "PURPLE" -> tintedPalette(191, 149, 243);
            case "ORANGE" -> tintedPalette(241, 177, 112);
            case "GREEN" -> tintedPalette(123, 212, 166);
            case "BRIGHT_PURPLE" -> tintedPalette(220, 102, 251);
            case "PINK" -> tintedPalette(247, 134, 181);
            case "CUSTOM" -> customThemePalette();
            default -> tintedPalette(132, 171, 243);
        };
    }

    private static ThemePalette tintedPalette(int accentR, int accentG, int accentB) {
        int accent = (accentR << 16) | (accentG << 8) | accentB;
        int top = mixRgb(0x1A1A20, accent, 0.08f);
        int bottom = mixRgb(0x141419, accent, 0.04f);
        return new ThemePalette(
                (top >> 16) & 0xFF, (top >> 8) & 0xFF, top & 0xFF,
                (bottom >> 16) & 0xFF, (bottom >> 8) & 0xFF, bottom & 0xFF,
                accentR, accentG, accentB
        );
    }

    private static ThemePalette customThemePalette() {
        StaffHelperConfig cfg = StaffHelperState.CONFIG;
        int c1 = cfg != null ? clampRgb(cfg.uiCustomColor1, 0x2D4A73) : 0x2D4A73;
        int c2 = cfg != null ? clampRgb(cfg.uiCustomColor2, 0x5F8FD6) : 0x5F8FD6;

        List<StaffHelperConfig.UiGradientStop> stops = normalizedCustomStops(cfg, c1, c2);
        float angleDeg = normalizeAngle(cfg != null ? cfg.uiCustomGradientAngle : 90.0f);
        float rad = (float) Math.toRadians(angleDeg);
        float dirX = (float) Math.cos(rad);
        float dirY = (float) Math.sin(rad);

        // Project gradient direction to panel colors so all custom stops affect the theme.
        float topSample = clamp01(0.5f + ((-0.44f * dirY) + (-0.20f * dirX)));
        float bottomSample = clamp01(0.5f + ((0.44f * dirY) + (0.20f * dirX)));
        if (Math.abs(bottomSample - topSample) < 0.06f) {
            topSample = clamp01(topSample - 0.12f);
            bottomSample = clamp01(bottomSample + 0.12f);
        }

        int topShade = sampleGradientColor(stops, topSample);
        int bottomShade = sampleGradientColor(stops, bottomSample);
        int accent = sampleGradientColor(stops, clamp01((topSample + bottomSample) * 0.5f));

        // Make custom palette tint stronger so added colors are visible in the menu.
        int top = mixRgb(0x1A1A20, topShade, 0.34f);
        int bottom = mixRgb(0x141419, bottomShade, 0.26f);

        return new ThemePalette(
                (top >> 16) & 0xFF, (top >> 8) & 0xFF, top & 0xFF,
                (bottom >> 16) & 0xFF, (bottom >> 8) & 0xFF, bottom & 0xFF,
                (accent >> 16) & 0xFF, (accent >> 8) & 0xFF, accent & 0xFF
        );
    }

    private static List<StaffHelperConfig.UiGradientStop> normalizedCustomStops(StaffHelperConfig cfg, int fallback1, int fallback2) {
        List<StaffHelperConfig.UiGradientStop> out = new ArrayList<>();
        if (cfg != null && cfg.uiCustomGradientStops != null) {
            for (StaffHelperConfig.UiGradientStop stop : cfg.uiCustomGradientStops) {
                if (stop == null) continue;
                StaffHelperConfig.UiGradientStop clean = new StaffHelperConfig.UiGradientStop();
                clean.position = clamp01(stop.position);
                clean.color = clampRgb(stop.color, clean.position <= 0.5f ? fallback1 : fallback2);
                out.add(clean);
                if (out.size() >= 12) break;
            }
        }

        if (out.isEmpty()) {
            out.add(new StaffHelperConfig.UiGradientStop(0.0f, fallback1));
            out.add(new StaffHelperConfig.UiGradientStop(1.0f, fallback2));
            return out;
        }

        out.sort((a, b) -> Float.compare(a.position, b.position));
        if (out.size() == 1) {
            StaffHelperConfig.UiGradientStop only = out.get(0);
            out.add(new StaffHelperConfig.UiGradientStop(only.position < 0.5f ? 1.0f : 0.0f, only.position < 0.5f ? fallback2 : fallback1));
            out.sort((a, b) -> Float.compare(a.position, b.position));
        }
        return out;
    }

    private static int sampleGradientColor(List<StaffHelperConfig.UiGradientStop> stops, float t) {
        if (stops == null || stops.isEmpty()) {
            return 0x4A6999;
        }
        float pos = clamp01(t);
        StaffHelperConfig.UiGradientStop first = stops.get(0);
        if (pos <= first.position) return first.color;

        StaffHelperConfig.UiGradientStop last = stops.get(stops.size() - 1);
        if (pos >= last.position) return last.color;

        for (int i = 0; i < stops.size() - 1; i++) {
            StaffHelperConfig.UiGradientStop a = stops.get(i);
            StaffHelperConfig.UiGradientStop b = stops.get(i + 1);
            if (pos < a.position || pos > b.position) continue;
            float span = Math.max(0.0001f, b.position - a.position);
            float local = clamp01((pos - a.position) / span);
            return mixRgb(a.color, b.color, local);
        }
        return last.color;
    }

    private static int clampRgb(int value, int fallback) {
        if (value < 0 || value > 0xFFFFFF) return fallback;
        return value;
    }

    private static int mixRgb(int a, int b, float t) {
        float k = clamp01(t);
        int ar = (a >> 16) & 0xFF;
        int ag = (a >> 8) & 0xFF;
        int ab = a & 0xFF;
        int br = (b >> 16) & 0xFF;
        int bg = (b >> 8) & 0xFF;
        int bb = b & 0xFF;

        int rr = (int) (ar + (br - ar) * k);
        int rg = (int) (ag + (bg - ag) * k);
        int rb = (int) (ab + (bb - ab) * k);
        return (rr << 16) | (rg << 8) | rb;
    }

    private static boolean isUiSheenAnimationEnabled() {
        if (StaffHelperState.CONFIG == null) return true;
        return StaffHelperState.CONFIG.uiSheenAnimationEnabled;
    }

    private record ThemePalette(
            int topR, int topG, int topB,
            int bottomR, int bottomG, int bottomB,
            int accentR, int accentG, int accentB
    ) {}
}
