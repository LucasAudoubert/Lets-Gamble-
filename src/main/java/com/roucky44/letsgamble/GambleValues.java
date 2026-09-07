package com.roucky44.letsgamble;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.util.Mth;

public final class GambleValues {
    private GambleValues() {
    }

    public static float chance(Item stake, Item target) {
        float stakeValue = value(stake);
        float targetValue = value(target);
        return Mth.clamp((stakeValue / (stakeValue + targetValue)) * 0.90F, 0.01F, 0.85F);
    }

    private static float value(Item item) {
        if (item == Items.ROTTEN_FLESH) return 1.0F;
        if (item == Items.IRON_INGOT) return 10.0F;
        if (item == Items.GOLD_INGOT) return 20.0F;
        if (item == Items.DIAMOND) return 100.0F;
        if (item == Items.NETHERITE_INGOT) return 1000.0F;

        return switch (item.getRarity(item.getDefaultInstance())) {
            case UNCOMMON -> 8.0F;
            case RARE -> 25.0F;
            case EPIC -> 100.0F;
            default -> 2.0F;
        };
    }
}
