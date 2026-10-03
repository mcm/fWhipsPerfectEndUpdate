package io.mcmaster.someonesperfectend.neoforge.mixin;

import io.mcmaster.someonesperfectend.fluid.OasisWaterInteractions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidInteractionRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * NeoForge replaces vanilla's lava check with this registry. Oasis Water shares the water fluid type, so it
 * would match vanilla's water interaction (cobblestone); handle it first to give end stone.
 */
@Mixin(value = FluidInteractionRegistry.class, remap = false)
public abstract class FluidInteractionRegistryMixin {
    @Inject(method = "canInteract", at = @At("HEAD"), cancellable = true)
    private static void someonesperfectend$oasisWaterLava(
            Level level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (OasisWaterInteractions.tryLavaInteraction(level, pos)) {
            cir.setReturnValue(true);
        }
    }
}
