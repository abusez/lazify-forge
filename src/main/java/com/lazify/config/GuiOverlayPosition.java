package com.lazify.config;

import com.lazify.overlay.OverlayManager;
import com.lazify.overlay.OverlayTheme;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import java.io.IOException;

/** Drag the live overlay (real stats/columns) to reposition it. */
public class GuiOverlayPosition extends GuiScreen {

    private final GuiScreen parent;
    private int overlayX, overlayY;
    private boolean dragging = false;
    private int dragOffsetX, dragOffsetY;

    public GuiOverlayPosition(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        LazifyConfig cfg = LazifyConfig.INSTANCE;
        overlayX = cfg.getOverlayX();
        overlayY = cfg.getOverlayY();
        OverlayManager.INSTANCE.defaultSettings();
        this.buttonList.add(new GuiButton(0, this.width / 2 - 50, this.height - 28, 100, 20, "Done"));
    }

    private float previewScale() {
        return LazifyConfig.INSTANCE.getOverlayScale();
    }

    private int scaledOverlayW() {
        return Math.max(1, Math.round(OverlayManager.INSTANCE.getOverlayContentWidth() * previewScale()));
    }

    private int scaledOverlayH() {
        return Math.max(1, Math.round(OverlayManager.INSTANCE.getOverlayContentHeight() * previewScale()));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        drawCenteredString(fontRendererObj, "Drag the overlay to reposition it", width / 2, 10, 0xFFFFFF);
        drawCenteredString(fontRendererObj, "Current position: " + overlayX + ", " + overlayY
                + "  Scale: " + LazifyConfig.INSTANCE.getOverlayScalePercent() + "%", width / 2, 22, 0xAAAAAA);

        if (OverlayTheme.isMellow(LazifyConfig.INSTANCE.getOverlayTheme())) {
            drawCenteredString(fontRendererObj,
                    "Mellow uses the tab list — switch to Lazify/Nerdify to position the HUD",
                    width / 2, 36, 0xFFAA55);
        }

        if (dragging) {
            overlayX = mouseX - dragOffsetX;
            overlayY = mouseY - dragOffsetY;
            overlayX = Math.max(0, Math.min(overlayX, width - scaledOverlayW()));
            overlayY = Math.max(0, Math.min(overlayY, height - scaledOverlayH()));
        }

        // Live overlay: same players, columns, colors as in-game
        OverlayManager.INSTANCE.renderAtForEditor(overlayX, overlayY);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0) {
            if (mouseX >= overlayX && mouseX <= overlayX + scaledOverlayW()
                    && mouseY >= overlayY && mouseY <= overlayY + scaledOverlayH()) {
                dragging = true;
                dragOffsetX = mouseX - overlayX;
                dragOffsetY = mouseY - overlayY;
                return;
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (dragging) {
            dragging = false;
        }
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            savePosition();
            mc.displayGuiScreen(parent);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
            savePosition();
            mc.displayGuiScreen(parent);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    private void savePosition() {
        LazifyConfig cfg = LazifyConfig.INSTANCE;
        cfg.setOverlayX(overlayX);
        cfg.setOverlayY(overlayY);
        cfg.save();
        OverlayManager.INSTANCE.defaultSettings();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
