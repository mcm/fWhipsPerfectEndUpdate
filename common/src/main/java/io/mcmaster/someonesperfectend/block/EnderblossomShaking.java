package io.mcmaster.someonesperfectend.block;

import com.google.common.collect.ImmutableSet;
import dev.architectury.registry.registries.RegistrySupplier;
import io.mcmaster.someonesperfectend.SomeonesPerfectEnd;
import io.mcmaster.someonesperfectend.mixin.PoiTypesInvoker;
import io.mcmaster.someonesperfectend.registry.ModRegistries;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Wall Enderblossoms shake when a player looks at them, like an Enderman notices a stare, and when an Enderman
 * teleports within {@link Enderblossoms#RANGE} blocks. A shake is a block event ({@link #shake}) that the client
 * renderer plays as a {@link ShakeCurve}. Potted blossoms never shake.
 */
public final class EnderblossomShaking {
    /** Players this close to the flower head can make it shake by looking at it. */
    public static final double LOOK_RANGE = 16.0;

    public static final ResourceKey<PoiType> POI_KEY =
            ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, SomeonesPerfectEnd.id("enderblossom"));

    @SuppressWarnings("DataFlowIssue") // the data fixer type is nullable
    public static final RegistrySupplier<BlockEntityType<EnderblossomBlockEntity>> BLOCK_ENTITY =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "enderblossom",
                    () -> BlockEntityType.Builder.of(
                                    EnderblossomBlockEntity::new,
                                    ModBlocks.OPEN_ENDERBLOSSOM.get(),
                                    ModBlocks.CLOSED_ENDERBLOSSOM.get())
                            .build(null));

    /** Both wall blossoms' states, so teleporting Endermen find them through the {@link PoiManager}. */
    public static final RegistrySupplier<PoiType> POI_TYPE =
            ModRegistries.POI_TYPES.register("enderblossom", () -> new PoiType(poiStates(), 0, 1));

    private EnderblossomShaking() {}

    /** Loads this class so its entries are added to the deferred registers. */
    public static void init() {
        // NeoForge maps a POI type's states when it is registered; on Fabric only vanilla's own types are mapped.
        POI_TYPE.listen(type -> {
            Set<BlockState> states = type.matchingStates();
            if (!PoiTypes.hasPoi(states.iterator().next())) {
                PoiTypesInvoker.someonesperfectend$registerBlockStates(
                        BuiltInRegistries.POINT_OF_INTEREST_TYPE.getHolderOrThrow(POI_KEY), states);
            }
        });
    }

    private static Set<BlockState> poiStates() {
        return Stream.of(ModBlocks.OPEN_ENDERBLOSSOM.get(), ModBlocks.CLOSED_ENDERBLOSSOM.get())
                .flatMap(block -> block.getStateDefinition().getPossibleStates().stream())
                .collect(ImmutableSet.toImmutableSet());
    }

    /** Sends the shake block event for the wall blossom at {@code pos}; does nothing for anything else. */
    public static void shake(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof EnderblossomBlock) {
            level.blockEvent(pos, state.getBlock(), EnderblossomBlockEntity.SHAKE_EVENT_ID, 0);
        }
    }

    /** Shakes every wall blossom within {@link Enderblossoms#RANGE} blocks of any of {@code positions}. */
    public static void shakeNear(ServerLevel level, Vec3... positions) {
        double rangeSqr = Enderblossoms.RANGE * Enderblossoms.RANGE;
        Set<BlockPos> blossoms = new LinkedHashSet<>();
        for (Vec3 position : positions) {
            if (position == null) {
                continue;
            }
            level.getPoiManager()
                    .getInRange(
                            type -> type.is(POI_KEY),
                            BlockPos.containing(position),
                            (int) Enderblossoms.RANGE + 1,
                            PoiManager.Occupancy.ANY)
                    .map(PoiRecord::getPos)
                    .filter(pos -> Vec3.atCenterOf(pos).distanceToSqr(position) <= rangeSqr)
                    .forEach(blossoms::add);
        }
        blossoms.forEach(pos -> shake(level, pos));
    }

    /**
     * Run from the blossom's regular check: shakes it when a player within {@link #LOOK_RANGE} blocks starts looking
     * at it, and again every {@link LookShakeTracker#REPEAT_INTERVAL} ticks while they keep looking.
     */
    public static void checkLookers(
            ServerLevel level, BlockPos pos, BlockState state, Iterable<? extends Player> players) {
        if (!(state.getBlock() instanceof EnderblossomBlock)
                || !(level.getBlockEntity(pos) instanceof EnderblossomBlockEntity entity)) {
            return;
        }
        AABB flower = lookBox(pos);
        Set<UUID> looking = new HashSet<>();
        for (Player player : players) {
            if (player.level() == level && isLookingAt(player, flower)) {
                looking.add(player.getUUID());
            }
        }
        if (entity.lookers().update(looking, level.getGameTime())) {
            shake(level, pos);
        }
    }

    /**
     * What a player has to look at to make the blossom at {@code pos} shake: its whole block space, which the tilted
     * flower fills. (The Enderman stare test aimed at the flower head only caught its top few pixels.)
     */
    public static AABB lookBox(BlockPos pos) {
        return new AABB(pos);
    }

    /**
     * Whether {@code player} is staring at {@code target}, like an Enderman notices a stare: the player's look ray hits
     * the box within {@link #LOOK_RANGE} blocks, nothing solid is in between, and the player isn't wearing a carved
     * pumpkin.
     */
    public static boolean isLookingAt(Player player, AABB target) {
        if (player.isSpectator()
                || !player.isAlive()
                || player.getItemBySlot(EquipmentSlot.HEAD).is(Items.CARVED_PUMPKIN)) {
            return false;
        }
        Vec3 eye = player.getEyePosition();
        if (target.contains(eye)) {
            return true;
        }
        Vec3 end = eye.add(player.getViewVector(1.0F).normalize().scale(LOOK_RANGE));
        Optional<Vec3> hit = target.clip(eye, end);
        return hit.isPresent()
                && player.level()
                                .clip(new ClipContext(
                                        eye, hit.get(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
                                .getType()
                        == HitResult.Type.MISS;
    }
}
