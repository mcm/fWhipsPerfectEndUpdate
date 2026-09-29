package io.mcmaster.someonesperfectend.block;

import io.mcmaster.someonesperfectend.SomeonesPerfectEnd;
import io.mcmaster.someonesperfectend.mixin.PlayerAccessor;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Ender Bed travel (US-038): using an Ender Bed while the other dimension's bed is stored lies the player down in
 * it, at any time and in any weather, with no monster check. After vanilla's fully-asleep time (100 ticks) the
 * player wakes and is moved to the other bed's drop point; leaving the bed earlier cancels the trip. Sleeping in an
 * Ender Bed never counts toward skipping the night (see {@code SleepStatusMixin}).
 */
public final class EnderBedTravel {
    public static final String MISSING_KEY = "message." + SomeonesPerfectEnd.MOD_ID + ".ender_bed_missing";
    /** Vanilla's fully-asleep time ({@link Player#isSleepingLongEnough()}). */
    public static final int SLEEP_TICKS = 100;

    private EnderBedTravel() {
    }

    /** The dimension an Ender Bed in {@code dimension} leads to: the End from the Overworld and back. */
    @Nullable
    public static ResourceKey<Level> destinationDimension(ResourceKey<Level> dimension) {
        if (dimension == Level.OVERWORLD) {
            return Level.END;
        }
        return dimension == Level.END ? Level.OVERWORLD : null;
    }

    /**
     * Called after the bed at {@code head} was stored: lies the player down in it if the other dimension's bed is
     * stored. Unlike {@code ServerPlayer#startSleepInBed}, this skips the time, monster, and spawn-point logic.
     */
    public static void tryStart(ServerPlayer player, BlockPos head) {
        ResourceKey<Level> destination = destinationDimension(player.level().dimension());
        if (destination == null || EnderBedLocations.of(player).get(destination).isEmpty() || player.isSleeping()) {
            return;
        }
        player.ejectPassengers();
        player.startSleeping(head);
        ((PlayerAccessor) player).someonesperfectend$setSleepCounter(0);
    }

    /** The head half of the Ender Bed the entity sleeps in, if any. */
    public static Optional<BlockPos> enderBed(LivingEntity entity) {
        if (!entity.isSleeping()) {
            return Optional.empty();
        }
        return entity.getSleepingPos()
                .filter(pos -> entity.level().getBlockState(pos).getBlock() instanceof EnderBedBlock);
    }

    public static boolean isInEnderBed(LivingEntity entity) {
        return enderBed(entity).isPresent();
    }

    /**
     * Runs at the end of every living entity tick on both sides. Keeps a player lying in a hanging bed from falling
     * out of it (nothing holds them up; see {@link #pin}), and on the server finishes the trip once they've slept
     * long enough.
     */
    public static void tick(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return;
        }
        Optional<BlockPos> bed = enderBed(player);
        if (bed.isEmpty()) {
            return;
        }
        pin(player, bed.get());
        if (player instanceof ServerPlayer serverPlayer && player.getSleepTimer() >= SLEEP_TICKS) {
            wakeAndTravel(serverPlayer);
        }
    }

    /** Holds the player still with the top of their sleeping hitbox against the underside of the bed at {@code head}. */
    public static void pin(Player player, BlockPos head) {
        player.setDeltaMovement(Vec3.ZERO);
        player.setPos(head.getX() + 0.5,
                EnderBedPose.sleepingY(head.getY(), player.getDimensions(Pose.SLEEPING).height()), head.getZ() + 0.5);
    }

    /**
     * Wakes the player and moves them to the other dimension's bed. If that bed is missing or its drop point is
     * obstructed, they stay in (under) the same bed, see {@link #MISSING_KEY}, and the stored destination is cleared.
     */
    public static void wakeAndTravel(ServerPlayer player) {
        ResourceKey<Level> dimension = destinationDimension(player.level().dimension());
        player.stopSleepInBed(false, true);
        if (dimension == null) {
            return;
        }
        EnderBedLocations beds = EnderBedLocations.of(player);
        Optional<BlockPos> head = beds.get(dimension);
        ServerLevel level = player.server.getLevel(dimension);
        if (head.isEmpty() || level == null || !EnderBedRespawn.isValidDestination(level, head.get())) {
            beds.set(dimension, null);
            player.displayClientMessage(Component.translatable(MISSING_KEY), true);
            return;
        }
        Vec3 arrival = EnderBedRules.arrivalPos(head.get());
        float yRot = level.getBlockState(head.get()).getValue(BedBlock.FACING).toYRot();
        // Only the player travels: riders are dropped, and pets and leashed mobs stay behind. A plain dimension
        // change never builds the End platform, starts the dragon fight, or shows the credits.
        player.ejectPassengers();
        player.teleportTo(level, arrival.x, arrival.y, arrival.z, yRot, 0.0F);
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
    }

    /**
     * Where a player waking in an Ender Bed without travelling (cancelled or missing destination) stands: the bed's
     * drop point if it's open, so they drop out of the bed; {@code null} for vanilla's stand-up position.
     */
    @Nullable
    public static Vec3 wakePosition(Level level, BlockPos head) {
        return EnderBedRespawn.isValidDestination(level, head) ? EnderBedRules.arrivalPos(head) : null;
    }
}
