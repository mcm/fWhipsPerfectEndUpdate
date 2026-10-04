package io.mcmaster.fwhipsperfectendupdate.mixin;

import io.mcmaster.fwhipsperfectendupdate.fluid.OasisWaterFluid;
import io.mcmaster.fwhipsperfectendupdate.fluid.OasisWaterInteractions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.LavaFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lava flowing down into Oasis Water turns the water into end stone instead of stone. */
@Mixin(LavaFluid.class)
public abstract class LavaFluidMixin {
    @Shadow
    private void fizz(LevelAccessor level, BlockPos pos) {
        throw new AssertionError();
    }

    @Inject(method = "spreadTo", at = @At("HEAD"), cancellable = true)
    private void fwhipsperfectendupdate$oasisWaterBelow(
            LevelAccessor level,
            BlockPos pos,
            BlockState blockState,
            Direction direction,
            FluidState fluidState,
            CallbackInfo ci) {
        if (direction != Direction.DOWN || !((LavaFluid) (Object) this).is(FluidTags.LAVA)) {
            return;
        }
        if (OasisWaterFluid.isOasisWater(level.getFluidState(pos))) {
            if (blockState.getBlock() instanceof LiquidBlock) {
                level.setBlock(pos, OasisWaterInteractions.waterResult(), 3);
            }
            fizz(level, pos);
            ci.cancel();
        }
    }
}
