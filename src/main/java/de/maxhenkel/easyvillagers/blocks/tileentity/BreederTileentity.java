package de.maxhenkel.easyvillagers.blocks.tileentity;

import de.maxhenkel.easyvillagers.blocks.ModBlocks;
import de.maxhenkel.easyvillagers.datacomponents.VillagerData;
import de.maxhenkel.easyvillagers.entity.EasyVillagerEntity;
import de.maxhenkel.easyvillagers.items.VillagerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/**
 * Breeder tileentity.
 * Container layout:
 *   slot 0      = villager1 (adult)
 *   slot 1      = villager2 (adult)
 *   slots 2–5   = food input (4 slots, any item)
 *   slots 6–9   = baby villager output (4 slots)
 */
public class BreederTileentity extends FakeWorldTileentity implements Container {

    public static final int BREED_INTERVAL = 600; // 30 seconds

    protected NonNullList<ItemStack> foodInventory   = NonNullList.withSize(4, ItemStack.EMPTY);
    protected NonNullList<ItemStack> outputInventory = NonNullList.withSize(4, ItemStack.EMPTY);

    protected ItemStack villager1 = ItemStack.EMPTY;
    protected ItemStack villager2 = ItemStack.EMPTY;
    @Nullable protected EasyVillagerEntity villagerEntity1;
    @Nullable protected EasyVillagerEntity villagerEntity2;

    private int breedTimer = 0;

    public BreederTileentity(BlockPos pos, BlockState state) {
        super(ModTileEntities.BREEDER, ModBlocks.BREEDER.defaultBlockState(), pos, state);
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------

    public ItemStack getVillager1() { return villager1; }
    public ItemStack getVillager2() { return villager2; }
    public boolean hasVillager1() { return !villager1.isEmpty(); }
    public boolean hasVillager2() { return !villager2.isEmpty(); }

    @Nullable
    public EasyVillagerEntity getVillagerEntity1() {
        if (villagerEntity1 == null && !villager1.isEmpty())
            villagerEntity1 = VillagerData.createEasyVillager(villager1, level);
        return villagerEntity1;
    }

    @Nullable
    public EasyVillagerEntity getVillagerEntity2() {
        if (villagerEntity2 == null && !villager2.isEmpty())
            villagerEntity2 = VillagerData.createEasyVillager(villager2, level);
        return villagerEntity2;
    }

    public void setVillager1(ItemStack stack) {
        villager1 = stack;
        villagerEntity1 = stack.isEmpty() ? null : VillagerData.createEasyVillager(stack, level);
        setChanged(); sync();
    }

    public void setVillager2(ItemStack stack) {
        villager2 = stack;
        villagerEntity2 = stack.isEmpty() ? null : VillagerData.createEasyVillager(stack, level);
        setChanged(); sync();
    }

    public ItemStack removeVillager1() { ItemStack v = villager1; setVillager1(ItemStack.EMPTY); return v; }
    public ItemStack removeVillager2() { ItemStack v = villager2; setVillager2(ItemStack.EMPTY); return v; }

    public int getBreedTimer() { return breedTimer; }

    public boolean hasFoodAvailable() {
        for (ItemStack s : foodInventory) if (!s.isEmpty()) return true;
        return false;
    }

    public boolean hasOutputSpace() {
        for (ItemStack s : outputInventory) if (s.isEmpty()) return true;
        return false;
    }

    // -----------------------------------------------------------------------
    // Server tick
    // -----------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, BreederTileentity te) {
        if (!te.hasVillager1() || !te.hasVillager2() || !te.hasFoodAvailable() || !te.hasOutputSpace()) {
            te.breedTimer = 0;
            return;
        }

        te.breedTimer++;
        if (level.getGameTime() % 20 == 0) te.setChanged();

        if (te.breedTimer >= BREED_INTERVAL) {
            te.breedTimer = 0;
            te.breedVillagers(level);
            te.setChanged();
            te.sync();
        }
    }

    private void breedVillagers(Level level) {
        // Consume 1 food item
        for (int i = 0; i < 4; i++) {
            if (!foodInventory.get(i).isEmpty()) {
                foodInventory.get(i).shrink(1);
                if (foodInventory.get(i).isEmpty()) foodInventory.set(i, ItemStack.EMPTY);
                break;
            }
        }

        // Create baby from villager1
        ItemStack baby = createBaby();
        if (baby.isEmpty()) return;

        // Insert into first empty output slot
        for (int i = 0; i < 4; i++) {
            if (outputInventory.get(i).isEmpty()) {
                outputInventory.set(i, baby);
                if (level != null) {
                    level.playSound(null, worldPosition, SoundEvents.VILLAGER_CELEBRATE,
                            SoundSource.BLOCKS, 1F, 1.6F);
                }
                return;
            }
        }
    }

    private ItemStack createBaby() {
        ItemStack babyStack = villager1.copy();
        babyStack.setCount(1);
        EasyVillagerEntity entity = VillagerData.createEasyVillager(babyStack, level);
        if (entity == null) return ItemStack.EMPTY;
        entity.setAge(-24000); // make baby
        VillagerData.applyToItem(babyStack, entity);
        return babyStack;
    }

    // -----------------------------------------------------------------------
    // Container interface (10 slots)
    // -----------------------------------------------------------------------

    @Override public int getContainerSize() { return 10; }

    @Override
    public boolean isEmpty() {
        if (!villager1.isEmpty() || !villager2.isEmpty()) return false;
        for (ItemStack s : foodInventory)   if (!s.isEmpty()) return false;
        for (ItemStack s : outputInventory) if (!s.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot == 0) return villager1;
        if (slot == 1) return villager2;
        if (slot >= 2 && slot <= 5) return foodInventory.get(slot - 2);
        if (slot >= 6 && slot <= 9) return outputInventory.get(slot - 6);
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot == 0) {
            ItemStack t = villager1.split(amount);
            if (villager1.isEmpty()) { villager1 = ItemStack.EMPTY; villagerEntity1 = null; }
            setChanged(); return t;
        }
        if (slot == 1) {
            ItemStack t = villager2.split(amount);
            if (villager2.isEmpty()) { villager2 = ItemStack.EMPTY; villagerEntity2 = null; }
            setChanged(); return t;
        }
        if (slot >= 2 && slot <= 5) {
            ItemStack s = foodInventory.get(slot - 2);
            if (s.isEmpty()) return ItemStack.EMPTY;
            ItemStack t = s.split(amount);
            if (s.isEmpty()) foodInventory.set(slot - 2, ItemStack.EMPTY);
            setChanged(); return t;
        }
        if (slot >= 6 && slot <= 9) {
            ItemStack s = outputInventory.get(slot - 6);
            if (s.isEmpty()) return ItemStack.EMPTY;
            ItemStack t = s.split(amount);
            if (s.isEmpty()) outputInventory.set(slot - 6, ItemStack.EMPTY);
            setChanged(); return t;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot == 0) { ItemStack s = villager1; villager1 = ItemStack.EMPTY; villagerEntity1 = null; return s; }
        if (slot == 1) { ItemStack s = villager2; villager2 = ItemStack.EMPTY; villagerEntity2 = null; return s; }
        if (slot >= 2 && slot <= 5) { ItemStack s = foodInventory.get(slot-2); foodInventory.set(slot-2, ItemStack.EMPTY); return s; }
        if (slot >= 6 && slot <= 9) { ItemStack s = outputInventory.get(slot-6); outputInventory.set(slot-6, ItemStack.EMPTY); return s; }
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == 0) { villager1 = stack; villagerEntity1 = stack.isEmpty() ? null : VillagerData.createEasyVillager(stack, level); setChanged(); }
        else if (slot == 1) { villager2 = stack; villagerEntity2 = stack.isEmpty() ? null : VillagerData.createEasyVillager(stack, level); setChanged(); }
        else if (slot >= 2 && slot <= 5) { foodInventory.set(slot - 2, stack); setChanged(); }
        else if (slot >= 6 && slot <= 9) { outputInventory.set(slot - 6, stack); setChanged(); }
    }

    @Override public boolean stillValid(Player player) { return true; }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == 0 || slot == 1) return stack.getItem() instanceof VillagerItem && !VillagerData.isBaby(stack);
        if (slot >= 2 && slot <= 5) return true;
        return false; // output slots: no placing
    }

    @Override
    public void clearContent() {
        villager1 = ItemStack.EMPTY; villagerEntity1 = null;
        villager2 = ItemStack.EMPTY; villagerEntity2 = null;
        for (int i = 0; i < 4; i++) foodInventory.set(i, ItemStack.EMPTY);
        for (int i = 0; i < 4; i++) outputInventory.set(i, ItemStack.EMPTY);
        breedTimer = 0;
    }

    // -----------------------------------------------------------------------
    // NBT
    // -----------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag compound, HolderLookup.Provider provider) {
        super.saveAdditional(compound, provider);
        if (!villager1.isEmpty()) compound.put("Villager1", villager1.save(provider));
        if (!villager2.isEmpty()) compound.put("Villager2", villager2.save(provider));
        compound.put("FoodInventory",   ContainerHelper.saveAllItems(new CompoundTag(), foodInventory, true, provider));
        compound.put("OutputInventory", ContainerHelper.saveAllItems(new CompoundTag(), outputInventory, true, provider));
        compound.putInt("BreedTimer", breedTimer);
    }

    @Override
    protected void loadAdditional(CompoundTag compound, HolderLookup.Provider provider) {
        villager1 = compound.contains("Villager1") ? VillagerData.convert(provider, compound.getCompound("Villager1")) : ItemStack.EMPTY;
        villager2 = compound.contains("Villager2") ? VillagerData.convert(provider, compound.getCompound("Villager2")) : ItemStack.EMPTY;
        villagerEntity1 = null;
        villagerEntity2 = null;
        VillagerData.convertInventory(compound.getCompound("FoodInventory"),   foodInventory, provider);
        VillagerData.convertInventory(compound.getCompound("OutputInventory"), outputInventory, provider);
        breedTimer = compound.getInt("BreedTimer");
        super.loadAdditional(compound, provider);
    }
}
