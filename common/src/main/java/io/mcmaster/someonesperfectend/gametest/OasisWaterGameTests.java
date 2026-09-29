package io.mcmaster.someonesperfectend.gametest;

import com.mojang.authlib.GameProfile;
import io.mcmaster.someonesperfectend.SomeonesPerfectEnd;
import io.mcmaster.someonesperfectend.alchemy.OasisPotions;
import io.mcmaster.someonesperfectend.fluid.ModFluids;
import io.mcmaster.someonesperfectend.fluid.OasisLogging;
import io.mcmaster.someonesperfectend.fluid.OasisWaterCauldronBlock;
import io.mcmaster.someonesperfectend.fluid.OasisWaterFluid;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BannerPatterns;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class OasisWaterGameTests {
    private OasisWaterGameTests() {}

    private static BlockState oasisSource() {
        return ModFluids.OASIS_WATER.get().defaultFluidState().createLegacyBlock();
    }

    /** Fills a box (inclusive, relative coordinates). */
    private static void fill(GameTestHelper helper, int x0, int y0, int z0, int x1, int y1, int z1, Block block) {
        for (BlockPos pos : BlockPos.betweenClosed(x0, y0, z0, x1, y1, z1)) {
            helper.setBlock(pos, block);
        }
    }

    /**
     * A one-block-wide channel along x at y=1, z=1, from x=1 to x=length, with a stone floor and walls, so
     * fluids placed in it can't leave.
     */
    private static void channel(GameTestHelper helper, int length, int height) {
        fill(helper, 0, 0, 0, length + 1, height, 2, Blocks.STONE);
        fill(helper, 1, 1, 1, length, height, 1, Blocks.AIR);
    }

    private static void assertOasisSource(GameTestHelper helper, BlockPos pos) {
        FluidState fluid = helper.getLevel().getFluidState(helper.absolutePos(pos));
        helper.assertTrue(
                OasisWaterFluid.isOasisWater(fluid) && fluid.isSource(),
                "expected an Oasis Water source at " + pos + ", found " + fluid);
    }

    /** A survival mock player standing at the relative position, looking straight down. */
    private static Player playerLookingDown(GameTestHelper helper, Vec3 relative, ItemStack held) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 pos = helper.absoluteVec(relative);
        player.moveTo(pos.x, pos.y, pos.z, 0.0F, 90.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        return player;
    }

    @GameTest
    public static void bucketPlacesSource(GameTestHelper helper) {
        channel(helper, 1, 1);
        Player player =
                playerLookingDown(helper, new Vec3(1.5, 1.0, 1.5), new ItemStack(ModFluids.OASIS_WATER_BUCKET.get()));
        ItemStack result = player.getMainHandItem()
                .use(helper.getLevel(), player, InteractionHand.MAIN_HAND)
                .getObject();
        assertOasisSource(helper, new BlockPos(1, 1, 1));
        helper.assertTrue(result.is(Items.BUCKET), "bucket was not emptied: " + result);
        helper.succeed();
    }

    @GameTest
    public static void bucketWaterlogsSlab(GameTestHelper helper) {
        channel(helper, 1, 1);
        BlockPos slab = new BlockPos(1, 1, 1);
        helper.setBlock(slab, Blocks.STONE_SLAB);
        Player player =
                playerLookingDown(helper, new Vec3(1.5, 1.5, 1.5), new ItemStack(ModFluids.OASIS_WATER_BUCKET.get()));
        player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertBlockProperty(slab, BlockStateProperties.WATERLOGGED, true);
        helper.assertBlockProperty(slab, OasisLogging.OASIS_LOGGED, true);
        helper.succeed();
    }

    @GameTest
    public static void twoSourcesFormThird(GameTestHelper helper) {
        channel(helper, 3, 1);
        helper.setBlock(new BlockPos(1, 1, 1), oasisSource());
        helper.setBlock(new BlockPos(3, 1, 1), oasisSource());
        helper.succeedWhen(() -> assertOasisSource(helper, new BlockPos(2, 1, 1)));
    }

    @GameTest
    public static void flowsLikeWater(GameTestHelper helper) {
        // Like water, the flow loses one level per block: 5 blocks from the source it is at level 3.
        channel(helper, 6, 1);
        helper.setBlock(new BlockPos(1, 1, 1), oasisSource());
        helper.succeedWhen(() -> {
            FluidState last = helper.getLevel().getFluidState(helper.absolutePos(new BlockPos(6, 1, 1)));
            helper.assertTrue(
                    OasisWaterFluid.isOasisWater(last) && last.getAmount() == 3,
                    "expected level-3 Oasis Water 5 blocks from the source, found " + last);
        });
    }

    @GameTest
    public static void extinguishesAndDrownsEntities(GameTestHelper helper) {
        channel(helper, 1, 3);
        helper.setBlock(new BlockPos(1, 1, 1), oasisSource());
        helper.setBlock(new BlockPos(1, 2, 1), oasisSource());
        Pig pig = helper.spawn(EntityType.PIG, new BlockPos(1, 1, 1));
        // Without AI the pig doesn't swim up, so it stays under water.
        pig.setNoAi(true);
        pig.setRemainingFireTicks(200);
        helper.succeedWhen(() -> {
            helper.assertTrue(pig.isInWater(), "pig is not in water");
            helper.assertTrue(pig.isEyeInFluid(FluidTags.WATER), "pig's eyes are not under water");
            helper.assertFalse(pig.isOnFire(), "pig is still on fire");
            helper.assertTrue(pig.getAirSupply() < pig.getMaxAirSupply() - 20, "pig is not losing air");
        });
    }

    @GameTest
    public static void hydratesFarmland(GameTestHelper helper) {
        channel(helper, 2, 1);
        BlockPos farmland = new BlockPos(1, 1, 1);
        helper.setBlock(farmland, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 0));
        helper.setBlock(new BlockPos(2, 1, 1), oasisSource());
        BlockPos abs = helper.absolutePos(farmland);
        helper.getLevel().getBlockState(abs).randomTick(helper.getLevel(), abs, helper.getLevel().random);
        helper.assertBlockProperty(farmland, FarmBlock.MOISTURE, 7);
        helper.succeed();
    }

    @GameTest
    public static void glassBottleFillsWithOasisWater(GameTestHelper helper) {
        channel(helper, 1, 1);
        helper.setBlock(new BlockPos(1, 1, 1), oasisSource());
        Player player = playerLookingDown(helper, new Vec3(1.5, 2.0, 1.5), new ItemStack(Items.GLASS_BOTTLE));
        ItemStack result = player.getMainHandItem()
                .use(helper.getLevel(), player, InteractionHand.MAIN_HAND)
                .getObject();
        helper.assertTrue(
                result.is(ModFluids.OASIS_WATER_BOTTLE.get()), "expected an Oasis Water bottle, got " + result);
        helper.succeed();
    }

    // Lava interactions. Each Oasis Water setup is paired with the same setup using vanilla water.

    /** Water flowing up to a lava source turns it into obsidian. */
    private static void waterFlowsToLavaSource(GameTestHelper helper, BlockState water) {
        channel(helper, 4, 1);
        helper.setBlock(new BlockPos(4, 1, 1), Blocks.LAVA);
        helper.setBlock(new BlockPos(1, 1, 1), water);
        helper.succeedWhenBlockPresent(Blocks.OBSIDIAN, new BlockPos(4, 1, 1));
    }

    @GameTest
    public static void oasisFlowNextToLavaSourceGivesObsidian(GameTestHelper helper) {
        waterFlowsToLavaSource(helper, oasisSource());
    }

    @GameTest
    public static void waterFlowNextToLavaSourceGivesObsidian(GameTestHelper helper) {
        waterFlowsToLavaSource(helper, Blocks.WATER.defaultBlockState());
    }

    /** Lava flows along the channel; water next to the flowing lava turns it into {@code expected}. */
    private static void lavaFlowMeetsWater(GameTestHelper helper, BlockState water, Block expected) {
        channel(helper, 4, 1);
        BlockPos lavaFlow = new BlockPos(2, 1, 1);
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.LAVA);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        helper.getLevel()
                                .getFluidState(helper.absolutePos(lavaFlow))
                                .is(FluidTags.LAVA),
                        "lava has not flowed yet"))
                .thenExecute(() -> helper.setBlock(new BlockPos(3, 1, 1), water))
                .thenWaitUntil(() -> helper.assertBlockPresent(expected, lavaFlow))
                .thenSucceed();
    }

    @GameTest(timeoutTicks = 200)
    public static void lavaFlowingIntoOasisWaterGivesEndStone(GameTestHelper helper) {
        lavaFlowMeetsWater(helper, oasisSource(), Blocks.END_STONE);
    }

    @GameTest(timeoutTicks = 200)
    public static void lavaFlowingIntoWaterGivesCobblestone(GameTestHelper helper) {
        lavaFlowMeetsWater(helper, Blocks.WATER.defaultBlockState(), Blocks.COBBLESTONE);
    }

    /** A lava source above a water source flows down into it and turns it into {@code expected}. */
    private static void lavaFallsOntoWater(GameTestHelper helper, BlockState water, Block expected) {
        channel(helper, 1, 3);
        BlockPos pool = new BlockPos(1, 1, 1);
        helper.setBlock(pool, water);
        helper.setBlock(new BlockPos(1, 3, 1), Blocks.LAVA);
        helper.succeedWhenBlockPresent(expected, pool);
    }

    @GameTest(timeoutTicks = 200)
    public static void lavaFlowingDownOntoOasisWaterGivesEndStone(GameTestHelper helper) {
        lavaFallsOntoWater(helper, oasisSource(), Blocks.END_STONE);
    }

    @GameTest(timeoutTicks = 200)
    public static void lavaFlowingDownOntoWaterGivesStone(GameTestHelper helper) {
        lavaFallsOntoWater(helper, Blocks.WATER.defaultBlockState(), Blocks.STONE);
    }

    // Brewing.

    private static Holder<Potion> oasis(String vanilla) {
        return BuiltInRegistries.POTION.getHolderOrThrow(
                ResourceKey.create(Registries.POTION, SomeonesPerfectEnd.id("oasis_" + vanilla)));
    }

    private static PotionContents contents(ItemStack stack) {
        return stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
    }

    private static void assertPotion(GameTestHelper helper, ItemStack stack, Item item, Holder<Potion> potion) {
        helper.assertTrue(
                stack.is(item) && contents(stack).is(potion),
                "expected " + item + " of " + potion.getRegisteredName() + ", got " + stack + " " + contents(stack));
    }

    private static MobEffectInstance onlyEffect(GameTestHelper helper, ItemStack stack) {
        List<MobEffectInstance> effects = new ArrayList<>();
        contents(stack).forEachEffect(effects::add);
        helper.assertTrue(effects.size() == 1, "expected one effect, got " + effects);
        return effects.get(0);
    }

    @GameTest
    public static void oasisBrewingExtendsDurations(GameTestHelper helper) {
        PotionBrewing brewing = helper.getLevel().potionBrewing();
        ItemStack bottle = new ItemStack(ModFluids.OASIS_WATER_BOTTLE.get());
        helper.assertTrue(brewing.hasMix(bottle, new ItemStack(Items.NETHER_WART)), "bottle cannot be brewed");

        ItemStack awkward = brewing.mix(new ItemStack(Items.NETHER_WART), bottle);
        assertPotion(helper, awkward, Items.POTION, oasis("awkward"));

        ItemStack swiftness = brewing.mix(new ItemStack(Items.SUGAR), awkward);
        assertPotion(helper, swiftness, Items.POTION, oasis("swiftness"));
        MobEffectInstance speed = onlyEffect(helper, swiftness);
        helper.assertTrue(
                speed.getEffect().is(MobEffects.MOVEMENT_SPEED) && speed.getDuration() == 3600 * 3 / 2,
                "Oasis swiftness should last 5400 ticks: " + speed);

        ItemStack vanillaSwiftness =
                brewing.mix(new ItemStack(Items.SUGAR), PotionContents.createItemStack(Items.POTION, Potions.AWKWARD));
        helper.assertTrue(
                onlyEffect(helper, vanillaSwiftness).getDuration() == 3600,
                "vanilla swiftness changed: " + vanillaSwiftness);

        ItemStack longSwiftness = brewing.mix(new ItemStack(Items.REDSTONE), swiftness);
        assertPotion(helper, longSwiftness, Items.POTION, oasis("long_swiftness"));
        helper.assertTrue(
                onlyEffect(helper, longSwiftness).getDuration() == 9600 * 3 / 2,
                "Oasis long swiftness should last 14400 ticks");

        ItemStack splash = brewing.mix(new ItemStack(Items.GUNPOWDER), swiftness);
        assertPotion(helper, splash, Items.SPLASH_POTION, oasis("swiftness"));

        // Instant effects are unchanged: healing has no Oasis variant.
        ItemStack healing = brewing.mix(new ItemStack(Items.GLISTERING_MELON_SLICE), awkward);
        assertPotion(helper, healing, Items.POTION, Potions.HEALING);
        helper.assertTrue(OasisPotions.variantOf(Potions.HEALING).isEmpty(), "healing should have no variant");

        // Weakness brews straight from water.
        ItemStack weakness = brewing.mix(new ItemStack(Items.FERMENTED_SPIDER_EYE), bottle);
        assertPotion(helper, weakness, Items.POTION, oasis("weakness"));
        helper.succeed();
    }

    @GameTest(timeoutTicks = 500)
    public static void brewingStandBrewsOasisWater(GameTestHelper helper) {
        BlockPos standPos = new BlockPos(1, 1, 1);
        helper.setBlock(standPos, Blocks.BREWING_STAND);
        BrewingStandBlockEntity stand = helper.getBlockEntity(standPos);
        ItemStack bottle = new ItemStack(ModFluids.OASIS_WATER_BOTTLE.get());
        helper.assertTrue(stand.canPlaceItem(0, bottle), "brewing stand rejects Oasis Water bottles");
        stand.setItem(0, bottle);
        stand.setItem(3, new ItemStack(Items.NETHER_WART));
        stand.setItem(4, new ItemStack(Items.BLAZE_POWDER));
        helper.succeedWhen(() -> assertPotion(helper, stand.getItem(0), Items.POTION, oasis("awkward")));
    }

    // Oasis Water Cauldron.

    private static BlockState oasisCauldron(int level) {
        return ModFluids.OASIS_WATER_CAULDRON.get().defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, level);
    }

    /**
     * Uses {@code held} on the cauldron at {@code pos} (absolute) in {@code level} like a player clicking its top
     * face; returns what the player holds afterwards.
     */
    private static ItemStack useOnCauldron(Level level, BlockPos pos, Player player, ItemStack held) {
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        BlockHitResult hit =
                new BlockHitResult(Vec3.atCenterOf(pos).relative(Direction.UP, 0.5), Direction.UP, pos, false);
        level.getBlockState(pos).useItemOn(held, level, player, InteractionHand.MAIN_HAND, hit);
        return player.getMainHandItem();
    }

    /** Places {@code cauldron} at (1, 1, 1) of the test and uses {@code held} on it as a survival player. */
    private static ItemStack useOnCauldron(GameTestHelper helper, BlockState cauldron, ItemStack held) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, cauldron);
        return useOnCauldron(
                helper.getLevel(), helper.absolutePos(pos), helper.makeMockPlayer(GameType.SURVIVAL), held);
    }

    private static void assertCauldron(GameTestHelper helper, BlockState expected) {
        helper.assertBlockState(new BlockPos(1, 1, 1), state -> state == expected, () -> "expected " + expected);
    }

    private static void assertHolding(GameTestHelper helper, ItemStack held, Item expected) {
        helper.assertTrue(held.is(expected) && held.getCount() == 1, "expected one " + expected + ", got " + held);
    }

    @GameTest
    public static void oasisCauldronHasNoItem(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, oasisCauldron(3));
        BlockPos abs = helper.absolutePos(pos);
        OasisWaterCauldronBlock block = ModFluids.OASIS_WATER_CAULDRON.get();
        helper.assertFalse(
                BuiltInRegistries.ITEM.containsKey(BuiltInRegistries.BLOCK.getKey(block)),
                "the Oasis Water Cauldron should have no item");
        ItemStack picked = block.getCloneItemStack(helper.getLevel(), abs, oasisCauldron(3));
        helper.assertTrue(picked.is(Items.CAULDRON), "pick-block should give a cauldron, got " + picked);
        List<ItemStack> drops =
                Block.getDrops(oasisCauldron(2), helper.getLevel(), abs, null, null, new ItemStack(Items.IRON_PICKAXE));
        helper.assertTrue(
                drops.size() == 1
                        && drops.get(0).is(Items.CAULDRON)
                        && drops.get(0).getCount() == 1,
                "it should drop a cauldron, dropped " + drops);
        helper.succeed();
    }

    @GameTest
    public static void oasisBucketFillsEmptyCauldron(GameTestHelper helper) {
        ItemStack held = useOnCauldron(
                helper, Blocks.CAULDRON.defaultBlockState(), new ItemStack(ModFluids.OASIS_WATER_BUCKET.get()));
        assertCauldron(helper, oasisCauldron(3));
        assertHolding(helper, held, Items.BUCKET);
        helper.succeed();
    }

    @GameTest
    public static void oasisBucketReplacesWaterCauldron(GameTestHelper helper) {
        ItemStack held = useOnCauldron(
                helper,
                Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 1),
                new ItemStack(ModFluids.OASIS_WATER_BUCKET.get()));
        assertCauldron(helper, oasisCauldron(3));
        assertHolding(helper, held, Items.BUCKET);
        helper.succeed();
    }

    @GameTest
    public static void bucketDrainsFullOasisCauldron(GameTestHelper helper) {
        ItemStack held = useOnCauldron(helper, oasisCauldron(3), new ItemStack(Items.BUCKET));
        assertCauldron(helper, Blocks.CAULDRON.defaultBlockState());
        assertHolding(helper, held, ModFluids.OASIS_WATER_BUCKET.get());

        // Like water, a bucket only takes a full cauldron.
        held = useOnCauldron(helper, oasisCauldron(2), new ItemStack(Items.BUCKET));
        assertCauldron(helper, oasisCauldron(2));
        assertHolding(helper, held, Items.BUCKET);
        helper.succeed();
    }

    @GameTest
    public static void oasisBottleFillsCauldron(GameTestHelper helper) {
        ItemStack held = useOnCauldron(
                helper, Blocks.CAULDRON.defaultBlockState(), new ItemStack(ModFluids.OASIS_WATER_BOTTLE.get()));
        assertCauldron(helper, oasisCauldron(1));
        assertHolding(helper, held, Items.GLASS_BOTTLE);

        held = useOnCauldron(helper, oasisCauldron(2), new ItemStack(ModFluids.OASIS_WATER_BOTTLE.get()));
        assertCauldron(helper, oasisCauldron(3));
        assertHolding(helper, held, Items.GLASS_BOTTLE);

        held = useOnCauldron(helper, oasisCauldron(3), new ItemStack(ModFluids.OASIS_WATER_BOTTLE.get()));
        assertCauldron(helper, oasisCauldron(3));
        assertHolding(helper, held, ModFluids.OASIS_WATER_BOTTLE.get());

        // A plain water bottle doesn't mix into Oasis Water.
        held = useOnCauldron(helper, oasisCauldron(1), PotionContents.createItemStack(Items.POTION, Potions.WATER));
        assertCauldron(helper, oasisCauldron(1));
        assertHolding(helper, held, Items.POTION);
        helper.succeed();
    }

    @GameTest
    public static void glassBottleDrainsOasisCauldron(GameTestHelper helper) {
        ItemStack held = useOnCauldron(helper, oasisCauldron(2), new ItemStack(Items.GLASS_BOTTLE));
        assertCauldron(helper, oasisCauldron(1));
        assertHolding(helper, held, ModFluids.OASIS_WATER_BOTTLE.get());

        held = useOnCauldron(helper, oasisCauldron(1), new ItemStack(Items.GLASS_BOTTLE));
        assertCauldron(helper, Blocks.CAULDRON.defaultBlockState());
        assertHolding(helper, held, ModFluids.OASIS_WATER_BOTTLE.get());
        helper.succeed();
    }

    @GameTest
    public static void oasisCauldronWashesLikeWater(GameTestHelper helper) {
        ItemStack helmet = new ItemStack(Items.LEATHER_HELMET);
        helmet.set(DataComponents.DYED_COLOR, new DyedItemColor(0xFF0000, true));
        ItemStack held = useOnCauldron(helper, oasisCauldron(3), helmet);
        helper.assertFalse(held.has(DataComponents.DYED_COLOR), "the helmet was not washed");
        assertCauldron(helper, oasisCauldron(2));

        ItemStack banner = new ItemStack(Items.WHITE_BANNER);
        banner.set(
                DataComponents.BANNER_PATTERNS,
                new BannerPatternLayers.Builder()
                        .add(
                                helper.getLevel()
                                        .registryAccess()
                                        .registryOrThrow(Registries.BANNER_PATTERN)
                                        .getHolderOrThrow(BannerPatterns.STRIPE_TOP),
                                DyeColor.RED)
                        .build());
        held = useOnCauldron(helper, oasisCauldron(2), banner);
        helper.assertTrue(
                held.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY)
                        .layers()
                        .isEmpty(),
                "the banner was not washed: " + held);
        assertCauldron(helper, oasisCauldron(1));

        held = useOnCauldron(helper, oasisCauldron(1), new ItemStack(Items.RED_SHULKER_BOX));
        assertHolding(helper, held, Items.SHULKER_BOX);
        assertCauldron(helper, Blocks.CAULDRON.defaultBlockState());
        helper.succeed();
    }

    @GameTest
    public static void oasisCauldronPutsOutBurningEntities(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, oasisCauldron(3));
        // With AI, so it moves: entities only touch the blocks they are in while moving.
        Chicken chicken =
                helper.spawn(EntityType.CHICKEN, Vec3.atBottomCenterOf(pos).add(0.0, 0.25, 0.0));
        chicken.setRemainingFireTicks(200);
        helper.succeedWhen(() -> {
            helper.assertFalse(chicken.isOnFire(), "the chicken is still on fire");
            assertCauldron(helper, oasisCauldron(2));
        });
    }

    /** A survival mock player in {@code level} ({@code makeMockPlayer} always uses the test level). */
    private static Player playerIn(Level level) {
        return new Player(level, BlockPos.ZERO, 0.0F, new GameProfile(UUID.randomUUID(), "test-mock-player")) {
            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return false;
            }
        };
    }

    /**
     * Fills and drains a cauldron with Oasis Water buckets and bottles in another dimension, at {@code pos} (outside
     * the test area, so it restores the block it replaced).
     */
    private static void cauldronRoundTrip(GameTestHelper helper, ResourceKey<Level> dimension, BlockPos pos) {
        ServerLevel level = helper.getLevel().getServer().getLevel(dimension);
        helper.assertTrue(level != null, "the test server should have " + dimension.location());
        BlockState previous = level.getBlockState(pos);
        try {
            Player player = playerIn(level);
            level.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
            ItemStack held = useOnCauldron(level, pos, player, new ItemStack(ModFluids.OASIS_WATER_BUCKET.get()));
            helper.assertTrue(
                    level.getBlockState(pos) == oasisCauldron(3) && held.is(Items.BUCKET),
                    "bucket fill in " + dimension.location() + ": " + level.getBlockState(pos) + ", " + held);
            held = useOnCauldron(level, pos, player, new ItemStack(Items.GLASS_BOTTLE));
            helper.assertTrue(
                    level.getBlockState(pos) == oasisCauldron(2) && held.is(ModFluids.OASIS_WATER_BOTTLE.get()),
                    "bottle drain in " + dimension.location() + ": " + level.getBlockState(pos) + ", " + held);
            held = useOnCauldron(level, pos, player, held);
            helper.assertTrue(
                    level.getBlockState(pos) == oasisCauldron(3) && held.is(Items.GLASS_BOTTLE),
                    "bottle fill in " + dimension.location() + ": " + level.getBlockState(pos) + ", " + held);
            held = useOnCauldron(level, pos, player, new ItemStack(Items.BUCKET));
            helper.assertTrue(
                    level.getBlockState(pos).is(Blocks.CAULDRON) && held.is(ModFluids.OASIS_WATER_BUCKET.get()),
                    "bucket drain in " + dimension.location() + ": " + level.getBlockState(pos) + ", " + held);
        } finally {
            level.setBlockAndUpdate(pos, previous);
        }
        helper.succeed();
    }

    @GameTest
    public static void oasisCauldronWorksInTheNether(GameTestHelper helper) {
        helper.assertTrue(
                helper.getLevel()
                        .getServer()
                        .getLevel(Level.NETHER)
                        .dimensionType()
                        .ultraWarm(),
                "the Nether should be ultraWarm");
        cauldronRoundTrip(helper, Level.NETHER, new BlockPos(8, 64, -4000));
    }

    @GameTest
    public static void oasisCauldronWorksInTheEnd(GameTestHelper helper) {
        cauldronRoundTrip(helper, Level.END, new BlockPos(8, 64, -4000));
    }
}
