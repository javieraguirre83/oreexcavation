package oreexcavation.groups;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import oreexcavation.utils.JsonHelper;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BlockGroups {
    public static final BlockGroups INSTANCE = new BlockGroups();
    private final Map<BlockEntry, String> staged = new HashMap<>();
    private final Set<List<BlockEntry>> groupList = new HashSet<>();
    private final Set<BlockEntry> strictSubs = new HashSet<>();
    private final Map<Integer, Set<BlockEntry>> groupCache = new HashMap<>();

    public String getStage(BlockState state) {
        return staged.entrySet().stream().filter(e -> e.getKey().checkMatch(state)).map(Map.Entry::getValue).findFirst().orElse(null);
    }

    public Set<BlockEntry> getGroup(BlockState state) {
        int id = Block.getId(state);
        Set<BlockEntry> cached = groupCache.get(id);
        if (cached != null) return cached;
        Set<BlockEntry> result = new HashSet<>();
        for (List<BlockEntry> group : groupList) {
            if (group.stream().anyMatch(e -> e.checkMatch(state))) result.addAll(group);
        }
        groupCache.put(id, result);
        return result;
    }

    public boolean isStrict(@Nonnull BlockState state) {
        return quickCheck(strictSubs, state);
    }

    public boolean quickCheck(@Nonnull Collection<BlockEntry> list, @Nonnull BlockState state) {
        return list.stream().anyMatch(e -> e.checkMatch(state));
    }

    public void readFromJson(JsonObject json) {
        groupCache.clear();
        groupList.clear();
        for (JsonElement element : JsonHelper.GetArray(json, "blockGroups")) {
            if (element == null || !element.isJsonArray()) continue;
            List<BlockEntry> group = new ArrayList<>();
            for (JsonElement e2 : element.getAsJsonArray()) {
                if (e2 != null && e2.isJsonPrimitive()) {
                    BlockEntry entry = BlockEntry.readFromString(e2.getAsString());
                    if (entry != null) group.add(entry);
                }
            }
            if (!group.isEmpty()) groupList.add(group);
        }

        strictSubs.clear();
        for (JsonElement element : JsonHelper.GetArray(json, "strictSubtypes")) {
            if (element != null && element.isJsonPrimitive()) {
                BlockEntry entry = BlockEntry.readFromString(element.getAsString());
                if (entry != null) strictSubs.add(entry);
            }
        }

        staged.clear();
        for (Map.Entry<String, JsonElement> e : JsonHelper.GetObject(json, "gamestages").entrySet()) {
            if (e.getValue() != null && e.getValue().isJsonPrimitive()) {
                BlockEntry entry = BlockEntry.readFromString(e.getKey());
                if (entry != null) staged.put(entry, e.getValue().getAsString());
            }
        }
    }

    private static JsonArray group(String... entries) {
        JsonArray array = new JsonArray();
        for (String s : entries) array.add(new JsonPrimitive(s));
        return array;
    }

    public JsonObject getDefaultJson() {
        JsonObject json = new JsonObject();
        JsonArray groups = new JsonArray();
        groups.add(group("tag:minecraft:leaves"));
        groups.add(group("tag:minecraft:dirt_like"));
        groups.add(group("tag:minecraft:logs"));
        groups.add(group("tag:minecraft:flower_pots"));
        groups.add(group("minecraft:stone", "minecraft:infested_stone", "minecraft:granite", "minecraft:diorite", "minecraft:andesite"));
        groups.add(group("minecraft:cobblestone", "minecraft:infested_cobblestone"));
        groups.add(group("minecraft:stone_bricks", "minecraft:infested_stone_bricks"));
        groups.add(group("minecraft:cracked_stone_bricks", "minecraft:infested_cracked_stone_bricks"));
        groups.add(group("minecraft:mossy_stone_bricks", "minecraft:infested_mossy_stone_bricks"));
        groups.add(group("minecraft:chiseled_stone_bricks", "minecraft:infested_chiseled_stone_bricks"));
        json.add("blockGroups", groups);
        json.add("strictSubtypes", group("tag:minecraft:crops", "minecraft:beetroots", "minecraft:carrots",
                "minecraft:potatoes", "minecraft:wheat", "minecraft:melon_stem", "minecraft:pumpkin_stem"));
        JsonObject stages = new JsonObject();
        stages.addProperty("examplemod:example_block1", "example_stage1");
        stages.addProperty("examplemod:example_block2", "example_stage2");
        json.add("gamestages", stages);
        return json;
    }
}
