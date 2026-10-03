package io.mcmaster.someonesperfectend.infuser;

import static io.mcmaster.someonesperfectend.infuser.InfusionEffect.HASTE;
import static io.mcmaster.someonesperfectend.infuser.InfusionEffect.JUMP_BOOST;
import static io.mcmaster.someonesperfectend.infuser.InfusionEffect.REGENERATION;
import static io.mcmaster.someonesperfectend.infuser.InfusionEffect.RESISTANCE;
import static io.mcmaster.someonesperfectend.infuser.InfusionEffect.SPEED;
import static io.mcmaster.someonesperfectend.infuser.InfusionEffect.STRENGTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mcmaster.someonesperfectend.infuser.InfusionRules.Charge;
import io.mcmaster.someonesperfectend.infuser.InfusionRules.Current;
import io.mcmaster.someonesperfectend.infuser.InfusionRules.Kind;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class InfusionRulesTest {
    private static final Set<Kind> BOOTS = EnumSet.of(Kind.FEET);
    private static final Set<Kind> CHESTPLATE = EnumSet.of(Kind.CHEST);
    private static final Set<Kind> PICKAXE = EnumSet.of(Kind.MINING);
    private static final Set<Kind> SWORD = EnumSet.of(Kind.WEAPON);
    private static final Set<Kind> AXE = EnumSet.of(Kind.MINING, Kind.WEAPON);

    @Test
    void allowedEffectsPerItemType() {
        assertEquals(List.of(SPEED, JUMP_BOOST), InfusionRules.allowedEffects(BOOTS));
        assertEquals(List.of(RESISTANCE, REGENERATION), InfusionRules.allowedEffects(CHESTPLATE));
        assertEquals(List.of(HASTE), InfusionRules.allowedEffects(PICKAXE));
        assertEquals(List.of(STRENGTH), InfusionRules.allowedEffects(SWORD));
        assertEquals(List.of(HASTE, STRENGTH), InfusionRules.allowedEffects(AXE));
        assertEquals(List.of(), InfusionRules.allowedEffects(EnumSet.noneOf(Kind.class)));
        assertFalse(InfusionRules.canSelect(BOOTS, null, HASTE, 4));
        assertFalse(InfusionRules.canSelect(PICKAXE, null, STRENGTH, 4));
        assertFalse(InfusionRules.canSelect(SWORD, null, SPEED, 4));
    }

    @Test
    void tierGating() {
        assertFalse(InfusionRules.canSelect(BOOTS, null, SPEED, 0));
        assertTrue(InfusionRules.canSelect(BOOTS, null, SPEED, 1));
        assertTrue(InfusionRules.canSelect(PICKAXE, null, HASTE, 1));
        assertFalse(InfusionRules.canSelect(BOOTS, null, JUMP_BOOST, 1));
        assertTrue(InfusionRules.canSelect(BOOTS, null, JUMP_BOOST, 2));
        assertFalse(InfusionRules.canSelect(CHESTPLATE, null, RESISTANCE, 1));
        assertTrue(InfusionRules.canSelect(CHESTPLATE, null, RESISTANCE, 2));
        assertFalse(InfusionRules.canSelect(SWORD, null, STRENGTH, 2));
        assertTrue(InfusionRules.canSelect(SWORD, null, STRENGTH, 3));
        assertFalse(InfusionRules.canSelect(CHESTPLATE, null, REGENERATION, 3));
        assertTrue(InfusionRules.canSelect(CHESTPLATE, null, REGENERATION, 4));
        assertEquals(Optional.empty(), InfusionRules.charge(SWORD, null, STRENGTH, 2));
    }

    @Test
    void levelIsTwoOnlyAtTierFourAndNeverForRegeneration() {
        for (InfusionEffect effect : List.of(SPEED, HASTE, RESISTANCE, JUMP_BOOST, STRENGTH)) {
            assertEquals(1, InfusionRules.amplifierFor(effect, 4), effect.name());
            assertEquals(0, InfusionRules.amplifierFor(effect, 3), effect.name());
        }
        assertEquals(0, InfusionRules.amplifierFor(SPEED, 1));
        assertEquals(0, InfusionRules.amplifierFor(REGENERATION, 4));
        assertEquals(
                new Charge(REGENERATION, 0, 340),
                InfusionRules.charge(CHESTPLATE, null, REGENERATION, 4).orElseThrow());
        assertEquals(
                new Charge(HASTE, 1, 340),
                InfusionRules.charge(PICKAXE, null, HASTE, 4).orElseThrow());
        assertEquals(
                new Charge(HASTE, 0, 220),
                InfusionRules.charge(PICKAXE, null, HASTE, 1).orElseThrow());
    }

    @Test
    void chargeAndCap() {
        assertEquals(220, InfusionRules.chargeTicks(1));
        assertEquals(340, InfusionRules.chargeTicks(4));
        assertEquals(1088 * 20, InfusionRules.capTicks(4));
        assertEquals(704 * 20, InfusionRules.capTicks(1));

        // Adds one payment's worth.
        Current current = new Current(SPEED, 0, 1000);
        assertEquals(
                1220,
                InfusionRules.charge(BOOTS, current, SPEED, 1).orElseThrow().remainingTicks());
        // Clamped to the cap.
        int cap = InfusionRules.capTicks(1);
        assertEquals(
                cap,
                InfusionRules.charge(BOOTS, new Current(SPEED, 0, cap - 10), SPEED, 1)
                        .orElseThrow()
                        .remainingTicks());
        // At or above the cap: no charge.
        assertEquals(Optional.empty(), InfusionRules.charge(BOOTS, new Current(SPEED, 0, cap), SPEED, 1));
        // Charged at tier 4 above the tier-1 cap: a tier-1 charge can't add (and never reduces).
        Current tierFourCharged = new Current(SPEED, 1, InfusionRules.capTicks(4));
        assertEquals(Optional.empty(), InfusionRules.charge(BOOTS, tierFourCharged, SPEED, 1));
        // 64 payments fill an empty item exactly to the cap.
        Current filling = null;
        for (int i = 0; i < 64; i++) {
            Charge charge = InfusionRules.charge(PICKAXE, filling, HASTE, 4).orElseThrow();
            filling = new Current(charge.effect(), charge.amplifier(), charge.remainingTicks());
        }
        assertEquals(InfusionRules.capTicks(4), filling.remainingTicks());
        assertEquals(Optional.empty(), InfusionRules.charge(PICKAXE, filling, HASTE, 4));
    }

    @Test
    void reInfusionKeepsEffectAndLevel() {
        Current speedTwo = new Current(SPEED, 1, 500);
        assertFalse(InfusionRules.canSelect(BOOTS, speedTwo, JUMP_BOOST, 4));
        assertEquals(Optional.empty(), InfusionRules.charge(BOOTS, speedTwo, JUMP_BOOST, 4));
        // Topping up at tier 1 keeps level II.
        assertEquals(
                new Charge(SPEED, 1, 720),
                InfusionRules.charge(BOOTS, speedTwo, SPEED, 1).orElseThrow());
        // Topping up a level I infusion at tier 4 keeps level I.
        Current hasteOne = new Current(HASTE, 0, 100);
        assertEquals(
                new Charge(HASTE, 0, 440),
                InfusionRules.charge(AXE, hasteOne, HASTE, 4).orElseThrow());
        assertFalse(InfusionRules.canSelect(AXE, hasteOne, STRENGTH, 4));
        // An infusion with an effect gear can't normally take can't be charged at all.
        Current unknown = new Current(null, 0, 100);
        for (InfusionEffect effect : InfusionEffect.values()) {
            assertFalse(InfusionRules.canSelect(AXE, unknown, effect, 4));
        }
    }

    @Test
    void formatsTime() {
        assertEquals("18:08", InfusionRules.formatTime(InfusionRules.capTicks(4)));
        assertEquals("11:44", InfusionRules.formatTime(InfusionRules.capTicks(1)));
        assertEquals("0:05", InfusionRules.formatTime(119));
        assertEquals("0:00", InfusionRules.formatTime(0));
    }

    @Test
    void gearPausesUnderAnEqualOrStrongerBeacon() {
        assertFalse(InfusionRules.pausedByBeacon(0, -1), "no beacon effect");
        assertTrue(InfusionRules.pausedByBeacon(0, 0), "same level");
        assertTrue(InfusionRules.pausedByBeacon(0, 1), "beacon higher");
        assertFalse(InfusionRules.pausedByBeacon(1, 0), "gear higher");
    }

    @Test
    void gearDepletesTwentyTicksPerUpdate() {
        assertEquals(180, InfusionRules.deplete(200));
        assertEquals(0, InfusionRules.deplete(20));
        assertEquals(0, InfusionRules.deplete(5));
    }
}
