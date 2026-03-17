package de.maxhenkel.easyvillagers.blocks.tileentity;

import de.maxhenkel.easyvillagers.blocks.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Iron Farm tileentity.
 * Container layout:
 *   slot 0      = villager input (managed by parent VillagerTileentity)
 *   slots 1–4   = output (2×2 grid): iron ingots and poppies
 */
public class IronFarmTileentity extends TraderTileentityBase implements Container, WorldlyContainer {

    private static final int[] SLOTS_OUTPUT = {1, 2, 3, 4};
    private static final int[] SLOTS_NONE   = {};

    public static final int IRON_INTERVAL = 600; // 30 seconds

    private final ItemStack[] outputSlots = new ItemStack[4];
    private int ironTimer = 0;

    public IronFarmTileentity(BlockPos pos, BlockState state) {
        super(ModTileEntities.IRON_FARM, ModBlocks.IRON_FARM.defaultBlockState(), pos, state);
        for (int i = 0; i < 4; i++) outputSlots[i] = ItemStack.EMPTY;
    }

    // -----------------------------------------------------------------------
    // Server tick
    // -----------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, IronFarmTileentity te) {
        if (!te.hasVillager()) {
            te.ironTimer = 0;
            return;
        }

        te.ironTimer++;
        if (level.getGameTime() % 20 == 0) {
            te.setChanged();
            te.sync(); // keep client timer up-to-date for golem renderer
        }

        if (te.ironTimer >= IRON_INTERVAL) {
            te.ironTimer = 0;
            te.generateDrops(level);
            te.setChanged();
            te.sync();
        }
    }

    private void generateDrops(Level level) {
        // Always produce 3-5 iron ingots
        int ironCount = 3 + level.random.nextInt(3);
        tryInsertOutput(new ItemStack(Items.IRON_INGOT, ironCount));

        // 50% chance to produce 1 poppy (simulates golem kill drops)
        if (level.random.nextBoolean()) {
            tryInsertOutput(new ItemStack(Items.POPPY, 1));
        }
    }

    private void tryInsertOutput(ItemStack stack) {
        // Try stacking
        for (int i = 0; i < 4; i++) {
            ItemStack slot = outputSlots[i];
            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, stack)) {
                int space = slot.getMaxStackSize() - slot.getCount();
                if (space > 0) {
                    int add = Math.min(space, stack.getCount());
                    slot.grow(add);
                    stack.shrink(add);
                    if (stack.isEmpty()) return;
                }
            }
        }
        // Fill empty slot
        for (int i = 0; i < 4; i++) {
            if (outputSlots[i].isEmpty()) {
                outputSlots[i] = stack.copy();
                return;
            }
        }
    }

    public int getIronTimer() { return ironTimer; }

    /** Alias used by the block entity renderer. */
    public int getTimer() { return ironTimer; }

    public static int getGolemSpawnTime() { return IRON_INTERVAL - 40; }
    public static int getGolemKillTime()  { return IRON_INTERVAL; }

    // -----------------------------------------------------------------------
    // Container (slot 0=villager, slots 1-4=output)
    // -----------------------------------------------------------------------

    @Override public int getContainerSize() { return 5; }

    @Override
    public boolean isEmpty() {
        if (hasVillager()) return false;
        for (ItemStack s : outputSlots) if (!s.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot == 0) return getVillager();
        if (slot >= 1 && slot <= 4) return outputSlots[slot - 1];
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot == 0) {
            ItemStack taken = getVillager().split(amount);
            if (getVillager().isEmpty()) setVillager(ItemStack.EMPTY);
            setChanged(); return taken;
        }
        if (slot >= 1 && slot <= 4) {
            ItemStack s = outputSlots[slot - 1];
            if (s.isEmpty()) return ItemStack.EMPTY;
            ItemStack taken = s.split(amount);
            if (s.isEmpty()) outputSlots[slot - 1] = ItemStack.EMPTY;
            setChanged(); return taken;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot == 0) { ItemStack s = getVillager(); setVillager(ItemStack.EMPTY); return s; }
        if (slot >= 1 && slot <= 4) { ItemStack s = outputSlots[slot-1]; outputSlots[slot-1] = ItemStack.EMPTY; return s; }
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == 0) setVillager(stack);
        else if (slot >= 1 && slot <= 4) { outputSlots[slot - 1] = stack; setChanged(); }
    }

    @Override public boolean stillValid(Player player) { return true; }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    // -----------------------------------------------------------------------
    // WorldlyContainer (hopper / pipe support)
    // -----------------------------------------------------------------------

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) return SLOTS_OUTPUT; // pull iron/poppies from below
        return SLOTS_NONE;                               // villager slot never accessible
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return false; // nothing goes in via hopper
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return slot >= 1 && slot <= 4; // only output slots
    }

    @Override
    public void clearContent() {
        setVillager(ItemStack.EMPTY);
        for (int i = 0; i < 4; i++) outputSlots[i] = ItemStack.EMPTY;
    }

    // -----------------------------------------------------------------------
    // NBT
    // -----------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("IronTimer", ironTimer);
        ListTag list = new ListTag();
        for (int i = 0; i < 4; i++) {
            if (!outputSlots[i].isEmpty()) {
                CompoundTag entry = new CompoundTag();
                entry.putByte("Slot", (byte) i);
                entry.put("Item", outputSlots[i].save(provider));
                list.add(entry);
            }
        }
        if (!list.isEmpty()) tag.put("OutputSlots", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        ironTimer = tag.getInt("IronTimer");
        for (int i = 0; i < 4; i++) outputSlots[i] = ItemStack.EMPTY;
        if (tag.contains("OutputSlots", Tag.TAG_LIST)) {
            ListTag list = tag.getList("OutputSlots", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                int s = entry.getByte("Slot") & 0xFF;
                if (s < 4) outputSlots[s] = ItemStack.parseOptional(provider, entry.getCompound("Item"));
            }
        }
        super.loadAdditional(tag, provider);
    }
}
