package io.mcmaster.someonesperfectend.gametest;

import io.mcmaster.someonesperfectend.block.ModBlocks;
import io.mcmaster.someonesperfectend.entity.Driftjellies;
import io.mcmaster.someonesperfectend.entity.Driftjelly;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** US-020: tempting, breeding, and leashing driftjellies, and the End lock (dormancy outside the End). */
public final class DriftjellyHusbandryGameTests {
    private static final String TALL = "someonesperfectend:gametest/empty_tall";
    private static final int MID = 8;

    private DriftjellyHusbandryGameTests() {}

    private static void turfFloor(GameTestHelper helper) {
        for (int x = 0; x <= 16; x++) {
            for (int z = 0; z <= 16; z++) {
                helper.setBlock(x, 0, z, ModBlocks.END_TURF.get());
            }
        }
    }

    private static Driftjelly spawn(GameTestHelper helper, double x, double y, double z) {
        return helper.spawn(Driftjellies.TYPE.get(), new Vec3(x, y, z));
    }

    private static Player playerWithVoidfruit(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModBlocks.VOIDFRUIT.get(), 16));
        return player;
    }

    private static List<Driftjelly> jellies(GameTestHelper helper) {
        return helper.getLevel()
                .getEntitiesOfClass(
                        Driftjelly.class, new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(17, 24, 17));
    }

    @GameTest(template = TALL, timeoutTicks = 200)
    public static void feedingTwoAdultsProducesABaby(GameTestHelper helper) {
        turfFloor(helper);
        Driftjelly a = spawn(helper, MID + 0.5, 4.0, MID + 0.5);
        Driftjelly b = spawn(helper, MID + 1.5, 4.0, MID + 0.5);
        Player player = playerWithVoidfruit(helper);
        helper.assertTrue(a.isFood(player.getMainHandItem()), "voidfruit should be driftjelly food");
        a.mobInteract(player, InteractionHand.MAIN_HAND);
        b.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(a.isInLove() && b.isInLove(), "feeding voidfruit should put both adults in love");
        helper.assertTrue(player.getMainHandItem().getCount() == 14, "feeding should use one voidfruit each");
        helper.succeedWhen(() -> {
            Driftjelly baby = jellies(helper).stream()
                    .filter(Driftjelly::isBaby)
                    .findFirst()
                    .orElse(null);
            helper.assertTrue(baby != null, "no baby driftjelly yet");
            helper.assertTrue(a.getAge() > 0 && b.getAge() > 0, "parents should get the vanilla breeding cooldown");
            helper.assertTrue(
                    baby.getAge() <= AgeableMob.BABY_START_AGE + 200,
                    "a baby should take 20 minutes to grow up, age is " + baby.getAge());
            int before = baby.getAge();
            baby.mobInteract(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(baby.getAge() > before, "feeding a baby should speed up its growth");
            jellies(helper).forEach(Driftjelly::discard);
        });
    }

    @GameTest(template = TALL, timeoutTicks = 200)
    public static void voidfruitTemptsAndLowersHover(GameTestHelper helper) {
        turfFloor(helper);
        Player player = playerWithVoidfruit(helper);
        player.moveTo(helper.absoluteVec(new Vec3(2.5, 1.0, 2.5)));
        helper.getLevel().addFreshEntity(player);
        Driftjelly jelly = spawn(helper, 8.5, 4.0, 8.5);
        double groundY = helper.absolutePos(new BlockPos(0, 1, 0)).getY();
        helper.succeedWhen(() -> {
            helper.assertTrue(jelly.isTempted(), "driftjelly should be tempted by voidfruit");
            double height = jelly.getY() - groundY;
            helper.assertTrue(
                    height >= 0.8 && height <= 2.2, "tempted driftjelly should hover 1-2 blocks, got " + height);
            double dx = jelly.getX() - player.getX();
            double dz = jelly.getZ() - player.getZ();
            helper.assertTrue(
                    dx * dx + dz * dz < 4.0 * 4.0,
                    "tempted driftjelly should follow the player, offset "
                            + jelly.position().subtract(player.position()));
            jelly.discard();
            player.discard();
        });
    }

    @GameTest(template = TALL, timeoutTicks = 400)
    public static void becomesDormantOutsideTheEnd(GameTestHelper helper) {
        turfFloor(helper);
        Driftjelly jelly = spawn(helper, MID + 0.5, 5.0, MID + 0.5);
        helper.assertTrue(helper.getLevel().dimension() != Level.END, "this test must run outside the End");
        int[] tick = {0};
        int[] dormantAt = {-1};
        helper.onEachTick(() -> {
            tick[0]++;
            if (dormantAt[0] < 0 && jelly.isDormant()) {
                dormantAt[0] = tick[0];
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(dormantAt[0] > 0, "driftjelly not dormant yet after " + tick[0] + " ticks");
            helper.assertTrue(
                    dormantAt[0] >= Driftjelly.DORMANCY_DELAY - 5 && dormantAt[0] <= Driftjelly.DORMANCY_DELAY + 5,
                    "driftjelly should turn dormant after 300 ticks, turned at " + dormantAt[0]);
            helper.assertFalse(jelly.isNoGravity(), "a dormant driftjelly should stop floating");
            helper.assertTrue(jelly.onGround(), "a dormant driftjelly should sink to the ground");
            jelly.discard();
        });
    }

    @GameTest(template = TALL)
    public static void dormantDriftjellyCannotBreedAndIsNotTempted(GameTestHelper helper) {
        turfFloor(helper);
        Driftjelly a = spawn(helper, MID + 0.5, 1.0, MID + 0.5);
        Driftjelly b = spawn(helper, MID + 1.5, 1.0, MID + 0.5);
        a.setDormant(true);
        Player player = playerWithVoidfruit(helper);
        player.moveTo(helper.absoluteVec(new Vec3(MID + 2.5, 1.0, MID + 2.5)));
        helper.getLevel().addFreshEntity(player);

        a.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertFalse(a.isInLove(), "a dormant driftjelly should not fall in love");
        helper.assertTrue(player.getMainHandItem().getCount() == 16, "a dormant driftjelly should not eat voidfruit");
        // Even if forced into love, a dormant driftjelly can't mate.
        b.setInLove(player);
        a.setInLoveTime(600);
        helper.assertFalse(b.canMate(a) || a.canMate(b), "a dormant driftjelly can't mate");
        helper.runAfterDelay(40, () -> {
            helper.assertFalse(a.isTempted(), "a dormant driftjelly should not be tempted");
            helper.assertTrue(b.isTempted(), "an awake driftjelly next to it should be tempted");
            a.discard();
            b.discard();
            player.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TALL)
    public static void dormancyPersistsAndWakesInTheEnd(GameTestHelper helper) {
        Driftjelly jelly = spawn(helper, MID + 0.5, 5.0, MID + 0.5);
        for (int i = 1; i < Driftjelly.DORMANCY_DELAY; i++) {
            jelly.updateDormancy(false);
        }
        helper.assertFalse(jelly.isDormant(), "dormant too early");
        jelly.updateDormancy(true);
        helper.assertTrue(jelly.ticksOutsideEnd() == 0, "being in the End should reset the timer");
        for (int i = 0; i < Driftjelly.DORMANCY_DELAY; i++) {
            jelly.updateDormancy(false);
        }
        helper.assertTrue(jelly.isDormant(), "should be dormant after 300 ticks outside the End");

        CompoundTag tag = new CompoundTag();
        jelly.saveWithoutId(tag);
        Driftjelly loaded = Driftjellies.TYPE.get().create(helper.getLevel());
        loaded.load(tag);
        helper.assertTrue(loaded.isDormant(), "dormancy should persist in NBT");
        helper.assertFalse(loaded.isNoGravity(), "a loaded dormant driftjelly should not float");

        loaded.updateDormancy(true);
        helper.assertFalse(loaded.isDormant(), "returning to the End should wake a driftjelly");
        helper.assertTrue(loaded.isNoGravity(), "an awake driftjelly floats again");
        helper.assertTrue(loaded.canFallInLove(), "an awake driftjelly can breed again");
        jelly.discard();
        helper.succeed();
    }

    @GameTest(template = ModGameTests.EMPTY_TEMPLATE)
    public static void dormantDriftjellyDropsNothing(GameTestHelper helper) {
        Driftjelly jelly = spawn(helper, 2.5, 2.0, 2.5);
        jelly.setDormant(true);
        jelly.kill();
        AABB box = new AABB(helper.absolutePos(new BlockPos(2, 2, 2))).inflate(3.0);
        helper.assertTrue(
                helper.getLevel().getEntitiesOfClass(ItemEntity.class, box).isEmpty(),
                "a dormant driftjelly should drop nothing");
        helper.assertTrue(
                helper.getLevel().getEntitiesOfClass(ExperienceOrb.class, box).isEmpty(),
                "a dormant driftjelly should drop no experience");
        helper.succeed();
    }

    @GameTest(template = ModGameTests.EMPTY_TEMPLATE)
    public static void canBeLeashed(GameTestHelper helper) {
        Driftjelly jelly = spawn(helper, 2.5, 2.0, 2.5);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.moveTo(helper.absoluteVec(new Vec3(4.5, 1.0, 4.5)));
        helper.getLevel().addFreshEntity(player);
        helper.assertTrue(jelly.canBeLeashed(), "driftjellies should be leashable");
        jelly.setLeashedTo(player, true);
        helper.assertTrue(jelly.isLeashed() && jelly.getLeashHolder() == player, "driftjelly should be leashed");
        jelly.dropLeash(true, false);
        jelly.discard();
        player.discard();
        helper.succeed();
    }
}
