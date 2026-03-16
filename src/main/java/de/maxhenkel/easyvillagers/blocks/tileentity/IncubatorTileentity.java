package de.maxhenkel.easyvillagers.blocks.tileentity;

import de.maxhenkel.easyvillagers.Main;
import de.maxhenkel.easyvillagers.blocks.ModBlocks;
import org.slf4j.LoggerFactory;
import de.maxhenkel.easyvillagers.datacomponents.VillagerData;
import de.maxhenkel.easyvillagers.entity.EasyVillagerEntity;
import de.maxhenkel.easyvillagers.items.VillagerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class IncubatorTileentity extends FakeWorldTileentity implements Container {

    private ItemStack inputVillager = ItemStack.EMPTY;
    private ItemStack outputVillager = ItemStack.EMPTY;
    @Nullable
    private EasyVillagerEntity villagerEntity = null;
    private int incubationTicks = 0;

    public final ContainerData dataAccess = new ContainerData() {
        @Override public int get(int i) {
            return switch (i) {
                case 0 -> incubationTicks;
                case 1 -> maxIncubationTicks();
                default -> 0;
            };
        }
        @Override public void set(int i, int value) { if (i == 0) incubationTicks = value; }
        @Override public int getCount() { return 2; }
    };

    public IncubatorTileentity(BlockPos pos, BlockState state) {
        super(ModTileEntities.INCUBATOR, ModBlocks.INCUBATOR.defaultBlockState(), pos, state);
    }

    public static int maxIncubationTicks() {
        return 1200; // 1 minute at 20 ticks/sec
    }

    public boolean hasInput()  { return !inputVillager.isEmpty(); }
    public boolean hasOutput() { return !outputVillager.isEmpty(); }

    @Nullable
    public EasyVillagerEntity getVillagerEntity() {
        if (villagerEntity == null && !inputVillager.isEmpty()) {
            villagerEntity = VillagerData.createEasyVillager(inputVillager, level);
        }
        return villagerEntity;
    }

    private void saveVillagerEntity() {
        if (villagerEntity != null) {
            VillagerData.applyToItem(inputVillager, villagerEntity);
        }
    }

    // -----------------------------------------------------------------------
    // Server tick
    // -----------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, IncubatorTileentity te) {
        if (!te.hasInput() || te.hasOutput()) return; // wait until player picks up output

        // Only validate "is baby" at the very start (tick 0). Once in progress,
        // the entity age reaches 0 before incubationTicks reaches max, which would
        // cause isBaby() to return false and stop the process prematurely.
        if (te.incubationTicks == 0) {
            EasyVillagerEntity entity = te.getVillagerEntity();
            if (entity == null || !entity.isBaby()) {
                LoggerFactory.getLogger("EasyVillagers").debug(
                    "Incubator skipped: entity={} isBaby={}", entity, entity != null && entity.isBaby());
                return;
            }
        }

        te.incubationTicks++;
        if (te.incubationTicks % 200 == 0) {
            LoggerFactory.getLogger("EasyVillagers").info(
                "Incubator tick: {}/{}", te.incubationTicks, maxIncubationTicks());
        }

        if (te.incubationTicks >= maxIncubationTicks()) {
            // Fully grown — force age to 0 and move to output slot
            EasyVillagerEntity grown = te.getVillagerEntity();
            if (grown != null) grown.setAge(0);
            te.saveVillagerEntity();
            te.outputVillager = te.inputVillager.copy();
            te.inputVillager = ItemStack.EMPTY;
            te.villagerEntity = null;
            te.incubationTicks = 0;
            te.setChanged();
            te.sync();
            level.playSound(null, pos, SoundEvents.VILLAGER_CELEBRATE, SoundSource.BLOCKS, 1F, 0.9F);
        } else if (level.getGameTime() % 20 == 0) {
            te.setChanged();
        }
    }

    // -----------------------------------------------------------------------
    // Container interface (slot 0 = input baby, slot 1 = output adult)
    // -----------------------------------------------------------------------

    @Override public int getContainerSize() { return 2; }
    @Override public boolean isEmpty() { return inputVillager.isEmpty() && outputVillager.isEmpty(); }

    @Override
    public ItemStack getItem(int slot) {
        return slot == 0 ? inputVillager : slot == 1 ? outputVillager : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot == 0 && !inputVillager.isEmpty()) {
            ItemStack taken = inputVillager.split(amount);
            if (inputVillager.isEmpty()) {
                inputVillager = ItemStack.EMPTY;
                villagerEntity = null;
                incubationTicks = 0;
            }
            setChanged();
            sync();
            return taken;
        }
        if (slot == 1 && !outputVillager.isEmpty()) {
            ItemStack taken = outputVillager.split(amount);
            if (outputVillager.isEmpty()) outputVillager = ItemStack.EMPTY;
            setChanged();
            sync();
            return taken;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot == 0) { ItemStack s = inputVillager; inputVillager = ItemStack.EMPTY; villagerEntity = null; incubationTicks = 0; return s; }
        if (slot == 1) { ItemStack s = outputVillager; outputVillager = ItemStack.EMPTY; return s; }
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == 0) {
            inputVillager = stack;
            villagerEntity = stack.isEmpty() ? null : VillagerData.createEasyVillager(stack, level);
            incubationTicks = 0;
            setChanged();
            sync();
        } else if (slot == 1) {
            outputVillager = stack;
            setChanged();
        }
    }

    @Override public boolean stillValid(Player player) { return true; }
    @Override public void clearContent() {
        inputVillager = ItemStack.EMPTY;
        outputVillager = ItemStack.EMPTY;
        villagerEntity = null;
        incubationTicks = 0;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == 0) return stack.getItem() instanceof VillagerItem && VillagerData.isBaby(stack);
        return false; // output slot: no placing
    }

    // -----------------------------------------------------------------------
    // NBT
    // -----------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        if (!inputVillager.isEmpty()) {
            if (villagerEntity != null) saveVillagerEntity();
            tag.put("InputVillager", inputVillager.save(provider));
        }
        if (!outputVillager.isEmpty()) {
            tag.put("OutputVillager", outputVillager.save(provider));
        }
        tag.putInt("IncubationTicks", incubationTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        inputVillager = tag.contains("InputVillager")
                ? VillagerData.convert(provider, tag.getCompound("InputVillager"))
                : ItemStack.EMPTY;
        outputVillager = tag.contains("OutputVillager")
                ? VillagerData.convert(provider, tag.getCompound("OutputVillager"))
                : ItemStack.EMPTY;
        villagerEntity = null;
        incubationTicks = tag.getInt("IncubationTicks");
        super.loadAdditional(tag, provider);
    }

    // Called by IncubatorBlock.onRemove to drop both items
    public ItemStack getInputVillager()  { return inputVillager; }
    public ItemStack getOutputVillager() { return outputVillager; }
}
