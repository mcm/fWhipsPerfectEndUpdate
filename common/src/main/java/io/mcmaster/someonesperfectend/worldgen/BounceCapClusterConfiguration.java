package io.mcmaster.someonesperfectend.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * Configuration of {@link BounceCapClusterFeature}.
 *
 * @param state          the cap block
 * @param size           number of caps in a cluster
 * @param searchRadius   how far from the placement origin to look for an island edge
 * @param candidates     number of positions tried when looking for an edge
 * @param clusterRadius  horizontal spread of the caps around the chosen position
 */
public record BounceCapClusterConfiguration(
        BlockStateProvider state, IntProvider size, int searchRadius, int candidates, int clusterRadius)
        implements FeatureConfiguration {
    public static final Codec<BounceCapClusterConfiguration> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                            BlockStateProvider.CODEC.fieldOf("state").forGetter(BounceCapClusterConfiguration::state),
                            IntProvider.codec(1, 16).fieldOf("size").forGetter(BounceCapClusterConfiguration::size),
                            Codec.intRange(0, 6)
                                    .fieldOf("search_radius")
                                    .forGetter(BounceCapClusterConfiguration::searchRadius),
                            Codec.intRange(1, 64)
                                    .fieldOf("candidates")
                                    .forGetter(BounceCapClusterConfiguration::candidates),
                            Codec.intRange(0, 2)
                                    .fieldOf("cluster_radius")
                                    .forGetter(BounceCapClusterConfiguration::clusterRadius))
                    .apply(instance, BounceCapClusterConfiguration::new));
}
