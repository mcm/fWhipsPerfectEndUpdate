package io.mcmaster.someonesperfectend.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.mcmaster.someonesperfectend.fluid.ModFluids;
import io.mcmaster.someonesperfectend.fluid.OasisLogging;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Waterloggable blocks take an Oasis Water source (setting {@code waterlogged} and {@code oasis_logged}) and give an
 * Oasis Water bucket back when emptied (MOD-45). Applies to every block that keeps the default methods or calls them
 * (slabs do).
 */
@Mixin(SimpleWaterloggedBlock.class)
public interface SimpleWaterloggedBlockMixin {
    @ModifyReturnValue(method = "canPlaceLiquid", at = @At("RETURN"))
    private boolean someonesperfectend$acceptOasisWater(boolean accepted, Player player, BlockGetter level,
            BlockPos pos, BlockState state, Fluid fluid) {
        return accepted || (OasisLogging.isOasisSource(fluid) && OasisLogging.canBeOasisLogged(state));
    }

    @Inject(method = "placeLiquid", at = @At("HEAD"), cancellable = true)
    private void someonesperfectend$placeOasisWater(LevelAccessor level, BlockPos pos, BlockState state,
            FluidState fluid, CallbackInfoReturnable<Boolean> cir) {
        if (!OasisLogging.isOasisSource(fluid.getType()) || !OasisLogging.canBeOasisLogged(state)) {
            return;
        }
        if (state.getValue(BlockStateProperties.WATERLOGGED)) {
            cir.setReturnValue(false);
            return;
        }
        if (!level.isClientSide()) {
            level.setBlock(pos, state.setValue(BlockStateProperties.WATERLOGGED, true)
                    .setValue(OasisLogging.OASIS_LOGGED, true), 3);
            level.scheduleTick(pos, fluid.getType(), fluid.getType().getTickDelay(level));
        }
        cir.setReturnValue(true);
    }

    /** Vanilla clears {@code waterlogged} (which clears {@code oasis_logged}) and returns a water bucket. */
    @ModifyReturnValue(method = "pickupBlock", at = @At("RETURN"))
    private ItemStack someonesperfectend$pickUpOasisWater(ItemStack bucket, Player player, LevelAccessor level,
            BlockPos pos, BlockState state) {
        return bucket.is(Items.WATER_BUCKET) && OasisLogging.isOasisLogged(state)
                ? new ItemStack(ModFluids.OASIS_WATER_BUCKET.get())
                : bucket;
    }
}
