package de.maxhenkel.easyvillagers.gui;

import de.maxhenkel.easyvillagers.blocks.tileentity.IncubatorTileentity;
import de.maxhenkel.easyvillagers.datacomponents.VillagerData;
import de.maxhenkel.easyvillagers.items.VillagerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class IncubatorMenu extends AbstractContainerMenu {

    private final ContainerData containerData;
    private final BlockPos incubatorPos;

    /** Server-side constructor. */
    public IncubatorMenu(int containerId, Inventory playerInventory, IncubatorTileentity incubator, BlockPos pos) {
        super(ModMenuTypes.INCUBATOR, containerId);
        this.containerData = incubator.dataAccess;
        this.incubatorPos = pos;

        checkContainerSize(incubator, 2);

        // Slot 0 – input baby villager (same y as output so both are aligned)
        addSlot(new Slot(incubator, 0, 56, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof VillagerItem && VillagerData.isBaby(stack);
            }
        });

        // Slot 1 – output adult villager, take-only (furnace output slot position)
        addSlot(new Slot(incubator, 1, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }
        });

        // Player inventory (3 rows)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        addDataSlots(containerData);
    }

    /** Client-side constructor (data synced from server via addDataSlots). */
    public IncubatorMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        super(ModMenuTypes.INCUBATOR, containerId);
        this.containerData = new SimpleContainerData(2);
        this.incubatorPos = pos;

        SimpleContainer dummy = new SimpleContainer(2);

        addSlot(new Slot(dummy, 0, 56, 35));
        addSlot(new Slot(dummy, 1, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        addDataSlots(containerData);
    }

    public BlockPos getIncubatorPos() { return incubatorPos; }

    /** Returns arrow width 0..24 for rendering progress. */
    public int getIncubationProgress() {
        int ticks = containerData.get(0);
        int max = containerData.get(1);
        if (max <= 0) return 0;
        return ticks * 24 / max;
    }

    @Override
    public boolean stillValid(Player player) {
        double dx = incubatorPos.getX() + 0.5 - player.getX();
        double dy = incubatorPos.getY() + 0.5 - player.getY();
        double dz = incubatorPos.getZ() + 0.5 - player.getZ();
        return dx * dx + dy * dy + dz * dz <= 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < 2) {
                // From incubator → player inventory
                if (!moveItemStackTo(stack, 2, 38, true)) return ItemStack.EMPTY;
            } else {
                // From player → incubator input slot only
                if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
