package com.tiers;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import com.tiers.network.PlayerDataFetcher;
import com.tiers.config.TiersConfig;
import com.tiers.utils.CrackedPlayerHandler;

public class TiersClient implements ClientModInitializer {
    public static final String MOD_ID = "tiers";
    
    public static KeyBinding SEARCH_NEAREST_KEY;
    public static KeyBinding AUTO_DETECT_KEY;
    public static KeyBinding CYCLE_LEFT_KEY;
    public static KeyBinding CYCLE_RIGHT_KEY;
    public static KeyBinding CONFIG_KEY;

    @Override
    public void onInitializeClient() {
        SEARCH_NEAREST_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.tiers.search_nearest", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, "category.tiers.main"));
        AUTO_DETECT_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.tiers.auto_detect", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_Y, "category.tiers.main"));
        CYCLE_LEFT_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.tiers.cycle_left", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_U, "category.tiers.main"));
        CYCLE_RIGHT_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.tiers.cycle_right", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_I, "category.tiers.main"));
        CONFIG_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.tiers.config", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_O, "category.tiers.main"));

        TiersConfig.load();
        CrackedPlayerHandler.initialize();
        PlayerDataFetcher.initialize();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {});
    }
}
