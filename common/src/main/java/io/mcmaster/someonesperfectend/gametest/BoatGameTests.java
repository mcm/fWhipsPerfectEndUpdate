package io.mcmaster.someonesperfectend.gametest;

import static io.mcmaster.someonesperfectend.gametest.AstralWoodGameTests.craft;

import dev.architectury.registry.fuel.FuelRegistry;
import io.mcmaster.someonesperfectend.SomeonesPerfectEnd;
import io.mcmaster.someonesperfectend.entity.ModBoat;
import io.mcmaster.someonesperfectend.entity.ModBoatType;
import io.mcmaster.someonesperfectend.entity.ModBoats;
import io.mcmaster.someonesperfectend.entity.ModChestBoat;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.phys.AABB;

public final class BoatGameTests {
    private BoatGameTests() {
    }

    @GameTest
    public static void boatItemsSpawnTheirEntityOnWater(GameTestHelper helper) {
        BlockPos water = new BlockPos(3, 1, 3);
        helper.setBlock(water.below(), Blocks.STONE);
        helper.setBlock(water, Blocks.WATER);
        BlockPos abs = helper.absolutePos(water);
        for (ModBoatType type : ModBoatType.values()) {
            for (boolean chest : new boolean[] {false, true}) {
                Item item = ModBoats.item(type, chest).get();
                Player player = helper.makeMockPlayer(GameType.SURVIVAL);
                player.moveTo(abs.getX() + 0.5, abs.getY() + 2, abs.getZ() + 0.5, 0.0F, 90.0F);
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
                player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                helper.assertTrue(player.getMainHandItem().isEmpty(), item + " was not used up");

                EntityType<? extends Boat> entityType = ModBoats.entityType(type, chest).get();
                List<? extends Boat> boats = helper.getLevel().getEntities(entityType, new AABB(abs).inflate(2),
                        boat -> true);
                helper.assertTrue(boats.size() == 1, item + " spawned " + boats.size() + " " + entityType);
                Boat boat = boats.get(0);
                helper.assertTrue(chest ? boat instanceof ModChestBoat : boat instanceof ModBoat,
                        "wrong boat class " + boat.getClass());
                helper.assertTrue(boat.getVariant() == type.shape(), "boat shape is " + boat.getVariant());
                helper.assertTrue(boat.getDropItem() == item, boat + " drops " + boat.getDropItem());
                helper.assertTrue(boat.getPickResult().is(item), "pick result is " + boat.getPickResult());
                boat.discard();
            }
        }
        helper.succeed();
    }

    @GameTest
    public static void boatsAreRideableAndDropThemselves(GameTestHelper helper) {
        BlockPos abs = helper.absolutePos(new BlockPos(3, 2, 3));
        for (ModBoatType type : ModBoatType.values()) {
            for (boolean chest : new boolean[] {false, true}) {
                Item item = ModBoats.item(type, chest).get();
                Boat boat = ModBoats.create(helper.getLevel(), type, chest, abs.getX() + 0.5, abs.getY(),
                        abs.getZ() + 0.5);
                helper.getLevel().addFreshEntity(boat);
                Player player = helper.makeMockPlayer(GameType.SURVIVAL);
                helper.assertTrue(player.startRiding(boat) && player.getVehicle() == boat, item + " is not rideable");
                player.stopRiding();

                boat.hurt(helper.getLevel().damageSources().playerAttack(player), 100.0F);
                helper.assertTrue(boat.isRemoved(), item + " did not break");
                List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                        new AABB(abs).inflate(3), drop -> true);
                helper.assertTrue(drops.size() == 1 && drops.get(0).getItem().is(item) && drops.get(0).getItem()
                        .getCount() == 1, "broken " + item + " dropped " + drops.stream().map(ItemEntity::getItem)
                        .toList());
                drops.forEach(ItemEntity::discard);

                // A boat destroyed by a fall drops its shape's planks; ours must drop our planks instead.
                ItemEntity planks = boat.spawnAtLocation(type.shape().getPlanks());
                helper.assertTrue(planks != null && planks.getItem().is(type.planks().asItem()),
                        item + " fall drop is " + (planks == null ? null : planks.getItem()));
                planks.discard();
            }
        }
        helper.succeed();
    }

    @GameTest
    public static void boatRecipesMatchVanilla(GameTestHelper helper) {
        for (ModBoatType type : ModBoatType.values()) {
            Item boat = ModBoats.item(type, false).get();
            Item chestBoat = ModBoats.item(type, true).get();
            ItemStack planks = new ItemStack(type.planks());
            ItemStack crafted = craft(helper, CraftingInput.of(3, 2,
                    List.of(planks, ItemStack.EMPTY, planks, planks, planks, planks)));
            helper.assertTrue(crafted.is(boat) && crafted.getCount() == 1, type + " planks crafted into " + crafted);
            ItemStack withChest = craft(helper, CraftingInput.of(2, 1,
                    List.of(new ItemStack(Items.CHEST), new ItemStack(boat))));
            helper.assertTrue(withChest.is(chestBoat), boat + " and chest crafted into " + withChest);

            for (Item item : List.of(boat, chestBoat)) {
                ItemStack stack = new ItemStack(item);
                helper.assertTrue(stack.is(ItemTags.BOATS) && stack.getMaxStackSize() == 1,
                        item + " is not a boat that stacks to 1");
                helper.assertTrue(FuelRegistry.get(stack) == 1200, item + " burns for " + FuelRegistry.get(stack));
                helper.assertTrue(helper.getLevel().getServer().getAdvancements()
                        .get(SomeonesPerfectEnd.id("recipes/transportation/" + type.id(item == chestBoat))) != null,
                        "recipe advancement for " + item + " is missing");
            }
            helper.assertTrue(new ItemStack(chestBoat).is(ItemTags.CHEST_BOATS), chestBoat + " is not a chest boat");
        }
        helper.succeed();
    }

    @GameTest
    public static void dispenserPlacesRaftOnWater(GameTestHelper helper) {
        BlockPos dispenser = new BlockPos(1, 2, 3);
        BlockPos water = dispenser.east();
        helper.setBlock(water.below(), Blocks.STONE);
        helper.setBlock(water.east().below(), Blocks.STONE);
        helper.setBlock(water, Blocks.WATER);
        helper.setBlock(water.east(), Blocks.WATER);
        helper.setBlock(dispenser, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.EAST));
        DispenserBlockEntity entity = (DispenserBlockEntity) helper.getBlockEntity(dispenser);
        entity.setItem(0, new ItemStack(ModBoats.item(ModBoatType.VOIDVINE, false).get()));
        helper.setBlock(dispenser.west(), Blocks.REDSTONE_BLOCK);
        EntityType<? extends Boat> raft = ModBoats.entityType(ModBoatType.VOIDVINE, false).get();
        helper.succeedWhen(() -> {
            helper.assertTrue(!helper.getLevel().getEntities(raft, new AABB(helper.absolutePos(water)).inflate(2),
                    boat -> true).isEmpty(), "no raft was dispensed");
            helper.assertTrue(entity.isEmpty(), "dispenser still holds the raft");
        });
    }
}
