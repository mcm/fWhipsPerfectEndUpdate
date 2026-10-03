package io.mcmaster.someonesperfectend.infuser;

import io.mcmaster.someonesperfectend.SomeonesPerfectEnd;
import io.mcmaster.someonesperfectend.block.ModBlocks;
import io.mcmaster.someonesperfectend.mixin.BeaconBlockEntityAccessor;
import io.mcmaster.someonesperfectend.mixin.BlockEntityAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A full vanilla beacon (pyramid, beam, powers, payment, range, and cadence all come from
 * {@link BeaconBlockEntity#tick}) that opens an {@link InfuserMenu} instead of a beacon menu.
 */
public class InfuserBlockEntity extends BeaconBlockEntity {
    public static final Component DEFAULT_NAME =
            Component.translatable("container." + SomeonesPerfectEnd.MOD_ID + ".infuser");

    public InfuserBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
        // BeaconBlockEntity hardcodes BlockEntityType.BEACON; save, sync, and tickers need our own type.
        ((BlockEntityAccessor) this).someonesperfectend$setType(Infusers.BLOCK_ENTITY.get());
    }

    /** Called from the super constructor, before the type is replaced, so it can't use {@code getType()}. */
    @Override
    public boolean isValidBlockState(BlockState state) {
        return state.is(ModBlocks.INFUSER.get());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        BeaconBlockEntityAccessor beacon = (BeaconBlockEntityAccessor) this;
        if (!BaseContainerBlockEntity.canUnlock(player, beacon.someonesperfectend$lockKey(), getDisplayName())) {
            return null;
        }
        return new InfuserMenu(
                containerId,
                inventory,
                beacon.someonesperfectend$dataAccess(),
                ContainerLevelAccess.create(level, getBlockPos()));
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public Component getName() {
        Component customName = getCustomName();
        return customName != null ? customName : DEFAULT_NAME;
    }
}
