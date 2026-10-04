package io.mcmaster.someonesperfectend.block;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Which players are looking at one Enderblossom, so it shakes when a player starts looking and then at most once
 * every {@link #REPEAT_INTERVAL} ticks while they keep looking. Kept in memory only.
 */
public final class LookShakeTracker {
    public static final int REPEAT_INTERVAL = 20;

    /** Game time of the last shake each looking player caused. */
    private final Map<UUID, Long> lastShake = new HashMap<>();

    /**
     * Records that exactly {@code looking} are looking at {@code gameTime}; players who looked away are forgotten.
     * Returns whether the blossom should shake.
     */
    public boolean update(Set<UUID> looking, long gameTime) {
        lastShake.keySet().retainAll(looking);
        boolean shake = false;
        for (UUID player : looking) {
            Long last = lastShake.get(player);
            if (last == null || gameTime - last >= REPEAT_INTERVAL) {
                lastShake.put(player, gameTime);
                shake = true;
            }
        }
        return shake;
    }
}
