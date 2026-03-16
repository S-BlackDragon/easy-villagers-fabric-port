package de.maxhenkel.easyvillagers.gui;

import de.maxhenkel.easyvillagers.network.C2SRerollPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

@Environment(EnvType.CLIENT)
public class TraderScreen extends MerchantScreen {

    private final TraderMenu traderMenu;
    private Button rerollButton;

    public TraderScreen(TraderMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.traderMenu = menu;
    }

    @Override
    protected void init() {
        super.init();
        int buttonX = this.leftPos - 62;
        int buttonY = this.topPos + 16;
        rerollButton = Button.builder(
                Component.translatable("gui.easy_villagers.reroll"),
                btn -> ClientPlayNetworking.send(new C2SRerollPacket(traderMenu.getTraderPos()))
        ).bounds(buttonX, buttonY, 58, 20).build();
        addRenderableWidget(rerollButton);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        // Hide Reroll once the villager has accumulated XP (has been traded with).
        // getTraderXp() is updated client-side via ClientboundMerchantOffersPacket.
        if (rerollButton != null) {
            rerollButton.visible = traderMenu.getTraderXp() == 0;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
    }

}
