package io.mcmaster.someonesperfectend.gametest;

import io.mcmaster.someonesperfectend.SomeonesPerfectEnd;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.gametest.framework.TestFunction;

/**
 * Single GameTest entry point shared by both loaders.
 *
 * <p>Fabric and NeoForge build structure template ids differently from a {@link GameTest} annotation, so
 * instead of registering test classes directly, each loader registers only this class. Its generator reads
 * the vanilla {@link GameTest} annotations of every class in {@link #TEST_CLASSES} and uses the
 * {@code template} value as a full template id (default: {@link #EMPTY_TEMPLATE}).
 *
 * <p>To add tests: write public static {@code void name(GameTestHelper)} methods annotated with
 * {@link GameTest} and add their class to {@link #TEST_CLASSES}.
 */
public final class ModGameTests {
    /** An 8x8x8 air structure. */
    public static final String EMPTY_TEMPLATE = SomeonesPerfectEnd.MOD_ID + ":gametest/empty";

    public static final List<Class<?>> TEST_CLASSES = List.of(
            SmokeGameTests.class,
            RecolorGameTests.class,
            EndGroundGameTests.class,
            OasisWaterGameTests.class,
            EndFloraGameTests.class);

    public ModGameTests() {
    }

    @GameTestGenerator
    public Collection<TestFunction> generate() {
        List<TestFunction> functions = new ArrayList<>();
        for (Class<?> testClass : TEST_CLASSES) {
            String suite = testClass.getSimpleName().toLowerCase(Locale.ROOT);
            for (Method method : testClass.getDeclaredMethods()) {
                GameTest test = method.getAnnotation(GameTest.class);
                if (test == null) {
                    continue;
                }
                if (!Modifier.isStatic(method.getModifiers())) {
                    throw new IllegalStateException("GameTest method must be static: " + method);
                }
                String template = test.template().isEmpty() ? EMPTY_TEMPLATE : test.template();
                functions.add(new TestFunction(
                        test.batch(),
                        suite + "." + method.getName().toLowerCase(Locale.ROOT),
                        template,
                        StructureUtils.getRotationForRotationSteps(test.rotationSteps()),
                        test.timeoutTicks(),
                        test.setupTicks(),
                        test.required(),
                        test.manualOnly(),
                        test.attempts(),
                        test.requiredSuccesses(),
                        test.skyAccess(),
                        helper -> invoke(method, helper)));
            }
        }
        return functions;
    }

    private static void invoke(Method method, GameTestHelper helper) {
        try {
            method.invoke(null, helper);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new RuntimeException(e.getCause());
        }
    }
}
