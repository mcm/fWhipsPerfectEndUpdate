package io.mcmaster.someonesperfectend.registry;

import com.mojang.serialization.MapCodec;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import io.mcmaster.someonesperfectend.SomeonesPerfectEnd;
import io.mcmaster.someonesperfectend.block.ModBlocks;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.material.Fluid;

/**
 * All of the mod's deferred registers. Content classes (blocks, items, ...) add entries through the
 * helpers here; {@link #register()} is called once from common init on both loaders.
 */
public final class ModRegistries {
    public static final DeferredRegister<Block> BLOCKS = create(Registries.BLOCK);
    public static final DeferredRegister<Item> ITEMS = create(Registries.ITEM);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = create(Registries.BLOCK_ENTITY_TYPE);
    public static final DeferredRegister<MenuType<?>> MENUS = create(Registries.MENU);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = create(Registries.ENTITY_TYPE);
    public static final DeferredRegister<Fluid> FLUIDS = create(Registries.FLUID);
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            create(Registries.DATA_COMPONENT_TYPE);
    public static final DeferredRegister<Potion> POTIONS = create(Registries.POTION);
    public static final DeferredRegister<Feature<?>> FEATURES = create(Registries.FEATURE);
    public static final DeferredRegister<MapCodec<? extends DensityFunction>> DENSITY_FUNCTION_TYPES =
            create(Registries.DENSITY_FUNCTION_TYPE);
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = create(Registries.STRUCTURE_TYPE);
    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES = create(Registries.STRUCTURE_PIECE);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = create(Registries.CREATIVE_MODE_TAB);

    public static final String TAB_TRANSLATION_KEY = "itemGroup." + SomeonesPerfectEnd.MOD_ID + ".main";

    public static final RegistrySupplier<CreativeModeTab> CREATIVE_TAB = CREATIVE_TABS.register(
            "main",
            () -> CreativeTabRegistry.create(
                    Component.translatable(TAB_TRANSLATION_KEY), () -> new ItemStack(ModBlocks.END_TURF.get())));

    private static boolean registered;

    private ModRegistries() {}

    private static <T> DeferredRegister<T> create(
            net.minecraft.resources.ResourceKey<net.minecraft.core.Registry<T>> key) {
        return DeferredRegister.create(SomeonesPerfectEnd.MOD_ID, key);
    }

    /** Item properties that place the item in the mod's creative tab. */
    public static Item.Properties itemProperties() {
        return new Item.Properties().arch$tab(CREATIVE_TAB);
    }

    public static <T extends Item> RegistrySupplier<T> item(String name, Supplier<T> item) {
        return ITEMS.register(name, item);
    }

    public static RegistrySupplier<Item> simpleItem(String name) {
        return item(name, () -> new Item(itemProperties()));
    }

    /** Registers a block without a block item. */
    public static <T extends Block> RegistrySupplier<T> blockOnly(String name, Supplier<T> block) {
        return BLOCKS.register(name, block);
    }

    /** Registers a block plus a {@link BlockItem} in the mod's creative tab. */
    public static <T extends Block> RegistrySupplier<T> block(String name, Supplier<T> block) {
        return block(name, block, b -> new BlockItem(b, itemProperties()));
    }

    public static <T extends Block> RegistrySupplier<T> block(
            String name, Supplier<T> block, Function<T, ? extends Item> itemFactory) {
        RegistrySupplier<T> registered = BLOCKS.register(name, block);
        ITEMS.register(name, () -> itemFactory.apply(registered.get()));
        return registered;
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        // Fabric registers entries immediately, in this order: liquid blocks and buckets need their fluid.
        FLUIDS.register();
        DATA_COMPONENT_TYPES.register();
        BLOCKS.register();
        ITEMS.register();
        BLOCK_ENTITY_TYPES.register();
        MENUS.register();
        POTIONS.register();
        ENTITY_TYPES.register();
        FEATURES.register();
        DENSITY_FUNCTION_TYPES.register();
        STRUCTURE_TYPES.register();
        STRUCTURE_PIECES.register();
        CREATIVE_TABS.register();
    }
}
