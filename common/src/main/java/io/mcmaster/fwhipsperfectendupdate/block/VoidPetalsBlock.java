package io.mcmaster.fwhipsperfectendupdate.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PinkPetalsBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Pale blue End ground cover: 1-4 petals per block like Pink Petals, placeable on End soil
 * ({@link ModBlockTags#END_PLANT_SOIL}) and on any vanilla dirt-like block.
 */
public class VoidPetalsBlock extends PinkPetalsBlock {
    public static final MapCodec<VoidPetalsBlock> CODEC = simpleCodec(VoidPetalsBlock::new);

    public VoidPetalsBlock(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public MapCodec<PinkPetalsBlock> codec() {
        // PinkPetalsBlock#codec() is not declared with a wildcard, so the subclass codec needs a cast.
        return (MapCodec) CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(ModBlockTags.END_PLANT_SOIL) || state.is(BlockTags.DIRT) || state.is(Blocks.FARMLAND);
    }
}
