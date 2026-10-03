package io.mcmaster.someonesperfectend.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.mcmaster.someonesperfectend.SomeonesPerfectEnd;
import io.mcmaster.someonesperfectend.entity.Driftjelly;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

/** Translucent bell plus an emissive glow layer (off while dormant); both textures are recolored from the glow squid. */
public class DriftjellyRenderer extends MobRenderer<Driftjelly, DriftjellyModel> {
    private static final ResourceLocation TEXTURE = SomeonesPerfectEnd.id("textures/entity/driftjelly/driftjelly.png");
    private static final RenderType GLOW =
            RenderType.eyes(SomeonesPerfectEnd.id("textures/entity/driftjelly/driftjelly_glow.png"));

    public DriftjellyRenderer(EntityRendererProvider.Context context) {
        super(context, new DriftjellyModel(context.bakeLayer(DriftjellyModel.LAYER)), 0.3F);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return GLOW;
            }

            @Override
            public void render(
                    PoseStack poseStack,
                    MultiBufferSource buffers,
                    int packedLight,
                    Driftjelly entity,
                    float limbSwing,
                    float limbSwingAmount,
                    float partialTick,
                    float ageInTicks,
                    float netHeadYaw,
                    float headPitch) {
                // Dormant driftjellies (outside the End) lose their glow.
                if (!entity.isDormant()) {
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
    }

    @Override
    public ResourceLocation getTextureLocation(Driftjelly entity) {
        return TEXTURE;
    }
}
