package de.maxhenkel.easyvillagers.loottable;

import de.maxhenkel.easyvillagers.Main;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;

public class ModLootTables {

    public static LootItemFunctionType<CopyBlockEntityData> COPY_BLOCK_ENTITY;

    public static void init() {
        COPY_BLOCK_ENTITY = Registry.register(
                BuiltInRegistries.LOOT_FUNCTION_TYPE,
                ResourceLocation.fromNamespaceAndPath(Main.MODID, "copy_block_entity"),
                new LootItemFunctionType<>(CopyBlockEntityData.CODEC)
        );
    }

}
