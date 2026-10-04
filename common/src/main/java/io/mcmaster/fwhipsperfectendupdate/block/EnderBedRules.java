package io.mcmaster.fwhipsperfectendupdate.block;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Registry-free Ender Bed rules (unit-tested). */
public final class EnderBedRules {
    public static final ResourceLocation OVERWORLD = ResourceLocation.withDefaultNamespace("overworld");
    public static final ResourceLocation THE_END = ResourceLocation.withDefaultNamespace("the_end");

    private EnderBedRules() {}

    /** Ender Beds work in the Overworld and the End; used anywhere else, they explode like a vanilla bed in the Nether. */
    public static boolean explodesIn(ResourceLocation dimension) {
        return !dimension.equals(OVERWORLD) && !dimension.equals(THE_END);
    }

    /**
     * An Ender Bed's drop point (US-037): the block directly below its head half. Together with the block below it,
     * it must be open (no collision) for a player to arrive there.
     */
    public static BlockPos dropPoint(BlockPos head) {
        return head.below();
    }

    /** The two blocks an arriving player occupies: the drop point and the block below it. */
    public static BlockPos[] arrivalSpace(BlockPos head) {
        return new BlockPos[] {dropPoint(head), dropPoint(head).below()};
    }

    /**
     * Where an arriving player's feet go: centered under the head half, with the top of the player's 1.8-block
     * hitbox inside the drop point, so they hang just under the bed and fall.
     */
    public static Vec3 arrivalPos(BlockPos head) {
        return Vec3.atBottomCenterOf(dropPoint(head).below());
    }
}
