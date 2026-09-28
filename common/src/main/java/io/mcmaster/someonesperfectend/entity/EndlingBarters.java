package io.mcmaster.someonesperfectend.entity;

import io.mcmaster.someonesperfectend.SomeonesPerfectEnd;
import java.util.List;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
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
 *
 * <p>Every barter is locked until the offering player has beaten the Ender Dragon (US-039, {@link #mayBarter}).
 */
public final class EndlingBarters {
    /** Overworld life blocks endlings accept for the default barter. */
    public static final TagKey<Item> INPUTS = TagKey.create(Registries.ITEM,
            SomeonesPerfectEnd.id("endling_barter_inputs"));
    public static final ResourceKey<LootTable> DEFAULT_LOOT_TABLE = ResourceKey.create(Registries.LOOT_TABLE,
            SomeonesPerfectEnd.id("gameplay/endling_bartering"));

    /** The advancement a player needs before endlings barter with them (US-039). */
    public static final ResourceLocation UNLOCK_ADVANCEMENT = ResourceLocation.withDefaultNamespace("end/kill_dragon");

    private EndlingBarters() {
    }

    /**
     * Whether endlings barter with {@code offerer}: the player handing an item over, or the thrower of a dropped item.
     * Only a server player with {@link #UNLOCK_ADVANCEMENT} qualifies, so items with no thrower (dispensers, hoppers,
     * mob drops) or whose thrower is offline are refused.
     */
    public static boolean mayBarter(@Nullable Entity offerer) {
        if (!(offerer instanceof ServerPlayer player)) {
            return false;
        }
        AdvancementHolder advancement = player.server.getAdvancements().get(UNLOCK_ADVANCEMENT);
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
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
