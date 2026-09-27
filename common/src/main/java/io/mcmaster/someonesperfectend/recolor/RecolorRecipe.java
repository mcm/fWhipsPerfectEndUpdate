package io.mcmaster.someonesperfectend.recolor;

/** "Recolor texture {@code source} with {@code palette} and serve it as {@code target}". */
public record RecolorRecipe(TextureId source, TextureId target, Palette palette) {
}
