package com.retro.headdbserver.menu;

import com.retro.headdbserver.HeadDbServerMod;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class CategoryMenu extends PagedHeadMenu {
    public CategoryMenu(int containerId, Inventory playerInventory, String category, int page) {
        super(containerId, playerInventory,
                category,
                HeadDbServerMod.get().repository().byCategory(category),
                page,
                (player, nextPage) -> MenuOpener.open(player, Component.literal("HeadDB - " + category),
                        (id, inv, p) -> new CategoryMenu(id, inv, category, nextPage)),
                player -> MenuOpener.open(player, Component.literal("HeadDB - Main"),
                        (id, inv, p) -> new MainMenu(id, inv)));
    }
}
