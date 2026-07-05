package com.retro.headdbserver.util;

import com.retro.headdbserver.HeadDbServerMod;
import com.retro.headdbserver.config.HeadDbConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public record PriceData(Item item, int count) {
    public static PriceData current() {
        HeadDbServerMod mod = HeadDbServerMod.get();
        if (mod != null && mod.priceState() != null) {
            return new PriceData(mod.priceState().item(), mod.priceState().count());
        }

        String raw = HeadDbConfig.PriceItem.get();
        Item item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(raw)).orElse(Items.AIR);
        return new PriceData(item, HeadDbConfig.PriceCount.get());
    }

    public boolean isFree() {
        return item == Items.AIR || count <= 0;
    }
}
