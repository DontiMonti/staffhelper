package com.dmsh.staffhelper.gui.widget;

import com.dmsh.staffhelper.gui.util.MinimalIconRenderer;
import com.dmsh.staffhelper.gui.util.ModernGui;
import com.dmsh.staffhelper.gui.util.UiChrome;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class IconTabButtonWidget extends SoupButtonWidget {
    public enum IconType {
        HOME,
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
        int base = !this.active
                ? ModernGui.argb(Math.max(72, a), 148, 156, 170)
                : ModernGui.argb(Math.max(112, a), 224, 231, 242);
        int hover = ModernGui.argb(Math.max(140, a), 248, 250, 255);
        int iconColor = this.isHovered() ? hover : base;
        int accent = ModernGui.lerpColor(iconColor, UiChrome.accentColor(Math.max(124, a)), this.isHovered() ? 0.42f : 0.26f);

        MinimalIconRenderer.drawCentered(ctx, toGlyph(iconType), cx, cy, Math.min(w, h) - 4, iconColor, accent);
    }

    private static MinimalIconRenderer.Glyph toGlyph(IconType iconType) {
        return switch (iconType) {
            case HOME -> MinimalIconRenderer.Glyph.HOME;
            case NICKSEARCH -> MinimalIconRenderer.Glyph.SEARCH;
            case AFKZONE -> MinimalIconRenderer.Glyph.MAP_PIN;
            case COMMANDBUILDER -> MinimalIconRenderer.Glyph.COMMAND;
            case MODULES -> MinimalIconRenderer.Glyph.MODULES;
            case APPEARANCE -> MinimalIconRenderer.Glyph.SLIDERS;
        };
    }
}
