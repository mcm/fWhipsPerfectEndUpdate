package io.mcmaster.someonesperfectend.fluid.neoforge;

import io.mcmaster.someonesperfectend.fluid.OasisWaterFluid;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidType;

/**
 * NeoForge keys most water behavior (swimming, drowning, fire, boats, farmland, lava interactions) on the
 * fluid type rather than {@code #minecraft:water}, so Oasis Water uses the vanilla water fluid type. Its
 * different look is applied by {@code LiquidBlockRendererMixin}.
 */
public final class OasisWaterPlatformImpl {
    private OasisWaterPlatformImpl() {
    }

    public static OasisWaterFluid.Source createSource() {
        return new OasisWaterFluid.Source() {
            @Override
            public FluidType getFluidType() {
                return NeoForgeMod.WATER_TYPE.value();
            }
        };
    }

    public static OasisWaterFluid.Flowing createFlowing() {
        return new OasisWaterFluid.Flowing() {
            @Override
            public FluidType getFluidType() {
                return NeoForgeMod.WATER_TYPE.value();
            }
        };
    }
}
