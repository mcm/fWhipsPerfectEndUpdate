package io.mcmaster.someonesperfectend.block;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Per-player Ender Bed locations (US-037), saved with the player as {@code someonesperfectend:ender_beds}: the head
 * half of the last Ender Bed used in the End ({@code end}) and in the Overworld ({@code overworld}). Held by
 * {@link Player} through {@code PlayerMixin}, which also saves/loads it; {@code ServerPlayerMixin} copies it to the
 * new player entity on every respawn (death and End exit).
 */
public final class EnderBedLocations {
    public static final String TAG = "someonesperfectend:ender_beds";
    private static final String END = "end";
    private static final String OVERWORLD = "overworld";

    @Nullable
    private BlockPos end;
    @Nullable
    private BlockPos overworld;

    /** Implemented by {@link Player} (through a mixin) to hold the locations. */
    public interface Holder {
        EnderBedLocations someonesperfectend$enderBeds();
    }

    public static EnderBedLocations of(Player player) {
        return ((Holder) player).someonesperfectend$enderBeds();
    }

    public Optional<BlockPos> end() {
        return Optional.ofNullable(end);
    }

    public Optional<BlockPos> overworld() {
        return Optional.ofNullable(overworld);
    }

    /** The stored bed for an Ender Bed dimension (the End or the Overworld); empty for any other dimension. */
    public Optional<BlockPos> get(ResourceKey<Level> dimension) {
        if (dimension == Level.END) {
            return end();
        }
        return dimension == Level.OVERWORLD ? overworld() : Optional.empty();
    }

    /** Stores (or with {@code null}, clears) the bed for the End or the Overworld; other dimensions are ignored. */
    public void set(ResourceKey<Level> dimension, @Nullable BlockPos head) {
        BlockPos pos = head == null ? null : head.immutable();
        if (dimension == Level.END) {
            end = pos;
        } else if (dimension == Level.OVERWORLD) {
            overworld = pos;
        }
    }

    public void copyFrom(EnderBedLocations other) {
        end = other.end;
        overworld = other.overworld;
    }

    public void save(CompoundTag playerTag) {
        CompoundTag tag = new CompoundTag();
        if (end != null) {
            tag.put(END, NbtUtils.writeBlockPos(end));
        }
        if (overworld != null) {
            tag.put(OVERWORLD, NbtUtils.writeBlockPos(overworld));
        }
        playerTag.put(TAG, tag);
    }

    public void load(CompoundTag playerTag) {
        CompoundTag tag = playerTag.getCompound(TAG);
        end = NbtUtils.readBlockPos(tag, END).orElse(null);
        overworld = NbtUtils.readBlockPos(tag, OVERWORLD).orElse(null);
    }
}
