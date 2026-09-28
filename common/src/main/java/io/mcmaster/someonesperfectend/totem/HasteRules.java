package io.mcmaster.someonesperfectend.totem;

/**
 * Mining speed with totem-given Haste (MOD-46), free of registry access so it can be unit-tested. Haste III or
 * higher with an activated Totem of Haste worn mines three times as fast per level as vanilla Haste; Haste from
 * anywhere else (other mods included) stays vanilla.
 */
public final class HasteRules {
    /** Vanilla's mining speed bonus per Haste level. */
    public static final float VANILLA_PER_LEVEL = 0.2F;
    /** The boosted mining speed bonus per Haste level. */
    public static final float BOOSTED_PER_LEVEL = 0.6F;
    /** The lowest Haste amplifier (Haste III) that is boosted. */
    public static final int MIN_BOOSTED_AMPLIFIER = 2;

    private HasteRules() {
    }

    /** Whether Haste is boosted: Haste III or higher with an activated Totem of Haste in the trinket slot. */
    public static boolean boosted(int hasteAmplifier, boolean activeHasteTotem) {
        return hasteAmplifier >= MIN_BOOSTED_AMPLIFIER && activeHasteTotem;
    }

    /** The mining speed bonus per Haste level. */
    public static float perLevel(boolean boosted) {
        return boosted ? BOOSTED_PER_LEVEL : VANILLA_PER_LEVEL;
    }

    /** The Haste mining speed multiplier: vanilla's {@code 1 + 0.2 × (amplifier + 1)}, or 0.6 per level boosted. */
    public static float digSpeedMultiplier(int amplifier, boolean boosted) {
        return 1.0F + (amplifier + 1) * perLevel(boosted);
    }
}
