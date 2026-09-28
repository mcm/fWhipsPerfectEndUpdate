package io.mcmaster.someonesperfectend.mixin;

import net.minecraft.world.LockCode;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Private beacon state the Infuser's menu needs. */
@Mixin(BeaconBlockEntity.class)
public interface BeaconBlockEntityAccessor {
    @Accessor("dataAccess")
    ContainerData someonesperfectend$dataAccess();

    @Accessor("lockKey")
    LockCode someonesperfectend$lockKey();
}
