package io.mcmaster.someonesperfectend.mixin;

import io.mcmaster.someonesperfectend.fluid.OasisLogging;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * A state with {@code oasis_logged=true} caches the shapes, light and face sturdiness of the same state with
 * {@code oasis_logged=false}, which differ only in the fluid (see {@code BlockStateBaseMixin}, MOD-45).
 */
@Mixin(targets = "net.minecraft.world.level.block.state.BlockBehaviour$BlockStateBase$Cache")
public abstract class BlockStateCacheMixin {
    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true)
    private static BlockState someonesperfectend$shapeState(BlockState state) {
        return OasisLogging.shapeState(state);
    }
}
