package io.mcmaster.fwhipsperfectendupdate.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

/**
 * Vanilla 1.21.1 chorus fruit's random teleport, shared by everything in the mod that teleports like chorus fruit
 * (the {@link DisplacementEffect}).
 */
public final class ChorusTeleport {
    /** Like chorus fruit: 16 tries, up to 8 blocks away on each axis. */
    public static final int TRIES = 16;

    public static final double RANGE = 8.0;

    private ChorusTeleport() {}

    /**
     * Tries {@value #TRIES} random spots within {@value #RANGE} blocks horizontally and vertically (clamped to the
     * dimension's logical height) and moves {@code entity} to the first safe one, with the chorus fruit sound, the
     * {@code TELEPORT} game event and a fall distance reset.
     *
     * @return whether the entity teleported
     */
    public static boolean teleport(ServerLevel level, LivingEntity entity) {
        RandomSource random = entity.getRandom();
        for (int i = 0; i < TRIES; i++) {
            double x = entity.getX() + (random.nextDouble() - 0.5) * RANGE * 2;
            double y = Mth.clamp(
                    entity.getY() + (random.nextInt((int) RANGE * 2) - RANGE),
                    level.getMinBuildHeight(),
                    level.getMinBuildHeight() + level.getLogicalHeight() - 1);
            double z = entity.getZ() + (random.nextDouble() - 0.5) * RANGE * 2;
            if (entity.isPassenger()) {
                entity.stopRiding();
            }
            Vec3 from = entity.position();
            if (entity.randomTeleport(x, y, z, true)) {
                level.gameEvent(GameEvent.TELEPORT, from, GameEvent.Context.of(entity));
                level.playSound(
                        null,
                        entity.getX(),
                        entity.getY(),
                        entity.getZ(),
                        SoundEvents.CHORUS_FRUIT_TELEPORT,
                        SoundSource.PLAYERS);
                entity.resetFallDistance();
                return true;
            }
        }
        return false;
    }
}
