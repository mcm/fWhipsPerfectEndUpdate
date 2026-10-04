package io.mcmaster.fwhipsperfectendupdate.effect;

import dev.architectury.registry.registries.RegistrySupplier;
import io.mcmaster.fwhipsperfectendupdate.registry.ModRegistries;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;

/** The mod's mob effects. */
public final class ModEffects {
    // Enderblossom suspicious stew (MOD-58).
    public static final RegistrySupplier<MobEffect> DISPLACEMENT =
            ModRegistries.MOB_EFFECTS.register("displacement", DisplacementEffect::new);

    private ModEffects() {}

    public static Holder<MobEffect> displacement() {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(DISPLACEMENT.get());
    }

    /** Loads this class so its entries are added to the deferred register. */
    public static void init() {}
}
