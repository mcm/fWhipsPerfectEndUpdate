package io.mcmaster.someonesperfectend.infuser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * The infusion rules (US-030), independent of items and registries: which effects an item may take, tier gating,
 * the level set at the first infusion, and how much time a charge adds.
 */
public final class InfusionRules {
    public static final int MAX_TIER = 4;
    /** A charge can store at most this many payments' worth of time. */
    public static final int MAX_PAYMENTS = 64;
    /** Infused gear updates every this many ticks and uses up this much stored time when active (US-031). */
    public static final int GEAR_INTERVAL = 20;
    /** Duration of the effect active infused gear applies, refreshed every {@link #GEAR_INTERVAL} ticks. */
    public static final int GEAR_EFFECT_TICKS = 80;

    private InfusionRules() {}

    /** Which infusion slots an item counts as, from its item tags. */
    public enum Kind {
        /** {@code #minecraft:foot_armor} */
        FEET,
        /** {@code #minecraft:chest_armor} */
        CHEST,
        /** {@code #minecraft:enchantable/mining} */
        MINING,
        /** {@code #minecraft:enchantable/weapon} */
        WEAPON
    }

    /** An item's existing infusion. {@code effect} is null if it holds an effect gear can't normally take. */
    public record Current(@Nullable InfusionEffect effect, int amplifier, int remainingTicks) {}

    /** The infusion an item gets from a charge. */
    public record Charge(InfusionEffect effect, int amplifier, int remainingTicks) {}

    /** Effects an item may be infused with, ignoring tier; an item in both tool tags may take either. */
    public static List<InfusionEffect> allowedEffects(Set<Kind> kinds) {
        List<InfusionEffect> effects = new ArrayList<>();
        if (kinds.contains(Kind.FEET)) {
            effects.add(InfusionEffect.SPEED);
            effects.add(InfusionEffect.JUMP_BOOST);
        }
        if (kinds.contains(Kind.CHEST)) {
            effects.add(InfusionEffect.RESISTANCE);
            effects.add(InfusionEffect.REGENERATION);
        }
        if (kinds.contains(Kind.MINING)) {
            effects.add(InfusionEffect.HASTE);
        }
        if (kinds.contains(Kind.WEAPON)) {
            effects.add(InfusionEffect.STRENGTH);
        }
        return effects;
    }

    public static boolean isUnlocked(InfusionEffect effect, int tier) {
        return tier >= effect.minTier();
    }

    /** The amplifier set at the first infusion: level II on a tier-4 pyramid, except Regeneration. */
    public static int amplifierFor(InfusionEffect effect, int tier) {
        return tier >= MAX_TIER && effect.canBeLevelTwo() ? 1 : 0;
    }

    /** Ticks one payment adds: a beacon's effect duration at this tier, (9 + 2 × tier) seconds. */
    public static int chargeTicks(int tier) {
        return (9 + 2 * Math.min(tier, MAX_TIER)) * 20;
    }

    /** The most time a charge at this tier can store: 64 payments' worth. */
    public static int capTicks(int tier) {
        return MAX_PAYMENTS * chargeTicks(tier);
    }

    /**
     * Whether the effect can be chosen for this item at this tier: the item may take it, the tier unlocks it, and
     * an already-infused item only takes its existing effect.
     */
    public static boolean canSelect(Set<Kind> kinds, @Nullable Current current, InfusionEffect effect, int tier) {
        if (!allowedEffects(kinds).contains(effect) || !isUnlocked(effect, tier)) {
            return false;
        }
        return current == null || current.effect() == effect;
    }

    /**
     * The result of charging with one payment, or empty if the effect can't be selected or the stored time is
     * already at the cap. A first infusion sets the level from the tier; a top-up keeps the existing level. The
     * new time is clamped to the cap and is never less than the existing time.
     */
    public static Optional<Charge> charge(Set<Kind> kinds, @Nullable Current current, InfusionEffect effect, int tier) {
        if (!canSelect(kinds, current, effect, tier)) {
            return Optional.empty();
        }
        int existing = current == null ? 0 : current.remainingTicks();
        int cap = capTicks(tier);
        if (existing >= cap) {
            return Optional.empty();
        }
        int amplifier = current == null ? amplifierFor(effect, tier) : current.amplifier();
        return Optional.of(new Charge(effect, amplifier, Math.min(existing + chargeTicks(tier), cap)));
    }

    /**
     * Whether infused gear pauses (no effect, no depletion) under a live tracked beacon effect of the same type:
     * the beacon's level is the same or higher. {@code beaconAmplifier} is negative when there is none.
     */
    public static boolean pausedByBeacon(int amplifier, int beaconAmplifier) {
        return beaconAmplifier >= amplifier;
    }

    /** Stored time left after one active gear update; the infusion is removed at 0. */
    public static int deplete(int remainingTicks) {
        return Math.max(remainingTicks - GEAR_INTERVAL, 0);
    }

    /** Formats a tick count as {@code m:ss}, e.g. 65280 ticks → {@code 54:24}. */
    public static String formatTime(int ticks) {
        int seconds = Math.max(ticks, 0) / 20;
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }
}
