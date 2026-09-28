package io.mcmaster.someonesperfectend.totem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TotemRulesTest {
    @Test
    void activationNeedsTheEndTierFourNetheriteAndAnInactiveTotem() {
        assertTrue(TotemRules.canActivate(true, 4, true, false));
        assertFalse(TotemRules.canActivate(false, 4, true, false), "End only");
        for (int tier = 0; tier < 4; tier++) {
            assertFalse(TotemRules.canActivate(true, tier, true, false), "tier " + tier);
        }
        assertFalse(TotemRules.canActivate(true, 4, false, false), "netherite only");
        assertFalse(TotemRules.canActivate(true, 4, true, true), "already active");
    }

    @Test
    void effectIsLevelOneEvery80TicksFor340Ticks() {
        assertEquals(0, TotemRules.AMPLIFIER);
        assertEquals(80, TotemRules.EFFECT_INTERVAL);
        assertEquals(340, TotemRules.EFFECT_DURATION);
        assertTrue(TotemRules.appliesOn(0));
        assertTrue(TotemRules.appliesOn(160));
        assertFalse(TotemRules.appliesOn(79));
    }
}
