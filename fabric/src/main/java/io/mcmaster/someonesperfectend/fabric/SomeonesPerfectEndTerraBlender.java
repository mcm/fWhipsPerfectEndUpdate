package io.mcmaster.someonesperfectend.fabric;

import io.mcmaster.someonesperfectend.worldgen.ModBiomes;
import terrablender.api.TerraBlenderApi;

/** TerraBlender's {@code terrablender} entrypoint: runs once TerraBlender's config is loaded. */
public final class SomeonesPerfectEndTerraBlender implements TerraBlenderApi {
    @Override
    public void onTerraBlenderInitialized() {
        ModBiomes.registerPlacement();
    }
}
