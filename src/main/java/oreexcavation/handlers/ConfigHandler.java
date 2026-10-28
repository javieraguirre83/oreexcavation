package oreexcavation.handlers;

import com.google.gson.JsonObject;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import oreexcavation.core.ExcavationSettings;
import oreexcavation.core.OreExcavation;
import oreexcavation.groups.BlockBlacklist;
import oreexcavation.groups.BlockGroups;
import oreexcavation.groups.ItemBlacklist;
import oreexcavation.overrides.ToolOverrideHandler;
import oreexcavation.shapes.ShapeRegistry;
import oreexcavation.utils.JsonHelper;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class ConfigHandler {
    public static final ForgeConfigSpec commonSpec;
    public static final ForgeConfigSpec clientSpec;
    public static final ConfigCommon COMMON;
    public static final ConfigClient CLIENT;

    private static final Pattern PATTERN_SPACE = Pattern.compile("\\s");
    private static final File FILE_OVERRIDES = new File("config/oreexcavation/tool_overrides.json");
    private static final File FILE_GROUPS = new File("config/oreexcavation/block_groups.json");

    static {
        Pair<ConfigClient, ForgeConfigSpec> client = new ForgeConfigSpec.Builder().configure(ConfigClient::new);
        clientSpec = client.getRight();
        CLIENT = client.getLeft();
        Pair<ConfigCommon, ForgeConfigSpec> common = new ForgeConfigSpec.Builder().configure(ConfigCommon::new);
        commonSpec = common.getRight();
        COMMON = common.getLeft();
    }

    private static ForgeConfigSpec.IntValue lazyInt(ForgeConfigSpec.Builder b, String name, int def, int min, int max, String comment) {
        b.comment(comment);
        b.translation(getLangKey(name));
        return b.defineInRange(name, def, min, max);
    }

    private static ForgeConfigSpec.DoubleValue lazyDouble(ForgeConfigSpec.Builder b, String name, double def, double min, double max, String comment) {
        b.comment(comment);
        b.translation(getLangKey(name));
        return b.defineInRange(name, def, min, max);
    }

    private static ForgeConfigSpec.BooleanValue lazyBool(ForgeConfigSpec.Builder b, String name, boolean def, String comment) {
        b.comment(comment);
        b.translation(getLangKey(name));
        return b.define(name, def);
    }

    private static ForgeConfigSpec.ConfigValue<String> lazyString(ForgeConfigSpec.Builder b, String name, String def, String comment) {
        b.comment(comment);
        b.translation(getLangKey(name));
        return b.define(name, def);
    }

    private static ForgeConfigSpec.ConfigValue<List<? extends String>> lazyList(ForgeConfigSpec.Builder b, String name, List<String> def, String comment) {
        b.comment(comment);
        b.translation(getLangKey(name));
        return b.defineList(name, def, o -> o instanceof String);
    }

    private static String getLangKey(String name) {
        return OreExcavation.MODID.toLowerCase(Locale.ROOT) + ".config.file." + PATTERN_SPACE.matcher(name.toLowerCase(Locale.ROOT)).replaceAll("_");
    }

    public static void onLoad(ModConfigEvent.Loading event) {
        onConfig(event.getConfig());
    }

    public static void onReload(ModConfigEvent.Reloading event) {
        onConfig(event.getConfig());
    }

    private static void onConfig(ModConfig config) {
        if (!OreExcavation.MODID.equalsIgnoreCase(config.getModId())) return;
        if (config.getType() == ModConfig.Type.CLIENT) {
            CLIENT.apply();
        } else if (config.getType() == ModConfig.Type.COMMON) {
            COMMON.apply();
            loadJsonFiles();
        }
    }

    public static void loadJsonFiles() {
        if (FILE_OVERRIDES.exists()) {
            ToolOverrideHandler.INSTANCE.loadOverrides(JsonHelper.ReadFromFile(FILE_OVERRIDES));
        } else {
            JsonObject json = ToolOverrideHandler.INSTANCE.getDefaultOverrides();
            JsonHelper.WriteToFile(FILE_OVERRIDES, json);
            ToolOverrideHandler.INSTANCE.loadOverrides(json);
        }

        ShapeRegistry.INSTANCE.loadShapes(ShapeRegistry.FILE_SHAPES);

        if (FILE_GROUPS.exists()) {
            BlockGroups.INSTANCE.readFromJson(JsonHelper.ReadFromFile(FILE_GROUPS));
        } else {
            JsonObject json = BlockGroups.INSTANCE.getDefaultJson();
            JsonHelper.WriteToFile(FILE_GROUPS, json);
            BlockGroups.INSTANCE.readFromJson(json);
        }
    }

    public static class ConfigClient {
        public final ForgeConfigSpec.IntValue mineMode;
        public final ForgeConfigSpec.BooleanValue mustHold;
        public final ForgeConfigSpec.BooleanValue useSideHit;
        public final ForgeConfigSpec.BooleanValue showOutline;

        private ConfigClient(ForgeConfigSpec.Builder b) {
            b.push("client");
            mineMode = lazyInt(b, "Mode", 0, -1, 2, "Excavation mode (-1 Disabled, 0 = Keybind, 1 = Sneak, 2 = Always)");
            mustHold = lazyBool(b, "Must Hold", true, "Allows players to cancel excavation by releasing the keys");
            useSideHit = lazyBool(b, "Use Side Hit", true, "Use the side of the block hit to determine shape mining direction");
            showOutline = lazyBool(b, "Show Outline", true, "Show an outline when using the shape mining function");
            b.pop();
        }

        public void apply() {
            ExcavationSettings.mineMode = mineMode.get();
            ExcavationSettings.mustHold = mustHold.get();
            ExcavationSettings.useSideHit = useSideHit.get();
            ExcavationSettings.showOutline = showOutline.get();
        }
    }

    public static class ConfigCommon {
        public final ForgeConfigSpec.IntValue mineSpeed;
        public final ForgeConfigSpec.IntValue mineLimit;
        public final ForgeConfigSpec.IntValue mineRange;
        public final ForgeConfigSpec.DoubleValue exhaustion;
        public final ForgeConfigSpec.IntValue experience;
        public final ForgeConfigSpec.IntValue maxUndos;
        public final ForgeConfigSpec.BooleanValue undoReplace;
        public final ForgeConfigSpec.BooleanValue invertTBlacklist;
        public final ForgeConfigSpec.BooleanValue invertBBlacklist;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> toolBlacklist;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> blockBlacklist;
        public final ForgeConfigSpec.BooleanValue openHand;
        public final ForgeConfigSpec.BooleanValue ignoreTools;
        public final ForgeConfigSpec.BooleanValue toolClass;
        public final ForgeConfigSpec.BooleanValue altTools;
        public final ForgeConfigSpec.BooleanValue tpsGuard;
        public final ForgeConfigSpec.BooleanValue autoPickup;
        public final ForgeConfigSpec.BooleanValue allowShapes;
        public final ForgeConfigSpec.ConfigValue<String> gamestage;

        private ConfigCommon(ForgeConfigSpec.Builder b) {
            b.comment("Common settings").push("common");
            mineLimit = lazyInt(b, "Limit", 128, 1, Integer.MAX_VALUE, "The maximum number of blocks that can be excavated at once");
            mineSpeed = lazyInt(b, "Speed", 64, 1, Integer.MAX_VALUE, "How many blocks per tick can be excavated");
            mineRange = lazyInt(b, "Range", 16, 1, Integer.MAX_VALUE, "How far from the origin an excavation can travel");
            exhaustion = lazyDouble(b, "Ehxaustion", 0.1, 0.0, Double.MAX_VALUE, "Amount of exhaustion per block excavated");
            experience = lazyInt(b, "Experience", 0, 0, Integer.MAX_VALUE, "Experience cost per block excavated");
            openHand = lazyBool(b, "Open Hand", true, "Allow excavation with an open hand");
            invertTBlacklist = lazyBool(b, "Invert Tool Blacklist", false, "Inverts the tool blacklist to function as a whitelist");
            invertBBlacklist = lazyBool(b, "Invert Block Blacklist", false, "Inverts the block blacklist to function as a whitelist");
            ignoreTools = lazyBool(b, "Ignore Tool", false, "Ignores whether or not the held tool is valid");
            altTools = lazyBool(b, "Alt Tools", false, "Use alternate check for tool validity (e.g. swords on webs)");
            toolClass = lazyBool(b, "Only Standard Types", false, "Limit excavation to standard tool types (Picks, Shoves, Axes & Shears)");
            tpsGuard = lazyBool(b, "TPS Guard", true, "Temporarily reduces excavation speed if TPS begins to slow down");
            autoPickup = lazyBool(b, "Auto Pickup", false, "Skips spawning drops in world adding them directly to your inventory");
            allowShapes = lazyBool(b, "Allow Shapes", true, "Allow players to use shape mining");
            maxUndos = lazyInt(b, "Max Undos", 3, 0, Integer.MAX_VALUE, "How many excavations should be kept in undo history (may lead to exploits or instability)");
            undoReplace = lazyBool(b, "Undo Replaceables ", true, "Allows excavation undo function to work through liquids and other replaceable blocks");
            gamestage = lazyString(b, "Game Stage", "", "The default game stage required to unlock excavations (requires gamestages to be installed)");
            toolBlacklist = lazyList(b, "Tool Blacklist", Collections.emptyList(), "Tools blacklisted from excavating");
            blockBlacklist = lazyList(b, "Block Blacklist", Collections.emptyList(), "Blocks blacklisted from being excavated");
            b.pop();
        }

        public void apply() {
            ExcavationSettings.mineLimit = mineLimit.get();
            ExcavationSettings.mineSpeed = mineSpeed.get();
            ExcavationSettings.mineRange = mineRange.get();
            ExcavationSettings.exhaustion = exhaustion.get().floatValue();
            ExcavationSettings.experience = experience.get();
            ExcavationSettings.openHand = openHand.get();
            ExcavationSettings.invertTBlacklist = invertTBlacklist.get();
            ExcavationSettings.invertBBlacklist = invertBBlacklist.get();
            ExcavationSettings.ignoreTools = ignoreTools.get();
            ExcavationSettings.altTools = altTools.get();
            ExcavationSettings.toolClass = toolClass.get();
            ExcavationSettings.tpsGuard = tpsGuard.get();
            ExcavationSettings.autoPickup = autoPickup.get();
            ExcavationSettings.allowShapes = allowShapes.get();
            ExcavationSettings.maxUndos = maxUndos.get();
            ExcavationSettings.undoReplace = undoReplace.get();
            ExcavationSettings.gamestage = gamestage.get();
            BlockBlacklist.INSTANCE.loadList(blockBlacklist.get().toArray(new String[0]));
            ItemBlacklist.INSTANCE.loadList(toolBlacklist.get().toArray(new String[0]));
        }
    }
}
