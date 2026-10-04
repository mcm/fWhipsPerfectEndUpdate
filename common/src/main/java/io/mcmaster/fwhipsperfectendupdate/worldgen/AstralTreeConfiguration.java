package io.mcmaster.fwhipsperfectendupdate.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
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
 * @param floweringLogChance chance (0-1) that each log position becomes a Flowering Astral Log instead
 * @param enderblossoms   number of Closed Enderblossoms placed on the trunk sides (fewer if there isn't room)
 */
public record AstralTreeConfiguration(
        BlockStateProvider trunkProvider,
        BlockStateProvider foliageProvider,
        IntProvider height,
        IntProvider branchCount,
        Optional<Integer> maxOverhang,
        float floweringLogChance,
        IntProvider enderblossoms)
        implements FeatureConfiguration {
    public static final float DEFAULT_FLOWERING_LOG_CHANCE = 0.1F;
    public static final IntProvider DEFAULT_ENDERBLOSSOMS = UniformInt.of(1, 4);
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
                            .forGetter(AstralTreeConfiguration::maxOverhang),
                    Codec.floatRange(0.0F, 1.0F)
                            .optionalFieldOf("flowering_log_chance", DEFAULT_FLOWERING_LOG_CHANCE)
                            .forGetter(AstralTreeConfiguration::floweringLogChance),
                    IntProvider.codec(0, 16)
                            .optionalFieldOf("enderblossoms", DEFAULT_ENDERBLOSSOMS)
                            .forGetter(AstralTreeConfiguration::enderblossoms))
            .apply(instance, AstralTreeConfiguration::new));

    /** This configuration with another {@link #floweringLogChance()}. */
    public AstralTreeConfiguration withFloweringLogChance(float chance) {
        return new AstralTreeConfiguration(
                trunkProvider, foliageProvider, height, branchCount, maxOverhang, chance, enderblossoms);
    }

    /** This configuration with another {@link #enderblossoms()}. */
    public AstralTreeConfiguration withEnderblossoms(IntProvider count) {
        return new AstralTreeConfiguration(
                trunkProvider, foliageProvider, height, branchCount, maxOverhang, floweringLogChance, count);
    }
}
