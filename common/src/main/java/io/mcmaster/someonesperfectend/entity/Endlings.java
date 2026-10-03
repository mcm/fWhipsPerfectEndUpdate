package io.mcmaster.someonesperfectend.entity;

import dev.architectury.core.item.ArchitecturySpawnEggItem;
import dev.architectury.registry.ReloadListenerRegistry;
import dev.architectury.registry.level.entity.EntityAttributeRegistry;
import dev.architectury.registry.registries.RegistrySupplier;
import io.mcmaster.someonesperfectend.registry.ModRegistries;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;

/** The Endling entity and its spawn egg. Endlings don't spawn naturally; villages (US-023) place them. */
public final class Endlings {
    public static final RegistrySupplier<EntityType<Endling>> TYPE = ModRegistries.ENTITY_TYPES.register(
            "endling",
            () -> EntityType.Builder.<Endling>of(Endling::new, MobCategory.CREATURE)
                    .sized(0.9F, 1.2F)
                    .eyeHeight(0.95F)
                    .clientTrackingRange(8)
                    .build("endling"));

    public static final RegistrySupplier<Item> SPAWN_EGG = ModRegistries.item(
            "endling_spawn_egg",
            () -> new ArchitecturySpawnEggItem(TYPE, 0x161616, 0xCC00FA, ModRegistries.itemProperties()));

    private Endlings() {}

    /** Must run after {@code ModRegistries.register()}. */
    public static void init() {
        EntityAttributeRegistry.register(TYPE, Endling::createAttributes);
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new EndlingSpecialBarters(), EndlingSpecialBarters.ID);
    }
}
