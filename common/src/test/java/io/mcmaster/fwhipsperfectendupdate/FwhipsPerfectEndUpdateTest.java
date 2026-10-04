package io.mcmaster.fwhipsperfectendupdate;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FwhipsPerfectEndUpdateTest {
    @Test
    void idUsesModNamespace() {
        assertEquals(
                "fwhipsperfectendupdate:foo", FwhipsPerfectEndUpdate.id("foo").toString());
    }
}
