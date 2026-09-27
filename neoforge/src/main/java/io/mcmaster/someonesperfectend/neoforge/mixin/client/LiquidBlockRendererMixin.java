package io.mcmaster.someonesperfectend.neoforge.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.mcmaster.someonesperfectend.fluid.ModFluids;
import io.mcmaster.someonesperfectend.fluid.OasisWaterFluid;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.textures.FluidSpriteCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Oasis Water shares the vanilla water fluid type, whose client extensions give water's textures and biome
 * tint. Swap in the Oasis Water textures, untinted.
 */
@Mixin(LiquidBlockRenderer.class)
public abstract class LiquidBlockRendererMixin {
    @WrapOperation(method = "tesselate", at = @At(value = "INVOKE",
            target = "Lnet/neoforged/neoforge/client/textures/FluidSpriteCache;getFluidSprites(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/material/FluidState;)[Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;"))
    private TextureAtlasSprite[] someonesperfectend$oasisWaterSprites(BlockAndTintGetter level, BlockPos pos,
            FluidState fluid, Operation<TextureAtlasSprite[]> original) {
        if (!OasisWaterFluid.isOasisWater(fluid)) {
            return original.call(level, pos, fluid);
        }
        return new TextureAtlasSprite[] {
                FluidSpriteCache.getSprite(ModFluids.STILL_TEXTURE),
                FluidSpriteCache.getSprite(ModFluids.FLOWING_TEXTURE),
                FluidSpriteCache.getSprite(ModFluids.OVERLAY_TEXTURE),
        };
    }

    @WrapOperation(method = "tesselate", at = @At(value = "INVOKE",
            target = "Lnet/neoforged/neoforge/client/extensions/common/IClientFluidTypeExtensions;getTintColor(Lnet/minecraft/world/level/material/FluidState;Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;)I"))
    private int someonesperfectend$oasisWaterTint(IClientFluidTypeExtensions extensions, FluidState fluid,
            BlockAndTintGetter level, BlockPos pos, Operation<Integer> original) {
        return OasisWaterFluid.isOasisWater(fluid) ? 0xFFFFFFFF : original.call(extensions, fluid, level, pos);
    }
}
