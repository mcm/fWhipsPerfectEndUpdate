package io.mcmaster.someonesperfectend.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void islandsNearTheVoidRingStayMiddle() {
        ElevationBandLayout layout = layout();
        for (int angle = 0; angle < 360; angle += 2) {
            for (int radius = 1000; radius <= 1300; radius += 8) {
                int x = (int) (Math.cos(Math.toRadians(angle)) * radius);
                int z = (int) (Math.sin(Math.toRadians(angle)) * radius);
                assertEquals(0, layout.offsetAt(x, z), x + "," + z);
            }
        }
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
                int offset = layout.offsetAt(cellX * 8, cellZ * 8);
                counts[offset < 0 ? 0 : offset == 0 ? 1 : 2]++;
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
    void offsetsMatchTheBands() {
        assertEquals(-20, SETTINGS.offset(ElevationBands.Band.LOWER));
        assertEquals(0, SETTINGS.offset(ElevationBands.Band.MIDDLE));
        assertEquals(40, SETTINGS.offset(ElevationBands.Band.UPPER));
        assertNotEquals(ElevationBands.GAP, SETTINGS.offset(ElevationBands.Band.UPPER));
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
