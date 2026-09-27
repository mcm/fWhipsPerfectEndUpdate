package io.mcmaster.someonesperfectend.gametest;

import io.mcmaster.someonesperfectend.SomeonesPerfectEnd;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

public final class SmokeGameTests {
    private SmokeGameTests() {
    }

    @GameTest
    public static void placesBlockInTemplate(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, Blocks.END_STONE);
        helper.assertBlockPresent(Blocks.END_STONE, pos);
        helper.succeed();
    }

    @GameTest
    public static void creativeTabIsRegistered(GameTestHelper helper) {
        helper.assertTrue(BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(SomeonesPerfectEnd.id("main")),
                "creative tab someonesperfectend:main is not registered");
        helper.succeed();
    }
}
