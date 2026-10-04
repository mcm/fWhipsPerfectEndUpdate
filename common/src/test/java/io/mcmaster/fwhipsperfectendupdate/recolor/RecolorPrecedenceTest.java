package io.mcmaster.fwhipsperfectendupdate.recolor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RecolorPrecedenceTest {
    private static final Palette PALETTE = Palette.hsv(new Palette.HsvShift(Optional.empty(), 90, 1, 1));
    private static final Palette OTHER = Palette.hsv(new Palette.HsvShift(Optional.empty(), 180, 1, 1));

    private static RecolorRecipe recipe(String target, Palette palette) {
        return new RecolorRecipe(TextureId.parse("minecraft:block/stone"), TextureId.parse(target), palette);
    }

    @Test
    void realPngAtTargetWins() {
        RecolorRecipe handDrawn = recipe("fwhipsperfectendupdate:block/hand_drawn", PALETTE);
        RecolorRecipe placeholder = recipe("fwhipsperfectendupdate:block/placeholder", PALETTE);
        Set<TextureId> realPngs = Set.of(handDrawn.target());

        RecolorPrecedence.Plan plan = RecolorPrecedence.plan(List.of(handDrawn, placeholder), realPngs::contains);

        assertEquals(List.of(placeholder), plan.generate());
        assertEquals(List.of(handDrawn), plan.overridden());
    }

    @Test
    void everythingIsGeneratedWithoutRealPngs() {
        RecolorRecipe a = recipe("fwhipsperfectendupdate:block/a", PALETTE);
        RecolorRecipe b = recipe("fwhipsperfectendupdate:block/b", PALETTE);
        assertEquals(
                List.of(a, b),
                RecolorPrecedence.plan(List.of(a, b), id -> false).generate());
    }

    @Test
    void laterRecipeForTheSameTargetWins() {
        RecolorRecipe first = recipe("fwhipsperfectendupdate:block/a", PALETTE);
        RecolorRecipe second = recipe("fwhipsperfectendupdate:block/a", OTHER);
        assertEquals(
                List.of(second),
                RecolorPrecedence.plan(List.of(first, second), id -> false).generate());
    }

    @Test
    void generatorSkipsTargetsWithRealPngs() {
        RecolorRecipe handDrawn = recipe("fwhipsperfectendupdate:block/hand_drawn", PALETTE);
        RecolorGenerator.Result result = RecolorGenerator.generate(
                List.of(handDrawn),
                id -> {
                    throw new AssertionError("source must not be read for an overridden target");
                },
                id -> true);
        assertEquals(Map.of(), result.files());
        assertEquals(List.of(handDrawn.target()), result.overridden());
    }
}
