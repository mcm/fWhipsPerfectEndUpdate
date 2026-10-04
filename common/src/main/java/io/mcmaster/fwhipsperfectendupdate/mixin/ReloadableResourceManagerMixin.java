package io.mcmaster.fwhipsperfectendupdate.mixin;

import io.mcmaster.fwhipsperfectendupdate.recolor.RecolorPackResources;
import java.util.List;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Adds the generated recolor pack at the lowest priority whenever client resources (re)load. Both loaders
 * route the initial load and every F3+T / pack change through this method, so this is the only hook needed.
 * It is a common (not client-only) mixin so the GameTests can drive it on the test server; dedicated servers
 * never create a client-resources manager, so it does nothing there.
 */
@Mixin(ReloadableResourceManager.class)
public abstract class ReloadableResourceManagerMixin {
    @Shadow
    @Final
    private PackType type;

    @ModifyVariable(method = "createReload", at = @At("HEAD"), argsOnly = true)
    private List<PackResources> fwhipsperfectendupdate$addRecolorPack(List<PackResources> packs) {
        return type == PackType.CLIENT_RESOURCES ? RecolorPackResources.prependTo(packs) : packs;
    }
}
