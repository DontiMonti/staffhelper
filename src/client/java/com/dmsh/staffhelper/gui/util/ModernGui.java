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
        for (int i = spread; i >= 1; i--) {
            float k = (float)i / (float)spread;
            int a = (int)(((colorARGB>>>24)&255) * k * 0.35f);
            int c = (colorARGB & 0x00FFFFFF) | (a<<24);
            roundedRect(ctx, x - i, y - i, w + i*2, h + i*2, radius + i, c);
        }
    }

    public static void roundedRect(DrawContext ctx, int x, int y, int w, int h, int radius, int colorARGB) {
        if (w <= 0 || h <= 0) return;
        if (radius <= 0) {
            ctx.fill(x, y, x + w, y + h, colorARGB);
            return;
        }
        int r = Math.min(radius, Math.min(w, h) / 2);

        ctx.fill(x + r, y, x + w - r, y + h, colorARGB);

        ctx.fill(x, y + r, x + r, y + h - r, colorARGB);
        ctx.fill(x + w - r, y + r, x + w, y + h - r, colorARGB);

        for (int dy = 0; dy < r; dy++) {
            int yyTop = y + dy;
            int yyBot = y + h - 1 - dy;
            int dx = (int)Math.floor(Math.sqrt((double)r*r - (double)(r - dy)*(r - dy)));
            int left = x + r - dx;
            int right = x + w - r + dx;

            ctx.fill(left, yyTop, right, yyTop + 1, colorARGB);
            ctx.fill(left, yyBot, right, yyBot + 1, colorARGB);
        }
    }

    public static void roundedOutline(DrawContext ctx, int x, int y, int w, int h, int radius, int colorARGB) {

        roundedRect(ctx, x, y, w, 1, radius, colorARGB);
        roundedRect(ctx, x, y + h - 1, w, 1, radius, colorARGB);

        roundedRect(ctx, x, y, 1, h, radius, colorARGB);
        roundedRect(ctx, x + w - 1, y, 1, h, radius, colorARGB);
    }

    public static void roundedVerticalGradient(DrawContext ctx, int x, int y, int w, int h, int radius, int topColor, int bottomColor) {
        if (w <= 0 || h <= 0) return;
        int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));

        for (int i = 0; i < h; i++) {
            float t = (h <= 1) ? 1f : (float)i / (float)(h - 1);
            int c = lerpColor(topColor, bottomColor, t);

            int yy = y + i;

            int insetL = 0;
            int insetR = 0;

            if (r > 0) {
                if (i < r) {
                    int dy = i;
                    int dx = (int)Math.floor(Math.sqrt((double)r*r - (double)(r - dy)*(r - dy)));
                    insetL = r - dx;
                    insetR = r - dx;
                } else if (i >= h - r) {
                    int dy = h - 1 - i;
                    int dx = (int)Math.floor(Math.sqrt((double)r*r - (double)(r - dy)*(r - dy)));
                    insetL = r - dx;
                    insetR = r - dx;
                }
            }

            ctx.fill(x + insetL, yy, x + w - insetR, yy + 1, c);
        }
    }

    public static void topHighlight(DrawContext ctx, int x, int y, int w, int radius, int colorARGB) {
        roundedRect(ctx, x + 1, y + 1, w - 2, 2, radius, colorARGB);
    }
}
