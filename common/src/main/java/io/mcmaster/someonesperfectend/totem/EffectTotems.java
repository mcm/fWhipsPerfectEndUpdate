package io.mcmaster.someonesperfectend.totem;

import dev.architectury.registry.registries.RegistrySupplier;
import io.mcmaster.someonesperfectend.SomeonesPerfectEnd;
import io.mcmaster.someonesperfectend.infuser.InfusionEffect;
import io.mcmaster.someonesperfectend.registry.ModRegistries;
import io.mcmaster.someonesperfectend.trinket.Trinkets;
import io.wispforest.accessories.api.AccessoriesAPI;
import java.util.List;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

/** The five effect totems (US-032) and their activation component. */
public final class EffectTotems {
    public static final String ACTIVE_TOOLTIP_KEY = "tooltip." + SomeonesPerfectEnd.MOD_ID + ".totem.active";
    public static final String INACTIVE_TOOLTIP_KEY = "tooltip." + SomeonesPerfectEnd.MOD_ID + ".totem.inactive";

    /** Present on a totem once it has been activated with netherite at a tier-4 Infuser in the End. */
    public static final RegistrySupplier<DataComponentType<Unit>> TOTEM_ACTIVATED =
            ModRegistries.DATA_COMPONENT_TYPES.register(
                    "totem_activated",
                    () -> DataComponentType.<Unit>builder()
                            .persistent(Unit.CODEC)
                            .networkSynchronized(StreamCodec.unit(Unit.INSTANCE))
                            .build());

    public static final RegistrySupplier<EffectTotemItem> TOTEM_OF_SPEED = totem("speed", InfusionEffect.SPEED);
    public static final RegistrySupplier<EffectTotemItem> TOTEM_OF_HASTE = totem("haste", InfusionEffect.HASTE);
    public static final RegistrySupplier<EffectTotemItem> TOTEM_OF_RESISTANCE =
            totem("resistance", InfusionEffect.RESISTANCE);
    public static final RegistrySupplier<EffectTotemItem> TOTEM_OF_JUMP_BOOST =
            totem("jump_boost", InfusionEffect.JUMP_BOOST);
    public static final RegistrySupplier<EffectTotemItem> TOTEM_OF_STRENGTH =
            totem("strength", InfusionEffect.STRENGTH);

    public static final List<RegistrySupplier<EffectTotemItem>> ALL =
            List.of(TOTEM_OF_SPEED, TOTEM_OF_HASTE, TOTEM_OF_RESISTANCE, TOTEM_OF_JUMP_BOOST, TOTEM_OF_STRENGTH);

    private EffectTotems() {}

    private static RegistrySupplier<EffectTotemItem> totem(String name, InfusionEffect effect) {
        return ModRegistries.item(
                "totem_of_" + name,
                () -> new EffectTotemItem(
                        effect, ModRegistries.itemProperties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    }

    public static boolean isActive(ItemStack stack) {
        return stack.has(TOTEM_ACTIVATED.get());
    }

    /** Whether the entity wears an active effect totem in its trinket slot (it suppresses infused gear, US-033). */
    public static boolean hasActiveTotemEquipped(LivingEntity entity) {
        ItemStack stack = Trinkets.getEquipped(entity);
        return stack.getItem() instanceof EffectTotemItem && isActive(stack);
    }

    /** An active copy of a totem, for creative use and tests. */
    public static ItemStack activated(EffectTotemItem item) {
        ItemStack stack = new ItemStack(item);
        stack.set(TOTEM_ACTIVATED.get(), Unit.INSTANCE);
        return stack;
    }

    /** Registers the totems' trinket behavior. */
    public static void init() {
        ALL.forEach(totem -> totem.listen(item -> AccessoriesAPI.registerAccessory(item, item)));
    }
}
