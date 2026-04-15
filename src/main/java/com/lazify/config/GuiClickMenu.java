package com.lazify.config;

import com.lazify.LazifyMod;
import com.lazify.overlay.OverlayManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class GuiClickMenu extends GuiScreen {

    private int activeTab = 0;
    private int scrollY = 0;
    private final List<Entry> entries = new ArrayList<>();
    private int draggingIndex = -1;

    private static final String[] TABS = {"Overlay", "Features", "Customize", "Appearance", "Columns"};
    private static final int ROW_H = 22;
    private static final int CHILD_INDENT = 14;
    private static final int SIDEBAR_W = 70;
    private static final int HEADER_H = 24;
    private static final int SLIDER_W = 90;
    private static final int TOGGLE_W = 20;
    private static final int TOGGLE_H = 10;

    // Colors
    private static final int COL_BG = 0xF0141420;
    private static final int COL_SIDEBAR = 0xF0101018;
    private static final int COL_HEADER = 0xF0181828;
    private static final int COL_ROW_HOVER = 0x18FFFFFF;
    private static final int COL_ROW_ALT = 0x08FFFFFF;
    private static final int COL_CHILD_BG = 0x0CFFFFFF;
    private static final int COL_ACCENT = 0xFF3B82F6;
    private static final int COL_ACCENT_DIM = 0xFF1E3A5F;
    private static final int COL_TOGGLE_OFF = 0xFF3A3A4A;
    private static final int COL_TOGGLE_KNOB = 0xFFE0E0E0;
    private static final int COL_SLIDER_TRACK = 0xFF252535;
    private static final int COL_SLIDER_FILL = 0xFF3B82F6;
    private static final int COL_TEXT = 0xFFE0E0E0;
    private static final int COL_TEXT_DIM = 0xFF707080;
    private static final int COL_TEXT_VALUE = 0xFF9090A0;
    private static final int COL_DIVIDER = 0xFF222233;
    private static final int COL_TAB_HOVER = 0x15FFFFFF;
    private static final int COL_TAB_ACTIVE_LINE = 0xFF3B82F6;
    private static final int COL_EXPAND_ARROW = 0xFF5588CC;

    private int px, py, pw, ph, contentX, contentY, contentW, contentH;

    // Flattened visible entries (parents + expanded children)
    private final List<VisibleRow> visibleRows = new ArrayList<>();

    private static class VisibleRow {
        final Entry entry;
        final int indent; // 0 = parent, 1 = child
        VisibleRow(Entry e, int indent) { this.entry = e; this.indent = indent; }
    }

    // ── Entry types ───────────────────────────────────────────────────────────

    private abstract static class Entry {
        final String label;
        final String desc;
        final List<Entry> children = new ArrayList<>();
        boolean expanded = false;
        Entry(String l, String d) { label = l; desc = d; }
        abstract String valueText();
        abstract void onClick(int button);
        boolean isToggle() { return false; }
        boolean isOn() { return false; }
        boolean isSlider() { return false; }
        float getSliderPos() { return 0; }
        void setFromSlider(float ratio) {}
        boolean hasChildren() { return !children.isEmpty(); }
        Entry addChild(Entry child) { children.add(child); return this; }
    }

    private static class BoolEntry extends Entry {
        final Supplier<Boolean> getter;
        final Consumer<Boolean> setter;
        BoolEntry(String l, String d, Supplier<Boolean> g, Consumer<Boolean> s) { super(l, d); getter = g; setter = s; }
        String valueText() { return ""; }
        void onClick(int button) { setter.accept(!getter.get()); }
        boolean isToggle() { return true; }
        boolean isOn() { return getter.get(); }
    }

    private static class IntEntry extends Entry {
        final Supplier<Integer> getter;
        final Consumer<Integer> setter;
        final int min, max, step;
        final String[] names;
        IntEntry(String l, String d, Supplier<Integer> g, Consumer<Integer> s, int min, int max, int step, String[] names) {
            super(l, d); getter = g; setter = s; this.min = min; this.max = max; this.step = step; this.names = names;
        }
        String valueText() {
            int v = getter.get();
            if (names != null && v >= 0 && v < names.length) return names[v];
            return String.valueOf(v);
        }
        void onClick(int button) {
            int v = getter.get();
            v += (button == 0) ? step : -step;
            if (v > max) v = min;
            if (v < min) v = max;
            setter.accept(v);
        }
        boolean isSlider() { return true; }
        float getSliderPos() { return (float)(getter.get() - min) / (max - min); }
        void setFromSlider(float ratio) {
            int v = min + Math.round(ratio * (max - min));
            v = Math.round((float)(v - min) / step) * step + min;
            v = Math.max(min, Math.min(max, v));
            setter.accept(v);
        }
    }

    private static class DblEntry extends Entry {
        final Supplier<Double> getter;
        final Consumer<Double> setter;
        final double min, max, step;
        DblEntry(String l, String d, Supplier<Double> g, Consumer<Double> s, double min, double max, double step) {
            super(l, d); getter = g; setter = s; this.min = min; this.max = max; this.step = step;
        }
        String valueText() { return String.valueOf(Math.round(getter.get() * 100.0) / 100.0); }
        void onClick(int button) {
            double v = getter.get() + ((button == 0) ? step : -step);
            setter.accept(Math.max(min, Math.min(max, Math.round(v * 100.0) / 100.0)));
        }
        boolean isSlider() { return true; }
        float getSliderPos() { return (float)((getter.get() - min) / (max - min)); }
        void setFromSlider(float ratio) {
            double v = min + ratio * (max - min);
            v = Math.round(v / step) * step;
            v = Math.max(min, Math.min(max, Math.round(v * 100.0) / 100.0));
            setter.accept(v);
        }
    }

    private static class ColorEntry extends Entry {
        final Supplier<String> getter;
        final Consumer<String> setter;
        private static final String CODES = "0123456789abcdef";
        ColorEntry(String l, String d, Supplier<String> g, Consumer<String> s) { super(l, d); getter = g; setter = s; }
        String valueText() { return "\u00a7" + getter.get() + "\u2588\u2588 \u00a77" + getter.get(); }
        void onClick(int button) {
            int i = CODES.indexOf(getter.get().charAt(0));
            i = (button == 0) ? (i + 1) % 16 : (i + 15) % 16;
            setter.accept(String.valueOf(CODES.charAt(i)));
        }
    }

    private static class ActionEntry extends Entry {
        final Runnable action;
        ActionEntry(String l, String d, Runnable a) { super(l, d); action = a; }
        String valueText() { return "\u00a7b\u25B6"; }
        void onClick(int button) { action.run(); }
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    @Override
    public void initGui() {
        pw = Math.min(380, width - 40);
        ph = Math.min(height - 30, 280);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        contentX = px + SIDEBAR_W;
        contentY = py + HEADER_H;
        contentW = pw - SIDEBAR_W;
        contentH = ph - HEADER_H;
        scrollY = 0;
        buildEntries();
    }

    private void buildEntries() {
        entries.clear();
        LazifyConfig c = LazifyConfig.INSTANCE;
        switch (activeTab) {
            case 0: { // Overlay
                Entry teams = new BoolEntry("Teams", "Group players by team", c::isTeams, c::setTeams);
                teams.addChild(new BoolEntry("Team Prefix", "Show team color prefix", c::isTeamPrefix, c::setTeamPrefix));
                entries.add(teams);
                entries.add(new BoolEntry("Show Yourself", "Include yourself in overlay", c::isShowYourself, c::setShowYourself));
                entries.add(new BoolEntry("Show Ranks", "Display Hypixel ranks", c::isShowRanks, c::setShowRanks));
                entries.add(new BoolEntry("Remove Final Kill", "Remove players on final kill", c::isRemoveFinalKill, c::setRemoveFinalKill));
                Entry autoTab = new BoolEntry("Auto Tablist", "Auto-add tablist players", c::isAutoTablist, c::setAutoTablist);
                autoTab.addChild(new BoolEntry("Clear on /who", "Clear overlay on /who", c::isClearOnWho, c::setClearOnWho));
                entries.add(autoTab);
                break;
            }
            case 1: { // Features
                entries.add(new BoolEntry("Skin Denick", "Detect nicks by skin", c::isSkinDenick, c::setSkinDenick));
                entries.add(new BoolEntry("Middle Click Shop", "Auto middle-click in BW shop", c::isMiddleClickShop, c::setMiddleClickShop));
                Entry autoWho = new BoolEntry("Auto /who", "Auto send /who on game start", c::isAutoWho, c::setAutoWho);
                autoWho.addChild(new DblEntry("Delay", "Seconds before sending /who", c::getWhoDelay, c::setWhoDelay, 0.0, 10.0, 0.5));
                autoWho.addChild(new BoolEntry("Hide /who", "Hide ONLINE: message from chat", c::isHideWho, c::setHideWho));
                entries.add(autoWho);
                Entry dodge = new BoolEntry("Dodge Warning", "Warn if lobby is sweaty", c::isDodgeWarning, c::setDodgeWarning);
                dodge.addChild(new DblEntry("Threshold", "Avg FKDR to trigger", c::getDodgeThreshold, c::setDodgeThreshold, 0.5, 20.0, 0.5));
                entries.add(dodge);
                entries.add(new BoolEntry("No Hurt Cam", "Disable damage camera tilt", c::isNoHurtCam, c::setNoHurtCam));
                entries.add(new BoolEntry("Anti Debuff", "Remove visual debuffs", c::isAntiDebuff, c::setAntiDebuff));
                break;
            }
            case 2: { // Customize
                Entry sortBy = new IntEntry("Sort By", null, c::getSortByIndex, c::setSortByIndex, 0, 5, 1,
                    new String[]{"Encounters", "Star", "FKDR", "Index", "Winstreak", "Join Time"});
                sortBy.addChild(new IntEntry("Sort Mode", null, c::getSortMode, c::setSortMode, 0, 1, 1,
                    new String[]{"Asc (highest top)", "Desc (lowest top)"}));
                sortBy.addChild(new IntEntry("Winstreak Mode", null, c::getWinstreakMode, c::setWinstreakMode, 0, 5, 1,
                    new String[]{"Overall", "Solos", "Doubles", "Threes", "Fours", "4v4"}));
                entries.add(sortBy);
                entries.add(new IntEntry("Enc. Timeout", "Minutes", c::getEncountersTimeoutMins, c::setEncountersTimeoutMins, 1, 1440, 5, null));
                entries.add(new BoolEntry("Hold Mode", "Hold key to show overlay", c::isKeybindHold, c::setKeybindHold));
                entries.add(new BoolEntry("Show on Tab", "Show overlay when tab is held", c::isShowOnTab, c::setShowOnTab));
                entries.add(new BoolEntry("Send Nicked", "Announce nicked players", c::isSendNickedToChat, c::setSendNickedToChat));
                entries.add(new BoolEntry("Send Urchin Reason", "Show Urchin tag reasons", c::isSendUrchinReasonToChat, c::setSendUrchinReasonToChat));
                entries.add(new BoolEntry("Debug", "Verbose debug output", c::isDebug, c::setDebug));
                break;
            }
            case 3: { // Appearance
                Entry bg = new IntEntry("BG Opacity", null, c::getBgOpacity, c::setBgOpacity, 0, 255, 10, null);
                bg.addChild(new IntEntry("BG Hue", null, c::getBgHue, c::setBgHue, 0, 360, 10, null));
                bg.addChild(new IntEntry("Header Hue", null, c::getHeaderHue, c::setHeaderHue, 0, 360, 10, null));
                bg.addChild(new IntEntry("Border Hue", null, c::getBorderHue, c::setBorderHue, 0, 360, 10, null));
                entries.add(bg);
                Entry fkdr = new BoolEntry("FKDR Colors", "Color-code FKDR values", c::isFkdrColors, c::setFkdrColors);
                fkdr.addChild(new ColorEntry("< 1.4", null, c::getFkdrColor1, c::setFkdrColor1));
                fkdr.addChild(new ColorEntry("1.4 - 2.4", null, c::getFkdrColor2, c::setFkdrColor2));
                fkdr.addChild(new ColorEntry("2.4 - 5", null, c::getFkdrColor3, c::setFkdrColor3));
                fkdr.addChild(new ColorEntry("5 - 10", null, c::getFkdrColor4, c::setFkdrColor4));
                fkdr.addChild(new ColorEntry("10 - 100", null, c::getFkdrColor5, c::setFkdrColor5));
                fkdr.addChild(new ColorEntry("100 - 1k", null, c::getFkdrColor6, c::setFkdrColor6));
                fkdr.addChild(new ColorEntry("1000+", null, c::getFkdrColor7, c::setFkdrColor7));
                entries.add(fkdr);
                final GuiClickMenu self = this;
                entries.add(new ActionEntry("Drag Position", "Reposition the overlay", () ->
                    Minecraft.getMinecraft().displayGuiScreen(new GuiOverlayPosition(self))));
                break;
            }
            case 4: // Columns
                entries.add(new BoolEntry("Encounters", "Show encounter count", c::isColEncounters, c::setColEncounters));
                entries.add(new BoolEntry("Username", "Show player name", c::isColUsername, c::setColUsername));
                entries.add(new BoolEntry("Star", "Show Bedwars star", c::isColStar, c::setColStar));
                entries.add(new BoolEntry("FKDR", "Show final K/D ratio", c::isColFkdr, c::setColFkdr));
                entries.add(new BoolEntry("Winstreaks", "Show winstreak", c::isColWinstreaks, c::setColWinstreaks));
                entries.add(new BoolEntry("Urchin", "Show Urchin tags", c::isColUrchin, c::setColUrchin));
                entries.add(new BoolEntry("Session", "Show session stats", c::isColSession, c::setColSession));
                break;
        }
        rebuildVisible();
    }

    private void rebuildVisible() {
        visibleRows.clear();
        for (Entry e : entries) {
            visibleRows.add(new VisibleRow(e, 0));
            if (e.expanded && e.hasChildren()) {
                for (Entry child : e.children) {
                    visibleRows.add(new VisibleRow(child, 1));
                }
            }
        }
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        // Main panel background
        Gui.drawRect(px, py, px + pw, py + ph, COL_BG);

        // Sidebar background
        Gui.drawRect(px, py, px + SIDEBAR_W, py + ph, COL_SIDEBAR);
        Gui.drawRect(px + SIDEBAR_W - 1, py, px + SIDEBAR_W, py + ph, COL_DIVIDER);

        // Header bar
        Gui.drawRect(px + SIDEBAR_W, py, px + pw, py + HEADER_H, COL_HEADER);
        Gui.drawRect(px + SIDEBAR_W, py + HEADER_H - 1, px + pw, py + HEADER_H, COL_DIVIDER);

        // Title in sidebar top
        fontRendererObj.drawStringWithShadow("\u00a7bLazify", px + 8, py + 8, 0xFFFFFFFF);

        // Category header text
        fontRendererObj.drawStringWithShadow(TABS[activeTab], contentX + 10, py + 8, COL_TEXT);

        // Sidebar tabs (vertical)
        int tabStartY = py + HEADER_H + 4;
        for (int i = 0; i < TABS.length; i++) {
            int ty = tabStartY + i * 20;
            boolean hover = mouseX >= px && mouseX < px + SIDEBAR_W - 1 && mouseY >= ty && mouseY < ty + 20;
            boolean active = (i == activeTab);

            if (active) {
                Gui.drawRect(px, ty, px + SIDEBAR_W - 1, ty + 20, 0x15FFFFFF);
                Gui.drawRect(px, ty + 2, px + 2, ty + 18, COL_TAB_ACTIVE_LINE);
            } else if (hover) {
                Gui.drawRect(px, ty, px + SIDEBAR_W - 1, ty + 20, COL_TAB_HOVER);
            }

            int textColor = active ? 0xFFFFFFFF : (hover ? 0xFFBBBBCC : COL_TEXT_DIM);
            fontRendererObj.drawStringWithShadow(TABS[i], px + 10, ty + 6, textColor);
        }

        // Content area — clip to content bounds
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        ScaledResolution sr = new ScaledResolution(mc);
        int sf = sr.getScaleFactor();
        GL11.glScissor(contentX * sf, mc.displayHeight - (contentY + contentH) * sf, contentW * sf, contentH * sf);

        for (int i = 0; i < visibleRows.size(); i++) {
            int ry = contentY + i * ROW_H - scrollY;
            if (ry + ROW_H < contentY - ROW_H || ry > contentY + contentH + ROW_H) continue;

            VisibleRow row = visibleRows.get(i);
            Entry e = row.entry;
            int indent = row.indent * CHILD_INDENT;
            boolean isChild = row.indent > 0;
            boolean hover = mouseX >= contentX && mouseX < px + pw && mouseY >= ry && mouseY < ry + ROW_H
                    && mouseY >= contentY && mouseY < contentY + contentH;

            // Background
            if (isChild) {
                Gui.drawRect(contentX, ry, px + pw, ry + ROW_H, COL_CHILD_BG);
                // Left accent bar for children
                Gui.drawRect(contentX + 6, ry + 2, contentX + 7, ry + ROW_H - 2, 0x30FFFFFF);
            } else if (i % 2 == 1) {
                Gui.drawRect(contentX, ry, px + pw, ry + ROW_H, COL_ROW_ALT);
            }
            if (hover) Gui.drawRect(contentX, ry, px + pw, ry + ROW_H, COL_ROW_HOVER);
            // Bottom separator
            if (!isChild) {
                Gui.drawRect(contentX + 8, ry + ROW_H - 1, px + pw - 8, ry + ROW_H, 0x08FFFFFF);
            }

            int labelX = contentX + 10 + indent;

            // Expand arrow for parents with children
            if (!isChild && e.hasChildren()) {
                String arrow = e.expanded ? "\u25BC" : "\u25B6"; // ▼ or ▶
                fontRendererObj.drawStringWithShadow(arrow, contentX + 4, ry + 7, COL_EXPAND_ARROW);
            }

            // Label
            fontRendererObj.drawStringWithShadow(e.label, labelX, ry + 4, isChild ? 0xFFBBBBCC : COL_TEXT);
            // Description
            if (e.desc != null) {
                fontRendererObj.drawStringWithShadow(e.desc, labelX, ry + 13, COL_TEXT_DIM);
            }

            // Right-side widget
            int widgetRight = px + pw - 10;

            if (e.isToggle()) {
                drawToggle(widgetRight - TOGGLE_W, ry + (ROW_H - TOGGLE_H) / 2, e.isOn());
            } else if (e.isSlider()) {
                if (draggingIndex == i) {
                    int sx = widgetRight - SLIDER_W;
                    float ratio = (float)(mouseX - sx) / SLIDER_W;
                    ratio = Math.max(0, Math.min(1, ratio));
                    e.setFromSlider(ratio);
                }

                int sx = widgetRight - SLIDER_W;
                int sy = ry + (ROW_H - 6) / 2;
                drawSlider(sx, sy, SLIDER_W, 6, e.getSliderPos(), draggingIndex == i || hover);

                String val = e.valueText();
                int vw = fontRendererObj.getStringWidth(val);
                fontRendererObj.drawStringWithShadow(val, sx - 4 - vw, ry + (ROW_H - 8) / 2, COL_TEXT_VALUE);
            } else {
                String val = e.valueText();
                int vw = fontRendererObj.getStringWidth(val);
                fontRendererObj.drawStringWithShadow(val, widgetRight - vw, ry + (ROW_H - 8) / 2, 0xFFFFFFFF);
            }
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        // Scrollbar
        int totalH = visibleRows.size() * ROW_H;
        if (totalH > contentH) {
            int barH = Math.max(10, contentH * contentH / totalH);
            int barY = contentY + (int)((float) scrollY / (totalH - contentH) * (contentH - barH));
            Gui.drawRect(px + pw - 3, barY, px + pw - 1, barY + barH, 0x40FFFFFF);
        }

        // Panel border
        Gui.drawRect(px, py, px + pw, py + 1, COL_ACCENT);
        Gui.drawRect(px, py + ph - 1, px + pw, py + ph, COL_DIVIDER);
        Gui.drawRect(px, py, px + 1, py + ph, COL_DIVIDER);
        Gui.drawRect(px + pw - 1, py, px + pw, py + ph, COL_DIVIDER);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawToggle(int x, int y, boolean on) {
        int trackColor = on ? COL_ACCENT : COL_TOGGLE_OFF;
        Gui.drawRect(x, y, x + TOGGLE_W, y + TOGGLE_H, trackColor);
        int knobSize = TOGGLE_H - 2;
        int knobX = on ? (x + TOGGLE_W - knobSize - 1) : (x + 1);
        int knobY = y + 1;
        Gui.drawRect(knobX, knobY, knobX + knobSize, knobY + knobSize, COL_TOGGLE_KNOB);
    }

    private void drawSlider(int x, int y, int w, int h, float pos, boolean highlight) {
        Gui.drawRect(x, y, x + w, y + h, COL_SLIDER_TRACK);
        int fillW = (int)(w * pos);
        if (fillW > 0) {
            Gui.drawRect(x, y, x + fillW, y + h, highlight ? COL_ACCENT : COL_ACCENT_DIM);
        }
        int hx = x + fillW;
        if (hx > x + w - 2) hx = x + w - 2;
        if (hx < x) hx = x;
        Gui.drawRect(hx - 1, y - 1, hx + 3, y + h + 1, COL_TOGGLE_KNOB);
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        // Sidebar tab clicks
        int tabStartY = py + HEADER_H + 4;
        if (mouseX >= px && mouseX < px + SIDEBAR_W - 1) {
            for (int i = 0; i < TABS.length; i++) {
                int ty = tabStartY + i * 20;
                if (mouseY >= ty && mouseY < ty + 20) {
                    activeTab = i;
                    scrollY = 0;
                    buildEntries();
                    return;
                }
            }
        }

        // Content row clicks
        if (mouseX >= contentX && mouseX < px + pw && mouseY >= contentY && mouseY < contentY + contentH) {
            int widgetRight = px + pw - 10;
            for (int i = 0; i < visibleRows.size(); i++) {
                int ry = contentY + i * ROW_H - scrollY;
                if (ry + ROW_H <= contentY || ry >= contentY + contentH) continue;
                if (mouseY >= ry && mouseY < ry + ROW_H) {
                    VisibleRow row = visibleRows.get(i);
                    Entry e = row.entry;

                    // Right-click on parent with children → toggle expand
                    if (mouseButton == 1 && row.indent == 0 && e.hasChildren()) {
                        e.expanded = !e.expanded;
                        rebuildVisible();
                        return;
                    }

                    if (e.isSlider()) {
                        int sx = widgetRight - SLIDER_W;
                        if (mouseX >= sx - 4 && mouseX <= widgetRight + 4) {
                            draggingIndex = i;
                            float ratio = (float)(mouseX - sx) / SLIDER_W;
                            ratio = Math.max(0, Math.min(1, ratio));
                            e.setFromSlider(ratio);
                            LazifyConfig.INSTANCE.save();
                            OverlayManager.INSTANCE.defaultSettings();
                            return;
                        }
                    }
                    e.onClick(mouseButton);
                    LazifyConfig.INSTANCE.save();
                    OverlayManager.INSTANCE.defaultSettings();
                    return;
                }
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (draggingIndex >= 0) {
            LazifyConfig.INSTANCE.save();
            OverlayManager.INSTANCE.defaultSettings();
            draggingIndex = -1;
        }
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel != 0) {
            scrollY -= Integer.signum(dWheel) * ROW_H * 2;
            int maxScroll = Math.max(0, visibleRows.size() * ROW_H - contentH);
            scrollY = Math.max(0, Math.min(scrollY, maxScroll));
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1 || keyCode == LazifyMod.guiKeybind.getKeyCode()) {
            mc.displayGuiScreen(null);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
