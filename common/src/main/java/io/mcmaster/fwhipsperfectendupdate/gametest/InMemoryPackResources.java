package io.mcmaster.fwhipsperfectendupdate.gametest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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

/** A client resource pack held in memory, for tests that feed packs to a resource manager. */
final class InMemoryPackResources implements PackResources {
    private final PackLocationInfo location;
    private final Map<ResourceLocation, byte[]> files = new TreeMap<>();

    InMemoryPackResources(String id) {
        location = new PackLocationInfo(id, Component.literal(id), PackSource.DEFAULT, Optional.empty());
    }

    InMemoryPackResources put(String namespace, String path, byte[] bytes) {
        files.put(ResourceLocation.fromNamespaceAndPath(namespace, path), bytes);
        return this;
    }

    InMemoryPackResources put(String namespace, String path, String text) {
        return put(namespace, path, text.getBytes(StandardCharsets.UTF_8));
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getRootResource(String... elements) {
        return null;
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation id) {
        byte[] bytes = type == PackType.CLIENT_RESOURCES ? files.get(id) : null;
        return bytes == null ? null : () -> new ByteArrayInputStream(bytes);
    }

    @Override
    public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
        if (type != PackType.CLIENT_RESOURCES) {
            return;
        }
        files.forEach((id, bytes) -> {
            if (id.getNamespace().equals(namespace) && id.getPath().startsWith(path + "/")) {
                output.accept(id, () -> new ByteArrayInputStream(bytes));
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
    public void close() {}
}
