package de.maxhenkel.easyvillagers.items.render;

import com.mojang.blaze3d.vertex.PoseStack;
import de.maxhenkel.easyvillagers.datacomponents.VillagerData;
import de.maxhenkel.easyvillagers.entity.EasyVillagerEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.Minecraft;
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
        double yOffset = baby ? 0.15D : 0.0D;
        matrices.pushPose();
        matrices.translate(0.5D, yOffset, 0.5D);
        matrices.scale(1.0F, 1.0F, 1.0F);
        renderer.render(cacheVillager, 0F, 1F, matrices, vertexConsumers, light);
        matrices.popPose();
    }
}
