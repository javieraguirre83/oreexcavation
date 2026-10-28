package oreexcavation.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import oreexcavation.core.OreExcavation;
import oreexcavation.shapes.ExcavateShape;
import oreexcavation.shapes.ShapeRegistry;

/** Copy a shape as a short code, or paste a code to load a shape. */
public class GuiShareCode extends Screen {
    private static final Identifier GUI_TEX = Identifier.fromNamespaceAndPath(OreExcavation.MODID, "textures/gui/share_code.png");
    private static final Component TXT_LOAD = Component.translatable("structure_block.mode.load");
    private static final Component TXT_COPY = Component.translatable("chat.copy");

    private final Screen parent;
    private int guiLeft = 0;
    private int guiTop = 0;
    private EditBox codeField = null;
    private Button btnSave;
    private ExcavateShape shape;

    public GuiShareCode(Screen parent, ExcavateShape shape) {
        super(Component.literal("Ore Excavation - Share Code"));
        this.parent = parent;
        this.shape = shape;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        guiLeft = width / 2 - 128;
        guiTop = height / 2 - 40;

        btnSave = Button.builder(TXT_LOAD, b -> {
            if (shape == null) {
                GuiEditShapes.idx = ShapeRegistry.INSTANCE.getShapeList().size();
                shape = new ExcavateShape();
                ShapeRegistry.INSTANCE.addShape(shape).thenRun(() -> {
                    shape.readShareCode(codeField.getValue());
                    minecraft.gui.setScreen(parent);
                });
            } else {
                shape.readShareCode(codeField.getValue());
                minecraft.gui.setScreen(parent);
            }
        }).bounds(guiLeft + 14, guiTop + 46, 52, 20).build();
        addRenderableWidget(btnSave);
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> minecraft.gui.setScreen(parent))
                .bounds(guiLeft + 190, guiTop + 46, 52, 20).build());
        addRenderableWidget(Button.builder(TXT_COPY, b -> minecraft.keyboardHandler.setClipboard(codeField.getValue()))
                .bounds(guiLeft + 78, guiTop + 46, 100, 20).build());

        codeField = new EditBox(font, guiLeft + 16, guiTop + 16, 224, 16, Component.empty());
        codeField.setMaxLength(256);
        if (shape != null) codeField.setValue(shape.writeShareCode());
        codeField.setResponder(s -> btnSave.active = ExcavateShape.validateShareCode(s));
        addRenderableWidget(codeField);
        btnSave.active = shape != null && ExcavateShape.validateShareCode(codeField.getValue());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.blit(RenderPipelines.GUI_TEXTURED, GUI_TEX, guiLeft, guiTop, 0.0F, 0.0F, 256, 80, 256, 256);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        g.centeredText(font, title, width / 2, guiTop - 16, -1);
    }
}
