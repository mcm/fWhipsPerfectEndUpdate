package io.mcmaster.someonesperfectend.recolor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PaletteTest {
    /** Black-to-red gradient on absolute luminance, plus an explicit green-to-blue entry. */
    private static final Palette BLACK_TO_RED = Palette.gradient(
                    Palette.Gradient.evenlySpaced(new int[] {0x000000, 0xFF0000}, false))
            .withColors(Map.of(0x00FF00, 0x0000FF));

    @Test
    void gradientMapsKnown4x4ImageAndPreservesAlpha() {
        int[] input = {
            0xFF000000, 0xFF333333, 0x80666666, 0xFFFFFFFF,
            0x00123456, 0xFF00FF00, 0x4000FF00, 0xFF999999,
            0x01CCCCCC, 0xFFFF0000, 0xFF0000FF, 0xFF808080,
            0xFFFFFFFF, 0xFF000000, 0xFF333333, 0x00000000,
        };
        int[] expected = {
            // grays: luminance g/255 -> red channel g
            0xFF000000, 0xFF330000, 0x80660000, 0xFFFF0000,
            // transparent pixel untouched; explicit #00ff00 -> #0000ff keeps each pixel's alpha
            0x00123456, 0xFF0000FF, 0x400000FF, 0xFF990000,
            // pure red has luma 0.299 -> 76 (0x4c), pure blue 0.114 -> 29 (0x1d)
            0x01CC0000, 0xFF4C0000, 0xFF1D0000, 0xFF800000,
            0xFFFF0000, 0xFF000000, 0xFF330000, 0x00000000,
        };
        assertArrayEquals(expected, BLACK_TO_RED.apply(input));
    }

    @Test
    void normalizedGradientStretchesLuminanceRange() {
        Palette palette = Palette.gradient(Palette.Gradient.evenlySpaced(new int[] {0x000000, 0xFFFFFF}, true));
        int[] out = palette.apply(new int[] {0xFF323232, 0xFFC8C8C8, 0xFF6E6E6E, 0x00FFFFFF});
        // 50 -> 0, 200 -> 255, 110 -> (110 - 50) / 150 = 0.4 -> 102; transparent pixels don't count
        assertArrayEquals(new int[] {0xFF000000, 0xFFFFFFFF, 0xFF666666, 0x00FFFFFF}, out);
    }

    @Test
    void gradientWithExplicitStopPositions() {
        Palette.Gradient gradient =
                new Palette.Gradient(new float[] {0f, 0.25f, 1f}, new int[] {0x000000, 0x0000FF, 0xFFFFFF}, false);
        assertEquals(0x000000, gradient.colorAt(0f));
        assertEquals(0x0000FF, gradient.colorAt(0.25f));
        assertEquals(0x000080, gradient.colorAt(0.125f) & 0xFFFFFF, "halfway to blue");
        assertEquals(0xFFFFFF, gradient.colorAt(2f));
    }

    @Test
    void hsvShift() {
        int red = 0xFFFF0000;
        assertEquals(0xFF00FF00, hsv(Optional.empty(), 120, 1, 1).apply(new int[] {red})[0]);
        assertEquals(0x800000FF, hsv(Optional.of(240f), 0, 1, 1).apply(new int[] {0x80FF0000})[0]);
        assertEquals(0xFFFFFFFF, hsv(Optional.empty(), 0, 0, 1).apply(new int[] {red})[0]);
        assertEquals(0xFF800000, hsv(Optional.empty(), 0, 1, 0.5f).apply(new int[] {red})[0]);
        assertEquals(0xFFFF0000, hsv(Optional.empty(), -360, 1, 1).apply(new int[] {red})[0]);
    }

    @Test
    void gradientAndHsvAreExclusive() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Palette(
                        Map.of(),
                        Optional.of(Palette.Gradient.evenlySpaced(new int[] {0}, false)),
                        Optional.of(new Palette.HsvShift(Optional.empty(), 0, 1, 1))));
    }

    private static Palette hsv(Optional<Float> hue, float shift, float saturation, float value) {
        return Palette.hsv(new Palette.HsvShift(hue, shift, saturation, value));
    }
}
