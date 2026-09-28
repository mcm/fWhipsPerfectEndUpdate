package io.mcmaster.someonesperfectend.client;

import io.mcmaster.someonesperfectend.SomeonesPerfectEnd;
import io.mcmaster.someonesperfectend.entity.Driftjelly;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

/** Translucent bell plus an emissive glow layer; both textures are recolored from the glow squid. */
public class DriftjellyRenderer extends MobRenderer<Driftjelly, DriftjellyModel> {
    private static final ResourceLocation TEXTURE =
            SomeonesPerfectEnd.id("textures/entity/driftjelly/driftjelly.png");
    private static final RenderType GLOW = RenderType.eyes(
            SomeonesPerfectEnd.id("textures/entity/driftjelly/driftjelly_glow.png"));

    public DriftjellyRenderer(EntityRendererProvider.Context context) {
        super(context, new DriftjellyModel(context.bakeLayer(DriftjellyModel.LAYER)), 0.3F);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return GLOW;
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(Driftjelly entity) {
        return TEXTURE;
    }
}
