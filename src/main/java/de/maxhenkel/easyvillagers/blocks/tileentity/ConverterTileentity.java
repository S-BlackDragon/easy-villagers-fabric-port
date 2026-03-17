package de.maxhenkel.easyvillagers.blocks.tileentity;

import de.maxhenkel.easyvillagers.blocks.ModBlocks;
import de.maxhenkel.easyvillagers.datacomponents.VillagerData;
import de.maxhenkel.easyvillagers.entity.EasyVillagerEntity;
import de.maxhenkel.easyvillagers.items.ModItems;
import de.maxhenkel.easyvillagers.items.VillagerItem;
import de.maxhenkel.easyvillagers.items.ZombieVillagerItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class ConverterTileentity extends FakeWorldTileentity implements Container {

    private ItemStack inputVillager  = ItemStack.EMPTY;
    private ItemStack outputVillager = ItemStack.EMPTY;
    private int convertingTicks = 0;

    public static final int MAX_TICKS = 3600; // 3 minutes

    public final ContainerData dataAccess = new ContainerData() {
        @Override public int get(int i) {
            return switch (i) {
                case 0 -> convertingTicks;
                case 1 -> MAX_TICKS;
                default -> 0;
            };
        }
        @Override public void set(int i, int value) { if (i == 0) convertingTicks = value; }
        @Override public int getCount() { return 2; }
    };

    public ConverterTileentity(BlockPos pos, BlockState state) {
        super(ModTileEntities.CONVERTER, ModBlocks.CONVERTER.defaultBlockState(), pos, state);
    }

    public boolean hasInput()  { return !inputVillager.isEmpty(); }
    public boolean hasOutput() { return !outputVillager.isEmpty(); }

    // -----------------------------------------------------------------------
    // Server tick
    // -----------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, ConverterTileentity te) {
        if (!te.hasInput() || te.hasOutput()) return;

        if (te.convertingTicks == 0) {
            if (!(te.inputVillager.getItem() instanceof ZombieVillagerItem)) return;
        }

        te.convertingTicks++;

        if (te.convertingTicks >= MAX_TICKS) {
            // Build output: normal villager with discounted trades + visual mark
            ItemStack output = new ItemStack(ModItems.VILLAGER);
            VillagerData data = te.inputVillager.get(ModItems.VILLAGER_DATA_COMPONENT);
            if (data != null) output.set(ModItems.VILLAGER_DATA_COMPONENT, data);

            EasyVillagerEntity entity = VillagerData.createEasyVillager(output, level);
            entity.generateTrades();
            for (MerchantOffer offer : entity.getOffers()) {
                offer.addToSpecialPriceDiff(-5);
            }
            VillagerData.applyToItem(output, entity);

            // Mark as converted so players can distinguish discounted villagers
            output.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);

            te.outputVillager = output;
            te.inputVillager = ItemStack.EMPTY;
            te.convertingTicks = 0;
            te.setChanged();
            te.sync();
            level.playSound(null, pos, SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.BLOCKS, 1F, 1F);
        } else if (level.getGameTime() % 20 == 0) {
            te.setChanged();
        }
    }

    // -----------------------------------------------------------------------
    // Container (slot 0 = input, slot 1 = output)
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
            if (inputVillager.isEmpty()) { inputVillager = ItemStack.EMPTY; convertingTicks = 0; }
            setChanged(); sync(); return taken;
        }
        if (slot == 1 && !outputVillager.isEmpty()) {
            ItemStack taken = outputVillager.split(amount);
            if (outputVillager.isEmpty()) outputVillager = ItemStack.EMPTY;
            setChanged(); sync(); return taken;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot == 0) { ItemStack s = inputVillager; inputVillager = ItemStack.EMPTY; convertingTicks = 0; return s; }
        if (slot == 1) { ItemStack s = outputVillager; outputVillager = ItemStack.EMPTY; return s; }
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == 0) { inputVillager = stack; convertingTicks = 0; setChanged(); sync(); }
        else if (slot == 1) { outputVillager = stack; setChanged(); }
    }

    @Override public boolean stillValid(Player player) { return true; }
    @Override public void clearContent() {
        inputVillager = ItemStack.EMPTY; outputVillager = ItemStack.EMPTY; convertingTicks = 0;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == 0) return stack.getItem() instanceof ZombieVillagerItem;
        return false;
    }

    // -----------------------------------------------------------------------
    // NBT
    // -----------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        if (!inputVillager.isEmpty())  tag.put("InputVillager",  inputVillager.save(provider));
        if (!outputVillager.isEmpty()) tag.put("OutputVillager", outputVillager.save(provider));
        tag.putInt("ConvertingTicks", convertingTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        inputVillager  = tag.contains("InputVillager")  ? VillagerData.convert(provider, tag.getCompound("InputVillager"))  : ItemStack.EMPTY;
        outputVillager = tag.contains("OutputVillager") ? VillagerData.convert(provider, tag.getCompound("OutputVillager")) : ItemStack.EMPTY;
        convertingTicks = tag.getInt("ConvertingTicks");
        super.loadAdditional(tag, provider);
    }

    public ItemStack getInputVillager()  { return inputVillager; }
    public ItemStack getOutputVillager() { return outputVillager; }

    // -----------------------------------------------------------------------
    // Renderer helpers
    // -----------------------------------------------------------------------

    public int getTimer() { return convertingTicks; }

    public static int getZombifyTime() { return MAX_TICKS * 2 / 3; } // 2400 ticks

    public static int getConvertTime() { return MAX_TICKS; }

    public EasyVillagerEntity getVillagerEntity() {
        if (inputVillager.isEmpty() || level == null) return null;
        return VillagerData.getCacheVillager(inputVillager, level);
    }
}
