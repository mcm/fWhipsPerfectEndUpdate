package io.mcmaster.someonesperfectend.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class EnderBedRulesTest {
    /** Vanilla dye order. */
    private static final List<String> COLORS = List.of("white", "orange", "magenta", "light_blue", "yellow", "lime",
            "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black");

    @Test
    void explodesEverywhereButTheOverworldAndTheEnd() {
        assertFalse(EnderBedRules.explodesIn(ResourceLocation.withDefaultNamespace("overworld")));
        assertFalse(EnderBedRules.explodesIn(ResourceLocation.withDefaultNamespace("the_end")));
        assertTrue(EnderBedRules.explodesIn(ResourceLocation.withDefaultNamespace("the_nether")));
        assertTrue(EnderBedRules.explodesIn(ResourceLocation.fromNamespaceAndPath("somemod", "overworld")));
        assertTrue(EnderBedRules.explodesIn(ResourceLocation.fromNamespaceAndPath("somemod", "the_end")));
    }

    @Test
    void dropPointIsBelowTheHeadWithRoomForAPlayer() {
        BlockPos head = new BlockPos(10, 64, -3);
        assertEquals(new BlockPos(10, 63, -3), EnderBedRules.dropPoint(head));
        assertEquals(List.of(new BlockPos(10, 63, -3), new BlockPos(10, 62, -3)),
                List.of(EnderBedRules.arrivalSpace(head)));
        // Feet at the bottom of the lower block: a 1.8-block player ends 0.2 below the bed's head half.
        assertEquals(new Vec3(10.5, 62.0, -2.5), EnderBedRules.arrivalPos(head));
    }

    private static JsonObject read(String path) throws IOException {
        try (InputStream in = EnderBedRulesTest.class.getResourceAsStream("/" + path)) {
            assertNotNull(in, "missing " + path);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @Test
    void everyVanillaBedHasASameColorBarter() throws IOException {
        for (String color : COLORS) {
            JsonObject barter = read("data/someonesperfectend/endling_special_barter/" + color + "_bed.json");
            assertEquals("minecraft:" + color + "_bed", barter.get("input").getAsString());
            String table = barter.get("loot_table").getAsString();
            assertEquals("someonesperfectend:gameplay/endling_barter/" + color + "_bed", table);

            JsonObject loot = read("data/someonesperfectend/loot_table/" + table.substring(table.indexOf(':') + 1) + ".json");
            var pools = loot.getAsJsonArray("pools");
            assertEquals(1, pools.size(), color);
            JsonObject pool = pools.get(0).getAsJsonObject();
            assertEquals(1, pool.get("rolls").getAsInt(), color);
            var entries = pool.getAsJsonArray("entries");
            assertEquals(1, entries.size(), color);
            JsonObject entry = entries.get(0).getAsJsonObject();
            assertEquals("someonesperfectend:" + color + "_ender_bed", entry.get("name").getAsString());
            assertFalse(entry.has("functions"), "one bed per barter");
        }
    }
}
