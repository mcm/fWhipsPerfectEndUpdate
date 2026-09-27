package io.mcmaster.someonesperfectend.recolor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Parses recolor recipe files ({@code assets/someonesperfectend/recolor/*.json}). A file holds either one
 * recipe:
 *
 * <pre>{@code
 * {
 *   "source": "minecraft:block/bamboo_planks",
 *   "target": "someonesperfectend:block/astral_planks",
 *   "palette": {"gradient": ["#241f52", "#6f6fcf", "#d8d0ff"]}
 * }
 * }</pre>
 *
 * or several recipes sharing a default palette, each of which may override it:
 *
 * <pre>{@code
 * {
 *   "palette": {"hsv": {"hue": 250}},
 *   "recipes": [
 *     {"source": "minecraft:block/bamboo_planks", "target": "someonesperfectend:block/astral_planks"},
 *     {"source": "minecraft:block/bamboo_door_top", "target": "someonesperfectend:block/astral_door_top",
 *      "palette": {"hsv": {"hue": 260}}}
 *   ]
 * }
 * }</pre>
 *
 * <p>A palette has at most one of:
 * <ul>
 *   <li>{@code "gradient"}: a list of colors spaced evenly from dark to light, or of
 *       {@code {"at": 0..1, "color": "#rrggbb"}} stops, plus optional {@code "normalize"} (default
 *       {@code true}: stretch the source's luminance range over the whole gradient);</li>
 *   <li>{@code "hsv"}: {@code "hue"} (absolute, degrees) or {@code "hue_shift"} (degrees), and
 *       {@code "saturation"} / {@code "value"} multipliers (default 1);</li>
 * </ul>
 * and optionally {@code "colors"}: explicit {@code {"#rrggbb": "#rrggbb"}} replacements, which win over the
 * gradient or shift. Alpha is always kept.
 */
public final class RecolorRecipeParser {
    private RecolorRecipeParser() {
    }

    public static List<RecolorRecipe> parse(String json) {
        JsonElement root = JsonParser.parseString(json);
        if (!root.isJsonObject()) {
            throw new JsonParseException("Recolor file must be a JSON object");
        }
        JsonObject object = root.getAsJsonObject();
        List<RecolorRecipe> recipes = new ArrayList<>();
        if (object.has("recipes")) {
            Optional<Palette> shared = object.has("palette")
                    ? Optional.of(parsePalette(object.getAsJsonObject("palette")))
                    : Optional.empty();
            for (JsonElement entry : object.getAsJsonArray("recipes")) {
                recipes.add(parseRecipe(entry.getAsJsonObject(), shared));
            }
        } else {
            recipes.add(parseRecipe(object, Optional.empty()));
        }
        return recipes;
    }

    private static RecolorRecipe parseRecipe(JsonObject object, Optional<Palette> shared) {
        TextureId source = TextureId.parse(requireString(object, "source"));
        TextureId target = TextureId.parse(requireString(object, "target"));
        Palette palette = object.has("palette") ? parsePalette(object.getAsJsonObject("palette"))
                : shared.orElseThrow(() -> new JsonParseException("Recipe for " + target + " has no palette"));
        return new RecolorRecipe(source, target, palette);
    }

    public static Palette parsePalette(JsonObject object) {
        Optional<Palette.Gradient> gradient = Optional.empty();
        Optional<Palette.HsvShift> hsv = Optional.empty();
        if (object.has("gradient")) {
            boolean normalize = !object.has("normalize") || object.get("normalize").getAsBoolean();
            gradient = Optional.of(parseGradient(object.getAsJsonArray("gradient"), normalize));
        }
        if (object.has("hsv")) {
            JsonObject shift = object.getAsJsonObject("hsv");
            hsv = Optional.of(new Palette.HsvShift(
                    shift.has("hue") ? Optional.of(shift.get("hue").getAsFloat()) : Optional.empty(),
                    shift.has("hue_shift") ? shift.get("hue_shift").getAsFloat() : 0f,
                    shift.has("saturation") ? shift.get("saturation").getAsFloat() : 1f,
                    shift.has("value") ? shift.get("value").getAsFloat() : 1f));
        }
        Map<Integer, Integer> colors = new HashMap<>();
        if (object.has("colors")) {
            for (Map.Entry<String, JsonElement> entry : object.getAsJsonObject("colors").entrySet()) {
                colors.put(parseColor(entry.getKey()), parseColor(entry.getValue().getAsString()));
            }
        }
        try {
            return new Palette(colors, gradient, hsv);
        } catch (IllegalArgumentException e) {
            throw new JsonParseException(e.getMessage(), e);
        }
    }

    private static Palette.Gradient parseGradient(JsonArray array, boolean normalize) {
        int[] rgb = new int[array.size()];
        float[] positions = new float[array.size()];
        boolean explicitPositions = false;
        for (int i = 0; i < array.size(); i++) {
            JsonElement stop = array.get(i);
            if (stop.isJsonObject()) {
                explicitPositions = true;
                positions[i] = stop.getAsJsonObject().get("at").getAsFloat();
                rgb[i] = parseColor(requireString(stop.getAsJsonObject(), "color"));
            } else {
                rgb[i] = parseColor(stop.getAsString());
            }
        }
        try {
            return explicitPositions ? new Palette.Gradient(positions, rgb, normalize)
                    : Palette.Gradient.evenlySpaced(rgb, normalize);
        } catch (IllegalArgumentException e) {
            throw new JsonParseException(e.getMessage(), e);
        }
    }

    /** Parses {@code #rrggbb} (the {@code #} is optional) into a 24-bit RGB int. */
    public static int parseColor(String color) {
        String hex = color.startsWith("#") ? color.substring(1) : color;
        if (hex.length() != 6) {
            throw new JsonParseException("Colors must be #rrggbb: " + color);
        }
        try {
            return Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            throw new JsonParseException("Colors must be #rrggbb: " + color, e);
        }
    }

    private static String requireString(JsonObject object, String key) {
        if (!object.has(key)) {
            throw new JsonParseException("Missing \"" + key + "\"");
        }
        return object.get(key).getAsString();
    }
}
