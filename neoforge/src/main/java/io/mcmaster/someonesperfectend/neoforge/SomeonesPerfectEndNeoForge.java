package io.mcmaster.someonesperfectend.neoforge;

import io.mcmaster.someonesperfectend.SomeonesPerfectEnd;
import io.mcmaster.someonesperfectend.client.SomeonesPerfectEndClient;
import io.mcmaster.someonesperfectend.gametest.ModGameTests;
import io.mcmaster.someonesperfectend.worldgen.ModBiomes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

@Mod(SomeonesPerfectEnd.MOD_ID)
public final class SomeonesPerfectEndNeoForge {
    public SomeonesPerfectEndNeoForge(IEventBus modBus) {
        SomeonesPerfectEnd.init();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            SomeonesPerfectEndClient.init();
        }
        // TerraBlender's registries aren't thread-safe; common setup runs mods in parallel.
        modBus.addListener(FMLCommonSetupEvent.class, event -> event.enqueueWork(ModBiomes::registerPlacement));
        modBus.addListener(RegisterGameTestsEvent.class, event -> event.register(ModGameTests.class));
    }
}
