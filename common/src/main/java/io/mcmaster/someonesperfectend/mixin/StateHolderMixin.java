package io.mcmaster.someonesperfectend.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.mcmaster.someonesperfectend.fluid.OasisLogging;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Keeps {@code oasis_logged} consistent with {@code waterlogged} (MOD-45): setting {@code waterlogged=false} also
 * clears {@code oasis_logged}, and setting {@code oasis_logged=true} also sets {@code waterlogged}. Every state change
 * (block code, commands, the state codec used for chunks and structures) goes through these methods, so no reachable
 * state has {@code waterlogged=false, oasis_logged=true}.
 */
@Mixin(StateHolder.class)
public abstract class StateHolderMixin {
    @ModifyReturnValue(method = {"setValue", "trySetValue"}, at = @At("RETURN"))
    private Object someonesperfectend$keepOasisLoggedConsistent(Object result, Property<?> property,
            Comparable<?> value) {
        if (property == BlockStateProperties.WATERLOGGED) {
            if (Boolean.FALSE.equals(value) && result instanceof BlockState state
                    && state.hasProperty(OasisLogging.OASIS_LOGGED) && state.getValue(OasisLogging.OASIS_LOGGED)) {
                return state.setValue(OasisLogging.OASIS_LOGGED, false);
            }
        } else if (property == OasisLogging.OASIS_LOGGED) {
            if (Boolean.TRUE.equals(value) && result instanceof BlockState state
                    && state.hasProperty(BlockStateProperties.WATERLOGGED)
                    && !state.getValue(BlockStateProperties.WATERLOGGED)) {
                return state.setValue(BlockStateProperties.WATERLOGGED, true);
            }
        }
        return result;
    }
}
