package io.mcmaster.fwhipsperfectendupdate.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

class EndlingVillageLayoutTest {
    /** Half the width of a hut including its one-block roof overhang. */
    private static final int ROOF_HALF = EndlingVillageLayout.HUT_SIZE / 2 + 1;
    /** Half the width of a huge glowcap's cap. */
    private static final int CAP_HALF = 2;

    @Test
    void countsStayInRangeAndPiecesDontOverlap() {
        boolean[] hutCounts = new boolean[EndlingVillageLayout.MAX_HUTS + 1];
        boolean[] glowcapCounts = new boolean[EndlingVillageLayout.MAX_GLOWCAPS + 1];
        for (long seed = 0; seed < 5000; seed++) {
            EndlingVillageLayout layout = EndlingVillageLayout.create(RandomSource.create(seed));
            List<EndlingVillageLayout.Hut> huts = layout.huts();
            List<EndlingVillageLayout.Site> glowcaps = layout.glowcaps();
            assertTrue(huts.size() >= EndlingVillageLayout.MIN_HUTS && huts.size() <= EndlingVillageLayout.MAX_HUTS);
            assertTrue(glowcaps.size() >= EndlingVillageLayout.MIN_GLOWCAPS
                    && glowcaps.size() <= EndlingVillageLayout.MAX_GLOWCAPS);
            hutCounts[huts.size()] = true;
            glowcapCounts[glowcaps.size()] = true;
            for (int i = 0; i < huts.size(); i++) {
                EndlingVillageLayout.Hut a = huts.get(i);
                assertTrue(chebyshev(a.dx(), a.dz()) + ROOF_HALF <= EndlingVillageLayout.RADIUS, "hut outside radius");
                for (int j = i + 1; j < huts.size(); j++) {
                    EndlingVillageLayout.Hut b = huts.get(j);
                    assertTrue(
                            chebyshev(a.dx() - b.dx(), a.dz() - b.dz()) > 2 * ROOF_HALF,
                            "hut roofs overlap, seed " + seed);
                }
                for (EndlingVillageLayout.Site cap : glowcaps) {
                    assertTrue(
                            chebyshev(a.dx() - cap.dx(), a.dz() - cap.dz()) > ROOF_HALF + CAP_HALF,
                            "hut overlaps a glowcap, seed " + seed);
                }
            }
            for (int i = 0; i < glowcaps.size(); i++) {
                for (int j = i + 1; j < glowcaps.size(); j++) {
                    EndlingVillageLayout.Site a = glowcaps.get(i);
                    EndlingVillageLayout.Site b = glowcaps.get(j);
                    assertTrue(
                            chebyshev(a.dx() - b.dx(), a.dz() - b.dz()) > 2 * CAP_HALF,
                            "glowcaps overlap, seed " + seed);
                }
            }
        }
        for (int n = EndlingVillageLayout.MIN_HUTS; n <= EndlingVillageLayout.MAX_HUTS; n++) {
            assertTrue(hutCounts[n], "never " + n + " huts");
        }
        for (int n = EndlingVillageLayout.MIN_GLOWCAPS; n <= EndlingVillageLayout.MAX_GLOWCAPS; n++) {
            assertTrue(glowcapCounts[n], "never " + n + " glowcaps");
        }
    }

    @Test
    void doorsFaceTheCenter() {
        assertEquals(3, EndlingVillageLayout.doorFacingCenter(14, 2)); // east of center: door on the west side
        assertEquals(1, EndlingVillageLayout.doorFacingCenter(-14, 2));
        assertEquals(0, EndlingVillageLayout.doorFacingCenter(2, 14)); // south of center: door on the north side
        assertEquals(2, EndlingVillageLayout.doorFacingCenter(2, -14));
    }

    @Test
    void villagesStayOutOfTheInnerEnd() {
        assertFalse(EndlingVillageLayout.farEnough(1030, 0));
        assertFalse(EndlingVillageLayout.farEnough(700, 700));
        assertTrue(EndlingVillageLayout.farEnough(0, -1100));
    }

    private static int chebyshev(int dx, int dz) {
        return Math.max(Math.abs(dx), Math.abs(dz));
    }
}
