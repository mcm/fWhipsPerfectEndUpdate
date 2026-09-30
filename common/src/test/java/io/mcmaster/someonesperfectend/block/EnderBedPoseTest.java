package io.mcmaster.someonesperfectend.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.Direction;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class EnderBedPoseTest {
    private static final float EPSILON = 1.0E-5F;
    /** Vanilla's sleeping hitbox height. */
    private static final float HEIGHT = 0.2F;

    private static void assertVector(Vector3f expected, Vector3f actual, String message) {
        assertEquals(expected.x, actual.x, EPSILON, message + " x");
        assertEquals(expected.y, actual.y, EPSILON, message + " y");
        assertEquals(expected.z, actual.z, EPSILON, message + " z");
    }

    @Test
    void sleeperIsPinnedWithTheHitboxTopAtTheUnderside() {
        assertEquals(64 + 7.0 / 16.0, EnderBedPose.sleepingY(64, HEIGHT) + HEIGHT, 1.0E-9);
        assertEquals(-3 + 7.0 / 16.0, EnderBedPose.sleepingY(-3, HEIGHT) + HEIGHT, 1.0E-9);
    }

    @Test
    void rollsFaceDownWithTheBackOnTheUndersideAndTheHeadInPlaceForAllFacings() {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            Matrix4f transform = EnderBedPose.transform(facing, 1.0F, HEIGHT);
            // Relative to the entity, whose position is HEIGHT below the underside.
            float underside = HEIGHT;
            // Vanilla lies the model along the bed with its back BACK_DEPTH below the entity and the face up.
            Vector3f headOnAxis = new Vector3f(facing.getStepX(), 0.0F, facing.getStepZ());
            assertVector(
                    new Vector3f(facing.getStepX(), underside - EnderBedPose.BACK_DEPTH, facing.getStepZ()),
                    transform.transformPosition(new Vector3f(headOnAxis)),
                    facing + " head stays at the head end");
            assertVector(
                    new Vector3f(0.0F, underside, 0.0F),
                    transform.transformPosition(new Vector3f(0.0F, -EnderBedPose.BACK_DEPTH, 0.0F)),
                    facing + " back rests on the underside");
            assertVector(
                    new Vector3f(0.0F, -1.0F, 0.0F),
                    transform.transformDirection(new Vector3f(0.0F, 1.0F, 0.0F)),
                    facing + " face points down");
            // The long axis is fixed, so the sleeper stays within the bed's footprint sideways.
            Vector3f side = new Vector3f(facing.getStepZ(), 0.0F, -facing.getStepX());
            assertVector(
                    side.negate(new Vector3f()).add(0.0F, underside - EnderBedPose.BACK_DEPTH, 0.0F),
                    transform.transformPosition(new Vector3f(side)),
                    facing + " sides swap");
        }
    }

    @Test
    void cameraGoesThroughTheSameRollAndLiftAsTheModel() {
        // Vanilla's sleeping camera: eye height plus 0.3 above the entity.
        float vanillaOffset = HEIGHT + 0.3F;
        double entityY = 64 + 7.0 / 16.0 - HEIGHT;
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            Vector3f moved = EnderBedPose.transform(facing, 1.0F, HEIGHT)
                    .transformPosition(new Vector3f(0.0F, vanillaOffset, 0.0F));
            assertEquals(
                    entityY + moved.y,
                    EnderBedPose.cameraY(entityY, entityY + vanillaOffset, 1.0F, HEIGHT),
                    EPSILON,
                    facing + " camera");
        }
        assertTrue(
                EnderBedPose.cameraY(entityY, entityY + vanillaOffset, 1.0F, HEIGHT) < 64 + 7.0 / 16.0,
                "the camera is below the bed's underside");
    }

    @Test
    void scaledSleeperStillRestsOnTheUnderside() {
        float scale = 0.5F;
        Matrix4f transform = EnderBedPose.transform(Direction.NORTH, scale, HEIGHT * scale);
        // The frame is scaled by `scale`, so world offsets are the transformed point times `scale`.
        Vector3f back = transform
                .transformPosition(new Vector3f(0.0F, -EnderBedPose.BACK_DEPTH, 0.0F))
                .mul(scale);
        assertVector(new Vector3f(0.0F, HEIGHT * scale, 0.0F), back, "scaled back");
    }
}
