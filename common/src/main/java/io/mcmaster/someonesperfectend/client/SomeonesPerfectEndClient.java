package io.mcmaster.someonesperfectend.client;

import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import io.mcmaster.someonesperfectend.block.ModBlocks;
import io.mcmaster.someonesperfectend.fluid.ModFluids;
import net.minecraft.client.renderer.RenderType;

/** Common client init; called from each loader's client entrypoint. */
public final class SomeonesPerfectEndClient {
    private SomeonesPerfectEndClient() {
    }

    public static void init() {
        ClientLifecycleEvent.CLIENT_SETUP.register(minecraft -> registerRenderTypes());
    }

    private static void registerRenderTypes() {
        RenderTypeRegistry.register(RenderType.cutoutMipped(), ModBlocks.END_TURF.get());
        RenderTypeRegistry.register(RenderType.cutout(), ModBlocks.SHORT_VOID_GRASS.get());
        RenderTypeRegistry.register(RenderType.translucent(), ModFluids.OASIS_WATER.get(),
                ModFluids.FLOWING_OASIS_WATER.get());
    }
}
