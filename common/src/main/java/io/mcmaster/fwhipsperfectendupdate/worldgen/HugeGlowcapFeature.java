package io.mcmaster.fwhipsperfectendupdate.worldgen;

import com.mojang.serialization.Codec;
import io.mcmaster.fwhipsperfectendupdate.block.ModBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.HugeRedMushroomFeature;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;

/**
 * A huge red-mushroom-shaped glowcap, 5-9 blocks tall (a 4-8 block stem plus the cap's top layer),
 * growing on End soil instead of dirt or mycelium.
 */
public class HugeGlowcapFeature extends HugeRedMushroomFeature {
    public static final int MIN_STEM_HEIGHT = 4;
    public static final int MAX_STEM_HEIGHT = 8;

    public HugeGlowcapFeature(Codec<HugeMushroomFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    protected int getTreeHeight(RandomSource random) {
        return MIN_STEM_HEIGHT + random.nextInt(MAX_STEM_HEIGHT - MIN_STEM_HEIGHT + 1);
    }

    /** Vanilla's check, with End soil as the only valid ground. */
    @Override
    protected boolean isValidPosition(
            LevelAccessor level,
            BlockPos pos,
            int height,
            BlockPos.MutableBlockPos mutable,
            HugeMushroomFeatureConfiguration config) {
        int y = pos.getY();
        if (y < level.getMinBuildHeight() + 1 || y + height + 1 >= level.getMaxBuildHeight()) {
            return false;
        }
        if (!level.getBlockState(pos.below()).is(ModBlockTags.END_PLANT_SOIL)) {
            return false;
        }
        for (int dy = 0; dy <= height; dy++) {
            int radius = getTreeRadiusForHeight(-1, -1, config.foliageRadius, dy);
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockState state = level.getBlockState(mutable.setWithOffset(pos, dx, dy, dz));
                    if (!state.isAir() && !state.is(BlockTags.LEAVES)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
