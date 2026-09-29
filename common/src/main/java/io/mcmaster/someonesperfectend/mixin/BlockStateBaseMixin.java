package io.mcmaster.someonesperfectend.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.serialization.MapCodec;
import io.mcmaster.someonesperfectend.fluid.OasisLogging;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Oasis-logged block states (MOD-45).
 *
 * <ul>
 *   <li>A state with {@code waterlogged=true, oasis_logged=true} holds an Oasis Water source. The flag is worked out
 *       once per state, so {@code getFluidState} stays a field read for every other state.</li>
 *   <li>Shapes of a state with {@code oasis_logged=true} are those of the same state with {@code oasis_logged=false}:
 *       some blocks (vanilla walls, and likely other mods' blocks) precompute their shapes per state from the
 *       {@code waterlogged} values only, and have no entry for the added states. See also {@code
 *       BlockStateCacheMixin}.</li>
 * </ul>
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {
    @Unique
    private boolean someonesperfectend$oasisLogged;

    @Unique
    private boolean someonesperfectend$hasOasisLoggedTrue;

    @Unique
    private BlockState someonesperfectend$shapeState;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void someonesperfectend$initOasisLogged(
            Block block,
            Reference2ObjectArrayMap<Property<?>, Comparable<?>> values,
            MapCodec<BlockState> codec,
            CallbackInfo ci) {
        someonesperfectend$hasOasisLoggedTrue = Boolean.TRUE.equals(values.get(OasisLogging.OASIS_LOGGED));
        someonesperfectend$oasisLogged = someonesperfectend$hasOasisLoggedTrue
                && Boolean.TRUE.equals(values.get(BlockStateProperties.WATERLOGGED));
    }

    @ModifyReturnValue(method = "getFluidState", at = @At("RETURN"))
    private FluidState someonesperfectend$oasisWater(FluidState fluid) {
        return someonesperfectend$oasisLogged ? OasisLogging.source(fluid) : fluid;
    }

    @ModifyExpressionValue(
            method = {
                "getOcclusionShape",
                "getShape",
                "getCollisionShape",
                "getBlockSupportShape",
                "getVisualShape",
                "getInteractionShape",
                "isCollisionShapeFullBlock"
            },
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/level/block/state/BlockBehaviour$BlockStateBase;asState()"
                                    + "Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState someonesperfectend$shapeState(BlockState state) {
        if (!someonesperfectend$hasOasisLoggedTrue) {
            return state;
        }
        BlockState shapeState = someonesperfectend$shapeState;
        if (shapeState == null) {
            shapeState = OasisLogging.shapeState(state);
            someonesperfectend$shapeState = shapeState;
        }
        return shapeState;
    }
}
