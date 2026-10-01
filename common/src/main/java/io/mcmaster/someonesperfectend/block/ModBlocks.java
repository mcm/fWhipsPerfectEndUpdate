package io.mcmaster.someonesperfectend.block;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.hooks.item.tool.AxeItemHooks;
import dev.architectury.registry.fuel.FuelRegistry;
import dev.architectury.registry.registries.RegistrySupplier;
import io.mcmaster.someonesperfectend.infuser.InfuserBlock;
import io.mcmaster.someonesperfectend.registry.ModRegistries;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public final class ModBlocks {
    public static final RegistrySupplier<EndTurfBlock> END_TURF = ModRegistries.block(
            "end_turf",
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

    public static final RegistrySupplier<Block> VOIDSOIL = ModRegistries.block(
            "voidsoil",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_PURPLE)
                    .strength(0.5F)
                    .sound(SoundType.ROOTED_DIRT)));

    public static final RegistrySupplier<VoidPetalsBlock> VOID_PETALS = ModRegistries.block(
            "void_petals",
            () -> new VoidPetalsBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .noCollission()
                    .sound(SoundType.PINK_PETALS)
                    .pushReaction(PushReaction.DESTROY)));

    public static final int GLOWCAP_LIGHT = 10;
    public static final int GLOWCAP_BLOCK_LIGHT = 12;

    public static final RegistrySupplier<GlowcapBlock> GLOWCAP = ModRegistries.block(
            "glowcap",
            () -> new GlowcapBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .lightLevel(state -> GLOWCAP_LIGHT)
                    .pushReaction(PushReaction.DESTROY)));

    public static final RegistrySupplier<HugeMushroomBlock> GLOWCAP_BLOCK = ModRegistries.block(
            "glowcap_block",
            () -> new HugeMushroomBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(0.2F)
                    .sound(SoundType.WOOD)
                    .lightLevel(state -> GLOWCAP_BLOCK_LIGHT)
                    .ignitedByLava()));

    public static final RegistrySupplier<HugeMushroomBlock> GLOWCAP_STEM = ModRegistries.block(
            "glowcap_stem",
            () -> new HugeMushroomBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOL)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(0.2F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava()));

    public static final RegistrySupplier<FlowerPotBlock> POTTED_GLOWCAP = ModRegistries.blockOnly(
            "potted_glowcap",
            () -> new FlowerPotBlock(
                    GLOWCAP.get(),
                    BlockBehaviour.Properties.of()
                            .instabreak()
                            .noOcclusion()
                            .lightLevel(state -> GLOWCAP_LIGHT)
                            .pushReaction(PushReaction.DESTROY)));

    public static final RegistrySupplier<BounceCapBlock> BOUNCE_CAP = ModRegistries.block(
            "bounce_cap",
            () -> new BounceCapBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_MAGENTA)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(0.2F)
                    .sound(SoundType.FUNGUS)
                    .noOcclusion()
                    .ignitedByLava()
                    .pushReaction(PushReaction.DESTROY)));

    /** Beacon copy (light 15, hardness 3, mined by hand); not craftable, bartered from Endlings (US-034). */
    public static final RegistrySupplier<InfuserBlock> INFUSER = ModRegistries.block(
            "infuser",
            () -> new InfuserBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.BEACON).mapColor(MapColor.COLOR_PURPLE)),
            block -> new BlockItem(block, ModRegistries.itemProperties().rarity(Rarity.RARE)));

    // Astral wood set (US-004). Properties follow vanilla's cherry set; sounds come from ModWoodTypes.ASTRAL.
    private static final MapColor ASTRAL_WOOD_COLOR = MapColor.COLOR_LIGHT_BLUE;
    private static final MapColor ASTRAL_BARK_COLOR = MapColor.COLOR_PURPLE;

    public static final RegistrySupplier<RotatedPillarBlock> ASTRAL_LOG = ModRegistries.block(
            "astral_log",
            () -> new RotatedPillarBlock(logProperties(ASTRAL_WOOD_COLOR, ASTRAL_BARK_COLOR, ModWoodTypes.ASTRAL)));
    /** Chorus Flower-look Astral Log (MOD-50 US-002); drops a plain Astral Log unless mined with Silk Touch. */
    public static final RegistrySupplier<RotatedPillarBlock> FLOWERING_ASTRAL_LOG = ModRegistries.block(
            "flowering_astral_log",
            () -> new RotatedPillarBlock(logProperties(ASTRAL_WOOD_COLOR, ASTRAL_BARK_COLOR, ModWoodTypes.ASTRAL)));

    public static final RegistrySupplier<RotatedPillarBlock> ASTRAL_WOOD = ModRegistries.block(
            "astral_wood",
            () -> new RotatedPillarBlock(logProperties(ASTRAL_BARK_COLOR, ASTRAL_BARK_COLOR, ModWoodTypes.ASTRAL)));
    public static final RegistrySupplier<RotatedPillarBlock> STRIPPED_ASTRAL_LOG = ModRegistries.block(
            "stripped_astral_log",
            () -> new RotatedPillarBlock(logProperties(ASTRAL_WOOD_COLOR, ASTRAL_WOOD_COLOR, ModWoodTypes.ASTRAL)));
    public static final RegistrySupplier<RotatedPillarBlock> STRIPPED_ASTRAL_WOOD = ModRegistries.block(
            "stripped_astral_wood",
            () -> new RotatedPillarBlock(logProperties(ASTRAL_WOOD_COLOR, ASTRAL_WOOD_COLOR, ModWoodTypes.ASTRAL)));
    public static final RegistrySupplier<Block> ASTRAL_PLANKS = ModRegistries.block(
            "astral_planks",
            () -> new Block(
                    woodProperties(ASTRAL_WOOD_COLOR, ModWoodTypes.ASTRAL).strength(2.0F, 3.0F)));
    public static final RegistrySupplier<StairBlock> ASTRAL_STAIRS = ModRegistries.block(
            "astral_stairs",
            () -> new StairBlock(
                    ASTRAL_PLANKS.get().defaultBlockState(),
                    BlockBehaviour.Properties.ofLegacyCopy(ASTRAL_PLANKS.get())));
    public static final RegistrySupplier<SlabBlock> ASTRAL_SLAB = ModRegistries.block(
            "astral_slab",
            () -> new SlabBlock(
                    woodProperties(ASTRAL_WOOD_COLOR, ModWoodTypes.ASTRAL).strength(2.0F, 3.0F)));
    public static final RegistrySupplier<FenceBlock> ASTRAL_FENCE = ModRegistries.block(
            "astral_fence",
            () -> new FenceBlock(woodProperties(ASTRAL_WOOD_COLOR, ModWoodTypes.ASTRAL)
                    .forceSolidOn()
                    .strength(2.0F, 3.0F)));
    public static final RegistrySupplier<FenceGateBlock> ASTRAL_FENCE_GATE = ModRegistries.block(
            "astral_fence_gate",
            () -> new FenceGateBlock(
                    ModWoodTypes.ASTRAL,
                    woodProperties(ASTRAL_WOOD_COLOR, ModWoodTypes.ASTRAL)
                            .forceSolidOn()
                            .strength(2.0F, 3.0F)));
    public static final RegistrySupplier<DoorBlock> ASTRAL_DOOR = ModRegistries.block(
            "astral_door",
            () -> new DoorBlock(
                    ModWoodTypes.ASTRAL_SET,
                    BlockBehaviour.Properties.of()
                            .mapColor(ASTRAL_WOOD_COLOR)
                            .instrument(NoteBlockInstrument.BASS)
                            .strength(3.0F)
                            .noOcclusion()
                            .ignitedByLava()
                            .pushReaction(PushReaction.DESTROY)),
            block -> new DoubleHighBlockItem(block, ModRegistries.itemProperties()));
    public static final RegistrySupplier<TrapDoorBlock> ASTRAL_TRAPDOOR = ModRegistries.block(
            "astral_trapdoor",
            () -> new TrapDoorBlock(
                    ModWoodTypes.ASTRAL_SET,
                    BlockBehaviour.Properties.of()
                            .mapColor(ASTRAL_WOOD_COLOR)
                            .instrument(NoteBlockInstrument.BASS)
                            .strength(3.0F)
                            .noOcclusion()
                            .isValidSpawn((state, level, pos, type) -> false)
                            .ignitedByLava()));
    public static final RegistrySupplier<ButtonBlock> ASTRAL_BUTTON = ModRegistries.block(
            "astral_button",
            () -> new ButtonBlock(
                    ModWoodTypes.ASTRAL_SET,
                    30,
                    BlockBehaviour.Properties.of().noCollission().strength(0.5F).pushReaction(PushReaction.DESTROY)));
    public static final RegistrySupplier<PressurePlateBlock> ASTRAL_PRESSURE_PLATE = ModRegistries.block(
            "astral_pressure_plate",
            () -> new PressurePlateBlock(
                    ModWoodTypes.ASTRAL_SET,
                    woodProperties(ASTRAL_WOOD_COLOR, ModWoodTypes.ASTRAL)
                            .forceSolidOn()
                            .noCollission()
                            .strength(0.5F)
                            .pushReaction(PushReaction.DESTROY)));
    public static final RegistrySupplier<StandingSignBlock> ASTRAL_SIGN = ModRegistries.blockOnly(
            "astral_sign", () -> new StandingSignBlock(ModWoodTypes.ASTRAL, signProperties(ASTRAL_WOOD_COLOR)));
    public static final RegistrySupplier<WallSignBlock> ASTRAL_WALL_SIGN = ModRegistries.blockOnly(
            "astral_wall_sign",
            () -> new WallSignBlock(
                    ModWoodTypes.ASTRAL, signProperties(ASTRAL_WOOD_COLOR).dropsLike(ASTRAL_SIGN.get())));
    public static final RegistrySupplier<CeilingHangingSignBlock> ASTRAL_HANGING_SIGN = ModRegistries.blockOnly(
            "astral_hanging_sign",
            () -> new CeilingHangingSignBlock(ModWoodTypes.ASTRAL, signProperties(ASTRAL_BARK_COLOR)));
    public static final RegistrySupplier<WallHangingSignBlock> ASTRAL_WALL_HANGING_SIGN = ModRegistries.blockOnly(
            "astral_wall_hanging_sign",
            () -> new WallHangingSignBlock(
                    ModWoodTypes.ASTRAL, signProperties(ASTRAL_BARK_COLOR).dropsLike(ASTRAL_HANGING_SIGN.get())));
    public static final RegistrySupplier<SignItem> ASTRAL_SIGN_ITEM = ModRegistries.item(
            "astral_sign",
            () -> new SignItem(ModRegistries.itemProperties().stacksTo(16), ASTRAL_SIGN.get(), ASTRAL_WALL_SIGN.get()));
    public static final RegistrySupplier<HangingSignItem> ASTRAL_HANGING_SIGN_ITEM = ModRegistries.item(
            "astral_hanging_sign",
            () -> new HangingSignItem(
                    ASTRAL_HANGING_SIGN.get(),
                    ASTRAL_WALL_HANGING_SIGN.get(),
                    ModRegistries.itemProperties().stacksTo(16)));

    // Voidvine wood set (US-006). Mirrors vanilla's bamboo set; sounds come from ModWoodTypes.VOIDVINE.
    private static final MapColor VOIDVINE_WOOD_COLOR = MapColor.COLOR_GREEN;
    private static final MapColor VOIDVINE_BARK_COLOR = MapColor.TERRACOTTA_GREEN;

    // Voidvine plant (US-008). Properties follow vanilla's cave vines; the stalk item plants the head block.
    /** The growing, lowest segment of a hanging Voidvine (block id {@code voidvine}). */
    public static final RegistrySupplier<VoidvineBlock> VOIDVINE_HEAD =
            ModRegistries.blockOnly("voidvine", () -> new VoidvineBlock(voidvineProperties()));
    /** Every Voidvine segment above the head (block id {@code voidvine_plant}). */
    public static final RegistrySupplier<VoidvinePlantBlock> VOIDVINE_BODY =
            ModRegistries.blockOnly("voidvine_plant", () -> new VoidvinePlantBlock(voidvineProperties()));
    /** The Voidvine stalk: dropped by every segment, used by the wood-set recipes below, planted under Voidsoil. */
    public static final RegistrySupplier<Item> VOIDVINE = ModRegistries.item(
            "voidvine", () -> new ItemNameBlockItem(VOIDVINE_HEAD.get(), ModRegistries.itemProperties()));
    /** Food grown on Voidvines. Unlike chorus fruit it never teleports. */
    public static final RegistrySupplier<Item> VOIDFRUIT = ModRegistries.item(
            "voidfruit",
            () -> new Item(ModRegistries.itemProperties()
                    .food(new FoodProperties.Builder()
                            .nutrition(4)
                            .saturationModifier(0.6F)
                            .build())));

    public static final RegistrySupplier<RotatedPillarBlock> VOIDVINE_BLOCK = ModRegistries.block(
            "voidvine_block",
            () -> new RotatedPillarBlock(
                    logProperties(VOIDVINE_WOOD_COLOR, VOIDVINE_BARK_COLOR, ModWoodTypes.VOIDVINE)));
    public static final RegistrySupplier<RotatedPillarBlock> STRIPPED_VOIDVINE_BLOCK = ModRegistries.block(
            "stripped_voidvine_block",
            () -> new RotatedPillarBlock(
                    logProperties(VOIDVINE_WOOD_COLOR, VOIDVINE_WOOD_COLOR, ModWoodTypes.VOIDVINE)));
    public static final RegistrySupplier<Block> VOIDVINE_PLANKS = ModRegistries.block(
            "voidvine_planks",
            () -> new Block(
                    woodProperties(VOIDVINE_WOOD_COLOR, ModWoodTypes.VOIDVINE).strength(2.0F, 3.0F)));
    public static final RegistrySupplier<Block> VOIDVINE_MOSAIC = ModRegistries.block(
            "voidvine_mosaic",
            () -> new Block(
                    woodProperties(VOIDVINE_WOOD_COLOR, ModWoodTypes.VOIDVINE).strength(2.0F, 3.0F)));
    public static final RegistrySupplier<StairBlock> VOIDVINE_STAIRS = ModRegistries.block(
            "voidvine_stairs",
            () -> new StairBlock(
                    VOIDVINE_PLANKS.get().defaultBlockState(),
                    BlockBehaviour.Properties.ofLegacyCopy(VOIDVINE_PLANKS.get())));
    public static final RegistrySupplier<SlabBlock> VOIDVINE_SLAB = ModRegistries.block(
            "voidvine_slab",
            () -> new SlabBlock(
                    woodProperties(VOIDVINE_WOOD_COLOR, ModWoodTypes.VOIDVINE).strength(2.0F, 3.0F)));
    public static final RegistrySupplier<StairBlock> VOIDVINE_MOSAIC_STAIRS = ModRegistries.block(
            "voidvine_mosaic_stairs",
            () -> new StairBlock(
                    VOIDVINE_MOSAIC.get().defaultBlockState(),
                    BlockBehaviour.Properties.ofLegacyCopy(VOIDVINE_MOSAIC.get())));
    public static final RegistrySupplier<SlabBlock> VOIDVINE_MOSAIC_SLAB = ModRegistries.block(
            "voidvine_mosaic_slab",
            () -> new SlabBlock(
                    woodProperties(VOIDVINE_WOOD_COLOR, ModWoodTypes.VOIDVINE).strength(2.0F, 3.0F)));
    public static final RegistrySupplier<FenceBlock> VOIDVINE_FENCE = ModRegistries.block(
            "voidvine_fence",
            () -> new FenceBlock(woodProperties(VOIDVINE_WOOD_COLOR, ModWoodTypes.VOIDVINE)
                    .forceSolidOn()
                    .strength(2.0F, 3.0F)));
    public static final RegistrySupplier<FenceGateBlock> VOIDVINE_FENCE_GATE = ModRegistries.block(
            "voidvine_fence_gate",
            () -> new FenceGateBlock(
                    ModWoodTypes.VOIDVINE,
                    woodProperties(VOIDVINE_WOOD_COLOR, ModWoodTypes.VOIDVINE)
                            .forceSolidOn()
                            .strength(2.0F, 3.0F)));
    public static final RegistrySupplier<DoorBlock> VOIDVINE_DOOR = ModRegistries.block(
            "voidvine_door",
            () -> new DoorBlock(
                    ModWoodTypes.VOIDVINE_SET,
                    BlockBehaviour.Properties.of()
                            .mapColor(VOIDVINE_WOOD_COLOR)
                            .instrument(NoteBlockInstrument.BASS)
                            .strength(3.0F)
                            .noOcclusion()
                            .ignitedByLava()
                            .pushReaction(PushReaction.DESTROY)),
            block -> new DoubleHighBlockItem(block, ModRegistries.itemProperties()));
    public static final RegistrySupplier<TrapDoorBlock> VOIDVINE_TRAPDOOR = ModRegistries.block(
            "voidvine_trapdoor",
            () -> new TrapDoorBlock(
                    ModWoodTypes.VOIDVINE_SET,
                    BlockBehaviour.Properties.of()
                            .mapColor(VOIDVINE_WOOD_COLOR)
                            .instrument(NoteBlockInstrument.BASS)
                            .strength(3.0F)
                            .noOcclusion()
                            .isValidSpawn((state, level, pos, type) -> false)
                            .ignitedByLava()));
    public static final RegistrySupplier<ButtonBlock> VOIDVINE_BUTTON = ModRegistries.block(
            "voidvine_button",
            () -> new ButtonBlock(
                    ModWoodTypes.VOIDVINE_SET,
                    30,
                    BlockBehaviour.Properties.of().noCollission().strength(0.5F).pushReaction(PushReaction.DESTROY)));
    public static final RegistrySupplier<PressurePlateBlock> VOIDVINE_PRESSURE_PLATE = ModRegistries.block(
            "voidvine_pressure_plate",
            () -> new PressurePlateBlock(
                    ModWoodTypes.VOIDVINE_SET,
                    woodProperties(VOIDVINE_WOOD_COLOR, ModWoodTypes.VOIDVINE)
                            .forceSolidOn()
                            .noCollission()
                            .strength(0.5F)
                            .pushReaction(PushReaction.DESTROY)));
    public static final RegistrySupplier<StandingSignBlock> VOIDVINE_SIGN = ModRegistries.blockOnly(
            "voidvine_sign", () -> new StandingSignBlock(ModWoodTypes.VOIDVINE, signProperties(VOIDVINE_WOOD_COLOR)));
    public static final RegistrySupplier<WallSignBlock> VOIDVINE_WALL_SIGN = ModRegistries.blockOnly(
            "voidvine_wall_sign",
            () -> new WallSignBlock(
                    ModWoodTypes.VOIDVINE, signProperties(VOIDVINE_WOOD_COLOR).dropsLike(VOIDVINE_SIGN.get())));
    public static final RegistrySupplier<CeilingHangingSignBlock> VOIDVINE_HANGING_SIGN = ModRegistries.blockOnly(
            "voidvine_hanging_sign",
            () -> new CeilingHangingSignBlock(ModWoodTypes.VOIDVINE, signProperties(VOIDVINE_WOOD_COLOR)));
    public static final RegistrySupplier<WallHangingSignBlock> VOIDVINE_WALL_HANGING_SIGN = ModRegistries.blockOnly(
            "voidvine_wall_hanging_sign",
            () -> new WallHangingSignBlock(
                    ModWoodTypes.VOIDVINE, signProperties(VOIDVINE_WOOD_COLOR).dropsLike(VOIDVINE_HANGING_SIGN.get())));
    public static final RegistrySupplier<SignItem> VOIDVINE_SIGN_ITEM = ModRegistries.item(
            "voidvine_sign",
            () -> new SignItem(
                    ModRegistries.itemProperties().stacksTo(16), VOIDVINE_SIGN.get(), VOIDVINE_WALL_SIGN.get()));
    public static final RegistrySupplier<HangingSignItem> VOIDVINE_HANGING_SIGN_ITEM = ModRegistries.item(
            "voidvine_hanging_sign",
            () -> new HangingSignItem(
                    VOIDVINE_HANGING_SIGN.get(),
                    VOIDVINE_WALL_HANGING_SIGN.get(),
                    ModRegistries.itemProperties().stacksTo(16)));

    // Astral trees (US-005). Leaves and sapling properties follow vanilla's cherry leaves and sapling.
    public static final RegistrySupplier<AstralLeavesBlock> ASTRAL_LEAVES =
            ModRegistries.block("astral_leaves", () -> new AstralLeavesBlock(leavesProperties(MapColor.COLOR_PURPLE)));
    public static final RegistrySupplier<AstralLeavesBlock> FLOWERING_ASTRAL_LEAVES = ModRegistries.block(
            "flowering_astral_leaves", () -> new AstralLeavesBlock(leavesProperties(MapColor.COLOR_PINK)));
    public static final RegistrySupplier<AstralSaplingBlock> ASTRAL_SAPLING = ModRegistries.block(
            "astral_sapling",
            () -> new AstralSaplingBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .noCollission()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.CHERRY_SAPLING)
                    .pushReaction(PushReaction.DESTROY)));
    public static final RegistrySupplier<FlowerPotBlock> POTTED_ASTRAL_SAPLING = ModRegistries.blockOnly(
            "potted_astral_sapling",
            () -> new FlowerPotBlock(
                    ASTRAL_SAPLING.get(),
                    BlockBehaviour.Properties.of().instabreak().noOcclusion().pushReaction(PushReaction.DESTROY)));

    private ModBlocks() {}

    private static BlockBehaviour.Properties logProperties(MapColor topColor, MapColor sideColor, WoodType type) {
        return BlockBehaviour.Properties.of()
                .mapColor(state -> state.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Y ? topColor : sideColor)
                .instrument(NoteBlockInstrument.BASS)
                .strength(2.0F)
                .sound(type.soundType())
                .ignitedByLava();
    }

    private static BlockBehaviour.Properties voidvineProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .randomTicks()
                .noCollission()
                .lightLevel(state -> state.getValue(Voidvines.FRUIT) ? Voidvines.FRUIT_LIGHT : 0)
                .instabreak()
                .sound(SoundType.CAVE_VINES)
                .pushReaction(PushReaction.DESTROY);
    }

    private static BlockBehaviour.Properties leavesProperties(MapColor color) {
        return BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength(0.2F)
                .randomTicks()
                .sound(SoundType.CHERRY_LEAVES)
                .noOcclusion()
                .isValidSpawn((state, level, pos, type) -> false)
                .isSuffocating((state, level, pos) -> false)
                .isViewBlocking((state, level, pos) -> false)
                .ignitedByLava()
                .pushReaction(PushReaction.DESTROY)
                .isRedstoneConductor((state, level, pos) -> false);
    }

    private static BlockBehaviour.Properties woodProperties(MapColor color, WoodType type) {
        return BlockBehaviour.Properties.of()
                .mapColor(color)
                .instrument(NoteBlockInstrument.BASS)
                .sound(type.soundType())
                .ignitedByLava();
    }

    private static BlockBehaviour.Properties signProperties(MapColor color) {
        return BlockBehaviour.Properties.of()
                .mapColor(color)
                .forceSolidOn()
                .instrument(NoteBlockInstrument.BASS)
                .noCollission()
                .strength(1.0F)
                .ignitedByLava();
    }

    /** Loads this class so its entries are added to the deferred registers. */
    public static void init() {
        ModWoodTypes.init();
        // Fabric reads the vanilla map; NeoForge reads data/neoforge/data_maps/item/compostables.json instead.
        LifecycleEvent.SETUP.register(() -> {
            ComposterBlock.COMPOSTABLES.put(VOID_PETALS.get().asItem(), 0.3F);
            ComposterBlock.COMPOSTABLES.put(GLOWCAP.get().asItem(), 0.65F);
            ComposterBlock.COMPOSTABLES.put(ASTRAL_LEAVES.get().asItem(), 0.3F);
            ComposterBlock.COMPOSTABLES.put(FLOWERING_ASTRAL_LEAVES.get().asItem(), 0.3F);
            ComposterBlock.COMPOSTABLES.put(ASTRAL_SAPLING.get().asItem(), 0.3F);
            ComposterBlock.COMPOSTABLES.put(VOIDFRUIT.get(), 0.3F);
            registerFlammables();
            AxeItemHooks.addStrippable(ASTRAL_LOG.get(), STRIPPED_ASTRAL_LOG.get());
            AxeItemHooks.addStrippable(FLOWERING_ASTRAL_LOG.get(), STRIPPED_ASTRAL_LOG.get());
            AxeItemHooks.addStrippable(ASTRAL_WOOD.get(), STRIPPED_ASTRAL_WOOD.get());
            AxeItemHooks.addStrippable(VOIDVINE_BLOCK.get(), STRIPPED_VOIDVINE_BLOCK.get());
            registerFuels();
        });
    }

    /**
     * Vanilla rates from {@code FireBlock.bootStrap}: planks and derived blocks 5/20, logs and wood 5/5, leaves 30/60.
     */
    private static void registerFlammables() {
        FireBlock fire = (FireBlock) Blocks.FIRE;
        for (RegistrySupplier<? extends Block> block : List.of(
                ASTRAL_PLANKS,
                ASTRAL_SLAB,
                ASTRAL_STAIRS,
                ASTRAL_FENCE,
                ASTRAL_FENCE_GATE,
                VOIDVINE_PLANKS,
                VOIDVINE_MOSAIC,
                VOIDVINE_SLAB,
                VOIDVINE_STAIRS,
                VOIDVINE_MOSAIC_SLAB,
                VOIDVINE_MOSAIC_STAIRS,
                VOIDVINE_FENCE,
                VOIDVINE_FENCE_GATE)) {
            fire.setFlammable(block.get(), 5, 20);
        }
        for (RegistrySupplier<? extends Block> block : List.of(
                ASTRAL_LOG,
                FLOWERING_ASTRAL_LOG,
                ASTRAL_WOOD,
                STRIPPED_ASTRAL_LOG,
                STRIPPED_ASTRAL_WOOD,
                VOIDVINE_BLOCK,
                STRIPPED_VOIDVINE_BLOCK)) {
            fire.setFlammable(block.get(), 5, 5);
        }
        fire.setFlammable(ASTRAL_LEAVES.get(), 30, 60);
        fire.setFlammable(FLOWERING_ASTRAL_LEAVES.get(), 30, 60);
    }

    /**
     * Fuel that vanilla gives bamboo by item rather than by tag (planks, slabs, doors, etc. burn through vanilla
     * item tags). Burn times match {@code AbstractFurnaceBlockEntity.getFuel}'s bamboo entries.
     */
    private static void registerFuels() {
        // Pass items: Architectury's NeoForge registry looks fuel up by the stack's item, so blocks would never match.
        FuelRegistry.register(50, VOIDVINE.get());
        FuelRegistry.register(
                300,
                VOIDVINE_BLOCK.get().asItem(),
                STRIPPED_VOIDVINE_BLOCK.get().asItem(),
                VOIDVINE_MOSAIC.get().asItem(),
                VOIDVINE_MOSAIC_STAIRS.get().asItem());
        FuelRegistry.register(150, VOIDVINE_MOSAIC_SLAB.get().asItem());
    }
}
