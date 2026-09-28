package io.mcmaster.someonesperfectend.entity;

import io.mcmaster.someonesperfectend.block.ModBlocks;
import java.util.function.Supplier;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.block.Block;

/**
 * The mod's boat woods. Each borrows a vanilla {@link Boat.Type} for its shape and physics (Astral boats behave like
 * oak boats, Voidvine rafts like bamboo rafts); our entities only swap the drops and the texture.
 */
public enum ModBoatType {
    ASTRAL("astral", Boat.Type.OAK, ModBlocks.ASTRAL_PLANKS),
    VOIDVINE("voidvine", Boat.Type.BAMBOO, ModBlocks.VOIDVINE_PLANKS);

    private final String name;
    private final Boat.Type shape;
    private final Supplier<? extends Block> planks;

    ModBoatType(String name, Boat.Type shape, Supplier<? extends Block> planks) {
        this.name = name;
        this.shape = shape;
        this.planks = planks;
    }

    public String getName() {
        return name;
    }

    /** The vanilla type whose model and physics this boat uses. */
    public Boat.Type shape() {
        return shape;
    }

    public boolean isRaft() {
        return shape == Boat.Type.BAMBOO;
    }

    public Block planks() {
        return planks.get();
    }

    /** Registry name of the boat entity/item, e.g. {@code astral_boat} or {@code voidvine_chest_raft}. */
    public String id(boolean chest) {
        return name + (chest ? "_chest" : "") + (isRaft() ? "_raft" : "_boat");
    }
}
