package io.mcmaster.someonesperfectend.fabric.client;

import io.mcmaster.someonesperfectend.client.SomeonesPerfectEndClient;
import io.mcmaster.someonesperfectend.fluid.ModFluids;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;

public final class SomeonesPerfectEndFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SomeonesPerfectEndClient.init();
        // Fixed textures and no tint: Oasis Water looks the same in every biome.
        FluidRenderHandlerRegistry.INSTANCE.register(ModFluids.OASIS_WATER.get(), ModFluids.FLOWING_OASIS_WATER.get(),
                new SimpleFluidRenderHandler(ModFluids.STILL_TEXTURE, ModFluids.FLOWING_TEXTURE,
                        ModFluids.OVERLAY_TEXTURE));
    }
}
