package io.mcmaster.fwhipsperfectendupdate.worldgen;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * A small pool sunk into an island's surface, with a ground-cover carpet around it. The pool's top level replaces
 * the surface block at the origin, and it gets deeper toward its center. Every fluid block must be enclosed below
 * and on all four sides by solid blocks (or other pool blocks), so the pool can never pour off an island edge or
 * out of its underside; if the pool doesn't fit, a smaller one is tried, and failing that nothing is placed.
 * The pool plus carpet stay within 7 blocks of the origin, inside the region a feature may write to.
 */
public class OasisPoolFeature extends Feature<OasisPoolConfiguration> {
    private static final Direction[] ENCLOSING = {
        Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    public OasisPoolFeature(Codec<OasisPoolConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<OasisPoolConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        OasisPoolConfiguration config = context.config();
        // The origin is the first free block above the surface (a MOTION_BLOCKING heightmap position).
        BlockPos surface = context.origin().below();
        if (surface.getY() - config.maxDepth() <= level.getMinBuildHeight() || !isSurface(level, surface)) {
            return false;
        }
        for (int radius = config.radius().sample(random); radius >= 1; radius--) {
            Map<BlockPos, Integer> columns = shape(level, random, surface, radius, config.maxDepth());
            Set<BlockPos> cells = new HashSet<>();
            columns.forEach((top, depth) -> {
                for (int dy = 0; dy < depth; dy++) {
                    cells.add(top.below(dy));
                }
            });
            if (columns.containsKey(surface) && enclosed(level, cells)) {
                fill(level, config, random, cells, columns);
                carpet(level, config, random, surface, radius, columns);
                return true;
            }
        }
        return false;
    }

    /** Pool columns (top block → depth): a jittered ellipse, keeping only columns whose surface is at pool level. */
    private static Map<BlockPos, Integer> shape(
            WorldGenLevel level, RandomSource random, BlockPos surface, int radius, int maxDepth) {
        float rx = radius + random.nextFloat() * 0.8F;
        float rz = radius + random.nextFloat() * 0.8F;
        Map<BlockPos, Integer> columns = new HashMap<>();
        for (int dx = -radius - 1; dx <= radius + 1; dx++) {
            for (int dz = -radius - 1; dz <= radius + 1; dz++) {
                float distance = (dx * dx) / (rx * rx) + (dz * dz) / (rz * rz);
                BlockPos top = surface.offset(dx, 0, dz);
                if (distance <= 1.0F && isSurface(level, top)) {
                    int depth = Math.max(1, Math.round(maxDepth * (1.0F - (float) Math.sqrt(distance))));
                    columns.put(top, depth);
                }
            }
        }
        return columns;
    }

    /** A solid block with nothing solid on top of it. */
    private static boolean isSurface(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockState above = level.getBlockState(pos.above());
        return state.isSolid()
                && state.getFluidState().isEmpty()
                && !above.isSolid()
                && above.getFluidState().isEmpty();
    }

    /** Whether every cell has a solid block or another cell below it and on each side. */
    static boolean enclosed(WorldGenLevel level, Set<BlockPos> cells) {
        for (BlockPos cell : cells) {
            for (Direction direction : ENCLOSING) {
                BlockPos neighbor = cell.relative(direction);
                if (!cells.contains(neighbor) && !level.getBlockState(neighbor).isSolid()) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void fill(
            WorldGenLevel level,
            OasisPoolConfiguration config,
            RandomSource random,
            Set<BlockPos> cells,
            Map<BlockPos, Integer> columns) {
        for (BlockPos cell : cells) {
            level.setBlock(cell, config.fluid().getState(random, cell), Block.UPDATE_CLIENTS);
        }
        // Plants standing on the old surface would float on the water.
        for (BlockPos top : columns.keySet()) {
            BlockPos above = top.above();
            if (!level.isEmptyBlock(above) && level.getBlockState(above).canBeReplaced()) {
                level.setBlock(above, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    private static void carpet(
            WorldGenLevel level,
            OasisPoolConfiguration config,
            RandomSource random,
            BlockPos surface,
            int radius,
            Map<BlockPos, Integer> columns) {
        int reach = radius + 1 + config.carpetRadius();
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                if (random.nextFloat() >= config.carpetChance()) {
                    continue;
                }
                // Follow the surface up or down a block around the pool.
                for (int dy = 1; dy >= -1; dy--) {
                    BlockPos pos = surface.offset(dx, dy + 1, dz);
                    if (columns.containsKey(pos.below())
                            || !level.getBlockState(pos).isAir()) {
                        continue;
                    }
                    BlockState state = config.carpet().getState(random, pos);
                    if (state.canSurvive(level, pos)) {
                        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
                        break;
                    }
                }
            }
        }
    }
}
