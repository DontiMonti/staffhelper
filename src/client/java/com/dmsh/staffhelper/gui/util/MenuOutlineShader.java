package com.dmsh.staffhelper.gui.util;

import com.dmsh.staffhelper.StaffHelper;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public final class MenuOutlineShader {
    private static final RenderPipeline MENU_OUTLINE_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.GUI_SNIPPET, RenderPipelines.GLOBALS_SNIPPET)
                    .withLocation(Identifier.of(StaffHelper.MOD_ID, "pipeline/menu_outline"))
                    .withFragmentShader(Identifier.of(StaffHelper.MOD_ID, "core/menu_outline"))
                    .build()
    );

    private MenuOutlineShader() {}

    public static void init() {
        // Static initializer already registers the pipeline; this method forces class loading.
    }

    public static void roundedOutline(DrawContext ctx, int x, int y, int w, int h, int radius, int colorArgb) {
        if (w <= 0 || h <= 0) return;
        roundedRect(ctx, x, y, w, 1, radius, colorArgb);
        roundedRect(ctx, x, y + h - 1, w, 1, radius, colorArgb);
        roundedRect(ctx, x, y, 1, h, radius, colorArgb);
        roundedRect(ctx, x + w - 1, y, 1, h, radius, colorArgb);
    }

    private static void roundedRect(DrawContext ctx, int x, int y, int w, int h, int radius, int colorArgb) {
        if (w <= 0 || h <= 0) return;
        if (radius <= 0) {
            shaderFill(ctx, x, y, x + w, y + h, colorArgb);
            return;
        }

        int r = Math.min(radius, Math.min(w, h) / 2);
        shaderFill(ctx, x + r, y, x + w - r, y + h, colorArgb);
        shaderFill(ctx, x, y + r, x + r, y + h - r, colorArgb);
        shaderFill(ctx, x + w - r, y + r, x + w, y + h - r, colorArgb);

        for (int dy = 0; dy < r; dy++) {
            int yyTop = y + dy;
            int yyBottom = y + h - 1 - dy;
            int dx = (int) Math.floor(Math.sqrt((double) r * r - (double) (r - dy) * (r - dy)));
            int left = x + r - dx;
            int right = x + w - r + dx;

            shaderFill(ctx, left, yyTop, right, yyTop + 1, colorArgb);
            shaderFill(ctx, left, yyBottom, right, yyBottom + 1, colorArgb);
        }
    }

    private static void shaderFill(DrawContext ctx, int x1, int y1, int x2, int y2, int colorArgb) {
        if (x1 >= x2 || y1 >= y2) return;
        try {
            ctx.fill(MENU_OUTLINE_PIPELINE, x1, y1, x2, y2, colorArgb);
        } catch (Throwable ignored) {
            ctx.fill(x1, y1, x2, y2, colorArgb);
        }
    }
}
