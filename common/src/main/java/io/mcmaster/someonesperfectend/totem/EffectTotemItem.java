package io.mcmaster.someonesperfectend.totem;

import io.mcmaster.someonesperfectend.infuser.BeaconEffectTracker;
import io.mcmaster.someonesperfectend.infuser.InfusionEffect;
import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.slot.SlotReference;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * An effect totem (US-032). Worn in the trinket slot, an active totem applies its fixed effect at level I every
 * 80 ticks for 340 ticks, ambient like a full-size beacon, in every dimension, stacking one level above a matching
 * beacon or Infuser effect (US-033). An inactive totem does nothing.
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
        // Also re-apply right away if the effect is gone (e.g. a stacked level ran out along with its beacon effect).
        if (TotemRules.appliesOn(entity.level().getGameTime()) || !entity.hasEffect(effect.holder())) {
            applyEffect(stack, entity);
        }
    }

    @Override
    public void onUnequip(ItemStack stack, SlotReference reference) {
        LivingEntity entity = reference.entity();
        if (entity.level().isClientSide() || !EffectTotems.isActive(stack)) {
            return;
        }
        Holder<MobEffect> holder = effect.holder();
        int beacon = beaconAmplifier(entity);
        MobEffectInstance current = entity.getEffect(holder);
        // Only remove the totem's own effect (its current level, ambient, at most one application long).
        if (current != null
                && current.getAmplifier() == TotemRules.amplifier(beacon)
                && current.isAmbient()
                && current.getDuration() <= TotemRules.EFFECT_DURATION) {
            entity.removeEffect(holder);
            if (beacon != TotemRules.NO_BEACON && entity instanceof Player player) {
                // Removing the stacked level also drops the beacon's effect hidden under it; restore it.
                entity.addEffect(new MobEffectInstance(
                        holder, BeaconEffectTracker.remainingTicks(player, holder, beacon), beacon, true, true));
            }
        }
    }

    /**
     * Applies level I, then (US-033) one level above a live beacon or Infuser effect of the same type, lasting no
     * longer than that beacon effect. The level I application stays underneath (vanilla hidden effects), so the
     * totem falls back to level I when the bonus ends.
     */
    private void applyEffect(ItemStack stack, LivingEntity entity) {
        if (entity.level().isClientSide() || !EffectTotems.isActive(stack)) {
            return;
        }
        Holder<MobEffect> holder = effect.holder();
        entity.addEffect(
                new MobEffectInstance(holder, TotemRules.EFFECT_DURATION, TotemRules.AMPLIFIER, true, false, true));
        int beacon = beaconAmplifier(entity);
        if (beacon != TotemRules.NO_BEACON && entity instanceof Player player) {
            int ticks = TotemRules.stackedDuration(BeaconEffectTracker.remainingTicks(player, holder, beacon));
            if (ticks > 0) {
                entity.addEffect(new MobEffectInstance(holder, ticks, TotemRules.amplifier(beacon), true, false, true));
            }
        }
    }

    /** The highest live tracked beacon amplifier for this totem's effect, or {@link TotemRules#NO_BEACON}. */
    private int beaconAmplifier(LivingEntity entity) {
        return entity instanceof Player player
                ? BeaconEffectTracker.highestAmplifier(player, effect.holder())
                : TotemRules.NO_BEACON;
    }
}
