package io.mcmaster.someonesperfectend.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A wall Enderblossom's shake state. The server tracks who is looking ({@link LookShakeTracker}) and counts shakes;
 * the client records when the last shake started so the renderer can play {@link ShakeCurve}. Nothing is saved.
 */
public class EnderblossomBlockEntity extends BlockEntity {
    /** The block event id that starts a shake; see {@link EnderblossomShaking#shake}. */
    public static final int SHAKE_EVENT_ID = 1;

    private final LookShakeTracker lookers = new LookShakeTracker();
    private long shakeStart = Long.MIN_VALUE;
    private int shakes;

    public EnderblossomBlockEntity(BlockPos pos, BlockState state) {
        super(EnderblossomShaking.BLOCK_ENTITY.get(), pos, state);
    }

    public LookShakeTracker lookers() {
        return lookers;
    }

    /** How many shakes this blossom has received; on the server this counts the block events it ran. */
    public int shakes() {
        return shakes;
    }

    @Override
    public boolean triggerEvent(int id, int param) {
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

    /** The current {@link ShakeCurve} angle in degrees; 0 when not shaking. */
    public float shakeAngle(float partialTick) {
        if (level == null || shakeStart == Long.MIN_VALUE) {
            return 0.0F;
        }
        return ShakeCurve.angle(level.getGameTime() - shakeStart, partialTick);
    }
}
