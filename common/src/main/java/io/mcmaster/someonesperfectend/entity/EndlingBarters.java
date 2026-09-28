package io.mcmaster.someonesperfectend.entity;

import io.mcmaster.someonesperfectend.SomeonesPerfectEnd;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.Nullable;

/**
 * What endlings barter (US-022): one item from {@link #INPUTS} rolls the {@link #DEFAULT_LOOT_TABLE}.
 *
 * <p>{@link #lootTableFor} is the single hook that decides whether an endling takes an item and what it gives back;
 * {@linkplain EndlingSpecialBarters data-driven special barters} (US-034: beacon, Totem of Undying, beds for Ender
 * Beds) plug in there.
 */
public final class EndlingBarters {
    /** Overworld life blocks endlings accept for the default barter. */
    public static final TagKey<Item> INPUTS = TagKey.create(Registries.ITEM,
            SomeonesPerfectEnd.id("endling_barter_inputs"));
    public static final ResourceKey<LootTable> DEFAULT_LOOT_TABLE = ResourceKey.create(Registries.LOOT_TABLE,
            SomeonesPerfectEnd.id("gameplay/endling_bartering"));

    private EndlingBarters() {
    }

    /** The loot table bartering {@code stack} rolls, or null if endlings don't take it. */
    @Nullable
    public static ResourceKey<LootTable> lootTableFor(ItemStack stack) {
        if (stack.is(INPUTS)) {
            return DEFAULT_LOOT_TABLE;
        }
        return EndlingSpecialBarters.lootTableFor(stack.getItem());
    }

    public static boolean isInput(ItemStack stack) {
        return !stack.isEmpty() && lootTableFor(stack) != null;
    }

    /** Rolls a barter table like a Piglin barter ({@code minecraft:barter} context, the endling as this entity). */
    public static List<ItemStack> roll(Endling endling, ResourceKey<LootTable> table) {
        ServerLevel level = (ServerLevel) endling.level();
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, endling)
                .create(LootContextParamSets.PIGLIN_BARTER);
        return level.getServer().reloadableRegistries().getLootTable(table).getRandomItems(params);
    }
}
