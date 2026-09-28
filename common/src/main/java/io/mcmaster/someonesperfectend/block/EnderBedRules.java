package io.mcmaster.someonesperfectend.block;

import net.minecraft.resources.ResourceLocation;

/** Registry-free Ender Bed rules (unit-tested). */
public final class EnderBedRules {
    public static final ResourceLocation OVERWORLD = ResourceLocation.withDefaultNamespace("overworld");
    public static final ResourceLocation THE_END = ResourceLocation.withDefaultNamespace("the_end");

    private EnderBedRules() {
    }

    /** Ender Beds work in the Overworld and the End; used anywhere else, they explode like a vanilla bed in the Nether. */
    public static boolean explodesIn(ResourceLocation dimension) {
        return !dimension.equals(OVERWORLD) && !dimension.equals(THE_END);
    }
}
