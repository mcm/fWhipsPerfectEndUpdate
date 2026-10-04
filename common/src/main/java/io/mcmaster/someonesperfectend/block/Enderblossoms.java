package io.mcmaster.someonesperfectend.block;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import io.mcmaster.someonesperfectend.sound.ModSounds;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Opening and closing of Enderblossoms, wall ({@link EnderblossomBlock}) and potted
 * ({@link PottedEnderblossomBlock}) alike.
 *
 * <p>Each blossom checks every {@link EnderblossomTiming#CHECK_INTERVAL} ticks with its own scheduled block tick
 * and is OPEN while an Enderman is within {@link #RANGE} blocks. A switch ripples to same-type blossoms nearby, like
 * vanilla 1.21.4's Eyeblossom. Ripple checks can't be scheduled block ticks: the level ignores a second tick for a
 * block that already has one pending, and every blossom always has its regular check pending. So they wait in a
 * per-level queue that is drained each server level tick and dropped on server stop.
 */
public final class Enderblossoms {
    public static final double RANGE = 8.0;
    /** Ripples reach this far horizontally and {@link #RIPPLE_VERTICAL} vertically, like vanilla. */
    public static final int RIPPLE_HORIZONTAL = 3;

    public static final int RIPPLE_VERTICAL = 2;
    private static final int PARTICLES = 6;

    private static final Map<ResourceKey<Level>, PriorityQueue<Ripple>> RIPPLES = new HashMap<>();

    private Enderblossoms() {}

    public static void init() {
        TickEvent.SERVER_LEVEL_POST.register(Enderblossoms::runDueRipples);
        LifecycleEvent.SERVER_STOPPING.register(server -> RIPPLES.clear());
    }

    /** Implemented by both blossom blocks. */
    public interface Blossom {
        EnderblossomBlock.Type type();

        /** The same blossom (keeping its facing) or pot as the other type. */
        BlockState withType(BlockState state, EnderblossomBlock.Type type);

        /** Where the flower head is, for particles. */
        Vec3 headPosition(BlockState state, BlockPos pos);
    }

    /** The blossom type of {@code state}, or null when it is no Enderblossom. */
    public static EnderblossomBlock.Type typeOf(BlockState state) {
        return state.getBlock() instanceof Blossom blossom ? blossom.type() : null;
    }

    /** Schedules the first regular check; called from {@code onPlace}. */
    public static void scheduleFirstCheck(Level level, BlockPos pos, Block block) {
        if (!level.isClientSide) {
            level.scheduleTick(pos, block, nextCheckDelay(level, pos));
        }
    }

    private static int nextCheckDelay(Level level, BlockPos pos) {
        return EnderblossomTiming.nextCheckDelay(level.getGameTime(), EnderblossomTiming.phase(pos.hashCode()));
    }

    /** True while an Enderman (and only an Enderman) is within {@link #RANGE} blocks of the block center. */
    public static boolean endermanNearby(Level level, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        return !level.getEntities(
                        EntityType.ENDERMAN,
                        new AABB(pos).inflate(RANGE),
                        enderman -> enderman.distanceToSqr(center) <= RANGE * RANGE)
                .isEmpty();
    }

    /** The regular check, run from the blossom's scheduled tick; reschedules the next one unless it switched. */
    public static void regularCheck(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!check(state, level, pos, random, false)) {
            level.scheduleTick(pos, state.getBlock(), nextCheckDelay(level, pos));
        }
        // After a switch the new block's onPlace schedules its own check.
    }

    /**
     * Switches the blossom at {@code pos} when the Enderman test disagrees with its type. Returns whether it switched.
     *
     * @param ripple whether a nearby switch set this check off (short sound) rather than the blossom's own check
     */
    public static boolean check(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random, boolean ripple) {
        if (!(state.getBlock() instanceof Blossom blossom)) {
            return false;
        }
        EnderblossomBlock.Type type = blossom.type();
        EnderblossomBlock.Type target =
                endermanNearby(level, pos) ? EnderblossomBlock.Type.OPEN : EnderblossomBlock.Type.CLOSED;
        if (target == type) {
            return false;
        }
        level.setBlock(pos, blossom.withType(state, target), Block.UPDATE_ALL);
        level.playSound(null, pos, switchSound(target, ripple), SoundSource.BLOCKS, 1.0F, 1.0F);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(state));
        Vec3 head = blossom.headPosition(state, pos);
        level.sendParticles(
                target.open() ? ParticleTypes.PORTAL : ParticleTypes.REVERSE_PORTAL,
                head.x,
                head.y,
                head.z,
                PARTICLES,
                0.15,
                0.15,
                0.15,
                target.open() ? 0.4 : 0.02);
        scheduleRipples(level, pos, type, random);
        return true;
    }

    static SoundEvent switchSound(EnderblossomBlock.Type target, boolean ripple) {
        if (target.open()) {
            return (ripple ? ModSounds.ENDERBLOSSOM_OPEN : ModSounds.ENDERBLOSSOM_OPEN_LONG).get();
        }
        return (ripple ? ModSounds.ENDERBLOSSOM_CLOSE : ModSounds.ENDERBLOSSOM_CLOSE_LONG).get();
    }

    /** Queues checks on blossoms of the switched blossom's old {@code type} within ±3/±2 blocks. */
    private static void scheduleRipples(
            ServerLevel level, BlockPos origin, EnderblossomBlock.Type type, RandomSource random) {
        long now = level.getGameTime();
        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-RIPPLE_HORIZONTAL, -RIPPLE_VERTICAL, -RIPPLE_HORIZONTAL),
                origin.offset(RIPPLE_HORIZONTAL, RIPPLE_VERTICAL, RIPPLE_HORIZONTAL))) {
            if (typeOf(level.getBlockState(pos)) != type) {
                continue;
            }
            double distance = Math.sqrt(origin.distSqr(pos));
            int delay = random.nextIntBetweenInclusive(
                    EnderblossomTiming.minRippleDelay(distance), EnderblossomTiming.maxRippleDelay(distance));
            RIPPLES.computeIfAbsent(level.dimension(), key -> new PriorityQueue<>())
                    .add(new Ripple(pos.immutable(), type, now + delay));
        }
    }

    /** Whether a ripple check is queued for {@code pos} in {@code level}. */
    public static boolean ripplePending(ServerLevel level, BlockPos pos) {
        PriorityQueue<Ripple> queue = RIPPLES.get(level.dimension());
        return queue != null && queue.stream().anyMatch(ripple -> ripple.pos().equals(pos));
    }

    private static void runDueRipples(ServerLevel level) {
        PriorityQueue<Ripple> queue = RIPPLES.get(level.dimension());
        if (queue == null || queue.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        List<Ripple> due = new ArrayList<>();
        while (!queue.isEmpty() && queue.peek().due() <= now) {
            due.add(queue.poll());
        }
        for (Ripple ripple : due) {
            if (!level.isLoaded(ripple.pos())) {
                continue;
            }
            BlockState state = level.getBlockState(ripple.pos());
            // Like vanilla, the ripple only checks a blossom that is still the type it was queued for.
            if (typeOf(state) == ripple.type()) {
                check(state, level, ripple.pos(), level.getRandom(), true);
            }
        }
    }

    /** The flower head of a wall blossom: leaning out from the log and up. */
    static Vec3 wallHead(Direction facing, BlockPos pos) {
        return Vec3.atCenterOf(pos).add(facing.getStepX() * 0.2, 0.25, facing.getStepZ() * 0.2);
    }

    private record Ripple(BlockPos pos, EnderblossomBlock.Type type, long due) implements Comparable<Ripple> {
        @Override
        public int compareTo(Ripple other) {
            return Long.compare(due, other.due);
        }
    }
}
