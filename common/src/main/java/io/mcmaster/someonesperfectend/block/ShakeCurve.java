package io.mcmaster.someonesperfectend.block;

/**
 * The Enderblossom shake: a damped rotation, in degrees, about the blossom's attachment point. Pure, so unit tests
 * cover it; the client renderer applies it.
 */
public final class ShakeCurve {
    /** The first swing reaches this many degrees; later swings shrink linearly to 0. */
    public static final float PEAK_DEGREES = 12.0F;
    /** The shake lasts this many ticks. */
    public static final int DURATION = 12;
    /** Ticks per full swing (there and back). */
    private static final float PERIOD = 4.0F;
    /** While stared at, the blossom trembles back and forth this many degrees for as long as the stare lasts. */
    public static final float STARE_DEGREES = 6.0F;
    /** Ticks per full tremble while stared at: faster than a shake, like a stared-at Enderman's jitter. */
    private static final float STARE_PERIOD = 2.0F;

    private ShakeCurve() {}

    /**
     * The shake angle {@code ticksSinceStart + partialTick} ticks after the shake started: 0 before it starts and
     * after {@link #DURATION} ticks, peaking at {@link #PEAK_DEGREES} one tick in.
     */
    public static float angle(long ticksSinceStart, float partialTick) {
        float t = ticksSinceStart + partialTick;
        if (t <= 0.0F || t >= DURATION) {
            return 0.0F;
        }
        float swing = (float) Math.sin(t * 2.0 * Math.PI / PERIOD);
        // Full strength up to the first peak, then shrinking linearly to 0 at the end.
        float damping = Math.min(1.0F, (DURATION - t) / (DURATION - 1));
        return PEAK_DEGREES * swing * damping;
    }

    /**
     * The tremble angle {@code ticksSinceStart + partialTick} ticks after a player started staring: 0 before that,
     * then swinging between ±{@link #STARE_DEGREES} with no end, like an Enderman shakes for as long as it is stared
     * at.
     */
    public static float stareAngle(long ticksSinceStart, float partialTick) {
        float t = ticksSinceStart + partialTick;
        if (t <= 0.0F) {
            return 0.0F;
        }
        return STARE_DEGREES * (float) Math.sin(t * 2.0 * Math.PI / STARE_PERIOD);
    }
}
