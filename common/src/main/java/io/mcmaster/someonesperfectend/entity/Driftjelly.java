package io.mcmaster.someonesperfectend.entity;

import io.mcmaster.someonesperfectend.block.ModBlockTags;
import io.mcmaster.someonesperfectend.block.ModBlocks;
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
 * slowly, and only wanders to spots over solid ground; if it ends up over the void anyway (pushed, knocked back), it
 * drifts back to the last spot where it had ground below.
 *
 * <p>Players holding Voidfruit tempt it down to {@link #TEMPTED_MIN_HOVER}–{@link #TEMPTED_MAX_HOVER} blocks, and
 * feeding Voidfruit breeds it (vanilla {@link Animal} love, cooldowns, and baby growth). Like a Hoglin outside the
 * Nether, it only thrives in the End: after {@link #DORMANCY_DELAY} ticks elsewhere it turns dormant, sinks to the
 * ground, can't breed or be tempted, and drops nothing. Returning to the End wakes it.
 */
public class Driftjelly extends Animal {
    public static final double MIN_HOVER = 2.0;
    public static final double MAX_HOVER = 6.0;
    /** How far below its feet a driftjelly looks for ground; with nothing solid within this range it is over the void. */
    public static final int GROUND_SCAN = 12;
    public static final double NO_GROUND = -1.0;
    public static final double TEMPTED_MIN_HOVER = 1.0;
    public static final double TEMPTED_MAX_HOVER = 2.0;
    /** Ticks outside the End before a driftjelly turns dormant (15 seconds). */
    public static final int DORMANCY_DELAY = 300;

    private static final EntityDataAccessor<Boolean> DATA_DORMANT =
            SynchedEntityData.defineId(Driftjelly.class, EntityDataSerializers.BOOLEAN);
    private static final String TAG_DORMANT = "Dormant";
    private static final String TAG_TICKS_OUTSIDE_END = "TicksOutsideEnd";

    @Nullable
    private DriftjellyTemptGoal temptGoal;
    private int ticksOutsideEnd;

    /** Last position (feet) that had ground below it, where a driftjelly drifting over the void heads back to. */
    @Nullable
    private Vec3 lastGroundPos;

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
    public static boolean checkDriftjellySpawnRules(EntityType<Driftjelly> type, ServerLevelAccessor level,
            MobSpawnType spawnType, BlockPos pos, RandomSource random) {
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
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setDormant(tag.getBoolean(TAG_DORMANT));
        ticksOutsideEnd = tag.getInt(TAG_TICKS_OUTSIDE_END);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (isDormant()) {
            return;
        }
        double height = heightAboveGround(level(), getX(), getY(), getZ());
        Vec3 motion = getDeltaMovement();
        double dy = Mth.sin(tickCount * 0.1F) * 0.003;
        if (height == NO_GROUND) {
            // Over the void: stop wandering and drift back toward ground, keeping the current height.
            getNavigation().stop();
            if (lastGroundPos != null) {
                Vec3 back = new Vec3(lastGroundPos.x - getX(), 0.0, lastGroundPos.z - getZ());
                if (back.lengthSqr() > 1.0E-4) {
                    motion = motion.add(back.normalize().scale(0.02));
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
        setDeltaMovement(motion.x, Mth.clamp(motion.y + dy, -0.15, 0.15), motion.z);
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
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
    }

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
