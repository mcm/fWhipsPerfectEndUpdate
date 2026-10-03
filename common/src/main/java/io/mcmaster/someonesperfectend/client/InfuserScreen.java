package io.mcmaster.someonesperfectend.client;

import io.mcmaster.someonesperfectend.infuser.InfuserMenu;
import io.mcmaster.someonesperfectend.infuser.Infusers;
import io.mcmaster.someonesperfectend.infuser.Infusion;
import io.mcmaster.someonesperfectend.infuser.InfusionEffect;
import io.mcmaster.someonesperfectend.infuser.InfusionRules;
import io.mcmaster.someonesperfectend.totem.EffectTotemItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetBeaconPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

/**
 * The beacon screen plus the infusion slot. With the slot empty it is the beacon screen (its confirm button follows
 * {@link InfuserMenu#hasPayment()}). With an item in the slot, the beacon's buttons are hidden and infusion buttons
 * take their place: one per effect the item may take, active only when the tier allows it, plus a confirm button
 * that sends the chosen effect as the beacon packet's primary effect and keeps the screen open for more charges.
 * An effect totem shows only its own (fixed) effect, and confirm activates it.
 * Outside the End the slot is drawn dark and locked.
 */
public class InfuserScreen extends BeaconScreen {
    private static final Component LOCKED_TOOLTIP = Component.translatable(Infusers.LOCKED_TOOLTIP_KEY);
    private static final Component INFUSION_LABEL = Component.translatable(Infusers.INFUSION_LABEL_KEY);
    private static final ResourceLocation BUTTON_DISABLED_SPRITE =
            ResourceLocation.withDefaultNamespace("container/beacon/button_disabled");
    private static final ResourceLocation BUTTON_SELECTED_SPRITE =
            ResourceLocation.withDefaultNamespace("container/beacon/button_selected");
    private static final ResourceLocation BUTTON_HIGHLIGHTED_SPRITE =
            ResourceLocation.withDefaultNamespace("container/beacon/button_highlighted");
    private static final ResourceLocation BUTTON_SPRITE =
            ResourceLocation.withDefaultNamespace("container/beacon/button");
    private static final ResourceLocation CONFIRM_SPRITE =
            ResourceLocation.withDefaultNamespace("container/beacon/confirm");
    private static final ResourceLocation CANCEL_SPRITE =
            ResourceLocation.withDefaultNamespace("container/beacon/cancel");

    private final InfuserMenu infuserMenu;
    private final List<AbstractWidget> beaconWidgets = new ArrayList<>();
    private final List<InfusionEffectButton> effectButtons = new ArrayList<>();
    private final List<AbstractWidget> infusionWidgets = new ArrayList<>();

    @Nullable
    private InfusionEffect selected;

    public InfuserScreen(InfuserMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.infuserMenu = menu;
    }

    @Override
    protected void init() {
        super.init();
        beaconWidgets.clear();
        effectButtons.clear();
        infusionWidgets.clear();
        children().forEach(child -> {
            if (child instanceof AbstractWidget widget) {
                beaconWidgets.add(widget);
            }
        });
        // The same places as the beacon's buttons for these effects.
        addEffectButton(InfusionEffect.SPEED, 53, 22);
        addEffectButton(InfusionEffect.HASTE, 77, 22);
        addEffectButton(InfusionEffect.RESISTANCE, 53, 47);
        addEffectButton(InfusionEffect.JUMP_BOOST, 77, 47);
        addEffectButton(InfusionEffect.STRENGTH, 65, 72);
        addEffectButton(InfusionEffect.REGENERATION, 144, 47);
        addInfusionWidget(new SpriteButton(leftPos + 164, topPos + 107, CONFIRM_SPRITE, CommonComponents.GUI_DONE) {
            @Override
            public void onPress() {
                if (selected != null) {
                    minecraft
                            .getConnection()
                            .send(new ServerboundSetBeaconPacket(Optional.of(selected.holder()), Optional.empty()));
                }
            }

            @Override
            void update() {
                active = infuserMenu.infusionTotem() != null
                        ? infuserMenu.canActivateTotem()
                        : infuserMenu.canInfuse(selected);
            }
        });
        addInfusionWidget(new SpriteButton(leftPos + 190, topPos + 107, CANCEL_SPRITE, CommonComponents.GUI_CANCEL) {
            @Override
            public void onPress() {
                minecraft.player.closeContainer();
            }
        });
        updateInfusionButtons();
    }

    private void addEffectButton(InfusionEffect effect, int x, int y) {
        InfusionEffectButton button = new InfusionEffectButton(leftPos + x, topPos + y, effect);
        effectButtons.add(button);
        addInfusionWidget(button);
    }

    private void addInfusionWidget(AbstractWidget widget) {
        infusionWidgets.add(widget);
        addRenderableWidget(widget);
    }

    @Override
    public void containerTick() {
        if (!infuserMenu.hasInfusionItem()) {
            // Vanilla's buttons are always visible, except the upgrade button, which the beacon tick updates.
            beaconWidgets.forEach(widget -> widget.visible = true);
        }
        super.containerTick();
        updateInfusionButtons();
    }

    private void updateInfusionButtons() {
        boolean infusing = infuserMenu.hasInfusionItem();
        if (infusing) {
            beaconWidgets.forEach(widget -> widget.visible = false);
        }
        infusionWidgets.forEach(widget -> widget.visible = infusing);
        if (!infusing) {
            selected = null;
            return;
        }
        EffectTotemItem totem = infuserMenu.infusionTotem();
        if (totem != null) {
            selected = totem.effect();
            for (InfusionEffectButton button : effectButtons) {
                button.update(button.effect == selected);
            }
            updateSpriteButtons();
            return;
        }
        Infusion existing = Infusion.get(infuserMenu.getInfusionSlot().getItem());
        InfusionEffect existingEffect = existing == null ? null : InfusionEffect.of(existing.effect());
        if (existingEffect != null) {
            selected = existingEffect;
        }
        if (selected != null && !infuserMenu.canSelectInfusion(selected)) {
            selected = null;
        }
        if (selected == null) {
            List<InfusionEffect> selectable = infuserMenu.infusionEffects().stream()
                    .filter(infuserMenu::canSelectInfusion)
                    .toList();
            if (selectable.size() == 1) {
                selected = selectable.get(0);
            }
        }
        List<InfusionEffect> offered = infuserMenu.infusionEffects();
        for (InfusionEffectButton button : effectButtons) {
            button.update(offered.contains(button.effect));
        }
        updateSpriteButtons();
    }

    private void updateSpriteButtons() {
        infusionWidgets.forEach(widget -> {
            if (widget instanceof SpriteButton button) {
                button.update();
            }
        });
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (infuserMenu.hasInfusionItem()) {
            graphics.drawCenteredString(font, INFUSION_LABEL, 62, 10, 0xE0E0E0);
        } else {
            super.renderLabels(graphics, mouseX, mouseY);
        }
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

    /** A 22x22 button drawn like the beacon's. */
    private abstract static class InfuserButton extends AbstractButton {
        boolean chosen;

        InfuserButton(int x, int y, Component message) {
            super(x, y, 22, 22, message);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            ResourceLocation sprite;
            if (!active) {
                sprite = BUTTON_DISABLED_SPRITE;
            } else if (chosen) {
                sprite = BUTTON_SELECTED_SPRITE;
            } else if (isHoveredOrFocused()) {
                sprite = BUTTON_HIGHLIGHTED_SPRITE;
            } else {
                sprite = BUTTON_SPRITE;
            }
            graphics.blitSprite(sprite, getX(), getY(), width, height);
            renderIcon(graphics);
        }

        abstract void renderIcon(GuiGraphics graphics);

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    private abstract static class SpriteButton extends InfuserButton {
        private final ResourceLocation sprite;

        SpriteButton(int x, int y, ResourceLocation sprite, Component message) {
            super(x, y, message);
            this.sprite = sprite;
        }

        @Override
        void renderIcon(GuiGraphics graphics) {
            graphics.blitSprite(sprite, getX() + 2, getY() + 2, 18, 18);
        }

        void update() {}
    }

    private class InfusionEffectButton extends InfuserButton {
        private final InfusionEffect effect;
        private final TextureAtlasSprite sprite;
        private int shownAmplifier = -1;

        InfusionEffectButton(int x, int y, InfusionEffect effect) {
            super(x, y, Component.translatable(effect.holder().value().getDescriptionId()));
            this.effect = effect;
            this.sprite = Minecraft.getInstance().getMobEffectTextures().get(effect.holder());
        }

        @Override
        public void onPress() {
            selected = effect;
            updateInfusionButtons();
        }

        @Override
        void renderIcon(GuiGraphics graphics) {
            graphics.blit(getX() + 2, getY() + 2, 0, 18, 18, sprite);
        }

        void update(boolean offered) {
            visible = offered;
            active = infuserMenu.infusionTotem() != null
                    ? infuserMenu.canActivateTotem()
                    : infuserMenu.canSelectInfusion(effect);
            chosen = effect == selected;
            int amplifier = infuserMenu
                    .previewInfusion(effect)
                    .map(InfusionRules.Charge::amplifier)
                    .orElse(0);
            if (amplifier != shownAmplifier) {
                shownAmplifier = amplifier;
                Component name = amplifier > 0
                        ? Component.translatable(
                                "potion.withAmplifier",
                                getMessage(),
                                Component.translatable("enchantment.level." + (amplifier + 1)))
                        : getMessage();
                setTooltip(Tooltip.create(name));
            }
        }
    }
}
