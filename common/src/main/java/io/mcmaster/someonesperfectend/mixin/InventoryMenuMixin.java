package io.mcmaster.someonesperfectend.mixin;

import io.mcmaster.someonesperfectend.trinket.TrinketSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds the trinket slot to the vanilla player inventory menu (after all vanilla slots, so vanilla slot indices
 * are unchanged), and makes shift-clicking a trinket from the inventory, hotbar, or off hand move it into the
 * empty trinket slot. Shift-clicking out of the trinket slot falls through to vanilla (moves to the inventory).
 */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin extends RecipeBookMenu<CraftingInput, CraftingRecipe> {
    private InventoryMenuMixin(MenuType<?> menuType, int containerId) {
        super(menuType, containerId);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void someonesperfectend$addTrinketSlot(Inventory inventory, boolean active, Player owner, CallbackInfo ci) {
        this.addSlot(new TrinketSlot(owner, TrinketSlot.X, TrinketSlot.Y));
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void someonesperfectend$quickMoveToTrinket(
            Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (index < InventoryMenu.INV_SLOT_START || index > InventoryMenu.SHIELD_SLOT) {
            return;
        }
        int trinketIndex = TrinketSlot.indexIn(this);
        Slot slot = this.slots.get(index);
        if (trinketIndex < 0 || !slot.hasItem()) {
            return;
        }
        Slot trinketSlot = this.slots.get(trinketIndex);
        ItemStack stack = slot.getItem();
        if (trinketSlot.hasItem() || !trinketSlot.mayPlace(stack)) {
            return;
        }
        ItemStack original = stack.copy();
        if (this.moveItemStackTo(stack, trinketIndex, trinketIndex + 1, false)) {
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY, original);
            } else {
                slot.setChanged();
            }
            slot.onTake(player, stack);
            cir.setReturnValue(original);
        }
    }
}
