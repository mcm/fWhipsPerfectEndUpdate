package io.mcmaster.someonesperfectend.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * Configuration of {@link OasisPoolFeature}.
 *
 * @param fluid         the pool's fluid block
 * @param radius        horizontal radius of the pool
 * @param maxDepth      depth at the pool's center (it gets shallower toward the rim)
 * @param carpet        the ground cover placed around the pool
 * @param carpetRadius  how far past the pool's rim the carpet reaches
 * @param carpetChance  chance of each free surface block in the carpet ring getting a carpet block
 */
public record OasisPoolConfiguration(BlockStateProvider fluid, IntProvider radius, int maxDepth,
        BlockStateProvider carpet, int carpetRadius, float carpetChance) implements FeatureConfiguration {
    public static final Codec<OasisPoolConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(BlockStateProvider.CODEC.fieldOf("fluid").forGetter(OasisPoolConfiguration::fluid),
                    IntProvider.codec(1, 4).fieldOf("radius").forGetter(OasisPoolConfiguration::radius),
                    Codec.intRange(1, 4).fieldOf("max_depth").forGetter(OasisPoolConfiguration::maxDepth),
                    BlockStateProvider.CODEC.fieldOf("carpet").forGetter(OasisPoolConfiguration::carpet),
                    Codec.intRange(0, 3).fieldOf("carpet_radius").forGetter(OasisPoolConfiguration::carpetRadius),
                    Codec.floatRange(0.0F, 1.0F).fieldOf("carpet_chance")
                            .forGetter(OasisPoolConfiguration::carpetChance))
            .apply(instance, OasisPoolConfiguration::new));
}
