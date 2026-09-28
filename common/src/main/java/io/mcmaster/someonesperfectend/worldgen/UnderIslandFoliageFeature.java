package io.mcmaster.someonesperfectend.worldgen;

import com.mojang.serialization.Codec;
import io.mcmaster.someonesperfectend.block.ModBlockTags;
import io.mcmaster.someonesperfectend.block.ModBlocks;
import io.mcmaster.someonesperfectend.block.Voidvines;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * Voidsoil on an island's underside with Voidvines hanging from it. Starting at the origin (a surface position), it
 * follows the column down through the island to its exposed bottom block, then replaces the bottom layer in a
 * jittered disc around it with soil. Only bottom blocks with air below and an island block
 * ({@code #someonesperfectend:end_plant_soil}) above are replaced, so the soil always sits under the island. Some
 * soil blocks get a vine hanging into the air below, never reaching below {@code min_y}; vines that would be
 * shorter than {@code vine_length}'s minimum are skipped. The patch stays within 4 blocks of the origin.
 */
public class UnderIslandFoliageFeature extends Feature<UnderIslandFoliageConfiguration> {
    /** How far a patch column's bottom may be above or below the center column's bottom. */
    private static final int BOTTOM_SEARCH = 3;
    private static final long REGION_SALT = 0x5E0D_F011L;

    public UnderIslandFoliageFeature(Codec<UnderIslandFoliageConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<UnderIslandFoliageConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        UnderIslandFoliageConfiguration config = context.config();
        BlockPos origin = context.origin();
        if (!regionSelected(level.getSeed(), origin.getX(), origin.getZ(), config.regionSize(),
                config.regionChance())) {
            return false;
        }
        BlockPos bottom = islandBottom(level, origin);
        if (bottom == null) {
            return false;
        }

        List<BlockPos> soil = new ArrayList<>();
        int radius = config.radius().sample(random);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double distance = Math.sqrt(dx * dx + dz * dz) + random.nextDouble() * 0.8;
                if (distance > radius + 0.4) {
                    continue;
                }
                BlockPos pos = columnBottom(level, config, bottom.offset(dx, 0, dz));
                if (pos != null) {
                    level.setBlock(pos, config.soil().getState(random, pos), Block.UPDATE_CLIENTS);
                    soil.add(pos);
                }
            }
        }

        int minY = config.minY().resolveY(new WorldGenerationContext(context.chunkGenerator(), level));
        for (BlockPos pos : soil) {
            if (random.nextFloat() < config.vineChance()) {
                hangVine(level, random, config, pos, minY);
            }
        }
        return !soil.isEmpty();
    }

    /**
     * Whether the region containing (x, z) gets foliage: a seeded roll per {@code size}×{@code size} region, so
     * about {@code chance} of all regions (and the islands in them) are decorated.
     */
    public static boolean regionSelected(long seed, int x, int z, int size, float chance) {
        if (chance >= 1.0F) {
            return true;
        }
        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
        random.setLargeFeatureWithSalt(seed, Math.floorDiv(x, size), Math.floorDiv(z, size), (int) REGION_SALT);
        return random.nextFloat() < chance;
    }

    /**
     * The lowest block of the first island below {@code origin}: skips air and plants down to the first island
     * block, then follows non-air blocks down. {@code null} if the column has no island.
     */
    private static BlockPos islandBottom(WorldGenLevel level, BlockPos origin) {
        BlockPos.MutableBlockPos cursor = origin.mutable();
        int min = level.getMinBuildHeight();
        while (cursor.getY() > min && !level.getBlockState(cursor).is(ModBlockTags.END_PLANT_SOIL)) {
            cursor.move(0, -1, 0);
        }
        if (cursor.getY() <= min) {
            return null;
        }
        while (cursor.getY() > min && !level.getBlockState(cursor.below()).isAir()) {
            cursor.move(0, -1, 0);
        }
        return cursor.getY() > min ? cursor.immutable() : null;
    }

    /** The lowest replaceable bottom block of the column near {@code center}'s height, or {@code null}. */
    private static BlockPos columnBottom(WorldGenLevel level, UnderIslandFoliageConfiguration config,
            BlockPos center) {
        for (int dy = -BOTTOM_SEARCH; dy <= BOTTOM_SEARCH; dy++) {
            BlockPos pos = center.above(dy);
            if (pos.getY() > level.getMinBuildHeight() && level.getBlockState(pos.below()).isAir()
                    && config.replaceable().test(level, pos)
                    && level.getBlockState(pos.above()).is(ModBlockTags.END_PLANT_SOIL)) {
                return pos;
            }
        }
        return null;
    }

    private static void hangVine(WorldGenLevel level, RandomSource random, UnderIslandFoliageConfiguration config,
            BlockPos soil, int minY) {
        int length = Math.min(config.vineLength().sample(random), soil.getY() - minY);
        int space = 0;
        while (space < length && level.getBlockState(soil.below(space + 1)).isAir()) {
            space++;
        }
        length = Math.min(length, space);
        if (length < config.vineLength().getMinValue()) {
            return;
        }
        for (int i = 1; i <= length; i++) {
            boolean head = i == length;
            BlockState state = head
                    ? ModBlocks.VOIDVINE_HEAD.get().defaultBlockState().setValue(GrowingPlantHeadBlock.AGE,
                            GrowingPlantHeadBlock.MAX_AGE)
                    : ModBlocks.VOIDVINE_BODY.get().defaultBlockState();
            state = state.setValue(Voidvines.FRUIT, random.nextFloat() < config.fruitChance());
            level.setBlock(soil.below(i), state, Block.UPDATE_CLIENTS);
        }
    }
}
