package io.mcmaster.someonesperfectend.gametest;

import io.mcmaster.someonesperfectend.SomeonesPerfectEnd;
import io.mcmaster.someonesperfectend.recolor.RecolorGenerator;
import io.mcmaster.someonesperfectend.recolor.RecolorPackResources;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.imageio.ImageIO;
import net.minecraft.Util;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraft.util.Unit;

/**
 * Drives the recolor pipeline through a real {@link ReloadableResourceManager} reload, the same path the
 * client takes, so the mixin and the generated pack are exercised on both loaders.
 */
public final class RecolorGameTests {
    private static final String NS = SomeonesPerfectEnd.MOD_ID;
    private static final String MCMETA = "{\"animation\":{\"frametime\":2}}";
    private static final int[] SOURCE_PIXELS = {0xFF333333, 0x80FFFFFF, 0x00123456, 0xFF000000};

    private RecolorGameTests() {}

    @GameTest
    public static void clientReloadServesRecoloredTextures(GameTestHelper helper) {
        PackResources vanilla = new InMemoryPackResources("test_vanilla")
                .put("minecraft", "textures/block/sps_test_source.png", png(SOURCE_PIXELS))
                .put("minecraft", "textures/block/sps_test_source.png.mcmeta", MCMETA);
        PackResources mod = new InMemoryPackResources("test_mod")
                .put(NS, "recolor/test.json", """
                        {
                          "palette": {"gradient": ["#000000", "#ff0000"], "normalize": false},
                          "recipes": [
                            {"source": "minecraft:block/sps_test_source", "target": "someonesperfectend:block/sps_generated"},
                            {"source": "minecraft:block/sps_test_source", "target": "someonesperfectend:block/sps_hand_drawn"},
                            {"source": "minecraft:block/sps_test_source", "target": "someonesperfectend:block/sps_user_pack"}
                          ]
                        }
                        """)
                .put(NS, "textures/block/sps_hand_drawn.png", png(new int[] {0xFF00FF00, 0, 0, 0}));
        PackResources userPack = new InMemoryPackResources("test_user_pack")
                .put(NS, "textures/block/sps_user_pack.png", png(new int[] {0xFF0000FF, 0, 0, 0}));

        try (ReloadableResourceManager manager = new ReloadableResourceManager(PackType.CLIENT_RESOURCES)) {
            manager.createReload(
                    Util.backgroundExecutor(),
                    Runnable::run,
                    CompletableFuture.completedFuture(Unit.INSTANCE),
                    List.of(vanilla, mod, userPack));

            List<PackResources> packs = manager.listPacks().toList();
            helper.assertTrue(
                    packs.get(0) instanceof RecolorPackResources,
                    "generated pack is not the lowest-priority pack: "
                            + packs.stream().map(PackResources::packId).toList());

            Resource generated = manager.getResource(id("textures/block/sps_generated.png"))
                    .orElseThrow(() -> new IllegalStateException("generated texture is missing"));
            helper.assertTrue(
                    generated.sourcePackId().equals(RecolorPackResources.PACK_ID),
                    "generated texture came from " + generated.sourcePackId());
            int[] expected = {0xFF330000, 0x80FF0000, 0x00123456, 0xFF000000};
            int[] actual = pixels(generated);
            helper.assertTrue(Arrays.equals(expected, actual), "recolored pixels were " + hex(actual));
            // atlases find block textures by listing the directory
            helper.assertTrue(
                    manager.listResources("textures/block", p -> true)
                            .containsKey(id("textures/block/sps_generated.png")),
                    "generated texture is not listed");

            Resource mcmeta = manager.getResource(id("textures/block/sps_generated.png.mcmeta"))
                    .orElseThrow(() -> new IllegalStateException("animation metadata is missing"));
            helper.assertTrue(MCMETA.equals(text(mcmeta)), "animation metadata changed: " + text(mcmeta));
            // the texture loader reads the animation through the PNG resource's attached metadata
            helper.assertTrue(
                    metadata(generated) != ResourceMetadata.EMPTY,
                    "generated texture has no attached .mcmeta metadata");

            assertServedBy(helper, manager, "textures/block/sps_hand_drawn.png", "test_mod", 0xFF00FF00);
            assertServedBy(helper, manager, "textures/block/sps_user_pack.png", "test_user_pack", 0xFF0000FF);
            RecolorPackResources pack = (RecolorPackResources) packs.get(0);
            helper.assertTrue(
                    pack.files().size() == 2, "hand-drawn targets must not be generated, pack holds " + pack.files());
        }
        helper.succeed();
    }

    private static void assertServedBy(
            GameTestHelper helper, ReloadableResourceManager manager, String path, String packId, int firstPixel) {
        Resource resource =
                manager.getResource(id(path)).orElseThrow(() -> new IllegalStateException(path + " is missing"));
        helper.assertTrue(
                resource.sourcePackId().equals(packId),
                path + " came from " + resource.sourcePackId() + " instead of " + packId);
        helper.assertTrue(pixels(resource)[0] == firstPixel, path + " is not the real PNG");
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(NS, path);
    }

    private static byte[] png(int[] argb) {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 2, 2, argb, 0, 2);
        try {
            return RecolorGenerator.encodePng(image);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static int[] pixels(Resource resource) {
        try (InputStream in = resource.open()) {
            BufferedImage image = ImageIO.read(in);
            return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static ResourceMetadata metadata(Resource resource) {
        try {
            return resource.metadata();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String text(Resource resource) {
        try (InputStream in = resource.open()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String hex(int[] argb) {
        return Arrays.stream(argb).mapToObj(Integer::toHexString).toList().toString();
    }
}
