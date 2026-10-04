package io.mcmaster.fwhipsperfectendupdate.alchemy;

import dev.architectury.registry.registries.RegistrySupplier;
import io.mcmaster.fwhipsperfectendupdate.registry.ModRegistries;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;

/**
 * Oasis variants of vanilla potions: the same potion with every non-instant effect lasting 1.5x as long.
 * Brewing from an Oasis Water bottle produces them (the variant mixes are added in
 * {@code PotionBrewingBuilderMixin}); once brewed they keep being variants through every later step
 * (redstone, glowstone, gunpowder, dragon's breath). Potions whose effects are all instant have no variant
 * and brew to the vanilla potion.
 */
public final class OasisPotions {
    public static final float DURATION_MULTIPLIER = 1.5F;

    /** Vanilla potions that get a variant: {@code awkward} (the brewing base) plus every potion with a timed effect. */
    static final List<String> VANILLA_BASES = List.of(
            "awkward",
            "night_vision",
            "long_night_vision",
            "invisibility",
            "long_invisibility",
            "leaping",
            "long_leaping",
            "strong_leaping",
            "fire_resistance",
            "long_fire_resistance",
            "swiftness",
            "long_swiftness",
            "strong_swiftness",
            "slowness",
            "long_slowness",
            "strong_slowness",
            "turtle_master",
            "long_turtle_master",
            "strong_turtle_master",
            "water_breathing",
            "long_water_breathing",
            "poison",
            "long_poison",
            "strong_poison",
            "regeneration",
            "long_regeneration",
            "strong_regeneration",
            "strength",
            "long_strength",
            "strong_strength",
            "weakness",
            "long_weakness",
            "slow_falling",
            "long_slow_falling",
            "wind_charged",
            "weaving",
            "oozing",
            "infested");

    /** Vanilla potion id → its Oasis variant. */
    private static final Map<ResourceLocation, RegistrySupplier<Potion>> VARIANTS = new LinkedHashMap<>();

    static {
        for (String base : VANILLA_BASES) {
            ResourceLocation baseId = ResourceLocation.withDefaultNamespace(base);
            VARIANTS.put(baseId, ModRegistries.POTIONS.register("oasis_" + base, () -> createVariant(base)));
        }
    }

    private OasisPotions() {}

    /** Loads this class so its entries are added to the deferred registers. */
    public static void init() {}

    private static Potion createVariant(String base) {
        Potion vanilla = BuiltInRegistries.POTION.getOrThrow(
                ResourceKey.create(Registries.POTION, ResourceLocation.withDefaultNamespace(base)));
        MobEffectInstance[] effects =
                vanilla.getEffects().stream().map(OasisPotions::extend).toArray(MobEffectInstance[]::new);
        // The name picks the lang key (item.minecraft.potion.effect.<name>); variants share vanilla's names.
        return new Potion(nameOf(base), effects);
    }

    /** Vanilla names its long_/strong_ potions after the base potion. */
    static String nameOf(String base) {
        if (base.startsWith("long_")) {
            return base.substring("long_".length());
        }
        if (base.startsWith("strong_")) {
            return base.substring("strong_".length());
        }
        return base;
    }

    /** The effect with 1.5x its duration; instant effects are returned unchanged. */
    public static MobEffectInstance extend(MobEffectInstance effect) {
        if (effect.getEffect().value().isInstantenous() || effect.isInfiniteDuration()) {
            return new MobEffectInstance(effect);
        }
        return new MobEffectInstance(
                effect.getEffect(),
                extendDuration(effect.getDuration()),
                effect.getAmplifier(),
                effect.isAmbient(),
                effect.isVisible(),
                effect.showIcon());
    }

    public static int extendDuration(int ticks) {
        return Math.round(ticks * DURATION_MULTIPLIER);
    }

    /** The Oasis variant of a vanilla potion, if it has one. */
    public static Optional<Holder<Potion>> variantOf(Holder<Potion> potion) {
        return potion.unwrapKey()
                .map(key -> VARIANTS.get(key.location()))
                .map(variant -> BuiltInRegistries.POTION.wrapAsHolder(variant.get()));
    }

    /** The Oasis variant of a vanilla potion, or the potion itself if it has none (for example, healing). */
    public static Holder<Potion> variantOrSelf(Holder<Potion> potion) {
        return variantOf(potion).orElse(potion);
    }

    public static boolean isVariant(Holder<Potion> potion) {
        return VARIANTS.values().stream().anyMatch(variant -> potion.value() == variant.get());
    }
}
