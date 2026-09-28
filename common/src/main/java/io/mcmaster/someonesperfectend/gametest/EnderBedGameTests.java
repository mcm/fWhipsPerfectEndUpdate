package io.mcmaster.someonesperfectend.gametest;

import io.mcmaster.someonesperfectend.SomeonesPerfectEnd;
import io.mcmaster.someonesperfectend.block.EnderBedBlock;
import io.mcmaster.someonesperfectend.block.EnderBeds;
import io.mcmaster.someonesperfectend.entity.Endling;
import io.mcmaster.someonesperfectend.entity.EndlingBarters;
import io.mcmaster.someonesperfectend.entity.EndlingSpecialBarters;
import io.mcmaster.someonesperfectend.entity.Endlings;
import io.mcmaster.someonesperfectend.gametest.EndlingBarterGameTests.Site;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** US-025: hanging Ender Beds (placement, support, drops, explosions) and their Endling barters. */
public final class EnderBedGameTests {
    /** Where the foot of a test bed goes; mock players face south, so the head is at {@code FOOT.south()}. */
    private static final BlockPos FOOT = new BlockPos(3, 3, 3);
    private static final BlockPos HEAD = FOOT.south();

    private EnderBedGameTests() {
    }

    private static Item vanillaBed(DyeColor color) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(color.getName() + "_bed"));
    }

    private static ResourceKey<LootTable> barterTable(DyeColor color) {
        return ResourceKey.create(Registries.LOOT_TABLE,
                SomeonesPerfectEnd.id("gameplay/endling_barter/" + color.getName() + "_bed"));
    }

    /** Uses a red Ender Bed item on {@code face} of the block at {@code target}; returns the player. */
    private static Player useBed(GameTestHelper helper, BlockPos target, Direction face) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(EnderBeds.get(DyeColor.RED)));
        BlockPos abs = helper.absolutePos(target);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs).relative(face, 0.5), face, abs, false);
        player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        return player;
    }

    /** Stone above both halves plus a placed red bed (foot at {@link #FOOT}, facing south). */
    private static void hangBed(GameTestHelper helper) {
        helper.setBlock(FOOT.above(), Blocks.STONE);
        helper.setBlock(HEAD.above(), Blocks.STONE);
        useBed(helper, FOOT.above(), Direction.DOWN);
        helper.assertBlockPresent(EnderBeds.get(DyeColor.RED), FOOT);
        helper.assertBlockPresent(EnderBeds.get(DyeColor.RED), HEAD);
    }

    private static List<ItemEntity> droppedBeds(GameTestHelper helper) {
        AABB area = new AABB(helper.absolutePos(FOOT)).inflate(4.0);
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, area,
                item -> item.getItem().is(EnderBeds.ITEM_TAG));
    }

    /** Succeeds once both halves are gone and exactly one red Ender Bed item was dropped. */
    private static void expectSingleDrop(GameTestHelper helper) {
        helper.succeedWhen(() -> {
            helper.assertBlockNotPresent(EnderBeds.get(DyeColor.RED), FOOT);
            helper.assertBlockNotPresent(EnderBeds.get(DyeColor.RED), HEAD);
            List<ItemEntity> drops = droppedBeds(helper);
            helper.assertTrue(drops.size() == 1 && drops.get(0).getItem().getCount() == 1
                            && drops.get(0).getItem().is(EnderBeds.get(DyeColor.RED).asItem()),
                    "expected exactly one red Ender Bed item, got " + drops.stream().map(ItemEntity::getItem).toList());
        });
    }

    @GameTest(template = ModGameTests.EMPTY_TEMPLATE)
    public static void placesUnderStone(GameTestHelper helper) {
        hangBed(helper);
        BlockState foot = helper.getBlockState(FOOT);
        BlockState head = helper.getBlockState(HEAD);
        helper.assertTrue(foot.getValue(EnderBedBlock.PART) == BedPart.FOOT, "the clicked block should be the foot");
        helper.assertTrue(head.getValue(EnderBedBlock.PART) == BedPart.HEAD, "the head should be beside the foot");
        helper.assertTrue(foot.getValue(EnderBedBlock.FACING) == Direction.SOUTH, "the bed should face the player's way");
        helper.succeed();
    }

    @GameTest(template = ModGameTests.EMPTY_TEMPLATE)
    public static void cannotPlaceOnTopOfStone(GameTestHelper helper) {
        helper.setBlock(FOOT.below(), Blocks.STONE);
        helper.setBlock(HEAD.below(), Blocks.STONE);
        Player player = useBed(helper, FOOT.below(), Direction.UP);
        helper.assertBlockNotPresent(EnderBeds.get(DyeColor.RED), FOOT);
        helper.assertBlockNotPresent(EnderBeds.get(DyeColor.RED), HEAD);
        helper.assertTrue(player.getMainHandItem().getCount() == 1, "a refused placement should keep the item");
        helper.succeed();
    }

    @GameTest(template = ModGameTests.EMPTY_TEMPLATE)
    public static void cannotPlaceWithOneHalfUnsupported(GameTestHelper helper) {
        helper.setBlock(FOOT.above(), Blocks.STONE);
        useBed(helper, FOOT.above(), Direction.DOWN);
        helper.assertBlockNotPresent(EnderBeds.get(DyeColor.RED), FOOT);
        helper.assertBlockNotPresent(EnderBeds.get(DyeColor.RED), HEAD);
        // A block whose bottom face isn't sturdy doesn't count as support.
        helper.setBlock(HEAD.above(), Blocks.OAK_FENCE);
        useBed(helper, FOOT.above(), Direction.DOWN);
        helper.assertBlockNotPresent(EnderBeds.get(DyeColor.RED), FOOT);
        helper.succeed();
    }

    @GameTest(template = ModGameTests.EMPTY_TEMPLATE)
    public static void removingFootSupportDropsTheBed(GameTestHelper helper) {
        hangBed(helper);
        helper.setBlock(FOOT.above(), Blocks.AIR);
        expectSingleDrop(helper);
    }

    @GameTest(template = ModGameTests.EMPTY_TEMPLATE)
    public static void removingHeadSupportDropsTheBed(GameTestHelper helper) {
        hangBed(helper);
        helper.setBlock(HEAD.above(), Blocks.AIR);
        expectSingleDrop(helper);
    }

    @GameTest(template = ModGameTests.EMPTY_TEMPLATE)
    public static void supportThatStopsBeingSturdyDropsTheBed(GameTestHelper helper) {
        hangBed(helper);
        helper.setBlock(HEAD.above(), Blocks.OAK_FENCE);
        expectSingleDrop(helper);
    }

    @GameTest(template = ModGameTests.EMPTY_TEMPLATE)
    public static void breakingTheFootDropsOneBed(GameTestHelper helper) {
        hangBed(helper);
        helper.getLevel().destroyBlock(helper.absolutePos(FOOT), true);
        expectSingleDrop(helper);
    }

    @GameTest(template = ModGameTests.EMPTY_TEMPLATE)
    public static void breakingTheHeadDropsOneBed(GameTestHelper helper) {
        hangBed(helper);
        helper.getLevel().destroyBlock(helper.absolutePos(HEAD), true);
        expectSingleDrop(helper);
    }

    /** Mirrors ServerPlayerGameMode#destroyBlock in creative: playerWillDestroy, then removal without drops. */
    @GameTest(template = ModGameTests.EMPTY_TEMPLATE)
    public static void creativeBreakingDropsNothing(GameTestHelper helper) {
        hangBed(helper);
        Player creative = helper.makeMockPlayer(GameType.CREATIVE);
        ServerLevel level = helper.getLevel();
        BlockPos foot = helper.absolutePos(FOOT);
        BlockState state = level.getBlockState(foot);
        state = state.getBlock().playerWillDestroy(level, foot, state, creative);
        level.removeBlock(foot, false);
        state.getBlock().destroy(level, foot, state);
        helper.runAfterDelay(5, () -> {
            helper.assertBlockNotPresent(EnderBeds.get(DyeColor.RED), HEAD);
            helper.assertTrue(droppedBeds(helper).isEmpty(), "creative breaking should drop nothing");
            helper.succeed();
        });
    }

    @GameTest(template = ModGameTests.EMPTY_TEMPLATE)
    public static void bedProperties(GameTestHelper helper) {
        for (DyeColor color : DyeColor.values()) {
            EnderBedBlock bed = EnderBeds.get(color);
            BlockState state = bed.defaultBlockState();
            helper.assertTrue(bed.getColor() == color, EnderBeds.name(color) + " has the wrong color");
            helper.assertTrue(BuiltInRegistries.BLOCK.getKey(bed).equals(SomeonesPerfectEnd.id(EnderBeds.name(color))),
                    "unexpected id " + BuiltInRegistries.BLOCK.getKey(bed));
            helper.assertTrue(state.is(EnderBeds.BLOCK_TAG), EnderBeds.name(color) + " should be in the block tag");
            helper.assertTrue(new ItemStack(bed).is(EnderBeds.ITEM_TAG), EnderBeds.name(color) + " should be in the item tag");
            helper.assertTrue(new ItemStack(bed).getMaxStackSize() == 1, "Ender Beds should stack to 1");
            helper.assertTrue(state.is(BlockTags.MINEABLE_WITH_AXE), "Ender Beds should be mineable with an axe");
            helper.assertFalse(state.requiresCorrectToolForDrops(), "Ender Beds should drop when mined by hand");
            helper.assertFalse(state.is(BlockTags.BEDS), "Ender Beds should not count as vanilla beds");
        }
        helper.succeed();
    }

    private static InteractionResult use(ServerLevel level, BlockPos pos, Player player) {
        return level.getBlockState(pos).useWithoutItem(level, player,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
    }

    @GameTest(template = ModGameTests.EMPTY_TEMPLATE)
    public static void doesNotExplodeInTheOverworld(GameTestHelper helper) {
        hangBed(helper);
        Player player = EndlingBarterGameTests.playerIn(helper.getLevel(), ItemStack.EMPTY);
        use(helper.getLevel(), helper.absolutePos(HEAD), player);
        use(helper.getLevel(), helper.absolutePos(FOOT), player);
        helper.assertBlockPresent(EnderBeds.get(DyeColor.RED), FOOT);
        helper.assertBlockPresent(EnderBeds.get(DyeColor.RED), HEAD);
        helper.assertBlockPresent(Blocks.STONE, FOOT.above());
        helper.succeed();
    }

    /** Builds a hanging bed in {@code level} at {@code foot} (facing south) inside an air pocket. */
    private static void hangBedIn(ServerLevel level, BlockPos foot) {
        for (BlockPos pos : BlockPos.betweenClosed(foot.offset(-3, -3, -3), foot.offset(3, 3, 4))) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
        EnderBedBlock bed = EnderBeds.get(DyeColor.RED);
        level.setBlock(foot.above(), Blocks.OBSIDIAN.defaultBlockState(), 2);
        level.setBlock(foot.south().above(), Blocks.OBSIDIAN.defaultBlockState(), 2);
        BlockState state = bed.defaultBlockState().setValue(EnderBedBlock.FACING, Direction.SOUTH);
        level.setBlock(foot, state, 2);
        level.setBlock(foot.south(), state.setValue(EnderBedBlock.PART, BedPart.HEAD), 2);
    }

    @GameTest(template = ModGameTests.EMPTY_TEMPLATE)
    public static void explodesInTheNether(GameTestHelper helper) {
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        BlockPos foot = new BlockPos(2000, 64, 2000);
        hangBedIn(nether, foot);
        BlockPos head = foot.south();
        List<BlockPos> netherrack = List.of(head.below(), head.east(), head.west(), head.south());
        netherrack.forEach(pos -> nether.setBlock(pos, Blocks.NETHERRACK.defaultBlockState(), 2));
        Player player = EndlingBarterGameTests.playerIn(nether, ItemStack.EMPTY);
        helper.assertTrue(use(nether, foot, player).consumesAction(), "using an Ender Bed in the Nether should act");
        helper.assertFalse(nether.getBlockState(foot).is(EnderBeds.BLOCK_TAG), "the foot should be gone");
        helper.assertFalse(nether.getBlockState(foot.south()).is(EnderBeds.BLOCK_TAG), "the head should be gone");
        helper.assertTrue(netherrack.stream().noneMatch(pos -> nether.getBlockState(pos).is(Blocks.NETHERRACK)),
                "the explosion should destroy the netherrack around the head");
        helper.succeed();
    }

    @GameTest(template = ModGameTests.EMPTY_TEMPLATE)
    public static void everyVanillaBedBartersForTheSameColor(GameTestHelper helper) {
        Endling endling = helper.spawn(Endlings.TYPE.get(), new Vec3(3.5, 1.0, 3.5));
        for (DyeColor color : DyeColor.values()) {
            ItemStack input = new ItemStack(vanillaBed(color));
            helper.assertTrue(barterTable(color).equals(EndlingSpecialBarters.lootTableFor(input.getItem())),
                    input + " should map to " + barterTable(color).location() + ", got "
                            + EndlingSpecialBarters.lootTableFor(input.getItem()));
            helper.assertTrue(EndlingBarters.isInput(input), input + " should be accepted by endlings");
            helper.assertFalse(input.is(EndlingBarters.INPUTS), input + " should not be a default barter input");
            List<ItemStack> roll = EndlingBarters.roll(endling, barterTable(color));
            helper.assertTrue(roll.size() == 1 && roll.get(0).getCount() == 1
                            && roll.get(0).is(EnderBeds.get(color).asItem()),
                    input + " should barter for one " + EnderBeds.name(color) + ", gave " + roll);
        }
        endling.discard();
        helper.succeed();
    }

    @GameTest(timeoutTicks = 200)
    public static void redBedBartersForRedEnderBed(GameTestHelper helper) {
        Site site = Site.open(helper, 8);
        site.drop(new ItemStack(vanillaBed(DyeColor.RED)), 2.0);
        Item red = EnderBeds.get(DyeColor.RED).asItem();
        helper.succeedWhen(() -> {
            List<ItemEntity> items = site.items();
            helper.assertTrue(items.stream().anyMatch(item -> item.getItem().is(red) && item.getItem().getCount() == 1),
                    "no red Ender Bed yet (items: " + items.stream().map(ItemEntity::getItem).toList() + ")");
            helper.assertFalse(site.endling().isInspecting(), "the endling should be done inspecting");
            helper.assertTrue(items.stream().noneMatch(item -> item.getItem().is(vanillaBed(DyeColor.RED))),
                    "the red bed should have been consumed");
            site.close();
        });
    }
}
