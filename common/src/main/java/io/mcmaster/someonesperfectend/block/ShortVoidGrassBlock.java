package io.mcmaster.someonesperfectend.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Light-blue End grass; lives only on End Turf or Voidsoil. */
public class ShortVoidGrassBlock extends BushBlock {
    public static final MapCodec<ShortVoidGrassBlock> CODEC = simpleCodec(ShortVoidGrassBlock::new);
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 13.0, 14.0);

    public ShortVoidGrassBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<ShortVoidGrassBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(ModBlocks.END_TURF.get()) || state.is(ModBlocks.VOIDSOIL.get());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Vec3 offset = state.getOffset(level, pos);
        return SHAPE.move(offset.x, offset.y, offset.z);
    }
}
