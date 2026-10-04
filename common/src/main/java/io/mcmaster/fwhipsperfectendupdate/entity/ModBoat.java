package io.mcmaster.fwhipsperfectendupdate.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** A boat or raft made of one of the mod's woods. */
public class ModBoat extends Boat {
    private final ModBoatType type;

    public ModBoat(EntityType<? extends Boat> entityType, Level level, ModBoatType type) {
        super(entityType, level);
        this.type = type;
        setVariant(type.shape());
    }

    public ModBoatType getModType() {
        return type;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setVariant(type.shape());
    }

    @Override
    public Item getDropItem() {
        return ModBoats.item(type, false).get();
    }

    /** A boat broken by a fall drops the vanilla shape's planks; drop our planks instead. */
    @Override
    public @Nullable ItemEntity spawnAtLocation(ItemLike item) {
        return super.spawnAtLocation(ModBoats.replacePlanks(type, item));
    }
}
