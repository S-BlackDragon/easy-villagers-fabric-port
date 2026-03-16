package de.maxhenkel.easyvillagers.blocks.tileentity;

import de.maxhenkel.easyvillagers.Main;
import de.maxhenkel.easyvillagers.entity.EasyVillagerEntity;
import de.maxhenkel.easyvillagers.gui.TraderMenu;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class TraderTileentityBase extends VillagerTileentity {

    protected Block workstation;
    protected long nextRestock;
    /** True if the villager has completed at least one trade while inside this block. */
    protected boolean tradedInThisBlock = false;

    public TraderTileentityBase(BlockEntityType<?> type, BlockState defaultState, BlockPos pos, BlockState state) {
        super(type, defaultState, pos, state);
        workstation = Blocks.AIR;
    }

    public boolean hasTradedInThisBlock() {
        return tradedInThisBlock;
    }

    public Block getWorkstation() {
        return workstation;
    }

    public boolean hasWorkstation() {
        return workstation != Blocks.AIR;
    }

    public void setWorkstation(Block workstation) {
        this.workstation = workstation;

        if (hasVillager()) {
            fixProfession();
        }

        setChanged();
        sync();
    }

    public Block removeWorkstation() {
        Block w = workstation;
        setWorkstation(Blocks.AIR);
        return w;
    }

    public boolean isValidBlock(Block block) {
        return block.getStateDefinition().getPossibleStates().stream()
                .anyMatch(state -> PoiTypes.forState(state).isPresent());
    }

    public VillagerProfession getWorkstationProfession() {
        return workstation.getStateDefinition().getPossibleStates().stream()
                .flatMap(state -> PoiTypes.forState(state).stream())
                .flatMap(poi -> BuiltInRegistries.VILLAGER_PROFESSION.stream()
                        .filter(p -> p.heldJobSite().test(poi)))
                .findFirst()
                .orElse(VillagerProfession.NONE);
    }

    @Override
    protected void onAddVillager(EasyVillagerEntity villager) {
        super.onAddVillager(villager);
        tradedInThisBlock = false;
        if (hasWorkstation()) {
            fixProfession();
        }
    }

    private void fixProfession() {
        EasyVillagerEntity v = getVillagerEntity();
        if (v == null || v.getVillagerXp() > 0 || v.getVillagerData().getProfession().equals(VillagerProfession.NITWIT)) {
            return;
        }
        v.setVillagerData(v.getVillagerData().setProfession(getWorkstationProfession()));
    }

    /**
     * Opens the custom trading GUI for this trader.
     * Returns true if the GUI was opened (or will be opened client-side).
     */
    public boolean openTradingGUI(Player player) {
        EasyVillagerEntity villagerEntity = getVillagerEntity();
        if (villagerEntity == null) return false;
        if (villagerEntity.isBaby()) return false;

        VillagerProfession profession = villagerEntity.getVillagerData().getProfession();
        if (profession.equals(VillagerProfession.NONE) || profession.equals(VillagerProfession.NITWIT)) return false;

        // Client-side: return true so the block doesn't suppress the interaction
        if (level == null || level.isClientSide()) return true;

        if (villagerEntity.isTrading()) return false;

        // Register trading player so getTradingPlayer() works in notifyTrade (XP orbs)
        // and in serverTick (restock notification). MerchantMenu.removed() clears it.
        villagerEntity.setTradingPlayer(player);

        // Generate trades if not yet done
        villagerEntity.generateTrades();

        // After each completed trade: save XP/level changes and refresh client offers
        TraderTileentityBase self = this;
        villagerEntity.setOnTradeCompleted(() -> {
            self.tradedInThisBlock = true;
            self.saveVillagerEntity();
            self.setChanged();
            if (villagerEntity.getTradingPlayer() instanceof ServerPlayer sp) {
                villagerEntity.sendOffers(sp);
            }
        });

        BlockPos blockPos = this.worldPosition;
        Component title = villagerEntity.getAdvancedName();
        EasyVillagerEntity finalVillager = villagerEntity;

        // ExtendedScreenHandlerFactory sends the BlockPos to the client so the
        // Reroll button knows which block to target.
        player.openMenu(new ExtendedScreenHandlerFactory<BlockPos>() {
            @Override
            public BlockPos getScreenOpeningData(ServerPlayer p) {
                return blockPos;
            }

            @Override
            public Component getDisplayName() {
                return title;
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                return new TraderMenu(id, inv, finalVillager, blockPos);
            }
        });

        // Send merchant offers to the client (mirrors what Villager.startTrading does)
        villagerEntity.sendOffers(player);

        return true;
    }

    // -------------------------------------------------------------------------
    // Server tick: periodic restock of depleted trade offers
    // -------------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, TraderTileentityBase trader) {
        if (!trader.hasVillager() || !trader.hasWorkstation()) return;

        EasyVillagerEntity villagerEntity = trader.getVillagerEntity();
        if (villagerEntity == null) return;

        long gameTime = level.getGameTime();

        if (trader.nextRestock == 0L) {
            // Initialize restock timer on first tick
            trader.nextRestock = gameTime + restockInterval(level);
            trader.setChanged();
        } else if (gameTime >= trader.nextRestock) {
            villagerEntity.restockOffers();
            trader.saveVillagerEntity();
            trader.nextRestock = gameTime + restockInterval(level);
            trader.setChanged();

            // Notify any player currently trading with this villager
            if (villagerEntity.getTradingPlayer() instanceof ServerPlayer serverPlayer) {
                villagerEntity.sendOffers(serverPlayer);
            }
        }
    }

    private static long restockInterval(Level level) {
        int min = Main.SERVER_CONFIG.traderMinRestockTime;
        int max = Main.SERVER_CONFIG.traderMaxRestockTime;
        if (max <= min) return min;
        return min + level.random.nextInt(max - min);
    }

    @Override
    protected void saveAdditional(CompoundTag compound, HolderLookup.Provider provider) {
        super.saveAdditional(compound, provider);

        if (hasWorkstation()) {
            compound.putString("Workstation", BuiltInRegistries.BLOCK.getKey(workstation).toString());
        }
        compound.putLong("NextRestock", nextRestock);
        compound.putBoolean("TradedInThisBlock", tradedInThisBlock);
    }

    @Override
    protected void loadAdditional(CompoundTag compound, HolderLookup.Provider provider) {
        if (compound.contains("Workstation")) {
            workstation = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(compound.getString("Workstation")));
        } else {
            workstation = Blocks.AIR;
        }
        nextRestock = compound.getLong("NextRestock");
        tradedInThisBlock = compound.getBoolean("TradedInThisBlock");
        super.loadAdditional(compound, provider);
    }

}
