package com.dmsh.staffhelper.gui.widget;

import com.dmsh.staffhelper.gui.util.GuiRenderUtils;
import com.dmsh.staffhelper.gui.util.ModernGui;
import com.dmsh.staffhelper.gui.util.UiChrome;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class IconTabButtonWidget extends SoupButtonWidget {
    private static final int STROKE_THICKNESS = 2;
    private static final int DOT_SIZE = 3;

    public enum IconType {
        NICKSEARCH,
        AFKZONE,
        COMMANDBUILDER,
        MODULES,
        APPEARANCE
    }

    private final IconType iconType;

    public IconTabButtonWidget(int x, int y, int width, int height, IconType iconType, PressAction onPress) {
        super(x, y, width, height, Text.empty(), onPress);
        this.iconType = iconType == null ? IconType.NICKSEARCH : iconType;
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.renderWidget(ctx, mouseX, mouseY, delta);
        int a = Math.max(0, Math.min(255, Math.round(this.alpha * 255.0f)));
        if (a <= 4) return;

        int x = getX();
        int y = getY();
        int w = this.width;
        int h = this.height;
        int cx = x + (w / 2);
        int cy = y + (h / 2);

        int plate = ModernGui.argb(Math.max(0, Math.round(a * (this.isHovered() ? 0.11f : 0.07f))), 255, 255, 255);
        GuiRenderUtils.roundedRect(ctx, x + 3, y + 3, x + w - 3, y + h - 3, 5, plate);

        int softBase = ModernGui.argb(Math.max(108, a), 226, 233, 244);
        int softHover = ModernGui.argb(Math.max(144, a), 247, 250, 255);
        int softMuted = ModernGui.argb(Math.max(78, a), 170, 181, 198);
        int stroke = !this.active ? softMuted : (this.isHovered() ? softHover : softBase);
        int detail = ModernGui.lerpColor(stroke, UiChrome.accentColor(Math.max(128, a)), this.isHovered() ? 0.22f : 0.12f);

        switch (iconType) {
            case NICKSEARCH -> drawNickSearchIcon(ctx, cx, cy, stroke, detail);
            case AFKZONE -> drawAfkZoneIcon(ctx, cx, cy, stroke, detail);
            case COMMANDBUILDER -> drawCommandBuilderIcon(ctx, cx, cy, stroke, detail);
            case MODULES -> drawModulesIcon(ctx, cx, cy, stroke, detail);
            case APPEARANCE -> drawAppearanceIcon(ctx, cx, cy, stroke, detail);
        }
    }

    private static void drawNickSearchIcon(DrawContext ctx, int cx, int cy, int stroke, int detail) {
        GuiRenderUtils.roundedOutline(ctx, cx - 6, cy - 6, cx + 1, cy + 1, 3, 1, stroke);
        drawDot(ctx, cx - 4, cy - 4, DOT_SIZE, detail);
        drawHorizontalStroke(ctx, cx - 4, cy - 1, 4, detail);
        GuiRenderUtils.roundedOutline(ctx, cx + 1, cy, cx + 6, cy + 5, 2, 1, stroke);
        drawDiagonalDown(ctx, cx + 4, cy + 3, 2, stroke);
    }

    private static void drawAfkZoneIcon(DrawContext ctx, int cx, int cy, int stroke, int detail) {
        GuiRenderUtils.roundedOutline(ctx, cx - 6, cy - 5, cx + 6, cy + 3, 3, 1, stroke);
        drawDot(ctx, cx - 1, cy - 2, DOT_SIZE, detail);
        drawVerticalStroke(ctx, cx - 1, cy + 3, 3, detail);
        drawHorizontalStroke(ctx, cx - 4, cy + 6, 8, detail);
    }

    private static void drawCommandBuilderIcon(DrawContext ctx, int cx, int cy, int stroke, int detail) {
        GuiRenderUtils.roundedOutline(ctx, cx - 6, cy - 5, cx + 6, cy + 4, 3, 1, stroke);
        drawDot(ctx, cx - 4, cy - 3, 2, detail);
        drawDot(ctx, cx - 1, cy - 3, 2, detail);
        drawDiagonalDown(ctx, cx - 4, cy - 1, 2, detail);
        drawDiagonalUp(ctx, cx - 4, cy + 2, 2, detail);
        drawHorizontalStroke(ctx, cx + 0, cy + 1, 4, detail);
    }

    private static void drawModulesIcon(DrawContext ctx, int cx, int cy, int stroke, int detail) {
        int left = cx - 5;
        int top = cy - 5;
        int size = 4;
        int gap = 2;
        GuiRenderUtils.roundedOutline(ctx, left, top, left + size, top + size, 1, 1, stroke);
        GuiRenderUtils.roundedOutline(ctx, left + size + gap, top, left + size + gap + size, top + size, 1, 1, detail);
        GuiRenderUtils.roundedOutline(ctx, left, top + size + gap, left + size, top + size + gap + size, 1, 1, detail);
        GuiRenderUtils.roundedOutline(ctx, left + size + gap, top + size + gap, left + size + gap + size, top + size + gap + size, 1, 1, stroke);
    }

    private static void drawAppearanceIcon(DrawContext ctx, int cx, int cy, int stroke, int detail) {
        drawHorizontalStroke(ctx, cx - 6, cy - 5, 12, stroke);
        drawHorizontalStroke(ctx, cx - 6, cy, 12, stroke);
        drawHorizontalStroke(ctx, cx - 6, cy + 5, 12, stroke);
        drawDot(ctx, cx - 4, cy - 6, 4, detail);
        drawDot(ctx, cx + 1, cy - 1, 4, detail);
        drawDot(ctx, cx - 1, cy + 4, 4, detail);
    }

    private static void drawHorizontalStroke(DrawContext ctx, int x, int y, int length, int color) {
        if (length <= 0) return;
        GuiRenderUtils.roundedRect(ctx, x, y, x + length, y + STROKE_THICKNESS, 1, color);
    }

    private static void drawVerticalStroke(DrawContext ctx, int x, int y, int length, int color) {
        if (length <= 0) return;
        GuiRenderUtils.roundedRect(ctx, x, y, x + STROKE_THICKNESS, y + length, 1, color);
    }

    private static void drawDot(DrawContext ctx, int x, int y, int size, int color) {
        if (size <= 0) return;
        GuiRenderUtils.roundedRect(ctx, x, y, x + size, y + size, Math.max(1, size / 2), color);
    }

    private static void drawDiagonalDown(DrawContext ctx, int x, int y, int steps, int color) {
        for (int i = 0; i < steps; i++) {
            drawDot(ctx, x + i, y + i, STROKE_THICKNESS, color);
        }
    }

    private static void drawDiagonalUp(DrawContext ctx, int x, int y, int steps, int color) {
        for (int i = 0; i < steps; i++) {
            drawDot(ctx, x + i, y - i, STROKE_THICKNESS, color);
        }
    }
}
