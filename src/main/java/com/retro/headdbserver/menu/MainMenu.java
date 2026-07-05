package com.retro.headdbserver.menu;

import com.retro.headdbserver.HeadDbServerMod;
import com.retro.headdbserver.data.HeadData;
import com.retro.headdbserver.util.HeadItems;
import com.retro.headdbserver.util.PriceButtons;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public final class MainMenu extends HeadDbMenu {
    private static final int[] CategoryIndices = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
    private final List<String> _categories;

    public MainMenu(int containerId, Inventory playerInventory) {
        super(containerId, playerInventory);
        _categories = HeadDbServerMod.get().repository().categories();
        render();
    }

    private void render() {
        setButton(50, PriceButtons.createPriceButton(), null);

        setButton(49, HeadItems.simpleNamed(
                new ItemStack(Items.NAME_TAG),
                Component.literal("Search").withStyle(ChatFormatting.YELLOW),
                List.of(Component.literal("Use /hdb search <query>").withStyle(ChatFormatting.GRAY))
        ), null);

        for (int i = 0; i < Math.min(_categories.size(), CategoryIndices.length); i++) {
            String category = _categories.get(i);
            List<HeadData> heads = HeadDbServerMod.get().repository().byCategory(category);
            if (heads.isEmpty()) continue;

            HeadData iconHead = heads.getFirst();

            setButton(
                    CategoryIndices[i],
                    HeadItems.createCategoryIcon(iconHead, category),
                    player -> MenuOpener.open(
                            player,
                            Component.literal("HeadDB - " + category),
                            (id, inv, p) -> new CategoryMenu(id, inv, category, 0)
                    )
            );
        }

        ItemStack filler = HeadItems.simpleNamed(
                new ItemStack(Items.GRAY_STAINED_GLASS_PANE),
                Component.literal(""),
                List.of()
        );
        fillEmpty(filler);
    }
}
