package io.mcmaster.someonesperfectend.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.mcmaster.someonesperfectend.SomeonesPerfectEnd;
import io.mcmaster.someonesperfectend.entity.ModBoatType;
import net.minecraft.client.model.BoatModel;
import net.minecraft.client.model.ChestBoatModel;
import net.minecraft.client.model.ChestRaftModel;
import net.minecraft.client.model.ListModel;
import net.minecraft.client.model.RaftModel;
import net.minecraft.client.model.WaterPatchModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.Boat;
import org.joml.Quaternionf;

/**
 * Renders one of the mod's boats with the vanilla model of its shape (baked from the vanilla model layers) and our
 * texture, {@code someonesperfectend:textures/entity/(chest_)boat/<name>.png}. The render body mirrors
 * {@link BoatRenderer#render}, which looks its model and texture up by vanilla boat type.
 */
public class ModBoatRenderer extends EntityRenderer<Boat> {
    private final ResourceLocation texture;
    private final ListModel<Boat> model;

    public ModBoatRenderer(EntityRendererProvider.Context context, ModBoatType type, boolean chest) {
        super(context);
        this.shadowRadius = 0.8F;
        this.texture = SomeonesPerfectEnd.id(
                "textures/entity/" + (chest ? "chest_boat/" : "boat/") + type.getName() + ".png");
        ModelPart root = context.bakeLayer(chest ? ModelLayers.createChestBoatModelName(type.shape())
                : ModelLayers.createBoatModelName(type.shape()));
        if (type.isRaft()) {
            this.model = chest ? new ChestRaftModel(root) : new RaftModel(root);
        } else {
            this.model = chest ? new ChestBoatModel(root) : new BoatModel(root);
        }
    }

    @Override
    public void render(Boat boat, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
            int light) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.375F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        float hurtTime = boat.getHurtTime() - partialTick;
        float damage = Math.max(0.0F, boat.getDamage() - partialTick);
        if (hurtTime > 0.0F) {
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.sin(hurtTime) * hurtTime * damage / 10.0F * boat.getHurtDir()));
        }
        float bubbleAngle = boat.getBubbleAngle(partialTick);
        if (!Mth.equal(bubbleAngle, 0.0F)) {
            poseStack.mulPose(new Quaternionf().setAngleAxis(bubbleAngle * (float) (Math.PI / 180.0), 1.0F, 0.0F, 1.0F));
        }
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        model.setupAnim(boat, partialTick, 0.0F, -0.1F, 0.0F, 0.0F);
        VertexConsumer consumer = buffers.getBuffer(model.renderType(texture));
        model.renderToBuffer(poseStack, consumer, light, OverlayTexture.NO_OVERLAY);
        if (!boat.isUnderWater() && model instanceof WaterPatchModel waterPatch) {
            waterPatch.waterPatch().render(poseStack, buffers.getBuffer(RenderType.waterMask()), light,
                    OverlayTexture.NO_OVERLAY);
        }
        poseStack.popPose();
        super.render(boat, yaw, partialTick, poseStack, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(Boat boat) {
        return texture;
    }
}
