package io.mcmaster.someonesperfectend.gametest;

import io.mcmaster.someonesperfectend.enchanting.VoidCleansingRules;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnchantingTableBlock;

/**
 * Void Cleansing (US-036). The enchanting table is at {@link #TABLE} with a dragon head at bookshelf offset (2, 0, 0)
 * and bookshelves at the other bookshelf positions. Menus are driven directly with a mock player.
 */
public final class VoidCleansingGameTests {
    private static final BlockPos TABLE = new BlockPos(3, 1, 3);
    private static final BlockPos HEAD_OFFSET = new BlockPos(2, 0, 0);

    private VoidCleansingGameTests() {}

    /** A table with {@code bookshelves} bookshelves and, optionally, a dragon head. */
    private static void table(GameTestHelper helper, int bookshelves, boolean head) {
        helper.setBlock(TABLE, Blocks.ENCHANTING_TABLE);
        if (head) {
            helper.setBlock(TABLE.offset(HEAD_OFFSET), Blocks.DRAGON_HEAD);
        }
        int placed = 0;
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            if (placed < bookshelves && !offset.equals(HEAD_OFFSET)) {
                helper.setBlock(TABLE.offset(offset), Blocks.BOOKSHELF);
                placed++;
            }
        }
    }

    private static Player player(GameTestHelper helper, int levels) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.experienceLevel = levels;
        return player;
    }

    private static EnchantmentMenu menu(GameTestHelper helper, Player player) {
        return new EnchantmentMenu(
                0, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(TABLE)));
    }

    /** Opens a menu with the given dragon's breath count (placed directly, bypassing the slot check) and item. */
    private static EnchantmentMenu menu(GameTestHelper helper, Player player, int breath, ItemStack item) {
        EnchantmentMenu menu = menu(helper, player);
        menu.getSlot(1).set(new ItemStack(Items.DRAGON_BREATH, breath));
        menu.getSlot(0).set(item);
        return menu;
    }

    private static ItemStack withRepairCost(ItemStack stack, int repairCost) {
        stack.set(DataComponents.REPAIR_COST, repairCost);
        return stack;
    }

    private static ItemStack mendingPickaxe(GameTestHelper helper, int repairCost) {
        ItemStack stack = new ItemStack(Items.IRON_PICKAXE);
        stack.enchant(
                helper.getLevel()
                        .registryAccess()
                        .registryOrThrow(Registries.ENCHANTMENT)
                        .getHolderOrThrow(Enchantments.MENDING),
                1);
        return withRepairCost(stack, repairCost);
    }

    private static int repairCost(ItemStack stack) {
        return stack.getOrDefault(DataComponents.REPAIR_COST, 0);
    }

    private static boolean offersVoidCleansing(EnchantmentMenu menu) {
        return menu.costs[0] == 0
                && menu.costs[1] == 0
                && menu.costs[2] == VoidCleansingRules.LEVEL_REQUIREMENT
                && menu.enchantClue[2] == VoidCleansingRules.CLUE
                && menu.enchantClue[0] == -1
                && menu.enchantClue[1] == -1;
    }

    private static boolean noOffers(EnchantmentMenu menu) {
        return Arrays.stream(menu.costs).allMatch(cost -> cost == 0);
    }

    @GameTest
    public static void repairCost31BecomesFifteen(GameTestHelper helper) {
        table(helper, 15, true);
        Player player = player(helper, 30);
        EnchantmentMenu menu = menu(helper, player, 2, mendingPickaxe(helper, 31));
        helper.assertTrue(
                offersVoidCleansing(menu),
                "slot 3 should offer Void Cleansing, costs " + Arrays.toString(menu.costs) + " clues "
                        + Arrays.toString(menu.enchantClue));
        helper.assertFalse(menu.clickMenuButton(player, 0), "slots 1 and 2 have no offer");
        ItemEnchantments before = menu.getSlot(0).getItem().getEnchantments();

        helper.assertTrue(menu.clickMenuButton(player, 2), "Void Cleansing should apply");
        ItemStack result = menu.getSlot(0).getItem();
        helper.assertTrue(
                repairCost(result) == 15, "repair cost should be (31 - 1) / 2 = 15, was " + repairCost(result));
        helper.assertTrue(result.getEnchantments().equals(before), "enchantments should be unchanged");
        helper.assertTrue(
                menu.getSlot(1).getItem().is(Items.DRAGON_BREATH)
                        && menu.getSlot(1).getItem().getCount() == 1,
                "1 dragon's breath should be consumed");
        helper.assertTrue(
                player.experienceLevel == 27, "3 levels should be consumed, level is " + player.experienceLevel);
        helper.assertTrue(offersVoidCleansing(menu), "repair cost 15 can be cleansed again");

        player.experienceLevel = 30;
        helper.assertTrue(menu.clickMenuButton(player, 2), "cleansing with the last dragon's breath should work");
        helper.assertTrue(repairCost(menu.getSlot(0).getItem()) == 7, "15 → 7");
        helper.assertTrue(menu.getSlot(1).getItem().isEmpty(), "the dragon's breath should be used up");
        helper.assertFalse(menu.clickMenuButton(player, 2), "no dragon's breath left");
        helper.succeed();
    }

    @GameTest
    public static void worksOnEnchantedBooksAndUnenchantedItems(GameTestHelper helper) {
        table(helper, 15, true);
        Player player = player(helper, 30);
        EnchantmentMenu book = menu(helper, player, 1, withRepairCost(new ItemStack(Items.ENCHANTED_BOOK), 7));
        helper.assertTrue(offersVoidCleansing(book), "an enchanted book with a repair cost should be offered");
        helper.assertTrue(book.clickMenuButton(player, 2), "cleansing a book should work");
        helper.assertTrue(repairCost(book.getSlot(0).getItem()) == 3, "7 → 3");

        player.experienceLevel = 30;
        EnchantmentMenu sword = menu(helper, player, 2, withRepairCost(new ItemStack(Items.IRON_SWORD), 1));
        helper.assertTrue(offersVoidCleansing(sword), "an unenchanted item with a repair cost should be offered");
        helper.assertTrue(sword.clickMenuButton(player, 2), "cleansing an unenchanted item should work");
        helper.assertTrue(repairCost(sword.getSlot(0).getItem()) == 0, "1 → 0");
        helper.assertFalse(sword.getSlot(0).getItem().isEnchanted(), "no enchantment added");
        helper.assertTrue(noOffers(sword), "repair cost 0: no option");
        helper.succeed();
    }

    @GameTest
    public static void repairCostZeroGetsNoOption(GameTestHelper helper) {
        table(helper, 15, true);
        Player player = player(helper, 30);
        EnchantmentMenu menu = menu(helper, player, 3, mendingPickaxe(helper, 0));
        helper.assertTrue(
                noOffers(menu), "repair cost 0: no Void Cleansing and no vanilla offers with dragon's breath");
        helper.assertFalse(menu.clickMenuButton(player, 2), "nothing to apply");
        EnchantmentMenu plain = menu(helper, player, 3, new ItemStack(Items.IRON_PICKAXE));
        helper.assertTrue(noOffers(plain), "dragon's breath is never spent on vanilla enchanting");
        helper.assertTrue(plain.getSlot(1).getItem().getCount() == 3, "dragon's breath untouched");
        helper.succeed();
    }

    @GameTest
    public static void fewerThan15BookshelvesGiveNoOption(GameTestHelper helper) {
        table(helper, 14, true);
        Player player = player(helper, 30);
        EnchantmentMenu menu = menu(helper, player, 1, mendingPickaxe(helper, 31));
        helper.assertTrue(noOffers(menu), "14 bookshelves: no option, costs " + Arrays.toString(menu.costs));
        helper.assertFalse(menu.clickMenuButton(player, 2), "nothing to apply");
        helper.assertTrue(repairCost(menu.getSlot(0).getItem()) == 31, "repair cost unchanged");

        table(helper, 15, true);
        EnchantmentMenu full = menu(helper, player, 1, mendingPickaxe(helper, 31));
        helper.assertTrue(offersVoidCleansing(full), "15 bookshelves: option offered");
        helper.succeed();
    }

    @GameTest
    public static void needs30LevelsOutsideCreative(GameTestHelper helper) {
        table(helper, 15, true);
        Player player = player(helper, 29);
        EnchantmentMenu menu = menu(helper, player, 1, mendingPickaxe(helper, 31));
        helper.assertTrue(offersVoidCleansing(menu), "the option is shown with its level requirement");
        helper.assertFalse(menu.clickMenuButton(player, 2), "29 levels are not enough");
        helper.assertTrue(repairCost(menu.getSlot(0).getItem()) == 31, "repair cost unchanged");

        Player creative = helper.makeMockPlayer(GameType.CREATIVE);
        creative.getAbilities().instabuild = true;
        creative.experienceLevel = 0;
        EnchantmentMenu creativeMenu = menu(helper, creative, 1, mendingPickaxe(helper, 31));
        helper.assertTrue(creativeMenu.clickMenuButton(creative, 2), "creative mode skips the level check");
        helper.assertTrue(repairCost(creativeMenu.getSlot(0).getItem()) == 15, "31 → 15 in creative");
        helper.succeed();
    }

    @GameTest
    public static void dragonBreathRejectedWithoutDragonHead(GameTestHelper helper) {
        table(helper, 15, false);
        Player player = player(helper, 30);
        EnchantmentMenu menu = menu(helper, player);
        ItemStack breath = new ItemStack(Items.DRAGON_BREATH);
        helper.assertFalse(menu.getSlot(1).mayPlace(breath), "no dragon head: dragon's breath is rejected");
        helper.assertTrue(menu.getSlot(1).mayPlace(new ItemStack(Items.LAPIS_LAZULI)), "lapis is still accepted");
        helper.assertTrue(menu.getSlot(1).getNoItemIcon() != null, "the lapis slot keeps its empty icon");

        player.getInventory().setItem(0, breath.copyWithCount(4));
        menu.quickMoveStack(player, 29);
        helper.assertTrue(menu.getSlot(1).getItem().isEmpty(), "shift-click must not put breath in the lapis slot");

        EnchantmentMenu forced = menu(helper, player, 1, mendingPickaxe(helper, 31));
        helper.assertTrue(noOffers(forced), "no dragon head: no Void Cleansing");

        helper.setBlock(TABLE.offset(HEAD_OFFSET), Blocks.DRAGON_HEAD);
        EnchantmentMenu active = menu(helper, player);
        helper.assertTrue(active.getSlot(1).mayPlace(breath), "an active End-chanter accepts dragon's breath");
        player.getInventory().setItem(0, breath.copyWithCount(4));
        active.quickMoveStack(player, 29);
        helper.assertTrue(
                active.getSlot(1).getItem().is(Items.DRAGON_BREATH)
                        && active.getSlot(1).getItem().getCount() == 4,
                "shift-click moves dragon's breath to the lapis slot");
        helper.assertTrue(active.getSlot(0).getItem().isEmpty(), "not into the item slot");
        helper.succeed();
    }
}
