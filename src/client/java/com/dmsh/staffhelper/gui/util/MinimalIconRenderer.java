package com.dmsh.staffhelper.gui.util;

import net.minecraft.client.gui.DrawContext;

public final class MinimalIconRenderer {
    private static final int GRID = 16;

    public enum Glyph {
        SEARCH,
        MAP_PIN,
        COMMAND,
        MODULES,
        SLIDERS,
        PROFILE,
        TAG,
        SIGNAL,
        TPS
    }

    private MinimalIconRenderer() {}

    public static void drawCentered(DrawContext ctx, Glyph glyph, int centerX, int centerY, int size, int color, int accent) {
        int clamped = Math.max(8, size);
        draw(ctx, glyph, centerX - (clamped / 2), centerY - (clamped / 2), clamped, color, accent);
    }

    public static void draw(DrawContext ctx, Glyph glyph, int x, int y, int size, int color, int accent) {
        if (ctx == null || glyph == null || size <= 0) return;
        GridPainter painter = new GridPainter(ctx, x, y, size);
        switch (glyph) {
            case SEARCH -> drawSearch(painter, color, accent);
            case MAP_PIN -> drawMapPin(painter, color, accent);
            case COMMAND -> drawCommand(painter, color, accent);
            case MODULES -> drawModules(painter, color, accent);
            case SLIDERS -> drawSliders(painter, color, accent);
            case PROFILE -> drawProfile(painter, color, accent);
            case TAG -> drawTag(painter, color, accent);
            case SIGNAL -> drawSignal(painter, color, accent);
            case TPS -> drawTps(painter, color, accent);
        }
    }

    private static void drawSearch(GridPainter p, int color, int accent) {
        p.outline(2, 2, 8, 8, 4, color);
        p.diagDown(8, 8, 4, color);
        p.dot(5, 5, 2, accent);
    }

    private static void drawMapPin(GridPainter p, int color, int accent) {
        p.outline(3, 1, 10, 10, 5, color);
        p.v(7, 8, 4, color);
        p.diagDown(6, 11, 2, color);
        p.diagUp(7, 12, 2, color);
        p.dot(6, 5, 2, accent);
    }

    private static void drawCommand(GridPainter p, int color, int accent) {
        p.diagDown(3, 4, 4, color);
        p.diagUp(3, 11, 4, color);
        p.h(9, 11, 4, accent);
    }

    private static void drawModules(GridPainter p, int color, int accent) {
        p.fill(2, 2, 4, 4, 2, color);
        p.fill(10, 2, 4, 4, 2, accent);
        p.fill(2, 10, 4, 4, 2, accent);
        p.fill(10, 10, 4, 4, 2, color);
    }

    private static void drawSliders(GridPainter p, int color, int accent) {
        p.h(2, 4, 12, color);
        p.h(2, 8, 12, color);
        p.h(2, 12, 12, color);
        p.dot(5, 3, 3, accent);
        p.dot(10, 7, 3, accent);
        p.dot(7, 11, 3, accent);
    }

    private static void drawProfile(GridPainter p, int color, int accent) {
        p.outline(5, 2, 6, 6, 3, color);
        p.fill(4, 10, 8, 2, 1, accent);
        p.fill(3, 11, 10, 3, 2, color);
    }

    private static void drawTag(GridPainter p, int color, int accent) {
        p.outline(2, 4, 9, 8, 3, color);
        p.diagDown(10, 4, 3, color);
        p.diagUp(10, 11, 3, color);
        p.dot(4, 7, 2, accent);
    }

    private static void drawSignal(GridPainter p, int color, int accent) {
        p.vBottom(2, 10, 14, color);
        p.vBottom(6, 8, 14, color);
        p.vBottom(10, 6, 14, accent);
        p.vBottom(13, 4, 14, accent);
    }

    private static void drawTps(GridPainter p, int color, int accent) {
        p.h(1, 10, 3, color);
        p.diagUp(4, 10, 3, accent);
        p.diagDown(7, 8, 4, accent);
        p.diagUp(10, 11, 3, color);
        p.h(12, 8, 3, color);
        p.dot(11, 7, 2, accent);
    }

    private static final class GridPainter {
        private final DrawContext ctx;
        private final int x;
        private final int y;
        private final int size;
        private final int stroke;

        private GridPainter(DrawContext ctx, int x, int y, int size) {
            this.ctx = ctx;
            this.x = x;
            this.y = y;
            this.size = Math.max(8, size);
            this.stroke = Math.max(1, Math.round(this.size / 8.0f));
        }

        private int cx(int unit) {
            return this.x + Math.round((unit / (float) GRID) * this.size);
        }

        private int cy(int unit) {
            return this.y + Math.round((unit / (float) GRID) * this.size);
        }

        private int span(int units) {
            return Math.max(1, Math.round((units / (float) GRID) * this.size));
        }

        private void fill(int ux, int uy, int uw, int uh, int radiusUnits, int color) {
            int x1 = cx(ux);
            int y1 = cy(uy);
            int x2 = cx(ux + uw);
            int y2 = cy(uy + uh);
            int w = Math.max(1, x2 - x1);
            int h = Math.max(1, y2 - y1);
            int radius = Math.max(1, Math.min(Math.min(w, h) / 2, span(radiusUnits)));
            ModernGui.roundedRect(ctx, x1, y1, w, h, radius, color);
        }

        private void outline(int ux, int uy, int uw, int uh, int radiusUnits, int color) {
            int x1 = cx(ux);
            int y1 = cy(uy);
            int x2 = cx(ux + uw);
            int y2 = cy(uy + uh);
            int w = Math.max(1, x2 - x1);
            int h = Math.max(1, y2 - y1);
            int radius = Math.max(1, Math.min(Math.min(w, h) / 2, span(radiusUnits)));
            GuiRenderUtils.roundedOutline(ctx, x1, y1, x2, y2, radius, stroke, color);
        }

        private void h(int ux, int uy, int lengthUnits, int color) {
            int x1 = cx(ux);
            int y1 = cy(uy);
            int x2 = cx(ux + lengthUnits);
            int w = Math.max(stroke, x2 - x1);
            ModernGui.roundedRect(ctx, x1, y1, w, stroke, Math.max(1, stroke / 2), color);
        }

        private void v(int ux, int uy, int heightUnits, int color) {
            int x1 = cx(ux);
            int y1 = cy(uy);
            int y2 = cy(uy + heightUnits);
            int h = Math.max(stroke, y2 - y1);
            ModernGui.roundedRect(ctx, x1, y1, stroke, h, Math.max(1, stroke / 2), color);
        }

        private void vBottom(int ux, int topUnit, int bottomUnit, int color) {
            int x1 = cx(ux);
            int y1 = cy(topUnit);
            int y2 = cy(bottomUnit);
            int h = Math.max(stroke, y2 - y1);
            ModernGui.roundedRect(ctx, x1, y1, stroke, h, Math.max(1, stroke / 2), color);
        }

        private void dot(int ux, int uy, int units, int color) {
            int sizePx = Math.max(stroke, span(units));
            int x1 = cx(ux);
            int y1 = cy(uy);
            ModernGui.roundedRect(ctx, x1, y1, sizePx, sizePx, Math.max(1, sizePx / 2), color);
        }

        private void diagDown(int ux, int uy, int steps, int color) {
            for (int i = 0; i < steps; i++) {
                dot(ux + i, uy + i, stroke, color);
            }
        }

        private void diagUp(int ux, int uy, int steps, int color) {
            for (int i = 0; i < steps; i++) {
                dot(ux + i, uy - i, stroke, color);
            }
        }
    }
}
