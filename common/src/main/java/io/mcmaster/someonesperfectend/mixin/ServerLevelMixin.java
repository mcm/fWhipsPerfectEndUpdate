package io.mcmaster.someonesperfectend.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.mcmaster.someonesperfectend.fluid.OasisWaterFluid;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Waterloggable blocks schedule a vanilla water tick when a neighbor changes. At an Oasis-logged block that tick runs
 * the Oasis Water there, so it flows out like water does from a waterlogged block (MOD-45).
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @WrapOperation(method = "tickFluid", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/material/FluidState;is(Lnet/minecraft/world/level/material/Fluid;)Z"))
    private boolean someonesperfectend$tickOasisWaterForWater(FluidState state, Fluid fluid,
            Operation<Boolean> original) {
        return original.call(state, fluid) || (Fluids.WATER.isSame(fluid) && OasisWaterFluid.isOasisWater(state));
    }
}
