package io.mcmaster.someonesperfectend.enchanting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/** Registry-free End-chanter rules (US-035), so they can be unit-tested without bootstrapping the game. */
public final class EndChanterRules {
    /** Enchanted items are accepted only while their repair cost is below this. */
    public static final int MAX_REPAIR_COST = 40;

    private EndChanterRules() {
    }

    public static boolean acceptsRepairCost(int repairCost) {
        return repairCost < MAX_REPAIR_COST;
    }

    /** The repair cost after re-enchanting: the same increase as one anvil use. */
    public static int nextRepairCost(int repairCost) {
        return repairCost * 2 + 1;
    }

    /**
     * Filters one vanilla offer against the enchantments already on the item. An enchantment already on the item is
     * kept only at a higher level (applying it upgrades the existing one); a new enchantment is kept only if it is
     * compatible with every existing one. The order of the offer is preserved.
     *
     * @param offered    the enchantments of the vanilla offer
     * @param existing   the item's enchantments and their levels
     * @param compatible whether two different enchantments may be on the same item
     */
    public static <T, E> List<T> filterOffer(List<T> offered, Function<T, E> enchantment, ToIntFunction<T> level,
            Map<E, Integer> existing, BiPredicate<E, E> compatible) {
        List<T> kept = new ArrayList<>();
        for (T offer : offered) {
            E ench = enchantment.apply(offer);
            Integer current = existing.get(ench);
            if (current != null) {
                if (level.applyAsInt(offer) > current) {
                    kept.add(offer);
                }
            } else if (existing.keySet().stream().allMatch(other -> compatible.test(ench, other))) {
                kept.add(offer);
            }
        }
        return kept;
    }
}
