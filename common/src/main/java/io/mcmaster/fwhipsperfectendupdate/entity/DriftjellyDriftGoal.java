package io.mcmaster.fwhipsperfectendupdate.entity;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Wanders to a nearby spot at hover height, reached by a straight line that never goes more than
 * {@link Driftjelly#MAX_VOID_DRIFT} blocks past the edge of the ground, so a driftjelly may drift a little way out over
 * the void (or across a gap that narrow) but never wanders off into it. Spots over the void keep the current height,
 * since there is no ground to hover above. A driftjelly contained by a Glowcap only picks spots inside its containment
 * box (which is convex, so the straight line stays inside too).
 */
public class DriftjellyDriftGoal extends Goal {
    public static final int RANGE = 8;
    private static final int ATTEMPTS = 10;

    private final Driftjelly jelly;
    private final double speed;

    public DriftjellyDriftGoal(Driftjelly jelly, double speed) {
        this.jelly = jelly;
        this.speed = speed;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return !jelly.isDormant()
                && jelly.getNavigation().isDone()
                && jelly.getRandom().nextInt(reducedTickDelay(60)) == 0;
    }

    @Override
    public boolean canContinueToUse() {
        return !jelly.isDormant() && jelly.getNavigation().isInProgress();
    }

    @Override
    public void start() {
        Vec3 target = pickTarget(jelly);
        if (target != null) {
            jelly.getNavigation().moveTo(target.x, target.y, target.z, speed);
        }
    }

    /** A random reachable target near ground, or {@code null} if no attempt found one. */
    @Nullable
    public static Vec3 pickTarget(Driftjelly jelly) {
        Level level = jelly.level();
        RandomSource random = jelly.getRandom();
        Vec3 from = jelly.position();
        for (int i = 0; i < ATTEMPTS; i++) {
            double x = Math.floor(from.x + random.nextInt(2 * RANGE + 1) - RANGE) + 0.5;
            double z = Math.floor(from.z + random.nextInt(2 * RANGE + 1) - RANGE) + 0.5;
            if (jelly.hasRestriction()
                    && jelly.getRestrictCenter()
                                    .distToCenterSqr(
                                            x, jelly.getRestrictCenter().getY(), z)
                            > (double) jelly.getRestrictRadius() * jelly.getRestrictRadius()) {
                continue;
            }
            // Look for ground below the column, starting a little above the current height (for rising terrain).
            double probe = from.y + 2.0;
            double height = Driftjelly.heightAboveGround(level, x, probe, z);
            Vec3 target;
            if (height == Driftjelly.NO_GROUND) {
                target = new Vec3(x, from.y, z);
            } else {
                double ground = probe - height;
                double hover =
                        jelly.minHover() + 1.0 + random.nextDouble() * (jelly.maxHover() - jelly.minHover() - 2.0);
                target = new Vec3(x, ground + hover, z);
            }
            BlockPos containment = jelly.containment();
            if (containment != null
                    && !Driftjelly.insideContainment(containment, target, Driftjelly.CONTAINMENT_MARGIN)) {
                continue;
            }
            if (staysNearGround(jelly, from, target)
                    && level.noCollision(
                            jelly, jelly.getDimensions(jelly.getPose()).makeBoundingBox(target))) {
                return target;
            }
        }
        return null;
    }

    /**
     * Every point along the straight line (one sample per block) either has ground within scan range below it or is
     * within {@link Driftjelly#MAX_VOID_DRIFT} blocks of the last point before it that did: the same leash the
     * driftjelly itself enforces over the void, so following the line never makes it turn back.
     */
    private static boolean staysNearGround(Driftjelly jelly, Vec3 from, Vec3 to) {
        Vec3 ground = Driftjelly.heightAboveGround(jelly.level(), from.x, from.y, from.z) == Driftjelly.NO_GROUND
                ? jelly.lastGroundPos()
                : from;
        int steps = Math.max(1, (int) Math.ceil(from.distanceTo(to)));
        for (int step = 1; step <= steps; step++) {
            Vec3 point = from.lerp(to, (double) step / steps);
            if (Driftjelly.heightAboveGround(jelly.level(), point.x, point.y, point.z) != Driftjelly.NO_GROUND) {
                ground = point;
            } else if (!Driftjelly.withinVoidDrift(ground, point.x, point.z)) {
                return false;
            }
        }
        return true;
    }
}
