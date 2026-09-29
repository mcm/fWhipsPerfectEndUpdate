package io.mcmaster.someonesperfectend.mixin;

import io.mcmaster.someonesperfectend.fluid.OasisLogging;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Default states are never Oasis-logged (MOD-45). Blocks build their default state from
 * {@code stateDefinition.any()}, whose {@code oasis_logged} is {@code true}.
 */
@Mixin(Block.class)
public abstract class BlockMixin {
    @ModifyVariable(method = "registerDefaultState", at = @At("HEAD"), argsOnly = true)
    private BlockState someonesperfectend$notOasisLogged(BlockState state) {
        return state.hasProperty(OasisLogging.OASIS_LOGGED) ? state.setValue(OasisLogging.OASIS_LOGGED, false) : state;
    }
}
