package com.dmsh.staffhelper.gui.util;

import com.dmsh.staffhelper.StaffHelper;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

public final class SmoothUiShader {
    private static final Identifier WHITE_TEXTURE = Identifier.of(StaffHelper.MOD_ID, "textures/gui/white.png");

    private static final RenderPipeline ROUNDED_FILL_PIPELINE = register("rounded_fill");
    private static final RenderPipeline ROUNDED_GRADIENT_PIPELINE = register("rounded_gradient");
    private static final RenderPipeline ROUNDED_OUTLINE_PIPELINE = register("rounded_outline");
    private static final RenderPipeline ROUNDED_GLOW_PIPELINE = register("rounded_glow");

    private SmoothUiShader() {}

    public static void init() {
        // Static fields register pipelines. Method is used to force class loading.
    }

    public static void drawRoundedFill(DrawContext ctx, int x, int y, int w, int h, int radius, int colorArgb) {
        drawRoundedQuad(ctx, ROUNDED_FILL_PIPELINE, x, y, w, h, radius, 0, colorArgb, false);
    }

    public static void drawRoundedGradient(DrawContext ctx, int x, int y, int w, int h, int radius, int topColor, int bottomColor) {
        drawRoundedQuad(ctx, ROUNDED_GRADIENT_PIPELINE, x, y, w, h, radius, 0, topColor, false);
        drawRoundedQuad(ctx, ROUNDED_GRADIENT_PIPELINE, x, y, w, h, radius, 0, bottomColor, true);
    }

    public static void drawRoundedOutline(DrawContext ctx, int x, int y, int w, int h, int radius, int thickness, int colorArgb) {
        int layers = Math.max(1, Math.min(4, thickness));
        int alpha = (colorArgb >>> 24) & 0xFF;
        for (int i = 0; i < layers; i++) {
            float fade = 1.0f - (i / (float) Math.max(1, layers)) * 0.45f;
            int layerColor = withAlpha(colorArgb, Math.round(alpha * fade));
            drawRoundedQuad(ctx, ROUNDED_OUTLINE_PIPELINE, x, y, w, h, radius, 1 + i, layerColor, false);
        }
    }

    public static void drawRoundedGlow(DrawContext ctx, int x, int y, int w, int h, int radius, int spread, int colorArgb) {
        int baseSpread = Math.max(1, spread);
        int baseMargin = Math.max(2, Math.min(12, baseSpread * 4));
        int layers = Math.max(1, Math.min(3, 1 + (baseSpread / 2)));
        int alpha = (colorArgb >>> 24) & 0xFF;
        for (int i = 0; i < layers; i++) {
            int margin = baseMargin + (i * 2);
            float fade = 1.0f - (i / (float) Math.max(1, layers + 1)) * 0.56f;
            int layerColor = withAlpha(colorArgb, Math.round(alpha * fade));
            drawRoundedQuad(ctx, ROUNDED_GLOW_PIPELINE, x, y, w, h, radius, margin, layerColor, false);
        }
    }

    private static void drawRoundedQuad(
            DrawContext ctx,
            RenderPipeline pipeline,
            int x,
            int y,
            int w,
            int h,
            int radius,
            int margin,
            int colorArgb,
            boolean flipV
    ) {
        if (w <= 0 || h <= 0) return;

        int m = Math.max(0, margin);
        int drawX = x - m;
        int drawY = y - m;
        int drawW = w + (m * 2);
        int drawH = h + (m * 2);
        if (drawW <= 0 || drawH <= 0) return;

        float u = -m;
        float v = flipV ? (h + m) : -m;
        int uvW = Math.max(1, w + (m * 2));
        int uvH = Math.max(1, h + (m * 2));
        if (flipV) uvH = -uvH;
        int texW = Math.max(1, w);
        int texH = Math.max(1, h);

        try {
            ctx.drawTexture(
                    pipeline,
                    WHITE_TEXTURE,
                    drawX,
                    drawY,
                    u,
                    v,
                    drawW,
                    drawH,
                    uvW,
                    uvH,
                    texW,
                    texH,
                    colorArgb
            );
        } catch (Throwable ignored) {
            ctx.fill(x, y, x + w, y + h, colorArgb);
        }
    }

    private static int withAlpha(int argb, int alpha) {
        int a = clamp255(alpha);
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    private static int clamp255(int value) {
        if (value < 0) return 0;
        return Math.min(255, value);
    }

    private static RenderPipeline register(String shaderName) {
        return RenderPipelines.register(
                RenderPipeline.builder(RenderPipelines.GUI_SNIPPET, RenderPipelines.GLOBALS_SNIPPET)
                        .withLocation(Identifier.of(StaffHelper.MOD_ID, "pipeline/" + shaderName))
                        .withVertexShader(Identifier.of(StaffHelper.MOD_ID, "core/rounded_panel"))
                        .withFragmentShader(Identifier.of(StaffHelper.MOD_ID, "core/" + shaderName))
                        .withSampler("Sampler0")
                        .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS)
                        .build()
        );
    }
}
