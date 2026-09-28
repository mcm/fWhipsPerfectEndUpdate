package io.mcmaster.someonesperfectend.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * Configuration of {@link UnderIslandFoliageFeature}.
 *
 * @param soil          the block that replaces the island's exposed bottom layer
 * @param replaceable   which bottom blocks may be replaced by {@code soil}
 * @param radius        horizontal radius of the soil patch
 * @param vineChance    chance of each soil block getting a hanging vine
 * @param vineLength    number of vine segments (clipped by the space below and {@code minY})
 * @param fruitChance   chance of each vine segment carrying fruit
 * @param minY          no vine segment is placed below this height
 * @param regionSize    size in blocks of the square regions that {@code regionChance} is rolled for
 * @param regionChance  chance that a region gets foliage at all (1 = everywhere)
 */
public record UnderIslandFoliageConfiguration(BlockStateProvider soil, BlockPredicate replaceable, IntProvider radius,
        float vineChance, IntProvider vineLength, float fruitChance, VerticalAnchor minY, int regionSize,
        float regionChance) implements FeatureConfiguration {
    public static final Codec<UnderIslandFoliageConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(BlockStateProvider.CODEC.fieldOf("soil").forGetter(UnderIslandFoliageConfiguration::soil),
                    BlockPredicate.CODEC.fieldOf("replaceable")
                            .forGetter(UnderIslandFoliageConfiguration::replaceable),
                    IntProvider.codec(0, 4).fieldOf("radius").forGetter(UnderIslandFoliageConfiguration::radius),
                    Codec.floatRange(0.0F, 1.0F).fieldOf("vine_chance")
                            .forGetter(UnderIslandFoliageConfiguration::vineChance),
                    IntProvider.codec(1, 16).fieldOf("vine_length")
                            .forGetter(UnderIslandFoliageConfiguration::vineLength),
                    Codec.floatRange(0.0F, 1.0F).fieldOf("fruit_chance")
                            .forGetter(UnderIslandFoliageConfiguration::fruitChance),
                    VerticalAnchor.CODEC.fieldOf("min_y").forGetter(UnderIslandFoliageConfiguration::minY),
                    Codec.intRange(16, 1024).optionalFieldOf("region_size", 64)
                            .forGetter(UnderIslandFoliageConfiguration::regionSize),
                    Codec.floatRange(0.0F, 1.0F).optionalFieldOf("region_chance", 1.0F)
                            .forGetter(UnderIslandFoliageConfiguration::regionChance))
            .apply(instance, UnderIslandFoliageConfiguration::new));
}
