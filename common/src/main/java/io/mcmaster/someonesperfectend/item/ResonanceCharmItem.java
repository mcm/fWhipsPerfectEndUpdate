package io.mcmaster.someonesperfectend.item;

import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Gives Haste I while equipped in the trinket slot; removing it takes the Haste away. */
public class ResonanceCharmItem extends Item implements Accessory {
    static final int DURATION = 80;
    private static final int REFRESH_BELOW = 40;

    public ResonanceCharmItem(Properties properties) {
        super(properties);
    }

    @Override
    public void tick(ItemStack stack, SlotReference reference) {
        LivingEntity entity = reference.entity();
        if (entity.level().isClientSide()) {
            return;
        }
        MobEffectInstance haste = entity.getEffect(MobEffects.DIG_SPEED);
        if (haste == null || haste.getAmplifier() == 0 && haste.getDuration() < REFRESH_BELOW) {
            entity.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, DURATION, 0, true, false, true));
        }
    }

    @Override
    public void onUnequip(ItemStack stack, SlotReference reference) {
        LivingEntity entity = reference.entity();
        MobEffectInstance haste = entity.getEffect(MobEffects.DIG_SPEED);
        // Only remove the charm's own effect (level I, ambient, at most one refresh long).
        if (!entity.level().isClientSide() && haste != null && haste.getAmplifier() == 0 && haste.isAmbient()
                && haste.getDuration() <= DURATION) {
            entity.removeEffect(MobEffects.DIG_SPEED);
        }
    }
}
