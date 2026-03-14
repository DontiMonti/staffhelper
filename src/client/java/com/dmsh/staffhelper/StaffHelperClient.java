package com.dmsh.staffhelper;

import com.dmsh.staffhelper.config.StaffHelperConfig;
import com.dmsh.staffhelper.feature.AfkZoneFeature;
import com.dmsh.staffhelper.feature.AutoBoxFeature;
import com.dmsh.staffhelper.feature.CommandBuilderFeature;
import com.dmsh.staffhelper.feature.NickSearchFeature;
import com.dmsh.staffhelper.feature.StaffStatsFeature;
import com.dmsh.staffhelper.feature.StatsHudFeature;
import com.dmsh.staffhelper.feature.VanishFeature;
import com.dmsh.staffhelper.gui.DebugScreen;
import com.dmsh.staffhelper.gui.StaffHelperMenuScreen;
import com.dmsh.staffhelper.gui.util.SmoothUiShader;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;

public class StaffHelperClient implements ClientModInitializer {

    public static KeyBinding OPEN_MENU_KEY;
    public static KeyBinding COPY_CLEAR_AFK_KEY;
    public static KeyBinding ALTS_CHECK_KEY;
    private static boolean debugHotkeyDown = false;

    @Override
    public void onInitializeClient() {
        SmoothUiShader.init();
        StaffHelperState.CONFIG = StaffHelperConfig.load();

        OPEN_MENU_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.staffhelper.open_menu",
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.staffhelper"
        ));

        COPY_CLEAR_AFK_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.staffhelper.copy_clear_afk",
                GLFW.GLFW_KEY_P,
                "category.staffhelper"
        ));

        ALTS_CHECK_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.staffhelper.alts_check",
                GLFW.GLFW_KEY_O,
                "category.staffhelper"
        ));

        StatsHudFeature.init();
        StaffStatsFeature.init();
        NickSearchFeature.init();
        AfkZoneFeature.init();
        VanishFeature.init();
        CommandBuilderFeature.init();
        AutoBoxFeature.init();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_MENU_KEY.wasPressed()) {
                MinecraftClient.getInstance().setScreen(new StaffHelperMenuScreen());
            }

            while (COPY_CLEAR_AFK_KEY.wasPressed()) {
                AfkZoneFeature.copyToClipboardAndClear();
            }

            while (ALTS_CHECK_KEY.wasPressed()) {
                AfkZoneFeature.startAltsCheck();
            }

            long handle = client.getWindow().getHandle();
            boolean ctrl = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                    || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
            boolean alt = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS
                    || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS;
            boolean f4 = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_F4) == GLFW.GLFW_PRESS;
            boolean nowDown = ctrl && alt && f4;
            if (nowDown && !debugHotkeyDown) {
                client.setScreen(new DebugScreen());
            }
            debugHotkeyDown = nowDown;
        });
    }
}
