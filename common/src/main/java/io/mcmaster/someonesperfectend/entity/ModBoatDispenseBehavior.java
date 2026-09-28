package io.mcmaster.someonesperfectend.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.Vec3;

/** Dispenses one of the mod's boats onto water, like vanilla {@code BoatDispenseItemBehavior}. */
public class ModBoatDispenseBehavior extends DefaultDispenseItemBehavior {
    private final DefaultDispenseItemBehavior defaultBehavior = new DefaultDispenseItemBehavior();
    private final ModBoatType type;
    private final boolean chest;

    public ModBoatDispenseBehavior(ModBoatType type, boolean chest) {
        this.type = type;
        this.chest = chest;
    }

    @Override
    public ItemStack execute(BlockSource source, ItemStack stack) {
        Direction facing = source.state().getValue(DispenserBlock.FACING);
        ServerLevel level = source.level();
        Vec3 center = source.center();
        double offset = 0.5625 + EntityType.BOAT.getWidth() / 2.0;
        double x = center.x() + facing.getStepX() * offset;
        double y = center.y() + facing.getStepY() * 1.125F;
        double z = center.z() + facing.getStepZ() * offset;
        BlockPos front = source.pos().relative(facing);
        double lift;
        if (level.getFluidState(front).is(FluidTags.WATER)) {
            lift = 1.0;
        } else if (level.getBlockState(front).isAir() && level.getFluidState(front.below()).is(FluidTags.WATER)) {
            lift = 0.0;
        } else {
            return defaultBehavior.dispense(source, stack);
        }
        Boat boat = ModBoats.create(level, type, chest, x, y + lift, z);
        EntityType.<Boat>createDefaultStackConfig(level, stack, null).accept(boat);
        boat.setYRot(facing.toYRot());
        level.addFreshEntity(boat);
        stack.shrink(1);
        return stack;
    }

    @Override
    protected void playSound(BlockSource source) {
        source.level().levelEvent(1000, source.pos(), 0);
    }
}
