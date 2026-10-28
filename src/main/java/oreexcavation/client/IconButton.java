package oreexcavation.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** A clickable icon (gear, share, donate). The lower half of the texture is the hover image. */
public class IconButton extends AbstractWidget {
    private final Runnable onPress;
    private Identifier atlas;
    private int atlasW = 256;
    private int atlasH = 256;
    private int texU = 0;
    private int texV = 0;
    private int texW = 32;
    private int texH = 32;

    public IconButton(int x, int y, int w, int h, Runnable onPress) {
        this(x, y, w, h, onPress, CommonComponents.EMPTY);
    }

    public IconButton(int x, int y, int w, int h, Runnable onPress, Component message) {
        super(x, y, w, h, message);
        this.onPress = onPress;
    }

    public IconButton setAtlas(Identifier atlas, int w, int h) {
        this.atlas = atlas;
        this.atlasW = w;
        this.atlasH = h;
        return this;
    }

    public IconButton setTexCoords(int u, int v, int w, int h) {
        this.texU = u;
        this.texV = v;
        this.texW = w;
        this.texH = h;
        return this;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        if (atlas == null) return;
        int v = isActive() && isHovered() ? texV + texH : texV;
        g.blit(RenderPipelines.GUI_TEXTURED, atlas, getX(), getY(), texU, v, texW, texH, atlasW, atlasH);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        onPress.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
