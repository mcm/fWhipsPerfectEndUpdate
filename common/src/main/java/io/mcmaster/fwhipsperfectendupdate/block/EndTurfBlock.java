package io.mcmaster.fwhipsperfectendupdate.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LightEngine;

/**
 * End stone with light-blue grass on top. Behaves like grass on dirt, but with end stone: it spreads onto
 * nearby end stone at light level 9 or more and turns back into end stone when covered.
 */
public class EndTurfBlock extends Block implements BonemealableBlock {
    public EndTurfBlock(Properties properties) {
        super(properties);
    }

    /** Same rule as vanilla grass: the block above must let light through (a single snow layer is fine). */
    public static boolean canStayTurf(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos abovePos = pos.above();
        BlockState above = level.getBlockState(abovePos);
        if (above.is(Blocks.SNOW) && above.getValue(SnowLayerBlock.LAYERS) == 1) {
            return true;
        }
        if (above.getFluidState().getAmount() == 8) {
            return false;
        }
        int lightBlock = LightEngine.getLightBlockInto(
                level, state, pos, above, abovePos, Direction.UP, above.getLightBlock(level, abovePos));
        return lightBlock < level.getMaxLightLevel();
    }

    private static boolean canSpreadTo(BlockState state, LevelReader level, BlockPos pos) {
        return canStayTurf(state, level, pos)
                && !level.getFluidState(pos.above()).is(FluidTags.WATER);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!canStayTurf(state, level, pos)) {
            level.setBlockAndUpdate(pos, Blocks.END_STONE.defaultBlockState());
            return;
        }
        if (level.getMaxLocalRawBrightness(pos.above()) < 9) {
            return;
        }
        BlockState turf = defaultBlockState();
        for (int i = 0; i < 4; i++) {
            BlockPos target = pos.offset(random.nextInt(3) - 1, random.nextInt(5) - 3, random.nextInt(3) - 1);
            if (level.getBlockState(target).is(Blocks.END_STONE) && canSpreadTo(turf, level, target)) {
                level.setBlockAndUpdate(target, turf);
            }
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return level.getBlockState(pos.above()).isAir();
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    /** Scatters Short Void Grass over nearby turf, like pre-1.17 grass bone meal. */
    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        BlockState grass = ModBlocks.SHORT_VOID_GRASS.get().defaultBlockState();
        BlockPos start = pos.above();
        attempts:
        for (int i = 0; i < 128; i++) {
            BlockPos target = start;
            for (int step = 0; step < i / 16; step++) {
                target = target.offset(
                        random.nextInt(3) - 1, (random.nextInt(3) - 1) * random.nextInt(3) / 2, random.nextInt(3) - 1);
                if (!level.getBlockState(target.below()).is(this)
                        || level.getBlockState(target).isCollisionShapeFullBlock(level, target)) {
                    continue attempts;
                }
            }
            if (level.getBlockState(target).isAir() && grass.canSurvive(level, target)) {
                level.setBlock(target, grass, Block.UPDATE_ALL);
            }
        }
    }
}
