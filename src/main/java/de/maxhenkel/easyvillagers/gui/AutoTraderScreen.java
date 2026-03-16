package de.maxhenkel.easyvillagers.gui;

import de.maxhenkel.easyvillagers.blocks.tileentity.AutoTraderTileentity;
import de.maxhenkel.easyvillagers.network.C2SRerollPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

@Environment(EnvType.CLIENT)
public class AutoTraderScreen extends AbstractContainerScreen<AutoTraderMenu> {

    // Layout constants (gui-relative y positions)
    private static final int Y_TITLE       = 6;
    private static final int Y_XP_BAR      = 18;
    private static final int Y_SEP1        = 27;   // below XP bar
    private static final int Y_TRADE_NUM   = 30;   // "N/total" counter line
    private static final int Y_TRADE_SEL   = 40;   // trade selector items + buttons
    private static final int Y_SEP2        = 60;   // below trade selector
    private static final int Y_SLOTS       = AutoTraderMenu.SLOT_INPUT_A_Y; // 66
    private static final int Y_SLOT_LABELS = 85;   // labels BELOW slots (slots end at 66+16=82)
    private static final int Y_SEP3        = 93;   // below labels
    private static final int Y_INV_LABEL   = 97;
    private static final int Y_INV_ROWS    = 106;  // player inventory rows
    private static final int Y_HOTBAR      = 164;

    // XP thresholds per level (vanilla values)
    private static final int[] XP_PER_LEVEL = {10, 70, 150, 250, Integer.MAX_VALUE};
    private static final String[] LEVEL_NAMES = {"Novato", "Aprendiz", "Artesano", "Experto", "Maestro"};

    private Button prevButton;
    private Button nextButton;
    private Button rerollButton;

    public AutoTraderScreen(AutoTraderMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth  = 176;
        this.imageHeight = 190;  // 164+16+10 bottom padding
    }

    @Override
    protected void init() {
        super.init();

        prevButton = Button.builder(Component.literal("<"), btn -> shiftTrade(-1))
                .bounds(leftPos + 5, topPos + Y_TRADE_SEL, 14, 18).build();
        nextButton = Button.builder(Component.literal(">"), btn -> shiftTrade(1))
                .bounds(leftPos + 157, topPos + Y_TRADE_SEL, 14, 18).build();
        addRenderableWidget(prevButton);
        addRenderableWidget(nextButton);

        rerollButton = Button.builder(
                Component.translatable("gui.easy_villagers.reroll"),
                btn -> ClientPlayNetworking.send(new C2SRerollPacket(menu.getAutoTraderPos()))
        ).bounds(leftPos - 62, topPos + 16, 58, 20).build();
        addRenderableWidget(rerollButton);
    }

    private void shiftTrade(int delta) {
        MerchantOffers offers = menu.getOffers();
        if (offers.isEmpty()) return;
        int next = Math.floorMod(menu.getClientSelectedIndex() + delta, offers.size());
        menu.setSelectionHint(next);
        minecraft.getConnection().send(new ServerboundSelectTradePacket(next));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        MerchantOffers offers = menu.getOffers();
        boolean hasOffers = !offers.isEmpty();
        prevButton.active = hasOffers;
        nextButton.active = hasOffers;
        rerollButton.visible = menu.getTraderXp() == 0;
    }

    // -----------------------------------------------------------------------
    // Rendering
    // -----------------------------------------------------------------------

    @Override
    protected void renderBg(GuiGraphics g, float delta, int mx, int my) {
        int l = leftPos, t = topPos;
        int r = l + imageWidth, b = t + imageHeight;

        // ---- Main background ----
        g.fill(l, t, r, b, 0xFFC6C6C6);
        // Outer border
        g.fill(l, t, r, t + 1, 0xFF555555);
        g.fill(l, b - 1, r, b, 0xFF555555);
        g.fill(l, t, l + 1, b, 0xFF555555);
        g.fill(r - 1, t, r, b, 0xFF555555);
        // Inner light/dark border
        g.fill(l + 1, t + 1, r - 1, t + 2, 0xFFFFFFFF);
        g.fill(l + 1, t + 1, l + 2, b - 1, 0xFFFFFFFF);
        g.fill(l + 1, b - 2, r - 1, b - 1, 0xFF373737);
        g.fill(r - 2, t + 1, r - 1, b - 1, 0xFF373737);

        // ---- Separator lines ----
        g.fill(l + 4, t + Y_SEP1, r - 4, t + Y_SEP1 + 1, 0xFF888888);
        g.fill(l + 4, t + Y_SEP2, r - 4, t + Y_SEP2 + 1, 0xFF888888);
        g.fill(l + 4, t + Y_SEP3, r - 4, t + Y_SEP3 + 1, 0xFF888888);

        // ---- XP bar ----
        int level = menu.getTraderLevel();
        int xp    = menu.getTraderXp();
        int maxXp = (level >= 1 && level <= 5) ? XP_PER_LEVEL[level - 1] : 1;
        g.fill(l + 8, t + Y_XP_BAR, l + 168, t + Y_XP_BAR + 5, 0xFF373737);
        int xpFill = Math.min(160, (int)(160 * xp / (float) maxXp));
        if (xpFill > 0)
            g.fill(l + 8, t + Y_XP_BAR, l + 8 + xpFill, t + Y_XP_BAR + 5, 0xFF55BB22);

        // ---- Trade preview (between < and > buttons) ----
        MerchantOffers offers = menu.getOffers();
        if (!offers.isEmpty()) {
            int sel = Math.min(menu.getClientSelectedIndex(), offers.size() - 1);
            MerchantOffer offer = offers.get(sel);
            int iy = t + Y_TRADE_SEL + 1;  // item y (1px below button top)

            // Layout: costA at x=28, costB at x=50 (if present), "→" text, result at x=116
            g.renderItem(offer.getCostA(), l + 28, iy);
            g.renderItemDecorations(font, offer.getCostA(), l + 28, iy);

            if (!offer.getCostB().isEmpty()) {
                g.renderItem(offer.getCostB(), l + 50, iy);
                g.renderItemDecorations(font, offer.getCostB(), l + 50, iy);
            }

            g.renderItem(offer.getResult(), l + 116, iy);
            g.renderItemDecorations(font, offer.getResult(), l + 116, iy);

            if (offer.isOutOfStock()) {
                g.fill(l + 28, iy + 7, l + 134, iy + 9, 0xBBFF3333);
            }
        }

        // ---- Input / Output slot backgrounds ----
        drawSlotBg(g, l + AutoTraderMenu.SLOT_INPUT_A_X, t + AutoTraderMenu.SLOT_INPUT_A_Y);
        drawSlotBg(g, l + AutoTraderMenu.SLOT_INPUT_B_X, t + AutoTraderMenu.SLOT_INPUT_B_Y);
        drawSlotBg(g, l + AutoTraderMenu.SLOT_OUTPUT_X,  t + AutoTraderMenu.SLOT_OUTPUT_Y);

        // ---- Progress arrow ----
        // Arrow spans x=95..138 (44px total), vertically centered on slots (Y_SLOTS+8)
        int ax    = l + 95;
        int cy    = t + Y_SLOTS + 8;   // vertical center of the 16px slot
        int bodyW = 28, tipW = 9, headH = 9;   // shaft + arrowhead
        int arrowW = bodyW + tipW;              // 44px total

        // Background arrow (dark gray)
        drawArrowShape(g, ax, cy, bodyW, tipW, headH, 0xFF555555);

        // Green fill based on timer progress
        int timer = menu.getSyncedTradeTimer();
        if (timer > 0) {
            int filled = (int)(arrowW * timer / (float) AutoTraderTileentity.TRADE_INTERVAL);
            drawArrowShapeClipped(g, ax, cy, bodyW, tipW, headH, filled, 0xFF55AA22);
        }

        // ---- Player inventory slot backgrounds ----
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                drawSlotBg(g, l + 8 + col * 18, t + Y_INV_ROWS + row * 18);
        // Gap of 4px between last inv row and hotbar
        for (int col = 0; col < 9; col++)
            drawSlotBg(g, l + 8 + col * 18, t + Y_HOTBAR);
    }

    /**
     * Draws a right-pointing arrow:
     *   - Thin shaft (4 px tall) for bodyW columns
     *   - Wide triangle head for tipW columns (full height at base, 2 px at tip)
     * cy = vertical center of the arrow.
     */
    private void drawArrowShape(GuiGraphics g, int ax, int cy, int bodyW, int tipW, int headH, int color) {
        // Shaft (4 px tall, centered)
        g.fill(ax, cy - 2, ax + bodyW, cy + 2, color);
        // Head (triangle): base is headH tall, tip is 2 px
        for (int i = 0; i < tipW; i++) {
            int half = Math.max(1, headH / 2 - (headH / 2 - 1) * i / (tipW - 1));
            g.fill(ax + bodyW + i, cy - half, ax + bodyW + i + 1, cy + half, color);
        }
    }

    /** Same arrow but only fills the first `filledPx` columns (for progress animation). */
    private void drawArrowShapeClipped(GuiGraphics g, int ax, int cy, int bodyW, int tipW, int headH, int filledPx, int color) {
        // Shaft portion
        int shaftFill = Math.min(filledPx, bodyW);
        if (shaftFill > 0)
            g.fill(ax, cy - 2, ax + shaftFill, cy + 2, color);
        // Head portion
        if (filledPx > bodyW) {
            int headFill = filledPx - bodyW;
            for (int i = 0; i < headFill && i < tipW; i++) {
                int half = Math.max(1, headH / 2 - (headH / 2 - 1) * i / (tipW - 1));
                g.fill(ax + bodyW + i, cy - half, ax + bodyW + i + 1, cy + half, color);
            }
        }
    }

    private void drawSlotBg(GuiGraphics g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y,       0xFF373737);
        g.fill(x - 1, y - 1, x,      y + 17,  0xFF373737);
        g.fill(x,     y + 16, x + 17, y + 17,  0xFFFFFFFF);
        g.fill(x + 16, y,    x + 17,  y + 17,  0xFFFFFFFF);
        g.fill(x, y, x + 16, y + 16, 0xFF8B8B8B);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        // Title: "VillagerName - Level"
        int level = menu.getTraderLevel();
        String levelName = (level >= 1 && level <= 5) ? LEVEL_NAMES[level - 1] : "?";
        String titleStr = title.getString() + " - " + levelName;
        g.drawString(font, titleStr, (imageWidth - font.width(titleStr)) / 2, Y_TITLE, 0x404040, false);

        // Trade count "N/total" — centered on its own line above the selector
        MerchantOffers offers = menu.getOffers();
        if (!offers.isEmpty()) {
            String tradeNum = (menu.getClientSelectedIndex() + 1) + "/" + offers.size();
            int tnW = font.width(tradeNum);
            g.drawString(font, tradeNum, (imageWidth - tnW) / 2, Y_TRADE_NUM, 0x404040, false);
        }

        // Slot labels: BELOW the slots, centered on each group
        // Input group center: (SLOT_INPUT_A_X + SLOT_INPUT_B_X + 16) / 2 = (53+91)/2 = 72
        String inLabel  = "Entrada";
        String outLabel = "Salida";
        int inCenter  = (AutoTraderMenu.SLOT_INPUT_A_X + AutoTraderMenu.SLOT_INPUT_B_X + 16) / 2;
        int outCenter = AutoTraderMenu.SLOT_OUTPUT_X + 8;
        g.drawString(font, inLabel,  inCenter  - font.width(inLabel)  / 2, Y_SLOT_LABELS, 0x404040, false);
        g.drawString(font, outLabel, outCenter - font.width(outLabel) / 2, Y_SLOT_LABELS, 0x404040, false);

        // Player inventory label
        g.drawString(font, playerInventoryTitle, inventoryLabelX, Y_INV_LABEL, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        renderBackground(g, mx, my, delta);
        super.render(g, mx, my, delta);
        renderTooltip(g, mx, my);
    }
}
