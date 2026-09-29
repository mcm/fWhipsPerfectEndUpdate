package io.mcmaster.someonesperfectend.fluid;

import dev.architectury.injectables.annotations.ExpectPlatform;

/**
 * Loader-specific Oasis Water code: the fluid instances (NeoForge needs a fluid type override) and the Oasis Water
 * Cauldron's registration with each loader's cauldron fluid API.
 */
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

    /**
     * Registers the Oasis Water Cauldron's interactions ({@link OasisWaterCauldronBlock#registerInteractions()}) at
     * setup, and the cauldron with the loader's fluid API (one bottle per level). Called during common init.
     */
    @ExpectPlatform
    public static void registerCauldron() {
        throw new AssertionError();
    }
}
