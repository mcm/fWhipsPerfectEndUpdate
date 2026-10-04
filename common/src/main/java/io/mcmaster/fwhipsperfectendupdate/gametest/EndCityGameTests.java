package io.mcmaster.fwhipsperfectendupdate.gametest;

import com.mojang.logging.LogUtils;
import io.mcmaster.fwhipsperfectendupdate.worldgen.ElevationBands;
import io.mcmaster.fwhipsperfectendupdate.worldgen.ElevationBandsFunction;
import io.mcmaster.fwhipsperfectendupdate.worldgen.EndCities;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BuiltinStructureSets;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import org.slf4j.Logger;
import terrablender.worldgen.IExtendedTheEndBiomeSource;

/** US-018: vanilla End Cities start on islands in every elevation band, about as often as in vanilla. */
public final class EndCityGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long SEED = 20260928L;
    private static final LevelHeightAccessor END_HEIGHT = LevelHeightAccessor.create(0, 256);

    private EndCityGameTests() {}

    /** A fixed-seed 4096×4096 outer area has End City starts in at least 2 bands, each at its band's height. */
    @GameTest(timeoutTicks = 400)
    public static void endCitiesInSeveralBands(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        Cities ours = Cities.create(
                server,
                server.registryAccess()
                        .registryOrThrow(Registries.NOISE_SETTINGS)
                        .getOrThrow(NoiseGeneratorSettings.END));
        ElevationBandsFunction bands =
                ElevationBandsFunction.find(ours.randomState()).orElseThrow();
        int[] counts = new int[3];
        long[] sums = new long[3];
        for (BlockPos start : ours.starts(2048, -2048, 4096)) {
            int offset = bands.offsetAt(start.getX(), start.getZ());
            helper.assertTrue(offset != ElevationBands.GAP, "End City start in a band gap: " + start);
            int band = bands.layout().groupAt(start.getX(), start.getZ()).band().ordinal();
            // The start sits on the island surface, moved with the band (vanilla requires Y >= 60 unmoved).
            helper.assertTrue(
                    start.getY() >= EndCities.minimumY(60, offset, 60),
                    "End City start below its band's minimum: " + start);
            counts[band]++;
            sums[band] += start.getY();
        }
        String stats = "lower=" + counts[0] + " (mean y " + mean(sums[0], counts[0]) + "), middle=" + counts[1]
                + " (mean y " + mean(sums[1], counts[1]) + "), upper=" + counts[2] + " (mean y "
                + mean(sums[2], counts[2]) + ")";
        LOGGER.info("End City starts by band: {}", stats);
        int bandsWithCities = 0;
        for (int count : counts) {
            bandsWithCities += count > 0 ? 1 : 0;
        }
        helper.assertTrue(bandsWithCities >= 2, "End City starts in fewer than 2 bands: " + stats);
        helper.succeed();
    }

    /** Over an 8192×8192 outer area, End City starts are within ±20% of the vanilla terrain's count. */
    @GameTest(timeoutTicks = 400)
    public static void endCityFrequencyMatchesVanilla(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        Cities ours = Cities.create(
                server,
                server.registryAccess()
                        .registryOrThrow(Registries.NOISE_SETTINGS)
                        .getOrThrow(NoiseGeneratorSettings.END));
        Cities vanilla = Cities.create(server, ElevationBandGameTests.vanillaEndSettings(server));
        int ourCount = ours.starts(2048, -4096, 8192).size();
        int vanillaCount = vanilla.starts(2048, -4096, 8192).size();
        LOGGER.info("End City starts over 8192x8192: {} (vanilla terrain {})", ourCount, vanillaCount);
        helper.assertTrue(vanillaCount >= 20, "Too few vanilla End Cities to compare: " + vanillaCount);
        helper.assertTrue(
                Math.abs(ourCount - vanillaCount) <= 0.2 * vanillaCount,
                "End City count " + ourCount + " is not within 20% of vanilla " + vanillaCount);
        helper.succeed();
    }

    private static String mean(long sum, int count) {
        return count == 0 ? "-" : Long.toString(sum / count);
    }

    /** The vanilla End City structure and structure set, run on a standalone fixed-seed End generator. */
    private record Cities(
            RegistryAccess access,
            NoiseBasedChunkGenerator generator,
            RandomState randomState,
            Structure structure,
            RandomSpreadStructurePlacement placement,
            MinecraftServer server) {
        static Cities create(MinecraftServer server, NoiseGeneratorSettings settings) {
            RegistryAccess access = server.registryAccess();
            TheEndBiomeSource biomes = TheEndBiomeSource.create(
                    access.registryOrThrow(Registries.BIOME).asLookup());
            ((IExtendedTheEndBiomeSource) biomes).initializeForTerraBlender(access, SEED);
            StructureSet set =
                    access.registryOrThrow(Registries.STRUCTURE_SET).getOrThrow(BuiltinStructureSets.END_CITIES);
            return new Cities(
                    access,
                    new NoiseBasedChunkGenerator(biomes, Holder.direct(settings)),
                    RandomState.create(settings, access.lookupOrThrow(Registries.NOISE), SEED),
                    access.registryOrThrow(Registries.STRUCTURE).getOrThrow(BuiltinStructures.END_CITY),
                    (RandomSpreadStructurePlacement) set.placement(),
                    server);
        }

        /** Start positions of End Cities whose candidate chunk lies in the square [x, x + size) × [z, z + size). */
        List<BlockPos> starts(int minX, int minZ, int size) {
            int spacing = placement.spacing();
            List<BlockPos> starts = new ArrayList<>();
            int minRegionX = Math.floorDiv(minX >> 4, spacing);
            int maxRegionX = Math.floorDiv((minX + size - 1) >> 4, spacing);
            int minRegionZ = Math.floorDiv(minZ >> 4, spacing);
            int maxRegionZ = Math.floorDiv((minZ + size - 1) >> 4, spacing);
            for (int rx = minRegionX; rx <= maxRegionX; rx++) {
                for (int rz = minRegionZ; rz <= maxRegionZ; rz++) {
                    ChunkPos chunk = placement.getPotentialStructureChunk(SEED, rx * spacing, rz * spacing);
                    if (chunk.getMinBlockX() < minX
                            || chunk.getMinBlockX() >= minX + size
                            || chunk.getMinBlockZ() < minZ
                            || chunk.getMinBlockZ() >= minZ + size) {
                        continue;
                    }
                    Structure.GenerationContext context = new Structure.GenerationContext(
                            access,
                            generator,
                            generator.getBiomeSource(),
                            randomState,
                            server.getStructureManager(),
                            SEED,
                            chunk,
                            END_HEIGHT,
                            structure.biomes()::contains);
                    Optional<Structure.GenerationStub> stub = structure.findValidGenerationPoint(context);
                    stub.ifPresent(s -> starts.add(s.position()));
                }
            }
            return starts;
        }
    }
}
