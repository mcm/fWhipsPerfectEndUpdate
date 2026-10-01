package io.mcmaster.someonesperfectend.worldgen;

import com.mojang.logging.LogUtils;
import io.mcmaster.someonesperfectend.block.ModBlockTags;
import io.mcmaster.someonesperfectend.block.ModBlocks;
import io.mcmaster.someonesperfectend.entity.Endling;
import io.mcmaster.someonesperfectend.entity.Endlings;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Code-placed pieces of the Endling village (US-023). All coordinates are absolute (no piece orientation);
 * every block write is clipped to the chunk box {@code postProcess} is called with.
 */
public final class EndlingVillagePieces {
    /** How far below the floor a hut's end stone foundation may reach to meet the ground. */
    public static final int FOUNDATION_DEPTH = 6;

    private static final int HUT_HEIGHT = 5;
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Test hook: told about every hut endling right after the level was asked to add it, with whether the level
     * took it. Lets GameTests follow endlings that never show up in entity lookups.
     */
    @Nullable
    public static volatile BiConsumer<Endling, Boolean> endlingSpawnListener;

    private EndlingVillagePieces() {}

    private static void place(WorldGenLevel level, BoundingBox box, BlockPos pos, BlockState state) {
        if (box.isInside(pos)) {
            level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        }
    }

    /**
     * A small 5×5 hut: an end stone foundation, a Voidvine plank floor, Astral or Voidvine walls with log
     * corners, fence windows, an open doorway facing the village center, and a glowcap-block roof. One
     * endling, bound to the village center, is spawned inside when the hut generates.
     */
    public static class Hut extends StructurePiece {
        private final BlockPos origin;
        private final int door;
        private final boolean astral;
        private final BlockPos village;

        public Hut(BlockPos origin, int door, boolean astral, BlockPos village) {
            super(
                    EndlingVillages.HUT_PIECE.get(),
                    0,
                    new BoundingBox(
                            origin.getX() - 1,
                            origin.getY() - FOUNDATION_DEPTH,
                            origin.getZ() - 1,
                            origin.getX() + EndlingVillageLayout.HUT_SIZE,
                            origin.getY() + HUT_HEIGHT,
                            origin.getZ() + EndlingVillageLayout.HUT_SIZE));
            this.origin = origin.immutable();
            this.door = door;
            this.astral = astral;
            this.village = village.immutable();
        }

        public Hut(CompoundTag tag) {
            super(EndlingVillages.HUT_PIECE.get(), tag);
            origin = NbtUtils.readBlockPos(tag, "Origin").orElseThrow();
            door = tag.getInt("Door");
            astral = tag.getBoolean("Astral");
            village = NbtUtils.readBlockPos(tag, "Village").orElseThrow();
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            tag.put("Origin", NbtUtils.writeBlockPos(origin));
            tag.putInt("Door", door);
            tag.putBoolean("Astral", astral);
            tag.put("Village", NbtUtils.writeBlockPos(village));
        }

        /** Where this hut's endling spawns: the middle of the floor. */
        public BlockPos endlingPos() {
            return origin.offset(2, 1, 2);
        }

        @Override
        public void postProcess(
                WorldGenLevel level,
                StructureManager structures,
                ChunkGenerator generator,
                RandomSource random,
                BoundingBox box,
                ChunkPos chunk,
                BlockPos pivot) {
            int size = EndlingVillageLayout.HUT_SIZE;
            int last = size - 1;
            BlockState floor = ModBlocks.VOIDVINE_PLANKS.get().defaultBlockState();
            BlockState wall = (astral ? ModBlocks.ASTRAL_PLANKS : ModBlocks.VOIDVINE_PLANKS)
                    .get()
                    .defaultBlockState();
            BlockState post =
                    (astral ? ModBlocks.ASTRAL_LOG.get() : ModBlocks.VOIDVINE_BLOCK.get()).defaultBlockState();
            BlockState window = (astral ? ModBlocks.ASTRAL_FENCE : ModBlocks.VOIDVINE_FENCE)
                    .get()
                    .defaultBlockState();
            BlockState roof = ModBlocks.GLOWCAP_BLOCK.get().defaultBlockState();
            BlockState foundation = Blocks.END_STONE.defaultBlockState();
            BlockState air = Blocks.AIR.defaultBlockState();
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int x = 0; x < size; x++) {
                for (int z = 0; z < size; z++) {
                    // Foundation down to the ground, then the floor.
                    for (int y = -1; y >= -FOUNDATION_DEPTH; y--) {
                        pos.setWithOffset(origin, x, y, z);
                        if (!box.isInside(pos) || level.getBlockState(pos).isSolid()) {
                            break;
                        }
                        level.setBlock(pos, foundation, Block.UPDATE_CLIENTS);
                    }
                    place(level, box, pos.setWithOffset(origin, x, 0, z), floor);
                    boolean edgeX = x == 0 || x == last;
                    boolean edgeZ = z == 0 || z == last;
                    for (int y = 1; y <= 3; y++) {
                        BlockState state;
                        if (edgeX && edgeZ) {
                            state = post;
                        } else if (edgeX || edgeZ) {
                            boolean middle = (edgeX ? z : x) == 2;
                            if (middle && y <= 2 && isDoorSide(x, z, last)) {
                                state = air;
                            } else if (middle && y == 2) {
                                state = window;
                            } else {
                                state = wall;
                            }
                        } else {
                            state = air;
                        }
                        place(level, box, pos.setWithOffset(origin, x, y, z), state);
                    }
                }
            }
            // Keep the doorway reachable from outside.
            int doorX = door == 1 ? size : door == 3 ? -1 : 2;
            int doorZ = door == 0 ? -1 : door == 2 ? size : 2;
            place(level, box, pos.setWithOffset(origin, doorX, 1, doorZ), air);
            place(level, box, pos.setWithOffset(origin, doorX, 2, doorZ), air);
            // A mushroom-cap roof overhanging the walls by one block, with a raised middle.
            for (int x = -1; x <= size; x++) {
                for (int z = -1; z <= size; z++) {
                    place(level, box, pos.setWithOffset(origin, x, 4, z), roof);
                    if (x >= 1 && x <= last - 1 && z >= 1 && z <= last - 1) {
                        place(level, box, pos.setWithOffset(origin, x, 5, z), roof);
                    }
                }
            }
            BlockPos spawn = endlingPos();
            if (box.isInside(spawn)) {
                spawnEndling(level, spawn, village);
            }
        }

        private boolean isDoorSide(int x, int z, int last) {
            return switch (door) {
                case 0 -> z == 0;
                case 1 -> x == last;
                case 2 -> z == last;
                default -> x == 0;
            };
        }
    }

    static void spawnEndling(WorldGenLevel level, BlockPos pos, BlockPos village) {
        Endling endling = Endlings.TYPE.get().create(level.getLevel());
        if (endling == null) {
            LOGGER.warn("Couldn't create the village endling at {}", pos);
            return;
        }
        endling.moveTo(
                pos.getX() + 0.5,
                pos.getY(),
                pos.getZ() + 0.5,
                level.getRandom().nextFloat() * 360.0F,
                0.0F);
        endling.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
        endling.setVillage(village, EndlingVillageLayout.RADIUS);
        // An endling has no passengers, so this is addFreshEntityWithPassengers with its result.
        boolean added = level.addFreshEntity(endling);
        if (added) {
            LOGGER.debug("Spawned village endling {} at {}", endling.getUUID(), pos);
        } else {
            LOGGER.warn("The level refused the village endling at {} (village {})", pos, village);
        }
        BiConsumer<Endling, Boolean> listener = endlingSpawnListener;
        if (listener != null) {
            listener.accept(endling, added);
        }
    }

    /** A {@code huge_glowcap} grown from End soil at {@code base} (the block above the ground). */
    public static class Glowcap extends StructurePiece {
        private final BlockPos base;

        public Glowcap(BlockPos base) {
            super(
                    EndlingVillages.GLOWCAP_PIECE.get(),
                    0,
                    new BoundingBox(
                            base.getX() - 3,
                            base.getY() - 1,
                            base.getZ() - 3,
                            base.getX() + 3,
                            base.getY() + HugeGlowcapFeature.MAX_STEM_HEIGHT + 1,
                            base.getZ() + 3));
            this.base = base.immutable();
        }

        public Glowcap(CompoundTag tag) {
            super(EndlingVillages.GLOWCAP_PIECE.get(), tag);
            base = NbtUtils.readBlockPos(tag, "Base").orElseThrow();
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            tag.put("Base", NbtUtils.writeBlockPos(base));
        }

        /**
         * Runs only in the chunk holding the base, like vanilla's feature pool elements; the cap may reach
         * into neighboring chunks, which the feature step allows.
         */
        @Override
        public void postProcess(
                WorldGenLevel level,
                StructureManager structures,
                ChunkGenerator generator,
                RandomSource random,
                BoundingBox box,
                ChunkPos chunk,
                BlockPos pivot) {
            if (!box.isInside(base)) {
                return;
            }
            // Clear plants (chorus, trees) from the mushroom's space, keeping the island's own soil.
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int y = 0; y <= HugeGlowcapFeature.MAX_STEM_HEIGHT + 1; y++) {
                for (int x = -2; x <= 2; x++) {
                    for (int z = -2; z <= 2; z++) {
                        BlockState state = level.getBlockState(pos.setWithOffset(base, x, y, z));
                        if (!state.isAir() && !state.is(ModBlockTags.END_PLANT_SOIL)) {
                            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                        }
                    }
                }
            }
            if (!level.getBlockState(base.below()).is(ModBlockTags.END_PLANT_SOIL)) {
                level.setBlock(base.below(), ModBlocks.END_TURF.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            }
            level.registryAccess()
                    .registryOrThrow(Registries.CONFIGURED_FEATURE)
                    .getHolder(ModFeatures.HUGE_GLOWCAP_CONFIGURED)
                    .ifPresent(feature -> feature.value().place(level, generator, random, base));
        }
    }
}
