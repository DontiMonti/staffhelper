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

        hoverAnim = hoverAnim + ((hovered ? 1.0f : 0.0f) - hoverAnim) * 0.28f;
        float accentBoost = enabled ? (-0.10f + hoverAnim * 0.34f) : -0.32f;

        UiChrome.drawPanel(ctx, getX(), getY(), getWidth(), getHeight(), 7, now, accentBoost, false, false);

        int trackX = getX() + 10;
        int trackY = getY() + getHeight() - 7;
        int trackW = Math.max(24, getWidth() - 20);
        int trackH = 3;
        GuiRenderUtils.roundedRect(ctx, trackX, trackY, trackX + trackW, trackY + trackH, 2, enabled ? 0x7A11151C : 0x5511151C);

        int fillW = Math.max(1, (int) Math.round(trackW * this.value));
        GuiRenderUtils.roundedRect(ctx, trackX, trackY, trackX + fillW, trackY + trackH, 2, enabled ? 0xFF7FA9E8 : 0x886E7E99);

        int knobW = 8;
        int knobH = 13;
        int knobX = trackX + (int) Math.round((trackW - knobW) * this.value);
        int knobY = getY() + getHeight() - 13;
        int knobColor = enabled ? 0xFFE3E8F2 : 0xFF8993A6;
        GuiRenderUtils.roundedRect(ctx, knobX, knobY, knobX + knobW, knobY + knobH, 4, knobColor);
        GuiRenderUtils.roundedOutline(ctx, knobX, knobY, knobX + knobW, knobY + knobH, 4, 1, 0xAA13161D);

        int textColor = enabled ? 0xFFEAEAEA : 0xFF7A7A7A;
        var tr = MinecraftClient.getInstance().textRenderer;
        ctx.drawText(tr, getMessage(), getX() + 8, getY() + 6, textColor, false);
    }
}
