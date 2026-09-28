package io.mcmaster.someonesperfectend.worldgen;

/**
 * Registry-free layout math for the outer-End elevation bands (US-012, MOD-47).
 *
 * <p>The outer End is split into Voronoi regions about {@code regionSize} blocks across. Each region gets one
 * band (lower, middle, or upper), and every island in it is moved up or down by that band's offset. Where two
 * neighboring regions have different bands, a strip {@code gapWidth} blocks wide along their border is forced
 * to void, so an island is split there instead of being sheared into a cliff.
 *
 * <p>Band offsets grow with the distance {@code d} of the region's point from the origin: they are
 * {@code lowerOffset} and {@code upperOffset} times {@link #scale}, which goes from 1 at the always-middle zone
 * edge ({@code minDistance + regionSize}) linearly to {@link #MAX_SCALE} at {@link #FULL_SCALE_DISTANCE}.
 * Inside a region, a smooth noise ({@link Jitter}) adds up to ±{@link #jitterAmplitude} whole blocks per
 * column, so islands in one band are not all at the same height.
 *
 * <p>Within {@code minDistance} blocks of the origin (the central island and the void ring) nothing changes.
 * Regions whose point lies within {@code minDistance + regionSize} are always middle band with no jitter. The
 * first {@link #INTERPOLATION_MARGIN} blocks past {@code minDistance} are unchanged too, so terrain
 * interpolation cannot carry an offset inside the circle, and other regions reaching the circle are split from
 * it by a gap as if it were a middle-band region.
 *
 * <p>The total offset is clamped to [{@link #MIN_OFFSET}, {@link #MAX_OFFSET}] to keep moved terrain inside
 * the End's build height.
 */
public record ElevationBands(int lowerOffset, int upperOffset, int regionSize, int gapWidth, int minDistance) {
    /** Returned by {@link #offsetAt} for positions in a void gap between regions. */
    public static final int GAP = Integer.MIN_VALUE;

    public static final ElevationBands DEFAULT = new ElevationBands(-32, 56, 160, 16, 1024);

    /** Region point distance at which band offsets and jitter stop growing. */
    public static final double FULL_SCALE_DISTANCE = 8000;
    /** Band offset multiplier at {@link #FULL_SCALE_DISTANCE} and beyond. */
    public static final double MAX_SCALE = 1.5;
    /** Jitter amplitude (before scaling) at the always-middle zone edge and at {@link #FULL_SCALE_DISTANCE}. */
    public static final int MIN_JITTER = 6;
    public static final int MAX_JITTER = 12;
    /** Blocks past {@code minDistance} kept vanilla: covers the 8-block horizontal noise cell interpolation. */
    public static final int INTERPOLATION_MARGIN = 16;
    /**
     * Lowest total offset, the full-scale lower band offset (so jitter never sinks an island deeper). Vanilla
     * outer island tops are at about Y=52 to 66, so lowered tops stay above Y=4; the floor slide that
     * {@link ElevationBandsFunction} reapplies at the real Y (Y 0 to 16) trims undersides that would reach the floor.
     */
    public static final int MIN_OFFSET = -48;
    /**
     * Highest total offset: the End noise is 192 blocks high (Y 0 to 191), so vanilla terrain topping out below
     * Y=72 stays inside it. The largest offset the defaults produce is {@code 84 + 18 = 102}.
     */
    public static final int MAX_OFFSET = 119;

    public enum Band {
        LOWER, MIDDLE, UPPER
    }

    /** Seeded random numbers in [0, 1) per region cell; {@code channel} 0 and 1 jitter the point, 2 picks the band. */
    @FunctionalInterface
    public interface CellRandom {
        double value(int cellX, int cellZ, int channel);
    }

    /** Smooth seeded noise over columns, about [-1, 1] (values outside are clamped). */
    @FunctionalInterface
    public interface Jitter {
        double value(int x, int z);
    }

    /** The region a column belongs to: its band, its point's distance from the origin, and if it is fixed middle. */
    public record Region(Band band, double distance, boolean alwaysMiddle) {
    }

    /** Vertical offset of the terrain at column (x, z), or {@link #GAP} if the column must be void. */
    public int offsetAt(int x, int z, CellRandom random, Jitter jitter) {
        Region region = regionAt(x, z, random);
        if (region == null) {
            return GAP;
        }
        if (region.alwaysMiddle()) {
            return 0;
        }
        double noise = Math.max(-1.0, Math.min(1.0, jitter.value(x, z)));
        int shift = (int) Math.round(jitterAmplitude(region.distance()) * noise);
        return clampOffset(offset(region.band(), region.distance()) + shift);
    }

    /** The region of column (x, z), or null if the column is in a void gap. */
    public Region regionAt(int x, int z, CellRandom random) {
        long radiusSquared = (long) x * x + (long) z * z;
        long vanillaRadius = minDistance + INTERPOLATION_MARGIN;
        if (radiusSquared < vanillaRadius * vanillaRadius) {
            return new Region(Band.MIDDLE, 0, true);
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
        double pointDistance = Math.sqrt(pointX[nearest] * pointX[nearest] + pointZ[nearest] * pointZ[nearest]);
        boolean alwaysMiddle = pointDistance < minDistance + (double) regionSize;
        if (!alwaysMiddle && Math.sqrt(radiusSquared) < vanillaRadius + gapWidth) {
            // The unchanged area inside the circle acts as a middle-band region with no jitter.
            return null;
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
                return null;
            }
        }
        return new Region(band, pointDistance, alwaysMiddle);
    }

    /** Band offset multiplier for a region point {@code distance} blocks from the origin, in [1, {@link #MAX_SCALE}]. */
    public double scale(double distance) {
        return 1 + (MAX_SCALE - 1) * growth(distance);
    }

    /** Offset of a band for a region point {@code distance} blocks from the origin (before jitter). */
    public int offset(Band band, double distance) {
        return switch (band) {
            case LOWER -> (int) Math.round(lowerOffset * scale(distance));
            case MIDDLE -> 0;
            case UPPER -> (int) Math.round(upperOffset * scale(distance));
        };
    }

    /** Largest jitter, in blocks, for a region point {@code distance} blocks from the origin. */
    public int jitterAmplitude(double distance) {
        double t = growth(distance);
        return (int) Math.round((MIN_JITTER + (MAX_JITTER - MIN_JITTER) * t) * scale(distance));
    }

    /** Keeps a total offset inside [{@link #MIN_OFFSET}, {@link #MAX_OFFSET}]. */
    public static int clampOffset(int offset) {
        return Math.max(MIN_OFFSET, Math.min(MAX_OFFSET, offset));
    }

    /** 0 at the always-middle zone edge, growing linearly to 1 at {@link #FULL_SCALE_DISTANCE}, then capped. */
    private double growth(double distance) {
        double edge = minDistance + (double) regionSize;
        return Math.max(0.0, Math.min(1.0, (distance - edge) / (FULL_SCALE_DISTANCE - edge)));
    }

    private Band band(int cellX, int cellZ, double pointX, double pointZ, CellRandom random) {
        double limit = minDistance + (double) regionSize;
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
