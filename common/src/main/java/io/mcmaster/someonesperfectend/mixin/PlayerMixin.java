package io.mcmaster.someonesperfectend.mixin;

import io.mcmaster.someonesperfectend.infuser.BeaconEffectTracker;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Holds the player's tracked beacon effects. */
@Mixin(Player.class)
public abstract class PlayerMixin implements BeaconEffectTracker.Tracking {
    @Unique
    private final Map<Holder<MobEffect>, Map<Integer, Long>> someonesperfectend$beaconEffects = new HashMap<>();

    @Override
    public Map<Holder<MobEffect>, Map<Integer, Long>> someonesperfectend$beaconEffects() {
        return someonesperfectend$beaconEffects;
    }
}
