package com.dmsh.staffhelper.gui.widget;

import com.dmsh.staffhelper.gui.util.GuiRenderUtils;
import com.dmsh.staffhelper.gui.util.UiChrome;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class SoupIntSliderWidget extends IntSliderWidget {
    private float hoverAnim = 0.0f;

    public SoupIntSliderWidget(int x, int y, int w, int h, String label, int min, int max, int initial, java.util.function.IntConsumer listener) {
        super(x, y, w, h, label, min, max, initial, listener);
    }

    @Override
    public void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        boolean hovered = this.isHovered();
        boolean enabled = this.active;
        long now = System.currentTimeMillis();

        hoverAnim = hoverAnim + ((hovered ? 1.0f : 0.0f) - hoverAnim) * 0.24f;
        float accentBoost = enabled ? (-0.18f + hoverAnim * 0.38f) : -0.36f;

        UiChrome.drawPanel(ctx, getX(), getY(), getWidth(), getHeight(), 7, now, accentBoost, false, false);

        int trackX = getX() + 9;
        int trackY = getY() + getHeight() - 8;
        int trackW = Math.max(24, getWidth() - 18);
        int trackH = 3;

        int trackColor = enabled ? UiChrome.outlineColor(132) : UiChrome.outlineColor(92);
        GuiRenderUtils.roundedRect(ctx, trackX, trackY, trackX + trackW, trackY + trackH, 2, trackColor);

        int fillW = Math.max(1, (int) Math.round(trackW * this.value));
        int fillColor = enabled ? UiChrome.accentColor(220) : UiChrome.accentColor(120);
        GuiRenderUtils.roundedRect(ctx, trackX, trackY, trackX + fillW, trackY + trackH, 2, fillColor);

        int knobW = 8;
        int knobH = 12;
        int knobX = trackX + (int) Math.round((trackW - knobW) * this.value);
        int knobY = getY() + getHeight() - 13;
        int knobColor = enabled ? UiChrome.mainTextColor(246) : UiChrome.mutedTextColor(188);
        GuiRenderUtils.roundedRect(ctx, knobX, knobY, knobX + knobW, knobY + knobH, 4, knobColor);
        GuiRenderUtils.roundedOutline(ctx, knobX, knobY, knobX + knobW, knobY + knobH, 4, 1, UiChrome.outlineColor(168));

        int textColor = enabled ? UiChrome.mainTextColor(242) : UiChrome.mutedTextColor(184);
        var tr = MinecraftClient.getInstance().textRenderer;
        ctx.drawText(tr, getMessage(), getX() + 8, getY() + 6, textColor, false);
    }
}
