package io.mcmaster.someonesperfectend.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.mcmaster.someonesperfectend.mixin.EndIslandDensityFunctionAccessor;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

/**
 * Density function {@code someonesperfectend:elevation_bands}: evaluates {@code argument} moved up or down by
 * the elevation band offset of the column, and returns void in the seams between island groups.
 *
 * <p>The End noise settings wrap the terrain density (inside {@code interpolated} and {@code blend_density})
 * with this function. Offsets are multiples of the End's 4-block noise cell height, so a moved island is the
 * vanilla island shape, translated. Within {@code min_distance} of the origin the argument is returned as is.
 *
 * <p>The island groups come from the seeded {@code minecraft:end_islands} function inside {@code argument}
 * (see {@link ElevationBandLayout}); {@code noise} seeds the band of each group, so both differ per world seed.
 */
public final class ElevationBandsFunction implements DensityFunction {
    /** Density of the void gaps; below the vanilla End's lowest value of {@code -0.234375} near the floor. */
    private static final double GAP_DENSITY = -1.0;
    // The vanilla End's bottom slide (y 4 -> 32, towards -0.234375), reapplied at the real Y for the lower band.
    private static final int BOTTOM_SLIDE_FROM = 4;
    private static final int BOTTOM_SLIDE_TO = 32;
    private static final double BOTTOM_SLIDE_VALUE = -0.234375;

    public static final MapCodec<ElevationBandsFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    DensityFunction.HOLDER_HELPER_CODEC.fieldOf("argument").forGetter(f -> f.argument),
                    NoiseHolder.CODEC.fieldOf("noise").forGetter(f -> f.noise),
                    Codec.INT
                            .optionalFieldOf("lower_offset", ElevationBands.DEFAULT.lowerOffset())
                            .forGetter(f -> f.layout.settings().lowerOffset()),
                    Codec.INT
                            .optionalFieldOf("upper_offset", ElevationBands.DEFAULT.upperOffset())
                            .forGetter(f -> f.layout.settings().upperOffset()),
                    Codec.intRange(-100, 80)
                            .optionalFieldOf("island_threshold", ElevationBands.DEFAULT.islandThreshold())
                            .forGetter(f -> f.layout.settings().islandThreshold()),
                    Codec.intRange(0, 256)
                            .optionalFieldOf("link_margin", ElevationBands.DEFAULT.linkMargin())
                            .forGetter(f -> f.layout.settings().linkMargin()),
                    Codec.intRange(0, 1 << 20)
                            .optionalFieldOf("min_distance", ElevationBands.DEFAULT.minDistance())
                            .forGetter(f -> f.layout.settings().minDistance()),
                    Codec.intRange(0, 1 << 20)
                            .optionalFieldOf("middle_distance", ElevationBands.DEFAULT.middleDistance())
                            .forGetter(f -> f.layout.settings().middleDistance()))
            .apply(
                    instance,
                    (argument, noise, lower, upper, threshold, margin, distance, middle) -> new ElevationBandsFunction(
                            argument, noise, new ElevationBands(lower, upper, threshold, margin, distance, middle))));

    private static final KeyDispatchDataCodec<ElevationBandsFunction> DISPATCH = KeyDispatchDataCodec.of(CODEC);

    /** Last column looked up on this thread; the noise chunk evaluates a column at many Y values in a row. */
    private static final ThreadLocal<ColumnCache> CACHE = ThreadLocal.withInitial(ColumnCache::new);

    /** The seeded function found in each {@link RandomState}'s final density (or none, e.g. other dimensions). */
    private static final Map<RandomState, Optional<ElevationBandsFunction>> BY_RANDOM_STATE =
            Collections.synchronizedMap(new WeakHashMap<>());

    private final DensityFunction argument;
    private final NoiseHolder noise;
    /** The island noise of the {@code end_islands} function in {@code argument}, or null if there is none. */
    private final SimplexNoise islandNoise;

    private final ElevationBandLayout layout;

    /**
     * Decoded (unseeded) function. Registry holders in {@code argument} may not be bound yet, so the island noise
     * is looked up when the random state seeds this function ({@link #mapAll}), not here.
     */
    public ElevationBandsFunction(DensityFunction argument, NoiseHolder noise, ElevationBands settings) {
        this(argument, noise, null, settings);
    }

    private ElevationBandsFunction(
            DensityFunction argument, NoiseHolder noise, SimplexNoise islandNoise, ElevationBands settings) {
        this(argument, noise, islandNoise, settings.layout(islandCenters(islandNoise), cellRandom(noise)));
    }

    private ElevationBandsFunction(
            DensityFunction argument, NoiseHolder noise, SimplexNoise islandNoise, ElevationBandLayout layout) {
        this.argument = argument;
        this.noise = noise;
        this.islandNoise = islandNoise;
        this.layout = layout;
    }

    private static ElevationBands.CellRandom cellRandom(NoiseHolder noise) {
        return (cellX, cellZ, channel) -> ElevationBands.mix(
                noise.getValue(cellX + 0.37, channel * 0.71 + 0.13, cellZ + 0.59), cellX, cellZ, channel);
    }

    private static ElevationBands.IslandCenters islandCenters(SimplexNoise islandNoise) {
        return islandNoise == null ? (gridX, gridZ) -> false : ElevationBandLayout.vanillaCenters(islandNoise);
    }

    /** The noise of the first {@code minecraft:end_islands} function in a density function tree. */
    private static SimplexNoise findIslandNoise(DensityFunction function) {
        SimplexNoise[] found = new SimplexNoise[1];
        function.mapAll(new Visitor() {
            @Override
            public DensityFunction apply(DensityFunction visited) {
                if (found[0] == null && visited instanceof EndIslandDensityFunctionAccessor islands) {
                    found[0] = islands.someonesperfectend$islandNoise();
                }
                return visited;
            }
        });
        return found[0];
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

    public ElevationBandLayout layout() {
        return layout;
    }

    /** The column's offset, or {@link ElevationBands#GAP}. */
    public int offsetAt(int x, int z) {
        ColumnCache cache = CACHE.get();
        if (cache.owner != this || cache.x != x || cache.z != z) {
            cache.owner = this;
            cache.x = x;
            cache.z = z;
            cache.offset = layout.offsetAt(x, z);
        }
        return cache.offset;
    }

    @Override
    public double compute(FunctionContext context) {
        int offset = offsetAt(context.blockX(), context.blockZ());
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
        DensityFunction mappedArgument = argument.mapAll(visitor);
        NoiseHolder mappedNoise = visitor.visitNoise(noise);
        SimplexNoise mappedIslands = findIslandNoise(mappedArgument);
        // Keep the layout (and its island group cache) unless seeding changed the noises, e.g. per-chunk copies.
        if (mappedNoise.noise() == noise.noise() && mappedIslands == islandNoise) {
            return visitor.apply(new ElevationBandsFunction(mappedArgument, mappedNoise, islandNoise, layout));
        }
        return visitor.apply(new ElevationBandsFunction(mappedArgument, mappedNoise, mappedIslands, layout.settings()));
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
    }
}
