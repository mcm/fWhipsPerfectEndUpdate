package io.mcmaster.someonesperfectend.gametest;

import io.mcmaster.someonesperfectend.SomeonesPerfectEnd;
import io.mcmaster.someonesperfectend.block.ModBlocks;
import io.mcmaster.someonesperfectend.block.ModWoodTypes;
import io.mcmaster.someonesperfectend.mixin.FireBlockInvoker;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.network.FilteredText;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class AstralWoodGameTests {
    private AstralWoodGameTests() {
    }

    @GameTest
    public static void axeStripsLogAndWood(GameTestHelper helper) {
        BlockPos log = new BlockPos(1, 1, 1);
        BlockPos wood = new BlockPos(3, 1, 1);
        helper.setBlock(log, ModBlocks.ASTRAL_LOG.get().defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        helper.setBlock(wood, ModBlocks.ASTRAL_WOOD.get());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
        useOnTop(helper, player, log);
        useOnTop(helper, player, wood);
        BlockState stripped = helper.getBlockState(log);
        helper.assertTrue(stripped.is(ModBlocks.STRIPPED_ASTRAL_LOG.get()), "log was not stripped: " + stripped);
        helper.assertTrue(stripped.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.X, "stripping lost the axis");
        helper.assertBlockPresent(ModBlocks.STRIPPED_ASTRAL_WOOD.get(), wood);
        helper.succeed();
    }

    @GameTest
    public static void logCraftsIntoPlanks(GameTestHelper helper) {
        for (ItemLike log : List.of(ModBlocks.ASTRAL_LOG.get(), ModBlocks.ASTRAL_WOOD.get(),
                ModBlocks.STRIPPED_ASTRAL_LOG.get(), ModBlocks.STRIPPED_ASTRAL_WOOD.get())) {
            ItemStack result = craft(helper, CraftingInput.of(1, 1, List.of(new ItemStack(log))));
            helper.assertTrue(result.is(ModBlocks.ASTRAL_PLANKS.get().asItem()) && result.getCount() == 4,
                    log + " crafted into " + result);
        }
        ItemStack stripped = new ItemStack(ModBlocks.STRIPPED_ASTRAL_LOG.get());
        ItemStack chain = new ItemStack(Items.CHAIN);
        ItemStack hangingSigns = craft(helper, CraftingInput.of(3, 3, List.of(
                chain, ItemStack.EMPTY, chain, stripped, stripped, stripped, stripped, stripped, stripped)));
        helper.assertTrue(hangingSigns.is(ModBlocks.ASTRAL_HANGING_SIGN_ITEM.get()) && hangingSigns.getCount() == 6,
                "stripped logs and chains crafted into " + hangingSigns);
        helper.assertTrue(helper.getLevel().getServer().getAdvancements()
                .get(SomeonesPerfectEnd.id("recipes/building_blocks/astral_planks")) != null,
                "planks recipe advancement is missing");
        helper.succeed();
    }

    @GameTest
    public static void woodBurnsAndFuelsFurnaces(GameTestHelper helper) {
        FireBlockInvoker fire = (FireBlockInvoker) Blocks.FIRE;
        for (Block block : List.of(ModBlocks.ASTRAL_PLANKS.get(), ModBlocks.ASTRAL_STAIRS.get(),
                ModBlocks.ASTRAL_SLAB.get(), ModBlocks.ASTRAL_FENCE.get(), ModBlocks.ASTRAL_FENCE_GATE.get())) {
            assertFireOdds(helper, fire, block, 5, 20);
        }
        for (Block block : List.of(ModBlocks.ASTRAL_LOG.get(), ModBlocks.ASTRAL_WOOD.get(),
                ModBlocks.STRIPPED_ASTRAL_LOG.get(), ModBlocks.STRIPPED_ASTRAL_WOOD.get())) {
            assertFireOdds(helper, fire, block, 5, 5);
        }
        for (Item item : List.of(ModBlocks.ASTRAL_LOG.get().asItem(), ModBlocks.ASTRAL_PLANKS.get().asItem(),
                ModBlocks.ASTRAL_STAIRS.get().asItem(), ModBlocks.ASTRAL_SLAB.get().asItem(),
                ModBlocks.ASTRAL_FENCE.get().asItem(), ModBlocks.ASTRAL_FENCE_GATE.get().asItem(),
                ModBlocks.ASTRAL_DOOR.get().asItem(), ModBlocks.ASTRAL_TRAPDOOR.get().asItem(),
                ModBlocks.ASTRAL_BUTTON.get().asItem(), ModBlocks.ASTRAL_PRESSURE_PLATE.get().asItem(),
                ModBlocks.ASTRAL_SIGN_ITEM.get(), ModBlocks.ASTRAL_HANGING_SIGN_ITEM.get())) {
            helper.assertTrue(AbstractFurnaceBlockEntity.isFuel(new ItemStack(item)), item + " is not furnace fuel");
        }
        helper.succeed();
    }

    @GameTest
    public static void signCanBePlacedAndEdited(GameTestHelper helper) {
        BlockPos ground = new BlockPos(1, 1, 1);
        helper.setBlock(ground, Blocks.STONE);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModBlocks.ASTRAL_SIGN_ITEM.get()));
        useOnTop(helper, player, ground);
        BlockPos signPos = ground.above();
        helper.assertBlockPresent(ModBlocks.ASTRAL_SIGN.get(), signPos);
        SignBlockEntity sign = assertSign(helper, signPos, SignBlockEntity.class);
        editAndCheck(helper, player, sign, "Astral");
        helper.succeed();
    }

    @GameTest
    public static void hangingSignCanBePlacedAndEdited(GameTestHelper helper) {
        BlockPos ceiling = new BlockPos(1, 3, 1);
        helper.setBlock(ceiling, Blocks.STONE);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModBlocks.ASTRAL_HANGING_SIGN_ITEM.get()));
        Vec3 bottomFace = Vec3.atCenterOf(helper.absolutePos(ceiling)).add(0, -0.5, 0);
        player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(bottomFace, Direction.DOWN, helper.absolutePos(ceiling), false)));
        BlockPos signPos = ceiling.below();
        helper.assertBlockPresent(ModBlocks.ASTRAL_HANGING_SIGN.get(), signPos);
        HangingSignBlockEntity sign = assertSign(helper, signPos, HangingSignBlockEntity.class);
        editAndCheck(helper, player, sign, "Void");
        helper.succeed();
    }

    @GameTest
    public static void astralWoodTypeIsRegistered(GameTestHelper helper) {
        helper.assertTrue(WoodType.values().anyMatch(type -> type == ModWoodTypes.ASTRAL), "astral WoodType missing");
        helper.assertTrue(ModWoodTypes.ASTRAL.name().equals("someonesperfectend:astral"),
                "unexpected name " + ModWoodTypes.ASTRAL.name());
        helper.assertTrue(ModBlocks.ASTRAL_DOOR.get().type() == ModWoodTypes.ASTRAL_SET, "door uses another set");
        helper.succeed();
    }

    private static void useOnTop(GameTestHelper helper, Player player, BlockPos pos) {
        BlockPos abs = helper.absolutePos(pos);
        player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.5, 0), Direction.UP, abs, false)));
    }

    private static ItemStack craft(GameTestHelper helper, CraftingInput input) {
        Optional<RecipeHolder<CraftingRecipe>> recipe = helper.getLevel().getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
        return recipe.map(holder -> holder.value().assemble(input, helper.getLevel().registryAccess()))
                .orElse(ItemStack.EMPTY);
    }

    private static void assertFireOdds(GameTestHelper helper, FireBlockInvoker fire, Block block, int ignite, int burn) {
        BlockState state = block.defaultBlockState();
        helper.assertTrue(fire.someonesperfectend$getIgniteOdds(state) == ignite
                        && fire.someonesperfectend$getBurnOdds(state) == burn,
                block + " has fire odds " + fire.someonesperfectend$getIgniteOdds(state) + "/"
                        + fire.someonesperfectend$getBurnOdds(state));
    }

    private static <T extends SignBlockEntity> T assertSign(GameTestHelper helper, BlockPos pos, Class<T> type) {
        BlockEntity blockEntity = helper.getBlockEntity(pos);
        helper.assertTrue(type.isInstance(blockEntity), "no sign block entity at " + pos + ": " + blockEntity);
        helper.assertTrue(blockEntity.getType().isValid(helper.getBlockState(pos)),
                "sign block entity type rejects " + helper.getBlockState(pos));
        return type.cast(blockEntity);
    }

    /** Placing a sign opens the editor for the placer (vanilla), so the placer may then submit text. */
    private static void editAndCheck(GameTestHelper helper, Player player, SignBlockEntity sign, String text) {
        helper.assertTrue(player.getUUID().equals(sign.getPlayerWhoMayEdit()), "placer may not edit the sign");
        sign.updateSignText(player, true, List.of(FilteredText.passThrough(text), FilteredText.passThrough(""),
                FilteredText.passThrough(""), FilteredText.passThrough("")));
        String line = sign.getFrontText().getMessage(0, false).getString();
        helper.assertTrue(text.equals(line), "sign reads '" + line + "', expected '" + text + "'");
    }
}
