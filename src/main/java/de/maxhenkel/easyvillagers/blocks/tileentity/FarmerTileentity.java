package de.maxhenkel.easyvillagers.blocks.tileentity;

import de.maxhenkel.easyvillagers.blocks.ModBlocks;
import de.maxhenkel.easyvillagers.datacomponents.VillagerData;
import de.maxhenkel.easyvillagers.items.VillagerItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Farmer tileentity.
 * Container layout:
 *   slot 0      = villager input (managed by parent VillagerTileentity)
 *   slot 1      = food input (any item; consumed each cycle)
 *   slots 2–5   = crop output (2×2 grid)
 */
public class FarmerTileentity extends TraderTileentityBase implements WorldlyContainer {

    public static final int FARM_INTERVAL = 300; // 15 seconds

    // localSlots[0] = food input, localSlots[1..4] = output slots
    private final ItemStack[] localSlots = new ItemStack[5];
    private int farmTimer = 0;

    private static final int[] SLOTS_TOP   = {0};
    private static final int[] SLOTS_SIDES = {1};
    private static final int[] SLOTS_DOWN  = {2, 3, 4, 5};

    public FarmerTileentity(BlockPos pos, BlockState state) {
        super(ModTileEntities.FARMER, ModBlocks.FARMER.defaultBlockState(), pos, state);
        for (int i = 0; i < 5; i++) localSlots[i] = ItemStack.EMPTY;
    }

    // -----------------------------------------------------------------------
    // Server tick
    // -----------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, FarmerTileentity te) {
        if (!te.hasVillager() || te.localSlots[0].isEmpty()) {
            te.farmTimer = 0;
            return;
        }
        te.farmTimer++;
        if (level.getGameTime() % 20 == 0) te.setChanged();

        if (te.farmTimer >= FARM_INTERVAL) {
            te.farmTimer = 0;
            te.generateCrop(level);
            te.setChanged();
            te.sync();
        }
    }

    private void generateCrop(Level level) {
        ItemStack food = localSlots[0];
        if (food.isEmpty()) return;

        // Consume 1 food item and produce 2-4 of the same type in output
        ItemStack produce = food.copy();
        produce.setCount(1);
        food.shrink(1);
        if (food.isEmpty()) localSlots[0] = ItemStack.EMPTY;

        int amount = 2 + level.random.nextInt(3); // 2, 3, or 4
        ItemStack output = produce.copy();
        output.setCount(amount);
        tryInsertOutput(output);
    }

    private void tryInsertOutput(ItemStack stack) {
        // Try to stack with existing matching slots
        for (int i = 1; i <= 4; i++) {
            ItemStack slot = localSlots[i];
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
        // Fill empty slots
        for (int i = 1; i <= 4; i++) {
            if (localSlots[i].isEmpty()) {
                localSlots[i] = stack.copy();
                return;
            }
        }
    }

    public int getFarmTimer() { return farmTimer; }

    /** Returns a crop block state to display in the renderer, derived from the food input slot. */
    @Nullable
    public BlockState getCrop() {
        ItemStack food = localSlots[0];
        if (food.isEmpty()) return null;
        if (food.getItem() instanceof BlockItem bi) return bi.getBlock().defaultBlockState();
        return null;
    }

    // -----------------------------------------------------------------------
    // WorldlyContainer (hopper support)
    // -----------------------------------------------------------------------

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.UP)   return SLOTS_TOP;
        if (side == Direction.DOWN) return SLOTS_DOWN;
        return SLOTS_SIDES;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction dir) {
        if (slot == 0) return canPlaceItem(0, stack);
        if (slot == 1) return true;
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return slot >= 2;
    }

    // -----------------------------------------------------------------------
    // Container  (slot 0=villager, slot 1=food, slots 2-5=output)
    // -----------------------------------------------------------------------

    @Override public int getContainerSize() { return 6; }

    @Override
    public boolean isEmpty() {
        if (hasVillager()) return false;
        for (ItemStack s : localSlots) if (!s.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot == 0) return getVillager();
        if (slot >= 1 && slot <= 5) return localSlots[slot - 1];
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot == 0) {
            ItemStack taken = getVillager().split(amount);
            if (getVillager().isEmpty()) setVillager(ItemStack.EMPTY);
            setChanged(); return taken;
        }
        if (slot >= 1 && slot <= 5) {
            ItemStack s = localSlots[slot - 1];
            if (s.isEmpty()) return ItemStack.EMPTY;
            ItemStack taken = s.split(amount);
            if (s.isEmpty()) localSlots[slot - 1] = ItemStack.EMPTY;
            setChanged(); return taken;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot == 0) { ItemStack s = getVillager(); setVillager(ItemStack.EMPTY); return s; }
        if (slot >= 1 && slot <= 5) {
            ItemStack s = localSlots[slot - 1];
            localSlots[slot - 1] = ItemStack.EMPTY;
            return s;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == 0) setVillager(stack);
        else if (slot >= 1 && slot <= 5) { localSlots[slot - 1] = stack; setChanged(); }
    }

    @Override public boolean stillValid(Player player) { return true; }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == 0) return stack.getItem() instanceof VillagerItem && !VillagerData.isBaby(stack);
        if (slot == 1) return true;
        return false;
    }

    @Override
    public void clearContent() {
        setVillager(ItemStack.EMPTY);
        for (int i = 0; i < 5; i++) localSlots[i] = ItemStack.EMPTY;
    }

    // -----------------------------------------------------------------------
    // NBT
    // -----------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("FarmTimer", farmTimer);
        ListTag list = new ListTag();
        for (int i = 0; i < 5; i++) {
            if (!localSlots[i].isEmpty()) {
                CompoundTag entry = new CompoundTag();
                entry.putByte("Slot", (byte) i);
                entry.put("Item", localSlots[i].save(provider));
                list.add(entry);
            }
        }
        if (!list.isEmpty()) tag.put("LocalSlots", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        farmTimer = tag.getInt("FarmTimer");
        for (int i = 0; i < 5; i++) localSlots[i] = ItemStack.EMPTY;
        if (tag.contains("LocalSlots", Tag.TAG_LIST)) {
            ListTag list = tag.getList("LocalSlots", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                int s = entry.getByte("Slot") & 0xFF;
                if (s < 5) localSlots[s] = ItemStack.parseOptional(provider, entry.getCompound("Item"));
            }
        }
        super.loadAdditional(tag, provider);
    }
}
