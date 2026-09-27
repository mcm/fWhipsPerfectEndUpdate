package io.mcmaster.someonesperfectend.neoforge;

import io.mcmaster.someonesperfectend.SomeonesPerfectEnd;
import io.mcmaster.someonesperfectend.gametest.ModGameTests;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

@Mod(SomeonesPerfectEnd.MOD_ID)
public final class SomeonesPerfectEndNeoForge {
    public SomeonesPerfectEndNeoForge(IEventBus modBus) {
        SomeonesPerfectEnd.init();
        modBus.addListener(RegisterGameTestsEvent.class, event -> event.register(ModGameTests.class));
    }
}
