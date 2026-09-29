package io.mcmaster.someonesperfectend.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/**
 * Oasis-logging: a waterlogged block that holds Oasis Water instead of vanilla water (MOD-45).
 *
 * <p>Every block whose state definition has {@link BlockStateProperties#WATERLOGGED} also gets
 * {@link #OASIS_LOGGED} ({@code StateDefinitionBuilderMixin}), default {@code false} ({@code BlockMixin}). A state
 * with both {@code true} holds an Oasis Water source ({@code BlockStateBaseMixin}). {@code StateHolderMixin} keeps
 * {@code oasis_logged=true} only on waterlogged states. {@code SimpleWaterloggedBlockMixin} lets waterloggable blocks
 * take and give back Oasis Water.
 *
 * <p>This class is used while vanilla blocks are built, before the mod registers anything, so only interaction code
 * may touch {@link ModFluids}.
 */
public final class OasisLogging {
    public static final String NAME = "oasis_logged";
    public static final BooleanProperty OASIS_LOGGED = BooleanProperty.create(NAME);

    private static volatile FluidState source;

    private OasisLogging() {
    }

    /** Whether the state has both properties, i.e. can hold Oasis Water. */
    public static boolean canBeOasisLogged(BlockState state) {
        return state.hasProperty(OASIS_LOGGED) && state.hasProperty(BlockStateProperties.WATERLOGGED);
    }

    public static boolean isOasisLogged(BlockState state) {
        return canBeOasisLogged(state) && state.getValue(OASIS_LOGGED) && state.getValue(BlockStateProperties.WATERLOGGED);
    }

    /**
     * The state whose shapes an {@code oasis_logged=true} state uses: the same state with {@code oasis_logged=false}.
     * Shapes don't depend on the fluid, and some blocks only precompute shapes for the vanilla states.
     */
    public static BlockState shapeState(BlockState state) {
        return state.hasProperty(OASIS_LOGGED) && state.getValue(OASIS_LOGGED) ? state.setValue(OASIS_LOGGED, false) : state;
    }

    /** The Oasis Water source (source only: like vanilla water, flowing Oasis Water never waterlogs). */
    public static boolean isOasisSource(Fluid fluid) {
        return fluid == ModFluids.OASIS_WATER.get();
    }

    /**
     * The fluid state of an Oasis-logged block, or {@code fallback} before Oasis Water exists: vanilla builds its block
     * state caches (which only ask whether the fluid is empty) before mods create their fluids.
     */
    public static FluidState source(FluidState fallback) {
        FluidState state = source;
        return state != null ? state : fallback;
    }

    public static FluidState source() {
        return source(Fluids.WATER.getSource(false));
    }

    /** Called by the Oasis Water source fluid once it is created. */
    static void setSource(FluidState state) {
        source = state;
    }

    /**
     * Fills a waterloggable block with Oasis Water, like {@link LiquidBlockContainer#placeLiquid} with vanilla water.
     *
     * <p>Blocks that override {@code placeLiquid} (campfires and candles go out, other mods' blocks) usually only
     * accept vanilla water. For those, the block runs its own logic with vanilla water, and the water it took is then
     * turned into Oasis Water.
     *
     * @return whether the block took the water
     */
    public static boolean placeOasisWater(LevelAccessor level, BlockPos pos, BlockState state,
            LiquidBlockContainer container) {
        if (container.placeLiquid(level, pos, state, source())) {
            return true;
        }
        if (!canBeOasisLogged(state) || state.getValue(BlockStateProperties.WATERLOGGED)
                || !container.placeLiquid(level, pos, state, Fluids.WATER.getSource(false))) {
            return false;
        }
        if (!level.isClientSide()) {
            BlockState placed = level.getBlockState(pos);
            if (placed.is(state.getBlock()) && canBeOasisLogged(placed)
                    && placed.getValue(BlockStateProperties.WATERLOGGED)) {
                level.setBlock(pos, placed.setValue(OASIS_LOGGED, true), 3);
                Fluid oasis = ModFluids.OASIS_WATER.get();
                level.scheduleTick(pos, oasis, oasis.getTickDelay(level));
            }
        }
        return true;
    }
}
