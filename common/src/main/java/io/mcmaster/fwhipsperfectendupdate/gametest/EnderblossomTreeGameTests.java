package io.mcmaster.fwhipsperfectendupdate.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.mcmaster.fwhipsperfectendupdate.FwhipsPerfectEndUpdate;
import io.mcmaster.fwhipsperfectendupdate.block.AstralSaplingBlock;
import io.mcmaster.fwhipsperfectendupdate.block.EnderblossomBlock;
import io.mcmaster.fwhipsperfectendupdate.block.ModBlockTags;
import io.mcmaster.fwhipsperfectendupdate.block.ModBlocks;
import io.mcmaster.fwhipsperfectendupdate.worldgen.AstralTreeConfiguration;
import io.mcmaster.fwhipsperfectendupdate.worldgen.AstralTreeFeature;
import io.mcmaster.fwhipsperfectendupdate.worldgen.ModFeatures;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;

/** Enderblossoms on generated Astral trees, and from Flowering Astral Leaves. */
public final class EnderblossomTreeGameTests {
    /** A 17x24x17 air structure, tall and wide enough for a full Astral tree grown from its center. */
    private static final String TALL = FwhipsPerfectEndUpdate.MOD_ID + ":gametest/empty_tall";

    private static final BlockPos CENTER = new BlockPos(8, 2, 8);

    private EnderblossomTreeGameTests() {}

    /**
     * 20 sapling-grown trees (the {@code astral_tree} feature) and 20 End Forest trees, each with a fixed seed:
     * every tree has at most 4 blossoms, at least 18 of 20 have one, and every blossom is a closed one on a trunk
     * log side at least 2 blocks above the base, facing away from the trunk.
     */
    @GameTest(template = TALL, timeoutTicks = 200)
    public static void generatedTreesCarryEnderblossoms(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockState sapling = ModBlocks.ASTRAL_SAPLING.get().defaultBlockState();
        int withBlossoms = 0;
        for (int seed = 0; seed < 20; seed++) {
            helper.setBlock(CENTER.below(), ModBlocks.END_TURF.get());
            helper.setBlock(CENTER, sapling);
            helper.assertTrue(
                    AstralSaplingBlock.TREE_GROWER.growTree(
                            level,
                            level.getChunkSource().getGenerator(),
                            helper.absolutePos(CENTER),
                            sapling,
                            RandomSource.create(seed)),
                    "sapling seed " + seed + " did not grow");
            if (!assertBlossoms(helper, "sapling seed " + seed).isEmpty()) {
                withBlossoms++;
            }
            clear(helper, 1);
        }
        helper.assertTrue(withBlossoms >= 18, "only " + withBlossoms + " of 20 sapling trees have blossoms");

        // End Forest trees only place over island ground, so give them a floor.
        for (BlockPos pos : BlockPos.betweenClosed(0, 1, 0, 16, 1, 16)) {
            helper.setBlock(pos, Blocks.END_STONE);
        }
        ConfiguredFeature<?, ?> forestTree = configured(level, ModFeatures.END_FOREST_ASTRAL_TREE_CONFIGURED);
        withBlossoms = 0;
        for (int seed = 0; seed < 20; seed++) {
            helper.setBlock(CENTER.below(), ModBlocks.END_TURF.get());
            helper.assertTrue(
                    forestTree.place(
                            level,
                            level.getChunkSource().getGenerator(),
                            RandomSource.create(seed),
                            helper.absolutePos(CENTER)),
                    "end forest tree seed " + seed + " failed to place");
            if (!assertBlossoms(helper, "end forest seed " + seed).isEmpty()) {
                withBlossoms++;
            }
            clear(helper, 2);
        }
        helper.assertTrue(withBlossoms >= 18, "only " + withBlossoms + " of 20 End Forest trees have blossoms");
        helper.succeed();
    }

    /** Asserts the blossom rules for the tree at {@link #CENTER}; returns the blossom positions. */
    private static List<BlockPos> assertBlossoms(GameTestHelper helper, String tree) {
        List<BlockPos> blossoms = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(0, 1, 0, 16, 23, 16)) {
            BlockState state = helper.getBlockState(pos);
            if (!(state.getBlock() instanceof EnderblossomBlock)) {
                continue;
            }
            BlockPos support = EnderblossomBlock.supportPos(state, pos);
            helper.assertTrue(state.is(ModBlocks.CLOSED_ENDERBLOSSOM.get()), tree + ": open blossom " + state);
            helper.assertTrue(
                    helper.getBlockState(support).is(ModBlockTags.ASTRAL_LOGS),
                    tree + ": blossom at " + pos.immutable() + " is not on an Astral log");
            helper.assertTrue(
                    support.getX() == CENTER.getX() && support.getZ() == CENTER.getZ(),
                    tree + ": blossom at " + pos.immutable() + " is not on the trunk");
            helper.assertTrue(
                    pos.getY() >= CENTER.getY() + AstralTreeFeature.ENDERBLOSSOM_MIN_HEIGHT,
                    tree + ": blossom at " + pos.immutable() + " is too low");
            blossoms.add(pos.immutable());
        }
        helper.assertTrue(blossoms.size() <= 4, tree + " has " + blossoms.size() + " blossoms");
        return blossoms;
    }

    /**
     * Places a tree through a {@link WorldGenLevel} that drops the tick {@code onPlace} schedules (real worldgen
     * never calls it), so only the feature's own scheduling is left; an Enderman then opens a blossom.
     */
    @GameTest(template = TALL, batch = "enderblossom_enderman_worldgen", timeoutTicks = 100)
    public static void worldgenBlossomsOpenNearEndermen(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        WorldGenLevel withoutOnPlace = (WorldGenLevel) Proxy.newProxyInstance(
                WorldGenLevel.class.getClassLoader(), new Class<?>[] {WorldGenLevel.class}, (proxy, method, args) -> {
                    Object result;
                    try {
                        result = method.invoke(level, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                    // setBlock(pos, state, flags[, recursionLeft]); matched by type, since names differ per loader.
                    if (args != null
                            && args.length >= 3
                            && method.getReturnType() == boolean.class
                            && args[0] instanceof BlockPos pos
                            && args[1] instanceof BlockState state
                            && args[2] instanceof Integer
                            && state.getBlock() instanceof EnderblossomBlock) {
                        level.getBlockTicks().clearArea(BoundingBox.fromCorners(pos, pos));
                    }
                    return result;
                });
        AstralTreeConfiguration config = (AstralTreeConfiguration)
                configured(level, ModFeatures.ASTRAL_TREE_CONFIGURED).config();
        helper.setBlock(CENTER.below(), ModBlocks.END_TURF.get());
        helper.assertTrue(
                ModFeatures.ASTRAL_TREE
                        .get()
                        .place(
                                config,
                                withoutOnPlace,
                                level.getChunkSource().getGenerator(),
                                RandomSource.create(3),
                                helper.absolutePos(CENTER)),
                "tree failed to place");
        List<BlockPos> blossoms = assertBlossoms(helper, "worldgen tree");
        helper.assertFalse(blossoms.isEmpty(), "worldgen tree has no blossoms");
        for (BlockPos pos : blossoms) {
            helper.assertTrue(
                    level.getBlockTicks()
                            .hasScheduledTick(helper.absolutePos(pos), ModBlocks.CLOSED_ENDERBLOSSOM.get()),
                    "no check scheduled for the blossom at " + pos);
        }
        // Stand the Enderman in clear air a few blocks out from a blossom (inside a block it would get hurt and
        // teleport away).
        BlockPos blossom = null;
        BlockPos endermanPos = null;
        for (BlockPos candidate : blossoms) {
            for (int out = 2; out <= 5 && endermanPos == null; out++) {
                BlockPos pos =
                        candidate.relative(helper.getBlockState(candidate).getValue(EnderblossomBlock.FACING), out);
                if (helper.getBlockState(pos).isAir()
                        && helper.getBlockState(pos.above()).isAir()
                        && helper.getBlockState(pos.above(2)).isAir()) {
                    blossom = candidate;
                    endermanPos = pos;
                }
            }
        }
        helper.assertTrue(endermanPos != null, "no room for an Enderman next to " + blossoms);
        EnderMan enderman = helper.spawn(EntityType.ENDERMAN, Vec3.atBottomCenterOf(endermanPos));
        enderman.setNoAi(true);
        enderman.setNoGravity(true);
        BlockPos opened = blossom;
        helper.succeedWhen(() -> helper.assertBlockPresent(ModBlocks.OPEN_ENDERBLOSSOM.get(), opened));
    }

    @GameTest
    public static void enderblossomCountConfig(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (var key : List.of(ModFeatures.ASTRAL_TREE_CONFIGURED, ModFeatures.END_FOREST_ASTRAL_TREE_CONFIGURED)) {
            AstralTreeConfiguration config =
                    (AstralTreeConfiguration) configured(level, key).config();
            helper.assertTrue(
                    config.enderblossoms() instanceof UniformInt uniform
                            && uniform.getMinValue() == 1
                            && uniform.getMaxValue() == 4,
                    key.location() + " enderblossoms is " + config.enderblossoms());
        }
        // A configuration without the field still loads, with uniform 1-4.
        JsonObject json = JsonParser.parseString("""
                {
                  "trunk_provider": {"type": "minecraft:simple_state_provider",
                      "state": {"Name": "fwhipsperfectendupdate:astral_log", "Properties": {"axis": "y"}}},
                  "foliage_provider": {"type": "minecraft:simple_state_provider",
                      "state": {"Name": "fwhipsperfectendupdate:astral_leaves"}},
                  "height": 10,
                  "branch_count": 2
                }""").getAsJsonObject();
        AstralTreeConfiguration legacy =
                AstralTreeConfiguration.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        helper.assertTrue(
                legacy.enderblossoms() instanceof UniformInt uniform
                        && uniform.getMinValue() == 1
                        && uniform.getMaxValue() == 4,
                "default enderblossoms " + legacy.enderblossoms());
        json.addProperty("enderblossoms", 17);
        helper.assertTrue(
                AstralTreeConfiguration.CODEC.parse(JsonOps.INSTANCE, json).isError(),
                "enderblossoms 17 should be rejected");
        helper.succeed();
    }

    /** With no room on the trunk the feature places fewer blossoms, and none with a count of 0. */
    @GameTest(template = TALL)
    public static void fewerBlossomsWithoutRoom(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        AstralTreeConfiguration astral = (AstralTreeConfiguration)
                configured(level, ModFeatures.ASTRAL_TREE_CONFIGURED).config();
        // No branches, so the walls don't block the tree.
        AstralTreeConfiguration config = new AstralTreeConfiguration(
                astral.trunkProvider(),
                astral.foliageProvider(),
                astral.height(),
                ConstantInt.of(0),
                Optional.empty(),
                astral.floweringLogChance(),
                astral.enderblossoms());
        // Stone walls around the trunk leave only its east side at y 4 free.
        BlockPos free = CENTER.above(2).east();
        for (int y = CENTER.getY(); y <= 23; y++) {
            for (BlockPos pos : List.of(
                    new BlockPos(7, y, 8), new BlockPos(9, y, 8), new BlockPos(8, y, 7), new BlockPos(8, y, 9))) {
                if (!pos.equals(free)) {
                    helper.setBlock(pos, Blocks.STONE);
                }
            }
        }
        helper.setBlock(CENTER.below(), ModBlocks.END_TURF.get());
        helper.assertTrue(
                ModFeatures.ASTRAL_TREE
                        .get()
                        .place(
                                config.withEnderblossoms(UniformInt.of(4, 4)),
                                level,
                                level.getChunkSource().getGenerator(),
                                RandomSource.create(1),
                                helper.absolutePos(CENTER)),
                "tree failed to place");
        List<BlockPos> blossoms = assertBlossoms(helper, "walled tree");
        helper.assertTrue(blossoms.equals(List.of(free)), "blossoms " + blossoms + ", expected only " + free);
        clear(helper, 1);

        helper.setBlock(CENTER.below(), ModBlocks.END_TURF.get());
        helper.assertTrue(
                ModFeatures.ASTRAL_TREE
                        .get()
                        .place(
                                config.withEnderblossoms(UniformInt.of(0, 0)),
                                level,
                                level.getChunkSource().getGenerator(),
                                RandomSource.create(1),
                                helper.absolutePos(CENTER)),
                "tree failed to place");
        helper.assertTrue(assertBlossoms(helper, "tree with 0").isEmpty(), "blossoms with enderblossoms 0");
        helper.succeed();
    }

    /** 2,000 Flowering Astral Leaves broken by hand drop 20-30% Closed Enderblossoms; shears and Silk Touch none. */
    @GameTest
    public static void floweringLeavesDropEnderblossoms(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(new BlockPos(1, 1, 1));
        ItemStack silk = new ItemStack(Items.DIAMOND_HOE);
        silk.enchant(
                level.registryAccess()
                        .registryOrThrow(Registries.ENCHANTMENT)
                        .getHolderOrThrow(Enchantments.SILK_TOUCH),
                1);
        BlockState flowering = naturalLeaves(ModBlocks.FLOWERING_ASTRAL_LEAVES.get());
        int handBlossoms = blossomDrops(level, flowering, abs, ItemStack.EMPTY);
        helper.assertTrue(
                handBlossoms >= 400 && handBlossoms <= 600,
                handBlossoms + " of 2000 hand-broken leaves dropped a blossom, expected 20-30%");
        int shears = blossomDrops(level, flowering, abs, new ItemStack(Items.SHEARS));
        helper.assertTrue(shears == 0, shears + " blossoms dropped with shears");
        int silkTouch = blossomDrops(level, flowering, abs, silk);
        helper.assertTrue(silkTouch == 0, silkTouch + " blossoms dropped with Silk Touch");
        int plain = blossomDrops(level, naturalLeaves(ModBlocks.ASTRAL_LEAVES.get()), abs, ItemStack.EMPTY);
        helper.assertTrue(plain == 0, plain + " blossoms dropped from plain Astral Leaves");
        helper.succeed();
    }

    private static int blossomDrops(ServerLevel level, BlockState state, BlockPos pos, ItemStack tool) {
        int blossoms = 0;
        for (int i = 0; i < 2000; i++) {
            for (ItemStack drop : Block.getDrops(state, level, pos, null, null, tool)) {
                if (drop.is(ModBlocks.CLOSED_ENDERBLOSSOM.get().asItem())) {
                    blossoms += drop.getCount();
                }
            }
        }
        return blossoms;
    }

    private static BlockState naturalLeaves(Block leaves) {
        return leaves.defaultBlockState()
                .setValue(LeavesBlock.PERSISTENT, false)
                .setValue(LeavesBlock.DISTANCE, 1);
    }

    private static ConfiguredFeature<?, ?> configured(ServerLevel level, ResourceKey<ConfiguredFeature<?, ?>> key) {
        return level.registryAccess()
                .registryOrThrow(Registries.CONFIGURED_FEATURE)
                .getOrThrow(key);
    }

    /** Clears the test area from {@code minY} up, without block updates (so blossoms don't pop off). */
    private static void clear(GameTestHelper helper, int minY) {
        for (BlockPos pos : BlockPos.betweenClosed(0, minY, 0, 16, 23, 16)) {
            if (!helper.getBlockState(pos).isAir()) {
                helper.getLevel()
                        .setBlock(helper.absolutePos(pos), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }
}
