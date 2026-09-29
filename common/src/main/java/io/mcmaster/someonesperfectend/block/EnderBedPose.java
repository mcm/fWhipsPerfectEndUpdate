package io.mcmaster.someonesperfectend.block;

import net.minecraft.core.Direction;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * Where a player sleeping under a hanging Ender Bed lies (MOD-42). Vanilla draws a sleeper face-up with their back
 * {@link #BACK_DEPTH} below the entity's position; under an Ender Bed the model is rolled 180° about the bed's long
 * axis (face toward the floor, head still at the head end) and lifted so the back rests against the underside of the
 * bed's base.
 */
public final class EnderBedPose {
    /** Height of the underside of the bed's base above the block's bottom. */
    public static final double UNDERSIDE = 7.0 / 16.0;
    /** How far below the entity's position vanilla's sleeping model has its back (half the 4-pixel body depth). */
    public static final float BACK_DEPTH = 2.0F / 16.0F;

    private EnderBedPose() {}

    /** The y a sleeper is pinned to so the top of their sleeping hitbox touches the bed's underside. */
    public static double sleepingY(int bedY, double hitboxHeight) {
        return bedY + UNDERSIDE - hitboxHeight;
    }

    /** The 180° roll about the horizontal axis the bed runs along. */
    public static Quaternionf roll(Direction facing) {
        return new Quaternionf()
                .rotateAxis((float) Math.PI, Math.abs(facing.getStepX()), 0.0F, Math.abs(facing.getStepZ()));
    }

    /**
     * How far to lift the rolled model, in the entity frame already scaled by {@code scale}: its back (now
     * {@code BACK_DEPTH * scale} above the entity) goes to the top of the {@code hitboxHeight}-tall sleeping hitbox.
     */
    public static float lift(float scale, float hitboxHeight) {
        return (hitboxHeight - BACK_DEPTH * scale) / scale;
    }

    /**
     * The whole transform, applied in the world-aligned entity frame on top of vanilla's sleeping rotations: the
     * {@link #roll}, then the {@link #lift}.
     */
    public static Matrix4f transform(Direction facing, float scale, float hitboxHeight) {
        return new Matrix4f().translate(0.0F, lift(scale, hitboxHeight), 0.0F).rotate(roll(facing));
    }
}
