package io.mcmaster.someonesperfectend.item;

import dev.architectury.core.item.ArchitecturyBucketItem;
import io.mcmaster.someonesperfectend.fluid.ModFluids;
import io.mcmaster.someonesperfectend.fluid.OasisLogging;
import java.util.function.Supplier;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Bucket of Oasis Water. Behaves like a water bucket. Emptied into a waterloggable block it Oasis-logs the block
 * ({@code waterlogged=true, oasis_logged=true}, see {@link OasisLogging}), so the block holds Oasis Water. In an
 * {@code ultraWarm} dimension (the Nether) it evaporates like water and never waterlogs anything.
 */
public class OasisWaterBucketItem extends ArchitecturyBucketItem {
    public OasisWaterBucketItem(Supplier<? extends Fluid> fluid, Properties properties) {
        super(fluid, properties);
    }

    /**
     * Vanilla (and so Fabric) only empties a bucket into the clicked block for vanilla water, so aim at a
     * waterloggable block here. Everything else is the vanilla bucket.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = hit.getBlockPos();
            Direction direction = hit.getDirection();
            if (canHoldOasisWater(player, level, pos, level.getBlockState(pos))
                    && level.mayInteract(player, pos)
                    && player.mayUseItemAt(pos.relative(direction), direction, stack)) {
                if (!emptyContents(player, level, pos, hit)) {
                    return InteractionResultHolder.fail(stack);
                }
                if (player instanceof ServerPlayer serverPlayer) {
                    CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, pos, stack);
                }
                player.awardStat(Stats.ITEM_USED.get(this));
                ItemStack result = ItemUtils.createFilledResult(stack, player, getEmptySuccessItem(stack, player));
                return InteractionResultHolder.sidedSuccess(result, level.isClientSide());
            }
        }
        return super.use(level, player, hand);
    }

    /**
     * Empties the bucket at {@code pos} (player use and dispensers). A waterloggable block is Oasis-logged, or, in an
     * {@code ultraWarm} dimension, the water evaporates. Anything else is the vanilla bucket, which also evaporates
     * Oasis Water in the Nether because it is in {@code #minecraft:water}.
     */
    @Override
    public boolean emptyContents(@Nullable Player player, Level level, BlockPos pos, @Nullable BlockHitResult hit) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof LiquidBlockContainer container && canHoldOasisWater(player, level, pos, state)) {
            if (level.dimensionType().ultraWarm()) {
                evaporate(player, level, pos);
            } else {
                OasisLogging.placeOasisWater(level, pos, state, container);
                playEmptySound(player, level, pos);
            }
            return true;
        }
        return super.emptyContents(player, level, pos, hit);
    }

    private static boolean canHoldOasisWater(@Nullable Player player, Level level, BlockPos pos, BlockState state) {
        return state.getBlock() instanceof LiquidBlockContainer container
                && container.canPlaceLiquid(player, level, pos, state, ModFluids.OASIS_WATER.get());
    }

    /** The vanilla water bucket's sound and smoke in the Nether. */
    private static void evaporate(@Nullable Player player, Level level, BlockPos pos) {
        level.playSound(player, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F,
                2.6F + (level.random.nextFloat() - level.random.nextFloat()) * 0.8F);
        for (int i = 0; i < 8; i++) {
            level.addParticle(ParticleTypes.LARGE_SMOKE, pos.getX() + Math.random(), pos.getY() + Math.random(),
                    pos.getZ() + Math.random(), 0.0, 0.0, 0.0);
        }
    }

    /**
     * The vanilla filled-bucket dispenser behavior, calling {@link #emptyContents(Player, Level, BlockPos,
     * BlockHitResult)} on every loader (NeoForge's own behavior calls a different overload).
     */
    public static DispenseItemBehavior dispenseBehavior() {
        return new DefaultDispenseItemBehavior() {
            private final DefaultDispenseItemBehavior fallback = new DefaultDispenseItemBehavior();

            @Override
            public ItemStack execute(BlockSource source, ItemStack stack) {
                OasisWaterBucketItem bucket = (OasisWaterBucketItem) stack.getItem();
                BlockPos pos = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
                Level level = source.level();
                if (bucket.emptyContents(null, level, pos, null)) {
                    bucket.checkExtraContent(null, level, stack, pos);
                    return consumeWithRemainder(source, stack, new ItemStack(Items.BUCKET));
                }
                return fallback.dispense(source, stack);
            }
        };
    }
}
