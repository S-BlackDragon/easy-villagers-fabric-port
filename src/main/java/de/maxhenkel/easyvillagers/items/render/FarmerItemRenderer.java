package de.maxhenkel.easyvillagers.items.render;

import de.maxhenkel.easyvillagers.blocks.ModBlocks;
import de.maxhenkel.easyvillagers.blocks.tileentity.FarmerTileentity;
import de.maxhenkel.easyvillagers.blocks.tileentity.render.FarmerRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;

@Environment(EnvType.CLIENT)
public class FarmerItemRenderer extends BlockItemRendererBase<FarmerRenderer, FarmerTileentity> {
    public FarmerItemRenderer() {
        super(FarmerRenderer::new, () -> new FarmerTileentity(BlockPos.ZERO, ModBlocks.FARMER.defaultBlockState()));
    }
}
