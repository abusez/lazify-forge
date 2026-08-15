package com.lazify.overlay;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import com.lazify.config.LazifyConfig;
import com.lazify.util.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.MathHelper;
import net.minecraft.world.WorldSettings;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Mellow 6.x extended tab layout using Lazify's configured column order and toggles.
 */
public final class MellowTabOverlay {

    private static final Ordering<NetworkPlayerInfo> PLAYER_ORDERING =
            Ordering.from(new PlayerComparator());

    private static final int MAX_TAB_PLAYERS = 80;
    private static final int TOP_Y = 20;
    private static final int ROW_HEIGHT = 12;
    private static final int COLUMN_GAP = 4;
    private static final int HEAD_SIZE = 8;
    private static final int HEAD_RESERVE = 10;

    private static int scrollIndex;
    private static int maxVisiblePlayers = 1;

    private MellowTabOverlay() {}

    public static boolean isActive() {
        return OverlayTheme.isMellow(LazifyConfig.INSTANCE.getOverlayTheme());
    }

    public static void resetScroll() {
        scrollIndex = 0;
        maxVisiblePlayers = 1;
    }

    public static void handleMouseWheel(int wheelDelta, int playerCount) {
        if (wheelDelta == 0) return;
        int effectiveCount = Math.min(playerCount, MAX_TAB_PLAYERS);
        if (effectiveCount <= maxVisiblePlayers) return;
        int maxScroll = Math.max(0, effectiveCount - maxVisiblePlayers);
        scrollIndex = wheelDelta > 0 ? scrollIndex - 1 : scrollIndex + 1;
        scrollIndex = Math.max(0, Math.min(maxScroll, scrollIndex));
    }

    public static void render(OverlayManager manager) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.getNetHandler() == null) return;
        if (mc.gameSettings == null || !mc.gameSettings.keyBindPlayerList.isKeyDown()) return;

        List<ColumnDef> tabColumns = manager.getEnabledTabColumns();
        if (tabColumns.isEmpty()) return;

        List<NetworkPlayerInfo> players = collectPlayers(mc.getNetHandler());
        if (players.isEmpty()) return;

        Map<String, Map<String, Object>> stats = manager.getOverlayPlayersSnapshot();
        if (LazifyConfig.INSTANCE.isStatFilter()) {
            List<NetworkPlayerInfo> filtered = new ArrayList<>();
            for (NetworkPlayerInfo info : players) {
                String uuid = info.getGameProfile().getId().toString().replace("-", "");
                if (manager.passesStatFilter(uuid, stats.get(uuid))) filtered.add(info);
            }
            players = filtered;
            if (players.isEmpty()) return;
        }

        List<Integer> columnWidths = computeColumnWidths(mc.fontRendererObj, manager, tabColumns, players, stats);
        int totalWidth = getTotalWidth(columnWidths);

        ScaledResolution scaled = new ScaledResolution(mc);
        int scaledWidth = scaled.getScaledWidth();
        int scaledHeight = scaled.getScaledHeight();
        int availableWidth = Math.max(100, scaledWidth - 8);
        float fitScale = totalWidth > availableWidth ? (float) availableWidth / (float) totalWidth : 1.0f;
        int scaledPanelWidth = MathHelper.ceiling_float_int(totalWidth * fitScale);
        int startX = Math.max(4, (scaledWidth - scaledPanelWidth) / 2);
        int maxRight = scaledWidth - 4;
        if (startX + scaledPanelWidth > maxRight) {
            startX = Math.max(4, maxRight - scaledPanelWidth);
        }

        int scaledHeader = Math.max(1, MathHelper.ceiling_float_int(12.0f * fitScale));
        int scaledRowStep = Math.max(1, MathHelper.ceiling_float_int(12.0f * fitScale));
        maxVisiblePlayers = Math.max(1, (scaledHeight - (TOP_Y + scaledHeader + 8)) / scaledRowStep);

        int maxScroll = Math.max(0, players.size() - maxVisiblePlayers);
        scrollIndex = Math.max(0, Math.min(maxScroll, scrollIndex));
        int endIndex = Math.min(players.size(), scrollIndex + maxVisiblePlayers);
        List<NetworkPlayerInfo> visible = players.subList(scrollIndex, endIndex);

        int visibleHeight = visible.size() * ROW_HEIGHT;
        int panelContentHeight = ROW_HEIGHT + visibleHeight;

        FontRenderer fr = mc.fontRendererObj;
        int headerTextY = (ROW_HEIGHT - fr.FONT_HEIGHT) / 2;

        GlStateManager.pushMatrix();
        GlStateManager.translate(startX, TOP_Y, 0.0f);
        GlStateManager.scale(fitScale, fitScale, 1.0f);

        LazifyConfig cfg = LazifyConfig.INSTANCE;
        int inset = 4 + cfg.getOverlayPad();
        Gui.drawRect(-inset, -inset, totalWidth + inset, panelContentHeight + inset, cfg.getMellowOuterColor());
        Gui.drawRect(0, 0, totalWidth, ROW_HEIGHT, cfg.getMellowHeaderColor());
        drawHeaders(fr, tabColumns, columnWidths, headerTextY, cfg);

        int rowY = ROW_HEIGHT;
        int rowIndex = 0;
        for (NetworkPlayerInfo info : visible) {
            String uuid = info.getGameProfile().getId().toString().replace("-", "");
            Map<String, Object> ps = stats.get(uuid);
            Gui.drawRect(0, rowY, totalWidth, rowY + ROW_HEIGHT, getRowBackground(cfg, manager, uuid, ps, rowIndex));
            int baselineY = rowY + (ROW_HEIGHT - fr.FONT_HEIGHT) / 2;
            drawValues(mc, fr, manager, tabColumns, columnWidths, info, ps, uuid, 0, baselineY, cfg);
            rowY += ROW_HEIGHT;
            rowIndex++;
        }

        if (maxScroll > 0) {
            int indicatorX = totalWidth - 8;
            if (scrollIndex > 0) {
                fr.drawStringWithShadow("\u00a7f\u25b2", indicatorX, ROW_HEIGHT, -1);
            }
            if (endIndex < players.size()) {
                fr.drawStringWithShadow("\u00a7f\u25bc", indicatorX,
                        panelContentHeight - fr.FONT_HEIGHT - 1, -1);
            }
        }

        GlStateManager.popMatrix();
    }

    private static List<NetworkPlayerInfo> collectPlayers(NetHandlerPlayClient netHandler) {
        List<NetworkPlayerInfo> sorted = PLAYER_ORDERING.sortedCopy(netHandler.getPlayerInfoMap());
        List<NetworkPlayerInfo> filtered = new ArrayList<>(sorted.size());
        for (NetworkPlayerInfo info : sorted) {
            if (isObfuscatedTabEntry(info)) continue;
            if (isGrayTabEntry(info)) continue;
            filtered.add(info);
        }
        if (filtered.size() <= MAX_TAB_PLAYERS) return filtered;
        return new ArrayList<>(filtered.subList(0, MAX_TAB_PLAYERS));
    }

    private static boolean isObfuscatedTabEntry(NetworkPlayerInfo info) {
        if (info == null || info.getGameProfile() == null) return false;
        String name = info.getGameProfile().getName();
        if (name == null || name.length() < 3) return false;
        char first = name.charAt(0);
        for (int i = 1; i < name.length(); i++) {
            if (name.charAt(i) != first) return false;
        }
        return true;
    }

    private static boolean isGrayTabEntry(NetworkPlayerInfo info) {
        if (info == null) return false;
        try {
            if (info.getGameType() == WorldSettings.GameType.SPECTATOR) return true;
        } catch (Throwable ignored) {}
        String formatted = info.getDisplayName() != null
                ? info.getDisplayName().getFormattedText()
                : null;
        if (formatted == null || formatted.isEmpty()) {
            ScorePlayerTeam team = info.getPlayerTeam();
            if (team != null) {
                formatted = team.getColorPrefix()
                        + info.getGameProfile().getName()
                        + team.getColorSuffix();
            } else {
                formatted = info.getGameProfile().getName();
            }
        }
        return ColorUtil.isGrayNamed(formatted);
    }

    private static List<Integer> computeColumnWidths(FontRenderer fr, OverlayManager manager,
            List<ColumnDef> tabColumns, List<NetworkPlayerInfo> players,
            Map<String, Map<String, Object>> stats) {
        List<Integer> widths = new ArrayList<>(tabColumns.size());
        for (ColumnDef col : tabColumns) {
            String colKey = col.getKey();
            String headerLabel = headerLabel(col);
            boolean bold = LazifyConfig.INSTANCE.isHeaderBold();
            String header = headerLabel.isEmpty() ? ""
                    : (bold ? "\u00a7l" + headerLabel + "\u00a7r" : headerLabel);
            int width = Math.max(minColumnWidth(colKey),
                    header.isEmpty() ? 0 : fr.getStringWidth(header) + 6);

            for (NetworkPlayerInfo info : players) {
                String uuid = info.getGameProfile().getId().toString().replace("-", "");
                Map<String, Object> ps = stats.get(uuid);
                String value = manager.resolveMellowTabCell(info, ps, uuid, colKey);
                int contentWidth = fr.getStringWidth(value);
                int extra = OverlayManager.isMellowTabHeadColumn(colKey) ? HEAD_RESERVE : 0;
                width = Math.max(width, contentWidth + 6 + extra);
            }
            width = Math.min(width, maxColumnWidth(colKey));
            widths.add(width);
        }
        return widths;
    }

    private static int getTotalWidth(List<Integer> columnWidths) {
        int total = 0;
        for (int i = 0; i < columnWidths.size(); i++) {
            if (i > 0) total += COLUMN_GAP;
            total += columnWidths.get(i);
        }
        return total;
    }

    private static void drawHeaders(FontRenderer fr, List<ColumnDef> tabColumns,
            List<Integer> columnWidths, int y, LazifyConfig cfg) {
        int x = 0;
        for (int i = 0; i < tabColumns.size(); i++) {
            ColumnDef col = tabColumns.get(i);
            String colKey = col.getKey();
            String headerText = headerLabel(col);
            String header = headerText.isEmpty() ? ""
                    : (cfg.isHeaderBold() ? "\u00a7l" + headerText + "\u00a7r" : headerText);
            int width = columnWidths.get(i);
            if (!header.isEmpty()) {
                int headerWidth = fr.getStringWidth(header);
                int drawX;
                if (OverlayManager.isMellowTabRightAligned(colKey)) {
                    drawX = x + width - 3 - headerWidth;
                } else {
                    drawX = x + 3 + (OverlayManager.isMellowTabHeadColumn(colKey) ? HEAD_RESERVE : 0);
                }
                drawMellowString(fr, header, drawX, y, OverlayManager.INSTANCE.headerColorFor(colKey), cfg.isTextShadow());
            }
            x += width;
            if (i < tabColumns.size() - 1) x += COLUMN_GAP;
        }
    }

    private static void drawValues(Minecraft mc, FontRenderer fr, OverlayManager manager,
            List<ColumnDef> tabColumns, List<Integer> columnWidths,
            NetworkPlayerInfo info, Map<String, Object> ps, String uuid, int startX, int baselineY,
            LazifyConfig cfg) {
        int x = startX;
        for (int i = 0; i < tabColumns.size(); i++) {
            ColumnDef col = tabColumns.get(i);
            String colKey = col.getKey();
            int width = columnWidths.get(i);
            int textStartX = x + 3;
            int reservedLeft = 6;

            if (OverlayManager.isMellowTabHeadColumn(colKey)) {
                int headX = x + 3;
                int headY = baselineY + (fr.FONT_HEIGHT - HEAD_SIZE) / 2;
                drawPlayerHead(mc, info, headX, headY);
                textStartX += HEAD_RESERVE;
                reservedLeft += HEAD_RESERVE;
            }

            int maxTextWidth = Math.max(1, width - reservedLeft);
            String value = fitToWidth(fr,
                    manager.resolveMellowTabCell(info, ps, uuid, colKey), maxTextWidth);
            if (value != null && !value.isEmpty()) {
                int cellColor = manager.mellowCellColor(colKey, ps);
                if (cellColor != -1) value = ColorUtil.strip(value);
                int drawX;
                if (OverlayManager.isMellowTabRightAligned(colKey)) {
                    drawX = x + width - 3 - fr.getStringWidth(value);
                } else {
                    drawX = textStartX;
                }
                drawMellowString(fr, value, drawX, baselineY, cellColor, cfg.isTextShadow());
            }
            x += width;
            if (i < tabColumns.size() - 1) x += COLUMN_GAP;
        }
    }

    private static void drawMellowString(FontRenderer fr, String text, int x, int y, int color, boolean shadow) {
        if (shadow) fr.drawStringWithShadow(text, x, y, color);
        else fr.drawString(text, x, y, color, false);
    }

    private static String headerLabel(ColumnDef col) {
        String header = col.getHeader();
        if (header != null && header.startsWith("[") && header.endsWith("]") && header.length() > 2) {
            return header.substring(1, header.length() - 1).toUpperCase();
        }
        String display = col.getDisplay();
        return display != null ? display.toUpperCase() : "";
    }

    private static int minColumnWidth(String colKey) {
        if (OverlayManager.PLAYER_KEY.equals(colKey)) return 70;
        if (OverlayManager.STAR_KEY.equals(colKey)) return 56;
        if (OverlayManager.FKDR_KEY.equals(colKey)) return 40;
        if (OverlayManager.WINSTREAK_KEY.equals(colKey)) return 42;
        if (OverlayManager.RANK_KEY.equals(colKey)) return 48;
        return 36;
    }

    private static int maxColumnWidth(String colKey) {
        if (OverlayManager.PLAYER_KEY.equals(colKey)) return 230;
        if (OverlayManager.STAR_KEY.equals(colKey)) return 70;
        if (OverlayManager.RANK_KEY.equals(colKey)) return 120;
        return 72;
    }

    private static int getRowBackground(LazifyConfig cfg, OverlayManager manager,
            String uuid, Map<String, Object> ps, int rowIndex) {
        int tint = manager.resolveRowTint(uuid, ps, rowIndex);
        if (tint != 0) return tint;
        if (ps != null && hasTag(ps)) return cfg.getMellowTaggedColor();
        return cfg.getMellowRowColor();
    }

    private static boolean hasTag(Map<String, Object> ps) {
        return !stringVal(ps.get("urchinTagType")).isEmpty()
                || !stringVal(ps.get("seraphTagType")).isEmpty();
    }

    private static void drawPlayerHead(Minecraft mc, NetworkPlayerInfo info, int x, int y) {
        if (info.getGameProfile() == null || info.getLocationSkin() == null) return;
        EntityPlayer entityPlayer = mc.theWorld == null ? null
                : mc.theWorld.getPlayerEntityByUUID(info.getGameProfile().getId());
        boolean upsideDown = entityPlayer != null && entityPlayer.isWearing(EnumPlayerModelParts.CAPE)
                && ("Dinnerbone".equals(info.getGameProfile().getName())
                || "Grumm".equals(info.getGameProfile().getName()));
        int vBase = 8 + (upsideDown ? 8 : 0);
        int vSize = 8 * (upsideDown ? -1 : 1);
        mc.getTextureManager().bindTexture(info.getLocationSkin());
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        Gui.drawScaledCustomSizeModalRect(x, y, 8.0f, vBase, 8, vSize, HEAD_SIZE, HEAD_SIZE, 64.0f, 64.0f);
        if (entityPlayer != null && entityPlayer.isWearing(EnumPlayerModelParts.HAT)) {
            Gui.drawScaledCustomSizeModalRect(x, y, 40.0f, vBase, 8, vSize, HEAD_SIZE, HEAD_SIZE, 64.0f, 64.0f);
        }
    }

    private static String fitToWidth(FontRenderer fr, String value, int width) {
        if (value == null || value.isEmpty()) return "";
        if (fr.getStringWidth(value) <= width) return value;
        String suffix = "\u00a77...";
        int suffixWidth = fr.getStringWidth(suffix);
        String trimmed = fr.trimStringToWidth(value, Math.max(0, width - suffixWidth));
        return trimmed == null || trimmed.isEmpty() ? "" : trimmed + suffix;
    }

    private static String stringVal(Object o) {
        if (o == null) return "";
        String s = String.valueOf(o);
        return "null".equals(s) ? "" : s;
    }

    private static final class PlayerComparator implements Comparator<NetworkPlayerInfo> {
        @Override
        public int compare(NetworkPlayerInfo first, NetworkPlayerInfo second) {
            ScorePlayerTeam firstTeam = first.getPlayerTeam();
            ScorePlayerTeam secondTeam = second.getPlayerTeam();
            return ComparisonChain.start()
                    .compareTrueFirst(first.getGameType() != WorldSettings.GameType.SPECTATOR,
                            second.getGameType() != WorldSettings.GameType.SPECTATOR)
                    .compare(firstTeam != null ? firstTeam.getRegisteredName() : "",
                            secondTeam != null ? secondTeam.getRegisteredName() : "")
                    .compare(first.getGameProfile().getName(), second.getGameProfile().getName())
                    .result();
        }
    }
}
