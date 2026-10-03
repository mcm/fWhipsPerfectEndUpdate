package io.mcmaster.someonesperfectend.gametest;

import com.mojang.authlib.GameProfile;
import io.mcmaster.someonesperfectend.infuser.InfuserMenu;
import io.mcmaster.someonesperfectend.infuser.Infusers;
import io.mcmaster.someonesperfectend.infuser.InfusionEffect;
import io.mcmaster.someonesperfectend.totem.EffectTotemItem;
import io.mcmaster.someonesperfectend.totem.EffectTotems;
import io.mcmaster.someonesperfectend.totem.TotemRules;
import io.mcmaster.someonesperfectend.trinket.TrinketSlot;
import io.mcmaster.someonesperfectend.trinket.Trinkets;
import io.netty.buffer.Unpooled;
import io.wispforest.accessories.impl.AccessoriesEventHandler;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

public final class EffectTotemGameTests {
    private EffectTotemGameTests() {}

    private static Slot equip(GameTestHelper helper, Player player, ItemStack totem) {
        int index = TrinketSlot.indexIn(player.inventoryMenu);
        helper.assertTrue(index >= 0, "the player inventory menu should have a trinket slot");
        Slot slot = player.inventoryMenu.getSlot(index);
        slot.setByPlayer(totem);
        AccessoriesEventHandler.onLivingEntityTick(player);
        return slot;
    }

    private static InfuserMenu menuAtTier(Level level, Player player, int tier) {
        SimpleContainerData data = new SimpleContainerData(3);
        data.set(0, tier);
        return new InfuserMenu(0, player.getInventory(), data, ContainerLevelAccess.create(level, BlockPos.ZERO));
    }

    private static ServerLevel level(GameTestHelper helper, ResourceKey<Level> key) {
        ServerLevel level = helper.getLevel().getServer().getLevel(key);
        helper.assertTrue(level != null, "the test server should have " + key.location());
        return level;
    }

    /** A mock survival player in another level ({@code makeMockPlayer} always uses the test level). */
    private static Player playerIn(Level level) {
        return new Player(level, BlockPos.ZERO, 0.0F, new GameProfile(UUID.randomUUID(), "test-mock-player")) {
            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return false;
            }
        };
    }

    /** Puts the totem and payment in the menu and confirms; returns the item left in the infusion slot. */
    private static ItemStack confirm(InfuserMenu menu, ItemStack totem, Item payment) {
        menu.getInfusionSlot().set(totem);
        menu.getSlot(InfuserMenu.PAYMENT_SLOT).set(new ItemStack(payment));
        menu.updateEffects(Optional.empty(), Optional.empty());
        return menu.getInfusionSlot().getItem();
    }

    @GameTest
    public static void totemItemBasics(GameTestHelper helper) {
        List<InfusionEffect> expected = List.of(
                InfusionEffect.SPEED,
                InfusionEffect.HASTE,
                InfusionEffect.RESISTANCE,
                InfusionEffect.JUMP_BOOST,
                InfusionEffect.STRENGTH);
        helper.assertTrue(
                EffectTotems.ALL.stream()
                        .map(totem -> totem.get().effect())
                        .toList()
                        .equals(expected),
                "there should be one totem per effect");
        List<RecipeHolder<?>> recipes =
                List.copyOf(helper.getLevel().getRecipeManager().getRecipes());
        for (var supplier : EffectTotems.ALL) {
            EffectTotemItem item = supplier.get();
            ItemStack stack = new ItemStack(item);
            helper.assertTrue(stack.getMaxStackSize() == 1, item + " should not stack");
            helper.assertTrue(stack.getRarity() == Rarity.UNCOMMON, item + " should be uncommon");
            helper.assertTrue(
                    stack.is(Infusers.EFFECT_TOTEMS) && stack.is(Trinkets.TRINKETS) && stack.is(Infusers.INFUSABLE),
                    item + " should be an effect totem, trinket, and infusable");
            helper.assertFalse(EffectTotems.isActive(stack), "a new totem should be inactive");
            helper.assertFalse(stack.hasFoil(), "an inactive totem has no glint");
            helper.assertTrue(EffectTotems.activated(item).hasFoil(), "an active totem has the enchantment glint");
            for (RecipeHolder<?> recipe : recipes) {
                helper.assertFalse(
                        recipe.value()
                                .getResultItem(helper.getLevel().registryAccess())
                                .is(item),
                        item + " should not be craftable (" + recipe.id() + ")");
            }
        }
        helper.succeed();
    }

    @GameTest
    public static void activationSurvivesSaveLoadAndSync(GameTestHelper helper) {
        HolderLookup.Provider registries = helper.getLevel().registryAccess();
        ItemStack active = EffectTotems.activated(EffectTotems.TOTEM_OF_STRENGTH.get());
        ItemStack loaded = ItemStack.parse(registries, active.save(registries)).orElseThrow();
        helper.assertTrue(EffectTotems.isActive(loaded), "activation should survive save/load");
        RegistryFriendlyByteBuf buf =
                new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        ItemStack.STREAM_CODEC.encode(buf, active);
        helper.assertTrue(EffectTotems.isActive(ItemStack.STREAM_CODEC.decode(buf)), "activation should sync");

        String activeTooltip = tooltip(active);
        String inactiveTooltip = tooltip(new ItemStack(EffectTotems.TOTEM_OF_STRENGTH.get()));
        helper.assertTrue(
                activeTooltip.contains(EffectTotems.ACTIVE_TOOLTIP_KEY)
                        && !activeTooltip.contains(EffectTotems.INACTIVE_TOOLTIP_KEY),
                "active tooltip was " + activeTooltip);
        helper.assertTrue(
                inactiveTooltip.contains(EffectTotems.INACTIVE_TOOLTIP_KEY), "inactive tooltip was " + inactiveTooltip);
        helper.succeed();
    }

    private static String tooltip(ItemStack stack) {
        return stack.getTooltipLines(Item.TooltipContext.EMPTY, null, TooltipFlag.NORMAL).stream()
                .map(Component::getContents)
                .map(Object::toString)
                .toList()
                .toString();
    }

    @GameTest
    public static void activeHasteTotemGivesHasteOne(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        equip(helper, player, EffectTotems.activated(EffectTotems.TOTEM_OF_HASTE.get()));
        MobEffectInstance haste = player.getEffect(MobEffects.DIG_SPEED);
        helper.assertTrue(
                haste != null
                        && haste.getAmplifier() == 0
                        && haste.getDuration() == TotemRules.EFFECT_DURATION
                        && haste.isAmbient()
                        && !haste.isVisible()
                        && haste.showIcon(),
                "an active Haste totem should give ambient Haste I for 340 ticks, was " + haste);
        helper.succeed();
    }

    @GameTest
    public static void inactiveTotemGivesNothing(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        equip(helper, player, new ItemStack(EffectTotems.TOTEM_OF_HASTE.get()));
        for (int i = 0; i < TotemRules.EFFECT_INTERVAL; i++) {
            AccessoriesEventHandler.onLivingEntityTick(player);
        }
        helper.assertFalse(player.hasEffect(MobEffects.DIG_SPEED), "an inactive totem should give nothing");
        helper.succeed();
    }

    @GameTest
    public static void eachTotemGivesItsEffect(GameTestHelper helper) {
        for (var supplier : EffectTotems.ALL) {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            EffectTotemItem item = supplier.get();
            equip(helper, player, EffectTotems.activated(item));
            MobEffectInstance effect = player.getEffect(item.effect().holder());
            helper.assertTrue(
                    effect != null && effect.getAmplifier() == 0, item + " should give level I, was " + effect);
            helper.assertTrue(player.getActiveEffects().size() == 1, item + " should give only its own effect");
        }
        helper.succeed();
    }

    @GameTest
    public static void totemsWorkInEveryDimension(GameTestHelper helper) {
        for (var key : List.of(Level.OVERWORLD, Level.NETHER, Level.END)) {
            ServerLevel level = level(helper, key);
            Player player = playerIn(level);
            equip(helper, player, EffectTotems.activated(EffectTotems.TOTEM_OF_SPEED.get()));
            helper.assertTrue(
                    player.hasEffect(MobEffects.MOVEMENT_SPEED), "the totem should work in " + key.location());
        }
        helper.succeed();
    }

    @GameTest
    public static void totemsDoNotPreventDeath(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        equip(helper, player, EffectTotems.activated(EffectTotems.TOTEM_OF_RESISTANCE.get()));
        player.removeAllEffects();
        player.setItemInHand(InteractionHand.MAIN_HAND, EffectTotems.activated(EffectTotems.TOTEM_OF_HASTE.get()));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(EffectTotems.TOTEM_OF_STRENGTH.get()));
        player.setHealth(1.0F);
        player.hurt(helper.getLevel().damageSources().generic(), 100.0F);
        helper.assertTrue(player.isDeadOrDying(), "effect totems must not act as a Totem of Undying");
        helper.assertFalse(
                player.hasEffect(MobEffects.ABSORPTION) || player.hasEffect(MobEffects.FIRE_RESISTANCE),
                "no Totem of Undying effects should be applied");
        helper.succeed();
    }

    @GameTest
    public static void netheriteActivatesTotemAtTierFourInTheEnd(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ServerLevel end = level(helper, Level.END);
        InfuserMenu menu = menuAtTier(end, player, 4);
        helper.assertTrue(
                menu.getInfusionSlot().mayPlace(new ItemStack(EffectTotems.TOTEM_OF_JUMP_BOOST.get())),
                "the infusion slot should accept totems in the End");
        menu.getInfusionSlot().set(new ItemStack(EffectTotems.TOTEM_OF_JUMP_BOOST.get()));
        menu.getSlot(InfuserMenu.PAYMENT_SLOT).set(new ItemStack(Items.NETHERITE_INGOT));
        helper.assertTrue(menu.canActivateTotem(), "confirm should be enabled");
        helper.assertFalse(menu.hasPayment(), "the beacon's own confirm stays disabled while an item is in the slot");
        // The packet's effect is ignored: the totem's effect is fixed.
        menu.updateEffects(Optional.of(MobEffects.DIG_SPEED), Optional.empty());
        ItemStack totem = menu.getInfusionSlot().getItem();
        helper.assertTrue(
                totem.is(EffectTotems.TOTEM_OF_JUMP_BOOST.get()) && EffectTotems.isActive(totem),
                "confirming should activate the totem");
        helper.assertTrue(
                menu.getSlot(InfuserMenu.PAYMENT_SLOT).getItem().isEmpty(), "one netherite ingot is consumed");
        helper.assertTrue(totem.get(Infusers.INFUSION.get()) == null, "a totem is not infused like gear");

        // An already-active totem can't be activated again.
        menu.getSlot(InfuserMenu.PAYMENT_SLOT).set(new ItemStack(Items.NETHERITE_INGOT));
        helper.assertFalse(menu.canActivateTotem(), "an active totem leaves confirm disabled");
        menu.updateEffects(Optional.empty(), Optional.empty());
        helper.assertTrue(
                menu.getSlot(InfuserMenu.PAYMENT_SLOT).getItem().is(Items.NETHERITE_INGOT),
                "an active totem should not consume netherite");
        helper.succeed();
    }

    @GameTest
    public static void activationRefusedWithoutNetheriteTierFourOrTheEnd(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ServerLevel end = level(helper, Level.END);
        for (Item payment : List.of(Items.IRON_INGOT, Items.GOLD_INGOT, Items.EMERALD, Items.DIAMOND)) {
            InfuserMenu menu = menuAtTier(end, player, 4);
            ItemStack totem = confirm(menu, new ItemStack(EffectTotems.TOTEM_OF_HASTE.get()), payment);
            helper.assertFalse(EffectTotems.isActive(totem), payment + " should not activate a totem");
            helper.assertTrue(
                    menu.getSlot(InfuserMenu.PAYMENT_SLOT).getItem().is(payment), payment + " should be kept");
        }
        for (int tier = 0; tier < 4; tier++) {
            InfuserMenu menu = menuAtTier(end, player, tier);
            ItemStack totem = confirm(menu, new ItemStack(EffectTotems.TOTEM_OF_HASTE.get()), Items.NETHERITE_INGOT);
            helper.assertFalse(EffectTotems.isActive(totem), "tier " + tier + " should not activate a totem");
            helper.assertTrue(
                    menu.getSlot(InfuserMenu.PAYMENT_SLOT).getItem().is(Items.NETHERITE_INGOT),
                    "netherite should be kept at tier " + tier);
        }
        for (var key : List.of(Level.OVERWORLD, Level.NETHER)) {
            InfuserMenu menu = menuAtTier(level(helper, key), player, 4);
            helper.assertFalse(
                    menu.getInfusionSlot().mayPlace(new ItemStack(EffectTotems.TOTEM_OF_HASTE.get())),
                    "the infusion slot is locked in " + key.location());
            ItemStack totem = confirm(menu, new ItemStack(EffectTotems.TOTEM_OF_HASTE.get()), Items.NETHERITE_INGOT);
            helper.assertFalse(EffectTotems.isActive(totem), "no activation in " + key.location());
        }
        helper.succeed();
    }
}
