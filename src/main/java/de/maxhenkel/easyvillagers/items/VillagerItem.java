package de.maxhenkel.easyvillagers.items;

import de.maxhenkel.easyvillagers.Main;
import de.maxhenkel.easyvillagers.blocks.VillagerBlockBase;
import de.maxhenkel.easyvillagers.datacomponents.VillagerData;
import de.maxhenkel.easyvillagers.entity.EasyVillagerEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class VillagerItem extends Item {

    public VillagerItem() {
        super(new Item.Properties().stacksTo(1));

        DispenserBlock.registerBehavior(this, (source, stack) -> {
            Direction direction = source.state().getValue(DispenserBlock.FACING);
            BlockPos blockpos = source.pos().relative(direction);
            Level world = source.level();
            EasyVillagerEntity villager = VillagerData.getOrCreate(stack).createEasyVillager(world, stack);
            villager.markAsRealWorldEntity();
            villager.absMoveTo(blockpos.getX() + 0.5D, blockpos.getY(), blockpos.getZ() + 0.5D, direction.toYRot(), 0F);
            world.addFreshEntity(villager);
            stack.shrink(1);
            return stack;
        });
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        if (world.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        ItemStack itemstack = context.getItemInHand();
        BlockPos blockpos = context.getClickedPos();
        Direction direction = context.getClickedFace();
        BlockState blockstate = world.getBlockState(blockpos);

        if (!blockstate.getCollisionShape(world, blockpos).isEmpty()) {
            blockpos = blockpos.relative(direction);
        }

        EasyVillagerEntity villager = VillagerData.getOrCreate(itemstack).createEasyVillager(world, itemstack);
        villager.markAsRealWorldEntity();
        villager.setPos(blockpos.getX() + 0.5D, blockpos.getY(), blockpos.getZ() + 0.5);

        if (world.addFreshEntity(villager)) {
            itemstack.shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, context, tooltip, flagIn);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public Component getName(ItemStack stack) {
        Level world = Minecraft.getInstance().level;
        if (world == null) {
            return super.getName(stack);
        }
        EasyVillagerEntity villager = VillagerData.getCacheVillager(stack, world);
        if (!villager.hasCustomName() && villager.isBaby()) {
            return Component.translatable("item.easy_villagers.baby_villager");
        }
        return villager.getDisplayName();
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int itemSlot, boolean isSelected) {
        super.inventoryTick(stack, world, entity, itemSlot, isSelected);
    }

    public static ItemStack fromEntity(Villager entity) {
        ItemStack stack = new ItemStack(ModItems.VILLAGER);
        net.minecraft.nbt.CompoundTag nbt = new net.minecraft.nbt.CompoundTag();
        entity.addAdditionalSaveData(nbt);
        stack.set(ModItems.VILLAGER_DATA_COMPONENT, VillagerData.of(nbt));
        return stack;
    }

    public static ItemStack createBabyVillager() {
        ItemStack babyVillager = new ItemStack(ModItems.VILLAGER);
        CompoundTag compound = new CompoundTag();
        compound.putInt("Age", -24000);
        VillagerData data = VillagerData.of(compound);
        babyVillager.set(ModItems.VILLAGER_DATA_COMPONENT, data);
        return babyVillager;
    }

}
