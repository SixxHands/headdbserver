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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class PagedHeadMenu extends HeadDbMenu {
    protected static final int[] ContentIndices = {
            10,11,12,13,14,15,16,
            19,20,21,22,23,24,25,
            28,29,30,31,32,33,34,
            37,38,39,40,41,42,43
    };

    public PagedHeadMenu(int containerId,
                         Inventory playerInventory,
                         String title,
                         List<HeadData> heads,
                         int page,
                         BiConsumer<ServerPlayer, Integer> openPage,
                         Consumer<ServerPlayer> backAction) {
        super(containerId, playerInventory);

        int pageSize = Math.min(ContentIndices.length, HeadDbConfig.PageSize.get());
        int start = page * pageSize;
        int end = Math.min(start + pageSize, heads.size());

        for (int i = start; i < end; i++) {
            int buttonIndex = ContentIndices[i - start];
            HeadData head = heads.get(i);

            setButton(buttonIndex, HeadItems.createGuiHead(head), player -> {
                // skip purchase menu if free
                if (PriceData.current().isFree()) {
                    ItemStack stack = HeadItems.createHead(head);

                    InventoryHandler.giveOrDrop(player, stack);

                    player.displayClientMessage(
                            Component.literal("Given: ")
                                    .append(stack.getHoverName())
                                    .withStyle(ChatFormatting.GREEN),
                            true
                    );

                    // nice feedback noise
                    player.playNotifySound(SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0f, 1.0f);

                    return;
                }

                // normal purchase flow
                MenuOpener.open(player,
                        Component.literal("Purchase - " + head.name()),
                        (id, inv, p) -> new PurchaseMenu(
                                id,
                                inv,
                                head,
                                (backId, backInv, backPlayer) ->
                                        new PagedHeadMenu(backId, backInv, title, heads, page, openPage, backAction)
                        ));
            });
        }

        setButton(48, HeadItems.simpleNamed(
                new ItemStack(Items.ARROW),
                Component.literal("Back").withStyle(ChatFormatting.RED),
                List.of()
        ), backAction);

        if (page > 0) {
            setButton(49, HeadItems.simpleNamed(
                    new ItemStack(Items.ARROW),
                    Component.literal("Previous Page").withStyle(ChatFormatting.YELLOW),
                    List.of()
            ), player -> openPage.accept(player, page - 1));
        }

        setButton(50, PriceButtons.createPriceButton(), null);

        if (end < heads.size()) {
            setButton(51, HeadItems.simpleNamed(
                    new ItemStack(Items.ARROW),
                    Component.literal("Next Page").withStyle(ChatFormatting.YELLOW),
                    List.of()
            ), player -> openPage.accept(player, page + 1));
        }

        fillEmpty(HeadItems.simpleNamed(
                new ItemStack(Items.GRAY_STAINED_GLASS_PANE),
                Component.literal(""),
                List.of()
        ));
    }
}