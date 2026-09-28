package io.mcmaster.someonesperfectend.worldgen;

import com.mojang.serialization.MapCodec;
import dev.architectury.registry.level.biome.BiomeModifications;
import dev.architectury.registry.registries.RegistrySupplier;
import io.mcmaster.someonesperfectend.SomeonesPerfectEnd;
import io.mcmaster.someonesperfectend.registry.ModRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/** Feature and density function types, plus keys of the data-driven configured features that code refers to. */
public final class ModFeatures {
    public static final RegistrySupplier<HugeGlowcapFeature> HUGE_GLOWCAP = ModRegistries.FEATURES.register(
            "huge_glowcap", () -> new HugeGlowcapFeature(HugeMushroomFeatureConfiguration.CODEC));

    public static final RegistrySupplier<AstralTreeFeature> ASTRAL_TREE = ModRegistries.FEATURES.register(
            "astral_tree", () -> new AstralTreeFeature(AstralTreeConfiguration.CODEC));

    public static final RegistrySupplier<BounceCapClusterFeature> BOUNCE_CAP_CLUSTER = ModRegistries.FEATURES.register(
            "bounce_cap_cluster", () -> new BounceCapClusterFeature(BounceCapClusterConfiguration.CODEC));

    public static final RegistrySupplier<OasisPoolFeature> OASIS_POOL = ModRegistries.FEATURES.register(
            "oasis_pool", () -> new OasisPoolFeature(OasisPoolConfiguration.CODEC));

    public static final RegistrySupplier<UnderIslandFoliageFeature> UNDER_ISLAND_FOLIAGE =
            ModRegistries.FEATURES.register("under_island_foliage",
                    () -> new UnderIslandFoliageFeature(UnderIslandFoliageConfiguration.CODEC));

    /** Density function type used by the End noise settings override (US-012). */
    public static final RegistrySupplier<MapCodec<ElevationBandsFunction>> ELEVATION_BANDS =
            ModRegistries.DENSITY_FUNCTION_TYPES.register("elevation_bands", () -> ElevationBandsFunction.CODEC);

    /** {@code data/someonesperfectend/worldgen/configured_feature/huge_glowcap.json}. */
    public static final ResourceKey<ConfiguredFeature<?, ?>> HUGE_GLOWCAP_CONFIGURED = configured("huge_glowcap");
    /** {@code data/someonesperfectend/worldgen/configured_feature/astral_tree.json}. */
    public static final ResourceKey<ConfiguredFeature<?, ?>> ASTRAL_TREE_CONFIGURED = configured("astral_tree");
    /** {@code astral_tree} with a 3-block island-edge overhang limit, used by {@code end_forest} (US-015). */
    public static final ResourceKey<ConfiguredFeature<?, ?>> END_FOREST_ASTRAL_TREE_CONFIGURED =
            configured("end_forest_astral_tree");

    /** {@code data/someonesperfectend/worldgen/configured_feature/bounce_cap_cluster.json}. */
    public static final ResourceKey<ConfiguredFeature<?, ?>> BOUNCE_CAP_CLUSTER_CONFIGURED =
            configured("bounce_cap_cluster");

    /** {@code data/someonesperfectend/worldgen/configured_feature/oasis_pool.json} (US-016). */
    public static final ResourceKey<ConfiguredFeature<?, ?>> OASIS_POOL_CONFIGURED = configured("oasis_pool");

    /** {@code data/someonesperfectend/worldgen/configured_feature/under_island_foliage.json} (US-017). */
    public static final ResourceKey<ConfiguredFeature<?, ?>> UNDER_ISLAND_FOLIAGE_CONFIGURED =
            configured("under_island_foliage");

    /** Placed in about 10% of vanilla {@code end_highlands} regions through a biome modification (US-017). */
    public static final ResourceKey<PlacedFeature> END_HIGHLANDS_UNDER_ISLAND_FOLIAGE = ResourceKey.create(
            Registries.PLACED_FEATURE, SomeonesPerfectEnd.id("end_highlands_under_island_foliage"));

    private ModFeatures() {
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> configured(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, SomeonesPerfectEnd.id(name));
    }

    /** Loads this class so its entries are added to the deferred registers, and adds features to vanilla biomes. */
    public static void init() {
        BiomeModifications.addProperties(context -> context.getKey().filter(Biomes.END_HIGHLANDS.location()::equals)
                .isPresent(), (context, properties) -> properties.getGenerationProperties().addFeature(
                        GenerationStep.Decoration.VEGETAL_DECORATION, END_HIGHLANDS_UNDER_ISLAND_FOLIAGE));
    }
}
