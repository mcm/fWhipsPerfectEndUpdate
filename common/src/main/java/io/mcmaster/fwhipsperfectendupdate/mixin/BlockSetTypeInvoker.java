package io.mcmaster.fwhipsperfectendupdate.mixin;

import net.minecraft.world.level.block.state.properties.BlockSetType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BlockSetType.class)
public interface BlockSetTypeInvoker {
    @Invoker("register")
    static BlockSetType fwhipsperfectendupdate$register(BlockSetType type) {
        throw new AssertionError();
    }
}
