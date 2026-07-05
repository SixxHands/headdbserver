package com.retro.headdbserver.menu;

import com.retro.headdbserver.config.HeadDbConfig;
import com.retro.headdbserver.data.HeadData;
import com.retro.headdbserver.util.HeadItems;
import com.retro.headdbserver.util.InventoryHandler;
import com.retro.headdbserver.util.PriceButtons;
import com.retro.headdbserver.util.PriceData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public final class PurchaseMenu extends HeadDbMenu {
    private final HeadData _head;
    private final IBackFactory _backFactory;

    public PurchaseMenu(int containerId, Inventory playerInventory, HeadData head, IBackFactory backFactory) {
        super(containerId, playerInventory);
        _head = head;
        _backFactory = backFactory;
        render();
    }

    private void render() {
        setButton(13, HeadItems.createGuiHead(_head), null);
        setButton(45, PriceButtons.createPriceButton(), null);

        setButton(29, HeadItems.simpleNamed(new ItemStack(Items.LIME_CONCRETE),
                Component.literal("Confirm Purchase").withStyle(ChatFormatting.GREEN),
                List.of(Component.literal("Buy 1x " + _head.name()).withStyle(ChatFormatting.GRAY))),
                this::purchase);

        setButton(33, HeadItems.simpleNamed(new ItemStack(Items.RED_CONCRETE),
                Component.literal("Cancel").withStyle(ChatFormatting.RED),
                List.of()),
                player -> MenuOpener.open(player, Component.literal("HeadDB"),
                        (id, inv, p) -> _backFactory.create(id, inv, p)));

        fillEmpty(HeadItems.simpleNamed(new ItemStack(Items.GRAY_STAINED_GLASS_PANE), Component.literal(""), List.of()));
    }

    private void purchase(ServerPlayer player) {
        PriceData price = PriceData.current();
        if (!price.isFree()) {
            boolean bypass = player.isCreative() && HeadDbConfig.AllowCreativeBypass.get();
            if (!bypass && !InventoryHandler.hasEnough(player, price.item(), price.count())) {
                player.sendSystemMessage(Component.literal("You do not have enough payment items.").withStyle(ChatFormatting.RED));
                return;
            }
            if (!bypass) {
                InventoryHandler.removeItem(player, price.item(), price.count());
            }
        }

        ItemStack stack = HeadItems.createHead(_head);
        InventoryHandler.giveOrDrop(player, stack);
        player.sendSystemMessage(Component.literal("Purchased: " + _head.name()).withStyle(ChatFormatting.GREEN));
        MenuOpener.open(player, Component.literal("HeadDB"), (id, inv, p) -> _backFactory.create(id, inv, p));
    }

    @FunctionalInterface
    public interface IBackFactory {
        HeadDbMenu create(int id, Inventory inventory, ServerPlayer player);
    }
}
