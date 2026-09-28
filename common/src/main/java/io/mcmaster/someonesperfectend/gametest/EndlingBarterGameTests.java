package io.mcmaster.someonesperfectend.gametest;

import com.mojang.authlib.GameProfile;
import io.mcmaster.someonesperfectend.SomeonesPerfectEnd;
import io.mcmaster.someonesperfectend.entity.Endling;
import io.mcmaster.someonesperfectend.entity.EndlingBarters;
import io.mcmaster.someonesperfectend.entity.Endlings;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * US-022: Endling bartering. Endlings only barter in the End, so most tests build a small platform in the real End
 * level's void ring (vanilla terrain, nothing generates there) and force-load its chunk so the endling ticks.
 */
public final class EndlingBarterGameTests {
    private static final int TIMEOUT = 200;

    private EndlingBarterGameTests() {
    }

    private static Set<Item> barterOutputs() {
        Set<Item> items = new HashSet<>();
        for (String name : List.of("astral_sapling", "astral_log", "voidvine", "voidsoil", "end_turf", "void_petals",
                "glowcap", "bounce_cap", "voidfruit")) {
            items.add(BuiltInRegistries.ITEM.get(SomeonesPerfectEnd.id(name)));
        }
        return items;
    }

    /** A platform with an endling in the End, cleaned up with {@link #close}. Each test uses its own {@code slot}. */
    private record Site(ServerLevel end, BlockPos center, Endling endling) {
        static final int RADIUS = 6;

        static Site open(GameTestHelper helper, int slot) {
            ServerLevel end = helper.getLevel().getServer().getLevel(Level.END);
            BlockPos center = new BlockPos(400 + slot * 48, 100, 400);
            ChunkPos chunk = new ChunkPos(center);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    end.setChunkForced(chunk.x + dx, chunk.z + dz, true);
                }
            }
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-RADIUS, -1, -RADIUS),
                    center.offset(RADIUS, -1, RADIUS))) {
                end.setBlockAndUpdate(pos, Blocks.END_STONE.defaultBlockState());
            }
            Endling endling = Endlings.TYPE.get().create(end);
            endling.moveTo(Vec3.atBottomCenterOf(center));
            end.addFreshEntity(endling);
            return new Site(end, center, endling);
        }

        AABB area() {
            return new AABB(center).inflate(24.0, 100.0, 24.0);
        }

        ItemEntity drop(ItemStack stack, double dx) {
            ItemEntity item = new ItemEntity(end, center.getX() + 0.5 + dx, center.getY(), center.getZ() + 0.5, stack,
                    0.0, 0.0, 0.0);
            end.addFreshEntity(item);
            return item;
        }

        List<ItemEntity> items() {
            return end.getEntitiesOfClass(ItemEntity.class, area());
        }

        void close() {
            for (Entity entity : end.getEntitiesOfClass(Entity.class, area(), e -> !(e instanceof Player))) {
                entity.discard();
            }
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-RADIUS, -1, -RADIUS),
                    center.offset(RADIUS, -1, RADIUS))) {
                end.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
            ChunkPos chunk = new ChunkPos(center);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    end.setChunkForced(chunk.x + dx, chunk.z + dz, false);
                }
            }
        }
    }

    private static Player playerIn(Level level, ItemStack held) {
        Player player = new Player(level, BlockPos.ZERO, 0.0F, new GameProfile(UUID.randomUUID(), "test-mock-player")) {
            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return false;
            }
        };
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        return player;
    }

    /** Succeeds once a barter output lies around the site, at least an inspection's length after the pickup. */
    private static void expectBarterOutput(GameTestHelper helper, Site site, Item input) {
        Set<Item> outputs = barterOutputs();
        int[] tick = {0};
        int[] takenAt = {-1};
        helper.onEachTick(() -> {
            tick[0]++;
            if (takenAt[0] < 0 && site.endling().isInspecting()) {
                takenAt[0] = tick[0];
                helper.assertTrue(site.endling().getMainHandItem().is(input) && site.endling().getMainHandItem()
                        .getCount() == 1, "the endling should hold exactly one input, holds "
                        + site.endling().getMainHandItem());
            }
        });
        helper.succeedWhen(() -> {
            List<ItemEntity> items = site.items();
            boolean paid = items.stream().anyMatch(item -> outputs.contains(item.getItem().getItem()));
            helper.assertTrue(paid, "no barter output yet after " + tick[0] + " ticks (items: "
                    + items.stream().map(ItemEntity::getItem).toList() + ")");
            helper.assertTrue(takenAt[0] > 0 && tick[0] - takenAt[0] >= Endling.INSPECT_TICKS - 5,
                    "the endling should inspect for 6 seconds, took the item at " + takenAt[0] + " and paid at "
                            + tick[0]);
            helper.assertFalse(site.endling().isInspecting(), "the endling should be done inspecting");
            helper.assertTrue(items.stream().noneMatch(item -> item.getItem().is(input)),
                    "the input should have been consumed");
            site.close();
        });
    }

    @GameTest(timeoutTicks = TIMEOUT)
    public static void droppedInputIsBartered(GameTestHelper helper) {
        Site site = Site.open(helper, 0);
        site.drop(new ItemStack(Items.DIRT), 2.0);
        expectBarterOutput(helper, site, Items.DIRT);
    }

    @GameTest(timeoutTicks = TIMEOUT)
    public static void handedOverInputIsBartered(GameTestHelper helper) {
        Site site = Site.open(helper, 1);
        Player player = playerIn(site.end(), new ItemStack(Items.OAK_LOG, 5));
        InteractionResult result = site.endling().interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(result.consumesAction(), "handing over a log should succeed, got " + result);
        helper.assertTrue(player.getMainHandItem().getCount() == 4, "handing over should take exactly one log");
        helper.assertTrue(site.endling().inspectTicks() == Endling.INSPECT_TICKS, "the endling should start inspecting");
        // Busy: a second hand-over is refused.
        helper.assertFalse(site.endling().interact(player, InteractionHand.MAIN_HAND).consumesAction(),
                "an endling inspects one item at a time");
        helper.assertTrue(player.getMainHandItem().getCount() == 4, "a busy endling takes nothing");
        expectBarterOutput(helper, site, Items.OAK_LOG);
    }

    @GameTest(timeoutTicks = TIMEOUT)
    public static void nonInputIsNotPickedUp(GameTestHelper helper) {
        Site site = Site.open(helper, 2);
        ItemEntity cobble = site.drop(new ItemStack(Items.COBBLESTONE), 0.0);
        ItemEntity diamond = site.drop(new ItemStack(Items.DIAMOND), 0.5);
        Player player = playerIn(site.end(), new ItemStack(Items.END_STONE, 2));
        helper.assertFalse(site.endling().interact(player, InteractionHand.MAIN_HAND).consumesAction(),
                "handing over end stone should do nothing");
        helper.assertTrue(player.getMainHandItem().getCount() == 2, "end stone should stay in the hand");
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(cobble.isAlive() && diamond.isAlive(), "non-input items should stay on the ground");
            helper.assertFalse(site.endling().isInspecting(), "the endling should not take non-input items");
            site.close();
            helper.succeed();
        });
    }

    @GameTest(timeoutTicks = TIMEOUT)
    public static void lostEndlingDoesNotBarter(GameTestHelper helper) {
        Site site = Site.open(helper, 3);
        site.endling().setLost();
        ItemEntity dirt = site.drop(new ItemStack(Items.DIRT), 0.0);
        Player player = playerIn(site.end(), new ItemStack(Items.DIRT));
        helper.assertFalse(site.endling().interact(player, InteractionHand.MAIN_HAND).consumesAction(),
                "a Lost endling should refuse a hand-over");
        helper.assertTrue(player.getMainHandItem().getCount() == 1, "a Lost endling takes nothing");
        helper.runAfterDelay(100, () -> {
            helper.assertFalse(site.endling().isInspecting(), "a Lost endling should not pick up barter items");
            helper.assertTrue(dirt.isAlive(), "the dirt should stay on the ground");
            helper.assertTrue(site.items().stream().noneMatch(item -> barterOutputs().contains(item.getItem().getItem())),
                    "a Lost endling should give nothing");
            site.close();
            helper.succeed();
        });
    }

    /** The GameTest level is the Overworld: endlings there ignore barter items. */
    @GameTest(template = ModGameTests.EMPTY_TEMPLATE, timeoutTicks = TIMEOUT)
    public static void endlingOutsideTheEndIgnoresInputs(GameTestHelper helper) {
        helper.assertTrue(helper.getLevel().dimension() != Level.END, "this test must run outside the End");
        for (int x = 0; x < 8; x++) {
            for (int z = 0; z < 8; z++) {
                helper.setBlock(x, 0, z, Blocks.END_STONE);
            }
        }
        Endling endling = helper.spawn(Endlings.TYPE.get(), new Vec3(3.5, 1.0, 3.5));
        ItemEntity dirt = helper.spawnItem(Items.DIRT, 3.5F, 1.0F, 3.5F);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SAND));
        helper.assertFalse(endling.interact(player, InteractionHand.MAIN_HAND).consumesAction(),
                "an endling outside the End should refuse a hand-over");
        helper.runAfterDelay(60, () -> {
            helper.assertFalse(endling.isInspecting(), "an endling outside the End should not pick up barter items");
            helper.assertTrue(dirt.isAlive(), "the dirt should stay on the ground");
            endling.discard();
            dirt.discard();
            helper.succeed();
        });
    }

    @GameTest(timeoutTicks = TIMEOUT)
    public static void killedWhileInspectingDropsTheInput(GameTestHelper helper) {
        Site site = Site.open(helper, 4);
        Player player = playerIn(site.end(), new ItemStack(Items.MOSS_BLOCK));
        site.endling().interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(site.endling().isInspecting(), "the endling should take the moss block");
        site.endling().kill();
        // The freshly force-loaded chunk may take a few ticks before its entities show up in lookups.
        helper.succeedWhen(() -> {
            helper.assertTrue(site.items().stream().anyMatch(item -> item.getItem().is(Items.MOSS_BLOCK)),
                    "a killed endling should drop the item it was inspecting");
            site.close();
        });
    }

    @GameTest(template = ModGameTests.EMPTY_TEMPLATE)
    public static void barterInputTag(GameTestHelper helper) {
        for (Item item : List.of(Items.DIRT, Items.COARSE_DIRT, Items.ROOTED_DIRT, Items.OAK_SAPLING,
                Items.CHERRY_SAPLING, Items.DANDELION, Items.POPPY, Items.OAK_LOG, Items.BIRCH_LOG, Items.CRIMSON_STEM,
                Items.GRASS_BLOCK, Items.MOSS_BLOCK, Items.SAND, Items.RED_SAND)) {
            ItemStack stack = new ItemStack(item);
            helper.assertTrue(stack.is(EndlingBarters.INPUTS), item + " should be a barter input");
            helper.assertTrue(EndlingBarters.DEFAULT_LOOT_TABLE.equals(EndlingBarters.lootTableFor(stack)),
                    item + " should roll the default barter table");
        }
        for (Item item : List.of(Items.COBBLESTONE, Items.END_STONE, Items.DIAMOND, Items.OAK_PLANKS, Items.GRAVEL,
                Items.BEACON, Items.TOTEM_OF_UNDYING, Items.RED_BED, Items.AIR)) {
            helper.assertFalse(EndlingBarters.isInput(new ItemStack(item)), item + " should not be a barter input");
        }
        helper.succeed();
    }

    @GameTest(template = ModGameTests.EMPTY_TEMPLATE)
    public static void barterLootTableGivesEndBlocks(GameTestHelper helper) {
        Endling endling = helper.spawn(Endlings.TYPE.get(), new Vec3(3.5, 1.0, 3.5));
        Set<Item> expected = barterOutputs();
        Set<Item> seen = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            List<ItemStack> roll = EndlingBarters.roll(endling, EndlingBarters.DEFAULT_LOOT_TABLE);
            helper.assertTrue(roll.size() == 1, "one barter should give one stack, gave " + roll);
            for (ItemStack stack : roll) {
                helper.assertTrue(expected.contains(stack.getItem()), "unexpected barter output " + stack);
                seen.add(stack.getItem());
            }
        }
        helper.assertTrue(seen.equals(expected), "every End block should be bartered, missing "
                + expected.stream().filter(item -> !seen.contains(item)).toList());
        endling.discard();
        helper.succeed();
    }
}
