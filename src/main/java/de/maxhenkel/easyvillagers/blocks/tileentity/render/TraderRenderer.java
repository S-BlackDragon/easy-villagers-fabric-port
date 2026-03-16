package de.maxhenkel.easyvillagers.blocks.tileentity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.maxhenkel.easyvillagers.blocks.TraderBlock;
import de.maxhenkel.easyvillagers.blocks.tileentity.TraderTileentity;
import de.maxhenkel.easyvillagers.blocks.tileentity.TraderTileentityBase;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class TraderRenderer extends VillagerRendererBase<TraderTileentity> {

    private static final Minecraft mc = Minecraft.getInstance();

    // Simple HashMap replaces corelib CachedMap
    private static final Map<Block, BlockState> blockStateCache = new HashMap<>();

    public TraderRenderer(BlockEntityRendererProvider.Context renderer) {
        super(renderer);
    }

    @Override
    public void render(TraderTileentity trader, float partialTicks, PoseStack matrixStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        super.render(trader, partialTicks, matrixStack, buffer, combinedLight, combinedOverlay);
        renderTraderBase(getVillagerRenderer(), trader, partialTicks, matrixStack, buffer, combinedLight, combinedOverlay);
    }

    public static void renderTraderBase(VillagerRenderer renderer, TraderTileentityBase trader, float partialTicks, PoseStack matrixStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        matrixStack.pushPose();
        Direction direction = Direction.SOUTH;
        if (!trader.isFakeWorld()) {
            direction = trader.getBlockState().getValue(TraderBlock.FACING);
        }

        if (trader.getVillagerEntity() != null) {
            matrixStack.pushPose();
            matrixStack.translate(0.5D, 1D / 16D, 0.5D);
            matrixStack.mulPose(Axis.YP.rotationDegrees(-direction.toYRot()));
            matrixStack.translate(0D, 0D, -4D / 16D);
            matrixStack.scale(0.45F, 0.45F, 0.45F);
            renderer.render(trader.getVillagerEntity(), 0F, 1F, matrixStack, buffer, combinedLight);
            matrixStack.popPose();
        }

        if (trader.hasWorkstation()) {
            matrixStack.pushPose();
            matrixStack.translate(0.5D, 1D / 16D, 0.5D);
            matrixStack.mulPose(Axis.YP.rotationDegrees(-direction.toYRot()));
            matrixStack.translate(0D, 0D, 2D / 16D);
            matrixStack.translate(-0.5D, 0D, -0.5D);
            matrixStack.scale(0.45F, 0.45F, 0.45F);
            matrixStack.translate(0.5D / 0.45D - 0.5D, 0D, 0.5D / 0.45D - 0.5D);

            BlockState workstation = getState(trader.getWorkstation());
            getTransforms(workstation).accept(matrixStack);
            renderBlock(workstation, matrixStack, buffer, combinedLight, combinedOverlay);

            BlockState topBlock = getTopBlock(workstation);
            if (!topBlock.isAir()) {
                matrixStack.translate(0D, 1D, 0D);
                renderBlock(topBlock, matrixStack, buffer, combinedLight, combinedOverlay);
            }
            matrixStack.popPose();
        }

        matrixStack.popPose();
    }

    public static void renderBlock(BlockState state, PoseStack matrixStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        mc.getBlockRenderer().renderSingleBlock(state, matrixStack, buffer, combinedLight, combinedOverlay);
    }

    public static BlockState getState(Block block) {
        return blockStateCache.computeIfAbsent(block, TraderRenderer::getFittingState);
    }

    protected static BlockState getFittingState(Block block) {
        if (block == Blocks.GRINDSTONE) {
            return block.defaultBlockState().setValue(GrindstoneBlock.FACE, AttachFace.FLOOR);
        }
        return block.defaultBlockState();
    }

    public static final Map<ResourceLocation, Consumer<PoseStack>> TRANSFORMS = new HashMap<>();
    private static final Map<Block, Consumer<PoseStack>> TRANSFORMS_CACHE = new HashMap<>();

    public static final Map<ResourceLocation, ResourceLocation> TOP_BLOCKS = new HashMap<>();
    private static final Map<Block, BlockState> TOP_BLOCK_CACHE = new HashMap<>();

    static {
        Consumer<PoseStack> immersiveEngineering = stack -> stack.translate(-0.5D, 0D, 0D);
        TRANSFORMS.put(ResourceLocation.fromNamespaceAndPath("immersiveengineering", "workbench"), immersiveEngineering);
        TRANSFORMS.put(ResourceLocation.fromNamespaceAndPath("immersiveengineering", "circuit_table"), immersiveEngineering);
        TOP_BLOCKS.put(ResourceLocation.fromNamespaceAndPath("car", "gas_station"), ResourceLocation.fromNamespaceAndPath("car", "gas_station_top"));
    }

    protected static Consumer<PoseStack> getTransforms(BlockState block) {
        return TRANSFORMS_CACHE.computeIfAbsent(block.getBlock(), b -> {
            Consumer<PoseStack> t = TRANSFORMS.get(BuiltInRegistries.BLOCK.getKey(b));
            return t != null ? t : stack -> {};
        });
    }

    protected static BlockState getTopBlock(BlockState bottom) {
        return TOP_BLOCK_CACHE.computeIfAbsent(bottom.getBlock(), b -> {
            ResourceLocation rl = TOP_BLOCKS.get(BuiltInRegistries.BLOCK.getKey(b));
            if (rl == null || !BuiltInRegistries.BLOCK.containsKey(rl)) return Blocks.AIR.defaultBlockState();
            return BuiltInRegistries.BLOCK.get(rl).defaultBlockState();
        });
    }
}
