package de.maxhenkel.easyvillagers.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.trading.Merchant;

public class TraderMenu extends MerchantMenu {

    private final BlockPos traderPos;

    /** Server-side constructor: has the real Merchant and the block position. */
    public TraderMenu(int containerId, Inventory playerInventory, Merchant merchant, BlockPos traderPos) {
        super(containerId, playerInventory, merchant);
        this.traderPos = traderPos;
    }

    /** Client-side constructor: created by ExtendedScreenHandlerType with the BlockPos extra data. */
    public TraderMenu(int containerId, Inventory playerInventory, BlockPos traderPos) {
        super(containerId, playerInventory);
        this.traderPos = traderPos;
    }

    public BlockPos getTraderPos() {
        return traderPos;
    }

    @Override
    public MenuType<?> getType() {
        return ModMenuTypes.TRADER;
    }

    /**
     * Override stillValid to check distance to the trader block instead of
     * the villager entity (which has no world position — it lives in the tile entity).
     * Vanilla MerchantMenu delegates to Merchant.stillValid which checks entity
     * distance, causing the GUI to close immediately since the fake entity is at 0,0,0.
     */
    @Override
    public boolean stillValid(Player player) {
        double dx = traderPos.getX() + 0.5 - player.getX();
        double dy = traderPos.getY() + 0.5 - player.getY();
        double dz = traderPos.getZ() + 0.5 - player.getZ();
        return dx * dx + dy * dy + dz * dz <= 64.0; // 8 blocks
    }

}
