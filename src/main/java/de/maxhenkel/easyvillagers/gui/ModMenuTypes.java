package de.maxhenkel.easyvillagers.gui;

import de.maxhenkel.easyvillagers.Main;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;

public class ModMenuTypes {

    public static MenuType<TraderMenu> TRADER;
    public static MenuType<AutoTraderMenu> AUTO_TRADER;
    public static MenuType<IncubatorMenu> INCUBATOR;
    public static MenuType<ConverterMenu> CONVERTER;
    public static MenuType<FarmerMenu> FARMER;
    public static MenuType<BreederMenu> BREEDER;
    public static MenuType<IronFarmMenu> IRON_FARM;

    public static void init() {
        StreamCodec<RegistryFriendlyByteBuf, BlockPos> blockPosCodec = StreamCodec.of(
                (buf, pos) -> buf.writeBlockPos(pos),
                buf -> buf.readBlockPos()
        );

        TRADER = Registry.register(
                BuiltInRegistries.MENU,
                ResourceLocation.fromNamespaceAndPath(Main.MODID, "trader"),
                new ExtendedScreenHandlerType<>((id, inv, pos) -> new TraderMenu(id, inv, pos), blockPosCodec)
        );

        AUTO_TRADER = Registry.register(
                BuiltInRegistries.MENU,
                ResourceLocation.fromNamespaceAndPath(Main.MODID, "auto_trader"),
                new ExtendedScreenHandlerType<>((id, inv, pos) -> new AutoTraderMenu(id, inv, pos), blockPosCodec)
        );

        INCUBATOR = Registry.register(
                BuiltInRegistries.MENU,
                ResourceLocation.fromNamespaceAndPath(Main.MODID, "incubator"),
                new ExtendedScreenHandlerType<>((id, inv, pos) -> new IncubatorMenu(id, inv, pos), blockPosCodec)
        );

        CONVERTER = Registry.register(
                BuiltInRegistries.MENU,
                ResourceLocation.fromNamespaceAndPath(Main.MODID, "converter"),
                new ExtendedScreenHandlerType<>((id, inv, pos) -> new ConverterMenu(id, inv, pos), blockPosCodec)
        );

        FARMER = Registry.register(
                BuiltInRegistries.MENU,
                ResourceLocation.fromNamespaceAndPath(Main.MODID, "farmer"),
                new ExtendedScreenHandlerType<>((id, inv, pos) -> new FarmerMenu(id, inv, pos), blockPosCodec)
        );

        BREEDER = Registry.register(
                BuiltInRegistries.MENU,
                ResourceLocation.fromNamespaceAndPath(Main.MODID, "breeder"),
                new ExtendedScreenHandlerType<>((id, inv, pos) -> new BreederMenu(id, inv, pos), blockPosCodec)
        );

        IRON_FARM = Registry.register(
                BuiltInRegistries.MENU,
                ResourceLocation.fromNamespaceAndPath(Main.MODID, "iron_farm"),
                new ExtendedScreenHandlerType<>((id, inv, pos) -> new IronFarmMenu(id, inv, pos), blockPosCodec)
        );

    }

}
