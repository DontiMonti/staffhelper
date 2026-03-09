package com.dmsh.staffhelper.gui.util;

import net.minecraft.client.gui.DrawContext;

public final class ModernGui {
    private ModernGui() {}

    public static int argb(int a, int r, int g, int b) {
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    public static int lerpColor(int c1, int c2, float t) {
        t = clamp01(t);
        int a1=(c1>>>24)&255, r1=(c1>>>16)&255, g1=(c1>>>8)&255, b1=c1&255;
        int a2=(c2>>>24)&255, r2=(c2>>>16)&255, g2=(c2>>>8)&255, b2=c2&255;
        int a = (int)(a1 + (a2-a1)*t);
        int r = (int)(r1 + (r2-r1)*t);
        int g = (int)(g1 + (g2-g1)*t);
        int b = (int)(b1 + (b2-b1)*t);
        return (a<<24)|(r<<16)|(g<<8)|b;
    }

    public static float clamp01(float v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }

    public static float easeOut(float t) {
        t = clamp01(t);
        float p = 1f - t;
        return 1f - p*p*p;
    }

    public static void shadow(DrawContext ctx, int x, int y, int w, int h, int radius, int spread, int colorARGB) {
        if (w <= 0 || h <= 0 || spread <= 0) return;
        int alpha = (colorARGB >>> 24) & 0xFF;
        if (alpha <= 0) return;
        int glowAlpha = Math.max(1, Math.round(alpha * 0.30f));
        int glowColor = (glowAlpha << 24) | (colorARGB & 0x00FFFFFF);
        SmoothUiShader.drawRoundedGlow(ctx, x, y, w, h, radius, spread, glowColor);
    }

    public static void roundedRect(DrawContext ctx, int x, int y, int w, int h, int radius, int colorARGB) {
        if (w <= 0 || h <= 0) return;
        if (radius <= 0) {
            ctx.fill(x, y, x + w, y + h, colorARGB);
            return;
        }
        SmoothUiShader.drawRoundedFill(ctx, x, y, w, h, radius, colorARGB);
    }

    public static void roundedOutline(DrawContext ctx, int x, int y, int w, int h, int radius, int colorARGB) {
        if (w <= 0 || h <= 0) return;
        SmoothUiShader.drawRoundedOutline(ctx, x, y, w, h, radius, 1, colorARGB);
    }

    public static void roundedVerticalGradient(DrawContext ctx, int x, int y, int w, int h, int radius, int topColor, int bottomColor) {
        if (w <= 0 || h <= 0) return;
        SmoothUiShader.drawRoundedGradient(ctx, x, y, w, h, radius, topColor, bottomColor);
    }

    public static void topHighlight(DrawContext ctx, int x, int y, int w, int radius, int colorARGB) {
        if (w <= 2) return;
        int transparent = colorARGB & 0x00FFFFFF;
        SmoothUiShader.drawRoundedGradient(ctx, x + 1, y + 1, w - 2, 3, radius, colorARGB, transparent);
    }
}
