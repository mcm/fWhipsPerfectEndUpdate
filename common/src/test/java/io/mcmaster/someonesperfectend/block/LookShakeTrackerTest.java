package io.mcmaster.someonesperfectend.block;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LookShakeTrackerTest {
    private static final UUID ALICE = new UUID(0, 1);
    private static final UUID BOB = new UUID(0, 2);

    @Test
    void shakesWhenAPlayerStartsLooking() {
        LookShakeTracker tracker = new LookShakeTracker();
        assertFalse(tracker.update(Set.of(), 0));
        assertTrue(tracker.update(Set.of(ALICE), 10));
    }

    @Test
    void shakesAtMostEveryFortyTicksWhileLooking() {
        LookShakeTracker tracker = new LookShakeTracker();
        assertTrue(tracker.update(Set.of(ALICE), 0));
        for (long time = 10; time < LookShakeTracker.REPEAT_INTERVAL; time += 10) {
            assertFalse(tracker.update(Set.of(ALICE), time), "shook again at " + time);
        }
        assertTrue(tracker.update(Set.of(ALICE), 40));
        assertFalse(tracker.update(Set.of(ALICE), 50));
        assertTrue(tracker.update(Set.of(ALICE), 80));
    }

    @Test
    void lookingAwayAndBackShakesAgain() {
        LookShakeTracker tracker = new LookShakeTracker();
        assertTrue(tracker.update(Set.of(ALICE), 0));
        assertFalse(tracker.update(Set.of(), 10));
        assertTrue(tracker.update(Set.of(ALICE), 20));
    }

    @Test
    void tracksEachPlayerSeparately() {
        LookShakeTracker tracker = new LookShakeTracker();
        assertTrue(tracker.update(Set.of(ALICE), 0));
        assertTrue(tracker.update(Set.of(ALICE, BOB), 10));
        assertFalse(tracker.update(Set.of(ALICE, BOB), 20));
        // Alice's 40 ticks are up, Bob's aren't.
        assertTrue(tracker.update(Set.of(ALICE, BOB), 40));
        assertFalse(tracker.update(Set.of(BOB), 45));
        assertTrue(tracker.update(Set.of(BOB), 50));
    }
}
