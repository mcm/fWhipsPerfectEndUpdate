package io.mcmaster.someonesperfectend.totem;

import io.mcmaster.someonesperfectend.infuser.InfusionEffect;
import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.slot.SlotReference;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * An effect totem (US-032). Worn in the trinket slot, an active totem applies its fixed effect at level I every
 * 80 ticks for 340 ticks, ambient like a full-size beacon, in every dimension. An inactive totem does nothing.
 * It is not a Totem of Undying and never prevents death.
 */
public class EffectTotemItem extends Item implements Accessory {
    private final InfusionEffect effect;

    public EffectTotemItem(InfusionEffect effect, Properties properties) {
        super(properties);
        this.effect = effect;
    }

    public InfusionEffect effect() {
        return effect;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return EffectTotems.isActive(stack) || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        if (EffectTotems.isActive(stack)) {
            lines.add(Component.translatable(EffectTotems.ACTIVE_TOOLTIP_KEY).withStyle(ChatFormatting.BLUE));
        } else {
            lines.add(Component.translatable(EffectTotems.INACTIVE_TOOLTIP_KEY).withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public void onEquip(ItemStack stack, SlotReference reference) {
        applyEffect(stack, reference.entity());
    }

    @Override
    public void tick(ItemStack stack, SlotReference reference) {
        LivingEntity entity = reference.entity();
        if (TotemRules.appliesOn(entity.level().getGameTime())) {
            applyEffect(stack, entity);
        }
    }

    @Override
    public void onUnequip(ItemStack stack, SlotReference reference) {
        LivingEntity entity = reference.entity();
        MobEffectInstance current = entity.getEffect(effect.holder());
        // Only remove the totem's own effect (level I, ambient, at most one application long).
        if (!entity.level().isClientSide() && EffectTotems.isActive(stack) && current != null
                && current.getAmplifier() == TotemRules.AMPLIFIER && current.isAmbient()
                && current.getDuration() <= TotemRules.EFFECT_DURATION) {
            entity.removeEffect(effect.holder());
        }
    }

    private void applyEffect(ItemStack stack, LivingEntity entity) {
        if (entity.level().isClientSide() || !EffectTotems.isActive(stack)) {
            return;
        }
        entity.addEffect(new MobEffectInstance(effect.holder(), TotemRules.EFFECT_DURATION, TotemRules.AMPLIFIER,
                true, false, true));
    }
}
