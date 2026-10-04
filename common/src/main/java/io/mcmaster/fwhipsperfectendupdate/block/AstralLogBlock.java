package io.mcmaster.fwhipsperfectendupdate.block;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Astral Log has the shape of a chorus plant: a stem core with an arm towards each neighboring Astral Log or
 * Flowering Astral Log, and down into {@link ModBlockTags#END_PLANT_SOIL}. It keeps the log {@link #AXIS} so axes
 * strip it into a Stripped Astral Log facing the same way, but the axis does not change how it looks.
 */
public class AstralLogBlock extends RotatedPillarBlock {
    public static final MapCodec<AstralLogBlock> CODEC = simpleCodec(AstralLogBlock::new);
    public static final Map<Direction, BooleanProperty> CONNECTIONS = PipeBlock.PROPERTY_BY_DIRECTION;

    /** Same size as vanilla's chorus plant ({@code PipeBlock} apothem 0.3125, i.e. a 10-pixel core). */
    private static final double MIN = 3.0;

    private static final double MAX = 13.0;
    private static final VoxelShape CORE = Block.box(MIN, MIN, MIN, MAX, MAX, MAX);
    private static final Map<Direction, VoxelShape> ARMS = Map.of(
            Direction.NORTH, Block.box(MIN, MIN, 0, MAX, MAX, MIN),
            Direction.SOUTH, Block.box(MIN, MIN, MAX, MAX, MAX, 16),
            Direction.WEST, Block.box(0, MIN, MIN, MIN, MAX, MAX),
            Direction.EAST, Block.box(MAX, MIN, MIN, 16, MAX, MAX),
            Direction.DOWN, Block.box(MIN, 0, MIN, MAX, MIN, MAX),
            Direction.UP, Block.box(MIN, MAX, MIN, MAX, 16, MAX));
    /** Shape for each set of connections, indexed by a bit per {@link Direction#get3DDataValue()}. */
    private static final VoxelShape[] SHAPES = new VoxelShape[1 << 6];

    static {
        for (int mask = 0; mask < SHAPES.length; mask++) {
            VoxelShape shape = CORE;
            for (Direction direction : Direction.values()) {
                if ((mask & 1 << direction.get3DDataValue()) != 0) {
                    shape = Shapes.or(shape, ARMS.get(direction));
                }
            }
            SHAPES[mask] = shape.optimize();
        }
    }

    public AstralLogBlock(Properties properties) {
        super(properties);
        BlockState state = defaultBlockState();
        for (BooleanProperty connection : CONNECTIONS.values()) {
            state = state.setValue(connection, false);
        }
        registerDefaultState(state);
    }

    @Override
    public MapCodec<? extends RotatedPillarBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        CONNECTIONS.values().forEach(builder::add);
    }

    /** Sets every connection of an Astral Log state from its neighbors; other states are returned unchanged. */
    public static BlockState connect(BlockState state, BlockGetter level, BlockPos pos) {
        if (!(state.getBlock() instanceof AstralLogBlock)) {
            return state;
        }
        for (Direction direction : Direction.values()) {
            state = state.setValue(
                    CONNECTIONS.get(direction), connectsTo(direction, level.getBlockState(pos.relative(direction))));
        }
        return state;
    }

    /** Whether an Astral Log reaches towards a neighbor in that direction. */
    public static boolean connectsTo(Direction direction, BlockState neighbor) {
        return neighbor.is(ModBlocks.ASTRAL_LOG.get())
                || neighbor.is(ModBlocks.FLOWERING_ASTRAL_LOG.get())
                || direction == Direction.DOWN && neighbor.is(ModBlockTags.END_PLANT_SOIL);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return connect(super.getStateForPlacement(context), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos) {
        return state.setValue(CONNECTIONS.get(direction), connectsTo(direction, neighborState));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int mask = 0;
        for (Direction direction : Direction.values()) {
            if (state.getValue(CONNECTIONS.get(direction))) {
                mask |= 1 << direction.get3DDataValue();
            }
        }
        return SHAPES[mask];
    }
}
