package io.mcmaster.someonesperfectend;

import io.mcmaster.someonesperfectend.alchemy.OasisPotions;
import io.mcmaster.someonesperfectend.block.EnderBeds;
import io.mcmaster.someonesperfectend.block.ModBlocks;
import io.mcmaster.someonesperfectend.effect.ModEffects;
import io.mcmaster.someonesperfectend.entity.Driftjellies;
import io.mcmaster.someonesperfectend.entity.Endlings;
import io.mcmaster.someonesperfectend.entity.ModBoats;
import io.mcmaster.someonesperfectend.fluid.ModFluids;
import io.mcmaster.someonesperfectend.infuser.InfusedGear;
import io.mcmaster.someonesperfectend.infuser.Infusers;
import io.mcmaster.someonesperfectend.registry.ModRegistries;
import io.mcmaster.someonesperfectend.sound.ModSounds;
import io.mcmaster.someonesperfectend.totem.EffectTotems;
import io.mcmaster.someonesperfectend.trinket.Trinkets;
import io.mcmaster.someonesperfectend.worldgen.EndlingVillages;
import io.mcmaster.someonesperfectend.worldgen.ModFeatures;
import net.minecraft.resources.ResourceLocation;

public final class SomeonesPerfectEnd {
    public static final String MOD_ID = "someonesperfectend";
    public static final String MOD_NAME = "Someone's Perfect End Update";

    private SomeonesPerfectEnd() {}

    /** Common init; called from each loader's entrypoint during mod construction. */
    public static void init() {
        ModSounds.init();
        ModEffects.init();
        ModBlocks.init();
        EnderBeds.init();
        ModBoats.init();
        ModFluids.init();
        Infusers.init();
        InfusedGear.init();
        ModFeatures.init();
        EndlingVillages.init();
        OasisPotions.init();
        EffectTotems.init();
        Trinkets.init();
        ModRegistries.register();
        // Fabric resolves the entity type immediately when registering attributes and spawn placements.
        Driftjellies.init();
        Endlings.init();
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
