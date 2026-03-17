package de.maxhenkel.easyvillagers.items.render;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import de.maxhenkel.easyvillagers.datacomponents.VillagerData;
import de.maxhenkel.easyvillagers.entity.EasyVillagerEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
public class VillagerItemRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer {

    private VillagerRenderer renderer;

    @Override
    public void render(ItemStack stack, ItemDisplayContext mode, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        if (renderer == null) {
            EntityRendererProvider.Context ctx = new EntityRendererProvider.Context(
                    mc.getEntityRenderDispatcher(),
                    mc.getItemRenderer(),
                    mc.getBlockRenderer(),
                    mc.gameRenderer.itemInHandRenderer,
                    mc.getResourceManager(),
                    mc.getEntityModels(),
                    mc.font
            );
            renderer = new VillagerRenderer(ctx);
        }

        boolean baby = VillagerData.isBaby(stack);
        EasyVillagerEntity cacheVillager = VillagerData.getCacheVillager(stack, mc.level);
        double yBase = baby ? 0.05D : -0.1D;

        boolean isGui = mode == ItemDisplayContext.GUI || mode == ItemDisplayContext.FIXED;

        if (isGui) {
            // Identical pattern to InventoryScreen.renderEntityInInventory():
            // set entity lighting → render into the MAIN render buffer → flush → restore.
            // Using mc.renderBuffers().bufferSource() (not the passed vertexConsumers)
            // guarantees we always have a real BufferSource we can flush on demand.
            Lighting.setupForEntityInInventory();
            MultiBufferSource.BufferSource renderBuffer = mc.renderBuffers().bufferSource();

            matrices.pushPose();
            try {
                matrices.translate(0.5D, yBase, 0.5D);
                if (baby) matrices.scale(0.6F, 0.6F, 0.6F);
                renderer.render(cacheVillager, 0F, 1F, matrices, renderBuffer, LightTexture.FULL_BRIGHT);
            } finally {
                matrices.popPose();
            }

            renderBuffer.endBatch();
            Lighting.setupFor3DItems();
        } else {
            // In-hand / dropped / item-frame: use the provided buffer and world lighting
            matrices.pushPose();
            try {
                matrices.translate(0.5D, yBase, 0.5D);
                if (baby) matrices.scale(0.6F, 0.6F, 0.6F);
                renderer.render(cacheVillager, 0F, 1F, matrices, vertexConsumers, light);
            } finally {
                matrices.popPose();
            }
        }
    }
}
