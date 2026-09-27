package io.mcmaster.someonesperfectend.recolor;

import com.mojang.logging.LogUtils;
import io.mcmaster.someonesperfectend.SomeonesPerfectEnd;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * The dynamically generated, in-memory resource pack holding recolored textures.
 *
 * <p>On every client resource reload ({@code ReloadableResourceManagerMixin}) this pack is rebuilt from the
 * packs being loaded: recipes come from {@code assets/someonesperfectend/recolor/} in any pack, source
 * textures from the highest-priority pack that has them (so a resource pack that retextures bamboo also
 * changes the Astral placeholder), and the pack is inserted at the lowest priority. Targets that already have
 * a real PNG in any pack are not generated at all.
 */
public final class RecolorPackResources implements PackResources {
    public static final String PACK_ID = SomeonesPerfectEnd.MOD_ID + "_recolored";
    public static final String RECIPE_DIRECTORY = "recolor";

    private static final Logger LOGGER = LogUtils.getLogger();

    private final PackLocationInfo location = new PackLocationInfo(PACK_ID,
            Component.literal("Someone's Perfect End Update: generated textures"), PackSource.BUILT_IN,
            Optional.empty());
    private final Map<ResourceLocation, byte[]> files;

    private RecolorPackResources(Map<ResourceLocation, byte[]> files) {
        this.files = files;
    }

    /**
     * Returns {@code packs} (lowest priority first) with a freshly generated recolor pack at the bottom, or
     * {@code packs} unchanged when there is nothing to generate.
     */
    public static List<PackResources> prependTo(List<PackResources> packs) {
        List<PackResources> others = packs.stream().filter(p -> !(p instanceof RecolorPackResources)).toList();
        RecolorPackResources generated;
        try {
            generated = generate(others);
        } catch (RuntimeException e) {
            LOGGER.error("Recolor pipeline failed; continuing without generated textures", e);
            return others;
        }
        if (generated.files.isEmpty()) {
            return others;
        }
        List<PackResources> result = new ArrayList<>(others.size() + 1);
        result.add(generated);
        result.addAll(others);
        return result;
    }

    /** Builds the pack from {@code packs} (lowest priority first). */
    public static RecolorPackResources generate(List<PackResources> packs) {
        long start = System.nanoTime();
        List<RecolorRecipe> recipes = loadRecipes(packs);
        RecolorGenerator.Result result = RecolorGenerator.generate(recipes,
                id -> findSource(packs, id),
                id -> packs.stream().anyMatch(p -> hasFile(p, id.namespace(), id.pngPath())));
        result.errors().forEach(LOGGER::warn);
        Map<ResourceLocation, byte[]> files = new TreeMap<>();
        result.files().forEach((file, bytes) ->
                files.put(ResourceLocation.fromNamespaceAndPath(file.namespace(), file.path()), bytes));
        LOGGER.info("Recolor: generated {} textures ({} replaced by real PNGs, {} errors) in {} ms",
                result.generated().size(), result.overridden().size(), result.errors().size(),
                (System.nanoTime() - start) / 1_000_000);
        return new RecolorPackResources(files);
    }

    /** Recipe files from every pack; a file in a higher pack replaces the same file in a lower one. */
    private static List<RecolorRecipe> loadRecipes(List<PackResources> packs) {
        Map<ResourceLocation, IoSupplier<InputStream>> recipeFiles = new TreeMap<>();
        for (PackResources pack : packs) {
            pack.listResources(PackType.CLIENT_RESOURCES, SomeonesPerfectEnd.MOD_ID, RECIPE_DIRECTORY,
                    (id, supplier) -> {
                        if (id.getPath().endsWith(".json")) {
                            recipeFiles.put(id, supplier);
                        }
                    });
        }
        List<RecolorRecipe> recipes = new ArrayList<>();
        recipeFiles.forEach((id, supplier) -> {
            try (InputStream in = supplier.get()) {
                recipes.addAll(RecolorRecipeParser.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8)));
            } catch (IOException | RuntimeException e) {
                LOGGER.warn("Skipping invalid recolor recipe file {}: {}", id, e.toString());
            }
        });
        return recipes;
    }

    private static Optional<RecolorGenerator.SourceTexture> findSource(List<PackResources> packs, TextureId id)
            throws IOException {
        ResourceLocation png = ResourceLocation.fromNamespaceAndPath(id.namespace(), id.pngPath());
        ResourceLocation mcmeta = ResourceLocation.fromNamespaceAndPath(id.namespace(), id.mcmetaPath());
        for (int i = packs.size() - 1; i >= 0; i--) {
            PackResources pack = packs.get(i);
            IoSupplier<InputStream> supplier = pack.getResource(PackType.CLIENT_RESOURCES, png);
            if (supplier != null) {
                IoSupplier<InputStream> meta = pack.getResource(PackType.CLIENT_RESOURCES, mcmeta);
                return Optional.of(new RecolorGenerator.SourceTexture(readAll(supplier),
                        meta == null ? Optional.empty() : Optional.of(readAll(meta))));
            }
        }
        return Optional.empty();
    }

    private static boolean hasFile(PackResources pack, String namespace, String path) {
        return pack.getResource(PackType.CLIENT_RESOURCES, ResourceLocation.fromNamespaceAndPath(namespace, path))
                != null;
    }

    private static byte[] readAll(IoSupplier<InputStream> supplier) throws IOException {
        try (InputStream in = supplier.get()) {
            return in.readAllBytes();
        }
    }

    /** The generated files, keyed by namespace and path (e.g. {@code ns:textures/block/x.png}). */
    public Set<ResourceLocation> files() {
        return files.keySet();
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getRootResource(String... elements) {
        return null;
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
        byte[] bytes = type == PackType.CLIENT_RESOURCES ? files.get(location) : null;
        return bytes == null ? null : () -> new ByteArrayInputStream(bytes);
    }

    @Override
    public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
        if (type != PackType.CLIENT_RESOURCES) {
            return;
        }
        String prefix = path.endsWith("/") ? path : path + "/";
        files.forEach((location, bytes) -> {
            if (location.getNamespace().equals(namespace) && location.getPath().startsWith(prefix)) {
                output.accept(location, () -> new ByteArrayInputStream(bytes));
            }
        });
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return type == PackType.CLIENT_RESOURCES
                ? files.keySet().stream().map(ResourceLocation::getNamespace).collect(Collectors.toSet())
                : Set.of();
    }

    @Nullable
    @Override
    public <T> T getMetadataSection(MetadataSectionSerializer<T> serializer) {
        return null;
    }

    @Override
    public PackLocationInfo location() {
        return location;
    }

    @Override
    public void close() {
    }
}
