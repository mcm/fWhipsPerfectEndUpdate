package io.mcmaster.someonesperfectend.gametest;

import com.mojang.authlib.GameProfile;
import io.mcmaster.someonesperfectend.block.ModBlocks;
import io.mcmaster.someonesperfectend.infuser.InfuserBlockEntity;
import io.mcmaster.someonesperfectend.infuser.InfuserMenu;
import io.mcmaster.someonesperfectend.infuser.Infusers;
import io.mcmaster.someonesperfectend.mixin.BeaconBlockEntityAccessor;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class InfuserGameTests {
    private static final BlockPos INFUSER = new BlockPos(3, 2, 3);

    /**
     * Tests that expect no Speed run in their own batch: an Infuser gives Speed to every player within range,
     * including other tests' players.
     */
    private static final String NEGATIVE_BATCH = "infuser_negative";

    private InfuserGameTests() {
    }

    /** Places an Infuser at {@link #INFUSER}, optionally on a 3x3 tier-1 pyramid of iron blocks. */
    private static InfuserBlockEntity placeInfuser(GameTestHelper helper, boolean pyramid) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                helper.setBlock(INFUSER.offset(x, -1, z), pyramid ? Blocks.IRON_BLOCK : Blocks.STONE);
            }
        }
        helper.setBlock(INFUSER, ModBlocks.INFUSER.get());
        return helper.getBlockEntity(INFUSER);
    }

    /** Opens a server-side menu on the Infuser, pays one iron ingot, and confirms Speed, like the beacon screen. */
    private static InfuserMenu confirmSpeed(GameTestHelper helper, InfuserBlockEntity infuser, Player player) {
        InfuserMenu menu = new InfuserMenu(0, player.getInventory(),
                ((BeaconBlockEntityAccessor) infuser).someonesperfectend$dataAccess(),
                ContainerLevelAccess.create(helper.getLevel(), infuser.getBlockPos()));
        menu.getSlot(InfuserMenu.PAYMENT_SLOT).set(new ItemStack(Items.IRON_INGOT));
        helper.assertTrue(menu.hasPayment(), "the confirm button should be enabled with a payment and no infusion item");
        menu.updateEffects(Optional.of(MobEffects.MOVEMENT_SPEED), Optional.empty());
        helper.assertTrue(menu.getSlot(InfuserMenu.PAYMENT_SLOT).getItem().isEmpty(), "confirm should consume the payment");
        helper.assertTrue(menu.getPrimaryEffect() != null && menu.getPrimaryEffect().is(MobEffects.MOVEMENT_SPEED),
                "confirm should set Speed as the primary power");
        return menu;
    }

    /**
     * A mock player in the level near the Infuser. (Not {@code makeMockServerPlayerInLevel}: on NeoForge its login
     * fails because Accessories sends a payload the fake connection hasn't negotiated.)
     */
    private static Player playerNear(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 pos = helper.absoluteVec(Vec3.atBottomCenterOf(INFUSER.offset(0, -1, 3)));
        player.moveTo(pos.x, pos.y, pos.z);
        helper.getLevel().addFreshEntity(player);
        return player;
    }

    private static void removePlayer(Player player) {
        player.discard();
    }

    /** A server player with a dummy connection that isn't in the level or player list. */
    private static ServerPlayer detachedServerPlayer(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(
                new GameProfile(UUID.randomUUID(), "test-mock-player"), false);
        ServerPlayer player = new ServerPlayer(server, helper.getLevel(), cookie.gameProfile(),
                cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        new ServerGamePacketListenerImpl(server, connection, player, cookie);
        return player;
    }

    private static InfuserMenu menuIn(Level level, Player player) {
        return new InfuserMenu(0, player.getInventory(), new SimpleContainerData(3),
                ContainerLevelAccess.create(level, BlockPos.ZERO));
    }

    private static ServerLevel end(GameTestHelper helper) {
        ServerLevel end = helper.getLevel().getServer().getLevel(Level.END);
        helper.assertTrue(end != null, "the test server should have an End level");
        return end;
    }

    @GameTest
    public static void infuserBlockBasics(GameTestHelper helper) {
        InfuserBlockEntity infuser = placeInfuser(helper, false);
        BlockState state = helper.getBlockState(INFUSER);
        helper.assertTrue(infuser.getType() == Infusers.BLOCK_ENTITY.get(), "the Infuser should use its own block entity type");
        helper.assertTrue(state.getLightEmission() == 15, "the Infuser should give light level 15");
        CompoundTag saved = infuser.saveWithId(helper.getLevel().registryAccess());
        helper.assertTrue("someonesperfectend:infuser".equals(saved.getString("id")), "saved id was " + saved.getString("id"));
        helper.assertTrue(Block.getDrops(state, helper.getLevel(), helper.absolutePos(INFUSER), infuser).stream()
                .anyMatch(stack -> stack.is(ModBlocks.INFUSER.get().asItem())), "the Infuser should drop itself");
        helper.assertTrue(helper.getLevel().getRecipeManager().getRecipes().stream()
                .noneMatch(recipe -> recipe.value().getResultItem(helper.getLevel().registryAccess())
                        .is(ModBlocks.INFUSER.get().asItem())), "the Infuser should not be craftable");
        helper.succeed();
    }

    @GameTest(timeoutTicks = 300)
    public static void tierOnePyramidGivesSpeed(GameTestHelper helper) {
        InfuserBlockEntity infuser = placeInfuser(helper, true);
        Player player = playerNear(helper);
        InfuserMenu menu = confirmSpeed(helper, infuser, player);
        helper.succeedWhen(() -> {
            helper.assertTrue(menu.getLevels() == 1, "the iron pyramid should be tier 1, was " + menu.getLevels());
            helper.assertTrue(player.hasEffect(MobEffects.MOVEMENT_SPEED), "a nearby player should get Speed");
            // Tier 1: (9 + 2 * 1) * 20 ticks.
            helper.assertTrue(player.getEffect(MobEffects.MOVEMENT_SPEED).getDuration() <= 220, "Speed should last 220 ticks");
            removePlayer(player);
            helper.setBlock(INFUSER, Blocks.AIR);
        });
    }

    @GameTest(timeoutTicks = 300, batch = NEGATIVE_BATCH)
    public static void noPyramidGivesNothing(GameTestHelper helper) {
        InfuserBlockEntity infuser = placeInfuser(helper, false);
        Player player = playerNear(helper);
        InfuserMenu menu = confirmSpeed(helper, infuser, player);
        helper.runAtTickTime(200, () -> {
            removePlayer(player);
            helper.assertTrue(menu.getLevels() == 0, "without a pyramid the tier should be 0");
            helper.assertFalse(player.hasEffect(MobEffects.MOVEMENT_SPEED), "without a pyramid there should be no Speed");
            helper.succeed();
        });
    }

    @GameTest(timeoutTicks = 300, batch = NEGATIVE_BATCH)
    public static void blockedSkyGivesNothing(GameTestHelper helper) {
        InfuserBlockEntity infuser = placeInfuser(helper, true);
        helper.setBlock(INFUSER.above(3), Blocks.STONE);
        Player player = playerNear(helper);
        confirmSpeed(helper, infuser, player);
        helper.runAtTickTime(200, () -> {
            removePlayer(player);
            helper.assertFalse(player.hasEffect(MobEffects.MOVEMENT_SPEED), "a covered Infuser should give no Speed");
            helper.succeed();
        });
    }

    @GameTest
    public static void infusionOnlyWorksInTheEnd(GameTestHelper helper) {
        ServerLevel end = end(helper);
        helper.assertFalse(InfuserMenu.isInfusionEnabled(helper.getLevel()), "infusion should be off in the Overworld");
        helper.assertTrue(InfuserMenu.isInfusionEnabled(end), "infusion should be on in the End");
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        helper.assertFalse(InfuserMenu.isInfusionEnabled(nether), "infusion should be off in the Nether");

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        InfuserMenu overworldMenu = menuIn(helper.getLevel(), player);
        helper.assertFalse(overworldMenu.isInfusionEnabled(), "an Overworld menu should lock the infusion slot");
        helper.assertFalse(overworldMenu.getInfusionSlot().mayPlace(pickaxe), "a locked infusion slot should refuse items");

        InfuserMenu endMenu = menuIn(end, player);
        helper.assertTrue(endMenu.isInfusionEnabled(), "an End menu should enable the infusion slot");
        for (ItemStack ok : new ItemStack[] {pickaxe, new ItemStack(Items.IRON_BOOTS),
                new ItemStack(Items.NETHERITE_CHESTPLATE), new ItemStack(Items.DIAMOND_SWORD),
                new ItemStack(Items.IRON_AXE), new ItemStack(Items.STONE_SHOVEL)}) {
            helper.assertTrue(endMenu.getInfusionSlot().mayPlace(ok), "the infusion slot should accept " + ok);
        }
        for (ItemStack bad : new ItemStack[] {new ItemStack(Items.DIRT), new ItemStack(Items.IRON_HELMET),
                new ItemStack(Items.IRON_LEGGINGS), new ItemStack(Items.IRON_INGOT), new ItemStack(Items.BOW)}) {
            helper.assertFalse(endMenu.getInfusionSlot().mayPlace(bad), "the infusion slot should refuse " + bad);
        }
        helper.succeed();
    }

    @GameTest
    public static void infusionItemDisablesConfirmAndIsReturned(GameTestHelper helper) {
        // Vanilla's clearContainer only returns items to a ServerPlayer.
        ServerPlayer player = detachedServerPlayer(helper);
        InfuserMenu menu = menuIn(end(helper), player);
        menu.getSlot(InfuserMenu.PAYMENT_SLOT).set(new ItemStack(Items.IRON_INGOT));
        menu.getInfusionSlot().set(new ItemStack(Items.DIAMOND_PICKAXE));
        helper.assertFalse(menu.hasPayment(), "confirm should be disabled with an item in the infusion slot");
        menu.updateEffects(Optional.of(MobEffects.MOVEMENT_SPEED), Optional.empty());
        helper.assertTrue(menu.getSlot(InfuserMenu.PAYMENT_SLOT).getItem().is(Items.IRON_INGOT),
                "confirm with an infusion item should not consume the payment");
        helper.assertTrue(menu.getPrimaryEffect() == null, "confirm with an infusion item should not set beacon powers");

        menu.getSlot(InfuserMenu.PAYMENT_SLOT).set(ItemStack.EMPTY);
        menu.removed(player);
        helper.assertTrue(menu.getInfusionSlot().getItem().isEmpty(), "closing should empty the infusion slot");
        helper.assertTrue(player.getInventory().contains(new ItemStack(Items.DIAMOND_PICKAXE)),
                "closing should return the infusion item to the player");
        helper.succeed();
    }

    @GameTest
    public static void shiftClickMovesItemsIntoInfuserSlots(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        InfuserMenu endMenu = menuIn(end(helper), player);
        int firstInvSlot = InfuserMenu.INV_SLOT_START;
        endMenu.getSlot(firstInvSlot).set(new ItemStack(Items.DIAMOND_PICKAXE));
        endMenu.quickMoveStack(player, firstInvSlot);
        helper.assertTrue(endMenu.getInfusionSlot().getItem().is(Items.DIAMOND_PICKAXE),
                "shift-clicking a tool in the End should move it into the infusion slot");
        endMenu.getSlot(firstInvSlot).set(new ItemStack(Items.IRON_INGOT));
        endMenu.quickMoveStack(player, firstInvSlot);
        helper.assertTrue(endMenu.getSlot(InfuserMenu.PAYMENT_SLOT).getItem().is(Items.IRON_INGOT),
                "shift-clicking a payment item should move it into the payment slot");
        endMenu.quickMoveStack(player, InfuserMenu.INFUSION_SLOT);
        helper.assertTrue(endMenu.getInfusionSlot().getItem().isEmpty(),
                "shift-clicking the infusion slot should move the item back to the inventory");

        Player other = helper.makeMockPlayer(GameType.SURVIVAL);
        InfuserMenu overworldMenu = menuIn(helper.getLevel(), other);
        overworldMenu.getSlot(firstInvSlot).set(new ItemStack(Items.DIAMOND_PICKAXE));
        overworldMenu.quickMoveStack(other, firstInvSlot);
        helper.assertTrue(overworldMenu.getInfusionSlot().getItem().isEmpty(),
                "shift-clicking outside the End should not fill the locked infusion slot");
        helper.succeed();
    }
}
