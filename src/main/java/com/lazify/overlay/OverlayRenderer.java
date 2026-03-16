package com.lazify.overlay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

public class OverlayRenderer {

    /**
     * Draws a filled rectangle from (x1,y1) to (x2,y2) with the given ARGB color.
     */
    public static void drawRect(float x1, float y1, float x2, float y2, int color) {
        // Normalize coordinates
        if (x1 > x2) { float tmp = x1; x1 = x2; x2 = tmp; }
        if (y1 > y2) { float tmp = y1; y1 = y2; y2 = tmp; }

        float alpha = ((color >> 24) & 0xFF) / 255.0f;
        float red   = ((color >> 16) & 0xFF) / 255.0f;
        float green = ((color >>  8) & 0xFF) / 255.0f;
        float blue  = ( color        & 0xFF) / 255.0f;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(red, green, blue, alpha);

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        wr.pos(x1, y2, 0).endVertex();
        wr.pos(x2, y2, 0).endVertex();
        wr.pos(x2, y1, 0).endVertex();
        wr.pos(x1, y1, 0).endVertex();
        tess.draw();

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    /**
     * Draws a 2D line from (x1,y1) to (x2,y2) with given width and ARGB color.
     */
    public static void drawLine2D(float x1, float y1, float x2, float y2, float width, int color) {
        float alpha = ((color >> 24) & 0xFF) / 255.0f;
        float red   = ((color >> 16) & 0xFF) / 255.0f;
        float green = ((color >>  8) & 0xFF) / 255.0f;
        float blue  = ( color        & 0xFF) / 255.0f;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(red, green, blue, alpha);

        GL11.glLineWidth(width);

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
        wr.pos(x1, y1, 0).endVertex();
        wr.pos(x2, y2, 0).endVertex();
        tess.draw();

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    /**
     * Draws a string using the Minecraft font renderer.
     */
    public static void drawString(String text, float x, float y, int color, boolean shadow) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.fontRendererObj == null) return;
        GlStateManager.enableBlend();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        if (shadow) {
            mc.fontRendererObj.drawStringWithShadow(text, x, y, color);
        } else {
            mc.fontRendererObj.drawString(text, x, y, color, false);
        }
        GlStateManager.disableBlend();
    }

    public static int getFontWidth(String text) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.fontRendererObj == null) return 0;
        return mc.fontRendererObj.getStringWidth(text);
    }

    public static int getFontHeight() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.fontRendererObj == null) return 9;
        return mc.fontRendererObj.FONT_HEIGHT;
    }
}
