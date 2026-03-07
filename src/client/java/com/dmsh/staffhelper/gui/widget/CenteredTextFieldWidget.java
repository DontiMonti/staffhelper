package com.dmsh.staffhelper.gui.widget;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/**
 * TextFieldWidget with vertically centered text/cursor when drawsBackground is disabled.
 *
 * Vanilla TextFieldWidget aligns text to the top when setDrawsBackground(false).
 * This helper keeps the custom rounded background while rendering the text centered.
 */
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

        // Newer MC versions use a 2D matrix stack in DrawContext (Matrix3x2fStack)
        // which doesn't have push/pop/translate(z). Instead we temporarily shift the
        // widget Y so vanilla renders text/cursor centered, then restore it.
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

    /**
     * Allow resizing text boxes with mouse wheel when hovered.
     * Scroll up -> bigger, scroll down -> smaller.
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!this.visible || !this.active) return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);

        if (this.isMouseOver(mouseX, mouseY) && verticalAmount != 0) {
            // Width changes faster than height (feels better for long patterns)
            int dw = (verticalAmount > 0) ? 10 : -10;
            int dh = (verticalAmount > 0) ? 2 : -2;

            int newW = this.width + dw;
            int newH = this.height + dh;

            // sane limits
            newW = Math.max(80, Math.min(620, newW));
            newH = Math.max(14, Math.min(34, newH));

            this.width = newW;
            this.height = newH;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
}
