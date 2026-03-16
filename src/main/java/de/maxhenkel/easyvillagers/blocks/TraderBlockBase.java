package de.maxhenkel.easyvillagers.blocks;

import de.maxhenkel.easyvillagers.blocks.tileentity.TraderTileentityBase;
import de.maxhenkel.easyvillagers.blocks.tileentity.VillagerTileentity;
import de.maxhenkel.easyvillagers.datacomponents.VillagerData;
import de.maxhenkel.easyvillagers.items.VillagerItem;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;


public abstract class TraderBlockBase extends VillagerBlockBase implements EntityBlock {

    public TraderBlockBase() {
        super(Properties.of().mapColor(MapColor.METAL).strength(2.5F).sound(SoundType.METAL).noOcclusion());
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack heldItem, BlockState state, Level level, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {
        BlockEntity tileEntity = level.getBlockEntity(pos);
        if (!(tileEntity instanceof VillagerTileentity trader)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        TraderTileentityBase traderBase = tileEntity instanceof TraderTileentityBase t ? t : null;

        if (!trader.hasVillager() && heldItem.getItem() instanceof VillagerItem) {
            if (!isValidVillager(heldItem)) {
                if (!level.isClientSide) {
                    playVillagerSound(level, pos, SoundEvents.VILLAGER_NO, getRejectPitch(heldItem));
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
            if (!level.isClientSide) {
                float celebratePitch = VillagerData.isBaby(heldItem) ? 1.6F : 0.9F;
                trader.setVillager(heldItem.copy());
                heldItem.consume(1, player);
                if (traderBase != null && traderBase.hasWorkstation()) {
                    Villager villagerEntity = traderBase.getVillagerEntity();
                    if (villagerEntity != null) {
                        playWorkstationSound(level, pos, traderBase);
                    }
                } else {
                    playVillagerSound(level, pos, SoundEvents.VILLAGER_CELEBRATE, celebratePitch);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        } else if (traderBase != null && !traderBase.hasWorkstation() && heldItem.getItem() instanceof BlockItem blockItem && traderBase.isValidBlock(blockItem.getBlock())) {
            if (!level.isClientSide) {
                traderBase.setWorkstation(blockItem.getBlock());
                heldItem.consume(1, player);
                Villager villagerEntity = traderBase.getVillagerEntity();
                if (villagerEntity != null) {
                    playWorkstationSound(level, pos, traderBase);
                }
                SoundType type = blockItem.getBlock().defaultBlockState().getSoundType();
                level.playSound(null, pos, type.getPlaceSound(), SoundSource.BLOCKS, type.getVolume(), type.getPitch());
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        } else if (player.isShiftKeyDown() && trader.hasVillager()) {
            if (level.isClientSide) {
                trader.setVillager(ItemStack.EMPTY);
            } else {
                ItemStack stack = trader.removeVillager();
                if (heldItem.isEmpty()) {
                    player.setItemInHand(handIn, stack);
                } else {
                    if (!player.getInventory().add(stack)) {
                        Direction direction = state.getValue(TraderBlockBase.FACING);
                        Containers.dropItemStack(level, direction.getStepX() + pos.getX() + 0.5D, pos.getY() + 0.5D, direction.getStepZ() + pos.getZ() + 0.5D, stack);
                    }
                }
                float celebratePitch = VillagerData.isBaby(stack) ? 1.6F : 0.9F;
                playVillagerSound(level, pos, SoundEvents.VILLAGER_CELEBRATE, celebratePitch);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        } else if (traderBase != null && player.isShiftKeyDown() && traderBase.hasWorkstation()) {
            if (level.isClientSide) {
                traderBase.setWorkstation(net.minecraft.world.level.block.Blocks.AIR);
            } else {
                ItemStack blockStack = new ItemStack(traderBase.removeWorkstation());
                if (heldItem.isEmpty()) {
                    player.setItemInHand(handIn, blockStack);
                } else {
                    if (!player.getInventory().add(blockStack)) {
                        Direction direction = state.getValue(TraderBlockBase.FACING);
                        Containers.dropItemStack(level, direction.getStepX() + pos.getX() + 0.5D, pos.getY() + 0.5D, direction.getStepZ() + pos.getZ() + 0.5D, blockStack);
                    }
                }
                if (traderBase.hasVillager()) {
                    playVillagerSound(level, pos, SoundEvents.VILLAGER_NO);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        } else if (traderBase != null && openGUI(traderBase, player, level, pos)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** Override to restrict which villager types are accepted. Default: adults only (no babies). */
    protected boolean isValidVillager(ItemStack villager) {
        return !VillagerData.isBaby(villager);
    }

    /** Pitch for the rejection sound. Override for blocks that are baby-themed. */
    protected float getRejectPitch(ItemStack villager) {
        return VillagerData.isBaby(villager) ? 1.6F : 0.9F;
    }

    protected abstract boolean openGUI(TraderTileentityBase trader, Player player, Level level, BlockPos pos);

    protected void playWorkstationSound(Level world, BlockPos pos, TraderTileentityBase trader) {
        Villager villagerEntity = trader.getVillagerEntity();
        if (villagerEntity != null) {
            if (trader.getWorkstationProfession().equals(villagerEntity.getVillagerData().getProfession())) {
                playVillagerSound(world, pos, trader.getWorkstationProfession().workSound());
            } else {
                playVillagerSound(world, pos, SoundEvents.VILLAGER_NO);
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof VillagerTileentity trader) {
                if (trader.hasVillager()) {
                    Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, trader.getVillager());
                }
                if (be instanceof TraderTileentityBase traderBase && traderBase.hasWorkstation()) {
                    ItemStack workstationStack = new ItemStack(traderBase.getWorkstation());
                    Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, workstationStack);
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // Phase 1: no ticking. Full tick logic added in Phase 2.
        return null;
    }

    @Nullable
    @Override
    public abstract BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState);

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter worldIn, BlockPos pos) {
        return 1F;
    }

}
