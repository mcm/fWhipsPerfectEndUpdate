package io.mcmaster.someonesperfectend.mixin;

import io.mcmaster.someonesperfectend.worldgen.EndBiomeCategories;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps vanilla's {@code small_end_islands} with TerraBlender installed. TerraBlender 4.1 (1.21.1) builds its
 * "small islands" area from the edge-biome list, so without this the void between islands becomes
 * {@code end_barrens} and loses the small floating islands feature. Runs before TerraBlender's HEAD injection
 * (lower priority) and answers only for the small-islands category; everything else goes to TerraBlender.
 */
@Mixin(value = TheEndBiomeSource.class, priority = 900)
public abstract class TheEndBiomeSourceMixin {
    @Shadow
    @Final
    private Holder<Biome> islands;

    @Inject(method = "getNoiseBiome", at = @At("HEAD"), cancellable = true)
    private void someonesperfectend$keepSmallIslands(
            int x, int y, int z, Climate.Sampler sampler, CallbackInfoReturnable<Holder<Biome>> cir) {
        if (EndBiomeCategories.at(x, y, z, sampler) == EndBiomeCategories.SMALL_ISLANDS) {
            cir.setReturnValue(this.islands);
        }
    }
}
