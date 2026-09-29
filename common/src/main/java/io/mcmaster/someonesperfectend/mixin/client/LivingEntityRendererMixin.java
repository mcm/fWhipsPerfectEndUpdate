package io.mcmaster.someonesperfectend.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.mcmaster.someonesperfectend.block.EnderBedPose;
import io.mcmaster.someonesperfectend.block.EnderBedTravel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws a sleeper under a hanging Ender Bed upside down, back against the bed's underside (MOD-42). Applied at the
 * head so it wraps vanilla's sleeping rotations in the world-aligned frame; vanilla beds are untouched.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Inject(method = "setupRotations", at = @At("HEAD"))
    private void someonesperfectend$flipUnderEnderBed(
            LivingEntity entity,
            PoseStack poseStack,
            float bob,
            float yBodyRot,
            float partialTick,
            float scale,
            CallbackInfo ci) {
        if (!entity.hasPose(Pose.SLEEPING) || !EnderBedTravel.isInEnderBed(entity)) {
            return;
        }
        Direction facing = entity.getBedOrientation();
        if (facing != null) {
            poseStack.translate(
                    0.0F,
                    EnderBedPose.lift(scale, entity.getDimensions(Pose.SLEEPING).height()),
                    0.0F);
            poseStack.mulPose(EnderBedPose.roll(facing));
        }
    }
}
