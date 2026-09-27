package io.mcmaster.someonesperfectend.recolor;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import javax.imageio.ImageIO;

/**
 * Turns recolor recipes into generated PNG (and {@code .png.mcmeta}) files. Shared by the client resource
 * pack and the {@code exportRecoloredTextures} Gradle task, so it only uses plain Java.
 */
public final class RecolorGenerator {
    private RecolorGenerator() {
    }

    /** Where source textures are read from. */
    @FunctionalInterface
    public interface TextureSource {
        /**
         * Returns the PNG for the texture from the highest-priority place that has one, together with the
         * {@code .png.mcmeta} from that same place (if any).
         */
        Optional<SourceTexture> find(TextureId id) throws IOException;
    }

    public record SourceTexture(byte[] png, Optional<byte[]> mcmeta) {
    }

    /** A generated file, e.g. namespace {@code someonesperfectend}, path {@code textures/block/x.png}. */
    public record GeneratedFile(String namespace, String path) {
    }

    public record Result(Map<GeneratedFile, byte[]> files, List<TextureId> generated, List<TextureId> overridden,
            List<String> errors) {
    }

    public static Result generate(List<RecolorRecipe> recipes, TextureSource sources,
            Predicate<TextureId> realTextureExists) {
        RecolorPrecedence.Plan plan = RecolorPrecedence.plan(recipes, realTextureExists);
        Map<GeneratedFile, byte[]> files = new LinkedHashMap<>();
        List<TextureId> generated = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (RecolorRecipe recipe : plan.generate()) {
            try {
                Optional<SourceTexture> source = sources.find(recipe.source());
                if (source.isEmpty()) {
                    errors.add("Source texture " + recipe.source() + " for " + recipe.target() + " not found");
                    continue;
                }
                TextureId target = recipe.target();
                files.put(new GeneratedFile(target.namespace(), target.pngPath()),
                        recolorPng(source.get().png(), recipe.palette()));
                source.get().mcmeta().ifPresent(mcmeta ->
                        files.put(new GeneratedFile(target.namespace(), target.mcmetaPath()), mcmeta));
                generated.add(target);
            } catch (IOException | RuntimeException e) {
                errors.add("Failed to recolor " + recipe.source() + " into " + recipe.target() + ": " + e);
            }
        }
        List<TextureId> overridden = plan.overridden().stream().map(RecolorRecipe::target).toList();
        return new Result(files, List.copyOf(generated), overridden, List.copyOf(errors));
    }

    /** Decodes a PNG, applies the palette to every frame (the whole image) and encodes it as ARGB PNG. */
    public static byte[] recolorPng(byte[] png, Palette palette) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        if (image == null) {
            throw new IOException("Not a readable PNG");
        }
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        out.setRGB(0, 0, width, height, palette.apply(pixels), 0, width);
        return encodePng(out);
    }

    public static byte[] encodePng(BufferedImage image) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "png", bytes)) {
            throw new IOException("No PNG writer available");
        }
        return bytes.toByteArray();
    }
}
