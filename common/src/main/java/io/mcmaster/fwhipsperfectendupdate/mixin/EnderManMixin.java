package io.mcmaster.fwhipsperfectendupdate.mixin;

import io.mcmaster.fwhipsperfectendupdate.block.EnderblossomShaking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Enderblossoms near either end of a successful Enderman teleport shake. */
@Mixin(EnderMan.class)
public abstract class EnderManMixin {
    @Unique
    private Vec3 fwhipsperfectendupdate$teleportStart;

    @Inject(method = "teleport(DDD)Z", at = @At("HEAD"))
    private void fwhipsperfectendupdate$rememberStart(
            double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        fwhipsperfectendupdate$teleportStart = ((EnderMan) (Object) this).position();
    }

    @Inject(method = "teleport(DDD)Z", at = @At("RETURN"))
    private void fwhipsperfectendupdate$shakeBlossoms(
            double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        EnderMan enderman = (EnderMan) (Object) this;
        if (cir.getReturnValueZ() && enderman.level() instanceof ServerLevel level) {
            EnderblossomShaking.shakeNear(level, fwhipsperfectendupdate$teleportStart, enderman.position());
        }
    }
}
