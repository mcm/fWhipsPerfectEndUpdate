package io.mcmaster.someonesperfectend.mixin;

import io.mcmaster.someonesperfectend.worldgen.EndCities;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.structures.EndCityStructure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Moves the End City minimum start height with the elevation band of the start column (US-018). */
@Mixin(EndCityStructure.class)
public abstract class EndCityStructureMixin {
    @ModifyConstant(method = "findGenerationPoint", constant = @Constant(intValue = EndCities.VANILLA_MIN_Y))
    private int someonesperfectend$bandMinimumY(int vanillaMinY, Structure.GenerationContext context) {
        // The start column: vanilla's getLowestYIn5by5BoxOffset7Blocks returns its Y at chunk block (7, 7).
        return EndCities.minimumY(
                context.randomState(),
                vanillaMinY,
                context.chunkPos().getBlockX(7),
                context.chunkPos().getBlockZ(7));
    }
}
