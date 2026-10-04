package io.mcmaster.fwhipsperfectendupdate.gametest;

import com.mojang.logging.LogUtils;
import io.mcmaster.fwhipsperfectendupdate.block.ModBlockTags;
import io.mcmaster.fwhipsperfectendupdate.block.ModBlocks;
import io.mcmaster.fwhipsperfectendupdate.block.Voidvines;
import io.mcmaster.fwhipsperfectendupdate.worldgen.ModBiomes;
import io.mcmaster.fwhipsperfectendupdate.worldgen.ModFeatures;
import io.mcmaster.fwhipsperfectendupdate.worldgen.UnderIslandFoliageConfiguration;
import io.mcmaster.fwhipsperfectendupdate.worldgen.UnderIslandFoliageFeature;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.slf4j.Logger;

/** US-017: under-island foliage (Voidsoil patches on island undersides with hanging Voidvines). */
public final class UnderIslandFoliageGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TALL = "fwhipsperfectendupdate:gametest/empty_tall";
    private static final int CHUNKS = 12;
    /** Absolute Y below which generated vines never reach. */
    private static final int MIN_VINE_Y = 5;

    private static final int PLATFORM_Y = 14;

    private UnderIslandFoliageGameTests() {}

    /**
     * Generates outer-End island chunks of the (fixed-seed) GameTest world inside {@code end_forest} or
     * {@code end_oasis}: at least one Voidvine hangs from Voidsoil that has an island block above it, and every
     * vine hangs from Voidsoil, is 16 segments or shorter, and stays at or above Y=5.
     */
    @GameTest(timeoutTicks = 1200)
    public static void islandUndersidesHaveHangingVines(GameTestHelper helper) {
        ServerLevel end = helper.getLevel().getServer().getLevel(Level.END);
        helper.assertTrue(end != null, "No End level");
        BiomeSource source = end.getChunkSource().getGenerator().getBiomeSource();
        Climate.Sampler sampler = end.getChunkSource().randomState().sampler();
        List<ChunkPos> found = new ArrayList<>();
        for (int cx = 70; cx < 500 && found.size() < CHUNKS; cx += 3) {
            for (int cz = -250; cz <= 250 && found.size() < CHUNKS; cz += 3) {
                ChunkPos chunk = new ChunkPos(cx, cz);
                if (allForestOrOasis(source, sampler, chunk)
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
        helper.assertTrue(found.size() == CHUNKS, "Found only " + found.size() + " End Forest/Oasis island chunks");
        // Neighbors too, so patches that neighboring chunks place across the border are in place before scanning.
        for (ChunkPos chunk : found) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    end.getChunk(chunk.x + dx, chunk.z + dz);
                }
            }
        }

        int vines = 0;
        int segments = 0;
        int fruit = 0;
        int soil = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (ChunkPos chunk : found) {
            for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
                for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
                    int top = end.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                    for (int y = end.getMinBuildHeight(); y <= top; y++) {
                        BlockState state = end.getBlockState(pos.set(x, y, z));
                        if (state.is(ModBlocks.VOIDSOIL.get())
                                && end.getBlockState(pos.below()).isAir()) {
                            soil++;
                        }
                        if (!Voidvines.isVine(state)) {
                            continue;
                        }
                        segments++;
                        if (state.getValue(Voidvines.FRUIT)) {
                            fruit++;
                        }
                        helper.assertTrue(y >= MIN_VINE_Y, "Voidvine below Y=" + MIN_VINE_Y + " at " + pos);
                        BlockState above = end.getBlockState(pos.above());
                        if (Voidvines.isVine(above)) {
                            continue;
                        }
                        vines++;
                        helper.assertTrue(
                                above.is(ModBlocks.VOIDSOIL.get()), "Voidvine top without voidsoil at " + pos);
                        helper.assertTrue(
                                end.getBlockState(pos.above(2)).is(ModBlockTags.END_PLANT_SOIL),
                                "Voidsoil above the vine at " + pos + " has no island block above it");
                        int length = 1;
                        while (Voidvines.isVine(end.getBlockState(pos.below(length)))) {
                            length++;
                        }
                        helper.assertTrue(
                                length >= 4 && length <= Voidvines.MAX_LENGTH,
                                "Voidvine at " + pos + " has " + length + " segments");
                    }
                }
            }
        }
        LOGGER.info(
                "End Forest/Oasis chunks {}: {} underside voidsoil, {} voidvines, {} segments, {} with fruit",
                found,
                soil,
                vines,
                segments,
                fruit);
        helper.assertTrue(vines > 0, "No voidvine hanging from voidsoil in chunks " + found);
        helper.succeed();
    }

    /**
     * The feature is in {@code end_forest} and {@code end_oasis} (their JSON) and in vanilla {@code end_highlands}
     * (biome modification, region-gated to about 10%), but not in {@code end_plains} or other vanilla End biomes.
     */
    @GameTest(timeoutTicks = 100)
    public static void foliageIsPlacedInTheRightBiomes(GameTestHelper helper) {
        Registry<Biome> biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        ResourceKey<PlacedFeature> ours =
                ResourceKey.create(Registries.PLACED_FEATURE, ModFeatures.UNDER_ISLAND_FOLIAGE_CONFIGURED.location());
        helper.assertTrue(hasFeature(biomes, ModBiomes.END_FOREST, ours), "end_forest lacks under_island_foliage");
        helper.assertTrue(hasFeature(biomes, ModBiomes.END_OASIS, ours), "end_oasis lacks under_island_foliage");
        helper.assertTrue(!hasFeature(biomes, ModBiomes.END_PLAINS, ours), "end_plains has under_island_foliage");
        helper.assertTrue(
                hasFeature(biomes, Biomes.END_HIGHLANDS, ModFeatures.END_HIGHLANDS_UNDER_ISLAND_FOLIAGE),
                "end_highlands lacks the under-island foliage feature");
        for (ResourceKey<Biome> other :
                List.of(Biomes.END_MIDLANDS, Biomes.END_BARRENS, Biomes.SMALL_END_ISLANDS, Biomes.THE_END)) {
            helper.assertTrue(
                    !hasFeature(biomes, other, ModFeatures.END_HIGHLANDS_UNDER_ISLAND_FOLIAGE)
                            && !hasFeature(biomes, other, ours),
                    other.location() + " has under-island foliage");
        }

        ConfiguredFeature<?, ?> highlands = helper.getLevel()
                .registryAccess()
                .registryOrThrow(Registries.CONFIGURED_FEATURE)
                .getOrThrow(ResourceKey.create(
                        Registries.CONFIGURED_FEATURE, ModFeatures.END_HIGHLANDS_UNDER_ISLAND_FOLIAGE.location()));
        UnderIslandFoliageConfiguration config = (UnderIslandFoliageConfiguration) highlands.config();
        long seed =
                helper.getLevel().getServer().getWorldData().worldGenOptions().seed();
        int selected = 0;
        int regions = 0;
        for (int rx = -40; rx < 40; rx++) {
            for (int rz = -25; rz < 25; rz++) {
                regions++;
                if (UnderIslandFoliageFeature.regionSelected(
                        seed,
                        rx * config.regionSize(),
                        rz * config.regionSize(),
                        config.regionSize(),
                        config.regionChance())) {
                    selected++;
                }
            }
        }
        double share = (double) selected / regions;
        LOGGER.info("end_highlands under-island foliage regions: {} of {} ({})", selected, regions, share);
        helper.assertTrue(share >= 0.07 && share <= 0.13, "Highlands region share " + share + " is not about 10%");
        helper.succeed();
    }

    /**
     * On a floating slab, the feature turns the exposed bottom layer around the origin into Voidsoil (never where
     * there is no island block above), hangs 4-16 segment vines from it, and never places a vine below
     * {@code min_y}; if there is no room for a 4-segment vine above {@code min_y}, no vine is placed.
     */
    @GameTest(template = TALL, timeoutTicks = 200)
    public static void foliageHangsFromSlabUnderside(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        UnderIslandFoliageConfiguration base = (UnderIslandFoliageConfiguration) level.registryAccess()
                .registryOrThrow(Registries.CONFIGURED_FEATURE)
                .getOrThrow(ModFeatures.UNDER_ISLAND_FOLIAGE_CONFIGURED)
                .config();
        int minRel = 5;
        int minAbs = helper.absolutePos(new BlockPos(0, minRel, 0)).getY();
        ConfiguredFeature<?, ?> feature = new ConfiguredFeature<>(
                ModFeatures.UNDER_ISLAND_FOLIAGE.get(),
                new UnderIslandFoliageConfiguration(
                        base.soil(),
                        base.replaceable(),
                        base.radius(),
                        1.0F,
                        base.vineLength(),
                        base.fruitChance(),
                        VerticalAnchor.absolute(minAbs),
                        base.regionSize(),
                        base.regionChance()));
        BlockPos origin = helper.absolutePos(new BlockPos(8, PLATFORM_Y + 1, 8));

        for (int attempt = 0; attempt < 10; attempt++) {
            clear(helper);
            slab(helper);
            helper.assertTrue(
                    feature.place(level, level.getChunkSource().getGenerator(), level.random, origin),
                    "Foliage refused under a slab");
            int soil = 0;
            int vines = 0;
            for (BlockPos pos : BlockPos.betweenClosed(0, 1, 0, 16, PLATFORM_Y + 2, 16)) {
                BlockState state = helper.getBlockState(pos);
                if (state.is(ModBlocks.VOIDSOIL.get())) {
                    helper.assertTrue(pos.getY() == PLATFORM_Y - 2, "Voidsoil off the bottom layer at " + pos);
                    soil++;
                } else if (Voidvines.isVine(state)) {
                    helper.assertTrue(pos.getY() >= minRel, "Voidvine below min_y at " + pos);
                    if (!Voidvines.isVine(helper.getBlockState(pos.above()))) {
                        vines++;
                        helper.assertTrue(
                                helper.getBlockState(pos.above()).is(ModBlocks.VOIDSOIL.get()),
                                "Voidvine not hanging from voidsoil at " + pos);
                    }
                    if (!Voidvines.isVine(helper.getBlockState(pos.below()))) {
                        int length = Voidvines.lengthAbove(level, helper.absolutePos(pos));
                        helper.assertTrue(
                                length >= 4 && length <= PLATFORM_Y - 2 - minRel,
                                "Vine at " + pos + " has " + length + " segments");
                    }
                }
            }
            helper.assertTrue(soil >= 9, "Only " + soil + " voidsoil blocks under the slab");
            helper.assertTrue(vines == soil, vines + " vines for " + soil + " voidsoil blocks (vine chance 1)");
            helper.assertTrue(
                    helper.getBlockState(new BlockPos(8, PLATFORM_Y - 1, 8)).is(Blocks.END_STONE),
                    "The island above the soil was changed");
        }

        // With min_y 3 blocks below the underside, no 4-segment vine fits.
        ConfiguredFeature<?, ?> shallow = new ConfiguredFeature<>(
                ModFeatures.UNDER_ISLAND_FOLIAGE.get(),
                new UnderIslandFoliageConfiguration(
                        base.soil(),
                        base.replaceable(),
                        base.radius(),
                        1.0F,
                        base.vineLength(),
                        base.fruitChance(),
                        VerticalAnchor.absolute(helper.absolutePos(new BlockPos(0, PLATFORM_Y - 5, 0))
                                .getY()),
                        base.regionSize(),
                        base.regionChance()));
        clear(helper);
        slab(helper);
        helper.assertTrue(
                shallow.place(level, level.getChunkSource().getGenerator(), level.random, origin),
                "Foliage refused under a slab");
        for (BlockPos pos : BlockPos.betweenClosed(0, 1, 0, 16, PLATFORM_Y + 2, 16)) {
            helper.assertTrue(!Voidvines.isVine(helper.getBlockState(pos)), "Vine placed without room at " + pos);
        }
        clear(helper);
        helper.succeed();
    }

    /** Vines never grow (random ticks or bone meal) below Y=5 in the End. */
    @GameTest(timeoutTicks = 100)
    public static void vinesStopGrowingAtY5(GameTestHelper helper) {
        ServerLevel end = helper.getLevel().getServer().getLevel(Level.END);
        helper.assertTrue(end != null, "No End level");
        BlockPos soil = new BlockPos(0, MIN_VINE_Y + 3, 600);
        end.getChunk(soil);
        List<BlockPos> touched = List.of(soil.above(), soil, soil.below(), soil.below(2), soil.below(3), soil.below(4));
        touched.forEach(pos -> end.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS));
        end.setBlock(soil.above(), Blocks.END_STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        end.setBlock(soil, ModBlocks.VOIDSOIL.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        end.setBlock(soil.below(), ModBlocks.VOIDVINE_HEAD.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        for (int i = 0; i < 20; i++) {
            BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), end, soil.below());
            BlockPos head = soil.below();
            while (Voidvines.isVine(end.getBlockState(head.below()))) {
                head = head.below();
            }
            end.getBlockState(head).randomTick(end, head, end.random);
        }
        helper.assertTrue(
                Voidvines.isVine(end.getBlockState(new BlockPos(0, MIN_VINE_Y, 600))),
                "Vine did not grow down to Y=" + MIN_VINE_Y);
        helper.assertTrue(
                !Voidvines.isVine(end.getBlockState(new BlockPos(0, MIN_VINE_Y - 1, 600))),
                "Vine grew below Y=" + MIN_VINE_Y);
        touched.forEach(pos -> end.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS));
        helper.succeed();
    }

    private static boolean hasFeature(
            Registry<Biome> biomes, ResourceKey<Biome> biome, ResourceKey<PlacedFeature> feature) {
        List<HolderSet<PlacedFeature>> steps =
                biomes.getOrThrow(biome).getGenerationSettings().features();
        int step = GenerationStep.Decoration.VEGETAL_DECORATION.ordinal();
        return steps.size() > step && steps.get(step).stream().anyMatch(holder -> holder.is(feature));
    }

    /** A 3-block-thick end stone slab under a turf top at {@link #PLATFORM_Y}, floating in the air. */
    private static void slab(GameTestHelper helper) {
        for (int x = 0; x <= 16; x++) {
            for (int z = 0; z <= 16; z++) {
                for (int y = PLATFORM_Y - 2; y < PLATFORM_Y; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.END_STONE);
                }
                helper.setBlock(new BlockPos(x, PLATFORM_Y, z), ModBlocks.END_TURF.get());
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

    /** Whether the biome is {@code end_forest} or {@code end_oasis} across the chunk and 8 blocks around it. */
    private static boolean allForestOrOasis(BiomeSource source, Climate.Sampler sampler, ChunkPos chunk) {
        for (int x = chunk.getMinBlockX() - 8; x <= chunk.getMaxBlockX() + 11; x += 4) {
            for (int z = chunk.getMinBlockZ() - 8; z <= chunk.getMaxBlockZ() + 11; z += 4) {
                var biome = source.getNoiseBiome(
                        QuartPos.fromBlock(x), QuartPos.fromBlock(64), QuartPos.fromBlock(z), sampler);
                if (!biome.is(ModBiomes.END_FOREST) && !biome.is(ModBiomes.END_OASIS)) {
                    return false;
                }
            }
        }
        return true;
    }
}
