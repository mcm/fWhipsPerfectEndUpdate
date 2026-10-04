package io.mcmaster.fwhipsperfectendupdate.gametest;

import io.mcmaster.fwhipsperfectendupdate.block.ModBlocks;
import io.mcmaster.fwhipsperfectendupdate.entity.Driftjellies;
import io.mcmaster.fwhipsperfectendupdate.entity.Driftjelly;
import io.mcmaster.fwhipsperfectendupdate.entity.DriftjellyDriftGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** A planted Glowcap keeps driftjellies in the 15x15x15 box centered on it. */
public final class DriftjellyContainmentGameTests {
    private static final String TALL = "fwhipsperfectendupdate:gametest/empty_tall";
    /** Glowcap position: near a corner of the 17x17 floor, so the floor reaches well past the box's +x/+z faces. */
    private static final BlockPos GLOWCAP = new BlockPos(3, 1, 3);

    private DriftjellyContainmentGameTests() {}

    /** End turf over the whole 17x17 floor with a Glowcap planted at {@link #GLOWCAP}. */
    private static void plantGlowcap(GameTestHelper helper) {
        for (int x = 0; x <= 16; x++) {
            for (int z = 0; z <= 16; z++) {
                helper.setBlock(x, 0, z, ModBlocks.END_TURF.get());
            }
        }
        helper.setBlock(GLOWCAP, ModBlocks.GLOWCAP.get());
    }

    private static Driftjelly spawn(GameTestHelper helper, double x, double y, double z) {
        return helper.spawn(Driftjellies.TYPE.get(), new Vec3(x, y, z));
    }

    /** Whether a relative position is in the blocks within 7 of the Glowcap on every axis. */
    private static boolean inBox(Vec3 rel) {
        return inAxis(rel.x, GLOWCAP.getX()) && inAxis(rel.y, GLOWCAP.getY()) && inAxis(rel.z, GLOWCAP.getZ());
    }

    private static boolean inAxis(double pos, int center) {
        return pos >= center - Driftjelly.CONTAINMENT_RADIUS && pos <= center + Driftjelly.CONTAINMENT_RADIUS + 1;
    }

    @GameTest(template = TALL, timeoutTicks = 460)
    public static void staysInTheGlowcapBox(GameTestHelper helper) {
        plantGlowcap(helper);
        // Near the box's +x/+z corner, with open floor beyond it to drift over.
        Driftjelly jelly = spawn(helper, 9.5, 4.0, 9.5);
        Vec3 origin = helper.absoluteVec(Vec3.ZERO);
        BlockPos glowcap = helper.absolutePos(GLOWCAP);
        int[] tick = {0};
        helper.onEachTick(() -> {
            if (tick[0] > 400) {
                return;
            }
            Vec3 rel = jelly.position().subtract(origin);
            helper.assertTrue(inBox(rel), "driftjelly left the glowcap's 15x15x15 box: " + rel);
            if (tick[0] == 1) {
                helper.assertTrue(
                        glowcap.equals(jelly.containment()),
                        "driftjelly should be contained by the glowcap, is by " + jelly.containment());
                for (int i = 0; i < 200; i++) {
                    Vec3 target = DriftjellyDriftGoal.pickTarget(jelly);
                    if (target != null) {
                        helper.assertTrue(
                                inBox(target.subtract(origin)),
                                "wander target outside the glowcap's box: " + target.subtract(origin));
                    }
                }
            }
            if (++tick[0] > 400) {
                jelly.discard();
                helper.succeed();
            }
        });
    }

    @GameTest(template = TALL, timeoutTicks = 200)
    public static void driftsBackIntoTheBox(GameTestHelper helper) {
        plantGlowcap(helper);
        Driftjelly jelly = spawn(helper, 8.5, 4.0, 8.5);
        Vec3 origin = helper.absoluteVec(Vec3.ZERO);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(jelly.containment() != null, "driftjelly should be contained by the glowcap");
            // Carried 3.5 blocks past the box's +x face (the box ends at x = 11).
            jelly.setPos(origin.x + 14.5, jelly.getY(), origin.z + 8.5);
        });
        helper.runAfterDelay(
                10,
                () -> helper.succeedWhen(() -> {
                    Vec3 rel = jelly.position().subtract(origin);
                    helper.assertTrue(inBox(rel), "driftjelly has not come back into the box: " + rel);
                    jelly.discard();
                }));
    }

    @GameTest(template = TALL, timeoutTicks = 100)
    public static void onlyPlantedGlowcapsNearbyContain(GameTestHelper helper) {
        plantGlowcap(helper);
        Driftjelly contained = spawn(helper, 8.5, 4.0, 8.5);
        // 10 blocks from the glowcap along x, outside its box.
        Driftjelly free = spawn(helper, 13.5, 4.0, 3.5);
        BlockPos glowcap = helper.absolutePos(GLOWCAP);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(glowcap.equals(contained.containment()), "driftjelly 5 blocks away should be contained");
            helper.assertTrue(free.containment() == null, "driftjelly 10 blocks away should not be contained");

            CompoundTag tag = new CompoundTag();
            contained.saveWithoutId(tag);
            Driftjelly loaded = Driftjellies.TYPE.get().create(helper.getLevel());
            loaded.load(tag);
            helper.assertTrue(glowcap.equals(loaded.containment()), "containment should survive saving");

            helper.setBlock(GLOWCAP, Blocks.AIR);
        });
        helper.runAfterDelay(
                8,
                () -> helper.succeedWhen(() -> {
                    helper.assertTrue(
                            contained.containment() == null, "breaking the glowcap should free the driftjelly");
                    contained.discard();
                    free.discard();
                }));
    }
}
