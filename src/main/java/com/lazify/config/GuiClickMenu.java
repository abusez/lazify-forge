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
    private int colDragFromIndex = -1;
    private int colDropIndex = -1;
    private boolean colDragActive = false;
    private int colDragOffsetY = 0;

    private static final String[] TABS = {"Overlay", "Features", "Customize", "Appearance", "Columns", "API"};
    private static final int ROW_H = 22;
    private static final int COL_CARD_H = 34;
    private static final int COL_CARD_GAP = 6;
    private static final int COL_CARD_PAD = 8;
    private static final int COL_GRIP_W = 16;
    private static final int CHILD_INDENT = 14;
    private static final int SIDEBAR_W = 70;
    private static final int HEADER_H = 24;
    private static final int HEADER_BTN_H = 16;
    private static final int HEADER_BTN_PAD = 8;
    private static final String HEADER_POS_LABEL = "Move Overlay";
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

    private static class KeySettingEntry extends Entry {
        final Supplier<String> getter;
        final Consumer<String> setter;
        KeySettingEntry(String l, Supplier<String> g, Consumer<String> s) {
            super(l, "Click to set API key");
            getter = g; setter = s;
        }
        String valueText() {
            String key = getter.get();
            if (key == null || key.isEmpty()) return "\u00a7cNot set";
            if (key.length() <= 4) return "\u00a7a****";
            return "\u00a7a" + key.substring(0, 2) + "****" + key.substring(key.length() - 2);
        }
        void onClick(int button) {
            Minecraft.getMinecraft().displayGuiScreen(new GuiTextInput(
                    Minecraft.getMinecraft().currentScreen,
                    label,
                    "Leave blank to clear",
                    getter.get(),
                    setter));
        }
    }

    // ColEntry: draggable card with toggle for visibility
    private class ColEntry extends Entry {
        final String colName;
        final Supplier<Boolean> getter;
        final Consumer<Boolean> setter;
        ColEntry(String label, String colName, Supplier<Boolean> g, Consumer<Boolean> s) {
            super(label, "Drag to reorder");
            this.colName = colName; getter = g; setter = s;
        }
        String valueText() { return ""; }
        boolean isToggle() { return true; }
        boolean isOn() { return getter.get(); }
        void onClick(int button) { setter.accept(!getter.get()); }
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
                entries.add(new BoolEntry("Team FKDR Chat", "Send team avg FKDRs to party", c::isTeamFkdrChat, c::setTeamFkdrChat));
                Entry teamThreat = new BoolEntry("Team Threat Chat", "Send enemy team threat ratings to party", c::isTeamThreatChat, c::setTeamThreatChat);
                teamThreat.addChild(new DblEntry("Threshold", "Minimum score to notify", c::getTeamThreatThreshold, c::setTeamThreatThreshold, 1.0, 25.0, 0.5));
                teamThreat.addChild(new DblEntry("FKDR Weight", "How much FKDR matters", c::getThreatFkdrWeight, c::setThreatFkdrWeight, 0.0, 3.0, 0.05));
                teamThreat.addChild(new DblEntry("Star Weight", "How much stars matter", c::getThreatStarWeight, c::setThreatStarWeight, 0.0, 3.0, 0.05));
                teamThreat.addChild(new DblEntry("WS Weight", "How much winstreak matters", c::getThreatWinstreakWeight, c::setThreatWinstreakWeight, 0.0, 3.0, 0.05));
                teamThreat.addChild(new DblEntry("Tag Weight", "How much cheater tags matter", c::getThreatUrchinWeight, c::setThreatUrchinWeight, 0.0, 4.0, 0.05));
                teamThreat.addChild(new DblEntry("Team Size Wt", "Bonus per extra teammate", c::getThreatTeamSizeWeight, c::setThreatTeamSizeWeight, 0.0, 3.0, 0.05));
                teamThreat.addChild(new DblEntry("Encounter Wt", "How much repeat encounters matter", c::getThreatEncounterWeight, c::setThreatEncounterWeight, 0.0, 2.0, 0.05));
                teamThreat.addChild(new DblEntry("Nick Weight", "Bonus for nicked players", c::getThreatNickWeight, c::setThreatNickWeight, 0.0, 3.0, 0.05));
                entries.add(teamThreat);
                entries.add(new ActionEntry("Hidden Players", "Manage /ov hide list", () ->
                    Minecraft.getMinecraft().displayGuiScreen(new GuiHiddenPlayers(this))));
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
                entries.add(new ActionEntry("Overlay Key", "Rebind in Controls > Lazify", () ->
                    Minecraft.getMinecraft().displayGuiScreen(new net.minecraft.client.gui.GuiControls(
                        this, Minecraft.getMinecraft().gameSettings))));
                entries.add(new BoolEntry("Show on Tab", "Show overlay when tab is held", c::isShowOnTab, c::setShowOnTab));
                entries.add(new BoolEntry("Over Tablist", "Draw overlay above the tab list", c::isOverlayOverTab, c::setOverlayOverTab));
                entries.add(new BoolEntry("Send Nicked", "Announce nicked players", c::isSendNickedToChat, c::setSendNickedToChat));
                entries.add(new BoolEntry("Send Tag Reason", "Show tag reasons in chat (with source)", c::isSendUrchinReasonToChat, c::setSendUrchinReasonToChat));
                entries.add(new BoolEntry("Debug", "Verbose debug output", c::isDebug, c::setDebug));
                break;
            }
            case 3: { // Appearance
                Entry theme = new IntEntry("Overlay Theme", "Visual style for the overlay", c::getOverlayTheme, c::setOverlayTheme, 0, 1, 1,
                    new String[]{"Default", "Nerdify"});
                entries.add(theme);
                entries.add(new IntEntry("Column Gap", "Horizontal space between columns", c::getOverlayColGap, c::setOverlayColGap, 0, 40, 1, null));
                entries.add(new IntEntry("Row Gap", "Vertical space between rows", c::getOverlayRowGap, c::setOverlayRowGap, 0, 20, 1, null));
                entries.add(new IntEntry("Scale", "Overlay size (100 = normal)", c::getOverlayScalePercent, c::setOverlayScalePercent, 50, 200, 5, null));
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
                String[] colOrd = c.getColOrder().split(",");
                java.util.Map<String,ColEntry> colMap = new java.util.LinkedHashMap<>();
                colMap.put("encounters", new ColEntry("Encounters", "encounters", c::isColEncounters, c::setColEncounters));
                colMap.put("username",   new ColEntry("Username",   "username",   c::isColUsername,   c::setColUsername));
                colMap.put("rank",       new ColEntry("Rank",       "rank",       c::isColRank,       c::setColRank));
                colMap.put("star",       new ColEntry("Star",       "star",       c::isColStar,       c::setColStar));
                colMap.put("fkdr",       new ColEntry("FKDR",       "fkdr",       c::isColFkdr,       c::setColFkdr));
                colMap.put("winstreaks", new ColEntry("Winstreaks", "winstreaks", c::isColWinstreaks, c::setColWinstreaks));
                colMap.put("urchin",     new ColEntry("Tags",       "urchin",     c::isColUrchin,     c::setColUrchin));
                colMap.put("session",    new ColEntry("Session",    "session",    c::isColSession,    c::setColSession));
                colMap.put("ping",       new ColEntry("Ping",       "ping",       c::isColPing,       c::setColPing));
                colMap.put("level",      new ColEntry("Level",      "level",      c::isColLevel,      c::setColLevel));
                for (String col : colOrd) { ColEntry ce = colMap.remove(col.trim()); if (ce != null) entries.add(ce); }
                for (ColEntry ce : colMap.values()) entries.add(ce);
                break;
            case 5: { // API
                entries.add(new KeySettingEntry("Urchin/Coral Key", c::getUrchinKey, v -> {
                    c.setUrchinKey(v); c.save(); OverlayManager.INSTANCE.refreshOverlayTags();
                }));
                entries.add(new KeySettingEntry("Seraph Key", c::getSeraphKey, v -> {
                    c.setSeraphKey(v); c.save(); OverlayManager.INSTANCE.refreshOverlayTags();
                }));
                entries.add(new KeySettingEntry("Bordic Key", c::getBordicKey, v -> {
                    c.setBordicKey(v); c.save(); com.lazify.util.BordicSuperstar.clearCache();
                }));
                entries.add(new KeySettingEntry("Hypixel Key", c::getHypixelKey, v -> {
                    c.setHypixelKey(v); c.save();
                }));
                break;
            }
        }
        rebuildVisible();
    }

    private boolean isColumnsTab() {
        return activeTab == 4;
    }

    private int visibleRowHeight(int index) {
        if (isColumnsTab()) return COL_CARD_H + COL_CARD_GAP;
        return ROW_H;
    }

    private int visibleRowBodyHeight(int index) {
        return isColumnsTab() ? COL_CARD_H : ROW_H;
    }

    private int visibleRowY(int index) {
        int y = contentY;
        for (int i = 0; i < index; i++) y += visibleRowHeight(i);
        return y - scrollY;
    }

    private int totalVisibleHeight() {
        int total = 0;
        for (int i = 0; i < visibleRows.size(); i++) total += visibleRowHeight(i);
        return total;
    }

    private int hitTestVisibleRow(int mouseY) {
        for (int i = 0; i < visibleRows.size(); i++) {
            int ry = visibleRowY(i);
            int rh = visibleRowBodyHeight(i);
            if (mouseY >= ry && mouseY < ry + rh) return i;
        }
        return -1;
    }

    private int computeColDropIndex(int mouseY) {
        for (int i = 0; i < visibleRows.size(); i++) {
            int ry = visibleRowY(i);
            if (mouseY < ry + COL_CARD_H / 2) return i;
        }
        return visibleRows.size();
    }

    private void saveColOrderFromEntries() {
        StringBuilder sb = new StringBuilder();
        for (Entry e : entries) {
            if (e instanceof ColEntry) {
                if (sb.length() > 0) sb.append(',');
                sb.append(((ColEntry) e).colName);
            }
        }
        LazifyConfig cfg = LazifyConfig.INSTANCE;
        cfg.setColOrder(sb.toString());
        cfg.save();
        OverlayManager.INSTANCE.defaultSettings();
    }

    private void applyColumnReorder(int from, int to) {
        if (from < 0 || from >= entries.size() || to < 0 || to > entries.size() || from == to) return;
        Entry moved = entries.remove(from);
        if (to > from) to--;
        entries.add(to, moved);
        saveColOrderFromEntries();
        rebuildVisible();
    }

    private void drawColGrip(int x, int cy) {
        int dotColor = 0xFF606070;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 2; col++) {
                int dx = x + col * 4;
                int dy = cy - 4 + row * 4;
                Gui.drawRect(dx, dy, dx + 2, dy + 2, dotColor);
            }
        }
    }

    private void drawColCard(int cardX, int cardY, int cardW, ColEntry e, boolean hover, boolean dragging) {
        int cardH = COL_CARD_H;
        int bg = dragging ? 0x28000000 : (hover ? 0x22FFFFFF : 0x14FFFFFF);
        Gui.drawRect(cardX, cardY, cardX + cardW, cardY + cardH, bg);
        Gui.drawRect(cardX, cardY, cardX + cardW, cardY + 1, 0x30FFFFFF);
        Gui.drawRect(cardX, cardY + cardH - 1, cardX + cardW, cardY + cardH, 0x18FFFFFF);
        Gui.drawRect(cardX, cardY, cardX + 1, cardY + cardH, 0x18FFFFFF);
        Gui.drawRect(cardX + cardW - 1, cardY, cardX + cardW, cardY + cardH, 0x18FFFFFF);

        drawColGrip(cardX + 6, cardY + cardH / 2);

        int labelX = cardX + COL_GRIP_W + 10;
        fontRendererObj.drawStringWithShadow(e.label, labelX, cardY + 8, COL_TEXT);
        fontRendererObj.drawStringWithShadow(e.desc, labelX, cardY + 19, COL_TEXT_DIM);

        drawToggle(cardX + cardW - TOGGLE_W - 8, cardY + (cardH - TOGGLE_H) / 2, e.isOn());
    }

    /** @return {x, y, w, h} for the header "Move Overlay" button */
    private int[] headerPosBtnBounds() {
        int tw = fontRendererObj.getStringWidth(HEADER_POS_LABEL);
        int bw = tw + 12;
        int bx = px + pw - HEADER_BTN_PAD - bw;
        int by = py + (HEADER_H - HEADER_BTN_H) / 2;
        return new int[]{bx, by, bw, HEADER_BTN_H};
    }

    private boolean isHeaderPosBtnHovered(int mouseX, int mouseY) {
        int[] b = headerPosBtnBounds();
        return mouseX >= b[0] && mouseX < b[0] + b[2] && mouseY >= b[1] && mouseY < b[1] + b[3];
    }

    private void drawHeaderPosButton(int mouseX, int mouseY) {
        int[] b = headerPosBtnBounds();
        int bx = b[0], by = b[1], bw = b[2], bh = b[3];
        boolean hover = isHeaderPosBtnHovered(mouseX, mouseY);
        int bg = hover ? COL_ACCENT : COL_ACCENT_DIM;
        Gui.drawRect(bx, by, bx + bw, by + bh, bg);
        Gui.drawRect(bx, by, bx + bw, by + 1, 0x40FFFFFF);
        Gui.drawRect(bx, by + bh - 1, bx + bw, by + bh, 0x20FFFFFF);
        fontRendererObj.drawStringWithShadow(HEADER_POS_LABEL, bx + 6, by + 4, 0xFFFFFFFF);
    }

    private void openOverlayPositionScreen() {
        Minecraft.getMinecraft().displayGuiScreen(new GuiOverlayPosition(this));
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
        drawHeaderPosButton(mouseX, mouseY);

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
            int ry = visibleRowY(i);
            int rowH = visibleRowBodyHeight(i);
            if (ry + rowH < contentY - rowH || ry > contentY + contentH + rowH) continue;

            VisibleRow row = visibleRows.get(i);
            Entry e = row.entry;
            int indent = row.indent * CHILD_INDENT;
            boolean isChild = row.indent > 0;
            boolean hover = mouseX >= contentX && mouseX < px + pw && mouseY >= ry && mouseY < ry + rowH
                    && mouseY >= contentY && mouseY < contentY + contentH;
            boolean isDraggingThisCol = isColumnsTab() && colDragActive && colDragFromIndex == i;

            if (isColumnsTab() && e instanceof ColEntry && !isChild) {
                if (isDraggingThisCol) continue;

                int cardX = contentX + COL_CARD_PAD;
                int cardW = contentW - COL_CARD_PAD * 2 - 4;
                drawColCard(cardX, ry, cardW, (ColEntry) e, hover, false);
                continue;
            }

            // Background
            if (isChild) {
                Gui.drawRect(contentX, ry, px + pw, ry + ROW_H, COL_CHILD_BG);
                // Left accent bar for children
                Gui.drawRect(contentX + 6, ry + 2, contentX + 7, ry + ROW_H - 2, 0x30FFFFFF);
            } else if (i % 2 == 1) {
                Gui.drawRect(contentX, ry, px + pw, ry + ROW_H, COL_ROW_ALT);
            }
            if (hover) Gui.drawRect(contentX, ry, px + pw, ry + rowH, COL_ROW_HOVER);
            // Bottom separator
            if (!isChild) {
                Gui.drawRect(contentX + 8, ry + rowH - 1, px + pw - 8, ry + rowH, 0x08FFFFFF);
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
                drawToggle(widgetRight - TOGGLE_W, ry + (rowH - TOGGLE_H) / 2, e.isOn());
            } else if (e.isSlider()) {
                if (draggingIndex == i) {
                    int sx = widgetRight - SLIDER_W;
                    float ratio = (float)(mouseX - sx) / SLIDER_W;
                    ratio = Math.max(0, Math.min(1, ratio));
                    e.setFromSlider(ratio);
                }

                int sx = widgetRight - SLIDER_W;
                int sy = ry + (rowH - 6) / 2;
                drawSlider(sx, sy, SLIDER_W, 6, e.getSliderPos(), draggingIndex == i || hover);

                String val = e.valueText();
                int vw = fontRendererObj.getStringWidth(val);
                fontRendererObj.drawStringWithShadow(val, sx - 4 - vw, ry + (rowH - 8) / 2, COL_TEXT_VALUE);
            } else {
                String val = e.valueText();
                int vw = fontRendererObj.getStringWidth(val);
                fontRendererObj.drawStringWithShadow(val, widgetRight - vw, ry + (rowH - 8) / 2, 0xFFFFFFFF);
            }
        }

        // Column drag: drop indicator + floating card
        if (isColumnsTab() && colDragActive && colDragFromIndex >= 0 && colDragFromIndex < visibleRows.size()) {
            Entry dragEntry = visibleRows.get(colDragFromIndex).entry;
            if (dragEntry instanceof ColEntry) {
                if (colDropIndex >= 0) {
                    int lineY = colDropIndex < visibleRows.size()
                            ? visibleRowY(colDropIndex)
                            : visibleRowY(visibleRows.size() - 1) + visibleRowHeight(visibleRows.size() - 1);
                    int lineX = contentX + COL_CARD_PAD;
                    int lineW = contentW - COL_CARD_PAD * 2 - 4;
                    Gui.drawRect(lineX, lineY - 1, lineX + lineW, lineY + 1, COL_ACCENT);
                }

                int cardX = contentX + COL_CARD_PAD;
                int cardW = contentW - COL_CARD_PAD * 2 - 4;
                int ghostY = mouseY - colDragOffsetY;
                GlStateManager.pushMatrix();
                GlStateManager.enableBlend();
                drawColCard(cardX, ghostY, cardW, (ColEntry) dragEntry, true, true);
                GlStateManager.disableBlend();
                GlStateManager.popMatrix();
            }
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        // Scrollbar
        int totalH = totalVisibleHeight();
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
        if (mouseButton == 0 && mouseY >= py && mouseY < py + HEADER_H
                && mouseX >= contentX && mouseX < px + pw && isHeaderPosBtnHovered(mouseX, mouseY)) {
            openOverlayPositionScreen();
            return;
        }

        // Sidebar tab clicks
        int tabStartY = py + HEADER_H + 4;
        if (mouseX >= px && mouseX < px + SIDEBAR_W - 1) {
            for (int i = 0; i < TABS.length; i++) {
                int ty = tabStartY + i * 20;
                if (mouseY >= ty && mouseY < ty + 20) {
                    activeTab = i;
                    scrollY = 0;
                    colDragFromIndex = -1;
                    colDropIndex = -1;
                    colDragActive = false;
                    buildEntries();
                    return;
                }
            }
        }

        // Content row clicks
        if (mouseX >= contentX && mouseX < px + pw && mouseY >= contentY && mouseY < contentY + contentH) {
            int widgetRight = px + pw - 10;
            int rowIndex = hitTestVisibleRow(mouseY);
            if (rowIndex >= 0) {
                int ry = visibleRowY(rowIndex);
                int rowH = visibleRowBodyHeight(rowIndex);
                if (mouseY >= ry && mouseY < ry + rowH) {
                    VisibleRow row = visibleRows.get(rowIndex);
                    Entry e = row.entry;

                    // Columns tab: drag cards to reorder, toggle only on switch
                    if (isColumnsTab() && e instanceof ColEntry && row.indent == 0) {
                        int cardX = contentX + COL_CARD_PAD;
                        int cardW = contentW - COL_CARD_PAD * 2 - 4;
                        int toggleX = cardX + cardW - TOGGLE_W - 8;
                        if (mouseX >= toggleX - 2 && mouseX <= toggleX + TOGGLE_W + 2) {
                            e.onClick(mouseButton);
                            LazifyConfig.INSTANCE.save();
                            OverlayManager.INSTANCE.defaultSettings();
                            return;
                        }
                        if (mouseButton == 0) {
                            colDragFromIndex = rowIndex;
                            colDropIndex = rowIndex;
                            colDragActive = false;
                            colDragOffsetY = mouseY - ry;
                            return;
                        }
                    }

                    // Right-click on parent with children → toggle expand
                    if (mouseButton == 1 && row.indent == 0 && e.hasChildren()) {
                        e.expanded = !e.expanded;
                        rebuildVisible();
                        return;
                    }

                    if (e.isSlider()) {
                        int sx = widgetRight - SLIDER_W;
                        if (mouseX >= sx - 4 && mouseX <= widgetRight + 4) {
                            draggingIndex = rowIndex;
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
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (colDragFromIndex >= 0 && clickedMouseButton == 0) {
            colDragActive = true;
            colDropIndex = computeColDropIndex(mouseY);
            return;
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (colDragFromIndex >= 0) {
            if (colDragActive) {
                applyColumnReorder(colDragFromIndex, colDropIndex);
            }
            colDragFromIndex = -1;
            colDropIndex = -1;
            colDragActive = false;
        }
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
            int step = isColumnsTab() ? (COL_CARD_H + COL_CARD_GAP) * 2 : ROW_H * 2;
            scrollY -= Integer.signum(dWheel) * step;
            int maxScroll = Math.max(0, totalVisibleHeight() - contentH);
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
