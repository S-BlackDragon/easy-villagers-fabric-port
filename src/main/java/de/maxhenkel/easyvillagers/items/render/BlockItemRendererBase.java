package de.maxhenkel.easyvillagers.items.render;

import com.mojang.blaze3d.vertex.PoseStack;
import de.maxhenkel.easyvillagers.blocks.tileentity.FakeWorldTileentity;
import de.maxhenkel.easyvillagers.datacomponents.VillagerBlockEntityData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.function.Function;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public class BlockItemRendererBase<T extends BlockEntityRenderer<U>, U extends FakeWorldTileentity>
        implements BuiltinItemRendererRegistry.DynamicItemRenderer {

    private final Function<BlockEntityRendererProvider.Context, T> rendererSupplier;
    private final Supplier<U> tileEntitySupplier;
    private T renderer;

    public BlockItemRendererBase(Function<BlockEntityRendererProvider.Context, T> rendererSupplier, Supplier<U> tileentitySupplier) {
        this.rendererSupplier = rendererSupplier;
        this.tileEntitySupplier = tileentitySupplier;
    }

    @Override
    public void render(ItemStack stack, ItemDisplayContext mode, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        if (renderer == null) {
            renderer = rendererSupplier.apply(new BlockEntityRendererProvider.Context(
                    mc.getBlockEntityRenderDispatcher(),
                    mc.getBlockRenderer(),
                    mc.getItemRenderer(),
                    mc.getEntityRenderDispatcher(),
                    mc.getEntityModels(),
                    mc.font
            ));
        }

        if (stack.getItem() instanceof BlockItem blockItem) {
            mc.getBlockRenderer().renderSingleBlock(blockItem.getBlock().defaultBlockState(), matrices, vertexConsumers, light, overlay);
        }

        U be = VillagerBlockEntityData.getAndStoreBlockEntity(stack, mc.level.registryAccess(), mc.level, tileEntitySupplier);
        renderer.render(be, 0F, matrices, vertexConsumers, light, overlay);
    }
}
