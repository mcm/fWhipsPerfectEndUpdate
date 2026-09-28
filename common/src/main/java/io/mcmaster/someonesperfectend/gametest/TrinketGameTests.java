package io.mcmaster.someonesperfectend.gametest;

import io.mcmaster.someonesperfectend.totem.EffectTotems;
import io.mcmaster.someonesperfectend.trinket.TrinketSlot;
import io.mcmaster.someonesperfectend.trinket.Trinkets;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.impl.AccessoriesEventHandler;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

public final class TrinketGameTests {
    private TrinketGameTests() {
    }

    private static Slot trinketSlot(GameTestHelper helper, Player player) {
        int index = TrinketSlot.indexIn(player.inventoryMenu);
        helper.assertTrue(index >= 0, "the player inventory menu should have a trinket slot");
        return player.inventoryMenu.getSlot(index);
    }

    /**
     * Runs the Accessories per-entity tick (equip/unequip detection and accessory ticking) for a mock player,
     * which isn't in the world and so isn't ticked by the server.
     */
    private static void tickAccessories(Player player) {
        AccessoriesEventHandler.onLivingEntityTick(player);
    }

    /** An active Totem of Haste: Haste I while worn (replaced the US-024 test charm). */
    private static ItemStack hasteTotem() {
        return EffectTotems.activated(EffectTotems.TOTEM_OF_HASTE.get());
    }

    @GameTest
    public static void trinketIsTheOnlyAccessorySlot(GameTestHelper helper) {
        Set<String> expected = Set.of(Trinkets.SLOT_NAME);
        Set<String> playerSlots = EntitySlotLoader.getEntitySlots(helper.getLevel(), EntityType.PLAYER).keySet();
        helper.assertTrue(playerSlots.equals(expected), "player accessory slots should be only the trinket slot, got "
                + playerSlots);

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        AccessoriesCapability capability = AccessoriesCapability.get(player);
        helper.assertTrue(capability != null, "players should have an accessories capability");
        helper.assertTrue(capability.getContainers().keySet().equals(expected),
                "player accessory containers should be only the trinket slot, got " + capability.getContainers().keySet());

        // The library's default slots are bound to no entity (e.g. armor stands are a library default target).
        helper.assertTrue(EntitySlotLoader.getEntitySlots(helper.getLevel(), EntityType.ARMOR_STAND).isEmpty(),
                "armor stands should have no accessory slots");
        helper.succeed();
    }

    @GameTest
    public static void trinketSlotSitsAboveOffHand(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        InventoryMenu menu = player.inventoryMenu;
        Slot trinket = trinketSlot(helper, player);
        Slot offHand = menu.getSlot(InventoryMenu.SHIELD_SLOT);
        // Vanilla slot indices are unchanged; the trinket slot is added after them.
        helper.assertTrue(trinket.index == InventoryMenu.SHIELD_SLOT + 1, "trinket slot should follow the off hand");
        helper.assertTrue(trinket.x == offHand.x && trinket.y == offHand.y - 18,
                "trinket slot should be directly above the off-hand slot, was at " + trinket.x + "," + trinket.y);
        helper.succeed();
    }

    @GameTest
    public static void onlyTaggedTrinketsFit(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Slot trinket = trinketSlot(helper, player);
        ItemStack totem = hasteTotem();
        helper.assertTrue(totem.is(Trinkets.TRINKETS), "the haste totem should be tagged as a trinket");
        helper.assertTrue(trinket.mayPlace(totem), "the trinket slot should accept the haste totem");
        for (ItemStack other : new ItemStack[] {new ItemStack(Items.DIAMOND), new ItemStack(Items.TOTEM_OF_UNDYING),
                new ItemStack(Items.SHIELD), new ItemStack(Items.IRON_CHESTPLATE)}) {
            helper.assertFalse(trinket.mayPlace(other), "the trinket slot should refuse " + other);
        }
        helper.succeed();
    }

    @GameTest
    public static void shiftClickMovesTrinketIntoSlot(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Slot trinket = trinketSlot(helper, player);
        InventoryMenu menu = player.inventoryMenu;
        player.getInventory().setItem(9, hasteTotem());
        int inventorySlot = 9; // inventory index 9 is menu slot 9 (first main-inventory slot)

        menu.quickMoveStack(player, inventorySlot);
        helper.assertTrue(trinket.getItem().is(EffectTotems.TOTEM_OF_HASTE.get()), "shift-click should equip the totem");
        helper.assertTrue(menu.getSlot(inventorySlot).getItem().isEmpty(), "the totem should leave the inventory");
        // The slot is backed by the Accessories container, so other mods using the library see the item.
        helper.assertTrue(Trinkets.getEquipped(player).is(EffectTotems.TOTEM_OF_HASTE.get()),
                "the totem should be in the Accessories trinket container");

        // With the slot full, another trinket stays in the inventory (vanilla shift-click rules move it instead).
        player.getInventory().setItem(10, hasteTotem());
        menu.quickMoveStack(player, 10);
        helper.assertTrue(Trinkets.getEquipped(player).getCount() == 1, "the trinket slot holds one item");

        // Shift-clicking out of the trinket slot puts it back into the inventory.
        menu.quickMoveStack(player, trinket.index);
        helper.assertTrue(trinket.getItem().isEmpty(), "shift-click should unequip the totem");
        helper.assertTrue(player.getInventory().countItem(EffectTotems.TOTEM_OF_HASTE.get()) == 2,
                "both totems should be in the inventory");

        // Non-trinkets are never shift-clicked into the slot.
        player.getInventory().setItem(11, new ItemStack(Items.DIAMOND));
        menu.quickMoveStack(player, 11);
        helper.assertTrue(trinket.getItem().isEmpty(), "a diamond must not go into the trinket slot");
        helper.succeed();
    }

    @GameTest
    public static void hasteTotemGivesHasteWhileEquipped(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Slot trinket = trinketSlot(helper, player);

        trinket.setByPlayer(hasteTotem());
        tickAccessories(player);
        MobEffectInstance haste = player.getEffect(MobEffects.DIG_SPEED);
        helper.assertTrue(haste != null && haste.getAmplifier() == 0, "equipping the totem should give Haste I");

        trinket.setByPlayer(ItemStack.EMPTY);
        tickAccessories(player);
        helper.assertFalse(player.hasEffect(MobEffects.DIG_SPEED), "unequipping the totem should remove Haste");
        helper.succeed();
    }

    /** Worn in the level for longer than one application (340 ticks), the totem keeps re-applying Haste. */
    @GameTest(timeoutTicks = 500)
    public static void hasteTotemKeepsHasteRefreshed(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        trinketSlot(helper, player).setByPlayer(hasteTotem());
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        player.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        helper.getLevel().addFreshEntity(player);
        helper.runAfterDelay(420, () -> {
            MobEffectInstance haste = player.getEffect(MobEffects.DIG_SPEED);
            boolean refreshed = haste != null && haste.getAmplifier() == 0 && haste.getDuration() > 340 - 80 - 2;
            player.discard();
            helper.assertTrue(refreshed, "Haste I should stay refreshed while the totem is worn, was " + haste);
            helper.succeed();
        });
    }
}
