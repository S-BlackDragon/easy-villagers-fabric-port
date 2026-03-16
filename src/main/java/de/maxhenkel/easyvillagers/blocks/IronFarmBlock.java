package de.maxhenkel.easyvillagers.blocks;

import de.maxhenkel.easyvillagers.blocks.tileentity.IronFarmTileentity;
import de.maxhenkel.easyvillagers.blocks.tileentity.TraderTileentityBase;
import de.maxhenkel.easyvillagers.gui.IronFarmMenu;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;


public class IronFarmBlock extends TraderBlockBase {

    public IronFarmBlock() { }

    @Override
    protected boolean openGUI(TraderTileentityBase trader, Player player, Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof IronFarmTileentity ironFarm)) return false;
        if (level.isClientSide()) return true;

        player.openMenu(new ExtendedScreenHandlerFactory<BlockPos>() {
            @Override
            public BlockPos getScreenOpeningData(ServerPlayer p) { return pos; }

            @Override
            public Component getDisplayName() {
                return Component.translatable("block.easy_villagers.iron_farm");
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                return new IronFarmMenu(id, inv, ironFarm, pos);
            }
        });
        return true;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof IronFarmTileentity ironFarm) {
                // Drop output items and clear slots so copy_block_entity doesn't duplicate them
                for (int i = 1; i <= 4; i++) {
                    ItemStack item = ironFarm.getItem(i);
                    if (!item.isEmpty()) {
                        Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, item);
                        ironFarm.setItem(i, ItemStack.EMPTY);
                    }
                }
                // Villager stays inside the dropped block item (via copy_block_entity)
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return (lvl, p, s, te) -> {
            if (te instanceof IronFarmTileentity ironFarm) IronFarmTileentity.serverTick(lvl, p, s, ironFarm);
        };
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new IronFarmTileentity(pos, state);
    }
}
