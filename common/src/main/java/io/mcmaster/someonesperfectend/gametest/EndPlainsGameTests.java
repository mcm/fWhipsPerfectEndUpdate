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
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.slf4j.Logger;

/** US-014: End Plains decoration (short void grass, void petals, bounce cap clusters, no chorus). */
public final class EndPlainsGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TALL = "someonesperfectend:gametest/empty_tall";
    /** Chorus plants spread less than 8 blocks from their stem, so this margin keeps neighbors' chorus out. */
    private static final int MARGIN = 8;
    private static final int CHUNKS = 8;

    private EndPlainsGameTests() {
    }

    /**
     * Generates outer-End chunks of the (fixed-seed) GameTest world that lie entirely inside {@code end_plains},
     * with an island at their center: together they contain bounce caps and short void grass, and no chorus.
     */
    @GameTest(timeoutTicks = 1200)
    public static void endPlainsChunksAreDecorated(GameTestHelper helper) {
        ServerLevel end = helper.getLevel().getServer().getLevel(Level.END);
        helper.assertTrue(end != null, "No End level");
        BiomeSource source = end.getChunkSource().getGenerator().getBiomeSource();
        Climate.Sampler sampler = end.getChunkSource().randomState().sampler();
        List<ChunkPos> found = new ArrayList<>();
        for (int cx = 70; cx < 400 && found.size() < CHUNKS; cx += 3) {
            for (int cz = -200; cz <= 200 && found.size() < CHUNKS; cz += 3) {
                ChunkPos chunk = new ChunkPos(cx, cz);
                if (allPlains(source, sampler, chunk) && end.getChunkSource().getGenerator().getBaseHeight(
                        chunk.getMiddleBlockX(), chunk.getMiddleBlockZ(), Heightmap.Types.WORLD_SURFACE_WG, end,
                        end.getChunkSource().randomState()) > end.getMinBuildHeight()) {
                    found.add(chunk);
                }
            }
        }
        helper.assertTrue(found.size() == CHUNKS, "Found only " + found.size() + " End Plains island chunks");

        int caps = 0;
        int grass = 0;
        int petals = 0;
        int chorus = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (ChunkPos chunk : found) {
            LevelChunk levelChunk = end.getChunk(chunk.x, chunk.z);
            for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
                for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
                    int top = levelChunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                    for (int y = end.getMinBuildHeight(); y <= top; y++) {
                        BlockState state = levelChunk.getBlockState(pos.set(x, y, z));
                        if (state.is(ModBlocks.BOUNCE_CAP.get())) {
                            caps++;
                            helper.assertTrue(levelChunk.getBlockState(pos.below()).is(ModBlockTags.END_PLANT_SOIL),
                                    "Bounce cap without soil at " + pos);
                        } else if (state.is(ModBlocks.SHORT_VOID_GRASS.get())) {
                            grass++;
                        } else if (state.is(ModBlocks.VOID_PETALS.get())) {
                            petals++;
                        } else if (state.is(Blocks.CHORUS_PLANT) || state.is(Blocks.CHORUS_FLOWER)) {
                            chorus++;
                        }
                    }
                }
            }
        }
        LOGGER.info("End Plains chunks {}: {} bounce caps, {} short void grass, {} void petals, {} chorus", found,
                caps, grass, petals, chorus);
        helper.assertTrue(caps > 0, "No bounce caps in End Plains chunks " + found);
        helper.assertTrue(grass > 0, "No short void grass in End Plains chunks " + found);
        helper.assertTrue(chorus == 0, chorus + " chorus blocks in End Plains chunks " + found);
        helper.succeed();
    }

    /** A bounce cap cluster placed on a floating 9x9 turf platform: 1-4 caps, all standing on the turf. */
    @GameTest(template = TALL)
    public static void bounceCapClusterSitsOnIsland(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockState turf = ModBlocks.END_TURF.get().defaultBlockState();
        for (int x = 4; x <= 12; x++) {
            for (int z = 4; z <= 12; z++) {
                helper.setBlock(new BlockPos(x, 10, z), turf);
            }
        }
        // The test area is capped with barriers right above the template; the feature finds surfaces by heightmap.
        for (int x = -2; x <= 18; x++) {
            for (int z = -2; z <= 18; z++) {
                for (int y = 24; y <= 26; y++) {
                    BlockPos ceiling = helper.absolutePos(new BlockPos(x, y, z));
                    if (level.getBlockState(ceiling).is(Blocks.BARRIER)) {
                        level.setBlock(ceiling, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
        BlockPos origin = helper.absolutePos(new BlockPos(8, 11, 8));
        boolean placed = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .getOrThrow(ModFeatures.BOUNCE_CAP_CLUSTER_CONFIGURED)
                .place(level, level.getChunkSource().getGenerator(), level.random, origin);
        helper.assertTrue(placed, "Cluster was not placed; surface at origin is y="
                + level.getHeight(Heightmap.Types.MOTION_BLOCKING, origin.getX(), origin.getZ()) + ", origin " + origin);
        int caps = 0;
        for (int x = 0; x <= 16; x++) {
            for (int y = 0; y < 24; y++) {
                for (int z = 0; z <= 16; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (helper.getBlockState(pos).is(ModBlocks.BOUNCE_CAP.get())) {
                        caps++;
                        helper.assertTrue(y == 11 && helper.getBlockState(pos.below()).is(ModBlocks.END_TURF.get()),
                                "Bounce cap off the island at " + pos);
                    }
                }
            }
        }
        helper.assertTrue(caps >= 1 && caps <= 4, caps + " caps in a cluster");
        helper.succeed();
    }

    /** Whether the biome is {@code end_plains} across the chunk and {@link #MARGIN} blocks around it. */
    private static boolean allPlains(BiomeSource source, Climate.Sampler sampler, ChunkPos chunk) {
        for (int x = chunk.getMinBlockX() - MARGIN; x <= chunk.getMaxBlockX() + MARGIN + 3; x += 4) {
            for (int z = chunk.getMinBlockZ() - MARGIN; z <= chunk.getMaxBlockZ() + MARGIN + 3; z += 4) {
                if (!source.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(64), QuartPos.fromBlock(z),
                        sampler).is(ModBiomes.END_PLAINS)) {
                    return false;
                }
            }
        }
        return true;
    }
}
