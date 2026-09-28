package io.mcmaster.someonesperfectend.gametest;

import io.mcmaster.someonesperfectend.SomeonesPerfectEnd;
import io.mcmaster.someonesperfectend.block.ModBlocks;
import io.mcmaster.someonesperfectend.entity.Endling;
import io.mcmaster.someonesperfectend.entity.EndlingBarters;
import io.mcmaster.someonesperfectend.entity.EndlingSpecialBarters;
import io.mcmaster.someonesperfectend.entity.Endlings;
import io.mcmaster.someonesperfectend.gametest.EndlingBarterGameTests.Site;
import io.mcmaster.someonesperfectend.totem.EffectTotems;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;

/** US-034: data-driven Endling special barters (beacon → Infuser, Totem of Undying → inactive effect totem). */
public final class EndlingSpecialBarterGameTests {
    private static final int TIMEOUT = 200;
    private static final ResourceKey<LootTable> BEACON_TABLE = ResourceKey.create(Registries.LOOT_TABLE,
            SomeonesPerfectEnd.id("gameplay/endling_barter/beacon"));
    private static final ResourceKey<LootTable> TOTEM_TABLE = ResourceKey.create(Registries.LOOT_TABLE,
            SomeonesPerfectEnd.id("gameplay/endling_barter/totem_of_undying"));

    private EndlingSpecialBarterGameTests() {
    }

    private static Set<Item> totems() {
        Set<Item> items = new HashSet<>();
        EffectTotems.ALL.forEach(totem -> items.add(totem.get()));
        return items;
    }

    /** Succeeds once an output matching {@code output} lies around the site and the input was consumed. */
    private static void expectOutput(GameTestHelper helper, Site site, Item input, Predicate<ItemStack> output) {
        helper.succeedWhen(() -> {
            List<ItemEntity> items = site.items();
            helper.assertTrue(items.stream().anyMatch(item -> output.test(item.getItem())),
                    "no special barter output yet (items: " + items.stream().map(ItemEntity::getItem).toList() + ")");
            helper.assertFalse(site.endling().isInspecting(), "the endling should be done inspecting");
            helper.assertTrue(items.stream().noneMatch(item -> item.getItem().is(input)),
                    "the input should have been consumed");
            site.close();
        });
    }

    @GameTest(template = ModGameTests.EMPTY_TEMPLATE)
    public static void specialBartersAreLoadedFromData(GameTestHelper helper) {
        helper.assertTrue(BEACON_TABLE.equals(EndlingSpecialBarters.lootTableFor(Items.BEACON)),
                "a beacon should map to the beacon barter table, got " + EndlingSpecialBarters.all());
        helper.assertTrue(TOTEM_TABLE.equals(EndlingSpecialBarters.lootTableFor(Items.TOTEM_OF_UNDYING)),
                "a Totem of Undying should map to the totem barter table, got " + EndlingSpecialBarters.all());
        for (Item item : List.of(Items.BEACON, Items.TOTEM_OF_UNDYING)) {
            ItemStack stack = new ItemStack(item);
            helper.assertTrue(EndlingBarters.isInput(stack), item + " should be accepted by endlings");
            helper.assertFalse(stack.is(EndlingBarters.INPUTS), item + " should not be a default barter input");
        }
        helper.succeed();
    }

    @GameTest(timeoutTicks = TIMEOUT)
    public static void droppedBeaconGivesInfuser(GameTestHelper helper) {
        Site site = Site.open(helper, 5);
        site.drop(new ItemStack(Items.BEACON), 2.0);
        expectOutput(helper, site, Items.BEACON,
                stack -> stack.is(ModBlocks.INFUSER.get().asItem()) && stack.getCount() == 1);
    }

    @GameTest(timeoutTicks = TIMEOUT)
    public static void handedOverTotemGivesInactiveEffectTotem(GameTestHelper helper) {
        Site site = Site.open(helper, 6);
        Player player = EndlingBarterGameTests.serverPlayerIn(site.end(), new ItemStack(Items.TOTEM_OF_UNDYING), true);
        helper.assertTrue(site.endling().interact(player, InteractionHand.MAIN_HAND).consumesAction(),
                "handing over a Totem of Undying should succeed");
        helper.assertTrue(player.getMainHandItem().isEmpty(), "the totem should be taken");
        helper.assertTrue(site.endling().inspectTicks() == Endling.INSPECT_TICKS, "the endling should inspect it");
        Set<Item> totems = totems();
        expectOutput(helper, site, Items.TOTEM_OF_UNDYING,
                stack -> totems.contains(stack.getItem()) && stack.getCount() == 1 && !EffectTotems.isActive(stack));
    }

    @GameTest(timeoutTicks = TIMEOUT)
    public static void lostEndlingDoesNotAcceptBeacon(GameTestHelper helper) {
        Site site = Site.open(helper, 7);
        site.endling().setLost();
        ItemEntity dropped = site.drop(new ItemStack(Items.BEACON), 0.0);
        Player player = EndlingBarterGameTests.serverPlayerIn(site.end(), new ItemStack(Items.BEACON), true);
        helper.assertFalse(site.endling().interact(player, InteractionHand.MAIN_HAND).consumesAction(),
                "a Lost endling should refuse a beacon");
        helper.assertTrue(player.getMainHandItem().is(Items.BEACON), "a Lost endling takes nothing");
        helper.runAfterDelay(100, () -> {
            helper.assertFalse(site.endling().isInspecting(), "a Lost endling should not pick up a beacon");
            helper.assertTrue(dropped.isAlive(), "the beacon should stay on the ground");
            helper.assertTrue(site.items().stream().noneMatch(item -> item.getItem().is(ModBlocks.INFUSER.get().asItem())),
                    "a Lost endling should give nothing");
            site.close();
            helper.succeed();
        });
    }

    /** The GameTest level is the Overworld: endlings there ignore special barter inputs too. */
    @GameTest(template = ModGameTests.EMPTY_TEMPLATE)
    public static void endlingOutsideTheEndRefusesBeacon(GameTestHelper helper) {
        Endling endling = helper.spawn(Endlings.TYPE.get(), new Vec3(3.5, 1.0, 3.5));
        Player player = EndlingBarterGameTests.serverPlayerIn(helper.getLevel(), new ItemStack(Items.BEACON), true);
        helper.assertFalse(endling.interact(player, InteractionHand.MAIN_HAND).consumesAction(),
                "an endling outside the End should refuse a beacon");
        endling.discard();
        helper.succeed();
    }

    /** Rolls the totem table 200 times: always exactly one inactive effect totem, and every totem shows up. */
    @GameTest(template = ModGameTests.EMPTY_TEMPLATE)
    public static void totemLootTableGivesEveryTotem(GameTestHelper helper) {
        Endling endling = helper.spawn(Endlings.TYPE.get(), new Vec3(3.5, 1.0, 3.5));
        Set<Item> expected = totems();
        Set<Item> seen = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            List<ItemStack> roll = EndlingBarters.roll(endling, TOTEM_TABLE);
            helper.assertTrue(roll.size() == 1 && roll.get(0).getCount() == 1,
                    "one totem barter should give exactly one totem, gave " + roll);
            ItemStack totem = roll.get(0);
            helper.assertTrue(expected.contains(totem.getItem()), "unexpected totem barter output " + totem);
            helper.assertFalse(EffectTotems.isActive(totem), "bartered totems should be inactive");
            seen.add(totem.getItem());
        }
        helper.assertTrue(seen.equals(expected), "every effect totem should be bartered, missing "
                + expected.stream().filter(item -> !seen.contains(item)).toList());
        List<ItemStack> beacon = EndlingBarters.roll(endling, BEACON_TABLE);
        helper.assertTrue(beacon.size() == 1 && beacon.get(0).is(ModBlocks.INFUSER.get().asItem())
                && beacon.get(0).getCount() == 1, "a beacon barter should give one infuser, gave " + beacon);
        endling.discard();
        helper.succeed();
    }
}
