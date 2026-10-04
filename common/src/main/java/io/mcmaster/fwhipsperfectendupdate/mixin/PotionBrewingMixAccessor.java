package io.mcmaster.fwhipsperfectendupdate.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads the package-private {@code PotionBrewing.Mix} record. */
@Mixin(targets = "net.minecraft.world.item.alchemy.PotionBrewing$Mix")
public interface PotionBrewingMixAccessor {
    @Accessor("from")
    Holder<?> fwhipsperfectendupdate$from();

    @Accessor("ingredient")
    Ingredient fwhipsperfectendupdate$ingredient();

    @Accessor("to")
    Holder<?> fwhipsperfectendupdate$to();
}
