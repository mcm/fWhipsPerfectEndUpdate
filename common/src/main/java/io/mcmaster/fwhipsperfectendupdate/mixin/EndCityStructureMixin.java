package io.mcmaster.fwhipsperfectendupdate.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import io.mcmaster.fwhipsperfectendupdate.worldgen.EndCities;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.structures.EndCityStructure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Starts End Cities where vanilla terrain would, at the elevation band's height (US-018, MOD-47). */
@Mixin(EndCityStructure.class)
public abstract class EndCityStructureMixin extends Structure {
    protected EndCityStructureMixin(StructureSettings settings) {
        super(settings);
    }

    @ModifyConstant(method = "findGenerationPoint", constant = @Constant(intValue = EndCities.VANILLA_MIN_Y))
    private int fwhipsperfectendupdate$bandMinimumY(
            int vanillaMinY, Structure.GenerationContext context, @Local Rotation rotation) {
        // The start column: vanilla's getLowestYIn5by5BoxOffset7Blocks returns its Y at chunk block (7, 7).
        return EndCities.minimumY(
                context.randomState(),
                vanillaMinY,
                context.chunkPos().getBlockX(7),
                context.chunkPos().getBlockZ(7),
                () -> getLowestYIn5by5BoxOffset7Blocks(context, rotation).getY());
    }
}
