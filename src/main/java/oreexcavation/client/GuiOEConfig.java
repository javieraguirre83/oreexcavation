package oreexcavation.client;

import com.mojang.serialization.Codec;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import oreexcavation.core.OreExcavation;
import oreexcavation.handlers.ConfigHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** In-game settings screen (Mods -> OreExcavation -> Config, or the gear in the shape editor). */
public class GuiOEConfig extends OptionsSubScreen {
    private static int mineMode = 1;
    private static boolean mustHold = true;
    private static boolean useSideHit = true;
    private static boolean showOutline = true;
    private static int mineSpeed = 128;
    private static int mineLimit = 64;
    private static int mineRange = 16;
    private static float exhaustion = 0.1F;
    private static int experience = 0;
    private static int maxUndos = 3;
    private static boolean undoReplace = true;
    private static boolean invertTBlacklist = false;
    private static boolean invertBBlacklist = false;
    private static boolean openHand = false;
    private static boolean ignoreTools = false;
    private static boolean toolClass = false;
    private static boolean altTools = false;
    private static boolean tpsGuard = true;
    private static boolean autoPickup = false;
    private static boolean allowShapes = true;

    private static final Identifier ICO_PATREON = Identifier.fromNamespaceAndPath(OreExcavation.MODID, "textures/gui/ico_donate.png");
    private static final Component TXT_PATREON = Component.translatable("oreexcavation.config.gui.patreon");
    private static final File JSON_FOLDER = new File("config/oreexcavation/");
    private static final String[] modeNames = new String[]{"DISABLED", "KEY", "SNEAK", "ALWAYS"};

    public GuiOEConfig(Screen parent) {
        super(parent, net.minecraft.client.Minecraft.getInstance().options, Component.literal("Ore Excavation - Config"));
    }

    @Override
    protected void addOptions() {
        loadAll();
        List<OptionInstance<?>> options = new ArrayList<>();
        options.add(new OptionInstance<>("oreexcavation.config.gui.mode", OptionInstance.noTooltip(),
                (caption, value) -> Component.translatable("oreexcavation.config.gui.mode").append(": ").append(modeNames[value + 1]),
                new OptionInstance.IntRange(-1, 2), mineMode, v -> mineMode = v));
        options.add(OptionInstance.createBoolean("oreexcavation.config.gui.outline", showOutline, v -> showOutline = v));
        options.add(OptionInstance.createBoolean("oreexcavation.config.gui.must_hold", mustHold, v -> mustHold = v));
        options.add(OptionInstance.createBoolean("oreexcavation.config.gui.side_hit", useSideHit, v -> useSideHit = v));

        // Server settings can only be changed in singleplayer (like the original)
        if (minecraft.hasSingleplayerServer() || minecraft.getConnection() == null) {
            options.add(quickSlider("oreexcavation.config.gui.speed", 1.0, 128.0, 1.0, mineSpeed, v -> mineSpeed = v.intValue()));
            options.add(quickSlider("oreexcavation.config.gui.limit", 8.0, 2048.0, 8.0, mineLimit, v -> mineLimit = v.intValue()));
            options.add(quickSlider("oreexcavation.config.gui.range", 8.0, 256.0, 8.0, mineRange, v -> mineRange = v.intValue()));
            options.add(quickSlider("oreexcavation.config.gui.undos", 0.0, 10.0, 1.0, maxUndos, v -> maxUndos = v.intValue()));
            options.add(quickSlider("oreexcavation.config.gui.exhaustion", 0.0, 100.0, 1.0, exhaustion * 10.0F, v -> exhaustion = v.intValue() / 10.0F));
            options.add(quickSlider("oreexcavation.config.gui.experience", 0.0, 1000.0, 1.0, experience, v -> experience = v.intValue()));
            options.add(OptionInstance.createBoolean("oreexcavation.config.gui.open_hand", openHand, v -> openHand = v));
            options.add(OptionInstance.createBoolean("oreexcavation.config.gui.auto_pickup", autoPickup, v -> autoPickup = v));
            options.add(OptionInstance.createBoolean("oreexcavation.config.gui.ignore_tools", ignoreTools, v -> ignoreTools = v));
            options.add(OptionInstance.createBoolean("oreexcavation.config.gui.alt_tools", altTools, v -> altTools = v));
            options.add(OptionInstance.createBoolean("oreexcavation.config.gui.tool_class", toolClass, v -> toolClass = v));
            options.add(OptionInstance.createBoolean("oreexcavation.config.gui.allow_shapes", allowShapes, v -> allowShapes = v));
            options.add(OptionInstance.createBoolean("oreexcavation.config.gui.undo_replace", undoReplace, v -> undoReplace = v));
            options.add(OptionInstance.createBoolean("oreexcavation.config.gui.tps_guard", tpsGuard, v -> tpsGuard = v));
            options.add(OptionInstance.createBoolean("oreexcavation.config.gui.invert_tbl", invertTBlacklist, v -> invertTBlacklist = v));
            options.add(OptionInstance.createBoolean("oreexcavation.config.gui.invert_bbl", invertBBlacklist, v -> invertBBlacklist = v));
        }
        list.addSmall(options.toArray(new OptionInstance[0]));

        list.addSmall(List.of(
                Button.builder(Component.translatable("oreexcavation.config.gui.reload_json"), b -> ConfigHandler.loadJsonFiles()).width(150).build(),
                Button.builder(Component.translatable("oreexcavation.config.gui.open_folder"), b -> Util.getPlatform().openFile(JSON_FOLDER)).width(150).build()));
    }

    @Override
    protected void addFooter() {
        super.addFooter();
        addRenderableWidget(new IconButton(width - 48, height - 32, 32, 32,
                () -> ConfirmLinkScreen.confirmLinkNow(this, "https://www.patreon.com/Funwayguy"), TXT_PATREON)
                .setAtlas(ICO_PATREON, 32, 64).setTexCoords(0, 0, 32, 32));
    }

    @Override
    public void removed() {
        saveAll();
        super.removed();
    }

    private static OptionInstance<Double> quickSlider(String key, double min, double max, double step, double value, Consumer<Double> onChange) {
        int steps = Mth.ceil((max - min) / step);
        return new OptionInstance<>(key, OptionInstance.noTooltip(),
                (caption, v) -> Component.translatable(key, String.format("%.0f", min + v * step)),
                new OptionInstance.IntRange(0, steps).xmap(i -> (double) i, Double::intValue, false),
                Codec.doubleRange(min, max),
                (value - min) / step,
                v -> onChange.accept(min + v * step));
    }

    private void loadAll() {
        mineMode = ConfigHandler.CLIENT.mineMode.get();
        mustHold = ConfigHandler.CLIENT.mustHold.get();
        useSideHit = ConfigHandler.CLIENT.useSideHit.get();
        showOutline = ConfigHandler.CLIENT.showOutline.get();
        mineSpeed = ConfigHandler.COMMON.mineSpeed.get();
        mineLimit = ConfigHandler.COMMON.mineLimit.get();
        mineRange = ConfigHandler.COMMON.mineRange.get();
        maxUndos = ConfigHandler.COMMON.maxUndos.get();
        undoReplace = ConfigHandler.COMMON.undoReplace.get();
        exhaustion = ConfigHandler.COMMON.exhaustion.get().floatValue();
        experience = ConfigHandler.COMMON.experience.get();
        openHand = ConfigHandler.COMMON.openHand.get();
        autoPickup = ConfigHandler.COMMON.autoPickup.get();
        ignoreTools = ConfigHandler.COMMON.ignoreTools.get();
        altTools = ConfigHandler.COMMON.altTools.get();
        toolClass = ConfigHandler.COMMON.toolClass.get();
        allowShapes = ConfigHandler.COMMON.allowShapes.get();
        invertTBlacklist = ConfigHandler.COMMON.invertTBlacklist.get();
        invertBBlacklist = ConfigHandler.COMMON.invertBBlacklist.get();
        tpsGuard = ConfigHandler.COMMON.tpsGuard.get();
    }

    private void saveAll() {
        ConfigHandler.CLIENT.mineMode.set(mineMode);
        ConfigHandler.CLIENT.mustHold.set(mustHold);
        ConfigHandler.CLIENT.useSideHit.set(useSideHit);
        ConfigHandler.CLIENT.showOutline.set(showOutline);
        ConfigHandler.COMMON.mineSpeed.set(mineSpeed);
        ConfigHandler.COMMON.mineLimit.set(mineLimit);
        ConfigHandler.COMMON.mineRange.set(mineRange);
        ConfigHandler.COMMON.maxUndos.set(maxUndos);
        ConfigHandler.COMMON.undoReplace.set(undoReplace);
        ConfigHandler.COMMON.exhaustion.set((double) exhaustion);
        ConfigHandler.COMMON.experience.set(experience);
        ConfigHandler.COMMON.openHand.set(openHand);
        ConfigHandler.COMMON.autoPickup.set(autoPickup);
        ConfigHandler.COMMON.ignoreTools.set(ignoreTools);
        ConfigHandler.COMMON.altTools.set(altTools);
        ConfigHandler.COMMON.toolClass.set(toolClass);
        ConfigHandler.COMMON.allowShapes.set(allowShapes);
        ConfigHandler.COMMON.invertTBlacklist.set(invertTBlacklist);
        ConfigHandler.COMMON.invertBBlacklist.set(invertBBlacklist);
        ConfigHandler.COMMON.tpsGuard.set(tpsGuard);
        ConfigHandler.CLIENT.apply();
        ConfigHandler.COMMON.apply();
    }
}
