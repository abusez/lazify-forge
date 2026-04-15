package com.lazify.config;

import com.lazify.overlay.OverlayManager;
import com.lazify.overlay.OverlayRenderer;
import com.lazify.util.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;

import java.io.IOException;

public class GuiOverlayPosition extends GuiScreen {

    private final GuiScreen parent;
    private int overlayX, overlayY;
    private int overlayW, overlayH;
    private boolean dragging = false;
    private int dragOffsetX, dragOffsetY;

    // Preview overlay appearance
    private int bgColor, headerColor, borderColor;
    private static final String[] PREVIEW_NAMES = {
        "\u00a7aPlayer1", "\u00a7cPlayer2", "\u00a7bPlayer3"
    };
    private static final String[] PREVIEW_STARS = {"142\u2730", "87\u2730", "203\u2730"};
    private static final String[] PREVIEW_FKDR  = {"3.42", "1.05", "8.71"};

    public GuiOverlayPosition(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        LazifyConfig cfg = LazifyConfig.INSTANCE;
        overlayX = cfg.getOverlayX();
        overlayY = cfg.getOverlayY();

        bgColor     = ColorUtil.getHueRGB(cfg.getBgHue(),     cfg.getBgOpacity());
        headerColor = ColorUtil.getHueRGB(cfg.getHeaderHue(), 255);
        borderColor = ColorUtil.getHueRGB(cfg.getBorderHue(), 255);

        calculateOverlaySize();

        this.buttonList.add(new GuiButton(0, this.width / 2 - 50, this.height - 28, 100, 20, "Done"));
    }

    private void calculateOverlaySize() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.fontRendererObj == null) {
            overlayW = 200;
            overlayH = 60;
            return;
        }

        // Calculate width based on preview content
        int nameW = 0, starW = 0, fkdrW = 0;
        String nameH = "Name", starH = "Star", fkdrH = "FKDR";
        nameW = mc.fontRendererObj.getStringWidth(nameH);
        starW = mc.fontRendererObj.getStringWidth(starH);
        fkdrW = mc.fontRendererObj.getStringWidth(fkdrH);
        for (int i = 0; i < PREVIEW_NAMES.length; i++) {
            nameW = Math.max(nameW, mc.fontRendererObj.getStringWidth(PREVIEW_NAMES[i]));
            starW = Math.max(starW, mc.fontRendererObj.getStringWidth(PREVIEW_STARS[i]));
            fkdrW = Math.max(fkdrW, mc.fontRendererObj.getStringWidth(PREVIEW_FKDR[i]));
        }
        overlayW = 5 + nameW + 5 + starW + 5 + fkdrW + 5;

        int lineH = mc.fontRendererObj.FONT_HEIGHT + 3;
        overlayH = lineH + (PREVIEW_NAMES.length * lineH) + 6;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        // Instructions
        drawCenteredString(fontRendererObj, "Drag the overlay to reposition it", width / 2, 10, 0xFFFFFF);
        drawCenteredString(fontRendererObj, "Current position: " + overlayX + ", " + overlayY, width / 2, 22, 0xAAAAAA);

        // Handle dragging
        if (dragging) {
            overlayX = mouseX - dragOffsetX;
            overlayY = mouseY - dragOffsetY;
            // Clamp to screen
            overlayX = Math.max(0, Math.min(overlayX, width - overlayW));
            overlayY = Math.max(0, Math.min(overlayY, height - overlayH));
        }

        // Draw preview overlay
        drawPreviewOverlay();

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawPreviewOverlay() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.fontRendererObj == null) return;

        int x = overlayX;
        int y = overlayY;
        int x2 = x + overlayW;
        int y2 = y + overlayH;

        // Background
        OverlayRenderer.drawRect(x, y, x2, y2, bgColor);

        // Border
        OverlayRenderer.drawLine2D(x, y, x2, y, 2.5f, borderColor);
        OverlayRenderer.drawLine2D(x2, y, x2, y2, 2.5f, borderColor);
        OverlayRenderer.drawLine2D(x2, y2, x, y2, 2.5f, borderColor);
        OverlayRenderer.drawLine2D(x, y2, x, y, 2.5f, borderColor);

        int lineH = mc.fontRendererObj.FONT_HEIGHT + 3;
        int col1 = x + 5;

        // Measure column widths
        int nameW = mc.fontRendererObj.getStringWidth("Name");
        int starW = mc.fontRendererObj.getStringWidth("Star");
        int fkdrW = mc.fontRendererObj.getStringWidth("FKDR");
        for (int i = 0; i < PREVIEW_NAMES.length; i++) {
            nameW = Math.max(nameW, mc.fontRendererObj.getStringWidth(PREVIEW_NAMES[i]));
            starW = Math.max(starW, mc.fontRendererObj.getStringWidth(PREVIEW_STARS[i]));
            fkdrW = Math.max(fkdrW, mc.fontRendererObj.getStringWidth(PREVIEW_FKDR[i]));
        }

        int col2 = col1 + nameW + 5;
        int col3 = col2 + starW + 5;

        // Headers
        OverlayRenderer.drawString("Name", col1, y + 3, headerColor, true);
        OverlayRenderer.drawString("Star", col2 + (starW - mc.fontRendererObj.getStringWidth("Star")) / 2, y + 3, headerColor, true);
        OverlayRenderer.drawString("FKDR", col3 + (fkdrW - mc.fontRendererObj.getStringWidth("FKDR")) / 2, y + 3, headerColor, true);

        // Rows
        int rowY = y + lineH + 5;
        for (int i = 0; i < PREVIEW_NAMES.length; i++) {
            OverlayRenderer.drawString(PREVIEW_NAMES[i], col1, rowY, 0xFFFFFF, true);
            OverlayRenderer.drawString(PREVIEW_STARS[i], col2 + (starW - mc.fontRendererObj.getStringWidth(PREVIEW_STARS[i])) / 2, rowY, 0xFFFFFF, true);
            OverlayRenderer.drawString(PREVIEW_FKDR[i], col3 + (fkdrW - mc.fontRendererObj.getStringWidth(PREVIEW_FKDR[i])) / 2, rowY, 0xFFFFFF, true);
            rowY += lineH;
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0) {
            // Check if click is inside the overlay preview
            if (mouseX >= overlayX && mouseX <= overlayX + overlayW
                    && mouseY >= overlayY && mouseY <= overlayY + overlayH) {
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
            // Save position and go back
            savePosition();
            mc.displayGuiScreen(parent);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) { // Escape
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
}
