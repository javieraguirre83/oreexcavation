package oreexcavation.overrides;

import net.minecraft.world.item.ItemStack;
import oreexcavation.core.ExcavationSettings;
import oreexcavation.groups.ItemEntry;

public class ToolOverride {
    private final ItemEntry itemType;
    private int speed;
    private int limit;
    private int range;
    private float exhaustion;
    private int experience;
    private String gamestage;

    public ToolOverride(ItemEntry itemType) {
        this.itemType = itemType;
        this.speed = ExcavationSettings.mineSpeed;
        this.limit = ExcavationSettings.mineLimit;
        this.range = ExcavationSettings.mineRange;
        this.exhaustion = ExcavationSettings.exhaustion;
        this.experience = ExcavationSettings.experience;
        this.gamestage = ExcavationSettings.gamestage;
    }

    public boolean isApplicable(ItemStack stack) {
        return stack != null && !stack.isEmpty() && itemType.checkMatch(stack);
    }

    public void setSpeed(int value) { speed = value; }
    public int getSpeed() { return speed; }
    public void setLimit(int value) { limit = value; }
    public int getLimit() { return limit; }
    public void setRange(int value) { range = value; }
    public int getRange() { return range; }
    public void setExhaustion(float value) { exhaustion = value; }
    public float getExhaustion() { return exhaustion; }
    public void setExperience(int value) { experience = value; }
    public int getExperience() { return experience; }
    public void setGameStage(String value) { gamestage = value; }
    public String getGameStage() { return gamestage; }

    public static ToolOverride readFromString(String text) {
        ItemEntry entry = ItemEntry.readFromString(text);
        return entry == null ? null : new ToolOverride(entry);
    }
}
