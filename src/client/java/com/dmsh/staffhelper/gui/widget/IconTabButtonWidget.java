package com.dmsh.staffhelper.gui.widget;

import com.dmsh.staffhelper.gui.util.GuiRenderUtils;
import com.dmsh.staffhelper.gui.util.ModernGui;
import com.dmsh.staffhelper.gui.util.UiChrome;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class IconTabButtonWidget extends SoupButtonWidget {
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

        int plate = ModernGui.argb(Math.max(0, Math.round(a * 0.13f)), 255, 255, 255);
        GuiRenderUtils.roundedRect(ctx, x + 3, y + 3, x + w - 3, y + h - 3, 5, plate);

        int softBase = ModernGui.argb(Math.max(96, a), 226, 233, 244);
        int softHover = ModernGui.argb(Math.max(128, a), 245, 248, 255);
        int softMuted = ModernGui.argb(Math.max(74, a), 176, 186, 204);
        int stroke = !this.active ? softMuted : (this.isHovered() ? softHover : softBase);
        int detail = ModernGui.lerpColor(stroke, UiChrome.outlineColor(Math.max(96, a)), 0.34f);

        switch (iconType) {
            case NICKSEARCH -> drawNickSearchIcon(ctx, cx, cy, stroke, detail);
            case AFKZONE -> drawAfkZoneIcon(ctx, cx, cy, stroke, detail);
            case COMMANDBUILDER -> drawCommandBuilderIcon(ctx, cx, cy, stroke, detail);
            case MODULES -> drawModulesIcon(ctx, cx, cy, stroke, detail);
            case APPEARANCE -> drawAppearanceIcon(ctx, cx, cy, stroke, detail);
        }
    }

    private static void drawNickSearchIcon(DrawContext ctx, int cx, int cy, int stroke, int detail) {
        GuiRenderUtils.roundedOutline(ctx, cx - 6, cy - 5, cx + 0, cy + 1, 3, 1, stroke);
        GuiRenderUtils.roundedRect(ctx, cx - 4, cy - 2, cx - 2, cy + 0, 1, detail);
        GuiRenderUtils.roundedRect(ctx, cx + 1, cy + 1, cx + 6, cy + 6, 3, stroke);
        GuiRenderUtils.roundedRect(ctx, cx + 4, cy + 4, cx + 7, cy + 7, 1, stroke);
    }

    private static void drawAfkZoneIcon(DrawContext ctx, int cx, int cy, int stroke, int detail) {
        GuiRenderUtils.roundedOutline(ctx, cx - 5, cy - 6, cx + 5, cy + 3, 4, 1, stroke);
        GuiRenderUtils.roundedRect(ctx, cx - 2, cy - 2, cx + 2, cy + 1, 2, detail);
        GuiRenderUtils.roundedRect(ctx, cx - 1, cy + 3, cx + 1, cy + 7, 1, stroke);
    }

    private static void drawCommandBuilderIcon(DrawContext ctx, int cx, int cy, int stroke, int detail) {
        GuiRenderUtils.roundedOutline(ctx, cx - 6, cy - 5, cx + 6, cy + 5, 3, 1, stroke);
        GuiRenderUtils.roundedRect(ctx, cx - 4, cy - 1, cx - 1, cy + 0, 1, stroke);
        GuiRenderUtils.roundedRect(ctx, cx - 3, cy - 2, cx - 1, cy + 1, 1, stroke);
        GuiRenderUtils.roundedRect(ctx, cx + 1, cy + 1, cx + 4, cy + 2, 1, detail);
        GuiRenderUtils.roundedRect(ctx, cx - 4, cy - 4, cx + 4, cy - 3, 1, detail);
    }

    private static void drawModulesIcon(DrawContext ctx, int cx, int cy, int stroke, int detail) {
        int left = cx - 5;
        int top = cy - 5;
        int size = 4;
        int gap = 2;
        GuiRenderUtils.roundedRect(ctx, left, top, left + size, top + size, 1, stroke);
        GuiRenderUtils.roundedRect(ctx, left + size + gap, top, left + size + gap + size, top + size, 1, detail);
        GuiRenderUtils.roundedRect(ctx, left, top + size + gap, left + size, top + size + gap + size, 1, detail);
        GuiRenderUtils.roundedRect(ctx, left + size + gap, top + size + gap, left + size + gap + size, top + size + gap + size, 1, stroke);
    }

    private static void drawAppearanceIcon(DrawContext ctx, int cx, int cy, int stroke, int detail) {
        GuiRenderUtils.roundedRect(ctx, cx - 5, cy - 3, cx + 5, cy - 2, 1, stroke);
        GuiRenderUtils.roundedRect(ctx, cx - 5, cy + 1, cx + 5, cy + 2, 1, stroke);
        GuiRenderUtils.roundedRect(ctx, cx - 1, cy - 5, cx + 2, cy - 2, 1, detail);
        GuiRenderUtils.roundedRect(ctx, cx + 2, cy - 1, cx + 5, cy + 2, 1, detail);
        GuiRenderUtils.roundedRect(ctx, cx - 4, cy + 0, cx - 1, cy + 3, 1, stroke);
    }
}
