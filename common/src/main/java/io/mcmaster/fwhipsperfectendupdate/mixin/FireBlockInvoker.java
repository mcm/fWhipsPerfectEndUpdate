package io.mcmaster.fwhipsperfectendupdate.mixin;

import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Reads vanilla's fire table (private on Fabric, public on NeoForge); used by GameTests. */
@Mixin(FireBlock.class)
public interface FireBlockInvoker {
    @Invoker("getBurnOdds")
    int fwhipsperfectendupdate$getBurnOdds(BlockState state);

    @Invoker("getIgniteOdds")
    int fwhipsperfectendupdate$getIgniteOdds(BlockState state);
}
