package io.mcmaster.someonesperfectend.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * Configuration of {@link AstralTreeFeature}.
 *
 * @param trunkProvider   log blocks; the axis is set per log if the state has one
 * @param foliageProvider leaf blocks (a weighted provider mixes in flowering leaves)
 * @param height          total trunk height in blocks
 * @param branchCount     number of side branches
 * @param maxOverhang     if present, the tree is not placed when any of its blocks would stick out further than this
 *                        many blocks past the island edge (used by worldgen; saplings grow without the limit)
 */
public record AstralTreeConfiguration(
        BlockStateProvider trunkProvider,
        BlockStateProvider foliageProvider,
        IntProvider height,
        IntProvider branchCount,
        Optional<Integer> maxOverhang)
        implements FeatureConfiguration {
    public static final Codec<AstralTreeConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    BlockStateProvider.CODEC
                            .fieldOf("trunk_provider")
                            .forGetter(AstralTreeConfiguration::trunkProvider),
                    BlockStateProvider.CODEC
                            .fieldOf("foliage_provider")
                            .forGetter(AstralTreeConfiguration::foliageProvider),
                    IntProvider.codec(1, 32).fieldOf("height").forGetter(AstralTreeConfiguration::height),
                    IntProvider.codec(0, 4).fieldOf("branch_count").forGetter(AstralTreeConfiguration::branchCount),
                    Codec.intRange(0, 8)
                            .optionalFieldOf("max_overhang")
                            .forGetter(AstralTreeConfiguration::maxOverhang))
            .apply(instance, AstralTreeConfiguration::new));
}
