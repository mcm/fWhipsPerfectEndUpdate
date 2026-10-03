package io.mcmaster.someonesperfectend.gametest;

import io.mcmaster.someonesperfectend.SomeonesPerfectEnd;
import io.mcmaster.someonesperfectend.block.ModBlocks;
import io.mcmaster.someonesperfectend.block.Voidvines;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class VoidvineGameTests {
    private static final String TALL = SomeonesPerfectEnd.MOD_ID + ":gametest/empty_tall";

    private VoidvineGameTests() {}

    /** Uses a Voidvine stalk on the given face of the block at {@code target}. */
    private static void useStalk(GameTestHelper helper, BlockPos target, Direction face) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModBlocks.VOIDVINE.get()));
        BlockPos abs = helper.absolutePos(target);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs).relative(face, 0.5), face, abs, false);
        player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }

    /** Builds a vine of {@code length} segments hanging from Voidsoil at {@code soil}; returns the head. */
    private static BlockPos buildVine(GameTestHelper helper, BlockPos soil, int length) {
        helper.setBlock(soil, ModBlocks.VOIDSOIL.get());
        for (int i = 1; i < length; i++) {
            helper.setBlock(soil.below(i), ModBlocks.VOIDVINE_BODY.get());
        }
        BlockPos head = soil.below(length);
        helper.setBlock(head, ModBlocks.VOIDVINE_HEAD.get());
        return head;
    }

    private static int vineLength(GameTestHelper helper, BlockPos soil) {
        return Voidvines.lengthAbove(helper.getLevel(), helper.absolutePos(lowestSegment(helper, soil)));
    }

    private static BlockPos lowestSegment(GameTestHelper helper, BlockPos soil) {
        BlockPos pos = soil.below();
        while (pos.getY() > 0 && Voidvines.isVine(helper.getBlockState(pos.below()))) {
            pos = pos.below();
        }
        return pos;
    }

    @GameTest
    public static void stalkPlantsOnlyUnderVoidsoil(GameTestHelper helper) {
        BlockPos soil = new BlockPos(2, 4, 2);
        helper.setBlock(soil, ModBlocks.VOIDSOIL.get());
        useStalk(helper, soil, Direction.UP);
        helper.assertBlockNotPresent(ModBlocks.VOIDVINE_HEAD.get(), soil.above());
        useStalk(helper, soil, Direction.DOWN);
        helper.assertBlockPresent(ModBlocks.VOIDVINE_HEAD.get(), soil.below());
        // Stalks can't hang from other blocks either.
        BlockPos stone = new BlockPos(5, 4, 5);
        helper.setBlock(stone, Blocks.END_STONE);
        useStalk(helper, stone, Direction.DOWN);
        helper.assertBlockNotPresent(ModBlocks.VOIDVINE_HEAD.get(), stone.below());
        // A vine forced on top of Voidsoil can't survive there.
        helper.setBlock(soil.above(), ModBlocks.VOIDVINE_HEAD.get());
        helper.assertFalse(
                helper.getBlockState(soil.above()).canSurvive(helper.getLevel(), helper.absolutePos(soil.above())),
                "a voidvine on top of voidsoil can survive");
        // The planted one survives over time.
        helper.runAfterDelay(20, () -> {
            helper.assertBlockPresent(ModBlocks.VOIDVINE_HEAD.get(), soil.below());
            helper.succeed();
        });
    }

    @GameTest
    public static void stalkPlantedUnderVineExtendsIt(GameTestHelper helper) {
        BlockPos soil = new BlockPos(2, 5, 2);
        BlockPos head = buildVine(helper, soil, 1);
        useStalk(helper, head, Direction.DOWN);
        helper.assertBlockPresent(ModBlocks.VOIDVINE_BODY.get(), head);
        helper.assertBlockPresent(ModBlocks.VOIDVINE_HEAD.get(), head.below());
        helper.succeed();
    }

    @GameTest
    public static void boneMealExtendsVine(GameTestHelper helper) {
        BlockPos soil = new BlockPos(2, 7, 2);
        BlockPos head = buildVine(helper, soil, 1);
        helper.assertTrue(
                BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), helper.getLevel(), helper.absolutePos(head)),
                "bone meal was not applied");
        int length = vineLength(helper, soil);
        helper.assertTrue(length >= 2 && length <= 4, "bone meal should add 1-3 segments, vine is " + length);
        BlockPos lowest = lowestSegment(helper, soil);
        helper.assertBlockPresent(ModBlocks.VOIDVINE_HEAD.get(), lowest);
        helper.assertBlockPresent(ModBlocks.VOIDVINE_BODY.get(), soil.below());
        // Bone meal on a body segment grows the head too.
        helper.assertTrue(
                BoneMealItem.growCrop(
                        new ItemStack(Items.BONE_MEAL), helper.getLevel(), helper.absolutePos(soil.below())),
                "bone meal on the body was not applied");
        helper.assertTrue(vineLength(helper, soil) > length, "bone meal on the body did not grow the vine");
        helper.succeed();
    }

    @GameTest(template = TALL)
    public static void vineStopsAtSixteenSegments(GameTestHelper helper) {
        BlockPos soil = new BlockPos(8, 21, 8);
        BlockPos head = buildVine(helper, soil, 14);
        ServerLevel level = helper.getLevel();
        for (int i = 0; i < 20; i++) {
            BlockPos lowest = helper.absolutePos(lowestSegment(helper, soil));
            BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), level, lowest);
        }
        helper.assertTrue(
                vineLength(helper, soil) == Voidvines.MAX_LENGTH,
                "bone meal grew the vine to " + vineLength(helper, soil));
        BlockPos lowest = lowestSegment(helper, soil);
        helper.assertFalse(
                BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), level, helper.absolutePos(lowest)),
                "bone meal was used on a full-length vine");
        helper.assertFalse(
                BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), level, helper.absolutePos(head)),
                "bone meal was used on the body of a full-length vine");
        // Random ticks don't grow it any further either.
        BlockPos abs = helper.absolutePos(lowest);
        for (int i = 0; i < 500; i++) {
            level.getBlockState(abs).randomTick(level, abs, level.random);
        }
        helper.assertTrue(vineLength(helper, soil) == Voidvines.MAX_LENGTH, "random ticks grew a full vine");
        helper.succeed();
    }

    @GameTest(template = TALL)
    public static void randomTicksGrowVineDownward(GameTestHelper helper) {
        BlockPos soil = new BlockPos(8, 21, 8);
        buildVine(helper, soil, 1);
        ServerLevel level = helper.getLevel();
        int ticks = 0;
        while (vineLength(helper, soil) < 5 && ticks < 2000) {
            BlockPos abs = helper.absolutePos(lowestSegment(helper, soil));
            level.getBlockState(abs).randomTick(level, abs, level.random);
            ticks++;
        }
        // About 10% per random tick: 4 growths take about 40 ticks.
        helper.assertTrue(ticks >= 8 && ticks <= 200, "4 segments took " + ticks + " random ticks");
        helper.assertBlockPresent(ModBlocks.VOIDVINE_HEAD.get(), soil.below(5));
        helper.succeed();
    }

    @GameTest
    public static void breakingMiddleSegmentRemovesLowerOnes(GameTestHelper helper) {
        BlockPos soil = new BlockPos(2, 7, 2);
        BlockPos head = buildVine(helper, soil, 6);
        BlockPos fruiting = soil.below(5);
        helper.setBlock(fruiting, helper.getBlockState(fruiting).setValue(Voidvines.FRUIT, true));
        BlockPos middle = soil.below(3);
        helper.getLevel().destroyBlock(helper.absolutePos(middle), true);
        helper.succeedWhen(() -> {
            for (int i = 3; i <= 6; i++) {
                helper.assertBlockNotPresent(ModBlocks.VOIDVINE_BODY.get(), soil.below(i));
                helper.assertBlockNotPresent(ModBlocks.VOIDVINE_HEAD.get(), soil.below(i));
            }
            helper.assertBlockPresent(ModBlocks.VOIDVINE_BODY.get(), soil.below(1));
            helper.assertBlockPresent(ModBlocks.VOIDVINE_HEAD.get(), soil.below(2));
            List<ItemEntity> items = helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds());
            helper.assertTrue(
                    count(items, ModBlocks.VOIDVINE.get()) == 4,
                    "expected 4 stalks, got " + count(items, ModBlocks.VOIDVINE.get()));
            helper.assertTrue(
                    count(items, ModBlocks.VOIDFRUIT.get()) == 1,
                    "expected 1 voidfruit, got " + count(items, ModBlocks.VOIDFRUIT.get()));
        });
    }

    private static int count(List<ItemEntity> items, Item item) {
        return items.stream()
                .filter(e -> e.getItem().is(item))
                .mapToInt(e -> e.getItem().getCount())
                .sum();
    }

    @GameTest
    public static void fruitIsHarvestedAndRegrows(GameTestHelper helper) {
        BlockPos soil = new BlockPos(2, 5, 2);
        buildVine(helper, soil, 2);
        BlockPos body = soil.below();
        helper.setBlock(body, helper.getBlockState(body).setValue(Voidvines.FRUIT, true));
        ServerLevel level = helper.getLevel();
        helper.assertTrue(
                helper.getBlockState(body).getLightEmission() == Voidvines.FRUIT_LIGHT, "fruiting vine should glow");

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.useBlock(body, player);
        helper.assertBlockProperty(body, Voidvines.FRUIT, false);
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, helper.getBounds());
        helper.assertTrue(count(items, ModBlocks.VOIDFRUIT.get()) == 1, "harvesting did not drop one voidfruit");
        helper.assertBlockPresent(ModBlocks.VOIDVINE_BODY.get(), body);
        // Using a fruitless segment does nothing.
        helper.useBlock(body, player);
        items = level.getEntitiesOfClass(ItemEntity.class, helper.getBounds());
        helper.assertTrue(count(items, ModBlocks.VOIDFRUIT.get()) == 1, "a fruitless vine dropped fruit");

        // Both the body and the head regrow fruit on random ticks. A block under the head keeps it from growing.
        helper.setBlock(body.below(2), Blocks.STONE);
        for (BlockPos pos : List.of(body, body.below())) {
            BlockPos segment = helper.absolutePos(pos);
            for (int i = 0; i < 500 && !level.getBlockState(segment).getValue(Voidvines.FRUIT); i++) {
                level.getBlockState(segment).randomTick(level, segment, level.random);
            }
            helper.assertBlockProperty(pos, Voidvines.FRUIT, true);
        }
        helper.assertBlockPresent(ModBlocks.VOIDVINE_HEAD.get(), body.below());
        helper.succeed();
    }

    @GameTest
    public static void eatingVoidfruitFeedsWithoutTeleporting(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 start = helper.absoluteVec(new Vec3(2.5, 1, 2.5));
        player.moveTo(start.x, start.y, start.z, 0, 0);
        player.getFoodData().setFoodLevel(10);
        player.getFoodData().setSaturation(0);
        ItemStack fruit = new ItemStack(ModBlocks.VOIDFRUIT.get(), 2);
        ItemStack left = fruit.finishUsingItem(helper.getLevel(), player);
        helper.assertTrue(
                player.getFoodData().getFoodLevel() == 14,
                "voidfruit restored " + (player.getFoodData().getFoodLevel() - 10) + " hunger");
        helper.assertTrue(
                Math.abs(player.getFoodData().getSaturationLevel() - 4 * 0.6F * 2) < 1.0E-4F,
                "unexpected saturation " + player.getFoodData().getSaturationLevel());
        helper.assertTrue(left.getCount() == 1, "eating did not use one voidfruit");
        helper.assertTrue(player.position().equals(start), "eating voidfruit moved the player to " + player.position());
        helper.succeed();
    }

    @GameTest
    public static void voidfruitIsCompostable(GameTestHelper helper) {
        helper.assertTrue(EndFloraGameTests.composts(helper, ModBlocks.VOIDFRUIT.get()), "voidfruit doesn't compost");
        helper.succeed();
    }

    @GameTest
    public static void stalkItemPlantsTheVine(GameTestHelper helper) {
        helper.assertTrue(
                ModBlocks.VOIDVINE.get() instanceof BlockItem blockItem
                        && blockItem.getBlock() == ModBlocks.VOIDVINE_HEAD.get(),
                "the stalk doesn't place the vine");
        helper.assertTrue(
                ModBlocks.VOIDVINE_HEAD.get().asItem() == ModBlocks.VOIDVINE.get(), "the vine's item is not the stalk");
        helper.assertTrue(
                ModBlocks.VOIDVINE_BODY
                        .get()
                        .getCloneItemStack(
                                helper.getLevel(),
                                BlockPos.ZERO,
                                ModBlocks.VOIDVINE_BODY.get().defaultBlockState())
                        .is(ModBlocks.VOIDVINE.get()),
                "picking the body doesn't give a stalk");
        helper.succeed();
    }
}
