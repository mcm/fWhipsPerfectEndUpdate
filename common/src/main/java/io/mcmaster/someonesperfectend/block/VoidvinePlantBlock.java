package io.mcmaster.someonesperfectend.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GrowingPlantBodyBlock;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

/** A non-growing Voidvine segment above the head. Regrows Voidfruit on random ticks. */
public class VoidvinePlantBlock extends GrowingPlantBodyBlock {
    public static final MapCodec<VoidvinePlantBlock> CODEC = simpleCodec(VoidvinePlantBlock::new);

    public VoidvinePlantBlock(Properties properties) {
        super(properties, Direction.DOWN, Voidvines.SHAPE, false);
        registerDefaultState(stateDefinition.any().setValue(Voidvines.FRUIT, false));
    }

    @Override
    protected MapCodec<VoidvinePlantBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(Voidvines.FRUIT);
    }

    @Override
    protected boolean canAttachTo(BlockState state) {
        return Voidvines.isVine(state) || state.is(ModBlockTags.SUPPORTS_HANGING_PLANTS);
    }

    @Override
    protected GrowingPlantHeadBlock getHeadBlock() {
        return ModBlocks.VOIDVINE_HEAD.get();
    }

    @Override
    protected BlockState updateHeadAfterConvertedFromBody(BlockState body, BlockState head) {
        return head.setValue(Voidvines.FRUIT, body.getValue(Voidvines.FRUIT));
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Voidvines.tickFruit(state, level, pos, random);
    }

    /** Bone meal on any segment grows the head, but only while the vine is shorter than the cap. */
    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        BlockPos head = pos;
        while (level.getBlockState(head).is(this)) {
            head = head.below();
        }
        BlockState headState = level.getBlockState(head);
        return headState.is(getHeadBlock()) && getHeadBlock().isValidBonemealTarget(level, head, headState);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        return Voidvines.harvest(player, state, level, pos);
    }
}
