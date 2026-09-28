package io.mcmaster.someonesperfectend.mixin;

import io.mcmaster.someonesperfectend.block.BounceCapBlock;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Bounce Cap hooks: Slow Falling while standing on a cap, and the launch when jumping from one. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void someonesperfectend$tickBounceCap(CallbackInfo ci) {
        BounceCapBlock.tickEntity((LivingEntity) (Object) this);
    }

    @Inject(method = "jumpFromGround", at = @At("TAIL"))
    private void someonesperfectend$bounce(CallbackInfo ci) {
        BounceCapBlock.afterJump((LivingEntity) (Object) this);
    }
}
