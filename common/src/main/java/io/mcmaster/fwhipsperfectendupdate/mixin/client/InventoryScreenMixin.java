package io.mcmaster.fwhipsperfectendupdate.mixin.client;

import io.mcmaster.fwhipsperfectendupdate.client.TrinketSlotClient;
import io.mcmaster.fwhipsperfectendupdate.trinket.TrinketSlot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the trinket slot's frame above the off-hand slot (the vanilla texture has none there). */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractContainerScreen<InventoryMenu> {
    private InventoryScreenMixin(InventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "renderBg", at = @At("TAIL"))
    private void fwhipsperfectendupdate$drawTrinketSlot(
            GuiGraphics graphics, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
        TrinketSlotClient.drawSlotFrame(graphics, this.leftPos + TrinketSlot.X, this.topPos + TrinketSlot.Y);
    }
}
