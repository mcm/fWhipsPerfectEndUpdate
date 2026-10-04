package io.mcmaster.someonesperfectend.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.mcmaster.someonesperfectend.SomeonesPerfectEnd;
import io.mcmaster.someonesperfectend.block.EnderblossomBlock;
import io.mcmaster.someonesperfectend.block.EnderblossomBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Draws a wall Enderblossom's block model, turned by its {@link EnderblossomBlockEntity#shakeAngle shake} about the
 * stem base. On the open blossom the eye quads ({@link #EMISSIVE_TEXTURE}) are drawn at full brightness.
 */
public class EnderblossomRenderer implements BlockEntityRenderer<EnderblossomBlockEntity> {
    public static final ResourceLocation EMISSIVE_TEXTURE = SomeonesPerfectEnd.id("block/open_enderblossom_emissive");
    /** How far the stem base sits from the block center toward the log: model z 20 for facing north. */
    private static final float STEM_BASE_OFFSET = 0.5F + EnderblossomBlock.STEM_DEPTH;

    private static final float STEM_BASE_Y = 2.0F / 16.0F;

    private final BlockRenderDispatcher blockRenderer;
    private final RandomSource random = RandomSource.create();

    public EnderblossomRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(
            EnderblossomBlockEntity blossom,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay) {
        BlockState state = blossom.getBlockState();
        if (!(state.getBlock() instanceof EnderblossomBlock block)) {
            return;
        }
        poseStack.pushPose();
        Direction facing = state.getValue(EnderblossomBlock.FACING);
        if (blossom.getLevel() != null) {
            BlockState support =
                    blossom.getLevel().getBlockState(EnderblossomBlock.supportPos(state, blossom.getBlockPos()));
            float offset = EnderblossomBlock.stemOffset(support);
            poseStack.translate(facing.getStepX() * offset, 0.0F, facing.getStepZ() * offset);
        }
        float angle = blossom.shakeAngle(partialTick);
        if (angle != 0.0F) {
            float pivotX = 0.5F - facing.getStepX() * STEM_BASE_OFFSET;
            float pivotZ = 0.5F - facing.getStepZ() * STEM_BASE_OFFSET;
            // Swings side to side about the axis sticking out of the log face.
            poseStack.translate(pivotX, STEM_BASE_Y, pivotZ);
            poseStack.mulPose(Axis.of(facing.step()).rotationDegrees(angle));
            poseStack.translate(-pivotX, -STEM_BASE_Y, -pivotZ);
        }
        BakedModel model = blockRenderer.getBlockModel(state);
        // Not ItemBlockRenderTypes: NeoForge doesn't fill its block map from setRenderLayer, so it would say solid
        // and draw the transparent pixels black.
        VertexConsumer buffer = buffers.getBuffer(RenderType.cutout());
        TextureAtlasSprite emissive = block.type().open()
                ? Minecraft.getInstance()
                        .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                        .apply(EMISSIVE_TEXTURE)
                : null;
        for (Direction side : Direction.values()) {
            renderQuads(poseStack, buffer, model, state, side, emissive, packedLight, packedOverlay);
        }
        renderQuads(poseStack, buffer, model, state, null, emissive, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private void renderQuads(
            PoseStack poseStack,
            VertexConsumer buffer,
            BakedModel model,
            BlockState state,
            Direction side,
            TextureAtlasSprite emissive,
            int packedLight,
            int packedOverlay) {
        random.setSeed(42L);
        for (BakedQuad quad : model.getQuads(state, side, random)) {
            int light = emissive != null && quad.getSprite() == emissive ? LightTexture.FULL_BRIGHT : packedLight;
            buffer.putBulkData(poseStack.last(), quad, 1.0F, 1.0F, 1.0F, 1.0F, light, packedOverlay);
        }
    }
}
