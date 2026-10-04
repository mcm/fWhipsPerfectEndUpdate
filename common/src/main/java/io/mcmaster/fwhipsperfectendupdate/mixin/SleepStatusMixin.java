package io.mcmaster.fwhipsperfectendupdate.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.mcmaster.fwhipsperfectendupdate.block.EnderBedTravel;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.SleepStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Players sleeping in an Ender Bed count as awake, so they never help skip the night or count toward the
 * sleeping-players percentage (US-037, US-038).
 */
@Mixin(SleepStatus.class)
public abstract class SleepStatusMixin {
    @WrapOperation(
            method = "update",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;isSleeping()Z"))
    private boolean fwhipsperfectendupdate$enderBedIsAwake(ServerPlayer player, Operation<Boolean> original) {
        return original.call(player) && !EnderBedTravel.isInEnderBed(player);
    }

    @ModifyVariable(method = "areEnoughDeepSleeping", at = @At("HEAD"), argsOnly = true)
    private List<ServerPlayer> fwhipsperfectendupdate$skipEnderBeds(List<ServerPlayer> players) {
        if (players.stream().noneMatch(EnderBedTravel::isInEnderBed)) {
            return players;
        }
        return players.stream()
                .filter(player -> !EnderBedTravel.isInEnderBed(player))
                .toList();
    }
}
