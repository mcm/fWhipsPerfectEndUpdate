package io.mcmaster.someonesperfectend.mixin.client;

import io.mcmaster.someonesperfectend.block.EnderBedPose;
import io.mcmaster.someonesperfectend.block.EnderBedTravel;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Turns the first-person view upside down while sleeping under a hanging Ender Bed, matching the face-down model
 * ({@link LivingEntityRendererMixin}): vanilla's sleeping camera is rolled 180° and moved below the sleeper's face,
 * where it would otherwise sit inside the bed's base. The third-person cameras are left to vanilla.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    @Final
    private Quaternionf rotation;
    @Shadow
    @Final
    private Vector3f up;
    @Shadow
    @Final
    private Vector3f left;

    @Shadow
    public abstract Vec3 getPosition();

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Inject(method = "setup", at = @At("TAIL"))
    private void someonesperfectend$flipUnderEnderBed(BlockGetter level, Entity entity, boolean detached,
            boolean mirrored, float partialTick, CallbackInfo ci) {
        if (detached || !(entity instanceof LivingEntity living) || !living.hasPose(Pose.SLEEPING)
                || !EnderBedTravel.isInEnderBed(living)) {
            return;
        }
        Vec3 pos = getPosition();
        double entityY = Mth.lerp(partialTick, entity.yo, entity.getY());
        setPosition(pos.x, EnderBedPose.cameraY(entityY, pos.y, living.getScale(),
                living.getDimensions(Pose.SLEEPING).height()), pos.z);
        rotation.rotateZ(Mth.PI);
        up.negate();
        left.negate();
    }
}
