package io.mcmaster.fwhipsperfectendupdate.fabric.client;

import io.mcmaster.fwhipsperfectendupdate.client.FwhipsPerfectEndUpdateClient;
import io.mcmaster.fwhipsperfectendupdate.fluid.ModFluids;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;

public final class FwhipsPerfectEndUpdateFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FwhipsPerfectEndUpdateClient.init();
        // Fixed textures and no tint: Oasis Water looks the same in every biome.
        FluidRenderHandlerRegistry.INSTANCE.register(
                ModFluids.OASIS_WATER.get(),
                ModFluids.FLOWING_OASIS_WATER.get(),
                new SimpleFluidRenderHandler(
                        ModFluids.STILL_TEXTURE, ModFluids.FLOWING_TEXTURE, ModFluids.OVERLAY_TEXTURE));
    }
}
