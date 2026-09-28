package io.mcmaster.someonesperfectend.worldgen;

/**
 * Registry-free layout math for the outer-End elevation bands (US-012).
 *
 * <p>The outer End is split into Voronoi regions about {@code regionSize} blocks across. Each region gets one
 * band (lower, middle, or upper), and every island in it is moved up or down by that band's offset as a whole.
 * Where two neighboring regions have different bands, a strip {@code gapWidth} blocks wide along their border
 * is forced to void, so an island is split there instead of being sheared into a cliff.
 *
 * <p>Within {@code minDistance} blocks of the origin (the central island and the void ring) nothing changes.
 * Regions whose point lies within {@code minDistance + 2 × regionSize} are always middle band, so islands
 * crossing the {@code minDistance} circle keep their vanilla height and the vanilla area has no gaps.
 */
public record ElevationBands(int lowerOffset, int upperOffset, int regionSize, int gapWidth, int minDistance) {
    /** Returned by {@link #offsetAt} for positions in a void gap between regions. */
    public static final int GAP = Integer.MIN_VALUE;

    public static final ElevationBands DEFAULT = new ElevationBands(-20, 40, 320, 24, 1024);

    public enum Band {
        LOWER, MIDDLE, UPPER
    }

    /** Seeded random numbers in [0, 1) per region cell; {@code channel} 0 and 1 jitter the point, 2 picks the band. */
    @FunctionalInterface
    public interface CellRandom {
        double value(int cellX, int cellZ, int channel);
    }

    /** Vertical offset of the terrain at column (x, z), or {@link #GAP} if the column must be void. */
    public int offsetAt(int x, int z, CellRandom random) {
        if ((long) x * x + (long) z * z < (long) minDistance * minDistance) {
            return 0;
        }
        int baseX = Math.floorDiv(x, regionSize);
        int baseZ = Math.floorDiv(z, regionSize);
        double[] pointX = new double[9];
        double[] pointZ = new double[9];
        double[] distance = new double[9];
        int[] cellX = new int[9];
        int[] cellZ = new int[9];
        int nearest = 0;
        for (int i = 0; i < 9; i++) {
            cellX[i] = baseX + i % 3 - 1;
            cellZ[i] = baseZ + i / 3 - 1;
            pointX[i] = (cellX[i] + random.value(cellX[i], cellZ[i], 0)) * regionSize;
            pointZ[i] = (cellZ[i] + random.value(cellX[i], cellZ[i], 1)) * regionSize;
            distance[i] = (pointX[i] - x) * (pointX[i] - x) + (pointZ[i] - z) * (pointZ[i] - z);
            if (distance[i] < distance[nearest]) {
                nearest = i;
            }
        }
        Band band = band(cellX[nearest], cellZ[nearest], pointX[nearest], pointZ[nearest], random);
        for (int i = 0; i < 9; i++) {
            if (i == nearest) {
                continue;
            }
            double dx = pointX[i] - pointX[nearest];
            double dz = pointZ[i] - pointZ[nearest];
            // Distance from (x, z) to the border (bisector) between the nearest region and region i.
            double border = (distance[i] - distance[nearest]) / (2 * Math.sqrt(dx * dx + dz * dz));
            if (border < gapWidth / 2.0 && band(cellX[i], cellZ[i], pointX[i], pointZ[i], random) != band) {
                return GAP;
            }
        }
        return offset(band);
    }

    public int offset(Band band) {
        return switch (band) {
            case LOWER -> lowerOffset;
            case MIDDLE -> 0;
            case UPPER -> upperOffset;
        };
    }

    private Band band(int cellX, int cellZ, double pointX, double pointZ, CellRandom random) {
        double limit = minDistance + 2.0 * regionSize;
        if (pointX * pointX + pointZ * pointZ < limit * limit) {
            return Band.MIDDLE;
        }
        double roll = random.value(cellX, cellZ, 2);
        return roll < 1 / 3.0 ? Band.LOWER : roll < 2 / 3.0 ? Band.MIDDLE : Band.UPPER;
    }

    /** Maps any double to a well-mixed value in [0, 1) (SplitMix64 finalizer). */
    public static double mix(double value, int cellX, int cellZ, int channel) {
        long h = Double.doubleToLongBits(value) ^ (cellX * 0x9E3779B97F4A7C15L) ^ (cellZ * 0xC2B2AE3D27D4EB4FL)
                ^ ((long) channel << 56);
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return (h >>> 11) * 0x1.0p-53;
    }
}
