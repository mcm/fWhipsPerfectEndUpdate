package io.mcmaster.fwhipsperfectendupdate.fluid;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * A cauldron holding Oasis Water, levels 1-3. It behaves like the vanilla water cauldron (washing, putting out
 * burning entities) but is only filled and emptied with Oasis Water buckets and bottles, never by rain or dripstone.
 * It has no item: it picks and drops as a cauldron.
 */
public class OasisWaterCauldronBlock extends LayeredCauldronBlock {
    public static final CauldronInteraction.InteractionMap INTERACTIONS =
            CauldronInteraction.newInteractionMap("fwhipsperfectendupdate:oasis_water");

    /** Empties an Oasis Water bucket into any cauldron, replacing its contents (like vanilla's water bucket). */
    public static final CauldronInteraction FILL_OASIS_WATER =
            (state, level, pos, player, hand, stack) -> CauldronInteraction.emptyBucket(
                    level,
                    pos,
                    player,
                    hand,
                    stack,
                    ModFluids.OASIS_WATER_CAULDRON.get().defaultBlockState().setValue(LEVEL, MAX_FILL_LEVEL),
                    SoundEvents.BUCKET_EMPTY);

    public OasisWaterCauldronBlock(Properties properties) {
        super(Biome.Precipitation.NONE, INTERACTIONS, properties);
    }

    /** Vanilla's cauldron item stands for its filled cauldrons; this one has no item, so pick a cauldron too. */
    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(Items.CAULDRON);
    }

    /**
     * Fills {@link #INTERACTIONS} and adds the Oasis Water bucket and bottle to the vanilla cauldrons. Must run once
     * the vanilla interactions are bootstrapped and the Oasis items are registered.
     */
    public static void registerInteractions() {
        Item bucket = ModFluids.OASIS_WATER_BUCKET.get();
        Item bottle = ModFluids.OASIS_WATER_BOTTLE.get();
        for (CauldronInteraction.InteractionMap vanilla : new CauldronInteraction.InteractionMap[] {
            CauldronInteraction.EMPTY,
            CauldronInteraction.WATER,
            CauldronInteraction.LAVA,
            CauldronInteraction.POWDER_SNOW
        }) {
            vanilla.map().put(bucket, FILL_OASIS_WATER);
        }
        CauldronInteraction.EMPTY
                .map()
                .put(
                        bottle,
                        (state, level, pos, player, hand, stack) -> emptyBottle(
                                ModFluids.OASIS_WATER_CAULDRON.get().defaultBlockState(),
                                level,
                                pos,
                                player,
                                hand,
                                stack));

        Map<Item, CauldronInteraction> map = INTERACTIONS.map();
        // Washing (dyed leather, banners, shulker boxes) and the other buckets, as in a water cauldron. The water
        // bottle, glass bottle and bucket are Oasis-specific below; a water bottle does nothing.
        map.putAll(CauldronInteraction.WATER.map());
        map.remove(Items.POTION);
        map.put(bucket, FILL_OASIS_WATER);
        map.put(
                Items.BUCKET,
                (state, level, pos, player, hand, stack) -> CauldronInteraction.fillBucket(
                        state,
                        level,
                        pos,
                        player,
                        hand,
                        stack,
                        new ItemStack(bucket),
                        s -> s.getValue(LEVEL) == MAX_FILL_LEVEL,
                        SoundEvents.BUCKET_FILL));
        map.put(Items.GLASS_BOTTLE, (state, level, pos, player, hand, stack) -> {
            if (!level.isClientSide) {
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(bottle)));
                player.awardStat(Stats.USE_CAULDRON);
                player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
                lowerFillLevel(state, level, pos);
                level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.gameEvent(null, GameEvent.FLUID_PICKUP, pos);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        });
        map.put(
                bottle,
                (state, level, pos, player, hand, stack) -> state.getValue(LEVEL) == MAX_FILL_LEVEL
                        ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                        : emptyBottle(state.cycle(LEVEL), level, pos, player, hand, stack));
    }

    /** Vanilla's water bottle into a cauldron: the cauldron becomes {@code result}, the player gets a glass bottle. */
    private static ItemInteractionResult emptyBottle(
            BlockState result, Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack) {
        if (!level.isClientSide) {
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE)));
            player.awardStat(Stats.USE_CAULDRON);
            player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
            level.setBlockAndUpdate(pos, result);
            level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
}
