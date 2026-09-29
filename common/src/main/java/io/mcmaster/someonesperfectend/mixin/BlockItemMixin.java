package io.mcmaster.someonesperfectend.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.mcmaster.someonesperfectend.fluid.OasisLogging;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A waterloggable block placed into Oasis Water becomes Oasis-logged (MOD-48). Most blocks' {@code
 * getStateForPlacement} only waterlog in {@code Fluids.WATER}, so this runs on the state every block item places,
 * whatever the block class. See {@link OasisLogging#placementState}.
 */
@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
    @WrapOperation(method = "place", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/BlockItem;getPlacementState(Lnet/minecraft/world/item/context/BlockPlaceContext;)"
                    + "Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState someonesperfectend$oasisLogPlacement(BlockItem item, BlockPlaceContext context,
            Operation<BlockState> original) {
        return OasisLogging.placementState(context.getLevel(), context.getClickedPos(), original.call(item, context));
    }
}
