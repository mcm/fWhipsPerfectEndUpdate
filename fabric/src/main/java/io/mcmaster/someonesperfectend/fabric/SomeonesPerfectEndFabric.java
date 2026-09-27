package io.mcmaster.someonesperfectend.fabric;

import io.mcmaster.someonesperfectend.SomeonesPerfectEnd;
import net.fabricmc.api.ModInitializer;

public final class SomeonesPerfectEndFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SomeonesPerfectEnd.init();
    }
}
