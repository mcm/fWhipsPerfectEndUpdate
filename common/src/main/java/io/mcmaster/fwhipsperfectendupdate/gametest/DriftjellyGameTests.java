package io.mcmaster.fwhipsperfectendupdate.gametest;

import io.mcmaster.fwhipsperfectendupdate.block.ModBlocks;
import io.mcmaster.fwhipsperfectendupdate.entity.Driftjellies;
import io.mcmaster.fwhipsperfectendupdate.entity.Driftjelly;
import io.mcmaster.fwhipsperfectendupdate.entity.DriftjellyDriftGoal;
import io.mcmaster.fwhipsperfectendupdate.worldgen.ModBiomes;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class DriftjellyGameTests {
    private static final String TALL = "fwhipsperfectendupdate:gametest/empty_tall";
    /** {@code empty_tall} is 17 blocks wide; its middle column. */
    private static final int MID = 8;

    private DriftjellyGameTests() {}

    private static Driftjelly spawn(GameTestHelper helper, double x, double y, double z) {
        return helper.spawn(Driftjellies.TYPE.get(), new Vec3(x, y, z));
    }

    @GameTest(template = TALL, timeoutTicks = 260)
    public static void hoversTwoToSixBlocksAboveGround(GameTestHelper helper) {
        for (int x = 0; x <= 16; x++) {
            for (int z = 0; z <= 16; z++) {
                helper.setBlock(x, 0, z, ModBlocks.END_TURF.get());
            }
        }
        Driftjelly jelly = spawn(helper, MID + 0.5, 5.0, MID + 0.5);
        jelly.restrictTo(helper.absolutePos(new BlockPos(MID, 1, MID)), 6);
        double groundY = helper.absolutePos(new BlockPos(0, 1, 0)).getY();
        double[] range = {Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        forTicks(
                helper,
                200,
                () -> {
                    helper.assertTrue(jelly.isAlive(), "the driftjelly died");
                    double height = jelly.getY() - groundY;
                    helper.assertTrue(
                            height >= Driftjelly.MIN_HOVER && height <= Driftjelly.MAX_HOVER,
                            "driftjelly should hover 2-6 blocks above ground, got " + height);
                    range[0] = Math.min(range[0], jelly.getY());
                    range[1] = Math.max(range[1], jelly.getY());
                },
                () -> {
                    helper.assertTrue(
                            range[1] - range[0] > 0.05,
                            "driftjelly should bob, its height range was " + (range[1] - range[0]));
                    helper.assertTrue(jelly.isNoGravity(), "driftjelly should float");
                    jelly.discard();
                });
    }

    /** Runs {@code check} every tick for {@code ticks} ticks, then {@code last} and succeeds. */
    private static void forTicks(GameTestHelper helper, int ticks, Runnable check, Runnable last) {
        int[] tick = {0};
        helper.onEachTick(() -> {
            if (tick[0] > ticks) {
                return;
            }
            check.run();
            if (++tick[0] > ticks) {
                last.run();
                helper.succeed();
            }
        });
    }

    /** Lays a 5x5 end stone island centered on the template's middle column at {@code islandY}. */
    private static void island(GameTestHelper helper, int islandY) {
        for (int x = MID - 2; x <= MID + 2; x++) {
            for (int z = MID - 2; z <= MID + 2; z++) {
                helper.setBlock(x, islandY, z, Blocks.END_STONE);
            }
        }
    }

    @GameTest(template = TALL, timeoutTicks = 260)
    public static void driftsOnlyALittlePastTheEdge(GameTestHelper helper) {
        // A 5x5 island high enough above the template floor that the driftjelly sees void around it.
        int islandY = 14;
        island(helper, islandY);
        Driftjelly jelly = spawn(helper, MID + 0.5, islandY + 4.0, MID + 0.5);
        Vec3 origin = helper.absoluteVec(Vec3.ZERO);
        boolean pastTheEdge = false;
        for (int i = 0; i < 200; i++) {
            Vec3 target = DriftjellyDriftGoal.pickTarget(jelly);
            if (target != null) {
                Vec3 rel = target.subtract(origin);
                helper.assertTrue(nearIsland(rel, 0.0), "wander target too far out over the void: " + rel);
                pastTheEdge |= !nearIsland(rel, -Driftjelly.MAX_VOID_DRIFT);
            }
        }
        helper.assertTrue(pastTheEdge, "the driftjelly should pick some wander targets past the island's edge");
        forTicks(
                helper,
                200,
                () -> {
                    Vec3 rel = jelly.position().subtract(origin);
                    // Allow a little overshoot past the leash.
                    helper.assertTrue(nearIsland(rel, 1.0), "driftjelly drifted too far out over the void: " + rel);
                },
                jelly::discard);
    }

    /** Within {@link Driftjelly#MAX_VOID_DRIFT} + {@code slack} blocks of the island on each horizontal axis. */
    private static boolean nearIsland(Vec3 rel, double slack) {
        double reach = Driftjelly.MAX_VOID_DRIFT + slack;
        return rel.x >= MID - 2 - reach
                && rel.x <= MID + 3 + reach
                && rel.z >= MID - 2 - reach
                && rel.z <= MID + 3 + reach;
    }

    @GameTest(template = TALL)
    public static void wandersAcrossANarrowGap(GameTestHelper helper) {
        // Two 5x5 islands three blocks apart, with void below the gap.
        int islandY = 14;
        for (int z = MID - 2; z <= MID + 2; z++) {
            for (int x = 2; x <= 6; x++) {
                helper.setBlock(x, islandY, z, Blocks.END_STONE);
                helper.setBlock(x + 8, islandY, z, Blocks.END_STONE);
            }
        }
        Driftjelly jelly = spawn(helper, 4.5, islandY + 4.0, MID + 0.5);
        Vec3 origin = helper.absoluteVec(Vec3.ZERO);
        boolean across = false;
        for (int i = 0; i < 500 && !across; i++) {
            Vec3 target = DriftjellyDriftGoal.pickTarget(jelly);
            across = target != null && target.x - origin.x >= 10.0;
        }
        helper.assertTrue(across, "the driftjelly should pick wander targets on the island across the gap");
        jelly.discard();
        helper.succeed();
    }

    @GameTest(template = TALL, timeoutTicks = 200)
    public static void driftsBackFromTheVoid(GameTestHelper helper) {
        int islandY = 14;
        island(helper, islandY);
        Driftjelly jelly = spawn(helper, MID + 0.5, islandY + 4.0, MID + 0.5);
        Vec3 origin = helper.absoluteVec(Vec3.ZERO);
        double edge = MID + 3 + Driftjelly.MAX_VOID_DRIFT;
        double[] maxX = {0.0};
        helper.onEachTick(() -> maxX[0] = Math.max(maxX[0], jelly.getX() - origin.x));
        helper.runAfterDelay(5, () -> jelly.setDeltaMovement(1.5, 0.0, 0.0));
        helper.runAfterDelay(
                20,
                () -> helper.succeedWhen(() -> {
                    helper.assertTrue(
                            maxX[0] > edge + 2, "the push should have sent the driftjelly far out over the void");
                    double x = jelly.getX() - origin.x;
                    helper.assertTrue(x <= edge, "driftjelly has not come back near ground, x = " + x);
                    jelly.discard();
                }));
    }

    @GameTest(template = ModGameTests.EMPTY_TEMPLATE)
    public static void killingDropsGel(GameTestHelper helper) {
        Driftjelly jelly = spawn(helper, 1.5, 2.0, 1.5);
        helper.assertTrue(jelly.getMaxHealth() == 8.0F, "driftjelly should have 8 HP, has " + jelly.getMaxHealth());
        jelly.kill();
        assertDrops(helper, new BlockPos(1, 2, 1), Driftjellies.GEL.get(), Driftjellies.COOKED_GEL.get());

        Driftjelly burning = spawn(helper, 5.5, 2.0, 5.5);
        burning.igniteForSeconds(5.0F);
        burning.kill();
        assertDrops(helper, new BlockPos(5, 2, 5), Driftjellies.COOKED_GEL.get(), Driftjellies.GEL.get());
        helper.succeed();
    }

    private static void assertDrops(GameTestHelper helper, BlockPos rel, Item expected, Item unexpected) {
        List<ItemEntity> items =
                helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(rel)).inflate(1.5));
        int count = 0;
        for (ItemEntity item : items) {
            helper.assertFalse(item.getItem().is(unexpected), "unexpected drop " + item.getItem());
            if (item.getItem().is(expected)) {
                count += item.getItem().getCount();
            }
            item.discard();
        }
        helper.assertTrue(count >= 1 && count <= 2, "expected 1-2 " + expected + ", got " + count);
    }

    @GameTest(template = ModGameTests.EMPTY_TEMPLATE)
    public static void gelItemsEggAndRecipes(GameTestHelper helper) {
        FoodProperties food = new ItemStack(Driftjellies.GEL.get()).get(DataComponents.FOOD);
        helper.assertTrue(food != null && food.nutrition() == 3, "driftjelly gel should restore 3 hunger");
        helper.assertTrue(
                Math.abs(food.saturation() - 3 * 0.4F * 2.0F) < 1.0E-4,
                "driftjelly gel should have a 0.4 saturation modifier, saturation is " + food.saturation());
        helper.assertTrue(
                new ItemStack(Driftjellies.COOKED_GEL.get()).has(DataComponents.FOOD),
                "cooked driftjelly gel should be food");

        SpawnEggItem egg = SpawnEggItem.byId(Driftjellies.TYPE.get());
        helper.assertTrue(egg == Driftjellies.SPAWN_EGG.get(), "no spawn egg registered for driftjelly: " + egg);
        helper.assertTrue(
                egg.getType(new ItemStack(egg)) == Driftjellies.TYPE.get(), "spawn egg spawns the wrong type");

        for (RecipeType<?> type : List.of(RecipeType.SMELTING, RecipeType.SMOKING, RecipeType.CAMPFIRE_COOKING)) {
            @SuppressWarnings("unchecked")
            RecipeType<AbstractCookingRecipe> cooking = (RecipeType<AbstractCookingRecipe>) type;
            ItemStack result = helper.getLevel()
                    .getRecipeManager()
                    .getRecipeFor(
                            cooking, new SingleRecipeInput(new ItemStack(Driftjellies.GEL.get())), helper.getLevel())
                    .map(holder ->
                            holder.value().getResultItem(helper.getLevel().registryAccess()))
                    .orElse(ItemStack.EMPTY);
            helper.assertTrue(result.is(Driftjellies.COOKED_GEL.get()), type + " of gel gives " + result);
        }
        helper.succeed();
    }

    @GameTest(template = ModGameTests.EMPTY_TEMPLATE)
    public static void spawnsNaturallyInTheNewBiomes(GameTestHelper helper) {
        EntityType<Driftjelly> type = Driftjellies.TYPE.get();
        helper.assertTrue(type.getCategory() == MobCategory.CREATURE, "driftjelly should be a passive creature");
        var biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        for (ResourceKey<Biome> key : ModBiomes.ALL) {
            List<MobSpawnSettings.SpawnerData> spawners = biomes.getOrThrow(key)
                    .getMobSettings()
                    .getMobs(MobCategory.CREATURE)
                    .unwrap();
            MobSpawnSettings.SpawnerData data =
                    spawners.stream().filter(s -> s.type == type).findFirst().orElse(null);
            helper.assertTrue(data != null, key.location() + " has no driftjelly spawner");
            helper.assertTrue(
                    data.minCount == 2 && data.maxCount == 4,
                    key.location() + " should spawn driftjellies in groups of 2-4");
        }

        helper.setBlock(1, 0, 1, ModBlocks.END_TURF.get());
        helper.setBlock(3, 0, 3, Blocks.STONE);
        BlockPos onTurf = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos onStone = helper.absolutePos(new BlockPos(3, 1, 3));
        helper.assertTrue(
                SpawnPlacements.isSpawnPositionOk(type, helper.getLevel(), onTurf)
                        && SpawnPlacements.checkSpawnRules(
                                type, helper.getLevel(), MobSpawnType.NATURAL, onTurf, helper.getLevel().random),
                "driftjellies should spawn on end turf");
        helper.assertFalse(
                SpawnPlacements.checkSpawnRules(
                        type, helper.getLevel(), MobSpawnType.NATURAL, onStone, helper.getLevel().random),
                "driftjellies should not spawn on stone");
        helper.succeed();
    }
}
