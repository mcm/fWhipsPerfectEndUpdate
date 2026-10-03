package io.mcmaster.someonesperfectend.fluid.fabric;

import io.mcmaster.someonesperfectend.fluid.OasisWaterFluid;

public final class OasisWaterPlatformImpl {
    private OasisWaterPlatformImpl() {}

    public static OasisWaterFluid.Source createSource() {
        return new OasisWaterFluid.Source();
    }

    public static OasisWaterFluid.Flowing createFlowing() {
        return new OasisWaterFluid.Flowing();
    }
}
