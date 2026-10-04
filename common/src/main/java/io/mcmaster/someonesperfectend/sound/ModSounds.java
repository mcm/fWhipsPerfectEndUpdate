package io.mcmaster.someonesperfectend.sound;

import dev.architectury.registry.registries.RegistrySupplier;
import io.mcmaster.someonesperfectend.SomeonesPerfectEnd;
import io.mcmaster.someonesperfectend.registry.ModRegistries;
import net.minecraft.sounds.SoundEvent;

/**
 * The mod's sound events. {@code assets/someonesperfectend/sounds.json} maps each one to vanilla 1.21.1 sound files
 * only; the mod ships no audio.
 */
public final class ModSounds {
    // Enderblossoms (MOD-58): the long sounds play on a blossom's own check, the short ones when a ripple sets it off.
    public static final RegistrySupplier<SoundEvent> ENDERBLOSSOM_OPEN = register("block.enderblossom.open");
    public static final RegistrySupplier<SoundEvent> ENDERBLOSSOM_OPEN_LONG = register("block.enderblossom.open_long");
    public static final RegistrySupplier<SoundEvent> ENDERBLOSSOM_CLOSE = register("block.enderblossom.close");
    public static final RegistrySupplier<SoundEvent> ENDERBLOSSOM_CLOSE_LONG =
            register("block.enderblossom.close_long");
    public static final RegistrySupplier<SoundEvent> ENDERBLOSSOM_IDLE = register("block.enderblossom.idle");

    private ModSounds() {}

    private static RegistrySupplier<SoundEvent> register(String name) {
        return ModRegistries.SOUND_EVENTS.register(
                name, () -> SoundEvent.createVariableRangeEvent(SomeonesPerfectEnd.id(name)));
    }

    /** Loads this class so its entries are added to the deferred register. */
    public static void init() {}
}
