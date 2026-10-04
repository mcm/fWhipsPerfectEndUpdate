package io.mcmaster.fwhipsperfectendupdate.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** A potted Enderblossom: opens and closes near Endermen like the wall one, by swapping to the other pot. */
public class PottedEnderblossomBlock extends FlowerPotBlock implements Enderblossoms.Blossom {
    private final EnderblossomBlock blossom;

    public PottedEnderblossomBlock(EnderblossomBlock blossom, Properties properties) {
        super(blossom, properties);
        this.blossom = blossom;
    }

    @Override
    public EnderblossomBlock.Type type() {
        return blossom.type();
    }

    @Override
    public BlockState withType(BlockState state, EnderblossomBlock.Type type) {
        return ModBlocks.pottedEnderblossom(type).defaultBlockState();
    }

    @Override
    public Vec3 headPosition(BlockState state, BlockPos pos) {
        return Vec3.atBottomCenterOf(pos).add(0.0, 0.75, 0.0);
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
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        EnderblossomBlock.playIdleSound(type(), level, pos, random);
    }
}
