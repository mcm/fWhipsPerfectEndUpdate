package io.mcmaster.fwhipsperfectendupdate.mixin;

import io.mcmaster.fwhipsperfectendupdate.fluid.OasisWaterFluid;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Waterloggable blocks schedule a {@code Fluids.WATER} tick when a neighbor changes. Where the fluid is Oasis Water
 * (an Oasis-logged block), an Oasis Water tick is scheduled instead, so Oasis Water flows out (MOD-48). {@code
 * ServerLevelMixin} still runs Oasis Water for water ticks scheduled some other way (e.g. saved before the mod).
 */
@Mixin(LevelAccessor.class)
public interface LevelAccessorMixin {
    @ModifyVariable(
            method = {
                "scheduleTick(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/material/Fluid;I)V",
                "scheduleTick(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/material/Fluid;ILnet/minecraft/world/ticks/TickPriority;)V"
            },
            at = @At("HEAD"),
            argsOnly = true)
    private Fluid fwhipsperfectendupdate$scheduleOasisWater(Fluid fluid, BlockPos pos) {
        if (fluid != Fluids.WATER) {
            return fluid;
        }
        FluidState state = ((LevelAccessor) this).getFluidState(pos);
        return OasisWaterFluid.isOasisWater(state) ? state.getType() : fluid;
    }
}
