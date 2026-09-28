package io.mcmaster.someonesperfectend.gametest;

import io.mcmaster.someonesperfectend.block.EnderBedBlock;
import io.mcmaster.someonesperfectend.block.EnderBedLocations;
import io.mcmaster.someonesperfectend.block.EnderBedRules;
import io.mcmaster.someonesperfectend.block.EnderBedTravel;
import io.mcmaster.someonesperfectend.gametest.EnderBedRespawnGameTests.Detached;
import io.mcmaster.someonesperfectend.mixin.PlayerAccessor;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.SleepStatus;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** US-038: sleeping in an Ender Bed to travel between the Overworld and the End. */
public final class EnderBedTravelGameTests {
    private EnderBedTravelGameTests() {
    }

    /** End beds for these tests, away from the US-037 ones. */
    private static BlockPos endFoot(int slot) {
        return new BlockPos(300 + 16 * slot, 100, -900);
    }

    private static EnderBedLocations beds(ServerPlayer player) {
        return EnderBedLocations.of(player);
    }

    /**
     * The sleeping part of {@code Player#tick} plus the Ender Bed hook, {@code ticks} times. A full
     * {@code ServerPlayer#doTick} can't run here: on NeoForge, Accessories syncs to the detached connection, which
     * refuses the payload. {@link #sleeperStaysInBedAllDay} covers the real player tick.
     */
    private static void sleep(ServerPlayer player, int ticks) {
        for (int i = 0; i < ticks; i++) {
            if (player.isSleeping()) {
                ((PlayerAccessor) player).someonesperfectend$setSleepCounter(
                        Math.min(player.getSleepTimer() + 1, EnderBedTravel.SLEEP_TICKS));
            }
            EnderBedTravel.tick(player);
        }
    }

    /** A detached player standing under the Overworld test bed. */
    private static ServerPlayer playerUnderOverworldBed(GameTestHelper helper, Detached detached) {
        ServerPlayer player = detached.player();
        Vec3 pos = EnderBedRules.arrivalPos(helper.absolutePos(EnderBedGameTests.HEAD));
        player.moveTo(pos.x, pos.y, pos.z);
        return player;
    }

    private static void assertArrived(GameTestHelper helper, ServerPlayer player, ServerLevel level, BlockPos head) {
        helper.assertTrue(player.level() == level, "should arrive in " + level.dimension().location() + ", got "
                + player.level().dimension().location());
        helper.assertTrue(player.position().equals(EnderBedRules.arrivalPos(head)),
                "should arrive at the drop point " + EnderBedRules.arrivalPos(head) + ", got " + player.position());
        helper.assertTrue(player.getDeltaMovement().equals(Vec3.ZERO), "should arrive with zero velocity, got "
                + player.getDeltaMovement());
        helper.assertFalse(player.isSleeping(), "should be awake on arrival");
        helper.assertTrue(level.noCollision(player.getBoundingBox()), "should have room to fall at the drop point");
        helper.assertFalse(player.wonGame, "arriving should never show the End credits");
    }

    @GameTest(template = ModGameTests.EMPTY_TEMPLATE)
    public static void tripToTheEndAndBack(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        ServerLevel end = EnderBedRespawnGameTests.end(helper);
        EnderBedGameTests.hangBed(helper);
        BlockPos overworldHead = helper.absolutePos(EnderBedGameTests.HEAD);
        BlockPos endFoot = endFoot(0);
        BlockPos endHead = endFoot.south();
        EnderBedGameTests.hangBedIn(end, endFoot);
        BlockPos platform = new BlockPos(100, 48, 0);
        boolean platformBefore = end.getBlockState(platform).is(Blocks.OBSIDIAN);

        // A hostile mob right next to the bed and daytime (vanilla beds refuse both and wake sleepers at day)
        // don't matter. The test runs synchronously, so the time is restored before anything else ticks.
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, EnderBedGameTests.FOOT.below(2).west());
        long time = overworld.getDayTime();
        overworld.setDayTime(6000);
        overworld.updateSkyBrightness();
        helper.assertTrue(overworld.isDay(), "the test expects daytime in the Overworld");

        Detached detached = Detached.in(overworld);
        ServerPlayer player = playerUnderOverworldBed(helper, detached);
        beds(player).set(Level.END, endHead);
        EnderBedGameTests.use(overworld, overworldHead, player);
        helper.assertTrue(beds(player).overworld().equals(Optional.of(overworldHead)),
                "using the bed should still store it first");
        helper.assertTrue(player.isSleeping() && overworldHead.equals(player.getSleepingPos().orElse(null)),
                "with `end` stored, using an Overworld bed should lie the player down in it");
        helper.assertTrue(overworld.getBlockState(overworldHead).getValue(EnderBedBlock.OCCUPIED),
                "the bed should be occupied while sleeping");
        helper.assertTrue(player.getRespawnPosition() == null, "travel sleep should not set the vanilla spawn");

        sleep(player, EnderBedTravel.SLEEP_TICKS - 1);
        helper.assertTrue(player.isSleeping() && player.level() == overworld,
                "the player should still be asleep in the Overworld before 100 ticks");
        helper.assertTrue(player.getY() == overworldHead.getY() + 0.6875,
                "the sleeper should stay in the hanging bed, got y=" + player.getY());
        sleep(player, 1);
        try {
            assertArrived(helper, player, end, endHead);
            helper.assertFalse(overworld.getBlockState(overworldHead).getValue(EnderBedBlock.OCCUPIED),
                    "the Overworld bed should be free after leaving");
            helper.assertTrue(zombie.level() == overworld, "only the player should travel");
            helper.assertTrue(end.getBlockState(platform).is(Blocks.OBSIDIAN) == platformBefore,
                    "arriving should not build the End platform");
            helper.assertTrue(beds(player).end().equals(Optional.of(endHead)), "a trip should keep `end`");

            // And back: using the End bed stores it and sleeps, since `overworld` is stored.
            EnderBedGameTests.use(end, endHead, player);
            helper.assertTrue(player.isSleeping(), "with `overworld` stored, the End bed should lie the player down");
            sleep(player, EnderBedTravel.SLEEP_TICKS - 1);
            helper.assertTrue(player.isSleeping() && player.level() == end, "still asleep in the End before 100 ticks");
            sleep(player, 1);
            assertArrived(helper, player, overworld, overworldHead);
            helper.assertTrue(overworld.getDayTime() == 6000, "travel should never change the time");
        } finally {
            player.serverLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
            overworld.setDayTime(time);
            overworld.updateSkyBrightness();
        }
        helper.succeed();
    }

    /**
     * A (level-ticked) mock player lying in an Ender Bed during the day: vanilla would wake them at once, and nothing
     * holds them up in a hanging bed. Mock players aren't {@link ServerPlayer}s, so they never travel.
     */
    @GameTest(template = ModGameTests.EMPTY_TEMPLATE, timeoutTicks = 200)
    public static void sleeperStaysInBedAllDay(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        EnderBedGameTests.hangBed(helper);
        BlockPos head = helper.absolutePos(EnderBedGameTests.HEAD);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.moveTo(EnderBedRules.arrivalPos(head));
        overworld.addFreshEntity(player);
        player.startSleeping(head);
        ((PlayerAccessor) player).someonesperfectend$setSleepCounter(0);
        long time = overworld.getDayTime();
        int[] tick = {0};
        helper.onEachTick(() -> {
            if (tick[0] > 120) {
                return;
            }
            overworld.setDayTime(6000);
            overworld.updateSkyBrightness();
            helper.assertTrue(overworld.isDay(), "the test expects daytime");
            helper.assertTrue(player.isSleeping(), "an Ender Bed sleeper should stay asleep during the day, tick "
                    + tick[0]);
            helper.assertTrue(player.getY() == head.getY() + 0.6875 && player.getDeltaMovement().equals(Vec3.ZERO),
                    "the sleeper should stay in the hanging bed, got y=" + player.getY());
            if (++tick[0] > 120) {
                helper.assertTrue(player.isSleepingLongEnough(), "the player should be fully asleep");
                player.stopSleepInBed(false, true);
                helper.assertTrue(player.position().equals(EnderBedRules.arrivalPos(head)),
                        "waking should drop the player under the bed, got " + player.position());
                player.discard();
                overworld.setDayTime(time);
                overworld.updateSkyBrightness();
                helper.succeed();
            }
        });
    }

    @GameTest(template = ModGameTests.EMPTY_TEMPLATE)
    public static void leavingTheBedEarlyCancelsTheTrip(GameTestHelper helper) {
        ServerLevel end = EnderBedRespawnGameTests.end(helper);
        EnderBedGameTests.hangBed(helper);
        BlockPos overworldHead = helper.absolutePos(EnderBedGameTests.HEAD);
        BlockPos endFoot = endFoot(1);
        EnderBedGameTests.hangBedIn(end, endFoot);
        ServerPlayer player = playerUnderOverworldBed(helper, Detached.in(helper.getLevel()));
        beds(player).set(Level.END, endFoot.south());
        EnderBedGameTests.use(helper.getLevel(), overworldHead, player);
        helper.assertTrue(player.isSleeping(), "the player should lie down");

        sleep(player, 50);
        // What the "Leave Bed" button does on the server.
        player.stopSleepInBed(false, true);
        sleep(player, 100);
        helper.assertTrue(player.level() == helper.getLevel(), "leaving the bed early should cancel the trip");
        helper.assertFalse(player.isSleeping(), "the player should be awake");
        helper.assertTrue(beds(player).end().equals(Optional.of(endFoot.south())), "cancelling should keep `end`");
        helper.assertFalse(helper.getLevel().getBlockState(overworldHead).getValue(EnderBedBlock.OCCUPIED),
                "the bed should be free again");
        helper.succeed();
    }

    private static void missingDestination(GameTestHelper helper, BlockPos endHead) {
        BlockPos overworldHead = helper.absolutePos(EnderBedGameTests.HEAD);
        Detached detached = Detached.in(helper.getLevel());
        ServerPlayer player = playerUnderOverworldBed(helper, detached);
        beds(player).set(Level.END, endHead);
        EnderBedGameTests.use(helper.getLevel(), overworldHead, player);
        helper.assertTrue(player.isSleeping(), "a stored destination should lie the player down even if it's gone");
        sleep(player, EnderBedTravel.SLEEP_TICKS - 1);
        helper.assertTrue(player.isSleeping(), "the full sleep animation should play");
        sleep(player, 1);
        helper.assertTrue(player.level() == helper.getLevel(), "a missing destination keeps the player here");
        helper.assertFalse(player.isSleeping(), "the player should wake");
        helper.assertTrue(player.position().equals(EnderBedRules.arrivalPos(overworldHead)),
                "the player should wake under the same bed, got " + player.position());
        helper.assertTrue(detached.sentActionBar(EnderBedTravel.MISSING_KEY),
                "expected \"Your Ender Bed was missing or obstructed\"");
        helper.assertTrue(beds(player).end().isEmpty(), "the missing destination should be cleared");
        helper.assertTrue(beds(player).overworld().equals(Optional.of(overworldHead)), "`overworld` should stay");
        helper.succeed();
    }

    @GameTest(template = ModGameTests.EMPTY_TEMPLATE)
    public static void missingDestinationWakesInTheSameBed(GameTestHelper helper) {
        ServerLevel end = EnderBedRespawnGameTests.end(helper);
        BlockPos endHead = endFoot(2).south();
        end.setBlock(endHead, Blocks.AIR.defaultBlockState(), 2);
        EnderBedGameTests.hangBed(helper);
        missingDestination(helper, endHead);
    }

    @GameTest(template = ModGameTests.EMPTY_TEMPLATE)
    public static void obstructedDestinationWakesInTheSameBed(GameTestHelper helper) {
        ServerLevel end = EnderBedRespawnGameTests.end(helper);
        BlockPos endFoot = endFoot(3);
        EnderBedGameTests.hangBedIn(end, endFoot);
        end.setBlock(endFoot.south().below(2), Blocks.END_STONE.defaultBlockState(), 2);
        EnderBedGameTests.hangBed(helper);
        missingDestination(helper, endFoot.south());
    }

    @GameTest(template = ModGameTests.EMPTY_TEMPLATE)
    public static void enderBedSleepersDontSkipTheNight(GameTestHelper helper) {
        EnderBedGameTests.hangBed(helper);
        ServerPlayer player = playerUnderOverworldBed(helper, Detached.in(helper.getLevel()));
        beds(player).set(Level.END, endFoot(4).south());
        EnderBedGameTests.use(helper.getLevel(), helper.absolutePos(EnderBedGameTests.HEAD), player);
        ((PlayerAccessor) player).someonesperfectend$setSleepCounter(EnderBedTravel.SLEEP_TICKS);
        helper.assertTrue(player.isSleeping() && player.isSleepingLongEnough(), "the player should be deep asleep");
        SleepStatus status = new SleepStatus();
        status.update(List.of(player));
        helper.assertTrue(status.amountSleeping() == 0, "an Ender Bed sleeper should not count as sleeping");
        helper.assertFalse(status.areEnoughDeepSleeping(0, List.of(player)),
                "an Ender Bed sleeper should never let the night be skipped");
        player.stopSleepInBed(false, true);
        helper.succeed();
    }
}
