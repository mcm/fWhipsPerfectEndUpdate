package io.mcmaster.someonesperfectend.mixin;

import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The seeded island noise of {@code minecraft:end_islands} (a protected class); used by the elevation bands. */
@Mixin(targets = "net.minecraft.world.level.levelgen.DensityFunctions$EndIslandDensityFunction")
public interface EndIslandDensityFunctionAccessor {
    @Accessor("islandNoise")
    SimplexNoise someonesperfectend$islandNoise();
}
