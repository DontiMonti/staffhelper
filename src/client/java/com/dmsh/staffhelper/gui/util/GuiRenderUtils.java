package com.dmsh.staffhelper.gui.util;

import net.minecraft.client.gui.DrawContext;

/**
 * Small rendering helpers for a "modern/rounded" UI style.
 * Purely visual; does not change widget layout.
 */
public final class GuiRenderUtils {

    private GuiRenderUtils() {}

    /**
     * Draw a rounded rectangle (filled).
     *
     * @param ctx DrawContext
     * @param x1 left
     * @param y1 top
     * @param x2 right (exclusive-ish, but works like fill)
     * @param y2 bottom
     * @param r  corner radius in pixels
     * @param argb ARGB color
     */
    public static void roundedRect(DrawContext ctx, int x1, int y1, int x2, int y2, int r, int argb) {
        if (r <= 0) {
            ctx.fill(x1, y1, x2, y2, argb);
            return;
        }

        int w = x2 - x1;
        int h = y2 - y1;
        int rr = Math.min(r, Math.min(w, h) / 2);

        // Center + side rects
        ctx.fill(x1 + rr, y1, x2 - rr, y2, argb);
        ctx.fill(x1, y1 + rr, x1 + rr, y2 - rr, argb);
        ctx.fill(x2 - rr, y1 + rr, x2, y2 - rr, argb);

        // Corners (quarter-circles) — implemented via scanline fills.
        cornerTL(ctx, x1 + rr, y1 + rr, rr, argb);
        cornerTR(ctx, x2 - rr, y1 + rr, rr, argb);
        cornerBR(ctx, x2 - rr, y2 - rr, rr, argb);
        cornerBL(ctx, x1 + rr, y2 - rr, rr, argb);
    }

    /**
     * Rounded rectangle with ONLY top corners rounded.
     * Useful for drawing vertical gradients without creating a "dark half" seam.
     */
    public static void roundedRectTop(DrawContext ctx, int x1, int y1, int x2, int y2, int r, int argb) {
        if (r <= 0) {
            ctx.fill(x1, y1, x2, y2, argb);
            return;
        }
        int w = x2 - x1;
        int h = y2 - y1;
        int rr = Math.min(r, Math.min(w, h) / 2);

        // Fill main body (square bottom)
        ctx.fill(x1, y1 + rr, x2, y2, argb);
        // Top strip (excluding rounded cutouts)
        ctx.fill(x1 + rr, y1, x2 - rr, y1 + rr, argb);
        // Top corners
        cornerTL(ctx, x1 + rr, y1 + rr, rr, argb);
        cornerTR(ctx, x2 - rr, y1 + rr, rr, argb);
    }

    /**
     * Rounded rectangle with ONLY bottom corners rounded.
     */
    public static void roundedRectBottom(DrawContext ctx, int x1, int y1, int x2, int y2, int r, int argb) {
        if (r <= 0) {
            ctx.fill(x1, y1, x2, y2, argb);
            return;
        }
        int w = x2 - x1;
        int h = y2 - y1;
        int rr = Math.min(r, Math.min(w, h) / 2);

        // Fill main body (square top)
        ctx.fill(x1, y1, x2, y2 - rr, argb);
        // Bottom strip (excluding rounded cutouts)
        ctx.fill(x1 + rr, y2 - rr, x2 - rr, y2, argb);
        // Bottom corners
        cornerBL(ctx, x1 + rr, y2 - rr, rr, argb);
        cornerBR(ctx, x2 - rr, y2 - rr, rr, argb);
    }

    /**
     * Draw a rounded rectangle outline.
     * This uses four thin rounded rectangles to approximate an outline (fast & good enough).
     */
    public static void roundedOutline(DrawContext ctx, int x1, int y1, int x2, int y2, int r, int thickness, int argb) {
        if (thickness <= 0) return;
        // Top
        roundedRect(ctx, x1, y1, x2, y1 + thickness, r, argb);
        // Bottom
        roundedRect(ctx, x1, y2 - thickness, x2, y2, r, argb);
        // Left
        roundedRect(ctx, x1, y1, x1 + thickness, y2, r, argb);
        // Right
        roundedRect(ctx, x2 - thickness, y1, x2, y2, r, argb);
    }

    /**
     * Soft shadow by drawing multiple expanded rounded rects with fading alpha.
     */
    public static void shadow(DrawContext ctx, int x1, int y1, int x2, int y2, int r, int layers, int maxAlpha) {
        layers = Math.max(1, Math.min(layers, 12));
        maxAlpha = Math.max(0, Math.min(maxAlpha, 255));
        for (int i = 1; i <= layers; i++) {
            int a = (int) (maxAlpha * (1f - (i - 1f) / layers));
            int color = (a << 24); // black
            roundedRect(ctx, x1 - i, y1 - i, x2 + i, y2 + i, r + i, color);
        }
    }

    // === Rounded corner helpers ===
    // We avoid low-level RenderSystem/Tessellator calls because the API changes frequently
    // between Minecraft versions. DrawContext#fill supports alpha and is stable.

    private static void cornerTL(DrawContext ctx, int cx, int cy, int r, int argb) {
        // Top-left quarter
        for (int y = 0; y < r; y++) {
            int dy = r - 1 - y;
            int dx = (int) Math.ceil(Math.sqrt((double) r * (double) r - (double) dy * (double) dy));
            int yPix = cy - r + y;
            ctx.fill(cx - r, yPix, cx - r + dx, yPix + 1, argb);
        }
    }

    private static void cornerTR(DrawContext ctx, int cx, int cy, int r, int argb) {
        // Top-right quarter
        for (int y = 0; y < r; y++) {
            int dy = r - 1 - y;
            int dx = (int) Math.ceil(Math.sqrt((double) r * (double) r - (double) dy * (double) dy));
            int yPix = cy - r + y;
            ctx.fill(cx + r - dx, yPix, cx + r, yPix + 1, argb);
        }
    }

    private static void cornerBR(DrawContext ctx, int cx, int cy, int r, int argb) {
        // Bottom-right quarter
        for (int y = 0; y < r; y++) {
            int dy = y;
            int dx = (int) Math.ceil(Math.sqrt((double) r * (double) r - (double) dy * (double) dy));
            int yPix = cy + y;
            ctx.fill(cx + r - dx, yPix, cx + r, yPix + 1, argb);
        }
    }

    private static void cornerBL(DrawContext ctx, int cx, int cy, int r, int argb) {
        // Bottom-left quarter
        for (int y = 0; y < r; y++) {
            int dy = y;
            int dx = (int) Math.ceil(Math.sqrt((double) r * (double) r - (double) dy * (double) dy));
            int yPix = cy + y;
            ctx.fill(cx - r, yPix, cx - r + dx, yPix + 1, argb);
        }
    }
}
