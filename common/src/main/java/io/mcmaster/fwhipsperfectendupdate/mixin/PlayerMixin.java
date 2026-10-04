package io.mcmaster.fwhipsperfectendupdate.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.mcmaster.fwhipsperfectendupdate.block.EnderBedLocations;
import io.mcmaster.fwhipsperfectendupdate.block.EnderBedTravel;
import io.mcmaster.fwhipsperfectendupdate.infuser.BeaconEffectTracker;
import io.mcmaster.fwhipsperfectendupdate.totem.EffectTotems;
import io.mcmaster.fwhipsperfectendupdate.totem.HasteRules;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Holds the player's tracked beacon effects and (saved with the player) Ender Bed locations, and lets players sleep
 * in an Ender Bed during the day (US-038). Haste III+ with an activated Totem of Haste mines faster (MOD-46).
 */
@Mixin(Player.class)
public abstract class PlayerMixin implements BeaconEffectTracker.Tracking, EnderBedLocations.Holder {
    @Unique
    private final Map<Holder<MobEffect>, Map<Integer, Long>> fwhipsperfectendupdate$beaconEffects = new HashMap<>();

    @Unique
    private final EnderBedLocations fwhipsperfectendupdate$enderBeds = new EnderBedLocations();

    @Override
    public Map<Holder<MobEffect>, Map<Integer, Long>> fwhipsperfectendupdate$beaconEffects() {
        return fwhipsperfectendupdate$beaconEffects;
    }

    @Override
    public EnderBedLocations fwhipsperfectendupdate$enderBeds() {
        return fwhipsperfectendupdate$enderBeds;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void fwhipsperfectendupdate$saveEnderBeds(CompoundTag tag, CallbackInfo ci) {
        fwhipsperfectendupdate$enderBeds.save(tag);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void fwhipsperfectendupdate$loadEnderBeds(CompoundTag tag, CallbackInfo ci) {
        fwhipsperfectendupdate$enderBeds.load(tag);
    }

    /** Vanilla wakes sleepers when it's day (NeoForge: through {@code canEntityContinueSleeping}); Ender Beds don't. */
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;isDay()Z"))
    private boolean fwhipsperfectendupdate$enderBedIgnoresDay(Level level, Operation<Boolean> original) {
        return original.call(level) && !EnderBedTravel.isInEnderBed((Player) (Object) this);
    }

    /**
     * Replaces vanilla's 0.2 per Haste level with the boosted 0.6 when {@link HasteRules#boosted} (MOD-46); the rest
     * of the mining speed (Mining Fatigue, underwater, not on the ground, attributes, Conduit Power alone) is
     * untouched. NeoForge moves the vanilla body of {@code getDestroySpeed} into {@code getDigSpeed}.
     */
    @ModifyExpressionValue(
            method = {"getDestroySpeed", "getDigSpeed"},
            at = @At(value = "CONSTANT", args = "floatValue=0.2F"))
    private float fwhipsperfectendupdate$totemHaste(float perLevel) {
        Player player = (Player) (Object) this;
        MobEffectInstance haste = player.getEffect(MobEffects.DIG_SPEED);
        boolean boosted = haste != null
                && HasteRules.boosted(
                        haste.getAmplifier(),
                        EffectTotems.hasActiveTotemEquipped(player, EffectTotems.TOTEM_OF_HASTE.get()));
        return boosted ? HasteRules.perLevel(true) : perLevel;
    }
}
