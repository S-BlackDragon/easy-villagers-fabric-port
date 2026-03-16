package de.maxhenkel.easyvillagers.gui;

import de.maxhenkel.easyvillagers.blocks.tileentity.ConverterTileentity;
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

public class ConverterMenu extends AbstractContainerMenu {

    private final ContainerData containerData;
    private final BlockPos converterPos;

    /** Server-side constructor. */
    public ConverterMenu(int containerId, Inventory playerInventory, ConverterTileentity converter, BlockPos pos) {
        super(ModMenuTypes.CONVERTER, containerId);
        this.containerData = converter.dataAccess;
        this.converterPos = pos;

        checkContainerSize(converter, 2);

        // Slot 0 – input villager
        addSlot(new Slot(converter, 0, 56, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof VillagerItem && !VillagerData.isBaby(stack);
            }
        });

        // Slot 1 – output discounted villager (take-only)
        addSlot(new Slot(converter, 1, 116, 35) {
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

    /** Client-side constructor. */
    public ConverterMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        super(ModMenuTypes.CONVERTER, containerId);
        this.containerData = new SimpleContainerData(2);
        this.converterPos = pos;

        SimpleContainer dummy = new SimpleContainer(2);
        addSlot(new Slot(dummy, 0, 56, 35));
        addSlot(new Slot(dummy, 1, 116, 35) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
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

    public BlockPos getConverterPos() { return converterPos; }

    /** 0..24 progress arrow width. */
    public int getConversionProgress() {
        int ticks = containerData.get(0);
        int max = containerData.get(1);
        if (max <= 0) return 0;
        return ticks * 24 / max;
    }

    @Override
    public boolean stillValid(Player player) {
        double dx = converterPos.getX() + 0.5 - player.getX();
        double dy = converterPos.getY() + 0.5 - player.getY();
        double dz = converterPos.getZ() + 0.5 - player.getZ();
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
                // From converter → player
                if (!moveItemStackTo(stack, 2, 38, true)) return ItemStack.EMPTY;
            } else {
                // From player → input slot
                if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
