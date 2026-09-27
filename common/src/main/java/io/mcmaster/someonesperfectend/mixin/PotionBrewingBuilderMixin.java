package io.mcmaster.someonesperfectend.mixin;

import io.mcmaster.someonesperfectend.alchemy.OasisPotions;
import io.mcmaster.someonesperfectend.fluid.ModFluids;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Right before the brewing registry is built (after vanilla's and every mod's mixes are added), makes the
 * Oasis Water bottle a brewing input and mirrors each potion mix for the Oasis variants: if {@code from} has a
 * variant, {@code variant(from) + ingredient → variant(to)} (or vanilla {@code to} when it has none).
 */
@Mixin(PotionBrewing.Builder.class)
public abstract class PotionBrewingBuilderMixin {
    @Shadow
    @Final
    private List<Object> potionMixes;

    @SuppressWarnings("unchecked")
    @Inject(method = "build", at = @At("HEAD"))
    private void someonesperfectend$addOasisMixes(CallbackInfoReturnable<PotionBrewing> cir) {
        PotionBrewing.Builder builder = (PotionBrewing.Builder) (Object) this;
        builder.addContainer(ModFluids.OASIS_WATER_BOTTLE.get());
        for (Object entry : List.copyOf(potionMixes)) {
            PotionBrewingMixAccessor mix = (PotionBrewingMixAccessor) entry;
            Holder<Potion> from = (Holder<Potion>) mix.someonesperfectend$from();
            Holder<Potion> to = (Holder<Potion>) mix.someonesperfectend$to();
            OasisPotions.variantOf(from).ifPresent(variantFrom -> {
                Holder<Potion> variantTo = OasisPotions.variantOrSelf(to);
                for (ItemStack ingredient : mix.someonesperfectend$ingredient().getItems()) {
                    builder.addMix(variantFrom, ingredient.getItem(), variantTo);
                }
            });
        }
    }
}
