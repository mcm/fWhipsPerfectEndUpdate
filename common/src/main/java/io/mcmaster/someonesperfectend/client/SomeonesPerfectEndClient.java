package io.mcmaster.someonesperfectend.client;

import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.event.events.client.ClientTooltipEvent;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import io.mcmaster.someonesperfectend.block.ModBlocks;
import io.mcmaster.someonesperfectend.block.ModWoodTypes;
import io.mcmaster.someonesperfectend.entity.ModBoatType;
import io.mcmaster.someonesperfectend.entity.ModBoats;
import io.mcmaster.someonesperfectend.fluid.ModFluids;
import io.mcmaster.someonesperfectend.infuser.InfuserMenu;
import io.mcmaster.someonesperfectend.infuser.Infusion;
import io.mcmaster.someonesperfectend.infuser.Infusers;
import io.mcmaster.someonesperfectend.totem.EffectTotems;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.state.properties.WoodType;

/** Common client init; called from each loader's client entrypoint. */
public final class SomeonesPerfectEndClient {
    private SomeonesPerfectEndClient() {
    }

    public static void init() {
        // NeoForge collects screens in RegisterMenuScreensEvent, which can fire before CLIENT_SETUP.
        Infusers.MENU.listen(type -> MenuRegistry.<BeaconMenu, InfuserScreen>registerScreenFactory(type,
                (menu, inventory, title) -> new InfuserScreen((InfuserMenu) menu, inventory, title)));
        ClientTooltipEvent.ITEM.register((stack, lines, context, flag) -> {
            Infusion infusion = Infusion.get(stack);
            if (infusion != null) {
                lines.add(Math.min(1, lines.size()), infusion.tooltip().copy().withStyle(ChatFormatting.BLUE));
            }
        });
        registerSignMaterials();
        registerBoatRenderers();
        ClientLifecycleEvent.CLIENT_SETUP.register(minecraft -> {
            registerRenderTypes();
            registerTrinketRenderers();
            BlockEntityRendererProvider<BeaconBlockEntity> beaconRenderer = BeaconRenderer::new;
            BlockEntityRendererRegistry.register(Infusers.BLOCK_ENTITY.get(), beaconRenderer);
        });
    }

    /** Every trinket item needs a renderer so it shows on the player model (see {@link TrinketRenderer}). */
    private static void registerTrinketRenderers() {
        EffectTotems.ALL.forEach(totem -> AccessoriesRendererRegistry.registerRenderer(totem.get(), TrinketRenderer::new));
    }

    private static void registerBoatRenderers() {
        for (ModBoatType type : ModBoatType.values()) {
            for (boolean chest : new boolean[] {false, true}) {
                EntityRendererRegistry.register(ModBoats.entityType(type, chest),
                        context -> new ModBoatRenderer(context, type, chest));
            }
        }
    }

    /**
     * {@link Sheets} snapshots {@link WoodType#values()} when it is first loaded, which may happen before our wood
     * types are registered; add any that are missing.
     */
    private static void registerSignMaterials() {
        for (WoodType type : List.of(ModWoodTypes.ASTRAL, ModWoodTypes.VOIDVINE)) {
            ResourceLocation id = ResourceLocation.parse(type.name());
            Sheets.SIGN_MATERIALS.putIfAbsent(type, new Material(Sheets.SIGN_SHEET, id.withPrefix("entity/signs/")));
            Sheets.HANGING_SIGN_MATERIALS.putIfAbsent(type,
                    new Material(Sheets.SIGN_SHEET, id.withPrefix("entity/signs/hanging/")));
        }
    }

    private static void registerRenderTypes() {
        RenderTypeRegistry.register(RenderType.cutoutMipped(), ModBlocks.END_TURF.get(), ModBlocks.ASTRAL_LEAVES.get(),
                ModBlocks.FLOWERING_ASTRAL_LEAVES.get());
        RenderTypeRegistry.register(RenderType.cutout(), ModBlocks.SHORT_VOID_GRASS.get(), ModBlocks.VOID_PETALS.get(),
                ModBlocks.GLOWCAP.get(), ModBlocks.POTTED_GLOWCAP.get(), ModBlocks.INFUSER.get(),
                ModBlocks.ASTRAL_DOOR.get(), ModBlocks.ASTRAL_TRAPDOOR.get(),
                ModBlocks.VOIDVINE_DOOR.get(), ModBlocks.VOIDVINE_TRAPDOOR.get(),
                ModBlocks.ASTRAL_SAPLING.get(), ModBlocks.POTTED_ASTRAL_SAPLING.get(),
                ModBlocks.VOIDVINE_HEAD.get(), ModBlocks.VOIDVINE_BODY.get());
        RenderTypeRegistry.register(RenderType.translucent(), ModFluids.OASIS_WATER.get(),
                ModFluids.FLOWING_OASIS_WATER.get());
    }
}
