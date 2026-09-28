package io.mcmaster.someonesperfectend.worldgen;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

/**
 * The Endling village (US-023): huts around huge glowcaps, generated only on outer End islands (at least
 * {@link EndlingVillageLayout#MIN_DISTANCE} blocks from the origin) whose surface is large and flat enough for the whole
 * footprint. Biomes and rarity come from data (the structure's biome tag and its structure set).
 */
public class EndlingVillageStructure extends Structure {
    public static final MapCodec<EndlingVillageStructure> CODEC = simpleCodec(EndlingVillageStructure::new);

    /** Columns whose surface is below this are void. */
    public static final int MIN_SURFACE_Y = 10;
    /** How far the island edge (at the village radius) may drop below the center. */
    public static final int MAX_EDGE_DROP = 16;
    /** How far a glowcap's ground may be above or below the center. */
    public static final int MAX_GLOWCAP_STEP = 4;
    /** How far a hut floor may be above or below the center. */
    public static final int MAX_HUT_STEP = 8;
    private static final int EDGE_SAMPLES = 16;

    public EndlingVillageStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public StructureType<?> type() {
        return EndlingVillages.STRUCTURE_TYPE.get();
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        int x = chunk.getMiddleBlockX();
        int z = chunk.getMiddleBlockZ();
        if (!EndlingVillageLayout.farEnough(x, z)) {
            return Optional.empty();
        }
        int centerY = surface(context, x, z);
        if (centerY < MIN_SURFACE_Y || !context.validBiome().test(context.biomeSource().getNoiseBiome(
                QuartPos.fromBlock(x), QuartPos.fromBlock(centerY + 1), QuartPos.fromBlock(z),
                context.randomState().sampler()))) {
            return Optional.empty();
        }
        // The island must reach past the village radius all around (no hut hangs over the void).
        for (int i = 0; i < EDGE_SAMPLES; i++) {
            double angle = i * Math.PI * 2 / EDGE_SAMPLES;
            int y = surface(context, x + (int) Math.round(Math.cos(angle) * EndlingVillageLayout.RADIUS),
                    z + (int) Math.round(Math.sin(angle) * EndlingVillageLayout.RADIUS));
            if (y < MIN_SURFACE_Y || y < centerY - MAX_EDGE_DROP) {
                return Optional.empty();
            }
        }
        EndlingVillageLayout layout = EndlingVillageLayout.create(context.random());
        BlockPos center = new BlockPos(x, centerY + 1, z);
        List<BlockPos> glowcaps = new ArrayList<>();
        for (EndlingVillageLayout.Site site : layout.glowcaps()) {
            int y = surface(context, x + site.dx(), z + site.dz());
            if (y < MIN_SURFACE_Y || Math.abs(y - centerY) > MAX_GLOWCAP_STEP) {
                return Optional.empty();
            }
            glowcaps.add(new BlockPos(x + site.dx(), y + 1, z + site.dz()));
        }
        List<EndlingVillagePieces.Hut> huts = new ArrayList<>();
        int half = EndlingVillageLayout.HUT_SIZE / 2;
        for (EndlingVillageLayout.Hut hut : layout.huts()) {
            int hx = x + hut.dx();
            int hz = z + hut.dz();
            int low = Integer.MAX_VALUE;
            int high = Integer.MIN_VALUE;
            for (int[] corner : new int[][] {{0, 0}, {-half, -half}, {half, -half}, {-half, half}, {half, half}}) {
                int y = surface(context, hx + corner[0], hz + corner[1]);
                low = Math.min(low, y);
                high = Math.max(high, y);
            }
            // The floor sits on the highest corner; the foundation fills down to the lowest one.
            if (low < MIN_SURFACE_Y || high - low >= EndlingVillagePieces.FOUNDATION_DEPTH
                    || Math.abs(high - centerY) > MAX_HUT_STEP) {
                return Optional.empty();
            }
            huts.add(new EndlingVillagePieces.Hut(new BlockPos(hx - half, high, hz - half), hut.door(), hut.astral(),
                    center));
        }
        return Optional.of(new GenerationStub(center, (StructurePiecesBuilder builder) -> {
            glowcaps.forEach(pos -> builder.addPiece(new EndlingVillagePieces.Glowcap(pos)));
            huts.forEach(builder::addPiece);
        }));
    }

    /** The Y of the top block of column (x, z), or below {@link #MIN_SURFACE_Y} over the void. */
    private static int surface(GenerationContext context, int x, int z) {
        return context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(), context.randomState());
    }
}
