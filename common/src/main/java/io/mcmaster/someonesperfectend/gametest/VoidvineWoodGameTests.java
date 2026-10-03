package io.mcmaster.someonesperfectend.gametest;

import static io.mcmaster.someonesperfectend.gametest.AstralWoodGameTests.assertFireOdds;
import static io.mcmaster.someonesperfectend.gametest.AstralWoodGameTests.assertSign;
import static io.mcmaster.someonesperfectend.gametest.AstralWoodGameTests.craft;
import static io.mcmaster.someonesperfectend.gametest.AstralWoodGameTests.editAndCheck;
import static io.mcmaster.someonesperfectend.gametest.AstralWoodGameTests.useOnTop;

import dev.architectury.registry.fuel.FuelRegistry;
import io.mcmaster.someonesperfectend.SomeonesPerfectEnd;
import io.mcmaster.someonesperfectend.block.ModBlocks;
import io.mcmaster.someonesperfectend.block.ModWoodTypes;
import io.mcmaster.someonesperfectend.mixin.FireBlockInvoker;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;

public final class VoidvineWoodGameTests {
    private VoidvineWoodGameTests() {}

    @GameTest
    public static void axeStripsVoidvineBlock(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(
                pos,
                ModBlocks.VOIDVINE_BLOCK.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
        useOnTop(helper, player, pos);
        BlockState stripped = helper.getBlockState(pos);
        helper.assertTrue(stripped.is(ModBlocks.STRIPPED_VOIDVINE_BLOCK.get()), "block was not stripped: " + stripped);
        helper.assertTrue(stripped.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Z, "stripping lost the axis");
        helper.succeed();
    }

    @GameTest
    public static void stalksCraftIntoBlockAndSticks(GameTestHelper helper) {
        ItemStack stalk = new ItemStack(ModBlocks.VOIDVINE.get());
        ItemStack block = craft(helper, CraftingInput.of(3, 3, Collections.nCopies(9, stalk)));
        helper.assertTrue(
                block.is(ModBlocks.VOIDVINE_BLOCK.get().asItem()) && block.getCount() == 1,
                "9 stalks crafted into " + block);
        ItemStack sticks = craft(helper, CraftingInput.of(1, 2, List.of(stalk, stalk)));
        helper.assertTrue(sticks.is(Items.STICK) && sticks.getCount() == 1, "2 stalks crafted into " + sticks);
        helper.succeed();
    }

    @GameTest
    public static void blockCraftsIntoPlanks(GameTestHelper helper) {
        for (ItemLike block : List.of(ModBlocks.VOIDVINE_BLOCK.get(), ModBlocks.STRIPPED_VOIDVINE_BLOCK.get())) {
            ItemStack result = craft(helper, CraftingInput.of(1, 1, List.of(new ItemStack(block))));
            helper.assertTrue(
                    result.is(ModBlocks.VOIDVINE_PLANKS.get().asItem()) && result.getCount() == 2,
                    block + " crafted into " + result);
        }
        ItemStack stripped = new ItemStack(ModBlocks.STRIPPED_VOIDVINE_BLOCK.get());
        ItemStack chain = new ItemStack(Items.CHAIN);
        ItemStack hangingSigns = craft(
                helper,
                CraftingInput.of(
                        3,
                        3,
                        List.of(
                                chain,
                                ItemStack.EMPTY,
                                chain,
                                stripped,
                                stripped,
                                stripped,
                                stripped,
                                stripped,
                                stripped)));
        helper.assertTrue(
                hangingSigns.is(ModBlocks.VOIDVINE_HANGING_SIGN_ITEM.get()) && hangingSigns.getCount() == 6,
                "stripped blocks and chains crafted into " + hangingSigns);
        helper.assertTrue(
                helper.getLevel()
                                .getServer()
                                .getAdvancements()
                                .get(SomeonesPerfectEnd.id("recipes/building_blocks/voidvine_planks"))
                        != null,
                "planks recipe advancement is missing");
        helper.succeed();
    }

    @GameTest
    public static void slabsCraftIntoMosaic(GameTestHelper helper) {
        ItemStack slab = new ItemStack(ModBlocks.VOIDVINE_SLAB.get());
        ItemStack mosaic = craft(helper, CraftingInput.of(1, 2, List.of(slab, slab)));
        helper.assertTrue(
                mosaic.is(ModBlocks.VOIDVINE_MOSAIC.get().asItem()) && mosaic.getCount() == 1,
                "2 slabs crafted into " + mosaic);
        ItemStack mosaicStack = new ItemStack(ModBlocks.VOIDVINE_MOSAIC.get());
        ItemStack mosaicSlabs = craft(helper, CraftingInput.of(3, 1, Collections.nCopies(3, mosaicStack)));
        helper.assertTrue(
                mosaicSlabs.is(ModBlocks.VOIDVINE_MOSAIC_SLAB.get().asItem()) && mosaicSlabs.getCount() == 6,
                "3 mosaic crafted into " + mosaicSlabs);
        helper.succeed();
    }

    @GameTest
    public static void voidvineBurnsAndFuelsFurnaces(GameTestHelper helper) {
        FireBlockInvoker fire = (FireBlockInvoker) Blocks.FIRE;
        for (Block block : List.of(
                ModBlocks.VOIDVINE_PLANKS.get(),
                ModBlocks.VOIDVINE_MOSAIC.get(),
                ModBlocks.VOIDVINE_STAIRS.get(),
                ModBlocks.VOIDVINE_SLAB.get(),
                ModBlocks.VOIDVINE_MOSAIC_STAIRS.get(),
                ModBlocks.VOIDVINE_MOSAIC_SLAB.get(),
                ModBlocks.VOIDVINE_FENCE.get(),
                ModBlocks.VOIDVINE_FENCE_GATE.get())) {
            assertFireOdds(helper, fire, block, 5, 20);
        }
        for (Block block : List.of(ModBlocks.VOIDVINE_BLOCK.get(), ModBlocks.STRIPPED_VOIDVINE_BLOCK.get())) {
            assertFireOdds(helper, fire, block, 5, 5);
        }
        // Burn times of the matching bamboo items (vanilla furnace fuel map).
        Map<ItemLike, Integer> fuel = Map.ofEntries(
                Map.entry(ModBlocks.VOIDVINE.get(), 50),
                Map.entry(ModBlocks.VOIDVINE_BLOCK.get(), 300),
                Map.entry(ModBlocks.STRIPPED_VOIDVINE_BLOCK.get(), 300),
                Map.entry(ModBlocks.VOIDVINE_PLANKS.get(), 300),
                Map.entry(ModBlocks.VOIDVINE_MOSAIC.get(), 300),
                Map.entry(ModBlocks.VOIDVINE_STAIRS.get(), 300),
                Map.entry(ModBlocks.VOIDVINE_MOSAIC_STAIRS.get(), 300),
                Map.entry(ModBlocks.VOIDVINE_SLAB.get(), 150),
                Map.entry(ModBlocks.VOIDVINE_MOSAIC_SLAB.get(), 150),
                Map.entry(ModBlocks.VOIDVINE_FENCE.get(), 300),
                Map.entry(ModBlocks.VOIDVINE_FENCE_GATE.get(), 300),
                Map.entry(ModBlocks.VOIDVINE_TRAPDOOR.get(), 300),
                Map.entry(ModBlocks.VOIDVINE_PRESSURE_PLATE.get(), 300),
                Map.entry(ModBlocks.VOIDVINE_DOOR.get(), 200),
                Map.entry(ModBlocks.VOIDVINE_BUTTON.get(), 100),
                Map.entry(ModBlocks.VOIDVINE_SIGN_ITEM.get(), 200),
                Map.entry(ModBlocks.VOIDVINE_HANGING_SIGN_ITEM.get(), 800));
        fuel.forEach((item, ticks) -> {
            int burnTime = FuelRegistry.get(new ItemStack(item));
            helper.assertTrue(burnTime == ticks, item.asItem() + " burns for " + burnTime + ", expected " + ticks);
        });
        helper.succeed();
    }

    @GameTest
    public static void voidvineSignCanBePlacedAndEdited(GameTestHelper helper) {
        BlockPos ground = new BlockPos(1, 1, 1);
        helper.setBlock(ground, Blocks.STONE);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModBlocks.VOIDVINE_SIGN_ITEM.get()));
        useOnTop(helper, player, ground);
        BlockPos signPos = ground.above();
        helper.assertBlockPresent(ModBlocks.VOIDVINE_SIGN.get(), signPos);
        editAndCheck(helper, player, assertSign(helper, signPos, SignBlockEntity.class), "Voidvine");
        helper.succeed();
    }

    @GameTest
    public static void voidvineWoodTypeIsRegistered(GameTestHelper helper) {
        helper.assertTrue(
                WoodType.values().anyMatch(type -> type == ModWoodTypes.VOIDVINE), "voidvine WoodType missing");
        helper.assertTrue(
                ModWoodTypes.VOIDVINE.name().equals("someonesperfectend:voidvine"),
                "unexpected name " + ModWoodTypes.VOIDVINE.name());
        helper.assertTrue(ModBlocks.VOIDVINE_DOOR.get().type() == ModWoodTypes.VOIDVINE_SET, "door uses another set");
        Item stalk = ModBlocks.VOIDVINE.get();
        helper.assertTrue(
                stalk.builtInRegistryHolder().key().location().equals(SomeonesPerfectEnd.id("voidvine")),
                "stalk item has the wrong id");
        helper.succeed();
    }
}
