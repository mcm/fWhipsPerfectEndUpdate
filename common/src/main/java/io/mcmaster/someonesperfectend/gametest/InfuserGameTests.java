package io.mcmaster.someonesperfectend.gametest;

import com.mojang.authlib.GameProfile;
import io.mcmaster.someonesperfectend.block.ModBlocks;
import io.mcmaster.someonesperfectend.infuser.InfuserBlockEntity;
import io.mcmaster.someonesperfectend.infuser.InfuserMenu;
import io.mcmaster.someonesperfectend.infuser.Infusers;
import io.mcmaster.someonesperfectend.mixin.BeaconBlockEntityAccessor;
import io.mcmaster.someonesperfectend.infuser.Infusion;
import io.mcmaster.someonesperfectend.infuser.InfusionEffect;
import io.mcmaster.someonesperfectend.infuser.InfusionRules;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.inventory.BeaconMenu;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
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

    /** A menu in the level whose beacon data reports the given pyramid tier (and Haste as the beacon power). */
    private static InfuserMenu menuAtTier(Level level, Player player, int tier) {
        SimpleContainerData data = new SimpleContainerData(3);
        data.set(0, tier);
        data.set(1, BeaconMenu.encodeEffect(MobEffects.DIG_SPEED));
        return new InfuserMenu(0, player.getInventory(), data, ContainerLevelAccess.create(level, BlockPos.ZERO));
    }

    /** Puts the item in the infusion slot and one payment item in the payment slot, then confirms the effect. */
    private static ItemStack confirmInfusion(InfuserMenu menu, ItemStack item, Holder<MobEffect> effect) {
        if (!menu.getInfusionSlot().hasItem()) {
            menu.getInfusionSlot().set(item);
        }
        menu.getSlot(InfuserMenu.PAYMENT_SLOT).set(new ItemStack(Items.IRON_INGOT));
        menu.updateEffects(Optional.of(effect), Optional.empty());
        return menu.getInfusionSlot().getItem();
    }

    private static void assertInfusion(GameTestHelper helper, ItemStack stack, InfusionEffect effect, int amplifier,
            int ticks) {
        Infusion infusion = Infusion.get(stack);
        helper.assertTrue(infusion != null, stack + " should be infused");
        helper.assertTrue(infusion.effect().is(effect.holder()) && infusion.amplifier() == amplifier
                        && infusion.remainingTicks() == ticks,
                "expected " + effect + " amplifier " + amplifier + " with " + ticks + " ticks, was " + infusion);
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

    @GameTest
    public static void confirmingWithOneIronIngotInfusesAPickaxe(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        InfuserMenu menu = menuAtTier(end(helper), player, 1);
        menu.getInfusionSlot().set(new ItemStack(Items.DIAMOND_PICKAXE));
        menu.getSlot(InfuserMenu.PAYMENT_SLOT).set(new ItemStack(Items.IRON_INGOT));
        helper.assertTrue(menu.infusionEffects().equals(java.util.List.of(InfusionEffect.HASTE)),
                "a pickaxe should only offer Haste, offered " + menu.infusionEffects());
        helper.assertTrue(menu.canInfuse(InfusionEffect.HASTE), "confirm should be enabled for Haste");
        helper.assertFalse(menu.canInfuse(InfusionEffect.SPEED), "a pickaxe can't take Speed");

        // A wrong effect consumes nothing.
        menu.updateEffects(Optional.of(MobEffects.MOVEMENT_SPEED), Optional.empty());
        helper.assertTrue(menu.getSlot(InfuserMenu.PAYMENT_SLOT).getItem().is(Items.IRON_INGOT),
                "confirming an effect the item can't take should keep the payment");
        helper.assertTrue(Infusion.get(menu.getInfusionSlot().getItem()) == null, "the pickaxe should not be infused");

        menu.updateEffects(Optional.of(MobEffects.DIG_SPEED), Optional.empty());
        helper.assertTrue(menu.getSlot(InfuserMenu.PAYMENT_SLOT).getItem().isEmpty(),
                "confirming should consume the one iron ingot");
        assertInfusion(helper, menu.getInfusionSlot().getItem(), InfusionEffect.HASTE, 0, 220);
        helper.assertTrue(menu.getLevels() == 1 && menu.getPrimaryEffect() != null
                        && menu.getPrimaryEffect().is(MobEffects.DIG_SPEED) && menu.getSecondaryEffect() == null,
                "infusing should not change the beacon powers");

        // A second charge adds another (9 + 2 * 1) * 20 ticks.
        confirmInfusion(menu, ItemStack.EMPTY, MobEffects.DIG_SPEED);
        assertInfusion(helper, menu.getInfusionSlot().getItem(), InfusionEffect.HASTE, 0, 440);
        helper.succeed();
    }

    @GameTest
    public static void tierSetsTheLevelAtTheFirstInfusion(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ServerLevel end = end(helper);
        ItemStack boots = confirmInfusion(menuAtTier(end, player, 4), new ItemStack(Items.IRON_BOOTS),
                MobEffects.MOVEMENT_SPEED);
        assertInfusion(helper, boots, InfusionEffect.SPEED, 1, 340);
        ItemStack chestplate = confirmInfusion(menuAtTier(end, player, 4), new ItemStack(Items.DIAMOND_CHESTPLATE),
                MobEffects.REGENERATION);
        assertInfusion(helper, chestplate, InfusionEffect.REGENERATION, 0, 340);

        // Topping up at tier 1 keeps level II and adds a tier-1 charge; another effect is refused.
        InfuserMenu tierOne = menuAtTier(end, player, 1);
        tierOne.getInfusionSlot().set(boots);
        helper.assertFalse(tierOne.canSelectInfusion(InfusionEffect.JUMP_BOOST), "Jump Boost needs tier 2");
        confirmInfusion(tierOne, boots, MobEffects.MOVEMENT_SPEED);
        assertInfusion(helper, tierOne.getInfusionSlot().getItem(), InfusionEffect.SPEED, 1, 560);
        InfuserMenu tierTwo = menuAtTier(end, player, 2);
        tierTwo.getInfusionSlot().set(tierOne.getInfusionSlot().getItem());
        confirmInfusion(tierTwo, ItemStack.EMPTY, MobEffects.JUMP);
        assertInfusion(helper, tierTwo.getInfusionSlot().getItem(), InfusionEffect.SPEED, 1, 560);
        helper.assertTrue(tierTwo.getSlot(InfuserMenu.PAYMENT_SLOT).getItem().is(Items.IRON_INGOT),
                "a refused re-infusion should keep the payment");

        // An axe takes Haste or Strength, one at a time; Strength needs tier 3.
        InfuserMenu tierTwoAxe = menuAtTier(end, player, 2);
        tierTwoAxe.getInfusionSlot().set(new ItemStack(Items.IRON_AXE));
        helper.assertFalse(tierTwoAxe.canSelectInfusion(InfusionEffect.STRENGTH), "Strength needs tier 3");
        InfuserMenu tierThree = menuAtTier(end, player, 3);
        ItemStack axe = confirmInfusion(tierThree, new ItemStack(Items.IRON_AXE), MobEffects.DAMAGE_BOOST);
        assertInfusion(helper, axe, InfusionEffect.STRENGTH, 0, 300);
        helper.assertFalse(tierThree.canSelectInfusion(InfusionEffect.HASTE),
                "an axe infused with Strength can't take Haste");
        helper.succeed();
    }

    @GameTest
    public static void capDisablesConfirm(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        InfuserMenu menu = menuAtTier(end(helper), player, 4);
        ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        int cap = InfusionRules.capTicks(4);
        pickaxe.set(Infusers.INFUSION.get(), new Infusion(MobEffects.DIG_SPEED, 1, cap - 100));
        confirmInfusion(menu, pickaxe, MobEffects.DIG_SPEED);
        assertInfusion(helper, menu.getInfusionSlot().getItem(), InfusionEffect.HASTE, 1, cap);
        menu.getSlot(InfuserMenu.PAYMENT_SLOT).set(new ItemStack(Items.IRON_INGOT));
        helper.assertFalse(menu.canInfuse(InfusionEffect.HASTE), "confirm should be disabled at the cap");
        menu.updateEffects(Optional.of(MobEffects.DIG_SPEED), Optional.empty());
        helper.assertTrue(menu.getSlot(InfuserMenu.PAYMENT_SLOT).getItem().is(Items.IRON_INGOT),
                "confirm at the cap should keep the payment");
        helper.succeed();
    }

    @GameTest
    public static void infusingOnlyWorksInTheEnd(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        InfuserMenu menu = menuAtTier(helper.getLevel(), player, 4);
        // Bypass the locked slot to check that confirm itself refuses outside the End.
        ItemStack pickaxe = confirmInfusion(menu, new ItemStack(Items.DIAMOND_PICKAXE), MobEffects.DIG_SPEED);
        helper.assertTrue(Infusion.get(pickaxe) == null, "infusing should not work outside the End");
        helper.assertTrue(menu.getSlot(InfuserMenu.PAYMENT_SLOT).getItem().is(Items.IRON_INGOT),
                "confirm outside the End should keep the payment");
        helper.succeed();
    }

    @GameTest
    public static void infusionComponentSavesAndSyncs(GameTestHelper helper) {
        RegistryAccess registries = helper.getLevel().registryAccess();
        ItemStack stack = new ItemStack(Items.NETHERITE_BOOTS);
        Infusion infusion = new Infusion(MobEffects.MOVEMENT_SPEED, 1, 12345);
        stack.set(Infusers.INFUSION.get(), infusion);

        Tag saved = stack.save(registries);
        ItemStack loaded = ItemStack.parse(registries, saved).orElseThrow();
        helper.assertTrue(infusion.equals(Infusion.get(loaded)), "the infusion should survive save/load, was " + saved);
        helper.assertTrue(saved.toString().contains("someonesperfectend:infusion")
                && saved.toString().contains("remaining_ticks"), "saved NBT was " + saved);

        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
        ItemStack.STREAM_CODEC.encode(buf, stack);
        ItemStack synced = ItemStack.STREAM_CODEC.decode(buf);
        helper.assertTrue(infusion.equals(Infusion.get(synced)), "the infusion should sync to clients");

        Component tooltip = infusion.tooltip();
        helper.assertTrue(tooltip.getContents() instanceof TranslatableContents contents
                        && contents.getKey().equals(Infusion.TOOLTIP_KEY) && contents.getArgs().length == 2
                        && "10:17".equals(contents.getArgs()[1]),
                "tooltip should be 'Infused: <effect level> — 10:17 remaining', was " + tooltip);
        helper.succeed();
    }
}
