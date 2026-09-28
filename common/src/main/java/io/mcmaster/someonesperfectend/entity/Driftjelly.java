package io.mcmaster.someonesperfectend.entity;

import io.mcmaster.someonesperfectend.block.ModBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
 */
public class Driftjelly extends Animal {
    public static final double MIN_HOVER = 2.0;
    public static final double MAX_HOVER = 6.0;
    /** How far below its feet a driftjelly looks for ground; with nothing solid within this range it is over the void. */
    public static final int GROUND_SCAN = 12;
    public static final double NO_GROUND = -1.0;

    /** Last position (feet) that had ground below it, where a driftjelly drifting over the void heads back to. */
    @Nullable
    private Vec3 lastGroundPos;

    public Driftjelly(EntityType<? extends Driftjelly> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 10, true);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0)
                .add(Attributes.FLYING_SPEED, 0.1)
                .add(Attributes.MOVEMENT_SPEED, 0.1);
    }

    /** Natural spawns land on End ground (end turf, end stone, voidsoil); the driftjelly then rises to hover. */
    public static boolean checkDriftjellySpawnRules(EntityType<Driftjelly> type, ServerLevelAccessor level,
            MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        return level.getBlockState(pos.below()).is(ModBlockTags.END_PLANT_SOIL);
    }

    @Override
    protected void registerGoals() {
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

    public double minHover() {
        return MIN_HOVER;
    }

    public double maxHover() {
        return MAX_HOVER;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
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
            if (height < minHover() + 0.5) {
                dy += 0.02;
            } else if (height > maxHover() - 0.5) {
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

    /** Breeding with Voidfruit is added in US-020. */
    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return Driftjellies.TYPE.get().create(level);
    }
}
