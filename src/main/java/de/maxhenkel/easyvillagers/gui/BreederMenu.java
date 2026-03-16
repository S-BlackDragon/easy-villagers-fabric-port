package de.maxhenkel.easyvillagers.gui;

import de.maxhenkel.easyvillagers.blocks.tileentity.BreederTileentity;
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

public class BreederMenu extends AbstractContainerMenu {

    // Item slot positions (x,y = top-left of the 16×16 item area)
    public static final int SLOT_V1_X   = 8,  SLOT_V1_Y   = 17;
    public static final int SLOT_V2_X   = 26, SLOT_V2_Y   = 17;
    public static final int SLOT_FOOD_X = 8,  SLOT_FOOD_Y = 42;  // 4 slots spaced 18px
    public static final int SLOT_OUT_X  = 98, SLOT_OUT_Y1 = 17, SLOT_OUT_Y2 = 35; // 2×2

    private final ContainerData containerData;
    private final BlockPos breederPos;

    /** Server-side constructor. */
    public BreederMenu(int containerId, Inventory playerInventory, BreederTileentity breeder, BlockPos pos) {
        super(ModMenuTypes.BREEDER, containerId);
        this.breederPos = pos;

        checkContainerSize(breeder, 10);

        // Slots 0–1: villager inputs (adult only)
        addSlot(new Slot(breeder, 0, SLOT_V1_X, SLOT_V1_Y) {
            @Override public boolean mayPlace(ItemStack s) {
                return s.getItem() instanceof VillagerItem && !VillagerData.isBaby(s);
            }
        });
        addSlot(new Slot(breeder, 1, SLOT_V2_X, SLOT_V2_Y) {
            @Override public boolean mayPlace(ItemStack s) {
                return s.getItem() instanceof VillagerItem && !VillagerData.isBaby(s);
            }
        });

        // Slots 2–5: food input (accepts anything)
        for (int i = 0; i < 4; i++) {
            addSlot(new Slot(breeder, 2 + i, SLOT_FOOD_X + i * 18, SLOT_FOOD_Y));
        }

        // Slots 6–9: 2×2 baby output (take-only)
        addSlot(new Slot(breeder, 6, SLOT_OUT_X,      SLOT_OUT_Y1) { @Override public boolean mayPlace(ItemStack s) { return false; } });
        addSlot(new Slot(breeder, 7, SLOT_OUT_X + 18, SLOT_OUT_Y1) { @Override public boolean mayPlace(ItemStack s) { return false; } });
        addSlot(new Slot(breeder, 8, SLOT_OUT_X,      SLOT_OUT_Y2) { @Override public boolean mayPlace(ItemStack s) { return false; } });
        addSlot(new Slot(breeder, 9, SLOT_OUT_X + 18, SLOT_OUT_Y2) { @Override public boolean mayPlace(ItemStack s) { return false; } });

        addPlayerInventory(playerInventory);

        this.containerData = new ContainerData() {
            @Override public int get(int i) {
                return i == 0 ? breeder.getBreedTimer() : i == 1 ? BreederTileentity.BREED_INTERVAL : 0;
            }
            @Override public void set(int i, int val) { }
            @Override public int getCount() { return 2; }
        };
        addDataSlots(containerData);
    }

    /** Client-side constructor. */
    public BreederMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        super(ModMenuTypes.BREEDER, containerId);
        this.breederPos = pos;

        SimpleContainer dummy = new SimpleContainer(10);

        addSlot(new Slot(dummy, 0, SLOT_V1_X, SLOT_V1_Y));
        addSlot(new Slot(dummy, 1, SLOT_V2_X, SLOT_V2_Y));
        for (int i = 0; i < 4; i++) {
            addSlot(new Slot(dummy, 2 + i, SLOT_FOOD_X + i * 18, SLOT_FOOD_Y));
        }
        addSlot(new Slot(dummy, 6, SLOT_OUT_X,      SLOT_OUT_Y1) { @Override public boolean mayPlace(ItemStack s) { return false; } });
        addSlot(new Slot(dummy, 7, SLOT_OUT_X + 18, SLOT_OUT_Y1) { @Override public boolean mayPlace(ItemStack s) { return false; } });
        addSlot(new Slot(dummy, 8, SLOT_OUT_X,      SLOT_OUT_Y2) { @Override public boolean mayPlace(ItemStack s) { return false; } });
        addSlot(new Slot(dummy, 9, SLOT_OUT_X + 18, SLOT_OUT_Y2) { @Override public boolean mayPlace(ItemStack s) { return false; } });

        addPlayerInventory(playerInventory);

        this.containerData = new SimpleContainerData(2);
        addDataSlots(containerData);
    }

    private void addPlayerInventory(Inventory inv) {
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 83 + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inv, col, 8 + col * 18, 141));
    }

    public BlockPos getBreederPos() { return breederPos; }

    /** Returns 0–100 progress for the progress bar. */
    public int getBreedProgress() {
        int ticks = containerData.get(0);
        int max   = containerData.get(1);
        if (max <= 0) return 0;
        return ticks * 100 / max;
    }

    @Override
    public boolean stillValid(Player player) {
        double dx = breederPos.getX() + 0.5 - player.getX();
        double dy = breederPos.getY() + 0.5 - player.getY();
        double dz = breederPos.getZ() + 0.5 - player.getZ();
        return dx * dx + dy * dy + dz * dz <= 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy  = stack.copy();

        if (index < 10) {
            // Container → player inventory
            if (!moveItemStackTo(stack, 10, 46, true)) return ItemStack.EMPTY;
        } else {
            // Player inventory → villager slots if VillagerItem, else food slots
            if (stack.getItem() instanceof VillagerItem && !VillagerData.isBaby(stack)) {
                if (!moveItemStackTo(stack, 0, 2, false)) return ItemStack.EMPTY;
            } else {
                if (!moveItemStackTo(stack, 2, 6, false)) return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }
}
