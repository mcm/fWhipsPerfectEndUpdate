package io.mcmaster.someonesperfectend.gametest;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import io.mcmaster.someonesperfectend.block.ModBlocks;
import io.mcmaster.someonesperfectend.worldgen.EndBiomeCategories;
import io.mcmaster.someonesperfectend.worldgen.ModBiomes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import org.slf4j.Logger;
import terrablender.api.RegionType;
import terrablender.api.Regions;
import terrablender.worldgen.IExtendedParameterList;
import terrablender.worldgen.IExtendedTheEndBiomeSource;

/** US-013: End biome placement through TerraBlender, surface rules, and purity of every dimension's biomes. */
public final class EndBiomeGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long SEED = 20260928L;
    private static final Set<ResourceKey<Biome>> VANILLA_END = Set.of(Biomes.THE_END, Biomes.END_HIGHLANDS,
            Biomes.END_MIDLANDS, Biomes.END_BARRENS, Biomes.SMALL_END_ISLANDS);

    private EndBiomeGameTests() {
    }

    /**
     * 2000 outer-End positions of a fixed seed: every new biome appears, and the new biomes cover 20–40% of the
     * island positions (every position but vanilla's small-islands category, which is the void between islands).
     * Small-islands positions stay {@code small_end_islands} (see {@code TheEndBiomeSourceMixin}).
     */
    @GameTest(timeoutTicks = 400)
    public static void outerEndBiomeCoverage(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        EndBiomes end = EndBiomes.create(server.registryAccess(), SEED);
        Random random = new Random(SEED);
        Map<ResourceKey<Biome>, Integer> counts = new HashMap<>();
        int islands = 0;
        for (int i = 0; i < 2000; i++) {
            double radius = 1100 + random.nextDouble() * 5000;
            double angle = random.nextDouble() * Math.PI * 2;
            int x = (int) (Math.cos(angle) * radius);
            int z = (int) (Math.sin(angle) * radius);
            ResourceKey<Biome> biome = end.biomeAt(x, z);
            counts.merge(biome, 1, Integer::sum);
            boolean smallIslands = end.category(x, z) == EndBiomeCategories.SMALL_ISLANDS;
            helper.assertTrue(smallIslands == biome.equals(Biomes.SMALL_END_ISLANDS),
                    "Small islands mismatch at " + x + "," + z + ": " + biome.location());
            if (!smallIslands) {
                islands++;
            }
        }
        LOGGER.info("Outer End biomes over 2000 positions: {}", counts);
        for (ResourceKey<Biome> biome : counts.keySet()) {
            helper.assertTrue(VANILLA_END.contains(biome) || ModBiomes.ALL.contains(biome),
                    "Unexpected End biome " + biome.location());
        }
        int ours = 0;
        for (ResourceKey<Biome> biome : ModBiomes.ALL) {
            int count = counts.getOrDefault(biome, 0);
            helper.assertTrue(count > 0, biome.location() + " never appears: " + counts);
            ours += count;
        }
        helper.assertTrue(counts.getOrDefault(Biomes.SMALL_END_ISLANDS, 0) > 0, "No small_end_islands: " + counts);
        double share = ours / (double) islands;
        LOGGER.info("New End biomes cover {}% of {} island positions", Math.round(share * 1000) / 10.0, islands);
        helper.assertTrue(share >= 0.20 && share <= 0.40,
                "New biomes cover " + Math.round(share * 100) + "% of island positions: " + counts);
        helper.assertTrue(counts.get(ModBiomes.END_OASIS) < counts.get(ModBiomes.END_PLAINS)
                && counts.get(ModBiomes.END_OASIS) < counts.get(ModBiomes.END_FOREST), "Oasis isn't rarest: " + counts);
        helper.succeed();
    }

    /** The real End level's biome source can only produce the vanilla End biomes plus ours. */
    @GameTest
    public static void endBiomeSourceIsVanillaPlusOurs(GameTestHelper helper) {
        ServerLevel end = helper.getLevel().getServer().getLevel(Level.END);
        helper.assertTrue(end != null, "No End level");
        Set<ResourceKey<Biome>> possible = keys(end.getChunkSource().getGenerator().getBiomeSource());
        Set<ResourceKey<Biome>> expected = new HashSet<>(VANILLA_END);
        expected.addAll(ModBiomes.ALL);
        helper.assertTrue(possible.equals(expected), "End biomes are " + possible + ", expected " + expected);
        helper.succeed();
    }

    /**
     * No Overworld or Nether regions are registered besides vanilla's, and TerraBlender's positional lookup gives
     * the vanilla biome at 500 sampled positions of each dimension.
     */
    @GameTest(timeoutTicks = 400)
    public static void overworldAndNetherAreVanilla(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        RegistryAccess access = server.registryAccess();
        for (RegionType type : List.of(RegionType.OVERWORLD, RegionType.NETHER)) {
            List<String> regions = Regions.get(type).stream().map(r -> r.getName().toString()).toList();
            String vanilla = type == RegionType.OVERWORLD ? "minecraft:overworld" : "minecraft:nether";
            helper.assertTrue(regions.equals(List.of(vanilla)), type + " regions: " + regions);
        }
        compareWithVanilla(helper, access, RegionType.OVERWORLD, MultiNoiseBiomeSourceParameterLists.OVERWORLD,
                NoiseGeneratorSettings.OVERWORLD, Level.OVERWORLD);
        compareWithVanilla(helper, access, RegionType.NETHER, MultiNoiseBiomeSourceParameterLists.NETHER,
                NoiseGeneratorSettings.NETHER, Level.NETHER);
        helper.succeed();
    }

    /** Island tops: {@code end_turf} in End Plains, {@code end_turf} and some {@code voidsoil} in the others. */
    @GameTest(timeoutTicks = 1200)
    public static void surfaceRulesTopIslands(GameTestHelper helper) {
        ServerLevel end = helper.getLevel().getServer().getLevel(Level.END);
        helper.assertTrue(end != null, "No End level");
        BiomeSource source = end.getChunkSource().getGenerator().getBiomeSource();
        Climate.Sampler sampler = end.getChunkSource().randomState().sampler();
        Map<ResourceKey<Biome>, Map<BlockState, Integer>> tops = new HashMap<>();
        Map<ResourceKey<Biome>, Integer> chunks = new HashMap<>();
        // Scan an outer area and generate the chunks whose center is on an island in a wanted biome.
        for (int cx = 70; cx < 400 && !enoughChunks(chunks); cx += 7) {
            for (int cz = -200; cz <= 200 && !enoughChunks(chunks); cz += 7) {
                BlockPos center = new ChunkPos(cx, cz).getMiddleBlockPosition(64);
                ResourceKey<Biome> biome = biomeKey(source.getNoiseBiome(QuartPos.fromBlock(center.getX()),
                        QuartPos.fromBlock(64), QuartPos.fromBlock(center.getZ()), sampler));
                boolean wanted = ModBiomes.ALL.contains(biome) || biome.equals(Biomes.END_HIGHLANDS);
                if (!wanted || chunks.getOrDefault(biome, 0) >= 4 || end.getChunkSource().getGenerator()
                        .getBaseHeight(center.getX(), center.getZ(), Heightmap.Types.WORLD_SURFACE_WG, end,
                                end.getChunkSource().randomState()) <= end.getMinBuildHeight()) {
                    continue;
                }
                chunks.merge(biome, 1, Integer::sum);
                end.getChunk(cx, cz);
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        int bx = cx * 16 + x;
                        int bz = cz * 16 + z;
                        int y = end.getHeight(Heightmap.Types.WORLD_SURFACE, bx, bz) - 1;
                        if (y <= end.getMinBuildHeight()) {
                            continue;
                        }
                        BlockPos top = new BlockPos(bx, y, bz);
                        ResourceKey<Biome> here = biomeKey(end.getBiome(top));
                        tops.computeIfAbsent(here, k -> new HashMap<>()).merge(end.getBlockState(top), 1,
                                Integer::sum);
                        if (here.equals(ModBiomes.END_PLAINS)) {
                            helper.assertTrue(!end.getBlockState(top).is(Blocks.CHORUS_PLANT)
                                    && !end.getBlockState(top).is(Blocks.CHORUS_FLOWER), "Chorus in End Plains at "
                                    + top);
                        }
                    }
                }
            }
        }
        LOGGER.info("End surface blocks by biome: {} (chunks {})", tops, chunks);
        helper.assertTrue(enoughChunks(chunks), "Didn't find island chunks in every biome: " + chunks);
        BlockState turf = ModBlocks.END_TURF.get().defaultBlockState();
        BlockState voidsoil = ModBlocks.VOIDSOIL.get().defaultBlockState();
        for (ResourceKey<Biome> biome : ModBiomes.ALL) {
            Map<BlockState, Integer> blocks = tops.getOrDefault(biome, Map.of());
            int total = blocks.values().stream().mapToInt(Integer::intValue).sum();
            int turfCount = blocks.getOrDefault(turf, 0);
            int voidsoilCount = blocks.getOrDefault(voidsoil, 0);
            helper.assertTrue(total > 0, "No surface found in " + biome.location());
            helper.assertTrue(turfCount + voidsoilCount >= 0.9 * total,
                    biome.location() + " surface is not turf/voidsoil: " + blocks);
            helper.assertTrue(turfCount > voidsoilCount, biome.location() + " is mostly voidsoil: " + blocks);
            if (biome.equals(ModBiomes.END_PLAINS)) {
                helper.assertTrue(voidsoilCount == 0, "Voidsoil in End Plains: " + blocks);
            }
        }
        int patchy = 0;
        for (ResourceKey<Biome> biome : List.of(ModBiomes.END_FOREST, ModBiomes.END_OASIS)) {
            patchy += tops.getOrDefault(biome, Map.of()).getOrDefault(voidsoil, 0);
        }
        helper.assertTrue(patchy > 0, "No voidsoil patches in End Forest / End Oasis: " + tops);
        Map<BlockState, Integer> highlands = tops.getOrDefault(Biomes.END_HIGHLANDS, Map.of());
        helper.assertTrue(!highlands.containsKey(turf) && !highlands.containsKey(voidsoil),
                "Vanilla End Highlands got our surface: " + highlands);
        helper.succeed();
    }

    private static boolean enoughChunks(Map<ResourceKey<Biome>, Integer> chunks) {
        for (ResourceKey<Biome> biome : List.of(ModBiomes.END_PLAINS, ModBiomes.END_FOREST, ModBiomes.END_OASIS,
                Biomes.END_HIGHLANDS)) {
            if (chunks.getOrDefault(biome, 0) < 2) {
                return false;
            }
        }
        return true;
    }

    /**
     * Compares TerraBlender's lookup (what {@link MultiNoiseBiomeSource} uses with TerraBlender installed) against
     * vanilla's plain parameter search over the same parameters and climate sampler.
     */
    private static void compareWithVanilla(GameTestHelper helper, RegistryAccess access, RegionType type,
            ResourceKey<MultiNoiseBiomeSourceParameterList> preset, ResourceKey<NoiseGeneratorSettings> settings,
            ResourceKey<Level> dimension) {
        List<Pair<Climate.ParameterPoint, Holder<Biome>>> values = access
                .registryOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST).getOrThrow(preset).parameters()
                .values();
        Climate.ParameterList<Holder<Biome>> vanilla = new Climate.ParameterList<>(values);
        Climate.ParameterList<Holder<Biome>> blended = new Climate.ParameterList<>(values);
        @SuppressWarnings("unchecked")
        IExtendedParameterList<Holder<Biome>> extended = (IExtendedParameterList<Holder<Biome>>) blended;
        extended.initializeForTerraBlender(access, type, SEED);
        Climate.Sampler sampler = RandomState.create(access.asGetterLookup(), settings, SEED).sampler();

        // The real level too, when its generator uses the multi-noise biome source (not the case for a flat world).
        ServerLevel level = helper.getLevel().getServer().getLevel(dimension);
        BiomeSource levelSource = level == null ? null : level.getChunkSource().getGenerator().getBiomeSource();
        Climate.Sampler levelSampler = level == null ? null : level.getChunkSource().randomState().sampler();
        boolean checkLevel = levelSource instanceof MultiNoiseBiomeSource;

        Random random = new Random(SEED ^ type.ordinal());
        for (int i = 0; i < 500; i++) {
            int x = QuartPos.fromBlock(random.nextInt(20000) - 10000);
            int y = QuartPos.fromBlock(random.nextInt(256) - 64);
            int z = QuartPos.fromBlock(random.nextInt(20000) - 10000);
            Holder<Biome> expected = vanilla.findValue(sampler.sample(x, y, z));
            Holder<Biome> actual = extended.findValuePositional(sampler.sample(x, y, z), x, y, z);
            helper.assertTrue(expected.equals(actual), type + " biome at quart " + x + "," + y + "," + z + " is "
                    + actual.unwrapKey() + ", vanilla " + expected.unwrapKey());
            if (checkLevel) {
                Holder<Biome> vanillaHere = vanilla.findValue(levelSampler.sample(x, y, z));
                Holder<Biome> levelHere = levelSource.getNoiseBiome(x, y, z, levelSampler);
                helper.assertTrue(vanillaHere.equals(levelHere), dimension.location() + " level biome at quart " + x
                        + "," + y + "," + z + " is " + levelHere.unwrapKey() + ", vanilla " + vanillaHere.unwrapKey());
            }
        }
        LOGGER.info("{}: 500 positions match vanilla (level source checked: {})", dimension.location(), checkLevel);
    }

    private static Set<ResourceKey<Biome>> keys(BiomeSource source) {
        return source.possibleBiomes().stream().map(EndBiomeGameTests::biomeKey).collect(Collectors.toSet());
    }

    private static ResourceKey<Biome> biomeKey(Holder<Biome> holder) {
        return holder.unwrapKey().orElseThrow(() -> new IllegalStateException("Unregistered biome " + holder));
    }

    /** A standalone End biome source with TerraBlender's placement, for a fixed seed. */
    private record EndBiomes(TheEndBiomeSource source, Climate.Sampler sampler) {
        static EndBiomes create(RegistryAccess access, long seed) {
            Registry<Biome> biomes = access.registryOrThrow(Registries.BIOME);
            TheEndBiomeSource source = TheEndBiomeSource.create(biomes.asLookup());
            ((IExtendedTheEndBiomeSource) source).initializeForTerraBlender(access, seed);
            return new EndBiomes(source, RandomState.create(access.asGetterLookup(), NoiseGeneratorSettings.END, seed).sampler());
        }

        EndBiomeCategories category(int blockX, int blockZ) {
            return EndBiomeCategories.at(QuartPos.fromBlock(blockX), QuartPos.fromBlock(64),
                    QuartPos.fromBlock(blockZ), sampler);
        }

        ResourceKey<Biome> biomeAt(int blockX, int blockZ) {
            return biomeKey(source.getNoiseBiome(QuartPos.fromBlock(blockX), QuartPos.fromBlock(64),
                    QuartPos.fromBlock(blockZ), sampler));
        }
    }
}
