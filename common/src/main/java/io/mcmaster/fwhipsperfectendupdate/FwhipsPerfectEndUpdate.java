package io.mcmaster.fwhipsperfectendupdate;

import io.mcmaster.fwhipsperfectendupdate.alchemy.OasisPotions;
import io.mcmaster.fwhipsperfectendupdate.block.EnderBeds;
import io.mcmaster.fwhipsperfectendupdate.block.ModBlocks;
import io.mcmaster.fwhipsperfectendupdate.effect.ModEffects;
import io.mcmaster.fwhipsperfectendupdate.entity.Driftjellies;
import io.mcmaster.fwhipsperfectendupdate.entity.Endlings;
import io.mcmaster.fwhipsperfectendupdate.entity.ModBoats;
import io.mcmaster.fwhipsperfectendupdate.fluid.ModFluids;
import io.mcmaster.fwhipsperfectendupdate.infuser.InfusedGear;
import io.mcmaster.fwhipsperfectendupdate.infuser.Infusers;
import io.mcmaster.fwhipsperfectendupdate.registry.ModRegistries;
import io.mcmaster.fwhipsperfectendupdate.sound.ModSounds;
import io.mcmaster.fwhipsperfectendupdate.totem.EffectTotems;
import io.mcmaster.fwhipsperfectendupdate.trinket.Trinkets;
import io.mcmaster.fwhipsperfectendupdate.worldgen.EndlingVillages;
import io.mcmaster.fwhipsperfectendupdate.worldgen.ModFeatures;
import net.minecraft.resources.ResourceLocation;

public final class FwhipsPerfectEndUpdate {
    public static final String MOD_ID = "fwhipsperfectendupdate";
    public static final String MOD_NAME = "fWhip's Perfect End Update";

    private FwhipsPerfectEndUpdate() {}

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
