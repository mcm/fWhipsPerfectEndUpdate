package io.mcmaster.someonesperfectend.worldgen;

import com.mojang.serialization.Codec;
import io.mcmaster.someonesperfectend.block.AstralLogBlock;
import io.mcmaster.someonesperfectend.block.EnderblossomBlock;
import io.mcmaster.someonesperfectend.block.Enderblossoms;
import io.mcmaster.someonesperfectend.block.ModBlockTags;
import io.mcmaster.someonesperfectend.block.ModBlocks;
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
import net.minecraft.world.level.ChunkPos;
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
 * Nothing is placed if any log position is blocked, or, with {@link AstralTreeConfiguration#maxOverhang()}, if any
 * log or leaf would stick out further than that past the island edge. Each log independently becomes a Flowering
 * Astral Log (same axis) with {@link AstralTreeConfiguration#floweringLogChance()}. Astral Logs are then connected
 * to their neighbors ({@link AstralLogBlock#connect}), since neighbor shape updates are skipped while placing.
 * Finally {@link AstralTreeConfiguration#enderblossoms()} Closed Enderblossoms go on random free trunk sides.
 */
public class AstralTreeFeature extends Feature<AstralTreeConfiguration> {
    /** Leaves are placed within this distance of a branch tip. */
    public static final int CLUSTER_RADIUS = 2;
    /** Enderblossoms grow on trunk logs at least this many blocks above the base. */
    public static final int ENDERBLOSSOM_MIN_HEIGHT = 2;
    /** A column counts as island ground if it has a solid block at most this far below the tree's base. */
    public static final int GROUND_DEPTH = 8;

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
        List<Direction> directions =
                new ArrayList<>(Direction.Plane.HORIZONTAL.stream().toList());
        Util.shuffle(directions, random);
        int branches = Math.min(config.branchCount().sample(random), directions.size());
        int lowest = origin.getY() + Math.max(3, height / 3);
        List<BlockPos> starts = trunk.stream()
                .filter(p -> p.getY() >= lowest && p.getY() <= topY - 2)
                .toList();
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
            for (BlockPos pos : BlockPos.betweenClosed(
                    center.offset(-CLUSTER_RADIUS, -CLUSTER_RADIUS, -CLUSTER_RADIUS),
                    center.offset(CLUSTER_RADIUS, CLUSTER_RADIUS, CLUSTER_RADIUS))) {
                int dx = pos.getX() - center.getX();
                int dy = pos.getY() - center.getY();
                int dz = pos.getZ() - center.getZ();
                int distanceSq = dx * dx + dy * dy + dz * dz;
                if ((distanceSq <= 4 || distanceSq == 5 && random.nextBoolean())
                        && !logs.containsKey(pos)
                        && isFree(level, pos)) {
                    leaves.add(pos.immutable());
                }
            }
        }

        Map<BlockPos, Integer> distances = leafDistances(logs.keySet(), leaves);
        if (config.maxOverhang().isPresent()
                && overhangs(
                        level,
                        origin,
                        topY,
                        logs.keySet(),
                        distances.keySet(),
                        config.maxOverhang().get())) {
            return false;
        }

        for (Map.Entry<BlockPos, Direction.Axis> log : logs.entrySet()) {
            BlockState state = random.nextFloat() < config.floweringLogChance()
                    ? ModBlocks.FLOWERING_ASTRAL_LOG.get().defaultBlockState()
                    : config.trunkProvider().getState(random, log.getKey());
            if (state.hasProperty(BlockStateProperties.AXIS)) {
                state = state.setValue(BlockStateProperties.AXIS, log.getValue());
            }
            level.setBlock(log.getKey(), state, Block.UPDATE_ALL | Block.UPDATE_KNOWN_SHAPE);
        }
        // Chorus Plant-shaped Astral Logs reach towards each other once they are all placed.
        for (BlockPos pos : logs.keySet()) {
            BlockState state = level.getBlockState(pos);
            BlockState connected = AstralLogBlock.connect(state, level, pos);
            if (connected != state) {
                level.setBlock(pos, connected, Block.UPDATE_ALL | Block.UPDATE_KNOWN_SHAPE);
            }
        }
        for (Map.Entry<BlockPos, Integer> leaf : distances.entrySet()) {
            BlockPos pos = leaf.getKey();
            BlockState state = config.foliageProvider().getState(random, pos);
            if (state.hasProperty(LeavesBlock.DISTANCE)) {
                state = state.setValue(LeavesBlock.DISTANCE, leaf.getValue());
            }
            if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
                state = state.setValue(
                        BlockStateProperties.WATERLOGGED,
                        level.getFluidState(pos).isSourceOfType(Fluids.WATER));
            }
            level.setBlock(pos, state, Block.UPDATE_ALL | Block.UPDATE_KNOWN_SHAPE);
        }
        placeEnderblossoms(level, random, config.enderblossoms().sample(random), origin, trunk);
        return true;
    }

    /**
     * Places up to {@code count} Closed Enderblossoms on random horizontal faces of the trunk logs at least
     * {@link #ENDERBLOSSOM_MIN_HEIGHT} above the base, facing away from the trunk, only into air, and schedules
     * each one's first check (worldgen placement doesn't call {@code onPlace}).
     */
    private static void placeEnderblossoms(
            WorldGenLevel level, RandomSource random, int count, BlockPos origin, List<BlockPos> trunk) {
        List<BlockPos> logs = new ArrayList<>();
        List<Direction> faces = new ArrayList<>();
        for (BlockPos log : trunk) {
            if (log.getY() >= origin.getY() + ENDERBLOSSOM_MIN_HEIGHT) {
                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    logs.add(log);
                    faces.add(direction);
                }
            }
        }
        Block block = ModBlocks.CLOSED_ENDERBLOSSOM.get();
        int placed = 0;
        while (placed < count && !logs.isEmpty()) {
            int i = random.nextInt(logs.size());
            BlockPos log = logs.remove(i);
            Direction face = faces.remove(i);
            BlockPos pos = log.relative(face);
            BlockState state = block.defaultBlockState().setValue(EnderblossomBlock.FACING, face);
            if (level.isEmptyBlock(pos) && state.canSurvive(level, pos)) {
                level.setBlock(pos, state, Block.UPDATE_ALL);
                Enderblossoms.scheduleFirstCheck(level, pos, block);
                placed++;
            }
        }
    }

    private static boolean isFree(WorldGenLevel level, BlockPos pos) {
        return level.isStateAtPosition(pos, state -> state.isAir() || state.is(BlockTags.REPLACEABLE_BY_TREES));
    }

    /**
     * Whether any tree column is more than {@code maxOverhang} blocks (horizontally, in both axes) away from every
     * ground column, i.e. whether the tree would stick out past the island edge by more than that.
     */
    private static boolean overhangs(
            WorldGenLevel level, BlockPos origin, int topY, Set<BlockPos> logs, Set<BlockPos> leaves, int maxOverhang) {
        Set<Long> columns = new LinkedHashSet<>();
        logs.forEach(pos -> columns.add(ChunkPos.asLong(pos.getX(), pos.getZ())));
        leaves.forEach(pos -> columns.add(ChunkPos.asLong(pos.getX(), pos.getZ())));
        Map<Long, Boolean> ground = new HashMap<>();
        for (long column : columns) {
            int x = ChunkPos.getX(column);
            int z = ChunkPos.getZ(column);
            boolean supported = false;
            for (int dx = -maxOverhang; dx <= maxOverhang && !supported; dx++) {
                for (int dz = -maxOverhang; dz <= maxOverhang && !supported; dz++) {
                    int gx = x + dx;
                    int gz = z + dz;
                    supported = ground.computeIfAbsent(
                            ChunkPos.asLong(gx, gz),
                            key -> isGround(level, gx, gz, origin.getY() - GROUND_DEPTH, topY));
                }
            }
            if (!supported) {
                return true;
            }
        }
        return false;
    }

    /** Whether the column has an island block (not a plant, fluid, log, or leaf) between the two heights. */
    private static boolean isGround(WorldGenLevel level, int x, int z, int minY, int maxY) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = Math.max(minY, level.getMinBuildHeight()); y <= maxY; y++) {
            pos.set(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (!state.canBeReplaced() && !state.is(BlockTags.LOGS) && !state.is(BlockTags.LEAVES)) {
                return true;
            }
        }
        return false;
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
