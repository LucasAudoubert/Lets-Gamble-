package com.roucky44.letsgamble;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class KeyInputHandler {
    public static final String KEY_CATEGORY_GAMBLE = "key.category.letsgamble.game";
    public static final String KEY_OPEN_GAMBLE = "key.letsgamble.open";

    private static KeyMapping gambleKey;

    private KeyInputHandler() {
    }

    public static void register() {
        gambleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                KEY_OPEN_GAMBLE,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                KEY_CATEGORY_GAMBLE
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (gambleKey.consumeClick()) {
                if (client.player != null) {
                    Minecraft.getInstance().setScreen(new GambleScreen());
                }
            }
        });
    }
}
