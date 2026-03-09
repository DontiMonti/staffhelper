package com.dmsh.staffhelper.gui.widget;

import com.dmsh.staffhelper.gui.util.ModernGui;
import com.dmsh.staffhelper.gui.util.UiChrome;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class SoupButtonWidget extends ButtonWidget {

    private static final int TEXT = 0xFFE1E1EB;
    private static final int TEXT_ACTIVE = 0xFFF5F6FF;
    private static final int TEXT_DISABLED = 0xFF7A7A7A;

    private static final int RADIUS = 5;
    private static final float HOVER_EASE = 0.24f;
    private static final float PRESSED_EASE = 0.20f;
    private static final float HOVER_LIFT_PX = 1.35f;

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

        int drawX = getX();
        int drawY = getY() - Math.round(hoverAnim * HOVER_LIFT_PX) + Math.round(pressedAnim);
        int drawW = width;
        int drawH = height;

        if (enabled) {
            float accentBoost = pressedState ? 0.90f : (-0.22f + (hoverAnim * 0.52f));
            UiChrome.drawPanel(ctx, drawX, drawY, drawW, drawH, RADIUS, now, accentBoost, hovered || pressedState, false);

            if (pressedState) {
                int accentFill = UiChrome.accentColor(Math.round(46 * alphaMul));
                drawInnerOverlay(ctx, drawX, drawY, drawW, drawH, accentFill);
            } else if (hoverAnim > 0.001f) {
                int hoverGlow = Math.max(0, Math.min(255, Math.round((8 + (18 * hoverAnim)) * alphaMul)));
                drawInnerOverlay(ctx, drawX, drawY, drawW, drawH, (hoverGlow << 24) | 0xFFFFFF);
            }
            if (pressedAnim > 0.001f && !pressedState) {
                int shade = Math.max(0, Math.min(255, Math.round((16 + (40 * pressedAnim)) * alphaMul)));
                drawInnerOverlay(ctx, drawX, drawY, drawW, drawH, shade << 24);
            }
        } else {
            UiChrome.drawPanel(ctx, drawX, drawY, drawW, drawH, RADIUS, now, -0.40f, false, false);
            drawInnerOverlay(ctx, drawX, drawY, drawW, drawH, 0x55000000);
        }

        int color = enabled ? (pressedState ? TEXT_ACTIVE : TEXT) : TEXT_DISABLED;
        color = applyAlpha(color, alphaMul);
        int textOffsetY = pressedState ? 1 : Math.round(-hoverAnim * 0.25f);
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
        Text message = UiChrome.uiText(getMessage());
        int tx = x + (w - tr.getWidth(message)) / 2;
        int ty = y + (h - 8) / 2 + yOffset;
        ctx.drawText(tr, message, tx, ty, color, false);
    }

    private static void drawInnerOverlay(DrawContext ctx, int x, int y, int w, int h, int colorArgb) {
        int ix = x + 1;
        int iy = y + 1;
        int iw = w - 2;
        int ih = h - 2;
        if (iw <= 0 || ih <= 0) return;
        ModernGui.roundedRect(ctx, ix, iy, iw, ih, Math.max(0, RADIUS - 1), colorArgb);
    }
}
