package de.maxhenkel.easyvillagers.blocks;

import de.maxhenkel.easyvillagers.Main;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public class ModBlocks {

    public static final TraderBlock TRADER = new TraderBlock();
    public static final AutoTraderBlock AUTO_TRADER = new AutoTraderBlock();
    public static final FarmerBlock FARMER = new FarmerBlock();
    public static final BreederBlock BREEDER = new BreederBlock();
    public static final ConverterBlock CONVERTER = new ConverterBlock();
    public static final IronFarmBlock IRON_FARM = new IronFarmBlock();
    public static final IncubatorBlock INCUBATOR = new IncubatorBlock();
    public static void init() {
        Registry.register(BuiltInRegistries.BLOCK, id("trader"), TRADER);
        Registry.register(BuiltInRegistries.BLOCK, id("auto_trader"), AUTO_TRADER);
        Registry.register(BuiltInRegistries.BLOCK, id("farmer"), FARMER);
        Registry.register(BuiltInRegistries.BLOCK, id("breeder"), BREEDER);
        Registry.register(BuiltInRegistries.BLOCK, id("converter"), CONVERTER);
        Registry.register(BuiltInRegistries.BLOCK, id("iron_farm"), IRON_FARM);
        Registry.register(BuiltInRegistries.BLOCK, id("incubator"), INCUBATOR);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Main.MODID, path);
    }

}
