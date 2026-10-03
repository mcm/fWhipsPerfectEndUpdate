package io.mcmaster.someonesperfectend.worldgen;

import net.minecraft.world.level.levelgen.RandomState;

/**
 * End City placement across the elevation bands (US-018).
 *
 * <p>Vanilla's {@code EndCityStructure} only starts a city where the lowest corner of its footprint is at
 * Y ≥ 60, which would rule out every lower-band island and let fewer upper-band islands qualify than their
 * vanilla shapes did. The threshold is moved by the band offset of the start column instead, so exactly the
 * islands that would host a city in vanilla host one in their band, at their real surface height. Vanilla's
 * structure, structure set, and pieces are used unchanged.
 */
public final class EndCities {
    /** Vanilla's minimum start height, hard-coded in {@code EndCityStructure#findGenerationPoint}. */
    public static final int VANILLA_MIN_Y = 60;

    private EndCities() {}

    /** Minimum start height at column (x, z) for a generator with this random state (unchanged without bands). */
    public static int minimumY(RandomState randomState, int vanillaMinY, int x, int z) {
        return ElevationBandsFunction.find(randomState)
                .map(bands -> minimumY(vanillaMinY, bands.offsetAt(x, z)))
                .orElse(vanillaMinY);
    }

    /** {@code vanillaMinY} moved by a band offset; no city can start in a void gap between bands. */
    public static int minimumY(int vanillaMinY, int offset) {
        return offset == ElevationBands.GAP ? Integer.MAX_VALUE : vanillaMinY + offset;
    }
}
