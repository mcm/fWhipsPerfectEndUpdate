package io.mcmaster.someonesperfectend.block;

import dev.architectury.registry.registries.RegistrySupplier;
import io.mcmaster.someonesperfectend.registry.ModRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public final class ModBlocks {
    public static final RegistrySupplier<EndTurfBlock> END_TURF = ModRegistries.block("end_turf",
            () -> new EndTurfBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F, 9.0F)
                    .randomTicks()));

    public static final RegistrySupplier<ShortVoidGrassBlock> SHORT_VOID_GRASS = ModRegistries.block(
            "short_void_grass",
            () -> new ShortVoidGrassBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .replaceable()
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .offsetType(BlockBehaviour.OffsetType.XYZ)
                    .ignitedByLava()
                    .pushReaction(PushReaction.DESTROY)));

    public static final RegistrySupplier<Block> VOIDSOIL = ModRegistries.block("voidsoil",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_PURPLE)
                    .strength(0.5F)
                    .sound(SoundType.ROOTED_DIRT)));

    private ModBlocks() {
    }

    /** Loads this class so its entries are added to the deferred registers. */
    public static void init() {
    }
}
