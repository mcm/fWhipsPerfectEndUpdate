package io.mcmaster.someonesperfectend.mixin;

import io.mcmaster.someonesperfectend.fluid.OasisLogging;
import java.util.Map;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds {@code oasis_logged} to every block (vanilla, this mod, other mods) whose state definition has
 * {@code waterlogged}, right before the states are built (MOD-45).
 */
@Mixin(StateDefinition.Builder.class)
public abstract class StateDefinitionBuilderMixin {
    @Shadow
    @Final
    private Object owner;

    @Shadow
    @Final
    private Map<String, Property<?>> properties;

    @Inject(method = "create", at = @At("HEAD"))
    private void someonesperfectend$addOasisLogged(CallbackInfoReturnable<StateDefinition<?, ?>> cir) {
        if (owner instanceof Block
                && properties.get(BlockStateProperties.WATERLOGGED.getName()) == BlockStateProperties.WATERLOGGED
                && !properties.containsKey(OasisLogging.NAME)) {
            properties.put(OasisLogging.NAME, OasisLogging.OASIS_LOGGED);
        }
    }
}
