package io.mcmaster.someonesperfectend.fluid;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.WaterFluid;

/**
 * Oasis Water: vanilla water with a fixed teal color. It extends {@link WaterFluid} and is in
 * {@code #minecraft:water}, so every vanilla water behavior (flow, sources, swimming, drowning, fire,
 * farmland, evaporation in the Nether) applies unchanged. On NeoForge it also reports the vanilla water
 * fluid type (see {@code OasisWaterPlatformImpl}). The differences live elsewhere: rendering (client),
 * lava results ({@link OasisWaterInteractions}) and brewing ({@code OasisPotions}).
 */
public abstract class OasisWaterFluid extends WaterFluid {
    @Override
    public Fluid getFlowing() {
        return ModFluids.FLOWING_OASIS_WATER.get();
    }

    @Override
    public Fluid getSource() {
        return ModFluids.OASIS_WATER.get();
    }

    @Override
    public Item getBucket() {
        return ModFluids.OASIS_WATER_BUCKET.get();
    }

    @Override
    public BlockState createLegacyBlock(FluidState state) {
        return ModFluids.OASIS_WATER_BLOCK.get().defaultBlockState()
                .setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
    }

    @Override
    public boolean isSame(Fluid fluid) {
        return isOasisWater(fluid);
    }

    public static boolean isOasisWater(Fluid fluid) {
        return fluid instanceof OasisWaterFluid;
    }

    public static boolean isOasisWater(FluidState state) {
        return isOasisWater(state.getType());
    }

    public static class Source extends OasisWaterFluid {
        public Source() {
            OasisLogging.setSource(defaultFluidState().setValue(FALLING, false));
        }

        @Override
        public int getAmount(FluidState state) {
            return 8;
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }
    }

    public static class Flowing extends OasisWaterFluid {
        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }

        @Override
        public boolean isSource(FluidState state) {
            return false;
        }
    }
}
