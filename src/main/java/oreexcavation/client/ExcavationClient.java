package oreexcavation.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.PacketDistributor;
import oreexcavation.core.ExcavationSettings;
import oreexcavation.core.OreExcavation;
import oreexcavation.handlers.EventHandler;
import oreexcavation.network.PacketExcavation;
import oreexcavation.shapes.ExcavateShape;
import oreexcavation.shapes.ShapeRegistry;
import org.lwjgl.glfw.GLFW;

/** Everything that only runs on the player's computer: keys, shape switching, outline and screens. */
public class ExcavationClient {
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(OreExcavation.MODID, "keys"));
    public static final KeyMapping excavateKey = new KeyMapping("oreexcavation.key.excavate", GLFW.GLFW_KEY_R, CATEGORY);
    public static final KeyMapping shapeKey = new KeyMapping("oreexcavation.key.shape_toggle", GLFW.GLFW_KEY_V, CATEGORY);
    public static final KeyMapping shapeEdit = new KeyMapping("oreexcavation.key.shape_edit", GLFW.GLFW_KEY_B, CATEGORY);

    private static int cTick = 0;

    public static void init(FMLJavaModLoadingContext context) {
        RegisterKeyMappingsEvent.BUS.addListener(event -> {
            event.register(excavateKey);
            event.register(shapeKey);
            event.register(shapeEdit);
        });
        InputEvent.Key.BUS.addListener(ExcavationClient::onKeyEvent);
        InputEvent.MouseScrollingEvent.BUS.addListener((java.util.function.Predicate<InputEvent.MouseScrollingEvent>) ExcavationClient::onScrollEvent);
        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> onClientTick());
        RenderHighlightEvent.Block.BUS.addListener(ShapeOverlay::renderOverlay);
        context.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new GuiOEConfig(parent)));
    }

    private static boolean keyMatches(KeyMapping mapping, int key) {
        return !mapping.isUnbound() && mapping.getKey().getValue() == key;
    }

    public static void onKeyEvent(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() != null || event.getAction() != GLFW.GLFW_PRESS) return;
        boolean toggle = keyMatches(shapeKey, event.getKey());
        boolean edit = keyMatches(shapeEdit, event.getKey());

        if (edit || toggle && (event.getModifiers() & GLFW.GLFW_MOD_CONTROL) != 0) {
            mc.gui.setScreen(new GuiEditShapes());
        } else if (toggle && mc.player != null) {
            ShapeRegistry.INSTANCE.toggleShape();
            showShape(mc.player);
        }
    }

    /** Hold the shape key and scroll to switch shapes. Returns true to cancel the scroll. */
    public static boolean onScrollEvent(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() != null || mc.player == null) return false;
        int dir = (int) Math.signum(event.getDeltaY());
        if (!shapeKey.isDown() || dir == 0) return false;
        if (dir > 0) {
            ShapeRegistry.INSTANCE.toggleShape();
        } else {
            ShapeRegistry.INSTANCE.toggleShapeRev();
        }
        showShape(mc.player);
        return true;
    }

    private static void showShape(LocalPlayer player) {
        ExcavateShape shape = ShapeRegistry.INSTANCE.getActiveShape();
        player.sendOverlayMessage(Component.translatable("oreexcavation.key.shape_msg", shape == null ? "NONE" : shape.getName()));
    }

    /** Stops the excavation when the keys are released (if "Must Hold" is on). */
    private static void onClientTick() {
        cTick = (cTick + 1) % 10;
        Minecraft mc = Minecraft.getInstance();
        if (cTick != 0 || mc.player == null || !EventHandler.isExcavating || !ExcavationSettings.mustHold) return;
        boolean active = true;
        if (ExcavationSettings.mineMode < 0) {
            active = false;
        } else if (ExcavationSettings.mineMode == 0) {
            if (!excavateKey.isDown()) active = false;
        } else if (ExcavationSettings.mineMode == 1 && !mc.player.isCrouching()) {
            active = false;
        }
        if (!active) {
            EventHandler.isExcavating = false;
            CompoundTag tag = new CompoundTag();
            tag.putBoolean("cancel", true);
            OreExcavation.instance.network.send(new PacketExcavation(tag), PacketDistributor.SERVER.noArg());
        }
    }

    /** Is the player currently asking to excavate (key held / sneaking / always)? */
    public static boolean isActivated(LocalPlayer player) {
        if (ExcavationSettings.mineMode < 0) return false;
        if (ExcavationSettings.mineMode == 0 && (excavateKey.isUnbound() || !excavateKey.isDown())) return false;
        return ExcavationSettings.mineMode != 1 || player.isCrouching();
    }

    /** The server tells us a block was broken; if we want to excavate, answer with our shape and direction. */
    public static void acceptClient(PacketExcavation msg) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !isActivated(player)) return;

        CompoundTag tags = msg.getTags();
        Direction facing = player.getDirection();
        Direction side = ExcavationSettings.useSideHit ? Direction.from3DDataValue(tags.getIntOr("side", 0)) : ExcavateShape.getFacing(player);
        ExcavateShape shape = ShapeRegistry.INSTANCE.getActiveShape();

        CompoundTag reply = tags.copy();
        reply.putInt("side", side.get3DDataValue());
        reply.putInt("facing", facing.get3DDataValue());
        if (shape != null) reply.put("shape", shape.writeToNBT(new CompoundTag()));
        EventHandler.isExcavating = true;
        OreExcavation.instance.network.send(new PacketExcavation(reply), PacketDistributor.SERVER.noArg());
    }
}
