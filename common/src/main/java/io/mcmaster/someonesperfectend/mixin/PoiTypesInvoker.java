package io.mcmaster.someonesperfectend.mixin;

import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Maps block states to a POI type; NeoForge does this when the type is registered, Fabric doesn't. */
@Mixin(PoiTypes.class)
public interface PoiTypesInvoker {
    @Invoker("registerBlockStates")
    static void someonesperfectend$registerBlockStates(Holder<PoiType> type, Set<BlockState> states) {
        throw new AssertionError();
    }
}
