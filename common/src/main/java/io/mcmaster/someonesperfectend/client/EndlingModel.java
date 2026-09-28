package io.mcmaster.someonesperfectend.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.mcmaster.someonesperfectend.SomeonesPerfectEnd;
import io.mcmaster.someonesperfectend.entity.Endling;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

/**
 * Copper-Golem proportions: a wide, flat head on a stocky body with short legs and hanging arms, 19 pixels tall.
 *
 * <p>The texture is recolored from the 64x32 Enderman texture, so each box takes its UVs from the matching Enderman
 * part (head at 0,0; body at 32,16; limbs at 56,0) at a size that stays inside that part's opaque pixels, and is then
 * stretched to its real size with a per-axis {@link CubeDeformation}. The head's front face keeps the Enderman's eye
 * row, which lines up with the eyes layer.
 *
 * <p>While inspecting a barter item (US-022) it holds the item up in its right hand, cups it with the left, and bends
 * its head over it, turning it slowly from side to side.
 */
public class EndlingModel extends HierarchicalModel<Endling> implements ArmedModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(SomeonesPerfectEnd.id("endling"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public EndlingModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    /**
     * A box whose UVs come from a {@code uvW x uvH x uvD} box at {@code (u, v)} but which spans {@code w x h x d}
     * pixels starting at {@code (x, y, z)} (relative to the part's pivot).
     */
    private static CubeListBuilder box(int u, int v, int uvW, int uvH, int uvD, float x, float y, float z, float w,
            float h, float d) {
        float gx = (w - uvW) / 2.0F;
        float gy = (h - uvH) / 2.0F;
        float gz = (d - uvD) / 2.0F;
        return CubeListBuilder.create().texOffs(u, v)
                .addBox(x + gx, y + gy, z + gz, uvW, uvH, uvD, new CubeDeformation(gx, gy, gz));
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // Model y grows downward and y = 24 is the feet: legs 18..24, body 11..18, head 5..11.
        root.addOrReplaceChild("body", box(32, 16, 8, 7, 4, -5.0F, 0.0F, -3.0F, 10, 7, 6),
                PartPose.offset(0.0F, 11.0F, 0.0F));
        root.addOrReplaceChild("head", box(0, 0, 8, 6, 8, -5.0F, -6.0F, -4.0F, 10, 6, 8),
                PartPose.offset(0.0F, 11.0F, 0.0F));
        root.addOrReplaceChild("right_arm", box(56, 0, 2, 9, 2, -3.0F, 0.0F, -1.5F, 3, 9, 3),
                PartPose.offset(-5.0F, 11.5F, 0.0F));
        root.addOrReplaceChild("left_arm", box(56, 0, 2, 9, 2, 0.0F, 0.0F, -1.5F, 3, 9, 3).mirror(),
                PartPose.offset(5.0F, 11.5F, 0.0F));
        root.addOrReplaceChild("right_leg", box(56, 0, 2, 6, 2, -2.0F, 0.0F, -2.0F, 4, 6, 4),
                PartPose.offset(-2.5F, 18.0F, 0.0F));
        root.addOrReplaceChild("left_leg", box(56, 0, 2, 6, 2, -2.0F, 0.0F, -2.0F, 4, 6, 4).mirror(),
                PartPose.offset(2.5F, 18.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(Endling entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
            float headPitch) {
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;
        // Short legs take quick steps; arms swing opposite and idle-sway a little.
        float swing = Mth.cos(limbSwing * 0.9F) * 1.2F * limbSwingAmount;
        rightLeg.xRot = swing;
        leftLeg.xRot = -swing;
        float idle = Mth.sin(ageInTicks * 0.067F) * 0.05F;
        rightArm.xRot = -swing * 0.8F;
        leftArm.xRot = swing * 0.8F;
        rightArm.zRot = 0.05F + idle;
        leftArm.zRot = -0.05F - idle;
        rightArm.yRot = 0.0F;
        leftArm.yRot = 0.0F;
        if (entity.isInspecting()) {
            float turn = Mth.sin(ageInTicks * 0.15F);
            head.xRot = 0.6F;
            head.yRot = turn * 0.35F;
            rightArm.xRot = -1.1F + Mth.sin(ageInTicks * 0.3F) * 0.05F;
            rightArm.yRot = -0.25F + turn * 0.15F;
            rightArm.zRot = 0.0F;
            leftArm.xRot = -0.9F;
            leftArm.yRot = 0.45F;
            leftArm.zRot = 0.0F;
        }
    }

    @Override
    public void translateToHand(HumanoidArm arm, PoseStack poseStack) {
        (arm == HumanoidArm.RIGHT ? rightArm : leftArm).translateAndRotate(poseStack);
    }
}
