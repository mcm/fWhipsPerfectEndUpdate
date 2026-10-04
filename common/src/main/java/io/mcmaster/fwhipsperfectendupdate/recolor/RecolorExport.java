package io.mcmaster.fwhipsperfectendupdate.recolor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Dev-only entry point behind the {@code exportRecoloredTextures} Gradle task. Reads the recipes from the
 * mod's resources, recolors textures from a local vanilla client jar and writes the results (PNG plus any
 * {@code .png.mcmeta}) to an output folder laid out like a resource pack, as a starting point for artists.
 * Every recipe is exported, including those already replaced by a hand-drawn PNG.
 *
 * <p>Arguments: {@code <client jar> <output dir> <resources dir>...}
 */
public final class RecolorExport {
    private RecolorExport() {}

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            throw new IllegalArgumentException("Usage: RecolorExport <client jar> <output dir> <resources dir>...");
        }
        Path clientJar = Path.of(args[0]);
        Path output = Path.of(args[1]);
        List<RecolorRecipe> recipes = new ArrayList<>();
        for (int i = 2; i < args.length; i++) {
            Path recipeDir =
                    Path.of(args[i], "assets", "fwhipsperfectendupdate", RecolorPackResources.RECIPE_DIRECTORY);
            if (!Files.isDirectory(recipeDir)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(recipeDir)) {
                for (Path file : files.filter(f -> f.toString().endsWith(".json"))
                        .sorted()
                        .toList()) {
                    recipes.addAll(
                            RecolorRecipeParser.parse(file.toString(), Files.readString(file, StandardCharsets.UTF_8)));
                }
            }
        }

        RecolorGenerator.Result result;
        try (ZipFile jar = new ZipFile(clientJar.toFile())) {
            result = RecolorGenerator.generate(recipes, id -> readFromJar(jar, id), id -> false);
        }
        for (Map.Entry<RecolorGenerator.GeneratedFile, byte[]> file :
                result.files().entrySet()) {
            Path target = output.resolve("assets")
                    .resolve(file.getKey().namespace())
                    .resolve(file.getKey().path());
            Files.createDirectories(target.getParent());
            Files.write(target, file.getValue());
        }
        result.errors().forEach(System.err::println);
        System.out.println("Exported " + result.generated().size() + " recolored textures to " + output);
        if (!result.errors().isEmpty()) {
            throw new IllegalStateException(result.errors().size() + " recolor recipes failed");
        }
    }

    private static Optional<RecolorGenerator.SourceTexture> readFromJar(ZipFile jar, TextureId id) throws IOException {
        ZipEntry png = jar.getEntry(id.assetPath());
        if (png == null) {
            return Optional.empty();
        }
        ZipEntry mcmeta = jar.getEntry(id.assetPath() + ".mcmeta");
        return Optional.of(new RecolorGenerator.SourceTexture(
                read(jar, png), mcmeta == null ? Optional.empty() : Optional.of(read(jar, mcmeta))));
    }

    private static byte[] read(ZipFile jar, ZipEntry entry) throws IOException {
        try (InputStream in = jar.getInputStream(entry)) {
            return in.readAllBytes();
        }
    }
}
