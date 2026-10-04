package io.mcmaster.fwhipsperfectendupdate.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SuspiciousEffectHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Lets brown mooshrooms eat every {@link SuspiciousEffectHolder} flower, not just {@code #minecraft:small_flowers}.
 * 1.21.1 checks the tag first; Enderblossoms stay out of it so bees ignore them, but should still give their stew.
 */
@Mixin(MushroomCow.class)
public abstract class MushroomCowMixin {
    @WrapOperation(
            method = "mobInteract",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean fwhipsperfectendupdate$acceptEffectHolders(
            ItemStack stack, TagKey<Item> tag, Operation<Boolean> original) {
        return original.call(stack, tag)
                || (tag == ItemTags.SMALL_FLOWERS && SuspiciousEffectHolder.tryGet(stack.getItem()) != null);
    }
}
