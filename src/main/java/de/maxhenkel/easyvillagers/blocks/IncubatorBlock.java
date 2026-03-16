package de.maxhenkel.easyvillagers.blocks;

import de.maxhenkel.easyvillagers.blocks.tileentity.IncubatorTileentity;
import de.maxhenkel.easyvillagers.blocks.tileentity.ModTileEntities;
import de.maxhenkel.easyvillagers.blocks.tileentity.TraderTileentityBase;
import de.maxhenkel.easyvillagers.datacomponents.VillagerData;
import de.maxhenkel.easyvillagers.gui.IncubatorMenu;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;


public class IncubatorBlock extends TraderBlockBase {

    public IncubatorBlock() {
    }

    @Override
    protected boolean isValidVillager(ItemStack villager) {
        return VillagerData.isBaby(villager);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack heldItem, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return ItemInteractionResult.sidedSuccess(true);

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof IncubatorTileentity incubator)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new ExtendedScreenHandlerFactory<BlockPos>() {
                @Override
                public BlockPos getScreenOpeningData(ServerPlayer player) { return pos; }

                @Override
                public Component getDisplayName() {
                    return Component.translatable("container.easy_villagers.incubator");
                }

                @Override
                public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player p) {
                    return new IncubatorMenu(syncId, inv, incubator, pos);
                }
            });
        }
        return ItemInteractionResult.sidedSuccess(false);
    }

    @Override
    protected boolean openGUI(TraderTileentityBase trader, Player player, Level level, BlockPos pos) {
        return false; // Not used — useItemOn is fully overridden above
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof IncubatorTileentity incubator) {
                if (!incubator.getInputVillager().isEmpty()) {
                    Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, incubator.getInputVillager());
                }
                if (!incubator.getOutputVillager().isEmpty()) {
                    Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, incubator.getOutputVillager());
                }
            }
        }
        // Call Block.onRemove (skip TraderBlockBase — it handles VillagerTileentity which doesn't apply here)
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        if (type != ModTileEntities.INCUBATOR) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<IncubatorTileentity>) IncubatorTileentity::serverTick;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new IncubatorTileentity(blockPos, blockState);
    }

}
