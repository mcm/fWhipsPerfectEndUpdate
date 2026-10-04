package io.mcmaster.fwhipsperfectendupdate.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Runs vanilla's End Gateway exit search; used by GameTests. */
@Mixin(TheEndGatewayBlockEntity.class)
public interface TheEndGatewayBlockEntityInvoker {
    @Invoker("findOrCreateValidTeleportPos")
    static BlockPos fwhipsperfectendupdate$findOrCreateValidTeleportPos(ServerLevel level, BlockPos gateway) {
        throw new AssertionError();
    }
}
