package com.retro.headdbserver.menu;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

public abstract class HeadDbMenu extends AbstractContainerMenu
{
    private static final int ChestColumns = 9;
    private static final int ChestRows = 6;
    private static final int ChestCellCount = ChestRows * ChestColumns;
    private static final int PlayerInventoryRows = 3;
    private static final int GridMarginX = 8;
    private static final int GridMarginY = 18;
    private static final int PlayerInventoryStartY = 139;
    private static final int PlayerHotbarY = 197;
    private static final int CellStep = 18;
    private static final int LeftMouseButton = 0;
    private static final int RightMouseButton = 1;

    private final SimpleContainer _container = new SimpleContainer(ChestCellCount);
    private final MenuButton[] _buttons = new MenuButton[ChestCellCount];

    protected HeadDbMenu(int containerId, Inventory playerInventory)
    {
        super(MenuType.GENERIC_9x6, containerId);
        bindChestButtons();
        bindPlayerInventory(playerInventory);
    }

    protected final void setButton(int index, ItemStack stack, Consumer<ServerPlayer> leftAction)
    {
        setButton(index, stack, leftAction, null);
    }

    protected final void setButton(
            int index,
            ItemStack stack,
            Consumer<ServerPlayer> leftAction,
            Consumer<ServerPlayer> rightAction)
    {
        if (index < 0 || index >= ChestCellCount)
        {
            return;
        }

        _container.setItem(index, stack);
        _buttons[index].bindActions(leftAction, rightAction);
    }

    protected final void fillEmpty(ItemStack filler)
    {
        for (int index = 0; index < ChestCellCount; index++)
        {
            if (_container.getItem(index).isEmpty())
            {
                _container.setItem(index, filler.copy());
            }
        }
    }

    private void bindChestButtons()
    {
        for (int index = 0; index < ChestCellCount; index++)
        {
            int column = index % ChestColumns;
            int row = index / ChestColumns;
            int x = GridMarginX + column * CellStep;
            int y = GridMarginY + row * CellStep;

            MenuButton button = new MenuButton(_container, index, x, y);
            _buttons[index] = button;
            addSlot(button);
        }
    }

    private void bindPlayerInventory(Inventory playerInventory)
    {
        for (int row = 0; row < PlayerInventoryRows; row++)
        {
            for (int column = 0; column < ChestColumns; column++)
            {
                int inventoryIndex = ChestColumns + row * ChestColumns + column;
                int x = GridMarginX + column * CellStep;
                int y = PlayerInventoryStartY + row * CellStep;
                addSlot(new ReadOnlyPlayerCell(playerInventory, inventoryIndex, x, y));
            }
        }

        for (int column = 0; column < ChestColumns; column++)
        {
            int x = GridMarginX + column * CellStep;
            addSlot(new ReadOnlyPlayerCell(playerInventory, column, x, PlayerHotbarY));
        }
    }

    @Override
    public void clicked(int index, int buttonCode, ClickType clickType, Player player)
    {
        if (!(player instanceof ServerPlayer serverPlayer))
        {
            return;
        }

        if (clickType == ClickType.PICKUP && index >= 0 && index < _buttons.length)
        {
            _buttons[index].runAction(serverPlayer, buttonCode);
        }

        serverPlayer.containerMenu.sendAllDataToRemote();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        return ItemStack.EMPTY;
    }

    @Override
    protected boolean moveItemStackTo(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection)
    {
        return false;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot cell)
    {
        return false;
    }

    @Override
    public boolean canDragTo(Slot cell)
    {
        return false;
    }

    @Override
    public boolean stillValid(Player player)
    {
        return true;
    }

    private static final class MenuButton extends Slot
    {
        private Consumer<ServerPlayer> _leftAction;
        private Consumer<ServerPlayer> _rightAction;

        private MenuButton(Container container, int index, int x, int y)
        {
            super(container, index, x, y);
        }

        private void bindActions(Consumer<ServerPlayer> leftAction, Consumer<ServerPlayer> rightAction)
        {
            _leftAction = leftAction;
            _rightAction = rightAction;
        }

        private void runAction(ServerPlayer player, int buttonCode)
        {
            if (buttonCode == LeftMouseButton && _leftAction != null)
            {
                _leftAction.accept(player);
                return;
            }

            if (buttonCode == RightMouseButton && _rightAction != null)
            {
                _rightAction.accept(player);
            }
        }

        @Override
        public boolean mayPlace(ItemStack stack)
        {
            return false;
        }

        @Override
        public boolean mayPickup(Player player)
        {
            return false;
        }
    }

    private static final class ReadOnlyPlayerCell extends Slot
    {
        private ReadOnlyPlayerCell(Container container, int index, int x, int y)
        {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack)
        {
            return false;
        }

        @Override
        public boolean mayPickup(Player player)
        {
            return false;
        }
    }
}
