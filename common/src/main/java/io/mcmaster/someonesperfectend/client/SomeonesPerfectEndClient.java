package io.mcmaster.someonesperfectend.client;

import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import io.mcmaster.someonesperfectend.block.ModBlocks;
import io.mcmaster.someonesperfectend.fluid.ModFluids;
import io.mcmaster.someonesperfectend.item.ModItems;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import net.minecraft.client.renderer.RenderType;

/** Common client init; called from each loader's client entrypoint. */
public final class SomeonesPerfectEndClient {
    private SomeonesPerfectEndClient() {
    }

    public static void init() {
        ClientLifecycleEvent.CLIENT_SETUP.register(minecraft -> {
            registerRenderTypes();
            registerTrinketRenderers();
        });
    }

    /** Every trinket item needs a renderer so it shows on the player model (see {@link TrinketRenderer}). */
    private static void registerTrinketRenderers() {
        AccessoriesRendererRegistry.registerRenderer(ModItems.RESONANCE_CHARM.get(), TrinketRenderer::new);
    }

    private static void registerRenderTypes() {
        RenderTypeRegistry.register(RenderType.cutoutMipped(), ModBlocks.END_TURF.get());
        RenderTypeRegistry.register(RenderType.cutout(), ModBlocks.SHORT_VOID_GRASS.get(), ModBlocks.VOID_PETALS.get(),
                ModBlocks.GLOWCAP.get(), ModBlocks.POTTED_GLOWCAP.get());
        RenderTypeRegistry.register(RenderType.translucent(), ModFluids.OASIS_WATER.get(),
                ModFluids.FLOWING_OASIS_WATER.get());
    }
}
