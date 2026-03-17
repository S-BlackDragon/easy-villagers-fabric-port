package de.maxhenkel.easyvillagers.blocks.tileentity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import de.maxhenkel.easyvillagers.blocks.tileentity.FakeWorldTileentity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.VillagerRenderer;

@Environment(EnvType.CLIENT)
public class VillagerRendererBase<T extends FakeWorldTileentity> extends BlockRendererBase<T> {

    // Strong reference — a WeakReference would be GC'd between frames, causing
    // the renderer to be recreated in an inconsistent state each time.
    protected VillagerRenderer villagerRendererCache;

    public VillagerRendererBase(BlockEntityRendererProvider.Context renderer) {
        super(renderer);
    }

    @Override
    public void render(T tileEntity, float partialTicks, PoseStack matrixStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        super.render(tileEntity, partialTicks, matrixStack, buffer, combinedLight, combinedOverlay);
    }

    protected VillagerRenderer getVillagerRenderer() {
        if (villagerRendererCache == null) {
            villagerRendererCache = new VillagerRenderer(createEntityRenderer());
        }
        return villagerRendererCache;
    }
}
