package io.mcmaster.someonesperfectend;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SomeonesPerfectEndTest {
    @Test
    void idUsesModNamespace() {
        assertEquals("someonesperfectend:foo", SomeonesPerfectEnd.id("foo").toString());
    }
}
