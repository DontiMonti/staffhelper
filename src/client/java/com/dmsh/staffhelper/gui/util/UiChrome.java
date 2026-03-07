package com.dmsh.staffhelper.gui.util;

import com.dmsh.staffhelper.StaffHelperState;
import com.dmsh.staffhelper.config.StaffHelperConfig;
import net.minecraft.client.gui.DrawContext;

/**
 * Shared animated "modern cheat-like" panel style for HUD/widgets.
 */
public final class UiChrome {
    private UiChrome() {}

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
        boolean animated = isUiSheenAnimationEnabled();
        float pulse = animated ? (float) ((Math.sin(nowMs / 600.0) + 1.0) * 0.5) : 0.5f;
        float accent = clamp01(0.55f + accentBoost);

        ThemePalette palette = getThemePalette();

        int top = ModernGui.argb(215, palette.topR, palette.topG, palette.topB);
        int bottom = ModernGui.argb(215, palette.bottomR, palette.bottomG, palette.bottomB);
        int border = ModernGui.argb(90 + (int) (45 * pulse * accent), palette.accentR, palette.accentG, palette.accentB);
        int inner = ModernGui.argb(24 + (int) (16 * pulse * accent), 255, 255, 255);

        if (shadow) {
            // User-requested: very small shadow extent (1px).
            ModernGui.shadow(ctx, x, y, w, h, radius, 1, ModernGui.argb(125, 0, 0, 0));
        }

        ModernGui.roundedVerticalGradient(ctx, x, y, w, h, radius, top, bottom);
        ModernGui.roundedOutline(ctx, x, y, w, h, radius, border);
        ModernGui.roundedOutline(ctx, x + 1, y + 1, w - 2, h - 2, Math.max(0, radius - 1), inner);
        ModernGui.topHighlight(ctx, x, y, w, radius, ModernGui.argb(58, 255, 255, 255));

        if (sheen && animated) {
            drawSheen(ctx, x, y, w, h, nowMs);
        }
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
        int c1 = ModernGui.argb(14, 255, 255, 255);
        int c2 = ModernGui.argb(30, 255, 255, 255);
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

    private static ThemePalette getThemePalette() {
        String theme = (StaffHelperState.CONFIG != null && StaffHelperState.CONFIG.uiTheme != null)
                ? StaffHelperState.CONFIG.uiTheme.trim().toUpperCase()
                : "BLUE";

        return switch (theme) {
            case "RED" -> new ThemePalette(48, 33, 36, 30, 18, 20, 240, 134, 143);
            case "PURPLE" -> new ThemePalette(43, 36, 54, 25, 20, 34, 196, 157, 240);
            case "ORANGE" -> new ThemePalette(50, 40, 30, 30, 22, 16, 242, 181, 120);
            case "GREEN" -> new ThemePalette(33, 48, 40, 18, 30, 25, 120, 224, 170);
            case "BRIGHT_PURPLE" -> new ThemePalette(48, 34, 62, 27, 19, 40, 228, 104, 255);
            case "PINK" -> new ThemePalette(56, 36, 49, 33, 20, 30, 255, 143, 193);
            case "CUSTOM" -> customThemePalette();
            default -> new ThemePalette(35, 40, 52, 20, 24, 34, 134, 178, 240);
        };
    }

    private static ThemePalette customThemePalette() {
        StaffHelperConfig cfg = StaffHelperState.CONFIG;
        int c1 = cfg != null ? clampRgb(cfg.uiCustomColor1, 0x2D4A73) : 0x2D4A73;
        int c2 = cfg != null ? clampRgb(cfg.uiCustomColor2, 0x5F8FD6) : 0x5F8FD6;

        int top = mixRgb(c1, 0x14181F, 0.46f);
        int bottom = mixRgb(c2, 0x0C0F14, 0.68f);
        int accent = mixRgb(c1, c2, 0.55f);

        return new ThemePalette(
                (top >> 16) & 0xFF, (top >> 8) & 0xFF, top & 0xFF,
                (bottom >> 16) & 0xFF, (bottom >> 8) & 0xFF, bottom & 0xFF,
                (accent >> 16) & 0xFF, (accent >> 8) & 0xFF, accent & 0xFF
        );
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
