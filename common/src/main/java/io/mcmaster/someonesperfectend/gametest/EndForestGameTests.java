package io.mcmaster.someonesperfectend.gametest;

import com.mojang.logging.LogUtils;
import io.mcmaster.someonesperfectend.block.ModBlockTags;
import io.mcmaster.someonesperfectend.block.ModBlocks;
import io.mcmaster.someonesperfectend.worldgen.ModBiomes;
import io.mcmaster.someonesperfectend.worldgen.ModFeatures;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import org.slf4j.Logger;

/** US-015: End Forest decoration (Astral trees, short void grass, glowcaps) and the tree overhang limit. */
public final class EndForestGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TALL = "someonesperfectend:gametest/empty_tall";
    /** Trees reach at most 5 blocks from their trunk, so this margin keeps other biomes' trees out. */
    private static final int MARGIN = 8;
    private static final int CHUNKS = 8;
    private static final int MAX_OVERHANG = 3;
    /** How far below a tree block the test looks for the island it overhangs. */
    private static final int GROUND_SEARCH = 24;
    private static final int PLATFORM_Y = 8;
    /** Highest relative y a tree on the platform can reach (16-block trunk plus a leaf cluster). */
    private static final int TOP_Y = PLATFORM_Y + 20;

    private EndForestGameTests() {
    }

    /**
     * Generates outer-End chunks of the (fixed-seed) GameTest world that lie entirely inside {@code end_forest},
     * with an island at their center: together they contain astral logs, astral leaves, and short void grass, and
     * no tree block sticks out more than 3 blocks past the island edge.
     */
    @GameTest(timeoutTicks = 1200)
    public static void endForestChunksAreDecorated(GameTestHelper helper) {
        ServerLevel end = helper.getLevel().getServer().getLevel(Level.END);
        helper.assertTrue(end != null, "No End level");
        BiomeSource source = end.getChunkSource().getGenerator().getBiomeSource();
        Climate.Sampler sampler = end.getChunkSource().randomState().sampler();
        List<ChunkPos> found = new ArrayList<>();
        for (int cx = 70; cx < 400 && found.size() < CHUNKS; cx += 3) {
            for (int cz = -200; cz <= 200 && found.size() < CHUNKS; cz += 3) {
                ChunkPos chunk = new ChunkPos(cx, cz);
                if (allForest(source, sampler, chunk) && end.getChunkSource().getGenerator().getBaseHeight(
                        chunk.getMiddleBlockX(), chunk.getMiddleBlockZ(), Heightmap.Types.WORLD_SURFACE_WG, end,
                        end.getChunkSource().randomState()) > end.getMinBuildHeight()) {
                    found.add(chunk);
                }
            }
        }
        helper.assertTrue(found.size() == CHUNKS, "Found only " + found.size() + " End Forest island chunks");

        int logs = 0;
        int leaves = 0;
        int flowering = 0;
        int grass = 0;
        int glowcaps = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (ChunkPos chunk : found) {
            end.getChunk(chunk.x, chunk.z);
            for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
                for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
                    int top = end.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                    for (int y = end.getMinBuildHeight(); y <= top; y++) {
                        BlockState state = end.getBlockState(pos.set(x, y, z));
                        boolean tree = false;
                        if (state.is(ModBlockTags.ASTRAL_LOGS)) {
                            logs++;
                            tree = true;
                        } else if (state.is(ModBlocks.ASTRAL_LEAVES.get())) {
                            leaves++;
                            tree = true;
                        } else if (state.is(ModBlocks.FLOWERING_ASTRAL_LEAVES.get())) {
                            flowering++;
                            tree = true;
                        } else if (state.is(ModBlocks.SHORT_VOID_GRASS.get())) {
                            grass++;
                        } else if (state.is(ModBlocks.GLOWCAP.get())) {
                            glowcaps++;
                            helper.assertTrue(end.getBlockState(pos.below()).is(ModBlockTags.END_PLANT_SOIL),
                                    "Glowcap without soil at " + pos);
                        }
                        if (tree) {
                            helper.assertTrue(overIsland(end, pos),
                                    "Astral tree block more than " + MAX_OVERHANG + " blocks past the edge at " + pos);
                        }
                    }
                }
            }
        }
        LOGGER.info("End Forest chunks {}: {} astral logs, {} astral leaves, {} flowering leaves, {} short void grass,"
                + " {} glowcaps", found, logs, leaves, flowering, grass, glowcaps);
        helper.assertTrue(logs > 0, "No astral logs in End Forest chunks " + found);
        helper.assertTrue(leaves > 0, "No astral leaves in End Forest chunks " + found);
        helper.assertTrue(grass > 0, "No short void grass in End Forest chunks " + found);
        helper.succeed();
    }

    /**
     * {@code end_forest_astral_tree} placed repeatedly on a floating 3x3 turf platform: every tree that is placed
     * stays within 3 blocks of the platform, and trees that would reach further are refused. The platform floats
     * high enough that the test floor is out of the feature's ground search.
     */
    @GameTest(template = TALL, timeoutTicks = 200)
    public static void forestTreeRespectsOverhangLimit(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ConfiguredFeature<?, ?> feature = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .getOrThrow(ModFeatures.END_FOREST_ASTRAL_TREE_CONFIGURED);
        BlockState turf = ModBlocks.END_TURF.get().defaultBlockState();
        // The test area is capped with barriers right above the template; tall trees reach past it.
        for (BlockPos pos : BlockPos.betweenClosed(-2, 24, -2, 18, 26, 18)) {
            if (helper.getBlockState(pos).is(Blocks.BARRIER)) {
                helper.setBlock(pos, Blocks.AIR);
            }
        }
        int placed = 0;
        int refused = 0;
        for (int attempt = 0; attempt < 40; attempt++) {
            clear(helper);
            for (int x = 7; x <= 9; x++) {
                for (int z = 7; z <= 9; z++) {
                    helper.setBlock(new BlockPos(x, PLATFORM_Y, z), turf);
                }
            }
            if (!feature.place(level, level.getChunkSource().getGenerator(), level.random,
                    helper.absolutePos(new BlockPos(8, PLATFORM_Y + 1, 8)))) {
                refused++;
                continue;
            }
            placed++;
            for (BlockPos pos : BlockPos.betweenClosed(0, 0, 0, 16, TOP_Y, 16)) {
                BlockState state = helper.getBlockState(pos);
                if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)) {
                    int overhang = Math.max(Math.max(7 - pos.getX(), pos.getX() - 9),
                            Math.max(7 - pos.getZ(), pos.getZ() - 9));
                    helper.assertTrue(overhang <= MAX_OVERHANG, "Tree block " + overhang + " blocks past the edge at "
                            + pos);
                }
            }
        }
        clear(helper);
        LOGGER.info("Overhang-limited astral trees on a 3x3 platform: {} placed, {} refused", placed, refused);
        helper.assertTrue(refused > 0, "No tree was refused on a 3x3 platform");
        helper.assertTrue(placed > 0, "No tree fit on a 3x3 platform");

        // A wide island has room for any tree.
        for (int x = 0; x <= 16; x++) {
            for (int z = 0; z <= 16; z++) {
                helper.setBlock(new BlockPos(x, PLATFORM_Y, z), turf);
            }
        }
        for (int attempt = 0; attempt < 5; attempt++) {
            helper.assertTrue(feature.place(level, level.getChunkSource().getGenerator(), level.random,
                    helper.absolutePos(new BlockPos(8, PLATFORM_Y + 1, 8))), "Tree refused on a 17x17 island");
            clearAbove(helper, PLATFORM_Y + 1);
        }
        helper.succeed();
    }

    private static void clear(GameTestHelper helper) {
        clearAbove(helper, 0);
    }

    private static void clearAbove(GameTestHelper helper, int minY) {
        for (BlockPos pos : BlockPos.betweenClosed(0, minY, 0, 16, TOP_Y, 16)) {
            helper.getLevel().setBlock(helper.absolutePos(pos), Blocks.AIR.defaultBlockState(),
                    Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    /** Whether some column within {@link #MAX_OVERHANG} blocks has island ground below the given tree block. */
    private static boolean overIsland(ServerLevel level, BlockPos treeBlock) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -MAX_OVERHANG; dx <= MAX_OVERHANG; dx++) {
            for (int dz = -MAX_OVERHANG; dz <= MAX_OVERHANG; dz++) {
                for (int y = treeBlock.getY(); y >= treeBlock.getY() - GROUND_SEARCH; y--) {
                    BlockState state = level.getBlockState(pos.set(treeBlock.getX() + dx, y, treeBlock.getZ() + dz));
                    if (!state.canBeReplaced() && !state.is(BlockTags.LOGS) && !state.is(BlockTags.LEAVES)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Whether the biome is {@code end_forest} across the chunk and {@link #MARGIN} blocks around it. */
    private static boolean allForest(BiomeSource source, Climate.Sampler sampler, ChunkPos chunk) {
        for (int x = chunk.getMinBlockX() - MARGIN; x <= chunk.getMaxBlockX() + MARGIN + 3; x += 4) {
            for (int z = chunk.getMinBlockZ() - MARGIN; z <= chunk.getMaxBlockZ() + MARGIN + 3; z += 4) {
                if (!source.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(64), QuartPos.fromBlock(z),
                        sampler).is(ModBiomes.END_FOREST)) {
                    return false;
                }
            }
        }
        return true;
    }
}
