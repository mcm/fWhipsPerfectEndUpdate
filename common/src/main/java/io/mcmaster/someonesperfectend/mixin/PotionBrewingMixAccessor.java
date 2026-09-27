package io.mcmaster.someonesperfectend.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads the package-private {@code PotionBrewing.Mix} record. */
@Mixin(targets = "net.minecraft.world.item.alchemy.PotionBrewing$Mix")
public interface PotionBrewingMixAccessor {
    @Accessor("from")
    Holder<?> someonesperfectend$from();

    @Accessor("ingredient")
    Ingredient someonesperfectend$ingredient();

    @Accessor("to")
    Holder<?> someonesperfectend$to();
}
