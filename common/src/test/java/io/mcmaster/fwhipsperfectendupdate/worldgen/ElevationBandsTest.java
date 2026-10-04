package io.mcmaster.fwhipsperfectendupdate.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import org.junit.jupiter.api.Test;

class ElevationBandsTest {
    private static final ElevationBands SETTINGS = ElevationBands.DEFAULT;
    private static final ElevationBands.CellRandom RANDOM =
            (cellX, cellZ, channel) -> ElevationBands.mix(12345.678, cellX, cellZ, channel);
    private static final ElevationBands.IslandCenters CENTERS = ElevationBandLayout.vanillaCenters(islandNoise(42L));
    /** Outer test area, in noise cells (8 blocks); positive so cell corners are every 8 blocks. */
    private static final int MIN_CELL = 2000 / 8;

    private static final int MAX_CELL = 6000 / 8;

    /** The {@code end_islands} noise of a world seed, as vanilla seeds it. */
    private static SimplexNoise islandNoise(long seed) {
        LegacyRandomSource random = new LegacyRandomSource(seed);
        random.consumeCount(17292);
        return new SimplexNoise(random);
    }

    private static ElevationBandLayout layout() {
        return SETTINGS.layout(CENTERS, RANDOM);
    }

    @Test
    void defaults() {
        assertEquals(1024, SETTINGS.minDistance());
        assertEquals(1184, SETTINGS.middleDistance());
        assertEquals(-32, SETTINGS.lowerOffset());
        assertEquals(56, SETTINGS.upperOffset());
    }

    @Test
    void insideMinDistanceIsUnchanged() {
        ElevationBandLayout layout = layout();
        for (int x = -1100; x <= 1100; x += 7) {
            for (int z = -1100; z <= 1100; z += 13) {
                if ((long) x * x + (long) z * z < 1024L * 1024L) {
                    assertEquals(0, layout.offsetAt(x, z), x + "," + z);
                }
            }
        }
    }

    /**
     * Islands reaching inside 1090 blocks have a center within the always-middle zone (vanilla islands are at
     * most about 89 blocks in radius), so the islands at the void ring and End Gateway exits stay vanilla.
     */
    @Test
    void islandsNearTheVoidRingStayMiddle() {
        ElevationBandLayout layout = layout();
        int islandColumns = 0;
        for (int angle = 0; angle < 360; angle++) {
            for (int radius = 1000; radius <= 1090; radius += 2) {
                int x = (int) (Math.cos(Math.toRadians(angle)) * radius);
                int z = (int) (Math.sin(Math.toRadians(angle)) * radius);
                if (layout.islandValue(x, z) > SETTINGS.islandThreshold()) {
                    islandColumns++;
                    assertEquals(0, layout.offsetAt(x, z), x + "," + z);
                }
            }
        }
        assertTrue(islandColumns > 0, "no island columns near the void ring");
    }

    /** MOD-47: groups with a center within 1184 blocks are middle band without jitter; groups past it move. */
    @Test
    void alwaysMiddleZoneEdge() {
        ElevationBandLayout layout = layout();
        int alwaysMiddle = 0;
        int outside = 0;
        for (int angle = 0; angle < 360; angle++) {
            for (int radius = 1024; radius <= 1600; radius += 4) {
                int x = (int) Math.round(Math.cos(Math.toRadians(angle)) * radius);
                int z = (int) Math.round(Math.sin(Math.toRadians(angle)) * radius);
                ElevationBandLayout.Group group = layout.groupAt(x, z);
                if (group == null || group == ElevationBandLayout.Group.VANILLA) {
                    continue;
                }
                boolean inside = group.distance() < SETTINGS.middleDistance();
                assertEquals(inside, group.alwaysMiddle(), x + "," + z);
                if (inside) {
                    assertEquals(ElevationBands.Band.MIDDLE, group.band());
                    assertEquals(0, group.offset(), x + "," + z);
                    alwaysMiddle++;
                } else if (group.offset() != 0) {
                    outside++;
                }
            }
        }
        assertTrue(alwaysMiddle > 0 && outside > 0, alwaysMiddle + " / " + outside);
    }

    /** MOD-71: every island column has a band (no void cut through it), and touching island columns share it. */
    @Test
    void islandsAreNeverSplit() {
        ElevationBandLayout layout = layout();
        int islandCorners = 0;
        int seams = 0;
        for (int cellX = MIN_CELL; cellX < MAX_CELL; cellX++) {
            for (int cellZ = MIN_CELL; cellZ < MAX_CELL; cellZ++) {
                int offset = layout.offsetAt(cellX * 8, cellZ * 8);
                seams += offset == ElevationBands.GAP ? 1 : 0;
                if (!isIsland(layout, cellX, cellZ)) {
                    continue;
                }
                islandCorners++;
                assertNotEquals(
                        ElevationBands.GAP, offset, "void cuts through an island at cell " + cellX + "," + cellZ);
                // Neighboring island corners (the 8-block cells between them) move together.
                int[][] neighbors = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};
                for (int[] d : neighbors) {
                    if (isIsland(layout, cellX + d[0], cellZ + d[1])) {
                        assertEquals(
                                offset,
                                layout.offsetAt((cellX + d[0]) * 8, (cellZ + d[1]) * 8),
                                "island sheared between cells " + cellX + "," + cellZ + " and " + (cellX + d[0]) + ","
                                        + (cellZ + d[1]));
                    }
                }
            }
        }
        assertTrue(islandCorners > 10_000, "too few island corners: " + islandCorners);
        assertTrue(seams > 0, "no seams between island groups with different bands");
    }

    /** Islands extend a little past the threshold through the 3D noise; seams stay clear of that fringe too. */
    @Test
    void seamsKeepClearOfIslandEdges() {
        ElevationBandLayout layout = layout();
        for (int cellX = MIN_CELL; cellX < MAX_CELL; cellX++) {
            for (int cellZ = MIN_CELL; cellZ < MAX_CELL; cellZ++) {
                if (layout.offsetAt(cellX * 8, cellZ * 8) == ElevationBands.GAP) {
                    float value = layout.islandValue(cellX * 8, cellZ * 8);
                    // end_islands = (value - 8) / 128: at least 0.0625 below an island's vanilla edge.
                    assertTrue(value <= 0, "seam at island value " + value + " at cell " + cellX + "," + cellZ);
                }
            }
        }
    }

    @Test
    void outerIslandsUseAllThreeBands() {
        ElevationBandLayout layout = layout();
        int[] counts = new int[3];
        int total = 0;
        for (int cellX = MIN_CELL; cellX < MAX_CELL * 2; cellX += 2) {
            for (int cellZ = -MAX_CELL; cellZ < MAX_CELL; cellZ += 2) {
                if (!isIsland(layout, cellX, cellZ)) {
                    continue;
                }
                ElevationBandLayout.Group group = layout.groupAt(cellX * 8, cellZ * 8);
                counts[group.band().ordinal()]++;
                total++;
            }
        }
        for (int band = 0; band < 3; band++) {
            double share = counts[band] / (double) total;
            assertTrue(share > 0.15 && share < 0.55, "band " + band + " share " + share);
        }
    }

    /** A group's band does not depend on which of its columns is looked up first (or on other lookups). */
    @Test
    void bandsDoNotDependOnLookupOrder() {
        ElevationBandLayout forward = layout();
        ElevationBandLayout backward = layout();
        int[] offsets = new int[(MAX_CELL - MIN_CELL) * 64];
        for (int i = 0; i < offsets.length; i++) {
            offsets[i] = forward.offsetAt((MIN_CELL + i / 64) * 8, (MIN_CELL + i % 64) * 8);
        }
        for (int i = offsets.length - 1; i >= 0; i--) {
            assertEquals(offsets[i], backward.offsetAt((MIN_CELL + i / 64) * 8, (MIN_CELL + i % 64) * 8));
        }
    }

    @Test
    void offsetScalesWithGroupDistance() {
        assertEquals(1.0, SETTINGS.scale(1184), 1e-9);
        assertEquals(-32, SETTINGS.offset(ElevationBands.Band.LOWER, 1184));
        assertEquals(56, SETTINGS.offset(ElevationBands.Band.UPPER, 1184));
        // s = 1 + 0.5 × 2816 / 6816 ≈ 1.2066
        assertEquals(1 + 0.5 * 2816 / 6816, SETTINGS.scale(4000), 1e-9);
        assertEquals(-39, SETTINGS.offset(ElevationBands.Band.LOWER, 4000));
        assertEquals(68, SETTINGS.offset(ElevationBands.Band.UPPER, 4000));
        assertEquals(1.5, SETTINGS.scale(8000), 1e-9);
        assertEquals(-48, SETTINGS.offset(ElevationBands.Band.LOWER, 8000));
        assertEquals(84, SETTINGS.offset(ElevationBands.Band.UPPER, 8000));
        assertEquals(1.5, SETTINGS.scale(12000), 1e-9);
        assertEquals(-48, SETTINGS.offset(ElevationBands.Band.LOWER, 12000));
        assertEquals(84, SETTINGS.offset(ElevationBands.Band.UPPER, 12000));
        assertEquals(1.0, SETTINGS.scale(0), 1e-9);
        for (double d : new double[] {1184, 4000, 8000, 12000}) {
            assertEquals(0, SETTINGS.offset(ElevationBands.Band.MIDDLE, d));
        }
    }

    @Test
    void jitterAmplitudeGrowsFromTheZoneEdgeAndIsCapped() {
        assertEquals(6, SETTINGS.jitterAmplitude(1184));
        // (6 + 6 × 0.4131) × 1.2066 ≈ 10.2
        assertEquals(10, SETTINGS.jitterAmplitude(4000));
        assertEquals(18, SETTINGS.jitterAmplitude(8000));
        assertEquals(18, SETTINGS.jitterAmplitude(12000));
        int previous = 0;
        for (int d = 1184; d <= 12000; d += 16) {
            int amplitude = SETTINGS.jitterAmplitude(d);
            assertTrue(
                    amplitude >= previous && amplitude >= 6 && amplitude <= 18, "amplitude " + amplitude + " at " + d);
            previous = amplitude;
        }
    }

    /** A group's offset is its band offset plus at most the jitter amplitude, snapped to the 4-block noise cell. */
    @Test
    void groupOffsetsAreSnappedAndStayWithinTheJitter() {
        for (ElevationBands.Band band : ElevationBands.Band.values()) {
            for (int d = 1184; d <= 12000; d += 97) {
                int base = SETTINGS.offset(band, d);
                int amplitude = SETTINGS.jitterAmplitude(d);
                for (double jitter = -3; jitter <= 3; jitter += 0.05) {
                    int offset = SETTINGS.groupOffset(band, d, jitter);
                    assertEquals(0, offset % ElevationBands.CELL_HEIGHT, band + " at " + d + ": " + offset);
                    assertTrue(
                            Math.abs(offset - base) <= amplitude + ElevationBands.CELL_HEIGHT / 2,
                            band + " at " + d + ": " + offset + " vs " + base);
                    assertTrue(offset >= ElevationBands.MIN_OFFSET && offset <= ElevationBands.MAX_OFFSET);
                }
            }
        }
        assertEquals(-40, SETTINGS.groupOffset(ElevationBands.Band.LOWER, 4000, 0));
        assertEquals(68, SETTINGS.groupOffset(ElevationBands.Band.UPPER, 4000, 0));
        assertEquals(104, SETTINGS.groupOffset(ElevationBands.Band.UPPER, 8000, 1));
        assertEquals(-48, SETTINGS.groupOffset(ElevationBands.Band.LOWER, 8000, -1));
    }

    /** MOD-47: islands in one band sit at different heights. */
    @Test
    void groupsInOneBandVaryInHeight() {
        ElevationBandLayout layout = layout();
        List<Set<Integer>> offsets = List.of(new HashSet<>(), new HashSet<>(), new HashSet<>());
        for (int cellX = MIN_CELL; cellX < MAX_CELL; cellX += 2) {
            for (int cellZ = MIN_CELL; cellZ < MAX_CELL; cellZ += 2) {
                ElevationBandLayout.Group group = layout.groupAt(cellX * 8, cellZ * 8);
                if (group != null && !group.alwaysMiddle() && isIsland(layout, cellX, cellZ)) {
                    offsets.get(group.band().ordinal()).add(group.offset());
                }
            }
        }
        for (int band = 0; band < 3; band++) {
            assertTrue(offsets.get(band).size() >= 3, "band " + band + " offsets " + offsets.get(band));
        }
    }

    @Test
    void clampKeepsTerrainInsideTheBuildHeight() {
        assertEquals(-48, ElevationBands.MIN_OFFSET);
        assertEquals(116, ElevationBands.MAX_OFFSET);
        assertEquals(ElevationBands.MIN_OFFSET, ElevationBands.clampOffset(-66));
        assertEquals(ElevationBands.MAX_OFFSET, ElevationBands.clampOffset(500));
        assertEquals(-40, ElevationBands.clampOffset(-40));
        assertEquals(104, ElevationBands.clampOffset(104));
        // Vanilla outer terrain tops out at Y=66: the highest moved top stays below the 192-block noise height.
        assertTrue(72 + ElevationBands.MAX_OFFSET <= 191);
        assertEquals(0, ElevationBands.MIN_OFFSET % ElevationBands.CELL_HEIGHT);
        assertEquals(0, ElevationBands.MAX_OFFSET % ElevationBands.CELL_HEIGHT);
    }

    @Test
    void mixIsInUnitInterval() {
        for (int i = 0; i < 1000; i++) {
            double v = ElevationBands.mix(i * 0.1, i, -i, i % 3);
            assertTrue(v >= 0 && v < 1);
        }
    }

    /** Part of an island by vanilla's own {@code end_islands} edge (value 8, where the density crosses 0). */
    private static boolean isIsland(ElevationBandLayout layout, int cellX, int cellZ) {
        return layout.islandValue(cellX * 8, cellZ * 8) > 8;
    }
}
