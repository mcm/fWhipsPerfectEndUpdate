package io.mcmaster.fwhipsperfectendupdate.fabric.gametest;

import io.mcmaster.fwhipsperfectendupdate.gametest.ModGameTests;
import java.util.Collection;
import java.util.List;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.TestFunction;

/** Fabric-only GameTests (Fabric API integrations), generated like {@link ModGameTests}. */
public final class FabricGameTests {
    public static final List<Class<?>> TEST_CLASSES = List.of(OasisCauldronTransferGameTests.class);

    public FabricGameTests() {}

    @GameTestGenerator
    public Collection<TestFunction> generate() {
        return ModGameTests.functions(TEST_CLASSES);
    }
}
