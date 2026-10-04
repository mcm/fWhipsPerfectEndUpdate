package io.mcmaster.fwhipsperfectendupdate.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Enderblossom functional requirements that can be read straight from the resources (MOD-64). */
class EnderblossomRequirementsTest {
    private static final List<String> TYPES = List.of("open", "closed");

    /** FR-1: wall blockstates rotate one tilted model per facing; pots use an upright cross; both drop themselves. */
    @Test
    void wallAndPottedModels() throws IOException, URISyntaxException {
        Map<String, Integer> rotations = Map.of("north", 0, "east", 90, "south", 180, "west", 270);
        for (String type : TYPES) {
            String name = type + "_enderblossom";
            JsonObject variants = read("assets/fwhipsperfectendupdate/blockstates/" + name + ".json")
                    .getAsJsonObject("variants");
            assertEquals(4, variants.size(), name);
            rotations.forEach((facing, y) -> {
                JsonObject variant = variants.getAsJsonObject("facing=" + facing);
                assertEquals(
                        "fwhipsperfectendupdate:block/" + name,
                        variant.get("model").getAsString());
                assertEquals(y, variant.has("y") ? variant.get("y").getAsInt() : 0, name + " " + facing);
            });
            assertEquals(
                    "minecraft:block/flower_pot_cross",
                    model("potted_" + name).get("parent").getAsString());
            assertEquals(
                    "minecraft:item/generated",
                    read("assets/fwhipsperfectendupdate/models/item/" + name + ".json")
                            .get("parent")
                            .getAsString());
            assertTrue(lootItems("blocks/" + name).contains("fwhipsperfectendupdate:" + name), name + " drops itself");
            assertTrue(
                    lootItems("blocks/potted_" + name).contains("fwhipsperfectendupdate:" + name),
                    "potted " + name + " drops the blossom");
        }
        for (String template : List.of("template_wall_enderblossom", "template_wall_enderblossom_emissive")) {
            for (JsonElement element : model(template).getAsJsonArray("elements")) {
                JsonObject rotation = element.getAsJsonObject().getAsJsonObject("rotation");
                assertEquals("x", rotation.get("axis").getAsString(), template);
                assertEquals(-45.0F, rotation.get("angle").getAsFloat(), template);
            }
        }
    }

    /** FR-4 and FR-9: original textures with editable sources, the emissive layer only on the open model. */
    @Test
    void texturesAreOriginalAndOpenModelIsEmissive() throws IOException, URISyntaxException {
        Path repo = resources().getParent().getParent().getParent().getParent();
        for (String texture : List.of("closed_enderblossom", "open_enderblossom", "open_enderblossom_emissive")) {
            assertTrue(Files.isRegularFile(
                    resources().resolve("assets/fwhipsperfectendupdate/textures/block/" + texture + ".png")));
            assertTrue(Files.isRegularFile(repo.resolve("art/textures/block/" + texture + ".grid")), texture);
        }
        assertEquals(
                "fwhipsperfectendupdate:block/template_wall_enderblossom_emissive",
                model("open_enderblossom").get("parent").getAsString());
        assertEquals(
                "fwhipsperfectendupdate:block/open_enderblossom_emissive",
                model("open_enderblossom")
                        .getAsJsonObject("textures")
                        .get("emissive")
                        .getAsString());
        assertEquals(
                "fwhipsperfectendupdate:block/template_wall_enderblossom",
                model("closed_enderblossom").get("parent").getAsString());
        try (Stream<Path> files = Files.walk(resources())) {
            List<Path> shipped = files.filter(path -> {
                        String file = path.getFileName().toString();
                        return file.endsWith(".ogg") || file.endsWith(".wav");
                    })
                    .toList();
            assertEquals(List.of(), shipped, "no audio files are shipped");
        }
        assertFalse(Files.exists(resources().resolve("assets/minecraft/textures")), "no Mojang textures");
    }

    /** FR-6: both stew recipes give the one-tick displacement effect. */
    @Test
    void stewRecipesCarryDisplacement() throws IOException, URISyntaxException {
        for (String type : TYPES) {
            JsonObject recipe =
                    read("data/fwhipsperfectendupdate/recipe/suspicious_stew_from_" + type + "_enderblossom.json");
            JsonArray ingredients = recipe.getAsJsonArray("ingredients");
            assertTrue(ingredients.toString().contains("fwhipsperfectendupdate:" + type + "_enderblossom"));
            JsonObject result = recipe.getAsJsonObject("result");
            assertEquals("minecraft:suspicious_stew", result.get("id").getAsString());
            JsonObject effect = result.getAsJsonObject("components")
                    .getAsJsonArray("minecraft:suspicious_stew_effects")
                    .get(0)
                    .getAsJsonObject();
            assertEquals("fwhipsperfectendupdate:displacement", effect.get("id").getAsString());
        }
    }

    /** FR-7: both Astral tree features place 1–4 blossoms. */
    @Test
    void treesConfigureOneToFourBlossoms() throws IOException, URISyntaxException {
        for (String feature : List.of("astral_tree", "end_forest_astral_tree")) {
            JsonObject blossoms = read("data/fwhipsperfectendupdate/worldgen/configured_feature/" + feature + ".json")
                    .getAsJsonObject("config")
                    .getAsJsonObject("enderblossoms");
            assertEquals("minecraft:uniform", blossoms.get("type").getAsString(), feature);
            assertEquals(1, blossoms.get("min_inclusive").getAsInt(), feature);
            assertEquals(4, blossoms.get("max_inclusive").getAsInt(), feature);
        }
    }

    /** FR-8: a 25% closed blossom pool, kept off shears and Silk Touch. */
    @Test
    void floweringLeavesDropClosedBlossoms() throws IOException, URISyntaxException {
        JsonArray pools = read("data/fwhipsperfectendupdate/loot_table/blocks/flowering_astral_leaves.json")
                .getAsJsonArray("pools");
        JsonObject pool = pools.get(pools.size() - 1).getAsJsonObject();
        String json = pool.toString();
        assertTrue(json.contains("\"minecraft:inverted\""), json);
        assertTrue(json.contains("minecraft:shears"), json);
        assertTrue(json.contains("minecraft:silk_touch"), json);
        JsonObject entry = pool.getAsJsonArray("entries").get(0).getAsJsonObject();
        assertEquals(
                "fwhipsperfectendupdate:closed_enderblossom", entry.get("name").getAsString());
        String conditions = entry.getAsJsonArray("conditions").toString();
        assertTrue(conditions.contains("minecraft:survives_explosion"), conditions);
        assertTrue(conditions.contains("\"chance\":0.25"), conditions);
    }

    /** Non-goal: no flower tags (bees ignore them); the pots are flower pots like the mod's others. */
    @Test
    void tags() throws IOException, URISyntaxException {
        Path tags = resources().resolve("data/minecraft/tags");
        try (Stream<Path> files = Files.walk(tags)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                boolean flowerPots = file.endsWith(Path.of("block", "flower_pots.json"));
                String values = read(resources().relativize(file).toString()).toString();
                for (String type : TYPES) {
                    assertFalse(
                            values.contains("\"fwhipsperfectendupdate:" + type + "_enderblossom\""),
                            file + " lists the " + type + " blossom");
                    assertEquals(
                            flowerPots,
                            values.contains("\"fwhipsperfectendupdate:potted_" + type + "_enderblossom\""),
                            file + " and the potted " + type + " blossom");
                }
            }
        }
    }

    private static List<String> lootItems(String table) throws IOException, URISyntaxException {
        String json = read("data/fwhipsperfectendupdate/loot_table/" + table + ".json")
                .toString();
        return Stream.of("fwhipsperfectendupdate:open_enderblossom", "fwhipsperfectendupdate:closed_enderblossom")
                .filter(item -> json.contains("\"" + item + "\""))
                .toList();
    }

    private static JsonObject model(String name) throws IOException, URISyntaxException {
        return read("assets/fwhipsperfectendupdate/models/block/" + name + ".json");
    }

    private static JsonObject read(String path) throws IOException, URISyntaxException {
        return JsonParser.parseString(Files.readString(resources().resolve(path)))
                .getAsJsonObject();
    }

    /** The main resources root, which holds both {@code assets/} and {@code data/}. */
    private static Path resources() throws URISyntaxException {
        return Path.of(EnderblossomRequirementsTest.class
                        .getResource("/assets/fwhipsperfectendupdate/sounds.json")
                        .toURI())
                .getParent()
                .getParent()
                .getParent();
    }
}
