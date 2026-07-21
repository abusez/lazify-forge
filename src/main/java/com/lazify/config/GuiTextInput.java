package com.lazify.config;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.function.Consumer;

/** Simple single-field text input screen (API keys, etc.). */
public class GuiTextInput extends GuiScreen {

    private final GuiScreen parent;
    private final String title;
    private final String hint;
    private final String initial;
    private final Consumer<String> onSave;
    private GuiTextField field;

    public GuiTextInput(GuiScreen parent, String title, String hint, String initial, Consumer<String> onSave) {
        this.parent = parent;
        this.title = title;
        this.hint = hint;
        this.initial = initial == null ? "" : initial;
        this.onSave = onSave;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        int cx = width / 2;
        field = new GuiTextField(0, fontRendererObj, cx - 120, height / 2 - 6, 240, 18);
        field.setMaxStringLength(256);
        field.setFocused(true);
        field.setText(initial);
        buttonList.add(new GuiButton(0, cx - 105, height / 2 + 24, 100, 20, "Save"));
        buttonList.add(new GuiButton(1, cx + 5, height / 2 + 24, 100, 20, "Cancel"));
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            onSave.accept(field.getText().trim());
            mc.displayGuiScreen(parent);
        } else if (button.id == 1) {
            mc.displayGuiScreen(parent);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(parent);
            return;
        }
        field.textboxKeyTyped(typedChar, keyCode);
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        field.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void updateScreen() {
        field.updateCursorCounter();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRendererObj, title, width / 2, height / 2 - 36, 0xFFFFFF);
        if (hint != null && !hint.isEmpty()) {
            drawCenteredString(fontRendererObj, hint, width / 2, height / 2 - 24, 0xAAAAAA);
        }
        field.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
