package io.mcmaster.someonesperfectend.totem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HasteRulesTest {
    private static final float EPSILON = 1.0E-5F;

    @Test
    void onlyHasteThreeOrHigherWithAnActiveTotemIsBoosted() {
        for (int amplifier = 0; amplifier <= 3; amplifier++) {
            assertEquals(amplifier >= 2, HasteRules.boosted(amplifier, true), "amplifier " + amplifier + " with totem");
            assertFalse(HasteRules.boosted(amplifier, false), "amplifier " + amplifier + " without totem");
        }
    }

    @Test
    void boostedMultiplierIsThreeTimesVanillaPerLevel() {
        float[] vanilla = {1.2F, 1.4F, 1.6F, 1.8F};
        float[] boosted = {1.6F, 2.2F, 2.8F, 3.4F};
        for (int amplifier = 0; amplifier <= 3; amplifier++) {
            assertEquals(
                    vanilla[amplifier],
                    HasteRules.digSpeedMultiplier(amplifier, false),
                    EPSILON,
                    "vanilla amplifier " + amplifier);
            assertEquals(
                    boosted[amplifier],
                    HasteRules.digSpeedMultiplier(amplifier, true),
                    EPSILON,
                    "boosted amplifier " + amplifier);
        }
    }

    @Test
    void vanillaMultiplierMatchesVanillaArithmetic() {
        for (int amplifier = 0; amplifier <= 3; amplifier++) {
            assertEquals(
                    1.0F + (float) (amplifier + 1) * 0.2F,
                    HasteRules.digSpeedMultiplier(amplifier, false),
                    "amplifier " + amplifier);
        }
        assertTrue(HasteRules.digSpeedMultiplier(2, true) > HasteRules.digSpeedMultiplier(2, false));
    }
}
