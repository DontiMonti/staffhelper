package com.dmsh.staffhelper.gui.util;

import net.minecraft.client.gui.DrawContext;

public final class GuiRenderUtils {

    private GuiRenderUtils() {}

    public static void roundedRect(DrawContext ctx, int x1, int y1, int x2, int y2, int r, int argb) {
        if (r <= 0) {
            ctx.fill(x1, y1, x2, y2, argb);
            return;
        }

        int w = x2 - x1;
        int h = y2 - y1;
        int rr = Math.min(r, Math.min(w, h) / 2);

        ctx.fill(x1 + rr, y1, x2 - rr, y2, argb);
        ctx.fill(x1, y1 + rr, x1 + rr, y2 - rr, argb);
        ctx.fill(x2 - rr, y1 + rr, x2, y2 - rr, argb);

        cornerTL(ctx, x1 + rr, y1 + rr, rr, argb);
        cornerTR(ctx, x2 - rr, y1 + rr, rr, argb);
        cornerBR(ctx, x2 - rr, y2 - rr, rr, argb);
        cornerBL(ctx, x1 + rr, y2 - rr, rr, argb);
    }

    public static void roundedRectTop(DrawContext ctx, int x1, int y1, int x2, int y2, int r, int argb) {
        if (r <= 0) {
            ctx.fill(x1, y1, x2, y2, argb);
            return;
        }
        int w = x2 - x1;
        int h = y2 - y1;
        int rr = Math.min(r, Math.min(w, h) / 2);

        ctx.fill(x1, y1 + rr, x2, y2, argb);

        ctx.fill(x1 + rr, y1, x2 - rr, y1 + rr, argb);

        cornerTL(ctx, x1 + rr, y1 + rr, rr, argb);
        cornerTR(ctx, x2 - rr, y1 + rr, rr, argb);
    }

    public static void roundedRectBottom(DrawContext ctx, int x1, int y1, int x2, int y2, int r, int argb) {
        if (r <= 0) {
            ctx.fill(x1, y1, x2, y2, argb);
            return;
        }
        int w = x2 - x1;
        int h = y2 - y1;
        int rr = Math.min(r, Math.min(w, h) / 2);

        ctx.fill(x1, y1, x2, y2 - rr, argb);

        ctx.fill(x1 + rr, y2 - rr, x2 - rr, y2, argb);

        cornerBL(ctx, x1 + rr, y2 - rr, rr, argb);
        cornerBR(ctx, x2 - rr, y2 - rr, rr, argb);
    }

    public static void roundedOutline(DrawContext ctx, int x1, int y1, int x2, int y2, int r, int thickness, int argb) {
        if (thickness <= 0) return;

        roundedRect(ctx, x1, y1, x2, y1 + thickness, r, argb);

        roundedRect(ctx, x1, y2 - thickness, x2, y2, r, argb);

        roundedRect(ctx, x1, y1, x1 + thickness, y2, r, argb);

        roundedRect(ctx, x2 - thickness, y1, x2, y2, r, argb);
    }

    public static void shadow(DrawContext ctx, int x1, int y1, int x2, int y2, int r, int layers, int maxAlpha) {
        layers = Math.max(1, Math.min(layers, 12));
        maxAlpha = Math.max(0, Math.min(maxAlpha, 255));
        for (int i = 1; i <= layers; i++) {
            int a = (int) (maxAlpha * (1f - (i - 1f) / layers));
            int color = (a << 24);
            roundedRect(ctx, x1 - i, y1 - i, x2 + i, y2 + i, r + i, color);
        }
    }

    private static void cornerTL(DrawContext ctx, int cx, int cy, int r, int argb) {

        for (int y = 0; y < r; y++) {
            int dy = r - 1 - y;
            int dx = (int) Math.ceil(Math.sqrt((double) r * (double) r - (double) dy * (double) dy));
            int yPix = cy - r + y;
            ctx.fill(cx - r, yPix, cx - r + dx, yPix + 1, argb);
        }
    }

    private static void cornerTR(DrawContext ctx, int cx, int cy, int r, int argb) {

        for (int y = 0; y < r; y++) {
            int dy = r - 1 - y;
            int dx = (int) Math.ceil(Math.sqrt((double) r * (double) r - (double) dy * (double) dy));
            int yPix = cy - r + y;
            ctx.fill(cx + r - dx, yPix, cx + r, yPix + 1, argb);
        }
    }

    private static void cornerBR(DrawContext ctx, int cx, int cy, int r, int argb) {

        for (int y = 0; y < r; y++) {
            int dy = y;
            int dx = (int) Math.ceil(Math.sqrt((double) r * (double) r - (double) dy * (double) dy));
            int yPix = cy + y;
            ctx.fill(cx + r - dx, yPix, cx + r, yPix + 1, argb);
        }
    }

    private static void cornerBL(DrawContext ctx, int cx, int cy, int r, int argb) {

        for (int y = 0; y < r; y++) {
            int dy = y;
            int dx = (int) Math.ceil(Math.sqrt((double) r * (double) r - (double) dy * (double) dy));
            int yPix = cy + y;
            ctx.fill(cx - r, yPix, cx - r + dx, yPix + 1, argb);
        }
    }
}
