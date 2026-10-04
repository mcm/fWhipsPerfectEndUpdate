package io.mcmaster.fwhipsperfectendupdate.infuser;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import org.jetbrains.annotations.Nullable;

/**
 * The beacon effects gear can be infused with, and the pyramid tier each needs (the vanilla beacon tiers).
 * Kept free of registry access (see {@link #holder()}) so the rules can be unit-tested without bootstrapping.
 */
public enum InfusionEffect {
    SPEED(1),
    HASTE(1),
    RESISTANCE(2),
    JUMP_BOOST(2),
    STRENGTH(3),
    REGENERATION(4);

    private final int minTier;

    InfusionEffect(int minTier) {
        this.minTier = minTier;
    }

    public int minTier() {
        return minTier;
    }

    /** Whether the beacon offers this effect at level II on a tier-4 pyramid (Regeneration is always level I). */
    public boolean canBeLevelTwo() {
        return this != REGENERATION;
    }

    public Holder<MobEffect> holder() {
        return switch (this) {
            case SPEED -> MobEffects.MOVEMENT_SPEED;
            case HASTE -> MobEffects.DIG_SPEED;
            case RESISTANCE -> MobEffects.DAMAGE_RESISTANCE;
            case JUMP_BOOST -> MobEffects.JUMP;
            case STRENGTH -> MobEffects.DAMAGE_BOOST;
            case REGENERATION -> MobEffects.REGENERATION;
        };
    }

    /** The infusion effect for a mob effect, or null if gear can't be infused with it. */
    @Nullable
    public static InfusionEffect of(@Nullable Holder<MobEffect> effect) {
        if (effect == null) {
            return null;
        }
        for (InfusionEffect value : values()) {
            if (effect.is(value.holder())) {
                return value;
            }
        }
        return null;
    }
}
