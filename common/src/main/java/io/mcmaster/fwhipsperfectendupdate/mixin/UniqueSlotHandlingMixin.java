package io.mcmaster.fwhipsperfectendupdate.mixin;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.slot.UniqueSlotHandling;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Dependency purity: Accessories registers its own "unique" cosmetic-armor slots ({@code accessories:head},
 * {@code accessories:chest}, {@code accessories:legs}, {@code accessories:feet}, {@code accessories:animal_body})
 * for players, armor stands, and horses. They are only reachable through the library's screen, which this mod
 * disables, so leave them registered (the library keeps references to them) but bind them to no entity.
 * Unique slots from other mods are untouched.
 */
@Mixin(value = UniqueSlotHandling.class, remap = false)
public abstract class UniqueSlotHandlingMixin {
    @Inject(method = "getSlotToEntities", at = @At("RETURN"), cancellable = true)
    private static void fwhipsperfectendupdate$unbindLibrarySlots(
            CallbackInfoReturnable<Map<String, Set<EntityType<?>>>> cir) {
        String libraryPrefix = Accessories.MODID + ":";
        cir.setReturnValue(cir.getReturnValue().entrySet().stream()
                .filter(entry -> !entry.getKey().startsWith(libraryPrefix))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue)));
    }
}
