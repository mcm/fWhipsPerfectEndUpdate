package io.mcmaster.fwhipsperfectendupdate.infuser;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeaconBlock;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A beacon whose block entity is an {@link InfuserBlockEntity}. Everything else (opening the menu, the beam
 * color, rendering as a model) is the vanilla beacon's.
 */
public class InfuserBlock extends BeaconBlock {
    public static final MapCodec<InfuserBlock> CODEC = simpleCodec(InfuserBlock::new);

    public InfuserBlock(Properties properties) {
        super(properties);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public MapCodec<BeaconBlock> codec() {
        return (MapCodec) CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InfuserBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, Infusers.BLOCK_ENTITY.get(), BeaconBlockEntity::tick);
    }
}
