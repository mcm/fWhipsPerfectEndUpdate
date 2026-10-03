package io.mcmaster.someonesperfectend.gametest;

import com.mojang.authlib.GameProfile;
import io.mcmaster.someonesperfectend.SomeonesPerfectEnd;
import io.mcmaster.someonesperfectend.entity.Endling;
import io.mcmaster.someonesperfectend.entity.EndlingBarters;
import io.mcmaster.someonesperfectend.entity.Endlings;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * US-022: Endling bartering. Endlings only barter in the End, so most tests build a small platform in the real End
 * level's void ring (vanilla terrain, nothing generates there) and force-load its chunk so the endling ticks.
 *
 * <p>US-039: endlings only barter with players who have {@code minecraft:end/kill_dragon}, so offering players are
 * detached {@link ServerPlayer}s ({@link #serverPlayerIn}) and dropped items get a thrower ({@link Site#drop}).
 */
public final class EndlingBarterGameTests {
    private static final int TIMEOUT = 200;

    private EndlingBarterGameTests() {}

    private static Set<Item> barterOutputs() {
        Set<Item> items = new HashSet<>();
        for (String name : List.of(
                "astral_sapling",
                "astral_log",
                "voidvine",
                "voidsoil",
                "end_turf",
                "void_petals",
                "glowcap",
                "bounce_cap",
                "voidfruit")) {
            items.add(BuiltInRegistries.ITEM.get(SomeonesPerfectEnd.id(name)));
        }
        return items;
    }

    /**
     * A platform with an endling in the End, cleaned up with {@link #close}. Each test uses its own {@code slot}.
     * {@code player} has beaten the dragon and throws the items {@link #drop}ped here.
     */
    record Site(ServerLevel end, BlockPos center, Endling endling, ServerPlayer player) {
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
            for (BlockPos pos :
                    BlockPos.betweenClosed(center.offset(-RADIUS, -1, -RADIUS), center.offset(RADIUS, -1, RADIUS))) {
                end.setBlockAndUpdate(pos, Blocks.END_STONE.defaultBlockState());
            }
            Endling endling = Endlings.TYPE.get().create(end);
            endling.moveTo(Vec3.atBottomCenterOf(center));
            end.addFreshEntity(endling);
            return new Site(end, center, endling, serverPlayerIn(end, ItemStack.EMPTY, true));
        }

        AABB area() {
            return new AABB(center).inflate(24.0, 100.0, 24.0);
        }

        /** Drops an item thrown by {@link #player}, who has beaten the dragon. */
        ItemEntity drop(ItemStack stack, double dx) {
            return drop(stack, dx, player);
        }

        /** Drops an item thrown by {@code thrower}, or with no thrower (like a dispenser) if null. */
        ItemEntity drop(ItemStack stack, double dx, @Nullable Entity thrower) {
            ItemEntity item = new ItemEntity(
                    end, center.getX() + 0.5 + dx, center.getY(), center.getZ() + 0.5, stack, 0.0, 0.0, 0.0);
            if (thrower != null) {
                item.setThrower(thrower);
            }
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
            for (BlockPos pos :
                    BlockPos.betweenClosed(center.offset(-RADIUS, -1, -RADIUS), center.offset(RADIUS, -1, RADIUS))) {
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

    /**
     * A real server player in {@code level}, not added to it (see {@code InfuserGameTests}), holding {@code held}, with
     * the {@code minecraft:end/kill_dragon} advancement if {@code killedDragon}.
     */
    static ServerPlayer serverPlayerIn(ServerLevel level, ItemStack held, boolean killedDragon) {
        MinecraftServer server = level.getServer();
        CommonListenerCookie cookie =
                CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "test-mock-player"), false);
        ServerPlayer player = new ServerPlayer(server, level, cookie.gameProfile(), cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        new ServerGamePacketListenerImpl(server, connection, player, cookie);
        if (killedDragon) {
            AdvancementHolder advancement = server.getAdvancements().get(EndlingBarters.UNLOCK_ADVANCEMENT);
            for (String criterion : advancement.value().criteria().keySet()) {
                player.getAdvancements().award(advancement, criterion);
            }
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        return player;
    }

    static Player playerIn(Level level, ItemStack held) {
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
                helper.assertTrue(
                        site.endling().getMainHandItem().is(input)
                                && site.endling().getMainHandItem().getCount() == 1,
                        "the endling should hold exactly one input, holds "
                                + site.endling().getMainHandItem());
            }
        });
        helper.succeedWhen(() -> {
            List<ItemEntity> items = site.items();
            boolean paid = items.stream()
                    .anyMatch(item -> outputs.contains(item.getItem().getItem()));
            helper.assertTrue(
                    paid,
                    "no barter output yet after " + tick[0] + " ticks (items: "
                            + items.stream().map(ItemEntity::getItem).toList() + ")");
            helper.assertTrue(
                    takenAt[0] > 0 && tick[0] - takenAt[0] >= Endling.INSPECT_TICKS - 5,
                    "the endling should inspect for 6 seconds, took the item at " + takenAt[0] + " and paid at "
                            + tick[0]);
            helper.assertFalse(site.endling().isInspecting(), "the endling should be done inspecting");
            helper.assertTrue(
                    items.stream().noneMatch(item -> item.getItem().is(input)), "the input should have been consumed");
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
        Player player = serverPlayerIn(site.end(), new ItemStack(Items.OAK_LOG, 5), true);
        InteractionResult result = site.endling().interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(result.consumesAction(), "handing over a log should succeed, got " + result);
        helper.assertTrue(player.getMainHandItem().getCount() == 4, "handing over should take exactly one log");
        helper.assertTrue(
                site.endling().inspectTicks() == Endling.INSPECT_TICKS, "the endling should start inspecting");
        // Busy: a second hand-over is refused.
        helper.assertFalse(
                site.endling().interact(player, InteractionHand.MAIN_HAND).consumesAction(),
                "an endling inspects one item at a time");
        helper.assertTrue(player.getMainHandItem().getCount() == 4, "a busy endling takes nothing");
        expectBarterOutput(helper, site, Items.OAK_LOG);
    }

    @GameTest(timeoutTicks = TIMEOUT)
    public static void nonInputIsNotPickedUp(GameTestHelper helper) {
        Site site = Site.open(helper, 2);
        ItemEntity cobble = site.drop(new ItemStack(Items.COBBLESTONE), 0.0);
        ItemEntity diamond = site.drop(new ItemStack(Items.DIAMOND), 0.5);
        Player player = serverPlayerIn(site.end(), new ItemStack(Items.END_STONE, 2), true);
        helper.assertFalse(
                site.endling().interact(player, InteractionHand.MAIN_HAND).consumesAction(),
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
        Player player = serverPlayerIn(site.end(), new ItemStack(Items.DIRT), true);
        helper.assertFalse(
                site.endling().interact(player, InteractionHand.MAIN_HAND).consumesAction(),
                "a Lost endling should refuse a hand-over");
        helper.assertTrue(player.getMainHandItem().getCount() == 1, "a Lost endling takes nothing");
        helper.runAfterDelay(100, () -> {
            helper.assertFalse(site.endling().isInspecting(), "a Lost endling should not pick up barter items");
            helper.assertTrue(dirt.isAlive(), "the dirt should stay on the ground");
            helper.assertTrue(
                    site.items().stream()
                            .noneMatch(item ->
                                    barterOutputs().contains(item.getItem().getItem())),
                    "a Lost endling should give nothing");
            helper.assertTrue(site.endling().refusals() == 0, "a Lost endling ignores items silently");
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
        Player player = serverPlayerIn(helper.getLevel(), new ItemStack(Items.SAND), true);
        ItemEntity dirt = helper.spawnItem(Items.DIRT, 3.5F, 1.0F, 3.5F);
        dirt.setThrower(player);
        helper.assertFalse(
                endling.interact(player, InteractionHand.MAIN_HAND).consumesAction(),
                "an endling outside the End should refuse a hand-over");
        helper.runAfterDelay(60, () -> {
            helper.assertFalse(endling.isInspecting(), "an endling outside the End should not pick up barter items");
            helper.assertTrue(dirt.isAlive(), "the dirt should stay on the ground");
            helper.assertTrue(endling.refusals() == 0, "an endling outside the End ignores items silently");
            endling.discard();
            dirt.discard();
            helper.succeed();
        });
    }

    @GameTest(timeoutTicks = TIMEOUT)
    public static void killedWhileInspectingDropsTheInput(GameTestHelper helper) {
        Site site = Site.open(helper, 4);
        Player player = serverPlayerIn(site.end(), new ItemStack(Items.MOSS_BLOCK), true);
        site.endling().interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(site.endling().isInspecting(), "the endling should take the moss block");
        site.endling().kill();
        // The freshly force-loaded chunk may take a few ticks before its entities show up in lookups.
        helper.succeedWhen(() -> {
            helper.assertTrue(
                    site.items().stream().anyMatch(item -> item.getItem().is(Items.MOSS_BLOCK)),
                    "a killed endling should drop the item it was inspecting");
            site.close();
        });
    }

    /** US-039: a player without {@code minecraft:end/kill_dragon} is refused, dropped or handed over. */
    @GameTest(timeoutTicks = TIMEOUT)
    public static void playerWithoutDragonKillIsRefused(GameTestHelper helper) {
        Site site = Site.open(helper, 9);
        ServerPlayer locked = serverPlayerIn(site.end(), new ItemStack(Items.DIRT, 3), false);
        ItemEntity dirt = site.drop(new ItemStack(Items.DIRT), 0.0, locked);
        helper.assertTrue(
                EndlingBarters.mayBarter(site.player()) && !EndlingBarters.mayBarter(locked),
                "only the player with the advancement may barter");
        int[] tick = {0};
        helper.onEachTick(() -> {
            if (++tick[0] == 60) {
                // Hand-over, after the drop's refusal cooldown has run out.
                helper.assertFalse(
                        site.endling()
                                .interact(locked, InteractionHand.MAIN_HAND)
                                .consumesAction(),
                        "a player who hasn't beaten the dragon should be refused");
                helper.assertTrue(locked.getMainHandItem().getCount() == 3, "a refused item stays in the hand");
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(tick[0] >= 100, "waiting");
            helper.assertTrue(dirt.isAlive() && dirt.getItem().getCount() == 1, "the refused dirt stays on the ground");
            helper.assertFalse(site.endling().isInspecting(), "the endling should not take a refused item");
            helper.assertTrue(
                    site.endling().refusals() == 2,
                    "the endling should refuse the drop once and the " + "hand-over once, refused "
                            + site.endling().refusals() + " times");
            helper.assertTrue(
                    site.items().stream()
                            .noneMatch(item ->
                                    barterOutputs().contains(item.getItem().getItem())),
                    "a refused barter gives nothing");
            site.close();
        });
    }

    /** US-039: refusals show a head shake and are rate-limited, so a pile of items doesn't spam the sound. */
    @GameTest(timeoutTicks = TIMEOUT)
    public static void refusalsAreRateLimited(GameTestHelper helper) {
        Site site = Site.open(helper, 10);
        ServerPlayer locked = serverPlayerIn(site.end(), ItemStack.EMPTY, false);
        for (int i = 0; i < 5; i++) {
            site.drop(new ItemStack(Items.SAND), 0.1 * i, locked);
        }
        int[] tick = {0};
        int[] firstRefusal = {-1};
        helper.onEachTick(() -> {
            tick[0]++;
            if (firstRefusal[0] < 0 && site.endling().refusals() > 0) {
                firstRefusal[0] = tick[0];
                helper.assertTrue(site.endling().headShakeTicks() > 0, "a refusing endling should shake its head");
            }
            if (firstRefusal[0] > 0 && tick[0] < firstRefusal[0] + Endling.REFUSE_COOLDOWN) {
                helper.assertTrue(
                        site.endling().refusals() == 1,
                        "an endling should refuse at most once every " + Endling.REFUSE_COOLDOWN + " ticks");
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(firstRefusal[0] > 0 && tick[0] >= firstRefusal[0] + 100, "waiting");
            helper.assertTrue(
                    site.endling().refusals() <= 1 + 100 / Endling.REFUSE_COOLDOWN,
                    "too many refusals: " + site.endling().refusals());
            helper.assertTrue(
                    site.items().stream()
                                    .filter(item -> item.getItem().is(Items.SAND))
                                    .mapToInt(item -> item.getItem().getCount())
                                    .sum()
                            == 5,
                    "all the sand stays on the ground");
            site.close();
        });
    }

    /** US-039: dropped items with no thrower (dispensers, hoppers, mob drops) are refused. */
    @GameTest(timeoutTicks = TIMEOUT)
    public static void itemWithoutThrowerIsRefused(GameTestHelper helper) {
        Site site = Site.open(helper, 11);
        ItemEntity log = site.drop(new ItemStack(Items.OAK_LOG), 0.0, null);
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(log.isAlive() && log.getItem().getCount() == 1, "the log should stay on the ground");
            helper.assertFalse(site.endling().isInspecting(), "the endling should not take an item with no thrower");
            helper.assertTrue(
                    site.endling().refusals() == 1,
                    "the endling should refuse it once, refused "
                            + site.endling().refusals() + " times");
            site.close();
            helper.succeed();
        });
    }

    @GameTest(template = ModGameTests.EMPTY_TEMPLATE)
    public static void barterInputTag(GameTestHelper helper) {
        for (Item item : List.of(
                Items.DIRT,
                Items.COARSE_DIRT,
                Items.ROOTED_DIRT,
                Items.OAK_SAPLING,
                Items.CHERRY_SAPLING,
                Items.DANDELION,
                Items.POPPY,
                Items.OAK_LOG,
                Items.BIRCH_LOG,
                Items.CRIMSON_STEM,
                Items.GRASS_BLOCK,
                Items.MOSS_BLOCK,
                Items.SAND,
                Items.RED_SAND)) {
            ItemStack stack = new ItemStack(item);
            helper.assertTrue(stack.is(EndlingBarters.INPUTS), item + " should be a barter input");
            helper.assertTrue(
                    EndlingBarters.DEFAULT_LOOT_TABLE.equals(EndlingBarters.lootTableFor(stack)),
                    item + " should roll the default barter table");
        }
        for (Item item :
                List.of(Items.COBBLESTONE, Items.END_STONE, Items.DIAMOND, Items.OAK_PLANKS, Items.GRAVEL, Items.AIR)) {
            helper.assertFalse(EndlingBarters.isInput(new ItemStack(item)), item + " should not be a barter input");
        }
        // Special barter inputs (US-034, US-025) are accepted, but never roll the default table.
        for (Item item : List.of(Items.BEACON, Items.TOTEM_OF_UNDYING, Items.RED_BED)) {
            ItemStack stack = new ItemStack(item);
            helper.assertFalse(stack.is(EndlingBarters.INPUTS), item + " should not be in the barter input tag");
            helper.assertFalse(
                    EndlingBarters.DEFAULT_LOOT_TABLE.equals(EndlingBarters.lootTableFor(stack)),
                    item + " should not roll the default barter table");
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
        helper.assertTrue(
                seen.equals(expected),
                "every End block should be bartered, missing "
                        + expected.stream().filter(item -> !seen.contains(item)).toList());
        endling.discard();
        helper.succeed();
    }
}
