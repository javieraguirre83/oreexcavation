package oreexcavation.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import oreexcavation.core.OreExcavation;
import oreexcavation.shapes.ExcavateShape;
import oreexcavation.shapes.ShapeRegistry;

/** Shape editor: left click toggles a block of the 5x5 grid, right click moves the crosshair (start point). */
public class GuiEditShapes extends Screen {
    private static final Identifier GUI_TEX = Identifier.fromNamespaceAndPath(OreExcavation.MODID, "textures/gui/edit_shapes.png");
    private static final Identifier STONE_TEX = Identifier.withDefaultNamespace("textures/block/stone.png");
    private static final Identifier CROSSHAIR = Identifier.withDefaultNamespace("hud/crosshair");
    private static final Identifier ICO_SHARE = Identifier.fromNamespaceAndPath(OreExcavation.MODID, "textures/gui/ico_share.png");
    private static final Identifier ICO_GEAR = Identifier.fromNamespaceAndPath(OreExcavation.MODID, "textures/gui/ico_gear.png");
    private static final Component TXT_TITLE = Component.literal("Ore Excavation - Shape Editor");
    private static final Component TXT_CONFIG = Component.translatable("fml.menu.mods.config");
    private static final Component TXT_SHARE = Component.translatable("oreexcavation.config.gui.share_code");

    private int guiLeft = 0;
    private int guiTop = 0;
    protected static int idx = 0;
    private ExcavateShape curShape = null;
    private EditBox txtField;
    private GuiNumberField depthField;
    private GuiNumberField reverseField;
    private GuiNumberField strideField;
    private GuiNumberField elevField;
    private Button btnLeft;
    private Button btnRight;
    private Button btnRemove;

    public GuiEditShapes() {
        super(TXT_TITLE);
    }

    @Override
    protected void init() {
        guiLeft = width / 2 - 128;
        guiTop = height / 2 - 128;

        btnLeft = Button.builder(Component.literal("<"), b -> { idx--; refreshShape(); })
                .bounds(guiLeft + 14, guiTop + 14, 20, 20).build();
        btnRight = Button.builder(Component.literal(">"), b -> { idx++; refreshShape(); })
                .bounds(guiLeft + 222, guiTop + 14, 20, 20).build();
        Button btnAdd = Button.builder(Component.literal("+"), b -> {
            idx = ShapeRegistry.INSTANCE.getShapeList().size();
            ExcavateShape shape = new ExcavateShape();
            shape.setMask(473536);
            ShapeRegistry.INSTANCE.addShape(shape).thenRun(this::refreshShape);
        }).bounds(guiLeft + 118, guiTop + 222, 20, 20).build();
        btnRemove = Button.builder(Component.literal("-"), b -> {
            ShapeRegistry.INSTANCE.removeShape(curShape).thenRun(this::refreshShape);
            refreshShape();
        }).bounds(guiLeft + 54, guiTop + 222, 20, 20).build();

        txtField = new EditBox(font, guiLeft + 48, guiTop + 16, 160, 16, Component.empty());
        depthField = new GuiNumberField(font, guiLeft + 184, guiTop + 56, 32, 16);
        reverseField = new GuiNumberField(font, guiLeft + 184, guiTop + 88, 32, 16);
        strideField = new GuiNumberField(font, guiLeft + 184, guiTop + 136, 32, 16);
        elevField = new GuiNumberField(font, guiLeft + 184, guiTop + 168, 32, 16);

        txtField.setResponder(s -> { if (curShape != null) curShape.setName(txtField.getValue()); });
        depthField.setResponder(s -> { if (curShape != null) curShape.setMaxDepth(depthField.getNumber().intValue()); });
        reverseField.setResponder(s -> { if (curShape != null) curShape.setRevDepth(reverseField.getNumber().intValue()); });
        strideField.setResponder(s -> { if (curShape != null) curShape.setStairStride(strideField.getNumber().intValue()); });
        elevField.setResponder(s -> { if (curShape != null) curShape.setStairElev(elevField.getNumber().intValue()); });

        IconButton gear = new IconButton(guiLeft + 216, guiTop + 216, 32, 32,
                () -> minecraft.gui.setScreen(new GuiOEConfig(this)), TXT_CONFIG)
                .setAtlas(ICO_GEAR, 32, 64).setTexCoords(0, 0, 32, 32);
        IconButton share = new IconButton(guiLeft + 184, guiTop + 216, 32, 32,
                () -> minecraft.gui.setScreen(new GuiShareCode(this, curShape)), TXT_SHARE)
                .setAtlas(ICO_SHARE, 32, 64).setTexCoords(0, 0, 32, 32);

        addRenderableWidget(gear);
        addRenderableWidget(share);
        addRenderableWidget(txtField);
        addRenderableWidget(depthField);
        addRenderableWidget(reverseField);
        addRenderableWidget(strideField);
        addRenderableWidget(elevField);
        addRenderableWidget(btnLeft);
        addRenderableWidget(btnRight);
        addRenderableWidget(btnAdd);
        addRenderableWidget(btnRemove);
        refreshShape();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.blit(RenderPipelines.GUI_TEXTURED, GUI_TEX, guiLeft, guiTop, 0.0F, 0.0F, 256, 256, 256, 256);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        if (curShape == null) return;
        int mask = curShape.getShapeMask();
        int reticle = curShape.getReticle();
        g.pose().pushMatrix();
        g.pose().translate(guiLeft + 144, guiTop + 176);
        g.pose().scale(2.0F, 2.0F);
        for (int i = 0; i < 25; i++) {
            if ((mask & 1 << i) == 0) {
                g.blit(RenderPipelines.GUI_TEXTURED, STONE_TEX, -(i % 5) * 16, -(i / 5) * 16, 0.0F, 0.0F, 16, 16, 16, 16);
            }
        }
        g.blitSprite(RenderPipelines.CROSSHAIR, CROSSHAIR, -(reticle % 5) * 16 + 4, -(reticle / 5) * 16 + 4, 8, 8);
        g.pose().popMatrix();
    }

    @Override
    public void onClose() {
        ShapeRegistry.INSTANCE.saveShapes(ShapeRegistry.FILE_SHAPES);
        super.onClose();
    }

    public void refreshShape() {
        if (ShapeRegistry.INSTANCE.getShapeList().isEmpty()) {
            idx = 0;
            curShape = null;
        } else {
            idx = Mth.clamp(idx, 0, ShapeRegistry.INSTANCE.getShapeList().size() - 1);
            curShape = ShapeRegistry.INSTANCE.getShapeAt(idx + 1);
        }
        if (curShape != null) {
            txtField.setValue(curShape.getName());
            depthField.setValue(curShape.getMaxDepth() + "");
            reverseField.setValue(curShape.getRevDepth() + "");
            strideField.setValue(curShape.getStairStride() + "");
            elevField.setValue(curShape.getStairElevation() + "");
            btnRemove.active = true;
        } else {
            txtField.setValue("");
            depthField.setValue("-1");
            reverseField.setValue("0");
            strideField.setValue("0");
            elevField.setValue("0");
            btnRemove.active = false;
        }
        btnLeft.active = idx > 0;
        btnRight.active = idx + 1 < ShapeRegistry.INSTANCE.getShapeList().size();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        txtField.setFocused(false);
        depthField.setFocused(false);
        reverseField.setFocused(false);
        strideField.setFocused(false);
        elevField.setFocused(false);
        boolean result = super.mouseClicked(event, doubleClick);

        double mx = event.x();
        double my = event.y();
        int gx = (int) Math.floor((mx - guiLeft - 16.0) / 32.0);
        int gy = (int) Math.floor((my - guiTop - 48.0) / 32.0);
        if (curShape != null && mx - guiLeft >= 16.0 && gx < 5 && my - guiTop >= 48.0 && gy < 5) {
            if (event.button() == 0) {
                curShape.setMask(4 - gx, 4 - gy, (curShape.getShapeMask() & 1 << (4 - gy) * 5 + (4 - gx)) == 0);
            } else if (event.button() == 1) {
                curShape.setReticle(4 - gx, 4 - gy);
            }
        }
        return result;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
