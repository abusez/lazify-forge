package com.lazify.overlay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

public class OverlayRenderer {

    public static int withAlpha(int argb, int alpha) {
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }

    public static int opaque(int argb) {
        return withAlpha(argb, 0xFF);
    }

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

    /** Trim without splitting Minecraft section-code pairs. */
    public static String trimStringToWidth(String text, int width) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.fontRendererObj == null || text == null) return "";
        return mc.fontRendererObj.trimStringToWidth(text, Math.max(0, width));
    }

    public static void drawBorderRect(float x1, float y1, float x2, float y2, int color) {
        drawRect(x1, y1, x2, y1 + 1, color);
        drawRect(x1, y2 - 1, x2, y2, color);
        drawRect(x1, y1, x1 + 1, y2, color);
        drawRect(x2 - 1, y1, x2, y2, color);
    }

    /**
     * Filled rounded rectangle. Built from quads + corner triangle fans so it
     * stays visible under Minecraft GUI GL state (cull/depth safe).
     */
    public static void drawRoundedRect(float x1, float y1, float x2, float y2, float radius, int color) {
        if (x1 > x2) { float t = x1; x1 = x2; x2 = t; }
        if (y1 > y2) { float t = y1; y1 = y2; y2 = t; }
        float w = x2 - x1;
        float h = y2 - y1;
        if (w <= 0 || h <= 0) return;

        float r = Math.min(Math.max(0f, radius), Math.min(w, h) * 0.5f);
        if (r < 0.5f) {
            drawRect(x1, y1, x2, y2, color);
            return;
        }

        // Axis-aligned body (always works via drawRect)
        drawRect(x1 + r, y1, x2 - r, y2, color);
        if (y2 - r > y1 + r + 0.01f) {
            drawRect(x1, y1 + r, x1 + r, y2 - r, color);
            drawRect(x2 - r, y1 + r, x2, y2 - r, color);
        }

        // Corners
        int segs = Math.max(8, Math.min(32, (int) (r * 3f)));
        drawCornerFan(x1 + r, y1 + r, r, 180, 270, segs, color); // top-left
        drawCornerFan(x2 - r, y1 + r, r, 270, 360, segs, color); // top-right
        drawCornerFan(x2 - r, y2 - r, r, 0, 90, segs, color);     // bottom-right
        drawCornerFan(x1 + r, y2 - r, r, 90, 180, segs, color);   // bottom-left
    }

    /** Outline flush with the fill bounds (no gap). */
    public static void drawRoundedBorder(float x1, float y1, float x2, float y2,
                                         float radius, float lineWidth, int color) {
        if (x1 > x2) { float t = x1; x1 = x2; x2 = t; }
        if (y1 > y2) { float t = y1; y1 = y2; y2 = t; }
        float w = x2 - x1;
        float h = y2 - y1;
        if (w <= 0 || h <= 0) return;

        float r = Math.min(Math.max(0f, radius), Math.min(w, h) * 0.5f);
        if (r < 0.5f) {
            if (lineWidth <= 1.01f) {
                drawBorderRect(x1, y1, x2, y2, color);
            } else {
                drawLine2D(x1, y1, x2, y1, lineWidth, color);
                drawLine2D(x2, y1, x2, y2, lineWidth, color);
                drawLine2D(x2, y2, x1, y2, lineWidth, color);
                drawLine2D(x1, y2, x1, y1, lineWidth, color);
            }
            return;
        }

        beginSolid(color);
        GlStateManager.disableCull();
        GlStateManager.disableDepth();
        GL11.glLineWidth(Math.max(1.0f, lineWidth));
        GL11.glEnable(GL11.GL_LINE_SMOOTH);

        int segs = Math.max(8, Math.min(32, (int) (r * 3f)));
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION);
        appendArc(wr, x1 + r, y1 + r, r, 180, 270, segs); // TL
        appendArc(wr, x2 - r, y1 + r, r, 270, 360, segs); // TR
        appendArc(wr, x2 - r, y2 - r, r, 0, 90, segs);     // BR
        appendArc(wr, x1 + r, y2 - r, r, 90, 180, segs);   // BL
        tess.draw();

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        endSolid();
    }

    private static void drawCornerFan(float cx, float cy, float r,
                                      int startDeg, int endDeg, int segs, int color) {
        beginSolid(color);
        GlStateManager.disableCull();
        GlStateManager.disableDepth();

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION);
        wr.pos(cx, cy, 0).endVertex();
        for (int i = 0; i <= segs; i++) {
            double a = Math.toRadians(startDeg + (endDeg - startDeg) * (i / (double) segs));
            wr.pos(cx + Math.cos(a) * r, cy + Math.sin(a) * r, 0).endVertex();
        }
        tess.draw();

        endSolid();
    }

    private static void appendArc(WorldRenderer wr, float cx, float cy, float r,
                                  int startDeg, int endDeg, int segs) {
        for (int i = 0; i <= segs; i++) {
            double a = Math.toRadians(startDeg + (endDeg - startDeg) * (i / (double) segs));
            wr.pos(cx + Math.cos(a) * r, cy + Math.sin(a) * r, 0).endVertex();
        }
    }

    private static void beginSolid(int color) {
        float alpha = ((color >> 24) & 0xFF) / 255.0f;
        float red   = ((color >> 16) & 0xFF) / 255.0f;
        float green = ((color >>  8) & 0xFF) / 255.0f;
        float blue  = ( color        & 0xFF) / 255.0f;
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(red, green, blue, alpha);
    }

    private static void endSolid() {
        GlStateManager.enableDepth();
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    public static int nerdifyLineHeight() {
        return getFontHeight() + OverlayTheme.lineExtra();
    }

    public static int getFontHeight() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.fontRendererObj == null) return 9;
        return mc.fontRendererObj.FONT_HEIGHT;
    }

    /** Scale HUD drawing around a fixed anchor (top-left of overlay). Returns true if pushed. */
    public static boolean pushScale(float scale, float anchorX, float anchorY) {
        if (scale <= 0.001f || Math.abs(scale - 1.0f) < 0.001f) return false;
        GlStateManager.pushMatrix();
        GlStateManager.translate(anchorX, anchorY, 0.0f);
        GlStateManager.scale(scale, scale, 1.0f);
        GlStateManager.translate(-anchorX, -anchorY, 0.0f);
        return true;
    }

    public static void popScale(boolean pushed) {
        if (pushed) GlStateManager.popMatrix();
    }
}
