package de.maxhenkel.easyvillagers;

import de.maxhenkel.easyvillagers.blocks.ModBlocks;
import de.maxhenkel.easyvillagers.items.ModItems;
import de.maxhenkel.easyvillagers.items.VillagerItem;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModCreativeTabs {

    public static final CreativeModeTab TAB_EASY_VILLAGERS = FabricItemGroup.builder()
            .icon(() -> new ItemStack(ModItems.VILLAGER))
            .displayItems((features, output) -> {
                output.accept(new ItemStack(ModItems.VILLAGER));
                output.accept(VillagerItem.createBabyVillager());

                output.accept(new ItemStack(ModBlocks.TRADER));
                output.accept(new ItemStack(ModBlocks.AUTO_TRADER));
                output.accept(new ItemStack(ModBlocks.FARMER));
                output.accept(new ItemStack(ModBlocks.BREEDER));
                output.accept(new ItemStack(ModBlocks.CONVERTER));
                output.accept(new ItemStack(ModBlocks.IRON_FARM));
                output.accept(new ItemStack(ModBlocks.INCUBATOR));
            })
            .title(Component.translatable("itemGroup.easy_villagers"))
            .build();

    public static void init() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
                ResourceLocation.fromNamespaceAndPath(Main.MODID, "easy_villagers"),
                TAB_EASY_VILLAGERS);
    }

}
