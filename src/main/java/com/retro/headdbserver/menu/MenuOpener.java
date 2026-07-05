package com.retro.headdbserver.menu;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;

public final class MenuOpener {
    private MenuOpener() {}

    public static void open(ServerPlayer player, Component title, IMenuFactory factory) {
        player.getServer().execute(() -> player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, p) -> factory.create(containerId, inventory, player),
                title
        )));
    }

    @FunctionalInterface
    public interface IMenuFactory {
        HeadDbMenu create(int containerId, net.minecraft.world.entity.player.Inventory inventory, ServerPlayer player);
    }
}
