package com.retro.headdbserver.util;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class InventoryHandler {
    private InventoryHandler() {}

    public static int countItem(Player player, Item item) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    public static boolean hasEnough(Player player, Item item, int count) {
        return countItem(player, item) >= count;
    }

    public static void removeItem(Player player, Item item, int count) {
        Inventory inventory = player.getInventory();
        int remaining = count;
        for (int i = 0; i < inventory.items.size() && remaining > 0; i++) {
            ItemStack stack = inventory.items.get(i);
            if (!stack.isEmpty() && stack.is(item)) {
                int taken = Math.min(remaining, stack.getCount());
                stack.shrink(taken);
                remaining -= taken;
            }
        }
    }

    public static void giveOrDrop(Player player, ItemStack stack) {
        boolean added = player.getInventory().add(stack);
        if (!added || !stack.isEmpty()) {
            player.drop(stack.copy(), false);
        }
        player.containerMenu.broadcastChanges();
    }
}
