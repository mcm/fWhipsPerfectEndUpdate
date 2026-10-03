package io.mcmaster.someonesperfectend.entity;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A short, wide, friendly Ender folk. Unlike an Enderman it never teleports, never attacks, and doesn't care about
 * being looked at, but water and rain still hurt it, so it paths around water.
 *
 * <p>An endling may belong to a village (US-023): {@link #setVillage} restricts its wandering to the village and makes
 * it walk back when it strays. Like a Piglin zombifying outside the Nether, after {@link #LOST_DELAY} continuous ticks
 * outside the End an endling becomes {@linkplain #isLost() Lost} for good: it leaves its village, keeps its distance
 * from players, and never picks up items or barters (US-022), even back in the End.
 *
 * <p>Bartering (US-022): a non-Lost endling in the End walks to and picks up one {@linkplain EndlingBarters barter
 * input} (dropped, or handed over with use-item), holds it in its main hand and inspects it for
 * {@link #INSPECT_TICKS}, then throws a roll from the input's barter loot table toward the nearest player. It takes
 * nothing else; its vanilla {@link #canPickUpLoot()} stays off.
 *
 * <p>Barters are locked until the offering player (the one handing the item over, or the thrower of a dropped item)
 * has beaten the Ender Dragon (US-039, {@link EndlingBarters#mayBarter}). Otherwise the endling leaves the item where it
 * is, shakes its head, and says no, at most once per {@link #REFUSE_COOLDOWN}. Each dropped item is refused once; it
 * isn't walked to again, but is still taken if its thrower unlocks bartering later.
 */
public class Endling extends PathfinderMob {
    /** Continuous ticks outside the End before an endling becomes Lost (15 seconds). */
    public static final int LOST_DELAY = 300;
    /** A Lost endling flees players that come within this many blocks. */
    public static final float LOST_FLEE_DISTANCE = 6.0F;
    /** How far a village endling may wander from its village point of interest. */
    public static final int VILLAGE_RADIUS = 16;
    /** How long an endling inspects a barter item before paying out (6 seconds). */
    public static final int INSPECT_TICKS = 120;
    /** How far an endling looks for dropped barter items to walk to. */
    public static final double BARTER_SEARCH_RANGE = 8.0;
    /** Minimum ticks between two refusals (head shake + "no"), so a pile of items doesn't spam the sound. */
    public static final int REFUSE_COOLDOWN = 40;
    /** How long the refusal head shake lasts. */
    public static final int HEAD_SHAKE_TICKS = 20;
    /** Entity event telling clients to play the head shake (unused by vanilla living entities and mobs). */
    private static final byte EVENT_SHAKE_HEAD = 64;

    private static final EntityDataAccessor<Boolean> DATA_LOST =
            SynchedEntityData.defineId(Endling.class, EntityDataSerializers.BOOLEAN);
    private static final String TAG_LOST = "Lost";
    private static final String TAG_TICKS_OUTSIDE_END = "TicksOutsideEnd";
    private static final String TAG_VILLAGE = "Village";
    private static final String TAG_VILLAGE_RADIUS = "VillageRadius";
    private static final String TAG_INSPECT_TICKS = "InspectTicks";
    private static final float DEFAULT_HAND_DROP_CHANCE = 0.085F;

    private int ticksOutsideEnd;
    private int inspectTicks;
    private int refuseCooldown;
    private int headShakeTicks;
    private int refusals;
    /** Ids of dropped items this endling has already refused, so it doesn't walk back to them or refuse them again. */
    private final Set<Integer> refusedItems = new HashSet<>();

    @Nullable
    private BlockPos village;

    public Endling(EntityType<? extends Endling> type, Level level) {
        super(type, level);
        setPathfindingMalus(PathType.WATER, -1.0F);
        setPathfindingMalus(PathType.WATER_BORDER, 8.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.25);
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
        goalSelector.addGoal(3, new InspectGoal());
        goalSelector.addGoal(4, new SeekBarterItemGoal());
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
        if (headShakeTicks > 0) {
            headShakeTicks--;
        }
        if (!level().isClientSide) {
            updateLostState(level().dimension() == Level.END);
            if (isInspecting() && --inspectTicks <= 0) {
                finishInspecting();
            }
            if (refuseCooldown > 0) {
                refuseCooldown--;
            }
            if (!refusedItems.isEmpty() && tickCount % 20 == 0) {
                double forget = BARTER_SEARCH_RANGE * 2.0;
                refusedItems.removeIf(id -> !(level().getEntity(id) instanceof ItemEntity item)
                        || item.isRemoved()
                        || distanceToSqr(item) > forget * forget);
            }
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && isAlive() && wantsToBarter()) {
            // Same reach as vanilla mob item pickup.
            for (ItemEntity item : level().getEntitiesOfClass(
                            ItemEntity.class, getBoundingBox().inflate(1.0, 0.0, 1.0))) {
                if (!item.isRemoved() && !item.hasPickUpDelay() && EndlingBarters.isInput(item.getItem())) {
                    if (!EndlingBarters.mayBarter(item.getOwner())) {
                        if (refusedItems.add(item.getId())) {
                            refuse();
                        }
                        continue;
                    }
                    onItemPickup(item);
                    take(item, 1);
                    ItemStack one = item.getItem().split(1);
                    if (item.getItem().isEmpty()) {
                        item.discard();
                    }
                    startInspecting(one);
                    break;
                }
            }
        }
    }

    /** Whether this endling would take a barter item right now: it can barter, is an adult, and isn't busy. */
    public boolean wantsToBarter() {
        return canBarter() && !isBaby() && !isInspecting();
    }

    /** Whether this endling is holding a barter item and inspecting it (synced: it's the main-hand item). */
    public boolean isInspecting() {
        return !getMainHandItem().isEmpty();
    }

    public int inspectTicks() {
        return inspectTicks;
    }

    /** Remaining ticks of the refusal head shake (on both sides; the client learns of it by entity event). */
    public int headShakeTicks() {
        return headShakeTicks;
    }

    /** How many times this endling has refused a barter (head shake + "no"), for tests. */
    public int refusals() {
        return refusals;
    }

    /** Refuses a barter from a player who hasn't beaten the dragon, unless it refused one less than 2 seconds ago. */
    private void refuse() {
        if (refuseCooldown > 0) {
            return;
        }
        refuseCooldown = REFUSE_COOLDOWN;
        headShakeTicks = HEAD_SHAKE_TICKS;
        refusals++;
        level().broadcastEntityEvent(this, EVENT_SHAKE_HEAD);
        playSound(SoundEvents.VILLAGER_NO);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_SHAKE_HEAD) {
            headShakeTicks = HEAD_SHAKE_TICKS;
        } else {
            super.handleEntityEvent(id);
        }
    }

    private void startInspecting(ItemStack item) {
        setItemSlot(EquipmentSlot.MAINHAND, item);
        // Killed mid-inspection, it gives the item back.
        setGuaranteedDrop(EquipmentSlot.MAINHAND);
        inspectTicks = INSPECT_TICKS;
        getNavigation().stop();
    }

    private void finishInspecting() {
        ItemStack input = getMainHandItem();
        setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        setDropChance(EquipmentSlot.MAINHAND, DEFAULT_HAND_DROP_CHANCE);
        inspectTicks = 0;
        ResourceKey<LootTable> table = EndlingBarters.lootTableFor(input);
        // Lost or out of the End since it took the item: hand it back instead.
        List<ItemStack> output = canBarter() && table != null ? EndlingBarters.roll(this, table) : List.of(input);
        Player player = nearestPlayer(16.0);
        Vec3 target = player != null
                ? player.position()
                : position().add(getLookAngle().scale(2.0));
        for (ItemStack stack : output) {
            BehaviorUtils.throwItem(this, stack, target.add(0.0, 1.0, 0.0));
        }
    }

    /** Uses entity lookups (not {@code level.players()}) so GameTest mock players count. */
    @Nullable
    private Player nearestPlayer(double range) {
        List<Player> players = level().getEntitiesOfClass(
                        Player.class, getBoundingBox().inflate(range), p -> !p.isSpectator() && p.isAlive());
        return level().getNearestEntity(
                        players, TargetingConditions.forNonCombat().ignoreLineOfSight(), this, getX(), getY(), getZ());
    }

    /**
     * Handing over a barter item with use-item gives it one (a creative player keeps theirs), if the player has beaten
     * the dragon; otherwise the endling refuses and the player keeps the item. Clients can't see advancements, so the
     * server decides.
     */
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (EndlingBarters.isInput(stack) && wantsToBarter()) {
            if (level().isClientSide) {
                return InteractionResult.SUCCESS;
            }
            if (!EndlingBarters.mayBarter(player)) {
                refuse();
                return InteractionResult.FAIL;
            }
            startInspecting(stack.consumeAndReturn(1, player));
            return InteractionResult.CONSUME;
        }
        return super.mobInteract(player, hand);
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
        tag.putInt(TAG_INSPECT_TICKS, inspectTicks);
        if (village != null) {
            tag.put(TAG_VILLAGE, NbtUtils.writeBlockPos(village));
            tag.putInt(TAG_VILLAGE_RADIUS, (int) getRestrictRadius());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ticksOutsideEnd = tag.getInt(TAG_TICKS_OUTSIDE_END);
        inspectTicks = tag.getInt(TAG_INSPECT_TICKS);
        leaveVillage();
        if (tag.getBoolean(TAG_LOST)) {
            setLost();
        } else {
            NbtUtils.readBlockPos(tag, TAG_VILLAGE)
                    .ifPresent(pos -> setVillage(
                            pos, tag.contains(TAG_VILLAGE_RADIUS) ? tag.getInt(TAG_VILLAGE_RADIUS) : VILLAGE_RADIUS));
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

    /** Stands still and looks down at the held barter item while inspecting it. */
    private final class InspectGoal extends Goal {
        InspectGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return isInspecting();
        }

        @Override
        public void start() {
            getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Vec3 ahead =
                    position().add(Vec3.directionFromRotation(0.0F, yBodyRot).scale(0.6));
            getLookControl().setLookAt(ahead.x, getY() + 0.3, ahead.z);
        }
    }

    /** Walks to the nearest dropped barter item it wants. */
    private final class SeekBarterItemGoal extends Goal {
        @Nullable
        private ItemEntity target;

        SeekBarterItemGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!wantsToBarter() || getRandom().nextInt(reducedTickDelay(5)) != 0) {
                return false;
            }
            List<ItemEntity> items = level().getEntitiesOfClass(
                            ItemEntity.class,
                            getBoundingBox().inflate(BARTER_SEARCH_RANGE, 4.0, BARTER_SEARCH_RANGE),
                            item -> EndlingBarters.isInput(item.getItem())
                                    && (!refusedItems.contains(item.getId())
                                            || EndlingBarters.mayBarter(item.getOwner())));
            target = null;
            double best = Double.MAX_VALUE;
            for (ItemEntity item : items) {
                double distance = distanceToSqr(item);
                if (distance < best) {
                    best = distance;
                    target = item;
                }
            }
            return target != null;
        }

        @Override
        public boolean canContinueToUse() {
            return target != null
                    && target.isAlive()
                    && wantsToBarter()
                    && !getNavigation().isDone();
        }

        @Override
        public void start() {
            getNavigation().moveTo(target, 1.0);
        }

        @Override
        public void stop() {
            target = null;
        }
    }
}
