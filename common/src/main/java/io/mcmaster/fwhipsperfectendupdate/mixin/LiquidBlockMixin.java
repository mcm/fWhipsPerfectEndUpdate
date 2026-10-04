package io.mcmaster.fwhipsperfectendupdate.mixin;

import io.mcmaster.fwhipsperfectendupdate.fluid.OasisWaterInteractions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lava next to Oasis Water becomes end stone instead of cobblestone (vanilla path; NeoForge routes this check
 * through {@code FluidInteractionRegistry}, handled by a NeoForge mixin).
 */
@Mixin(LiquidBlock.class)
public abstract class LiquidBlockMixin {
    @Inject(method = "shouldSpreadLiquid", at = @At("HEAD"), cancellable = true)
    private void fwhipsperfectendupdate$oasisWaterLava(
            Level level, BlockPos pos, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (OasisWaterInteractions.tryLavaInteraction(level, pos)) {
            cir.setReturnValue(false);
        }
    }
}
