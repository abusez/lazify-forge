package com.lazify.config;

import com.lazify.LazifyMod;
import com.lazify.overlay.OverlayManager;
import com.lazify.util.ColorUtil;
import com.lazify.util.ThresholdColorScale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ChatComponentText;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class GuiClickMenu extends GuiScreen {

    private static final String[] TABS = {"Overlay", "Features", "Customize", "Appearance", "Columns", "API"};

    /** Persists across open/close so tuning Appearance doesn't reset navigation. */
    private static int savedActiveTab = 0;
    private static final int[] savedScroll = new int[TABS.length];
    private static final Set<String> savedExpanded = new HashSet<>();

    private int activeTab = savedActiveTab;
    private int scrollY = 0;
    private final List<Entry> entries = new ArrayList<>();
    private int draggingIndex = -1;
    /** Shared list-drag for Columns + color tiers. */
    private int listDragFromVis = -1;
    private int listDragDrop = -1;
    private boolean listDragActive = false;
    private int listDragOffsetY = 0;
    private boolean listDragIsTier = false;
    private String listDragScaleId = null;
    private int listDragFromLocal = -1;

    private static final int ROW_H = 30;
    private static final int ROW_GAP = 4;
    private static final int COL_CARD_H = 38;
    private static final int COL_CARD_GAP = 8;
    private static final int COL_CARD_PAD = 8;
    private static final int TIER_CARD_H = 36;
    private static final int TIER_CARD_GAP = 6;
    private static final int COL_GRIP_W = 16;
    private static final int CHILD_INDENT = 16;
    private static final int SIDEBAR_W = 70;
    private static final int HEADER_H = 24;
    private static final int HEADER_BTN_H = 16;
    private static final int HEADER_BTN_PAD = 8;
    private static final String HEADER_POS_LABEL = "Move Overlay";
    private static final int SLIDER_W = 80;
    private static final int VALUE_RESERVE = 54;
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
        /** Left-click expands/collapses instead of changing a value. */
        boolean expandsOnClick() { return false; }
        boolean hasColorSwatch() { return false; }
        int swatchColor() { return 0xFFFFFFFF; }
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
        /** Optional live RGB preview: supplies {r,g,b}; channel 0/1/2 colors the slider. */
        Supplier<int[]> rgbPreview;
        int rgbChannel = -1;
        IntEntry(String l, String d, Supplier<Integer> g, Consumer<Integer> s, int min, int max, int step, String[] names) {
            super(l, d); getter = g; setter = s; this.min = min; this.max = max; this.step = step; this.names = names;
        }
        IntEntry withRgbPreview(Supplier<int[]> rgb, int channel) {
            this.rgbPreview = rgb;
            this.rgbChannel = channel;
            return this;
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

    /** Expandable section header — left-click toggles children (keeps Appearance compact). */
    private static class GroupEntry extends Entry {
        GroupEntry(String l, String d) { super(l, d); }
        String valueText() { return ""; }
        void onClick(int button) { expanded = !expanded; }
        boolean expandsOnClick() { return true; }
    }

    /** Attach Red/Green/Blue sliders with live channel + mix previews. */
    private static void addRgbSliderChildren(Entry parent,
                                            Supplier<Integer> rG, Consumer<Integer> rS,
                                            Supplier<Integer> gG, Consumer<Integer> gS,
                                            Supplier<Integer> bG, Consumer<Integer> bS) {
        final Supplier<int[]> preview = () -> new int[]{
                rG.get() != null ? rG.get() : 0,
                gG.get() != null ? gG.get() : 0,
                bG.get() != null ? bG.get() : 0
        };
        parent.addChild(new IntEntry("Red",   null, rG, rS, 0, 255, 1, null).withRgbPreview(preview, 0));
        parent.addChild(new IntEntry("Green", null, gG, gS, 0, 255, 1, null).withRgbPreview(preview, 1));
        parent.addChild(new IntEntry("Blue",  null, bG, bS, 0, 255, 1, null).withRgbPreview(preview, 2));
    }

    private static Entry rgbGroup(String name, String tip,
                                  Supplier<Integer> rG, Consumer<Integer> rS,
                                  Supplier<Integer> gG, Consumer<Integer> gS,
                                  Supplier<Integer> bG, Consumer<Integer> bS) {
        final Supplier<int[]> preview = () -> new int[]{
                rG.get() != null ? rG.get() : 0,
                gG.get() != null ? gG.get() : 0,
                bG.get() != null ? bG.get() : 0
        };
        Entry g = new GroupEntry(name, tip) {
            @Override String valueText() { return ""; }
            @Override boolean hasColorSwatch() { return true; }
            @Override int swatchColor() {
                int[] rgb = preview.get();
                return ColorUtil.rgb(rgb[0], rgb[1], rgb[2]);
            }
        };
        addRgbSliderChildren(g, rG, rS, gG, gS, bG, bS);
        return g;
    }

    private static Entry rgbaGroup(String name, String tip,
                                   Supplier<Integer> rG, Consumer<Integer> rS,
                                   Supplier<Integer> gG, Consumer<Integer> gS,
                                   Supplier<Integer> bG, Consumer<Integer> bS,
                                   Supplier<Integer> aG, Consumer<Integer> aS) {
        return rgbaGroup(name, tip, "Alpha", rG, rS, gG, gS, bG, bS, aG, aS);
    }

    private static Entry rgbaGroup(String name, String tip, String alphaLabel,
                                   Supplier<Integer> rG, Consumer<Integer> rS,
                                   Supplier<Integer> gG, Consumer<Integer> gS,
                                   Supplier<Integer> bG, Consumer<Integer> bS,
                                   Supplier<Integer> aG, Consumer<Integer> aS) {
        Entry g = rgbGroup(name, tip, rG, rS, gG, gS, bG, bS);
        g.addChild(new IntEntry(alphaLabel, null, aG, aS, 0, 255, 1, null));
        return g;
    }

    private static Entry headerRgbGroup(final LazifyConfig c, final String colKey, String tip) {
        return rgbGroup(LazifyConfig.headerLabelForKey(colKey), tip,
            () -> c.getHeaderR(colKey), v -> c.setHeaderR(colKey, v),
            () -> c.getHeaderG(colKey), v -> c.setHeaderG(colKey, v),
            () -> c.getHeaderB(colKey), v -> c.setHeaderB(colKey, v));
    }

    private static Entry highlightModule(String name, String tip,
                                         Supplier<Boolean> enG, Consumer<Boolean> enS,
                                         Supplier<Integer> rG, Consumer<Integer> rS,
                                         Supplier<Integer> gG, Consumer<Integer> gS,
                                         Supplier<Integer> bG, Consumer<Integer> bS,
                                         Supplier<Integer> aG, Consumer<Integer> aS,
                                         final Supplier<Integer> packed) {
        Entry root = new BoolEntry(name, tip, enG, enS) {
            @Override boolean hasColorSwatch() { return true; }
            @Override int swatchColor() { return packed.get(); }
        };
        addRgbSliderChildren(root, rG, rS, gG, gS, bG, bS);
        root.addChild(new IntEntry("Opacity", null, aG, aS, 0, 255, 1, null));
        return root;
    }

    private void guiChat(String msg) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(OverlayManager.PREFIX + msg));
        }
    }

    private Entry buildPresetsModule() {
        final GuiClickMenu self = this;
        Entry presets = new GroupEntry("Presets", "Save, load, or reset appearance packs");
        presets.addChild(new ActionEntry("Save Preset", "Save appearance pack and show the file path", () ->
            Minecraft.getMinecraft().displayGuiScreen(new GuiTextInput(self, "Save Preset",
                "Name (letters, numbers, _ -)", "", name -> {
                    try {
                        AppearancePreset.save(LazifyConfig.INSTANCE.getConfigDir(), name);
                        File f = AppearancePreset.fileFor(LazifyConfig.INSTANCE.getConfigDir(), name);
                        guiChat("\u00a7aSaved to \u00a7e" + (f != null ? f.getAbsolutePath() : AppearancePreset.sanitizeName(name)));
                        persistUiState();
                        buildEntries();
                        clampScroll();
                    } catch (Exception e) {
                        guiChat("\u00a7cSave failed: \u00a7e" + e.getMessage());
                    }
                }))));
        presets.addChild(new ActionEntry("Reset Appearance", "Restore default colors / layout / scales", () -> {
            LazifyConfig.INSTANCE.resetAppearance();
            LazifyConfig.INSTANCE.save();
            OverlayManager.INSTANCE.defaultSettings();
            guiChat("\u00a7aAppearance reset to defaults");
            persistUiState();
            buildEntries();
            clampScroll();
        }));
        presets.addChild(new ActionEntry("Open Folder", "Open the presets directory in Explorer", () -> {
            try {
                File dir = AppearancePreset.presetsDir(LazifyConfig.INSTANCE.getConfigDir());
                AppearancePreset.openFolder(LazifyConfig.INSTANCE.getConfigDir());
                guiChat("\u00a7aOpened \u00a7e" + dir.getAbsolutePath());
            } catch (Exception e) {
                guiChat("\u00a7cCould not open folder: \u00a7e" + e.getMessage());
            }
        }));
        Entry load = new GroupEntry("Load", "Apply a saved preset");
        List<String> names = AppearancePreset.listNames(LazifyConfig.INSTANCE.getConfigDir());
        if (names.isEmpty()) {
            load.addChild(new GroupEntry("(none saved)", "Use Save Preset first"));
        } else {
            for (final String n : names) {
                Entry item = new GroupEntry(n, "Load or delete this preset");
                item.addChild(new ActionEntry("Load", "Apply " + n, () -> {
                    try {
                        AppearancePreset.load(LazifyConfig.INSTANCE.getConfigDir(), n);
                        OverlayManager.INSTANCE.defaultSettings();
                        guiChat("\u00a7aLoaded preset \u00a7e" + n);
                        persistUiState();
                        buildEntries();
                        clampScroll();
                    } catch (Exception e) {
                        guiChat("\u00a7cLoad failed: \u00a7e" + e.getMessage());
                    }
                }));
                item.addChild(new ActionEntry("Delete", "Remove " + n + " from disk", () -> {
                    if (AppearancePreset.delete(LazifyConfig.INSTANCE.getConfigDir(), n)) {
                        guiChat("\u00a7eDeleted preset \u00a7c" + n);
                    } else {
                        guiChat("\u00a7cCould not delete \u00a7e" + n);
                    }
                    persistUiState();
                    buildEntries();
                    clampScroll();
                }));
                load.addChild(item);
            }
        }
        presets.addChild(load);
        return presets;
    }

    private Entry buildScaleModule(String name, String tip, final String scaleId,
                                   Supplier<Boolean> enG, Consumer<Boolean> enS,
                                   Supplier<ThresholdColorScale> scaleG, Consumer<ThresholdColorScale> scaleS,
                                   double minBound, double maxBound, double step, String unit) {
        Entry root = new BoolEntry(name, tip, enG, enS);
        final ThresholdColorScale scale = scaleG.get();
        for (int i = 0; i < scale.size(); i++) {
            final int idx = i;
            TierEntry tier = new TierEntry("Tier " + (i + 1), scaleId, idx, scaleG, scaleS);
            tier.addChild(new DblEntry("Min", unit.isEmpty() ? "Minimum value for this color" : "Minimum (" + unit + ")",
                    () -> scaleG.get().get(idx).min,
                    v -> {
                        ThresholdColorScale s = scaleG.get();
                        ThresholdColorScale.Tier nt = s.get(idx).copy();
                        nt.min = v;
                        s.set(idx, nt);
                        scaleS.accept(s);
                    }, minBound, maxBound, step));
            tier.addChild(rgbGroup("Color", "RGB for this tier",
                    () -> scaleG.get().get(idx).r, v -> updateTierChannel(scaleG, scaleS, idx, 0, v),
                    () -> scaleG.get().get(idx).g, v -> updateTierChannel(scaleG, scaleS, idx, 1, v),
                    () -> scaleG.get().get(idx).b, v -> updateTierChannel(scaleG, scaleS, idx, 2, v)));
            if (scale.size() > 1) {
                tier.addChild(new ActionEntry("Remove Tier", "Delete this threshold", () -> {
                    ThresholdColorScale s = scaleG.get();
                    s.remove(idx);
                    scaleS.accept(s);
                    LazifyConfig.INSTANCE.save();
                    persistUiState();
                    buildEntries();
                    clampScroll();
                }));
            }
            root.addChild(tier);
        }
        root.addChild(new ActionEntry("+ Add Tier", "Add another threshold", () -> {
            ThresholdColorScale s = scaleG.get();
            double nextMin = s.size() == 0 ? 0 : s.get(s.size() - 1).min + step;
            if (nextMin > maxBound) nextMin = maxBound;
            s.add(new ThresholdColorScale.Tier(nextMin, 255, 255, 255));
            scaleS.accept(s);
            LazifyConfig.INSTANCE.save();
            persistUiState();
            buildEntries();
            clampScroll();
        }));
        return root;
    }

    /** Draggable color-tier card (same interaction model as column cards). */
    private class TierEntry extends Entry {
        final String scaleId;
        final int tierIndex;
        final Supplier<ThresholdColorScale> scaleG;
        final Consumer<ThresholdColorScale> scaleS;

        TierEntry(String label, String scaleId, int tierIndex,
                  Supplier<ThresholdColorScale> scaleG, Consumer<ThresholdColorScale> scaleS) {
            super(label, "Drag to reorder");
            this.scaleId = scaleId;
            this.tierIndex = tierIndex;
            this.scaleG = scaleG;
            this.scaleS = scaleS;
        }

        String valueText() { return ""; }
        void onClick(int button) { expanded = !expanded; }
        boolean expandsOnClick() { return false; }
        boolean hasColorSwatch() { return true; }
        int swatchColor() {
            ThresholdColorScale s = scaleG.get();
            if (tierIndex < 0 || tierIndex >= s.size()) return 0xFFFFFFFF;
            return s.get(tierIndex).argb();
        }
    }

    private void updateTierChannel(Supplier<ThresholdColorScale> scaleG, Consumer<ThresholdColorScale> scaleS,
                                   int idx, int channel, int value) {
        ThresholdColorScale s = scaleG.get();
        ThresholdColorScale.Tier nt = s.get(idx).copy();
        if (channel == 0) nt.r = value;
        else if (channel == 1) nt.g = value;
        else nt.b = value;
        s.set(idx, nt);
        scaleS.accept(s);
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
        activeTab = savedActiveTab;
        if (activeTab < 0 || activeTab >= TABS.length) activeTab = 0;
        pw = Math.min(420, width - 40);
        ph = Math.min(height - 30, 320);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        contentX = px + SIDEBAR_W;
        contentY = py + HEADER_H;
        contentW = pw - SIDEBAR_W;
        contentH = ph - HEADER_H;
        buildEntries();
        scrollY = savedScroll[activeTab];
        clampScroll();
    }

    @Override
    public void onGuiClosed() {
        persistUiState();
        super.onGuiClosed();
    }

    private void persistUiState() {
        savedActiveTab = activeTab;
        savedScroll[activeTab] = scrollY;
        String prefix = TABS[activeTab] + "/";
        Iterator<String> it = savedExpanded.iterator();
        while (it.hasNext()) {
            if (it.next().startsWith(prefix)) it.remove();
        }
        collectExpanded(entries, TABS[activeTab], savedExpanded);
    }

    private void collectExpanded(List<Entry> list, String pathPrefix, Set<String> out) {
        for (Entry e : list) {
            String path = pathPrefix + "/" + e.label;
            if (e.expanded) out.add(path);
            if (!e.children.isEmpty()) collectExpanded(e.children, path, out);
        }
    }

    private void applyExpanded(List<Entry> list, String pathPrefix) {
        for (Entry e : list) {
            String path = pathPrefix + "/" + e.label;
            e.expanded = savedExpanded.contains(path);
            if (!e.children.isEmpty()) applyExpanded(e.children, path);
        }
    }

    private void clampScroll() {
        int maxScroll = Math.max(0, totalVisibleHeight() - contentH);
        scrollY = Math.max(0, Math.min(scrollY, maxScroll));
    }

    /** Rebuild Appearance when theme changes so Mellow Colors show/hide. */
    private void maybeRefreshAppearance(Entry e) {
        if (activeTab != 3 || e == null) return;
        if (!"Overlay Theme".equals(e.label)) return;
        persistUiState();
        buildEntries();
        clampScroll();
    }

    /** Right-click a slider → type an exact number (clamped to its min/max). */
    private void openNumberInput(Entry e) {
        persistUiState();
        final GuiClickMenu self = this;
        if (e instanceof IntEntry) {
            final IntEntry ie = (IntEntry) e;
            String hint = "Integer " + ie.min + " – " + ie.max + "  (Enter to save)";
            if (ie.names != null) {
                hint = "Index " + ie.min + " – " + ie.max + "  (Enter to save)";
            }
            Minecraft.getMinecraft().displayGuiScreen(new GuiTextInput(
                    self, ie.label, hint, String.valueOf(ie.getter.get()),
                    text -> {
                        try {
                            int v = Integer.parseInt(text.trim());
                            v = Math.max(ie.min, Math.min(ie.max, v));
                            // Snap to step
                            v = Math.round((float) (v - ie.min) / ie.step) * ie.step + ie.min;
                            v = Math.max(ie.min, Math.min(ie.max, v));
                            ie.setter.accept(v);
                            LazifyConfig.INSTANCE.save();
                            OverlayManager.INSTANCE.defaultSettings();
                            maybeRefreshAppearance(ie);
                        } catch (NumberFormatException ignored) { }
                    }));
            return;
        }
        if (e instanceof DblEntry) {
            final DblEntry de = (DblEntry) e;
            Minecraft.getMinecraft().displayGuiScreen(new GuiTextInput(
                    self, de.label,
                    "Number " + de.min + " – " + de.max + "  (Enter to save)",
                    String.valueOf(de.getter.get()),
                    text -> {
                        try {
                            double v = Double.parseDouble(text.trim());
                            v = Math.max(de.min, Math.min(de.max, v));
                            v = Math.round(v / de.step) * de.step;
                            v = Math.max(de.min, Math.min(de.max, Math.round(v * 1000.0) / 1000.0));
                            de.setter.accept(v);
                            LazifyConfig.INSTANCE.save();
                            OverlayManager.INSTANCE.defaultSettings();
                        } catch (NumberFormatException ignored) { }
                    }));
        }
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
                autoTab.addChild(new BoolEntry("Disable in Lobby", "Skip auto-add + chat alerts in main BW lobby", c::isDisableInLobby, c::setDisableInLobby));
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
                Entry autoPl = new BoolEntry("Auto /pl", "Auto send /pl for party detection", c::isAutoPl, c::setAutoPl);
                autoPl.addChild(new BoolEntry("Hide /pl", "Hide party list from auto /pl in chat", c::isHidePl, c::setHidePl));
                entries.add(autoPl);
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
                // Top-level actions / packs
                entries.add(buildPresetsModule());

                Entry layout = new GroupEntry("Layout", "Theme, spacing, text, and number format");
                layout.addChild(new IntEntry("Overlay Theme", "Lazify HUD, Nerdify HUD, or Mellow tab stats",
                    c::getOverlayTheme, c::setOverlayTheme, 0, 2, 1,
                    new String[]{"Lazify", "Nerdify", "Mellow"}));
                layout.addChild(new IntEntry("Column Gap", "Horizontal space between columns", c::getOverlayColGap, c::setOverlayColGap, 0, 40, 1, null));
                layout.addChild(new IntEntry("Row Gap", "Vertical space between rows", c::getOverlayRowGap, c::setOverlayRowGap, 0, 20, 1, null));
                layout.addChild(new IntEntry("Padding", "Equal inset on all four sides (headers + rows)", c::getOverlayPad, c::setOverlayPad, 0, 24, 1, null));
                layout.addChild(new IntEntry("Scale", "Overlay size (100 = normal)", c::getOverlayScalePercent, c::setOverlayScalePercent, 50, 200, 5, null));
                layout.addChild(new BoolEntry("Text Shadow", "Drop shadow on overlay text", c::isTextShadow, c::setTextShadow));
                layout.addChild(new BoolEntry("Header Bold", "Bold column header labels", c::isHeaderBold, c::setHeaderBold));
                Entry numbers = new GroupEntry("Number Format", "How stats are displayed");
                numbers.addChild(new IntEntry("FKDR Decimals", "Decimal places for FKDR/WLR/BBLR/KDR",
                    c::getFkdrDecimals, c::setFkdrDecimals, 0, 3, 1, null));
                numbers.addChild(new BoolEntry("Abbreviate", "1200 → 1.2k for stars / encounters / level",
                    c::isAbbreviateNumbers, c::setAbbreviateNumbers));
                numbers.addChild(new IntEntry("Ping Style", null, c::getPingStyle, c::setPingStyle, 0, 1, 1,
                    new String[]{"Number", "With ms"}));
                layout.addChild(numbers);
                entries.add(layout);

                Entry panel = new GroupEntry("Panel", "Background, corners, and outline");
                panel.addChild(rgbaGroup("Background", "Overlay background RGB + opacity", "Opacity",
                    c::getBgR, c::setBgR, c::getBgG, c::setBgG, c::getBgB, c::setBgB,
                    c::getBgOpacity, c::setBgOpacity));
                panel.addChild(new IntEntry("Corner Radius", "Rounds background corners (works without outline)",
                    c::getBorderRadius, c::setBorderRadius, 0, 16, 1, null));
                Entry outline = new BoolEntry("Outline", "Border drawn outside the rounded background",
                        c::isOutlineEnabled, c::setOutlineEnabled) {
                    @Override boolean hasColorSwatch() { return true; }
                    @Override int swatchColor() {
                        if (LazifyConfig.INSTANCE.isOutlineChroma()) {
                            return ColorUtil.getChroma(1L, 255);
                        }
                        return LazifyConfig.INSTANCE.getOutlineColor();
                    }
                };
                outline.addChild(new BoolEntry("Chroma", "Rainbow outline (ignores RGB below)", c::isOutlineChroma, c::setOutlineChroma));
                outline.addChild(new DblEntry("Thickness", "Outline stroke width",
                    () -> (double) c.getOutlineWidth(), c::setOutlineWidth, 0.5, 8.0, 0.5));
                addRgbSliderChildren(outline, c::getOutlineR, c::setOutlineR, c::getOutlineG, c::setOutlineG,
                        c::getOutlineB, c::setOutlineB);
                panel.addChild(outline);
                entries.add(panel);

                Entry rows = new GroupEntry("Rows", "Stripes and role highlights");
                Entry stripes = new BoolEntry("Row Stripes", "Alternating row tint",
                        c::isStripeEnabled, c::setStripeEnabled) {
                    @Override boolean hasColorSwatch() { return true; }
                    @Override int swatchColor() { return LazifyConfig.INSTANCE.getStripeColor(); }
                };
                addRgbSliderChildren(stripes, c::getStripeR, c::setStripeR, c::getStripeG, c::setStripeG,
                        c::getStripeB, c::setStripeB);
                stripes.addChild(new IntEntry("Opacity", null, c::getStripeA, c::setStripeA, 0, 255, 1, null));
                rows.addChild(stripes);
                Entry highlights = new GroupEntry("Highlights", "Tint by role (self > party > tagged > nick)");
                highlights.addChild(highlightModule("Self", "Tint your own row",
                    c::isHighlightSelf, c::setHighlightSelf,
                    c::getHighlightSelfR, c::setHighlightSelfR, c::getHighlightSelfG, c::setHighlightSelfG,
                    c::getHighlightSelfB, c::setHighlightSelfB, c::getHighlightSelfA, c::setHighlightSelfA,
                    c::getHighlightSelfColor));
                highlights.addChild(highlightModule("Party", "Tint party members",
                    c::isHighlightParty, c::setHighlightParty,
                    c::getHighlightPartyR, c::setHighlightPartyR, c::getHighlightPartyG, c::setHighlightPartyG,
                    c::getHighlightPartyB, c::setHighlightPartyB, c::getHighlightPartyA, c::setHighlightPartyA,
                    c::getHighlightPartyColor));
                highlights.addChild(highlightModule("Nicked", "Tint unresolved nicks",
                    c::isHighlightNicked, c::setHighlightNicked,
                    c::getHighlightNickedR, c::setHighlightNickedR, c::getHighlightNickedG, c::setHighlightNickedG,
                    c::getHighlightNickedB, c::setHighlightNickedB, c::getHighlightNickedA, c::setHighlightNickedA,
                    c::getHighlightNickedColor));
                highlights.addChild(highlightModule("Tagged", "Tint cheater-tagged rows",
                    c::isHighlightTagged, c::setHighlightTagged,
                    c::getHighlightTaggedR, c::setHighlightTaggedR, c::getHighlightTaggedG, c::setHighlightTaggedG,
                    c::getHighlightTaggedB, c::setHighlightTaggedB, c::getHighlightTaggedA, c::setHighlightTaggedA,
                    c::getHighlightTaggedColor));
                rows.addChild(highlights);
                entries.add(rows);

                Entry headers = new GroupEntry("Header Colors", "Colors for enabled column tags");
                headers.addChild(rgbGroup("All", "Apply RGB to every header at once",
                    c::getHeaderAllR, c::setHeaderAllR,
                    c::getHeaderAllG, c::setHeaderAllG,
                    c::getHeaderAllB, c::setHeaderAllB));
                String[] colOrd = c.getColOrder().split(",");
                Set<String> listed = new HashSet<>();
                for (String colName : colOrd) {
                    colName = colName.trim();
                    if (colName.isEmpty() || !c.isColumnEnabledByName(colName)) continue;
                    String key = LazifyConfig.colNameToKey(colName);
                    if (!listed.add(key)) continue;
                    headers.addChild(headerRgbGroup(c, key, "[" + LazifyConfig.headerLabelForKey(key).toUpperCase() + "] header RGB"));
                }
                for (String key : LazifyConfig.HEADER_COL_KEYS) {
                    if (listed.contains(key)) continue;
                    String name = key;
                    if ("player".equals(key)) name = "username";
                    else if ("seen".equals(key)) name = "encounters";
                    else if ("netlevel".equals(key)) name = "level";
                    if (!c.isColumnEnabledByName(name)) continue;
                    headers.addChild(headerRgbGroup(c, key, "[" + LazifyConfig.headerLabelForKey(key).toUpperCase() + "] header RGB"));
                }
                entries.add(headers);

                // Stat threshold colors — only for enabled columns
                Entry statColors = new GroupEntry("Stat Colors", "Threshold RGB for enabled columns");
                boolean anyStatColor = false;
                if (c.isColFkdr() || c.isColWlr() || c.isColBblr() || c.isColKdr()) {
                    statColors.addChild(buildScaleModule("FKDR Colors", "FKDR / WLR / BBLR / KDR tiers (RGB)", "fkdr",
                            c::isFkdrColors, c::setFkdrColors, c::getFkdrScale, c::setFkdrScale,
                            0, 2000, 0.1, ""));
                    anyStatColor = true;
                }
                if (c.isColWinstreaks()) {
                    statColors.addChild(buildScaleModule("WS Colors", "Winstreak color tiers", "ws",
                            c::isWsColors, c::setWsColors, c::getWsScale, c::setWsScale,
                            0, 2000, 1, ""));
                    anyStatColor = true;
                }
                if (c.isColPing()) {
                    statColors.addChild(buildScaleModule("Ping Colors", "Ping tiers (ms)", "ping",
                            c::isPingColors, c::setPingColors, c::getPingScale, c::setPingScale,
                            0, 500, 1, "ms"));
                    anyStatColor = true;
                }
                if (c.isColSession()) {
                    statColors.addChild(buildScaleModule("Session Colors", "Session length tiers (minutes)", "session",
                            c::isSessionColors, c::setSessionColors, c::getSessionScale, c::setSessionScale,
                            0, 600, 0.5, "m"));
                    anyStatColor = true;
                }
                if (c.isColEncounters()) {
                    statColors.addChild(buildScaleModule("Encounter Colors", "Times-seen tiers", "encounters",
                            c::isEncountersColors, c::setEncountersColors, c::getEncountersScale, c::setEncountersScale,
                            0, 50, 1, ""));
                    anyStatColor = true;
                }
                if (anyStatColor) entries.add(statColors);

                if (c.getOverlayTheme() == 2) {
                    Entry mellow = new GroupEntry("Mellow Colors", "Tab panel colors");
                    mellow.addChild(rgbaGroup("Outer", "Outer panel",
                        c::getMellowOuterR, c::setMellowOuterR, c::getMellowOuterG, c::setMellowOuterG,
                        c::getMellowOuterB, c::setMellowOuterB, c::getMellowOuterA, c::setMellowOuterA));
                    mellow.addChild(rgbaGroup("Header Row", "Header background",
                        c::getMellowHeaderR, c::setMellowHeaderR, c::getMellowHeaderG, c::setMellowHeaderG,
                        c::getMellowHeaderB, c::setMellowHeaderB, c::getMellowHeaderA, c::setMellowHeaderA));
                    mellow.addChild(rgbaGroup("Player Row", "Normal row background",
                        c::getMellowRowR, c::setMellowRowR, c::getMellowRowG, c::setMellowRowG,
                        c::getMellowRowB, c::setMellowRowB, c::getMellowRowA, c::setMellowRowA));
                    mellow.addChild(rgbaGroup("Tagged Row", "Row with cheater tags",
                        c::getMellowTaggedR, c::setMellowTaggedR, c::getMellowTaggedG, c::setMellowTaggedG,
                        c::getMellowTaggedB, c::setMellowTaggedB, c::getMellowTaggedA, c::setMellowTaggedA));
                    entries.add(mellow);
                }

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
                colMap.put("wlr",        new ColEntry("WLR",        "wlr",        c::isColWlr,        c::setColWlr));
                colMap.put("bblr",       new ColEntry("BBLR",       "bblr",       c::isColBblr,       c::setColBblr));
                colMap.put("kdr",        new ColEntry("KDR",        "kdr",        c::isColKdr,        c::setColKdr));
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
        applyExpanded(entries, TABS[activeTab]);
        rebuildVisible();
    }

    private boolean isColumnsTab() {
        return activeTab == 4;
    }

    private int visibleRowHeight(int index) {
        if (index < 0 || index >= visibleRows.size()) return ROW_H + ROW_GAP;
        Entry e = visibleRows.get(index).entry;
        if (e instanceof ColEntry) return COL_CARD_H + COL_CARD_GAP;
        if (e instanceof TierEntry) return TIER_CARD_H + TIER_CARD_GAP;
        return ROW_H + ROW_GAP;
    }

    private int visibleRowBodyHeight(int index) {
        if (index < 0 || index >= visibleRows.size()) return ROW_H;
        Entry e = visibleRows.get(index).entry;
        if (e instanceof ColEntry) return COL_CARD_H;
        if (e instanceof TierEntry) return TIER_CARD_H;
        return ROW_H;
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

    /** Drop index among TierEntry cards of the same scale (0..count). */
    private int computeTierDropIndex(int mouseY, String scaleId) {
        List<Integer> tierVis = new ArrayList<>();
        for (int i = 0; i < visibleRows.size(); i++) {
            Entry e = visibleRows.get(i).entry;
            if (e instanceof TierEntry && scaleId.equals(((TierEntry) e).scaleId)) {
                tierVis.add(i);
            }
        }
        for (int local = 0; local < tierVis.size(); local++) {
            int vi = tierVis.get(local);
            int ry = visibleRowY(vi);
            int h = visibleRowBodyHeight(vi);
            if (mouseY < ry + h / 2) return local;
        }
        return tierVis.size();
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

    private void applyTierReorder(int fromLocal, int toLocal, String scaleId) {
        TierEntry sample = null;
        for (VisibleRow vr : visibleRows) {
            if (vr.entry instanceof TierEntry && scaleId.equals(((TierEntry) vr.entry).scaleId)) {
                sample = (TierEntry) vr.entry;
                break;
            }
        }
        if (sample == null) return;
        ThresholdColorScale s = sample.scaleG.get();
        s.move(fromLocal, toLocal);
        sample.scaleS.accept(s);
        LazifyConfig.INSTANCE.save();
        persistUiState();
        buildEntries();
        clampScroll();
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

    private String ellipsize(String s, int maxW) {
        if (s == null || s.isEmpty()) return "";
        if (fontRendererObj.getStringWidth(s) <= maxW) return s;
        String out = s;
        while (out.length() > 0 && fontRendererObj.getStringWidth(out + "...") > maxW) {
            out = out.substring(0, out.length() - 1);
        }
        return out.isEmpty() ? "..." : out + "...";
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
        int maxTextW = cardW - COL_GRIP_W - TOGGLE_W - 28;
        fontRendererObj.drawStringWithShadow(ellipsize(e.label, maxTextW), labelX, cardY + 9, COL_TEXT);
        fontRendererObj.drawStringWithShadow(ellipsize(e.desc, maxTextW), labelX, cardY + 21, COL_TEXT_DIM);

        drawToggle(cardX + cardW - TOGGLE_W - 8, cardY + (cardH - TOGGLE_H) / 2, e.isOn());
    }

    private void drawTierCard(int cardX, int cardY, int cardW, TierEntry e, boolean hover, boolean dragging) {
        int cardH = TIER_CARD_H;
        int bg = dragging ? 0x28000000 : (hover ? 0x22FFFFFF : 0x14FFFFFF);
        Gui.drawRect(cardX, cardY, cardX + cardW, cardY + cardH, bg);
        Gui.drawRect(cardX, cardY, cardX + cardW, cardY + 1, 0x30FFFFFF);
        Gui.drawRect(cardX, cardY + cardH - 1, cardX + cardW, cardY + cardH, 0x18FFFFFF);
        Gui.drawRect(cardX, cardY, cardX + 1, cardY + cardH, 0x18FFFFFF);
        Gui.drawRect(cardX + cardW - 1, cardY, cardX + cardW, cardY + cardH, 0x18FFFFFF);

        drawColGrip(cardX + 6, cardY + cardH / 2);

        String arrow = e.expanded ? "\u25BC" : "\u25B6";
        fontRendererObj.drawStringWithShadow(arrow, cardX + COL_GRIP_W + 6, cardY + 13, COL_EXPAND_ARROW);

        int labelX = cardX + COL_GRIP_W + 20;
        int maxTextW = cardW - COL_GRIP_W - 40;
        fontRendererObj.drawStringWithShadow(ellipsize(e.label, maxTextW), labelX, cardY + 8, COL_TEXT);
        fontRendererObj.drawStringWithShadow(ellipsize(e.desc, maxTextW), labelX, cardY + 20, COL_TEXT_DIM);

        int sc = e.swatchColor() | 0xFF000000;
        Gui.drawRect(cardX + cardW - 18, cardY + (cardH - 10) / 2, cardX + cardW - 6, cardY + (cardH - 10) / 2 + 10, sc);
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
            addVisibleRows(e, 0);
        }
    }

    private void addVisibleRows(Entry e, int indent) {
        visibleRows.add(new VisibleRow(e, indent));
        if (e.expanded && e.hasChildren()) {
            for (Entry child : e.children) {
                addVisibleRows(child, indent + 1);
            }
        }
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        // Live overlay above the dim (undimmed) so Appearance tweaks are visible
        OverlayManager.INSTANCE.renderOverClickGui();

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
            boolean isDraggingThis = listDragActive && listDragFromVis == i;

            if (e instanceof ColEntry && !isChild) {
                if (isDraggingThis) continue;
                int cardX = contentX + COL_CARD_PAD;
                int cardW = contentW - COL_CARD_PAD * 2 - 4;
                drawColCard(cardX, ry, cardW, (ColEntry) e, hover, false);
                continue;
            }

            if (e instanceof TierEntry) {
                if (isDraggingThis) continue;
                int cardX = contentX + COL_CARD_PAD + indent;
                int cardW = contentW - COL_CARD_PAD * 2 - 4 - indent;
                drawTierCard(cardX, ry, cardW, (TierEntry) e, hover, false);
                continue;
            }

            // Background
            if (isChild) {
                Gui.drawRect(contentX, ry, px + pw, ry + rowH, COL_CHILD_BG);
                Gui.drawRect(contentX + 6, ry + 3, contentX + 7, ry + rowH - 3, 0x30FFFFFF);
            } else if (i % 2 == 1) {
                Gui.drawRect(contentX, ry, px + pw, ry + rowH, COL_ROW_ALT);
            }
            if (hover) Gui.drawRect(contentX, ry, px + pw, ry + rowH, COL_ROW_HOVER);
            if (!isChild) {
                Gui.drawRect(contentX + 8, ry + rowH - 1, px + pw - 8, ry + rowH, 0x08FFFFFF);
            }

            int labelX = contentX + 12 + indent;
            int widgetRight = px + pw - 12;
            int widgetLeft = widgetRight - (e.isSlider() ? SLIDER_W + VALUE_RESERVE + 8
                    : (e.isToggle() ? TOGGLE_W + 8 : (e.hasColorSwatch() ? 24 : 40)));
            int maxLabelW = Math.max(40, widgetLeft - labelX - 6);

            // Arrow sits in the left margin — do not shift label/desc
            if (e.hasChildren()) {
                String arrow = e.expanded ? "\u25BC" : "\u25B6";
                fontRendererObj.drawStringWithShadow(arrow, contentX + 2 + indent, ry + 10, COL_EXPAND_ARROW);
            }

            fontRendererObj.drawStringWithShadow(ellipsize(e.label, maxLabelW), labelX, ry + 6,
                    isChild ? 0xFFBBBBCC : COL_TEXT);
            if (e.desc != null) {
                fontRendererObj.drawStringWithShadow(ellipsize(e.desc, maxLabelW), labelX, ry + 17, COL_TEXT_DIM);
            }

            if (e.isToggle()) {
                drawToggle(widgetRight - TOGGLE_W, ry + (rowH - TOGGLE_H) / 2, e.isOn());
            } else if (e.isSlider()) {
                if (draggingIndex == i) {
                    int sx = widgetRight - SLIDER_W;
                    float ratio = (float) (mouseX - sx) / SLIDER_W;
                    ratio = Math.max(0, Math.min(1, ratio));
                    e.setFromSlider(ratio);
                }

                int sx = widgetRight - SLIDER_W;
                int sy = ry + (rowH - 6) / 2;
                int previewRgb = -1;
                int channel = -1;
                if (e instanceof IntEntry) {
                    IntEntry ie = (IntEntry) e;
                    if (ie.rgbPreview != null && ie.rgbChannel >= 0) {
                        int[] rgb = ie.rgbPreview.get();
                        if (rgb != null && rgb.length >= 3) {
                            previewRgb = ColorUtil.rgb(rgb[0], rgb[1], rgb[2]);
                            channel = ie.rgbChannel;
                        }
                    }
                }
                drawSlider(sx, sy, SLIDER_W, 6, e.getSliderPos(), draggingIndex == i || hover, previewRgb, channel);

                // Layout left of slider: [swatch][gap][value][gap][slider] — never overlap
                String val = ellipsize(e.valueText(), VALUE_RESERVE - 4);
                int vw = fontRendererObj.getStringWidth(val);
                int valX = sx - 8 - vw;
                if (previewRgb != -1) {
                    int sw = 8;
                    int swatchX = valX - 4 - sw;
                    int swatchY = ry + (rowH - 8) / 2;
                    Gui.drawRect(swatchX, swatchY, swatchX + sw, swatchY + 8, previewRgb | 0xFF000000);
                }
                fontRendererObj.drawStringWithShadow(val, valX, ry + (rowH - 8) / 2, COL_TEXT_VALUE);
            } else {
                if (e.hasColorSwatch()) {
                    int sc = e.swatchColor() | 0xFF000000;
                    Gui.drawRect(widgetRight - 16, ry + (rowH - 8) / 2, widgetRight - 2, ry + (rowH - 8) / 2 + 8, sc);
                } else {
                    String val = e.valueText();
                    if (val != null && !val.isEmpty()) {
                        int vw = fontRendererObj.getStringWidth(val);
                        fontRendererObj.drawStringWithShadow(val, widgetRight - vw, ry + (rowH - 8) / 2, 0xFFFFFFFF);
                    }
                }
            }
        }

        // List drag ghost (columns or tiers)
        if (listDragActive && listDragFromVis >= 0 && listDragFromVis < visibleRows.size()) {
            Entry dragEntry = visibleRows.get(listDragFromVis).entry;
            int indent = visibleRows.get(listDragFromVis).indent * CHILD_INDENT;
            if (dragEntry instanceof ColEntry) {
                if (listDragDrop >= 0) {
                    int lineY = listDragDrop < visibleRows.size()
                            ? visibleRowY(listDragDrop)
                            : visibleRowY(visibleRows.size() - 1) + visibleRowHeight(visibleRows.size() - 1);
                    int lineX = contentX + COL_CARD_PAD;
                    int lineW = contentW - COL_CARD_PAD * 2 - 4;
                    Gui.drawRect(lineX, lineY - 1, lineX + lineW, lineY + 1, COL_ACCENT);
                }
                int cardX = contentX + COL_CARD_PAD;
                int cardW = contentW - COL_CARD_PAD * 2 - 4;
                drawColCard(cardX, mouseY - listDragOffsetY, cardW, (ColEntry) dragEntry, true, true);
            } else if (dragEntry instanceof TierEntry && listDragScaleId != null) {
                // Drop line among same-scale tier cards
                List<Integer> tierVis = new ArrayList<>();
                for (int ti = 0; ti < visibleRows.size(); ti++) {
                    Entry te = visibleRows.get(ti).entry;
                    if (te instanceof TierEntry && listDragScaleId.equals(((TierEntry) te).scaleId)) {
                        tierVis.add(ti);
                    }
                }
                if (listDragDrop >= 0 && !tierVis.isEmpty()) {
                    int lineY;
                    if (listDragDrop < tierVis.size()) {
                        lineY = visibleRowY(tierVis.get(listDragDrop));
                    } else {
                        int last = tierVis.get(tierVis.size() - 1);
                        lineY = visibleRowY(last) + visibleRowHeight(last);
                    }
                    int lineX = contentX + COL_CARD_PAD + indent;
                    int lineW = contentW - COL_CARD_PAD * 2 - 4 - indent;
                    Gui.drawRect(lineX, lineY - 1, lineX + lineW, lineY + 1, COL_ACCENT);
                }
                int cardX = contentX + COL_CARD_PAD + indent;
                int cardW = contentW - COL_CARD_PAD * 2 - 4 - indent;
                drawTierCard(cardX, mouseY - listDragOffsetY, cardW, (TierEntry) dragEntry, true, true);
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

    private void drawSlider(int x, int y, int w, int h, float pos, boolean highlight,
                            int previewRgb, int channel) {
        if (previewRgb != -1 && channel >= 0 && channel <= 2) {
            // Channel gradient track (black → full channel color)
            int steps = Math.max(8, w / 2);
            for (int i = 0; i < steps; i++) {
                float t = i / (float) (steps - 1);
                int r = channel == 0 ? Math.round(255 * t) : 0;
                int g = channel == 1 ? Math.round(255 * t) : 0;
                int b = channel == 2 ? Math.round(255 * t) : 0;
                int segX = x + (i * w) / steps;
                int segX2 = x + ((i + 1) * w) / steps;
                Gui.drawRect(segX, y, segX2, y + h, 0xFF000000 | (r << 16) | (g << 8) | b);
            }
            // Fill overlay using mixed preview color at current intensity
            int fillW = (int) (w * pos);
            if (fillW > 0) {
                int pr = (previewRgb >> 16) & 0xFF;
                int pg = (previewRgb >> 8) & 0xFF;
                int pb = previewRgb & 0xFF;
                Gui.drawRect(x, y, x + fillW, y + h, 0xFF000000 | (pr << 16) | (pg << 8) | pb);
            }
        } else {
            Gui.drawRect(x, y, x + w, y + h, COL_SLIDER_TRACK);
            int fillW = (int) (w * pos);
            if (fillW > 0) {
                Gui.drawRect(x, y, x + fillW, y + h, highlight ? COL_ACCENT : COL_ACCENT_DIM);
            }
        }
        int fillW = (int) (w * pos);
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
                    persistUiState();
                    activeTab = i;
                    savedActiveTab = i;
                    scrollY = savedScroll[activeTab];
                    listDragFromVis = -1;
                    listDragDrop = -1;
                    listDragActive = false;
                    listDragIsTier = false;
                    listDragScaleId = null;
                    listDragFromLocal = -1;
                    buildEntries();
                    clampScroll();
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

                    // Column cards: drag to reorder, toggle only on switch
                    if (e instanceof ColEntry && row.indent == 0) {
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
                            listDragFromVis = rowIndex;
                            listDragDrop = rowIndex;
                            listDragActive = false;
                            listDragOffsetY = mouseY - ry;
                            listDragIsTier = false;
                            listDragScaleId = null;
                            listDragFromLocal = -1;
                            return;
                        }
                    }

                    // Tier cards: drag to reorder; click arrow / right-click to expand
                    if (e instanceof TierEntry) {
                        TierEntry te = (TierEntry) e;
                        int indentPx = row.indent * CHILD_INDENT;
                        int cardX = contentX + COL_CARD_PAD + indentPx;
                        int arrowX = cardX + COL_GRIP_W + 4;
                        if (mouseButton == 1 || (mouseButton == 0 && mouseX >= arrowX && mouseX <= arrowX + 12)) {
                            te.expanded = !te.expanded;
                            persistUiState();
                            rebuildVisible();
                            return;
                        }
                        if (mouseButton == 0) {
                            listDragFromVis = rowIndex;
                            listDragIsTier = true;
                            listDragScaleId = te.scaleId;
                            listDragFromLocal = te.tierIndex;
                            listDragDrop = computeTierDropIndex(mouseY, te.scaleId);
                            listDragActive = false;
                            listDragOffsetY = mouseY - ry;
                            return;
                        }
                    }

                    // Right-click any numeric slider to type an exact value
                    if (e.isSlider() && mouseButton == 1) {
                        openNumberInput(e);
                        return;
                    }

                    // Expand/collapse nested groups (any indent)
                    if (e.hasChildren() && (mouseButton == 1 || e.expandsOnClick())) {
                        e.expanded = !e.expanded;
                        persistUiState();
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
                    maybeRefreshAppearance(e);
                    return;
                }
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (listDragFromVis >= 0 && clickedMouseButton == 0) {
            listDragActive = true;
            if (listDragIsTier && listDragScaleId != null) {
                listDragDrop = computeTierDropIndex(mouseY, listDragScaleId);
            } else {
                listDragDrop = computeColDropIndex(mouseY);
            }
            return;
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (listDragFromVis >= 0) {
            if (listDragActive) {
                if (listDragIsTier && listDragScaleId != null && listDragFromLocal >= 0) {
                    applyTierReorder(listDragFromLocal, listDragDrop, listDragScaleId);
                } else if (!listDragIsTier) {
                    applyColumnReorder(listDragFromVis, listDragDrop);
                }
            } else if (listDragIsTier && listDragFromVis < visibleRows.size()) {
                // Click without drag → toggle expand
                Entry e = visibleRows.get(listDragFromVis).entry;
                if (e instanceof TierEntry) {
                    e.expanded = !e.expanded;
                    persistUiState();
                    rebuildVisible();
                }
            }
            listDragFromVis = -1;
            listDragDrop = -1;
            listDragActive = false;
            listDragIsTier = false;
            listDragScaleId = null;
            listDragFromLocal = -1;
        }
        if (draggingIndex >= 0) {
            Entry dragged = (draggingIndex < visibleRows.size()) ? visibleRows.get(draggingIndex).entry : null;
            LazifyConfig.INSTANCE.save();
            OverlayManager.INSTANCE.defaultSettings();
            draggingIndex = -1;
            maybeRefreshAppearance(dragged);
        }
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel != 0) {
            int step = isColumnsTab() ? (COL_CARD_H + COL_CARD_GAP) * 2 : (ROW_H + ROW_GAP) * 2;
            scrollY -= Integer.signum(dWheel) * step;
            int maxScroll = Math.max(0, totalVisibleHeight() - contentH);
            scrollY = Math.max(0, Math.min(scrollY, maxScroll));
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1 || keyCode == LazifyMod.guiKeybind.getKeyCode()) {
            persistUiState();
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
