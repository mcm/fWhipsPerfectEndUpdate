package io.mcmaster.fwhipsperfectendupdate.recolor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Decides which recipes produce a generated texture. A real PNG at a recipe's target (in the mod's own
 * assets or in any resource pack) always wins, so hand-drawn art replaces a placeholder without code
 * changes. When several recipes name the same target, the last one wins.
 */
public final class RecolorPrecedence {
    private RecolorPrecedence() {}

    public record Plan(List<RecolorRecipe> generate, List<RecolorRecipe> overridden) {}

    /**
     * @param recipes recipes in load order (later recipes override earlier ones with the same target)
     * @param realTextureExists whether any pack other than the generated one has a PNG for the texture id
     */
    public static Plan plan(List<RecolorRecipe> recipes, Predicate<TextureId> realTextureExists) {
        Map<TextureId, RecolorRecipe> byTarget = new LinkedHashMap<>();
        for (RecolorRecipe recipe : recipes) {
            byTarget.remove(recipe.target());
            byTarget.put(recipe.target(), recipe);
        }
        List<RecolorRecipe> generate = new ArrayList<>();
        List<RecolorRecipe> overridden = new ArrayList<>();
        for (RecolorRecipe recipe : byTarget.values()) {
            (realTextureExists.test(recipe.target()) ? overridden : generate).add(recipe);
        }
        return new Plan(List.copyOf(generate), List.copyOf(overridden));
    }
}
