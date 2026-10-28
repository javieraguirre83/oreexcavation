package oreexcavation.overrides;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.world.item.ItemStack;
import oreexcavation.core.ExcavationSettings;
import oreexcavation.utils.JsonHelper;

import java.util.HashSet;
import java.util.Set;

public final class ToolOverrideHandler {
    public static final ToolOverrideHandler INSTANCE = new ToolOverrideHandler();
    private final Set<ToolOverride> list = new HashSet<>();

    public void loadOverrides(JsonObject json) {
        list.clear();
        for (JsonElement element : JsonHelper.GetArray(json, "overrides")) {
            if (element == null || !element.isJsonObject()) continue;
            JsonObject obj = element.getAsJsonObject();
            ToolOverride override = ToolOverride.readFromString(JsonHelper.GetString(obj, "itemID", ""));
            if (override == null) continue;
            override.setSpeed(JsonHelper.GetNumber(obj, "speed", ExcavationSettings.mineSpeed).intValue());
            override.setLimit(JsonHelper.GetNumber(obj, "limit", ExcavationSettings.mineLimit).intValue());
            override.setRange(JsonHelper.GetNumber(obj, "range", ExcavationSettings.mineRange).intValue());
            override.setExhaustion(JsonHelper.GetNumber(obj, "exhaustion", ExcavationSettings.exhaustion).floatValue());
            override.setExperience(JsonHelper.GetNumber(obj, "experience", ExcavationSettings.experience).intValue());
            override.setGameStage(JsonHelper.GetString(obj, "gamestage", ExcavationSettings.gamestage));
            list.add(override);
        }
    }

    public ToolOverride getOverride(ItemStack stack) {
        return list.stream().filter(o -> o.isApplicable(stack)).findFirst().orElse(null);
    }

    public JsonObject getDefaultOverrides() {
        JsonObject json = new JsonObject();
        JsonArray array = new JsonArray();
        JsonObject example = new JsonObject();
        example.addProperty("itemID", "examplemod:nerfed_pickaxe");
        example.addProperty("speed", 1);
        example.addProperty("limit", 0);
        example.addProperty("range", 0);
        example.addProperty("exhaustion", 0.1F);
        example.addProperty("experience", 0);
        example.addProperty("gamestage", "");
        array.add(example);
        json.add("overrides", array);
        return json;
    }
}
