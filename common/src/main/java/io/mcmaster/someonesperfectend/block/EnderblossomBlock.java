package io.mcmaster.someonesperfectend.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.mcmaster.someonesperfectend.effect.ModEffects;
import io.mcmaster.someonesperfectend.sound.ModSounds;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SuspiciousEffectHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A flower that grows out of the side of an Astral Log ({@link ModBlockTags#ASTRAL_LOGS}), backported and twisted
 * from 1.21.4's Eyeblossom. Like the Eyeblossom it comes as two blocks, one per {@link Type}. {@link #FACING} points
 * away from the supporting log; the blossom pops off when that log is gone. It opens while an Enderman is near
 * ({@link Enderblossoms}) and shakes when looked at or when an Enderman teleports nearby ({@link EnderblossomShaking}).
 * A block entity renderer draws it, so it can shake and the open blossom's eye can glow. In suspicious stew either
 * type gives {@code displacement}.
 */
public class EnderblossomBlock extends Block implements Enderblossoms.Blossom, SuspiciousEffectHolder, EntityBlock {
    public static final MapCodec<EnderblossomBlock> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(Type.CODEC.fieldOf("type").forGetter(EnderblossomBlock::type), propertiesCodec())
                    .apply(instance, EnderblossomBlock::new));
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    /** Outline around the stem and the head leaning out from the log, which is on the facing's opposite side. */
    private static final Map<Direction, VoxelShape> SHAPES = Map.of(
            Direction.NORTH, Block.box(3, 1, 4, 13, 15, 16),
            Direction.SOUTH, Block.box(3, 1, 0, 13, 15, 12),
            Direction.WEST, Block.box(4, 1, 3, 16, 15, 13),
            Direction.EAST, Block.box(0, 1, 3, 12, 15, 13));

    /**
     * How deep the model's stem base sits into the log's block, onto the face of its 8-pixel core: model z 20 for
     * facing north.
     */
    public static final float STEM_DEPTH = 4.0F / 16.0F;

    /** Stew duration in ticks; Displacement is instant, so it only needs one tick. */
    public static final int STEW_EFFECT_DURATION = 1;

    /** Like vanilla's Eyeblossom. */
    public static final int IDLE_SOUND_CHANCE = 700;

    private final Type type;

    public EnderblossomBlock(Type type, Properties properties) {
        super(properties);
        this.type = type;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<EnderblossomBlock> codec() {
        return CODEC;
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public SuspiciousStewEffects getSuspiciousEffects() {
        return new SuspiciousStewEffects(
                List.of(new SuspiciousStewEffects.Entry(ModEffects.displacement(), STEW_EFFECT_DURATION)));
    }

    @Override
    public BlockState withType(BlockState state, Type type) {
        return ModBlocks.enderblossom(type).defaultBlockState().setValue(FACING, state.getValue(FACING));
    }

    @Override
    public Vec3 headPosition(BlockState state, BlockPos pos) {
        return Enderblossoms.wallHead(state.getValue(FACING), pos);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        Enderblossoms.scheduleFirstCheck(level, pos, this);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Enderblossoms.regularCheck(state, level, pos, random);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnderblossomBlockEntity(pos, state);
    }

    /** On the server the block entity checks for staring players every tick. */
    @Override
    @SuppressWarnings("unchecked") // the type is checked against the blossom's block entity type
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide() && type == EnderblossomShaking.BLOCK_ENTITY.get()
                ? (BlockEntityTicker<T>)
                        (BlockEntityTicker<EnderblossomBlockEntity>) EnderblossomBlockEntity::serverTick
                : null;
    }

    /** The block entity renderer draws the model, so it can shake. */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        super.triggerEvent(state, level, pos, id, param);
        BlockEntity entity = level.getBlockEntity(pos);
        return entity != null && entity.triggerEvent(id, param);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        playIdleSound(type, level, pos, random);
    }

    /** Client side: an open blossom has a 1 in {@value #IDLE_SOUND_CHANCE} chance per animate tick to play idle. */
    static void playIdleSound(Type type, Level level, BlockPos pos, RandomSource random) {
        if (type.open() && random.nextInt(IDLE_SOUND_CHANCE) == 0) {
            level.playLocalSound(pos, ModSounds.ENDERBLOSSOM_IDLE.get(), SoundSource.BLOCKS, 1.0F, 1.0F, false);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    /** Only a click on a horizontal face of an Astral log places it, facing away from that log. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        if (!face.getAxis().isHorizontal()) {
            return null;
        }
        BlockState state = defaultBlockState().setValue(FACING, face);
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return isSupport(level.getBlockState(supportPos(state, pos)));
    }

    public static boolean isSupport(BlockState state) {
        return state.is(ModBlockTags.ASTRAL_LOGS);
    }

    /**
     * How far, in blocks, to push the drawn blossom out of the log's block. The model sets the stem base on a chorus
     * plant-shaped {@link AstralLogBlock}'s core, 4 pixels into the log's block; a support with a face at the block
     * edge (the Flowering Astral Log, wood and stripped logs) would swallow the stem, so there it moves out to the face.
     */
    public static float stemOffset(BlockState support) {
        return support.getBlock() instanceof AstralLogBlock ? 0.0F : STEM_DEPTH;
    }

    /** The position of the log this blossom grows from. */
    public static BlockPos supportPos(BlockState state, BlockPos pos) {
        return pos.relative(state.getValue(FACING).getOpposite());
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos) {
        // Returning air makes vanilla destroy the block with drops, so it pops off.
        if (direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    /** Open or closed, like vanilla's {@code EyeblossomBlock.Type}; each type is its own block. */
    public enum Type implements StringRepresentable {
        OPEN(true),
        CLOSED(false);

        public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

        private final boolean open;

        Type(boolean open) {
            this.open = open;
        }

        public boolean open() {
            return open;
        }

        @Override
        public String getSerializedName() {
            return open ? "open" : "closed";
        }
    }
}
