package io.mcmaster.fwhipsperfectendupdate.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.mcmaster.fwhipsperfectendupdate.infuser.BeaconEffectTracker;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Tracks every effect a beacon (or an Infuser, which runs the same code) applies to a player. */
@Mixin(BeaconBlockEntity.class)
public abstract class BeaconBlockEntityMixin {
    @WrapOperation(
            method = "applyEffects",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/player/Player;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    private static boolean fwhipsperfectendupdate$trackBeaconEffect(
            Player player, MobEffectInstance effect, Operation<Boolean> original) {
        BeaconEffectTracker.record(player, effect);
        return original.call(player, effect);
    }
}
