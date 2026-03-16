package de.maxhenkel.easyvillagers;

import de.maxhenkel.easyvillagers.blocks.ModBlocks;
import de.maxhenkel.easyvillagers.blocks.tileentity.ModTileEntities;
import de.maxhenkel.easyvillagers.gui.ModMenuTypes;
import de.maxhenkel.easyvillagers.items.ModItems;
import de.maxhenkel.easyvillagers.items.VillagerItem;
import de.maxhenkel.easyvillagers.items.ZombieVillagerItem;
import de.maxhenkel.easyvillagers.loottable.ModLootTables;
import de.maxhenkel.easyvillagers.network.ModPackets;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main implements ModInitializer {

    public static final String MODID = "easy_villagers";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public static final ServerConfig SERVER_CONFIG = new ServerConfig();
    public static final ClientConfig CLIENT_CONFIG = new ClientConfig();

    @Override
    public void onInitialize() {
        ModBlocks.init();
        ModItems.init();
        ModTileEntities.init();
        ModCreativeTabs.init();
        ModLootTables.init();
        ModMenuTypes.init();
        ModPackets.init();
        registerEntityEvents();

        LOGGER.info("Easy Villagers (Fabric) initialized");
    }

    private void registerEntityEvents() {
        // Shift + right-click on a Villager with empty hand → capture as item
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClientSide()) return InteractionResult.PASS;
            if (!(entity instanceof Villager villager)) return InteractionResult.PASS;
            if (entity instanceof ZombieVillager) return InteractionResult.PASS;
            if (!player.getItemInHand(hand).isEmpty()) return InteractionResult.PASS;
            if (!player.isShiftKeyDown()) return InteractionResult.PASS;

            ItemStack stack = VillagerItem.fromEntity(villager);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            villager.discard();
            return InteractionResult.sidedSuccess(false);
        });

        // Shift + right-click on a ZombieVillager with empty hand → capture as item
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClientSide()) return InteractionResult.PASS;
            if (!(entity instanceof ZombieVillager zv)) return InteractionResult.PASS;
            if (!player.getItemInHand(hand).isEmpty()) return InteractionResult.PASS;
            if (!player.isShiftKeyDown()) return InteractionResult.PASS;

            ItemStack stack = ZombieVillagerItem.fromEntity(zv);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            zv.discard();
            return InteractionResult.sidedSuccess(false);
        });
    }

}
