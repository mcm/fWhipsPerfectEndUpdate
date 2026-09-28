package io.mcmaster.someonesperfectend.mixin.client;

import io.mcmaster.someonesperfectend.client.TrinketSlotClient;
import io.mcmaster.someonesperfectend.trinket.TrinketSlot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Places the trinket slot in the creative inventory tab. Vanilla lays out every player-inventory slot by index,
 * and would put the extra slot on top of the hotbar; move it next to the off-hand slot and draw its frame.
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin
        extends AbstractContainerScreen<CreativeModeInventoryScreen.ItemPickerMenu> {
    private static final String NEW_SLOT_WRAPPER = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen$SlotWrapper;<init>(Lnet/minecraft/world/inventory/Slot;III)V";

    @Shadow
    private static CreativeModeTab selectedTab;

    private CreativeModeInventoryScreenMixin(CreativeModeInventoryScreen.ItemPickerMenu menu, Inventory inventory,
            Component title) {
        super(menu, inventory, title);
    }

    @ModifyArg(method = "selectTab", index = 2, at = @At(value = "INVOKE", target = NEW_SLOT_WRAPPER))
    private int someonesperfectend$trinketSlotX(Slot target, int index, int x, int y) {
        return target instanceof TrinketSlot ? TrinketSlotClient.CREATIVE_X : x;
    }

    @ModifyArg(method = "selectTab", index = 3, at = @At(value = "INVOKE", target = NEW_SLOT_WRAPPER))
    private int someonesperfectend$trinketSlotY(Slot target, int index, int x, int y) {
        return target instanceof TrinketSlot ? TrinketSlotClient.CREATIVE_Y : y;
    }

    @Inject(method = "renderBg", at = @At("TAIL"))
    private void someonesperfectend$drawTrinketSlot(GuiGraphics graphics, float partialTick, int mouseX, int mouseY,
            CallbackInfo ci) {
        if (selectedTab.getType() == CreativeModeTab.Type.INVENTORY) {
            TrinketSlotClient.drawSlotFrame(graphics, this.leftPos + TrinketSlotClient.CREATIVE_X,
                    this.topPos + TrinketSlotClient.CREATIVE_Y);
        }
    }
}
