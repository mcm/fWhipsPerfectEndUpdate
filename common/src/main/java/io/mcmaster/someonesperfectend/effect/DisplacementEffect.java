package io.mcmaster.someonesperfectend.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.InstantenousMobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/** Displacement: an instant effect that teleports the entity like eating chorus fruit ({@link ChorusTeleport}). */
public class DisplacementEffect extends InstantenousMobEffect {
    public static final int COLOR = 0x9C5BD9;

    public DisplacementEffect() {
        super(MobEffectCategory.NEUTRAL, COLOR);
    }

    /**
     * Runs once for an effect instance (suspicious stew, {@code /effect}); splash and lingering potions reach it
     * through vanilla's {@code applyInstantenousEffect}.
     */
    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level() instanceof ServerLevel level) {
            ChorusTeleport.teleport(level, entity);
        }
        return true;
    }
}
