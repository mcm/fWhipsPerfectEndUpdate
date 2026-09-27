package io.mcmaster.someonesperfectend.gametest;

import io.mcmaster.someonesperfectend.block.ModBlockTags;
import io.mcmaster.someonesperfectend.block.ModBlocks;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class EndGroundGameTests {
    private EndGroundGameTests() {
    }

    @GameTest
    public static void turfSpreadsOntoEndStone(GameTestHelper helper) {
        BlockPos turf = new BlockPos(1, 1, 1);
        BlockPos stone = new BlockPos(2, 1, 1);
        helper.setBlock(turf, ModBlocks.END_TURF.get());
        helper.setBlock(stone, Blocks.END_STONE);
        // A light block keeps the test independent of the time of day.
        helper.setBlock(new BlockPos(1, 3, 1), Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15));
        ServerLevel level = helper.getLevel();
        BlockPos absTurf = helper.absolutePos(turf);
        helper.succeedWhen(() -> {
            for (int i = 0; i < 20; i++) {
                level.getBlockState(absTurf).randomTick(level, absTurf, level.random);
            }
            helper.assertBlockPresent(ModBlocks.END_TURF.get(), stone);
        });
    }

    @GameTest
    public static void turfDoesNotSpreadInDarkness(GameTestHelper helper) {
        BlockPos turf = new BlockPos(1, 1, 1);
        BlockPos stone = new BlockPos(2, 1, 1);
        helper.setBlock(turf, ModBlocks.END_TURF.get());
        helper.setBlock(stone, Blocks.END_STONE);
        // Roof the test in so no sky light reaches the turf.
        for (int x = 0; x <= 3; x++) {
            for (int z = 0; z <= 2; z++) {
                helper.setBlock(new BlockPos(x, 3, z), Blocks.STONE);
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
            helper.setBlock(new BlockPos(x, 1, 0), Blocks.STONE);
            helper.setBlock(new BlockPos(x, 2, 0), Blocks.STONE);
            helper.setBlock(new BlockPos(x, 1, 2), Blocks.STONE);
            helper.setBlock(new BlockPos(x, 2, 2), Blocks.STONE);
        }
        helper.setBlock(new BlockPos(0, 2, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(3, 1, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(3, 2, 1), Blocks.STONE);
        ServerLevel level = helper.getLevel();
        BlockPos absTurf = helper.absolutePos(turf);
        // Give the light engine time to darken the enclosed space, then tick.
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(level.getMaxLocalRawBrightness(absTurf.above()) < 9, "enclosed space is not dark");
            for (int i = 0; i < 400; i++) {
                level.getBlockState(absTurf).randomTick(level, absTurf, level.random);
            }
            helper.assertBlockPresent(Blocks.END_STONE, stone);
            helper.succeed();
        });
    }

    @GameTest
    public static void turfRevertsWhenCovered(GameTestHelper helper) {
        BlockPos turf = new BlockPos(1, 1, 1);
        helper.setBlock(turf, ModBlocks.END_TURF.get());
        helper.setBlock(turf.above(), Blocks.STONE);
        BlockPos absTurf = helper.absolutePos(turf);
        ServerLevel level = helper.getLevel();
        level.getBlockState(absTurf).randomTick(level, absTurf, level.random);
        helper.assertBlockPresent(Blocks.END_STONE, turf);
        helper.succeed();
    }

    @GameTest
    public static void turfStaysUnderTransparentBlock(GameTestHelper helper) {
        BlockPos turf = new BlockPos(1, 1, 1);
        helper.setBlock(turf, ModBlocks.END_TURF.get());
        helper.setBlock(turf.above(), Blocks.GLASS);
        BlockPos absTurf = helper.absolutePos(turf);
        ServerLevel level = helper.getLevel();
        level.getBlockState(absTurf).randomTick(level, absTurf, level.random);
        helper.assertBlockPresent(ModBlocks.END_TURF.get(), turf);
        helper.succeed();
    }

    @GameTest
    public static void turfDropsDependOnSilkTouch(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.END_TURF.get());
        BlockState state = helper.getBlockState(pos);
        BlockPos absPos = helper.absolutePos(pos);

        ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        helper.assertTrue(state.requiresCorrectToolForDrops() && pickaxe.isCorrectToolForDrops(state),
                "end_turf should need a pickaxe");
        assertDrops(helper, Block.getDrops(state, level, absPos, null, null, pickaxe), Items.END_STONE);

        ItemStack silkPickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        silkPickaxe.enchant(level.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                .getHolderOrThrow(Enchantments.SILK_TOUCH), 1);
        assertDrops(helper, Block.getDrops(state, level, absPos, null, null, silkPickaxe),
                ModBlocks.END_TURF.get().asItem());
        helper.succeed();
    }

    @GameTest
    public static void shortVoidGrassPopsOffWithoutSupport(GameTestHelper helper) {
        BlockPos support = new BlockPos(1, 1, 1);
        BlockPos grass = support.above();
        helper.setBlock(support, ModBlocks.END_TURF.get());
        helper.setBlock(grass, ModBlocks.SHORT_VOID_GRASS.get());
        helper.assertBlockPresent(ModBlocks.SHORT_VOID_GRASS.get(), grass);
        helper.setBlock(support, Blocks.AIR);
        helper.assertBlockPresent(Blocks.AIR, grass);
        helper.succeed();
    }

    @GameTest
    public static void shortVoidGrassNeedsEndSoil(GameTestHelper helper) {
        BlockState grass = ModBlocks.SHORT_VOID_GRASS.get().defaultBlockState();
        BlockPos below = new BlockPos(1, 1, 1);
        BlockPos absAbove = helper.absolutePos(below.above());
        helper.setBlock(below, ModBlocks.END_TURF.get());
        helper.assertTrue(grass.canSurvive(helper.getLevel(), absAbove), "should survive on end_turf");
        helper.setBlock(below, ModBlocks.VOIDSOIL.get());
        helper.assertTrue(grass.canSurvive(helper.getLevel(), absAbove), "should survive on voidsoil");
        for (Block other : List.of(Blocks.END_STONE, Blocks.GRASS_BLOCK, Blocks.DIRT)) {
            helper.setBlock(below, other);
            helper.assertFalse(grass.canSurvive(helper.getLevel(), absAbove), "should not survive on " + other);
        }
        helper.succeed();
    }

    @GameTest
    public static void shortVoidGrassDropsOnlyWithShears(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos.below(), ModBlocks.END_TURF.get());
        helper.setBlock(pos, ModBlocks.SHORT_VOID_GRASS.get());
        BlockState state = helper.getBlockState(pos);
        BlockPos absPos = helper.absolutePos(pos);
        assertDrops(helper, Block.getDrops(state, level, absPos, null, null, ItemStack.EMPTY));
        assertDrops(helper, Block.getDrops(state, level, absPos, null, null, new ItemStack(Items.SHEARS)),
                ModBlocks.SHORT_VOID_GRASS.get().asItem());
        helper.succeed();
    }

    @GameTest
    public static void boneMealOnTurfGrowsShortVoidGrass(GameTestHelper helper) {
        BlockPos turf = new BlockPos(3, 1, 3);
        for (int x = 1; x <= 5; x++) {
            for (int z = 1; z <= 5; z++) {
                helper.setBlock(new BlockPos(x, 1, z), ModBlocks.END_TURF.get());
            }
        }
        helper.assertTrue(BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), helper.getLevel(),
                helper.absolutePos(turf)), "bone meal was not used");
        helper.assertBlockPresent(ModBlocks.SHORT_VOID_GRASS.get(), turf.above());
        helper.succeed();
    }

    @GameTest
    public static void voidsoilSupportsHangingPlants(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 3, 1);
        helper.setBlock(pos, ModBlocks.VOIDSOIL.get());
        BlockState state = helper.getBlockState(pos);
        helper.assertTrue(state.is(ModBlockTags.SUPPORTS_HANGING_PLANTS), "voidsoil is not a hanging-plant support");
        helper.assertTrue(state.isFaceSturdy(helper.getLevel(), helper.absolutePos(pos), Direction.DOWN),
                "voidsoil's underside is not sturdy");
        helper.assertTrue(new ItemStack(Items.IRON_SHOVEL).getDestroySpeed(state) > 1.0F,
                "voidsoil should be mined with a shovel");
        helper.succeed();
    }

    private static void assertDrops(GameTestHelper helper, List<ItemStack> drops, net.minecraft.world.item.Item... expected) {
        List<net.minecraft.world.item.Item> actual = drops.stream().map(ItemStack::getItem).toList();
        helper.assertTrue(actual.equals(List.of(expected)), "expected drops " + List.of(expected) + ", got " + actual);
    }
}
