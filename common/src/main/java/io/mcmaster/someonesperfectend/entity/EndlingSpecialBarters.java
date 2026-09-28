package io.mcmaster.someonesperfectend.entity;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.mcmaster.someonesperfectend.SomeonesPerfectEnd;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Data-driven special barters (US-034): each {@code data/<ns>/endling_special_barter/<name>.json} maps one
 * {@code input} item to a {@code loot_table} that replaces the default barter table for that item.
 */
public final class EndlingSpecialBarters extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "endling_special_barter";
    public static final ResourceLocation ID = SomeonesPerfectEnd.id(DIRECTORY);
    private static final Logger LOGGER = LogUtils.getLogger();

    public record SpecialBarter(Item input, ResourceKey<LootTable> lootTable) {
        public static final Codec<SpecialBarter> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("input").forGetter(SpecialBarter::input),
                ResourceKey.codec(Registries.LOOT_TABLE).fieldOf("loot_table").forGetter(SpecialBarter::lootTable)
        ).apply(instance, SpecialBarter::new));
    }

    /** Input item → loot table, replaced wholesale on every data reload. */
    private static volatile Map<Item, ResourceKey<LootTable>> barters = Map.of();

    public EndlingSpecialBarters() {
        super(new Gson(), DIRECTORY);
    }

    /** The special barter table for {@code item}, or null if it has none. */
    @Nullable
    public static ResourceKey<LootTable> lootTableFor(Item item) {
        return barters.get(item);
    }

    public static Map<Item, ResourceKey<LootTable>> all() {
        return barters;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<Item, ResourceKey<LootTable>> loaded = new HashMap<>();
        files.forEach((id, json) -> SpecialBarter.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> LOGGER.error("Invalid endling special barter {}: {}", id, error))
                .ifPresent(barter -> {
                    if (loaded.put(barter.input(), barter.lootTable()) != null) {
                        LOGGER.warn("Endling special barter {} replaces another barter for {}", id,
                                BuiltInRegistries.ITEM.getKey(barter.input()));
                    }
                }));
        barters = Map.copyOf(loaded);
        LOGGER.debug("Loaded {} endling special barters", loaded.size());
    }
}
