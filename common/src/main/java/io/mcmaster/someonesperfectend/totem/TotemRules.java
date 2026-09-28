package io.mcmaster.someonesperfectend.totem;

import io.mcmaster.someonesperfectend.infuser.InfusionRules;

/** Effect totem rules (US-032), free of registry access so they can be unit-tested. */
public final class TotemRules {
    /** Totems are activated only at a full-size (tier-4) pyramid. */
    public static final int ACTIVATION_TIER = InfusionRules.MAX_TIER;
    /** An active totem re-applies its effect this often, like a beacon. */
    public static final int EFFECT_INTERVAL = 80;
    /** Effect duration: (9 + 2 × 4) seconds, as a full-size beacon. */
    public static final int EFFECT_DURATION = InfusionRules.chargeTicks(ACTIVATION_TIER);
    /** Totems give level I (beacon stacking is US-033). */
    public static final int AMPLIFIER = 0;

    private TotemRules() {
    }

    /**
     * Whether confirming at the Infuser activates a totem: only in the End, only at tier 4, only with a netherite
     * ingot as payment, and only if the totem isn't already active.
     */
    public static boolean canActivate(boolean inEnd, int tier, boolean netheritePayment, boolean alreadyActive) {
        return inEnd && tier >= ACTIVATION_TIER && netheritePayment && !alreadyActive;
    }

    /** Whether an equipped active totem applies its effect on this game tick. */
    public static boolean appliesOn(long gameTime) {
        return gameTime % EFFECT_INTERVAL == 0;
    }
}
