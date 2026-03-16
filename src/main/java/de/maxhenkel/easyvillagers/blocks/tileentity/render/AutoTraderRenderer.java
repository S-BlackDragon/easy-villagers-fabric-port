package de.maxhenkel.easyvillagers.blocks.tileentity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import de.maxhenkel.easyvillagers.blocks.tileentity.AutoTraderTileentity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

@Environment(EnvType.CLIENT)
public class AutoTraderRenderer extends VillagerRendererBase<AutoTraderTileentity> {

    public AutoTraderRenderer(BlockEntityRendererProvider.Context renderer) {
        super(renderer);
    }

    @Override
    public void render(AutoTraderTileentity trader, float partialTicks, PoseStack matrixStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        super.render(trader, partialTicks, matrixStack, buffer, combinedLight, combinedOverlay);
        TraderRenderer.renderTraderBase(getVillagerRenderer(), trader, partialTicks, matrixStack, buffer, combinedLight, combinedOverlay);
    }
}
