package io.mcmaster.someonesperfectend.worldgen;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;

/**
 * The horizontal plan of an Endling village (US-023), relative to the village center: 1–3 huge glowcaps
 * near the center and a ring of 3–6 huts around them, each with its door facing the center. Kept free of
 * registries so it can be unit-tested.
 */
public record EndlingVillageLayout(List<Site> glowcaps, List<Hut> huts) {
    public static final int MIN_GLOWCAPS = 1;
    public static final int MAX_GLOWCAPS = 3;
    public static final int MIN_HUTS = 3;
    public static final int MAX_HUTS = 6;
    /** Huts are {@code HUT_SIZE}×{@code HUT_SIZE} blocks, centered on their site. */
    public static final int HUT_SIZE = 5;
    /** Distance of extra glowcaps from the center (a lone glowcap stands at the center). */
    public static final int GLOWCAP_RING = 4;

    public static final int MIN_HUT_RING = 14;
    public static final int MAX_HUT_RING = 16;
    /** Horizontal radius (from the center) that the whole village, huts included, fits in. */
    public static final int RADIUS = MAX_HUT_RING + HUT_SIZE / 2 + 1;

    /** No village part may come closer than this to (0, 0). */
    public static final int MIN_DISTANCE = 1024;

    /** A column offset from the village center. */
    public record Site(int dx, int dz) {}

    /**
     * A hut centered on (dx, dz). {@code door} is the side facing the village center: 0 north (−z), 1 east
     * (+x), 2 south (+z), 3 west (−x). {@code astral} picks Astral walls (else Voidvine).
     */
    public record Hut(int dx, int dz, int door, boolean astral) {}

    public static EndlingVillageLayout create(RandomSource random) {
        int glowcapCount = MIN_GLOWCAPS + random.nextInt(MAX_GLOWCAPS - MIN_GLOWCAPS + 1);
        List<Site> glowcaps = new ArrayList<>();
        if (glowcapCount == 1) {
            glowcaps.add(new Site(0, 0));
        } else {
            double base = random.nextDouble() * Math.PI * 2;
            for (int i = 0; i < glowcapCount; i++) {
                double angle = base + i * Math.PI * 2 / glowcapCount;
                glowcaps.add(new Site((int) Math.round(Math.cos(angle) * GLOWCAP_RING), (int)
                        Math.round(Math.sin(angle) * GLOWCAP_RING)));
            }
        }
        int hutCount = MIN_HUTS + random.nextInt(MAX_HUTS - MIN_HUTS + 1);
        List<Hut> huts = new ArrayList<>();
        double base = random.nextDouble() * Math.PI * 2;
        double step = Math.PI * 2 / hutCount;
        for (int i = 0; i < hutCount; i++) {
            double angle = base + i * step + (random.nextDouble() - 0.5) * 0.2 * step;
            int ring = MIN_HUT_RING + random.nextInt(MAX_HUT_RING - MIN_HUT_RING + 1);
            int dx = (int) Math.round(Math.cos(angle) * ring);
            int dz = (int) Math.round(Math.sin(angle) * ring);
            huts.add(new Hut(dx, dz, doorFacingCenter(dx, dz), random.nextBoolean()));
        }
        return new EndlingVillageLayout(List.copyOf(glowcaps), List.copyOf(huts));
    }

    /** Whether a village centered on (x, z) keeps its whole footprint at least {@link #MIN_DISTANCE} out. */
    public static boolean farEnough(int x, int z) {
        long min = MIN_DISTANCE + RADIUS;
        return (long) x * x + (long) z * z >= min * min;
    }

    /** The side (0 N, 1 E, 2 S, 3 W) of a hut at (dx, dz) that faces the center. */
    public static int doorFacingCenter(int dx, int dz) {
        if (Math.abs(dx) >= Math.abs(dz)) {
            return dx > 0 ? 3 : 1;
        }
        return dz > 0 ? 0 : 2;
    }
}
