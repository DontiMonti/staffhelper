package com.dmsh.staffhelper.gui.widget;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class CenteredTextFieldWidget extends TextFieldWidget {

    private boolean drawsBg = true;
    private static final int TEXT_SHIFT_X = 4;

    public CenteredTextFieldWidget(TextRenderer textRenderer, int x, int y, int width, int height, Text text) {
        super(textRenderer, x, y, width, height, text);
    }

    @Override
    public void setDrawsBackground(boolean drawsBackground) {
        this.drawsBg = drawsBackground;
        super.setDrawsBackground(drawsBackground);
    }

    @Override
    public void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int yOffset = (this.height - 8) / 2;

        if (!this.drawsBg && yOffset > 0) {
            int oldX = this.getX();
            int oldY = this.getY();
            this.setX(oldX + TEXT_SHIFT_X);
            this.setY(oldY + yOffset);
            super.renderWidget(ctx, mouseX, mouseY, delta);
            this.setX(oldX);
            this.setY(oldY);
            return;
        }

        super.renderWidget(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!this.visible || !this.active) return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);

        if (this.isMouseOver(mouseX, mouseY) && verticalAmount != 0) {

            int dw = (verticalAmount > 0) ? 10 : -10;
            int dh = (verticalAmount > 0) ? 2 : -2;

            int newW = this.width + dw;
            int newH = this.height + dh;

            newW = Math.max(80, Math.min(620, newW));
            newH = Math.max(14, Math.min(34, newH));

            this.width = newW;
            this.height = newH;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
}
