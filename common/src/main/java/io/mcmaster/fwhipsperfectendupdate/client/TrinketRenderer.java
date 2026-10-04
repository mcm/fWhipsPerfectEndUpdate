package io.mcmaster.fwhipsperfectendupdate.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.wispforest.accessories.api.client.AccessoryRenderer;
import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Renders an equipped trinket as a small item hanging on the front of the wearer's belt, on one hip. Accessories
 * calls this from its render layer for every player (the slot contents are synced to tracking clients), so
 * other players can see what someone is wearing.
 */
public final class TrinketRenderer implements AccessoryRenderer {
    private static final float SCALE = 0.5F;

    @Override
    public <M extends LivingEntity> void render(
            ItemStack stack,
            SlotReference reference,
            PoseStack matrices,
            EntityModel<M> model,
            MultiBufferSource buffers,
            int light,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch) {
        if (!(model instanceof HumanoidModel<?> humanoid)) {
            return;
        }
        // x: toward one hip, y: belt height near the bottom of the torso, z: front face.
        AccessoryRenderer.transformToModelPart(matrices, humanoid.body, 0.55, -0.75, 1);
        matrices.scale(SCALE, SCALE, SCALE);
        Minecraft.getInstance()
                .getItemRenderer()
                .renderStatic(
                        stack,
                        ItemDisplayContext.FIXED,
                        light,
                        OverlayTexture.NO_OVERLAY,
                        matrices,
                        buffers,
                        reference.entity().level(),
                        0);
    }
}
