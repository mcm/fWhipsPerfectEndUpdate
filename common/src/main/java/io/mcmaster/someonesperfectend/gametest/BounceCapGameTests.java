package io.mcmaster.someonesperfectend.gametest;

import io.mcmaster.someonesperfectend.block.BounceCapBlock;
import io.mcmaster.someonesperfectend.block.ModBlocks;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class BounceCapGameTests {
    private static final String TALL = "someonesperfectend:gametest/empty_tall";
    /** The cap sits on end stone at (8, 2, 8), so its top face is at relative y = 3. */
    private static final BlockPos CAP = new BlockPos(8, 2, 8);

    private BounceCapGameTests() {}

    private static void placeCap(GameTestHelper helper) {
        helper.setBlock(CAP.below(), Blocks.END_STONE);
        helper.setBlock(CAP, ModBlocks.BOUNCE_CAP.get());
    }

    private static Vec3 capTop(GameTestHelper helper) {
        return Vec3.atBottomCenterOf(helper.absolutePos(CAP.above()));
    }

    private static Player playerOnCap(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 top = capTop(helper);
        player.moveTo(top.x, top.y, top.z, 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(player);
        return player;
    }

    @GameTest
    public static void placementRules(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        BlockState cap = ModBlocks.BOUNCE_CAP.get().defaultBlockState();
        for (Block soil : List.of(ModBlocks.END_TURF.get(), Blocks.END_STONE, ModBlocks.VOIDSOIL.get())) {
            helper.setBlock(pos.below(), soil);
            helper.assertTrue(
                    cap.canSurvive(helper.getLevel(), helper.absolutePos(pos)), "bounce cap should survive on " + soil);
        }
        for (Block soil :
                List.of(Blocks.STONE, Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.AIR, ModBlocks.BOUNCE_CAP.get())) {
            helper.setBlock(pos.below(), soil);
            helper.assertFalse(
                    cap.canSurvive(helper.getLevel(), helper.absolutePos(pos)),
                    "bounce cap should not survive on " + soil);
        }
        // It pops off when its soil goes away, and it never spreads.
        helper.setBlock(pos.below(), Blocks.END_STONE);
        helper.setBlock(pos, cap);
        helper.assertFalse(cap.isRandomlyTicking(), "bounce caps must not spread");
        helper.setBlock(pos.below(), Blocks.AIR);
        helper.assertBlockNotPresent(ModBlocks.BOUNCE_CAP.get(), pos);
        helper.succeed();
    }

    @GameTest
    public static void dropsItselfByHandOrAxe(GameTestHelper helper) {
        placeCap(helper);
        BlockState state = helper.getBlockState(CAP);
        for (ItemStack tool : List.of(ItemStack.EMPTY, new ItemStack(Items.IRON_AXE))) {
            List<ItemStack> drops = Block.getDrops(state, helper.getLevel(), helper.absolutePos(CAP), null, null, tool);
            helper.assertTrue(
                    drops.size() == 1
                            && drops.get(0).is(ModBlocks.BOUNCE_CAP.get().asItem()),
                    "bounce cap should drop itself with " + tool + ", got " + drops);
        }
        helper.assertTrue(new ItemStack(Items.IRON_AXE).isCorrectToolForDrops(state), "axes should mine bounce caps");
        helper.assertTrue(
                new ItemStack(Items.IRON_AXE).getDestroySpeed(state) > 1.0F, "axes should mine bounce caps faster");
        helper.succeed();
    }

    @GameTest(template = TALL)
    public static void standingGivesSlowFalling(GameTestHelper helper) {
        placeCap(helper);
        Pig pig = helper.spawn(EntityType.PIG, CAP.above());
        helper.succeedWhen(() -> {
            MobEffectInstance effect = pig.getEffect(MobEffects.SLOW_FALLING);
            helper.assertTrue(effect != null, "a pig standing on a bounce cap should get Slow Falling");
            helper.assertFalse(effect.isVisible(), "Slow Falling particles should be hidden");
            helper.assertTrue(
                    effect.getDuration() > BounceCapBlock.SLOW_FALLING_TICKS - 2,
                    "Slow Falling should be refreshed every tick, got " + effect.getDuration());
        });
    }

    @GameTest(template = TALL, timeoutTicks = 120)
    public static void jumpReachesApex(GameTestHelper helper) {
        placeCap(helper);
        Player player = playerOnCap(helper);
        double base = capTop(helper).y;
        double[] maxY = {Double.NEGATIVE_INFINITY};
        boolean[] jumped = {false};
        helper.onEachTick(() -> {
            if (!jumped[0] && player.onGround()) {
                player.jumpFromGround();
                jumped[0] = true;
            }
            if (jumped[0]) {
                maxY[0] = Math.max(maxY[0], player.getY());
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(jumped[0] && player.getDeltaMovement().y < 0.0, "still rising");
            double apex = maxY[0] - base;
            helper.assertTrue(
                    apex >= 11.0 && apex <= 13.0, "apex should be about 12 blocks above the cap, got " + apex);
            player.discard();
        });
    }

    @GameTest(template = TALL)
    public static void sneakingSuppressesBounce(GameTestHelper helper) {
        placeCap(helper);
        Player player = playerOnCap(helper);
        player.setShiftKeyDown(true);
        helper.succeedWhen(() -> {
            helper.assertTrue(player.onGround(), "not on the cap yet");
            player.jumpFromGround();
            double vy = player.getDeltaMovement().y;
            helper.assertTrue(Math.abs(vy - 0.42) < 0.01, "a sneaking jump should be a normal jump, got " + vy);
            player.discard();
        });
    }

    @GameTest(template = TALL)
    public static void fallOntoCapCausesNoDamage(GameTestHelper helper) {
        placeCap(helper);
        LivingEntity pig = helper.spawn(EntityType.PIG, CAP.above(21));
        helper.succeedWhen(() -> {
            helper.assertTrue(pig.onGround(), "still falling");
            helper.assertTrue(
                    pig.isAlive() && pig.getHealth() == pig.getMaxHealth(),
                    "falling 20 blocks onto a bounce cap should cause no damage, health " + pig.getHealth());
        });
    }

    @GameTest(template = TALL)
    public static void fallOntoEndStoneHurts(GameTestHelper helper) {
        helper.setBlock(CAP, Blocks.END_STONE);
        LivingEntity pig = helper.spawn(EntityType.PIG, CAP.above(21));
        helper.succeedWhen(() -> helper.assertTrue(
                !pig.isAlive() || pig.getHealth() < pig.getMaxHealth(),
                "control: a 20-block fall onto end stone should hurt"));
    }
}
