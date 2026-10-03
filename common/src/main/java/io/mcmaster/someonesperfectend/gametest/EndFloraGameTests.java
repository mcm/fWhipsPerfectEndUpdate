package io.mcmaster.someonesperfectend.gametest;

import io.mcmaster.someonesperfectend.block.ModBlocks;
import io.mcmaster.someonesperfectend.worldgen.HugeGlowcapFeature;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.PinkPetalsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class EndFloraGameTests {
    private EndFloraGameTests() {}

    private static boolean survivesOn(GameTestHelper helper, Block plant, Block soil) {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos.below(), soil);
        return plant.defaultBlockState().canSurvive(helper.getLevel(), helper.absolutePos(pos));
    }

    @GameTest
    public static void petalsPlacementRules(GameTestHelper helper) {
        Block petals = ModBlocks.VOID_PETALS.get();
        for (Block soil : List.of(
                ModBlocks.END_TURF.get(),
                ModBlocks.VOIDSOIL.get(),
                Blocks.END_STONE,
                Blocks.DIRT,
                Blocks.GRASS_BLOCK,
                Blocks.PODZOL,
                Blocks.ROOTED_DIRT,
                Blocks.FARMLAND)) {
            helper.assertTrue(survivesOn(helper, petals, soil), "void petals should survive on " + soil);
        }
        for (Block soil : List.of(Blocks.STONE, Blocks.SAND, Blocks.OBSIDIAN)) {
            helper.assertFalse(survivesOn(helper, petals, soil), "void petals should not survive on " + soil);
        }
        helper.succeed();
    }

    @GameTest
    public static void usingPetalsIncrementsCount(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos.below(), ModBlocks.END_TURF.get());
        helper.setBlock(pos, ModBlocks.VOID_PETALS.get());
        helper.assertBlockProperty(pos, PinkPetalsBlock.AMOUNT, 1);

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModBlocks.VOID_PETALS.get(), 8));
        BlockPos abs = helper.absolutePos(pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atBottomCenterOf(abs), Direction.UP, abs, false);
        for (int expected = 2; expected <= 4; expected++) {
            player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
            helper.assertBlockProperty(pos, PinkPetalsBlock.AMOUNT, expected);
        }
        helper.assertTrue(player.getMainHandItem().getCount() == 5, "each added petal uses one item");
        // A full block of petals can't take more.
        player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        helper.assertBlockProperty(pos, PinkPetalsBlock.AMOUNT, 4);
        helper.succeed();
    }

    @GameTest
    public static void boneMealAddsPetals(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos.below(), ModBlocks.END_TURF.get());
        helper.setBlock(pos, ModBlocks.VOID_PETALS.get());
        helper.assertTrue(
                BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), helper.getLevel(), helper.absolutePos(pos)),
                "bone meal was not applied");
        helper.assertBlockProperty(pos, PinkPetalsBlock.AMOUNT, 2);
        helper.succeed();
    }

    @GameTest
    public static void glowcapPlacementRules(GameTestHelper helper) {
        // Full light: vanilla mushrooms would pop off anything but mycelium/podzol here.
        helper.setBlock(new BlockPos(1, 4, 1), Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15));
        Block glowcap = ModBlocks.GLOWCAP.get();
        helper.runAfterDelay(2, () -> {
            for (Block soil : List.of(ModBlocks.END_TURF.get(), ModBlocks.VOIDSOIL.get(), Blocks.END_STONE)) {
                helper.assertTrue(survivesOn(helper, glowcap, soil), "glowcap should survive on " + soil);
            }
            for (Block soil : List.of(Blocks.STONE, Blocks.DIRT, Blocks.MYCELIUM)) {
                helper.assertFalse(survivesOn(helper, glowcap, soil), "glowcap should not survive on " + soil);
            }
            helper.succeed();
        });
    }

    @GameTest
    public static void glowcapsEmitLight(GameTestHelper helper) {
        BlockPos glowcap = new BlockPos(1, 2, 1);
        BlockPos cap = new BlockPos(6, 2, 6);
        BlockPos pot = new BlockPos(1, 2, 6);
        helper.setBlock(glowcap.below(), Blocks.END_STONE);
        helper.setBlock(glowcap, ModBlocks.GLOWCAP.get());
        helper.setBlock(cap, ModBlocks.GLOWCAP_BLOCK.get());
        helper.setBlock(pot, ModBlocks.POTTED_GLOWCAP.get());
        ServerLevel level = helper.getLevel();
        helper.succeedWhen(() -> {
            helper.assertTrue(
                    level.getBrightness(LightLayer.BLOCK, helper.absolutePos(glowcap)) == ModBlocks.GLOWCAP_LIGHT,
                    "glowcap block light is " + level.getBrightness(LightLayer.BLOCK, helper.absolutePos(glowcap)));
            helper.assertTrue(
                    level.getBrightness(LightLayer.BLOCK, helper.absolutePos(cap)) == ModBlocks.GLOWCAP_BLOCK_LIGHT,
                    "glowcap_block light is " + level.getBrightness(LightLayer.BLOCK, helper.absolutePos(cap)));
            helper.assertTrue(
                    level.getBrightness(LightLayer.BLOCK, helper.absolutePos(pot)) == ModBlocks.GLOWCAP_LIGHT,
                    "potted glowcap light is " + level.getBrightness(LightLayer.BLOCK, helper.absolutePos(pot)));
        });
    }

    @GameTest
    public static void boneMealGrowsHugeGlowcap(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 2, 3);
        helper.setBlock(pos.below(), ModBlocks.END_TURF.get());
        helper.setBlock(pos, ModBlocks.GLOWCAP.get());
        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(pos);
        for (int i = 0; i < 100 && !level.getBlockState(abs).is(ModBlocks.GLOWCAP_STEM.get()); i++) {
            BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), level, abs);
        }
        helper.assertBlockPresent(ModBlocks.GLOWCAP_STEM.get(), pos);
        int stem = 0;
        while (level.getBlockState(abs.above(stem)).is(ModBlocks.GLOWCAP_STEM.get())) {
            stem++;
        }
        helper.assertTrue(
                stem >= HugeGlowcapFeature.MIN_STEM_HEIGHT && stem <= HugeGlowcapFeature.MAX_STEM_HEIGHT,
                "stem height " + stem + " is outside 4-8");
        BlockState top = level.getBlockState(abs.above(stem));
        helper.assertTrue(top.is(ModBlocks.GLOWCAP_BLOCK.get()), "cap missing on top of the stem: " + top);
        helper.succeed();
    }

    @GameTest
    public static void glowcapGoesInFlowerPot(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, Blocks.FLOWER_POT);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModBlocks.GLOWCAP.get()));
        helper.useBlock(pos, player);
        helper.assertBlockPresent(ModBlocks.POTTED_GLOWCAP.get(), pos);
        helper.succeed();
    }

    /** Inserts the item into a fresh composter up to 50 times; true once the fill level goes up. */
    static boolean composts(GameTestHelper helper, Item item) {
        BlockPos pos = new BlockPos(1, 1, 1);
        BlockPos abs = helper.absolutePos(pos);
        for (int i = 0; i < 50; i++) {
            helper.setBlock(pos, Blocks.COMPOSTER);
            BlockState result = ComposterBlock.insertItem(
                    null, helper.getBlockState(pos), helper.getLevel(), new ItemStack(item), abs);
            if (result.getValue(ComposterBlock.LEVEL) > 0) {
                return true;
            }
        }
        return false;
    }

    @GameTest
    public static void plantsAreCompostable(GameTestHelper helper) {
        helper.assertTrue(composts(helper, ModBlocks.VOID_PETALS.get().asItem()), "void petals don't compost");
        helper.assertTrue(composts(helper, ModBlocks.GLOWCAP.get().asItem()), "glowcap doesn't compost");
        helper.assertFalse(composts(helper, Items.STONE), "control: stone composted");
        helper.succeed();
    }

    private static void assertCrafts(GameTestHelper helper, Item input, Item output) {
        ServerLevel level = helper.getLevel();
        ItemStack result = level.getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(1, 1, List.of(new ItemStack(input))), level)
                .map(recipe -> recipe.value()
                        .assemble(CraftingInput.of(1, 1, List.of(new ItemStack(input))), level.registryAccess()))
                .orElse(ItemStack.EMPTY);
        helper.assertTrue(result.is(output), input + " crafted into " + result + ", expected " + output);
    }

    @GameTest
    public static void plantsCraftIntoDye(GameTestHelper helper) {
        assertCrafts(helper, ModBlocks.VOID_PETALS.get().asItem(), Items.LIGHT_BLUE_DYE);
        assertCrafts(helper, ModBlocks.GLOWCAP.get().asItem(), Items.CYAN_DYE);
        helper.succeed();
    }
}
