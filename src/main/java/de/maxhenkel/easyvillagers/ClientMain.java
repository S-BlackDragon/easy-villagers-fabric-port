package de.maxhenkel.easyvillagers;

import de.maxhenkel.easyvillagers.blocks.ModBlocks;
import de.maxhenkel.easyvillagers.blocks.tileentity.ModTileEntities;
import de.maxhenkel.easyvillagers.blocks.tileentity.render.*;
import de.maxhenkel.easyvillagers.gui.*;
import de.maxhenkel.easyvillagers.items.ModItems;
import de.maxhenkel.easyvillagers.items.render.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.RenderType;

public class ClientMain implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Block render layers
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.TRADER,     RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.AUTO_TRADER, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.FARMER,     RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.BREEDER,    RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.CONVERTER,  RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.IRON_FARM,  RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.INCUBATOR,  RenderType.cutout());

        // Block entity renderers
        BlockEntityRendererRegistry.register(ModTileEntities.TRADER,     TraderRenderer::new);
        BlockEntityRendererRegistry.register(ModTileEntities.AUTO_TRADER, AutoTraderRenderer::new);
        BlockEntityRendererRegistry.register(ModTileEntities.FARMER,     FarmerRenderer::new);
        BlockEntityRendererRegistry.register(ModTileEntities.BREEDER,    BreederRenderer::new);
        BlockEntityRendererRegistry.register(ModTileEntities.CONVERTER,  ConverterRenderer::new);
        BlockEntityRendererRegistry.register(ModTileEntities.IRON_FARM,  IronFarmRenderer::new);
        BlockEntityRendererRegistry.register(ModTileEntities.INCUBATOR,  IncubatorRenderer::new);

        // Item renderers (builtin/entity)
        BuiltinItemRendererRegistry.INSTANCE.register(ModItems.VILLAGER,    new VillagerItemRenderer());
        BuiltinItemRendererRegistry.INSTANCE.register(ModItems.TRADER,      new TraderItemRenderer());
        BuiltinItemRendererRegistry.INSTANCE.register(ModItems.AUTO_TRADER, new AutoTraderItemRenderer());
        BuiltinItemRendererRegistry.INSTANCE.register(ModItems.FARMER,      new FarmerItemRenderer());
        BuiltinItemRendererRegistry.INSTANCE.register(ModItems.BREEDER,     new BreederItemRenderer());

        // GUI screens
        //noinspection unchecked,rawtypes
        MenuScreens.<net.minecraft.world.inventory.MerchantMenu, TraderScreen>register(
                (net.minecraft.world.inventory.MenuType<? extends net.minecraft.world.inventory.MerchantMenu>)
                        (net.minecraft.world.inventory.MenuType<?>) ModMenuTypes.TRADER,
                (menu, inv, title) -> new TraderScreen((TraderMenu) menu, inv, title)
        );
        MenuScreens.register(ModMenuTypes.AUTO_TRADER, AutoTraderScreen::new);
        MenuScreens.register(ModMenuTypes.INCUBATOR,   IncubatorScreen::new);
        MenuScreens.register(ModMenuTypes.CONVERTER,   ConverterScreen::new);
        MenuScreens.register(ModMenuTypes.FARMER,      FarmerScreen::new);
        MenuScreens.register(ModMenuTypes.BREEDER,     BreederScreen::new);
        MenuScreens.register(ModMenuTypes.IRON_FARM,   IronFarmScreen::new);
    }
}
