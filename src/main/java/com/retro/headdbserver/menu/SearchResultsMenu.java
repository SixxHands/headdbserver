package com.retro.headdbserver.menu;

import com.retro.headdbserver.data.HeadData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public final class SearchResultsMenu extends PagedHeadMenu {
    public SearchResultsMenu(int containerId, Inventory playerInventory, String title, List<HeadData> results, IMenuFactory backFactory) {
        this(containerId, playerInventory, title, results, 0, backFactory);
    }

    private SearchResultsMenu(int containerId, Inventory playerInventory, String title, List<HeadData> results, int page, IMenuFactory backFactory) {
        super(containerId, playerInventory,
                title,
                results,
                page,
                (player, nextPage) -> MenuOpener.open(player, Component.literal("HeadDB - " + title),
                        (id, inv, p) -> new SearchResultsMenu(id, inv, title, results, nextPage, backFactory)),
                player -> MenuOpener.open(player, Component.literal("HeadDB - Main"),
                        (id, inv, p) -> backFactory.create(id, inv, p)));
    }

    @FunctionalInterface
    public interface IMenuFactory {
        HeadDbMenu create(int id, Inventory inv, ServerPlayer player);
    }
}
