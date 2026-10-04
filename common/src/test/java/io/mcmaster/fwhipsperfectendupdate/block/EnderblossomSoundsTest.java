package io.mcmaster.fwhipsperfectendupdate.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The five Enderblossom sound events only reference vanilla 1.21.1 sound files and all have subtitles. */
class EnderblossomSoundsTest {
    private static final Set<String> EVENTS = Set.of(
            "block.enderblossom.open",
            "block.enderblossom.open_long",
            "block.enderblossom.close",
            "block.enderblossom.close_long",
            "block.enderblossom.idle");
    /** Vanilla 1.21.1 sound folders the entries may use. */
    private static final Set<String> VANILLA_FOLDERS =
            Set.of("minecraft:block/chorus_flower/", "minecraft:mob/endermen/");

    @Test
    void soundsPointOnlyAtVanillaFilesAndHaveSubtitles() throws IOException {
        JsonObject sounds = read("/assets/fwhipsperfectendupdate/sounds.json");
        JsonObject lang = read("/assets/fwhipsperfectendupdate/lang/en_us.json");
        assertEquals(EVENTS, sounds.keySet());
        for (String event : EVENTS) {
            JsonObject entry = sounds.getAsJsonObject(event);
            String subtitle = entry.get("subtitle").getAsString();
            assertTrue(lang.has(subtitle), event + " subtitle " + subtitle + " is not in en_us.json");
            assertFalse(entry.getAsJsonArray("sounds").isEmpty(), event + " has no sounds");
            for (JsonElement sound : entry.getAsJsonArray("sounds")) {
                String name = sound.isJsonObject()
                        ? sound.getAsJsonObject().get("name").getAsString()
                        : sound.getAsString();
                assertTrue(
                        VANILLA_FOLDERS.stream().anyMatch(name::startsWith),
                        event + " uses " + name + ", which is not a vanilla 1.21.1 sound");
                assertFalse(
                        sound.isJsonObject() && sound.getAsJsonObject().has("type"),
                        event + " must reference sound files, not other events");
            }
        }
    }

    private static JsonObject read(String path) throws IOException {
        try (InputStream in = EnderblossomSoundsTest.class.getResourceAsStream(path)) {
            assertTrue(in != null, path + " is missing");
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                    .getAsJsonObject();
        }
    }
}
