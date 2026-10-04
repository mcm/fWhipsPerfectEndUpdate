package io.mcmaster.fwhipsperfectendupdate.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.serialization.MapCodec;
import io.mcmaster.fwhipsperfectendupdate.fluid.OasisLogging;
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
    private boolean fwhipsperfectendupdate$oasisLogged;

    @Unique
    private boolean fwhipsperfectendupdate$hasOasisLoggedTrue;

    @Unique
    private BlockState fwhipsperfectendupdate$shapeState;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void fwhipsperfectendupdate$initOasisLogged(
            Block block,
            Reference2ObjectArrayMap<Property<?>, Comparable<?>> values,
            MapCodec<BlockState> codec,
            CallbackInfo ci) {
        fwhipsperfectendupdate$hasOasisLoggedTrue = Boolean.TRUE.equals(values.get(OasisLogging.OASIS_LOGGED));
        fwhipsperfectendupdate$oasisLogged = fwhipsperfectendupdate$hasOasisLoggedTrue
                && Boolean.TRUE.equals(values.get(BlockStateProperties.WATERLOGGED));
    }

    @ModifyReturnValue(method = "getFluidState", at = @At("RETURN"))
    private FluidState fwhipsperfectendupdate$oasisWater(FluidState fluid) {
        return fwhipsperfectendupdate$oasisLogged ? OasisLogging.source(fluid) : fluid;
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
    private BlockState fwhipsperfectendupdate$shapeState(BlockState state) {
        if (!fwhipsperfectendupdate$hasOasisLoggedTrue) {
            return state;
        }
        BlockState shapeState = fwhipsperfectendupdate$shapeState;
        if (shapeState == null) {
            shapeState = OasisLogging.shapeState(state);
            fwhipsperfectendupdate$shapeState = shapeState;
        }
        return shapeState;
    }
}
