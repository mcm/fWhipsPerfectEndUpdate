package io.mcmaster.someonesperfectend.fluid.neoforge;

import io.mcmaster.someonesperfectend.fluid.ModFluids;
import io.mcmaster.someonesperfectend.fluid.OasisWaterCauldronBlock;
import io.mcmaster.someonesperfectend.fluid.OasisWaterFluid;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.RegisterCauldronFluidContentEvent;

/**
 * NeoForge keys most water behavior (swimming, drowning, fire, boats, farmland, lava interactions) on the
 * fluid type rather than {@code #minecraft:water}, so Oasis Water uses the vanilla water fluid type. Its
 * different look is applied by {@code LiquidBlockRendererMixin}.
 */
public final class OasisWaterPlatformImpl {
    private OasisWaterPlatformImpl() {
    }

    public static OasisWaterFluid.Source createSource() {
        return new OasisWaterFluid.Source() {
            @Override
            public FluidType getFluidType() {
                return NeoForgeMod.WATER_TYPE.value();
            }
        };
    }

    public static OasisWaterFluid.Flowing createFlowing() {
        return new OasisWaterFluid.Flowing() {
            @Override
            public FluidType getFluidType() {
                return NeoForgeMod.WATER_TYPE.value();
            }
        };
    }

    /** Called while our mod is being constructed, so the active container is ours. */
    public static void registerCauldron() {
        IEventBus modBus = ModLoadingContext.get().getActiveContainer().getEventBus();
        // Common setup runs mods in parallel; the vanilla interaction maps aren't thread-safe.
        modBus.addListener(FMLCommonSetupEvent.class,
                event -> event.enqueueWork(OasisWaterCauldronBlock::registerInteractions));
        // The total amount is a bucket, so each of the 3 levels is a bottle, as for the vanilla water cauldron.
        modBus.addListener(RegisterCauldronFluidContentEvent.class, event -> event.register(
                ModFluids.OASIS_WATER_CAULDRON.get(), ModFluids.OASIS_WATER.get(), FluidType.BUCKET_VOLUME,
                LayeredCauldronBlock.LEVEL));
    }
}
