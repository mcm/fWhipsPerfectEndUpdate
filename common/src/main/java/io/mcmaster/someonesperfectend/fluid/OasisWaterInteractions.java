package io.mcmaster.someonesperfectend.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/**
 * Lava + Oasis Water. Mirrors vanilla's lava/water rules exactly, except that stone and cobblestone results
 * are end stone. Obsidian and basalt are unchanged.
 */
public final class OasisWaterInteractions {
    private OasisWaterInteractions() {}

    /** What lava next to water turns into: obsidian for a lava source, otherwise end stone (vanilla: cobblestone). */
    public static BlockState lavaResult(boolean lavaIsSource) {
        return (lavaIsSource ? Blocks.OBSIDIAN : Blocks.END_STONE).defaultBlockState();
    }

    /** What water turns into when lava flows down into it: end stone (vanilla: stone). */
    public static BlockState waterResult() {
        return Blocks.END_STONE.defaultBlockState();
    }

    /**
     * The lava-block check vanilla runs in {@code LiquidBlock#shouldSpreadLiquid} (NeoForge:
     * {@code FluidInteractionRegistry#canInteract}), handling only the case where the first water neighbor
     * vanilla would find is Oasis Water. Returns true if it converted the lava; false leaves everything to
     * vanilla (no water, vanilla water first, or basalt first).
     */
    public static boolean tryLavaInteraction(Level level, BlockPos pos) {
        FluidState lava = level.getFluidState(pos);
        if (!lava.is(FluidTags.LAVA)) {
            return false;
        }
        boolean soulSoilBelow = level.getBlockState(pos.below()).is(Blocks.SOUL_SOIL);
        for (Direction direction : LiquidBlock.POSSIBLE_FLOW_DIRECTIONS) {
            BlockPos neighbor = pos.relative(direction.getOpposite());
            FluidState neighborFluid = level.getFluidState(neighbor);
            if (neighborFluid.is(FluidTags.WATER)) {
                if (!OasisWaterFluid.isOasisWater(neighborFluid)) {
                    return false;
                }
                level.setBlockAndUpdate(pos, lavaResult(lava.isSource()));
                level.levelEvent(1501, pos, 0);
                return true;
            }
            if (soulSoilBelow && level.getBlockState(neighbor).is(Blocks.BLUE_ICE)) {
                return false;
            }
        }
        return false;
    }
}
