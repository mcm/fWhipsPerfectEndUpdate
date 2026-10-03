package io.mcmaster.someonesperfectend.recolor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParseException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RecolorRecipeParserTest {
    @Test
    void parsesSingleRecipeWithGradientAndColors() {
        List<RecolorRecipe> recipes = RecolorRecipeParser.parse("""
                {
                  "source": "minecraft:block/bamboo_planks",
                  "target": "someonesperfectend:block/astral_planks",
                  "palette": {"gradient": ["#000000", "#FF0000"], "normalize": false,
                              "colors": {"#00ff00": "0000ff"}}
                }
                """);
        assertEquals(1, recipes.size());
        RecolorRecipe recipe = recipes.get(0);
        assertEquals(new TextureId("minecraft", "block/bamboo_planks"), recipe.source());
        assertEquals(new TextureId("someonesperfectend", "block/astral_planks"), recipe.target());
        assertEquals(0x0000FF, recipe.palette().colors().get(0x00FF00));
        Palette.Gradient gradient = recipe.palette().gradient().orElseThrow();
        assertEquals(false, gradient.normalize());
        assertEquals(0x800000, gradient.colorAt(0.5f));
    }

    @Test
    void parsesRecipeListWithSharedAndOverriddenPalette() {
        List<RecolorRecipe> recipes = RecolorRecipeParser.parse("""
                {
                  "palette": {"hsv": {"hue": 250, "saturation": 0.5}},
                  "recipes": [
                    {"source": "block/bamboo_planks", "target": "someonesperfectend:block/a"},
                    {"source": "block/bamboo_door_top", "target": "someonesperfectend:block/b",
                     "palette": {"hsv": {"hue_shift": -30}}}
                  ]
                }
                """);
        assertEquals(2, recipes.size());
        assertEquals("minecraft", recipes.get(0).source().namespace(), "namespace defaults to minecraft");
        Palette.HsvShift shared = recipes.get(0).palette().hsv().orElseThrow();
        assertEquals(Optional.of(250f), shared.hue());
        assertEquals(0.5f, shared.saturation());
        assertEquals(1f, shared.value());
        Palette.HsvShift own = recipes.get(1).palette().hsv().orElseThrow();
        assertTrue(own.hue().isEmpty());
        assertEquals(-30f, own.hueShift());
    }

    @Test
    void parsesGradientStopObjectsAndDefaultsNormalizeToTrue() {
        RecolorRecipe recipe = RecolorRecipeParser.parse("""
                {"source": "a", "target": "b",
                 "palette": {"gradient": [{"at": 0, "color": "#000000"}, {"at": 0.25, "color": "#0000ff"}]}}
                """).get(0);
        Palette.Gradient gradient = recipe.palette().gradient().orElseThrow();
        assertTrue(gradient.normalize());
        assertEquals(0x0000FF, gradient.colorAt(0.5f));
    }

    @Test
    void rejectsInvalidRecipes() {
        assertThrows(JsonParseException.class, () -> RecolorRecipeParser.parse("{\"target\": \"b\", \"palette\": {}}"));
        assertThrows(
                JsonParseException.class, () -> RecolorRecipeParser.parse("{\"source\": \"a\", \"target\": \"b\"}"));
        assertThrows(
                JsonParseException.class,
                () -> RecolorRecipeParser.parse(
                        "{\"source\": \"a\", \"target\": \"b\", \"palette\": {\"gradient\": [\"#fff\"]}}"));
        assertThrows(
                JsonParseException.class,
                () -> RecolorRecipeParser.parse(
                        "{\"source\": \"a\", \"target\": \"b\", \"palette\": {\"gradient\": [\"#ffffff\"], \"hsv\": {}}}"));
        assertThrows(
                IllegalArgumentException.class,
                () -> RecolorRecipeParser.parse("{\"source\": \"Bad Id\", \"target\": \"b\", \"palette\": {}}"));
    }
}
