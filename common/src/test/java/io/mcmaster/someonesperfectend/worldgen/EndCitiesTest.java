package io.mcmaster.someonesperfectend.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EndCitiesTest {
    @Test
    void minimumYFollowsTheBand() {
        ElevationBands layout = ElevationBands.DEFAULT;
        assertEquals(40, EndCities.minimumY(EndCities.VANILLA_MIN_Y, layout.offset(ElevationBands.Band.LOWER)));
        assertEquals(60, EndCities.minimumY(EndCities.VANILLA_MIN_Y, layout.offset(ElevationBands.Band.MIDDLE)));
        assertEquals(100, EndCities.minimumY(EndCities.VANILLA_MIN_Y, layout.offset(ElevationBands.Band.UPPER)));
    }

    @Test
    void noCityInAGap() {
        assertEquals(Integer.MAX_VALUE, EndCities.minimumY(EndCities.VANILLA_MIN_Y, ElevationBands.GAP));
    }
}
