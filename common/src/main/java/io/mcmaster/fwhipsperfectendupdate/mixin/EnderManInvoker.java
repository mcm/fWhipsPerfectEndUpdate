package io.mcmaster.fwhipsperfectendupdate.mixin;

import net.minecraft.world.entity.monster.EnderMan;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Teleports an Enderman to a chosen spot; used by GameTests. */
@Mixin(EnderMan.class)
public interface EnderManInvoker {
    @Invoker("teleport")
    boolean fwhipsperfectendupdate$teleport(double x, double y, double z);
}
