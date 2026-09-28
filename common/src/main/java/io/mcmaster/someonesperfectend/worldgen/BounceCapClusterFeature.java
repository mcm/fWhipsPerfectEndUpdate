package io.mcmaster.someonesperfectend.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * A small cluster of bounce caps on an island surface. Several positions around the placement origin are tried
 * and the one closest to an island edge (most nearby columns that drop away) wins, so clusters favor edges, where
 * they work as launch points to other islands. Search and cluster radii together stay within 8 blocks of the
 * origin, inside the region a feature may write to.
 */
public class BounceCapClusterFeature extends Feature<BounceCapClusterConfiguration> {
    /** Distance of the columns checked around a candidate to score how close it is to an edge. */
    static final int EDGE_PROBE = 3;
    /** A probed column whose surface is this far below the candidate counts as a drop. */
    static final int DROP = 4;

    public BounceCapClusterFeature(Codec<BounceCapClusterConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<BounceCapClusterConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BounceCapClusterConfiguration config = context.config();
        BlockPos origin = context.origin();

        BlockPos best = null;
        int bestScore = -1;
        for (int i = 0; i < config.candidates(); i++) {
            int dx = i == 0 ? 0 : random.nextInt(config.searchRadius() * 2 + 1) - config.searchRadius();
            int dz = i == 0 ? 0 : random.nextInt(config.searchRadius() * 2 + 1) - config.searchRadius();
            BlockPos candidate = surface(level, origin.getX() + dx, origin.getZ() + dz);
            if (!canPlace(level, config.state().getState(random, candidate), candidate)) {
                continue;
            }
            int score = edgeScore(level, candidate);
            if (score > bestScore) {
                best = candidate;
                bestScore = score;
            }
        }
        if (best == null) {
            return false;
        }

        int size = config.size().sample(random);
        int placed = 0;
        for (int i = 0; i < size * 4 && placed < size; i++) {
            int r = config.clusterRadius();
            BlockPos pos = i == 0 ? best : surface(level, best.getX() + random.nextInt(r * 2 + 1) - r,
                    best.getZ() + random.nextInt(r * 2 + 1) - r);
            BlockState state = config.state().getState(random, pos);
            if (Math.abs(pos.getY() - best.getY()) <= 1 && canPlace(level, state, pos)) {
                level.setBlock(pos, state, Block.UPDATE_CLIENTS);
                placed++;
            }
        }
        return placed > 0;
    }

    private static BlockPos surface(WorldGenLevel level, int x, int z) {
        return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z);
    }

    private static boolean canPlace(WorldGenLevel level, BlockState state, BlockPos pos) {
        return pos.getY() > level.getMinBuildHeight() && level.isEmptyBlock(pos) && state.canSurvive(level, pos);
    }

    /** Number of the 8 surrounding columns, {@link #EDGE_PROBE} blocks away, whose surface drops away or is void. */
    static int edgeScore(WorldGenLevel level, BlockPos pos) {
        int score = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if ((dx != 0 || dz != 0) && level.getHeight(Heightmap.Types.MOTION_BLOCKING,
                        pos.getX() + dx * EDGE_PROBE, pos.getZ() + dz * EDGE_PROBE) < pos.getY() - DROP) {
                    score++;
                }
            }
        }
        return score;
    }
}
