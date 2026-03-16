package de.maxhenkel.easyvillagers;

// Phase 1 stub — hardcoded defaults. Full config system in Phase 2.
public class ClientConfig {

    public boolean enableRightClickPickup = true;
    public double villagerVolume = 1.0;
    public CycleTradesButtonLocation cycleTradesButtonLocation = CycleTradesButtonLocation.TOP_LEFT;
    public boolean renderBlockContents = true;
    public int blockRenderDistance = 32;

    public enum CycleTradesButtonLocation {
        TOP_LEFT, TOP_RIGHT, NONE
    }

}
