package de.maxhenkel.easyvillagers.entity;

import de.maxhenkel.easyvillagers.Main;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class EasyVillagerEntity extends Villager {

    // XP needed to advance from level N to N+1 (index 0 = level 1→2, ..., index 3 = level 4→5)
    private static final int[] LEVEL_XP_THRESHOLDS = {10, 70, 150, 250};

    /** True when this entity is a real world entity (placed via item), false when used as a fake trader inside a block. */
    private boolean isRealWorldEntity = false;

    public void markAsRealWorldEntity() {
        this.isRealWorldEntity = true;
        setSilent(false);
    }

    /** Called by the tile entity after each completed trade to save XP/level changes. */
    @Nullable
    private Runnable onTradeCompleted;

    public void setOnTradeCompleted(@Nullable Runnable callback) {
        this.onTradeCompleted = callback;
    }

    public EasyVillagerEntity(EntityType<? extends Villager> type, Level worldIn) {
        super(type, worldIn);
    }

    public EasyVillagerEntity(EntityType<? extends Villager> type, Level worldIn, VillagerType villagerType) {
        super(type, worldIn, villagerType);
    }

    @Override
    public int getPlayerReputation(Player player) {
        if (Main.SERVER_CONFIG.universalReputation) {
            return getUniversalReputation(this);
        } else {
            return super.getPlayerReputation(player);
        }
    }

    public static int getReputation(Villager villager) {
        if (Main.SERVER_CONFIG.universalReputation) {
            return getUniversalReputation(villager);
        } else {
            return 0;
        }
    }

    public static int getUniversalReputation(Villager villager) {
        return villager.getGossips().getGossipEntries().keySet().stream()
                .map(uuid -> villager.getGossips().getReputation(uuid, EasyVillagerEntity::isPositive))
                .reduce(0, Integer::sum);
    }

    public static boolean isPositive(GossipType gossipType) {
        return switch (gossipType) {
            case MAJOR_NEGATIVE, MINOR_NEGATIVE -> false;
            default -> true;
        };
    }

    public void recalculateOffers() {
        recalculateOffers(this);
    }

    @Override
    public int getAge() {
        if (level() != null && level().isClientSide) {
            return super.getAge() < 0 ? -24000 : 1;
        } else {
            return age;
        }
    }

    public static void recalculateOffers(Villager villager) {
        resetOffers(villager);
        calculateOffers(villager);
    }

    private static void resetOffers(Villager villager) {
        for (MerchantOffer offer : villager.getOffers()) {
            offer.resetSpecialPriceDiff();
        }
    }

    private static void calculateOffers(Villager villager) {
        int i = getReputation(villager);
        if (i != 0) {
            for (MerchantOffer offer : villager.getOffers()) {
                offer.addToSpecialPriceDiff(-Mth.floor((float) i * offer.getPriceMultiplier()));
            }
        }
    }

    @Override
    public Component getName() {
        if (hasCustomName()) {
            return super.getName();
        }
        VillagerData villagerData = getVillagerData();
        VillagerProfession profession = villagerData.getProfession();
        if (profession.equals(VillagerProfession.NONE)) {
            return EntityType.VILLAGER.getDescription().copy();
        } else {
            return getTypeName();
        }
    }

    public Component getAdvancedName() {
        return Component.translatable("tooltip.easy_villagers.villager_profession",
                getName().copy(),
                Component.translatable("merchant.level." + getVillagerData().getLevel())
        ).withStyle(ChatFormatting.GRAY);
    }

    /**
     * Called when the player completes a trade (takes the result item).
     * Vanilla's rewardTradeXp spawns XP orbs at the entity's position, which for
     * our fake entity would be (0,0,0). We temporarily move the entity to the
     * player's position so the orbs appear at the right spot.
     */
    @Override
    public void notifyTrade(MerchantOffer offer) {
        Player tradingPlayer = getTradingPlayer();
        double origX = getX(), origY = getY(), origZ = getZ();
        if (tradingPlayer != null && !isRealWorldEntity) {
            // Only reposition fake (block-entity) villagers so XP orbs spawn at player
            setPos(tradingPlayer.getX(), tradingPlayer.getY(), tradingPlayer.getZ());
        }
        super.notifyTrade(offer); // adds villagerXp, spawns orbs, marks offer as used
        if (tradingPlayer != null && !isRealWorldEntity) {
            setPos(origX, origY, origZ);
        }
        tryLevelUp();
        if (onTradeCompleted != null) {
            onTradeCompleted.run();
        }
    }

    /** Immediately applies a level-up if the villager has accumulated enough XP. */
    private void tryLevelUp() {
        int level = getVillagerData().getLevel();
        if (level >= 5) return;
        int thresholdIndex = level - 1; // level 1 → index 0, level 4 → index 3
        if (thresholdIndex < LEVEL_XP_THRESHOLDS.length && getVillagerXp() >= LEVEL_XP_THRESHOLDS[thresholdIndex]) {
            setVillagerData(getVillagerData().setLevel(level + 1));
            updateTrades(); // adds trade offers unlocked at the new level
        }
    }

    /** Generates trades if the offer list is empty. */
    public void generateTrades() {
        if (getOffers().isEmpty()) {
            updateTrades();
        }
    }

    /** Clears all offers and regenerates them (reroll). */
    public void resetTrades() {
        getOffers().clear();
        updateTrades();
    }

    /** Restocks depleted offers (periodic restock). */
    public void restockOffers() {
        restock();
    }

    /** Sends merchant offers to the player via ClientboundMerchantOffersPacket. */
    public void sendOffers(Player player) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) return;
        serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket(
                serverPlayer.containerMenu.containerId,
                getOffers(),
                getVillagerData().getLevel(),
                getVillagerXp(),
                showProgressBar(),
                canRestock()
        ));
    }

}
