package de.maxhenkel.easyvillagers.gui;

import de.maxhenkel.easyvillagers.blocks.tileentity.FarmerTileentity;
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

public class FarmerMenu extends AbstractContainerMenu {

    // Slot positions (item area x,y)
    public static final int SLOT_VILLAGER_X   = 17,  SLOT_VILLAGER_Y   = 28;
    public static final int SLOT_FOOD_X       = 62,  SLOT_FOOD_Y       = 28;
    public static final int OUTPUT_X          = 98,  OUTPUT_Y_TOP      = 19, OUTPUT_Y_BOT = 37;

    private final ContainerData containerData;
    private final BlockPos farmerPos;

    /** Server-side constructor. */
    public FarmerMenu(int containerId, Inventory playerInventory, FarmerTileentity farmer, BlockPos pos) {
        super(ModMenuTypes.FARMER, containerId);
        this.farmerPos = pos;

        checkContainerSize(farmer, 6);

        // Slot 0 – villager input
        addSlot(new Slot(farmer, 0, SLOT_VILLAGER_X, SLOT_VILLAGER_Y) {
            @Override public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof VillagerItem && !VillagerData.isBaby(stack);
            }
        });

        // Slot 1 – food input (only plantable crops)
        addSlot(new Slot(farmer, 1, SLOT_FOOD_X, SLOT_FOOD_Y) {
            @Override public boolean mayPlace(ItemStack stack) {
                return FarmerTileentity.isPlantableCrop(stack);
            }
        });

        // Slots 2–5 – 2×2 output grid (take-only)
        addSlot(new Slot(farmer, 2, OUTPUT_X,      OUTPUT_Y_TOP) { @Override public boolean mayPlace(ItemStack s) { return false; } });
        addSlot(new Slot(farmer, 3, OUTPUT_X + 18, OUTPUT_Y_TOP) { @Override public boolean mayPlace(ItemStack s) { return false; } });
        addSlot(new Slot(farmer, 4, OUTPUT_X,      OUTPUT_Y_BOT) { @Override public boolean mayPlace(ItemStack s) { return false; } });
        addSlot(new Slot(farmer, 5, OUTPUT_X + 18, OUTPUT_Y_BOT) { @Override public boolean mayPlace(ItemStack s) { return false; } });

        addPlayerInventory(playerInventory);

        this.containerData = new ContainerData() {
            @Override public int get(int i) {
                return i == 0 ? farmer.getFarmTimer() : i == 1 ? FarmerTileentity.FARM_INTERVAL : 0;
            }
            @Override public void set(int i, int val) { }
            @Override public int getCount() { return 2; }
        };
        addDataSlots(containerData);
    }

    /** Client-side constructor. */
    public FarmerMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        super(ModMenuTypes.FARMER, containerId);
        this.farmerPos = pos;

        SimpleContainer dummy = new SimpleContainer(6);

        addSlot(new Slot(dummy, 0, SLOT_VILLAGER_X, SLOT_VILLAGER_Y));
        addSlot(new Slot(dummy, 1, SLOT_FOOD_X, SLOT_FOOD_Y));
        addSlot(new Slot(dummy, 2, OUTPUT_X,      OUTPUT_Y_TOP) { @Override public boolean mayPlace(ItemStack s) { return false; } });
        addSlot(new Slot(dummy, 3, OUTPUT_X + 18, OUTPUT_Y_TOP) { @Override public boolean mayPlace(ItemStack s) { return false; } });
        addSlot(new Slot(dummy, 4, OUTPUT_X,      OUTPUT_Y_BOT) { @Override public boolean mayPlace(ItemStack s) { return false; } });
        addSlot(new Slot(dummy, 5, OUTPUT_X + 18, OUTPUT_Y_BOT) { @Override public boolean mayPlace(ItemStack s) { return false; } });

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

    public BlockPos getFarmerPos() { return farmerPos; }

    /** Returns 0–100 progress for the progress bar. */
    public int getFarmProgress() {
        int ticks = containerData.get(0);
        int max   = containerData.get(1);
        if (max <= 0) return 0;
        return ticks * 100 / max;
    }

    @Override
    public boolean stillValid(Player player) {
        double dx = farmerPos.getX() + 0.5 - player.getX();
        double dy = farmerPos.getY() + 0.5 - player.getY();
        double dz = farmerPos.getZ() + 0.5 - player.getZ();
        return dx * dx + dy * dy + dz * dz <= 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy  = stack.copy();

        if (index < 6) {
            // Container slots → player inventory
            if (!moveItemStackTo(stack, 6, 42, true)) return ItemStack.EMPTY;
        } else {
            // Player inventory → villager slot if villager, food slot if plantable crop, else reject
            if (stack.getItem() instanceof VillagerItem && !VillagerData.isBaby(stack)) {
                if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            } else if (FarmerTileentity.isPlantableCrop(stack)) {
                if (!moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
            } else {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }
}
