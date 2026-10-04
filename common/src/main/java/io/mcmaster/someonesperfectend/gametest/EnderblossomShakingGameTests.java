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
 * 8 blocks or a player looks at them; potted ones never do.
 *
 * <p>Tests with an Enderman run in their own batch, like the switching tests. Looking tests pass a mock player to
 * {@link EnderblossomShaking#checkLookers} directly: a mock server player in the level breaks Accessories' networking
 * on NeoForge.
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
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 head = head(helper, pos);
        player.moveTo(head.x - 5.0, head.y - player.getEyeHeight(), head.z, 0.0F, 0.0F);
        player.lookAt(EntityAnchorArgument.Anchor.EYES, head);
        return player;
    }

    @GameTest
    public static void playerLookingAtBlossomShakesIt(GameTestHelper helper) {
        placeBlossom(helper, LOOKED_AT, ModBlocks.CLOSED_ENDERBLOSSOM.get());
        Player player = playerLookingAt(helper, LOOKED_AT);
        BlockPos abs = helper.absolutePos(LOOKED_AT);
        helper.assertTrue(
                EnderblossomShaking.isLookingAt(player, EnderblossomShaking.lookBox(abs)),
                "the player isn't looking at the blossom");
        EnderblossomShaking.checkLookers(helper.getLevel(), abs, helper.getBlockState(LOOKED_AT), List.of(player));
        helper.runAfterDelay(1, () -> {
            helper.assertTrue(blossomEntity(helper, LOOKED_AT).shakes() == 1, "looking didn't shake the blossom");
            // Still looking a tick later: no new shake yet.
            EnderblossomShaking.checkLookers(helper.getLevel(), abs, helper.getBlockState(LOOKED_AT), List.of(player));
            helper.runAfterDelay(1, () -> {
                helper.assertTrue(blossomEntity(helper, LOOKED_AT).shakes() == 1, "it shook again right away");
                helper.succeed();
            });
        });
    }

    private static AABB lookBox(GameTestHelper helper, BlockPos pos) {
        return EnderblossomShaking.lookBox(helper.absolutePos(pos));
    }

    /** The owner's playtest found that only the top few pixels counted; the stem and lower petals must count too. */
    @GameTest
    public static void playerLookingAtLowerPetalsShakesIt(GameTestHelper helper) {
        placeBlossom(helper, LOOKED_AT, ModBlocks.CLOSED_ENDERBLOSSOM.get());
        Player player = playerLookingAt(helper, LOOKED_AT);
        Vec3 lowerPetals = Vec3.atCenterOf(helper.absolutePos(LOOKED_AT)).add(0.0, -0.3, 0.3);
        player.lookAt(EntityAnchorArgument.Anchor.EYES, lowerPetals);
        BlockPos abs = helper.absolutePos(LOOKED_AT);
        helper.assertTrue(
                EnderblossomShaking.isLookingAt(player, EnderblossomShaking.lookBox(abs)),
                "looking at the lower petals didn't count");
        EnderblossomShaking.checkLookers(helper.getLevel(), abs, helper.getBlockState(LOOKED_AT), List.of(player));
        helper.runAfterDelay(1, () -> {
            helper.assertTrue(blossomEntity(helper, LOOKED_AT).shakes() == 1, "looking didn't shake the blossom");
            helper.succeed();
        });
    }

    @GameTest
    public static void playerWearingCarvedPumpkinDoesNotShakeIt(GameTestHelper helper) {
        placeBlossom(helper, LOOKED_AT, ModBlocks.CLOSED_ENDERBLOSSOM.get());
        Player player = playerLookingAt(helper, LOOKED_AT);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CARVED_PUMPKIN));
        helper.assertFalse(
                EnderblossomShaking.isLookingAt(player, lookBox(helper, LOOKED_AT)),
                "the pumpkin didn't hide the stare");
        EnderblossomShaking.checkLookers(
                helper.getLevel(), helper.absolutePos(LOOKED_AT), helper.getBlockState(LOOKED_AT), List.of(player));
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(blossomEntity(helper, LOOKED_AT).shakes() == 0, "the blossom shook");
            helper.succeed();
        });
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
        EnderblossomShaking.checkLookers(
                helper.getLevel(),
                helper.absolutePos(LOOKED_AT),
                helper.getBlockState(LOOKED_AT),
                List.of(away, blocked));
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(blossomEntity(helper, LOOKED_AT).shakes() == 0, "the blossom shook");
            helper.succeed();
        });
    }
}
