package io.mcmaster.someonesperfectend.block;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.registry.registries.RegistrySupplier;
import io.mcmaster.someonesperfectend.registry.ModRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.HugeMushroomBlock;
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

    public static final RegistrySupplier<VoidPetalsBlock> VOID_PETALS = ModRegistries.block("void_petals",
            () -> new VoidPetalsBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .noCollission()
                    .sound(SoundType.PINK_PETALS)
                    .pushReaction(PushReaction.DESTROY)));

    public static final int GLOWCAP_LIGHT = 10;
    public static final int GLOWCAP_BLOCK_LIGHT = 12;

    public static final RegistrySupplier<GlowcapBlock> GLOWCAP = ModRegistries.block("glowcap",
            () -> new GlowcapBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .lightLevel(state -> GLOWCAP_LIGHT)
                    .pushReaction(PushReaction.DESTROY)));

    public static final RegistrySupplier<HugeMushroomBlock> GLOWCAP_BLOCK = ModRegistries.block("glowcap_block",
            () -> new HugeMushroomBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(0.2F)
                    .sound(SoundType.WOOD)
                    .lightLevel(state -> GLOWCAP_BLOCK_LIGHT)
                    .ignitedByLava()));

    public static final RegistrySupplier<HugeMushroomBlock> GLOWCAP_STEM = ModRegistries.block("glowcap_stem",
            () -> new HugeMushroomBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOL)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(0.2F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava()));

    public static final RegistrySupplier<FlowerPotBlock> POTTED_GLOWCAP = ModRegistries.blockOnly("potted_glowcap",
            () -> new FlowerPotBlock(GLOWCAP.get(), BlockBehaviour.Properties.of()
                    .instabreak()
                    .noOcclusion()
                    .lightLevel(state -> GLOWCAP_LIGHT)
                    .pushReaction(PushReaction.DESTROY)));

    private ModBlocks() {
    }

    /** Loads this class so its entries are added to the deferred registers. */
    public static void init() {
        // Fabric reads the vanilla map; NeoForge reads data/neoforge/data_maps/item/compostables.json instead.
        LifecycleEvent.SETUP.register(() -> {
            ComposterBlock.COMPOSTABLES.put(VOID_PETALS.get().asItem(), 0.3F);
            ComposterBlock.COMPOSTABLES.put(GLOWCAP.get().asItem(), 0.65F);
        });
    }
}
