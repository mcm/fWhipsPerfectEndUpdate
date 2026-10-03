package io.mcmaster.someonesperfectend.gametest;

import io.mcmaster.someonesperfectend.SomeonesPerfectEnd;
import io.mcmaster.someonesperfectend.block.AstralSaplingBlock;
import io.mcmaster.someonesperfectend.block.ModBlocks;
import io.mcmaster.someonesperfectend.worldgen.ModFeatures;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public final class AstralTreeGameTests {
    /** A 17x24x17 air structure, tall and wide enough for a full Astral tree grown from its center. */
    private static final String TALL = SomeonesPerfectEnd.MOD_ID + ":gametest/empty_tall";

    private static final BlockPos CENTER = new BlockPos(8, 2, 8);

    private AstralTreeGameTests() {}

    @GameTest
    public static void saplingPlacementRules(GameTestHelper helper) {
        Block sapling = ModBlocks.ASTRAL_SAPLING.get();
        BlockPos pos = new BlockPos(1, 2, 1);
        for (Block soil : List.of(
                ModBlocks.END_TURF.get(),
                Blocks.END_STONE,
                ModBlocks.VOIDSOIL.get(),
                Blocks.DIRT,
                Blocks.GRASS_BLOCK,
                Blocks.STONE,
                Blocks.SAND)) {
            helper.setBlock(pos.below(), soil);
            boolean expected =
                    soil == ModBlocks.END_TURF.get() || soil == Blocks.END_STONE || soil == ModBlocks.VOIDSOIL.get();
            helper.assertTrue(
                    sapling.defaultBlockState().canSurvive(helper.getLevel(), helper.absolutePos(pos)) == expected,
                    "astral sapling on " + soil + " should survive: " + expected);
        }
        helper.succeed();
    }

    @GameTest(template = TALL)
    public static void boneMealGrowsAstralTree(GameTestHelper helper) {
        helper.setBlock(CENTER.below(), ModBlocks.END_TURF.get());
        helper.setBlock(CENTER, ModBlocks.ASTRAL_SAPLING.get());
        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(CENTER);
        for (int i = 0; i < 100 && level.getBlockState(abs).is(ModBlocks.ASTRAL_SAPLING.get()); i++) {
            BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), level, abs);
        }
        helper.assertBlockPresent(ModBlocks.ASTRAL_LOG.get(), CENTER.above());
        assertTreeShape(helper, CENTER);
        helper.succeed();
    }

    @GameTest(template = TALL)
    public static void randomTicksGrowSaplingWithoutLight(GameTestHelper helper) {
        helper.setBlock(CENTER.below(), Blocks.END_STONE);
        helper.setBlock(CENTER, ModBlocks.ASTRAL_SAPLING.get());
        ServerLevel level = helper.getLevel();
        level.setDayTime(18000);
        BlockPos abs = helper.absolutePos(CENTER);
        helper.succeedWhen(() -> {
            for (int i = 0; i < 20 && level.getBlockState(abs).is(ModBlocks.ASTRAL_SAPLING.get()); i++) {
                level.getBlockState(abs).randomTick(level, abs, level.random);
            }
            helper.assertBlockPresent(ModBlocks.ASTRAL_LOG.get(), CENTER.above());
        });
    }

    @GameTest
    public static void saplingNeedsTwoBlocksOfClearance(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 2, 3);
        helper.setBlock(pos.below(), ModBlocks.END_TURF.get());
        helper.setBlock(pos, ModBlocks.ASTRAL_SAPLING.get());
        helper.setBlock(pos.above(2), Blocks.STONE);
        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(pos);
        helper.assertFalse(
                BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), level, abs),
                "bone meal should not apply without clearance");
        for (int i = 0; i < 200; i++) {
            level.getBlockState(abs).randomTick(level, abs, level.random);
        }
        helper.assertBlockPresent(ModBlocks.ASTRAL_SAPLING.get(), pos);
        helper.assertBlockProperty(pos, AstralSaplingBlock.STAGE, 0);
        helper.assertFalse(AstralSaplingBlock.hasClearance(level, abs), "blocked sapling reports clearance");
        helper.setBlock(pos.above(2), Blocks.AIR);
        helper.assertTrue(AstralSaplingBlock.hasClearance(level, abs), "open sapling reports no clearance");
        helper.succeed();
    }

    @GameTest(template = TALL)
    public static void configuredFeatureShape(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ConfiguredFeature<?, ?> tree = level.registryAccess()
                .registryOrThrow(Registries.CONFIGURED_FEATURE)
                .getOrThrow(ModFeatures.ASTRAL_TREE_CONFIGURED);
        int leaves = 0;
        int flowering = 0;
        for (int i = 0; i < 6; i++) {
            helper.setBlock(CENTER.below(), ModBlocks.END_TURF.get());
            helper.assertTrue(
                    tree.place(level, level.getChunkSource().getGenerator(), level.random, helper.absolutePos(CENTER)),
                    "astral_tree failed to place");
            int[] counts = assertTreeShape(helper, CENTER);
            leaves += counts[0];
            flowering += counts[1];
            clear(helper);
        }
        double share = (double) flowering / (leaves + flowering);
        helper.assertTrue(share >= 0.1 && share <= 0.3, "flowering leaf share " + share + " is not about 20%");

        // Needs End soil.
        helper.setBlock(CENTER.below(), Blocks.DIRT);
        helper.assertFalse(
                tree.place(level, level.getChunkSource().getGenerator(), level.random, helper.absolutePos(CENTER)),
                "astral_tree placed on dirt");
        helper.succeed();
    }

    /**
     * Asserts a straight trunk 8-16 tall at the origin, 2-4 branches (log ends other than the trunk top), leaves
     * that aren't decaying; returns {leaves, flowering}.
     */
    private static int[] assertTreeShape(GameTestHelper helper, BlockPos origin) {
        Block log = ModBlocks.ASTRAL_LOG.get();
        int height = 0;
        while (helper.getBlockState(origin.above(height)).is(log)) {
            height++;
        }
        helper.assertTrue(height >= 8 && height <= 16, "trunk height " + height + " is outside 8-16");
        int ends = 0;
        int leaves = 0;
        int flowering = 0;
        for (BlockPos pos : BlockPos.betweenClosed(0, origin.getY(), 0, 16, 23, 16)) {
            BlockState state = helper.getBlockState(pos);
            if (state.is(log)) {
                helper.assertTrue(pos.getY() < origin.getY() + height, "log above the trunk top at " + pos.immutable());
                int neighbors = 0;
                for (Direction direction : net.minecraft.core.Direction.values()) {
                    if (helper.getBlockState(pos.relative(direction)).is(log)) {
                        neighbors++;
                    }
                }
                if (neighbors == 1 && !pos.equals(origin)) {
                    ends++;
                }
            } else if (state.getBlock() instanceof LeavesBlock) {
                helper.assertTrue(
                        state.getValue(LeavesBlock.DISTANCE) < LeavesBlock.DECAY_DISTANCE,
                        "decaying leaves at " + pos.immutable());
                if (state.is(ModBlocks.FLOWERING_ASTRAL_LEAVES.get())) {
                    flowering++;
                } else {
                    helper.assertTrue(state.is(ModBlocks.ASTRAL_LEAVES.get()), "unexpected leaves " + state);
                    leaves++;
                }
            }
        }
        int branches = ends - 1;
        helper.assertTrue(branches >= 2 && branches <= 4, branches + " branches, expected 2-4");
        helper.assertTrue(leaves + flowering > 20, "only " + (leaves + flowering) + " leaves");
        return new int[] {leaves, flowering};
    }

    private static void clear(GameTestHelper helper) {
        for (BlockPos pos : BlockPos.betweenClosed(0, 1, 0, 16, 23, 16)) {
            if (!helper.getBlockState(pos).isAir()) {
                helper.getLevel()
                        .setBlock(helper.absolutePos(pos), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    private static BlockState naturalLeaves(Block leaves) {
        return leaves.defaultBlockState()
                .setValue(LeavesBlock.PERSISTENT, false)
                .setValue(LeavesBlock.DISTANCE, 1);
    }

    @GameTest
    public static void leavesDecayWhenDisconnected(GameTestHelper helper) {
        BlockPos log = new BlockPos(2, 2, 2);
        BlockPos leaves = log.east();
        BlockPos flowering = log.west();
        helper.setBlock(log, ModBlocks.ASTRAL_LOG.get());
        helper.setBlock(leaves, naturalLeaves(ModBlocks.ASTRAL_LEAVES.get()));
        helper.setBlock(flowering, naturalLeaves(ModBlocks.FLOWERING_ASTRAL_LEAVES.get()));
        // A second, still connected tree must keep its leaves.
        BlockPos keptLog = new BlockPos(6, 2, 6);
        helper.setBlock(keptLog, ModBlocks.ASTRAL_LOG.get());
        helper.setBlock(keptLog.above(), naturalLeaves(ModBlocks.ASTRAL_LEAVES.get()));
        helper.setBlock(log, Blocks.AIR);
        ServerLevel level = helper.getLevel();
        helper.succeedWhen(() -> {
            for (BlockPos pos : List.of(leaves, flowering, keptLog.above())) {
                BlockPos abs = helper.absolutePos(pos);
                level.getBlockState(abs).randomTick(level, abs, level.random);
            }
            helper.assertBlockNotPresent(ModBlocks.ASTRAL_LEAVES.get(), leaves);
            helper.assertBlockNotPresent(ModBlocks.FLOWERING_ASTRAL_LEAVES.get(), flowering);
            helper.assertBlockPresent(ModBlocks.ASTRAL_LEAVES.get(), keptLog.above());
        });
    }

    @GameTest
    public static void onlyAstralLogsKeepLeavesAlive(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos astral = new BlockPos(1, 2, 1);
        BlockPos oak = new BlockPos(5, 2, 5);
        helper.setBlock(astral, ModBlocks.STRIPPED_ASTRAL_WOOD.get());
        helper.setBlock(oak, Blocks.OAK_LOG);
        for (BlockPos log : List.of(astral, oak)) {
            // Default state: distance 7, not persistent; a tick recomputes the distance.
            helper.setBlock(log.above(), ModBlocks.ASTRAL_LEAVES.get());
            helper.setBlock(log.above(2), ModBlocks.ASTRAL_LEAVES.get());
            for (BlockPos pos : List.of(log.above(), log.above(2))) {
                BlockPos abs = helper.absolutePos(pos);
                level.getBlockState(abs).tick(level, abs, level.random);
            }
        }
        helper.assertBlockProperty(astral.above(), LeavesBlock.DISTANCE, 1);
        helper.assertBlockProperty(astral.above(2), LeavesBlock.DISTANCE, 2);
        helper.assertBlockProperty(oak.above(), LeavesBlock.DISTANCE, LeavesBlock.DECAY_DISTANCE);
        helper.assertBlockProperty(oak.above(2), LeavesBlock.DISTANCE, LeavesBlock.DECAY_DISTANCE);
        helper.succeed();
    }

    @GameTest
    public static void leavesDrops(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(new BlockPos(1, 1, 1));
        for (Block leaves : List.of(ModBlocks.ASTRAL_LEAVES.get(), ModBlocks.FLOWERING_ASTRAL_LEAVES.get())) {
            BlockState state = naturalLeaves(leaves);
            helper.assertTrue(
                    dropsOnly(Block.getDrops(state, level, abs, null, null, new ItemStack(Items.SHEARS)), leaves),
                    leaves + " should drop itself with shears");
            ItemStack silk = new ItemStack(Items.DIAMOND_HOE);
            silk.enchant(
                    level.registryAccess()
                            .registryOrThrow(Registries.ENCHANTMENT)
                            .getHolderOrThrow(Enchantments.SILK_TOUCH),
                    1);
            helper.assertTrue(
                    dropsOnly(Block.getDrops(state, level, abs, null, null, silk), leaves),
                    leaves + " should drop itself with Silk Touch");

            int rolls = 4000;
            int saplings = 0;
            int fruit = 0;
            int flowers = 0;
            for (int i = 0; i < rolls; i++) {
                for (ItemStack drop : Block.getDrops(state, level, abs, null, null, ItemStack.EMPTY)) {
                    helper.assertFalse(drop.is(leaves.asItem()), leaves + " dropped itself without shears");
                    if (drop.is(ModBlocks.ASTRAL_SAPLING.get().asItem())) {
                        saplings++;
                    } else if (drop.is(Items.CHORUS_FRUIT)) {
                        fruit++;
                    } else if (drop.is(Items.CHORUS_FLOWER)) {
                        flowers++;
                    }
                }
            }
            // Expected: 200 saplings (5%), 20 chorus fruit and 20 chorus flowers (0.5%).
            helper.assertTrue(saplings >= 140 && saplings <= 260, leaves + " dropped " + saplings + " saplings");
            helper.assertTrue(fruit >= 5 && fruit <= 45, leaves + " dropped " + fruit + " chorus fruit");
            helper.assertTrue(flowers >= 5 && flowers <= 45, leaves + " dropped " + flowers + " chorus flowers");
        }
        helper.succeed();
    }

    private static boolean dropsOnly(List<ItemStack> drops, Block block) {
        return drops.size() == 1
                && drops.get(0).is(block.asItem())
                && drops.get(0).getCount() == 1;
    }

    @GameTest
    public static void saplingGoesInFlowerPot(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, Blocks.FLOWER_POT);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModBlocks.ASTRAL_SAPLING.get()));
        helper.useBlock(pos, player);
        helper.assertBlockPresent(ModBlocks.POTTED_ASTRAL_SAPLING.get(), pos);
        helper.succeed();
    }

    @GameTest
    public static void leavesAreFlammableAndMineableWithHoe(GameTestHelper helper) {
        BlockState leaves = ModBlocks.ASTRAL_LEAVES.get().defaultBlockState();
        helper.assertTrue(leaves.is(BlockTags.LEAVES), "astral leaves not in #minecraft:leaves");
        helper.assertTrue(leaves.is(BlockTags.MINEABLE_WITH_HOE), "not mineable with a hoe");
        helper.assertTrue(leaves.ignitedByLava(), "astral leaves should be flammable");
        helper.succeed();
    }
}
