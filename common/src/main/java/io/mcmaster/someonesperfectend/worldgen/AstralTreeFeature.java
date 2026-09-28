package io.mcmaster.someonesperfectend.worldgen;

import com.mojang.serialization.Codec;
import io.mcmaster.someonesperfectend.block.ModBlockTags;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.material.Fluids;

/**
 * A scaled-up chorus plant: a straight vertical stem with side branches in distinct directions that reach out two
 * or three blocks and then turn upward (the elbowed segments of a chorus plant), and a leaf cluster at the tip of the
 * stem and of every branch. The trunk starts at the origin, which must sit on {@link ModBlockTags#END_PLANT_SOIL}.
 * Nothing is placed if any log position is blocked.
 */
public class AstralTreeFeature extends Feature<AstralTreeConfiguration> {
    /** Leaves are placed within this distance of a branch tip. */
    public static final int CLUSTER_RADIUS = 2;

    public AstralTreeFeature(Codec<AstralTreeConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<AstralTreeConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        AstralTreeConfiguration config = context.config();
        if (!level.getBlockState(origin.below()).is(ModBlockTags.END_PLANT_SOIL)) {
            return false;
        }
        int height = config.height().sample(random);
        int topY = origin.getY() + height - 1;
        if (topY + CLUSTER_RADIUS + 1 >= level.getMaxBuildHeight()) {
            return false;
        }

        Map<BlockPos, Direction.Axis> logs = new LinkedHashMap<>();
        List<BlockPos> trunk = new ArrayList<>();
        List<BlockPos> tips = new ArrayList<>();

        // Trunk: a straight stem, like a chorus plant's.
        for (int y = origin.getY(); y <= topY; y++) {
            BlockPos pos = new BlockPos(origin.getX(), y, origin.getZ());
            logs.put(pos, Direction.Axis.Y);
            trunk.add(pos);
        }
        tips.add(trunk.get(trunk.size() - 1));

        // Branches: out 2-3 blocks in distinct directions, then up 2-3 blocks (never above the trunk top).
        List<Direction> directions = new ArrayList<>(Direction.Plane.HORIZONTAL.stream().toList());
        Util.shuffle(directions, random);
        int branches = Math.min(config.branchCount().sample(random), directions.size());
        int lowest = origin.getY() + Math.max(3, height / 3);
        List<BlockPos> starts = trunk.stream().filter(p -> p.getY() >= lowest && p.getY() <= topY - 2).toList();
        for (int i = 0; i < branches && !starts.isEmpty(); i++) {
            Direction direction = directions.get(i);
            BlockPos pos = starts.get(random.nextInt(starts.size()));
            int out = 2 + random.nextInt(2);
            for (int j = 0; j < out; j++) {
                pos = pos.relative(direction);
                logs.put(pos, direction.getAxis());
            }
            int up = Math.min(2 + random.nextInt(2), topY - 1 - pos.getY());
            for (int j = 0; j < up; j++) {
                pos = pos.above();
                logs.put(pos, Direction.Axis.Y);
            }
            tips.add(pos);
        }

        for (BlockPos pos : logs.keySet()) {
            if (!isFree(level, pos)) {
                return false;
            }
        }

        Set<BlockPos> leaves = new LinkedHashSet<>();
        for (BlockPos tip : tips) {
            BlockPos center = tip.above();
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-CLUSTER_RADIUS, -CLUSTER_RADIUS, -CLUSTER_RADIUS),
                    center.offset(CLUSTER_RADIUS, CLUSTER_RADIUS, CLUSTER_RADIUS))) {
                int dx = pos.getX() - center.getX();
                int dy = pos.getY() - center.getY();
                int dz = pos.getZ() - center.getZ();
                int distanceSq = dx * dx + dy * dy + dz * dz;
                if ((distanceSq <= 4 || distanceSq == 5 && random.nextBoolean())
                        && !logs.containsKey(pos) && isFree(level, pos)) {
                    leaves.add(pos.immutable());
                }
            }
        }

        for (Map.Entry<BlockPos, Direction.Axis> log : logs.entrySet()) {
            BlockState state = config.trunkProvider().getState(random, log.getKey());
            if (state.hasProperty(BlockStateProperties.AXIS)) {
                state = state.setValue(BlockStateProperties.AXIS, log.getValue());
            }
            level.setBlock(log.getKey(), state, Block.UPDATE_ALL | Block.UPDATE_KNOWN_SHAPE);
        }
        Map<BlockPos, Integer> distances = leafDistances(logs.keySet(), leaves);
        for (Map.Entry<BlockPos, Integer> leaf : distances.entrySet()) {
            BlockPos pos = leaf.getKey();
            BlockState state = config.foliageProvider().getState(random, pos);
            if (state.hasProperty(LeavesBlock.DISTANCE)) {
                state = state.setValue(LeavesBlock.DISTANCE, leaf.getValue());
            }
            if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
                state = state.setValue(BlockStateProperties.WATERLOGGED,
                        level.getFluidState(pos).isSourceOfType(Fluids.WATER));
            }
            level.setBlock(pos, state, Block.UPDATE_ALL | Block.UPDATE_KNOWN_SHAPE);
        }
        return true;
    }

    private static boolean isFree(WorldGenLevel level, BlockPos pos) {
        return level.isStateAtPosition(pos, state -> state.isAir() || state.is(BlockTags.REPLACEABLE_BY_TREES));
    }

    /** Leaf distance to the nearest log (breadth-first through leaves); leaves farther than 6 are left out. */
    private static Map<BlockPos, Integer> leafDistances(Set<BlockPos> logs, Set<BlockPos> leaves) {
        Map<BlockPos, Integer> distances = new HashMap<>();
        Deque<BlockPos> queue = new ArrayDeque<>(logs);
        Map<BlockPos, Integer> depth = new HashMap<>();
        logs.forEach(log -> depth.put(log, 0));
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            int next = depth.get(pos) + 1;
            if (next >= LeavesBlock.DECAY_DISTANCE) {
                continue;
            }
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = pos.relative(direction);
                if (leaves.contains(neighbor) && !depth.containsKey(neighbor)) {
                    depth.put(neighbor, next);
                    distances.put(neighbor, next);
                    queue.add(neighbor);
                }
            }
        }
        return distances;
    }
}
