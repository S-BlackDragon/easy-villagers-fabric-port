package de.maxhenkel.easyvillagers.blocks;

import de.maxhenkel.easyvillagers.blocks.tileentity.AutoTraderTileentity;
import de.maxhenkel.easyvillagers.blocks.tileentity.ModTileEntities;
import de.maxhenkel.easyvillagers.blocks.tileentity.TraderTileentityBase;
import de.maxhenkel.easyvillagers.entity.EasyVillagerEntity;
import de.maxhenkel.easyvillagers.gui.AutoTraderMenu;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class AutoTraderBlock extends TraderBlockBase {

    public AutoTraderBlock() {
    }

    @Override
    protected boolean openGUI(TraderTileentityBase trader, Player player, Level level, BlockPos pos) {
        if (level.isClientSide) return true;
        if (!(trader instanceof AutoTraderTileentity at)) return false;
        if (!at.hasVillager()) return false;

        EasyVillagerEntity entity = at.getVillagerEntity();
        if (entity == null) return false;
        if (entity.isTrading()) return false;

        if (player instanceof ServerPlayer serverPlayer) {
            entity.setTradingPlayer(player);
            entity.generateTrades();

            entity.setOnTradeCompleted(() -> {
                at.saveVillagerEntity();
                at.setChanged();
                if (entity.getTradingPlayer() instanceof ServerPlayer sp) {
                    entity.sendOffers(sp);
                }
            });

            serverPlayer.openMenu(new ExtendedScreenHandlerFactory<BlockPos>() {
                @Override
                public BlockPos getScreenOpeningData(ServerPlayer p) { return pos; }

                @Override
                public Component getDisplayName() {
                    return entity.getAdvancedName();
                }

                @Override
                public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                    return new AutoTraderMenu(id, inv, entity, at, pos);
                }
            });

            entity.sendOffers(serverPlayer);
        }
        return true;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AutoTraderTileentity at) {
                double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
                for (ItemStack stack : at.getInputSlots())
                    if (!stack.isEmpty()) Containers.dropItemStack(level, x, y, z, stack);
                for (ItemStack stack : at.getOutputSlots())
                    if (!stack.isEmpty()) Containers.dropItemStack(level, x, y, z, stack);

            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        if (type != ModTileEntities.AUTO_TRADER) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<AutoTraderTileentity>) AutoTraderTileentity::serverTick;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new AutoTraderTileentity(blockPos, blockState);
    }
}
