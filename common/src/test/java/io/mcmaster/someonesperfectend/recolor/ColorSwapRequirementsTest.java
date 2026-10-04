package io.mcmaster.someonesperfectend.recolor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** MOD-70: the Astral wood set and Voidvine swap their color schemes, read straight from the recipe files. */
class ColorSwapRequirementsTest {
    private static final List<String> GREEN_WOOD = List.of("#0f2616", "#24522e", "#3d7d45", "#76b371");
    private static final List<String> GREEN_BARK = List.of("#0b1f12", "#1d4526", "#3f7a45");
    private static final List<String> PURPLE_WOOD = List.of("#2a2659", "#5d64b8", "#98a4e6", "#d4dbff");
    private static final List<String> PURPLE_BARK = List.of("#1f1a45", "#4a3f8f", "#8a7fd0");
    private static final List<String> PURPLE_FOLIAGE = List.of("#3b3272", "#7f78c9", "#b9b3ef", "#e4e0ff");

    private static final Map<String, String> SHARED_BERRY_COLORS =
            Map.of("#a46422", "#157a86", "#eb8931", "#3fd3dd", "#f7e26b", "#c6fbff");
    private static final Map<String, String> VOIDFRUIT_COLORS = Map.of(
            "#92441a", "#0d5560",
            "#bf6717", "#157a86",
            "#d57219", "#1f98a4",
            "#f19645", "#3fd3dd",
            "#f4c05e", "#8becf2",
            "#f7e26b", "#c6fbff",
            "#c3c62f", "#8becf2");

    /** FR-1: the Astral wood set takes the old Voidvine greens. */
    @Test
    void astralWoodSetUsesTheGreens() throws IOException {
        JsonObject file = read("astral_wood.json");
        assertEquals(GREEN_WOOD, gradient(file.getAsJsonObject("palette")));
        assertEquals(GREEN_BARK, gradient(recipePalette(file, "someonesperfectend:block/astral_log")));
        assertEquals(GREEN_BARK, gradient(recipePalette(file, "someonesperfectend:block/flowering_astral_log")));
    }

    /** FR-2: the Voidvine wood set takes the old Astral wood purples. */
    @Test
    void voidvineWoodSetUsesThePurples() throws IOException {
        JsonObject file = read("voidvine_wood.json");
        assertEquals(PURPLE_WOOD, gradient(file.getAsJsonObject("palette")));
        assertEquals(PURPLE_BARK, gradient(recipePalette(file, "someonesperfectend:block/voidvine_block")));
    }

    /** FR-4: the Voidvine plant and Voidfruit take the Astral leaves purple and keep their cyan accents. */
    @Test
    void voidvinePlantUsesTheLeafPurpleAndKeepsCyanAccents() throws IOException {
        JsonObject file = read("voidvine_plant.json");
        JsonObject shared = file.getAsJsonObject("palette");
        assertEquals(PURPLE_FOLIAGE, gradient(shared));
        assertEquals(SHARED_BERRY_COLORS, colors(shared));
        JsonObject voidfruit = recipePalette(file, "someonesperfectend:item/voidfruit");
        assertEquals(PURPLE_FOLIAGE, gradient(voidfruit));
        assertEquals(VOIDFRUIT_COLORS, colors(voidfruit));
    }

    /** FR-5: no old stops survive on either side of the swap. */
    @Test
    void oldStopsAreGone() throws IOException {
        String astral = readText("astral_wood.json").toLowerCase(Locale.ROOT);
        for (String stop : concat(PURPLE_WOOD, PURPLE_BARK)) {
            assertFalse(astral.contains(stop), "astral_wood.json still contains " + stop);
        }
        for (String name : List.of("voidvine_wood.json", "voidvine_plant.json")) {
            String voidvine = readText(name).toLowerCase(Locale.ROOT);
            for (String stop : concat(GREEN_WOOD, GREEN_BARK)) {
                assertFalse(voidvine.contains(stop), name + " still contains " + stop);
            }
        }
    }

    private static List<String> concat(List<String> a, List<String> b) {
        List<String> all = new ArrayList<>(a);
        all.addAll(b);
        return all;
    }

    private static JsonObject recipePalette(JsonObject file, String target) {
        for (JsonElement element : file.getAsJsonArray("recipes")) {
            JsonObject recipe = element.getAsJsonObject();
            if (recipe.get("target").getAsString().equals(target)) {
                return recipe.getAsJsonObject("palette");
            }
        }
        throw new AssertionError("no recipe targets " + target);
    }

    private static List<String> gradient(JsonObject palette) {
        List<String> stops = new ArrayList<>();
        palette.getAsJsonArray("gradient")
                .forEach(e -> stops.add(e.getAsString().toLowerCase(Locale.ROOT)));
        return stops;
    }

    private static Map<String, String> colors(JsonObject palette) {
        Map<String, String> colors = new HashMap<>();
        palette.getAsJsonObject("colors")
                .entrySet()
                .forEach(e -> colors.put(e.getKey(), e.getValue().getAsString()));
        return colors;
    }

    private static JsonObject read(String name) throws IOException {
        return JsonParser.parseString(readText(name)).getAsJsonObject();
    }

    private static String readText(String name) throws IOException {
        try (InputStream in =
                ColorSwapRequirementsTest.class.getResourceAsStream("/assets/someonesperfectend/recolor/" + name)) {
            if (in == null) {
                throw new IOException("missing resource " + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
