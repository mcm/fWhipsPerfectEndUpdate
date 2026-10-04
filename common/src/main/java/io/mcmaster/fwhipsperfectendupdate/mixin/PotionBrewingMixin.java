package io.mcmaster.fwhipsperfectendupdate.mixin;

import io.mcmaster.fwhipsperfectendupdate.alchemy.OasisPotions;
import io.mcmaster.fwhipsperfectendupdate.fluid.ModFluids;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Brewing an Oasis Water bottle: vanilla matches it as a water bottle and returns the result in the same
 * item, so the result is turned into a normal potion holding the Oasis variant of the brewed potion.
 */
@Mixin(PotionBrewing.class)
public abstract class PotionBrewingMixin {
    @Inject(method = "mix", at = @At("RETURN"), cancellable = true)
    private void fwhipsperfectendupdate$brewOasisWater(
            ItemStack ingredient, ItemStack potion, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack result = cir.getReturnValue();
        if (result == potion
                || !potion.is(ModFluids.OASIS_WATER_BOTTLE.get())
                || !result.is(ModFluids.OASIS_WATER_BOTTLE.get())) {
            return;
        }
        result.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)
                .potion()
                .ifPresent(brewed -> cir.setReturnValue(
                        PotionContents.createItemStack(Items.POTION, OasisPotions.variantOrSelf(brewed))));
    }
}
