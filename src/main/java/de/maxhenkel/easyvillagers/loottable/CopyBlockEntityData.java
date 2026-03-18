package de.maxhenkel.easyvillagers.loottable;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.maxhenkel.easyvillagers.blocks.tileentity.TraderTileentityBase;
import de.maxhenkel.easyvillagers.blocks.tileentity.VillagerTileentity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;

public class CopyBlockEntityData extends LootItemConditionalFunction {

    public static final MapCodec<CopyBlockEntityData> CODEC = RecordCodecBuilder.mapCodec(
            instance -> commonFields(instance).apply(instance, CopyBlockEntityData::new));

    protected CopyBlockEntityData(List<LootItemCondition> conditions) {
        super(conditions);
    }

    @Override
    public ItemStack run(ItemStack stack, LootContext context) {
        BlockEntity blockEntity = context.getParamOrNull(LootContextParams.BLOCK_ENTITY);
        if (blockEntity == null) {
            return stack;
        }
        // For villager-based blocks, only embed block entity data when there is
        // meaningful content (villager or workstation).  Empty blocks should stack
        // normally — adding block-entity NBT to an empty block prevents stacking.
        if (blockEntity instanceof VillagerTileentity vt) {
            boolean hasWorkstation = blockEntity instanceof TraderTileentityBase tb && tb.hasWorkstation();
            if (!vt.hasVillager() && !hasWorkstation) {
                return stack;
            }
        }
        stack.applyComponents(blockEntity.collectComponents());
        CompoundTag compoundtag = blockEntity.saveCustomAndMetadata(context.getLevel().registryAccess());
        BlockItem.setBlockEntityData(stack, blockEntity.getType(), compoundtag);
        return stack;
    }

    @Override
    public LootItemFunctionType<CopyBlockEntityData> getType() {
        return ModLootTables.COPY_BLOCK_ENTITY;
    }

}
