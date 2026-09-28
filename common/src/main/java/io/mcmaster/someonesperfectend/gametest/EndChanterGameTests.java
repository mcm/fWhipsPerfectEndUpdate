package io.mcmaster.someonesperfectend.gametest;

import io.mcmaster.someonesperfectend.enchanting.EndChanter;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.IdMap;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/**
 * The End-chanter (US-035). The enchanting table is at {@link #TABLE}; a dragon head at bookshelf offset (2, 0, 0)
 * sits at {@link #HEAD} with {@link #BETWEEN} in between. Menus are driven directly with a mock player.
 */
public final class EndChanterGameTests {
    private static final BlockPos TABLE = new BlockPos(3, 1, 3);
    private static final BlockPos BETWEEN = new BlockPos(4, 1, 3);
    private static final BlockPos HEAD = new BlockPos(5, 1, 3);

    private EndChanterGameTests() {
    }

    private static void table(GameTestHelper helper, boolean head) {
        helper.setBlock(TABLE, Blocks.ENCHANTING_TABLE);
        if (head) {
            helper.setBlock(HEAD, Blocks.DRAGON_HEAD);
        }
    }

    private static boolean active(GameTestHelper helper) {
        return EndChanter.isActive(helper.getLevel(), helper.absolutePos(TABLE));
    }

    private static Player player(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.experienceLevel = 30;
        return player;
    }

    /** Opens a menu on the table with 3 lapis and the given item. */
    private static EnchantmentMenu menu(GameTestHelper helper, Player player, ItemStack item) {
        EnchantmentMenu menu = new EnchantmentMenu(0, player.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(TABLE)));
        menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 3));
        menu.getSlot(0).set(item);
        return menu;
    }

    private static Registry<Enchantment> enchantments(GameTestHelper helper) {
        return helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
    }

    private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
        return enchantments(helper).getHolderOrThrow(key);
    }

    /** An iron pickaxe with Mending I (compatible with every pickaxe table enchantment) and the given repair cost. */
    private static ItemStack mendingPickaxe(GameTestHelper helper, int repairCost) {
        ItemStack stack = new ItemStack(Items.IRON_PICKAXE);
        stack.enchant(enchantment(helper, Enchantments.MENDING), 1);
        stack.set(DataComponents.REPAIR_COST, repairCost);
        return stack;
    }

    private static boolean hasOffer(EnchantmentMenu menu) {
        return Arrays.stream(menu.costs).anyMatch(cost -> cost > 0);
    }

    private static int firstOffer(EnchantmentMenu menu) {
        for (int slot = 0; slot < menu.costs.length; slot++) {
            if (menu.costs[slot] > 0) {
                return slot;
            }
        }
        return -1;
    }

    @GameTest
    public static void dragonHeadWithAirInBetweenActivates(GameTestHelper helper) {
        table(helper, true);
        helper.assertTrue(active(helper), "a dragon head at a bookshelf position with air in between should activate");
        EnchantmentMenu menu = menu(helper, player(helper), mendingPickaxe(helper, 0));
        helper.assertTrue(hasOffer(menu), "an active End-chanter should make offers for an enchanted pickaxe");

        helper.setBlock(HEAD, Blocks.AIR);
        helper.setBlock(new BlockPos(2, 2, 1), Blocks.DRAGON_WALL_HEAD);
        helper.assertTrue(active(helper), "a dragon wall head on the upper bookshelf row should activate");
        helper.succeed();
    }

    @GameTest
    public static void solidBlockInBetweenDoesNotActivate(GameTestHelper helper) {
        table(helper, true);
        helper.setBlock(BETWEEN, Blocks.STONE);
        helper.assertFalse(active(helper), "a solid block between the head and the table should block it");
        EnchantmentMenu menu = menu(helper, player(helper), mendingPickaxe(helper, 0));
        helper.assertFalse(hasOffer(menu), "an inactive End-chanter should make no offers for an enchanted item");

        helper.setBlock(BETWEEN, Blocks.AIR);
        helper.setBlock(HEAD, Blocks.AIR);
        helper.setBlock(new BlockPos(2, 1, 3), Blocks.DRAGON_HEAD);
        helper.assertFalse(active(helper), "a head next to the table (not at a bookshelf position) should not count");
        helper.succeed();
    }

    @GameTest
    public static void noDragonHeadBehavesLikeVanilla(GameTestHelper helper) {
        table(helper, false);
        Player player = player(helper);
        helper.assertFalse(active(helper), "no head, no End-chanter");
        helper.assertFalse(hasOffer(menu(helper, player, mendingPickaxe(helper, 0))),
                "vanilla makes no offers for an enchanted item");
        EnchantmentMenu enchantedBook = menu(helper, player, new ItemStack(Items.ENCHANTED_BOOK));
        helper.assertFalse(hasOffer(enchantedBook), "no offers for an enchanted book");

        EnchantmentMenu vanilla = menu(helper, player, new ItemStack(Items.IRON_PICKAXE));
        int[] costs = vanilla.costs.clone();
        int[] clues = vanilla.enchantClue.clone();
        int[] levels = vanilla.levelClue.clone();
        helper.assertTrue(hasOffer(vanilla), "an unenchanted pickaxe should get vanilla offers");

        // Two dragon heads: same offers (no enchanting power, no filtering) and no repair cost for unenchanted items.
        helper.setBlock(HEAD, Blocks.DRAGON_HEAD);
        helper.setBlock(new BlockPos(1, 1, 5), Blocks.DRAGON_HEAD);
        EnchantmentMenu endChanter = menu(helper, player, new ItemStack(Items.IRON_PICKAXE));
        helper.assertTrue(Arrays.equals(costs, endChanter.costs) && Arrays.equals(clues, endChanter.enchantClue)
                && Arrays.equals(levels, endChanter.levelClue),
                "dragon heads should not change offers for unenchanted items: " + Arrays.toString(costs) + " vs "
                        + Arrays.toString(endChanter.costs));
        EnchantmentMenu book = menu(helper, player, new ItemStack(Items.ENCHANTED_BOOK));
        helper.assertFalse(hasOffer(book), "enchanted books cannot be re-enchanted");

        int slot = firstOffer(endChanter);
        helper.assertTrue(endChanter.clickMenuButton(player, slot), "enchanting an unenchanted pickaxe should work");
        ItemStack result = endChanter.getSlot(0).getItem();
        helper.assertTrue(result.isEnchanted(), "the pickaxe should be enchanted");
        helper.assertTrue(result.getOrDefault(DataComponents.REPAIR_COST, 0) == 0,
                "enchanting an unenchanted item leaves the repair cost at 0");
        helper.succeed();
    }

    @GameTest
    public static void repairCost31IsAcceptedAndEndsAt63(GameTestHelper helper) {
        table(helper, true);
        Player player = player(helper);
        EnchantmentMenu menu = menu(helper, player, mendingPickaxe(helper, 31));
        int slot = firstOffer(menu);
        helper.assertTrue(slot >= 0, "a Mending pickaxe with repair cost 31 should get an offer");
        int cost = slot + 1;
        helper.assertTrue(menu.enchantClue[slot] >= 0, "the offer should have a clue");

        helper.assertTrue(menu.clickMenuButton(player, slot), "applying the offer should work");
        ItemStack result = menu.getSlot(0).getItem();
        helper.assertTrue(result.getOrDefault(DataComponents.REPAIR_COST, 0) == 63,
                "repair cost should be 31 * 2 + 1 = 63, was " + result.get(DataComponents.REPAIR_COST));
        helper.assertTrue(result.getEnchantments().getLevel(enchantment(helper, Enchantments.MENDING)) == 1,
                "Mending should be kept");
        helper.assertTrue(result.getEnchantments().size() > 1, "an enchantment should have been added");
        helper.assertTrue(menu.getSlot(1).getItem().getCount() == 3 - cost, "the vanilla lapis cost should be charged");
        helper.assertTrue(player.experienceLevel == 30 - cost, "the vanilla XP cost should be charged, level is "
                + player.experienceLevel);
        helper.assertFalse(hasOffer(menu), "at repair cost 63 the item should no longer get offers");
        helper.succeed();
    }

    @GameTest
    public static void repairCost63IsRejected(GameTestHelper helper) {
        table(helper, true);
        Player player = player(helper);
        helper.assertFalse(hasOffer(menu(helper, player, mendingPickaxe(helper, 63))), "repair cost 63: no offers");
        helper.assertFalse(hasOffer(menu(helper, player, mendingPickaxe(helper, 40))), "repair cost 40: no offers");
        helper.assertTrue(hasOffer(menu(helper, player, mendingPickaxe(helper, 39))), "repair cost 39: offers");
        helper.succeed();
    }

    @GameTest
    public static void offersAreFilteredAgainstExistingEnchantments(GameTestHelper helper) {
        table(helper, true);
        Player player = player(helper);
        Holder<Enchantment> sharpness = enchantment(helper, Enchantments.SHARPNESS);
        Holder<Enchantment> smite = enchantment(helper, Enchantments.SMITE);
        Holder<Enchantment> bane = enchantment(helper, Enchantments.BANE_OF_ARTHROPODS);
        IdMap<Holder<Enchantment>> ids = enchantments(helper).asHolderIdMap();
        // Try a few enchantment seeds so at least one offer survives the filter.
        boolean applied = false;
        for (int attempt = 0; attempt < 20 && !applied; attempt++) {
            player.onEnchantmentPerformed(ItemStack.EMPTY, 0);
            ItemStack sword = new ItemStack(Items.IRON_SWORD);
            sword.enchant(sharpness, 5);
            EnchantmentMenu menu = menu(helper, player, sword);
            for (int slot = 0; slot < 3; slot++) {
                if (menu.costs[slot] > 0) {
                    Holder<Enchantment> clue = ids.byId(menu.enchantClue[slot]);
                    helper.assertTrue(clue != null && !clue.equals(sharpness) && !clue.equals(smite)
                            && !clue.equals(bane), "a Sharpness V sword should never be offered " + clue);
                }
            }
            int slot = firstOffer(menu);
            if (slot >= 0) {
                player.experienceLevel = 30;
                helper.assertTrue(menu.clickMenuButton(player, slot), "applying the offer should work");
                ItemStack result = menu.getSlot(0).getItem();
                helper.assertTrue(result.getEnchantments().getLevel(sharpness) == 5, "Sharpness V should be kept");
                helper.assertTrue(result.getEnchantments().getLevel(smite) == 0
                        && result.getEnchantments().getLevel(bane) == 0, "no incompatible enchantment added");
                helper.assertTrue(result.getEnchantments().size() > 1, "something should have been added");
                applied = true;
            }
        }
        helper.assertTrue(applied, "some seed should give an offer for a Sharpness V sword");
        helper.succeed();
    }
}
