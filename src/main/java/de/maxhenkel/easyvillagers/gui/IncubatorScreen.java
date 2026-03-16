package de.maxhenkel.easyvillagers.gui;

import de.maxhenkel.easyvillagers.Main;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

@Environment(EnvType.CLIENT)
public class IncubatorScreen extends AbstractContainerScreen<IncubatorMenu> {

    // Custom texture derived from the furnace — same style, slots aligned at y=35
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/gui/container/incubator.png");

    public IncubatorScreen(IncubatorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        // Draw incubator background (furnace-style, fuel slot removed, slots aligned)
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        // Draw progress arrow fill (UV 176,14 = white arrow sprite from furnace)
        int progress = menu.getIncubationProgress();
        if (progress > 0) {
            graphics.blit(TEXTURE, leftPos + 79, topPos + 34, 176, 14, progress, 16);
        }
    }
}
