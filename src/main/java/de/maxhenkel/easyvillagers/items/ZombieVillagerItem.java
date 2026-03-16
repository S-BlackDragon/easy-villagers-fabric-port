package de.maxhenkel.easyvillagers.items;

import de.maxhenkel.easyvillagers.Main;
import de.maxhenkel.easyvillagers.datacomponents.VillagerData;
import de.maxhenkel.easyvillagers.entity.EasyVillagerEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class ZombieVillagerItem extends Item {

    public ZombieVillagerItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public Component getName(ItemStack stack) {
        Level world = Minecraft.getInstance().level;
        if (world == null) return super.getName(stack);

        EasyVillagerEntity entity = VillagerData.createEasyVillager(stack, world);
        VillagerProfession profession = entity.getVillagerData().getProfession();

        if (!profession.equals(VillagerProfession.NONE) && !profession.equals(VillagerProfession.NITWIT)) {
            Component professionName = Component.translatable(
                    "entity.minecraft.villager." + net.minecraft.core.registries.BuiltInRegistries.VILLAGER_PROFESSION
                            .getKey(profession).getPath()
            );
            return Component.translatable("tooltip.easy_villagers.zombie_villager_profession",
                    Component.translatable("item.easy_villagers.zombie_villager"),
                    professionName);
        }
        return Component.translatable("item.easy_villagers.zombie_villager");
    }

    /** Create a zombie villager item capturing a world ZombieVillager entity. */
    public static ItemStack fromEntity(net.minecraft.world.entity.monster.ZombieVillager entity) {
        ItemStack stack = new ItemStack(ModItems.ZOMBIE_VILLAGER);
        net.minecraft.nbt.CompoundTag nbt = new net.minecraft.nbt.CompoundTag();
        entity.addAdditionalSaveData(nbt);
        stack.set(ModItems.VILLAGER_DATA_COMPONENT, VillagerData.of(nbt));
        return stack;
    }

    /** Create a zombie villager item from a regular villager item (infection). */
    public static ItemStack fromVillager(ItemStack villagerItem) {
        ItemStack stack = new ItemStack(ModItems.ZOMBIE_VILLAGER);
        VillagerData data = villagerItem.get(ModItems.VILLAGER_DATA_COMPONENT);
        if (data != null) {
            stack.set(ModItems.VILLAGER_DATA_COMPONENT, data);
        }
        return stack;
    }
}
