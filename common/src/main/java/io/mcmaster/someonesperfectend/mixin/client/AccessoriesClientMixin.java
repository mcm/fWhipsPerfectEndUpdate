package io.mcmaster.someonesperfectend.mixin.client;

import io.wispforest.accessories.client.AccessoriesClient;
import io.wispforest.accessories.compat.config.ScreenType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Dependency purity: the trinket slot lives in the vanilla inventory, so the Accessories library's own screen is
 * disabled. {@code initLayer} adds the "open accessories" button to the survival and creative inventory screens;
 * {@code attemptToOpenScreen} is behind that button and the library's key binding.
 */
@Mixin(value = AccessoriesClient.class, remap = false)
public abstract class AccessoriesClientMixin {
    @Inject(method = "initLayer", at = @At("HEAD"), cancellable = true)
    private static void someonesperfectend$noInventoryButton(CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(
            method = "attemptToOpenScreen(ZLio/wispforest/accessories/compat/config/ScreenType;)Z",
            at = @At("HEAD"),
            cancellable = true)
    private static void someonesperfectend$noAccessoriesScreen(
            boolean targetingLookingEntity, ScreenType screenType, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
