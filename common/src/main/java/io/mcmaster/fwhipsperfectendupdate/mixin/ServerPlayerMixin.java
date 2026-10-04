package io.mcmaster.fwhipsperfectendupdate.mixin;

import io.mcmaster.fwhipsperfectendupdate.block.EnderBedLocations;
import io.mcmaster.fwhipsperfectendupdate.block.EnderBedRespawn;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.DimensionTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Ender Bed End respawn, and copying Ender Bed locations to the new player entity on every respawn (US-037). */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Inject(method = "findRespawnPositionAndUseSpawnBlock", at = @At("HEAD"), cancellable = true)
    private void fwhipsperfectendupdate$enderBedRespawn(
            boolean keepInventory,
            DimensionTransition.PostDimensionTransition post,
            CallbackInfoReturnable<DimensionTransition> cir) {
        DimensionTransition transition = EnderBedRespawn.findRespawn((ServerPlayer) (Object) this, keepInventory, post);
        if (transition != null) {
            cir.setReturnValue(transition);
        }
    }

    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void fwhipsperfectendupdate$copyEnderBeds(ServerPlayer oldPlayer, boolean keepEverything, CallbackInfo ci) {
        EnderBedLocations.of((ServerPlayer) (Object) this).copyFrom(EnderBedLocations.of(oldPlayer));
    }
}
