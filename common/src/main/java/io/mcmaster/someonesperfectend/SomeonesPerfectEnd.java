package io.mcmaster.someonesperfectend;

import io.mcmaster.someonesperfectend.registry.ModRegistries;
import net.minecraft.resources.ResourceLocation;

public final class SomeonesPerfectEnd {
    public static final String MOD_ID = "someonesperfectend";
    public static final String MOD_NAME = "Someone's Perfect End Update";

    private SomeonesPerfectEnd() {
    }

    /** Common init; called from each loader's entrypoint during mod construction. */
    public static void init() {
        ModRegistries.register();
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
