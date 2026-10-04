package io.mcmaster.fwhipsperfectendupdate.fabric;

import io.mcmaster.fwhipsperfectendupdate.FwhipsPerfectEndUpdate;
import net.fabricmc.api.ModInitializer;

public final class FwhipsPerfectEndUpdateFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        FwhipsPerfectEndUpdate.init();
    }
}
