package io.mcmaster.fwhipsperfectendupdate.entity;

import io.mcmaster.fwhipsperfectendupdate.block.ModBlockTags;
import io.mcmaster.fwhipsperfectendupdate.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A passive, floating jellyfish. It hovers {@link #MIN_HOVER}–{@link #MAX_HOVER} blocks above the ground, bobs
 * slowly, and wanders at most {@link #MAX_VOID_DRIFT} blocks out past the edge of the ground; if it ends up further
 * out over the void anyway (pushed, knocked back), it drifts back toward the last spot where it had ground below.
 *
 * <p>Players holding Voidfruit tempt it down to {@link #TEMPTED_MIN_HOVER}–{@link #TEMPTED_MAX_HOVER} blocks, and
 * feeding Voidfruit breeds it (vanilla {@link Animal} love, cooldowns, and baby growth). Like a Hoglin outside the
 * Nether, it only thrives in the End: after {@link #DORMANCY_DELAY} ticks elsewhere it turns dormant, sinks to the
 * ground, can't breed or be tempted, and drops nothing. Returning to the End wakes it.
 *
 * <p>A planted Glowcap contains driftjellies for farms: one that comes within {@link #CONTAINMENT_RADIUS} blocks of
 * it (on every axis) latches onto it and from then on stays in the 15x15x15 box centered on it. It doesn't head for
 * the Glowcap, it keeps floating freely inside the box; breaking the Glowcap or leashing the driftjelly frees it.
 */
public class Driftjelly extends Animal {
    public static final double MIN_HOVER = 2.0;
    public static final double MAX_HOVER = 6.0;
    /** How far below its feet a driftjelly looks for ground; with nothing solid within this range it is over the void. */
    public static final int GROUND_SCAN = 12;

    public static final double NO_GROUND = -1.0;
    /** How far (horizontally) past the last spot with ground below a driftjelly may drift out over the void. */
    public static final double MAX_VOID_DRIFT = 5.0;

    public static final double TEMPTED_MIN_HOVER = 1.0;
    public static final double TEMPTED_MAX_HOVER = 2.0;
    /** Ticks outside the End before a driftjelly turns dormant (15 seconds). */
    public static final int DORMANCY_DELAY = 300;
    /** A planted Glowcap keeps a driftjelly within this many blocks of it on each axis (a 15x15x15 box). */
    public static final int CONTAINMENT_RADIUS = 7;
    /** How far inside the containment box's faces a driftjelly's position is held. */
    public static final double CONTAINMENT_MARGIN = 0.5;
    /** A driftjelly moved (teleported, carried) further than this from its Glowcap forgets it. */
    private static final int CONTAINMENT_FORGET_DISTANCE = 16;
    /** Ticks between scans for a Glowcap to latch onto while uncontained. */
    private static final int CONTAINMENT_SCAN_INTERVAL = 20;

    private static final EntityDataAccessor<Boolean> DATA_DORMANT =
            SynchedEntityData.defineId(Driftjelly.class, EntityDataSerializers.BOOLEAN);
    private static final String TAG_DORMANT = "Dormant";
    private static final String TAG_TICKS_OUTSIDE_END = "TicksOutsideEnd";
    private static final String TAG_CONTAINMENT = "Containment";

    @Nullable
    private DriftjellyTemptGoal temptGoal;

    private int ticksOutsideEnd;

    /** Last position (feet) that had ground below it, where a driftjelly drifting over the void heads back to. */
    @Nullable
    private Vec3 lastGroundPos;

    /** The planted Glowcap this driftjelly is contained by, if any. */
    @Nullable
    private BlockPos containment;

    private int containmentScanCooldown;

    public Driftjelly(EntityType<? extends Driftjelly> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 10, true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0)
                // Air thrust is 0.02 x this per tick; much lower and it stays under LivingEntity's 0.003 per-axis
                // velocity dead zone, so the driftjelly barely moves.
                .add(Attributes.FLYING_SPEED, 0.3)
                .add(Attributes.MOVEMENT_SPEED, 0.1);
    }

    /** Natural spawns land on End ground (end turf, end stone, voidsoil); the driftjelly then rises to hover. */
    public static boolean checkDriftjellySpawnRules(
            EntityType<Driftjelly> type,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random) {
        return level.getBlockState(pos.below()).is(ModBlockTags.END_PLANT_SOIL);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_DORMANT, false);
    }

    @Override
    protected void registerGoals() {
        this.temptGoal = new DriftjellyTemptGoal(this, 1.2);
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(3, temptGoal);
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.1) {
            @Override
            public boolean canUse() {
                return !isDormant() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !isDormant() && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(5, new DriftjellyDriftGoal(this, 1.0));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    public boolean isTempted() {
        return temptGoal != null && temptGoal.isRunning();
    }

    public double minHover() {
        return isTempted() ? TEMPTED_MIN_HOVER : MIN_HOVER;
    }

    public double maxHover() {
        return isTempted() ? TEMPTED_MAX_HOVER : MAX_HOVER;
    }

    public boolean isDormant() {
        return entityData.get(DATA_DORMANT);
    }

    public void setDormant(boolean dormant) {
        entityData.set(DATA_DORMANT, dormant);
        if (dormant) {
            resetLove();
            getNavigation().stop();
        }
    }

    /** An awake driftjelly always floats; a dormant one sinks to the ground (no fall damage). */
    @Override
    public boolean isNoGravity() {
        return !isDormant();
    }

    public int ticksOutsideEnd() {
        return ticksOutsideEnd;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            updateDormancy(level().dimension() == Level.END);
        }
    }

    /** One tick of the End lock: wakes up in the End, turns dormant after {@link #DORMANCY_DELAY} ticks elsewhere. */
    public void updateDormancy(boolean inEnd) {
        if (inEnd) {
            ticksOutsideEnd = 0;
            if (isDormant()) {
                setDormant(false);
            }
        } else if (!isDormant() && ++ticksOutsideEnd >= DORMANCY_DELAY) {
            setDormant(true);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(TAG_DORMANT, isDormant());
        tag.putInt(TAG_TICKS_OUTSIDE_END, ticksOutsideEnd);
        if (containment != null) {
            tag.putLong(TAG_CONTAINMENT, containment.asLong());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setDormant(tag.getBoolean(TAG_DORMANT));
        ticksOutsideEnd = tag.getInt(TAG_TICKS_OUTSIDE_END);
        containment = tag.contains(TAG_CONTAINMENT) ? BlockPos.of(tag.getLong(TAG_CONTAINMENT)) : null;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (isDormant()) {
            return;
        }
        updateContainment();
        double height = heightAboveGround(level(), getX(), getY(), getZ());
        Vec3 motion = getDeltaMovement();
        double dy = Mth.sin(tickCount * 0.1F) * 0.003;
        if (height == NO_GROUND) {
            // Over the void: free to drift a little past the edge, keeping the current height. Too far out, it stops
            // wandering and drifts back toward ground.
            if (!withinVoidDrift(lastGroundPos, getX(), getZ())) {
                getNavigation().stop();
                if (lastGroundPos != null) {
                    Vec3 back = new Vec3(lastGroundPos.x - getX(), 0.0, lastGroundPos.z - getZ());
                    if (back.lengthSqr() > 1.0E-4) {
                        motion = motion.add(back.normalize().scale(0.02));
                    }
                }
            }
        } else {
            lastGroundPos = position();
            // Push back toward the middle of the band; the margin shrinks for the narrow tempted band.
            double margin = Math.min(0.5, (maxHover() - minHover()) / 4.0);
            if (height < minHover() + margin) {
                dy += 0.02;
            } else if (height > maxHover() - margin) {
                dy -= 0.02;
            }
        }
        motion = new Vec3(motion.x, Mth.clamp(motion.y + dy, -0.15, 0.15), motion.z);
        if (containment != null) {
            motion = new Vec3(
                    containAxis(getX(), containment.getX(), motion.x),
                    containAxis(getY(), containment.getY(), motion.y),
                    containAxis(getZ(), containment.getZ(), motion.z));
        }
        setDeltaMovement(motion);
    }

    /** Last position (feet) that had ground below it, or {@code null} if it hasn't been over ground since loading. */
    @Nullable
    public Vec3 lastGroundPos() {
        return lastGroundPos;
    }

    /**
     * Whether ({@code x}, {@code z}) is within {@link #MAX_VOID_DRIFT} blocks horizontally of {@code groundPos}, i.e.
     * close enough to ground for a driftjelly to drift there over the void. Always false without a ground position.
     */
    public static boolean withinVoidDrift(@Nullable Vec3 groundPos, double x, double z) {
        if (groundPos == null) {
            return false;
        }
        double dx = x - groundPos.x;
        double dz = z - groundPos.z;
        return dx * dx + dz * dz <= MAX_VOID_DRIFT * MAX_VOID_DRIFT;
    }

    /** The planted Glowcap containing this driftjelly, or {@code null} if it is free. */
    @Nullable
    public BlockPos containment() {
        return containment;
    }

    /**
     * Drops the Glowcap once it is gone, out of reach or the driftjelly is leashed; while free, looks for one to
     * latch onto every {@link #CONTAINMENT_SCAN_INTERVAL} ticks.
     */
    private void updateContainment() {
        if (isLeashed()) {
            containment = null;
            return;
        }
        if (containment != null && level().isLoaded(containment)) {
            BlockPos at = blockPosition();
            int distance = Math.max(
                    Math.abs(at.getX() - containment.getX()),
                    Math.max(Math.abs(at.getY() - containment.getY()), Math.abs(at.getZ() - containment.getZ())));
            if (!isContainer(level().getBlockState(containment)) || distance > CONTAINMENT_FORGET_DISTANCE) {
                containment = null;
            }
        }
        if (containment == null && --containmentScanCooldown <= 0) {
            containmentScanCooldown = CONTAINMENT_SCAN_INTERVAL;
            containment = findContainment(level(), blockPosition());
        }
    }

    public static boolean isContainer(BlockState state) {
        return state.is(ModBlocks.GLOWCAP.get());
    }

    /** The nearest planted Glowcap whose containment box holds {@code at}, or {@code null} if there is none. */
    @Nullable
    public static BlockPos findContainment(BlockGetter level, BlockPos at) {
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(
                at.offset(-CONTAINMENT_RADIUS, -CONTAINMENT_RADIUS, -CONTAINMENT_RADIUS),
                at.offset(CONTAINMENT_RADIUS, CONTAINMENT_RADIUS, CONTAINMENT_RADIUS))) {
            if (isContainer(level.getBlockState(pos))) {
                double distance = pos.distSqr(at);
                if (distance < nearestDistance) {
                    nearest = pos.immutable();
                    nearestDistance = distance;
                }
            }
        }
        return nearest;
    }

    /**
     * Whether {@code pos} lies in the box of blocks within {@link #CONTAINMENT_RADIUS} of {@code glowcap}, at least
     * {@code margin} inside its faces.
     */
    public static boolean insideContainment(BlockPos glowcap, Vec3 pos, double margin) {
        return insideAxis(pos.x, glowcap.getX(), margin)
                && insideAxis(pos.y, glowcap.getY(), margin)
                && insideAxis(pos.z, glowcap.getZ(), margin);
    }

    private static boolean insideAxis(double pos, int center, double margin) {
        return pos >= center - CONTAINMENT_RADIUS + margin && pos <= center + CONTAINMENT_RADIUS + 1 - margin;
    }

    /** One axis of the containment wall: past the margin, outward motion stops and the driftjelly is pushed back. */
    private static double containAxis(double pos, int center, double motion) {
        if (pos < center - CONTAINMENT_RADIUS + CONTAINMENT_MARGIN) {
            return Math.max(motion, 0.0) + 0.02;
        }
        if (pos > center + CONTAINMENT_RADIUS + 1 - CONTAINMENT_MARGIN) {
            return Math.min(motion, 0.0) - 0.02;
        }
        return motion;
    }

    /**
     * Height of {@code y} above the top of the first collidable block or fluid below it, or {@link #NO_GROUND} when
     * there is nothing within {@link #GROUND_SCAN} blocks (the void, as far as a driftjelly is concerned).
     */
    public static double heightAboveGround(BlockGetter level, double x, double y, double z) {
        BlockPos.MutableBlockPos pos = BlockPos.containing(x, y, z).mutable();
        for (int i = 0; i <= GROUND_SCAN; i++, pos.move(Direction.DOWN)) {
            if (pos.getY() < level.getMinBuildHeight()) {
                break;
            }
            BlockState state = level.getBlockState(pos);
            VoxelShape shape = state.getCollisionShape(level, pos);
            if (!shape.isEmpty()) {
                return Math.max(0.0, y - (pos.getY() + shape.max(Direction.Axis.Y)));
            }
            FluidState fluid = state.getFluidState();
            if (!fluid.isEmpty()) {
                return Math.max(0.0, y - (pos.getY() + fluid.getHeight(level, pos)));
            }
        }
        return NO_GROUND;
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        return level.getBlockState(pos.below()).is(ModBlockTags.END_PLANT_SOIL) ? 10.0F : 0.0F;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {}

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {}

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.GLOW_SQUID_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.GLOW_SQUID_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GLOW_SQUID_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 1.3F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ModBlocks.VOIDFRUIT.get());
    }

    @Override
    public boolean canFallInLove() {
        return !isDormant() && super.canFallInLove();
    }

    @Override
    public boolean canMate(Animal other) {
        return !isDormant() && other instanceof Driftjelly jelly && !jelly.isDormant() && super.canMate(other);
    }

    @Override
    protected boolean shouldDropLoot() {
        return !isDormant() && super.shouldDropLoot();
    }

    @Override
    public boolean shouldDropExperience() {
        return !isDormant() && super.shouldDropExperience();
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return Driftjellies.TYPE.get().create(level);
    }
}
