package com.lazify.config;

import com.lazify.overlay.OverlayManager;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Manage players hidden from the overlay via /ov hide. */
public class GuiHiddenPlayers extends GuiScreen {

    private final GuiScreen parent;
    private GuiTextField addField;
    private List<String> hidden = new ArrayList<>();
    private int scroll = 0;

    public GuiHiddenPlayers(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        refreshList();
        int cx = width / 2;
        addField = new GuiTextField(0, fontRendererObj, cx - 100, height - 52, 200, 18);
        addField.setMaxStringLength(16);
        addField.setFocused(true);
        buttonList.add(new GuiButton(0, cx - 105, height - 28, 100, 20, "Add Hide"));
        buttonList.add(new GuiButton(1, cx + 5, height - 28, 100, 20, "Clear All"));
        buttonList.add(new GuiButton(2, cx - 50, height - 4, 100, 20, "Done"));
    }

    private void refreshList() {
        hidden.clear();
        hidden.addAll(OverlayManager.INSTANCE.getHiddenPlayers());
        hidden.sort(String.CASE_INSENSITIVE_ORDER);
        int maxScroll = Math.max(0, hidden.size() - 8);
        if (scroll > maxScroll) scroll = maxScroll;
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            String name = addField.getText().trim();
            if (!name.isEmpty()) {
                OverlayManager.INSTANCE.hidePlayer(name);
                addField.setText("");
                refreshList();
            }
        } else if (button.id == 1) {
            OverlayManager.INSTANCE.clearHiddenPlayers();
            refreshList();
        } else if (button.id == 2) {
            mc.displayGuiScreen(parent);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(parent);
            return;
        }
        addField.textboxKeyTyped(typedChar, keyCode);
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        addField.mouseClicked(mouseX, mouseY, mouseButton);

        int listX = width / 2 - 110;
        int listY = 40;
        for (int i = 0; i < 8; i++) {
            int idx = scroll + i;
            if (idx >= hidden.size()) break;
            int rowY = listY + i * 16;
            if (mouseX >= listX && mouseX < listX + 220 && mouseY >= rowY && mouseY < rowY + 14) {
                OverlayManager.INSTANCE.unhidePlayer(hidden.get(idx));
                refreshList();
                return;
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = org.lwjgl.input.Mouse.getEventDWheel();
        if (dWheel != 0) {
            scroll -= Integer.signum(dWheel);
            int maxScroll = Math.max(0, hidden.size() - 8);
            scroll = Math.max(0, Math.min(scroll, maxScroll));
        }
    }

    @Override
    public void updateScreen() {
        addField.updateCursorCounter();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRendererObj, "Hidden Players", width / 2, 12, 0xFFFFFF);
        drawCenteredString(fontRendererObj, "Click a name to unhide", width / 2, 24, 0x888888);

        int listX = width / 2 - 110;
        int listY = 40;
        for (int i = 0; i < 8; i++) {
            int idx = scroll + i;
            if (idx >= hidden.size()) break;
            String name = hidden.get(idx);
            boolean hover = mouseX >= listX && mouseX < listX + 220
                    && mouseY >= listY + i * 16 && mouseY < listY + i * 16 + 14;
            int color = hover ? 0xFF66AAFF : 0xFFCCCCCC;
            fontRendererObj.drawStringWithShadow(name + " \u00a77(x)", listX, listY + i * 16, color);
        }

        if (hidden.isEmpty()) {
            drawCenteredString(fontRendererObj, "No hidden players", width / 2, listY + 20, 0x666666);
        }

        addField.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
