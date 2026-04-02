package com.lazify.overlay;

import com.lazify.LazifyMod;
import com.lazify.api.ApiCredentials;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    static final String PREGAME_KEEP_KEY = "pregamekeep";

    // ── API keys (read from config) ────────────────────────────────────────────
    private String urchinKey()  { return LazifyConfig.INSTANCE.getUrchinKey(); }

    // ── API headers ─────────────────────────────────────────────────────────────
    private static final Map<String, String> API_HEADERS;
    static {
        Map<String, String> h = new HashMap<>();
        h.put("X-API-Key", ApiCredentials.getKey());
        API_HEADERS = h;
    }

    // UUID → raw username mapping (for API calls that take username)
    private final Map<String, String> uuidToName = new ConcurrentHashMap<>();

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

    // ── Debug ──────────────────────────────────────────────────────────────────
    boolean debugScoreboard = false;
    private int debugSbCooldown = 0;
    boolean debugTablist = false;
    private int debugTabCooldown = 0;

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
    boolean inBwPregame  = false;

    // ── Overlay layout ─────────────────────────────────────────────────────────
    int   startX = 500, startY = 12, offsetY = 3;
    int   endX = 0, endY = 0;
    float borderWidth = 2.5f;
    int   background, borderColorRGB, columnTitles;

    static final String PREFIX = "\u00a77[\u00a7dL\u00a77]\u00a7r ";

    private static final Pattern CHAT_SENDER  = Pattern.compile("^(?:\\[[\\w+]+\\] )?(\\w+) ?: .+");
    private static final Pattern LOBBY_JOIN   = Pattern.compile("^(\\w+) has joined \\(\\d+/\\d+\\)!$");
    private static final Pattern PREGAME_LIST = Pattern.compile("^\\+ \\(\\d+/\\d+\\) (\\w+)$");

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
        tags.add("apinicked");

        defaultSettings();
        print(PREFIX + "\u00a7eWelcome to \u00a73Lazify\u00a7e! Please run \u00a73/ov\u00a7e for commands.");
        if (urchinKey().isEmpty())
            print(PREFIX + "\u00a7eNo Urchin API key set. Use \u00a73/ov key urchin <key>\u00a7e to enable cheater tags.");
        if (ApiCredentials.getUrl().isEmpty())
            print(PREFIX + "\u00a7cStats API not configured. Overlay will not fetch player stats.");
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

        // Debug scoreboard dump (throttled to every 100 ticks / 5 seconds)
        if (debugScoreboard && LazifyConfig.INSTANCE.isDebug()) {
            if (debugSbCooldown <= 0) {
                debugSbCooldown = 100;
                dumpScoreboard();
            } else {
                debugSbCooldown--;
            }
        }

        // Debug tab list dump (throttled to every 100 ticks / 5 seconds)
        if (debugTablist && LazifyConfig.INSTANCE.isDebug()) {
            if (debugTabCooldown <= 0) {
                debugTabCooldown = 100;
                dumpTablist();
            } else {
                debugTabCooldown--;
            }
        }

        doColumns(true);

        if (status < 1 && !inBwPregame) return;
        if (!LazifyConfig.INSTANCE.isAutoTablist()) return;

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
            if (isInOverlay(uuid)) {
                if (showTeamColors && status == 3) {
                    String teamDisplay = getTeamDisplayFromTab(pla, displayName);
                    if (teamDisplay != null) {
                        String prev = teams.put(uuid, teamDisplay);
                        if (!teamDisplay.equals(prev)) {
                            // Prepend rank prefix if showRanks is on
                            String finalDisplay = teamDisplay;
                            if (LazifyConfig.INSTANCE.isShowRanks()) {
                                Map<String, Object> existing = overlayPlayers.get(uuid);
                                if (existing != null) {
                                    String rp = (String) existing.get("rankPrefix");
                                    if (rp != null && !rp.isEmpty() && !rp.equals("\u00a77")) {
                                        finalDisplay = rp + " " + teamDisplay;
                                    }
                                }
                            }
                            Map<String, Object> teamData = new HashMap<>();
                            teamData.put(PLAYER_KEY, finalDisplay);
                            addToOverlay(uuid, teamData);
                        }
                    }
                }
                if (isBot(pla)) continue;
                continue;
            }

            if (isBot(pla)) continue;

            // ── Track encounters ──────────────────────────────────────────────
            // UUID v4: char[14] of UUID-with-dashes is the version digit
            String encKey = isV4DashedUuid(uuidWithDashes) ? uuid : username;
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
            if (isNickedKey(uuid)) {
                debug("Nick detected: " + username + " uuid=" + uuid);
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

            debug("Adding player from tab: " + username + " uuid=" + uuid);
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
                if (Boolean.TRUE.equals(entry.getValue().get(PREGAME_KEEP_KEY))) continue;
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
                    boolean isNicked = isNickedKey(uuid);
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
        int oldStatus = status;
        status = getBedwarsStatus();
        if (status != oldStatus) {
            debug("Status changed: " + oldStatus + " -> " + status + " | lobby=" + currentLobby + " inBwPregame=" + inBwPregame);
        }
        if (!lastLobby.equals(currentLobby)) {
            debug("Lobby changed: " + lastLobby + " -> " + currentLobby);
            clearMaps();
        }
    }

    private int getBedwarsStatus() {
        String title = getSidebarTitle();
        List<String> sidebar = getSidebarLines();

        if (title == null || sidebar == null) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.theWorld != null) {
                String dim = mc.theWorld.provider.getDimensionName();
                if ("The End".equals(dim)) return 0;
            }
            return -1;
        }

        // The objective display name is "BED WARS" — score lines do NOT contain the title
        if (!ColorUtil.strip(title).startsWith("BED WARS")) return -1;
        if (sidebar.isEmpty()) return -1;

        // Extract server/lobby ID from the last line: "03/31/26  m85CG" or "03/31/26  L29H"
        String lastLine = ColorUtil.strip(sidebar.get(sidebar.size() - 1)).trim();
        String[] dateParts = lastLine.split("  ");
        if (dateParts.length >= 2) {
            String lobbyId = dateParts[dateParts.length - 1].trim();
            if (!lobbyId.isEmpty()) {
                currentLobby = lobbyId;
                // Lobby IDs starting with 'L' = BW lobby (not in a game yet)
                if (lobbyId.charAt(0) == 'L') return 1;
            }
        }

        // Check all lines for status indicators
        for (String line : sidebar) {
            String stripped = ColorUtil.strip(line).trim();
            if (stripped.equals("Waiting...") || stripped.startsWith("Starting in")) return 2;
            // Team status lines like "R Red: ✓" or "B Blue: ✗" indicate in-game
            if (stripped.length() >= 2 && stripped.charAt(1) == ' '
                    && (stripped.contains("Red:") || stripped.contains("Blue:")
                     || stripped.contains("Green:") || stripped.contains("Yellow:")
                     || stripped.contains("Aqua:") || stripped.contains("White:")
                     || stripped.contains("Pink:") || stripped.contains("Gray:"))) {
                return 3;
            }
        }

        return -1;
    }

    /** Returns sidebar title (objective display name), or null if no sidebar. */
    private String getSidebarTitle() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) return null;
        Scoreboard sb = mc.theWorld.getScoreboard();
        ScoreObjective obj = sb.getObjectiveInDisplaySlot(1);
        if (obj == null) return null;
        return obj.getDisplayName();
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

    private void dumpScoreboard() {
        String title = getSidebarTitle();
        List<String> sidebar = getSidebarLines();
        if (sidebar == null || title == null) {
            debug("Scoreboard: \u00a7cnull \u00a77(no sidebar objective)");
            return;
        }
        debug("Scoreboard: \u00a7a" + sidebar.size() + " lines \u00a77| title=\u00a7f" + title + " \u00a78-> \u00a7e" + ColorUtil.strip(title)
            + " \u00a77| status=\u00a7e" + status + " \u00a77| lobby=\u00a7e" + currentLobby + " \u00a77| inBwPregame=\u00a7e" + inBwPregame);
        for (int i = 0; i < sidebar.size(); i++) {
            String raw = sidebar.get(i);
            String stripped = ColorUtil.strip(raw);
            debug("  [" + i + "] \u00a7f" + raw + " \u00a78-> \u00a77" + stripped);
        }
    }

    private void dumpTablist() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getNetHandler() == null) {
            debug("Tablist: \u00a7cnull \u00a77(no net handler)");
            return;
        }

        List<NetworkPlayerInfo> tab = new ArrayList<>(mc.getNetHandler().getPlayerInfoMap());
        debug("Tablist: \u00a7a" + tab.size() + " entries \u00a77| status=\u00a7e" + status + " \u00a77| lobby=\u00a7e" + currentLobby
            + " \u00a77| inBwPregame=\u00a7e" + inBwPregame + " \u00a77| showYourself=\u00a7e" + showYourself);

        for (int i = 0; i < tab.size(); i++) {
            NetworkPlayerInfo npi = tab.get(i);
            String raw = npi.getDisplayName() != null
                    ? npi.getDisplayName().getFormattedText()
                    : npi.getGameProfile().getName();
            String stripped = ColorUtil.strip(raw);
            String name = npi.getGameProfile().getName();
            String uuid = npi.getGameProfile().getId() != null ? npi.getGameProfile().getId().toString() : "null";
            int ping = npi.getResponseTime();
            boolean bot = isBot(npi);
            ScorePlayerTeam sbTeam = mc.theWorld != null ? mc.theWorld.getScoreboard().getPlayersTeam(name) : null;
            String teamName = sbTeam != null ? sbTeam.getRegisteredName() : "none";

            debug("  [" + i + "] \u00a7f" + raw + " \u00a78-> \u00a77" + stripped
                + " \u00a77| name=\u00a7e" + name
                + " \u00a77| ping=\u00a7e" + ping
                + " \u00a77| bot=\u00a7e" + bot
                + " \u00a77| team=\u00a7e" + teamName
                + " \u00a77| uuid=\u00a7e" + uuid);
        }
    }

    private String getTeamDisplayFromTab(NetworkPlayerInfo pla, String fallbackDisplayName) {
        String raw = fallbackDisplayName != null ? fallbackDisplayName : pla.getGameProfile().getName();
        String stripped = ColorUtil.strip(raw);
        String baseName = pla.getGameProfile().getName();
        if (!stripped.equalsIgnoreCase(baseName)) {
            String[] parts = stripped.split(" ");
            if (parts.length >= 2) {
                String suffixName = parts[parts.length - 1];
                if (suffixName.equalsIgnoreCase(baseName)) {
                    return showTeamPrefix ? raw : suffixName;
                }
            }
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) return null;
        ScorePlayerTeam team = mc.theWorld.getScoreboard().getPlayersTeam(baseName);
        if (team == null) return null;

        if (showTeamPrefix) {
            String formatted = ScorePlayerTeam.formatPlayerName(team, baseName);
            String formattedStripped = ColorUtil.strip(formatted);
            if (!formattedStripped.equalsIgnoreCase(baseName)) {
                return formatted;
            }
            String marker = getTeamMarker(team.getRegisteredName());
            if (!marker.isEmpty()) {
                return team.getColorPrefix() + marker + " " + baseName + team.getColorSuffix();
            }
            return team.getColorPrefix() + baseName + team.getColorSuffix();
        }

        return team.getColorPrefix() + baseName + team.getColorSuffix();
    }

    private String getTeamMarker(String teamName) {
        if (teamName == null) return "";
        String n = teamName.toLowerCase(Locale.ROOT);
        if (n.contains("red")) return "R";
        if (n.contains("blue")) return "B";
        if (n.contains("green")) return "G";
        if (n.contains("yellow")) return "Y";
        if (n.contains("aqua")) return "A";
        if (n.contains("white")) return "W";
        if (n.contains("pink")) return "P";
        if (n.contains("gray") || n.contains("grey")) return "G";
        return "";
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
        if (uuidDashes.length() < 15) return true;
        char c14 = uuidDashes.charAt(14);
        if (c14 != '4' && c14 != '1') return true;

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
            if (!ColorUtil.strip(dn).contains(" ")) {
                Minecraft mc = Minecraft.getMinecraft();
                ScorePlayerTeam team = mc.theWorld != null ? mc.theWorld.getScoreboard().getPlayersTeam(pla.getGameProfile().getName()) : null;
                if (team == null) return true;
            }
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
            boolean isNicked = isNickedKey(uuid);
            if (ascending) {
                currentPlayers.add(isNicked ? 0 : currentPlayers.size(), uuid);
            } else {
                currentPlayers.add(isNicked ? currentPlayers.size() : 0, uuid);
            }
            doColumns(false);
        }
    }

    void addPlaceholderStats(String uuid, String username, boolean doName) {
        String raw = ColorUtil.strip(username);
        if (!raw.isEmpty() && !raw.equals("-")) uuidToName.put(uuid, raw);
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
        boolean tabHeld = LazifyConfig.INSTANCE.isShowOnTab()
                && mc.gameSettings != null && mc.gameSettings.keyBindPlayerList.isKeyDown();
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
                            if (key.equals(URCHIN_KEY) && ps.get(URCHIN_KEY) != null) {
                                statValue = ps.get(URCHIN_KEY);
                            } else {
                                statValue = "\u00a77-";
                            }
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

        // Add players who join the pre-game lobby (no status check — pattern is specific enough)
        {
            Matcher joinMatcher = LOBBY_JOIN.matcher(msg);
            if (joinMatcher.matches()) {
                inBwPregame = true;
                debug("Lobby join detected: " + joinMatcher.group(1) + " | lobby=" + currentLobby + " status=" + status);
                addChatPlayer(joinMatcher.group(1), currentLobby);
            }
        }

        // Alternate pregame format: "+ (01/16) PlayerName" (used in some game modes)
        {
            Matcher listMatcher = PREGAME_LIST.matcher(msg);
            if (listMatcher.matches()) {
                inBwPregame = true;
                debug("Pregame list entry: " + listMatcher.group(1) + " | lobby=" + currentLobby + " status=" + status);
                addChatPlayer(listMatcher.group(1), currentLobby);
            }
        }

        // Detect game start via "Protect your bed" message
        if (msg.contains("Protect your bed and destroy the enemy beds")) {
            inBwPregame = true;
            debug("Game start detected (Protect your bed) | lobby=" + currentLobby + " status=" + status);
        }

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
            if (LazifyConfig.INSTANCE.isClearOnWho()) {
                debug("clearOnWho: clearing overlay before processing /who");
                clearMaps();
                overlayTicks = 5;
            }
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
                    Map<String, Object> keepData = new HashMap<>();
                    keepData.put(PREGAME_KEEP_KEY, true);
                    addToOverlay(fu, keepData);
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
                            Map<String, Object> keepData = new HashMap<>();
                            keepData.put(PREGAME_KEEP_KEY, true);
                            addToOverlay(fu, keepData);
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

        // Remove players from overlay on final kill
        if ((inBwPregame || status >= 2) && LazifyConfig.INSTANCE.isRemoveFinalKill()) {
            if (msg.endsWith("FINAL KILL!")) {
                String victim = msg.split(" ")[0];
                if (!victim.isEmpty()) {
                    debug("Final kill detected: victim=" + victim + " | inBwPregame=" + inBwPregame + " status=" + status);
                    removePlayerByName(victim);
                }
            }
        }

        // Auto-add players who chat during pre-game lobby
        if (inBwPregame || status >= 1) {
            Matcher m = CHAT_SENDER.matcher(msg);
            if (m.matches()) {
                String sender = m.group(1);
                debug("Chat sender detected: " + sender + " | inBwPregame=" + inBwPregame + " status=" + status);
                addChatPlayer(sender, currentLobby);
            }
        }

        return true;
    }

    private void addChatPlayer(String name, String lobby) {
        boolean fromPregame = inBwPregame || status == 2;
        addChatPlayer(name, lobby, true, fromPregame);
    }

    private void addChatPlayer(String name, String lobby, boolean allowApiFallback) {
        addChatPlayer(name, lobby, allowApiFallback, false);
    }

    private void addChatPlayer(String name, String lobby, boolean allowApiFallback, boolean markPregameKeep) {
        for (Map<String, Object> op : overlayPlayers.values()) {
            Object u = op.get(PLAYER_KEY);
            if (u instanceof String && ColorUtil.strip((String) u).equalsIgnoreCase(name)) {
                debug("addChatPlayer: " + name + " already in overlay, skipping");
                return;
            }
        }
        if (ignoredPlayers.containsKey(name.toLowerCase())) {
            debug("addChatPlayer: " + name + " is ignored, skipping");
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        NetworkPlayerInfo npi = findTabPlayer(name);
        if (npi != null) {
            String uuid = npi.getGameProfile().getId().toString().replace("-", "");
            if (isInOverlay(uuid)) return;
            String displayName = npi.getDisplayName() != null
                    ? npi.getDisplayName().getFormattedText() : npi.getGameProfile().getName();
            debug("addChatPlayer: " + name + " found in tab list, uuid=" + uuid);
            final String fu = uuid, fn = displayName, fl = lobby;
            addPlaceholderStats(fu, fn, true);
            if (markPregameKeep) {
                Map<String, Object> keepData = new HashMap<>();
                keepData.put(PREGAME_KEEP_KEY, true);
                addToOverlay(fu, keepData);
            }
            addToPlayers(fu);
            new Thread(() -> handlePlayerStats(fu, fl)).start();
            new Thread(() -> handleUrchinTag(fu, fl)).start();
        } else {
            if (!allowApiFallback) {
                debug("addChatPlayer: " + name + " not in tab list, skipping API fallback");
                return;
            }
            debug("addChatPlayer: " + name + " not in tab list, resolving UUID async...");
            final String playerName = name, fl = lobby;
            new Thread(() -> {
                // Wait for the tab list to populate before giving up on it
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                if (ignoredPlayers.containsKey(playerName.toLowerCase())) return;

                // Retry tab list lookup after the delay
                NetworkPlayerInfo npi2 = findTabPlayer(playerName);
                if (npi2 != null) {
                    String uuid = npi2.getGameProfile().getId().toString().replace("-", "");
                    if (isInOverlay(uuid) || ignoredPlayers.containsKey(playerName.toLowerCase())) return;
                    String displayName = npi2.getDisplayName() != null
                            ? npi2.getDisplayName().getFormattedText() : npi2.getGameProfile().getName();
                    final String fu = uuid, fn = displayName;
                    synchronized (currentPlayers) {
                        addPlaceholderStats(fu, fn, true);
                        if (markPregameKeep) {
                            Map<String, Object> keepData = new HashMap<>();
                            keepData.put(PREGAME_KEEP_KEY, true);
                            addToOverlay(fu, keepData);
                        }
                        addToPlayers(fu);
                    }
                    handlePlayerStats(fu, fl);
                    handleUrchinTag(fu, fl);
                    return;
                }

                // Not in tab list — resolve UUID via Mojang API
                String[] conv = convertPlayer(playerName);
                String uuid = conv[0], username = conv[1];
                if (uuid == null || uuid.isEmpty()) { conv = convertPlayerPlayerdb(playerName); uuid = conv[0]; username = conv[1]; }
                if (uuid == null || uuid.isEmpty()) {
                    // Unresolvable name — add as nicked with N tag
                    final String fu = "nick_" + playerName.toLowerCase(), fn = playerName;
                    if (isInOverlay(fu)) return;
                    debugFromThread("addChatPlayer: " + playerName + " unresolvable, adding as nicked");
                    synchronized (currentPlayers) {
                        addPlaceholderStats(fu, fn, true);
                        Map<String, Object> nickData = new ConcurrentHashMap<>();
                        nickData.put("nicked", true);
                        nickData.put("apinicked", "\u00a7eN");
                        nickData.put(URCHIN_KEY, "\u00a7bN");
                        if (markPregameKeep) nickData.put(PREGAME_KEEP_KEY, true);
                        addToOverlay(fu, nickData);
                        addToPlayers(fu);
                    }
                    return;
                }
                final String fu = uuid, fn = username.isEmpty() ? playerName : username;
                if (isInOverlay(fu) || ignoredPlayers.containsKey(playerName.toLowerCase())) return;
                synchronized (currentPlayers) {
                    addPlaceholderStats(fu, fn, true);
                    if (markPregameKeep) {
                        Map<String, Object> keepData = new HashMap<>();
                        keepData.put(PREGAME_KEEP_KEY, true);
                        addToOverlay(fu, keepData);
                    }
                    addToPlayers(fu);
                }
                handlePlayerStats(fu, fl);
                handleUrchinTag(fu, fl);
            }).start();
        }
    }

    private NetworkPlayerInfo findTabPlayer(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getNetHandler() == null) return null;
        for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
            if (info.getGameProfile().getName().equalsIgnoreCase(name)) return info;
        }
        return null;
    }

    private void removePlayerByName(String name) {
        synchronized (currentPlayers) {
            String targetUuid = null;
            for (Map.Entry<String, Map<String, Object>> entry : overlayPlayers.entrySet()) {
                if (matchesPlayerName(entry.getValue(), name)) {
                    targetUuid = entry.getKey();
                    break;
                }
            }
            if (targetUuid != null) {
                debug("Removing final-killed player: " + name + " uuid=" + targetUuid);
                overlayPlayers.remove(targetUuid);
                currentPlayers.remove(targetUuid);
            } else {
                debug("Final kill: player " + name + " not found in overlay");
            }
        }
    }

    private boolean matchesPlayerName(Map<String, Object> playerData, String name) {
        Object usernameObj = playerData.get("username");
        if (usernameObj instanceof String && ((String) usernameObj).equalsIgnoreCase(name)) {
            return true;
        }

        Object playerObj = playerData.get(PLAYER_KEY);
        if (!(playerObj instanceof String)) {
            return false;
        }

        String plain = ColorUtil.strip((String) playerObj).trim();
        if (plain.equalsIgnoreCase(name)) {
            return true;
        }

        int spaceIdx = plain.lastIndexOf(' ');
        if (spaceIdx >= 0 && spaceIdx + 1 < plain.length()) {
            String trailing = plain.substring(spaceIdx + 1);
            return trailing.equalsIgnoreCase(name);
        }

        return false;
    }

    // ==========================================================================
    // World change
    // ==========================================================================

    public void onWorldChange() {
        debug("World change: clearing overlay, resetting state | was lobby=" + currentLobby + " status=" + status);
        dowho = true;
        didwho = false;
        inBwPregame = false;
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
                debugFromThread("Stats cache hit for " + uuid);
                if (isInOverlay(uuid) && currentLobby.equals(lobby)) addToOverlay(uuid, cached);
                return;
            }
            debugFromThread("Stats cache expired for " + uuid);
            statsCache.remove(uuid);
        }

        String username = uuidToName.get(uuid);
        if (username == null || username.isEmpty()) {
            debugFromThread("No username mapped for " + uuid + ", skipping stats fetch");
            return;
        }

        Map<String, Object> playerStats = new ConcurrentHashMap<>();
        try {
            String baseUrl = ApiCredentials.getUrl();
            if (baseUrl.isEmpty()) {
                debugFromThread("API URL not configured");
                playerStats.put("error", true);
                if (isInOverlay(uuid) && currentLobby.equals(lobby)) addToOverlay(uuid, playerStats);
                return;
            }
            String url = baseUrl + "/v1/player/" + username;
            debugFromThread("Fetching stats for " + username + " (" + uuid + ")");
            Object[] res = HttpUtil.get(url, 3000, API_HEADERS);
            int code = (int) res[1];
            debugFromThread("Stats API response: HTTP " + code + " for " + username);
            if (code == 200) {
                playerStats = parseStats((JsonWrapper) res[0], uuid);
            } else if (code == 404) {
                playerStats.put("nicked", true);
                playerStats.put("apinicked", "\u00a7eN");
                playerStats.put(URCHIN_KEY, "\u00a7bN");
            } else {
                printFromThread(PREFIX + "\u00a7eHTTP Error \u00a73" + code + " \u00a7ewhile getting stats.");
                playerStats.put("error", true);
            }
        } catch (Exception e) {
            debugFromThread("Stats API exception for " + username + ": " + e.getMessage());
            printFromThread(PREFIX + "\u00a7eRuntime error while getting stats.");
            playerStats.put("error", true);
        }

        if (isInOverlay(uuid) && currentLobby.equals(lobby)) addToOverlay(uuid, playerStats);
    }

    private Map<String, Object> parseStats(JsonWrapper jsonData, String uuid) {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        try {
            JsonWrapper data = jsonData.object();

            // No bedwars data → nicked or never played
            if (!data.object("bedwars").exists()) {
                stats.put("nicked", true);
                stats.put("apinicked", "\u00a7eN");
                stats.put(URCHIN_KEY, "\u00a7bN");
                return stats;
            }

            JsonWrapper network = data.object("network");
            JsonWrapper bw      = data.object("bedwars");
            JsonWrapper overall  = bw.object("overall");

            String username = data.get("username", "");
            if (username.isEmpty()) username = uuidToName.getOrDefault(uuid, "");
            stats.put("username", username);
            if (!username.isEmpty()) uuidToName.put(uuid, username);

            // Rank
            String rankStr = network.exists() ? network.get("rank", "") : "";
            boolean showRanks = LazifyConfig.INSTANCE.isShowRanks();
            String rankPrefix = showRanks ? ColorUtil.getFormattedRankFromStr(rankStr) : "";
            String rankColor  = ColorUtil.getRankColor(rankStr);
            stats.put("rankPrefix", rankPrefix);
            stats.put("rankColor",  rankColor);

            if (teams.containsKey(uuid)) {
                // Team display already set; prepend rank if enabled
                String existing = (String) overlayPlayers.getOrDefault(uuid, Collections.<String, Object>emptyMap()).get(PLAYER_KEY);
                if (existing != null && showRanks && !rankPrefix.equals("\u00a77") && !rankPrefix.isEmpty()) {
                    stats.put(PLAYER_KEY, rankPrefix + " " + existing);
                }
            } else {
                String coloredUsername = showRanks && !rankPrefix.equals("\u00a77") && !rankPrefix.isEmpty()
                        ? rankPrefix + " " + rankColor + username
                        : rankColor + username;
                stats.put(PLAYER_KEY, coloredUsername);
            }

            // Language
            String language = network.exists() ? network.get("language", "ENGLISH") : "ENGLISH";
            if (!language.equals("ENGLISH")) stats.put("language", "\u00a73L");

            // Star
            int star = (int) Double.parseDouble(overall.get("stars", overall.get("level", "0")));
            stats.put(STAR_KEY,   ColorUtil.getPrestigeColor(star));
            stats.put(STAR_VALUE, (double) star);

            // FKDR
            double fkdr = Double.parseDouble(overall.get("fkdr", "0"));
            double finalKills  = Double.parseDouble(overall.get("final_kills", "0"));
            double finalDeaths = Double.parseDouble(overall.get("final_deaths", "0"));
            if (finalDeaths == 0 && fkdr == 0 && finalKills == 0) stats.put("nofinaldeaths", "\u00a75Z");
            if (fkdr == 0 && finalDeaths > 0) fkdr = finalKills / finalDeaths;
            fkdr = fkdr < 10 ? ColorUtil.round(fkdr, 2) : ColorUtil.round(fkdr, 1);
            double index = star * Math.pow(fkdr, 2);
            stats.put(FKDR_KEY,   ColorUtil.getFkdrColor(ColorUtil.formatDoubleStr(fkdr)));
            stats.put(FKDR_VALUE, fkdr);
            stats.put(INDEX_VALUE, index);

            // Session
            long lastLogin  = Long.parseLong(network.exists() ? network.get("last_login",  network.get("lastLogin",  "0")) : "0");
            long lastLogout = Long.parseLong(network.exists() ? network.get("last_logout", network.get("lastLogout", "0")) : "0");
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

            // Winstreak
            String wsMode = parseWinstreakMode(LazifyConfig.INSTANCE.getWinstreakMode());
            JsonWrapper wsSource = wsMode.isEmpty() ? overall : bw.object(wsMode);
            if (!wsSource.exists()) wsSource = overall;
            int winstreak = (int) Double.parseDouble(wsSource.get("winstreak", "0"));
            boolean highWS = winstreak > 50;
            stats.put(WINSTREAK_KEY,   ColorUtil.getWinstreakColor(String.valueOf(winstreak)));
            stats.put(WINSTREAK_VALUE, (double) winstreak);
            stats.put(TAGS_KEY, "");

            // Cache
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

    private static String parseWinstreakMode(int i) {
        switch (i) {
            case 1: return "solos";
            case 2: return "doubles";
            case 3: return "threes";
            case 4: return "fours";
            case 5: return "4v4";
            default: return "";
        }
    }

    private boolean isNickedKey(String uuid) {
        return !isV4UndashedUuid(uuid);
    }

    private boolean isV4UndashedUuid(String uuid) {
        return uuid != null && uuid.length() == 32 && uuid.charAt(12) == '4';
    }

    private boolean isV4DashedUuid(String uuid) {
        return uuid != null && uuid.length() == 36 && uuid.charAt(14) == '4';
    }

    // ==========================================================================
    // Urchin tag fetching (async)
    // ==========================================================================

    private void handleUrchinTag(String uuid, String lobby) {
        if (urchinCache.containsKey(uuid)) {
            debugFromThread("Urchin cache hit for " + uuid);
            return;
        }
        if (urchinKey() == null || urchinKey().isEmpty()) {
            debugFromThread("Urchin skipped for " + uuid + " (no key set)");
            return;
        }

        try {
            String url = "https://urchin.ws/player/" + uuid + "?key=" + urchinKey() + "&sources=GAME";
            debugFromThread("Fetching Urchin tag for " + uuid);
            Object[] res = HttpUtil.get(url, 3000);
            debugFromThread("Urchin API response: HTTP " + (int) res[1] + " for " + uuid);

            if ((int) res[1] == 200) {
                JsonWrapper json = (JsonWrapper) res[0];
                List<JsonWrapper> tagsArray = json.object().array("tags");

                if (tagsArray != null && !tagsArray.isEmpty()) {
                    JsonWrapper firstTag = tagsArray.get(0);
                    String tagType = firstTag.object().get("type", "");
                    String reason  = firstTag.object().get("reason", "");

                    if (!tagType.isEmpty()) {
                        debugFromThread("Urchin tag found for " + uuid + ": type=" + tagType + " reason=" + reason);
                        String username = uuidToName.getOrDefault(uuid, uuid);

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
        debugFromThread("Mojang API lookup: " + player + " -> " + url);
        try {
            Object[] res = HttpUtil.get(url, 3000);
            debugFromThread("Mojang API response: HTTP " + (int) res[1] + " for " + player);
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
        debugFromThread("PlayerDB API lookup: " + player);
        try {
            Object[] res = HttpUtil.get(url, 3000);
            debugFromThread("PlayerDB API response: HTTP " + (int) res[1] + " for " + player);
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
        "teams","teamprefix","showyourself","showranks","removefinalkill","autotablist","clearonwho",
        "sendnicked","sendurchinreason","keybindhold","showontab","keybind",
        "debug","col","sortby","sortmode","winstreak","enctimeout",
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
                    }
                    if (uuid == null || uuid.isEmpty()) {
                        // Unresolvable name — add as nicked with N tag
                        final String fu = "nick_" + scPlayer.toLowerCase(), fn = scPlayer;
                        synchronized (currentPlayers) {
                            addPlaceholderStats(fu, fn, true); addToPlayers(fu);
                            Map<String, Object> m = new ConcurrentHashMap<>();
                            m.put("nicked", true);
                            m.put("apinicked", "\u00a7eN");
                            m.put(URCHIN_KEY, "\u00a7bN");
                            m.put("manual", true);
                            addToOverlay(fu, m);
                        }
                        printFromThread(PREFIX + "\u00a7eAdded \u00a73" + fn + "\u00a7e as nicked.");
                    } else {
                        final String fu = uuid, fn = username.isEmpty() ? scPlayer : username, fl = currentLobby;
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

            case "tags":
                print(PREFIX + "\u00a77\u2500\u2500\u2500 \u00a7dOverlay Tags \u00a77\u2500\u2500\u2500");
                print(PREFIX + "\u00a7eN \u00a77- Nicked (API returned no data)");
                print(PREFIX + "\u00a7eS \u00a77- Sniper");
                print(PREFIX + "\u00a7cC \u00a77- Confirmed Cheater");
                print(PREFIX + "\u00a74CC \u00a77- Closet Cheater");
                return;

            case "debugsb":
                if (!LazifyConfig.INSTANCE.isDebug()) {
                    print(PREFIX + "\u00a7cEnable debug mode first: \u00a73/ov debug");
                    return;
                }
                debugScoreboard = !debugScoreboard;
                debugSbCooldown = 0;
                print(PREFIX + "\u00a7eScoreboard debug " + (debugScoreboard ? "\u00a7aenabled \u00a7e(printing every 5s)" : "\u00a7cdisabled"));
                return;

            case "debugtab":
                if (!LazifyConfig.INSTANCE.isDebug()) {
                    print(PREFIX + "\u00a7cEnable debug mode first: \u00a73/ov debug");
                    return;
                }
                debugTablist = !debugTablist;
                debugTabCooldown = 0;
                print(PREFIX + "\u00a7eTablist debug " + (debugTablist ? "\u00a7aenabled \u00a7e(printing every 5s)" : "\u00a7cdisabled"));
                return;

            case "key":
                if (args.length < 3) { print(PREFIX + "\u00a7eUsage: \u00a73/ov key urchin <key>"); return; }
                String keyType = args[1].toLowerCase();
                if (keyType.equals("urchin")) {
                    LazifyConfig.INSTANCE.setUrchinKey(args[2]); LazifyConfig.INSTANCE.save();
                    print(PREFIX + "\u00a7eUrchin API key saved.");
                } else {
                    print(PREFIX + "\u00a7eUnknown key type: \u00a73" + args[1] + "\u00a7e. Use \u00a73urchin\u00a7e.");
                }
                return;
        }

        // All remaining tokens are settings
        applySetting(cmd, args);
    }

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
                case "removefinalkill":
                    cfg.setRemoveFinalKill(args.length > 1 ? parseBool(args[1]) : !cfg.isRemoveFinalKill()); break;
                case "autotablist":
                    cfg.setAutoTablist(args.length > 1 ? parseBool(args[1]) : !cfg.isAutoTablist()); break;
                case "clearonwho":
                    cfg.setClearOnWho(args.length > 1 ? parseBool(args[1]) : !cfg.isClearOnWho()); break;
                case "sendnicked":
                    cfg.setSendNickedToChat(args.length > 1 ? parseBool(args[1]) : !cfg.isSendNickedToChat()); break;
                case "sendurchinreason":
                    cfg.setSendUrchinReasonToChat(args.length > 1 ? parseBool(args[1]) : !cfg.isSendUrchinReasonToChat()); break;
                case "keybindhold":
                    cfg.setKeybindHold(args.length > 1 ? parseBool(args[1]) : !cfg.isKeybindHold()); break;
                case "showontab":
                    cfg.setShowOnTab(args.length > 1 ? parseBool(args[1]) : !cfg.isShowOnTab()); break;
                case "debug":
                    cfg.setDebug(args.length > 1 ? parseBool(args[1]) : !cfg.isDebug()); break;

                // ── Integers (show current value when no arg given) ───────────
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

                // ── Special: keybind ──────────────────────────────────────────
                case "keybind": {
                    if (args.length < 2) {
                        int cur = cfg.getKeybind();
                        print(PREFIX + "\u00a7ekeybind: \u00a73" + Keyboard.getKeyName(cur) + " \u00a7e(" + cur + ")"); return;
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
                    print(PREFIX + "\u00a7ekeybind \u00a72\u2192\u00a7e " + Keyboard.getKeyName(code) + " (" + code + ")"); return;
                }

                // ── Column visibility ──────────────────────────────────────────
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
                    print(PREFIX + "\u00a7eUnknown setting: \u00a73" + name + "\u00a7e. Run \u00a73/ov\u00a7e for help.");
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
        print(PREFIX + "\u00a77key \u00a7e<urchin> <key>\u00a77 \u00a7e\u2013 set API key");
        print(PREFIX + "\u00a77tags\u00a77 \u00a7e\u2013 show overlay tag definitions");
        if (LazifyConfig.INSTANCE.isDebug()) {
            print(PREFIX + "\u00a78debugsb\u00a77 \u00a7e\u2013 dump scoreboard data to chat");
            print(PREFIX + "\u00a78debugtab\u00a77 \u00a7e\u2013 dump tab list data to chat");
        }
    }

    private void printStatus() {
        LazifyConfig c = LazifyConfig.INSTANCE;
        print(PREFIX + "\u00a77\u2500\u2500\u2500 \u00a7dLazify settings \u00a77\u2500\u2500\u2500  \u00a77/ov <setting> [value]  |  /ov for commands");
        print(PREFIX
            + "\u00a77keybind \u00a7e" + Keyboard.getKeyName(c.getKeybind()) + "  "
            + "\u00a77keybindhold \u00a7" + (c.isKeybindHold() ? "a" : "c") + c.isKeybindHold() + "  "
            + "\u00a77showontab \u00a7" + (c.isShowOnTab() ? "a" : "c") + c.isShowOnTab());
        print(PREFIX
            + "\u00a77teams \u00a7" + (c.isTeams() ? "a" : "c") + c.isTeams() + "  "
            + "\u00a77teamprefix \u00a7" + (c.isTeamPrefix() ? "a" : "c") + c.isTeamPrefix() + "  "
            + "\u00a77showyourself \u00a7" + (c.isShowYourself() ? "a" : "c") + c.isShowYourself() + "  "
            + "\u00a77showranks \u00a7" + (c.isShowRanks() ? "a" : "c") + c.isShowRanks() + "  "
            + "\u00a77removefinalkill \u00a7" + (c.isRemoveFinalKill() ? "a" : "c") + c.isRemoveFinalKill());
        print(PREFIX
            + "\u00a77autotablist \u00a7" + (c.isAutoTablist() ? "a" : "c") + c.isAutoTablist() + "  "
            + "\u00a77clearonwho \u00a7" + (c.isClearOnWho() ? "a" : "c") + c.isClearOnWho() + "  "
            + "\u00a77sendnicked \u00a7" + (c.isSendNickedToChat() ? "a" : "c") + c.isSendNickedToChat() + "  "
            + "\u00a77sendurchinreason \u00a7" + (c.isSendUrchinReasonToChat() ? "a" : "c") + c.isSendUrchinReasonToChat() + "  "
            + "\u00a77debug \u00a7" + (c.isDebug() ? "a" : "c") + c.isDebug());
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
            case "removefinalkill":  return boolStr(c.isRemoveFinalKill());
            case "autotablist":      return boolStr(c.isAutoTablist());
            case "clearonwho":       return boolStr(c.isClearOnWho());
            case "sendnicked":       return boolStr(c.isSendNickedToChat());
            case "sendurchinreason": return boolStr(c.isSendUrchinReasonToChat());
            case "keybindhold":      return boolStr(c.isKeybindHold());
            case "showontab":        return boolStr(c.isShowOnTab());
            case "debug":            return boolStr(c.isDebug());
            case "keybind":          return "\u00a7e" + Keyboard.getKeyName(c.getKeybind()) + " (" + c.getKeybind() + ")";
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

    private void debug(String msg) {
        if (LazifyConfig.INSTANCE.isDebug()) print(PREFIX + "\u00a78[DEBUG] \u00a77" + msg);
    }

    private void debugFromThread(String msg) {
        if (LazifyConfig.INSTANCE.isDebug()) printFromThread(PREFIX + "\u00a78[DEBUG] \u00a77" + msg);
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
        if (sb == null) return -1;
        for (String rawLine : sb) {
            String line = ColorUtil.strip(rawLine).trim();
            if (line.equals("Waiting...")) return 20;
            if (line.startsWith("Starting in ")) {
                String[] parts = line.split(" ");
                String last = parts[parts.length - 1];
                if (!last.endsWith("s")) continue;
                try { return Integer.parseInt(last.substring(0, last.length() - 1)); }
                catch (NumberFormatException e) { continue; }
            }
        }
        return -1;
    }
}
