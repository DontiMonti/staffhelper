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

/**
 * Shows a small HUD widget when vanish is enabled for the current player.
 * Detects server messages like:
 * "исчезновение Включено для <nick>" / "исчезновение Отключено для <nick>".
 */
public final class VanishFeature {
    private VanishFeature() {}

    private static boolean vanishEnabled = false;

    private static final int PAD = 6;
    private static final int BOX_H = 14 + 10; // 1 line
    private static final Text LINE = Text.literal("⚗ ваниш включен").formatted(Formatting.GREEN);

    // "... исчезновение Включено для DontiMonti"
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

        // При смене сервера (/queue, /hub и т.п.) ваниш обычно сбрасывается сервером,
        // но клиент не всегда получает сообщение "Отключено". Поэтому сбрасываем плашку
        // при переподключении/отключении.
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
        // делаем ширину по фактической длине текста, чтобы плашка не была слишком длинной
        int pad = Math.max(4, Math.round(PAD * getScale()));
        return tr.getWidth(LINE) + pad * 2;
    }

    public static int getPreviewWidth(TextRenderer tr) {
        return getBoxWidth(tr);
    }

    public static int getPreviewHeight() {
        return Math.max(16, Math.round(BOX_H * getScale()));
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
        int pad = Math.max(4, Math.round(PAD * getScale()));
        int w = getBoxWidth(mc.textRenderer);
        int h = getPreviewHeight();

        UiChrome.drawPanel(ctx, x, y, w, h, 8, System.currentTimeMillis());
        int textY = y + Math.max(4, (h - mc.textRenderer.fontHeight) / 2);
        ctx.drawText(mc.textRenderer, line, x + pad, textY, 0xFFFFFFFF, true);
    }

    private static void onChatMessage(String msg) {
        if (!AllowedUsersAccessGate.isModAllowed()) {
            vanishEnabled = false;
            return;
        }
        if (msg == null || msg.isBlank()) return;

        // strip colors
        String clean = msg.replaceAll("§.", "");
        // server may send "\\n" as literal chars
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
}
