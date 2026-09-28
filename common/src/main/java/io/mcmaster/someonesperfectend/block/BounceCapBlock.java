package io.mcmaster.someonesperfectend.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A bouncy End mushroom: a full-width cap on a short stem. Living entities standing on it get Slow Falling,
 * jumping from it launches them about {@link #APEX_BLOCKS} blocks up (sneaking suppresses the bounce, see
 * {@code LivingEntityMixin}), and landing on it causes no fall damage. It survives only on
 * {@link ModBlockTags#END_PLANT_SOIL} and never spreads.
 */
public class BounceCapBlock extends Block {
    public static final MapCodec<BounceCapBlock> CODEC = simpleCodec(BounceCapBlock::new);

    /** Slow Falling duration, refreshed every tick an entity stands on the cap. */
    public static final int SLOW_FALLING_TICKS = 80;
    /** Nominal jump apex above the cap's top for an entity without Jump Boost. */
    public static final double APEX_BLOCKS = 12.0;
    /**
     * Upward velocity of a bounce. With vanilla entity physics ({@code vy = (vy - 0.08) * 0.98} per tick) a
     * launch at 1.49 blocks/tick peaks about 12.0 blocks up (vanilla's 0.42 jump peaks about 1.25).
     */
    public static final double BOUNCE_VELOCITY = 1.49;

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0.0, 6.0, 0.0, 16.0, 16.0, 16.0),
            Block.box(5.0, 0.0, 5.0, 11.0, 6.0, 11.0));

    public BounceCapBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).is(ModBlockTags.END_PLANT_SOIL);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.DOWN && !state.canSurvive(level, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        // Like a slime block, but without the landing bounce.
        entity.causeFallDamage(fallDistance, 0.0F, level.damageSources().fall());
    }

    /** Whether {@code entity} is standing on a bounce cap. */
    public static boolean isStandingOn(LivingEntity entity) {
        return entity.onGround() && entity.level().getBlockState(entity.getOnPos()).getBlock() instanceof BounceCapBlock;
    }

    /** Called every tick for every living entity (server side): refreshes Slow Falling while standing on a cap. */
    public static void tickEntity(LivingEntity entity) {
        if (!entity.level().isClientSide() && isStandingOn(entity)) {
            entity.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, SLOW_FALLING_TICKS, 0, false, false, true));
        }
    }

    /** Called after a living entity jumps (both sides): launches it if it jumped from a cap without sneaking. */
    public static void afterJump(LivingEntity entity) {
        if (!entity.isShiftKeyDown() && isStandingOn(entity)) {
            Vec3 motion = entity.getDeltaMovement();
            entity.setDeltaMovement(motion.x, BOUNCE_VELOCITY + entity.getJumpBoostPower(), motion.z);
        }
    }
}
