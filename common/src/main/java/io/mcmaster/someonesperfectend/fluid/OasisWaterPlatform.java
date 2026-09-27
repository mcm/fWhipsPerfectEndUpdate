package io.mcmaster.someonesperfectend.fluid;

import dev.architectury.injectables.annotations.ExpectPlatform;

/** Loader-specific Oasis Water fluid instances (NeoForge needs a fluid type override). */
public final class OasisWaterPlatform {
    private OasisWaterPlatform() {
    }

    @ExpectPlatform
    public static OasisWaterFluid.Source createSource() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static OasisWaterFluid.Flowing createFlowing() {
        throw new AssertionError();
    }
}
