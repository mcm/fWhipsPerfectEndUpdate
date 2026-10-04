package io.mcmaster.fwhipsperfectendupdate.block;

import com.mojang.serialization.MapCodec;
import io.mcmaster.fwhipsperfectendupdate.FwhipsPerfectEndUpdate;
import io.mcmaster.fwhipsperfectendupdate.worldgen.ModFeatures;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Grows into {@code fwhipsperfectendupdate:astral_tree}. Unlike vanilla saplings it lives on End soil
 * ({@link ModBlockTags#END_PLANT_SOIL}), grows without light, and only grows (by random tick or bone meal) with
 * {@link #CLEARANCE} free blocks above it.
 */
public class AstralSaplingBlock extends SaplingBlock {
    public static final MapCodec<AstralSaplingBlock> CODEC = simpleCodec(AstralSaplingBlock::new);
    public static final TreeGrower TREE_GROWER = new TreeGrower(
            FwhipsPerfectEndUpdate.id("astral").toString(),
            Optional.empty(),
            Optional.of(ModFeatures.ASTRAL_TREE_CONFIGURED),
            Optional.empty());
    /** Free (air or replaceable) blocks needed directly above the sapling for it to grow. */
    public static final int CLEARANCE = 2;

    public AstralSaplingBlock(Properties properties) {
        super(TREE_GROWER, properties);
    }

    @Override
    public MapCodec<? extends SaplingBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(ModBlockTags.END_PLANT_SOIL);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        // Checked directly: NeoForge's BushBlock#canSurvive would ask the soil's canSustainPlant first.
        BlockPos below = pos.below();
        return mayPlaceOn(level.getBlockState(below), level, below);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Vanilla needs light level 9 above the sapling; Astral saplings grow in the dark.
        if (random.nextInt(7) == 0) {
            advanceTree(level, pos, state, random);
        }
    }

    @Override
    public void advanceTree(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        if (hasClearance(level, pos)) {
            super.advanceTree(level, pos, state, random);
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return hasClearance(level, pos);
    }

    public static boolean hasClearance(LevelReader level, BlockPos pos) {
        for (int i = 1; i <= CLEARANCE; i++) {
            if (!level.getBlockState(pos.above(i)).canBeReplaced()) {
                return false;
            }
        }
        return true;
    }
}
