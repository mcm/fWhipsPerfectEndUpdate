package io.mcmaster.someonesperfectend.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.IntSupplier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * Density function {@code someonesperfectend:elevation_bands}: evaluates {@code argument} moved up or down by
 * the {@link ElevationBands} offset of the column, and returns void in the gaps between bands.
 *
 * <p>The End noise settings wrap the terrain density (inside {@code interpolated} and {@code blend_density})
 * with this function, so a moved island is the vanilla island shape, translated by its band offset and gently
 * warped by the per-column jitter. Within {@code min_distance} of the origin the argument is returned as is.
 *
 * <p>{@code noise} seeds the region layout and {@code jitter_noise} (about 64 blocks per feature) the jitter,
 * so both differ per world seed.
 */
public final class ElevationBandsFunction implements DensityFunction {
    /** Density of the void gaps; below the vanilla End's lowest value of {@code -0.234375} near the floor. */
    private static final double GAP_DENSITY = -1.0;
    // A floor slide like the vanilla End's (towards -0.234375), reapplied at the real Y for lowered terrain. It is
    // shorter than vanilla's (y 4 -> 32) so the deepest lower-band islands keep their tops and only lose undersides.
    private static final int BOTTOM_SLIDE_FROM = 0;
    private static final int BOTTOM_SLIDE_TO = 16;
    private static final double BOTTOM_SLIDE_VALUE = -0.234375;

    public static final MapCodec<ElevationBandsFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(
                    DensityFunction.HOLDER_HELPER_CODEC.fieldOf("argument").forGetter(f -> f.argument),
                    NoiseHolder.CODEC.fieldOf("noise").forGetter(f -> f.noise),
                    NoiseHolder.CODEC.fieldOf("jitter_noise").forGetter(f -> f.jitterNoise),
                    Codec.INT.optionalFieldOf("lower_offset", ElevationBands.DEFAULT.lowerOffset())
                            .forGetter(f -> f.layout.lowerOffset()),
                    Codec.INT.optionalFieldOf("upper_offset", ElevationBands.DEFAULT.upperOffset())
                            .forGetter(f -> f.layout.upperOffset()),
                    Codec.intRange(16, 4096).optionalFieldOf("region_size", ElevationBands.DEFAULT.regionSize())
                            .forGetter(f -> f.layout.regionSize()),
                    Codec.intRange(0, 256).optionalFieldOf("gap_width", ElevationBands.DEFAULT.gapWidth())
                            .forGetter(f -> f.layout.gapWidth()),
                    Codec.intRange(0, 1 << 20).optionalFieldOf("min_distance", ElevationBands.DEFAULT.minDistance())
                            .forGetter(f -> f.layout.minDistance()))
            .apply(instance, (argument, noise, jitterNoise, lower, upper, size, gap, distance) ->
                    new ElevationBandsFunction(argument, noise, jitterNoise,
                            new ElevationBands(lower, upper, size, gap, distance))));

    private static final KeyDispatchDataCodec<ElevationBandsFunction> DISPATCH = KeyDispatchDataCodec.of(CODEC);

    /** Last column looked up on this thread; the noise chunk evaluates a column at many Y values in a row. */
    private static final ThreadLocal<ColumnCache> CACHE = ThreadLocal.withInitial(ColumnCache::new);

    /** The seeded function found in each {@link RandomState}'s final density (or none, e.g. other dimensions). */
    private static final Map<RandomState, Optional<ElevationBandsFunction>> BY_RANDOM_STATE =
            Collections.synchronizedMap(new WeakHashMap<>());

    private final DensityFunction argument;
    private final NoiseHolder noise;
    private final NoiseHolder jitterNoise;
    private final ElevationBands layout;
    private final ElevationBands.CellRandom cellRandom;
    private final ElevationBands.Jitter jitter;

    public ElevationBandsFunction(DensityFunction argument, NoiseHolder noise, NoiseHolder jitterNoise,
            ElevationBands layout) {
        this.argument = argument;
        this.noise = noise;
        this.jitterNoise = jitterNoise;
        this.layout = layout;
        this.cellRandom = (cellX, cellZ, channel) -> ElevationBands.mix(
                noise.getValue(cellX + 0.37, channel * 0.71 + 0.13, cellZ + 0.59), cellX, cellZ, channel);
        this.jitter = (x, z) -> jitterNoise.getValue(x, 0.0, z);
    }

    /** The seeded elevation bands of a generator's random state, if its final density uses them. */
    public static Optional<ElevationBandsFunction> find(RandomState randomState) {
        return BY_RANDOM_STATE.computeIfAbsent(randomState, state -> {
            ElevationBandsFunction[] found = new ElevationBandsFunction[1];
            state.router().finalDensity().mapAll(new Visitor() {
                @Override
                public DensityFunction apply(DensityFunction function) {
                    if (found[0] == null && function instanceof ElevationBandsFunction bands) {
                        found[0] = bands;
                    }
                    return function;
                }
            });
            return Optional.ofNullable(found[0]);
        });
    }

    public DensityFunction argument() {
        return argument;
    }

    public ElevationBands layout() {
        return layout;
    }

    /**
     * Runs {@code action} with every elevation bands function on this thread returning its argument unchanged,
     * so terrain queried inside (e.g. heights) is the vanilla terrain.
     */
    public static int withoutBands(IntSupplier action) {
        ColumnCache cache = CACHE.get();
        boolean previous = cache.bypass;
        cache.bypass = true;
        try {
            return action.getAsInt();
        } finally {
            cache.bypass = previous;
        }
    }

    /** The column's offset, or {@link ElevationBands#GAP}. */
    public int offsetAt(int x, int z) {
        return offsetAt(CACHE.get(), x, z);
    }

    private int offsetAt(ColumnCache cache, int x, int z) {
        if (cache.owner != this || cache.x != x || cache.z != z) {
            cache.owner = this;
            cache.x = x;
            cache.z = z;
            cache.offset = layout.offsetAt(x, z, cellRandom, jitter);
        }
        return cache.offset;
    }

    /** The column's region (band and region point distance), or null in a gap. */
    public ElevationBands.Region regionAt(int x, int z) {
        return layout.regionAt(x, z, cellRandom);
    }

    @Override
    public double compute(FunctionContext context) {
        ColumnCache cache = CACHE.get();
        if (cache.bypass) {
            return argument.compute(context);
        }
        int offset = offsetAt(cache, context.blockX(), context.blockZ());
        if (offset == 0) {
            return argument.compute(context);
        }
        if (offset == ElevationBands.GAP) {
            return GAP_DENSITY;
        }
        int y = context.blockY();
        double value = argument.compute(new SinglePointContext(context.blockX(), y - offset, context.blockZ()));
        if (offset < 0) {
            // Lowered islands would otherwise hit the bottom of the world; taper them like vanilla does.
            double slide = Mth.clampedMap(y, BOTTOM_SLIDE_FROM, BOTTOM_SLIDE_TO, 0.0, 1.0);
            value = BOTTOM_SLIDE_VALUE + slide * (value - BOTTOM_SLIDE_VALUE);
        }
        return value;
    }

    @Override
    public void fillArray(double[] array, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(array, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new ElevationBandsFunction(argument.mapAll(visitor), visitor.visitNoise(noise),
                visitor.visitNoise(jitterNoise), layout));
    }

    @Override
    public double minValue() {
        return Math.min(GAP_DENSITY, Math.min(argument.minValue(), BOTTOM_SLIDE_VALUE));
    }

    @Override
    public double maxValue() {
        return Math.max(argument.maxValue(), BOTTOM_SLIDE_VALUE);
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return DISPATCH;
    }

    private static final class ColumnCache {
        private Object owner;
        private int x;
        private int z;
        private int offset;
        private boolean bypass;
    }
}
