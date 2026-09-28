package io.mcmaster.someonesperfectend.worldgen;

import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;

/** Vanilla {@code TheEndBiomeSource}'s choice of End biome category, reproduced exactly. */
public enum EndBiomeCategories {
    CENTER, HIGHLANDS, MIDLANDS, BARRENS, SMALL_ISLANDS;

    /** The category vanilla picks at the given quart position. */
    public static EndBiomeCategories at(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
        int sectionX = SectionPos.blockToSectionCoord(QuartPos.toBlock(quartX));
        int sectionZ = SectionPos.blockToSectionCoord(QuartPos.toBlock(quartZ));
        if ((long) sectionX * sectionX + (long) sectionZ * sectionZ <= 4096L) {
            return CENTER;
        }
        // Vanilla samples the erosion (the End's 2D island noise) at the section's center.
        double erosion = sampler.erosion().compute(new DensityFunction.SinglePointContext(
                (sectionX * 2 + 1) * 8, QuartPos.toBlock(quartY), (sectionZ * 2 + 1) * 8));
        if (erosion > 0.25) {
            return HIGHLANDS;
        }
        if (erosion >= -0.0625) {
            return MIDLANDS;
        }
        return erosion < -0.21875 ? SMALL_ISLANDS : BARRENS;
    }
}
