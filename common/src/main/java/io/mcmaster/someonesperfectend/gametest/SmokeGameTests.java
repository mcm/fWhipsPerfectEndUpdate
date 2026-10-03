package io.mcmaster.someonesperfectend.gametest;

import io.mcmaster.someonesperfectend.SomeonesPerfectEnd;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;

public final class SmokeGameTests {
    private SmokeGameTests() {}

    @GameTest
    public static void placesBlockInTemplate(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, Blocks.END_STONE);
        helper.assertBlockPresent(Blocks.END_STONE, pos);
        helper.succeed();
    }

    @GameTest
    public static void creativeTabIsRegistered(GameTestHelper helper) {
        helper.assertTrue(
                BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(SomeonesPerfectEnd.id("main")),
                "creative tab someonesperfectend:main is not registered");
        helper.succeed();
    }

    /** Every mod block that doesn't opt out with {@code noLootTable()} must ship a loot table. */
    @GameTest
    public static void everyBlockHasALootTable(GameTestHelper helper) {
        var lootTables = helper.getLevel().getServer().reloadableRegistries();
        BuiltInRegistries.BLOCK.entrySet().stream()
                .filter(entry -> entry.getKey().location().getNamespace().equals(SomeonesPerfectEnd.MOD_ID))
                .filter(entry -> entry.getValue().getLootTable() != BuiltInLootTables.EMPTY)
                .forEach(entry -> helper.assertTrue(
                        lootTables.getLootTable(entry.getValue().getLootTable()) != LootTable.EMPTY,
                        "missing loot table " + entry.getValue().getLootTable().location() + " for "
                                + entry.getKey().location()));
        helper.succeed();
    }
}
