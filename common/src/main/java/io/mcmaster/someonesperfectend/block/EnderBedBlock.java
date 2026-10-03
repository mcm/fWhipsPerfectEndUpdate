package io.mcmaster.someonesperfectend.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.mcmaster.someonesperfectend.SomeonesPerfectEnd;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A vanilla-style two-block bed that only hangs: both halves need a block with a sturdy bottom face directly
 * above them. It renders upside down through a plain block model (no block entity), so both loaders draw it the
 * same way. Explodes like a vanilla bed in the Nether when used outside the Overworld and the End (US-025); in
 * those two, using it stores it as the player's End or Overworld Ender Bed (US-037) and, if the other dimension's
 * bed is stored, lies the player down to travel there (US-038, {@link EnderBedTravel}).
 */
public class EnderBedBlock extends BedBlock {
    public static final MapCodec<EnderBedBlock> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(DyeColor.CODEC.fieldOf("color").forGetter(BedBlock::getColor), propertiesCodec())
                    .apply(instance, EnderBedBlock::new));

    private static final VoxelShape BASE = Block.box(0, 7, 0, 16, 13, 16);
    private static final VoxelShape LEG_NORTH_WEST = Block.box(0, 13, 0, 3, 16, 3);
    private static final VoxelShape LEG_SOUTH_WEST = Block.box(0, 13, 13, 3, 16, 16);
    private static final VoxelShape LEG_NORTH_EAST = Block.box(13, 13, 0, 16, 16, 3);
    private static final VoxelShape LEG_SOUTH_EAST = Block.box(13, 13, 13, 16, 16, 16);
    private static final VoxelShape NORTH_SHAPE = Shapes.or(BASE, LEG_NORTH_WEST, LEG_NORTH_EAST);
    private static final VoxelShape SOUTH_SHAPE = Shapes.or(BASE, LEG_SOUTH_WEST, LEG_SOUTH_EAST);
    private static final VoxelShape WEST_SHAPE = Shapes.or(BASE, LEG_NORTH_WEST, LEG_SOUTH_WEST);
    private static final VoxelShape EAST_SHAPE = Shapes.or(BASE, LEG_NORTH_EAST, LEG_SOUTH_EAST);

    public EnderBedBlock(DyeColor color, Properties properties) {
        super(color, properties);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public MapCodec<BedBlock> codec() {
        return (MapCodec) CODEC;
    }

    /** Whether the block above {@code pos} can hold up a hanging bed half. */
    public static boolean hasSupport(LevelReader level, BlockPos pos) {
        BlockPos above = pos.above();
        return level.getBlockState(above).isFaceSturdy(level, above, Direction.DOWN);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return hasSupport(level, pos);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }
        Level level = context.getLevel();
        BlockPos foot = context.getClickedPos();
        BlockPos head = foot.relative(state.getValue(FACING));
        return hasSupport(level, foot) && hasSupport(level, head) ? state : null;
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos) {
        if (direction == Direction.UP && !canSurvive(state, level, pos)) {
            // Destroyed with drops by Block#updateOrDestroy; the loot table only drops from the head, and the
            // other half then goes too because its neighbour is no longer this bed.
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return EnderBedRules.explodesIn(level.dimension().location())
                    ? InteractionResult.CONSUME
                    : InteractionResult.SUCCESS;
        }
        if (state.getValue(PART) != BedPart.HEAD) {
            pos = pos.relative(state.getValue(FACING));
            state = level.getBlockState(pos);
            if (!state.is(this)) {
                return InteractionResult.CONSUME;
            }
        }
        if (EnderBedRules.explodesIn(level.dimension().location())) {
            explode(level, pos, state);
            return InteractionResult.SUCCESS;
        }
        if (state.getValue(OCCUPIED)) {
            player.displayClientMessage(Component.translatable("block.minecraft.bed.occupied"), true);
            return InteractionResult.SUCCESS;
        }
        storeLocation(level, pos, player);
        if (player instanceof ServerPlayer serverPlayer) {
            EnderBedTravel.tryStart(serverPlayer, pos);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Stores the bed (its head half) as the player's End or Overworld Ender Bed (US-037). Never touches the vanilla
     * respawn point. Travel sleep (US-038) never skips the night or counts toward the sleeping percentage.
     */
    private static void storeLocation(Level level, BlockPos head, Player player) {
        EnderBedLocations.of(player).set(level.dimension(), head);
        String key = level.dimension() == Level.END ? "ender_bed_end_set" : "ender_bed_overworld_set";
        player.displayClientMessage(Component.translatable("message." + SomeonesPerfectEnd.MOD_ID + "." + key), true);
    }

    /** Vanilla's Nether bed explosion: removes both halves and explodes at the head (power 5, with fire). */
    private void explode(Level level, BlockPos head, BlockState state) {
        level.removeBlock(head, false);
        BlockPos foot = head.relative(state.getValue(FACING).getOpposite());
        if (level.getBlockState(foot).is(this)) {
            level.removeBlock(foot, false);
        }
        Vec3 center = head.getCenter();
        level.explode(
                null,
                level.damageSources().badRespawnPointExplosion(center),
                null,
                center,
                5.0F,
                true,
                Level.ExplosionInteraction.BLOCK);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (getConnectedDirection(state).getOpposite()) {
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            default -> EAST_SHAPE;
        };
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return null;
    }
}
