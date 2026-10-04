package io.mcmaster.someonesperfectend.gametest;

import io.mcmaster.someonesperfectend.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

/** MOD-70 FR-6: map colors follow the swap of the Astral wood and Voidvine color schemes. */
public final class ColorSwapGameTests {
    private ColorSwapGameTests() {}

    @GameTest
    public static void astralWoodSetMapsGreen(GameTestHelper helper) {
        assertLog(helper, ModBlocks.ASTRAL_LOG.get(), MapColor.COLOR_GREEN, MapColor.TERRACOTTA_GREEN);
        assertLog(helper, ModBlocks.FLOWERING_ASTRAL_LOG.get(), MapColor.COLOR_GREEN, MapColor.TERRACOTTA_GREEN);
        assertLog(helper, ModBlocks.ASTRAL_WOOD.get(), MapColor.TERRACOTTA_GREEN, MapColor.TERRACOTTA_GREEN);
        assertLog(helper, ModBlocks.STRIPPED_ASTRAL_LOG.get(), MapColor.COLOR_GREEN, MapColor.COLOR_GREEN);
        assertLog(helper, ModBlocks.STRIPPED_ASTRAL_WOOD.get(), MapColor.COLOR_GREEN, MapColor.COLOR_GREEN);
        for (Block block : new Block[] {
            ModBlocks.ASTRAL_PLANKS.get(),
            ModBlocks.ASTRAL_STAIRS.get(),
            ModBlocks.ASTRAL_SLAB.get(),
            ModBlocks.ASTRAL_FENCE.get(),
            ModBlocks.ASTRAL_FENCE_GATE.get(),
            ModBlocks.ASTRAL_DOOR.get(),
            ModBlocks.ASTRAL_TRAPDOOR.get(),
            ModBlocks.ASTRAL_PRESSURE_PLATE.get(),
            ModBlocks.ASTRAL_SIGN.get(),
            ModBlocks.ASTRAL_WALL_SIGN.get()
        }) {
            assertColor(helper, block.defaultBlockState(), MapColor.COLOR_GREEN);
        }
        assertColor(helper, ModBlocks.ASTRAL_HANGING_SIGN.get().defaultBlockState(), MapColor.TERRACOTTA_GREEN);
        assertColor(helper, ModBlocks.ASTRAL_WALL_HANGING_SIGN.get().defaultBlockState(), MapColor.TERRACOTTA_GREEN);
        helper.succeed();
    }

    @GameTest
    public static void astralLeavesAndSaplingKeepTheirMapColors(GameTestHelper helper) {
        assertColor(helper, ModBlocks.ASTRAL_LEAVES.get().defaultBlockState(), MapColor.COLOR_PURPLE);
        assertColor(helper, ModBlocks.FLOWERING_ASTRAL_LEAVES.get().defaultBlockState(), MapColor.COLOR_PINK);
        assertColor(helper, ModBlocks.ASTRAL_SAPLING.get().defaultBlockState(), MapColor.COLOR_PURPLE);
        helper.succeed();
    }

    @GameTest
    public static void voidvineMapsPurple(GameTestHelper helper) {
        assertColor(helper, ModBlocks.VOIDVINE_HEAD.get().defaultBlockState(), MapColor.COLOR_PURPLE);
        assertColor(helper, ModBlocks.VOIDVINE_BODY.get().defaultBlockState(), MapColor.COLOR_PURPLE);
        assertLog(helper, ModBlocks.VOIDVINE_BLOCK.get(), MapColor.COLOR_LIGHT_BLUE, MapColor.COLOR_PURPLE);
        assertLog(
                helper, ModBlocks.STRIPPED_VOIDVINE_BLOCK.get(), MapColor.COLOR_LIGHT_BLUE, MapColor.COLOR_LIGHT_BLUE);
        for (Block block : new Block[] {
            ModBlocks.VOIDVINE_PLANKS.get(),
            ModBlocks.VOIDVINE_STAIRS.get(),
            ModBlocks.VOIDVINE_SLAB.get(),
            ModBlocks.VOIDVINE_FENCE.get(),
            ModBlocks.VOIDVINE_FENCE_GATE.get(),
            ModBlocks.VOIDVINE_DOOR.get(),
            ModBlocks.VOIDVINE_TRAPDOOR.get(),
            ModBlocks.VOIDVINE_PRESSURE_PLATE.get(),
            ModBlocks.VOIDVINE_SIGN.get(),
            ModBlocks.VOIDVINE_WALL_SIGN.get(),
            ModBlocks.VOIDVINE_HANGING_SIGN.get(),
            ModBlocks.VOIDVINE_WALL_HANGING_SIGN.get()
        }) {
            assertColor(helper, block.defaultBlockState(), MapColor.COLOR_LIGHT_BLUE);
        }
        helper.succeed();
    }

    private static void assertLog(GameTestHelper helper, Block block, MapColor top, MapColor side) {
        BlockState state = block.defaultBlockState();
        assertColor(helper, state.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y), top);
        assertColor(helper, state.setValue(RotatedPillarBlock.AXIS, Direction.Axis.X), side);
        assertColor(helper, state.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z), side);
    }

    private static void assertColor(GameTestHelper helper, BlockState state, MapColor expected) {
        MapColor actual = state.getMapColor(helper.getLevel(), BlockPos.ZERO);
        helper.assertTrue(actual == expected, state + " maps to color " + actual.id + ", expected " + expected.id);
    }
}
