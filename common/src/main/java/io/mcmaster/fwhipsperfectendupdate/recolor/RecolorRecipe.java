package io.mcmaster.fwhipsperfectendupdate.recolor;

import java.util.Optional;

/**
 * "Recolor texture {@code source} with {@code palette} and serve it as {@code target}". With
 * {@code keepOnlyMapped}, source pixels whose RGB is not a key of the palette's {@link Palette#colors()} become
 * transparent first; {@code flip} mirrors the image after recoloring.
 */
public record RecolorRecipe(
        TextureId source, TextureId target, Palette palette, Optional<Flip> flip, boolean keepOnlyMapped) {
    public RecolorRecipe(TextureId source, TextureId target, Palette palette) {
        this(source, target, palette, Optional.empty(), false);
    }

    public enum Flip {
        /** Upside down (top row becomes the bottom row). Animated textures are flipped frame by frame. */
        VERTICAL,
        /** Mirrored left to right. */
        HORIZONTAL
    }
}
