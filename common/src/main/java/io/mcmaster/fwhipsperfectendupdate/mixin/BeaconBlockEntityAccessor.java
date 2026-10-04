package io.mcmaster.fwhipsperfectendupdate.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.LockCode;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Private beacon state the Infuser's menu needs. */
@Mixin(BeaconBlockEntity.class)
public interface BeaconBlockEntityAccessor {
    @Accessor("dataAccess")
    ContainerData fwhipsperfectendupdate$dataAccess();

    @Accessor("lockKey")
    LockCode fwhipsperfectendupdate$lockKey();

    /** Applies a beacon's powers to the players in range, as its 80-tick update does. */
    @Invoker("applyEffects")
    static void fwhipsperfectendupdate$applyEffects(
            Level level, BlockPos pos, int levels, Holder<MobEffect> primary, Holder<MobEffect> secondary) {
        throw new AssertionError();
    }
}
