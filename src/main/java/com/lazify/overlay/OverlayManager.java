package com.lazify.overlay;

import com.lazify.LazifyMod;
import com.lazify.api.HttpUtil;
import com.lazify.api.JsonWrapper;
import com.lazify.config.LazifyConfig;
import com.lazify.util.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.ChatComponentText;

import org.lwjgl.input.Keyboard;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class OverlayManager {

    public static final OverlayManager INSTANCE = new OverlayManager();

    // ── Data keys (display) ────────────────────────────────────────────────────
    static final String PLAYER_KEY    = "player";
    static final String ENCOUNTERS_KEY= "seen";
    static final String TAGS_KEY      = "tags";
    static final String STAR_KEY      = "star";
    static final String FKDR_KEY      = "fkdr";
    static final String WINSTREAK_KEY = "winstreaks";
    static final String SESSION_KEY   = "session";
    static final String URCHIN_KEY    = "urchin";

    // ── Data keys (sort values) ────────────────────────────────────────────────
    static final String ENCOUNTERS_VALUE = "seenvalue";
    static final String JOIN_VALUE       = "joinvalue";
    static final String STAR_VALUE       = "starvalue";
    static final String FKDR_VALUE       = "fkdrvalue";
    static final String INDEX_VALUE      = "indexvalue";
    static final String SESSION_VALUE    = "sessionvalue";
    static final String WINSTREAK_VALUE  = "winstreakvalue";

    // ── API keys (read from config) ────────────────────────────────────────────
    private String hypixelKey() { return LazifyConfig.INSTANCE.getHypixelKey(); }
    private String urchinKey()  { return LazifyConfig.INSTANCE.getUrchinKey(); }

    // ── Core state (keyed by UUID without dashes) ──────────────────────────────
    Map<String, Map<String, Object>> overlayPlayers = new ConcurrentHashMap<>();
    Map<String, String>              ignoredPlayers  = new HashMap<>();
    List<String>                     currentPlayers  = Collections.synchronizedList(new ArrayList<>());
    Map<String, List<Object[]>>      playerEncounters= new HashMap<>();
    Map<String, String>              teams           = new HashMap<>();
    Map<String, Map<String, Object>> statsCache      = new ConcurrentHashMap<>();
    Map<String, String>              urchinCache     = new ConcurrentHashMap<>();

    // ── Column / sort / tag metadata ──────────────────────────────────────────
    List<ColumnDef>       columns        = new ArrayList<>();
    List<String>          sortingOptions = new ArrayList<>();
    Map<String, String>   parseSortingMode = new HashMap<>();
    List<String>          tags           = new ArrayList<>();

    // ── Visibility ─────────────────────────────────────────────────────────────
    boolean visible = true;

    public void toggleVisible()       { visible = !visible; }
    public void setVisible(boolean v) { visible = v; }

    // ── Game state ─────────────────────────────────────────────────────────────
    String  currentLobby = "";
    String  lastLobby    = "";
    int     status       = -1;
    boolean ascending    = false;
    boolean showYourself    = false;
    boolean showTeamPrefix  = false;
    boolean showTeamColors  = true;
    String  sortBy       = FKDR_VALUE;
    int     overlayTicks = 5;
    boolean dowho        = true;
    boolean didwho       = false;

    // ── Overlay layout ─────────────────────────────────────────────────────────
    int   startX = 500, startY = 12, offsetY = 3;
    int   endX = 0, endY = 0;
    float borderWidth = 2.5f;
    int   background, borderColorRGB, columnTitles;

    static final String PREFIX = "\u00a77[\u00a7dL\u00a77]\u00a7r ";

    private final Queue<String> pendingMessages = new java.util.concurrent.ConcurrentLinkedQueue<>();
    private final Queue<String> pendingCommands = new java.util.concurrent.ConcurrentLinkedQueue<>();

    private OverlayManager() {}

    // ==========================================================================
    // Init (called once from LazifyMod.init)
    // ==========================================================================

    public void init(File configDir) {
        LazifyConfig.INSTANCE.load(configDir);

        columns.clear(); sortingOptions.clear(); parseSortingMode.clear(); tags.clear();

        addColumn("Encounters", "[E]",       ENCOUNTERS_KEY);
        addColumn("Username",   "[PLAYER]",  PLAYER_KEY);
        addColumn("Star",       "[STAR]",    STAR_KEY);
        addColumn("FKDR",       "[FKDR]",    FKDR_KEY);
        addColumn("Winstreaks", "[WS]",      WINSTREAK_KEY);
        addColumn("Urchin",     "[U]",       URCHIN_KEY);
        addColumn("Session",    "[SESSION]", SESSION_KEY);

        addSortingOption("Encounters", ENCOUNTERS_VALUE);
        addSortingOption("Star",       STAR_VALUE);
        addSortingOption("FKDR",       FKDR_VALUE);
        addSortingOption("Index",      INDEX_VALUE);
        addSortingOption("Winstreak",  WINSTREAK_VALUE);
        addSortingOption("Join Time",  JOIN_VALUE);

        tags.add("nofinaldeaths");
        tags.add("language");

        defaultSettings();
        print(PREFIX + "\u00a7eWelcome to \u00a73Lazify\u00a7e! Please run \u00a73/ov\u00a7e for commands.");
        if (hypixelKey().isEmpty())
            print(PREFIX + "\u00a7cNo Hypixel API key set! Use \u00a73/ov key hypixel <key>\u00a7c to set one.");
        if (urchinKey().isEmpty())
            print(PREFIX + "\u00a7eNo Urchin API key set. Use \u00a73/ov key urchin <key>\u00a7e to enable cheater tags.");
    }

    private void addColumn(String display, String header, String key) {
        ColumnDef col = new ColumnDef(display, header, key,
                OverlayRenderer.getFontWidth(header),
                OverlayRenderer.getFontWidth(header),
                0, true);
        columns.add(col);
    }

    private void addSortingOption(String display, String key) {
        sortingOptions.add(display);
        parseSortingMode.put(display, key);
    }

    public void defaultSettings() {
        LazifyConfig cfg = LazifyConfig.INSTANCE;

        showYourself   = cfg.isShowYourself();
        showTeamPrefix = cfg.isTeamPrefix();
        showTeamColors = cfg.isTeams();
        ascending      = cfg.getSortMode() == 0;
        startX         = cfg.getOverlayX();
        startY         = cfg.getOverlayY();

        int idx = cfg.getSortByIndex();
        if (idx >= 0 && idx < sortingOptions.size())
            sortBy = parseSortingMode.getOrDefault(sortingOptions.get(idx), FKDR_VALUE);

        background     = ColorUtil.getHueRGB(cfg.getBgHue(),     cfg.getBgOpacity());
        columnTitles   = ColorUtil.getHueRGB(cfg.getHeaderHue(), 255);
        borderColorRGB = ColorUtil.getHueRGB(cfg.getBorderHue(), 255);

        // per-column visibility
        for (ColumnDef col : columns) {
            switch (col.getKey()) {
                case ENCOUNTERS_KEY: col.setEnabled(cfg.isColEncounters()); break;
                case PLAYER_KEY:     col.setEnabled(cfg.isColUsername());   break;
                case STAR_KEY:       col.setEnabled(cfg.isColStar());       break;
                case FKDR_KEY:       col.setEnabled(cfg.isColFkdr());       break;
                case WINSTREAK_KEY:  col.setEnabled(cfg.isColWinstreaks()); break;
                case URCHIN_KEY:     col.setEnabled(cfg.isColUrchin());     break;
                case SESSION_KEY:    col.setEnabled(cfg.isColSession());    break;
            }
        }
    }

    // ==========================================================================
    // Tick (called every 5 ticks from EventHandler)
    // ==========================================================================

    public void onTick() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (overlayTicks < 200) overlayTicks++;
        flushPendingMessages();
        defaultSettings();
        updateStatus();

        doColumns(true);

        if (status < 1) return;

        Set<String> currentEntityUUIDs = new HashSet<>();
        long currentTime = System.currentTimeMillis();
        int threshold = LazifyConfig.INSTANCE.getEncountersTimeoutMins() * 60000;

        for (NetworkPlayerInfo pla : mc.getNetHandler().getPlayerInfoMap()) {
            String uuidWithDashes = pla.getGameProfile().getId().toString();
            String uuid = uuidWithDashes.replace("-", "");
            String displayName = pla.getDisplayName() != null
                    ? pla.getDisplayName().getFormattedText()
                    : pla.getGameProfile().getName();
            String username = pla.getGameProfile().getName();

            if (ignoredPlayers.containsKey(username.toLowerCase())) {
                if (isInOverlay(uuid)) {
                    overlayPlayers.remove(uuid);
                    synchronized (currentPlayers) { currentPlayers.remove(uuid); }
                }
                continue;
            }

            currentEntityUUIDs.add(uuid);
            if (isBot(pla)) continue;

            if (isInOverlay(uuid)) {
                // Assign team color once in-game if not yet set
                if (showTeamColors && status == 3 && !teams.containsKey(uuid)
                        && displayName.contains(" ")) {
                    teams.put(uuid, displayName);
                    Map<String, Object> teamData = new HashMap<>();
                    String teamPart = showTeamPrefix ? displayName : displayName.split(" ")[1];
                    teamData.put(PLAYER_KEY, teamPart);
                    addToOverlay(uuid, teamData);
                }
                continue;
            }

            // ── Track encounters ──────────────────────────────────────────────
            // UUID v4: char[14] of UUID-with-dashes is the version digit
            String encKey = uuidWithDashes.charAt(14) == '4' ? uuid : username;
            List<Object[]> encounters = playerEncounters.getOrDefault(encKey, new ArrayList<>());
            final long ct = currentTime;
            final int th = threshold;
            encounters.removeIf(e -> ct - (long) e[1] > th);
            if (encounters.isEmpty() || !encounters.get(encounters.size() - 1)[0].equals(currentLobby)) {
                encounters.add(new Object[]{currentLobby, currentTime});
            }
            playerEncounters.put(encKey, encounters);
            String formattedEncounters = ColorUtil.getSeenColor(encounters.size());

            // ── Build placeholder stats entry ─────────────────────────────────
            Map<String, Object> placeholder = new ConcurrentHashMap<>();
            placeholder.put(JOIN_VALUE,       (int)(currentTime / 1000) * -1);
            placeholder.put(ENCOUNTERS_KEY,   formattedEncounters);
            placeholder.put(ENCOUNTERS_VALUE, (double) encounters.size());
            placeholder.put(PLAYER_KEY,       displayName);

            // Nick detection: UUID without dashes char[12] != '4' → nicked
            if (uuid.charAt(12) != '4') {
                placeholder.put("nicked", true);
                placeholder.put(PLAYER_KEY, username);
                overlayPlayers.put(uuid, placeholder);
                sortOverlay();
                if (LazifyConfig.INSTANCE.isSendNickedToChat()) {
                    print(PREFIX + "\u00a7c" + username + " \u00a7eis nicked");
                }
                addToPlayers(uuid);
                continue;
            }

            overlayPlayers.put(uuid, placeholder);
            addPlaceholderStats(uuid, displayName, false);
            addToPlayers(uuid);

            final String fUuid  = uuid;
            final String fLobby = currentLobby;
            new Thread(() -> handlePlayerStats(fUuid, fLobby)).start();
            new Thread(() -> handleUrchinTag(fUuid, fLobby)).start();
        }

        // Remove players who left (pregame only; in-game tab is authoritative)
        if (status == 2) {
            Iterator<Map.Entry<String, Map<String, Object>>> it = overlayPlayers.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Map<String, Object>> entry = it.next();
                if (currentEntityUUIDs.contains(entry.getKey())) continue;
                if (entry.getValue().containsKey("manual")) continue;
                it.remove();
                doColumns(false);
            }
        }

        // Sync currentPlayers ↔ overlayPlayers
        synchronized (currentPlayers) {
            if (status != 3) {
                Iterator<String> it = currentPlayers.iterator();
                while (it.hasNext()) {
                    if (!isInOverlay(it.next())) { it.remove(); doColumns(false); }
                }
            }
            for (String uuid : overlayPlayers.keySet()) {
                if (!currentPlayers.contains(uuid)) {
                    boolean isNicked = uuid.charAt(12) != '4';
                    int insertAt = (ascending == isNicked) ? 0 : currentPlayers.size();
                    currentPlayers.add(insertAt, uuid);
                    doColumns(false);
                }
            }
        }
    }

    // ==========================================================================
    // Bedwars status detection (mirrors original getBedwarsStatus exactly)
    // ==========================================================================

    private void updateStatus() {
        lastLobby = currentLobby;
        status = getBedwarsStatus();
        if (!lastLobby.equals(currentLobby)) {
            clearMaps();
        }
    }

    private int getBedwarsStatus() {
        List<String> sidebar = getSidebarLines();

        if (sidebar == null) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.theWorld != null) {
                String dim = mc.theWorld.provider.getDimensionName();
                if ("The End".equals(dim)) return 0;
            }
            return -1;
        }

        if (sidebar.size() < 7) return -1;
        if (!ColorUtil.strip(sidebar.get(0)).startsWith("BED WARS")) return -1;

        // Extract lobby ID from line 1: "  LOBBYID  [N]" → split on double-space
        String[] parts = ColorUtil.strip(sidebar.get(1)).split("  ");
        if (parts.length < 2) return -1;
        String lobbyId = parts[1];
        if (lobbyId.charAt(lobbyId.length() - 1) == ']') {
            lobbyId = lobbyId.split(" ")[0];
        }
        currentLobby = lobbyId;

        if (lobbyId.charAt(0) == 'L') return 1;

        String line5 = ColorUtil.strip(sidebar.get(5));
        String line6 = ColorUtil.strip(sidebar.get(6));
        if (line5.startsWith("R Red:") && line6.startsWith("B Blue:")) return 3;
        if (line6.equals("Waiting...") || line6.startsWith("Starting in")) return 2;

        return -1;
    }

    /** Returns sidebar lines top→bottom (index 0 = top). getSortedScores is descending, so no reverse. */
    private List<String> getSidebarLines() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) return null;
        Scoreboard sb = mc.theWorld.getScoreboard();
        ScoreObjective obj = sb.getObjectiveInDisplaySlot(1);
        if (obj == null) return null;

        Collection<Score> scores = sb.getSortedScores(obj);
        List<String> lines = new ArrayList<>();
        for (Score score : scores) {
            ScorePlayerTeam team = sb.getPlayersTeam(score.getPlayerName());
            String prefix = team != null ? team.getColorPrefix() : "";
            String suffix = team != null ? team.getColorSuffix() : "";
            lines.add(prefix + score.getPlayerName() + suffix);
        }
        // getSortedScores returns descending (highest score = top of sidebar = index 0)
        return lines;
    }

    // ==========================================================================
    // Bot detection (mirrors original isBot exactly)
    // ==========================================================================

    private boolean isBot(NetworkPlayerInfo pla) {
        // Original: ping > 1 → is bot (Hypixel NPC entries have 0 or 1 ping)
        if (pla.getResponseTime() > 1) return true;
        if (pla.getGameProfile().getName().length() < 2) return true;

        // UUID with dashes: char[14] is version digit
        String uuidDashes = pla.getGameProfile().getId().toString();
        if (uuidDashes.length() >= 15) {
            char c14 = uuidDashes.charAt(14);
            if (c14 != '4' && c14 != '1') return true;
        }

        // Early ticks: red-named entries are boss bars / injected NPCs
        if (overlayTicks < 80) {
            String dn = pla.getDisplayName() != null ? pla.getDisplayName().getFormattedText() : "";
            if (dn.startsWith("\u00a7c")) return true;
        }

        if (!showYourself) {
            String selfUUID = Minecraft.getMinecraft().thePlayer.getGameProfile().getId().toString();
            if (uuidDashes.equals(selfUUID)) return true;
        }

        // In-game: display name must contain a space (team prefix like "R PlayerName")
        if (status == 3) {
            String dn = pla.getDisplayName() != null ? pla.getDisplayName().getFormattedText() : "";
            if (!ColorUtil.strip(dn).contains(" ")) return true;
        }

        return false;
    }

    // ==========================================================================
    // Column width calculation (mirrors original doColumns)
    // ==========================================================================

    void doColumns(boolean updateEnabled) {
        int currentX = startX + 5;

        for (ColumnDef col : columns) {
            int longest = OverlayRenderer.getFontWidth(col.getHeader());
            String key  = col.getKey();

            if (!col.isEnabled()) continue;

            synchronized (currentPlayers) {
                for (String uuid : currentPlayers) {
                    Map<String, Object> pd = overlayPlayers.get(uuid);
                    if (pd == null) continue;

                    String value;
                    if (key.equals(TAGS_KEY)) {
                        StringBuilder sb = new StringBuilder();
                        for (String tag : tags) {
                            Object t = pd.get(tag);
                            if (t != null) sb.append(t.toString());
                        }
                        value = sb.toString();
                    } else {
                        Object obj = pd.get(key);
                        if (obj == null) continue;
                        value = obj.toString();
                    }

                    int w = OverlayRenderer.getFontWidth(value);
                    if (w > longest) longest = w;
                }
            }

            col.setMaxwidth(longest);
            col.setPosition(currentX);
            currentX += longest + 5;
        }

        int lineHeight = OverlayRenderer.getFontHeight() + offsetY;
        endX = currentX;
        endY = startY + lineHeight + (currentPlayers.size() * lineHeight)
                + (currentPlayers.size() > 0 ? 6 : 1);
    }

    // ==========================================================================
    // Overlay data helpers
    // ==========================================================================

    boolean isInOverlay(String uuid) { return overlayPlayers.containsKey(uuid); }

    void addToOverlay(String uuid, Map<String, Object> newData) {
        try {
            Map<String, Object> existing = overlayPlayers.get(uuid);
            if (existing == null) return;
            existing.putAll(newData);
            overlayPlayers.put(uuid, existing);
            doColumns(false);
            sortOverlay();
        } catch (Exception e) {
            print(PREFIX + "\u00a7eError detected. Please check \u00a73latest.log\u00a7e.");
        }
    }

    void addToPlayers(String uuid) {
        synchronized (currentPlayers) {
            boolean isNicked = uuid.charAt(12) != '4';
            if (ascending) {
                currentPlayers.add(isNicked ? 0 : currentPlayers.size(), uuid);
            } else {
                currentPlayers.add(isNicked ? currentPlayers.size() : 0, uuid);
            }
            doColumns(false);
        }
    }

    void addPlaceholderStats(String uuid, String username, boolean doName) {
        Map<String, Object> ph = new ConcurrentHashMap<>();
        for (ColumnDef col : columns) {
            if (!col.isEnabled()) continue;
            String key = col.getKey();
            if (key.equals(ENCOUNTERS_KEY)) {
                ph.put(key, ColorUtil.getSeenColor(1));
            } else if (key.equals(PLAYER_KEY)) {
                if (doName) ph.put(key, "\u00a77" + username);
            } else {
                ph.put(key, "\u00a77-");
            }
        }
        if (doName) overlayPlayers.put(uuid, ph);
        else        addToOverlay(uuid, ph);
    }

    void sortOverlay() {
        synchronized (currentPlayers) {
            currentPlayers.sort((u1, u2) -> {
                Map<String, Object> s1 = overlayPlayers.get(u1);
                Map<String, Object> s2 = overlayPlayers.get(u2);
                boolean n1 = s1 != null && Boolean.TRUE.equals(s1.get("nicked"));
                boolean n2 = s2 != null && Boolean.TRUE.equals(s2.get("nicked"));

                if (!sortBy.equals(JOIN_VALUE)) {
                    if (n1 && !n2) return ascending ? -1 :  1;
                    if (!n1 && n2) return ascending ?  1 : -1;
                }

                String v1 = (s1 != null && s1.get(sortBy) != null) ? s1.get(sortBy).toString() : "-";
                String v2 = (s2 != null && s2.get(sortBy) != null) ? s2.get(sortBy).toString() : "-";
                v1 = ColorUtil.strip(v1); v2 = ColorUtil.strip(v2);

                boolean num1 = containsDigit(v1), num2 = containsDigit(v2);
                if (!num1 && !num2) return 0;
                if (!num1) return ascending ?  1 : -1;
                if (!num2) return ascending ? -1 :  1;

                try {
                    double d1 = Double.parseDouble(v1), d2 = Double.parseDouble(v2);
                    return ascending ? Double.compare(d2, d1) : Double.compare(d1, d2);
                } catch (NumberFormatException e) {
                    return ascending ? -1 : 1;
                }
            });
        }
    }

    private boolean containsDigit(String s) {
        for (char c : s.toCharArray()) if (Character.isDigit(c)) return true;
        return false;
    }

    void clearMaps() {
        teams.clear();
        overlayPlayers.clear();
        urchinCache.clear();
        synchronized (currentPlayers) { currentPlayers.clear(); }
    }

    // ==========================================================================
    // Rendering (mirrors original onRenderTick)
    // ==========================================================================

    public void onRender() {
        Minecraft mc = Minecraft.getMinecraft();
        boolean tabHeld = mc.gameSettings != null && mc.gameSettings.keyBindPlayerList.isKeyDown();
        if (!visible && !tabHeld) return;
        if (mc.thePlayer == null) return;
        if (mc.currentScreen != null && !(mc.currentScreen instanceof GuiChat)) return;
        if (overlayTicks < 5 || columns.isEmpty()) return;

        OverlayRenderer.drawRect(startX, startY, endX, endY, background);

        // Border: 4 lines forming a rectangle
        OverlayRenderer.drawLine2D(startX, startY, endX, startY, borderWidth, borderColorRGB);
        OverlayRenderer.drawLine2D(endX, startY, endX, endY, borderWidth, borderColorRGB);
        OverlayRenderer.drawLine2D(endX, endY, startX, startY + (endY - startY), borderWidth, borderColorRGB);
        OverlayRenderer.drawLine2D(startX, startY + (endY - startY), startX, startY, borderWidth, borderColorRGB);

        // Column headers
        for (ColumnDef col : columns) {
            if (!col.isEnabled()) continue;
            int x = col.getPosition();
            if (!col.getKey().equals(PLAYER_KEY)) {
                x += (col.getMaxwidth() - OverlayRenderer.getFontWidth(col.getHeader())) / 2;
            }
            OverlayRenderer.drawString(col.getHeader(), x, startY + offsetY, columnTitles, true);
        }

        int lineHeight = OverlayRenderer.getFontHeight() + offsetY;
        int y = startY + lineHeight + 5;

        synchronized (currentPlayers) {
            for (String uuid : currentPlayers) {
                Map<String, Object> ps = overlayPlayers.get(uuid);
                if (ps == null) { overlayPlayers.remove(uuid); continue; }

                boolean isNicked = Boolean.TRUE.equals(ps.get("nicked"));
                boolean isError  = Boolean.TRUE.equals(ps.get("error"));

                for (ColumnDef col : columns) {
                    if (!col.isEnabled()) continue;
                    String key = col.getKey();
                    int    maxWidth = col.getMaxwidth();
                    Object statValue = ps.get(key);
                    String stringVal = String.valueOf(statValue);
                    int    x = col.getPosition();

                    if (isNicked) {
                        if (!key.equals(PLAYER_KEY) && !key.equals(ENCOUNTERS_KEY)) {
                            statValue = "\u00a77-";
                        } else if (!teams.containsKey(uuid) && key.equals(PLAYER_KEY)) {
                            statValue = "\u00a7e" + stringVal.replaceAll("\u00a7.", "");
                        }
                    } else if (isError && (statValue == null || stringVal.isEmpty())) {
                        statValue = "\u00a74E";
                    }

                    switch (key) {
                        case PLAYER_KEY:
                            if (isNicked && !teams.containsKey(uuid)) {
                                statValue = "\u00a7e" + stringVal.replaceAll("\u00a7.", "");
                            }
                            if (isError && (statValue == null || stringVal.isEmpty() || stringVal.equals("\u00a77-"))) {
                                statValue = "\u00a74E";
                            }
                            if (statValue == null || stringVal.isEmpty()) {
                                overlayPlayers.remove(uuid); continue;
                            }
                            break;
                        case TAGS_KEY:
                            if (stringVal.isEmpty()) {
                                StringBuilder sb = new StringBuilder();
                                for (String tag : tags) {
                                    if (!ps.containsKey(tag)) continue;
                                    String realTag = String.valueOf(ps.get(tag));
                                    if (!realTag.startsWith("\u00a7")) continue;
                                    sb.append(realTag);
                                }
                                statValue = sb.length() > 0 ? sb.toString() : (isNicked ? "\u00a77-" : null);
                            }
                            break;
                        case ENCOUNTERS_KEY:
                            if (statValue == null || stringVal.isEmpty()) statValue = "\u00a7a1";
                            break;
                    }

                    String text = statValue != null ? statValue.toString() : "";
                    int    tw   = OverlayRenderer.getFontWidth(text);
                    if (!key.equals(PLAYER_KEY)) x += (maxWidth - tw) / 2;
                    OverlayRenderer.drawString(text, x, y, -1, true);
                }
                y += lineHeight;
            }
        }
    }

    // ==========================================================================
    // Chat handling (mirrors original onChat)
    // ==========================================================================

    public boolean onChat(String message) {
        String msg = ColorUtil.strip(message);

        // Auto-trigger /who when someone joins (only needed for join-time sorting)
        if (sortBy.equals(JOIN_VALUE) && dowho
                && ((msg.endsWith("!") && msg.contains("has joined"))
                    || msg.startsWith("You will respawn in"))) {
            dowho = false;
            new Thread(() -> {
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                if (status > 1 && timeUntilStart() > 5) pendingCommands.add("/who");
            }).start();
            return true;
        }

        if (msg.startsWith("ONLINE: ")) {
            String[] names = msg.replace("ONLINE: ", "").split(", ");

            // Update join-order values for players already in the overlay
            Map<String, Integer> joinOrder = new ConcurrentHashMap<>();
            int order = names.length - 1;
            for (String n : names) joinOrder.put(n.trim(), order--);

            for (String uuid : overlayPlayers.keySet()) {
                Map<String, Object> op = overlayPlayers.get(uuid);
                Object u = op.get(PLAYER_KEY);
                if (!(u instanceof String)) continue;
                String plain = ColorUtil.strip((String) u);
                if (!joinOrder.containsKey(plain)) continue;
                Map<String, Object> tmp = new ConcurrentHashMap<>();
                tmp.put(JOIN_VALUE, joinOrder.get(plain));
                addToOverlay(uuid, tmp);
            }

            // Add players that are not yet in the overlay
            Minecraft mc = Minecraft.getMinecraft();
            Map<String, NetworkPlayerInfo> tabMap = new HashMap<>();
            if (mc.getNetHandler() != null) {
                for (NetworkPlayerInfo npi : mc.getNetHandler().getPlayerInfoMap()) {
                    tabMap.put(npi.getGameProfile().getName().toLowerCase(), npi);
                }
            }

            for (String rawName : names) {
                String name = rawName.trim();
                if (name.isEmpty()) continue;

                // Skip if already tracked by username
                boolean alreadyIn = false;
                for (Map<String, Object> op : overlayPlayers.values()) {
                    Object u = op.get(PLAYER_KEY);
                    if (u instanceof String && ColorUtil.strip((String) u).equalsIgnoreCase(name)) {
                        alreadyIn = true; break;
                    }
                }
                if (alreadyIn) continue;

                NetworkPlayerInfo npi = tabMap.get(name.toLowerCase());
                if (npi != null) {
                    // Player is in the current tab list — add directly
                    String uuid = npi.getGameProfile().getId().toString().replace("-", "");
                    if (isInOverlay(uuid) || ignoredPlayers.containsKey(name.toLowerCase())) continue;
                    String displayName = npi.getDisplayName() != null
                            ? npi.getDisplayName().getFormattedText()
                            : npi.getGameProfile().getName();
                    final String fu = uuid, fn = displayName, fl = currentLobby;
                    addPlaceholderStats(fu, fn, true);
                    addToPlayers(fu);
                    new Thread(() -> handlePlayerStats(fu, fl)).start();
                    new Thread(() -> handleUrchinTag(fu, fl)).start();
                } else {
                    // Not in tab list — resolve UUID via Mojang API
                    final String playerName = name, lobby = currentLobby;
                    new Thread(() -> {
                        String[] conv = convertPlayer(playerName);
                        String uuid = conv[0], username = conv[1];
                        if (uuid == null || uuid.isEmpty()) {
                            conv = convertPlayerPlayerdb(playerName);
                            uuid = conv[0]; username = conv[1];
                        }
                        if (uuid == null || uuid.isEmpty()) return;
                        final String fu = uuid, fn = username.isEmpty() ? playerName : username;
                        if (isInOverlay(fu) || ignoredPlayers.containsKey(playerName.toLowerCase())) return;
                        synchronized (currentPlayers) {
                            addPlaceholderStats(fu, fn, true);
                            addToPlayers(fu);
                        }
                        handlePlayerStats(fu, lobby);
                        handleUrchinTag(fu, lobby);
                    }).start();
                }
            }

            if (!didwho) { didwho = true; }
            return false; // suppress ONLINE: line from chat
        }

        return true;
    }

    // ==========================================================================
    // World change
    // ==========================================================================

    public void onWorldChange() {
        dowho = true;
        didwho = false;
        overlayTicks = 0;
        clearMaps();
    }

    // ==========================================================================
    // Stats fetching (async)
    // ==========================================================================

    private void handlePlayerStats(String uuid, String lobby) {
        Map<String, Object> cached = statsCache.get(uuid);
        if (cached != null) {
            long cacheTime = cached.containsKey("cachetime") ? (long)(Object)cached.get("cachetime") : 0L;
            if (System.currentTimeMillis() < cacheTime) {
                if (isInOverlay(uuid) && currentLobby.equals(lobby)) addToOverlay(uuid, cached);
                return;
            }
            statsCache.remove(uuid);
        }

        Map<String, Object> playerStats = new ConcurrentHashMap<>();
        try {
            String url = "https://api.hypixel.net/v2/player?key=" + hypixelKey() + "&uuid=" + uuid;
            Object[] res = HttpUtil.get(url, 3000);
            if ((int) res[1] == 200) {
                playerStats = parseStats((JsonWrapper) res[0], uuid);
            } else {
                printFromThread(PREFIX + "\u00a7eHTTP Error \u00a73" + res[1] + " \u00a7ewhile getting stats.");
                playerStats.put("error", true);
            }
        } catch (Exception e) {
            printFromThread(PREFIX + "\u00a7eRuntime error while getting stats.");
            playerStats.put("error", true);
        }

        if (isInOverlay(uuid) && currentLobby.equals(lobby)) addToOverlay(uuid, playerStats);
    }

    private Map<String, Object> parseStats(JsonWrapper jsonData, String uuid) {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        try {
            JsonWrapper data = jsonData.object();

            // Null player → nicked
            if (jsonData.string().contains("\"player\":null") || !data.object("player").exists()) {
                stats.put("nicked", true);
                return stats;
            }

            JsonWrapper playerObject = data.object("player");
            String username = playerObject.get("displayname", "");
            stats.put("username", username);

            boolean showRanks = LazifyConfig.INSTANCE.isShowRanks();
            String rank = showRanks ? ColorUtil.getFormattedRank(jsonData) : ColorUtil.getRankColor(ColorUtil.getRank(jsonData));
            String coloredUsername = rank.length() == 2 ? rank + username : rank + " " + username;
            if (status <= 2 && !teams.containsKey(uuid)) {
                stats.put(PLAYER_KEY, coloredUsername);
            }

            boolean hasBW = playerObject.object("stats").exists()
                    && playerObject.object("stats").object("Bedwars").exists();

            String language = playerObject.get("userLanguage", "ENGLISH");
            if (!language.equals("ENGLISH")) stats.put("language", "\u00a73L");

            JsonWrapper bw = hasBW
                    ? playerObject.object("stats").object("Bedwars")
                    : JsonWrapper.parse("{}");

            int expInt = (int) Double.parseDouble(bw.get("Experience", "0"));
            int star   = (int) Math.floor(expToStars(expInt));
            stats.put(STAR_KEY,   ColorUtil.getPrestigeColor(star));
            stats.put(STAR_VALUE, (double) star);

            double finalKills  = Double.parseDouble(bw.get("final_kills_bedwars",  "0"));
            double finalDeaths = Double.parseDouble(bw.get("final_deaths_bedwars", "0"));
            if (finalDeaths == 0) stats.put("nofinaldeaths", "\u00a75Z");

            double fkdr;
            if (finalDeaths == 0) {
                fkdr = finalKills;
            } else {
                fkdr = finalKills / finalDeaths < 10
                        ? ColorUtil.round(finalKills / finalDeaths, 2)
                        : ColorUtil.round(finalKills / finalDeaths, 1);
            }
            double index = star * Math.pow(fkdr, 2);
            stats.put(FKDR_KEY,   ColorUtil.getFkdrColor(ColorUtil.formatDoubleStr(fkdr)));
            stats.put(FKDR_VALUE, fkdr);
            stats.put(INDEX_VALUE, index);

            long lastLogin  = Long.parseLong(playerObject.get("lastLogin",  "0"));
            long lastLogout = Long.parseLong(playerObject.get("lastLogout", "0"));
            boolean statusOn = lastLogin != 0;
            String coloredSession = "\u00a7cAPI";
            if (statusOn) {
                if (lastLogin - lastLogout > -10000) {
                    long nowMs = System.currentTimeMillis();
                    String sessionStr = ColorUtil.calculateRelativeTimestamp(lastLogin, nowMs);
                    coloredSession = ColorUtil.getSessionColor(lastLogin, nowMs, sessionStr);
                } else {
                    coloredSession = "\u00a7cOFFLINE";
                }
            }
            stats.put(SESSION_KEY,   coloredSession);
            stats.put(SESSION_VALUE, lastLogin * -1.0);

            String wsPrefix  = parseWinstreakMode(LazifyConfig.INSTANCE.getWinstreakMode());
            int    winstreak = Integer.parseInt(bw.get(wsPrefix + "winstreak", "0"));
            boolean highWS   = winstreak > 50;
            stats.put(WINSTREAK_KEY,   ColorUtil.getWinstreakColor(String.valueOf(winstreak)));
            stats.put(WINSTREAK_VALUE, (double) winstreak);
            stats.put(TAGS_KEY, "");

            long CACHE = highWS ? 600000L
                    : Math.max(300, Math.min(86400, 60 * (60 * ((int) finalDeaths / 120)))) * 1000L;
            stats.put("cachetime", System.currentTimeMillis() + CACHE);
            statsCache.put(uuid, stats);

        } catch (Exception e) {
            LazifyMod.LOGGER.warn("parseStats error for {}: {}", uuid, e.getMessage());
            stats.put("error", true);
        }
        return stats;
    }

    private static double expToStars(int exp) {
        // Exact port of original expToStars: one prestige = 487000 exp (100 stars)
        int levelBase = (exp / 487000) * 100;
        int expMod    = exp % 487000;
        int[][] levels = {
            {7000, 4, 5000},
            {3500, 3, 3500},
            {1500, 2, 2000},
            {500,  1, 1000},
            {0,    0,  500}
        };
        for (int[] lvl : levels) {
            if (expMod < lvl[0]) continue;
            return levelBase + lvl[1] + ((double)(expMod - lvl[0]) / lvl[2]);
        }
        return 0;
    }

    private static String parseWinstreakMode(int i) {
        switch (i) {
            case 1: return "eight_one_";
            case 2: return "eight_two_";
            case 3: return "four_three_";
            case 4: return "four_four_";
            case 5: return "two_four_";
            default: return "";
        }
    }

    // ==========================================================================
    // Urchin tag fetching (async)
    // ==========================================================================

    private void handleUrchinTag(String uuid, String lobby) {
        if (urchinCache.containsKey(uuid)) return;
        if (urchinKey() == null || urchinKey().isEmpty()) return;

        try {
            String url = "https://urchin.ws/player/" + uuid + "?key=" + urchinKey() + "&sources=GAME";
            Object[] res = HttpUtil.get(url, 3000);

            if ((int) res[1] == 200) {
                JsonWrapper json = (JsonWrapper) res[0];
                List<JsonWrapper> tagsArray = json.object().array("tags");

                if (tagsArray != null && !tagsArray.isEmpty()) {
                    JsonWrapper firstTag = tagsArray.get(0);
                    String tagType = firstTag.object().get("type", "");
                    String reason  = firstTag.object().get("reason", "");

                    if (!tagType.isEmpty()) {
                        Map<String, Object> op = overlayPlayers.get(uuid);
                        String username = op != null ? (String) op.getOrDefault("username", uuid) : uuid;

                        String coloredTag = ColorUtil.getUrchinTagColor(tagType);
                        urchinCache.put(uuid, tagType);

                        Map<String, Object> tagData = new ConcurrentHashMap<>();
                        tagData.put(URCHIN_KEY, coloredTag);
                        if (isInOverlay(uuid) && currentLobby.equals(lobby)) addToOverlay(uuid, tagData);

                        // addEnemy not available in vanilla Forge; skipped

                        if (LazifyConfig.INSTANCE.isSendUrchinReasonToChat() && !reason.isEmpty()) {
                            printFromThread(PREFIX + "\u00a7c" + username
                                    + " \u00a7eis tagged as \u00a73" + tagType + " \u00a7efor: \u00a73" + reason);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    // ==========================================================================
    // Player UUID conversion (for /ov sc)
    // ==========================================================================

    private String[] convertPlayer(String player) {
        boolean isUUID = (player.length() == 32 && player.charAt(12) == '4')
                      || (player.length() == 36 && player.charAt(14) == '4');
        String url = isUUID
                ? "https://sessionserver.mojang.com/session/minecraft/profile/" + player
                : "https://api.mojang.com/users/profiles/minecraft/" + player;
        try {
            Object[] res = HttpUtil.get(url, 3000);
            if ((int) res[1] == 200) {
                JsonWrapper j = (JsonWrapper) res[0];
                return new String[]{ j.get("id", ""), j.get("name", "") };
            }
        } catch (Exception e) {
            print(PREFIX + "\u00a7eRuntime error while getting uuid.");
        }
        return new String[]{ "", "" };
    }

    private String[] convertPlayerPlayerdb(String player) {
        String url = "https://playerdb.co/api/player/minecraft/" + player;
        try {
            Object[] res = HttpUtil.get(url, 3000);
            if ((int) res[1] == 200) {
                JsonWrapper thing = ((JsonWrapper) res[0]).object().object("data").object("player");
                return new String[]{ thing.get("raw_id", ""), thing.get("username", "") };
            }
        } catch (Exception e) {
            print(PREFIX + "\u00a7eRuntime error while getting uuid.");
        }
        return new String[]{ "", "" };
    }

    // ==========================================================================
    // /ov command handling
    // ==========================================================================

    // All setting names (for tab complete)
    public static final String[] ALL_SETTINGS = {
        "teams","teamprefix","showyourself","showranks",
        "sendnicked","sendurchinreason","addtaggedtoenemy","keybindhold","keybind",
        "col","sortby","sortmode","winstreak","enctimeout",
        "x","y","bgopacity","bghue","headerhue","borderhue"
    };
    public static final String[] ALL_COLUMNS = {
        "encounters","username","star","fkdr","winstreaks","urchin","session"
    };

    public void handleCommand(String[] args) {
        if (args.length == 0) { printHelp(); return; }
        if (args.length == 1 && args[0].equals("2")) { printStatus(); return; }

        String cmd = args[0].toLowerCase();

        // Backward compat: /ov set <name> [val] → /ov <name> [val]
        if (cmd.equals("set")) {
            if (args.length == 1) { printStatus(); return; }
            String[] shifted = new String[args.length - 1];
            System.arraycopy(args, 1, shifted, 0, shifted.length);
            handleCommand(shifted);
            return;
        }

        switch (cmd) {
            case "sc":
                if (args.length < 2) { print(PREFIX + "\u00a7eUsage: \u00a73/ov sc <username>"); return; }
                final String scPlayer = args[1];
                new Thread(() -> {
                    String[] conv = convertPlayer(scPlayer);
                    String uuid = conv[0], username = conv[1];
                    if (uuid == null || uuid.isEmpty()) {
                        String[] conv2 = convertPlayerPlayerdb(scPlayer);
                        uuid = conv2[0]; username = conv2[1];
                        if (uuid == null || uuid.isEmpty()) {
                            printFromThread(PREFIX + "\u00a7eFailed to find \u00a73" + scPlayer); return;
                        }
                    }
                    final String fu = uuid, fn = username, fl = currentLobby;
                    synchronized (currentPlayers) {
                        overlayPlayers.remove(fu); currentPlayers.remove(fu);
                        addPlaceholderStats(fu, fn, true); addToPlayers(fu);
                        Map<String, Object> m = new ConcurrentHashMap<>(); m.put("manual", true);
                        addToOverlay(fu, m);
                        statsCache.remove(fu); urchinCache.remove(fu);
                        new Thread(() -> handlePlayerStats(fu, fl)).start();
                        new Thread(() -> handleUrchinTag(fu, fl)).start();
                        printFromThread(PREFIX + "\u00a7eAdded \u00a73" + fn + "\u00a7e to overlay.");
                    }
                }).start();
                return;

            case "hide":
                if (args.length < 2) { print(PREFIX + "\u00a7eUsage: \u00a73/ov hide <username>"); return; }
                ignoredPlayers.put(args[1].toLowerCase(), "");
                print(PREFIX + "\u00a73" + args[1] + "\u00a7e is now hidden.");
                return;

            case "clearhidden":
                print(PREFIX + "\u00a7eCleared \u00a73" + ignoredPlayers.size() + "\u00a7e hidden player" + (ignoredPlayers.size() != 1 ? "s." : "."));
                ignoredPlayers.clear();
                return;

            case "reload":
                List<String> rPlayers = new ArrayList<>(overlayPlayers.keySet());
                clearMaps();
                for (String uuid : rPlayers) {
                    addPlaceholderStats(uuid, "\u00a77-", true); addToPlayers(uuid);
                    final String fl = currentLobby;
                    new Thread(() -> handlePlayerStats(uuid, fl)).start();
                    new Thread(() -> handleUrchinTag(uuid, fl)).start();
                }
                overlayTicks = 5;
                print(PREFIX + "\u00a7eReloaded \u00a73" + rPlayers.size() + "\u00a7e player" + (rPlayers.size() != 1 ? "s." : "."));
                return;

            case "clear":
                int cnt = overlayPlayers.size();
                clearMaps(); overlayTicks = 5;
                print(PREFIX + "\u00a7eCleared \u00a73" + cnt + "\u00a7e player" + (cnt != 1 ? "s." : "."));
                return;

            case "key":
                if (args.length < 3) { print(PREFIX + "\u00a7eUsage: \u00a73/ov key <hypixel|urchin> <key>"); return; }
                String which = args[1].toLowerCase();
                if (which.equals("hypixel")) {
                    LazifyConfig.INSTANCE.setHypixelKey(args[2]); LazifyConfig.INSTANCE.save();
                    print(PREFIX + "\u00a7eHypixel API key saved.");
                } else if (which.equals("urchin")) {
                    LazifyConfig.INSTANCE.setUrchinKey(args[2]); LazifyConfig.INSTANCE.save();
                    print(PREFIX + "\u00a7eUrchin API key saved.");
                } else {
                    print(PREFIX + "\u00a7eUnknown key type: \u00a73" + args[1] + "\u00a7e. Use \u00a73hypixel\u00a7e or \u00a73urchin\u00a7e.");
                }
                return;
        }

        // All remaining tokens are settings
        applySetting(cmd, args);
    }

    /** Apply a setting. args[0] = name, args[1] = value (optional for booleans / col). */
    private void applySetting(String name, String[] args) {
        LazifyConfig cfg = LazifyConfig.INSTANCE;
        try {
            switch (name) {
                // ── Booleans (toggle when no value given) ─────────────────────
                case "teams":
                    cfg.setTeams(args.length > 1 ? parseBool(args[1]) : !cfg.isTeams()); break;
                case "teamprefix":
                    cfg.setTeamPrefix(args.length > 1 ? parseBool(args[1]) : !cfg.isTeamPrefix()); break;
                case "showyourself":
                    cfg.setShowYourself(args.length > 1 ? parseBool(args[1]) : !cfg.isShowYourself()); break;
                case "showranks":
                    cfg.setShowRanks(args.length > 1 ? parseBool(args[1]) : !cfg.isShowRanks()); break;
                case "sendnicked":
                    cfg.setSendNickedToChat(args.length > 1 ? parseBool(args[1]) : !cfg.isSendNickedToChat()); break;
                case "sendurchinreason":
                    cfg.setSendUrchinReasonToChat(args.length > 1 ? parseBool(args[1]) : !cfg.isSendUrchinReasonToChat()); break;
                case "addtaggedtoenemy":
                    cfg.setAddTaggedToEnemy(args.length > 1 ? parseBool(args[1]) : !cfg.isAddTaggedToEnemy()); break;
                case "keybindhold":
                    cfg.setKeybindHold(args.length > 1 ? parseBool(args[1]) : !cfg.isKeybindHold()); break;

                // ── Integers (show hint when no value given) ───────────────────
                case "sortby":
                    if (args.length < 2) { printSortByHelp(); return; }
                    cfg.setSortByIndex(clamp(Integer.parseInt(args[1]), 0, 5)); break;
                case "sortmode":
                    if (args.length < 2) { print(PREFIX + "\u00a7esortmode: \u00a73" + cfg.getSortMode() + " \u00a7e(0=asc highest-first, 1=desc lowest-first)"); return; }
                    cfg.setSortMode(clamp(Integer.parseInt(args[1]), 0, 1)); break;
                case "winstreak":
                    if (args.length < 2) { printWinstreakHelp(); return; }
                    cfg.setWinstreakMode(clamp(Integer.parseInt(args[1]), 0, 5)); break;
                case "enctimeout":
                    if (args.length < 2) { print(PREFIX + "\u00a7eenctimeout: \u00a73" + cfg.getEncountersTimeoutMins() + " \u00a7emins (1-1440)"); return; }
                    cfg.setEncountersTimeoutMins(clamp(Integer.parseInt(args[1]), 1, 1440)); break;
                case "x":
                    if (args.length < 2) { print(PREFIX + "\u00a7ex: \u00a73" + cfg.getOverlayX()); return; }
                    cfg.setOverlayX(Math.max(0, Integer.parseInt(args[1]))); break;
                case "y":
                    if (args.length < 2) { print(PREFIX + "\u00a7ey: \u00a73" + cfg.getOverlayY()); return; }
                    cfg.setOverlayY(Math.max(0, Integer.parseInt(args[1]))); break;
                case "bgopacity":
                    if (args.length < 2) { print(PREFIX + "\u00a7ebgopacity: \u00a73" + cfg.getBgOpacity() + " \u00a7e(0-255)"); return; }
                    cfg.setBgOpacity(clamp(Integer.parseInt(args[1]), 0, 255)); break;
                case "bghue":
                    if (args.length < 2) { print(PREFIX + "\u00a7ebghue: \u00a73" + cfg.getBgHue() + " \u00a7e(0=black, 360=chroma)"); return; }
                    cfg.setBgHue(clamp(Integer.parseInt(args[1]), 0, 360)); break;
                case "headerhue":
                    if (args.length < 2) { print(PREFIX + "\u00a7eheaderhue: \u00a73" + cfg.getHeaderHue() + " \u00a7e(0-360)"); return; }
                    cfg.setHeaderHue(clamp(Integer.parseInt(args[1]), 0, 360)); break;
                case "borderhue":
                    if (args.length < 2) { print(PREFIX + "\u00a7eborderhue: \u00a73" + cfg.getBorderHue() + " \u00a7e(0-360)"); return; }
                    cfg.setBorderHue(clamp(Integer.parseInt(args[1]), 0, 360)); break;

                // ── Column visibility ──────────────────────────────────────────
                case "keybind": {
                    if (args.length < 2) {
                        int cur = cfg.getKeybind();
                        print(PREFIX + "\u00a7ekeybind: \u00a73" + cur + " \u00a7e(" + Keyboard.getKeyName(cur) + "). Use /ov keybind <KEY_NAME or code>"); return;
                    }
                    int code;
                    try {
                        code = Integer.parseInt(args[1]);
                    } catch (NumberFormatException ex) {
                        code = Keyboard.getKeyIndex(args[1].toUpperCase());
                    }
                    if (code == Keyboard.KEY_NONE) {
                        print(PREFIX + "\u00a7cUnknown key: \u00a73" + args[1]); return;
                    }
                    cfg.setKeybind(code); cfg.save();
                    LazifyMod.setKeyCode(LazifyMod.OVERLAY_KEY, code);
                    print(PREFIX + "\u00a7ekeybind \u00a72\u2192\u00a7e " + Keyboard.getKeyName(code) + " (" + code + ")"); return;
                }

                case "col":
                    if (args.length < 2) { printColStatus(); return; }
                    String colName = args[1].toLowerCase();
                    boolean curVal;
                    switch (colName) {
                        case "encounters": curVal = cfg.isColEncounters(); break;
                        case "username":   curVal = cfg.isColUsername();   break;
                        case "star":       curVal = cfg.isColStar();       break;
                        case "fkdr":       curVal = cfg.isColFkdr();       break;
                        case "winstreaks": curVal = cfg.isColWinstreaks(); break;
                        case "urchin":     curVal = cfg.isColUrchin();     break;
                        case "session":    curVal = cfg.isColSession();    break;
                        default: print(PREFIX + "\u00a7eUnknown column: \u00a73" + args[1]
                            + "\u00a7e. Options: encounters username star fkdr winstreaks urchin session"); return;
                    }
                    boolean newVal = args.length > 2 ? parseBool(args[2]) : !curVal;
                    switch (colName) {
                        case "encounters": cfg.setColEncounters(newVal); break;
                        case "username":   cfg.setColUsername(newVal);   break;
                        case "star":       cfg.setColStar(newVal);       break;
                        case "fkdr":       cfg.setColFkdr(newVal);       break;
                        case "winstreaks": cfg.setColWinstreaks(newVal); break;
                        case "urchin":     cfg.setColUrchin(newVal);     break;
                        case "session":    cfg.setColSession(newVal);    break;
                    }
                    cfg.save(); defaultSettings();
                    print(PREFIX + "\u00a7eColumn \u00a73" + colName + "\u00a7e \u2192 " + boolStr(newVal));
                    return;

                default:
                    print(PREFIX + "\u00a7eUnknown setting: \u00a73" + name + "\u00a7e. Run \u00a73/ov\u00a7e for all settings.");
                    return;
            }
            cfg.save();
            defaultSettings();
            print(PREFIX + "\u00a7e" + name + " \u00a72\u2192\u00a7e " + currentValStr(name));
        } catch (NumberFormatException e) {
            print(PREFIX + "\u00a7cExpected a number for \u00a73" + name + "\u00a7c, got: \u00a73" + (args.length > 1 ? args[1] : "?"));
        }
    }

    // ── /ov help/status display ────────────────────────────────────────────────

    private void printHelp() {
        print(PREFIX + "\u00a77\u2500\u2500\u2500 \u00a7dLazify \u00a77\u2500\u2500\u2500  \u00a77run \u00a73/ov 2\u00a77 for settings");
        print(PREFIX + "\u00a77sc \u00a7e<user>\u00a77 \u00a7e\u2013 add player to overlay");
        print(PREFIX + "\u00a77hide \u00a7e<user>\u00a77 \u00a7e\u2013 hide player from overlay");
        print(PREFIX + "\u00a77clearhidden\u00a77 \u00a7e\u2013 show all hidden players again");
        print(PREFIX + "\u00a77reload\u00a77 \u00a7e\u2013 re-fetch stats for everyone");
        print(PREFIX + "\u00a77clear\u00a77 \u00a7e\u2013 remove all players from overlay");
        print(PREFIX + "\u00a77key \u00a7e<hypixel|urchin> <key>\u00a77 \u00a7e\u2013 set API key");
    }

    private void printStatus() {
        LazifyConfig c = LazifyConfig.INSTANCE;
        print(PREFIX + "\u00a77\u2500\u2500\u2500 \u00a7dLazify settings \u00a77\u2500\u2500\u2500  \u00a77/ov <setting> [value]  |  /ov for commands");
        print(PREFIX
            + "\u00a77keybind \u00a7e" + c.getKeybind() + "\u00a77(" + Keyboard.getKeyName(c.getKeybind()) + ")  "
            + "\u00a77keybindhold \u00a7" + (c.isKeybindHold() ? "a" : "c") + c.isKeybindHold());
        print(PREFIX
            + "\u00a77teams \u00a7" + (c.isTeams() ? "a" : "c") + c.isTeams() + "  "
            + "\u00a77teamprefix \u00a7" + (c.isTeamPrefix() ? "a" : "c") + c.isTeamPrefix() + "  "
            + "\u00a77showyourself \u00a7" + (c.isShowYourself() ? "a" : "c") + c.isShowYourself() + "  "
            + "\u00a77showranks \u00a7" + (c.isShowRanks() ? "a" : "c") + c.isShowRanks());
        print(PREFIX
            + "\u00a77sendnicked \u00a7" + (c.isSendNickedToChat() ? "a" : "c") + c.isSendNickedToChat() + "  "
            + "\u00a77sendurchinreason \u00a7" + (c.isSendUrchinReasonToChat() ? "a" : "c") + c.isSendUrchinReasonToChat() + "  "
            + "\u00a77addtaggedtoenemy \u00a7" + (c.isAddTaggedToEnemy() ? "a" : "c") + c.isAddTaggedToEnemy() + "  "
            + "\u00a77keybindhold \u00a7" + (c.isKeybindHold() ? "a" : "c") + c.isKeybindHold());
        print(PREFIX
            + "\u00a77sortby \u00a7e" + c.getSortByIndex() + "\u00a77(" + sortByName(c.getSortByIndex()) + ")  "
            + "\u00a77sortmode \u00a7e" + c.getSortMode() + "\u00a77(" + (c.getSortMode() == 0 ? "asc" : "desc") + ")  "
            + "\u00a77winstreak \u00a7e" + c.getWinstreakMode() + "\u00a77(" + winstreakName(c.getWinstreakMode()) + ")  "
            + "\u00a77enctimeout \u00a7e" + c.getEncountersTimeoutMins() + "\u00a77m");
        print(PREFIX
            + "\u00a77x \u00a7e" + c.getOverlayX() + "  "
            + "\u00a77y \u00a7e" + c.getOverlayY() + "  "
            + "\u00a77bgopacity \u00a7e" + c.getBgOpacity() + "  "
            + "\u00a77bghue \u00a7e" + c.getBgHue() + "  "
            + "\u00a77headerhue \u00a7e" + c.getHeaderHue() + "  "
            + "\u00a77borderhue \u00a7e" + c.getBorderHue());
        print(PREFIX
            + "\u00a77col encounters \u00a7" + (c.isColEncounters() ? "a" : "c") + c.isColEncounters() + "  "
            + "\u00a77username \u00a7" + (c.isColUsername() ? "a" : "c") + c.isColUsername() + "  "
            + "\u00a77star \u00a7" + (c.isColStar() ? "a" : "c") + c.isColStar() + "  "
            + "\u00a77fkdr \u00a7" + (c.isColFkdr() ? "a" : "c") + c.isColFkdr() + "  "
            + "\u00a77winstreaks \u00a7" + (c.isColWinstreaks() ? "a" : "c") + c.isColWinstreaks() + "  "
            + "\u00a77urchin \u00a7" + (c.isColUrchin() ? "a" : "c") + c.isColUrchin() + "  "
            + "\u00a77session \u00a7" + (c.isColSession() ? "a" : "c") + c.isColSession());
        print(PREFIX
            + "\u00a77hypixel key: " + (c.getHypixelKey().isEmpty() ? "\u00a7cnot set" : "\u00a7aset") + "  "
            + "\u00a77urchin key: "  + (c.getUrchinKey().isEmpty()  ? "\u00a7cnot set" : "\u00a7aset") + "  "
            + "\u00a77overlay: " + (visible ? "\u00a7avisible" : "\u00a7chidden"));
    }

    private void printColStatus() {
        LazifyConfig c = LazifyConfig.INSTANCE;
        print(PREFIX + "\u00a77Columns \u00a77(use /ov col <name> to toggle):");
        print(PREFIX
            + "  encounters " + boolStr(c.isColEncounters()) + "  username " + boolStr(c.isColUsername())
            + "  star " + boolStr(c.isColStar()) + "  fkdr " + boolStr(c.isColFkdr())
            + "  winstreaks " + boolStr(c.isColWinstreaks()) + "  urchin " + boolStr(c.isColUrchin())
            + "  session " + boolStr(c.isColSession()));
    }

    private void printSortByHelp() {
        LazifyConfig c = LazifyConfig.INSTANCE;
        print(PREFIX + "\u00a7esortby: \u00a73" + c.getSortByIndex()
            + " \u00a7e(" + sortByName(c.getSortByIndex()) + "). Options: 0=Encounters 1=Star 2=FKDR 3=Index 4=Winstreak 5=JoinTime");
    }

    private void printWinstreakHelp() {
        LazifyConfig c = LazifyConfig.INSTANCE;
        print(PREFIX + "\u00a7ewinstreak: \u00a73" + c.getWinstreakMode()
            + " \u00a7e(" + winstreakName(c.getWinstreakMode()) + "). Options: 0=Overall 1=Solos 2=Doubles 3=Threes 4=Fours 5=4v4");
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private static boolean parseBool(String s) {
        return s.equalsIgnoreCase("true") || s.equals("1") || s.equalsIgnoreCase("on");
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static String boolStr(boolean b) {
        return b ? "\u00a7atrue" : "\u00a7cfalse";
    }

    private static String sortByName(int i) {
        switch (i) { case 0: return "Encounters"; case 1: return "Star"; case 2: return "FKDR";
                     case 3: return "Index"; case 4: return "Winstreak"; case 5: return "JoinTime"; default: return "?"; }
    }

    private static String winstreakName(int i) {
        switch (i) { case 1: return "Solos"; case 2: return "Doubles"; case 3: return "Threes";
                     case 4: return "Fours"; case 5: return "4v4"; default: return "Overall"; }
    }

    private String currentValStr(String name) {
        LazifyConfig c = LazifyConfig.INSTANCE;
        switch (name) {
            case "teams":            return boolStr(c.isTeams());
            case "teamprefix":       return boolStr(c.isTeamPrefix());
            case "showyourself":     return boolStr(c.isShowYourself());
            case "showranks":        return boolStr(c.isShowRanks());
            case "sendnicked":       return boolStr(c.isSendNickedToChat());
            case "sendurchinreason": return boolStr(c.isSendUrchinReasonToChat());
            case "addtaggedtoenemy": return boolStr(c.isAddTaggedToEnemy());
            case "keybindhold":      return boolStr(c.isKeybindHold());
            case "keybind":          return "\u00a7e" + c.getKeybind() + "\u00a7e (" + Keyboard.getKeyName(c.getKeybind()) + ")";
            case "sortby":     return "\u00a7e" + c.getSortByIndex() + "\u00a7e (" + sortByName(c.getSortByIndex()) + ")";
            case "sortmode":   return "\u00a7e" + c.getSortMode() + "\u00a7e (" + (c.getSortMode() == 0 ? "asc" : "desc") + ")";
            case "winstreak":  return "\u00a7e" + c.getWinstreakMode() + "\u00a7e (" + winstreakName(c.getWinstreakMode()) + ")";
            case "enctimeout": return "\u00a7e" + c.getEncountersTimeoutMins() + "\u00a7em";
            case "x":          return "\u00a7e" + c.getOverlayX();
            case "y":          return "\u00a7e" + c.getOverlayY();
            case "bgopacity":  return "\u00a7e" + c.getBgOpacity();
            case "bghue":      return "\u00a7e" + c.getBgHue();
            case "headerhue":  return "\u00a7e" + c.getHeaderHue();
            case "borderhue":  return "\u00a7e" + c.getBorderHue();
            default:           return "?";
        }
    }

    // ==========================================================================
    // Chat / misc helpers
    // ==========================================================================

    private void print(String msg) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;
        mc.thePlayer.addChatMessage(new ChatComponentText(ColorUtil.colorize(msg)));
    }

    private void printFromThread(String msg) { pendingMessages.add(msg); }

    private void flushPendingMessages() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;
        String msg; int n = 0;
        while ((msg = pendingMessages.poll()) != null && n++ < 5) {
            mc.thePlayer.addChatMessage(new ChatComponentText(ColorUtil.colorize(msg)));
        }
        String cmd;
        while ((cmd = pendingCommands.poll()) != null) {
            mc.thePlayer.sendChatMessage(cmd);
        }
    }

    private int timeUntilStart() {
        List<String> sb = getSidebarLines();
        if (sb == null || sb.size() < 7) return -1;
        String line = ColorUtil.strip(sb.get(6));
        if (line.equals("Waiting...")) return 20;
        if (!line.startsWith("Starting in ")) return -1;
        String[] parts = line.split(" ");
        String last = parts[parts.length - 1];
        if (!last.endsWith("s")) return -1;
        try { return Integer.parseInt(last.substring(0, last.length() - 1)); }
        catch (NumberFormatException e) { return -1; }
    }
}
