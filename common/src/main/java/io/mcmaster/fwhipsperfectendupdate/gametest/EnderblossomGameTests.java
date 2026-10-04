package io.mcmaster.fwhipsperfectendupdate.gametest;

import io.mcmaster.fwhipsperfectendupdate.block.EnderblossomBlock;
import io.mcmaster.fwhipsperfectendupdate.block.ModBlocks;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** MOD-59: Enderblossom placement on Astral logs, popping off, drops, pots and tags. */
public final class EnderblossomGameTests {
    private static final BlockPos LOG = new BlockPos(3, 2, 3);

    private EnderblossomGameTests() {}

    private static List<EnderblossomBlock> blossoms() {
        return List.of(ModBlocks.OPEN_ENDERBLOSSOM.get(), ModBlocks.CLOSED_ENDERBLOSSOM.get());
    }

    /** Clicks {@code face} of the block at {@code pos} with {@code blossom} in a survival mock player's hand. */
    private static void clickFace(GameTestHelper helper, Block blossom, BlockPos pos, Direction face) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(blossom));
        BlockPos abs = helper.absolutePos(pos);
        Vec3 hit = Vec3.atCenterOf(abs).relative(face, 0.5);
        player.getMainHandItem()
                .useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, abs, false)));
    }

    @GameTest
    public static void placesOnEverySideOfAstralLogs(GameTestHelper helper) {
        for (Block log : List.of(ModBlocks.ASTRAL_LOG.get(), ModBlocks.FLOWERING_ASTRAL_LOG.get())) {
            for (EnderblossomBlock blossom : blossoms()) {
                helper.setBlock(LOG, log);
                for (Direction face : Direction.Plane.HORIZONTAL) {
                    BlockPos pos = LOG.relative(face);
                    helper.setBlock(pos, Blocks.AIR);
                    clickFace(helper, blossom, LOG, face);
                    BlockState state = helper.getBlockState(pos);
                    helper.assertTrue(
                            state.is(blossom),
                            blossom + " not placed on the " + face + " side of " + log + ": " + state);
                    helper.assertTrue(
                            state.getValue(EnderblossomBlock.FACING) == face,
                            blossom + " on the " + face + " side faces " + state.getValue(EnderblossomBlock.FACING));
                }
            }
        }
        helper.succeed();
    }

    @GameTest
    public static void topAndBottomFacesFail(GameTestHelper helper) {
        helper.setBlock(LOG, ModBlocks.ASTRAL_LOG.get());
        for (EnderblossomBlock blossom : blossoms()) {
            for (Direction face : List.of(Direction.UP, Direction.DOWN)) {
                clickFace(helper, blossom, LOG, face);
                helper.assertBlockPresent(Blocks.AIR, LOG.relative(face));
            }
        }
        helper.succeed();
    }

    @GameTest
    public static void endStoneSideFails(GameTestHelper helper) {
        helper.setBlock(LOG, Blocks.END_STONE);
        for (EnderblossomBlock blossom : blossoms()) {
            for (Direction face : Direction.Plane.HORIZONTAL) {
                clickFace(helper, blossom, LOG, face);
                helper.assertBlockPresent(Blocks.AIR, LOG.relative(face));
            }
        }
        helper.succeed();
    }

    @GameTest
    public static void breakingTheLogPopsItOff(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (EnderblossomBlock blossom : blossoms()) {
            BlockPos pos = LOG.east();
            helper.setBlock(LOG, ModBlocks.ASTRAL_LOG.get());
            helper.setBlock(pos, blossom.defaultBlockState().setValue(EnderblossomBlock.FACING, Direction.EAST));
            level.destroyBlock(helper.absolutePos(LOG), false);
            helper.assertBlockPresent(Blocks.AIR, pos);
            assertDropped(helper, pos, blossom);
        }
        helper.succeed();
    }

    @GameTest
    public static void replacingTheLogPopsItOff(GameTestHelper helper) {
        for (EnderblossomBlock blossom : blossoms()) {
            BlockPos pos = LOG.north();
            helper.setBlock(LOG, ModBlocks.ASTRAL_LOG.get());
            helper.setBlock(pos, blossom.defaultBlockState().setValue(EnderblossomBlock.FACING, Direction.NORTH));
            // Another Astral log keeps it; end stone does not.
            helper.setBlock(LOG, ModBlocks.STRIPPED_ASTRAL_LOG.get());
            helper.assertBlockPresent(blossom, pos);
            helper.setBlock(LOG, Blocks.END_STONE);
            helper.assertBlockPresent(Blocks.AIR, pos);
            assertDropped(helper, pos, blossom);
        }
        helper.succeed();
    }

    /** Asserts that exactly one {@code blossom} item lies around {@code pos}, then removes the drops. */
    private static void assertDropped(GameTestHelper helper, BlockPos pos, Block blossom) {
        List<ItemEntity> drops =
                helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(pos)).inflate(1.5));
        int count = drops.stream()
                .filter(drop -> drop.getItem().is(blossom.asItem()))
                .mapToInt(drop -> drop.getItem().getCount())
                .sum();
        helper.assertTrue(
                count == 1 && drops.size() == 1,
                blossom + " popped off and dropped "
                        + drops.stream().map(ItemEntity::getItem).toList());
        drops.forEach(ItemEntity::discard);
    }

    @GameTest
    public static void breaksInstantlyDropsItselfAndHasNoCollision(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = LOG.south();
        helper.setBlock(LOG, ModBlocks.ASTRAL_LOG.get());
        for (EnderblossomBlock blossom : blossoms()) {
            BlockState state = blossom.defaultBlockState().setValue(EnderblossomBlock.FACING, Direction.SOUTH);
            helper.setBlock(pos, state);
            BlockPos abs = helper.absolutePos(pos);
            helper.assertTrue(state.getDestroySpeed(level, abs) == 0.0F, blossom + " does not break instantly");
            helper.assertTrue(state.getCollisionShape(level, abs).isEmpty(), blossom + " has a collision shape");
            List<ItemStack> drops = Block.getDrops(state, level, abs, null);
            helper.assertTrue(
                    drops.size() == 1
                            && drops.get(0).is(blossom.asItem())
                            && drops.get(0).getCount() == 1,
                    blossom + " drops " + drops);
        }
        helper.succeed();
    }

    @GameTest
    public static void goesInFlowerPots(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        assertPots(helper, pos, ModBlocks.OPEN_ENDERBLOSSOM.get(), ModBlocks.POTTED_OPEN_ENDERBLOSSOM.get());
        assertPots(helper, pos, ModBlocks.CLOSED_ENDERBLOSSOM.get(), ModBlocks.POTTED_CLOSED_ENDERBLOSSOM.get());
        helper.succeed();
    }

    private static void assertPots(GameTestHelper helper, BlockPos pos, Block blossom, FlowerPotBlock potted) {
        helper.assertTrue(potted.getPotted() == blossom, potted + " holds " + potted.getPotted());
        helper.setBlock(pos, Blocks.FLOWER_POT);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(blossom));
        helper.useBlock(pos, player);
        helper.assertBlockPresent(potted, pos);
    }

    @GameTest
    public static void notFlowersAndNoToolTag(GameTestHelper helper) {
        for (EnderblossomBlock blossom : blossoms()) {
            BlockState state = blossom.defaultBlockState();
            helper.assertFalse(state.is(BlockTags.FLOWERS), blossom + " is in #minecraft:flowers");
            helper.assertFalse(state.is(BlockTags.SMALL_FLOWERS), blossom + " is in #minecraft:small_flowers");
            helper.assertFalse(new ItemStack(blossom).is(ItemTags.FLOWERS), blossom + " item is a flower");
            helper.assertFalse(new ItemStack(blossom).is(ItemTags.SMALL_FLOWERS), blossom + " item is a small flower");
            for (var tool : List.of(
                    BlockTags.MINEABLE_WITH_AXE,
                    BlockTags.MINEABLE_WITH_HOE,
                    BlockTags.MINEABLE_WITH_PICKAXE,
                    BlockTags.MINEABLE_WITH_SHOVEL)) {
                helper.assertFalse(state.is(tool), blossom + " is in " + tool.location());
            }
        }
        helper.succeed();
    }
}
