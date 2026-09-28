package io.mcmaster.someonesperfectend.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import org.jetbrains.annotations.Nullable;

/**
 * A short, wide, friendly Ender folk. Unlike an Enderman it never teleports, never attacks, and doesn't care about
 * being looked at, but water and rain still hurt it, so it paths around water.
 *
 * <p>An endling may belong to a village (US-023): {@link #setVillage} restricts its wandering to the village and makes
 * it walk back when it strays. Like a Piglin zombifying outside the Nether, after {@link #LOST_DELAY} continuous ticks
 * outside the End an endling becomes {@linkplain #isLost() Lost} for good: it leaves its village, keeps its distance
 * from players, and never picks up items or barters (US-022), even back in the End.
 */
public class Endling extends PathfinderMob {
    /** Continuous ticks outside the End before an endling becomes Lost (15 seconds). */
    public static final int LOST_DELAY = 300;
    /** A Lost endling flees players that come within this many blocks. */
    public static final float LOST_FLEE_DISTANCE = 6.0F;
    /** How far a village endling may wander from its village point of interest. */
    public static final int VILLAGE_RADIUS = 16;

    private static final EntityDataAccessor<Boolean> DATA_LOST =
            SynchedEntityData.defineId(Endling.class, EntityDataSerializers.BOOLEAN);
    private static final String TAG_LOST = "Lost";
    private static final String TAG_TICKS_OUTSIDE_END = "TicksOutsideEnd";
    private static final String TAG_VILLAGE = "Village";
    private static final String TAG_VILLAGE_RADIUS = "VillageRadius";

    private int ticksOutsideEnd;
    @Nullable
    private BlockPos village;

    public Endling(EntityType<? extends Endling> type, Level level) {
        super(type, level);
        setPathfindingMalus(PathType.WATER, -1.0F);
        setPathfindingMalus(PathType.WATER_BORDER, 8.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_LOST, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.3));
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, LOST_FLEE_DISTANCE, 1.0, 1.3) {
            @Override
            public boolean canUse() {
                return isLost() && super.canUse();
            }
        });
        goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    /** Water and rain hurt endlings like Endermen (see {@code LivingEntity#aiStep}); splash water potions too. */
    @Override
    public boolean isSensitiveToWater() {
        return true;
    }

    public boolean isLost() {
        return entityData.get(DATA_LOST);
    }

    /** Makes this endling Lost; there is no way back. */
    public void setLost() {
        entityData.set(DATA_LOST, true);
        leaveVillage();
    }

    public int ticksOutsideEnd() {
        return ticksOutsideEnd;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            updateLostState(level().dimension() == Level.END);
        }
    }

    /** One tick of the End lock: in the End the timer resets; after {@link #LOST_DELAY} ticks elsewhere it's Lost. */
    public void updateLostState(boolean inEnd) {
        if (inEnd) {
            ticksOutsideEnd = 0;
        } else if (!isLost() && ++ticksOutsideEnd >= LOST_DELAY) {
            setLost();
        }
    }

    /** Whether this endling may take and barter items right now (not Lost, and in the End). */
    public boolean canBarter() {
        return !isLost() && level().dimension() == Level.END;
    }

    @Override
    public boolean canPickUpLoot() {
        return !isLost() && super.canPickUpLoot();
    }

    /** The village point of interest this endling belongs to, or null (always null once Lost). */
    @Nullable
    public BlockPos village() {
        return village;
    }

    /** Whether this endling counts toward its village's population. */
    public boolean countsTowardVillage() {
        return village != null && !isLost();
    }

    public void setVillage(BlockPos pos) {
        setVillage(pos, VILLAGE_RADIUS);
    }

    /** Binds this endling to a village point of interest; ignored for Lost endlings. */
    public void setVillage(BlockPos pos, int radius) {
        if (isLost()) {
            return;
        }
        village = pos.immutable();
        restrictTo(village, radius);
    }

    public void leaveVillage() {
        village = null;
        clearRestriction();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(TAG_LOST, isLost());
        tag.putInt(TAG_TICKS_OUTSIDE_END, ticksOutsideEnd);
        if (village != null) {
            tag.put(TAG_VILLAGE, NbtUtils.writeBlockPos(village));
            tag.putInt(TAG_VILLAGE_RADIUS, (int) getRestrictRadius());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ticksOutsideEnd = tag.getInt(TAG_TICKS_OUTSIDE_END);
        leaveVillage();
        if (tag.getBoolean(TAG_LOST)) {
            setLost();
        } else {
            NbtUtils.readBlockPos(tag, TAG_VILLAGE).ifPresent(pos -> setVillage(pos,
                    tag.contains(TAG_VILLAGE_RADIUS) ? tag.getInt(TAG_VILLAGE_RADIUS) : VILLAGE_RADIUS));
        }
    }

    /** Endlings live in their villages and never despawn, like villagers. */
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    /** A Lost endling drops nothing but what vanilla always drops (e.g. equipment); no loot table roll. */
    @Override
    protected boolean shouldDropLoot() {
        return !isLost() && super.shouldDropLoot();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENDERMAN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENDERMAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENDERMAN_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 1.5F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }
}
