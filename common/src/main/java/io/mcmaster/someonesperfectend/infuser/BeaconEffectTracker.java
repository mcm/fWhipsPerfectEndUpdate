package io.mcmaster.someonesperfectend.infuser;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

/**
 * Per-player record of the effects beacons and Infusers apply (US-031): for each effect and amplifier, the game time
 * the beacon's effect expires. Infused gear pauses under an equal-or-stronger live beacon effect, and effect totems
 * (US-033) stack one level above the strongest one. Server-side and in memory only: beacons re-apply every 80 ticks.
 */
public final class BeaconEffectTracker {
    /** No live beacon effect. */
    public static final int NONE = -1;

    private BeaconEffectTracker() {
    }

    /** Implemented by {@link Player} (through a mixin) to hold the tracked effects. */
    public interface Tracking {
        /** Effect → amplifier → expiry game time. */
        Map<Holder<MobEffect>, Map<Integer, Long>> someonesperfectend$beaconEffects();
    }

    /** Records a beacon or Infuser applying {@code effect} to the player now. */
    public static void record(Player player, MobEffectInstance effect) {
        long expiry = player.level().getGameTime() + effect.getDuration();
        effects(player).computeIfAbsent(effect.getEffect(), key -> new HashMap<>())
                .merge(effect.getAmplifier(), expiry, Math::max);
    }

    /** The highest amplifier of a live (unexpired) tracked beacon effect of this type, or {@link #NONE}. */
    public static int highestAmplifier(Player player, Holder<MobEffect> effect) {
        Map<Integer, Long> byAmplifier = effects(player).get(effect);
        if (byAmplifier == null) {
            return NONE;
        }
        long now = player.level().getGameTime();
        byAmplifier.values().removeIf(expiry -> expiry <= now);
        return byAmplifier.keySet().stream().mapToInt(Integer::intValue).max().orElse(NONE);
    }

    /**
     * Ticks until the tracked beacon effect of this type and amplifier expires, or 0 if there is none. Call
     * {@link #highestAmplifier} first to pick the amplifier.
     */
    public static int remainingTicks(Player player, Holder<MobEffect> effect, int amplifier) {
        Map<Integer, Long> byAmplifier = effects(player).get(effect);
        Long expiry = byAmplifier == null ? null : byAmplifier.get(amplifier);
        return expiry == null ? 0 : (int) Math.max(0, expiry - player.level().getGameTime());
    }

    /** Forgets every tracked effect (for tests). */
    public static void clear(Player player) {
        effects(player).clear();
    }

    private static Map<Holder<MobEffect>, Map<Integer, Long>> effects(Player player) {
        return ((Tracking) player).someonesperfectend$beaconEffects();
    }
}
