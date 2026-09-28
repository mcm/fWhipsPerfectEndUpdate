package io.mcmaster.someonesperfectend.gametest;

import io.mcmaster.someonesperfectend.totem.EffectTotems;
import io.mcmaster.someonesperfectend.trinket.TrinketSlot;
import io.wispforest.accessories.impl.AccessoriesEventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Totem-given Haste III+ mines much faster (MOD-46); other Haste stays vanilla. */
public final class HasteBoostGameTests {
    private static final Block[] BLOCKS = {Blocks.END_STONE, Blocks.DEEPSLATE};

    private HasteBoostGameTests() {
    }

    /** A survival player on the ground holding a diamond pickaxe with Efficiency {@code efficiency}. */
    private static Player miner(GameTestHelper helper, int efficiency, boolean hasteTotem, int hasteAmplifier) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setOnGround(true);
        ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        pickaxe.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.EFFICIENCY), efficiency);
        player.setItemInHand(InteractionHand.MAIN_HAND, pickaxe);
        // A mock player doesn't tick, so apply the Efficiency attribute the way an equipment change would.
        EnchantmentHelper.forEachModifier(pickaxe, EquipmentSlot.MAINHAND,
                (attribute, modifier) -> player.getAttribute(attribute).addTransientModifier(modifier));
        if (hasteTotem) {
            player.inventoryMenu.getSlot(TrinketSlot.indexIn(player.inventoryMenu))
                    .setByPlayer(EffectTotems.activated(EffectTotems.TOTEM_OF_HASTE.get()));
            AccessoriesEventHandler.onLivingEntityTick(player);
        }
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 600, hasteAmplifier));
        return player;
    }

    private static float progress(GameTestHelper helper, Player player, Block block) {
        return block.defaultBlockState().getDestroyProgress(player, helper.getLevel(), BlockPos.ZERO);
    }

    /** Vanilla's progress with a diamond pickaxe (Efficiency V), on the ground, and Haste at this amplifier. */
    private static float vanillaProgress(Block block, int hasteAmplifier) {
        float speed = 8.0F + (5 * 5 + 1);
        speed *= 1.0F + (float) (hasteAmplifier + 1) * 0.2F;
        return speed / block.defaultDestroyTime() / 30.0F;
    }

    @GameTest
    public static void totemHasteThreeMinesInstantly(GameTestHelper helper) {
        Player player = miner(helper, 5, true, 2);
        for (Block block : BLOCKS) {
            float progress = progress(helper, player, block);
            helper.assertTrue(progress >= 1.0F, block + " should break instantly, progress was " + progress);
        }
        helper.succeed();
    }

    @GameTest
    public static void totemHasteTwoIsNotBoosted(GameTestHelper helper) {
        Player player = miner(helper, 5, true, 1);
        for (Block block : BLOCKS) {
            float progress = progress(helper, player, block);
            helper.assertTrue(progress < 1.0F, block + " should not break instantly with Haste II, was " + progress);
        }
        helper.succeed();
    }

    @GameTest
    public static void hasteThreeWithoutTotemStaysVanilla(GameTestHelper helper) {
        Player player = miner(helper, 5, false, 2);
        for (Block block : BLOCKS) {
            float progress = progress(helper, player, block);
            float vanilla = vanillaProgress(block, 2);
            helper.assertTrue(progress < 1.0F && Math.abs(progress - vanilla) < 1.0E-6F,
                    block + " should mine at vanilla speed " + vanilla + " without the totem, was " + progress);
        }
        helper.succeed();
    }

    @GameTest
    public static void totemHasteThreeNeedsEfficiencyFive(GameTestHelper helper) {
        Player player = miner(helper, 4, true, 2);
        float progress = progress(helper, player, Blocks.END_STONE);
        helper.assertTrue(progress < 1.0F, "Efficiency IV should not break end stone instantly, was " + progress);
        helper.succeed();
    }
}
