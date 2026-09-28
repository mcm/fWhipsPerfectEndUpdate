package io.mcmaster.someonesperfectend.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ElevationBandsTest {
    private static final ElevationBands LAYOUT = ElevationBands.DEFAULT;
    private static final ElevationBands.CellRandom RANDOM =
            (cellX, cellZ, channel) -> ElevationBands.mix(12345.678, cellX, cellZ, channel);
    /** A smooth stand-in for the jitter noise, about 64 blocks per feature. */
    private static final ElevationBands.Jitter WAVES =
            (x, z) -> Math.sin(x * Math.PI / 64) * Math.cos(z * Math.PI / 64);

    @Test
    void defaults() {
        assertEquals(160, LAYOUT.regionSize());
        assertEquals(16, LAYOUT.gapWidth());
        assertEquals(1024, LAYOUT.minDistance());
    }

    @Test
    void insideMinDistanceIsUnchanged() {
        for (int x = -1100; x <= 1100; x += 7) {
            for (int z = -1100; z <= 1100; z += 13) {
                if ((long) x * x + (long) z * z < 1024L * 1024L) {
                    assertEquals(0, LAYOUT.offsetAt(x, z, RANDOM, WAVES), x + "," + z);
                }
            }
        }
    }

    @Test
    void offsetScalesWithRegionPointDistance() {
        assertEquals(1.0, LAYOUT.scale(1184), 1e-9);
        assertEquals(-32, LAYOUT.offset(ElevationBands.Band.LOWER, 1184));
        assertEquals(56, LAYOUT.offset(ElevationBands.Band.UPPER, 1184));
        // s = 1 + 0.5 × 2816 / 6816 ≈ 1.2066
        assertEquals(1 + 0.5 * 2816 / 6816, LAYOUT.scale(4000), 1e-9);
        assertEquals(-39, LAYOUT.offset(ElevationBands.Band.LOWER, 4000));
        assertEquals(68, LAYOUT.offset(ElevationBands.Band.UPPER, 4000));
        assertEquals(1.5, LAYOUT.scale(8000), 1e-9);
        assertEquals(-48, LAYOUT.offset(ElevationBands.Band.LOWER, 8000));
        assertEquals(84, LAYOUT.offset(ElevationBands.Band.UPPER, 8000));
        assertEquals(1.5, LAYOUT.scale(12000), 1e-9);
        assertEquals(-48, LAYOUT.offset(ElevationBands.Band.LOWER, 12000));
        assertEquals(84, LAYOUT.offset(ElevationBands.Band.UPPER, 12000));
        assertEquals(1.0, LAYOUT.scale(0), 1e-9);
        for (double d : new double[] {1184, 4000, 8000, 12000}) {
            assertEquals(0, LAYOUT.offset(ElevationBands.Band.MIDDLE, d));
        }
    }

    @Test
    void jitterAmplitudeGrowsFromTheZoneEdgeAndIsCapped() {
        assertEquals(6, LAYOUT.jitterAmplitude(1184));
        // (6 + 6 × 0.4131) × 1.2066 ≈ 10.2
        assertEquals(10, LAYOUT.jitterAmplitude(4000));
        assertEquals(18, LAYOUT.jitterAmplitude(8000));
        assertEquals(18, LAYOUT.jitterAmplitude(12000));
        int previous = 0;
        for (int d = 1184; d <= 12000; d += 16) {
            int amplitude = LAYOUT.jitterAmplitude(d);
            assertTrue(amplitude >= previous && amplitude >= 6 && amplitude <= 18, "amplitude " + amplitude + " at " + d);
            previous = amplitude;
        }
    }

    @Test
    void jitterStaysWithinItsAmplitude() {
        // Even a noise far outside [-1, 1] moves a column by at most the amplitude, in whole blocks.
        ElevationBands.Jitter wild = (x, z) -> 3 * Math.sin(x * 0.05) + 2 * Math.cos(z * 0.07);
        int moved = 0;
        for (int x = 1500; x < 14000; x += 37) {
            for (int z = -3000; z < 3000; z += 41) {
                ElevationBands.Region region = LAYOUT.regionAt(x, z, RANDOM);
                int offset = LAYOUT.offsetAt(x, z, RANDOM, wild);
                if (region == null) {
                    assertEquals(ElevationBands.GAP, offset);
                    continue;
                }
                int base = LAYOUT.offset(region.band(), region.distance());
                int amplitude = LAYOUT.jitterAmplitude(region.distance());
                assertTrue(Math.abs(offset - base) <= amplitude, x + "," + z + ": " + offset + " vs " + base);
                moved += offset != base ? 1 : 0;
            }
        }
        assertTrue(moved > 0, "jitter never moved a column");
    }

    @Test
    void jitterVariesInsideARegion() {
        int columns = 0;
        int varied = 0;
        for (int z = -3000; z < 3000; z += 211) {
            ElevationBands.Region first = LAYOUT.regionAt(6000, z, RANDOM);
            if (first == null) {
                continue;
            }
            Set<Integer> offsets = new HashSet<>();
            for (int x = 6000; x < 6064; x += 4) {
                if (first.equals(LAYOUT.regionAt(x, z, RANDOM))) {
                    offsets.add(LAYOUT.offsetAt(x, z, RANDOM, WAVES));
                }
            }
            columns++;
            varied += offsets.size() > 1 ? 1 : 0;
        }
        assertTrue(varied > columns / 2, varied + " of " + columns + " rows vary");
    }

    @Test
    void alwaysMiddleZoneEdge() {
        int alwaysMiddle = 0;
        int outside = 0;
        for (int angle = 0; angle < 360; angle += 2) {
            for (int radius = 1024; radius <= 1600; radius += 4) {
                int x = (int) Math.round(Math.cos(Math.toRadians(angle)) * radius);
                int z = (int) Math.round(Math.sin(Math.toRadians(angle)) * radius);
                long squared = (long) x * x + (long) z * z;
                ElevationBands.Region region = LAYOUT.regionAt(x, z, RANDOM);
                int offset = LAYOUT.offsetAt(x, z, RANDOM, WAVES);
                if (squared < 1040L * 1040L) {
                    // The interpolation margin past minDistance stays vanilla.
                    assertEquals(0, offset, x + "," + z);
                    continue;
                }
                if (region == null) {
                    assertEquals(ElevationBands.GAP, offset);
                    continue;
                }
                // Regions whose point is inside minDistance + regionSize are middle band without jitter.
                boolean inside = region.distance() < 1024 + 160;
                assertEquals(inside, region.alwaysMiddle(), x + "," + z);
                if (inside) {
                    assertEquals(ElevationBands.Band.MIDDLE, region.band());
                    assertEquals(0, offset, x + "," + z);
                    alwaysMiddle++;
                } else {
                    outside++;
                    if (squared < (1040L + 16) * (1040L + 16)) {
                        throw new AssertionError("Moved region touches the vanilla circle at " + x + "," + z);
                    }
                }
            }
        }
        assertTrue(alwaysMiddle > 0 && outside > 0, alwaysMiddle + " / " + outside);
    }

    @Test
    void outerAreaUsesAllThreeBandsInRoughThirds() {
        int[] counts = new int[4];
        int total = 0;
        for (int x = 4000; x < 36000; x += 97) {
            for (int z = -16000; z < 16000; z += 89) {
                ElevationBands.Region region = LAYOUT.regionAt(x, z, RANDOM);
                counts[region == null ? 3 : region.band().ordinal()]++;
                total++;
            }
        }
        for (int band = 0; band < 3; band++) {
            double share = counts[band] / (double) total;
            assertTrue(share > 0.2 && share < 0.45, "band " + band + " share " + share);
        }
        double gaps = counts[3] / (double) total;
        assertTrue(gaps > 0.01 && gaps < 0.25, "gap share " + gaps);
    }

    @Test
    void bandChangesOnlyThroughAGap() {
        // Walking in small steps, the band only changes by passing through a gap (islands are never sheared).
        for (int z = 2000; z < 12000; z += 500) {
            ElevationBands.Region previous = LAYOUT.regionAt(3000, z, RANDOM);
            for (int x = 3001; x < 20000; x++) {
                ElevationBands.Region region = LAYOUT.regionAt(x, z, RANDOM);
                if (region != null && previous != null && region.band() != previous.band()) {
                    throw new AssertionError("Band changes without a gap at " + x + "," + z);
                }
                previous = region;
            }
        }
    }

    @Test
    void gapsAreForcedToVoidAndAtLeastGapWidthWide() {
        int gaps = 0;
        for (int z = 2000; z < 12000; z += 500) {
            int run = 0;
            ElevationBands.Region before = LAYOUT.regionAt(3000, z, RANDOM);
            for (int x = 3001; x < 20000; x++) {
                ElevationBands.Region region = LAYOUT.regionAt(x, z, RANDOM);
                if (region == null) {
                    assertEquals(ElevationBands.GAP, LAYOUT.offsetAt(x, z, RANDOM, WAVES), x + "," + z);
                    run++;
                } else {
                    assertTrue(LAYOUT.offsetAt(x, z, RANDOM, WAVES) != ElevationBands.GAP);
                    if (run > 0 && before != null && before.band() != region.band()) {
                        // Crossing a border straight on gives the full width; at an angle the run is longer.
                        assertTrue(run >= LAYOUT.gapWidth() - 1, "gap of " + run + " at " + x + "," + z);
                        gaps++;
                    }
                    run = 0;
                    before = region;
                }
            }
        }
        assertTrue(gaps > 10, "only " + gaps + " gaps crossed");
    }

    @Test
    void clampKeepsTerrainInsideTheBuildHeight() {
        assertEquals(-48, ElevationBands.MIN_OFFSET);
        assertEquals(119, ElevationBands.MAX_OFFSET);
        assertEquals(ElevationBands.MIN_OFFSET, ElevationBands.clampOffset(-66));
        assertEquals(ElevationBands.MAX_OFFSET, ElevationBands.clampOffset(500));
        assertEquals(-40, ElevationBands.clampOffset(-40));
        assertEquals(102, ElevationBands.clampOffset(102));
        // Vanilla outer terrain tops out at Y=66: the highest moved top stays below the 192-block noise height.
        assertTrue(72 + ElevationBands.MAX_OFFSET <= 191);
        ElevationBands.Jitter lowest = (x, z) -> -5;
        for (int x = 8000; x < 20000; x += 53) {
            for (int z = -2000; z < 2000; z += 59) {
                int offset = LAYOUT.offsetAt(x, z, RANDOM, lowest);
                if (offset != ElevationBands.GAP) {
                    assertFalse(offset < ElevationBands.MIN_OFFSET || offset > ElevationBands.MAX_OFFSET);
                }
            }
        }
    }

    @Test
    void mixIsInUnitInterval() {
        for (int i = 0; i < 1000; i++) {
            double v = ElevationBands.mix(i * 0.1, i, -i, i % 3);
            assertTrue(v >= 0 && v < 1);
        }
    }
}
