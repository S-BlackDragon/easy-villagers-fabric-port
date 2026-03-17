package de.maxhenkel.easyvillagers.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

@Environment(EnvType.CLIENT)
public class FarmerScreen extends AbstractContainerScreen<FarmerMenu> {

    public FarmerScreen(FarmerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth  = 176;
        imageHeight = 166;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        renderBackground(g, mx, my, delta);
        super.render(g, mx, my, delta);
        renderTooltip(g, mx, my);
    }

    @Override
    protected void renderBg(GuiGraphics g, float delta, int mx, int my) {
        int l = leftPos, t = topPos;

        // Main background (vanilla gray panel)
        g.fill(l, t, l + imageWidth, t + imageHeight, 0xFFC6C6C6);
        // Beveled outer border
        g.fill(l,     t,     l + imageWidth, t + 1,              0xFF555555);
        g.fill(l,     t + imageHeight - 1, l + imageWidth, t + imageHeight, 0xFF555555);
        g.fill(l,     t,     l + 1,         t + imageHeight,    0xFF555555);
        g.fill(l + imageWidth - 1, t, l + imageWidth, t + imageHeight, 0xFF555555);
        g.fill(l + 1, t + 1, l + imageWidth - 1, t + 2,              0xFFFFFFFF);
        g.fill(l + 1, t + 1, l + 2,              t + imageHeight - 1, 0xFFFFFFFF);
        g.fill(l + 1, t + imageHeight - 2, l + imageWidth - 1, t + imageHeight - 1, 0xFF373737);
        g.fill(l + imageWidth - 2, t + 1, l + imageWidth - 1, t + imageHeight - 1, 0xFF373737);

        // Villager input slot (slot 0, menu x=17, y=28)
        drawSlot(g, l + 16, t + 27);
        // Food input slot (slot 1, menu x=62, y=28)
        drawSlot(g, l + 61, t + 27);

        // Arrow from food to output grid
        drawArrow(g, l + 82, t + 36);

        // 2×2 output grid (slots 2–5, menu x=98/116, y=19/37)
        drawSlot(g, l + 97,  t + 18);
        drawSlot(g, l + 115, t + 18);
        drawSlot(g, l + 97,  t + 36);
        drawSlot(g, l + 115, t + 36);

        // Progress bar (farm timer)
        int progress = menu.getFarmProgress();
        g.fill(l + 7,  t + 55, l + 139, t + 62, 0xFF373737);
        g.fill(l + 8,  t + 56, l + 138, t + 61, 0xFF333333);
        if (progress > 0)
            g.fill(l + 8, t + 56, l + 8 + 130 * progress / 100, t + 61, 0xFF55AA00);

        // Separator above player inventory
        g.fill(l + 7, t + 71, l + 169, t + 72, 0xFF888888);
        g.fill(l + 7, t + 72, l + 169, t + 73, 0xFFFFFFFF);

        // Player inventory (menu x=8+col*18, y=83+row*18 / y=141)
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                drawSlot(g, l + 7 + col * 18, t + 82 + row * 18);
        for (int col = 0; col < 9; col++)
            drawSlot(g, l + 7 + col * 18, t + 140);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, (imageWidth - font.width(title)) / 2, 6, 0x404040, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, imageHeight - 94, 0x404040, false);
    }

    /** Vanilla-style sunken slot: dark top+left, light bottom+right, gray fill.
     *  x,y = top-left of the 18×18 slot box (1px before the 16×16 item area). */
    private void drawSlot(GuiGraphics g, int x, int y) {
        g.fill(x,      y,      x + 18, y + 1,  0xFF373737);
        g.fill(x,      y,      x + 1,  y + 18, 0xFF373737);
        g.fill(x,      y + 17, x + 18, y + 18, 0xFFFFFFFF);
        g.fill(x + 17, y,      x + 18, y + 18, 0xFFFFFFFF);
        g.fill(x + 1,  y + 1,  x + 17, y + 17, 0xFF8B8B8B);
    }

    private void drawArrow(GuiGraphics g, int ax, int cy) {
        for (int i = 0; i < 7; i++) {
            int h = 3 - Math.abs(i - 3);
            g.fill(ax + i, cy - h, ax + i + 1, cy + h + 1, 0xFF666666);
        }
    }
}
