package io.mcmaster.someonesperfectend.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ShakeCurveTest {
    @Test
    void restsBeforeAndAfterTheShake() {
        assertEquals(0.0F, ShakeCurve.angle(-5, 0.0F));
        assertEquals(0.0F, ShakeCurve.angle(0, 0.0F));
        assertEquals(0.0F, ShakeCurve.angle(ShakeCurve.DURATION, 0.0F));
        assertEquals(0.0F, ShakeCurve.angle(ShakeCurve.DURATION + 20, 0.5F));
    }

    @Test
    void peaksAtTwelveDegreesOneTickIn() {
        assertEquals(12.0F, ShakeCurve.PEAK_DEGREES);
        assertEquals(ShakeCurve.PEAK_DEGREES, ShakeCurve.angle(1, 0.0F), 1.0E-4F);
        for (int step = 0; step <= ShakeCurve.DURATION * 20; step++) {
            float angle = ShakeCurve.angle(step / 20, (step % 20) / 20.0F);
            assertTrue(Math.abs(angle) <= ShakeCurve.PEAK_DEGREES + 1.0E-4F, "angle " + angle + " at step " + step);
        }
    }

    @Test
    void swingsBothWaysAndDampsOut() {
        float first = ShakeCurve.angle(1, 0.0F);
        float back = ShakeCurve.angle(3, 0.0F);
        float second = ShakeCurve.angle(5, 0.0F);
        float last = ShakeCurve.angle(9, 0.0F);
        assertTrue(first > 0.0F && back < 0.0F && second > 0.0F, first + " " + back + " " + second);
        assertTrue(
                Math.abs(back) < first && second < Math.abs(back) && last < second, back + " " + second + " " + last);
    }

    @Test
    void partialTicksInterpolateSmoothly() {
        float before = ShakeCurve.angle(0, 0.9F);
        float at = ShakeCurve.angle(1, 0.0F);
        float after = ShakeCurve.angle(1, 0.1F);
        assertTrue(Math.abs(at - before) < 1.0F && Math.abs(at - after) < 1.0F, before + " " + at + " " + after);
        assertEquals(ShakeCurve.angle(2, 0.5F), ShakeCurve.angle(2, 0.5F));
        assertTrue(ShakeCurve.angle(0, 0.5F) > 0.0F && ShakeCurve.angle(0, 0.5F) < at);
    }

    @Test
    void stareTrembleNeverDampsOut() {
        assertEquals(0.0F, ShakeCurve.stareAngle(-3, 0.5F));
        assertEquals(0.0F, ShakeCurve.stareAngle(0, 0.0F));
        for (long tick : new long[] {0, 2, 100, 10_000}) {
            assertEquals(ShakeCurve.STARE_DEGREES, ShakeCurve.stareAngle(tick, 0.5F), 1.0E-3F);
            assertEquals(-ShakeCurve.STARE_DEGREES, ShakeCurve.stareAngle(tick + 1, 0.5F), 1.0E-3F);
        }
        for (int step = 0; step <= 400; step++) {
            float angle = ShakeCurve.stareAngle(step / 20, (step % 20) / 20.0F);
            assertTrue(Math.abs(angle) <= ShakeCurve.STARE_DEGREES + 1.0E-4F, "angle " + angle + " at step " + step);
        }
    }
}
