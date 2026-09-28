package io.mcmaster.someonesperfectend.client;

import io.mcmaster.someonesperfectend.infuser.InfuserMenu;
import io.mcmaster.someonesperfectend.infuser.Infusers;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * The beacon screen plus the infusion slot. Its confirm button follows {@link InfuserMenu#hasPayment()}, so it
 * is disabled while the infusion slot holds an item. Outside the End the slot is drawn dark and locked.
 */
public class InfuserScreen extends BeaconScreen {
    private static final Component LOCKED_TOOLTIP = Component.translatable(Infusers.LOCKED_TOOLTIP_KEY);

    private final InfuserMenu infuserMenu;

    public InfuserScreen(InfuserMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.infuserMenu = menu;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        Slot slot = infuserMenu.getInfusionSlot();
        int x = leftPos + slot.x;
        int y = topPos + slot.y;
        TrinketSlotClient.drawSlotFrame(graphics, x, y);
        if (!infuserMenu.isInfusionEnabled()) {
            graphics.fill(x, y, x + 16, y + 16, 0xFF2A1F33);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        Slot slot = infuserMenu.getInfusionSlot();
        if (!infuserMenu.isInfusionEnabled() && isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
            graphics.renderTooltip(font, LOCKED_TOOLTIP, mouseX, mouseY);
        }
    }
}
