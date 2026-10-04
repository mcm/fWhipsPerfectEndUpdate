package io.mcmaster.fwhipsperfectendupdate.worldgen;

import java.util.function.IntSupplier;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * End City placement across the elevation bands (US-018, MOD-47).
 *
 * <p>Vanilla's {@code EndCityStructure} only starts a city where the lowest corner of its footprint is at
 * Y ≥ 60, which would rule out every lower-band island and let fewer upper-band islands qualify than their
 * vanilla shapes did. Instead, the footprint's lowest corner is also measured on the vanilla terrain (with the
 * bands bypassed), and a city starts exactly where it would in vanilla, at its real, moved surface height.
 * The moved lowest corner must still be near {@code 60 + offset} of the start column, so no city starts over
 * a void gap or on terrain the floor slide wore away. Vanilla's structure, structure set, and pieces are used
 * unchanged.
 */
public final class EndCities {
    /** Vanilla's minimum start height, hard-coded in {@code EndCityStructure#findGenerationPoint}. */
    public static final int VANILLA_MIN_Y = 60;
    /**
     * How far the moved lowest corner may sit below {@code 60 + offset}. A whole island moves by one offset, but
     * the floor slide wears a block or two off the deepest lowered islands.
     */
    public static final int SURFACE_TOLERANCE = 4;

    private EndCities() {}

    /**
     * Minimum start height at column (x, z) for a generator with this random state (unchanged without bands).
     * {@code vanillaLowestY} measures the footprint's lowest corner; it is run with the bands bypassed.
     */
    public static int minimumY(RandomState randomState, int vanillaMinY, int x, int z, IntSupplier vanillaLowestY) {
        return ElevationBandsFunction.find(randomState)
                .map(bands -> {
                    int offset = bands.offsetAt(x, z);
                    if (offset == ElevationBands.GAP) {
                        return Integer.MAX_VALUE;
                    }
                    return minimumY(vanillaMinY, offset, ElevationBandsFunction.withoutBands(vanillaLowestY));
                })
                .orElse(vanillaMinY);
    }

    /**
     * The start height a city needs: none (never starts) if vanilla terrain would not host it or the start is in
     * a gap, otherwise {@code vanillaMinY} moved by the band offset, less {@link #SURFACE_TOLERANCE}.
     */
    public static int minimumY(int vanillaMinY, int offset, int vanillaLowestY) {
        if (offset == ElevationBands.GAP || vanillaLowestY < vanillaMinY) {
            return Integer.MAX_VALUE;
        }
        return vanillaMinY + offset - SURFACE_TOLERANCE;
    }
}
