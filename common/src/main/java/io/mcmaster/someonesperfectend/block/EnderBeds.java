package io.mcmaster.someonesperfectend.block;

import dev.architectury.registry.registries.RegistrySupplier;
import io.mcmaster.someonesperfectend.SomeonesPerfectEnd;
import io.mcmaster.someonesperfectend.registry.ModRegistries;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

/** The 16 hanging Ender Beds (US-025), one per dye color, bartered from Endlings for the vanilla bed of that color. */
public final class EnderBeds {
    public static final TagKey<Block> BLOCK_TAG = TagKey.create(Registries.BLOCK, SomeonesPerfectEnd.id("ender_beds"));
    public static final TagKey<Item> ITEM_TAG = TagKey.create(Registries.ITEM, SomeonesPerfectEnd.id("ender_beds"));

    /** Beds by color, in vanilla dye order. */
    public static final Map<DyeColor, RegistrySupplier<EnderBedBlock>> BEDS = new EnumMap<>(DyeColor.class);

    static {
        for (DyeColor color : DyeColor.values()) {
            BEDS.put(
                    color,
                    ModRegistries.block(
                            name(color),
                            () -> new EnderBedBlock(
                                    color,
                                    BlockBehaviour.Properties.of()
                                            .mapColor(color.getMapColor())
                                            .sound(SoundType.WOOD)
                                            .strength(0.2F)
                                            .noOcclusion()
                                            .ignitedByLava()
                                            .pushReaction(PushReaction.DESTROY)),
                            block -> new BedItem(
                                    block, ModRegistries.itemProperties().stacksTo(1))));
        }
    }

    private EnderBeds() {}

    public static String name(DyeColor color) {
        return color.getName() + "_ender_bed";
    }

    public static EnderBedBlock get(DyeColor color) {
        return BEDS.get(color).get();
    }

    /** Class-loads the registrations; call before {@code ModRegistries.register()}. */
    public static void init() {}
}
