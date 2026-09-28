package io.mcmaster.someonesperfectend.enchanting;

import com.mojang.datafixers.util.Pair;
import java.util.function.BooleanSupplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The enchanting table's lapis slot with Void Cleansing (US-036): it also accepts dragon's breath while the End-chanter
 * is active. Everything else is delegated to the loader's original lapis slot.
 */
public class EndChanterFuelSlot extends Slot {
    private final Slot original;
    private final BooleanSupplier endChanterActive;

    public EndChanterFuelSlot(Slot original, BooleanSupplier endChanterActive) {
        super(original.container, original.getContainerSlot(), original.x, original.y);
        this.original = original;
        this.endChanterActive = endChanterActive;
        this.index = original.index;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return this.original.mayPlace(stack) || stack.is(Items.DRAGON_BREATH) && this.endChanterActive.getAsBoolean();
    }

    @Override
    public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
        return this.original.getNoItemIcon();
    }
}
