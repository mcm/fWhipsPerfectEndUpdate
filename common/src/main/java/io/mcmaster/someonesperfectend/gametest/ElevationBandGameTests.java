package io.mcmaster.someonesperfectend.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import io.mcmaster.someonesperfectend.mixin.TheEndGatewayBlockEntityInvoker;
import io.mcmaster.someonesperfectend.worldgen.ElevationBands;
import io.mcmaster.someonesperfectend.worldgen.ElevationBandsFunction;
import java.io.Reader;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import org.slf4j.Logger;

/** US-012, MOD-71: outer-End elevation bands move whole islands; vanilla terrain near the origin; End Gateway exits. */
public final class ElevationBandGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long SEED = 20260928L;
    /** Top of the vanilla End's bottom slide, which {@code elevation_bands} reapplies to lowered islands. */
    private static final int BOTTOM_SLIDE_TOP = 32;

    private static final LevelHeightAccessor END_HEIGHT = LevelHeightAccessor.create(0, 256);
    private static final ResourceLocation END_SETTINGS_FILE =
            ResourceLocation.withDefaultNamespace("worldgen/noise_settings/end.json");

    private ElevationBandGameTests() {}

    /** Within 1024 blocks of the origin, the columns match the vanilla End noise settings block for block. */
    @GameTest
    public static void innerEndMatchesVanilla(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        NoiseGeneratorSettings vanilla = vanillaEndSettings(server);
        Generator ours = new Generator(server, modEndSettings(server).value());
        Generator reference = new Generator(server, vanilla);
        Random random = new Random(SEED);
        int islandColumns = 0;
        for (int i = 0; i < 160; i++) {
            // Half the samples on the central island, half just inside the 1024 ring (outer island edges).
            double radius = i % 2 == 0 ? random.nextDouble() * 160 : 1023 - random.nextDouble() * 160;
            double angle = random.nextDouble() * Math.PI * 2;
            int x = (int) (Math.cos(angle) * radius);
            int z = (int) (Math.sin(angle) * radius);
            NoiseColumn expected = reference.column(x, z);
            NoiseColumn actual = ours.column(x, z);
            for (int y = 0; y < 128; y++) {
                if (expected.getBlock(y) != actual.getBlock(y)) {
                    helper.fail("Column " + x + "," + z + " differs from vanilla at y=" + y + ": " + actual.getBlock(y)
                            + " (vanilla " + expected.getBlock(y) + ")");
                    return;
                }
            }
            if (reference.surface(x, z) > 0) {
                islandColumns++;
            }
        }
        helper.assertTrue(islandColumns >= 40, "Too few island columns sampled: " + islandColumns);
        helper.succeed();
    }

    /** 400 columns over a 4096×4096 outer region: every band holds ≥ 15% of island columns, ≥ 50% void. */
    @GameTest(timeoutTicks = 400)
    public static void outerIslandsFormThreeBands(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        Generator ours = new Generator(server, modEndSettings(server).value());
        int voidColumns = 0;
        int[] counts = new int[3];
        long[] sums = new long[3];
        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 20; j++) {
                int x = 2048 + i * 4096 / 20 + 37;
                int z = -2048 + j * 4096 / 20 + 53;
                int surface = ours.surface(x, z);
                if (surface <= 0) {
                    voidColumns++;
                    continue;
                }
                int band = surface < 52 ? 0 : surface <= 82 ? 1 : 2;
                counts[band]++;
                sums[band] += surface;
            }
        }
        int islands = counts[0] + counts[1] + counts[2];
        String stats = "void=" + voidColumns + "/400, lower=" + counts[0] + " (mean y " + mean(sums[0], counts[0])
                + "), middle=" + counts[1] + " (mean y " + mean(sums[1], counts[1]) + "), upper=" + counts[2]
                + " (mean y " + mean(sums[2], counts[2]) + ")";
        LOGGER.info("Elevation bands: {}", stats);
        helper.assertTrue(voidColumns >= 200, "Less than 50% void: " + stats);
        for (int count : counts) {
            helper.assertTrue(islands > 0 && count >= 0.15 * islands, "A band has < 15% of islands: " + stats);
        }
        helper.succeed();
    }

    /** An island keeps its shape: the whole island is moved by one offset (the offset is per island group). */
    @GameTest
    public static void regionsMoveIslandsWhole(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        Generator ours = new Generator(server, modEndSettings(server).value());
        Generator reference = new Generator(server, vanillaEndSettings(server));
        ElevationBandsFunction bands = ours.bands();
        int checked = 0;
        for (int x = 3000; x < 7000 && checked < 30; x += 24) {
            int z = 1500;
            int offset = bands.offsetAt(x, z);
            if (offset == ElevationBands.GAP
                    || offset == 0
                    || reference.surface(x, z) <= 0
                    || !sameOffsetAtCellCorners(bands, x, z, offset)) {
                continue;
            }
            // In a moved region the column is the vanilla column shifted by the offset (above the floor slide).
            NoiseColumn expected = reference.column(x, z);
            NoiseColumn actual = ours.column(x, z);
            for (int y = 40; y < 128; y++) {
                int source = y - offset;
                if (source < 32 || source >= 128) {
                    continue;
                }
                if (expected.getBlock(source) != actual.getBlock(y)) {
                    helper.fail("Column " + x + "," + z + " (offset " + offset + ") is not the vanilla column moved: y="
                            + y);
                    return;
                }
            }
            checked++;
        }
        helper.assertTrue(checked > 0, "No moved island columns found");
        helper.succeed();
    }

    /**
     * MOD-71: no island is broken apart. Every vanilla island column of the outer End (at noise cell corners, where
     * the terrain is sampled) is still land, at its vanilla height moved by its group's band, and the columns of
     * one island along a line all move by the same offset.
     */
    @GameTest(timeoutTicks = 400)
    public static void islandsAreNeverSplit(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        Generator ours = new Generator(server, modEndSettings(server).value());
        Generator reference = new Generator(server, vanillaEndSettings(server));
        ElevationBandsFunction bands = ours.bands();
        int islandColumns = 0;
        int moved = 0;
        for (int line = 0; line < 4; line++) {
            int z = -3000 + line * 2000;
            int previousSurface = 0;
            int previousOffset = 0;
            for (int x = 2400; x < 6400; x += 8) {
                int vanillaSurface = reference.surface(x, z);
                if (vanillaSurface <= 0 || bands.layout().islandValue(x, z) <= 0) {
                    previousSurface = 0;
                    continue;
                }
                islandColumns++;
                int offset = bands.offsetAt(x, z);
                helper.assertTrue(offset != ElevationBands.GAP, "Void cuts through an island at " + x + "," + z);
                int surface = ours.surface(x, z);
                int expected = vanillaSurface + offset;
                // Lowered below the bottom slide, an island is tapered like vanilla's lowest islands: never higher.
                helper.assertTrue(
                        expected < BOTTOM_SLIDE_TOP ? surface <= expected : surface == expected,
                        "Island column " + x + "," + z + " is at " + surface + ", not vanilla " + vanillaSurface
                                + " moved by " + offset);
                helper.assertTrue(
                        previousSurface <= 0 || previousOffset == offset,
                        "Island sheared between " + (x - 8) + "," + z + " and " + x + "," + z);
                moved += offset != 0 ? 1 : 0;
                previousSurface = surface;
                previousOffset = offset;
            }
        }
        LOGGER.info("Island columns checked: {}, moved: {}", islandColumns, moved);
        helper.assertTrue(islandColumns >= 50, "Too few island columns sampled: " + islandColumns);
        helper.assertTrue(moved > 0, "No moved island columns sampled");
        helper.succeed();
    }

    /** End Gateways still send players to a safe spot on an outer island. */
    @GameTest(timeoutTicks = 400)
    public static void gatewayExitIsSafe(GameTestHelper helper) {
        ServerLevel end = helper.getLevel().getServer().getLevel(Level.END);
        helper.assertTrue(end != null, "No End level");
        BlockPos exit = TheEndGatewayBlockEntityInvoker.someonesperfectend$findOrCreateValidTeleportPos(
                end, new BlockPos(96, 75, 0));
        helper.assertTrue(
                Math.sqrt(exit.getX() * (double) exit.getX() + exit.getZ() * (double) exit.getZ()) > 700,
                "Exit is not on the outer islands: " + exit);
        helper.assertTrue(end.getBlockState(exit).isCollisionShapeFullBlock(end, exit), "Exit is not solid: " + exit);
        helper.assertTrue(
                end.getBlockState(exit.above()).isAir()
                        && end.getBlockState(exit.above(2)).isAir(),
                "No room to stand above " + exit);
        helper.succeed();
    }

    /** Terrain is interpolated between noise cell corners 8 blocks apart; they must all share the offset. */
    private static boolean sameOffsetAtCellCorners(ElevationBandsFunction bands, int x, int z, int offset) {
        int cornerX = Math.floorDiv(x, 8) * 8;
        int cornerZ = Math.floorDiv(z, 8) * 8;
        for (int dx = 0; dx <= 8; dx += 8) {
            for (int dz = 0; dz <= 8; dz += 8) {
                if (bands.offsetAt(cornerX + dx, cornerZ + dz) != offset) {
                    return false;
                }
            }
        }
        return true;
    }

    private static String mean(long sum, int count) {
        return count == 0 ? "-" : Long.toString(sum / count);
    }

    private static Holder<NoiseGeneratorSettings> modEndSettings(MinecraftServer server) {
        return server.registryAccess()
                .registryOrThrow(Registries.NOISE_SETTINGS)
                .getHolderOrThrow(NoiseGeneratorSettings.END);
    }

    /** {@code minecraft:end} as shipped in the vanilla data pack (our override replaces it in the registry). */
    static NoiseGeneratorSettings vanillaEndSettings(MinecraftServer server) {
        List<Resource> stack = server.getResourceManager().getResourceStack(END_SETTINGS_FILE);
        Resource vanilla = stack.stream()
                .filter(r -> r.sourcePackId().equals("vanilla"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No vanilla end.json in "
                        + stack.stream().map(Resource::sourcePackId).toList()));
        try (Reader reader = vanilla.openAsReader()) {
            JsonElement json = JsonParser.parseReader(reader);
            RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, server.registryAccess());
            return NoiseGeneratorSettings.DIRECT_CODEC.parse(ops, json).getOrThrow();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** A standalone End generator with a fixed seed. */
    private record Generator(NoiseBasedChunkGenerator generator, RandomState randomState) {
        Generator(MinecraftServer server, NoiseGeneratorSettings settings) {
            this(
                    server.registryAccess(),
                    settings,
                    server.getLevel(Level.END).getChunkSource().getGenerator().getBiomeSource());
        }

        Generator(RegistryAccess access, NoiseGeneratorSettings settings, BiomeSource biomes) {
            this(
                    new NoiseBasedChunkGenerator(biomes, Holder.direct(settings)),
                    RandomState.create(settings, access.lookupOrThrow(Registries.NOISE), SEED));
        }

        NoiseColumn column(int x, int z) {
            return generator.getBaseColumn(x, z, END_HEIGHT, randomState);
        }

        /** Y above the top solid block, or 0 for a void column. */
        int surface(int x, int z) {
            return generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, END_HEIGHT, randomState);
        }

        ElevationBandsFunction bands() {
            return ElevationBandsFunction.find(randomState)
                    .orElseThrow(
                            () -> new IllegalStateException("The End noise settings have no elevation_bands function"));
        }
    }
}
