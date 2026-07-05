package com.retro.headdbserver.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public final class PriceButtons {
    private PriceButtons() {}

    public static ItemStack createPriceButton() {
        PriceData price = PriceData.current();
        if (price.isFree()) {
            return HeadItems.simpleNamed(
                    new ItemStack(Items.EMERALD),
                    Component.literal("Price:").withStyle(ChatFormatting.GREEN),
                    List.of(Component.literal("Free").withStyle(ChatFormatting.GRAY))
            );
        }

        return HeadItems.simpleNamed(
                new ItemStack(Items.EMERALD),
                Component.literal("Price:").withStyle(ChatFormatting.GREEN),
                List.of(
                        Component.literal("Item: ").append(price.item().getDescription()).withStyle(ChatFormatting.GRAY),
                        Component.literal("Amount: " + price.count()).withStyle(ChatFormatting.GRAY)
                )
        );
    }
}
