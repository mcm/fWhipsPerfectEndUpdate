package io.mcmaster.fwhipsperfectendupdate.gametest;

import com.mojang.logging.LogUtils;
import io.mcmaster.fwhipsperfectendupdate.block.ModBlocks;
import io.mcmaster.fwhipsperfectendupdate.fluid.ModFluids;
import io.mcmaster.fwhipsperfectendupdate.worldgen.ModBiomes;
import io.mcmaster.fwhipsperfectendupdate.worldgen.ModFeatures;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.material.FluidState;
import org.slf4j.Logger;

/** US-016: End Oasis decoration (contained Oasis Water pools, void petal carpets, glowcaps). */
public final class EndOasisGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TALL = "fwhipsperfectendupdate:gametest/empty_tall";
    /** Pools and their carpets reach at most 7 blocks from their origin; this keeps other biomes' features out. */
    private static final int MARGIN = 8;

    private static final int CHUNKS = 12;
    private static final Direction[] ENCLOSING = {
        Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };
    private static final int PLATFORM_Y = 8;

    private EndOasisGameTests() {}

    /**
     * Generates outer-End chunks of the (fixed-seed) GameTest world that lie entirely inside {@code end_oasis},
     * with an island at their center: together they contain Oasis Water source blocks, and no Oasis Water block
     * has air below it or beside it (so none can pour off the island's edge or underside).
     */
    @GameTest(timeoutTicks = 1200)
    public static void endOasisChunksHaveContainedPools(GameTestHelper helper) {
        ServerLevel end = helper.getLevel().getServer().getLevel(Level.END);
        helper.assertTrue(end != null, "No End level");
        BiomeSource source = end.getChunkSource().getGenerator().getBiomeSource();
        Climate.Sampler sampler = end.getChunkSource().randomState().sampler();
        List<ChunkPos> found = new ArrayList<>();
        for (int cx = 70; cx < 500 && found.size() < CHUNKS; cx += 3) {
            for (int cz = -250; cz <= 250 && found.size() < CHUNKS; cz += 3) {
                ChunkPos chunk = new ChunkPos(cx, cz);
                if (allOasis(source, sampler, chunk)
                        && end.getChunkSource()
                                        .getGenerator()
                                        .getBaseHeight(
                                                chunk.getMiddleBlockX(),
                                                chunk.getMiddleBlockZ(),
                                                Heightmap.Types.WORLD_SURFACE_WG,
                                                end,
                                                end.getChunkSource().randomState())
                                > end.getMinBuildHeight()) {
                    found.add(chunk);
                }
            }
        }
        helper.assertTrue(found.size() == CHUNKS, "Found only " + found.size() + " End Oasis island chunks");
        // Neighbors too, so pools that neighboring chunks place across the border are in place before scanning.
        for (ChunkPos chunk : found) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    end.getChunk(chunk.x + dx, chunk.z + dz);
                }
            }
        }

        int sources = 0;
        int flowing = 0;
        int petals = 0;
        int glowcaps = 0;
        int hugeGlowcaps = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (ChunkPos chunk : found) {
            for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
                for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
                    int top = end.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                    for (int y = end.getMinBuildHeight(); y <= top; y++) {
                        BlockState state = end.getBlockState(pos.set(x, y, z));
                        FluidState fluid = state.getFluidState();
                        if (fluid.getType().isSame(ModFluids.OASIS_WATER.get())) {
                            if (fluid.isSource()) {
                                sources++;
                            } else {
                                flowing++;
                            }
                            for (Direction direction : ENCLOSING) {
                                BlockPos neighbor = pos.relative(direction);
                                helper.assertTrue(
                                        !end.getBlockState(neighbor).isAir(),
                                        "Oasis Water at " + pos + " touches air at " + neighbor);
                            }
                        } else if (state.is(ModBlocks.VOID_PETALS.get())) {
                            petals++;
                        } else if (state.is(ModBlocks.GLOWCAP.get())) {
                            glowcaps++;
                        } else if (state.is(ModBlocks.GLOWCAP_STEM.get())
                                && !end.getBlockState(pos.below()).is(ModBlocks.GLOWCAP_STEM.get())) {
                            hugeGlowcaps++;
                        }
                    }
                }
            }
        }
        LOGGER.info(
                "End Oasis chunks {}: {} oasis water sources, {} flowing, {} void petals, {} glowcaps,"
                        + " {} huge glowcaps",
                found,
                sources,
                flowing,
                petals,
                glowcaps,
                hugeGlowcaps);
        helper.assertTrue(sources > 0, "No Oasis Water source in End Oasis chunks " + found);
        helper.assertTrue(flowing == 0, flowing + " flowing Oasis Water blocks in End Oasis chunks " + found);
        helper.assertTrue(petals > 0, "No void petals in End Oasis chunks " + found);
        helper.assertTrue(glowcaps > 0, "No glowcaps in End Oasis chunks " + found);
        helper.succeed();
    }

    /**
     * {@code oasis_pool} on a floating 3x3 turf platform is always refused (every pool would pour off the edge);
     * on a 17x17 island it is placed, fully enclosed, with void petals around it.
     */
    @GameTest(template = TALL, timeoutTicks = 200)
    public static void oasisPoolIsContained(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ConfiguredFeature<?, ?> feature = level.registryAccess()
                .registryOrThrow(Registries.CONFIGURED_FEATURE)
                .getOrThrow(ModFeatures.OASIS_POOL_CONFIGURED);
        BlockState turf = ModBlocks.END_TURF.get().defaultBlockState();
        BlockPos origin = helper.absolutePos(new BlockPos(8, PLATFORM_Y + 1, 8));

        for (int attempt = 0; attempt < 20; attempt++) {
            clear(helper);
            platform(helper, 7, 9, turf);
            helper.assertTrue(
                    !feature.place(level, level.getChunkSource().getGenerator(), level.random, origin),
                    "Pool placed on a 3x3 platform");
        }

        for (int attempt = 0; attempt < 10; attempt++) {
            clear(helper);
            platform(helper, 0, 16, turf);
            helper.assertTrue(
                    feature.place(level, level.getChunkSource().getGenerator(), level.random, origin),
                    "Pool refused on a 17x17 island");
            int water = 0;
            int petals = 0;
            for (BlockPos pos : BlockPos.betweenClosed(0, 0, 0, 16, PLATFORM_Y + 2, 16)) {
                BlockState state = helper.getBlockState(pos);
                if (state.getFluidState().getType().isSame(ModFluids.OASIS_WATER.get())) {
                    helper.assertTrue(state.getFluidState().isSource(), "Flowing pool water at " + pos);
                    helper.assertTrue(pos.getY() <= PLATFORM_Y, "Pool water above the surface at " + pos);
                    water++;
                    for (Direction direction : ENCLOSING) {
                        BlockState neighbor = helper.getBlockState(pos.relative(direction));
                        helper.assertTrue(
                                neighbor.isSolid()
                                        || neighbor.getFluidState().getType().isSame(ModFluids.OASIS_WATER.get()),
                                "Pool water at " + pos + " not enclosed");
                    }
                } else if (state.is(ModBlocks.VOID_PETALS.get())) {
                    petals++;
                }
            }
            helper.assertTrue(water > 0, "No pool water on a 17x17 island");
            helper.assertTrue(petals > 0, "No void petal carpet around the pool");
        }
        clear(helper);
        helper.succeed();
    }

    /** A 4-block-thick end stone slab topped with the given block, floating at {@link #PLATFORM_Y}. */
    private static void platform(GameTestHelper helper, int min, int max, BlockState top) {
        for (int x = min; x <= max; x++) {
            for (int z = min; z <= max; z++) {
                for (int y = PLATFORM_Y - 4; y < PLATFORM_Y; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.END_STONE);
                }
                helper.setBlock(new BlockPos(x, PLATFORM_Y, z), top);
            }
        }
    }

    private static void clear(GameTestHelper helper) {
        for (BlockPos pos : BlockPos.betweenClosed(0, 1, 0, 16, PLATFORM_Y + 4, 16)) {
            helper.getLevel()
                    .setBlock(
                            helper.absolutePos(pos),
                            Blocks.AIR.defaultBlockState(),
                            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    /** Whether the biome is {@code end_oasis} across the chunk and {@link #MARGIN} blocks around it. */
    private static boolean allOasis(BiomeSource source, Climate.Sampler sampler, ChunkPos chunk) {
        for (int x = chunk.getMinBlockX() - MARGIN; x <= chunk.getMaxBlockX() + MARGIN + 3; x += 4) {
            for (int z = chunk.getMinBlockZ() - MARGIN; z <= chunk.getMaxBlockZ() + MARGIN + 3; z += 4) {
                if (!source.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(64), QuartPos.fromBlock(z), sampler)
                        .is(ModBiomes.END_OASIS)) {
                    return false;
                }
            }
        }
        return true;
    }
}
