package io.mcmaster.someonesperfectend.fabric.gametest;

import io.mcmaster.someonesperfectend.fluid.ModFluids;
import net.fabricmc.fabric.api.transfer.v1.fluid.CauldronFluidContent;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.LayeredCauldronBlock;

/** The Oasis Water Cauldron through the Fabric Transfer API. */
public final class OasisCauldronTransferGameTests {
    private OasisCauldronTransferGameTests() {
    }

    @GameTest
    public static void oasisCauldronIsRegisteredWithTransferApi(GameTestHelper helper) {
        CauldronFluidContent content = CauldronFluidContent.getForBlock(ModFluids.OASIS_WATER_CAULDRON.get());
        helper.assertTrue(content != null && content.fluid == ModFluids.OASIS_WATER.get(),
                "the Oasis Water Cauldron should hold Oasis Water, got " + (content == null ? null : content.fluid));
        helper.assertTrue(content.amountPerLevel == FluidConstants.BOTTLE && content.maxLevel == 3,
                "the Oasis Water Cauldron should hold 3 bottles");

        // Taking a bottle through the fluid storage lowers the level.
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModFluids.OASIS_WATER_CAULDRON.get().defaultBlockState()
                .setValue(LayeredCauldronBlock.LEVEL, 3));
        Storage<FluidVariant> storage = FluidStorage.SIDED.find(helper.getLevel(), helper.absolutePos(pos),
                Direction.UP);
        helper.assertTrue(storage != null, "no fluid storage for the Oasis Water Cauldron");
        try (Transaction transaction = Transaction.openOuter()) {
            long extracted = storage.extract(FluidVariant.of(ModFluids.OASIS_WATER.get()), FluidConstants.BOTTLE,
                    transaction);
            helper.assertTrue(extracted == FluidConstants.BOTTLE, "extracted " + extracted + " droplets");
            transaction.commit();
        }
        helper.assertBlockProperty(pos, LayeredCauldronBlock.LEVEL, 2);
        helper.succeed();
    }
}
