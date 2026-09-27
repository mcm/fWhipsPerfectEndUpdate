package io.mcmaster.someonesperfectend.item;

import dev.architectury.core.item.ArchitecturyBucketItem;
import java.util.function.Supplier;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Bucket of Oasis Water. Behaves like a water bucket, including waterlogging: a block state can only hold
 * vanilla water, so emptying it into a waterloggable block waterlogs that block with water.
 */
public class OasisWaterBucketItem extends ArchitecturyBucketItem {
    public OasisWaterBucketItem(Supplier<? extends Fluid> fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = hit.getBlockPos();
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof SimpleWaterloggedBlock container
                    && level.mayInteract(player, pos)
                    && player.mayUseItemAt(pos, hit.getDirection(), stack)
                    && container.canPlaceLiquid(player, level, pos, state, Fluids.WATER)) {
                container.placeLiquid(level, pos, state, Fluids.WATER.getSource(false));
                playEmptySound(player, level, pos);
                if (player instanceof ServerPlayer serverPlayer) {
                    CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, pos, stack);
                }
                player.awardStat(Stats.ITEM_USED.get(this));
                return InteractionResultHolder.sidedSuccess(getEmptySuccessItem(stack, player), level.isClientSide());
            }
        }
        return super.use(level, player, hand);
    }
}
