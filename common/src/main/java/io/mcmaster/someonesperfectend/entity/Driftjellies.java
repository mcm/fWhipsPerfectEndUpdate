package io.mcmaster.someonesperfectend.entity;

import dev.architectury.core.item.ArchitecturySpawnEggItem;
import dev.architectury.registry.level.entity.EntityAttributeRegistry;
import dev.architectury.registry.level.entity.SpawnPlacementsRegistry;
import dev.architectury.registry.registries.RegistrySupplier;
import io.mcmaster.someonesperfectend.registry.ModRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The Driftjelly entity, its spawn egg, and its gel drops. Natural spawns are data: the {@code creature} spawner lists
 * of the {@code end_plains}, {@code end_forest}, and {@code end_oasis} biome JSON.
 */
public final class Driftjellies {
    public static final RegistrySupplier<EntityType<Driftjelly>> TYPE = ModRegistries.ENTITY_TYPES.register("driftjelly",
            () -> EntityType.Builder.<Driftjelly>of(Driftjelly::new, MobCategory.CREATURE)
                    .sized(0.75F, 1.0F).eyeHeight(0.5F).clientTrackingRange(8).build("driftjelly"));

    public static final RegistrySupplier<Item> GEL = ModRegistries.item("driftjelly_gel",
            () -> new Item(ModRegistries.itemProperties()
                    .food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.4F).build())));
    public static final RegistrySupplier<Item> COOKED_GEL = ModRegistries.item("cooked_driftjelly_gel",
            () -> new Item(ModRegistries.itemProperties()
                    .food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.6F).build())));
    public static final RegistrySupplier<Item> SPAWN_EGG = ModRegistries.item("driftjelly_spawn_egg",
            () -> new ArchitecturySpawnEggItem(TYPE, 0xC9B6F2, 0x7FE3F0, ModRegistries.itemProperties()));

    private Driftjellies() {
    }

    /** Must run after {@code ModRegistries.register()}. */
    public static void init() {
        EntityAttributeRegistry.register(TYPE, Driftjelly::createAttributes);
        SpawnPlacementsRegistry.register(TYPE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Driftjelly::checkDriftjellySpawnRules);
    }
}
