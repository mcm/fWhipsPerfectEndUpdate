package io.mcmaster.someonesperfectend.worldgen;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

/**
 * Which elevation band each outer-End column is in (MOD-47, MOD-71): whole islands move, they are never split.
 *
 * <p>Vanilla's {@code minecraft:end_islands} places island centers on a 16-block grid and gives each a round
 * island: its height value falls off as {@code 100 - distance × falloff} (distance in 8-block noise cells), and
 * a column takes the highest value of the centers around it. This class repeats that model:
 *
 * <ol>
 *   <li>Each noise cell corner (every 8 blocks, where the terrain density is sampled) belongs to the center with
 *       the highest value there, its <em>owner</em>.
 *   <li>Two centers are in the same <em>island group</em> when their islands, cut at {@code islandThreshold},
 *       come within {@code linkMargin} blocks of each other. Groups are found by flood fill and cached.
 *   <li>A group's band and jitter are rolled from its lowest center, so every corner of it agrees. Its offset
 *       scales with the distance of its center nearest the origin ({@link ElevationBands#groupOffset}). Groups
 *       with a center within {@code middleDistance} of the origin are middle band with no jitter.
 *   <li>A corner next to a corner of a group with another offset is forced to void, so the 8-block cells
 *       between them are not sheared. Such corners are in the void between islands by construction (2).
 * </ol>
 */
public final class ElevationBandLayout {
    /** Owner of corners with no island center in vanilla's search window (deep void). */
    private static final long NONE = Long.MIN_VALUE;
    /** Vanilla searches ±12 grid cells around a column for island centers. */
    private static final int WINDOW = 12;
    /** Groups larger than this (never seen in practice) stay middle band instead of being flood-filled further. */
    private static final int MAX_GROUP = 50_000;

    private static final int TILE_BITS = 5;
    private static final int TILE = 1 << TILE_BITS;
    private static final int MAX_TILES = 8192;
    private static final int MAX_GROUP_ENTRIES = 500_000;
    private static final int OWNER_CACHE_BITS = 12;
    private static final int OWNER_CACHE = 1 << OWNER_CACHE_BITS;

    private final ElevationBands settings;
    private final ElevationBands.IslandCenters centers;
    private final ElevationBands.CellRandom random;
    /** Island radius at the threshold for the smallest falloff, in noise cells. */
    private final double maxRadius;

    private final double linkCells;

    private final Map<Long, Tile> tiles = new ConcurrentHashMap<>();
    private final Map<Long, Group> groups = new ConcurrentHashMap<>();
    private final ThreadLocal<OwnerCache> owners = ThreadLocal.withInitial(OwnerCache::new);

    ElevationBandLayout(
            ElevationBands settings, ElevationBands.IslandCenters centers, ElevationBands.CellRandom random) {
        this.settings = settings;
        this.centers = centers;
        this.random = random;
        this.maxRadius = radius(9.0F);
        this.linkCells = settings.linkMargin() / 8.0;
    }

    /** Vanilla's island centers for an {@code end_islands} noise: outside 1024 blocks, where the noise is low. */
    public static ElevationBands.IslandCenters vanillaCenters(SimplexNoise noise) {
        return (gridX, gridZ) ->
                (long) gridX * gridX + (long) gridZ * gridZ > 4096L && noise.getValue(gridX, gridZ) < -0.9F;
    }

    public ElevationBands settings() {
        return settings;
    }

    /** Vertical offset of the terrain at column (x, z), or {@link ElevationBands#GAP} if it must be void. */
    public int offsetAt(int x, int z) {
        Group group = groupAt(x, z);
        return group == null ? ElevationBands.GAP : group.offset();
    }

    /**
     * The island group that column (x, z) moves with, {@link Group#VANILLA} where nothing moves, or null if the
     * column is in a void seam between groups with different offsets.
     */
    public Group groupAt(int x, int z) {
        int minDistance = settings.minDistance();
        if ((long) x * x + (long) z * z < (long) minDistance * minDistance) {
            return Group.VANILLA;
        }
        int cellX = x / 8;
        int cellZ = z / 8;
        long owner = owner(cellX, cellZ);
        if (owner == NONE) {
            return Group.VANILLA;
        }
        Group group = group(owner);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                long neighbor = owner(cellX + dx, cellZ + dz);
                if (neighbor != NONE && neighbor != owner && group(neighbor).offset() != group.offset()) {
                    return null;
                }
            }
        }
        return group;
    }

    /**
     * Vanilla's island height value at column (x, z) from the outer island centers only ({@code end_islands} is
     * {@code (value - 8) / 128}); -100 in deep void.
     */
    public float islandValue(int x, int z) {
        int cellX = x / 8;
        int cellZ = z / 8;
        long owner = owner(cellX, cellZ);
        if (owner == NONE) {
            return -100.0F;
        }
        return (float) Math.max(-100.0, Math.min(80.0, value(gridX(owner), gridZ(owner), cellX, cellZ)));
    }

    /** The center with the highest island value at a noise cell corner, or {@link #NONE}. */
    private long owner(int cellX, int cellZ) {
        OwnerCache cache = owners.get();
        int slot = (cellX * 0x1F1F1F1F ^ cellZ * 0x5BD1E995) >>> (32 - OWNER_CACHE_BITS);
        long key = key(cellX, cellZ);
        if (cache.valid[slot] && cache.keys[slot] == key) {
            return cache.owners[slot];
        }
        // Same window as vanilla: integer division truncates towards zero.
        int minX = cellX / 2 - WINDOW;
        int minZ = cellZ / 2 - WINDOW;
        int maxX = minX + 2 * WINDOW;
        int maxZ = minZ + 2 * WINDOW;
        long best = NONE;
        double bestValue = Double.NEGATIVE_INFINITY;
        for (int tileX = minX >> TILE_BITS; tileX <= maxX >> TILE_BITS; tileX++) {
            for (int tileZ = minZ >> TILE_BITS; tileZ <= maxZ >> TILE_BITS; tileZ++) {
                for (int i : tile(tileX, tileZ).centers) {
                    int gx = (tileX << TILE_BITS) + (i & (TILE - 1));
                    int gz = (tileZ << TILE_BITS) + (i >> TILE_BITS);
                    if (gx < minX || gx > maxX || gz < minZ || gz > maxZ) {
                        continue;
                    }
                    double value = value(gx, gz, cellX, cellZ);
                    if (value > bestValue) {
                        bestValue = value;
                        best = key(gx, gz);
                    }
                }
            }
        }
        cache.valid[slot] = true;
        cache.keys[slot] = key;
        cache.owners[slot] = best;
        return best;
    }

    /** The island group of a center. */
    private Group group(long center) {
        Group cached = groups.get(center);
        if (cached != null) {
            return cached;
        }
        Set<Long> members = new HashSet<>();
        ArrayDeque<Long> queue = new ArrayDeque<>();
        members.add(center);
        queue.add(center);
        long lowest = center;
        double nearest = Double.MAX_VALUE;
        while (!queue.isEmpty() && members.size() <= MAX_GROUP) {
            long current = queue.poll();
            int gx = gridX(current);
            int gz = gridZ(current);
            double gridDistance = Math.sqrt((double) gx * gx + (double) gz * gz) * 16;
            nearest = Math.min(nearest, gridDistance);
            if (gx < gridX(lowest) || (gx == gridX(lowest) && gz < gridZ(lowest))) {
                lowest = current;
            }
            double ownRadius = radius(falloff(gx, gz));
            int reach = (int) Math.ceil((ownRadius + maxRadius + linkCells) / 2);
            for (int dx = -reach; dx <= reach; dx++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    int ox = gx + dx;
                    int oz = gz + dz;
                    if (!isCenter(ox, oz)) {
                        continue;
                    }
                    long other = key(ox, oz);
                    if (members.contains(other)) {
                        continue;
                    }
                    double cells = 2 * Math.sqrt((double) dx * dx + (double) dz * dz);
                    if (cells < ownRadius + radius(falloff(ox, oz)) + linkCells) {
                        members.add(other);
                        queue.add(other);
                    }
                }
            }
        }
        Group group;
        if (nearest < settings.middleDistance() || members.size() > MAX_GROUP) {
            group = new Group(ElevationBands.Band.MIDDLE, nearest, 0, true);
        } else {
            double roll = random.value(gridX(lowest), gridZ(lowest), 2);
            ElevationBands.Band band = roll < 1 / 3.0
                    ? ElevationBands.Band.LOWER
                    : roll < 2 / 3.0 ? ElevationBands.Band.MIDDLE : ElevationBands.Band.UPPER;
            double jitter = 2 * random.value(gridX(lowest), gridZ(lowest), 3) - 1;
            group = new Group(band, nearest, settings.groupOffset(band, nearest, jitter), false);
        }
        if (groups.size() + members.size() > MAX_GROUP_ENTRIES) {
            groups.clear();
        }
        for (long member : members) {
            groups.put(member, group);
        }
        return group;
    }

    private boolean isCenter(int gridX, int gridZ) {
        int i = (gridX & (TILE - 1)) | (gridZ & (TILE - 1)) << TILE_BITS;
        return (tile(gridX >> TILE_BITS, gridZ >> TILE_BITS).bits[i >> 6] & 1L << i) != 0;
    }

    /** The island centers of a 32×32 block of grid cells, computed once. */
    private Tile tile(int tileX, int tileZ) {
        long tileKey = key(tileX, tileZ);
        Tile tile = tiles.get(tileKey);
        if (tile == null) {
            long[] bits = new long[TILE * TILE / 64];
            int count = 0;
            for (int i = 0; i < TILE * TILE; i++) {
                if (centers.isCenter(
                        (tileX << TILE_BITS) + (i & (TILE - 1)), (tileZ << TILE_BITS) + (i >> TILE_BITS))) {
                    bits[i >> 6] |= 1L << i;
                    count++;
                }
            }
            int[] list = new int[count];
            for (int i = 0, n = 0; i < TILE * TILE; i++) {
                if ((bits[i >> 6] & 1L << i) != 0) {
                    list[n++] = i;
                }
            }
            tile = new Tile(bits, list);
            if (tiles.size() >= MAX_TILES) {
                tiles.clear();
            }
            tiles.put(tileKey, tile);
        }
        return tile;
    }

    /** Island radius in noise cells where the value drops to the threshold. */
    private double radius(float falloff) {
        return (100.0 - settings.islandThreshold()) / falloff;
    }

    /** Vanilla's island value of a center at a noise cell corner, unclamped. */
    private static double value(int gridX, int gridZ, int cellX, int cellZ) {
        double dx = cellX - 2.0 * gridX;
        double dz = cellZ - 2.0 * gridZ;
        return 100.0 - Math.sqrt(dx * dx + dz * dz) * falloff(gridX, gridZ);
    }

    /** Vanilla's per-center falloff, 9 to 21 (smaller is a bigger island). */
    private static float falloff(int gridX, int gridZ) {
        return (Math.abs((float) gridX) * 3439.0F + Math.abs((float) gridZ) * 147.0F) % 13.0F + 9.0F;
    }

    private static long key(int x, int z) {
        return (long) x << 32 | (z & 0xFFFFFFFFL);
    }

    private static int gridX(long key) {
        return (int) (key >> 32);
    }

    private static int gridZ(long key) {
        return (int) key;
    }

    /**
     * An island group: its band, the distance of its center nearest the origin, its total offset (band offset
     * plus jitter, snapped and clamped), and whether it is fixed middle band near the origin.
     */
    public record Group(ElevationBands.Band band, double distance, int offset, boolean alwaysMiddle) {
        /** Columns that keep their vanilla terrain: near the origin, or with no island center around. */
        public static final Group VANILLA = new Group(ElevationBands.Band.MIDDLE, 0, 0, true);
    }

    /** Island centers of a tile: a bit per grid cell, and the indices of the set bits. */
    private record Tile(long[] bits, int[] centers) {}

    private static final class OwnerCache {
        private final long[] keys = new long[OWNER_CACHE];
        private final long[] owners = new long[OWNER_CACHE];
        private final boolean[] valid = new boolean[OWNER_CACHE];
    }
}
