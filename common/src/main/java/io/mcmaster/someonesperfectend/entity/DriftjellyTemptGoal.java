package io.mcmaster.someonesperfectend.entity;

import io.mcmaster.someonesperfectend.block.ModBlocks;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Follows a player holding Voidfruit. Vanilla {@link TemptGoal} paths to the player's feet; a driftjelly instead heads
 * for a point at its (lowered) tempted hover height above the ground under the player. Dormant driftjellies ignore it.
 * Tempting players are found with an entity query around the driftjelly rather than {@code Level#players()}, so any
 * player entity in range counts (including GameTest mock players).
 */
public class DriftjellyTemptGoal extends TemptGoal {
    private static final double STOP_DISTANCE_SQR = 2.5 * 2.5;
    private static final double RANGE = 10.0;
    private static final TargetingConditions TEMPT_TARGETING = TargetingConditions.forNonCombat().range(RANGE)
            .ignoreLineOfSight();

    private final Driftjelly jelly;
    private final double speed;

    public DriftjellyTemptGoal(Driftjelly jelly, double speed) {
        super(jelly, speed, DriftjellyTemptGoal::isTemptItem, false);
        this.jelly = jelly;
        this.speed = speed;
    }

    public static boolean isTemptItem(ItemStack stack) {
        return stack.is(ModBlocks.VOIDFRUIT.get());
    }

    /** Also used by {@code canContinueToUse} (vanilla calls {@code canUse} when the goal can't scare). */
    @Override
    public boolean canUse() {
        if (jelly.isDormant()) {
            return false;
        }
        player = jelly.level().getNearestEntity(
                jelly.level().getEntitiesOfClass(Player.class, jelly.getBoundingBox().inflate(RANGE),
                        p -> isTemptItem(p.getMainHandItem()) || isTemptItem(p.getOffhandItem())),
                TEMPT_TARGETING, jelly, jelly.getX(), jelly.getY(), jelly.getZ());
        return player != null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /**
     * Flies straight toward the player (the move control's wanted position is reset every tick), holding position
     * rather than following the player out over the void.
     */
    @Override
    public void tick() {
        jelly.getLookControl().setLookAt(player, jelly.getMaxHeadYRot() + 20, jelly.getMaxHeadXRot());
        jelly.getNavigation().stop();
        double dx = player.getX() - jelly.getX();
        double dz = player.getZ() - jelly.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance * distance < STOP_DISTANCE_SQR) {
            return;
        }
        double aheadX = jelly.getX() + dx / distance;
        double aheadZ = jelly.getZ() + dz / distance;
        if (Driftjelly.heightAboveGround(jelly.level(), aheadX, jelly.getY(), aheadZ) == Driftjelly.NO_GROUND) {
            return;
        }
        double probe = player.getY() + 1.0;
        double height = Driftjelly.heightAboveGround(jelly.level(), player.getX(), probe, player.getZ());
        double ground = height == Driftjelly.NO_GROUND ? player.getY() : probe - height;
        double hover = (Driftjelly.TEMPTED_MIN_HOVER + Driftjelly.TEMPTED_MAX_HOVER) / 2.0;
        jelly.getMoveControl().setWantedPosition(player.getX(), ground + hover, player.getZ(), speed);
    }
}
