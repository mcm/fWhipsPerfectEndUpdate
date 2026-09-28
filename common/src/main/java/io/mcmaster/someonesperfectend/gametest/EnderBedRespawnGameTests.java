package io.mcmaster.someonesperfectend.gametest;

import com.mojang.authlib.GameProfile;
import io.mcmaster.someonesperfectend.block.EnderBedBlock;
import io.mcmaster.someonesperfectend.block.EnderBedLocations;
import io.mcmaster.someonesperfectend.block.EnderBedRules;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.AttributeKey;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

/** US-037: storing Ender Bed locations and respawning at the End Ender Bed. */
public final class EnderBedRespawnGameTests {
    private static final String END_SET = "message.someonesperfectend.ender_bed_end_set";
    private static final String OVERWORLD_SET = "message.someonesperfectend.ender_bed_overworld_set";

    private EnderBedRespawnGameTests() {
    }

    /** A real {@link ServerPlayer} (not in the level or player list) whose sent packets land in {@link #channel}. */
    record Detached(ServerPlayer player, EmbeddedChannel channel) {
        static Detached in(ServerLevel level) {
            MinecraftServer server = level.getServer();
            CommonListenerCookie cookie = CommonListenerCookie.createInitial(
                    new GameProfile(UUID.randomUUID(), "test-mock-player"), false);
            ServerPlayer player = new ServerPlayer(server, level, cookie.gameProfile(), cookie.clientInformation());
            Connection connection = new Connection(PacketFlow.SERVERBOUND);
            EmbeddedChannel channel = new EmbeddedChannel(connection);
            // On NeoForge, only payloads on negotiated channels may be sent; Accessories syncs on its own channel
            // (e.g. on a dimension change). NeoForge reads ad-hoc channels from this Netty attribute (a no-op on
            // Fabric).
            channel.attr(AttributeKey.<Set<ResourceLocation>>valueOf("neoforge:adhoc_channels"))
                    .set(new HashSet<>(Set.of(ResourceLocation.fromNamespaceAndPath("accessories", "main"))));
            new ServerGamePacketListenerImpl(server, connection, player, cookie);
            return new Detached(player, channel);
        }

        boolean sent(Predicate<Packet<?>> test) {
            return channel.outboundMessages().stream()
                    .anyMatch(message -> message instanceof Packet<?> packet && test.test(packet));
        }

        /** Whether a translated action-bar ({@code overlay}) message with this key was sent. */
        boolean sentActionBar(String key) {
            return sent(packet -> packet instanceof ClientboundSystemChatPacket chat && chat.overlay()
                    && chat.content().getContents() instanceof TranslatableContents text && text.getKey().equals(key));
        }

        /** Whether vanilla's "no respawn block" event ({@code block.minecraft.spawn.not_valid}) was sent. */
        boolean sentSpawnNotValid() {
            return sent(packet -> packet instanceof ClientboundGameEventPacket event
                    && event.getEvent() == ClientboundGameEventPacket.NO_RESPAWN_BLOCK_AVAILABLE);
        }
    }

    static ServerLevel end(GameTestHelper helper) {
        return helper.getLevel().getServer().getLevel(Level.END);
    }

    /** A spot in the End's void ring for each test, so tests running together don't share beds. */
    private static BlockPos endFoot(int slot) {
        return new BlockPos(300 + 16 * slot, 100, -700);
    }

    private static EnderBedLocations beds(Player player) {
        return EnderBedLocations.of(player);
    }

    /** Gives the player a vanilla respawn point in the test area (forced, so no bed is needed there). */
    private static BlockPos setVanillaSpawn(GameTestHelper helper, ServerPlayer player) {
        BlockPos spawn = helper.absolutePos(new BlockPos(1, 2, 1));
        player.setRespawnPosition(Level.OVERWORLD, spawn, 0.0F, true, false);
        return spawn;
    }

    private static void assertVanillaSpawn(GameTestHelper helper, ServerPlayer player, BlockPos spawn) {
        helper.assertTrue(spawn.equals(player.getRespawnPosition()) && player.getRespawnDimension() == Level.OVERWORLD,
                "the vanilla respawn point should be unchanged, got " + player.getRespawnPosition() + " in "
                        + player.getRespawnDimension().location());
    }

    private static DimensionTransition respawnAfterDeath(ServerPlayer player) {
        player.setHealth(0.0F);
        return player.findRespawnPositionAndUseSpawnBlock(false, DimensionTransition.DO_NOTHING);
    }

    @GameTest(template = ModGameTests.EMPTY_TEMPLATE)
    public static void usingAnEndBedStoresEndOnly(GameTestHelper helper) {
        ServerLevel end = end(helper);
        BlockPos foot = endFoot(0);
        EnderBedGameTests.hangBedIn(end, foot);
        Detached detached = Detached.in(end);
        ServerPlayer player = detached.player();
        BlockPos spawn = setVanillaSpawn(helper, player);

        // Using the foot stores the head half.
        helper.assertTrue(EnderBedGameTests.use(end, foot, player).consumesAction(), "using the bed should act");
        helper.assertTrue(beds(player).end().equals(Optional.of(foot.south())),
                "using an End bed should store its head as `end`, got " + beds(player).end());
        helper.assertTrue(beds(player).overworld().isEmpty(), "an End bed should not set `overworld`");
        helper.assertTrue(detached.sentActionBar(END_SET), "expected the \"End spawn point set\" action bar message");
        assertVanillaSpawn(helper, player, spawn);
        helper.assertFalse(player.isSleeping(), "storing a location should not start sleeping");

        // A different bed in the same dimension replaces it.
        BlockPos otherFoot = endFoot(1);
        EnderBedGameTests.hangBedIn(end, otherFoot);
        EnderBedGameTests.use(end, otherFoot.south(), player);
        helper.assertTrue(beds(player).end().equals(Optional.of(otherFoot.south())),
                "a second End bed should replace `end`, got " + beds(player).end());
        assertVanillaSpawn(helper, player, spawn);
        helper.succeed();
    }

    @GameTest(template = ModGameTests.EMPTY_TEMPLATE)
    public static void usingAnOverworldBedStoresOverworldOnly(GameTestHelper helper) {
        EnderBedGameTests.hangBed(helper);
        Detached detached = Detached.in(helper.getLevel());
        ServerPlayer player = detached.player();
        BlockPos spawn = setVanillaSpawn(helper, player);
        EnderBedGameTests.use(helper.getLevel(), helper.absolutePos(EnderBedGameTests.HEAD), player);
        helper.assertTrue(beds(player).overworld().equals(Optional.of(helper.absolutePos(EnderBedGameTests.HEAD))),
                "using an Overworld bed should store its head as `overworld`, got " + beds(player).overworld());
        helper.assertTrue(beds(player).end().isEmpty(), "an Overworld bed should not set `end`");
        helper.assertTrue(detached.sentActionBar(OVERWORLD_SET), "expected the \"Ender Bed location set\" message");
        assertVanillaSpawn(helper, player, spawn);
        helper.succeed();
    }

    @GameTest(template = ModGameTests.EMPTY_TEMPLATE)
    public static void occupiedBedShowsVanillaMessage(GameTestHelper helper) {
        EnderBedGameTests.hangBed(helper);
        for (BlockPos pos : new BlockPos[] {EnderBedGameTests.FOOT, EnderBedGameTests.HEAD}) {
            helper.setBlock(pos, helper.getBlockState(pos).setValue(EnderBedBlock.OCCUPIED, true));
        }
        Detached detached = Detached.in(helper.getLevel());
        EnderBedGameTests.use(helper.getLevel(), helper.absolutePos(EnderBedGameTests.FOOT), detached.player());
        helper.assertTrue(detached.sentActionBar("block.minecraft.bed.occupied"), "expected \"This bed is occupied\"");
        helper.assertTrue(beds(detached.player()).overworld().isEmpty(), "an occupied bed should not be stored");
        helper.succeed();
    }

    @GameTest(template = ModGameTests.EMPTY_TEMPLATE)
    public static void dyingInTheEndRespawnsAtTheDropPoint(GameTestHelper helper) {
        ServerLevel end = end(helper);
        BlockPos foot = endFoot(2);
        BlockPos head = foot.south();
        EnderBedGameTests.hangBedIn(end, foot);
        Detached detached = Detached.in(end);
        ServerPlayer player = detached.player();
        BlockPos spawn = setVanillaSpawn(helper, player);
        EnderBedGameTests.use(end, head, player);

        DimensionTransition transition = respawnAfterDeath(player);
        helper.assertTrue(transition.newLevel() == end, "should respawn in the End, got "
                + transition.newLevel().dimension().location());
        Vec3 expected = Vec3.atBottomCenterOf(head.below(2));
        helper.assertTrue(transition.pos().equals(expected) && expected.equals(EnderBedRules.arrivalPos(head)),
                "should respawn under the bed at " + expected + ", got " + transition.pos());
        helper.assertTrue(transition.speed().equals(Vec3.ZERO), "should arrive with zero velocity");
        helper.assertFalse(transition.missingRespawnBlock(), "a valid Ender Bed is not a missing respawn block");
        helper.assertFalse(detached.sentSpawnNotValid(), "a valid Ender Bed should not show the not-valid message");
        helper.assertTrue(end.noCollision(player.getType().getDimensions().makeBoundingBox(transition.pos())),
                "the player should have room to fall at the drop point");
        assertVanillaSpawn(helper, player, spawn);
        helper.assertTrue(beds(player).end().equals(Optional.of(head)), "a valid respawn keeps `end`");

        // The new player entity created on respawn keeps both locations.
        ServerPlayer respawned = Detached.in(end).player();
        respawned.restoreFrom(player, false);
        helper.assertTrue(beds(respawned).end().equals(Optional.of(head)),
                "`end` should be copied to the respawned player");
        helper.succeed();
    }

    @GameTest(template = ModGameTests.EMPTY_TEMPLATE)
    public static void missingEndBedGivesVanillaRespawnAndClearsEnd(GameTestHelper helper) {
        ServerLevel end = end(helper);
        BlockPos foot = endFoot(3);
        EnderBedGameTests.hangBedIn(end, foot);
        Detached detached = Detached.in(end);
        ServerPlayer player = detached.player();
        BlockPos spawn = setVanillaSpawn(helper, player);
        EnderBedGameTests.use(end, foot, player);
        end.setBlock(foot.above(), Blocks.AIR.defaultBlockState(), 3);
        end.setBlock(foot.south().above(), Blocks.AIR.defaultBlockState(), 3);
        helper.assertFalse(end.getBlockState(foot.south()).getBlock() instanceof EnderBedBlock,
                "the bed should break without support");

        DimensionTransition transition = respawnAfterDeath(player);
        helper.assertTrue(transition.newLevel().dimension() == Level.OVERWORLD,
                "a missing bed should give vanilla respawn, got " + transition.newLevel().dimension().location());
        helper.assertTrue(BlockPos.containing(transition.pos()).equals(spawn),
                "vanilla respawn should use the vanilla respawn point " + spawn + ", got " + transition.pos());
        helper.assertTrue(beds(player).end().isEmpty(), "a missing bed should clear `end`");
        helper.assertTrue(detached.sentSpawnNotValid(), "a missing bed should show block.minecraft.spawn.not_valid");
        helper.assertFalse(transition.missingRespawnBlock(),
                "the vanilla respawn point is valid, so PlayerList must keep it");
        assertVanillaSpawn(helper, player, spawn);
        helper.succeed();
    }

    @GameTest(template = ModGameTests.EMPTY_TEMPLATE)
    public static void obstructedDropPointGivesVanillaRespawnAndClearsEnd(GameTestHelper helper) {
        ServerLevel end = end(helper);
        BlockPos foot = endFoot(4);
        BlockPos head = foot.south();
        EnderBedGameTests.hangBedIn(end, foot);
        end.setBlock(head.below(2), Blocks.END_STONE.defaultBlockState(), 3);
        Detached detached = Detached.in(end);
        ServerPlayer player = detached.player();
        EnderBedGameTests.use(end, head, player);

        // No vanilla respawn point: the world spawn in the Overworld.
        DimensionTransition transition = respawnAfterDeath(player);
        helper.assertTrue(transition.newLevel().dimension() == Level.OVERWORLD,
                "an obstructed bed should give vanilla respawn, got " + transition.newLevel().dimension().location());
        helper.assertTrue(beds(player).end().isEmpty(), "an obstructed bed should clear `end`");
        helper.assertTrue(detached.sentSpawnNotValid(), "an obstructed bed should show block.minecraft.spawn.not_valid");
        helper.succeed();
    }

    @GameTest(template = ModGameTests.EMPTY_TEMPLATE)
    public static void onlyDeathsInTheEndUseTheEndBed(GameTestHelper helper) {
        ServerLevel end = end(helper);
        BlockPos foot = endFoot(5);
        EnderBedGameTests.hangBedIn(end, foot);

        // The End-exit respawn (alive, leaving through the exit portal) stays vanilla.
        ServerPlayer leaving = Detached.in(end).player();
        EnderBedGameTests.use(end, foot, leaving);
        DimensionTransition exit = leaving.findRespawnPositionAndUseSpawnBlock(true, DimensionTransition.DO_NOTHING);
        helper.assertTrue(exit.newLevel().dimension() == Level.OVERWORLD, "leaving the End should stay vanilla");
        helper.assertTrue(beds(leaving).end().isPresent(), "leaving the End should keep `end`");

        // Dying in the Overworld with `end` set: vanilla respawn.
        ServerPlayer overworldDeath = Detached.in(helper.getLevel()).player();
        beds(overworldDeath).copyFrom(beds(leaving));
        DimensionTransition respawn = respawnAfterDeath(overworldDeath);
        helper.assertTrue(respawn.newLevel().dimension() == Level.OVERWORLD, "dying elsewhere should stay vanilla");
        helper.assertTrue(beds(overworldDeath).end().isPresent(), "dying elsewhere should keep `end`");
        helper.succeed();
    }

    @GameTest(template = ModGameTests.EMPTY_TEMPLATE)
    public static void locationsSurviveSaveAndLoad(GameTestHelper helper) {
        ServerPlayer player = Detached.in(helper.getLevel()).player();
        BlockPos endBed = new BlockPos(1200, 80, -3000);
        BlockPos overworldBed = new BlockPos(-5, 70, 12);
        beds(player).set(Level.END, endBed);
        beds(player).set(Level.OVERWORLD, overworldBed);
        CompoundTag tag = player.saveWithoutId(new CompoundTag());
        helper.assertTrue(tag.contains(EnderBedLocations.TAG), "player NBT should hold " + EnderBedLocations.TAG);

        ServerPlayer loaded = Detached.in(helper.getLevel()).player();
        loaded.load(tag);
        helper.assertTrue(beds(loaded).end().equals(Optional.of(endBed)), "`end` should survive save/load");
        helper.assertTrue(beds(loaded).overworld().equals(Optional.of(overworldBed)),
                "`overworld` should survive save/load");

        beds(player).set(Level.END, null);
        ServerPlayer partial = Detached.in(helper.getLevel()).player();
        partial.load(player.saveWithoutId(new CompoundTag()));
        helper.assertTrue(beds(partial).end().isEmpty() && beds(partial).overworld().isPresent(),
                "a cleared `end` should stay cleared after save/load");

        // The End-exit respawn (keepEverything) copies them too.
        ServerPlayer exited = Detached.in(helper.getLevel()).player();
        exited.restoreFrom(loaded, true);
        helper.assertTrue(beds(exited).end().equals(Optional.of(endBed))
                        && beds(exited).overworld().equals(Optional.of(overworldBed)),
                "the End-exit respawn should copy both locations");
        helper.succeed();
    }
}
