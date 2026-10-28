package oreexcavation.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.text.NumberFormat;

/** A text box that only accepts numbers. */
public class GuiNumberField extends EditBox {
    public static final String REGEX_NUMBER = "[^.0123456789-]";

    public GuiNumberField(Font font, int x, int y, int w, int h) {
        super(font, x, y, w, h, Component.empty());
        setMaxLength(Integer.MAX_VALUE);
    }

    @Override
    public void insertText(String text) {
        super.insertText(text.replaceAll(REGEX_NUMBER, ""));
    }

    @Override
    public void setValue(String text) {
        super.setValue(text.replaceAll(REGEX_NUMBER, ""));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        boolean result = super.mouseClicked(event, doubleClick);
        if (!isFocused()) {
            String value = super.getValue().replaceAll(REGEX_NUMBER, "");
            setValue(value.isEmpty() ? "0" : value);
        }
        return result;
    }

    public Number getNumber() {
        try {
            return NumberFormat.getInstance().parse(super.getValue());
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public String getValue() {
        return getNumber().toString();
    }
}
