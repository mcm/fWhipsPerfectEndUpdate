package io.mcmaster.someonesperfectend.item;

import dev.architectury.registry.registries.RegistrySupplier;
import io.mcmaster.someonesperfectend.registry.ModRegistries;
import io.wispforest.accessories.api.AccessoriesAPI;
import net.minecraft.world.item.Rarity;

/** Standalone items (block items live with their blocks in {@code ModBlocks}). */
public final class ModItems {
    /** Test trinket (US-024): Haste I while worn in the trinket slot. Creative-only; replaced by totems in US-032. */
    public static final RegistrySupplier<ResonanceCharmItem> RESONANCE_CHARM = ModRegistries.item("resonance_charm",
            () -> new ResonanceCharmItem(ModRegistries.itemProperties().stacksTo(1).rarity(Rarity.EPIC)));

    private ModItems() {
    }

    public static void init() {
        RESONANCE_CHARM.listen(item -> AccessoriesAPI.registerAccessory(item, item));
    }
}
