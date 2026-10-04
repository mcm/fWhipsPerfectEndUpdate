package io.mcmaster.fwhipsperfectendupdate.recolor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class RecolorGeneratorTest {
    private static final Palette RED =
            Palette.gradient(Palette.Gradient.evenlySpaced(new int[] {0x000000, 0xFF0000}, false));

    static byte[] png(int width, int height, int[] argb) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, width, height, argb, 0, width);
        return RecolorGenerator.encodePng(image);
    }

    static int[] pixels(byte[] png) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
    }

    @Test
    void pngRoundTripAppliesPaletteAndKeepsAlpha() throws IOException {
        byte[] source = png(2, 2, new int[] {0xFF333333, 0x80FFFFFF, 0x00000000, 0x10666666});
        int[] out = pixels(RecolorGenerator.recolorPng(source, RED));
        assertArrayEquals(new int[] {0xFF330000, 0x80FF0000, 0x00000000, 0x10660000}, out);
    }

    @Test
    void animatedTexturesKeepTheirMcmeta() throws IOException {
        byte[] mcmeta = "{\"animation\":{\"frametime\":3}}".getBytes(StandardCharsets.UTF_8);
        // 1x2: two animation frames stacked vertically
        byte[] source = png(1, 2, new int[] {0xFFFFFFFF, 0xFF000000});
        RecolorRecipe recipe = new RecolorRecipe(
                TextureId.parse("block/water_still"),
                TextureId.parse("fwhipsperfectendupdate:block/oasis_water_still"),
                RED);

        RecolorGenerator.Result result = RecolorGenerator.generate(
                List.of(recipe),
                id -> Optional.of(new RecolorGenerator.SourceTexture(source, Optional.of(mcmeta))),
                id -> false);

        Map<RecolorGenerator.GeneratedFile, byte[]> files = result.files();
        assertArrayEquals(
                mcmeta,
                files.get(new RecolorGenerator.GeneratedFile(
                        "fwhipsperfectendupdate", "textures/block/oasis_water_still.png.mcmeta")));
        assertArrayEquals(
                new int[] {0xFFFF0000, 0xFF000000},
                pixels(files.get(new RecolorGenerator.GeneratedFile(
                        "fwhipsperfectendupdate", "textures/block/oasis_water_still.png"))));
        assertEquals(List.of(recipe.target()), result.generated());
    }

    @Test
    void missingSourceIsReportedAndSkipped() {
        RecolorRecipe recipe = new RecolorRecipe(
                TextureId.parse("block/missing"), TextureId.parse("fwhipsperfectendupdate:block/x"), RED);
        RecolorGenerator.Result result =
                RecolorGenerator.generate(List.of(recipe), id -> Optional.empty(), id -> false);
        assertTrue(result.files().isEmpty());
        assertFalse(result.errors().isEmpty());
        assertTrue(result.generated().isEmpty());
    }

    private static final Palette IDENTITY = new Palette(Map.of(), Optional.empty(), Optional.empty());

    private static RecolorRecipe recipe(Palette palette, Optional<RecolorRecipe.Flip> flip, boolean keepOnlyMapped) {
        return new RecolorRecipe(
                TextureId.parse("block/stone"),
                TextureId.parse("fwhipsperfectendupdate:block/x"),
                palette,
                flip,
                keepOnlyMapped);
    }

    @Test
    void verticalFlipTurnsTheImageUpsideDownAfterRecoloring() throws IOException {
        // 2x3, rows: black, grey, white
        byte[] source = png(2, 3, new int[] {0xFF000000, 0xFF000000, 0xFF808080, 0xFF808080, 0xFFFFFFFF, 0x80FFFFFF});
        int[] out = pixels(RecolorGenerator.recolorPng(
                source, recipe(RED, Optional.of(RecolorRecipe.Flip.VERTICAL), false), false));
        assertArrayEquals(new int[] {0xFFFF0000, 0x80FF0000, 0xFF800000, 0xFF800000, 0xFF000000, 0xFF000000}, out);
    }

    @Test
    void horizontalFlipMirrorsEachRow() throws IOException {
        byte[] source = png(3, 2, new int[] {0xFF000001, 0xFF000002, 0xFF000003, 0xFF000004, 0x00000000, 0xFF000006});
        int[] out = pixels(RecolorGenerator.recolorPng(
                source, recipe(IDENTITY, Optional.of(RecolorRecipe.Flip.HORIZONTAL), false), false));
        assertArrayEquals(new int[] {0xFF000003, 0xFF000002, 0xFF000001, 0xFF000006, 0x00000000, 0xFF000004}, out);
    }

    @Test
    void verticalFlipKeepsAnimationFrameOrder() throws IOException {
        // 2x4: an animation strip of two 2x2 frames
        int[] strip = {
            0xFF000001, 0xFF000001, 0xFF000002, 0xFF000002,
            0xFF000003, 0xFF000003, 0xFF000004, 0xFF000004
        };
        RecolorRecipe recipe = recipe(IDENTITY, Optional.of(RecolorRecipe.Flip.VERTICAL), false);
        int[] out = pixels(RecolorGenerator.recolorPng(png(2, 4, strip), recipe, true));
        assertArrayEquals(
                new int[] {
                    0xFF000002, 0xFF000002, 0xFF000001, 0xFF000001,
                    0xFF000004, 0xFF000004, 0xFF000003, 0xFF000003
                },
                out);
        int[] still = pixels(RecolorGenerator.recolorPng(png(2, 4, strip), recipe, false));
        assertArrayEquals(
                new int[] {
                    0xFF000004, 0xFF000004, 0xFF000003, 0xFF000003,
                    0xFF000002, 0xFF000002, 0xFF000001, 0xFF000001
                },
                still,
                "without .mcmeta the whole image flips");
    }

    @Test
    void keepOnlyMappedClearsUnmappedPixelsAndRecolorsTheRest() throws IOException {
        Palette berries = RED.withColors(Map.of(0xEB8931, 0x3FD3DD, 0xF7E26B, 0xC6FBFF));
        byte[] source = png(2, 2, new int[] {0xFFEB8931, 0xFF333333, 0x80F7E26B, 0x00EB8931});
        int[] out = pixels(RecolorGenerator.recolorPng(source, recipe(berries, Optional.empty(), true), false));
        assertArrayEquals(
                new int[] {0xFF3FD3DD, 0x00000000, 0x80C6FBFF, 0x00EB8931},
                out,
                "fully transparent pixels are left alone, as before");
    }

    @Test
    void keepOnlyMappedCombinesWithFlip() throws IOException {
        Palette berries = IDENTITY.withColors(Map.of(0x00FF00, 0x0000FF));
        byte[] source = png(1, 2, new int[] {0xFF00FF00, 0xFF123456});
        int[] out = pixels(RecolorGenerator.recolorPng(
                source, recipe(berries, Optional.of(RecolorRecipe.Flip.VERTICAL), true), false));
        assertArrayEquals(new int[] {0x00000000, 0xFF0000FF}, out);
    }

    /**
     * Output of the voidvine recipe captured before flip / keep_only_mapped existed; must never change. The recipe
     * is inlined as it was then, because the real voidvine recipe now flips its source.
     */
    @Test
    void existingRecipeWithoutNewFieldsIsUnchanged() throws IOException {
        String json = """
                {
                  "palette": {
                    "gradient": ["#0f2616", "#24522e", "#3d7d45", "#76b371"],
                    "colors": {"#a46422": "#157a86", "#eb8931": "#3fd3dd", "#f7e26b": "#c6fbff"}
                  },
                  "recipes": [
                    {"source": "minecraft:block/cave_vines", "target": "fwhipsperfectendupdate:block/voidvine"}
                  ]
                }
                """;
        RecolorRecipe recipe =
                RecolorRecipeParser.parse("voidvine_plant.json", json).get(0);
        assertTrue(recipe.flip().isEmpty());
        assertFalse(recipe.keepOnlyMapped());
        byte[] source = png(4, 4, new int[] {
            0xFF000000, 0xFF333333, 0xFF808080, 0xFFFFFFFF,
            0xFFA46422, 0xFFEB8931, 0xFFF7E26B, 0x00000000,
            0x80123456, 0xFF4A7A2C, 0xFF6BA03E, 0x40FFFFFF,
            0xFF1A2B3C, 0xFFC0C0C0, 0xFF2E5A1C, 0xFF99CC66
        });

        RecolorGenerator.Result result = RecolorGenerator.generate(
                List.of(recipe),
                id -> Optional.of(new RecolorGenerator.SourceTexture(source, Optional.empty())),
                id -> false);

        assertArrayEquals(
                new int[] {
                    0xFF0F2616, 0xFF1C4024, 0xFF31683A, 0xFF76B371,
                    0xFF157A86, 0xFF3FD3DD, 0xFFC6FBFF, 0x00000000,
                    0x801A3E23, 0xFF285932, 0xFF326A3B, 0x4076B371,
                    0xFF193B21, 0xFF4C8B50, 0xFF204A2A, 0xFF428249
                },
                pixels(result.files()
                        .get(new RecolorGenerator.GeneratedFile(
                                "fwhipsperfectendupdate", "textures/block/voidvine.png"))));
    }
}
