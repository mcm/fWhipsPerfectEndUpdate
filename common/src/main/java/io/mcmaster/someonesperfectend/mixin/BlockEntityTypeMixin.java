package io.mcmaster.someonesperfectend.mixin;

import io.mcmaster.someonesperfectend.block.ModWoodTypes;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Lets the vanilla sign and hanging sign block entity types accept our signs. */
@Mixin(BlockEntityType.class)
public abstract class BlockEntityTypeMixin {
    @Inject(method = "isValid", at = @At("HEAD"), cancellable = true)
    private void someonesperfectend$acceptModSigns(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (ModWoodTypes.isExtraValidSign((BlockEntityType<?>) (Object) this, state)) {
            cir.setReturnValue(true);
        }
    }
}
