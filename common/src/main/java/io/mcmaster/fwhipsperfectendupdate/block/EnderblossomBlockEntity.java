package io.mcmaster.fwhipsperfectendupdate.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A wall Enderblossom's shake state. Like an Enderman, the blossom keeps trembling for as long as a player stares at
 * it ({@link #staredAt()}, checked every tick on the server and sent to clients as a block event), and plays a one-off
 * {@link ShakeCurve#angle shake} when an Enderman teleports nearby. Nothing is saved.
 */
public class EnderblossomBlockEntity extends BlockEntity {
    /** The block event id that starts a shake; see {@link EnderblossomShaking#shake}. */
    public static final int SHAKE_EVENT_ID = 1;
    /** The block event id that starts (param 1) or stops (param 0) the stare tremble. */
    public static final int STARE_EVENT_ID = 2;

    private static final String STARED_AT_TAG = "stared_at";
    private static final long NOT_STARTED = Long.MIN_VALUE;

    private long shakeStart = NOT_STARTED;
    private int shakes;
    private boolean staredAt;
    private long stareStart = NOT_STARTED;
    private int stares;

    public EnderblossomBlockEntity(BlockPos pos, BlockState state) {
        super(EnderblossomShaking.BLOCK_ENTITY.get(), pos, state);
    }

    /** The server ticker: looks for staring players every tick, like an Enderman does. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, EnderblossomBlockEntity entity) {
        if (level instanceof ServerLevel serverLevel) {
            EnderblossomShaking.checkLookers(serverLevel, pos, state, serverLevel.players());
        }
    }

    /** How many shakes this blossom has received; on the server this counts the block events it ran. */
    public int shakes() {
        return shakes;
    }

    /** Whether a player is staring at the blossom, so it trembles. */
    public boolean staredAt() {
        return staredAt;
    }

    /** How many times a stare has started. */
    public int stares() {
        return stares;
    }

    /** Starts or stops the stare tremble; returns whether that changed anything. */
    boolean setStaredAt(boolean staredAt) {
        if (this.staredAt == staredAt) {
            return false;
        }
        this.staredAt = staredAt;
        if (staredAt) {
            stareStart = level != null ? level.getGameTime() : NOT_STARTED;
            stares++;
        }
        return true;
    }

    @Override
    public boolean triggerEvent(int id, int param) {
        if (id == STARE_EVENT_ID) {
            setStaredAt(param != 0);
            return true;
        }
        if (id != SHAKE_EVENT_ID) {
            return super.triggerEvent(id, param);
        }
        if (level != null) {
            shakeStart = level.getGameTime();
        }
        shakes++;
        // True sends the event on to clients.
        return true;
    }

    /** Lets players who load the chunk mid-stare see the tremble; never written to disk. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        if (staredAt) {
            tag.putBoolean(STARED_AT_TAG, true);
        }
        return tag;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        setStaredAt(tag.getBoolean(STARED_AT_TAG));
    }

    /** The current angle in degrees: the stare tremble plus any teleport shake; 0 when still. */
    public float shakeAngle(float partialTick) {
        if (level == null) {
            return 0.0F;
        }
        long now = level.getGameTime();
        float angle = 0.0F;
        if (shakeStart != NOT_STARTED) {
            angle += ShakeCurve.angle(now - shakeStart, partialTick);
        }
        if (staredAt) {
            if (stareStart == NOT_STARTED) {
                stareStart = now;
            }
            angle += ShakeCurve.stareAngle(now - stareStart, partialTick);
        }
        return angle;
    }
}
