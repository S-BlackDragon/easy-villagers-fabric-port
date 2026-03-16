package de.maxhenkel.easyvillagers.blocks;

import de.maxhenkel.easyvillagers.blocks.tileentity.BreederTileentity;
import de.maxhenkel.easyvillagers.blocks.tileentity.TraderTileentityBase;
import de.maxhenkel.easyvillagers.datacomponents.VillagerData;
import de.maxhenkel.easyvillagers.gui.BreederMenu;
import de.maxhenkel.easyvillagers.items.VillagerItem;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
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


public class BreederBlock extends TraderBlockBase {

    public BreederBlock() { }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack heldItem, BlockState state, Level level, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {
        BlockEntity tileEntity = level.getBlockEntity(pos);
        if (!(tileEntity instanceof BreederTileentity breeder)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (heldItem.getItem() instanceof VillagerItem) {
            // Breeder only accepts adult villagers
            if (VillagerData.isBaby(heldItem)) {
                if (!level.isClientSide) playVillagerSound(level, pos, SoundEvents.VILLAGER_NO, 1.6F);
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
            if (!breeder.hasVillager1()) {
                if (!level.isClientSide) {
                    breeder.setVillager1(heldItem.copy());
                    heldItem.consume(1, player);
                    playVillagerSound(level, pos, SoundEvents.VILLAGER_CELEBRATE, 0.9F);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            } else if (!breeder.hasVillager2()) {
                if (!level.isClientSide) {
                    breeder.setVillager2(heldItem.copy());
                    heldItem.consume(1, player);
                    playVillagerSound(level, pos, SoundEvents.VILLAGER_CELEBRATE, 0.9F);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
            // Both slots full: fall through to open GUI
        } else if (player.isShiftKeyDown()) {
            if (breeder.hasVillager2()) {
                if (level.isClientSide) { breeder.setVillager2(ItemStack.EMPTY); }
                else {
                    ItemStack stack = breeder.removeVillager2();
                    giveOrDrop(player, level, state, pos, heldItem, handIn, stack);
                    playVillagerSound(level, pos, SoundEvents.VILLAGER_CELEBRATE, VillagerData.isBaby(stack) ? 1.6F : 0.9F);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            } else if (breeder.hasVillager1()) {
                if (level.isClientSide) { breeder.setVillager1(ItemStack.EMPTY); }
                else {
                    ItemStack stack = breeder.removeVillager1();
                    giveOrDrop(player, level, state, pos, heldItem, handIn, stack);
                    playVillagerSound(level, pos, SoundEvents.VILLAGER_CELEBRATE, VillagerData.isBaby(stack) ? 1.6F : 0.9F);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
            // Nothing to remove: fall through to open GUI
        }

        // Open GUI
        if (!level.isClientSide) {
            player.openMenu(new ExtendedScreenHandlerFactory<BlockPos>() {
                @Override
                public BlockPos getScreenOpeningData(ServerPlayer p) { return pos; }

                @Override
                public Component getDisplayName() {
                    return Component.translatable("block.easy_villagers.breeder");
                }

                @Override
                public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                    return new BreederMenu(id, inv, breeder, pos);
                }
            });
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private void giveOrDrop(Player player, Level level, BlockState state, BlockPos pos, ItemStack heldItem, InteractionHand handIn, ItemStack toGive) {
        if (heldItem.isEmpty()) {
            player.setItemInHand(handIn, toGive);
        } else if (!player.getInventory().add(toGive)) {
            Direction direction = state.getValue(FACING);
            Containers.dropItemStack(level, direction.getStepX() + pos.getX() + 0.5D, pos.getY() + 0.5D, direction.getStepZ() + pos.getZ() + 0.5D, toGive);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof BreederTileentity breeder) {
                if (breeder.hasVillager1()) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, breeder.getVillager1());
                if (breeder.hasVillager2()) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, breeder.getVillager2());
                for (int i = 2; i <= 9; i++) {
                    Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, breeder.getItem(i));
                }
            }
        }
        // Skip TraderBlockBase.onRemove (it handles VillagerTileentity which Breeder doesn't extend)
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return (lvl, p, s, te) -> {
            if (te instanceof BreederTileentity breeder) BreederTileentity.serverTick(lvl, p, s, breeder);
        };
    }

    @Override
    protected boolean openGUI(TraderTileentityBase trader, Player player, Level level, BlockPos pos) {
        return false; // GUI is opened directly in useItemOn
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new BreederTileentity(blockPos, blockState);
    }
}
