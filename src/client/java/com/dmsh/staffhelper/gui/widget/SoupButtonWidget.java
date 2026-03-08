package com.dmsh.staffhelper.gui.widget;

import com.dmsh.staffhelper.gui.util.UiChrome;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class SoupButtonWidget extends ButtonWidget {

    private static final int TEXT = 0xFFEAEAEA;
    private static final int TEXT_DISABLED = 0xFF7A7A7A;

    private static final int RADIUS = 7;
    private static final float HOVER_EASE = 0.28f;
    private static final float PRESSED_EASE = 0.24f;
    private static final float HOVER_LIFT_PX = 1.6f;

    private float hoverAnim = 0.0f;
    private float pressedAnim = 0.0f;
    private boolean lockedPressed = false;

    public SoupButtonWidget(int x, int y, int width, int height, Text message, PressAction onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);
    }

    public void setLockedPressed(boolean value) {
        lockedPressed = value;
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        float alphaMul = Math.max(0.0f, Math.min(1.0f, this.alpha));
        if (alphaMul <= 0.01f) return;

        boolean hovered = this.isHovered();
        boolean enabled = this.active;
        boolean pressedState = lockedPressed;

        long now = System.currentTimeMillis();
        hoverAnim = approach(hoverAnim, hovered ? 1.0f : 0.0f, HOVER_EASE);
        pressedAnim = approach(pressedAnim, pressedState ? 1.0f : 0.0f, PRESSED_EASE);

        int grow = Math.round(hoverAnim);
        int drawX = getX() - grow;
        int drawY = getY() - Math.round(hoverAnim * HOVER_LIFT_PX) + Math.round(pressedAnim);
        int drawW = width + grow * 2;
        int drawH = height + grow * 2;

        if (enabled) {
            float accentBoost = (hoverAnim * 0.50f) - (pressedAnim * 0.34f);
            UiChrome.drawPanel(ctx, drawX, drawY, drawW, drawH, RADIUS, now, accentBoost, true);
            if (hoverAnim > 0.001f) {
                int glowAlpha = Math.max(0, Math.min(255, Math.round((10 + (38 * hoverAnim)) * alphaMul)));
                ctx.fill(drawX + 1, drawY + 1, drawX + drawW - 1, drawY + drawH - 1, (glowAlpha << 24) | 0xC8DFFF);
            }
            if (pressedAnim > 0.001f) {
                int a = Math.max(0, Math.min(255, Math.round((18 + (74 * pressedAnim)) * alphaMul)));
                ctx.fill(drawX + 1, drawY + 1, drawX + drawW - 1, drawY + drawH - 1, (a << 24));
            }
        } else {
            UiChrome.drawPanel(ctx, drawX, drawY, drawW, drawH, RADIUS, now, -0.35f, false);
        }

        int color = enabled ? (pressedState ? 0xFFDADADA : TEXT) : TEXT_DISABLED;
        color = applyAlpha(color, alphaMul);
        int textOffsetY = pressedState ? 1 : 0;
        drawMessageCentered(ctx, drawX, drawY, drawW, drawH, color, textOffsetY);
    }

    private static float approach(float current, float target, float speed) {
        return current + (target - current) * speed;
    }

    private static int applyAlpha(int argb, float factor) {
        int a = (argb >>> 24) & 0xFF;
        int outA = Math.max(0, Math.min(255, Math.round(a * factor)));
        return (outA << 24) | (argb & 0x00FFFFFF);
    }

    private void drawMessageCentered(DrawContext ctx, int x, int y, int w, int h, int color, int yOffset) {
        var tr = MinecraftClient.getInstance().textRenderer;
        int tx = x + (w - tr.getWidth(getMessage())) / 2;
        int ty = y + (h - 8) / 2 + yOffset;
        ctx.drawText(tr, getMessage(), tx, ty, color, false);
    }
}
