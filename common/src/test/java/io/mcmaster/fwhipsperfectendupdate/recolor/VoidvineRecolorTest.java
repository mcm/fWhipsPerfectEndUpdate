package io.mcmaster.fwhipsperfectendupdate.recolor;

import static io.mcmaster.fwhipsperfectendupdate.recolor.RecolorGeneratorTest.pixels;
import static io.mcmaster.fwhipsperfectendupdate.recolor.RecolorGeneratorTest.png;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** The Voidvine plant and item are flipped Twisting Vines with a cyan berry overlay cut from Cave Vines. */
class VoidvineRecolorTest {
    private static final String ASSETS = "/assets/fwhipsperfectendupdate/";
    private static final List<String> MODELS = List.of(
            "block/voidvine",
            "block/voidvine_plant",
            "block/voidvine_lit",
            "block/voidvine_plant_lit",
            "item/voidvine");

    @Test
    void voidvineRecipesFlipTwistingVinesAndMaskBerries() throws IOException {
        Map<String, RecolorRecipe> recipes =
                byTarget(RecolorRecipeParser.parse("voidvine_plant.json", read("recolor/voidvine_plant.json")));
        assertEquals(
                Set.of(
                        "block/voidvine",
                        "block/voidvine_plant",
                        "block/voidvine_berries",
                        "block/voidvine_plant_berries",
                        "item/voidvine",
                        "item/voidfruit"),
                recipes.keySet());

        assertRecipe(recipes.get("block/voidvine"), "block/twisting_vines", false);
        assertRecipe(recipes.get("block/voidvine_plant"), "block/twisting_vines_plant", false);
        assertRecipe(recipes.get("item/voidvine"), "block/twisting_vines_plant", false);
        assertRecipe(recipes.get("block/voidvine_berries"), "block/cave_vines_lit", true);
        assertRecipe(recipes.get("block/voidvine_plant_berries"), "block/cave_vines_plant_lit", true);

        RecolorRecipe voidfruit = recipes.get("item/voidfruit");
        assertEquals(TextureId.parse("minecraft:item/glow_berries"), voidfruit.source());
        assertTrue(voidfruit.flip().isEmpty());
        assertFalse(voidfruit.keepOnlyMapped());
    }

    @Test
    void vineIsFlippedWithTheVoidvinePurples() throws IOException {
        RecolorRecipe vine = byTarget(
                        RecolorRecipeParser.parse("voidvine_plant.json", read("recolor/voidvine_plant.json")))
                .get("block/voidvine");
        // 1x2: a light tip on top and a dark base below must come out base-up, tip-down
        int[] out = recolor(vine, png(1, 2, new int[] {0xFFFFFFFF, 0xFF000000}));
        assertArrayEquals(new int[] {0xFF3B3272, 0xFFE4E0FF}, out);
    }

    @Test
    void berryOverlayKeepsOnlyCyanBerries() throws IOException {
        Map<String, RecolorRecipe> recipes =
                byTarget(RecolorRecipeParser.parse("voidvine_plant.json", read("recolor/voidvine_plant.json")));
        for (String target : List.of("block/voidvine_berries", "block/voidvine_plant_berries")) {
            int[] out = recolor(recipes.get(target), png(2, 2, new int[] {
                0xFFA46422, 0xFFEB8931,
                0xFFF7E26B, 0xFF4A7A2C
            }));
            // flipped vertically: the bottom row (berry + leaf) comes first, the leaf is cut away
            assertArrayEquals(new int[] {0xFFC6FBFF, 0x00000000, 0xFF157A86, 0xFF3FD3DD}, out, target);
        }
    }

    @Test
    void bambooItemRecipeIsGone() throws IOException {
        Map<String, RecolorRecipe> wood =
                byTarget(RecolorRecipeParser.parse("voidvine_wood.json", read("recolor/voidvine_wood.json")));
        assertFalse(wood.containsKey("item/voidvine"));
        assertTrue(wood.values().stream().noneMatch(r -> r.source().path().equals("item/bamboo")));
    }

    @Test
    void planksStairsAndSlabsUseBambooTileArt() throws IOException {
        Map<String, RecolorRecipe> wood =
                byTarget(RecolorRecipeParser.parse("voidvine_wood.json", read("recolor/voidvine_wood.json")));
        TextureId tileArt = TextureId.parse("minecraft:block/bamboo_mosaic");
        assertEquals(tileArt, wood.get("block/voidvine_planks").source());
        assertEquals(
                1,
                wood.values().stream().filter(r -> r.source().equals(tileArt)).count(),
                "only the planks recolor the bamboo tile art");
        assertTrue(
                wood.values().stream().noneMatch(r -> r.source().path().equals("block/bamboo_planks")),
                "bamboo planks are still recolored");
        for (String model : List.of(
                "block/voidvine_planks",
                "block/voidvine_stairs",
                "block/voidvine_stairs_inner",
                "block/voidvine_stairs_outer",
                "block/voidvine_slab",
                "block/voidvine_slab_top")) {
            for (String texture : textures(model).values()) {
                assertEquals("fwhipsperfectendupdate:block/voidvine_planks", texture, model);
            }
        }
    }

    @Test
    void everyVoidvineModelTextureResolvesToARecipe() throws IOException, URISyntaxException {
        Set<String> targets = new TreeSet<>();
        Map<String, Integer> targetCounts = new HashMap<>();
        Path dir = Path.of(
                VoidvineRecolorTest.class.getResource(ASSETS + "recolor").toURI());
        try (Stream<Path> files = Files.list(dir)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".json")).toList()) {
                for (RecolorRecipe recipe :
                        RecolorRecipeParser.parse(file.getFileName().toString(), Files.readString(file))) {
                    if (recipe.target().namespace().equals("fwhipsperfectendupdate")) {
                        targets.add(recipe.target().path());
                        targetCounts.merge(recipe.target().path(), 1, Integer::sum);
                    }
                }
            }
        }
        targetCounts.forEach((target, count) -> assertEquals(1, count, target + " has several recipes"));
        assertFalse(targets.contains("block/voidvine_stalk"), "the bamboo stalk texture is unused");
        assertFalse(targets.contains("block/voidvine_lit"));
        assertFalse(targets.contains("block/voidvine_plant_lit"));

        Set<String> used = new TreeSet<>();
        for (String model : MODELS) {
            Map<String, String> textures = textures(model);
            assertFalse(textures.containsKey("stalk"), model + " still has a stalk");
            for (Map.Entry<String, String> texture : textures.entrySet()) {
                String value = texture.getValue();
                if (value.startsWith("#")) {
                    continue;
                }
                TextureId id = TextureId.parse(value);
                assertEquals("fwhipsperfectendupdate", id.namespace(), model + " uses " + value);
                assertTrue(targets.contains(id.path()), model + " texture " + value + " has no recipe");
                used.add(id.path());
            }
        }
        assertEquals(
                new TreeSet<>(Set.of(
                        "block/voidvine",
                        "block/voidvine_plant",
                        "block/voidvine_berries",
                        "block/voidvine_plant_berries",
                        "item/voidvine")),
                used);
        assertTrue(textures("block/voidvine_lit").containsKey("berries"));
        assertTrue(textures("block/voidvine_plant_lit").containsKey("berries"));
    }

    private static void assertRecipe(RecolorRecipe recipe, String source, boolean masked) {
        assertEquals(
                TextureId.parse("minecraft:" + source),
                recipe.source(),
                recipe.target().toString());
        assertEquals(
                Optional.of(RecolorRecipe.Flip.VERTICAL),
                recipe.flip(),
                recipe.target().toString());
        assertEquals(masked, recipe.keepOnlyMapped(), recipe.target().toString());
    }

    private static int[] recolor(RecolorRecipe recipe, byte[] source) throws IOException {
        return pixels(RecolorGenerator.recolorPng(source, recipe, false));
    }

    private static Map<String, RecolorRecipe> byTarget(List<RecolorRecipe> recipes) {
        Map<String, RecolorRecipe> map = new HashMap<>();
        recipes.forEach(r -> map.put(r.target().path(), r));
        return map;
    }

    /** Textures of a model merged with its parents' (children win), as the model baker resolves them. */
    private static Map<String, String> textures(String model) throws IOException {
        JsonObject json =
                JsonParser.parseString(read("models/" + model + ".json")).getAsJsonObject();
        Map<String, String> textures = new HashMap<>();
        if (json.has("parent")) {
            TextureId parent = TextureId.parse(json.get("parent").getAsString());
            if (parent.namespace().equals("fwhipsperfectendupdate")) {
                textures.putAll(textures(parent.path()));
            }
        }
        if (json.has("textures")) {
            json.getAsJsonObject("textures")
                    .entrySet()
                    .forEach(e -> textures.put(e.getKey(), e.getValue().getAsString()));
        }
        return textures;
    }

    private static String read(String path) throws IOException {
        try (InputStream in = VoidvineRecolorTest.class.getResourceAsStream(ASSETS + path)) {
            if (in == null) {
                throw new IOException("missing resource " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
