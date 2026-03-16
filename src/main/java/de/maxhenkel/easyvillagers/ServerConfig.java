package de.maxhenkel.easyvillagers;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.Collections;
import java.util.List;

// Phase 1 stub — hardcoded defaults. Full config system in Phase 2.
public class ServerConfig {

    public int breedingTime = 20 * 60;
    public int convertingTime = 20 * 60 * 5;
    public int farmSpeed = 10;
    public List<TagKey<Item>> farmCropsBlacklist = Collections.emptyList();
    public int golemSpawnTime = 20 * 60 * 4;
    public int traderMinRestockTime = 20 * 60;
    public int traderMaxRestockTime = 20 * 60 * 3;
    public int autoTraderMinRestockTime = 20 * 60;
    public int autoTraderMaxRestockTime = 20 * 60 * 3;
    public int autoTraderCooldown = 20;
    public boolean villagerInventorySounds = true;
    public int villagerSoundAmount = 20;
    public int incubatorSpeed = 1; // ticks-based: maxIncubationTicks() = 3600 = 3 min
    public boolean tradeCycling = true;
    public boolean universalReputation = true;

}
