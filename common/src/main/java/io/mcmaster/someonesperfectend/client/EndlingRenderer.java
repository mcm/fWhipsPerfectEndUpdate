package io.mcmaster.someonesperfectend.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.mcmaster.someonesperfectend.SomeonesPerfectEnd;
import io.mcmaster.someonesperfectend.entity.Endling;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Enderman-palette body with glowing purple eyes; a Lost endling uses a desaturated, hollow-eyed texture and no glow.
 * All textures are recolored from the Enderman's. The held barter item is drawn in its hand while it inspects it.
 */
public class EndlingRenderer extends MobRenderer<Endling, EndlingModel> {
    private static final ResourceLocation TEXTURE = SomeonesPerfectEnd.id("textures/entity/endling/endling.png");
    private static final ResourceLocation LOST_TEXTURE =
            SomeonesPerfectEnd.id("textures/entity/endling/endling_lost.png");
    private static final RenderType EYES =
            RenderType.eyes(SomeonesPerfectEnd.id("textures/entity/endling/endling_eyes.png"));

    public EndlingRenderer(EntityRendererProvider.Context context) {
        super(context, new EndlingModel(context.bakeLayer(EndlingModel.LAYER)), 0.45F);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return EYES;
            }

            @Override
            public void render(
                    PoseStack poseStack,
                    MultiBufferSource buffers,
                    int packedLight,
                    Endling entity,
                    float limbSwing,
                    float limbSwingAmount,
                    float partialTick,
                    float ageInTicks,
                    float netHeadYaw,
                    float headPitch) {
                if (!entity.isLost()) {
                    super.render(
                            poseStack,
                            buffers,
                            packedLight,
                            entity,
                            limbSwing,
                            limbSwingAmount,
                            partialTick,
                            ageInTicks,
                            netHeadYaw,
                            headPitch);
                }
            }
        });
        addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(Endling entity) {
        return entity.isLost() ? LOST_TEXTURE : TEXTURE;
    }
}
