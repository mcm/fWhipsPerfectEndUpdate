package io.mcmaster.someonesperfectend.gametest;

import com.mojang.authlib.GameProfile;
import io.mcmaster.someonesperfectend.infuser.BeaconEffectTracker;
import io.mcmaster.someonesperfectend.infuser.InfusedGear;
import io.mcmaster.someonesperfectend.infuser.Infusers;
import io.mcmaster.someonesperfectend.infuser.Infusion;
import io.mcmaster.someonesperfectend.mixin.BeaconBlockEntityAccessor;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Infused gear effects (US-031). Mock players added to the level are ticked, so the real 20-tick gear update runs.
 * Assert before {@code discard()}: removing a living entity clears its effects.
 * Gear tests run in their own batch so other tests' beacons and Infusers don't reach their players, and each beacon
 * test has its own batch because a beacon affects every player in range.
 */
public final class InfusedGearGameTests {
    private static final String BATCH = "infused_gear";
    private static final int STORED = 2000;

    private InfusedGearGameTests() {
    }

    private static ItemStack infused(ItemStack stack, Holder<MobEffect> effect, int amplifier, int remainingTicks) {
        stack.set(Infusers.INFUSION.get(), new Infusion(effect, amplifier, remainingTicks));
        return stack;
    }

    private static ItemStack hastePickaxe(int amplifier, int remainingTicks) {
        return infused(new ItemStack(Items.IRON_PICKAXE), MobEffects.DIG_SPEED, amplifier, remainingTicks);
    }

    private static Player player(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 pos = helper.absoluteVec(new Vec3(4, 2, 4));
        player.moveTo(pos.x, pos.y, pos.z);
        helper.setBlock(new BlockPos(4, 1, 4), Blocks.STONE);
        helper.getLevel().addFreshEntity(player);
        return player;
    }

    private static int remaining(ItemStack stack) {
        Infusion infusion = Infusion.get(stack);
        return infusion == null ? 0 : infusion.remainingTicks();
    }

    private static int amplifierOf(Player player, Holder<MobEffect> effect) {
        MobEffectInstance instance = player.getEffect(effect);
        return instance == null ? -1 : instance.getAmplifier();
    }

    @GameTest(batch = BATCH)
    public static void hastePickaxeInMainHandGivesHaste(GameTestHelper helper) {
        Player player = player(helper);
        player.setItemSlot(EquipmentSlot.MAINHAND, hastePickaxe(0, STORED));
        helper.runAtTickTime(61, () -> {
            MobEffectInstance haste = player.getEffect(MobEffects.DIG_SPEED);
            helper.assertTrue(haste != null && haste.getAmplifier() == 0, "the pickaxe should give Haste I");
            helper.assertTrue(haste.isAmbient() && !haste.isVisible() && haste.showIcon(),
                    "the effect should be ambient, without particles, with an icon");
            helper.assertTrue(haste.getDuration() > 20 && haste.getDuration() <= 80,
                    "the effect should last up to 80 ticks, was " + haste.getDuration());
            // Three updates in about three seconds.
            int used = STORED - remaining(player.getMainHandItem());
            helper.assertTrue(used >= 40 && used <= 80, "about 20 ticks per second should be used, used " + used);
            player.discard();
            helper.succeed();
        });
    }

    @GameTest(batch = BATCH, timeoutTicks = 300)
    public static void pickaxeInInventoryDoesNothing(GameTestHelper helper) {
        Player player = player(helper);
        player.setItemSlot(EquipmentSlot.MAINHAND, hastePickaxe(0, STORED));
        int[] stored = new int[1];
        helper.runAtTickTime(41, () -> {
            helper.assertTrue(player.hasEffect(MobEffects.DIG_SPEED), "the held pickaxe should give Haste");
            ItemStack pickaxe = player.getMainHandItem();
            player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            player.getInventory().setItem(9, pickaxe);
            stored[0] = remaining(pickaxe);
        });
        helper.runAtTickTime(160, () -> {
            helper.assertFalse(player.hasEffect(MobEffects.DIG_SPEED), "Haste should stop once the pickaxe is put away");
            helper.assertTrue(remaining(player.getInventory().getItem(9)) == stored[0],
                    "a pickaxe in the inventory should not lose time");
            player.discard();
            helper.succeed();
        });
    }

    @GameTest(batch = BATCH, timeoutTicks = 200)
    public static void bootsHeldInHandDoNothing(GameTestHelper helper) {
        Player player = player(helper);
        player.setItemSlot(EquipmentSlot.MAINHAND,
                infused(new ItemStack(Items.IRON_BOOTS), MobEffects.MOVEMENT_SPEED, 0, STORED));
        // A pickaxe on the feet doesn't count either.
        player.setItemSlot(EquipmentSlot.FEET, hastePickaxe(0, STORED));
        helper.runAtTickTime(100, () -> {
            helper.assertFalse(player.hasEffect(MobEffects.MOVEMENT_SPEED), "boots in a hand should not give Speed");
            helper.assertFalse(player.hasEffect(MobEffects.DIG_SPEED), "a pickaxe on the feet should not give Haste");
            helper.assertTrue(remaining(player.getMainHandItem()) == STORED, "boots in a hand should not lose time");
            helper.assertTrue(remaining(player.getItemBySlot(EquipmentSlot.FEET)) == STORED,
                    "a pickaxe on the feet should not lose time");
            player.discard();
            helper.succeed();
        });
    }

    @GameTest(batch = BATCH, timeoutTicks = 200)
    public static void wornBootsAndChestplateApply(GameTestHelper helper) {
        Player player = player(helper);
        player.setItemSlot(EquipmentSlot.FEET,
                infused(new ItemStack(Items.IRON_BOOTS), MobEffects.MOVEMENT_SPEED, 1, STORED));
        player.setItemSlot(EquipmentSlot.CHEST,
                infused(new ItemStack(Items.IRON_CHESTPLATE), MobEffects.DAMAGE_RESISTANCE, 0, STORED));
        helper.succeedWhen(() -> {
            helper.assertTrue(amplifierOf(player, MobEffects.MOVEMENT_SPEED) == 1, "worn boots should give Speed II");
            helper.assertTrue(amplifierOf(player, MobEffects.DAMAGE_RESISTANCE) == 0,
                    "a worn chestplate should give Resistance I");
            helper.assertTrue(remaining(player.getItemBySlot(EquipmentSlot.FEET)) < STORED, "boots should lose time");
            helper.assertTrue(remaining(player.getItemBySlot(EquipmentSlot.CHEST)) < STORED,
                    "the chestplate should lose time");
            player.discard();
        });
    }

    @GameTest(batch = BATCH, timeoutTicks = 100)
    public static void infusionIsRemovedWhenItRunsOut(GameTestHelper helper) {
        Player player = player(helper);
        player.setItemSlot(EquipmentSlot.MAINHAND, hastePickaxe(0, 20));
        helper.succeedWhen(() -> {
            helper.assertTrue(Infusion.get(player.getMainHandItem()) == null,
                    "the infusion should be removed when its time runs out");
            helper.assertTrue(player.hasEffect(MobEffects.DIG_SPEED), "the last 20 ticks should still give Haste");
            player.discard();
        });
    }

    @GameTest(batch = BATCH, timeoutTicks = 200)
    public static void mainHandWinsOverOffHand(GameTestHelper helper) {
        Player player = player(helper);
        player.setItemSlot(EquipmentSlot.MAINHAND, hastePickaxe(0, STORED));
        player.setItemSlot(EquipmentSlot.OFFHAND, hastePickaxe(0, STORED));
        helper.runAtTickTime(100, () -> {
            helper.assertTrue(player.hasEffect(MobEffects.DIG_SPEED), "the main-hand pickaxe should give Haste");
            helper.assertTrue(remaining(player.getMainHandItem()) < STORED, "the main-hand pickaxe should lose time");
            helper.assertTrue(remaining(player.getOffhandItem()) == STORED, "the off-hand pickaxe should pause");
            player.discard();
            helper.succeed();
        });
    }

    /** Applies a beacon's powers once at the test origin (it covers the test player for 220–340 ticks). */
    private static void beacon(GameTestHelper helper, int tier, Holder<MobEffect> primary,
            Holder<MobEffect> secondary) {
        BeaconBlockEntityAccessor.someonesperfectend$applyEffects(helper.getLevel(), helper.absolutePos(BlockPos.ZERO),
                tier, primary, secondary);
    }

    @GameTest(batch = "infused_gear_beacon_strong", timeoutTicks = 200)
    public static void strongerBeaconPausesGear(GameTestHelper helper) {
        Player player = player(helper);
        player.setItemSlot(EquipmentSlot.MAINHAND, hastePickaxe(0, STORED));
        // Tier 4 with Haste as primary and secondary: Haste II.
        beacon(helper, 4, MobEffects.DIG_SPEED, MobEffects.DIG_SPEED);
        helper.assertTrue(BeaconEffectTracker.highestAmplifier(player, MobEffects.DIG_SPEED) == 1,
                "the beacon's Haste II should be tracked");
        helper.runAtTickTime(100, () -> {
            helper.assertTrue(amplifierOf(player, MobEffects.DIG_SPEED) == 1, "the player should have Haste II");
            helper.assertTrue(remaining(player.getMainHandItem()) == STORED,
                    "Haste I gear should not lose time under a Haste II beacon");
            player.discard();
            helper.succeed();
        });
    }

    @GameTest(batch = "infused_gear_beacon_weak", timeoutTicks = 200)
    public static void strongerGearAppliesUnderAWeakerBeacon(GameTestHelper helper) {
        Player player = player(helper);
        player.setItemSlot(EquipmentSlot.MAINHAND, hastePickaxe(1, STORED));
        beacon(helper, 1, MobEffects.DIG_SPEED, null);
        helper.assertTrue(BeaconEffectTracker.highestAmplifier(player, MobEffects.DIG_SPEED) == 0,
                "the beacon's Haste I should be tracked");
        helper.runAtTickTime(100, () -> {
            helper.assertTrue(amplifierOf(player, MobEffects.DIG_SPEED) == 1, "the player should have Haste II");
            helper.assertTrue(remaining(player.getMainHandItem()) < STORED,
                    "Haste II gear should lose time under a Haste I beacon");
            player.discard();
            helper.succeed();
        });
    }

    @GameTest(batch = BATCH)
    public static void worksInEveryDimension(GameTestHelper helper) {
        for (ResourceKey<Level> key : List.of(Level.OVERWORLD,
                Level.NETHER, Level.END)) {
            Player player = new Player(helper.getLevel().getServer().getLevel(key), BlockPos.ZERO, 0,
                    new GameProfile(UUID.randomUUID(), "test-mock-player")) {
                @Override
                public boolean isSpectator() {
                    return false;
                }

                @Override
                public boolean isCreative() {
                    return false;
                }
            };
            player.setItemSlot(EquipmentSlot.MAINHAND, hastePickaxe(0, STORED));
            InfusedGear.update(player);
            helper.assertTrue(player.hasEffect(MobEffects.DIG_SPEED), "gear should give Haste in " + key.location());
            helper.assertTrue(remaining(player.getMainHandItem()) == STORED - 20,
                    "gear should lose time in " + key.location());
        }
        helper.succeed();
    }
}
