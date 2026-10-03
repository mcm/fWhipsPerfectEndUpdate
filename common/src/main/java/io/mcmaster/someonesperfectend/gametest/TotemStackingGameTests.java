package io.mcmaster.someonesperfectend.gametest;

import io.mcmaster.someonesperfectend.infuser.BeaconEffectTracker;
import io.mcmaster.someonesperfectend.infuser.Infusers;
import io.mcmaster.someonesperfectend.infuser.Infusion;
import io.mcmaster.someonesperfectend.mixin.BeaconBlockEntityAccessor;
import io.mcmaster.someonesperfectend.totem.EffectTotems;
import io.mcmaster.someonesperfectend.trinket.TrinketSlot;
import io.wispforest.accessories.impl.AccessoriesEventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Totem beacon stacking and gear suppression (US-033). Players are added to the level so they are ticked (effects
 * tick down, Accessories ticks the totem, and the 20-tick gear update runs). Assert before {@code discard()}.
 * Each beacon test has its own batch because a beacon affects every player in range; the rest share a batch with no
 * beacons.
 */
public final class TotemStackingGameTests {
    private static final String BATCH = "totem_stacking";
    private static final int STORED = 2000;

    private TotemStackingGameTests() {}

    private static Player player(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 pos = helper.absoluteVec(new Vec3(4, 2, 4));
        player.moveTo(pos.x, pos.y, pos.z);
        helper.setBlock(new BlockPos(4, 1, 4), Blocks.STONE);
        helper.getLevel().addFreshEntity(player);
        return player;
    }

    private static void equip(GameTestHelper helper, Player player, ItemStack stack) {
        int index = TrinketSlot.indexIn(player.inventoryMenu);
        helper.assertTrue(index >= 0, "the player inventory menu should have a trinket slot");
        Slot slot = player.inventoryMenu.getSlot(index);
        slot.setByPlayer(stack);
        AccessoriesEventHandler.onLivingEntityTick(player);
    }

    private static ItemStack activeHasteTotem() {
        return EffectTotems.activated(EffectTotems.TOTEM_OF_HASTE.get());
    }

    /** Applies a beacon's powers once at the test origin, as its 80-tick update does. */
    private static void beacon(
            GameTestHelper helper, int tier, Holder<MobEffect> primary, Holder<MobEffect> secondary) {
        BeaconBlockEntityAccessor.someonesperfectend$applyEffects(
                helper.getLevel(), helper.absolutePos(BlockPos.ZERO), tier, primary, secondary);
    }

    private static int amplifierOf(Player player, Holder<MobEffect> effect) {
        MobEffectInstance instance = player.getEffect(effect);
        return instance == null ? -1 : instance.getAmplifier();
    }

    private static ItemStack hastePickaxe() {
        ItemStack stack = new ItemStack(Items.IRON_PICKAXE);
        stack.set(Infusers.INFUSION.get(), new Infusion(MobEffects.DIG_SPEED, 0, STORED));
        return stack;
    }

    private static int remaining(ItemStack stack) {
        Infusion infusion = Infusion.get(stack);
        return infusion == null ? 0 : infusion.remainingTicks();
    }

    @GameTest(batch = "totem_stacking_haste_beacon")
    public static void hasteTwoBeaconPlusHasteTotemGivesHasteThree(GameTestHelper helper) {
        Player player = player(helper);
        beacon(helper, 4, MobEffects.DIG_SPEED, MobEffects.DIG_SPEED);
        equip(helper, player, activeHasteTotem());
        MobEffectInstance haste = player.getEffect(MobEffects.DIG_SPEED);
        helper.assertTrue(
                haste != null && haste.getAmplifier() == 2,
                "a Haste II beacon plus a Haste totem should give Haste III, was " + haste);
        helper.assertTrue(haste.isAmbient() && !haste.isVisible(), "the stacked effect should be ambient");
        player.discard();
        helper.succeed();
    }

    @GameTest(batch = "totem_stacking_strength_beacon")
    public static void strengthOneBeaconPlusStrengthTotemGivesStrengthTwo(GameTestHelper helper) {
        Player player = player(helper);
        beacon(helper, 3, MobEffects.DAMAGE_BOOST, null);
        equip(helper, player, EffectTotems.activated(EffectTotems.TOTEM_OF_STRENGTH.get()));
        helper.assertTrue(
                amplifierOf(player, MobEffects.DAMAGE_BOOST) == 1,
                "a Strength I beacon plus a Strength totem should give Strength II");
        player.discard();
        helper.succeed();
    }

    @GameTest(batch = "totem_stacking_speed_beacon")
    public static void speedBeaconDoesNotStackOnAHasteTotem(GameTestHelper helper) {
        Player player = player(helper);
        beacon(helper, 4, MobEffects.MOVEMENT_SPEED, MobEffects.MOVEMENT_SPEED);
        equip(helper, player, activeHasteTotem());
        helper.assertTrue(
                amplifierOf(player, MobEffects.DIG_SPEED) == 0,
                "a Speed beacon should not raise a Haste totem above level I");
        helper.assertTrue(amplifierOf(player, MobEffects.MOVEMENT_SPEED) == 1, "the beacon's Speed II is unchanged");
        player.discard();
        helper.succeed();
    }

    @GameTest(batch = BATCH)
    public static void hasteTotemWithoutBeaconGivesHasteOne(GameTestHelper helper) {
        Player player = player(helper);
        equip(helper, player, activeHasteTotem());
        helper.assertTrue(amplifierOf(player, MobEffects.DIG_SPEED) == 0, "a Haste totem alone should give Haste I");
        player.discard();
        helper.succeed();
    }

    @GameTest(batch = BATCH)
    public static void potionsDoNotCountTowardStacking(GameTestHelper helper) {
        Player player = player(helper);
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 2000, 1));
        equip(helper, player, activeHasteTotem());
        helper.assertTrue(
                amplifierOf(player, MobEffects.DIG_SPEED) == 1,
                "a Haste II potion plus a Haste totem should stay Haste II (no stacking)");
        player.discard();
        helper.succeed();
    }

    @GameTest(batch = BATCH, timeoutTicks = 200)
    public static void bonusEndsWhenTheBeaconEffectExpires(GameTestHelper helper) {
        Player player = player(helper);
        // A beacon's Haste II that expires in 40 ticks, as when the player has just left its range.
        MobEffectInstance beaconHaste = new MobEffectInstance(MobEffects.DIG_SPEED, 40, 1, true, true);
        BeaconEffectTracker.record(player, beaconHaste);
        player.addEffect(new MobEffectInstance(beaconHaste));
        equip(helper, player, activeHasteTotem());
        MobEffectInstance stacked = player.getEffect(MobEffects.DIG_SPEED);
        helper.assertTrue(
                stacked != null && stacked.getAmplifier() == 2 && stacked.getDuration() <= 40,
                "the stacked Haste III should last only as long as the beacon effect, was " + stacked);
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(
                    BeaconEffectTracker.highestAmplifier(player, MobEffects.DIG_SPEED) == BeaconEffectTracker.NONE,
                    "the beacon effect should have expired");
            helper.assertTrue(
                    amplifierOf(player, MobEffects.DIG_SPEED) == 0,
                    "after the beacon effect expires the totem should give Haste I");
            player.discard();
            helper.succeed();
        });
    }

    @GameTest(batch = BATCH, timeoutTicks = 300)
    public static void activeTotemSuppressesInfusedGear(GameTestHelper helper) {
        Player player = player(helper);
        equip(helper, player, activeHasteTotem());
        player.setItemSlot(EquipmentSlot.MAINHAND, hastePickaxe());
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(
                    remaining(player.getMainHandItem()) == STORED,
                    "gear should not lose time while an active totem is worn");
            equip(helper, player, ItemStack.EMPTY);
        });
        helper.runAfterDelay(160, () -> {
            helper.assertTrue(
                    remaining(player.getMainHandItem()) < STORED,
                    "gear should resume losing time once the totem is removed");
            helper.assertTrue(player.hasEffect(MobEffects.DIG_SPEED), "the pickaxe should give Haste again");
            player.discard();
            helper.succeed();
        });
    }

    @GameTest(batch = BATCH, timeoutTicks = 200)
    public static void inactiveTotemDoesNotSuppressGear(GameTestHelper helper) {
        Player player = player(helper);
        equip(helper, player, new ItemStack(EffectTotems.TOTEM_OF_HASTE.get()));
        player.setItemSlot(EquipmentSlot.MAINHAND, hastePickaxe());
        helper.runAfterDelay(61, () -> {
            helper.assertTrue(amplifierOf(player, MobEffects.DIG_SPEED) == 0, "the pickaxe should give Haste I");
            int used = STORED - remaining(player.getMainHandItem());
            helper.assertTrue(
                    used >= 40 && used <= 80, "the pickaxe should use about 20 ticks per second, used " + used);
            player.discard();
            helper.succeed();
        });
    }
}
