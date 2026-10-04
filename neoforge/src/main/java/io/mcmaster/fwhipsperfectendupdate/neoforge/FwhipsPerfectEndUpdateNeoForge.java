package io.mcmaster.fwhipsperfectendupdate.neoforge;

import io.mcmaster.fwhipsperfectendupdate.FwhipsPerfectEndUpdate;
import io.mcmaster.fwhipsperfectendupdate.client.FwhipsPerfectEndUpdateClient;
import io.mcmaster.fwhipsperfectendupdate.gametest.ModGameTests;
import io.mcmaster.fwhipsperfectendupdate.worldgen.ModBiomes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

@Mod(FwhipsPerfectEndUpdate.MOD_ID)
public final class FwhipsPerfectEndUpdateNeoForge {
    public FwhipsPerfectEndUpdateNeoForge(IEventBus modBus) {
        FwhipsPerfectEndUpdate.init();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            FwhipsPerfectEndUpdateClient.init();
        }
        // TerraBlender's registries aren't thread-safe; common setup runs mods in parallel.
        modBus.addListener(FMLCommonSetupEvent.class, event -> event.enqueueWork(ModBiomes::registerPlacement));
        modBus.addListener(RegisterGameTestsEvent.class, event -> event.register(ModGameTests.class));
    }
}
