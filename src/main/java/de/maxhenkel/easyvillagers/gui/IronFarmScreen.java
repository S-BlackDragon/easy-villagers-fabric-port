package de.maxhenkel.easyvillagers.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

@Environment(EnvType.CLIENT)
public class IronFarmScreen extends AbstractContainerScreen<IronFarmMenu> {

    private static final int COL_BG      = 0xFFC6C6C6;
    private static final int COL_BORDER  = 0xFF373737;
    private static final int COL_SLOT    = 0xFF8B8B8B;
    private static final int COL_SEP     = 0xFF555555;
    private static final int COL_BAR_BG  = 0xFF333333;
    private static final int COL_BAR_FG  = 0xFF888888; // iron gray
    private static final int COL_ARROW   = 0xFF666666;

    public IronFarmScreen(IronFarmMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth  = 176;
        imageHeight = 166;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        super.render(g, mx, my, delta);
        renderTooltip(g, mx, my);
    }

    @Override
    protected void renderBg(GuiGraphics g, float delta, int mx, int my) {
        int l = leftPos, t = topPos;

        // Main background
        g.fill(l, t, l + imageWidth, t + imageHeight, COL_BG);

        // Separator below title
        g.fill(l + 7, t + 17, l + 169, t + 18, COL_SEP);

        // Villager slot (slot 0)
        drawSlot(g, l + 16, t + 27);

        // Arrow ">" from villager to output
        for (int i = 0; i < 7; i++) {
            int half = 3 - Math.abs(i - 3);
            g.fill(l + 40 + i, t + 35 - half, l + 41 + i, t + 35 + half + 1, COL_ARROW);
        }

        // Output slots 2×2 (slots 1–4)
        drawSlot(g, l + 97,  t + 18);
        drawSlot(g, l + 115, t + 18);
        drawSlot(g, l + 97,  t + 36);
        drawSlot(g, l + 115, t + 36);

        // Progress bar
        int progress = menu.getIronProgress(); // 0..100
        int bx = l + 8, by = t + 50, bw = 70, bh = 6;
        g.fill(bx - 1, by - 1, bx + bw + 1, by + bh + 1, COL_BORDER);
        g.fill(bx, by, bx + bw, by + bh, COL_BAR_BG);
        if (progress > 0) {
            g.fill(bx, by, bx + bw * progress / 100, by + bh, COL_BAR_FG);
        }

        // Separator above inventory
        g.fill(l + 7, t + 71, l + 169, t + 72, COL_SEP);

        // Player inventory slot backgrounds
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                drawSlot(g, l + 7 + col * 18, t + 82 + row * 18);
        for (int col = 0; col < 9; col++)
            drawSlot(g, l + 7 + col * 18, t + 140);
    }

    private void drawSlot(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, COL_BORDER);
        g.fill(x + 1, y + 1, x + 17, y + 17, COL_SLOT);
    }
}
