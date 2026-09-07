package com.roucky44.letsgamble;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

public final class GambleClientNetworking {
    private GambleClientNetworking() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(GambleNetworking.GAMBLE_RESULT, (client, handler, buf, responseSender) -> {
            boolean won = buf.readBoolean();
            float chance = buf.readFloat();
            ItemStack target = buf.readItem();
            client.execute(() -> {
                if (Minecraft.getInstance().screen instanceof GambleScreen screen) {
                    screen.showResult(won, chance, target);
                }
            });
        });
    }
}
