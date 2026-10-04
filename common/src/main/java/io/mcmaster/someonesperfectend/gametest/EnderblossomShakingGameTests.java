package io.mcmaster.someonesperfectend.gametest;

import io.mcmaster.someonesperfectend.block.EnderblossomBlock;
import io.mcmaster.someonesperfectend.block.EnderblossomBlockEntity;
import io.mcmaster.someonesperfectend.block.EnderblossomShaking;
import io.mcmaster.someonesperfectend.block.ModBlocks;
import io.mcmaster.someonesperfectend.mixin.EnderManInvoker;
import java.util.List;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * MOD-62: wall Enderblossoms shake (a block event counted by their block entity) when an Enderman teleports within
 * 8 blocks, and tremble for as long as a player looks at them; potted ones never do.
 *
 * <p>Tests with an Enderman run in their own batch, like the switching tests. Looking tests pass a mock player to
 * {@link EnderblossomShaking#checkLookers} directly and check the result right away, before the blossom's own ticker
 * (which only sees real players) runs: a mock server player in the level breaks Accessories' networking on NeoForge.
 */
public final class EnderblossomShakingGameTests {
    /** Grows on the west side of its log, so its center is (0.5, 2.5, 0.5). */
    private static final BlockPos BLOSSOM = new BlockPos(0, 2, 0);
    /** (7.5, 2, 7.5) is 9.9 blocks from the blossom's center. */
    private static final BlockPos FAR_ENDERMAN = new BlockPos(7, 2, 7);
    /** A blossom near the east edge, facing west so players to the west can see it. */
    private static final BlockPos LOOKED_AT = new BlockPos(6, 2, 3);

    private EnderblossomShakingGameTests() {}

    private static void placeBlossom(GameTestHelper helper, BlockPos pos, Block block) {
        helper.setBlock(pos.east(), ModBlocks.ASTRAL_LOG.get());
        helper.setBlock(pos, block.defaultBlockState().setValue(EnderblossomBlock.FACING, Direction.WEST));
    }

    private static EnderblossomBlockEntity blossomEntity(GameTestHelper helper, BlockPos pos) {
        if (helper.getLevel().getBlockEntity(helper.absolutePos(pos)) instanceof EnderblossomBlockEntity entity) {
            return entity;
        }
        throw new IllegalStateException("no Enderblossom block entity at " + pos);
    }

    private static EnderMan spawnStillEnderman(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos.below(), Blocks.END_STONE);
        EnderMan enderman = helper.spawn(EntityType.ENDERMAN, Vec3.atBottomCenterOf(pos));
        enderman.setNoAi(true);
        return enderman;
    }

    /** Teleports {@code enderman} onto end stone placed under {@code target}; fails the test if it can't. */
    private static void teleport(GameTestHelper helper, EnderMan enderman, BlockPos target) {
        helper.setBlock(target.below(), Blocks.END_STONE);
        Vec3 to = helper.absoluteVec(Vec3.atBottomCenterOf(target));
        helper.assertTrue(
                ((EnderManInvoker) enderman).someonesperfectend$teleport(to.x, to.y, to.z),
                "the Enderman couldn't teleport to " + target);
        helper.assertTrue(enderman.position().distanceTo(to) < 0.01, "the Enderman ended up at " + enderman.position());
    }

    private static Vec3 head(GameTestHelper helper, BlockPos pos) {
        BlockPos abs = helper.absolutePos(pos);
        BlockState state = helper.getLevel().getBlockState(abs);
        return ((EnderblossomBlock) state.getBlock()).headPosition(state, abs);
    }

    @GameTest
    public static void wallBlossomsAreAnimatedPoisAndPotsAreNot(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        placeBlossom(helper, BLOSSOM, ModBlocks.CLOSED_ENDERBLOSSOM.get());
        placeBlossom(helper, new BlockPos(0, 2, 2), ModBlocks.OPEN_ENDERBLOSSOM.get());
        BlockPos pot = new BlockPos(3, 2, 0);
        helper.setBlock(pot, ModBlocks.POTTED_OPEN_ENDERBLOSSOM.get());
        for (BlockPos pos : List.of(BLOSSOM, new BlockPos(0, 2, 2))) {
            BlockState state = helper.getBlockState(pos);
            helper.assertTrue(
                    state.getRenderShape() == RenderShape.ENTITYBLOCK_ANIMATED, "not drawn by its renderer: " + state);
            blossomEntity(helper, pos);
            helper.assertTrue(
                    PoiTypes.forState(state)
                            .map(type -> type.is(EnderblossomShaking.POI_KEY))
                            .orElse(false),
                    "no enderblossom POI for " + state);
            helper.assertTrue(
                    level.getPoiManager().existsAtPosition(EnderblossomShaking.POI_KEY, helper.absolutePos(pos)),
                    "the POI manager doesn't know the blossom at " + pos);
        }
        for (Block potted :
                List.of(ModBlocks.POTTED_OPEN_ENDERBLOSSOM.get(), ModBlocks.POTTED_CLOSED_ENDERBLOSSOM.get())) {
            BlockState state = potted.defaultBlockState();
            helper.assertFalse(state.hasBlockEntity(), "potted blossoms have no block entity");
            helper.assertFalse(PoiTypes.hasPoi(state), "potted blossoms are no POI");
        }
        helper.assertTrue(level.getBlockEntity(helper.absolutePos(pot)) == null, "the pot got a block entity");
        // Shaking a pot does nothing; shaking a wall blossom runs the block event.
        EnderblossomShaking.shake(level, helper.absolutePos(pot));
        EnderblossomShaking.shake(level, helper.absolutePos(BLOSSOM));
        helper.succeedWhen(
                () -> helper.assertTrue(blossomEntity(helper, BLOSSOM).shakes() == 1, "no shake event"));
    }

    @GameTest(batch = "enderblossom_teleport_near", timeoutTicks = 60)
    public static void endermanTeleportingNearShakesBlossoms(GameTestHelper helper) {
        // Open, so the Enderman arriving within 8 blocks doesn't switch it (which would replace the block entity).
        placeBlossom(helper, BLOSSOM, ModBlocks.OPEN_ENDERBLOSSOM.get());
        BlockPos pot = new BlockPos(0, 2, 2);
        helper.setBlock(pot, ModBlocks.POTTED_OPEN_ENDERBLOSSOM.get());
        EnderMan enderman = spawnStillEnderman(helper, FAR_ENDERMAN);
        // (5.5, 2, 0.5) is 5.0 blocks from the blossom's center.
        teleport(helper, enderman, new BlockPos(5, 2, 0));
        helper.succeedWhen(() -> {
            helper.assertTrue(blossomEntity(helper, BLOSSOM).shakes() == 1, "the blossom didn't shake once");
            helper.assertBlockPresent(ModBlocks.POTTED_OPEN_ENDERBLOSSOM.get(), pot);
        });
    }

    @GameTest(batch = "enderblossom_teleport_start", timeoutTicks = 60)
    public static void endermanTeleportingAwayShakesBlossoms(GameTestHelper helper) {
        // Closed: it is still closed when the Enderman has left, so its block entity isn't replaced.
        placeBlossom(helper, BLOSSOM, ModBlocks.CLOSED_ENDERBLOSSOM.get());
        // Starts 5.0 blocks away and lands 10.5 away.
        EnderMan enderman = spawnStillEnderman(helper, new BlockPos(5, 2, 0));
        teleport(helper, enderman, new BlockPos(7, 6, 7));
        helper.succeedWhen(
                () -> helper.assertTrue(blossomEntity(helper, BLOSSOM).shakes() == 1, "the blossom didn't shake once"));
    }

    @GameTest(batch = "enderblossom_teleport_far", timeoutTicks = 60)
    public static void endermanTeleportingFarAwayDoesNothing(GameTestHelper helper) {
        placeBlossom(helper, BLOSSOM, ModBlocks.CLOSED_ENDERBLOSSOM.get());
        EnderMan enderman = spawnStillEnderman(helper, FAR_ENDERMAN);
        // From 9.9 to 10.5 blocks away.
        teleport(helper, enderman, new BlockPos(7, 6, 7));
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(blossomEntity(helper, BLOSSOM).shakes() == 0, "the blossom shook");
            helper.succeed();
        });
    }

    /** A player 5 blocks west of the blossom, looking straight at its flower head. */
    private static Player playerLookingAt(GameTestHelper helper, BlockPos pos) {
        Vec3 head = head(helper, pos);
        return playerAt(helper, head.add(-5.0, 0.0, 0.0), head);
    }

    /** A player whose eyes are at {@code eye}, looking at {@code target}. */
    private static Player playerAt(GameTestHelper helper, Vec3 eye, Vec3 target) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.moveTo(eye.x, eye.y - player.getEyeHeight(), eye.z, 0.0F, 0.0F);
        player.lookAt(EntityAnchorArgument.Anchor.EYES, target);
        return player;
    }

    private static void checkLookers(GameTestHelper helper, BlockPos pos, List<Player> players) {
        EnderblossomShaking.checkLookers(
                helper.getLevel(), helper.absolutePos(pos), helper.getBlockState(pos), players);
    }

    private static AABB lookBox(GameTestHelper helper, BlockPos pos) {
        return EnderblossomShaking.lookBox(helper.absolutePos(pos));
    }

    /** Like a stared-at Enderman, the blossom keeps trembling for as long as the look lasts, and stops right after. */
    @GameTest
    public static void playerLookingAtBlossomKeepsItTrembling(GameTestHelper helper) {
        placeBlossom(helper, LOOKED_AT, ModBlocks.CLOSED_ENDERBLOSSOM.get());
        Player player = playerLookingAt(helper, LOOKED_AT);
        helper.assertTrue(
                EnderblossomShaking.isLookingAt(player, lookBox(helper, LOOKED_AT)),
                "the player isn't looking at the blossom");
        EnderblossomBlockEntity entity = blossomEntity(helper, LOOKED_AT);
        for (int tick = 0; tick < 60; tick++) {
            checkLookers(helper, LOOKED_AT, List.of(player));
            helper.assertTrue(entity.staredAt(), "the blossom stopped trembling while looked at");
        }
        helper.assertTrue(entity.stares() == 1, "one long look started " + entity.stares() + " stares");
        checkLookers(helper, LOOKED_AT, List.of());
        helper.assertFalse(entity.staredAt(), "the blossom kept trembling after the player looked away");
        helper.succeed();
    }

    /** The stare is checked every tick, not only in the 10-tick switching check. */
    @GameTest
    public static void blossomChecksForStaresEveryTick(GameTestHelper helper) {
        placeBlossom(helper, LOOKED_AT, ModBlocks.CLOSED_ENDERBLOSSOM.get());
        BlockState state = helper.getBlockState(LOOKED_AT);
        helper.assertTrue(
                state.getTicker(helper.getLevel(), EnderblossomShaking.BLOCK_ENTITY.get()) != null,
                "the wall blossom has no server ticker");
        helper.succeed();
    }

    /** The owner's playtest found that only part of the blossom counted; every open face of its block must count. */
    @GameTest
    public static void playerLookingAtAnyFaceMakesItTremble(GameTestHelper helper) {
        placeBlossom(helper, LOOKED_AT, ModBlocks.CLOSED_ENDERBLOSSOM.get());
        Vec3 center = Vec3.atCenterOf(helper.absolutePos(LOOKED_AT));
        AABB flower = lookBox(helper, LOOKED_AT);
        // The blossom faces west; its east side is the log.
        for (Direction side :
                new Direction[] {Direction.WEST, Direction.NORTH, Direction.SOUTH, Direction.UP, Direction.DOWN}) {
            Vec3 face = center.relative(side, 0.5);
            Vec3 eye = center.relative(side, 1.2);
            // Aim at the middle of the face and at points near each of its edges.
            for (Direction offset : Direction.values()) {
                if (offset.getAxis() == side.getAxis()) {
                    continue;
                }
                for (Vec3 target : new Vec3[] {face, face.relative(offset, 0.4)}) {
                    Player player = playerAt(helper, eye, target);
                    helper.assertTrue(
                            EnderblossomShaking.isLookingAt(player, flower),
                            "looking at the " + side + " face (" + target + ") didn't count");
                    checkLookers(helper, LOOKED_AT, List.of(player));
                    helper.assertTrue(
                            blossomEntity(helper, LOOKED_AT).staredAt(),
                            "looking at the " + side + " face (" + target + ") didn't make it tremble");
                    checkLookers(helper, LOOKED_AT, List.of());
                }
            }
        }
        helper.succeed();
    }

    @GameTest
    public static void playerWearingCarvedPumpkinDoesNotShakeIt(GameTestHelper helper) {
        placeBlossom(helper, LOOKED_AT, ModBlocks.CLOSED_ENDERBLOSSOM.get());
        Player player = playerLookingAt(helper, LOOKED_AT);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CARVED_PUMPKIN));
        helper.assertFalse(
                EnderblossomShaking.isLookingAt(player, lookBox(helper, LOOKED_AT)),
                "the pumpkin didn't hide the stare");
        checkLookers(helper, LOOKED_AT, List.of(player));
        helper.assertTrue(blossomEntity(helper, LOOKED_AT).stares() == 0, "the blossom trembled");
        helper.succeed();
    }

    @GameTest
    public static void playerLookingAwayOrBlockedDoesNotShakeIt(GameTestHelper helper) {
        placeBlossom(helper, LOOKED_AT, ModBlocks.CLOSED_ENDERBLOSSOM.get());
        Vec3 head = head(helper, LOOKED_AT);
        Player away = playerLookingAt(helper, LOOKED_AT);
        away.lookAt(EntityAnchorArgument.Anchor.EYES, head.add(0.0, 3.0, 3.0));
        AABB flower = lookBox(helper, LOOKED_AT);
        helper.assertFalse(EnderblossomShaking.isLookingAt(away, flower), "looking away counted as looking");
        Player blocked = playerLookingAt(helper, LOOKED_AT);
        helper.setBlock(LOOKED_AT.west(2), Blocks.STONE);
        helper.assertFalse(EnderblossomShaking.isLookingAt(blocked, flower), "looking through stone counted");
        checkLookers(helper, LOOKED_AT, List.of(away, blocked));
        helper.assertTrue(blossomEntity(helper, LOOKED_AT).stares() == 0, "the blossom trembled");
        helper.succeed();
    }
}
