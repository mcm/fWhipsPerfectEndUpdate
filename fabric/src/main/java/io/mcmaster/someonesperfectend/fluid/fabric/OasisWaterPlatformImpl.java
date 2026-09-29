package io.mcmaster.someonesperfectend.fluid.fabric;

import dev.architectury.event.events.common.LifecycleEvent;
import io.mcmaster.someonesperfectend.fluid.ModFluids;
import io.mcmaster.someonesperfectend.fluid.OasisWaterCauldronBlock;
import io.mcmaster.someonesperfectend.fluid.OasisWaterFluid;
import net.fabricmc.fabric.api.transfer.v1.fluid.CauldronFluidContent;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.world.level.block.LayeredCauldronBlock;

public final class OasisWaterPlatformImpl {
    private OasisWaterPlatformImpl() {
    }

    public static OasisWaterFluid.Source createSource() {
        return new OasisWaterFluid.Source();
    }

    public static OasisWaterFluid.Flowing createFlowing() {
        return new OasisWaterFluid.Flowing();
    }

    public static void registerCauldron() {
        LifecycleEvent.SETUP.register(() -> {
            OasisWaterCauldronBlock.registerInteractions();
            CauldronFluidContent.registerCauldron(ModFluids.OASIS_WATER_CAULDRON.get(), ModFluids.OASIS_WATER.get(),
                    FluidConstants.BOTTLE, LayeredCauldronBlock.LEVEL);
        });
    }
}
