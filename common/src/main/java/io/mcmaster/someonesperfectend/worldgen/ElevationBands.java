package io.mcmaster.someonesperfectend.worldgen;

/**
 * Settings and registry-free helpers for the outer-End elevation bands (US-012, MOD-47, MOD-71).
 *
 * <p>Every outer island is moved up or down as a whole: vanilla builds the outer islands from island centers on
 * a 16-block grid ({@code minecraft:end_islands}), and centers whose islands touch form one island group. Each
 * group gets one band (lower, middle, or upper) and all of it moves by one offset, so a band never splits an
 * island. See {@link ElevationBandLayout} for the math.
 *
 * <p>A group's offset grows with the distance {@code d} of its center nearest the origin: the band offsets are
 * {@code lowerOffset} and {@code upperOffset} times {@link #scale}, which goes from 1 at the always-middle zone
 * edge ({@code middleDistance}) linearly to {@link #MAX_SCALE} at {@link #FULL_SCALE_DISTANCE}. A seeded jitter
 * of up to ±{@link #jitterAmplitude} blocks per group sets islands in one band at different heights. The total is
 * snapped to the 4-block noise cell height, so a moved island is the vanilla shape translated, and clamped to
 * [{@link #MIN_OFFSET}, {@link #MAX_OFFSET}] to keep it inside the End's build height.
 *
 * @param lowerOffset blocks the lower band is moved by at the always-middle zone edge
 * @param upperOffset blocks the upper band is moved by at the always-middle zone edge
 * @param islandThreshold vanilla island height value ({@code 100 - distance × falloff}) above which a column is
 *     part of an island; {@code end_islands} is 0 at 8, so a lower value also covers the 3D noise's fringe
 * @param linkMargin extra blocks between two island edges that still join them into one group; this keeps the
 *     void seam between groups with different offsets away from both islands
 * @param minDistance within this many blocks of the origin nothing changes
 * @param middleDistance groups with a center within this many blocks of the origin are always middle band with no
 *     jitter, so the islands near the void ring and End Gateway exits stay vanilla
 */
public record ElevationBands(
        int lowerOffset, int upperOffset, int islandThreshold, int linkMargin, int minDistance, int middleDistance) {
    /** Returned by {@link ElevationBandLayout#offsetAt} for positions in a void seam between island groups. */
    public static final int GAP = Integer.MIN_VALUE;

    public static final ElevationBands DEFAULT = new ElevationBands(-32, 56, 0, 24, 1024, 1184);

    /** Group distance at which band offsets and jitter stop growing. */
    public static final double FULL_SCALE_DISTANCE = 8000;
    /** Band offset multiplier at {@link #FULL_SCALE_DISTANCE} and beyond. */
    public static final double MAX_SCALE = 1.5;
    /** Jitter amplitude (before scaling) at the always-middle zone edge and at {@link #FULL_SCALE_DISTANCE}. */
    public static final int MIN_JITTER = 6;

    public static final int MAX_JITTER = 12;
    /** The End's noise cell height; offsets are multiples of it so moved islands keep their vanilla shape. */
    public static final int CELL_HEIGHT = 4;
    /**
     * Lowest total offset, the full-scale lower band offset (so jitter never sinks an island deeper). Vanilla
     * outer island tops are at about Y=52 to 66, so lowered tops stay above Y=4; the floor slide that
     * {@link ElevationBandsFunction} reapplies at the real Y (Y 0 to 16) trims undersides that would reach the floor.
     */
    public static final int MIN_OFFSET = -48;
    /**
     * Highest total offset: the End noise is 192 blocks high (Y 0 to 191), so vanilla terrain topping out below
     * Y=72 stays inside it. The largest offset the defaults produce is {@code 84 + 18 = 102}, snapped to 104.
     */
    public static final int MAX_OFFSET = 116;

    public enum Band {
        LOWER,
        MIDDLE,
        UPPER
    }

    /**
     * Seeded random numbers in [0, 1) per island center; {@code channel} 2 picks the band, 3 the jitter.
     */
    @FunctionalInterface
    public interface CellRandom {
        double value(int cellX, int cellZ, int channel);
    }

    /** Vanilla's outer island centers, in 16-block grid units ({@code minecraft:end_islands}). */
    @FunctionalInterface
    public interface IslandCenters {
        boolean isCenter(int gridX, int gridZ);
    }

    /** Band offset multiplier for a group {@code distance} blocks from the origin, in [1, {@link #MAX_SCALE}]. */
    public double scale(double distance) {
        return 1 + (MAX_SCALE - 1) * growth(distance);
    }

    /** Offset of a band for a group {@code distance} blocks from the origin (before jitter and snapping). */
    public int offset(Band band, double distance) {
        return switch (band) {
            case LOWER -> (int) Math.round(lowerOffset * scale(distance));
            case MIDDLE -> 0;
            case UPPER -> (int) Math.round(upperOffset * scale(distance));
        };
    }

    /** Largest jitter, in blocks, for a group {@code distance} blocks from the origin. */
    public int jitterAmplitude(double distance) {
        double t = growth(distance);
        return (int) Math.round((MIN_JITTER + (MAX_JITTER - MIN_JITTER) * t) * scale(distance));
    }

    /**
     * Total offset of a group: its band offset plus {@code jitter} (in [-1, 1], clamped) times the amplitude,
     * snapped to {@link #CELL_HEIGHT} and clamped by {@link #clampOffset}.
     */
    public int groupOffset(Band band, double distance, double jitter) {
        double shift = jitterAmplitude(distance) * Math.max(-1.0, Math.min(1.0, jitter));
        return clampOffset(snap(offset(band, distance) + shift));
    }

    /** Rounds an offset to the nearest multiple of {@link #CELL_HEIGHT}. */
    public static int snap(double offset) {
        return (int) Math.round(offset / CELL_HEIGHT) * CELL_HEIGHT;
    }

    /** Keeps a total offset inside [{@link #MIN_OFFSET}, {@link #MAX_OFFSET}]. */
    public static int clampOffset(int offset) {
        return Math.max(MIN_OFFSET, Math.min(MAX_OFFSET, offset));
    }

    /** The band layout for one world: island groups from {@code centers}, bands from {@code random}. */
    public ElevationBandLayout layout(IslandCenters centers, CellRandom random) {
        return new ElevationBandLayout(this, centers, random);
    }

    /** 0 at the always-middle zone edge, growing linearly to 1 at {@link #FULL_SCALE_DISTANCE}, then capped. */
    private double growth(double distance) {
        return Math.max(0.0, Math.min(1.0, (distance - middleDistance) / (FULL_SCALE_DISTANCE - middleDistance)));
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
