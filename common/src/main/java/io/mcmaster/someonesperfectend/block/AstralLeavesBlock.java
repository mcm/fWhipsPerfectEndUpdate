package io.mcmaster.someonesperfectend.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

/**
 * Astral leaves decay like vanilla leaves, but only {@link ModBlockTags#ASTRAL_LOGS} keep them alive (vanilla
 * leaves count any {@code #minecraft:logs}). Vanilla's distance helpers are private, so the three methods that
 * use them are reimplemented here with the same logic.
 */
public class AstralLeavesBlock extends LeavesBlock {
    public static final MapCodec<AstralLeavesBlock> CODEC = simpleCodec(AstralLeavesBlock::new);

    public AstralLeavesBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends LeavesBlock> codec() {
        return CODEC;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.setBlock(pos, updateDistance(state, level, pos), Block.UPDATE_ALL);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        int distance = distanceAt(neighborState) + 1;
        if (distance != 1 || state.getValue(DISTANCE) != distance) {
            level.scheduleTick(pos, this, 1);
        }
        return state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : updateDistance(state, context.getLevel(), context.getClickedPos());
    }

    private static BlockState updateDistance(BlockState state, LevelAccessor level, BlockPos pos) {
        int distance = DECAY_DISTANCE;
        BlockPos.MutableBlockPos neighbor = new BlockPos.MutableBlockPos();
        for (Direction direction : Direction.values()) {
            neighbor.setWithOffset(pos, direction);
            distance = Math.min(distance, distanceAt(level.getBlockState(neighbor)) + 1);
            if (distance == 1) {
                break;
            }
        }
        return state.setValue(DISTANCE, distance);
    }

    /** 0 for an Astral log, the neighbor's own distance for any leaves, otherwise {@link #DECAY_DISTANCE}. */
    public static int distanceAt(BlockState state) {
        if (state.is(ModBlockTags.ASTRAL_LOGS)) {
            return 0;
        }
        return state.hasProperty(DISTANCE) ? state.getValue(DISTANCE) : DECAY_DISTANCE;
    }
}
