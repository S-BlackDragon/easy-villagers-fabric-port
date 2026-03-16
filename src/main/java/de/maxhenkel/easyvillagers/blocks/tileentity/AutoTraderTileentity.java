package de.maxhenkel.easyvillagers.blocks.tileentity;

import de.maxhenkel.easyvillagers.Main;
import de.maxhenkel.easyvillagers.blocks.ModBlocks;
import de.maxhenkel.easyvillagers.entity.EasyVillagerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

public class AutoTraderTileentity extends TraderTileentityBase implements WorldlyContainer {

    // 3 slots: 0 = inputA, 1 = inputB, 2 = output
    private final ItemStack[] slots = new ItemStack[3];
    private int selectedTradeIndex = 0;

    public static final int TRADE_INTERVAL = 200; // 10 seconds
    private int tradeTimer = 0; // counts 0 → TRADE_INTERVAL, not persisted

    public int getTradeTimer() { return tradeTimer; }

    private static final int[] SLOTS_INPUT  = {0, 1};
    private static final int[] SLOTS_OUTPUT = {2};

    public AutoTraderTileentity(BlockPos pos, BlockState state) {
        super(ModTileEntities.AUTO_TRADER, ModBlocks.AUTO_TRADER.defaultBlockState(), pos, state);
        Arrays.fill(slots, ItemStack.EMPTY);
    }

    public int getSelectedTradeIndex() { return selectedTradeIndex; }

    public void setSelectedTradeIndex(int index) {
        selectedTradeIndex = index;
        setChanged();
    }

    // -----------------------------------------------------------------------
    // Server tick
    // -----------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, AutoTraderTileentity te) {
        if (!te.hasVillager()) {
            te.tradeTimer = 0;
            return;
        }

        // Restock logic
        long gameTime = level.getGameTime();
        if (te.nextRestock == 0L) {
            te.nextRestock = gameTime + restockInterval(level);
            te.setChanged();
        } else if (gameTime >= te.nextRestock) {
            EasyVillagerEntity entity = te.getVillagerEntity();
            if (entity != null) {
                entity.restockOffers();
                te.saveVillagerEntity();
            }
            te.nextRestock = gameTime + restockInterval(level);
            te.setChanged();
        }

        EasyVillagerEntity entity = te.getVillagerEntity();
        if (entity == null) { te.tradeTimer = 0; return; }
        entity.generateTrades();

        MerchantOffers offers = entity.getOffers();
        if (offers.isEmpty()) { te.tradeTimer = 0; return; }

        int idx = Math.max(0, Math.min(te.selectedTradeIndex, offers.size() - 1));
        MerchantOffer offer = offers.get(idx);

        // Only advance timer if the trade can actually be executed
        if (offer.isOutOfStock() || !te.canExecuteTrade(offer)) {
            te.tradeTimer = 0;
            return;
        }

        te.tradeTimer++;
        if (te.tradeTimer >= TRADE_INTERVAL) {
            te.tradeTimer = 0;
            te.tryExecuteTrade(entity, offer, level, pos);
        }
    }

    private static long restockInterval(Level level) {
        int min = Main.SERVER_CONFIG.traderMinRestockTime;
        int max = Main.SERVER_CONFIG.traderMaxRestockTime;
        return max > min ? min + level.random.nextInt(max - min) : min;
    }

    boolean canExecuteTrade(MerchantOffer offer) {
        ItemStack costA  = getAdjustedCostA(offer);
        ItemStack costB  = offer.getCostB();
        ItemStack result = offer.getResult();
        if (!matchesSlot(slots[0], costA)) return false;
        if (!costB.isEmpty() && !matchesSlot(slots[1], costB)) return false;
        ItemStack out = slots[2];
        if (!out.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(out, result)) return false;
            if (out.getCount() + result.getCount() > out.getMaxStackSize()) return false;
        }
        return true;
    }

    private boolean tryExecuteTrade(EasyVillagerEntity entity, MerchantOffer offer, Level level, BlockPos pos) {
        if (!canExecuteTrade(offer)) return false;
        ItemStack costA  = getAdjustedCostA(offer);
        ItemStack costB  = offer.getCostB();
        ItemStack result = offer.getResult();

        // Execute
        slots[0].shrink(costA.getCount());
        if (slots[0].isEmpty()) slots[0] = ItemStack.EMPTY;
        if (!costB.isEmpty()) {
            slots[1].shrink(costB.getCount());
            if (slots[1].isEmpty()) slots[1] = ItemStack.EMPTY;
        }
        if (slots[2].isEmpty()) {
            slots[2] = result.copy();
        } else {
            slots[2].grow(result.getCount());
        }

        entity.notifyTrade(offer);
        saveVillagerEntity();
        setChanged();
        sync();

        level.playSound(null, pos, SoundEvents.VILLAGER_TRADE, SoundSource.BLOCKS, 0.5F, 1F);
        return true;
    }

    private static ItemStack getAdjustedCostA(MerchantOffer offer) {
        ItemStack base = offer.getCostA().copy();
        int adjusted = Math.max(1, base.getCount() + offer.getSpecialPriceDiff());
        base.setCount(adjusted);
        return base;
    }

    private static boolean matchesSlot(ItemStack slot, ItemStack needed) {
        if (slot.isEmpty()) return false;
        if (!ItemStack.isSameItemSameComponents(slot, needed)) return false;
        return slot.getCount() >= needed.getCount();
    }

    // -----------------------------------------------------------------------
    // WorldlyContainer (slots 0,1 = input; slot 2 = output)
    // -----------------------------------------------------------------------

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? SLOTS_OUTPUT : SLOTS_INPUT;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return slot == 0 || slot == 1;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return slot == 2;
    }

    @Override public int getContainerSize() { return 3; }

    @Override
    public boolean isEmpty() {
        for (ItemStack s : slots) if (!s.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < 3 ? slots[slot] : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot < 0 || slot >= 3 || slots[slot].isEmpty()) return ItemStack.EMPTY;
        ItemStack taken = slots[slot].split(amount);
        if (slots[slot].isEmpty()) slots[slot] = ItemStack.EMPTY;
        setChanged(); sync();
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot < 0 || slot >= 3) return ItemStack.EMPTY;
        ItemStack s = slots[slot];
        slots[slot] = ItemStack.EMPTY;
        return s;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot >= 0 && slot < 3) slots[slot] = stack;
        setChanged();
    }

    @Override public boolean stillValid(Player player) { return true; }

    @Override
    public void clearContent() {
        Arrays.fill(slots, ItemStack.EMPTY);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0 || slot == 1;
    }

    public ItemStack[] getInputSlots()  { return new ItemStack[]{slots[0], slots[1]}; }
    public ItemStack[] getOutputSlots() { return new ItemStack[]{slots[2]}; }

    // -----------------------------------------------------------------------
    // NBT
    // -----------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ListTag list = new ListTag();
        for (int i = 0; i < slots.length; i++) {
            if (!slots[i].isEmpty()) {
                CompoundTag entry = new CompoundTag();
                entry.putByte("Slot", (byte) i);
                entry.put("Item", slots[i].save(provider));
                list.add(entry);
            }
        }
        tag.put("Slots", list);
        tag.putInt("SelectedTrade", selectedTradeIndex);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        Arrays.fill(slots, ItemStack.EMPTY);
        ListTag list = tag.getList("Slots", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int idx = entry.getByte("Slot") & 0xFF;
            if (idx < slots.length) slots[idx] = ItemStack.parseOptional(provider, entry.getCompound("Item"));
        }
        selectedTradeIndex = tag.getInt("SelectedTrade");
        super.loadAdditional(tag, provider);
    }
}
