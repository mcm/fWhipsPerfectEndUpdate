package io.mcmaster.someonesperfectend.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.mcmaster.someonesperfectend.enchanting.VoidCleansingRules;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentNames;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Void Cleansing (US-036) on the enchanting screen: the offer the server marks with {@link VoidCleansingRules#CLUE}
 * is labelled "Void Cleansing" instead of the galactic text, is drawn enabled with a single dragon's breath, and gets
 * its own tooltip.
 */
@Mixin(EnchantmentScreen.class)
public abstract class EnchantmentScreenMixin {
    @Unique
    private boolean someonesperfectend$voidCleansing() {
        @SuppressWarnings("unchecked")
        EnchantmentMenu menu = ((AbstractContainerScreen<EnchantmentMenu>) (Object) this).getMenu();
        int slot = VoidCleansingRules.OFFER_SLOT;
        return menu.costs[slot] > 0 && menu.enchantClue[slot] == VoidCleansingRules.CLUE;
    }

    /** Only the Void Cleansing slot has a cost while it is offered, so every name drawn belongs to it. */
    @WrapOperation(
            method = "renderBg",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/gui/screens/"
                                    + "inventory/EnchantmentNames;getRandomName(Lnet/minecraft/client/gui/Font;I)"
                                    + "Lnet/minecraft/network/chat/FormattedText;"))
    private FormattedText someonesperfectend$voidCleansingName(
            EnchantmentNames names, Font font, int width, Operation<FormattedText> original) {
        FormattedText name = original.call(names, font, width);
        return someonesperfectend$voidCleansing()
                ? Component.translatable("container.someonesperfectend.void_cleansing")
                : name;
    }

    /** The offer needs one dragon's breath, not one lapis per slot number. */
    @WrapOperation(
            method = "renderBg",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/EnchantmentMenu;getGoldCount()I"))
    private int someonesperfectend$voidCleansingFuel(EnchantmentMenu menu, Operation<Integer> original) {
        int count = original.call(menu);
        return someonesperfectend$voidCleansing() && count >= VoidCleansingRules.BREATH_COST
                ? VoidCleansingRules.OFFER_SLOT + 1
                : count;
    }

    @WrapOperation(
            method = "render",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/gui/GuiGraphics;"
                                    + "renderComponentTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;II)V"))
    private void someonesperfectend$voidCleansingTooltip(
            GuiGraphics graphics, Font font, List<Component> lines, int mouseX, int mouseY, Operation<Void> original) {
        Player player = Minecraft.getInstance().player;
        if (!someonesperfectend$voidCleansing() || player == null) {
            original.call(graphics, font, lines, mouseX, mouseY);
            return;
        }
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable("container.someonesperfectend.void_cleansing")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("container.someonesperfectend.void_cleansing.description")
                .withStyle(ChatFormatting.GRAY));
        if (!player.getAbilities().instabuild) {
            tooltip.add(CommonComponents.EMPTY);
            if (player.experienceLevel < VoidCleansingRules.LEVEL_REQUIREMENT) {
                tooltip.add(Component.translatable(
                                "container.enchant.level.requirement", VoidCleansingRules.LEVEL_REQUIREMENT)
                        .withStyle(ChatFormatting.RED));
            } else {
                tooltip.add(Component.translatable("container.someonesperfectend.void_cleansing.breath")
                        .withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.translatable("container.enchant.level.many", VoidCleansingRules.LEVEL_COST)
                        .withStyle(ChatFormatting.GRAY));
            }
        }
        original.call(graphics, font, tooltip, mouseX, mouseY);
    }
}
