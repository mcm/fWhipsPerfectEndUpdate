package io.mcmaster.someonesperfectend.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.mcmaster.someonesperfectend.block.EnderBedLocations;
import io.mcmaster.someonesperfectend.block.EnderBedTravel;
import io.mcmaster.someonesperfectend.infuser.BeaconEffectTracker;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Holds the player's tracked beacon effects and (saved with the player) Ender Bed locations, and lets players sleep
 * in an Ender Bed during the day (US-038).
 */
@Mixin(Player.class)
public abstract class PlayerMixin implements BeaconEffectTracker.Tracking, EnderBedLocations.Holder {
    @Unique
    private final Map<Holder<MobEffect>, Map<Integer, Long>> someonesperfectend$beaconEffects = new HashMap<>();
    @Unique
    private final EnderBedLocations someonesperfectend$enderBeds = new EnderBedLocations();

    @Override
    public Map<Holder<MobEffect>, Map<Integer, Long>> someonesperfectend$beaconEffects() {
        return someonesperfectend$beaconEffects;
    }

    @Override
    public EnderBedLocations someonesperfectend$enderBeds() {
        return someonesperfectend$enderBeds;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void someonesperfectend$saveEnderBeds(CompoundTag tag, CallbackInfo ci) {
        someonesperfectend$enderBeds.save(tag);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void someonesperfectend$loadEnderBeds(CompoundTag tag, CallbackInfo ci) {
        someonesperfectend$enderBeds.load(tag);
    }

    /** Vanilla wakes sleepers when it's day (NeoForge: through {@code canEntityContinueSleeping}); Ender Beds don't. */
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;isDay()Z"))
    private boolean someonesperfectend$enderBedIgnoresDay(Level level, Operation<Boolean> original) {
        return original.call(level) && !EnderBedTravel.isInEnderBed((Player) (Object) this);
    }
}
