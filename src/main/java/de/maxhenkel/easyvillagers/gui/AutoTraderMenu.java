package de.maxhenkel.easyvillagers.gui;

import de.maxhenkel.easyvillagers.blocks.tileentity.AutoTraderTileentity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;

public class AutoTraderMenu extends MerchantMenu {

    // Custom GUI slot positions (gui-relative)
    public static final int SLOT_INPUT_A_X = 53,  SLOT_INPUT_A_Y = 66;
    public static final int SLOT_INPUT_B_X = 75,  SLOT_INPUT_B_Y = 66;
    public static final int SLOT_OUTPUT_X  = 139, SLOT_OUTPUT_Y  = 66;

    private final BlockPos autoTraderPos;
    private final AutoTraderTileentity autoTrader;
    private final ContainerData tradeSync;

    // Tracks the currently selected index on the client side
    private int clientSelectedIndex = 0;

    /** Server-side constructor. */
    public AutoTraderMenu(int containerId, Inventory playerInventory, Merchant merchant,
                          AutoTraderTileentity autoTrader, BlockPos pos) {
        super(containerId, playerInventory, merchant);
        this.autoTraderPos = pos;
        this.autoTrader = autoTrader;

        // Replace merchant payment/result slots with tileentity-backed ones at our positions
        replaceSlot(0, new Slot(autoTrader, 0, SLOT_INPUT_A_X, SLOT_INPUT_A_Y));
        replaceSlot(1, new Slot(autoTrader, 1, SLOT_INPUT_B_X, SLOT_INPUT_B_Y));
        replaceSlot(2, new Slot(autoTrader, 2, SLOT_OUTPUT_X, SLOT_OUTPUT_Y) {
            @Override public boolean mayPlace(ItemStack s) { return false; }
        });

        // Reposition player inventory slots to match our custom GUI layout
        repositionPlayerInventory(playerInventory);

        this.tradeSync = new ContainerData() {
            @Override public int get(int i) {
                if (i == 0) return autoTrader.getSelectedTradeIndex();
                if (i == 1) return autoTrader.getTradeTimer();
                return 0;
            }
            @Override public void set(int i, int val) { }
            @Override public int getCount() { return 2; }
        };
        addDataSlots(this.tradeSync);
    }

    /** Client-side constructor. */
    public AutoTraderMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        super(containerId, playerInventory);
        this.autoTraderPos = pos;
        this.autoTrader = null;

        SimpleContainer dummy = new SimpleContainer(3);
        replaceSlot(0, new Slot(dummy, 0, SLOT_INPUT_A_X, SLOT_INPUT_A_Y));
        replaceSlot(1, new Slot(dummy, 1, SLOT_INPUT_B_X, SLOT_INPUT_B_Y));
        replaceSlot(2, new Slot(dummy, 2, SLOT_OUTPUT_X, SLOT_OUTPUT_Y) {
            @Override public boolean mayPlace(ItemStack s) { return false; }
        });

        repositionPlayerInventory(playerInventory);

        SimpleContainerData clientData = new SimpleContainerData(2);
        this.tradeSync = clientData;
        addDataSlots(this.tradeSync);
    }

    /**
     * Move player inventory slots to match our custom GUI layout.
     * MerchantMenu puts them at x=108, y=84/142. We want x=8, y=86/144.
     */
    private void repositionPlayerInventory(Inventory playerInventory) {
        // Rows (slots 3-29)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int menuSlot = 3 + row * 9 + col;
                int containerSlot = col + row * 9 + 9;
                replaceSlot(menuSlot, new Slot(playerInventory, containerSlot,
                        8 + col * 18, 106 + row * 18));
            }
        }
        // Hotbar (slots 30-38)
        for (int i = 0; i < 9; i++) {
            replaceSlot(30 + i, new Slot(playerInventory, i, 8 + i * 18, 164));
        }
    }

    private void replaceSlot(int index, Slot newSlot) {
        newSlot.index = index;
        slots.set(index, newSlot);
    }

    public BlockPos getAutoTraderPos() { return autoTraderPos; }
    public int getClientSelectedIndex() { return clientSelectedIndex; }
    public int getSyncedTradeTimer() { return tradeSync.get(1); }

    // MerchantMenu adds 3 data slots before ours (ids 0,1,2).
    // Our tradeSync: id=3 (selectedTrade), id=4 (timer).
    private static final int TRADE_SYNC_DATA_ID = 3;

    @Override
    public void setData(int id, int value) {
        super.setData(id, value);
        // When server sends the persisted selectedTradeIndex, update our local tracking
        if (id == TRADE_SYNC_DATA_ID && autoTrader == null) {
            clientSelectedIndex = value;
            super.setSelectionHint(value);
        }
    }

    @Override
    public void setSelectionHint(int index) {
        super.setSelectionHint(index);
        this.clientSelectedIndex = index;
        if (autoTrader != null) {
            autoTrader.setSelectedTradeIndex(index);
        }
    }

    /** Prevent vanilla from moving items into payment slots when a trade is clicked. */
    @Override
    public void tryMoveItems(int offerIndex) { }

    @Override
    public MenuType<?> getType() {
        return ModMenuTypes.AUTO_TRADER;
    }

    @Override
    public boolean stillValid(Player player) {
        double dx = autoTraderPos.getX() + 0.5 - player.getX();
        double dy = autoTraderPos.getY() + 0.5 - player.getY();
        double dz = autoTraderPos.getZ() + 0.5 - player.getZ();
        return dx * dx + dy * dy + dz * dz <= 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        if (index == 2) {
            // Output → player inventory
            if (!moveItemStackTo(stack, 3, 39, true)) return ItemStack.EMPTY;
        } else if (index == 0 || index == 1) {
            // Inputs → player inventory
            if (!moveItemStackTo(stack, 3, 39, true)) return ItemStack.EMPTY;
        } else if (index >= 3 && index < 39) {
            // Player inv → input slots
            if (!moveItemStackTo(stack, 0, 2, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }
}
