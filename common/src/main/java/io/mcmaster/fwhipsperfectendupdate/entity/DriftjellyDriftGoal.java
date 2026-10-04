package io.mcmaster.fwhipsperfectendupdate.entity;

import java.util.EnumSet;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Wanders to a nearby spot at hover height, but only to spots over solid ground reached by a straight line that stays
 * over ground the whole way, so a driftjelly never deliberately drifts out over the void.
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

    /** A random reachable target over ground at hover height, or {@code null} if no attempt found one. */
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
            if (height == Driftjelly.NO_GROUND) {
                continue;
            }
            double ground = probe - height;
            double hover = jelly.minHover() + 1.0 + random.nextDouble() * (jelly.maxHover() - jelly.minHover() - 2.0);
            Vec3 target = new Vec3(x, ground + hover, z);
            if (overGroundAllTheWay(jelly, from, target)
                    && level.noCollision(
                            jelly, jelly.getDimensions(jelly.getPose()).makeBoundingBox(target))) {
                return target;
            }
        }
        return null;
    }

    /** Every point along the straight line (one sample per block) has ground within scan range below it. */
    private static boolean overGroundAllTheWay(Driftjelly jelly, Vec3 from, Vec3 to) {
        int steps = Math.max(1, (int) Math.ceil(from.distanceTo(to)));
        for (int step = 1; step <= steps; step++) {
            Vec3 point = from.lerp(to, (double) step / steps);
            if (Driftjelly.heightAboveGround(jelly.level(), point.x, point.y, point.z) == Driftjelly.NO_GROUND) {
                return false;
            }
        }
        return true;
    }
}
