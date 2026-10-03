package io.mcmaster.someonesperfectend.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.mcmaster.someonesperfectend.SomeonesPerfectEnd;
import io.mcmaster.someonesperfectend.entity.Driftjelly;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

/**
 * A translucent bell with a ring of tendrils. UVs follow the vanilla squid layout (body at 0,0; tentacles at 48,0) so
 * the placeholder texture recolored from the glow squid lines up.
 */
public class DriftjellyModel extends HierarchicalModel<Driftjelly> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(SomeonesPerfectEnd.id("driftjelly"), "main");
    private static final int TENDRILS = 8;
    /** Alpha applied to every layer, so the bell always reads as translucent. */
    private static final int ALPHA = 0xC0;

    private final ModelPart root;
    private final ModelPart bell;
    private final ModelPart[] tendrils = new ModelPart[TENDRILS];
    private final float[] angles = new float[TENDRILS];

    public DriftjellyModel(ModelPart root) {
        super(RenderType::entityTranslucent);
        this.root = root;
        this.bell = root.getChild("bell");
        for (int i = 0; i < TENDRILS; i++) {
            tendrils[i] = root.getChild(tendrilName(i));
            angles[i] = angle(i);
        }
    }

    private static float angle(int i) {
        return i * Mth.TWO_PI / TENDRILS;
    }

    private static String tendrilName(int i) {
        return "tendril" + i;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // Model y grows downward; y = 24 is the entity's feet. The bell spans y 8..15, the tendrils 15..24.
        root.addOrReplaceChild(
                "bell",
                CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -7.0F, -6.0F, 12, 7, 12),
                PartPose.offset(0.0F, 15.0F, 0.0F));
        CubeListBuilder tendril = CubeListBuilder.create().texOffs(48, 0).addBox(-0.5F, 0.0F, -0.5F, 1, 9, 1);
        for (int i = 0; i < TENDRILS; i++) {
            root.addOrReplaceChild(
                    tendrilName(i),
                    tendril,
                    PartPose.offset(Mth.cos(angle(i)) * 4.0F, 15.0F, Mth.sin(angle(i)) * 4.0F));
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(
            Driftjelly entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch) {
        float pulse = Mth.sin(ageInTicks * 0.15F);
        bell.xScale = 1.0F + pulse * 0.06F;
        bell.zScale = bell.xScale;
        bell.yScale = 1.0F - pulse * 0.05F;
        for (int i = 0; i < TENDRILS; i++) {
            // Tendrils flare outward as the bell contracts, each swaying slightly out of phase.
            float tilt = 0.12F + Mth.sin(ageInTicks * 0.1F + i) * 0.1F - pulse * 0.08F;
            tendrils[i].xRot = tilt * Mth.sin(angles[i]);
            tendrils[i].zRot = -tilt * Mth.cos(angles[i]);
        }
    }

    @Override
    public void renderToBuffer(
            PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        super.renderToBuffer(
                poseStack,
                buffer,
                packedLight,
                packedOverlay,
                FastColor.ARGB32.multiply(color, FastColor.ARGB32.color(ALPHA, 0xFFFFFF)));
    }
}
