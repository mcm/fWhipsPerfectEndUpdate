package io.mcmaster.someonesperfectend.client;

import net.minecraft.client.gui.GuiGraphics;

/** Client helpers for drawing the trinket slot on vanilla inventory screens (and other extra slots). */
public final class TrinketSlotClient {
    /**
     * Trinket slot position in the creative inventory tab. Directly above the off hand (35, 20) would overlap
     * the window's top border, so it sits immediately to the left of the off-hand slot instead.
     */
    public static final int CREATIVE_X = 16;
    public static final int CREATIVE_Y = 20;

    private TrinketSlotClient() {
    }

    /**
     * Draws an empty vanilla-style slot frame (dark top/left edge, light bottom/right edge, gray inside) around
     * the 16x16 item area at ({@code x}, {@code y}); the vanilla inventory textures have no frame there.
     */
    public static void drawSlotFrame(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF8B8B8B);
        graphics.fill(x - 1, y - 1, x + 16, y, 0xFF373737);
        graphics.fill(x - 1, y, x, y + 16, 0xFF373737);
        graphics.fill(x, y + 16, x + 17, y + 17, 0xFFFFFFFF);
        graphics.fill(x + 16, y, x + 17, y + 16, 0xFFFFFFFF);
    }
}
