package io.mcmaster.someonesperfectend.worldgen;

import dev.architectury.registry.registries.RegistrySupplier;
import io.mcmaster.someonesperfectend.SomeonesPerfectEnd;
import io.mcmaster.someonesperfectend.registry.ModRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;

/** Feature types, plus keys of the data-driven configured features that code refers to. */
public final class ModFeatures {
    public static final RegistrySupplier<HugeGlowcapFeature> HUGE_GLOWCAP = ModRegistries.FEATURES.register(
            "huge_glowcap", () -> new HugeGlowcapFeature(HugeMushroomFeatureConfiguration.CODEC));

    public static final RegistrySupplier<AstralTreeFeature> ASTRAL_TREE = ModRegistries.FEATURES.register(
            "astral_tree", () -> new AstralTreeFeature(AstralTreeConfiguration.CODEC));

    /** {@code data/someonesperfectend/worldgen/configured_feature/huge_glowcap.json}. */
    public static final ResourceKey<ConfiguredFeature<?, ?>> HUGE_GLOWCAP_CONFIGURED = configured("huge_glowcap");
    /** {@code data/someonesperfectend/worldgen/configured_feature/astral_tree.json}. */
    public static final ResourceKey<ConfiguredFeature<?, ?>> ASTRAL_TREE_CONFIGURED = configured("astral_tree");

    private ModFeatures() {
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> configured(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, SomeonesPerfectEnd.id(name));
    }

    /** Loads this class so its entries are added to the deferred registers. */
    public static void init() {
    }
}
