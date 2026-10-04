package io.mcmaster.fwhipsperfectendupdate.enchanting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class VoidCleansingRulesTest {
    @Test
    void repairCostStepsDownToZero() {
        List<Integer> steps = new ArrayList<>();
        for (int cost = 63; cost > 0; cost = VoidCleansingRules.cleansedRepairCost(cost)) {
            steps.add(cost);
        }
        steps.add(0);
        assertEquals(List.of(63, 31, 15, 7, 3, 1, 0), steps);
    }

    @Test
    void cleansingReversesAnAnvilUse() {
        for (int cost = 0; cost < 1000; cost++) {
            assertEquals(cost, VoidCleansingRules.cleansedRepairCost(EndChanterRules.nextRepairCost(cost)));
        }
        assertEquals(1, VoidCleansingRules.cleansedRepairCost(4), "integer division");
        assertEquals(0, VoidCleansingRules.cleansedRepairCost(0));
    }

    @Test
    void offeredOnlyWithARepairCostAndFullPower() {
        assertTrue(VoidCleansingRules.offered(1, 15));
        assertTrue(VoidCleansingRules.offered(63, 15));
        assertFalse(VoidCleansingRules.offered(0, 15), "repair cost 0 gets no option");
        assertFalse(VoidCleansingRules.offered(31, 14), "fewer than 15 bookshelves");
    }

    @Test
    void needs30LevelsOutsideCreative() {
        assertTrue(VoidCleansingRules.canApply(30, false));
        assertFalse(VoidCleansingRules.canApply(29, false));
        assertTrue(VoidCleansingRules.canApply(0, true));
    }
}
