package io.mcmaster.someonesperfectend.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ElevationBandsTest {
    private static final ElevationBands LAYOUT = ElevationBands.DEFAULT;
    private static final ElevationBands.CellRandom RANDOM =
            (cellX, cellZ, channel) -> ElevationBands.mix(12345.678, cellX, cellZ, channel);

    @Test
    void insideMinDistanceIsUnchanged() {
        for (int x = -1100; x <= 1100; x += 7) {
            for (int z = -1100; z <= 1100; z += 13) {
                if ((long) x * x + (long) z * z < 1024L * 1024L) {
                    assertEquals(0, LAYOUT.offsetAt(x, z, RANDOM), x + "," + z);
                }
            }
        }
    }

    @Test
    void ringNearMinDistanceStaysMiddleWithoutGaps() {
        for (int angle = 0; angle < 360; angle += 3) {
            for (int radius = 1000; radius <= 1300; radius += 10) {
                int x = (int) (Math.cos(Math.toRadians(angle)) * radius);
                int z = (int) (Math.sin(Math.toRadians(angle)) * radius);
                assertEquals(0, LAYOUT.offsetAt(x, z, RANDOM), x + "," + z);
            }
        }
    }

    @Test
    void outerAreaUsesAllThreeBandsInRoughThirds() {
        int[] counts = new int[4];
        int total = 0;
        for (int x = 4000; x < 36000; x += 97) {
            for (int z = -16000; z < 16000; z += 89) {
                int offset = LAYOUT.offsetAt(x, z, RANDOM);
                counts[offset == ElevationBands.GAP ? 3 : offset < 0 ? 0 : offset == 0 ? 1 : 2]++;
                total++;
            }
        }
        for (int band = 0; band < 3; band++) {
            double share = counts[band] / (double) total;
            assertTrue(share > 0.2 && share < 0.45, "band " + band + " share " + share);
        }
        double gaps = counts[3] / (double) total;
        assertTrue(gaps > 0.01 && gaps < 0.2, "gap share " + gaps);
    }

    @Test
    void offsetIsConstantAwayFromRegionBorders() {
        // Walking in small steps, the offset only changes by passing through a gap (islands are never sheared).
        for (int z = 2000; z < 12000; z += 500) {
            int previous = LAYOUT.offsetAt(3000, z, RANDOM);
            for (int x = 3001; x < 20000; x++) {
                int offset = LAYOUT.offsetAt(x, z, RANDOM);
                if (offset != previous && previous != ElevationBands.GAP && offset != ElevationBands.GAP) {
                    throw new AssertionError("Band changes without a gap at " + x + "," + z);
                }
                previous = offset;
            }
        }
    }

    @Test
    void gapsAreAtLeastGapWidthWide() {
        for (int z = 2000; z < 12000; z += 500) {
            int run = 0;
            int before = LAYOUT.offsetAt(3000, z, RANDOM);
            for (int x = 3001; x < 20000; x++) {
                int offset = LAYOUT.offsetAt(x, z, RANDOM);
                if (offset == ElevationBands.GAP) {
                    run++;
                } else {
                    if (run > 0 && before != ElevationBands.GAP && before != offset) {
                        // Crossing a border straight on gives the full width; at an angle the run is longer.
                        assertTrue(run >= LAYOUT.gapWidth() - 1, "gap of " + run + " at " + x + "," + z);
                    }
                    run = 0;
                    before = offset;
                }
            }
        }
    }

    @Test
    void offsetsMatchTheBands() {
        assertEquals(-20, LAYOUT.offset(ElevationBands.Band.LOWER));
        assertEquals(0, LAYOUT.offset(ElevationBands.Band.MIDDLE));
        assertEquals(40, LAYOUT.offset(ElevationBands.Band.UPPER));
        assertNotEquals(ElevationBands.GAP, LAYOUT.offset(ElevationBands.Band.UPPER));
    }

    @Test
    void mixIsInUnitInterval() {
        for (int i = 0; i < 1000; i++) {
            double v = ElevationBands.mix(i * 0.1, i, -i, i % 3);
            assertTrue(v >= 0 && v < 1);
        }
    }
}
