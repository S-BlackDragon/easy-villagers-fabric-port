package de.maxhenkel.easyvillagers.items.render;

import com.mojang.blaze3d.vertex.PoseStack;
import de.maxhenkel.easyvillagers.datacomponents.VillagerData;
import de.maxhenkel.easyvillagers.entity.EasyVillagerEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.Lighting;
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

        boolean isGui = mode == ItemDisplayContext.GUI || mode == ItemDisplayContext.FIXED;

        if (isGui) {
            // Flush any pending vertices, then set up directional lights for entity rendering.
            // Without this, entities appear as dark silhouettes in GUI context.
            if (vertexConsumers instanceof MultiBufferSource.BufferSource bs) {
                bs.endBatch();
            }
            Lighting.setupForEntityInInventory();
        }

        double yBase = baby ? 0.05D : -0.1D;
        matrices.pushPose();
        try {
            matrices.translate(0.5D, yBase, 0.5D);
            if (baby) {
                matrices.scale(0.6F, 0.6F, 0.6F);
            }
            renderer.render(cacheVillager, 0F, 1F, matrices, vertexConsumers, LightTexture.FULL_BRIGHT);
        } finally {
            matrices.popPose();
        }

        if (isGui) {
            // Flush entity vertices, then restore the standard 3D item lighting.
            if (vertexConsumers instanceof MultiBufferSource.BufferSource bs) {
                bs.endBatch();
            }
            Lighting.setupFor3DItems();
        }
    }
}
