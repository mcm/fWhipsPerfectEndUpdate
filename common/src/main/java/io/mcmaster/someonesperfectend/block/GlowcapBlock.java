package io.mcmaster.someonesperfectend.block;

import com.mojang.serialization.MapCodec;
import io.mcmaster.someonesperfectend.worldgen.ModFeatures;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A small glowing End mushroom. Unlike vanilla mushrooms it survives on End soil
 * ({@link ModBlockTags#END_PLANT_SOIL}) at any light level, does not spread, and bone meal grows it into
 * {@code someonesperfectend:huge_glowcap}.
 */
public class GlowcapBlock extends MushroomBlock {
    public static final MapCodec<GlowcapBlock> CODEC = simpleCodec(GlowcapBlock::new);

    public GlowcapBlock(Properties properties) {
        super(ModFeatures.HUGE_GLOWCAP_CONFIGURED, properties);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public MapCodec<MushroomBlock> codec() {
        // MushroomBlock#codec() is not declared with a wildcard, so the subclass codec needs a cast.
        return (MapCodec) CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(ModBlockTags.END_PLANT_SOIL);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return mayPlaceOn(level.getBlockState(below), level, below);
    }
}
