package de.maxhenkel.easyvillagers.gui;

import de.maxhenkel.easyvillagers.blocks.tileentity.IronFarmTileentity;
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

public class IronFarmMenu extends AbstractContainerMenu {

    public static final int SLOT_VILLAGER_X = 17, SLOT_VILLAGER_Y = 28;
    public static final int OUTPUT_X = 98, OUTPUT_Y_TOP = 19, OUTPUT_Y_BOT = 37;

    private final ContainerData containerData;
    private final BlockPos ironFarmPos;

    /** Server-side constructor. */
    public IronFarmMenu(int containerId, Inventory playerInventory, IronFarmTileentity ironFarm, BlockPos pos) {
        super(ModMenuTypes.IRON_FARM, containerId);
        this.ironFarmPos = pos;

        checkContainerSize(ironFarm, 5);

        // Slot 0 – villager input
        addSlot(new Slot(ironFarm, 0, SLOT_VILLAGER_X, SLOT_VILLAGER_Y) {
            @Override public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof VillagerItem && !VillagerData.isBaby(stack);
            }
        });

        // Slots 1–4 – 2×2 output (take-only)
        addSlot(new Slot(ironFarm, 1, OUTPUT_X,      OUTPUT_Y_TOP) { @Override public boolean mayPlace(ItemStack s) { return false; } });
        addSlot(new Slot(ironFarm, 2, OUTPUT_X + 18, OUTPUT_Y_TOP) { @Override public boolean mayPlace(ItemStack s) { return false; } });
        addSlot(new Slot(ironFarm, 3, OUTPUT_X,      OUTPUT_Y_BOT) { @Override public boolean mayPlace(ItemStack s) { return false; } });
        addSlot(new Slot(ironFarm, 4, OUTPUT_X + 18, OUTPUT_Y_BOT) { @Override public boolean mayPlace(ItemStack s) { return false; } });

        addPlayerInventory(playerInventory);

        this.containerData = new ContainerData() {
            @Override public int get(int i) {
                return i == 0 ? ironFarm.getIronTimer() : i == 1 ? IronFarmTileentity.IRON_INTERVAL : 0;
            }
            @Override public void set(int i, int val) { }
            @Override public int getCount() { return 2; }
        };
        addDataSlots(containerData);
    }

    /** Client-side constructor. */
    public IronFarmMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        super(ModMenuTypes.IRON_FARM, containerId);
        this.ironFarmPos = pos;

        SimpleContainer dummy = new SimpleContainer(5);

        addSlot(new Slot(dummy, 0, SLOT_VILLAGER_X, SLOT_VILLAGER_Y));
        addSlot(new Slot(dummy, 1, OUTPUT_X,      OUTPUT_Y_TOP) { @Override public boolean mayPlace(ItemStack s) { return false; } });
        addSlot(new Slot(dummy, 2, OUTPUT_X + 18, OUTPUT_Y_TOP) { @Override public boolean mayPlace(ItemStack s) { return false; } });
        addSlot(new Slot(dummy, 3, OUTPUT_X,      OUTPUT_Y_BOT) { @Override public boolean mayPlace(ItemStack s) { return false; } });
        addSlot(new Slot(dummy, 4, OUTPUT_X + 18, OUTPUT_Y_BOT) { @Override public boolean mayPlace(ItemStack s) { return false; } });

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

    public BlockPos getIronFarmPos() { return ironFarmPos; }

    public int getIronProgress() {
        int ticks = containerData.get(0);
        int max   = containerData.get(1);
        if (max <= 0) return 0;
        return ticks * 100 / max;
    }

    @Override
    public boolean stillValid(Player player) {
        double dx = ironFarmPos.getX() + 0.5 - player.getX();
        double dy = ironFarmPos.getY() + 0.5 - player.getY();
        double dz = ironFarmPos.getZ() + 0.5 - player.getZ();
        return dx * dx + dy * dy + dz * dz <= 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy  = stack.copy();

        if (index < 5) {
            if (!moveItemStackTo(stack, 5, 41, true)) return ItemStack.EMPTY;
        } else {
            if (stack.getItem() instanceof VillagerItem && !VillagerData.isBaby(stack)) {
                if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            } else {
                return ItemStack.EMPTY; // no other container slot accepts player items
            }
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }
}
