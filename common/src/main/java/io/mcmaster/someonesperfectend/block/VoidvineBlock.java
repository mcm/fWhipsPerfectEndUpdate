package io.mcmaster.someonesperfectend.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The growing (lowest) segment of a Voidvine. It hangs below Voidsoil or another segment, grows one block down
 * on about 10% of random ticks up to {@link Voidvines#MAX_LENGTH} segments, and bone meal adds 1-3 segments.
 */
public class VoidvineBlock extends GrowingPlantHeadBlock {
    public static final MapCodec<VoidvineBlock> CODEC = simpleCodec(VoidvineBlock::new);

    public VoidvineBlock(Properties properties) {
        super(properties, Direction.DOWN, Voidvines.SHAPE, false, Voidvines.GROW_CHANCE);
        registerDefaultState(stateDefinition.any().setValue(AGE, 0).setValue(Voidvines.FRUIT, false));
    }

    @Override
    protected MapCodec<VoidvineBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(Voidvines.FRUIT);
    }

    @Override
    public BlockState getStateForPlacement(LevelAccessor level) {
        return defaultBlockState();
    }

    @Override
    protected boolean canAttachTo(BlockState state) {
        return Voidvines.isVine(state) || state.is(ModBlockTags.SUPPORTS_HANGING_PLANTS);
    }

    @Override
    protected boolean canGrowInto(BlockState state) {
        return state.isAir();
    }

    @Override
    protected Block getBodyBlock() {
        return ModBlocks.VOIDVINE_BODY.get();
    }

    @Override
    protected BlockState updateBodyAfterConvertedFromHead(BlockState head, BlockState body) {
        return body.setValue(Voidvines.FRUIT, head.getValue(Voidvines.FRUIT));
    }

    @Override
    protected BlockState getGrowIntoState(BlockState state, RandomSource random) {
        return defaultBlockState().setValue(Voidvines.FRUIT, random.nextFloat() < Voidvines.NEW_SEGMENT_FRUIT_CHANCE);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    /** Grows (ignoring vanilla's age limit, the length cap replaces it) or, failing that, may regrow fruit. */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos below = pos.below();
        if (canGrow(level, pos) && random.nextDouble() < Voidvines.GROW_CHANCE) {
            level.setBlockAndUpdate(below, getGrowIntoState(state, random));
        } else {
            Voidvines.tickFruit(state, level, pos, random);
        }
    }

    private boolean canGrow(LevelReader level, BlockPos pos) {
        return canGrowInto(level.getBlockState(pos.below()))
                && Voidvines.lengthAbove(level, pos) < Voidvines.MAX_LENGTH
                && pos.getY() - 1 >= level.getMinBuildHeight() + Voidvines.MIN_HEIGHT_ABOVE_BOTTOM;
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return canGrow(level, pos);
    }

    @Override
    protected int getBlocksToGrowWhenBonemealed(RandomSource random) {
        return 1 + random.nextInt(3);
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        int segments = Math.min(
                getBlocksToGrowWhenBonemealed(random), Voidvines.MAX_LENGTH - Voidvines.lengthAbove(level, pos));
        BlockPos head = pos;
        for (int i = 0; i < segments && canGrow(level, head); i++) {
            head = head.below();
            level.setBlockAndUpdate(head, getGrowIntoState(state, random));
        }
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return Voidvines.harvest(player, state, level, pos);
    }
}
