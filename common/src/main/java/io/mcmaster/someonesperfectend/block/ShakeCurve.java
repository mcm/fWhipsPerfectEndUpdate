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
}
