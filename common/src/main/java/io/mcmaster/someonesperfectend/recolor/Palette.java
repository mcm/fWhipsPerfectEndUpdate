package io.mcmaster.someonesperfectend.recolor;

import java.util.Map;
import java.util.Optional;

/**
 * How a recolor recipe changes colors. Pixels are ARGB ints; alpha is never changed and fully transparent
 * pixels are left untouched.
 *
 * <p>For each pixel, in order:
 * <ol>
 *   <li>if its RGB is a key of {@link #colors()}, it becomes the mapped RGB;</li>
 *   <li>otherwise, if a {@link Gradient} is set, the pixel's luminance picks a color from the gradient;</li>
 *   <li>otherwise, if an {@link HsvShift} is set, the pixel's hue/saturation/value are shifted;</li>
 *   <li>otherwise it is unchanged.</li>
 * </ol>
 */
public record Palette(Map<Integer, Integer> colors, Optional<Gradient> gradient, Optional<HsvShift> hsv) {
    public Palette {
        colors = Map.copyOf(colors);
        if (gradient.isPresent() && hsv.isPresent()) {
            throw new IllegalArgumentException("A palette may have a gradient or an hsv shift, not both");
        }
    }

    public static Palette gradient(Gradient gradient) {
        return new Palette(Map.of(), Optional.of(gradient), Optional.empty());
    }

    public static Palette hsv(HsvShift hsv) {
        return new Palette(Map.of(), Optional.empty(), Optional.of(hsv));
    }

    public Palette withColors(Map<Integer, Integer> colors) {
        return new Palette(colors, gradient, hsv);
    }

    /** Recolors a whole image (row-major ARGB pixels) and returns a new array. */
    public int[] apply(int[] argb) {
        float minLum = 1f;
        float maxLum = 0f;
        if (gradient.isPresent() && gradient.get().normalize()) {
            for (int pixel : argb) {
                if (alpha(pixel) != 0 && !colors.containsKey(pixel & 0xFFFFFF)) {
                    float lum = luminance(pixel);
                    minLum = Math.min(minLum, lum);
                    maxLum = Math.max(maxLum, lum);
                }
            }
        }
        int[] out = new int[argb.length];
        for (int i = 0; i < argb.length; i++) {
            int pixel = argb[i];
            if (alpha(pixel) == 0) {
                out[i] = pixel;
                continue;
            }
            int rgb = pixel & 0xFFFFFF;
            Integer mapped = colors.get(rgb);
            if (mapped != null) {
                rgb = mapped;
            } else if (gradient.isPresent()) {
                float lum = luminance(pixel);
                if (gradient.get().normalize()) {
                    lum = maxLum > minLum ? (lum - minLum) / (maxLum - minLum) : 0.5f;
                }
                rgb = gradient.get().colorAt(lum);
            } else if (hsv.isPresent()) {
                rgb = hsv.get().apply(rgb);
            }
            out[i] = (pixel & 0xFF000000) | (rgb & 0xFFFFFF);
        }
        return out;
    }

    static int alpha(int argb) {
        return argb >>> 24;
    }

    /** Rec. 601 luma of the pixel's RGB, 0..1. */
    static float luminance(int argb) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return (0.299f * r + 0.587f * g + 0.114f * b) / 255f;
    }

    static int clampByte(float value) {
        return Math.max(0, Math.min(255, Math.round(value)));
    }

    /**
     * A gradient map: luminance 0 maps to the first stop, 1 to the last, with linear RGB interpolation in
     * between. With {@code normalize}, the image's darkest opaque pixel maps to 0 and its brightest to 1, so the
     * whole gradient is used even for a mostly light or dark source.
     */
    public record Gradient(float[] positions, int[] rgb, boolean normalize) {
        public Gradient {
            if (positions.length != rgb.length || positions.length == 0) {
                throw new IllegalArgumentException("A gradient needs at least one stop");
            }
            for (int i = 1; i < positions.length; i++) {
                if (positions[i] < positions[i - 1]) {
                    throw new IllegalArgumentException("Gradient stops must be in ascending order");
                }
            }
            positions = positions.clone();
            rgb = rgb.clone();
        }

        /** Stops spaced evenly from 0 to 1. */
        public static Gradient evenlySpaced(int[] rgb, boolean normalize) {
            float[] positions = new float[rgb.length];
            for (int i = 0; i < rgb.length; i++) {
                positions[i] = rgb.length == 1 ? 0f : (float) i / (rgb.length - 1);
            }
            return new Gradient(positions, rgb, normalize);
        }

        public int colorAt(float t) {
            if (t <= positions[0]) {
                return rgb[0];
            }
            int last = positions.length - 1;
            if (t >= positions[last]) {
                return rgb[last];
            }
            int i = 1;
            while (positions[i] < t) {
                i++;
            }
            float span = positions[i] - positions[i - 1];
            float f = span == 0 ? 1f : (t - positions[i - 1]) / span;
            return lerp(rgb[i - 1], rgb[i], f);
        }

        private static int lerp(int from, int to, float f) {
            int r = clampByte(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * f);
            int g = clampByte(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * f);
            int b = clampByte((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * f);
            return (r << 16) | (g << 8) | b;
        }
    }

    /**
     * A hue/saturation/value shift. {@code hue} (degrees, 0..360) replaces the hue when present; otherwise
     * {@code hueShift} degrees are added. Saturation and value are multiplied and clamped to 0..1.
     */
    public record HsvShift(Optional<Float> hue, float hueShift, float saturation, float value) {
        public int apply(int rgb) {
            float r = ((rgb >> 16) & 0xFF) / 255f;
            float g = ((rgb >> 8) & 0xFF) / 255f;
            float b = (rgb & 0xFF) / 255f;
            float max = Math.max(r, Math.max(g, b));
            float min = Math.min(r, Math.min(g, b));
            float delta = max - min;
            float h;
            if (delta == 0) {
                h = 0;
            } else if (max == r) {
                h = 60f * (((g - b) / delta) % 6f);
            } else if (max == g) {
                h = 60f * ((b - r) / delta + 2f);
            } else {
                h = 60f * ((r - g) / delta + 4f);
            }
            float s = max == 0 ? 0 : delta / max;
            float v = max;

            h = hue.orElse(h + hueShift);
            h = ((h % 360f) + 360f) % 360f;
            s = Math.max(0f, Math.min(1f, s * saturation));
            v = Math.max(0f, Math.min(1f, v * value));
            return hsvToRgb(h, s, v);
        }

        private static int hsvToRgb(float h, float s, float v) {
            float c = v * s;
            float x = c * (1 - Math.abs((h / 60f) % 2f - 1));
            float m = v - c;
            float r;
            float g;
            float b;
            int sector = (int) (h / 60f) % 6;
            switch (sector) {
                case 0 -> {
                    r = c;
                    g = x;
                    b = 0;
                }
                case 1 -> {
                    r = x;
                    g = c;
                    b = 0;
                }
                case 2 -> {
                    r = 0;
                    g = c;
                    b = x;
                }
                case 3 -> {
                    r = 0;
                    g = x;
                    b = c;
                }
                case 4 -> {
                    r = x;
                    g = 0;
                    b = c;
                }
                default -> {
                    r = c;
                    g = 0;
                    b = x;
                }
            }
            return (clampByte((r + m) * 255f) << 16) | (clampByte((g + m) * 255f) << 8) | clampByte((b + m) * 255f);
        }
    }
}
