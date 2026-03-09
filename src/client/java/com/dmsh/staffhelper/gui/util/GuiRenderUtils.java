package com.dmsh.staffhelper.gui.util;

import net.minecraft.client.gui.DrawContext;

public final class GuiRenderUtils {

    private GuiRenderUtils() {}

    public static void roundedRect(DrawContext ctx, int x1, int y1, int x2, int y2, int r, int argb) {
        int w = x2 - x1;
        int h = y2 - y1;
        if (w <= 0 || h <= 0) return;
        ModernGui.roundedRect(ctx, x1, y1, w, h, Math.max(0, r), argb);
    }

    public static void roundedRectTop(DrawContext ctx, int x1, int y1, int x2, int y2, int r, int argb) {
        int w = x2 - x1;
        int h = y2 - y1;
        if (w <= 0 || h <= 0) return;

        int rr = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        if (rr <= 0) {
            ctx.fill(x1, y1, x2, y2, argb);
            return;
        }

        int bandBottom = Math.min(y2, y1 + rr + 2);
        try {
            ctx.enableScissor(x1, y1, x2, bandBottom);
            ModernGui.roundedRect(ctx, x1, y1, w, h, rr, argb);
        } finally {
            ctx.disableScissor();
        }
        int flatStart = Math.min(y2, y1 + rr);
        if (flatStart < y2) {
            ctx.fill(x1, flatStart, x2, y2, argb);
        }
    }

    public static void roundedRectBottom(DrawContext ctx, int x1, int y1, int x2, int y2, int r, int argb) {
        int w = x2 - x1;
        int h = y2 - y1;
        if (w <= 0 || h <= 0) return;

        int rr = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        if (rr <= 0) {
            ctx.fill(x1, y1, x2, y2, argb);
            return;
        }

        int bandTop = Math.max(y1, y2 - rr - 2);
        try {
            ctx.enableScissor(x1, bandTop, x2, y2);
            ModernGui.roundedRect(ctx, x1, y1, w, h, rr, argb);
        } finally {
            ctx.disableScissor();
        }
        int flatEnd = Math.max(y1, y2 - rr);
        if (flatEnd > y1) {
            ctx.fill(x1, y1, x2, flatEnd, argb);
        }
    }

    public static void roundedOutline(DrawContext ctx, int x1, int y1, int x2, int y2, int r, int thickness, int argb) {
        int w = x2 - x1;
        int h = y2 - y1;
        if (w <= 0 || h <= 0 || thickness <= 0) return;

        int layers = Math.max(1, Math.min(6, thickness));
        int alpha = (argb >>> 24) & 0xFF;
        for (int i = 0; i < layers; i++) {
            int nx = x1 + i;
            int ny = y1 + i;
            int nw = w - (i * 2);
            int nh = h - (i * 2);
            if (nw <= 0 || nh <= 0) break;

            float fade = 1.0f - (i / (float) Math.max(1, layers)) * 0.40f;
            int layerAlpha = clamp255(Math.round(alpha * fade));
            int layerColor = (layerAlpha << 24) | (argb & 0x00FFFFFF);
            SmoothUiShader.drawRoundedOutline(ctx, nx, ny, nw, nh, Math.max(0, r - i), 1, layerColor);
        }
    }

    public static void shadow(DrawContext ctx, int x1, int y1, int x2, int y2, int r, int layers, int maxAlpha) {
        int w = x2 - x1;
        int h = y2 - y1;
        if (w <= 0 || h <= 0) return;

        int spread = Math.max(1, Math.min(16, layers));
        int color = (clamp255(maxAlpha) << 24);
        SmoothUiShader.drawRoundedGlow(ctx, x1, y1, w, h, r, spread, color);
    }

    private static int clamp255(int value) {
        if (value < 0) return 0;
        return Math.min(255, value);
    }
}
