package io.mcmaster.someonesperfectend.block;

import io.mcmaster.someonesperfectend.SomeonesPerfectEnd;
import io.mcmaster.someonesperfectend.mixin.BlockSetTypeInvoker;
import io.mcmaster.someonesperfectend.mixin.WoodTypeInvoker;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;

/**
 * The mod's wood and block set types. Names are namespaced ({@code someonesperfectend:astral}); both loaders
 * resolve sign textures from them as {@code someonesperfectend:entity/signs/astral}.
 */
public final class ModWoodTypes {
    /** Astral wood sounds like cherry, the other pastel wood. */
    public static final BlockSetType ASTRAL_SET = BlockSetTypeInvoker.someonesperfectend$register(new BlockSetType(
            SomeonesPerfectEnd.id("astral").toString(), true, true, true, BlockSetType.PressurePlateSensitivity.EVERYTHING,
            SoundType.CHERRY_WOOD,
            SoundEvents.CHERRY_WOOD_DOOR_CLOSE, SoundEvents.CHERRY_WOOD_DOOR_OPEN,
            SoundEvents.CHERRY_WOOD_TRAPDOOR_CLOSE, SoundEvents.CHERRY_WOOD_TRAPDOOR_OPEN,
            SoundEvents.CHERRY_WOOD_PRESSURE_PLATE_CLICK_OFF, SoundEvents.CHERRY_WOOD_PRESSURE_PLATE_CLICK_ON,
            SoundEvents.CHERRY_WOOD_BUTTON_CLICK_OFF, SoundEvents.CHERRY_WOOD_BUTTON_CLICK_ON));

    public static final WoodType ASTRAL = WoodTypeInvoker.someonesperfectend$register(new WoodType(
            SomeonesPerfectEnd.id("astral").toString(), ASTRAL_SET,
            SoundType.CHERRY_WOOD, SoundType.CHERRY_WOOD_HANGING_SIGN,
            SoundEvents.CHERRY_WOOD_FENCE_GATE_CLOSE, SoundEvents.CHERRY_WOOD_FENCE_GATE_OPEN));

    private ModWoodTypes() {
    }

    /** Loads this class so the types are registered before any client code snapshots {@link WoodType#values()}. */
    public static void init() {
    }

    public static boolean isModWoodType(WoodType type) {
        return type.name().startsWith(SomeonesPerfectEnd.MOD_ID + ":");
    }

    /**
     * Whether {@code state} is one of our signs that the vanilla sign block entity type should accept. Vanilla
     * hardcodes the valid blocks of {@link BlockEntityType#SIGN} and {@link BlockEntityType#HANGING_SIGN}.
     */
    public static boolean isExtraValidSign(BlockEntityType<?> type, BlockState state) {
        if (!(state.getBlock() instanceof SignBlock sign) || !isModWoodType(sign.type())) {
            return false;
        }
        if (type == BlockEntityType.SIGN) {
            return sign instanceof StandingSignBlock || sign instanceof WallSignBlock;
        }
        if (type == BlockEntityType.HANGING_SIGN) {
            return sign instanceof CeilingHangingSignBlock || sign instanceof WallHangingSignBlock;
        }
        return false;
    }
}
