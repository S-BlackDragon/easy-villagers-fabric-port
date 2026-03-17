package de.maxhenkel.easyvillagers.items.render;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.maxhenkel.easyvillagers.datacomponents.VillagerData;
import de.maxhenkel.easyvillagers.entity.EasyVillagerEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
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
            // Mirror exactly what InventoryScreen.renderEntityInInventory() does in vanilla:
            // lighting → dispatcher.render() wrapped in runAsFancy → endBatch → restore
            Lighting.setupForEntityInInventory();
            EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
            dispatcher.setRenderShadow(false);
            MultiBufferSource.BufferSource renderBuffer = mc.renderBuffers().bufferSource();

            matrices.pushPose();
            matrices.translate(0.5D, yBase, 0.5D);
            if (baby) matrices.scale(0.6F, 0.6F, 0.6F);
            RenderSystem.runAsFancy(() ->
                    dispatcher.render(cacheVillager, 0D, 0D, 0D, 0F, 1F, matrices, renderBuffer, LightTexture.FULL_BRIGHT)
            );
            matrices.popPose();

            renderBuffer.endBatch();
            dispatcher.setRenderShadow(true);
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
