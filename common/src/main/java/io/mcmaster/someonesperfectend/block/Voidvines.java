package io.mcmaster.someonesperfectend.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Shared rules for the Voidvine plant ({@link VoidvineBlock} head, {@link VoidvinePlantBlock} body): a
 * bamboo-thick stalk hanging below Voidsoil that grows downward and bears Voidfruit, like cave vines.
 */
public final class Voidvines {
    public static final BooleanProperty FRUIT = BlockStateProperties.BERRIES;
    /** Bamboo's mature stalk width (3 pixels). */
    public static final VoxelShape SHAPE = Block.box(6.5, 0.0, 6.5, 9.5, 16.0, 9.5);
    /** A vine never grows longer than this many segments. */
    public static final int MAX_LENGTH = 16;
    /** Chance that the head grows one segment on a random tick. */
    public static final double GROW_CHANCE = 0.1;
    /** Chance that a fruitless segment grows fruit on a random tick (about every 11 minutes on average). */
    public static final float FRUIT_CHANCE = 0.1F;
    /** Chance that a newly grown segment already carries fruit. */
    public static final float NEW_SEGMENT_FRUIT_CHANCE = 0.11F;
    public static final int FRUIT_LIGHT = 8;
    /** Vines never grow lower than this many blocks above the bottom of the world (Y=5 in the End). */
    public static final int MIN_HEIGHT_ABOVE_BOTTOM = 5;

    private Voidvines() {
    }

    public static boolean isVine(BlockState state) {
        return state.is(ModBlocks.VOIDVINE_HEAD.get()) || state.is(ModBlocks.VOIDVINE_BODY.get());
    }

    /** Number of vine segments from {@code pos} up to the top of the vine, including {@code pos}. */
    public static int lengthAbove(BlockGetter level, BlockPos pos) {
        int length = 0;
        BlockPos.MutableBlockPos cursor = pos.mutable();
        while (length < MAX_LENGTH + 1 && isVine(level.getBlockState(cursor))) {
            length++;
            cursor.move(Direction.UP);
        }
        return length;
    }

    /** Randomly regrows fruit on a segment that has none. */
    public static void tickFruit(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(FRUIT) && random.nextFloat() < FRUIT_CHANCE) {
            level.setBlock(pos, state.setValue(FRUIT, true), Block.UPDATE_CLIENTS);
        }
    }

    /** Picks the fruit off a segment, like {@code CaveVines#use}. */
    public static InteractionResult harvest(Entity entity, BlockState state, Level level, BlockPos pos) {
        if (!state.getValue(FRUIT)) {
            return InteractionResult.PASS;
        }
        Block.popResource(level, pos, new ItemStack(ModBlocks.VOIDFRUIT.get()));
        float pitch = Mth.randomBetween(level.random, 0.8F, 1.2F);
        level.playSound(null, pos, SoundEvents.CAVE_VINES_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, pitch);
        BlockState picked = state.setValue(FRUIT, false);
        level.setBlock(pos, picked, Block.UPDATE_CLIENTS);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(entity, picked));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
