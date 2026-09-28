package io.mcmaster.someonesperfectend.mixin;

import io.mcmaster.someonesperfectend.block.EnderBedLocations;
import io.mcmaster.someonesperfectend.infuser.BeaconEffectTracker;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Holds the player's tracked beacon effects and (saved with the player) Ender Bed locations. */
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
}
