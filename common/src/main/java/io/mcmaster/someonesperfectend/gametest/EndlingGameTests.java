package io.mcmaster.someonesperfectend.gametest;

import io.mcmaster.someonesperfectend.entity.Endling;
import io.mcmaster.someonesperfectend.entity.Endlings;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** US-021: the Endling entity, its End lock ("Lost" state), and its village binding. */
public final class EndlingGameTests {
    private static final String TALL = "someonesperfectend:gametest/empty_tall";

    private EndlingGameTests() {}

    private static void floor(GameTestHelper helper, int size) {
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                helper.setBlock(x, 0, z, Blocks.END_STONE);
            }
        }
    }

    private static Endling spawn(GameTestHelper helper, double x, double z) {
        return helper.spawn(Endlings.TYPE.get(), new Vec3(x, 1.0, z));
    }

    @GameTest(template = ModGameTests.EMPTY_TEMPLATE)
    public static void basicProperties(GameTestHelper helper) {
        floor(helper, 8);
        Endling endling = spawn(helper, 3.5, 3.5);
        helper.assertTrue(endling.getMaxHealth() == 20.0F, "endling should have 20 HP, has " + endling.getMaxHealth());
        EntityDimensions size = Endlings.TYPE.get().getDimensions();
        helper.assertTrue(
                Math.abs(size.height() - 1.2F) < 0.01F && Math.abs(size.width() - 0.9F) < 0.01F,
                "endling should be 1.2 x 0.9 blocks, is " + size);
        helper.assertTrue(endling.isSensitiveToWater(), "water should hurt endlings");
        helper.assertFalse(endling.canPickUpLoot(), "endlings only take barter items (US-022)");
        helper.assertFalse(endling.removeWhenFarAway(1000.0), "endlings should not despawn");
        SpawnEggItem egg = SpawnEggItem.byId(Endlings.TYPE.get());
        helper.assertTrue(egg == Endlings.SPAWN_EGG.get(), "no spawn egg registered for endling: " + egg);
        endling.discard();
        helper.succeed();
    }

    @GameTest(template = ModGameTests.EMPTY_TEMPLATE, timeoutTicks = 100)
    public static void hitByPlayerDoesNotTeleportOrFightBack(GameTestHelper helper) {
        floor(helper, 8);
        Endling endling = spawn(helper, 3.5, 3.5);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.moveTo(helper.absoluteVec(new Vec3(1.5, 1.0, 3.5)));
        Vec3 start = endling.position();
        // Several hits so a 90% Enderman-style teleport chance would certainly have fired.
        for (int i = 0; i < 5; i++) {
            endling.invulnerableTime = 0;
            endling.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
        }
        endling.invulnerableTime = 0;
        endling.hurt(helper.getLevel().damageSources().magic(), 1.0F);
        helper.assertTrue(endling.getHealth() == 14.0F, "hits should hurt the endling, health " + endling.getHealth());
        helper.runAfterDelay(20, () -> {
            double moved = endling.position().distanceTo(start);
            // Panicking for 20 ticks covers ~5 blocks; an Enderman teleport goes up to 32.
            helper.assertTrue(
                    moved < 8.0, "endling moved " + moved + " blocks after being hit; it should not teleport");
            helper.assertTrue(endling.getTarget() == null, "endling should never target the player");
            endling.discard();
            helper.succeed();
        });
    }

    @GameTest(template = ModGameTests.EMPTY_TEMPLATE, timeoutTicks = 100)
    public static void waterHurts(GameTestHelper helper) {
        floor(helper, 8);
        helper.setBlock(3, 1, 3, Blocks.WATER);
        Endling endling = spawn(helper, 3.5, 3.5);
        helper.succeedWhen(() -> {
            helper.assertTrue(endling.getHealth() < endling.getMaxHealth(), "water should hurt the endling");
            endling.discard();
        });
    }

    @GameTest(template = ModGameTests.EMPTY_TEMPLATE, timeoutTicks = 400)
    public static void becomesLostOutsideTheEnd(GameTestHelper helper) {
        floor(helper, 8);
        helper.assertTrue(helper.getLevel().dimension() != Level.END, "this test must run outside the End");
        Endling endling = spawn(helper, 3.5, 3.5);
        endling.setVillage(helper.absolutePos(new BlockPos(3, 1, 3)));
        int[] tick = {0};
        int[] lostAt = {-1};
        helper.onEachTick(() -> {
            tick[0]++;
            if (lostAt[0] < 0 && endling.isLost()) {
                lostAt[0] = tick[0];
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(lostAt[0] > 0, "endling not Lost yet after " + tick[0] + " ticks");
            helper.assertTrue(
                    lostAt[0] >= Endling.LOST_DELAY - 5 && lostAt[0] <= Endling.LOST_DELAY + 5,
                    "endling should become Lost after 300 ticks, became Lost at " + lostAt[0]);
            helper.assertTrue(
                    endling.village() == null && !endling.hasRestriction(), "a Lost endling should leave its village");
            helper.assertFalse(endling.countsTowardVillage(), "a Lost endling should not count toward a village");
            endling.discard();
        });
    }

    @GameTest(template = ModGameTests.EMPTY_TEMPLATE)
    public static void lostIsPermanentAndSaved(GameTestHelper helper) {
        Endling endling = spawn(helper, 3.5, 3.5);
        // The timer needs continuous time outside the End.
        for (int i = 0; i < Endling.LOST_DELAY - 1; i++) {
            endling.updateLostState(false);
        }
        endling.updateLostState(true);
        for (int i = 0; i < Endling.LOST_DELAY - 1; i++) {
            endling.updateLostState(false);
        }
        helper.assertFalse(endling.isLost(), "time in the End should reset the Lost timer");
        endling.updateLostState(false);
        helper.assertTrue(endling.isLost(), "endling should be Lost after 300 continuous ticks outside the End");

        for (int i = 0; i < 100; i++) {
            endling.updateLostState(true);
        }
        helper.assertTrue(endling.isLost(), "returning to the End should not undo Lost");
        endling.setVillage(BlockPos.ZERO);
        helper.assertTrue(endling.village() == null, "a Lost endling can't join a village");
        helper.assertFalse(endling.canBarter(), "a Lost endling can't barter");
        helper.assertFalse(endling.canPickUpLoot(), "a Lost endling never picks up items");

        CompoundTag tag = endling.saveWithoutId(new CompoundTag());
        Endling loaded = Endlings.TYPE.get().create(helper.getLevel());
        loaded.load(tag);
        helper.assertTrue(loaded.isLost(), "Lost should persist in NBT");

        Endling villager = spawn(helper, 5.5, 5.5);
        villager.setVillage(new BlockPos(10, 64, 10), 7);
        Endling reloaded = Endlings.TYPE.get().create(helper.getLevel());
        reloaded.load(villager.saveWithoutId(new CompoundTag()));
        helper.assertTrue(
                new BlockPos(10, 64, 10).equals(reloaded.village()) && reloaded.getRestrictRadius() == 7.0F,
                "the village binding should persist in NBT");
        helper.assertTrue(reloaded.countsTowardVillage(), "a village endling counts toward its village");
        endling.discard();
        villager.discard();
        helper.succeed();
    }

    @GameTest(template = ModGameTests.EMPTY_TEMPLATE)
    public static void lostDropsNothing(GameTestHelper helper) {
        floor(helper, 8);
        Endling endling = spawn(helper, 3.5, 3.5);
        endling.setLost();
        endling.kill();
        List<ItemEntity> items = helper.getLevel()
                .getEntitiesOfClass(
                        ItemEntity.class, new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(8, 8, 8));
        helper.assertTrue(items.isEmpty(), "a Lost endling should drop nothing, dropped " + items);
        helper.succeed();
    }

    @GameTest(template = TALL, timeoutTicks = 300)
    public static void lostEndlingFleesPlayers(GameTestHelper helper) {
        floor(helper, 17);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.moveTo(helper.absoluteVec(new Vec3(8.5, 1.0, 8.5)));
        helper.getLevel().addFreshEntity(player);
        Endling endling = spawn(helper, 10.5, 8.5);
        endling.setLost();
        helper.succeedWhen(() -> {
            double distance = endling.distanceTo(player);
            helper.assertTrue(
                    distance > Endling.LOST_FLEE_DISTANCE,
                    "a Lost endling should keep more than 6 blocks from players, is " + distance);
            helper.assertTrue(endling.getTarget() == null, "a Lost endling is still non-hostile");
            endling.discard();
            player.discard();
        });
    }

    @GameTest(template = TALL, timeoutTicks = 400)
    public static void returnsToItsVillage(GameTestHelper helper) {
        floor(helper, 17);
        Endling endling = spawn(helper, 14.5, 14.5);
        BlockPos village = helper.absolutePos(new BlockPos(2, 1, 2));
        endling.setVillage(village, 4);
        helper.succeedWhen(() -> {
            double distance = Math.sqrt(endling.blockPosition().distSqr(village));
            helper.assertTrue(distance <= 5.0, "endling should walk back to its village, is " + distance + " away");
            endling.discard();
        });
    }
}
