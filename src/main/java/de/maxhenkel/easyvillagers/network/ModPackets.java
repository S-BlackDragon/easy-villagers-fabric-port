package de.maxhenkel.easyvillagers.network;

import de.maxhenkel.easyvillagers.blocks.tileentity.TraderTileentityBase;
import de.maxhenkel.easyvillagers.entity.EasyVillagerEntity;
import de.maxhenkel.easyvillagers.gui.AutoTraderMenu;
import de.maxhenkel.easyvillagers.gui.TraderMenu;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class ModPackets {

    public static void init() {
        // Register C2S packet type (must happen before any play session)
        PayloadTypeRegistry.playC2S().register(C2SRerollPacket.TYPE, C2SRerollPacket.CODEC);

        // Register server-side handler
        ServerPlayNetworking.registerGlobalReceiver(C2SRerollPacket.TYPE, (payload, context) -> {
            BlockPos pos = payload.pos();
            context.server().execute(() -> handleReroll(context.player(), pos));
        });
    }

    private static void handleReroll(ServerPlayer player, BlockPos pos) {
        if (!(player.containerMenu instanceof TraderMenu) && !(player.containerMenu instanceof AutoTraderMenu)) return;

        Level level = player.level();
        if (!(level.getBlockEntity(pos) instanceof TraderTileentityBase trader)) return;

        // Basic security: player must be within 8 blocks
        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64) return;

        EasyVillagerEntity villagerEntity = trader.getVillagerEntity();
        if (villagerEntity == null) return;

        // Regenerate all trade offers
        villagerEntity.resetTrades();
        trader.saveVillagerEntity();
        trader.setChanged();

        // Send updated offers back to the client
        player.connection.send(new ClientboundMerchantOffersPacket(
                player.containerMenu.containerId,
                villagerEntity.getOffers(),
                villagerEntity.getVillagerData().getLevel(),
                villagerEntity.getVillagerXp(),
                villagerEntity.showProgressBar(),
                villagerEntity.canRestock()
        ));
    }

}
