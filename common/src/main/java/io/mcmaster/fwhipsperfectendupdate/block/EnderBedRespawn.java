package io.mcmaster.fwhipsperfectendupdate.block;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * End respawn at an Ender Bed (US-037). Hooked at the head of {@code ServerPlayer#findRespawnPositionAndUseSpawnBlock}
 * (see {@code ServerPlayerMixin}), which {@code PlayerList#respawn} calls with the dead player; the vanilla respawn
 * point itself is never read or changed.
 */
public final class EnderBedRespawn {
    private EnderBedRespawn() {}

    /** Whether {@code head} holds the head half of an Ender Bed whose drop point and the block below are open. */
    public static boolean isValidDestination(BlockGetter level, BlockPos head) {
        BlockState state = level.getBlockState(head);
        if (!(state.getBlock() instanceof EnderBedBlock) || state.getValue(BedBlock.PART) != BedPart.HEAD) {
            return false;
        }
        for (BlockPos pos : EnderBedRules.arrivalSpace(head)) {
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * The respawn for a player who died in the End with an {@code end} bed stored, or {@code null} for vanilla
     * respawn. A missing or obstructed bed clears {@code end}, falls back to vanilla, and shows vanilla's
     * {@code block.minecraft.spawn.not_valid} message (unless vanilla already reports a missing respawn block).
     * The fallback never sets {@link DimensionTransition#missingRespawnBlock()}: {@code PlayerList#respawn} would
     * then drop the player's vanilla respawn point.
     */
    @Nullable
    public static DimensionTransition findRespawn(
            ServerPlayer player, boolean keepInventory, DimensionTransition.PostDimensionTransition post) {
        if (!player.isDeadOrDying() || player.level().dimension() != Level.END) {
            // Not a death in the End: the End-exit respawn and End portal exits stay vanilla.
            return null;
        }
        EnderBedLocations beds = EnderBedLocations.of(player);
        Optional<BlockPos> head = beds.end();
        if (head.isEmpty()) {
            return null;
        }
        ServerLevel end = player.server.getLevel(Level.END);
        if (end != null && isValidDestination(end, head.get())) {
            float yRot = end.getBlockState(head.get()).getValue(BedBlock.FACING).toYRot();
            return new DimensionTransition(end, EnderBedRules.arrivalPos(head.get()), Vec3.ZERO, yRot, 0.0F, post);
        }
        beds.set(Level.END, null);
        // With `end` cleared, this re-enters the hook, which now returns null: the plain vanilla lookup.
        DimensionTransition vanilla = player.findRespawnPositionAndUseSpawnBlock(keepInventory, post);
        if (!vanilla.missingRespawnBlock() && player.connection != null) {
            player.connection.send(
                    new ClientboundGameEventPacket(ClientboundGameEventPacket.NO_RESPAWN_BLOCK_AVAILABLE, 0.0F));
        }
        return vanilla;
    }
}
