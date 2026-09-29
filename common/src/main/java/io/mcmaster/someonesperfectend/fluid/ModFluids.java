package io.mcmaster.someonesperfectend.fluid;

import dev.architectury.core.block.ArchitecturyLiquidBlock;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.registry.registries.RegistrySupplier;
import io.mcmaster.someonesperfectend.SomeonesPerfectEnd;
import io.mcmaster.someonesperfectend.item.OasisWaterBottleItem;
import io.mcmaster.someonesperfectend.item.OasisWaterBucketItem;
import io.mcmaster.someonesperfectend.registry.ModRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public final class ModFluids {
    public static final ResourceLocation STILL_TEXTURE = SomeonesPerfectEnd.id("block/oasis_water_still");
    public static final ResourceLocation FLOWING_TEXTURE = SomeonesPerfectEnd.id("block/oasis_water_flow");
    public static final ResourceLocation OVERLAY_TEXTURE = SomeonesPerfectEnd.id("block/oasis_water_overlay");

    public static final RegistrySupplier<OasisWaterFluid.Source> OASIS_WATER =
            ModRegistries.FLUIDS.register("oasis_water", OasisWaterPlatform::createSource);
    public static final RegistrySupplier<OasisWaterFluid.Flowing> FLOWING_OASIS_WATER =
            ModRegistries.FLUIDS.register("flowing_oasis_water", OasisWaterPlatform::createFlowing);

    /** Same properties as {@code minecraft:water}, apart from the map color. */
    public static final RegistrySupplier<LiquidBlock> OASIS_WATER_BLOCK = ModRegistries.blockOnly(
            "oasis_water",
            () -> new ArchitecturyLiquidBlock(
                    OASIS_WATER,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_CYAN)
                            .replaceable()
                            .noCollission()
                            .strength(100.0F)
                            .pushReaction(PushReaction.DESTROY)
                            .noLootTable()
                            .liquid()
                            .sound(SoundType.EMPTY)));

    public static final RegistrySupplier<OasisWaterBucketItem> OASIS_WATER_BUCKET = ModRegistries.item(
            "oasis_water_bucket",
            () -> new OasisWaterBucketItem(
                    OASIS_WATER,
                    ModRegistries.itemProperties().craftRemainder(Items.BUCKET).stacksTo(1)));

    /** A water bottle filled from Oasis Water. It holds the water potion, so it drinks and brews like one. */
    public static final RegistrySupplier<OasisWaterBottleItem> OASIS_WATER_BOTTLE = ModRegistries.item(
            "oasis_water_bottle",
            () -> new OasisWaterBottleItem(ModRegistries.itemProperties()
                    .stacksTo(1)
                    .component(DataComponents.POTION_CONTENTS, new PotionContents(Potions.WATER))));

    private ModFluids() {}

    /** Loads this class so its entries are added to the deferred registers. */
    public static void init() {
        LifecycleEvent.SETUP.register(() ->
                DispenserBlock.registerBehavior(OASIS_WATER_BUCKET.get(), OasisWaterBucketItem.dispenseBehavior()));
    }
}
