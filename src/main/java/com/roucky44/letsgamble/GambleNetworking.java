package com.roucky44.letsgamble;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class GambleNetworking {
    public static final ResourceLocation GAMBLE_REQUEST = LetSGamble.id("gamble_request");
    public static final ResourceLocation GAMBLE_RESULT = LetSGamble.id("gamble_result");

    private GambleNetworking() {
    }

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(GAMBLE_REQUEST, (server, player, handler, buf, responseSender) -> {
            int slot = buf.readVarInt();
            ResourceLocation targetId = ResourceLocation.tryParse(buf.readUtf(32767));
            server.execute(() -> gamble(player, slot, targetId));
        });
    }

    private static void gamble(ServerPlayer player, int slot, ResourceLocation targetId) {
        if (slot < 0 || slot >= player.getInventory().items.size() || targetId == null) {
            return;
        }

        Item target = BuiltInRegistries.ITEM.get(targetId);
        ItemStack stake = player.getInventory().getItem(slot);
        if (target == Items.AIR || stake.isEmpty()) {
            return;
        }

        float chance = GambleValues.chance(stake.getItem(), target);
        boolean won = player.getRandom().nextFloat() < chance;
        stake.shrink(1);

        if (won) {
            ItemStack reward = new ItemStack(target);
            if (!player.getInventory().add(reward)) {
                player.drop(reward, false);
            }
        }

        FriendlyByteBuf result = PacketByteBufs.create();
        result.writeBoolean(won);
        result.writeFloat(chance);
        result.writeItem(new ItemStack(target));
        ServerPlayNetworking.send(player, GAMBLE_RESULT, result);
    }
}
