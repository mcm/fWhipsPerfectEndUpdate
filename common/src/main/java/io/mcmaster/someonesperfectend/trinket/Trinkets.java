package io.mcmaster.someonesperfectend.trinket;

import io.mcmaster.someonesperfectend.SomeonesPerfectEnd;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.api.slot.UniqueSlotHandling;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The mod's single accessory slot, {@code someonesperfectend:trinket}, backed by the Accessories library.
 * It is registered as an Accessories "unique slot" (strict mode, so data packs can't resize it or change what it
 * accepts), bound to players only, and accepts items tagged {@link #TRINKETS}. The library's default slots are
 * unbound from every entity by the shipped data override {@code data/accessories/accessories/entity/default.json}.
 */
public final class Trinkets {
    public static final ResourceLocation SLOT_ID = SomeonesPerfectEnd.id("trinket");
    /** Accessories slot name (unique slots are named by their full id). */
    public static final String SLOT_NAME = SLOT_ID.toString();
    /** Items that can be equipped in the trinket slot. */
    public static final TagKey<Item> TRINKETS = TagKey.create(Registries.ITEM, SomeonesPerfectEnd.id("trinkets"));
    /** Accessories slot predicate: only {@link #TRINKETS} items are valid. */
    public static final ResourceLocation TRINKETS_PREDICATE = SomeonesPerfectEnd.id("trinkets");

    private Trinkets() {}

    public static void init() {
        AccessoriesAPI.registerPredicate(
                TRINKETS_PREDICATE,
                (level, slotType, slot, stack) -> stack.is(TRINKETS) ? TriState.TRUE : TriState.FALSE);
        UniqueSlotHandling.EVENT.register(factory -> factory.create(SLOT_ID, 1)
                .slotPredicates(TRINKETS_PREDICATE)
                .validTypes(EntityType.PLAYER)
                .build());
    }

    /** The Accessories container behind the trinket slot, or null if the entity has none (yet). */
    @Nullable
    public static AccessoriesContainer container(LivingEntity entity) {
        AccessoriesCapability capability = AccessoriesCapability.get(entity);
        return capability == null ? null : capability.getContainers().get(SLOT_NAME);
    }

    public static SlotReference reference(LivingEntity entity) {
        return SlotReference.of(entity, SLOT_NAME, 0);
    }

    /** The item in the entity's trinket slot (empty if none). */
    public static ItemStack getEquipped(LivingEntity entity) {
        AccessoriesContainer container = container(entity);
        return container == null ? ItemStack.EMPTY : container.getAccessories().getItem(0);
    }

    /** Whether {@code stack} may go into the entity's trinket slot (tag check plus the item's own equip rules). */
    public static boolean canEquip(LivingEntity entity, ItemStack stack) {
        return container(entity) != null && AccessoriesAPI.canInsertIntoSlot(stack, reference(entity));
    }
}
