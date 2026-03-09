package com.dmsh.staffhelper.feature;

import com.dmsh.staffhelper.StaffHelperState;
import com.dmsh.staffhelper.gui.util.UiChrome;
import com.dmsh.staffhelper.util.AllowedUsersAccessGate;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VanishFeature {
    private VanishFeature() {}

    private static boolean vanishEnabled = false;

    private static final int PAD = 6;
    private static final Text HEADER = UiChrome.uiLiteral("Vanish");
    private static final Text LINE = UiChrome.uiLiteral("VANISH ENABLED").formatted(Formatting.GREEN);

    private static final Pattern P_VANISH_ON =
            Pattern.compile("(?iu).*исчезновение\\s+Включено\\s+для\\s+([A-Za-z0-9_]{3,16}).*");
    private static final Pattern P_VANISH_OFF =
            Pattern.compile("(?iu).*исчезновение\\s+Отключено\\s+для\\s+([A-Za-z0-9_]{3,16}).*");

    public static void init() {
        ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, receptionTimestamp) ->
                onChatMessage(message.getString())
        );
        ClientReceiveMessageEvents.GAME.register((message, overlay) ->
                onChatMessage(message.getString())
        );

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> clear());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());

        HudRenderCallback.EVENT.register(VanishFeature::renderHud);
    }

    public static void clear() {
        vanishEnabled = false;
    }

    public static boolean isEnabled() {
        return vanishEnabled;
    }

    public static int getBoxWidth(TextRenderer tr) {

        float scale = getScale();
        int pad = Math.max(4, Math.round(PAD * scale));
        int badge = getBadgeSize(scale);
        int bodyWidth = tr.getWidth(LINE) + pad * 2;
        int headerWidth = tr.getWidth(HEADER) + pad + badge + Math.max(3, Math.round(3 * scale)) + pad;
        return Math.max(bodyWidth, headerWidth);
    }

    public static int getPreviewWidth(TextRenderer tr) {
        return getBoxWidth(tr);
    }

    public static int getPreviewHeight() {
        MinecraftClient mc = MinecraftClient.getInstance();
        int lineH = (mc != null && mc.textRenderer != null) ? mc.textRenderer.fontHeight : 9;
        float scale = getScale();
        int pad = Math.max(4, Math.round(PAD * scale));
        return getHeaderHeight(scale) + pad + lineH + pad;
    }

    public static void renderPreview(DrawContext ctx, int x, int y) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.textRenderer == null) return;

        drawBox(ctx, mc, x, y, LINE);
    }

    private static void renderHud(DrawContext ctx, RenderTickCounter tickCounter) {
        if (!AllowedUsersAccessGate.isModAllowed()) return;
        if (StaffHelperState.CONFIG == null) return;
        if (!vanishEnabled) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.textRenderer == null) return;

        int x = StaffHelperState.CONFIG.vanishWidgetX;
        int y = StaffHelperState.CONFIG.vanishWidgetY;

        drawBox(ctx, mc, x, y, LINE);
    }

    private static void drawBox(DrawContext ctx, MinecraftClient mc, int x, int y, Text line) {
        float scale = getScale();
        int pad = Math.max(4, Math.round(PAD * scale));
        int headerH = getHeaderHeight(scale);
        int badgeSize = getBadgeSize(scale);
        int w = getBoxWidth(mc.textRenderer);
        int h = getPreviewHeight();

        UiChrome.drawHudPanel(ctx, x, y, w, h, 8, headerH, System.currentTimeMillis(), 0.10f, true);
        int titleY = y + Math.max(2, (headerH - mc.textRenderer.fontHeight) / 2);
        UiChrome.drawText(ctx, mc.textRenderer, HEADER, x + pad, titleY, UiChrome.mainTextColor(250), false);
        UiChrome.drawHudHeaderBadge(
                ctx,
                x + w - pad - badgeSize,
                y + Math.max(1, (headerH - badgeSize) / 2),
                badgeSize,
                vanishEnabled
        );

        int textY = y + headerH + pad;
        UiChrome.drawText(ctx, mc.textRenderer, line, x + pad, textY, UiChrome.mainTextColor(250), true);
    }

    private static void onChatMessage(String msg) {
        if (!AllowedUsersAccessGate.isModAllowed()) {
            vanishEnabled = false;
            return;
        }
        if (msg == null || msg.isBlank()) return;

        String clean = msg.replaceAll("§.", "");

        clean = clean.replace("\\n", "\n");

        String[] parts = clean.split("\\R");
        for (String p : parts) {
            String line = p.trim();
            if (line.isEmpty()) continue;
            onChatLine(line);
        }
    }

    private static void onChatLine(String line) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        String self = mc.player.getGameProfile().getName();
        if (self == null || self.isBlank()) return;

        Matcher on = P_VANISH_ON.matcher(line);
        if (on.matches()) {
            String nick = on.group(1);
            if (nick != null && nick.equalsIgnoreCase(self)) {
                vanishEnabled = true;
            }
            return;
        }

        Matcher off = P_VANISH_OFF.matcher(line);
        if (off.matches()) {
            String nick = off.group(1);
            if (nick != null && nick.equalsIgnoreCase(self)) {
                vanishEnabled = false;
            }
        }
    }

    private static float getScale() {
        if (StaffHelperState.CONFIG == null) return 1.0f;
        float v = StaffHelperState.CONFIG.vanishBoxScale;
        if (Float.isNaN(v)) return 1.0f;
        return Math.max(0.6f, Math.min(2.0f, v));
    }

    private static int getHeaderHeight(float scale) {
        return Math.max(14, Math.round(16 * scale));
    }

    private static int getBadgeSize(float scale) {
        return Math.max(7, Math.round(8 * scale));
    }
}
