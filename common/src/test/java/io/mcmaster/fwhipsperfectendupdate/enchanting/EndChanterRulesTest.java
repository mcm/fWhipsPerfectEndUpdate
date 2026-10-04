package io.mcmaster.fwhipsperfectendupdate.enchanting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import org.junit.jupiter.api.Test;

class EndChanterRulesTest {
    private record Offer(String enchantment, int level) {}

    /** Sharpness, Smite, and Bane of Arthropods exclude each other; everything else is compatible. */
    private static final Set<String> DAMAGE = Set.of("sharpness", "smite", "bane_of_arthropods");

    private static final BiPredicate<String, String> COMPATIBLE =
            (a, b) -> !a.equals(b) && !(DAMAGE.contains(a) && DAMAGE.contains(b));

    private static final Map<String, Integer> SHARPNESS_III_SWORD = Map.of("sharpness", 3);

    private static List<Offer> filter(Map<String, Integer> existing, Offer... offered) {
        return EndChanterRules.filterOffer(List.of(offered), Offer::enchantment, Offer::level, existing, COMPATIBLE);
    }

    @Test
    void higherLevelOfAnExistingEnchantmentIsKeptAsAnUpgrade() {
        assertEquals(List.of(new Offer("sharpness", 4)), filter(SHARPNESS_III_SWORD, new Offer("sharpness", 4)));
    }

    @Test
    void incompatibleEnchantmentIsDropped() {
        assertEquals(List.of(), filter(SHARPNESS_III_SWORD, new Offer("smite", 4)));
    }

    @Test
    void newCompatibleEnchantmentIsAdded() {
        assertEquals(List.of(new Offer("unbreaking", 2)), filter(SHARPNESS_III_SWORD, new Offer("unbreaking", 2)));
    }

    @Test
    void sameOrLowerLevelOfAnExistingEnchantmentIsDropped() {
        assertEquals(List.of(), filter(SHARPNESS_III_SWORD, new Offer("sharpness", 3), new Offer("sharpness", 2)));
    }

    @Test
    void offersAreFilteredPerEnchantmentKeepingOrder() {
        assertEquals(
                List.of(new Offer("unbreaking", 1), new Offer("sharpness", 5)),
                filter(
                        SHARPNESS_III_SWORD,
                        new Offer("unbreaking", 1),
                        new Offer("smite", 2),
                        new Offer("sharpness", 5),
                        new Offer("sharpness", 1)));
    }

    @Test
    void repairCostRules() {
        assertTrue(EndChanterRules.acceptsRepairCost(0));
        assertTrue(EndChanterRules.acceptsRepairCost(31));
        assertTrue(EndChanterRules.acceptsRepairCost(39));
        assertFalse(EndChanterRules.acceptsRepairCost(40));
        assertFalse(EndChanterRules.acceptsRepairCost(63));
        assertEquals(63, EndChanterRules.nextRepairCost(31));
        assertEquals(1, EndChanterRules.nextRepairCost(0));
    }
}
