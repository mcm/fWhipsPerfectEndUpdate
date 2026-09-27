package io.mcmaster.someonesperfectend.mixin;

import io.mcmaster.someonesperfectend.fluid.ModFluids;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Lets hoppers put Oasis Water bottles into an empty bottle slot, like water bottles. */
@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandBlockEntityMixin {
    @Inject(method = "canPlaceItem", at = @At("RETURN"), cancellable = true)
    private void someonesperfectend$acceptOasisWater(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && slot < 3 && stack.is(ModFluids.OASIS_WATER_BOTTLE.get())
                && ((Container) this).getItem(slot).isEmpty()) {
            cir.setReturnValue(true);
        }
    }
}
