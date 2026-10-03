package io.mcmaster.someonesperfectend.recolor;

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
                TextureId.parse("someonesperfectend:block/oasis_water_still"),
                RED);

        RecolorGenerator.Result result = RecolorGenerator.generate(
                List.of(recipe),
                id -> Optional.of(new RecolorGenerator.SourceTexture(source, Optional.of(mcmeta))),
                id -> false);

        Map<RecolorGenerator.GeneratedFile, byte[]> files = result.files();
        assertArrayEquals(
                mcmeta,
                files.get(new RecolorGenerator.GeneratedFile(
                        "someonesperfectend", "textures/block/oasis_water_still.png.mcmeta")));
        assertArrayEquals(
                new int[] {0xFFFF0000, 0xFF000000},
                pixels(files.get(new RecolorGenerator.GeneratedFile(
                        "someonesperfectend", "textures/block/oasis_water_still.png"))));
        assertEquals(List.of(recipe.target()), result.generated());
    }

    @Test
    void missingSourceIsReportedAndSkipped() {
        RecolorRecipe recipe =
                new RecolorRecipe(TextureId.parse("block/missing"), TextureId.parse("someonesperfectend:block/x"), RED);
        RecolorGenerator.Result result =
                RecolorGenerator.generate(List.of(recipe), id -> Optional.empty(), id -> false);
        assertTrue(result.files().isEmpty());
        assertFalse(result.errors().isEmpty());
        assertTrue(result.generated().isEmpty());
    }
}
