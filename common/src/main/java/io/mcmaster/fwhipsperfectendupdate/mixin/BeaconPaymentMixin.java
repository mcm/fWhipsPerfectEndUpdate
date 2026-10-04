package io.mcmaster.fwhipsperfectendupdate.mixin;

import io.mcmaster.fwhipsperfectendupdate.infuser.StackablePayment;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The beacon menu's payment slot ({@code PaymentSlot}) and container (the anonymous {@code SimpleContainer}) both
 * hold one item. Once {@link StackablePayment#fwhipsperfectendupdate$allowStacks()} is called they hold a full stack;
 * only the Infuser's menu calls it, so beacons are unchanged.
 */
@Mixin(targets = {"net.minecraft.world.inventory.BeaconMenu$PaymentSlot", "net.minecraft.world.inventory.BeaconMenu$1"})
public abstract class BeaconPaymentMixin implements StackablePayment {
    @Unique
    private boolean fwhipsperfectendupdate$stacks;

    @Override
    public void fwhipsperfectendupdate$allowStacks() {
        this.fwhipsperfectendupdate$stacks = true;
    }

    @Inject(method = "getMaxStackSize()I", at = @At("HEAD"), cancellable = true)
    private void fwhipsperfectendupdate$stackSize(CallbackInfoReturnable<Integer> cir) {
        if (this.fwhipsperfectendupdate$stacks) {
            cir.setReturnValue(Item.DEFAULT_MAX_STACK_SIZE);
        }
    }
}
