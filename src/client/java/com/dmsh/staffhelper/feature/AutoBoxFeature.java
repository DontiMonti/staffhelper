package com.dmsh.staffhelper.feature;

import com.dmsh.staffhelper.StaffHelperState;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;

public final class AutoBoxFeature {
    private AutoBoxFeature() {}

    private static final int SEND_DELAY_TICKS = 28;
    private static int pendingTicks = -1;
    private static boolean sentForCurrentJoin = false;

    public static void init() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            pendingTicks = SEND_DELAY_TICKS;
            sentForCurrentJoin = false;
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
        ClientTickEvents.END_CLIENT_TICK.register(AutoBoxFeature::tick);
    }

    private static void reset() {
        pendingTicks = -1;
        sentForCurrentJoin = false;
    }

    private static void tick(MinecraftClient client) {
        if (sentForCurrentJoin || pendingTicks < 0) return;
        if (client == null || client.player == null || client.getNetworkHandler() == null) return;
        if (StaffHelperState.CONFIG == null) return;

        String command = selectedCommand();
        if (command == null || command.isBlank()) {
            sentForCurrentJoin = true;
            pendingTicks = -1;
            return;
        }

        if (pendingTicks > 0) {
            pendingTicks--;
            return;
        }

        sendCommand(client, command);
        sentForCurrentJoin = true;
        pendingTicks = -1;
    }

    private static String selectedCommand() {
        int selected = StaffHelperState.CONFIG.autoBoxSelection;
        if (selected == 1) return StaffHelperState.CONFIG.autoBoxCommandBox1;
        if (selected == 2) return StaffHelperState.CONFIG.autoBoxCommandBox2;
        return "";
    }

    private static void sendCommand(MinecraftClient client, String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.isBlank()) return;
        String commandNoSlash = trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
        if (commandNoSlash.isBlank()) return;

        try {
            client.player.networkHandler.sendChatCommand(commandNoSlash);
        } catch (Throwable ignored) {
            client.player.networkHandler.sendChatMessage("/" + commandNoSlash);
        }
    }
}
