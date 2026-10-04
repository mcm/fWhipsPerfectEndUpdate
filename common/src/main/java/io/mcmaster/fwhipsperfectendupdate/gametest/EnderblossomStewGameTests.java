package io.mcmaster.fwhipsperfectendupdate.gametest;

import io.mcmaster.fwhipsperfectendupdate.block.EnderblossomBlock;
import io.mcmaster.fwhipsperfectendupdate.block.ModBlocks;
import io.mcmaster.fwhipsperfectendupdate.effect.ChorusTeleport;
import io.mcmaster.fwhipsperfectendupdate.effect.ModEffects;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.SuspiciousEffectHolder;
import net.minecraft.world.phys.Vec3;

/** MOD-61: the Displacement effect and Enderblossom suspicious stew (crafted and from brown mooshrooms). */
public final class EnderblossomStewGameTests {
    private static final String BATCH = "enderblossom_stew";

    /** What both blossoms give in stew: one tick of Displacement. */
    private static SuspiciousStewEffects expectedEffects() {
        return new SuspiciousStewEffects(List.of(
                new SuspiciousStewEffects.Entry(ModEffects.displacement(), EnderblossomBlock.STEW_EFFECT_DURATION)));
    }

    private EnderblossomStewGameTests() {}

    private static List<EnderblossomBlock> blossoms() {
        return List.of(ModBlocks.OPEN_ENDERBLOSSOM.get(), ModBlocks.CLOSED_ENDERBLOSSOM.get());
    }

    private static void assertDisplacementStew(GameTestHelper helper, ItemStack stew, String source) {
        helper.assertTrue(stew.is(Items.SUSPICIOUS_STEW), source + " gave " + stew + ", expected suspicious stew");
        helper.assertValueEqual(
                stew.get(DataComponents.SUSPICIOUS_STEW_EFFECTS), expectedEffects(), source + " stew effects");
    }

    @GameTest(batch = BATCH)
    public static void displacementIsInstantAndNeutral(GameTestHelper helper) {
        helper.assertTrue(ModEffects.DISPLACEMENT.get().isInstantenous(), "displacement should be instant");
        helper.assertTrue(
                ModEffects.DISPLACEMENT.get().getCategory() == MobEffectCategory.NEUTRAL,
                "displacement should be neutral");
        helper.assertValueEqual(
                ModEffects.DISPLACEMENT.get().getDescriptionId(),
                "effect.fwhipsperfectendupdate.displacement",
                "displacement translation key");
        for (EnderblossomBlock blossom : blossoms()) {
            helper.assertTrue(
                    SuspiciousEffectHolder.tryGet(blossom.asItem()) == blossom,
                    blossom + " should be a suspicious effect holder");
            helper.assertValueEqual(blossom.getSuspiciousEffects(), expectedEffects(), blossom + " effects");
        }
        helper.succeed();
    }

    @GameTest(batch = BATCH)
    public static void craftingStewFromEachBlossomGivesDisplacement(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (EnderblossomBlock blossom : blossoms()) {
            CraftingInput input = CraftingInput.of(
                    2,
                    2,
                    List.of(
                            new ItemStack(Items.BOWL),
                            new ItemStack(Items.BROWN_MUSHROOM),
                            new ItemStack(Items.RED_MUSHROOM),
                            new ItemStack(blossom)));
            ItemStack stew = level.getRecipeManager()
                    .getRecipeFor(RecipeType.CRAFTING, input, level)
                    .map(recipe -> recipe.value().assemble(input, level.registryAccess()))
                    .orElse(ItemStack.EMPTY);
            assertDisplacementStew(helper, stew, "crafting with " + blossom);
        }
        helper.succeed();
    }

    @GameTest(batch = BATCH)
    public static void brownMooshroomFedABlossomGivesDisplacementStew(GameTestHelper helper) {
        for (EnderblossomBlock blossom : blossoms()) {
            MushroomCow cow = helper.spawn(EntityType.MOOSHROOM, new BlockPos(4, 1, 4));
            cow.setVariant(MushroomCow.MushroomType.BROWN);
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(blossom));
            helper.assertTrue(
                    cow.mobInteract(player, InteractionHand.MAIN_HAND).consumesAction(),
                    "the brown mooshroom should eat " + blossom);
            helper.assertTrue(player.getMainHandItem().isEmpty(), blossom + " should be used up");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BOWL));
            cow.mobInteract(player, InteractionHand.MAIN_HAND);
            assertDisplacementStew(helper, player.getMainHandItem(), "a brown mooshroom fed " + blossom);
            cow.discard();
        }
        helper.succeed();
    }

    @GameTest(batch = BATCH)
    public static void redMooshroomIgnoresBlossoms(GameTestHelper helper) {
        MushroomCow cow = helper.spawn(EntityType.MOOSHROOM, new BlockPos(4, 1, 4));
        cow.setVariant(MushroomCow.MushroomType.RED);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModBlocks.OPEN_ENDERBLOSSOM.get()));
        cow.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "blossoms left after feeding a red mooshroom");
        helper.succeed();
    }

    @GameTest(batch = BATCH)
    public static void displacementTeleportsThePlayerNearby(GameTestHelper helper) {
        Vec3 start = helper.absoluteVec(new Vec3(4.5, 1, 4.5));
        for (long seed = 1; seed <= 5; seed++) {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.moveTo(start.x, start.y, start.z, 0, 0);
            player.getRandom().setSeed(seed);
            player.fallDistance = 5;
            new MobEffectInstance(ModEffects.displacement(), EnderblossomBlock.STEW_EFFECT_DURATION)
                    .tick(player, () -> {});
            Vec3 end = player.position();
            helper.assertFalse(end.equals(start), "seed " + seed + ": displacement didn't move the player");
            helper.assertTrue(
                    Math.abs(end.x - start.x) <= ChorusTeleport.RANGE
                            && Math.abs(end.z - start.z) <= ChorusTeleport.RANGE,
                    "seed " + seed + ": teleported more than 8 blocks horizontally, from " + start + " to " + end);
            helper.assertTrue(
                    Math.abs(end.y - start.y) <= ChorusTeleport.RANGE,
                    "seed " + seed + ": teleported more than 8 blocks vertically, from " + start + " to " + end);
            helper.assertValueEqual(player.fallDistance, 0.0F, "seed " + seed + ": fall distance after teleporting");
        }
        helper.succeed();
    }
}
