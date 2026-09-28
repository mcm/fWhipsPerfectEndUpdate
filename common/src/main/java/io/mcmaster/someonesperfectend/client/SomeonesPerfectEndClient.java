package io.mcmaster.someonesperfectend.client;

import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import io.mcmaster.someonesperfectend.block.ModBlocks;
import io.mcmaster.someonesperfectend.fluid.ModFluids;
import io.mcmaster.someonesperfectend.infuser.InfuserMenu;
import io.mcmaster.someonesperfectend.infuser.Infusers;
import io.mcmaster.someonesperfectend.item.ModItems;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;

/** Common client init; called from each loader's client entrypoint. */
public final class SomeonesPerfectEndClient {
    private SomeonesPerfectEndClient() {
    }

    public static void init() {
        // NeoForge collects screens in RegisterMenuScreensEvent, which can fire before CLIENT_SETUP.
        Infusers.MENU.listen(type -> MenuRegistry.<BeaconMenu, InfuserScreen>registerScreenFactory(type,
                (menu, inventory, title) -> new InfuserScreen((InfuserMenu) menu, inventory, title)));
        ClientLifecycleEvent.CLIENT_SETUP.register(minecraft -> {
            registerRenderTypes();
            registerTrinketRenderers();
            BlockEntityRendererProvider<BeaconBlockEntity> beaconRenderer = BeaconRenderer::new;
            BlockEntityRendererRegistry.register(Infusers.BLOCK_ENTITY.get(), beaconRenderer);
        });
    }

    /** Every trinket item needs a renderer so it shows on the player model (see {@link TrinketRenderer}). */
    private static void registerTrinketRenderers() {
        AccessoriesRendererRegistry.registerRenderer(ModItems.RESONANCE_CHARM.get(), TrinketRenderer::new);
    }

    private static void registerRenderTypes() {
        RenderTypeRegistry.register(RenderType.cutoutMipped(), ModBlocks.END_TURF.get());
        RenderTypeRegistry.register(RenderType.cutout(), ModBlocks.SHORT_VOID_GRASS.get(), ModBlocks.VOID_PETALS.get(),
                ModBlocks.GLOWCAP.get(), ModBlocks.POTTED_GLOWCAP.get(), ModBlocks.INFUSER.get());
        RenderTypeRegistry.register(RenderType.translucent(), ModFluids.OASIS_WATER.get(),
                ModFluids.FLOWING_OASIS_WATER.get());
    }
}
