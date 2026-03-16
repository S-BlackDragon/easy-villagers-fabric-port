package de.maxhenkel.easyvillagers.items;

import de.maxhenkel.easyvillagers.Main;
import de.maxhenkel.easyvillagers.blocks.ModBlocks;
import de.maxhenkel.easyvillagers.datacomponents.VillagerBlockEntityData;
import de.maxhenkel.easyvillagers.datacomponents.VillagerData;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public class ModItems {

    public static final VillagerItem VILLAGER = new VillagerItem();
    public static final ZombieVillagerItem ZOMBIE_VILLAGER = new ZombieVillagerItem();
    public static final BlockItem TRADER = new BlockItem(ModBlocks.TRADER, new Item.Properties());
    public static final BlockItem AUTO_TRADER = new BlockItem(ModBlocks.AUTO_TRADER, new Item.Properties());
    public static final BlockItem FARMER = new BlockItem(ModBlocks.FARMER, new Item.Properties());
    public static final BlockItem BREEDER = new BlockItem(ModBlocks.BREEDER, new Item.Properties());
    public static final BlockItem CONVERTER = new BlockItem(ModBlocks.CONVERTER, new Item.Properties());
    public static final BlockItem IRON_FARM = new BlockItem(ModBlocks.IRON_FARM, new Item.Properties());
    public static final BlockItem INCUBATOR = new BlockItem(ModBlocks.INCUBATOR, new Item.Properties());
    public static DataComponentType<VillagerData> VILLAGER_DATA_COMPONENT;
    public static DataComponentType<VillagerBlockEntityData> BLOCK_ENTITY_DATA_COMPONENT;

    public static void init() {
        Registry.register(BuiltInRegistries.ITEM, id("villager"), VILLAGER);
        Registry.register(BuiltInRegistries.ITEM, id("zombie_villager"), ZOMBIE_VILLAGER);
        Registry.register(BuiltInRegistries.ITEM, id("trader"), TRADER);
        Registry.register(BuiltInRegistries.ITEM, id("auto_trader"), AUTO_TRADER);
        Registry.register(BuiltInRegistries.ITEM, id("farmer"), FARMER);
        Registry.register(BuiltInRegistries.ITEM, id("breeder"), BREEDER);
        Registry.register(BuiltInRegistries.ITEM, id("converter"), CONVERTER);
        Registry.register(BuiltInRegistries.ITEM, id("iron_farm"), IRON_FARM);
        Registry.register(BuiltInRegistries.ITEM, id("incubator"), INCUBATOR);
        VILLAGER_DATA_COMPONENT = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id("villager"),
                DataComponentType.<VillagerData>builder()
                        .persistent(VillagerData.CODEC)
                        .networkSynchronized(VillagerData.STREAM_CODEC)
                        .build());

        BLOCK_ENTITY_DATA_COMPONENT = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id("block_entity"),
                DataComponentType.<VillagerBlockEntityData>builder()
                        .networkSynchronized(VillagerBlockEntityData.STREAM_CODEC)
                        .build());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Main.MODID, path);
    }

}
