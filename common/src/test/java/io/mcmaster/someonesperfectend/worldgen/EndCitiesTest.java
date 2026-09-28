package io.mcmaster.someonesperfectend.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EndCitiesTest {
    private static final ElevationBands LAYOUT = ElevationBands.DEFAULT;

    @Test
    void minimumYFollowsTheBand() {
        // At the always-middle zone edge the offsets are -32 / 0 / +56; the moved surface may be 4 blocks lower.
        assertEquals(24, EndCities.minimumY(60, LAYOUT.offset(ElevationBands.Band.LOWER, 1184), 60));
        assertEquals(56, EndCities.minimumY(60, LAYOUT.offset(ElevationBands.Band.MIDDLE, 1184), 61));
        assertEquals(112, EndCities.minimumY(60, LAYOUT.offset(ElevationBands.Band.UPPER, 1184), 60));
        // From 8,000 blocks on they are -48 / +84.
        assertEquals(8, EndCities.minimumY(60, LAYOUT.offset(ElevationBands.Band.LOWER, 8000), 62));
        assertEquals(140, EndCities.minimumY(60, LAYOUT.offset(ElevationBands.Band.UPPER, 8000), 60));
        // A group's jitter moves the threshold with it.
        assertEquals(160, EndCities.minimumY(60, LAYOUT.groupOffset(ElevationBands.Band.UPPER, 8000, 1), 60));
    }

    @Test
    void onlyWhereVanillaTerrainHostsACity() {
        assertEquals(Integer.MAX_VALUE, EndCities.minimumY(60, LAYOUT.offset(ElevationBands.Band.UPPER, 4000), 59));
        assertEquals(Integer.MAX_VALUE, EndCities.minimumY(60, 0, -1));
    }

    @Test
    void noCityInAGap() {
        assertEquals(Integer.MAX_VALUE, EndCities.minimumY(EndCities.VANILLA_MIN_Y, ElevationBands.GAP, 70));
    }
}
