package io.mcmaster.someonesperfectend.enchanting;

/** Registry-free Void Cleansing rules (US-036), so they can be unit-tested without bootstrapping the game. */
public final class VoidCleansingRules {
    /** Void Cleansing is always offered in the third offer slot. */
    public static final int OFFER_SLOT = 2;
    /** The level requirement shown on the offer. */
    public static final int LEVEL_REQUIREMENT = 30;
    /** The levels consumed by applying it. */
    public static final int LEVEL_COST = 3;
    /** The dragon's breath consumed by applying it. */
    public static final int BREATH_COST = 1;
    /** Enchanting power (bookshelves) the table needs: the vanilla maximum. */
    public static final int REQUIRED_POWER = 15;
    /** The {@code enchantClue} value marking the Void Cleansing offer (vanilla uses -1 for "no clue"). */
    public static final int CLUE = -2;

    private VoidCleansingRules() {
    }

    /** Whether the table offers Void Cleansing for an item with this repair cost. */
    public static boolean offered(int repairCost, int power) {
        return repairCost > 0 && power >= REQUIRED_POWER;
    }

    /** Whether the player may apply the offer. Creative mode skips the level check. */
    public static boolean canApply(int experienceLevel, boolean creative) {
        return creative || experienceLevel >= LEVEL_REQUIREMENT;
    }

    /** The repair cost after cleansing: the reverse of an anvil use ({@code cost * 2 + 1}). */
    public static int cleansedRepairCost(int repairCost) {
        return Math.max(0, (repairCost - 1) / 2);
    }
}
