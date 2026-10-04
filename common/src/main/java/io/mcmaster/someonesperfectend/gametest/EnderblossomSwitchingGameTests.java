package io.mcmaster.someonesperfectend.gametest;

import io.mcmaster.someonesperfectend.block.EnderblossomBlock;
import io.mcmaster.someonesperfectend.block.Enderblossoms;
import io.mcmaster.someonesperfectend.block.ModBlocks;
import io.mcmaster.someonesperfectend.entity.Endlings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * MOD-60: Enderblossoms open while an Enderman is within 8 blocks, close after it leaves, and ripple to neighbors.
 *
 * <p>Each test with an Enderman runs in its own batch: tests of one batch sit only 5 blocks apart, so an Enderman
 * in one test would reach the blossoms of the next.
 */
public final class EnderblossomSwitchingGameTests {
    /** The blossom grows on the west side of its log, so its center is (0.5, 2.5, 0.5). */
    private static final BlockPos BLOSSOM = new BlockPos(0, 2, 0);

    private static final BlockPos POTTED = new BlockPos(0, 2, 2);
    /** 6.0 blocks from the blossom's center, 6.3 from the pot's. */
    private static final BlockPos NEAR_ENDERMAN = new BlockPos(6, 2, 0);

    private EnderblossomSwitchingGameTests() {}

    /** Places a closed wall blossom at {@code pos} on an Astral log east of it. */
    private static void placeClosedBlossom(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos.east(), ModBlocks.ASTRAL_LOG.get());
        helper.setBlock(
                pos,
                ModBlocks.CLOSED_ENDERBLOSSOM
                        .get()
                        .defaultBlockState()
                        .setValue(EnderblossomBlock.FACING, Direction.WEST));
    }

    /** Spawns a mob without AI standing on end stone at {@code pos}. */
    private static <T extends Mob> T spawnStill(GameTestHelper helper, EntityType<T> type, BlockPos pos) {
        helper.setBlock(pos.below(), Blocks.END_STONE);
        T mob = helper.spawn(type, Vec3.atBottomCenterOf(pos));
        mob.setNoAi(true);
        return mob;
    }

    private static void assertBlossom(GameTestHelper helper, BlockPos pos, Block block) {
        BlockState state = helper.getBlockState(pos);
        helper.assertTrue(state.is(block), "expected " + block + " at " + pos + " but found " + state);
        if (state.hasProperty(EnderblossomBlock.FACING)) {
            helper.assertTrue(
                    state.getValue(EnderblossomBlock.FACING) == Direction.WEST,
                    "the blossom lost its facing: " + state);
        }
    }

    @GameTest
    public static void placingSchedulesARepeatingCheck(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        placeClosedBlossom(helper, BLOSSOM);
        helper.setBlock(POTTED, ModBlocks.POTTED_CLOSED_ENDERBLOSSOM.get());
        Runnable assertScheduled = () -> {
            helper.assertTrue(
                    level.getBlockTicks()
                            .hasScheduledTick(helper.absolutePos(BLOSSOM), ModBlocks.CLOSED_ENDERBLOSSOM.get()),
                    "the wall blossom has no scheduled check");
            helper.assertTrue(
                    level.getBlockTicks()
                            .hasScheduledTick(helper.absolutePos(POTTED), ModBlocks.POTTED_CLOSED_ENDERBLOSSOM.get()),
                    "the potted blossom has no scheduled check");
        };
        assertScheduled.run();
        // Still scheduled after several checks have run.
        helper.runAfterDelay(35, () -> {
            assertScheduled.run();
            helper.succeed();
        });
    }

    @GameTest(batch = "enderblossom_enderman_near", timeoutTicks = 100)
    public static void opensNearEndermanAndClosesWhenItLeaves(GameTestHelper helper) {
        placeClosedBlossom(helper, BLOSSOM);
        helper.setBlock(POTTED, ModBlocks.POTTED_CLOSED_ENDERBLOSSOM.get());
        EnderMan enderman = spawnStill(helper, EntityType.ENDERMAN, NEAR_ENDERMAN);
        helper.runAfterDelay(20, () -> {
            assertBlossom(helper, BLOSSOM, ModBlocks.OPEN_ENDERBLOSSOM.get());
            assertBlossom(helper, POTTED, ModBlocks.POTTED_OPEN_ENDERBLOSSOM.get());
            enderman.discard();
            helper.runAfterDelay(20, () -> {
                assertBlossom(helper, BLOSSOM, ModBlocks.CLOSED_ENDERBLOSSOM.get());
                assertBlossom(helper, POTTED, ModBlocks.POTTED_CLOSED_ENDERBLOSSOM.get());
                helper.succeed();
            });
        });
    }

    @GameTest(batch = "enderblossom_enderman_far", timeoutTicks = 100)
    public static void endermanTenBlocksAwayLeavesItClosed(GameTestHelper helper) {
        placeClosedBlossom(helper, BLOSSOM);
        // (7.5, 4, 7.5) is 10.0 blocks from the blossom's center.
        spawnStill(helper, EntityType.ENDERMAN, new BlockPos(7, 4, 7));
        helper.runAfterDelay(40, () -> {
            assertBlossom(helper, BLOSSOM, ModBlocks.CLOSED_ENDERBLOSSOM.get());
            helper.succeed();
        });
    }

    @GameTest(batch = "enderblossom_enderman_open", timeoutTicks = 100)
    public static void openBlossomStaysOpenNearEnderman(GameTestHelper helper) {
        helper.setBlock(BLOSSOM.east(), ModBlocks.ASTRAL_LOG.get());
        helper.setBlock(
                BLOSSOM,
                ModBlocks.OPEN_ENDERBLOSSOM
                        .get()
                        .defaultBlockState()
                        .setValue(EnderblossomBlock.FACING, Direction.WEST));
        spawnStill(helper, EntityType.ENDERMAN, NEAR_ENDERMAN);
        helper.runAfterDelay(40, () -> {
            assertBlossom(helper, BLOSSOM, ModBlocks.OPEN_ENDERBLOSSOM.get());
            helper.succeed();
        });
    }

    /** No Enderman around: an open blossom (wall or potted) closes on its own check. */
    @GameTest(timeoutTicks = 100)
    public static void openBlossomClosesWithoutEnderman(GameTestHelper helper) {
        helper.setBlock(BLOSSOM.east(), ModBlocks.ASTRAL_LOG.get());
        helper.setBlock(
                BLOSSOM,
                ModBlocks.OPEN_ENDERBLOSSOM
                        .get()
                        .defaultBlockState()
                        .setValue(EnderblossomBlock.FACING, Direction.WEST));
        helper.setBlock(POTTED, ModBlocks.POTTED_OPEN_ENDERBLOSSOM.get());
        helper.runAfterDelay(20, () -> {
            assertBlossom(helper, BLOSSOM, ModBlocks.CLOSED_ENDERBLOSSOM.get());
            assertBlossom(helper, POTTED, ModBlocks.POTTED_CLOSED_ENDERBLOSSOM.get());
            helper.succeed();
        });
    }

    @GameTest(timeoutTicks = 100)
    public static void endlingsAndOtherMobsNeverOpenIt(GameTestHelper helper) {
        placeClosedBlossom(helper, BLOSSOM);
        // 3 blocks away.
        spawnStill(helper, Endlings.TYPE.get(), new BlockPos(3, 2, 0));
        spawnStill(helper, EntityType.ZOMBIE, new BlockPos(0, 2, 3));
        spawnStill(helper, EntityType.PIG, new BlockPos(2, 2, 2));
        helper.runAfterDelay(40, () -> {
            assertBlossom(helper, BLOSSOM, ModBlocks.CLOSED_ENDERBLOSSOM.get());
            helper.succeed();
        });
    }

    @GameTest(batch = "enderblossom_enderman_ripple", timeoutTicks = 100)
    public static void switchRipplesToSameTypeNeighbor(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos neighbor = BLOSSOM.south(2);
        BlockPos outsideRipple = BLOSSOM.south(4);
        placeClosedBlossom(helper, BLOSSOM);
        placeClosedBlossom(helper, neighbor);
        placeClosedBlossom(helper, outsideRipple);
        // Within 8 blocks of all three blossoms.
        spawnStill(helper, EntityType.ENDERMAN, NEAR_ENDERMAN);

        BlockPos abs = helper.absolutePos(BLOSSOM);
        boolean switched = Enderblossoms.check(level.getBlockState(abs), level, abs, level.getRandom(), false);
        helper.assertTrue(switched, "the blossom did not open next to an Enderman");
        assertBlossom(helper, BLOSSOM, ModBlocks.OPEN_ENDERBLOSSOM.get());
        helper.assertTrue(
                Enderblossoms.ripplePending(level, helper.absolutePos(neighbor)),
                "no ripple check queued for the closed blossom 2 blocks away");
        helper.assertFalse(
                Enderblossoms.ripplePending(level, helper.absolutePos(outsideRipple)),
                "a ripple check was queued for a blossom 4 blocks away");
        helper.assertFalse(Enderblossoms.ripplePending(level, abs), "the switched blossom rippled to itself");
        // Otherwise its own check would open it and queue another ripple to the neighbor.
        helper.setBlock(outsideRipple, Blocks.AIR);
        // A ripple 2 blocks away runs within 2 x 10 ticks.
        helper.runAfterDelay(21, () -> {
            assertBlossom(helper, neighbor, ModBlocks.OPEN_ENDERBLOSSOM.get());
            helper.assertFalse(
                    Enderblossoms.ripplePending(level, helper.absolutePos(neighbor)), "the ripple check never ran");
            helper.succeed();
        });
    }
}
