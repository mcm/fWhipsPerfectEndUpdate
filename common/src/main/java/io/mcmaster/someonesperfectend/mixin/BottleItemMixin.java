package io.mcmaster.someonesperfectend.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import io.mcmaster.someonesperfectend.fluid.ModFluids;
import io.mcmaster.someonesperfectend.fluid.OasisWaterFluid;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BottleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** A glass bottle used on Oasis Water fills with Oasis Water instead of water. */
@Mixin(BottleItem.class)
public abstract class BottleItemMixin {
    @ModifyArg(method = "use", index = 2, at = @At(value = "INVOKE", ordinal = 1,
            target = "Lnet/minecraft/world/item/BottleItem;turnBottleIntoItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack someonesperfectend$fillOasisWater(ItemStack filled, @Local(argsOnly = true) Level level,
            @Local BlockPos pos) {
        return OasisWaterFluid.isOasisWater(level.getFluidState(pos))
                ? new ItemStack(ModFluids.OASIS_WATER_BOTTLE.get())
                : filled;
    }
}
