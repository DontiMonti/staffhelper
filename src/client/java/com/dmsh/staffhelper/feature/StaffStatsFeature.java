package com.dmsh.staffhelper.feature;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import com.dmsh.staffhelper.util.AllowedUsersAccessGate;

/**
 * Счётчик действий модерации по сообщениям чата:
 *  - "{ник} заблокировал чат" -> mute +1
 *  - "{ник} заблокировал"     -> ban  +1
 *
 * Важно: проверка на "заблокировал чат" должна идти первой, чтобы счётчики не пересекались.
 */
public final class StaffStatsFeature {
    private StaffStatsFeature() {}

    private static int bans = 0;
    private static int mutes = 0;

    public static void init() {
        ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, receptionTimestamp) -> {
            onChatLine(message.getString());
        });
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            // some servers send moderation lines as GAME messages
            onChatLine(message.getString());
        });
    }

    private static void onChatLine(String line) {
        if (!AllowedUsersAccessGate.isModAllowed()) {
            reset();
            return;
        }
        if (line == null || line.isEmpty()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

        String me = mc.player.getGameProfile().getName();
        if (me == null || me.isEmpty()) return;

        // normalize spaces a bit
        String s = line.trim();

        String muteNeedle = me + " заблокировал чат";
        String banNeedle  = me + " заблокировал";

        if (s.contains(muteNeedle)) {
            mutes++;
            return;
        }
        if (s.contains(banNeedle)) {
            bans++;
        }
    }

    public static int getBans() { return bans; }
    public static int getMutes() { return mutes; }

    public static void reset() {
        bans = 0;
        mutes = 0;
    }
}
