package io.mcmaster.someonesperfectend.gametest;

import com.mojang.logging.LogUtils;
import io.mcmaster.someonesperfectend.block.ModBlocks;
import io.mcmaster.someonesperfectend.entity.Endling;
import io.mcmaster.someonesperfectend.worldgen.EndlingVillageLayout;
import io.mcmaster.someonesperfectend.worldgen.EndlingVillagePieces;
import io.mcmaster.someonesperfectend.worldgen.EndlingVillages;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

/**
 * US-023: Endling villages on the GameTest server's End (a fixed seed). Villages are located the way
 * {@code /locate} does it, and one is generated for real to count its huts, glowcaps, and endlings.
 */
public final class EndlingVillageGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final BlockPos SEARCH_FROM = new BlockPos(2000, 64, 0);
    private static final int SEARCH_DISTANCE = 4096;
    /**
     * Seeds the random source the village is placed with, so a failure reproduces on every run. Override it with
     * {@code -Dsomeonesperfectend.villageSeed=<long>} to try other placements.
     */
    private static final long PLACEMENT_SEED = Long.getLong("someonesperfectend.villageSeed", 23L);
    /** Wall-clock limit for the village chunks to become entity-ticking (see {@link #awaitEntityTicking}). */
    private static final long ENTITY_TICKING_TIMEOUT_MS = 120_000L;

    private EndlingVillageGameTests() {}

    private static ServerLevel end(GameTestHelper helper) {
        return helper.getLevel().getServer().getLevel(Level.END);
    }

    private static Holder<Structure> structure(ServerLevel end) {
        return end.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolderOrThrow(EndlingVillages.STRUCTURE);
    }

    private static RandomSpreadStructurePlacement placement(ServerLevel end) {
        return (RandomSpreadStructurePlacement) end.registryAccess()
                .registryOrThrow(Registries.STRUCTURE_SET)
                .getOrThrow(EndlingVillages.STRUCTURE_SET)
                .placement();
    }

    /**
     * The village start chunk nearest to {@link #SEARCH_FROM} within {@link #SEARCH_DISTANCE} blocks, searched
     * over the structure set's candidate chunks like {@code /locate structure} does.
     */
    private static ChunkPos nearestVillage(GameTestHelper helper, ServerLevel end) {
        ChunkGeneratorStructureState state = end.getChunkSource().getGeneratorState();
        Structure structure = structure(end).value();
        RandomSpreadStructurePlacement placement = placement(end);
        int spacing = placement.spacing();
        int regions = SEARCH_DISTANCE / 16 / spacing + 1;
        int fromX = SectionPos.blockToSectionCoord(SEARCH_FROM.getX());
        int fromZ = SectionPos.blockToSectionCoord(SEARCH_FROM.getZ());
        ChunkPos nearest = null;
        double best = Double.MAX_VALUE;
        for (int rx = -regions; rx <= regions; rx++) {
            for (int rz = -regions; rz <= regions; rz++) {
                ChunkPos chunk = placement.getPotentialStructureChunk(
                        state.getLevelSeed(), fromX + rx * spacing, fromZ + rz * spacing);
                double distance = Math.hypot(
                        chunk.getMinBlockX() - SEARCH_FROM.getX(), chunk.getMinBlockZ() - SEARCH_FROM.getZ());
                if (distance > SEARCH_DISTANCE
                        || distance >= best
                        || !placement.isStructureChunk(state, chunk.x, chunk.z)) {
                    continue;
                }
                // The check /locate runs before loading the chunk.
                if (end.structureManager().checkStructurePresence(chunk, structure, placement, false)
                        == StructureCheckResult.CHUNK_LOAD_NEEDED) {
                    nearest = chunk;
                    best = distance;
                }
            }
        }
        helper.assertTrue(
                nearest != null, "No Endling village within " + SEARCH_DISTANCE + " blocks of " + SEARCH_FROM);
        LOGGER.info(
                "Nearest Endling village to {} (End seed {}): chunk {} at distance {}",
                SEARCH_FROM,
                end.getSeed(),
                nearest,
                (int) best);
        return nearest;
    }

    /**
     * Runs vanilla's structure-start step ({@code ChunkGenerator#createStructures}) on a fresh proto chunk.
     * The GameTest server's world has structure generation turned off, so its real chunks never get starts
     * (and {@code /locate} finds nothing there); this is the code path a normal world runs.
     */
    private static StructureStart createStart(ServerLevel end, ChunkPos chunk) {
        ProtoChunk proto = new ProtoChunk(
                chunk, UpgradeData.EMPTY, end, end.registryAccess().registryOrThrow(Registries.BIOME), null);
        end.getChunkSource()
                .getGenerator()
                .createStructures(
                        end.registryAccess(),
                        end.getChunkSource().getGeneratorState(),
                        end.structureManager(),
                        proto,
                        end.getStructureManager());
        return proto.getStartForStructure(structure(end).value());
    }

    /**
     * A fixed-seed village start exists within 4096 blocks of (2000, 0): {@code /locate structure}'s
     * presence check accepts it, and the chunk-generation step creates it. (The real command can't be run
     * here: with structure generation off it searches 100 rings of regions and finds nothing.)
     */
    @GameTest(timeoutTicks = 400)
    public static void villageStartNearby(GameTestHelper helper) {
        ServerLevel end = end(helper);
        ChunkPos chunk = nearestVillage(helper, end);
        StructureStart start = createStart(end, chunk);
        helper.assertTrue(start != null && start.isValid(), "Chunk generation made no village start in " + chunk);
        helper.succeed();
    }

    /**
     * Placing the nearest village (like {@code /place structure}) gives 3–6 huts, 1–3 huge glowcaps, and one
     * endling per hut, each bound to the village center. Its pieces also survive a save/load round trip.
     */
    @GameTest(timeoutTicks = 1200)
    public static void generatedVillageHasEndlings(GameTestHelper helper) {
        ServerLevel end = end(helper);
        StructureStart start = createStart(end, nearestVillage(helper, end));
        helper.assertTrue(start != null && start.isValid(), "No village start");
        List<EndlingVillagePieces.Hut> huts = start.getPieces().stream()
                .filter(EndlingVillagePieces.Hut.class::isInstance)
                .map(EndlingVillagePieces.Hut.class::cast)
                .toList();
        long glowcaps = start.getPieces().stream()
                .filter(EndlingVillagePieces.Glowcap.class::isInstance)
                .count();
        helper.assertTrue(
                huts.size() >= EndlingVillageLayout.MIN_HUTS && huts.size() <= EndlingVillageLayout.MAX_HUTS,
                "Hut count out of range: " + huts.size());
        helper.assertTrue(
                glowcaps >= EndlingVillageLayout.MIN_GLOWCAPS && glowcaps <= EndlingVillageLayout.MAX_GLOWCAPS,
                "Glowcap count out of range: " + glowcaps);

        // Pieces round-trip through NBT (chunk save/load).
        StructurePieceSerializationContext context = StructurePieceSerializationContext.fromLevel(end);
        StructureStart loaded =
                StructureStart.loadStaticStart(context, start.createTag(context, start.getChunkPos()), end.getSeed());
        helper.assertTrue(
                loaded != null
                        && loaded.getPieces().size() == start.getPieces().size()
                        && loaded.getBoundingBox().equals(start.getBoundingBox()),
                "Village start didn't survive NBT");

        // Load the village area and place every chunk of it, as /place structure does.
        BoundingBox box = start.getBoundingBox();
        ChunkPos min =
                new ChunkPos(SectionPos.blockToSectionCoord(box.minX()), SectionPos.blockToSectionCoord(box.minZ()));
        ChunkPos max =
                new ChunkPos(SectionPos.blockToSectionCoord(box.maxX()), SectionPos.blockToSectionCoord(box.maxZ()));
        List<ChunkPos> chunks = ChunkPos.rangeClosed(
                        new ChunkPos(min.x - 1, min.z - 1), new ChunkPos(max.x + 1, max.z + 1))
                .toList();
        for (ChunkPos chunk : chunks) {
            end.setChunkForced(chunk.x, chunk.z, true);
            end.getChunk(chunk.x, chunk.z);
        }
        awaitEntityTicking(helper, end, chunks);
        RandomSource random = RandomSource.create(PLACEMENT_SEED);
        Map<Endling, Boolean> spawned = new LinkedHashMap<>();
        EndlingVillagePieces.endlingSpawnListener = spawned::put;
        try {
            ChunkPos.rangeClosed(min, max)
                    .forEach(chunk -> start.placeInChunk(
                            end,
                            end.structureManager(),
                            end.getChunkSource().getGenerator(),
                            random,
                            new BoundingBox(
                                    chunk.getMinBlockX(),
                                    end.getMinBuildHeight(),
                                    chunk.getMinBlockZ(),
                                    chunk.getMaxBlockX(),
                                    end.getMaxBuildHeight(),
                                    chunk.getMaxBlockZ()),
                            chunk));
        } finally {
            EndlingVillagePieces.endlingSpawnListener = null;
        }
        for (EndlingVillagePieces.Hut hut : huts) {
            BlockPos pos = hut.endlingPos();
            Optional<Map.Entry<Endling, Boolean>> spawn = spawned.entrySet().stream()
                    .filter(e -> e.getKey().blockPosition().equals(pos))
                    .findFirst();
            LOGGER.info(
                    "Hut endling at {} (placement seed {}): {}",
                    pos,
                    PLACEMENT_SEED,
                    spawn.map(e -> (e.getValue() ? "added " : "NOT added ") + e.getKey() + ", visible "
                                    + (end.getEntity(e.getKey().getUUID()) != null) + ", entity ticking "
                                    + end.isPositionEntityTicking(pos) + ", entities loaded "
                                    + end.areEntitiesLoaded(new ChunkPos(pos).toLong()))
                            .orElse("NOT spawned"));
        }

        int stems = count(end, box, ModBlocks.GLOWCAP_STEM.get());
        int floors = count(end, box, ModBlocks.VOIDVINE_MOSAIC.get());
        int roofs = count(end, box, ModBlocks.GLOWCAP_BLOCK.get());
        LOGGER.info(
                "Endling village at {}: {} huts, {} glowcaps, {} glowcap stems, {} floor blocks, {} glowcap blocks",
                box,
                huts.size(),
                glowcaps,
                stems,
                floors,
                roofs);
        helper.assertTrue(stems > 0, "No huge glowcap stem in the village");
        int hutArea = EndlingVillageLayout.HUT_SIZE * EndlingVillageLayout.HUT_SIZE;
        helper.assertTrue(floors >= huts.size() * hutArea, "Missing hut floors: " + floors);
        AABB area = AABB.of(box).inflate(4);
        helper.succeedWhen(() -> {
            List<Endling> endlings = end.getEntitiesOfClass(Endling.class, area);
            helper.assertTrue(endlings.size() >= 3, "Only " + endlings.size() + " endlings in the village");
            helper.assertTrue(
                    endlings.size() == huts.size(),
                    endlings.size() + " endlings for " + huts.size() + " huts (placement seed " + PLACEMENT_SEED + "; "
                            + spawned.size() + " spawned; missing: " + missing(end, spawned, endlings) + ")");
            for (Endling endling : endlings) {
                helper.assertTrue(endling.countsTowardVillage(), "Endling not bound to the village: " + endling);
                helper.assertTrue(
                        box.isInside(endling.village()), "Village point outside the village: " + endling.village());
            }
            chunks.forEach(chunk -> end.setChunkForced(chunk.x, chunk.z, false));
        });
    }

    /**
     * Rarity: over a 12000×12000 area around the origin, about one village per 1500×1500 blocks
     * (0.5–1.5), none closer than 1024 blocks to the origin.
     */
    @GameTest(timeoutTicks = 400)
    public static void villageRarity(GameTestHelper helper) {
        ServerLevel end = end(helper);
        ChunkGenerator generator = end.getChunkSource().getGenerator();
        ChunkGeneratorStructureState state = end.getChunkSource().getGeneratorState();
        Structure structure = structure(end).value();
        RandomSpreadStructurePlacement placement = placement(end);
        int half = 6000;
        int spacing = placement.spacing();
        int minRegion = Math.floorDiv(-half >> 4, spacing);
        int maxRegion = Math.floorDiv((half - 1) >> 4, spacing);
        int candidates = 0;
        List<BlockPos> starts = new ArrayList<>();
        for (int rx = minRegion; rx <= maxRegion; rx++) {
            for (int rz = minRegion; rz <= maxRegion; rz++) {
                ChunkPos chunk = placement.getPotentialStructureChunk(state.getLevelSeed(), rx * spacing, rz * spacing);
                if (Math.abs(chunk.getMiddleBlockX()) >= half
                        || Math.abs(chunk.getMiddleBlockZ()) >= half
                        || !placement.isStructureChunk(state, chunk.x, chunk.z)) {
                    continue;
                }
                candidates++;
                Optional<Structure.GenerationStub> stub =
                        structure.findValidGenerationPoint(new Structure.GenerationContext(
                                end.registryAccess(),
                                generator,
                                generator.getBiomeSource(),
                                state.randomState(),
                                end.getServer().getStructureManager(),
                                state.getLevelSeed(),
                                chunk,
                                end,
                                structure.biomes()::contains));
                stub.ifPresent(s -> starts.add(s.position()));
            }
        }
        double areas = (4.0 * half * half) / (1500.0 * 1500.0);
        double perArea = starts.size() / areas;
        LOGGER.info(
                "Endling villages over {}x{}: {} of {} candidates, {} per 1500x1500",
                2 * half,
                2 * half,
                starts.size(),
                candidates,
                String.format("%.2f", perArea));
        for (BlockPos pos : starts) {
            helper.assertTrue(EndlingVillageLayout.farEnough(pos.getX(), pos.getZ()), "Village too close: " + pos);
            Holder<Biome> biome = generator
                    .getBiomeSource()
                    .getNoiseBiome(
                            QuartPos.fromBlock(pos.getX()),
                            QuartPos.fromBlock(pos.getY()),
                            QuartPos.fromBlock(pos.getZ()),
                            state.randomState().sampler());
            helper.assertTrue(structure.biomes().contains(biome), "Village in a wrong biome at " + pos);
        }
        helper.assertTrue(perArea >= 0.5 && perArea <= 1.5, "Village rarity " + perArea + " per 1500x1500");
        helper.succeed();
    }

    /**
     * Runs the level's chunk tasks on this thread (as {@code getChunk} does) until every chunk in {@code chunks} is
     * entity-ticking. Entities in a chunk are invisible to lookups until its full-status promotion runs, and for a
     * chunk that got its forced ticket while still loading, that is the entity-ticking promotion, which waits for
     * every chunk within 2 to generate. The GameTest server never sleeps between ticks, so a test's whole timeout
     * can pass in a second or two, before that generation is done on a busy machine.
     */
    private static void awaitEntityTicking(GameTestHelper helper, ServerLevel level, List<ChunkPos> chunks) {
        long start = Util.getMillis();
        List<ChunkPos> waiting = new ArrayList<>(chunks);
        while (true) {
            waiting.removeIf(chunk -> level.isPositionEntityTicking(chunk.getWorldPosition()));
            if (waiting.isEmpty()) {
                break;
            }
            helper.assertTrue(
                    Util.getMillis() - start < ENTITY_TICKING_TIMEOUT_MS,
                    "Village chunks not entity-ticking after " + ENTITY_TICKING_TIMEOUT_MS + " ms: " + waiting);
            if (!level.getChunkSource().pollTask()) {
                LockSupport.parkNanos(1_000_000L);
            }
        }
        LOGGER.info("Village chunks entity-ticking after {} ms", Util.getMillis() - start);
    }

    /** The spawned endlings not among {@code found}, with why: refused by the level, removed, or elsewhere. */
    private static String missing(ServerLevel level, Map<Endling, Boolean> spawned, List<Endling> found) {
        return spawned.entrySet().stream()
                .filter(e -> !found.contains(e.getKey()))
                .map(e -> {
                    Endling endling = e.getKey();
                    String state = !e.getValue()
                            ? "never added"
                            : endling.isRemoved()
                                    ? "removed (" + endling.getRemovalReason() + ")"
                                    : "not removed, health " + endling.getHealth() + ", chunk entities loaded "
                                            + level.areEntitiesLoaded(
                                                    endling.chunkPosition().toLong())
                                            + ", entity ticking "
                                            + level.isPositionEntityTicking(endling.blockPosition())
                                            + ", found by id " + (level.getEntity(endling.getId()) != null);
                    return endling.getUUID() + " at " + endling.blockPosition() + ": " + state;
                })
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private static int count(ServerLevel level, BoundingBox box, Block block) {
        int count = 0;
        for (BlockPos pos :
                BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            ChunkAccess chunk = level.getChunk(pos);
            if (chunk.getBlockState(pos).is(block)) {
                count++;
            }
        }
        return count;
    }
}
