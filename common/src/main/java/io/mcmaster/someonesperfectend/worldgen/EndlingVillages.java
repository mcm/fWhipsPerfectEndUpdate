package io.mcmaster.someonesperfectend.worldgen;

import dev.architectury.registry.registries.RegistrySupplier;
import io.mcmaster.someonesperfectend.SomeonesPerfectEnd;
import io.mcmaster.someonesperfectend.registry.ModRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

/**
 * Registrations for the Endling village structure (US-023). The structure itself
 * ({@code data/someonesperfectend/worldgen/structure/endling_village.json}), its biomes
 * ({@code #someonesperfectend:has_structure/endling_village}) and its rarity
 * ({@code worldgen/structure_set/endling_villages.json}) are data.
 */
public final class EndlingVillages {
    public static final RegistrySupplier<StructureType<EndlingVillageStructure>> STRUCTURE_TYPE =
            ModRegistries.STRUCTURE_TYPES.register("endling_village", () -> () -> EndlingVillageStructure.CODEC);

    public static final RegistrySupplier<StructurePieceType> HUT_PIECE = ModRegistries.STRUCTURE_PIECES.register(
            "endling_village_hut", () -> (StructurePieceType.ContextlessType) EndlingVillagePieces.Hut::new);
    public static final RegistrySupplier<StructurePieceType> GLOWCAP_PIECE = ModRegistries.STRUCTURE_PIECES.register(
            "endling_village_glowcap", () -> (StructurePieceType.ContextlessType) EndlingVillagePieces.Glowcap::new);

    public static final ResourceKey<Structure> STRUCTURE =
            ResourceKey.create(Registries.STRUCTURE, SomeonesPerfectEnd.id("endling_village"));
    public static final ResourceKey<StructureSet> STRUCTURE_SET =
            ResourceKey.create(Registries.STRUCTURE_SET, SomeonesPerfectEnd.id("endling_villages"));

    private EndlingVillages() {
    }

    /** Loads this class so its entries are added to the deferred registers. */
    public static void init() {
    }
}
