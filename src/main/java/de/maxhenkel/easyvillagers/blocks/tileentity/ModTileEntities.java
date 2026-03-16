package de.maxhenkel.easyvillagers.blocks.tileentity;

import de.maxhenkel.easyvillagers.Main;
import de.maxhenkel.easyvillagers.blocks.ModBlocks;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModTileEntities {

    public static BlockEntityType<TraderTileentity> TRADER;
    public static BlockEntityType<AutoTraderTileentity> AUTO_TRADER;
    public static BlockEntityType<FarmerTileentity> FARMER;
    public static BlockEntityType<BreederTileentity> BREEDER;
    public static BlockEntityType<ConverterTileentity> CONVERTER;
    public static BlockEntityType<IronFarmTileentity> IRON_FARM;
    public static BlockEntityType<IncubatorTileentity> INCUBATOR;
    public static void init() {
        TRADER = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("trader"),
                BlockEntityType.Builder.of(TraderTileentity::new, ModBlocks.TRADER).build(null));
        AUTO_TRADER = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("auto_trader"),
                BlockEntityType.Builder.of(AutoTraderTileentity::new, ModBlocks.AUTO_TRADER).build(null));
        FARMER = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("farmer"),
                BlockEntityType.Builder.of(FarmerTileentity::new, ModBlocks.FARMER).build(null));
        BREEDER = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("breeder"),
                BlockEntityType.Builder.of(BreederTileentity::new, ModBlocks.BREEDER).build(null));
        CONVERTER = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("converter"),
                BlockEntityType.Builder.of(ConverterTileentity::new, ModBlocks.CONVERTER).build(null));
        IRON_FARM = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("iron_farm"),
                BlockEntityType.Builder.of(IronFarmTileentity::new, ModBlocks.IRON_FARM).build(null));
        INCUBATOR = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("incubator"),
                BlockEntityType.Builder.of(IncubatorTileentity::new, ModBlocks.INCUBATOR).build(null));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Main.MODID, path);
    }

}
