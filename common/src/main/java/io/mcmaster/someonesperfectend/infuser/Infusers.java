package io.mcmaster.someonesperfectend.infuser;

import dev.architectury.registry.registries.RegistrySupplier;
import io.mcmaster.someonesperfectend.SomeonesPerfectEnd;
import io.mcmaster.someonesperfectend.block.ModBlocks;
import io.mcmaster.someonesperfectend.registry.ModRegistries;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Registry entries and tags for the Infuser (US-026); the block itself is {@link ModBlocks#INFUSER}. */
public final class Infusers {
    /** Items the Infuser's infusion slot accepts: boots, chestplates, tools/weapons, and effect totems. */
    public static final TagKey<Item> INFUSABLE = TagKey.create(Registries.ITEM, SomeonesPerfectEnd.id("infusable"));
    /** The five effect totems (filled in US-032). */
    public static final TagKey<Item> EFFECT_TOTEMS =
            TagKey.create(Registries.ITEM, SomeonesPerfectEnd.id("effect_totems"));

    public static final String LOCKED_TOOLTIP_KEY = "container." + SomeonesPerfectEnd.MOD_ID + ".infuser.locked";
    /** The screen's label while an item is in the infusion slot. */
    public static final String INFUSION_LABEL_KEY = "container." + SomeonesPerfectEnd.MOD_ID + ".infuser.infusion";

    @SuppressWarnings("DataFlowIssue") // the data fixer type is nullable
    public static final RegistrySupplier<BlockEntityType<InfuserBlockEntity>> BLOCK_ENTITY =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "infuser",
                    () -> BlockEntityType.Builder.of(InfuserBlockEntity::new, ModBlocks.INFUSER.get())
                            .build(null));

    public static final RegistrySupplier<MenuType<InfuserMenu>> MENU =
            ModRegistries.MENUS.register("infuser", () -> new MenuType<>(InfuserMenu::new, FeatureFlags.VANILLA_SET));

    /** Infused gear: effect, amplifier, and stored time (US-030). Saved and synced to clients. */
    public static final RegistrySupplier<DataComponentType<Infusion>> INFUSION =
            ModRegistries.DATA_COMPONENT_TYPES.register(
                    "infusion",
                    () -> DataComponentType.<Infusion>builder()
                            .persistent(Infusion.CODEC)
                            .networkSynchronized(Infusion.STREAM_CODEC)
                            .build());

    private Infusers() {}

    /** Loads this class so its entries are added to the deferred registers. */
    public static void init() {}
}
