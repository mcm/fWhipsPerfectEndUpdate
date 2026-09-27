package io.mcmaster.someonesperfectend.fabric.client;

import io.mcmaster.someonesperfectend.client.SomeonesPerfectEndClient;
import net.fabricmc.api.ClientModInitializer;

public final class SomeonesPerfectEndFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SomeonesPerfectEndClient.init();
    }
}
