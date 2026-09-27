package io.mcmaster.someonesperfectend.block;

import io.mcmaster.someonesperfectend.SomeonesPerfectEnd;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class ModBlockTags {
    /** Blocks that hanging End plants (Voidvine) can grow from the underside of. */
    public static final TagKey<Block> SUPPORTS_HANGING_PLANTS = create("supports_hanging_plants");
    /** End ground that End plants (Glowcap, Void Petals, ...) can be placed on: end turf, end stone, voidsoil. */
    public static final TagKey<Block> END_PLANT_SOIL = create("end_plant_soil");

    private ModBlockTags() {
    }

    private static TagKey<Block> create(String name) {
        return TagKey.create(Registries.BLOCK, SomeonesPerfectEnd.id(name));
    }
}
