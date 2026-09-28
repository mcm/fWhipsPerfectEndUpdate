package io.mcmaster.someonesperfectend.gametest;

import io.mcmaster.someonesperfectend.item.ModItems;
import io.mcmaster.someonesperfectend.trinket.TrinketSlot;
import io.mcmaster.someonesperfectend.trinket.Trinkets;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.impl.AccessoriesEventHandler;
import java.util.Set;
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
        ItemStack charm = new ItemStack(ModItems.RESONANCE_CHARM.get());
        helper.assertTrue(charm.is(Trinkets.TRINKETS), "resonance charm should be tagged as a trinket");
        helper.assertTrue(trinket.mayPlace(charm), "the trinket slot should accept the resonance charm");
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
        player.getInventory().setItem(9, new ItemStack(ModItems.RESONANCE_CHARM.get()));
        int inventorySlot = 9; // inventory index 9 is menu slot 9 (first main-inventory slot)

        menu.quickMoveStack(player, inventorySlot);
        helper.assertTrue(trinket.getItem().is(ModItems.RESONANCE_CHARM.get()), "shift-click should equip the charm");
        helper.assertTrue(menu.getSlot(inventorySlot).getItem().isEmpty(), "the charm should leave the inventory");
        // The slot is backed by the Accessories container, so other mods using the library see the item.
        helper.assertTrue(Trinkets.getEquipped(player).is(ModItems.RESONANCE_CHARM.get()),
                "the charm should be in the Accessories trinket container");

        // With the slot full, another trinket stays in the inventory (vanilla shift-click rules move it instead).
        player.getInventory().setItem(10, new ItemStack(ModItems.RESONANCE_CHARM.get()));
        menu.quickMoveStack(player, 10);
        helper.assertTrue(Trinkets.getEquipped(player).getCount() == 1, "the trinket slot holds one item");

        // Shift-clicking out of the trinket slot puts it back into the inventory.
        menu.quickMoveStack(player, trinket.index);
        helper.assertTrue(trinket.getItem().isEmpty(), "shift-click should unequip the charm");
        helper.assertTrue(player.getInventory().countItem(ModItems.RESONANCE_CHARM.get()) == 2,
                "both charms should be in the inventory");

        // Non-trinkets are never shift-clicked into the slot.
        player.getInventory().setItem(11, new ItemStack(Items.DIAMOND));
        menu.quickMoveStack(player, 11);
        helper.assertTrue(trinket.getItem().isEmpty(), "a diamond must not go into the trinket slot");
        helper.succeed();
    }

    @GameTest
    public static void resonanceCharmGivesHasteWhileEquipped(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Slot trinket = trinketSlot(helper, player);

        trinket.setByPlayer(new ItemStack(ModItems.RESONANCE_CHARM.get()));
        tickAccessories(player);
        MobEffectInstance haste = player.getEffect(MobEffects.DIG_SPEED);
        helper.assertTrue(haste != null && haste.getAmplifier() == 0, "equipping the charm should give Haste I");

        trinket.setByPlayer(ItemStack.EMPTY);
        tickAccessories(player);
        helper.assertFalse(player.hasEffect(MobEffects.DIG_SPEED), "unequipping the charm should remove Haste");
        helper.succeed();
    }

    @GameTest
    public static void resonanceCharmKeepsHasteRefreshed(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        trinketSlot(helper, player).setByPlayer(new ItemStack(ModItems.RESONANCE_CHARM.get()));
        // Simulate 200 ticks of wearing it: the effect never runs out.
        for (int tick = 0; tick < 200; tick++) {
            tickAccessories(player);
            MobEffectInstance haste = player.getEffect(MobEffects.DIG_SPEED);
            helper.assertTrue(haste != null, "Haste should stay active while the charm is worn (tick " + tick + ")");
            haste.tick(player, () -> {
            });
        }
        helper.succeed();
    }
}
