package io.mcmaster.someonesperfectend.worldgen;

/**
 * Settings and registry-free helpers for the outer-End elevation bands (US-012, MOD-71).
 *
 * <p>Every outer island is moved up or down as a whole: vanilla builds the outer islands from island centers on
 * a 16-block grid ({@code minecraft:end_islands}), and centers whose islands touch form one island group. Each
 * group gets one band (lower, middle, or upper) and all of it moves by that band's offset, so a band never splits
 * an island. See {@link ElevationBandLayout} for the math.
 *
 * @param lowerOffset blocks the lower band is moved by (a multiple of the 4-block noise cell height)
 * @param upperOffset blocks the upper band is moved by
 * @param islandThreshold vanilla island height value ({@code 100 - distance × falloff}) above which a column is
 *     part of an island; {@code end_islands} is 0 at 8, so a lower value also covers the 3D noise's fringe
 * @param linkMargin extra blocks between two island edges that still join them into one group; this keeps the
 *     void seam between groups with different bands away from both islands
 * @param minDistance within this many blocks of the origin nothing changes
 * @param middleDistance groups with a center within this many blocks of the origin are always middle band, so
 *     the islands near the void ring and End Gateway exits stay vanilla
 */
public record ElevationBands(
        int lowerOffset, int upperOffset, int islandThreshold, int linkMargin, int minDistance, int middleDistance) {
    /** Returned by {@link ElevationBandLayout#offsetAt} for positions in a void seam between island groups. */
    public static final int GAP = Integer.MIN_VALUE;

    public static final ElevationBands DEFAULT = new ElevationBands(-20, 40, 0, 24, 1024, 1536);

    public enum Band {
        LOWER,
        MIDDLE,
        UPPER
    }

    /** Seeded random numbers in [0, 1) per island center; {@code channel} 2 picks the band. */
    @FunctionalInterface
    public interface CellRandom {
        double value(int cellX, int cellZ, int channel);
    }

    /** Vanilla's outer island centers, in 16-block grid units ({@code minecraft:end_islands}). */
    @FunctionalInterface
    public interface IslandCenters {
        boolean isCenter(int gridX, int gridZ);
    }

    public int offset(Band band) {
        return switch (band) {
            case LOWER -> lowerOffset;
            case MIDDLE -> 0;
            case UPPER -> upperOffset;
        };
    }

    /** The band layout for one world: island groups from {@code centers}, bands from {@code random}. */
    public ElevationBandLayout layout(IslandCenters centers, CellRandom random) {
        return new ElevationBandLayout(this, centers, random);
    }

    /** Maps any double to a well-mixed value in [0, 1) (SplitMix64 finalizer). */
    public static double mix(double value, int cellX, int cellZ, int channel) {
        long h = Double.doubleToLongBits(value)
                ^ (cellX * 0x9E3779B97F4A7C15L)
                ^ (cellZ * 0xC2B2AE3D27D4EB4FL)
                ^ ((long) channel << 56);
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return (h >>> 11) * 0x1.0p-53;
    }
}
