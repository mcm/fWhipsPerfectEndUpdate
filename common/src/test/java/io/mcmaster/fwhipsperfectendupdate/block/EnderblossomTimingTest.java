package io.mcmaster.fwhipsperfectendupdate.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EnderblossomTimingTest {
    @Test
    void phaseIsTheLowThreeBitsOfTheHash() {
        assertEquals(0, EnderblossomTiming.phase(0));
        assertEquals(5, EnderblossomTiming.phase(13));
        assertEquals(7, EnderblossomTiming.phase(-1));
        for (int hash = -100; hash < 100; hash++) {
            int phase = EnderblossomTiming.phase(hash);
            assertTrue(phase >= 0 && phase <= 7, "phase " + phase + " for hash " + hash);
        }
    }

    @Test
    void firstCheckLandsOnThePhaseSlotWithinOneInterval() {
        for (int phase = 0; phase < 8; phase++) {
            for (long time = 0; time < 40; time++) {
                int delay = EnderblossomTiming.nextCheckDelay(time, phase);
                assertTrue(delay >= 1 && delay <= EnderblossomTiming.CHECK_INTERVAL, "delay " + delay);
                assertEquals(0, (time + delay + phase) % EnderblossomTiming.CHECK_INTERVAL);
            }
        }
    }

    @Test
    void checksRepeatEveryTenTicks() {
        for (int phase = 0; phase < 8; phase++) {
            long time = 1234 + EnderblossomTiming.nextCheckDelay(1234, phase);
            for (int i = 0; i < 5; i++) {
                int delay = EnderblossomTiming.nextCheckDelay(time, phase);
                assertEquals(EnderblossomTiming.CHECK_INTERVAL, delay);
                time += delay;
            }
        }
    }

    @Test
    void neighborsGetDifferentPhases() {
        // Positions hash differently, so blossoms side by side don't all check together.
        assertTrue(EnderblossomTiming.phase(1) != EnderblossomTiming.phase(2));
    }

    @Test
    void rippleDelaysScaleWithDistanceLikeVanilla() {
        assertEquals(5, EnderblossomTiming.minRippleDelay(1.0));
        assertEquals(10, EnderblossomTiming.maxRippleDelay(1.0));
        assertEquals(10, EnderblossomTiming.minRippleDelay(2.0));
        assertEquals(20, EnderblossomTiming.maxRippleDelay(2.0));
        assertEquals(7, EnderblossomTiming.minRippleDelay(Math.sqrt(2)));
        assertEquals(14, EnderblossomTiming.maxRippleDelay(Math.sqrt(2)));
    }
}
