package io.mcmaster.someonesperfectend.block;

/** Pure timing rules for Enderblossom checks and ripples; no world access, so unit tests cover them. */
public final class EnderblossomTiming {
    /** Every blossom checks for Endermen this often, in ticks. */
    public static final int CHECK_INTERVAL = 10;

    private EnderblossomTiming() {}

    /** A blossom's check phase, from its position hash, so neighbors don't all check on the same tick. */
    public static int phase(int posHash) {
        return posHash & 7;
    }

    /**
     * Ticks from {@code gameTime} until the blossom's next check: the next game time {@code t > gameTime} with
     * {@code (t + phase) % CHECK_INTERVAL == 0}. Always 1 to {@link #CHECK_INTERVAL}, and exactly
     * {@link #CHECK_INTERVAL} when called from a check, so a blossom keeps its phase.
     */
    public static int nextCheckDelay(long gameTime, int phase) {
        return CHECK_INTERVAL - (int) Math.floorMod(gameTime + phase, CHECK_INTERVAL);
    }

    /** The shortest ripple delay to a blossom {@code distance} blocks away, like vanilla's Eyeblossom. */
    public static int minRippleDelay(double distance) {
        return (int) (distance * 5.0);
    }

    /** The longest ripple delay to a blossom {@code distance} blocks away, like vanilla's Eyeblossom. */
    public static int maxRippleDelay(double distance) {
        return (int) (distance * 10.0);
    }
}
