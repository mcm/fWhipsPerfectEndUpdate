package io.mcmaster.someonesperfectend.worldgen;

import io.mcmaster.someonesperfectend.SomeonesPerfectEnd;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import terrablender.api.EndBiomeRegistry;

/**
 * The mod's End biomes (data in {@code data/someonesperfectend/worldgen/biome/}) and their placement through
 * TerraBlender's End biome registry. TerraBlender picks one biome per End category (highlands, midlands, ...) from
 * weighted lists; vanilla's own biome has weight 10 in each category by default (TerraBlender config).
 */
public final class ModBiomes {
    public static final ResourceKey<Biome> END_PLAINS = key("end_plains");
    public static final ResourceKey<Biome> END_FOREST = key("end_forest");
    public static final ResourceKey<Biome> END_OASIS = key("end_oasis");

    public static final List<ResourceKey<Biome>> ALL = List.of(END_PLAINS, END_FOREST, END_OASIS);

    /** Weights next to {@code minecraft:end_highlands} (10). Oasis is the rarest. */
    static final int PLAINS_HIGHLANDS_WEIGHT = 4;
    static final int FOREST_HIGHLANDS_WEIGHT = 4;
    static final int OASIS_HIGHLANDS_WEIGHT = 2;
    /** Weights next to {@code minecraft:end_midlands} (10). */
    static final int PLAINS_MIDLANDS_WEIGHT = 2;
    static final int FOREST_MIDLANDS_WEIGHT = 2;
    static final int OASIS_MIDLANDS_WEIGHT = 1;

    private ModBiomes() {
    }

    private static ResourceKey<Biome> key(String name) {
        return ResourceKey.create(Registries.BIOME, SomeonesPerfectEnd.id(name));
    }

    /**
     * Adds the biomes to TerraBlender's End highlands and midlands. Must run after TerraBlender has loaded its
     * config (Fabric: the {@code terrablender} entrypoint; NeoForge: common setup) and before a server starts.
     */
    public static void registerPlacement() {
        EndBiomeRegistry.registerHighlandsBiome(END_PLAINS, PLAINS_HIGHLANDS_WEIGHT);
        EndBiomeRegistry.registerHighlandsBiome(END_FOREST, FOREST_HIGHLANDS_WEIGHT);
        EndBiomeRegistry.registerHighlandsBiome(END_OASIS, OASIS_HIGHLANDS_WEIGHT);
        EndBiomeRegistry.registerMidlandsBiome(END_PLAINS, PLAINS_MIDLANDS_WEIGHT);
        EndBiomeRegistry.registerMidlandsBiome(END_FOREST, FOREST_MIDLANDS_WEIGHT);
        EndBiomeRegistry.registerMidlandsBiome(END_OASIS, OASIS_MIDLANDS_WEIGHT);
    }
}
