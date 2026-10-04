package io.mcmaster.someonesperfectend.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/**
 * The tilted wall model meets the Astral Log (MOD-73). Both planes lean 45 degrees out from the stem base, about the
 * x axis. The face-parallel plane's bottom edge lies along the log face, but the perpendicular plane is tilted within
 * itself, so there the log face is the texture diagonal through the bottom center: a pixel's center is
 * {@code (u + v - 24) / sqrt(2)} pixels into the log (u, v from the texture's top left, pixel centers at +0.5).
 */
class EnderblossomWallModelTest {
    private static final List<String> TYPES = List.of("open", "closed");
    private static final List<String> TEMPLATES =
            List.of("template_wall_enderblossom", "template_wall_enderblossom_emissive");

    /** Where an Astral Log's 8-pixel (chorus plant) core face is, in model z for facing north: 4 pixels in. */
    private static final float CORE_FACE_Z = 20.0F;

    @Test
    void stemBaseSitsOnTheLogCore() throws IOException, URISyntaxException {
        for (String template : TEMPLATES) {
            for (JsonElement element : model(template).getAsJsonArray("elements")) {
                JsonArray origin =
                        element.getAsJsonObject().getAsJsonObject("rotation").getAsJsonArray("origin");
                assertEquals(CORE_FACE_Z, origin.get(2).getAsFloat(), template);
                assertEquals(2.0F, origin.get(1).getAsFloat(), template);
            }
        }
    }

    @Test
    void perpendicularPlaneUsesTheSideTexture() throws IOException, URISyntaxException {
        for (String template : TEMPLATES) {
            for (JsonElement element : model(template).getAsJsonArray("elements")) {
                JsonObject object = element.getAsJsonObject();
                JsonArray from = object.getAsJsonArray("from");
                JsonArray to = object.getAsJsonArray("to");
                boolean perpendicular = from.get(0).getAsFloat() == to.get(0).getAsFloat();
                for (var face : object.getAsJsonObject("faces").entrySet()) {
                    String texture =
                            face.getValue().getAsJsonObject().get("texture").getAsString();
                    if (texture.equals("#emissive")) {
                        continue;
                    }
                    assertEquals(perpendicular ? "#side" : "#cross", texture, template + " " + face.getKey());
                }
            }
        }
        Path repo = resources().getParent().getParent().getParent().getParent();
        for (String type : TYPES) {
            String name = type + "_enderblossom";
            assertEquals(
                    "someonesperfectend:block/" + name + "_side",
                    model(name).getAsJsonObject("textures").get("side").getAsString());
            assertTrue(Files.isRegularFile(repo.resolve("art/textures/block/" + name + "_side.grid")), name);
        }
    }

    /**
     * The side texture keeps the front's head, and its stem runs into the log instead of ending in a flat cut: nothing
     * on the plane's bottom edge hangs in the air, and the stem carries on at least a pixel past the core face.
     */
    @Test
    void sideTextureStemRunsIntoTheLog() throws IOException, URISyntaxException {
        for (String type : TYPES) {
            String name = type + "_enderblossom";
            BufferedImage front = texture(name);
            BufferedImage side = texture(name + "_side");
            for (int v = 0; v < 11; v++) {
                for (int u = 0; u < 16; u++) {
                    assertEquals(front.getRGB(u, v), side.getRGB(u, v), name + " head pixel " + u + "," + v);
                }
            }
            for (int u = 0; u < 16; u++) {
                if (opaque(side, u, 15)) {
                    // The pixel's bottom-right corner (u + 1, 16) must reach the log face.
                    assertTrue(u + 1 + 16 - 24 >= 0, name + "_side bottom pixel " + u + " ends in the air");
                }
            }
            double deepest = Double.NEGATIVE_INFINITY;
            for (int v = 11; v < 16; v++) {
                for (int u = 0; u < 16; u++) {
                    if (opaque(side, u, v)) {
                        deepest = Math.max(deepest, (u + v + 1 - 24) / Math.sqrt(2.0));
                    }
                }
            }
            assertTrue(deepest >= 1.0, name + "_side stem reaches only " + deepest + " pixels into the log");
        }
    }

    private static boolean opaque(BufferedImage image, int u, int v) {
        return (image.getRGB(u, v) >>> 24) != 0;
    }

    private static BufferedImage texture(String name) throws IOException, URISyntaxException {
        BufferedImage image = ImageIO.read(resources()
                .resolve("assets/someonesperfectend/textures/block/" + name + ".png")
                .toFile());
        assertEquals(16, image.getWidth(), name);
        assertEquals(16, image.getHeight(), name);
        return image;
    }

    private static JsonObject model(String name) throws IOException, URISyntaxException {
        return JsonParser.parseString(Files.readString(
                        resources().resolve("assets/someonesperfectend/models/block/" + name + ".json")))
                .getAsJsonObject();
    }

    /** The main resources root, which holds both {@code assets/} and {@code data/}. */
    private static Path resources() throws URISyntaxException {
        return Path.of(EnderblossomWallModelTest.class
                        .getResource("/assets/someonesperfectend/sounds.json")
                        .toURI())
                .getParent()
                .getParent()
                .getParent();
    }
}
