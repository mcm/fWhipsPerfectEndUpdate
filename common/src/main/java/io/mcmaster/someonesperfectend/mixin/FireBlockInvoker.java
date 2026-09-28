package io.mcmaster.someonesperfectend.mixin;

import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Reads vanilla's fire table (private on Fabric, public on NeoForge); used by GameTests. */
@Mixin(FireBlock.class)
public interface FireBlockInvoker {
    @Invoker("getBurnOdds")
    int someonesperfectend$getBurnOdds(BlockState state);

    @Invoker("getIgniteOdds")
    int someonesperfectend$getIgniteOdds(BlockState state);
}
