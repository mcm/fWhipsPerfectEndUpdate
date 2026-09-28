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

    @Test
    void totemStacksOneLevelAboveTheBeacon() {
        assertEquals(0, TotemRules.amplifier(TotemRules.NO_BEACON), "no beacon: level I");
        assertEquals(1, TotemRules.amplifier(0), "beacon level I: level II");
        assertEquals(2, TotemRules.amplifier(1), "beacon level II: level III");
    }

    @Test
    void stackedLevelNeverOutlastsTheBeaconEffect() {
        assertEquals(40, TotemRules.stackedDuration(40));
        assertEquals(340, TotemRules.stackedDuration(1000));
        assertEquals(0, TotemRules.stackedDuration(-5));
    }
}
