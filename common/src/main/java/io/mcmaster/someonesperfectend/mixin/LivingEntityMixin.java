package io.mcmaster.someonesperfectend.mixin;

import io.mcmaster.someonesperfectend.block.BounceCapBlock;
import io.mcmaster.someonesperfectend.block.EnderBedBlock;
import io.mcmaster.someonesperfectend.block.EnderBedTravel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Bounce Cap hooks (Slow Falling while standing on a cap, and the launch when jumping from one) and Ender Bed
 * sleeping (US-038: holding the sleeper in the hanging bed, the trip, and where they wake).
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Unique
    @Nullable
    private BlockPos someonesperfectend$wakingEnderBed;

    @Inject(method = "tick", at = @At("TAIL"))
    private void someonesperfectend$tickBounceCap(CallbackInfo ci) {
        BounceCapBlock.tickEntity((LivingEntity) (Object) this);
        EnderBedTravel.tick((LivingEntity) (Object) this);
    }

    /** Vanilla lies the sleeper on top of the bed; under a hanging bed they go against its underside instead. */
    @Inject(method = "setPosToBed", at = @At("TAIL"))
    private void someonesperfectend$lieUnderEnderBed(BlockPos pos, CallbackInfo ci) {
        if ((Object) this instanceof Player player
                && player.level().getBlockState(pos).getBlock() instanceof EnderBedBlock) {
            EnderBedTravel.pin(player, pos);
        }
    }

    @Inject(method = "stopSleeping", at = @At("HEAD"))
    private void someonesperfectend$beforeWaking(CallbackInfo ci) {
        someonesperfectend$wakingEnderBed = EnderBedTravel.enderBed((LivingEntity) (Object) this).orElse(null);
    }

    /** Vanilla stands the sleeper up on top of the bed, which for a hanging bed is inside its support. */
    @Inject(method = "stopSleeping", at = @At("TAIL"))
    private void someonesperfectend$afterWaking(CallbackInfo ci) {
        BlockPos head = someonesperfectend$wakingEnderBed;
        someonesperfectend$wakingEnderBed = null;
        LivingEntity self = (LivingEntity) (Object) this;
        Vec3 pos = head == null ? null : EnderBedTravel.wakePosition(self.level(), head);
        if (pos != null) {
            self.setPos(pos.x, pos.y, pos.z);
        }
    }

    @Inject(method = "jumpFromGround", at = @At("TAIL"))
    private void someonesperfectend$bounce(CallbackInfo ci) {
        BounceCapBlock.afterJump((LivingEntity) (Object) this);
    }
}
