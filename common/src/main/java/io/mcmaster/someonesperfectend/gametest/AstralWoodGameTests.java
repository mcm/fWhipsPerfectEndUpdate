package io.mcmaster.someonesperfectend.gametest;

import io.mcmaster.someonesperfectend.SomeonesPerfectEnd;
import io.mcmaster.someonesperfectend.block.AstralLogBlock;
import io.mcmaster.someonesperfectend.block.EnderblossomBlock;
import io.mcmaster.someonesperfectend.block.ModBlockTags;
import io.mcmaster.someonesperfectend.block.ModBlocks;
import io.mcmaster.someonesperfectend.block.ModWoodTypes;
import io.mcmaster.someonesperfectend.mixin.FireBlockInvoker;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.FilteredText;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
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
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChorusPlantBlock;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class AstralWoodGameTests {
    private AstralWoodGameTests() {}

    @GameTest
    public static void axeStripsLogAndWood(GameTestHelper helper) {
        BlockPos log = new BlockPos(1, 1, 1);
        BlockPos wood = new BlockPos(3, 1, 1);
        helper.setBlock(
                log,
                ModBlocks.ASTRAL_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
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

    /**
     * The Astral Log is shaped like a chorus plant: a placed log reaches down into End Turf and towards Astral Logs
     * and Flowering Astral Logs next to it, and lets go when they are removed. The Flowering Astral Log keeps a full
     * collision box like a chorus flower, and neither block hides its neighbors' faces.
     */
    @GameTest
    public static void astralLogConnectsLikeChorusPlant(GameTestHelper helper) {
        BlockPos soil = new BlockPos(2, 1, 2);
        BlockPos lower = soil.above();
        BlockPos upper = lower.above();
        helper.setBlock(soil, ModBlocks.END_TURF.get());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModBlocks.ASTRAL_LOG.get(), 8));
        useOnTop(helper, player, soil);
        assertConnections(helper, lower, Direction.DOWN);
        useOnTop(helper, player, lower);
        assertConnections(helper, lower, Direction.DOWN, Direction.UP);
        assertConnections(helper, upper, Direction.DOWN);

        BlockPos flowering = upper.east();
        helper.setBlock(flowering, ModBlocks.FLOWERING_ASTRAL_LOG.get());
        assertConnections(helper, upper, Direction.DOWN, Direction.EAST);
        // Other logs, wood and leaves are not part of the plant.
        helper.setBlock(upper.west(), ModBlocks.ASTRAL_WOOD.get());
        helper.setBlock(upper.north(), ModBlocks.STRIPPED_ASTRAL_LOG.get());
        helper.setBlock(upper.south(), ModBlocks.ASTRAL_LEAVES.get());
        helper.setBlock(upper.above(), Blocks.END_STONE);
        assertConnections(helper, upper, Direction.DOWN, Direction.EAST);

        helper.setBlock(upper, Blocks.AIR);
        assertConnections(helper, lower, Direction.DOWN);
        helper.setBlock(soil, Blocks.AIR);
        assertConnections(helper, lower);

        ServerLevel level = helper.getLevel();
        BlockState log = helper.getBlockState(lower);
        AABB box = log.getShape(level, helper.absolutePos(lower)).bounds();
        helper.assertTrue(
                box.equals(new AABB(3 / 16.0, 3 / 16.0, 3 / 16.0, 13 / 16.0, 13 / 16.0, 13 / 16.0)),
                "a lone Astral Log should be a chorus plant core, not " + box);
        helper.assertTrue(
                !log.canOcclude() && !helper.getBlockState(flowering).canOcclude(),
                "Astral Logs should not hide their neighbors' faces");
        helper.assertTrue(
                Block.isShapeFullBlock(helper.getBlockState(flowering).getShape(level, helper.absolutePos(flowering))),
                "the Flowering Astral Log should keep a full block shape like a chorus flower");
        helper.succeed();
    }

    /** Asserts the Astral Log at {@code pos} connects in exactly the given directions. */
    static void assertConnections(GameTestHelper helper, BlockPos pos, Direction... directions) {
        BlockState state = helper.getBlockState(pos);
        helper.assertTrue(state.is(ModBlocks.ASTRAL_LOG.get()), "no Astral Log at " + pos + ": " + state);
        List<Direction> expected = List.of(directions);
        for (Direction direction : Direction.values()) {
            helper.assertTrue(
                    state.getValue(AstralLogBlock.CONNECTIONS.get(direction)) == expected.contains(direction),
                    "Astral Log at " + pos + " should connect only " + expected + ": " + state);
        }
    }

    /**
     * Astral Logs and Flowering Astral Logs don't join vanilla chorus: an Astral Log doesn't reach towards a Chorus
     * Plant or Chorus Flower, and a Chorus Plant doesn't reach towards an Astral Log or Flowering Astral Log.
     */
    @GameTest
    public static void astralLogsDoNotConnectToChorus(GameTestHelper helper) {
        BlockPos log = new BlockPos(2, 2, 2);
        BlockPos plant = log.above();
        helper.setBlock(log, ModBlocks.ASTRAL_LOG.get());
        helper.setBlock(log.east(), Blocks.CHORUS_PLANT);
        helper.setBlock(log.west(), Blocks.CHORUS_FLOWER);
        helper.setBlock(plant, Blocks.CHORUS_PLANT);
        helper.setBlock(plant.north(), ModBlocks.FLOWERING_ASTRAL_LOG.get());
        helper.setBlock(plant.south(), ModBlocks.ASTRAL_LOG.get());
        // Work the connections out from all neighbors, as placing each block does.
        ServerLevel level = helper.getLevel();
        BlockState astral = AstralLogBlock.connect(helper.getBlockState(log), level, helper.absolutePos(log));
        for (Direction direction : Direction.values()) {
            helper.assertFalse(
                    astral.getValue(AstralLogBlock.CONNECTIONS.get(direction)),
                    "the Astral Log should not connect to chorus: " + astral);
        }
        for (BlockPos pos : List.of(plant, log.east())) {
            BlockState chorus =
                    ChorusPlantBlock.getStateWithConnections(level, helper.absolutePos(pos), helper.getBlockState(pos));
            for (Direction direction : Direction.values()) {
                helper.assertFalse(
                        chorus.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction)),
                        "the Chorus Plant at " + pos + " should not connect to Astral Logs: " + chorus);
            }
        }
        helper.succeed();
    }

    /**
     * The Enderblossom model sets its stem on a chorus plant-shaped Astral Log's core, inside the log's block, so on
     * supports whose face is the block edge it is drawn out at that face instead of inside the support.
     */
    @GameTest
    public static void enderblossomStemSitsOnTheSupportFace(GameTestHelper helper) {
        helper.assertTrue(
                EnderblossomBlock.stemOffset(ModBlocks.ASTRAL_LOG.get().defaultBlockState()) == 0.0F,
                "on an Astral Log the stem should stay on the log's core");
        for (Block support : List.of(
                ModBlocks.FLOWERING_ASTRAL_LOG.get(),
                ModBlocks.ASTRAL_WOOD.get(),
                ModBlocks.STRIPPED_ASTRAL_LOG.get(),
                ModBlocks.STRIPPED_ASTRAL_WOOD.get())) {
            helper.assertTrue(
                    EnderblossomBlock.stemOffset(support.defaultBlockState()) == EnderblossomBlock.STEM_DEPTH,
                    "on " + support + " the stem should be moved out to the block face");
        }
        helper.succeed();
    }

    @GameTest
    public static void axeStripsFloweringLogKeepingAxis(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
        int x = 1;
        for (Direction.Axis axis : Direction.Axis.values()) {
            BlockPos pos = new BlockPos(x, 1, 1);
            x += 2;
            helper.setBlock(
                    pos,
                    ModBlocks.FLOWERING_ASTRAL_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis));
            useOnTop(helper, player, pos);
            BlockState stripped = helper.getBlockState(pos);
            helper.assertTrue(
                    stripped.is(ModBlocks.STRIPPED_ASTRAL_LOG.get()), "flowering log was not stripped: " + stripped);
            helper.assertTrue(stripped.getValue(RotatedPillarBlock.AXIS) == axis, "stripping lost the axis " + axis);
        }
        helper.succeed();
    }

    @GameTest
    public static void floweringLogDropsAstralLogWithoutSilkTouch(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.FLOWERING_ASTRAL_LOG.get());
        BlockState state = helper.getBlockState(pos);
        BlockPos abs = helper.absolutePos(pos);
        for (ItemStack tool : List.of(ItemStack.EMPTY, new ItemStack(Items.DIAMOND_AXE))) {
            List<Item> drops = Block.getDrops(state, level, abs, null, null, tool).stream()
                    .map(ItemStack::getItem)
                    .toList();
            helper.assertTrue(
                    drops.equals(List.of(ModBlocks.ASTRAL_LOG.get().asItem())),
                    "flowering log mined with " + tool + " dropped " + drops);
        }
        helper.succeed();
    }

    @GameTest
    public static void floweringLogDropsItselfWithSilkTouch(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.FLOWERING_ASTRAL_LOG.get());
        ItemStack silkAxe = new ItemStack(Items.DIAMOND_AXE);
        silkAxe.enchant(
                level.registryAccess()
                        .registryOrThrow(Registries.ENCHANTMENT)
                        .getHolderOrThrow(Enchantments.SILK_TOUCH),
                1);
        List<Item> drops =
                Block.getDrops(helper.getBlockState(pos), level, helper.absolutePos(pos), null, null, silkAxe).stream()
                        .map(ItemStack::getItem)
                        .toList();
        helper.assertTrue(
                drops.equals(List.of(ModBlocks.FLOWERING_ASTRAL_LOG.get().asItem())),
                "flowering log mined with Silk Touch dropped " + drops);
        helper.succeed();
    }

    @GameTest
    public static void floweringLogSharesAstralLogTagsAndBehavior(GameTestHelper helper) {
        BlockState flowering = ModBlocks.FLOWERING_ASTRAL_LOG.get().defaultBlockState();
        for (TagKey<Block> tag : List.of(
                ModBlockTags.ASTRAL_LOGS, BlockTags.LOGS_THAT_BURN, BlockTags.LOGS, BlockTags.MINEABLE_WITH_AXE)) {
            helper.assertTrue(flowering.is(tag), "flowering log is not in " + tag.location());
        }
        ItemStack stack = new ItemStack(ModBlocks.FLOWERING_ASTRAL_LOG.get());
        for (TagKey<Item> tag : List.of(
                TagKey.create(Registries.ITEM, SomeonesPerfectEnd.id("astral_logs")),
                ItemTags.LOGS_THAT_BURN,
                ItemTags.LOGS)) {
            helper.assertTrue(stack.is(tag), "flowering log item is not in " + tag.location());
        }
        helper.assertTrue(AbstractFurnaceBlockEntity.isFuel(stack), "flowering log is not furnace fuel");
        helper.assertTrue(
                ComposterBlock.COMPOSTABLES.getFloat(stack.getItem())
                        == ComposterBlock.COMPOSTABLES.getFloat(
                                ModBlocks.ASTRAL_LOG.get().asItem()),
                "flowering log compost value differs from the Astral Log");
        assertFireOdds(helper, (FireBlockInvoker) Blocks.FIRE, ModBlocks.FLOWERING_ASTRAL_LOG.get(), 5, 5);
        helper.assertTrue(
                ModBlocks.FLOWERING_ASTRAL_LOG.get().defaultDestroyTime()
                                == ModBlocks.ASTRAL_LOG.get().defaultDestroyTime()
                        && ModBlocks.FLOWERING_ASTRAL_LOG.get().getExplosionResistance()
                                == ModBlocks.ASTRAL_LOG.get().getExplosionResistance(),
                "flowering log strength differs from the Astral Log");
        helper.succeed();
    }

    @GameTest
    public static void logCraftsIntoPlanks(GameTestHelper helper) {
        for (ItemLike log : List.of(
                ModBlocks.ASTRAL_LOG.get(),
                ModBlocks.FLOWERING_ASTRAL_LOG.get(),
                ModBlocks.ASTRAL_WOOD.get(),
                ModBlocks.STRIPPED_ASTRAL_LOG.get(),
                ModBlocks.STRIPPED_ASTRAL_WOOD.get())) {
            ItemStack result = craft(helper, CraftingInput.of(1, 1, List.of(new ItemStack(log))));
            helper.assertTrue(
                    result.is(ModBlocks.ASTRAL_PLANKS.get().asItem()) && result.getCount() == 4,
                    log + " crafted into " + result);
        }
        ItemStack stripped = new ItemStack(ModBlocks.STRIPPED_ASTRAL_LOG.get());
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
                hangingSigns.is(ModBlocks.ASTRAL_HANGING_SIGN_ITEM.get()) && hangingSigns.getCount() == 6,
                "stripped logs and chains crafted into " + hangingSigns);
        helper.assertTrue(
                helper.getLevel()
                                .getServer()
                                .getAdvancements()
                                .get(SomeonesPerfectEnd.id("recipes/building_blocks/astral_planks"))
                        != null,
                "planks recipe advancement is missing");
        helper.succeed();
    }

    @GameTest
    public static void woodBurnsAndFuelsFurnaces(GameTestHelper helper) {
        FireBlockInvoker fire = (FireBlockInvoker) Blocks.FIRE;
        for (Block block : List.of(
                ModBlocks.ASTRAL_PLANKS.get(),
                ModBlocks.ASTRAL_STAIRS.get(),
                ModBlocks.ASTRAL_SLAB.get(),
                ModBlocks.ASTRAL_FENCE.get(),
                ModBlocks.ASTRAL_FENCE_GATE.get())) {
            assertFireOdds(helper, fire, block, 5, 20);
        }
        for (Block block : List.of(
                ModBlocks.ASTRAL_LOG.get(),
                ModBlocks.ASTRAL_WOOD.get(),
                ModBlocks.STRIPPED_ASTRAL_LOG.get(),
                ModBlocks.STRIPPED_ASTRAL_WOOD.get())) {
            assertFireOdds(helper, fire, block, 5, 5);
        }
        for (Item item : List.of(
                ModBlocks.ASTRAL_LOG.get().asItem(),
                ModBlocks.ASTRAL_PLANKS.get().asItem(),
                ModBlocks.ASTRAL_STAIRS.get().asItem(),
                ModBlocks.ASTRAL_SLAB.get().asItem(),
                ModBlocks.ASTRAL_FENCE.get().asItem(),
                ModBlocks.ASTRAL_FENCE_GATE.get().asItem(),
                ModBlocks.ASTRAL_DOOR.get().asItem(),
                ModBlocks.ASTRAL_TRAPDOOR.get().asItem(),
                ModBlocks.ASTRAL_BUTTON.get().asItem(),
                ModBlocks.ASTRAL_PRESSURE_PLATE.get().asItem(),
                ModBlocks.ASTRAL_SIGN_ITEM.get(),
                ModBlocks.ASTRAL_HANGING_SIGN_ITEM.get())) {
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
        player.getMainHandItem()
                .useOn(new UseOnContext(
                        player,
                        InteractionHand.MAIN_HAND,
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
        helper.assertTrue(
                ModWoodTypes.ASTRAL.name().equals("someonesperfectend:astral"),
                "unexpected name " + ModWoodTypes.ASTRAL.name());
        helper.assertTrue(ModBlocks.ASTRAL_DOOR.get().type() == ModWoodTypes.ASTRAL_SET, "door uses another set");
        helper.succeed();
    }

    static void useOnTop(GameTestHelper helper, Player player, BlockPos pos) {
        BlockPos abs = helper.absolutePos(pos);
        player.getMainHandItem()
                .useOn(new UseOnContext(
                        player,
                        InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.5, 0), Direction.UP, abs, false)));
    }

    static ItemStack craft(GameTestHelper helper, CraftingInput input) {
        Optional<RecipeHolder<CraftingRecipe>> recipe =
                helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
        return recipe.map(holder ->
                        holder.value().assemble(input, helper.getLevel().registryAccess()))
                .orElse(ItemStack.EMPTY);
    }

    static void assertFireOdds(GameTestHelper helper, FireBlockInvoker fire, Block block, int ignite, int burn) {
        BlockState state = block.defaultBlockState();
        helper.assertTrue(
                fire.someonesperfectend$getIgniteOdds(state) == ignite
                        && fire.someonesperfectend$getBurnOdds(state) == burn,
                block + " has fire odds " + fire.someonesperfectend$getIgniteOdds(state) + "/"
                        + fire.someonesperfectend$getBurnOdds(state));
    }

    static <T extends SignBlockEntity> T assertSign(GameTestHelper helper, BlockPos pos, Class<T> type) {
        BlockEntity blockEntity = helper.getBlockEntity(pos);
        helper.assertTrue(type.isInstance(blockEntity), "no sign block entity at " + pos + ": " + blockEntity);
        helper.assertTrue(
                blockEntity.getType().isValid(helper.getBlockState(pos)),
                "sign block entity type rejects " + helper.getBlockState(pos));
        return type.cast(blockEntity);
    }

    /** Placing a sign opens the editor for the placer (vanilla), so the placer may then submit text. */
    static void editAndCheck(GameTestHelper helper, Player player, SignBlockEntity sign, String text) {
        helper.assertTrue(player.getUUID().equals(sign.getPlayerWhoMayEdit()), "placer may not edit the sign");
        sign.updateSignText(
                player,
                true,
                List.of(
                        FilteredText.passThrough(text),
                        FilteredText.passThrough(""),
                        FilteredText.passThrough(""),
                        FilteredText.passThrough("")));
        String line = sign.getFrontText().getMessage(0, false).getString();
        helper.assertTrue(text.equals(line), "sign reads '" + line + "', expected '" + text + "'");
    }
}
