package io.mcmaster.someonesperfectend.recolor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Functional requirements of the Astral and Voidvine visual revamp that can be read straight from the resources. */
class VisualRevampRequirementsTest {
    private static final int[] ASTRAL_LOG_GRADIENT = {0x1F1A45, 0x4A3F8F, 0x8A7FD0};

    /** FR-2 and FR-3: chorus-based log sides and wood, bamboo log ends, stripped variants untouched. */
    @Test
    void astralLogTexturesComeFromChorus() throws IOException, URISyntaxException {
        Map<String, RecolorRecipe> recipes = new HashMap<>();
        Path recipeFile = resources().resolve("assets/someonesperfectend/recolor/astral_wood.json");
        RecolorRecipeParser.parse("astral_wood.json", Files.readString(recipeFile))
                .forEach(r -> recipes.put(r.target().path(), r));

        assertSource(recipes, "block/astral_log", "block/chorus_plant");
        assertSource(recipes, "block/flowering_astral_log", "block/chorus_flower");
        assertSource(recipes, "block/astral_log_top", "block/bamboo_block_top");
        assertSource(recipes, "block/stripped_astral_log", "block/stripped_bamboo_block");
        assertSource(recipes, "block/stripped_astral_log_top", "block/stripped_bamboo_block_top");
        for (String target : List.of("block/astral_log", "block/flowering_astral_log")) {
            int[] rgb = recipes.get(target).palette().gradient().orElseThrow().rgb();
            assertArrayEquals(ASTRAL_LOG_GRADIENT, rgb, target + " keeps the Astral log palette");
        }

        assertModel("astral_log", "block/astral_log_top", "block/astral_log");
        assertModel("astral_wood", "block/astral_log", "block/astral_log");
        assertModel("flowering_astral_log", "block/astral_log_top", "block/flowering_astral_log");
        assertModel("stripped_astral_log", "block/stripped_astral_log_top", "block/stripped_astral_log");
        assertModel("stripped_astral_wood", "block/stripped_astral_log", "block/stripped_astral_log");
    }

    /** FR-8: nothing crafts the Voidvine Block; Endlings barter it instead of the plant. */
    @Test
    void voidvineBlockOnlyComesFromBartering() throws IOException, URISyntaxException {
        Path data = resources().resolve("data/someonesperfectend");
        try (Stream<Path> files = Files.walk(data.resolve("recipe"))) {
            for (Path file : files.filter(f -> f.toString().endsWith(".json")).toList()) {
                JsonObject recipe =
                        JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                if (recipe.has("result")) {
                    JsonElement result = recipe.get("result");
                    String id = result.isJsonObject()
                            ? result.getAsJsonObject().get("id").getAsString()
                            : result.getAsString();
                    assertFalse(id.equals("someonesperfectend:voidvine_block"), file + " crafts the Voidvine Block");
                }
            }
        }
        assertFalse(Files.exists(data.resolve("advancement/recipes/building_blocks/voidvine_block.json")));

        JsonObject table = JsonParser.parseString(
                        Files.readString(data.resolve("loot_table/gameplay/endling_bartering.json")))
                .getAsJsonObject();
        JsonObject block = null;
        for (JsonElement element :
                table.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries")) {
            JsonObject entry = element.getAsJsonObject();
            String name = entry.get("name").getAsString();
            assertFalse(name.equals("someonesperfectend:voidvine"), "the Voidvine plant is still bartered");
            if (name.equals("someonesperfectend:voidvine_block")) {
                block = entry;
            }
        }
        assertTrue(block != null, "Endlings don't barter the Voidvine Block");
        assertEquals(12, block.get("weight").getAsInt());
        JsonObject count =
                block.getAsJsonArray("functions").get(0).getAsJsonObject().getAsJsonObject("count");
        assertEquals("minecraft:uniform", count.get("type").getAsString());
        assertEquals(1, count.get("min").getAsInt());
        assertEquals(2, count.get("max").getAsInt());
    }

    private static void assertSource(Map<String, RecolorRecipe> recipes, String target, String source) {
        assertEquals(TextureId.parse("minecraft:" + source), recipes.get(target).source(), target);
    }

    private static void assertModel(String model, String end, String side) throws IOException, URISyntaxException {
        JsonObject textures = JsonParser.parseString(Files.readString(
                        resources().resolve("assets/someonesperfectend/models/block/" + model + ".json")))
                .getAsJsonObject()
                .getAsJsonObject("textures");
        assertEquals("someonesperfectend:" + end, textures.get("end").getAsString(), model + " end");
        assertEquals("someonesperfectend:" + side, textures.get("side").getAsString(), model + " side");
    }

    /** The main resources root, holding both {@code assets/} and {@code data/}. */
    private static Path resources() throws URISyntaxException {
        return Path.of(VisualRevampRequirementsTest.class
                        .getResource("/assets/someonesperfectend/recolor")
                        .toURI())
                .getParent()
                .getParent()
                .getParent();
    }
}
