package io.mcmaster.someonesperfectend.recolor;

import java.util.regex.Pattern;

/**
 * A texture id such as {@code minecraft:block/bamboo_planks}, which lives at
 * {@code assets/minecraft/textures/block/bamboo_planks.png}.
 *
 * <p>Plain Java (no Minecraft classes) so the recolor core also runs in the {@code exportRecoloredTextures}
 * Gradle task.
 */
public record TextureId(String namespace, String path) {
    private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern PATH = Pattern.compile("[a-z0-9_./-]+");

    public TextureId {
        if (!NAMESPACE.matcher(namespace).matches()) {
            throw new IllegalArgumentException("Invalid texture namespace: " + namespace);
        }
        if (!PATH.matcher(path).matches() || path.endsWith(".png")) {
            throw new IllegalArgumentException("Invalid texture path: " + path);
        }
    }

    /** Parses {@code namespace:path}; the namespace defaults to {@code minecraft}. */
    public static TextureId parse(String id) {
        int colon = id.indexOf(':');
        return colon < 0 ? new TextureId("minecraft", id)
                : new TextureId(id.substring(0, colon), id.substring(colon + 1));
    }

    /** Path of the PNG inside the namespace, e.g. {@code textures/block/bamboo_planks.png}. */
    public String pngPath() {
        return "textures/" + path + ".png";
    }

    /** Path of the PNG's metadata (animation) file inside the namespace. */
    public String mcmetaPath() {
        return pngPath() + ".mcmeta";
    }

    /** Path of the PNG from the root of a resource pack or jar. */
    public String assetPath() {
        return "assets/" + namespace + "/" + pngPath();
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }
}
