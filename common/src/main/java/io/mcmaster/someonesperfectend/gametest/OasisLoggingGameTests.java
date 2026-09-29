package io.mcmaster.someonesperfectend.gametest;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import io.mcmaster.someonesperfectend.block.ModBlocks;
import io.mcmaster.someonesperfectend.fluid.ModFluids;
import io.mcmaster.someonesperfectend.fluid.OasisLogging;
import io.mcmaster.someonesperfectend.fluid.OasisWaterFluid;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/**
 * Oasis-logged blocks (MOD-45): the state property, fluid state, buckets, dispensers and the Nether. Placing blocks
 * into Oasis Water, flowing out after neighbor updates, lava and bottles (MOD-48).
 */
public final class OasisLoggingGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();

    private OasisLoggingGameTests() {
    }

    // State property.

    @GameTest
    public static void everyWaterloggableBlockHasOasisLogged(GameTestHelper helper) {
        int blocks = 0;
        int waterloggable = 0;
        int states = 0;
        int oasisStates = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            int count = block.getStateDefinition().getPossibleStates().size();
            blocks++;
            states += count;
            BlockState state = block.defaultBlockState();
            boolean hasWaterlogged = state.hasProperty(BlockStateProperties.WATERLOGGED);
            helper.assertTrue(hasWaterlogged == state.hasProperty(OasisLogging.OASIS_LOGGED),
                    BuiltInRegistries.BLOCK.getKey(block) + ": oasis_logged must be present exactly when waterlogged is");
            if (hasWaterlogged) {
                waterloggable++;
                oasisStates += count / 2;
                helper.assertFalse(state.getValue(OasisLogging.OASIS_LOGGED),
                        BuiltInRegistries.BLOCK.getKey(block) + " defaults to oasis_logged=true");
            }
        }
        LOGGER.info("Oasis-logging: {} of {} blocks are waterloggable; block states {} before, {} after (+{})",
                waterloggable, blocks, states - oasisStates, states, oasisStates);
        helper.assertTrue(waterloggable > 100, "expected over 100 waterloggable blocks, found " + waterloggable);
        helper.succeed();
    }

    @GameTest
    public static void onlyOasisLoggedStatesHoldOasisWater(GameTestHelper helper) {
        BlockState dry = Blocks.OAK_SLAB.defaultBlockState();
        BlockState water = dry.setValue(BlockStateProperties.WATERLOGGED, true);
        BlockState oasis = water.setValue(OasisLogging.OASIS_LOGGED, true);
        helper.assertTrue(dry.getFluidState().isEmpty(), "a dry slab holds " + dry.getFluidState());
        helper.assertTrue(water.getFluidState().getType() == Fluids.WATER, "a waterlogged slab holds " + water.getFluidState());
        helper.assertTrue(oasis.getFluidState().getType() == ModFluids.OASIS_WATER.get(),
                "an Oasis-logged slab holds " + oasis.getFluidState());
        helper.assertTrue(Blocks.STONE.defaultBlockState().getFluidState().isEmpty(), "stone holds a fluid");

        // waterlogged=false always clears oasis_logged, and oasis_logged=true implies waterlogged.
        BlockState drained = oasis.setValue(BlockStateProperties.WATERLOGGED, false);
        helper.assertFalse(drained.getValue(OasisLogging.OASIS_LOGGED), "draining kept oasis_logged");
        helper.assertTrue(oasis.cycle(BlockStateProperties.WATERLOGGED) == dry, "cycling waterlogged kept oasis_logged");
        helper.assertTrue(dry.setValue(OasisLogging.OASIS_LOGGED, true) == oasis,
                "oasis_logged=true on a dry slab should waterlog it");
        helper.assertTrue(oasis.setValue(OasisLogging.OASIS_LOGGED, false) == water, "clearing oasis_logged drained it");
        helper.succeed();
    }

    // Buckets.

    /** A survival mock player standing at the relative position, looking straight down. */
    private static Player playerLookingDown(GameTestHelper helper, Vec3 relative, ItemStack held) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 pos = helper.absoluteVec(relative);
        player.moveTo(pos.x, pos.y, pos.z, 0.0F, 90.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        return player;
    }

    private static ItemStack use(GameTestHelper helper, Level level, Player player) {
        return player.getMainHandItem().use(level, player, InteractionHand.MAIN_HAND).getObject();
    }

    private static void assertOasisLogged(GameTestHelper helper, BlockPos pos) {
        BlockState state = helper.getBlockState(pos);
        helper.assertTrue(OasisLogging.isOasisLogged(state), "expected an Oasis-logged block at " + pos + ", found " + state);
        FluidState fluid = helper.getLevel().getFluidState(helper.absolutePos(pos));
        helper.assertTrue(fluid.getType() == ModFluids.OASIS_WATER.get(), "expected Oasis Water, found " + fluid);
    }

    /** Oasis bucket onto the block, then picks the water back up. */
    private static void bucketRoundTrip(GameTestHelper helper, BlockState block) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos.below(), Blocks.STONE);
        helper.setBlock(pos, block);
        Vec3 eye = new Vec3(1.5, 2.2, 1.5);

        ItemStack emptied = use(helper, helper.getLevel(),
                playerLookingDown(helper, eye, new ItemStack(ModFluids.OASIS_WATER_BUCKET.get())));
        helper.assertTrue(emptied.is(Items.BUCKET), "the Oasis Water bucket was not emptied: " + emptied);
        assertOasisLogged(helper, pos);
        helper.assertTrue(helper.getBlockState(pos).is(block.getBlock()), "the block was replaced");

        // What an empty bucket does (using one needs a real ServerPlayer; dispenserOasisLogsAndEmpties uses one).
        BlockPos abs = helper.absolutePos(pos);
        ItemStack filled = ((BucketPickup) block.getBlock()).pickupBlock(null, helper.getLevel(), abs,
                helper.getLevel().getBlockState(abs));
        helper.assertTrue(filled.is(ModFluids.OASIS_WATER_BUCKET.get()), "expected an Oasis Water bucket, got " + filled);
        BlockState after = helper.getBlockState(pos);
        helper.assertTrue(after.is(block.getBlock()) && !after.getValue(BlockStateProperties.WATERLOGGED)
                && !after.getValue(OasisLogging.OASIS_LOGGED), "pickup left " + after);
        helper.assertTrue(after.getFluidState().isEmpty(), "pickup left a fluid: " + after.getFluidState());
        helper.succeed();
    }

    @GameTest
    public static void bucketOasisLogsOakSlab(GameTestHelper helper) {
        bucketRoundTrip(helper, Blocks.OAK_SLAB.defaultBlockState());
    }

    @GameTest
    public static void bucketOasisLogsOakStairs(GameTestHelper helper) {
        bucketRoundTrip(helper, Blocks.OAK_STAIRS.defaultBlockState());
    }

    @GameTest
    public static void bucketOasisLogsOakFence(GameTestHelper helper) {
        bucketRoundTrip(helper, Blocks.OAK_FENCE.defaultBlockState());
    }

    @GameTest
    public static void bucketOasisLogsOakLeaves(GameTestHelper helper) {
        bucketRoundTrip(helper, Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
    }

    @GameTest
    public static void bucketOasisLogsVoidvineTrapdoor(GameTestHelper helper) {
        bucketRoundTrip(helper, ModBlocks.VOIDVINE_TRAPDOOR.get().defaultBlockState());
    }

    @GameTest
    public static void bucketPutsOutCampfireWithOasisWater(GameTestHelper helper) {
        // Campfires override placeLiquid and only know vanilla water: they still go out and hold Oasis Water.
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos.below(), Blocks.STONE);
        helper.setBlock(pos, Blocks.CAMPFIRE);
        use(helper, helper.getLevel(), playerLookingDown(helper, new Vec3(1.5, 2.2, 1.5),
                new ItemStack(ModFluids.OASIS_WATER_BUCKET.get())));
        assertOasisLogged(helper, pos);
        helper.assertBlockProperty(pos, CampfireBlock.LIT, false);
        helper.succeed();
    }

    @GameTest
    public static void dispenserOasisLogsAndEmpties(GameTestHelper helper) {
        BlockPos dispenser = new BlockPos(1, 1, 1);
        BlockPos slab = dispenser.east();
        helper.setBlock(slab, Blocks.OAK_SLAB);
        helper.setBlock(dispenser, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.EAST));
        DispenserBlockEntity entity = (DispenserBlockEntity) helper.getBlockEntity(dispenser);
        entity.setItem(0, new ItemStack(ModFluids.OASIS_WATER_BUCKET.get()));
        helper.setBlock(dispenser.west(), Blocks.REDSTONE_BLOCK);
        helper.startSequence()
                .thenWaitUntil(() -> {
                    assertOasisLogged(helper, slab);
                    helper.assertTrue(entity.getItem(0).is(Items.BUCKET), "dispenser holds " + entity.getItem(0));
                })
                // Pulse again: the empty bucket picks the Oasis Water back up.
                .thenExecute(() -> helper.setBlock(dispenser.west(), Blocks.AIR))
                .thenIdle(2)
                .thenExecute(() -> helper.setBlock(dispenser.west(), Blocks.REDSTONE_BLOCK))
                .thenWaitUntil(() -> {
                    helper.assertTrue(entity.getItem(0).is(ModFluids.OASIS_WATER_BUCKET.get()),
                            "dispenser holds " + entity.getItem(0));
                    helper.assertBlockProperty(slab, BlockStateProperties.WATERLOGGED, false);
                    helper.assertBlockProperty(slab, OasisLogging.OASIS_LOGGED, false);
                })
                .thenSucceed();
    }

    // Oasis-logged blocks in the world.

    @GameTest
    public static void oasisLoggedBlockFlowsIntoOpenedSpace(GameTestHelper helper) {
        // A stone-walled channel: an Oasis-logged slab at x=1, stone at x=2 that is then removed.
        for (BlockPos pos : BlockPos.betweenClosed(0, 0, 0, 3, 1, 2)) {
            helper.setBlock(pos, Blocks.STONE);
        }
        BlockPos slab = new BlockPos(1, 1, 1);
        BlockPos gap = new BlockPos(2, 1, 1);
        helper.setBlock(slab, Blocks.OAK_SLAB.defaultBlockState().setValue(OasisLogging.OASIS_LOGGED, true));
        helper.setBlock(gap, Blocks.AIR);
        helper.succeedWhen(() -> {
            FluidState fluid = helper.getLevel().getFluidState(helper.absolutePos(gap));
            helper.assertTrue(OasisWaterFluid.isOasisWater(fluid), "Oasis Water did not flow out, found " + fluid);
        });
    }

    @GameTest
    public static void breakingOasisLoggedBlockLeavesOasisWater(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos.below(), Blocks.STONE);
        helper.setBlock(pos, Blocks.OAK_FENCE.defaultBlockState().setValue(OasisLogging.OASIS_LOGGED, true));
        helper.getLevel().destroyBlock(helper.absolutePos(pos), false);
        FluidState fluid = helper.getLevel().getFluidState(helper.absolutePos(pos));
        helper.assertTrue(fluid.getType() == ModFluids.OASIS_WATER.get() && fluid.isSource(),
                "expected an Oasis Water source, found " + fluid);
        helper.succeed();
    }

    // Placing blocks into Oasis Water, and Oasis-logged blocks reacting to their neighbors (MOD-48).

    private static BlockState oasisSource() {
        return ModFluids.OASIS_WATER.get().defaultFluidState().createLegacyBlock();
    }

    /** A stone channel along x at y=1, z=1, x=1..length, open at the top. */
    private static void channel(GameTestHelper helper, int length) {
        for (BlockPos pos : BlockPos.betweenClosed(0, 0, 0, length + 1, 1, 2)) {
            helper.setBlock(pos, Blocks.STONE);
        }
        for (BlockPos pos : BlockPos.betweenClosed(1, 1, 1, length, 1, 1)) {
            helper.setBlock(pos, Blocks.AIR);
        }
    }

    /** A survival player places the block item on top of the block below {@code pos}, like a right click. */
    private static void placeBlockItem(GameTestHelper helper, BlockPos pos, Item item) {
        Player player = playerLookingDown(helper, Vec3.atCenterOf(pos).add(0, 1.2, 0), new ItemStack(item));
        BlockPos below = helper.absolutePos(pos.below());
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(below).add(0, 0.5, 0), Direction.UP, below, false);
        InteractionResult result = ((BlockItem) item).place(new BlockPlaceContext(player,
                InteractionHand.MAIN_HAND, player.getMainHandItem(), hit));
        helper.assertTrue(result.consumesAction(), "placing " + item + " failed: " + result);
    }

    @GameTest
    public static void slabPlacedIntoOasisSourceIsOasisLogged(GameTestHelper helper) {
        channel(helper, 1);
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, oasisSource());
        placeBlockItem(helper, pos, Items.OAK_SLAB);
        helper.assertTrue(helper.getBlockState(pos).is(Blocks.OAK_SLAB), "no slab placed: " + helper.getBlockState(pos));
        assertOasisLogged(helper, pos);
        helper.succeed();
    }

    @GameTest
    public static void lanternPlacedIntoOasisSourceIsOasisLogged(GameTestHelper helper) {
        // Lanterns only waterlog in Fluids.WATER, like slabs; a different block class shows the hook is generic.
        channel(helper, 1);
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, oasisSource());
        placeBlockItem(helper, pos, Items.LANTERN);
        helper.assertTrue(helper.getBlockState(pos).is(Blocks.LANTERN), "no lantern placed: " + helper.getBlockState(pos));
        assertOasisLogged(helper, pos);
        helper.succeed();
    }

    @GameTest
    public static void slabPlacedIntoFlowingOasisWaterIsDry(GameTestHelper helper) {
        channel(helper, 1);
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModFluids.FLOWING_OASIS_WATER.get().defaultFluidState()
                .setValue(FlowingFluid.LEVEL, 7).createLegacyBlock());
        placeBlockItem(helper, pos, Items.OAK_SLAB);
        BlockState state = helper.getBlockState(pos);
        helper.assertTrue(state.is(Blocks.OAK_SLAB) && !state.getValue(BlockStateProperties.WATERLOGGED)
                && !state.getValue(OasisLogging.OASIS_LOGGED), "expected a dry slab, found " + state);
        helper.assertTrue(state.getFluidState().isEmpty(), "the slab holds " + state.getFluidState());
        helper.succeed();
    }

    @GameTest
    public static void oasisLoggedStairsFlowOutWhenNeighborRemoved(GameTestHelper helper) {
        channel(helper, 2);
        BlockPos stairs = new BlockPos(1, 1, 1);
        BlockPos side = new BlockPos(2, 1, 1);
        helper.setBlock(side, Blocks.STONE);
        helper.setBlock(stairs, Blocks.OAK_STAIRS.defaultBlockState().setValue(OasisLogging.OASIS_LOGGED, true));
        helper.setBlock(side, Blocks.AIR);
        BlockPos abs = helper.absolutePos(stairs);
        helper.assertTrue(helper.getLevel().getFluidTicks().hasScheduledTick(abs, ModFluids.OASIS_WATER.get()),
                "removing the neighbor did not schedule an Oasis Water tick");
        helper.assertFalse(helper.getLevel().getFluidTicks().hasScheduledTick(abs, Fluids.WATER),
                "removing the neighbor scheduled a vanilla water tick");
        helper.runAfterDelay(10, () -> {
            FluidState fluid = helper.getLevel().getFluidState(helper.absolutePos(side));
            helper.assertTrue(fluid.getType() == ModFluids.FLOWING_OASIS_WATER.get(),
                    "expected flowing Oasis Water, found " + fluid);
            helper.assertTrue(fluid.getType() != Fluids.FLOWING_WATER, "vanilla water flowed out");
            assertOasisLogged(helper, stairs);
            helper.succeed();
        });
    }

    @GameTest(timeoutTicks = 200)
    public static void oasisFlowFromOasisLoggedBlockTurnsLavaIntoEndStone(GameTestHelper helper) {
        // Stairs at x=1, stone at x=2, flowing lava at x=3 from a source at x=4. Removing the stone lets the
        // Oasis Water reach the flowing lava.
        channel(helper, 4);
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.OAK_STAIRS.defaultBlockState().setValue(OasisLogging.OASIS_LOGGED, true));
        helper.setBlock(new BlockPos(2, 1, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(4, 1, 1), Blocks.LAVA);
        BlockPos lavaFlow = new BlockPos(3, 1, 1);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        helper.getLevel().getFluidState(helper.absolutePos(lavaFlow)).is(FluidTags.LAVA),
                        "lava has not flowed yet"))
                .thenExecute(() -> helper.setBlock(new BlockPos(2, 1, 1), Blocks.AIR))
                .thenWaitUntil(() -> helper.assertBlockPresent(Blocks.END_STONE, lavaFlow))
                .thenExecute(() -> {
                    helper.assertBlockNotPresent(Blocks.COBBLESTONE, new BlockPos(2, 1, 1));
                    helper.assertBlockNotPresent(Blocks.STONE, lavaFlow);
                })
                .thenSucceed();
    }

    @GameTest
    public static void glassBottleOnOasisLoggedBlockGivesOasisBottle(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos.below(), Blocks.STONE);
        helper.setBlock(pos, Blocks.OAK_SLAB.defaultBlockState().setValue(OasisLogging.OASIS_LOGGED, true));
        ItemStack result = use(helper, helper.getLevel(),
                playerLookingDown(helper, new Vec3(1.5, 2.2, 1.5), new ItemStack(Items.GLASS_BOTTLE)));
        helper.assertTrue(result.is(ModFluids.OASIS_WATER_BOTTLE.get()), "expected an Oasis Water bottle, got " + result);
        assertOasisLogged(helper, pos);
        helper.succeed();
    }

    // The Nether.

    /** A mock survival player in another level ({@code makeMockPlayer} always uses the test level). */
    private static Player playerIn(Level level, Vec3 pos, ItemStack held) {
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
        player.moveTo(pos.x, pos.y, pos.z, 0.0F, 90.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        return player;
    }

    @GameTest
    public static void netherRefusesOasisWaterlogging(GameTestHelper helper) {
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null && nether.dimensionType().ultraWarm(), "the test server has no ultraWarm Nether");
        // Above the Nether roof, so nothing else is there.
        BlockPos origin = new BlockPos(0, 200, 0);
        BlockPos slab = origin.offset(1, 1, 1);
        BlockPos open = origin.offset(3, 1, 1);
        BlockPos dispensed = origin.offset(5, 1, 1);
        try {
            for (BlockPos pos : BlockPos.betweenClosed(origin, origin.offset(6, 0, 2))) {
                nether.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
            }
            nether.setBlockAndUpdate(slab, Blocks.OAK_SLAB.defaultBlockState());
            nether.setBlockAndUpdate(dispensed, Blocks.OAK_SLAB.defaultBlockState());
            ItemStack bucket = new ItemStack(ModFluids.OASIS_WATER_BUCKET.get());

            // Player on a slab: fizzles, returns an empty bucket.
            ItemStack onSlab = use(helper, nether, playerIn(nether, Vec3.atCenterOf(slab).add(0, 1.2, 0), bucket.copy()));
            helper.assertTrue(onSlab.is(Items.BUCKET), "expected an empty bucket, got " + onSlab);
            helper.assertTrue(nether.getBlockState(slab).equals(Blocks.OAK_SLAB.defaultBlockState()),
                    "the slab was waterlogged in the Nether: " + nether.getBlockState(slab));

            // Player on open ground: no source is placed.
            ItemStack onGround = use(helper, nether, playerIn(nether, Vec3.atCenterOf(open).add(0, 1.2, 0), bucket.copy()));
            helper.assertTrue(onGround.is(Items.BUCKET), "expected an empty bucket, got " + onGround);
            helper.assertTrue(nether.getFluidState(open).isEmpty(), "a fluid was placed in the Nether: " + nether.getFluidState(open));

            // Dispenser path.
            helper.assertTrue(ModFluids.OASIS_WATER_BUCKET.get().emptyContents(null, nether, dispensed, null),
                    "a dispenser should use up the bucket (the water evaporates)");
            helper.assertTrue(nether.getBlockState(dispensed).equals(Blocks.OAK_SLAB.defaultBlockState()),
                    "a dispenser waterlogged a slab in the Nether: " + nether.getBlockState(dispensed));
            helper.succeed();
        } finally {
            for (BlockPos pos : BlockPos.betweenClosed(origin, origin.offset(6, 1, 2))) {
                nether.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
        }
    }
}
