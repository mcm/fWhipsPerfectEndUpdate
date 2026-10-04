package io.mcmaster.fwhipsperfectendupdate.fabric;

import io.mcmaster.fwhipsperfectendupdate.worldgen.ModBiomes;
import terrablender.api.TerraBlenderApi;

/** TerraBlender's {@code terrablender} entrypoint: runs once TerraBlender's config is loaded. */
public final class FwhipsPerfectEndUpdateTerraBlender implements TerraBlenderApi {
    @Override
    public void onTerraBlenderInitialized() {
        ModBiomes.registerPlacement();
    }
}
