package io.mcmaster.someonesperfectend.entity;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.registry.registries.RegistrySupplier;
import io.mcmaster.someonesperfectend.registry.ModRegistries;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;

/**
 * Astral boats and Voidvine rafts, with and without chests. Entity types and items share ids ({@code astral_boat},
 * {@code astral_chest_boat}, {@code voidvine_raft}, {@code voidvine_chest_raft}).
 */
public final class ModBoats {
    private static final Map<ModBoatType, RegistrySupplier<EntityType<ModBoat>>> BOATS =
            new EnumMap<>(ModBoatType.class);
    private static final Map<ModBoatType, RegistrySupplier<EntityType<ModChestBoat>>> CHEST_BOATS =
            new EnumMap<>(ModBoatType.class);
    private static final Map<ModBoatType, RegistrySupplier<ModBoatItem>> BOAT_ITEMS = new EnumMap<>(ModBoatType.class);
    private static final Map<ModBoatType, RegistrySupplier<ModBoatItem>> CHEST_BOAT_ITEMS =
            new EnumMap<>(ModBoatType.class);

    static {
        for (ModBoatType type : ModBoatType.values()) {
            String boatId = type.id(false);
            String chestId = type.id(true);
            // Same size and tracking range as vanilla boats.
            BOATS.put(
                    type,
                    ModRegistries.ENTITY_TYPES.register(
                            boatId,
                            () -> EntityType.Builder.<ModBoat>of(
                                            (entityType, level) -> new ModBoat(entityType, level, type),
                                            MobCategory.MISC)
                                    .sized(1.375F, 0.5625F)
                                    .eyeHeight(0.5625F)
                                    .clientTrackingRange(10)
                                    .build(boatId)));
            CHEST_BOATS.put(
                    type,
                    ModRegistries.ENTITY_TYPES.register(
                            chestId,
                            () -> EntityType.Builder.<ModChestBoat>of(
                                            (entityType, level) -> new ModChestBoat(entityType, level, type),
                                            MobCategory.MISC)
                                    .sized(1.375F, 0.5625F)
                                    .eyeHeight(0.5625F)
                                    .clientTrackingRange(10)
                                    .build(chestId)));
            BOAT_ITEMS.put(
                    type,
                    ModRegistries.item(
                            boatId,
                            () -> new ModBoatItem(
                                    type, false, ModRegistries.itemProperties().stacksTo(1))));
            CHEST_BOAT_ITEMS.put(
                    type,
                    ModRegistries.item(
                            chestId,
                            () -> new ModBoatItem(
                                    type, true, ModRegistries.itemProperties().stacksTo(1))));
        }
    }

    private ModBoats() {}

    public static void init() {
        LifecycleEvent.SETUP.register(() -> {
            for (ModBoatType type : ModBoatType.values()) {
                DispenserBlock.registerBehavior(item(type, false).get(), new ModBoatDispenseBehavior(type, false));
                DispenserBlock.registerBehavior(item(type, true).get(), new ModBoatDispenseBehavior(type, true));
            }
        });
    }

    public static RegistrySupplier<? extends EntityType<? extends Boat>> entityType(ModBoatType type, boolean chest) {
        return chest ? CHEST_BOATS.get(type) : BOATS.get(type);
    }

    public static RegistrySupplier<ModBoatItem> item(ModBoatType type, boolean chest) {
        return chest ? CHEST_BOAT_ITEMS.get(type) : BOAT_ITEMS.get(type);
    }

    public static Boat create(Level level, ModBoatType type, boolean chest, double x, double y, double z) {
        Boat boat = entityType(type, chest).get().create(level);
        boat.setPos(x, y, z);
        boat.xo = x;
        boat.yo = y;
        boat.zo = z;
        return boat;
    }

    /** Swaps the vanilla shape's planks (dropped when a boat breaks from a fall) for the boat's own planks. */
    static ItemLike replacePlanks(ModBoatType type, ItemLike item) {
        return item.asItem() == type.shape().getPlanks().asItem() ? type.planks() : item;
    }
}
