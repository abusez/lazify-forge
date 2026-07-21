package com.lazify.overlay;

import com.lazify.LazifyMod;
import com.lazify.api.StatsProvider;
import com.lazify.api.HttpUtil;
import com.lazify.api.JsonWrapper;
import com.lazify.config.LazifyConfig;
import com.lazify.util.BordicSuperstar;
import com.lazify.util.ColorUtil;
import com.lazify.util.TagInfo;
import com.lazify.util.KillMessageDetector;
import com.lazify.util.SkinDenick;
import com.lazify.util.StarChatDetector;
import com.lazify.util.WoodSkinUtil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
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
    static final String RANK_KEY      = "rank";
    static final String ENCOUNTERS_KEY= "seen";
    static final String TAGS_KEY      = "tags";
    static final String STAR_KEY      = "star";
    static final String FKDR_KEY      = "fkdr";
    static final String WINSTREAK_KEY = "winstreaks";
    static final String SESSION_KEY   = "session";
    static final String URCHIN_KEY    = "urchin";
    static final String LEVEL_KEY     = "netlevel";
    static final String PING_KEY      = "ping";
    static final String KILLMSG_ID_KEY  = "killmsgId";
    static final String NICK_FINALS_KEY = "nickFinals";
    static final String NICK_BEDS_KEY   = "nickBeds";
    static final String NICK_STAR_KEY   = "nickStar";
    static final String NICK_WOOD_KEY   = "nickWood";

    // ── Data keys (sort values) ────────────────────────────────────────────────
    static final String ENCOUNTERS_VALUE = "seenvalue";
    static final String JOIN_VALUE       = "joinvalue";
    static final String STAR_VALUE       = "starvalue";
    static final String FKDR_VALUE       = "fkdrvalue";
    static final String INDEX_VALUE      = "indexvalue";
    static final String SESSION_VALUE    = "sessionvalue";
    static final String WINSTREAK_VALUE  = "winstreakvalue";
    static final String WS_OVERALL_VALUE   = "wsoverall";
    static final String WS_MODE1_VALUE     = "wsmode1";
    static final String WS_MODE2_VALUE     = "wsmode2";
    static final String WS_MODE3_VALUE     = "wsmode3";
    static final String WS_MODE4_VALUE     = "wsmode4";
    static final String WS_MODE5_VALUE     = "wsmode5";
    static final String PREGAME_KEEP_KEY = "pregamekeep";

    // ── API keys (read from config) ────────────────────────────────────────────
    private String urchinKey()  { return LazifyConfig.INSTANCE.getUrchinKey(); }
    private String seraphKey()  { return LazifyConfig.INSTANCE.getSeraphKey(); }
    private String bordicKey()  { return LazifyConfig.INSTANCE.getBordicKey(); }
    private String hypixelKey() { return LazifyConfig.INSTANCE.getHypixelKey(); }

    // UUID → raw username mapping (for API calls that take username)
    private final Map<String, String> uuidToName = new ConcurrentHashMap<>();

    /** Used by {@link OverlayTheme} for team-colored nerdify names. */
    String uuidToNameForRender(String uuid) {
        return uuidToName.get(uuid);
    }

    // ── Core state (keyed by UUID without dashes) ──────────────────────────────
    Map<String, Map<String, Object>> overlayPlayers = new ConcurrentHashMap<>();
    Map<String, String>              ignoredPlayers  = new HashMap<>();
    /** Nick overlay UUID -> real v4 UUID after denick. */
    private final Map<String, String> nickRealUuid   = new ConcurrentHashMap<>();
    List<String>                     currentPlayers  = Collections.synchronizedList(new ArrayList<>());
    Map<String, List<Object[]>>      playerEncounters= new HashMap<>();
    Map<String, String>              teams           = new HashMap<>();
    Map<String, Map<String, Object>> statsCache      = new ConcurrentHashMap<>();
    Map<String, TagInfo>             tagCache        = new ConcurrentHashMap<>();
    Map<String, Integer>             pingCache       = new ConcurrentHashMap<>();

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
    /** Current Bedwars mode: 1=solo, 2=doubles, 3=threes, 4=fours, 5=4v4; -1=unknown. */
    int     currentBwMode = -1;
    int     lobbyMaxPlayers = -1;
    public boolean isInBedwars() { return status >= 1 || inBwPregame; }
    boolean ascending    = false;
    boolean showYourself    = false;
    boolean showTeamPrefix  = false;
    boolean showTeamColors  = true;
    String  sortBy       = FKDR_VALUE;
    int     overlayTicks = 5;
    boolean dowho        = true;
    boolean didwho       = false;
    boolean inBwPregame  = false;
    boolean dodgeWarned  = false;
    boolean teamFkdrSent = false;
    boolean teamThreatSent = false;

    // ── Party tracking ────────────────────────────────────────────────────────
    private final Set<String> partyMembers = Collections.synchronizedSet(new HashSet<String>());
    private boolean parsingPartyList = false;
    private boolean partySent = false;

    // ── Overlay layout ─────────────────────────────────────────────────────────
    int   startX = 500, startY = 12, offsetY = 5;
    int   endX = 0, endY = 0;
    float borderWidth = 2.5f;
    int   background, borderColorRGB, columnTitles;

    static final String PREFIX = "\u00a77[\u00a7dL\u00a77]\u00a7r ";

    private static final Pattern CHAT_SENDER  = Pattern.compile("^(?:\\[[\\w+]+\\] )?(\\w+) ?: .+");
    private static final Pattern LOBBY_JOIN   = Pattern.compile("^(\\w+) has joined \\((\\d+)/(\\d+)\\)!$");
    private static final Pattern PREGAME_LIST = Pattern.compile("^\\+ \\((\\d+)/(\\d+)\\) (\\w+)$");

    private final Queue<String> pendingMessages = new java.util.concurrent.ConcurrentLinkedQueue<>();
    private final Queue<String> pendingCommands = new java.util.concurrent.ConcurrentLinkedQueue<>();

    private OverlayManager() {}

    private static final class TeamThreatSummary {
        final String teamLabel;
        final double score;
        final int players;
        final int taggedPlayers;
        final int nickedPlayers;

        TeamThreatSummary(String teamLabel, double score, int players, int taggedPlayers, int nickedPlayers) {
            this.teamLabel = teamLabel;
            this.score = score;
            this.players = players;
            this.taggedPlayers = taggedPlayers;
            this.nickedPlayers = nickedPlayers;
        }
    }

    private static final class TeamThreatAccumulator {
        final String teamLabel;
        double totalScore;
        int players;
        int taggedPlayers;
        int nickedPlayers;

        TeamThreatAccumulator(String teamLabel) {
            this.teamLabel = teamLabel;
        }

        void add(double playerScore, boolean tagged, boolean nicked) {
            totalScore += playerScore;
            players++;
            if (tagged) taggedPlayers++;
            if (nicked) nickedPlayers++;
        }
    }

    // ==========================================================================
    // Init (called once from LazifyMod.init)
    // ==========================================================================

    public void init(File configDir) {
        LazifyConfig.INSTANCE.load(configDir);

        columns.clear(); sortingOptions.clear(); parseSortingMode.clear(); tags.clear();

        addColumn("Encounters", "[E]",       ENCOUNTERS_KEY);
        addColumn("Username",   "[PLAYER]",  PLAYER_KEY);
        addColumn("Rank",       "[RANK]",    RANK_KEY);
        addColumn("Star",       "[STAR]",    STAR_KEY);
        addColumn("FKDR",       "[FKDR]",    FKDR_KEY);
        addColumn("Winstreaks", "[WS]",      WINSTREAK_KEY);
        addColumn("Tags",       "[T]",       URCHIN_KEY);
        addColumn("Session",    "[SESSION]", SESSION_KEY);
        addColumn("Level",      "[LVL]",     LEVEL_KEY);
        addColumn("Ping",       "[PING]",    PING_KEY);

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
        if (urchinKey().isEmpty() && seraphKey().isEmpty())
            print(PREFIX + "\u00a7eNo tag API keys set. Use \u00a73/ov key urchin <key>\u00a7e or \u00a73/ov key seraph <key>\u00a7e.");
        else if (urchinKey().isEmpty())
            print(PREFIX + "\u00a7eNo Urchin API key set. Use \u00a73/ov key urchin <key>\u00a7e for Urchin tags.");
        else if (seraphKey().isEmpty())
            print(PREFIX + "\u00a7eNo Seraph API key set. Use \u00a73/ov key seraph <key>\u00a7e for Seraph tags.");
        if (bordicKey().isEmpty())
            print(PREFIX + "\u00a7eNo Bordic API key set. Use \u00a73/ov key bordic <key>\u00a7e to enable superstar nick denicking.");
        if (hypixelKey().isEmpty())
            print(PREFIX + "\u00a7eNo Hypixel API key set. Stats use Abyss/Prism only. \u00a73/ov key hypixel <key>\u00a7e for fallback.");
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
        offsetY        = cfg.getOverlayRowGap();

        int idx = cfg.getSortByIndex();
        if (idx >= 0 && idx < sortingOptions.size())
            sortBy = parseSortingMode.getOrDefault(sortingOptions.get(idx), FKDR_VALUE);

        background     = ColorUtil.getHueRGB(cfg.getBgHue(),     cfg.getBgOpacity());
        columnTitles   = ColorUtil.getHueRGB(cfg.getHeaderHue(), 255);
        borderColorRGB = ColorUtil.getHueRGB(cfg.getBorderHue(), 255);

        if (OverlayTheme.isNerdify(cfg.getOverlayTheme())) {
            background     = OverlayTheme.backgroundColor(cfg.getBgOpacity());
            columnTitles   = OverlayTheme.NERDIFY_GREEN;
            borderColorRGB = OverlayTheme.NERDIFY_GREEN;
            borderWidth    = 1.0f;
        } else {
            borderWidth    = 2.5f;
        }

        if (!showTeamColors) {
            teams.clear();
        }

        for (ColumnDef col : columns) {
            switch (col.getKey()) {
                case ENCOUNTERS_KEY: col.setEnabled(cfg.isColEncounters()); break;
                case PLAYER_KEY:     col.setEnabled(cfg.isColUsername());   break;
                case RANK_KEY:       col.setEnabled(cfg.isColRank());       break;
                case STAR_KEY:       col.setEnabled(cfg.isColStar());       break;
                case FKDR_KEY:       col.setEnabled(cfg.isColFkdr());       break;
                case WINSTREAK_KEY:  col.setEnabled(cfg.isColWinstreaks()); break;
                case URCHIN_KEY:     col.setEnabled(cfg.isColUrchin());     break;
                case SESSION_KEY:    col.setEnabled(cfg.isColSession());    break;
                case LEVEL_KEY:      col.setEnabled(cfg.isColLevel());      break;
                case PING_KEY:       col.setEnabled(cfg.isColPing());       break;
            }
        }

        // Apply column order from config
        String[] order = cfg.getColOrder().split(",");
        final Map<String, Integer> orderMap = new HashMap<>();
        for (int i = 0; i < order.length; i++) orderMap.put(order[i].trim(), i);
        columns.sort((a, b) -> {
            int ia = orderMap.getOrDefault(colKeyToName(a.getKey()), 999);
            int ib = orderMap.getOrDefault(colKeyToName(b.getKey()), 999);
            return Integer.compare(ia, ib);
        });

        if (!columns.isEmpty()) {
            doColumns(false);
        }
    }

    private static String colKeyToName(String key) {
        switch (key) {
            case ENCOUNTERS_KEY: return "encounters";
            case PLAYER_KEY:     return "username";
            case RANK_KEY:       return "rank";
            case STAR_KEY:       return "star";
            case FKDR_KEY:       return "fkdr";
            case WINSTREAK_KEY:  return "winstreaks";
            case URCHIN_KEY:     return "urchin";
            case SESSION_KEY:    return "session";
            case LEVEL_KEY:      return "level";
            case PING_KEY:       return "ping";
            default:             return key;
        }
    }

    private static String moveColInOrder(String currentOrder, String colName, int pos) {
        List<String> order = new ArrayList<>(Arrays.asList(currentOrder.split(",")));
        for (int i = 0; i < order.size(); i++) order.set(i, order.get(i).trim());
        order.remove(colName);
        int insertAt = Math.max(0, Math.min(pos - 1, order.size()));
        order.add(insertAt, colName);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < order.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(order.get(i));
        }
        return sb.toString();
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
        if (status >= 1 || inBwPregame) {
            refreshWinstreakDisplays();
        }

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

        if (shouldScanWoodPlacements()) {
            scanNickWoodPlacements(mc);
        }

        // Auto /who: send /who once when the game starts (status becomes 3 / ingame)
        // Handled in updateStatus() on status transition to 3

        // Send /pl once per lobby to detect party members for dodge warning
        if (!partySent && (status >= 1 || inBwPregame)) {
            partySent = true;
            new Thread(() -> {
                try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                pendingCommands.add("/pl");
                debugFromThread("Auto /pl triggered for party detection");
            }).start();
        }

        if (status < 1 && !inBwPregame) return;
        if (!LazifyConfig.INSTANCE.isAutoTablist()) return;

        Set<String> currentEntityUUIDs = new HashSet<>();
        long currentTime = System.currentTimeMillis();
        int threshold = LazifyConfig.INSTANCE.getEncountersTimeoutMins() * 60000;

        // Pre-pass: group tab entries by username to detect duplicate-name nicks
        Map<String, List<String>> nameToUuids = new HashMap<>();
        for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
            String name = info.getGameProfile().getName().toLowerCase();
            String uid = info.getGameProfile().getId().toString().replace("-", "");
            List<String> list = nameToUuids.get(name);
            if (list == null) { list = new ArrayList<>(); nameToUuids.put(name, list); }
            list.add(uid);
        }

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
                if (status == 3) {
                    String formatted = formatTabPlayerName(pla, displayName);
                    if (formatted != null) {
                        if (showTeamColors) {
                            teams.put(uuid, formatted);
                        } else {
                            teams.remove(uuid);
                        }
                        String prev = String.valueOf(
                                overlayPlayers.getOrDefault(uuid, Collections.<String, Object>emptyMap()).get(PLAYER_KEY));
                        String finalDisplay = formatted;
                        if (LazifyConfig.INSTANCE.isShowRanks()) {
                            Map<String, Object> existing = overlayPlayers.get(uuid);
                            if (existing != null) {
                                String rp = (String) existing.get("rankPrefix");
                                if (rp != null && !rp.isEmpty() && !rp.equals("\u00a77")) {
                                    finalDisplay = rp + " " + formatted;
                                }
                            }
                        }
                        if (!finalDisplay.equals(prev)) {
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

            if (ColorUtil.hasObfuscationBefore(displayName, username)) {
                debug("Skipping obfuscated tab player: " + username + " uuid=" + uuid);
                continue;
            }

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
            placeholder.put(PLAYER_KEY,       formatTabPlayerName(pla, displayName));

            // Offline UUID heuristic — tab denick; API nick check runs in handlePlayerStats
            if (isNickedKey(uuid)) {
                debug("Nick detected: " + username + " uuid=" + uuid);
                placeholder.put("nicked", true);
                placeholder.put("apinicked", "\u00a7eN");
                placeholder.put(URCHIN_KEY, "\u00a7bN");
                placeholder.put(RANK_KEY, ColorUtil.formatRankColumn(true, ""));
                uuidToName.put(uuid, username);

                // Check if this nick appears twice in tab list (Hypixel leaks the real UUID)
                List<String> sameNameUuids = nameToUuids.get(username.toLowerCase());
                String leakedUuid = null;
                if (sameNameUuids != null && sameNameUuids.size() > 1) {
                    for (String u : sameNameUuids) {
                        if (isV4UndashedUuid(u) && !u.equals(uuid)) {
                            leakedUuid = u; break;
                        }
                    }
                }

                if (leakedUuid != null) {
                    // Tab denick: resolve real name from leaked UUID via session server
                    placeholder.put(PLAYER_KEY, username);
                    overlayPlayers.put(uuid, placeholder);
                    sortOverlay();
                    addToPlayers(uuid);
                    final String fLeaked = leakedUuid, fNickUuid = uuid;
                    final String fNickName = username, fLobby = currentLobby;
                    debug("Tab denick: duplicate name for " + username + ", leaked uuid=" + leakedUuid);
                    new Thread(() -> {
                        String[] conv = convertPlayer(fLeaked);
                        String resolvedName = (conv[1] != null && !conv[1].isEmpty()) ? conv[1] : null;
                        if (resolvedName != null) {
                            debugFromThread("Tab denick: " + fNickName + " -> " + resolvedName);
                            applyDenick(fNickUuid, fLeaked, fNickName, resolvedName, fLobby, "tab");
                        }
                    }).start();
                } else {
                    // Skin denick: match skin texture against known players
                    String realName = LazifyConfig.INSTANCE.isSkinDenick() ? SkinDenick.getRealName(pla) : null;
                    if (realName != null && !realName.isEmpty()) {
                        debug("Skin denick: " + username + " -> " + realName);
                        placeholder.put(PLAYER_KEY, "\u00a7e" + username + " \u00a7d> \u00a7a" + realName);
                        overlayPlayers.put(uuid, placeholder);
                        addPlaceholderStats(uuid, realName, false);
                        addToPlayers(uuid);
                        final String fUuid = uuid, fLobby = currentLobby;
                        final String fNick = username, fReal = realName;
                        new Thread(() -> {
                            String[] conv = convertPlayer(fReal);
                            String realUuid = conv[0];
                            if (realUuid == null || realUuid.isEmpty()) {
                                String[] conv2 = convertPlayerPlayerdb(fReal);
                                realUuid = conv2[0];
                            }
                            applyDenick(fUuid, realUuid, fNick, fReal, fLobby, "skin");
                        }).start();
                    } else {
                        placeholder.put(PLAYER_KEY, username);
                        overlayPlayers.put(uuid, placeholder);
                        sortOverlay();
                        if (LazifyConfig.INSTANCE.isSendNickedToChat()) {
                            print(PREFIX + "\u00a7c" + username + " \u00a7eis nicked");
                        }
                        addToPlayers(uuid);
                        final String fUuid = uuid, fLobby = currentLobby;
                        new Thread(() -> {
                            handlePlayerStats(fUuid, fLobby);
                            handlePlayerTags(fUuid, fLobby);
                            handleBordicPing(fUuid, fLobby);
                        }).start();
                    }
                }
                continue;
            }

            debug("Adding player from tab: " + username + " uuid=" + uuid);
            overlayPlayers.put(uuid, placeholder);
            addPlaceholderStats(uuid, displayName, false);
            addToPlayers(uuid);

            final String fUuid  = uuid;
            final String fLobby = currentLobby;
            new Thread(() -> {
                handlePlayerStats(fUuid, fLobby);
                handlePlayerTags(fUuid, fLobby);
                handleBordicPing(fUuid, fLobby);
            }).start();
        }

        // Re-sort when team grouping is active (order can change as teams fill)
        if (status == 3 && showTeamColors) {
            sortOverlay();
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

            // Auto /who: fire once when game starts (transition to ingame status 3)
            if (status == 3 && LazifyConfig.INSTANCE.isAutoWho() && dowho) {
                dowho = false;
                long whoDelayMs = (long) (LazifyConfig.INSTANCE.getWhoDelay() * 1000);
                new Thread(() -> {
                    if (whoDelayMs > 0) {
                        try { Thread.sleep(whoDelayMs); } catch (InterruptedException ignored) {}
                    }
                    pendingCommands.add("/who");
                    debugFromThread("Auto /who triggered on game start (delay " + whoDelayMs + "ms)");
                }).start();
            }
        }
        if (!lastLobby.equals(currentLobby)) {
            debug("Lobby changed: " + lastLobby + " -> " + currentLobby);
            currentBwMode = -1;
            lobbyMaxPlayers = -1;
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
            if (stripped.equals("Waiting...") || stripped.startsWith("Starting in")) {
                updateBedwarsMode(sidebar);
                return 2;
            }
            // Team status lines like "R Red: ✓" or "B Blue: ✗" indicate in-game
            if (stripped.length() >= 2 && stripped.charAt(1) == ' '
                    && (stripped.contains("Red:") || stripped.contains("Blue:")
                     || stripped.contains("Green:") || stripped.contains("Yellow:")
                     || stripped.contains("Aqua:") || stripped.contains("White:")
                     || stripped.contains("Pink:") || stripped.contains("Gray:"))) {
                updateBedwarsMode(sidebar);
                return 3;
            }
        }

        updateBedwarsMode(sidebar);
        return -1;
    }

    private void updateBedwarsMode(List<String> sidebar) {
        int detected = detectBedwarsMode(sidebar);
        if (detected > 0 && detected != currentBwMode) {
            debug("Bedwars mode: " + detected + winstreakSuffix(detected)
                    + " (lobbyMax=" + lobbyMaxPlayers + ")");
            currentBwMode = detected;
        }
    }

    private int detectBedwarsMode(List<String> sidebar) {
        if (sidebar != null) {
            for (String line : sidebar) {
                String s = ColorUtil.strip(line).trim().toLowerCase();
                if (s.equals("solo") || s.equals("solos") || s.endsWith(" solo")) return 1;
                if (s.equals("doubles") || s.endsWith(" doubles")) return 2;
                if (s.equals("threes") || s.contains("3v3v3v3")) return 3;
                if (s.equals("fours") || s.contains("4v4v4v4")) return 4;
                if (s.equals("4v4") || s.contains("2 x 4") || s.contains("2x4")) return 5;
            }
        }

        int teams = countSidebarTeams(sidebar);
        if (teams <= 0 || lobbyMaxPlayers <= 0) return -1;

        if (lobbyMaxPlayers == 12 && teams >= 3) return 3;
        if (lobbyMaxPlayers == 16 && teams >= 7) return 2;
        if (lobbyMaxPlayers == 16 && teams >= 3 && teams <= 4) return 4;
        if (lobbyMaxPlayers == 8 && teams >= 7) return 1;
        if (lobbyMaxPlayers == 8 && teams <= 2) return 5;
        return -1;
    }

    private static int countSidebarTeams(List<String> sidebar) {
        if (sidebar == null) return 0;
        int count = 0;
        for (String line : sidebar) {
            String stripped = ColorUtil.strip(line).trim();
            if (stripped.length() >= 2 && stripped.charAt(1) == ' '
                    && (stripped.contains("Red:") || stripped.contains("Blue:")
                     || stripped.contains("Green:") || stripped.contains("Yellow:")
                     || stripped.contains("Aqua:") || stripped.contains("White:")
                     || stripped.contains("Pink:") || stripped.contains("Gray:"))) {
                count++;
            }
        }
        return count;
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

    private String formatTabPlayerName(NetworkPlayerInfo pla, String displayName) {
        String baseName = pla.getGameProfile().getName();
        if (status == 3) {
            if (showTeamColors) {
                String teamDisplay = getTeamDisplayFromTab(pla, displayName);
                if (teamDisplay != null) return teamDisplay;
            }
            return stripHypixelTeamPrefix(displayName, baseName, false);
        }
        return displayName != null ? displayName : baseName;
    }

    private String stripHypixelTeamPrefix(String displayName, String baseName, boolean colorOnly) {
        if (baseName == null || baseName.isEmpty()) return displayName;
        String raw = displayName != null ? displayName : baseName;
        String stripped = ColorUtil.strip(raw);
        if (stripped.equalsIgnoreCase(baseName)) {
            return colorOnly ? getTeamColorOnlyName(baseName) : raw;
        }

        String extracted = extractNameAfterTeamMarker(stripped, baseName);
        if (extracted != null) {
            return colorOnly ? getTeamColorOnlyName(baseName) : baseName;
        }
        return colorOnly ? getTeamColorOnlyName(baseName) : baseName;
    }

    /** Pulls username from Hypixel tab forms like "G Name" or "G_Name". */
    private String extractNameAfterTeamMarker(String stripped, String baseName) {
        if (stripped == null || baseName == null) return null;
        if (stripped.equalsIgnoreCase(baseName)) return baseName;

        if (stripped.contains(" ")) {
            String[] parts = stripped.split(" ");
            if (parts.length >= 2 && parts[parts.length - 1].equalsIgnoreCase(baseName)) {
                return baseName;
            }
        }

        if (stripped.length() > baseName.length() + 1) {
            char sep = stripped.charAt(stripped.length() - baseName.length() - 1);
            if ((sep == ' ' || sep == '_') && stripped.regionMatches(true,
                    stripped.length() - baseName.length(), baseName, 0, baseName.length())) {
                return baseName;
            }
        }
        return null;
    }

    private String getTeamColorOnlyName(String baseName) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || baseName == null) return baseName;
        ScorePlayerTeam team = mc.theWorld.getScoreboard().getPlayersTeam(baseName);
        if (team == null) return baseName;
        return team.getColorPrefix() + baseName + team.getColorSuffix();
    }

    private int teamSortOrder(String uuid) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) return 999;
        String username = uuidToName.get(uuid);
        if (username == null || username.isEmpty()) {
            Map<String, Object> ps = overlayPlayers.get(uuid);
            if (ps != null) {
                Object u = ps.get("username");
                if (u instanceof String) username = (String) u;
            }
        }
        if (username == null || username.isEmpty()) return 999;
        ScorePlayerTeam team = mc.theWorld.getScoreboard().getPlayersTeam(username);
        if (team == null) return 999;
        return teamOrderFromName(team.getRegisteredName());
    }

    private static int teamOrderFromName(String teamName) {
        if (teamName == null) return 999;
        String n = teamName.toLowerCase(Locale.ROOT);
        if (n.contains("red"))    return 0;
        if (n.contains("blue"))   return 1;
        if (n.contains("green"))  return 2;
        if (n.contains("yellow")) return 3;
        if (n.contains("aqua"))   return 4;
        if (n.contains("white"))  return 5;
        if (n.contains("pink"))   return 6;
        if (n.contains("gray") || n.contains("grey")) return 7;
        return 999;
    }

    private String getTeamDisplayFromTab(NetworkPlayerInfo pla, String fallbackDisplayName) {
        String raw = fallbackDisplayName != null ? fallbackDisplayName : pla.getGameProfile().getName();
        String stripped = ColorUtil.strip(raw);
        String baseName = pla.getGameProfile().getName();
        if (!stripped.equalsIgnoreCase(baseName)) {
            String extracted = extractNameAfterTeamMarker(stripped, baseName);
            if (extracted != null) {
                if (showTeamPrefix) return raw;
                return getTeamColorOnlyName(baseName);
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
        boolean nerdify = OverlayTheme.isNerdify(LazifyConfig.INSTANCE.getOverlayTheme());
        int colGap = LazifyConfig.INSTANCE.getOverlayColGap();
        int edgePad = nerdify ? OverlayTheme.edgePad() : Math.max(4, colGap / 2);
        int currentX = startX + edgePad;

        for (ColumnDef col : columns) {
            int longest = OverlayRenderer.getFontWidth(
                    nerdify ? OverlayTheme.headerFor(col.getKey()) : col.getHeader());
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

                    int w;
                    if (nerdify) {
                        if (PLAYER_KEY.equals(key)) {
                            w = OverlayRenderer.getFontWidth(
                                    OverlayTheme.measurePlayer(uuid, value, pd));
                        } else {
                            w = OverlayRenderer.getFontWidth(OverlayTheme.measureText(key, value));
                        }
                        int minW = OverlayTheme.minColWidth(key);
                        if (minW > 0) w = Math.max(w, minW);
                    } else {
                        w = OverlayRenderer.getFontWidth(value);
                        if (key.equals(PLAYER_KEY)) {
                            w = OverlayRenderer.getFontWidth(ColorUtil.strip(value));
                        }
                    }
                    if (w > longest) longest = w;
                    if (key.equals(URCHIN_KEY)) {
                        longest = Math.max(longest, OverlayRenderer.getFontWidth("BC+C"));
                    }
                    if (key.equals(RANK_KEY)) {
                        longest = Math.max(longest, OverlayRenderer.getFontWidth("[MVP++]"));
                    }
                }
            }

            if (nerdify) {
                int minW = OverlayTheme.minColWidth(key);
                if (minW > longest) longest = minW;
            }
            col.setMaxwidth(longest);
            col.setPosition(currentX);
            currentX += longest + colGap;
        }

        int lineHeight = nerdify ? OverlayRenderer.nerdifyLineHeight()
                : OverlayRenderer.getFontHeight() + offsetY;
        int headerBlock = nerdify
                ? OverlayTheme.headerPadTop() + OverlayRenderer.getFontHeight()
                    + OverlayTheme.headerSeparatorGap() + OverlayTheme.rowGapAfterSeparator()
                : lineHeight + 5;
        endX = currentX + (nerdify ? edgePad : 0);
        endY = startY + headerBlock + (currentPlayers.size() * lineHeight)
                + (currentPlayers.size() > 0 ? OverlayTheme.bottomPad() : (nerdify ? OverlayTheme.bottomPad() : 1));
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
            checkDodgeWarning();
            sendTeamFkdrToPartyChat();
            sendTeamThreatToPartyChat();
        } catch (Exception e) {
            print(PREFIX + "\u00a7eError detected. Please check \u00a73latest.log\u00a7e.");
        }
    }

    private void checkDodgeWarning() {
        if (dodgeWarned || !LazifyConfig.INSTANCE.isDodgeWarning()) return;
        if (status < 1 && !inBwPregame) return;

        // Build set of UUIDs to exclude (self + party members)
        Set<String> excluded = new HashSet<>(partyMembers);
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            excluded.add(mc.thePlayer.getGameProfile().getId().toString().replace("-", ""));
        }

        double total = 0; int count = 0;
        for (Map.Entry<String, Map<String, Object>> entry : overlayPlayers.entrySet()) {
            if (excluded.contains(entry.getKey())) continue;
            Object fv = entry.getValue().get(FKDR_VALUE);
            if (fv instanceof Double) {
                total += (Double) fv;
                count++;
            }
        }
        if (count < 2) return;

        double avg = total / count;
        double threshold = LazifyConfig.INSTANCE.getDodgeThreshold();
        if (avg >= threshold) {
            dodgeWarned = true;
            String avgStr = ColorUtil.formatDoubleStr(ColorUtil.round(avg, 2));
            printFromThread(PREFIX + "\u00a7c\u26a0 Lobby dodge warning! \u00a7eAvg FKDR: \u00a7c" + avgStr
                + " \u00a7e(threshold: \u00a73" + ColorUtil.formatDoubleStr(threshold) + "\u00a7e)");
        }
    }

    private void sendTeamFkdrToPartyChat() {
        if (teamFkdrSent || !LazifyConfig.INSTANCE.isTeamFkdrChat()) return;
        if (status != 3) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) return;

        // Group player FKDR values by scoreboard team name
        Map<String, List<Double>> teamFkdrs = new LinkedHashMap<>();
        for (String uuid : currentPlayers) {
            Map<String, Object> data = overlayPlayers.get(uuid);
            if (data == null) continue;
            Object fv = data.get(FKDR_VALUE);
            if (!(fv instanceof Double)) continue;
            String username = uuidToName.get(uuid);
            if (username == null) continue;
            ScorePlayerTeam team = mc.theWorld.getScoreboard().getPlayersTeam(username);
            String teamName = team != null ? ColorUtil.strip(team.getColorPrefix()).trim() + ColorUtil.strip(team.getRegisteredName()) : "?";
            // Use just team color code + registered name for key
            String key = team != null ? team.getRegisteredName() : "?";
            if (!teamFkdrs.containsKey(key)) teamFkdrs.put(key, new ArrayList<>());
            teamFkdrs.get(key).add((Double) fv);
        }
        if (teamFkdrs.isEmpty()) return;

        // Build sorted list (highest avg first)
        List<Map.Entry<String, List<Double>>> sorted = new ArrayList<>(teamFkdrs.entrySet());
        sorted.sort((a, b) -> {
            double avgA = a.getValue().stream().mapToDouble(d -> d).average().orElse(0);
            double avgB = b.getValue().stream().mapToDouble(d -> d).average().orElse(0);
            return Double.compare(avgB, avgA);
        });

        StringBuilder msg = new StringBuilder("Team FKDRs: ");
        for (int i = 0; i < sorted.size(); i++) {
            Map.Entry<String, List<Double>> e = sorted.get(i);
            double avg = e.getValue().stream().mapToDouble(d -> d).average().orElse(0);
            String avgStr = ColorUtil.formatDoubleStr(ColorUtil.round(avg, 2));
            String teamLabel = e.getKey();
            // Capitalize first letter
            if (!teamLabel.isEmpty()) teamLabel = Character.toUpperCase(teamLabel.charAt(0)) + teamLabel.substring(1).toLowerCase();
            msg.append(teamLabel).append(": ").append(avgStr);
            if (i < sorted.size() - 1) msg.append(" | ");
        }

        teamFkdrSent = true;
        final String finalMsg = msg.toString();
        new Thread(() -> {
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            pendingCommands.add("/pc " + finalMsg);
            debugFromThread("Team FKDR sent to party chat: " + finalMsg);
        }).start();
    }

    private void sendTeamThreatToPartyChat() {
        if (teamThreatSent || !LazifyConfig.INSTANCE.isTeamThreatChat()) return;
        if (status != 3) return;

        List<TeamThreatSummary> summaries = buildTeamThreatSummaries();
        if (summaries.size() < 2) return;

        double threshold = LazifyConfig.INSTANCE.getTeamThreatThreshold();
        if (summaries.get(0).score < threshold) return;

        StringBuilder msg = new StringBuilder("Threat: ");
        int shown = 0;
        for (TeamThreatSummary summary : summaries) {
            if (shown >= 4) break;
            if (shown > 0) msg.append(" | ");
            msg.append(summary.teamLabel).append(" ")
               .append(getThreatLevel(summary.score, threshold)).append(" ")
               .append(ColorUtil.formatDoubleStr(ColorUtil.round(summary.score, 2)))
               .append(" [").append(summary.players).append("p");
            if (summary.taggedPlayers > 0) msg.append(", U").append(summary.taggedPlayers);
            if (summary.nickedPlayers > 0) msg.append(", N").append(summary.nickedPlayers);
            msg.append("]");
            shown++;
        }
        if (shown == 0) return;

        teamThreatSent = true;
        final String finalMsg = msg.toString();
        new Thread(() -> {
            try { Thread.sleep(650); } catch (InterruptedException ignored) {}
            pendingCommands.add("/pc " + finalMsg);
            debugFromThread("Team threat sent to party chat: " + finalMsg);
        }).start();
    }

    private List<TeamThreatSummary> buildTeamThreatSummaries() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) return Collections.emptyList();

        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        if (scoreboard == null) return Collections.emptyList();

        ScorePlayerTeam selfTeam = scoreboard.getPlayersTeam(mc.thePlayer.getName());
        String selfTeamKey = selfTeam != null ? selfTeam.getRegisteredName() : null;

        Map<String, TeamThreatAccumulator> teamThreats = new LinkedHashMap<>();
        List<String> playerCopy;
        synchronized (currentPlayers) {
            playerCopy = new ArrayList<>(currentPlayers);
        }

        for (String uuid : playerCopy) {
            Map<String, Object> data = overlayPlayers.get(uuid);
            if (data == null) continue;

            String username = uuidToName.get(uuid);
            if (username == null || username.isEmpty()) continue;

            ScorePlayerTeam team = scoreboard.getPlayersTeam(username);
            if (team == null) continue;

            String teamKey = team.getRegisteredName();
            if (teamKey == null || teamKey.isEmpty() || teamKey.equals(selfTeamKey)) continue;

            TeamThreatAccumulator acc = teamThreats.get(teamKey);
            if (acc == null) {
                acc = new TeamThreatAccumulator(formatTeamLabel(teamKey));
                teamThreats.put(teamKey, acc);
            }
            acc.add(getPlayerThreatScore(uuid, data), hasUrchinThreat(uuid), isThreatNicked(data));
        }

        if (teamThreats.isEmpty()) return Collections.emptyList();

        List<TeamThreatSummary> summaries = new ArrayList<>();
        double teamSizeWeight = LazifyConfig.INSTANCE.getThreatTeamSizeWeight();
        for (TeamThreatAccumulator acc : teamThreats.values()) {
            if (acc.players <= 0) continue;
            double avgScore = acc.totalScore / acc.players;
            double teamScore = avgScore + Math.max(0, acc.players - 1) * teamSizeWeight;
            summaries.add(new TeamThreatSummary(
                acc.teamLabel,
                ColorUtil.round(teamScore, 2),
                acc.players,
                acc.taggedPlayers,
                acc.nickedPlayers
            ));
        }

        summaries.sort((a, b) -> Double.compare(b.score, a.score));
        return summaries;
    }

    private double getPlayerThreatScore(String uuid, Map<String, Object> data) {
        LazifyConfig cfg = LazifyConfig.INSTANCE;
        double score = 0.0;

        score += Math.min(15.0, getDoubleStat(data, FKDR_VALUE)) * cfg.getThreatFkdrWeight();
        score += Math.min(10.0, getDoubleStat(data, STAR_VALUE) / 100.0) * cfg.getThreatStarWeight();
        score += Math.min(8.0, getDoubleStat(data, WINSTREAK_VALUE) / 25.0) * cfg.getThreatWinstreakWeight();
        score += Math.min(5.0, getDoubleStat(data, ENCOUNTERS_VALUE)) * cfg.getThreatEncounterWeight();
        score += getUrchinThreatValue(uuid) * cfg.getThreatUrchinWeight();
        if (isThreatNicked(data)) score += cfg.getThreatNickWeight();

        return ColorUtil.round(score, 3);
    }

    private double getDoubleStat(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value instanceof Double ? (Double) value : 0.0;
    }

    private boolean isThreatNicked(Map<String, Object> data) {
        Object nicked = data.get("nicked");
        if (nicked instanceof Boolean && (Boolean) nicked) return true;
        Object apiNicked = data.get("apinicked");
        return apiNicked instanceof String && !((String) apiNicked).isEmpty();
    }

    private boolean hasUrchinThreat(String uuid) {
        return getUrchinThreatValue(uuid) > 0.0;
    }

    private double getUrchinThreatValue(String uuid) {
        TagInfo info = tagCache.get(uuid);
        return info != null ? info.threatValue() : 0.0;
    }

    private String formatTeamLabel(String teamKey) {
        if (teamKey == null || teamKey.isEmpty()) return "?";
        return Character.toUpperCase(teamKey.charAt(0)) + teamKey.substring(1).toLowerCase(Locale.ROOT);
    }

    private String getThreatLevel(double score, double threshold) {
        if (score >= threshold * 1.75) return "EXTREME";
        if (score >= threshold * 1.3) return "HIGH";
        if (score >= threshold) return "ELEVATED";
        if (score >= threshold * 0.65) return "MEDIUM";
        return "LOW";
    }

    private void parsePartyLine(String line) {
        // Format: "Party Leader: [RANK] Name ●" or "Party Members: [RANK] Name ● [RANK] Name2 ●"
        int colon = line.indexOf(':');
        if (colon < 0) return;
        String rest = line.substring(colon + 1).trim();
        // Split on ● to get each player segment
        String[] parts = rest.split("\u25cf|\\[.*?\\]");
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getNetHandler() == null) return;
        for (String part : parts) {
            String name = part.trim();
            if (name.isEmpty()) continue;
            // Look up UUID from tab list
            for (NetworkPlayerInfo npi : mc.getNetHandler().getPlayerInfoMap()) {
                if (npi.getGameProfile().getName().equalsIgnoreCase(name)) {
                    String uuid = npi.getGameProfile().getId().toString().replace("-", "");
                    partyMembers.add(uuid);
                    debug("Party member: " + name + " (" + uuid + ")");
                    break;
                }
            }
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
                if (showTeamColors && status == 3) {
                    int t1 = teamSortOrder(u1);
                    int t2 = teamSortOrder(u2);
                    if (t1 != t2) return Integer.compare(t1, t2);
                }

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
        tagCache.clear();
        pingCache.clear();
        nickRealUuid.clear();
        BordicSuperstar.clearCache();
        synchronized (currentPlayers) { currentPlayers.clear(); }
    }

    // ── Hidden players (/ov hide) ─────────────────────────────────────────────

    public java.util.Set<String> getHiddenPlayers() {
        return new java.util.TreeSet<>(ignoredPlayers.keySet());
    }

    public void hidePlayer(String name) {
        if (name == null || name.isEmpty()) return;
        ignoredPlayers.put(name.toLowerCase(), "");
    }

    public void unhidePlayer(String name) {
        if (name == null) return;
        ignoredPlayers.remove(name.toLowerCase());
    }

    public void clearHiddenPlayers() {
        ignoredPlayers.clear();
    }

    /** Re-fetch Urchin/Seraph tags for everyone currently on the overlay. */
    public void refreshOverlayTags() {
        tagCache.clear();
        final String lobby = currentLobby;
        synchronized (currentPlayers) {
            for (String uuid : currentPlayers) {
                new Thread(() -> handlePlayerTags(uuid, lobby)).start();
            }
        }
    }

    private String statsUuidFor(String overlayUuid) {
        String real = nickRealUuid.get(overlayUuid);
        return real != null ? real : overlayUuid;
    }

    /** After denick, fetch real stats/tags for the nick row on the overlay. */
    private void applyDenick(String nickUuid, String realUuid, String nickName, String realName,
                             String lobby, String chatSuffix) {
        if (realUuid != null && !realUuid.isEmpty()) {
            realUuid = realUuid.replace("-", "");
        }
        if (realUuid != null && isV4UndashedUuid(realUuid)) {
            nickRealUuid.put(nickUuid, realUuid);
            statsCache.remove(realUuid);
            tagCache.remove(realUuid);
            pingCache.remove(realUuid);
        }
        statsCache.remove(nickUuid);
        tagCache.remove(nickUuid);
        pingCache.remove(nickUuid);

        if (!isInOverlay(nickUuid) || !currentLobby.equals(lobby)) return;

        Map<String, Object> denickData = new ConcurrentHashMap<>();
        denickData.put(PLAYER_KEY, "\u00a7e" + nickName + " \u00a7d> \u00a7a" + realName);
        denickData.put("nicked", true);
        denickData.put("denicked", true);
        addToOverlay(nickUuid, denickData);
        uuidToName.put(nickUuid, realName);

        handlePlayerStats(nickUuid, lobby);
        handlePlayerTags(nickUuid, lobby);
        handleBordicPing(nickUuid, lobby);

        if (LazifyConfig.INSTANCE.isSendNickedToChat()) {
            String suffix = chatSuffix == null ? "" : " \u00a77(" + chatSuffix + ")";
            printFromThread(PREFIX + "\u00a7e" + nickName + " \u00a7dis nicked \u00a7d> \u00a7a" + realName + suffix);
        }
    }

    private void preserveDenickDisplay(String overlayUuid, Map<String, Object> stats) {
        Map<String, Object> existing = overlayPlayers.get(overlayUuid);
        if (existing == null) return;
        Object display = existing.get(PLAYER_KEY);
        if (display != null && display.toString().contains("\u00a7d>")) {
            stats.put(PLAYER_KEY, display);
            stats.put("nicked", true);
            stats.put("denicked", true);
        } else if (Boolean.TRUE.equals(existing.get("denicked"))) {
            stats.put("nicked", true);
            stats.put("denicked", true);
        }
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

        float scale = LazifyConfig.INSTANCE.getOverlayScale();
        boolean scaled = OverlayRenderer.pushScale(scale, startX, startY);
        try {
            renderOverlayContent();
        } finally {
            OverlayRenderer.popScale(scaled);
        }
    }

    private void renderOverlayContent() {
        boolean nerdify = OverlayTheme.isNerdify(LazifyConfig.INSTANCE.getOverlayTheme());
        boolean textShadow = !nerdify;
        int cellPad = nerdify ? OverlayTheme.cellPad() : 0;

        OverlayRenderer.drawRect(startX, startY, endX, endY, background);

        if (nerdify) {
            OverlayRenderer.drawBorderRect(startX, startY, endX, endY, borderColorRGB);
        } else {
            OverlayRenderer.drawLine2D(startX, startY, endX, startY, borderWidth, borderColorRGB);
            OverlayRenderer.drawLine2D(endX, startY, endX, endY, borderWidth, borderColorRGB);
            OverlayRenderer.drawLine2D(endX, endY, startX, startY + (endY - startY), borderWidth, borderColorRGB);
            OverlayRenderer.drawLine2D(startX, startY + (endY - startY), startX, startY, borderWidth, borderColorRGB);
        }

        int lineHeight = nerdify ? OverlayRenderer.nerdifyLineHeight()
                : OverlayRenderer.getFontHeight() + offsetY;
        int headerY = nerdify ? startY + OverlayTheme.headerPadTop() : startY + offsetY;

        // Column headers
        for (ColumnDef col : columns) {
            if (!col.isEnabled()) continue;
            int x = col.getPosition();
            String header = nerdify ? OverlayTheme.headerFor(col.getKey()) : col.getHeader();
            int hw = OverlayRenderer.getFontWidth(header);
            if (nerdify) {
                if (OverlayTheme.isNumericCol(col.getKey())) {
                    x += (col.getMaxwidth() - hw) / 2;
                } else {
                    x += cellPad;
                }
            } else if (!col.getKey().equals(PLAYER_KEY) && !col.getKey().equals(RANK_KEY)) {
                x += (col.getMaxwidth() - hw) / 2;
            }
            OverlayRenderer.drawString(header, x, headerY, columnTitles, textShadow);
        }

        int headerSepY = startY + OverlayTheme.headerPadTop() + OverlayRenderer.getFontHeight()
                + OverlayTheme.headerSeparatorGap();
        if (nerdify) {
            OverlayRenderer.drawRect(startX + 1, headerSepY, endX - 1, headerSepY + 1, borderColorRGB);
        }
        int y = nerdify ? headerSepY + OverlayTheme.rowGapAfterSeparator()
                : startY + lineHeight + 5;

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
                        if (key.equals(RANK_KEY)) {
                            statValue = ColorUtil.formatRankColumn(true, "");
                        } else if (!key.equals(PLAYER_KEY) && !key.equals(ENCOUNTERS_KEY)) {
                            if (key.equals(URCHIN_KEY) && ps.get(URCHIN_KEY) != null) {
                                statValue = ps.get(URCHIN_KEY);
                            } else if (key.equals(PING_KEY) && ps.get(PING_KEY) != null) {
                                statValue = ps.get(PING_KEY);
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
                    if (nerdify && !text.isEmpty()) {
                        text = OverlayTheme.styleCell(key, text, ps, uuid);
                    }
                    int    tw   = OverlayRenderer.getFontWidth(text);
                    if (key.equals(PLAYER_KEY)) {
                        tw = OverlayRenderer.getFontWidth(ColorUtil.strip(text));
                        while (tw > maxWidth && text.length() > 1) {
                            text = text.substring(0, text.length() - 1);
                            tw = OverlayRenderer.getFontWidth(ColorUtil.strip(text));
                        }
                    }
                    if (!nerdify && !key.equals(PLAYER_KEY) && !key.equals(RANK_KEY)) {
                        x += (maxWidth - OverlayRenderer.getFontWidth(text)) / 2;
                    } else if (nerdify) {
                        if (OverlayTheme.isNumericCol(key)) {
                            x += (maxWidth - tw) / 2;
                        } else {
                            x += cellPad;
                        }
                    }
                    OverlayRenderer.drawString(text, x, y, -1, textShadow);
                }
                y += lineHeight;
            }
        }
    }

    // ==========================================================================
    // Chat handling (mirrors original onChat)
    // ==========================================================================

    public boolean onChat(String message, String formatted) {
        String msg = ColorUtil.strip(message);
        if (formatted == null) formatted = message;

        // Add players who join the pre-game lobby (no status check — pattern is specific enough)
        {
            Matcher joinMatcher = LOBBY_JOIN.matcher(msg);
            if (joinMatcher.matches()) {
                inBwPregame = true;
                lobbyMaxPlayers = Integer.parseInt(joinMatcher.group(3));
                String name = joinMatcher.group(1);
                if (ColorUtil.hasObfuscationBefore(formatted, name)) {
                    debug("Lobby join skipped (obfuscated): " + name + " | lobby=" + currentLobby + " status=" + status);
                } else {
                    debug("Lobby join detected: " + name + " | lobby=" + currentLobby + " status=" + status);
                    addChatPlayer(name, currentLobby);
                }
            }
        }

        // Alternate pregame format: "+ (01/16) PlayerName" (used in some game modes)
        {
            Matcher listMatcher = PREGAME_LIST.matcher(msg);
            if (listMatcher.matches()) {
                inBwPregame = true;
                lobbyMaxPlayers = Integer.parseInt(listMatcher.group(2));
                String name = listMatcher.group(3);
                if (ColorUtil.hasObfuscationBefore(formatted, name)) {
                    debug("Pregame list skipped (obfuscated): " + name + " | lobby=" + currentLobby + " status=" + status);
                } else {
                    debug("Pregame list entry: " + name + " | lobby=" + currentLobby + " status=" + status);
                    addChatPlayer(name, currentLobby);
                }
            }
        }

        // Detect game start via "Protect your bed" message
        if (msg.contains("Protect your bed and destroy the enemy beds")) {
            inBwPregame = true;
            debug("Game start detected (Protect your bed) | lobby=" + currentLobby + " status=" + status);
        }

        // ── Party list parsing (/pl response) ─────────────────────────────────
        if (msg.equals("You are not currently in a party.")) {
            partyMembers.clear();
            parsingPartyList = false;
            debug("No party detected");
        }
        if (msg.startsWith("Party Leader:") || msg.startsWith("Party Members") || msg.startsWith("Party Moderators")) {
            if (msg.startsWith("Party Leader:")) {
                partyMembers.clear();
                debug("Parsing party list...");
            }
            parsingPartyList = true;
            parsePartyLine(msg);
        } else if (parsingPartyList && msg.startsWith("-----")) {
            parsingPartyList = false;
            debug("Party members: " + partyMembers.size() + " found");
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
                    if (ColorUtil.hasObfuscationBefore(displayName, npi.getGameProfile().getName())) {
                        debug("Who add skipped (obfuscated tab): " + name);
                        continue;
                    }
                    final String fu = uuid, fn = displayName, fl = currentLobby;
                    addPlaceholderStats(fu, fn, true);
                    Map<String, Object> keepData = new HashMap<>();
                    keepData.put(PREGAME_KEEP_KEY, true);
                    if (isNickedKey(fu)) {
                        keepData.put("nicked", true);
                        keepData.put("apinicked", "\u00a7eN");
                        keepData.put(URCHIN_KEY, "\u00a7bN");
                        keepData.put(RANK_KEY, ColorUtil.formatRankColumn(true, ""));
                    }
                    addToOverlay(fu, keepData);
                    addToPlayers(fu);
                    new Thread(() -> {
                        handlePlayerStats(fu, fl);
                        handlePlayerTags(fu, fl);
                        handleBordicPing(fu, fl);
                    }).start();
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
                        handlePlayerTags(fu, lobby);
                        handleBordicPing(fu, lobby);
                    }).start();
                }
            }

            if (!didwho) { didwho = true; }
            return !LazifyConfig.INSTANCE.isHideWho();
        }

        // Kill message detection from Bedwars chat
        if (inBwPregame || status >= 2) {
            KillMessageDetector.Match km = KillMessageDetector.detect(msg);
            if (km != null) {
                recordNickKillMessage(km);
            }
            recordNickStarFromLine(msg);
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
                if (ColorUtil.hasObfuscationBefore(formatted, sender)) {
                    debug("Chat sender skipped (obfuscated): " + sender + " | inBwPregame=" + inBwPregame + " status=" + status);
                } else {
                    debug("Chat sender detected: " + sender + " | inBwPregame=" + inBwPregame + " status=" + status);
                    addChatPlayer(sender, currentLobby);
                }
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
            if (ColorUtil.hasObfuscationBefore(displayName, npi.getGameProfile().getName())) {
                debug("addChatPlayer: " + name + " obfuscated in tab, skipping");
                return;
            }
            debug("addChatPlayer: " + name + " found in tab list, uuid=" + uuid);
            final String fu = uuid, fn = displayName, fl = lobby;
            addPlaceholderStats(fu, fn, true);
            if (markPregameKeep) {
                Map<String, Object> keepData = new HashMap<>();
                keepData.put(PREGAME_KEEP_KEY, true);
                addToOverlay(fu, keepData);
            }
            addToPlayers(fu);
            new Thread(() -> {
                handlePlayerStats(fu, fl);
                handlePlayerTags(fu, fl);
                handleBordicPing(fu, fl);
            }).start();
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
                    if (ColorUtil.hasObfuscationBefore(displayName, npi2.getGameProfile().getName())) {
                        debugFromThread("addChatPlayer: " + playerName + " obfuscated in tab, skipping");
                        return;
                    }
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
                    handlePlayerTags(fu, fl);
                    handleBordicPing(fu, fl);
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
                        nickData.put(RANK_KEY, ColorUtil.formatRankColumn(true, ""));
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
                handlePlayerTags(fu, fl);
                handleBordicPing(fu, fl);
            }).start();
        }
    }

    private void recordNickKillMessage(KillMessageDetector.Match km) {
        if (km.killer == null || km.killer.isEmpty()) return;

        String uuid = findUuidForUsername(km.killer);
        if (uuid == null) {
            debug("Nick killmsg: killer " + km.killer + " not in tab/overlay");
            return;
        }
        if (!isNickedPlayer(uuid)) {
            return;
        }

        Map<String, Object> existing = overlayPlayers.get(uuid);
        if ("stat".equals(km.packageId) && existing != null) {
            Object knownId = existing.get(KILLMSG_ID_KEY);
            if (knownId instanceof String && !"stat".equals(knownId)) {
                debug("Nick killmsg: skipping ambiguous stat line for " + km.killer
                        + " (already " + knownId + ")");
                if (km.observedFinals == null && km.observedBeds == null) return;
            }
        }

        Map<String, Object> data = new ConcurrentHashMap<>();
        if (km.packageId != null && !km.packageId.isEmpty()) {
            boolean keepExistingPkg = existing != null
                    && "stat".equals(km.packageId)
                    && existing.get(KILLMSG_ID_KEY) instanceof String
                    && !"stat".equals(existing.get(KILLMSG_ID_KEY));
            if (!keepExistingPkg) {
                data.put(KILLMSG_ID_KEY, km.packageId);
            }
        }
        if (km.observedFinals != null) {
            data.put(NICK_FINALS_KEY, km.observedFinals);
        }
        if (km.observedBeds != null) {
            data.put(NICK_BEDS_KEY, km.observedBeds);
        }
        addToOverlay(uuid, data);
        debug("Nick killmsg: " + km.killer + " pkg=" + km.packageId
                + " finals=" + km.observedFinals + " beds=" + km.observedBeds);

        trySuperstarDenick(uuid, km.killer, currentLobby);
    }

    private void recordNickStarFromLine(String strippedChat) {
        if (strippedChat == null || strippedChat.isEmpty()) return;
        int colonIdx = strippedChat.lastIndexOf(':');
        if (colonIdx <= 0) return;

        String beforeColon = strippedChat.substring(0, colonIdx).trim();
        if (beforeColon.isEmpty()) return;

        int lastSpace = beforeColon.lastIndexOf(' ');
        String username = lastSpace >= 0 ? beforeColon.substring(lastSpace + 1) : beforeColon;
        if (!username.matches("[\\w+]{1,16}")) return;

        recordNickStar(username, strippedChat);
    }

    private void recordNickStar(String username, String strippedChat) {
        Integer star = StarChatDetector.extract(strippedChat, username);
        if (star == null || star <= 0) return;

        String uuid = findUuidForUsername(username);
        if (uuid == null) {
            debug("Nick star: " + username + " star=" + star + " (not in tab/overlay)");
            return;
        }
        if (!isNickedPlayer(uuid)) return;

        Map<String, Object> existing = overlayPlayers.get(uuid);
        Object known = existing != null ? existing.get(NICK_STAR_KEY) : null;
        if (known instanceof Number && ((Number) known).intValue() == star) return;

        Map<String, Object> data = new ConcurrentHashMap<>();
        data.put(NICK_STAR_KEY, star);
        addToOverlay(uuid, data);
        debug("Nick star: " + username + " star=" + star);
        trySuperstarDenick(uuid, username, currentLobby);
    }

    /** Called from BlockEvent.PlaceEvent when available (local placement). */
    public void onBlockPlaced(EntityPlayer player, IBlockState state, BlockPos pos) {
        if (player == null) return;
        if (!WoodSkinUtil.isHoldingWood(player)) return;
        String woodId = WoodSkinUtil.fromItemStack(player.getHeldItem());
        if (woodId == null) return;

        String uuid = player.getUniqueID().toString().replace("-", "");
        observeWoodHeld(uuid, player.getName(), woodId);
    }

    private boolean shouldScanWoodPlacements() {
        if (LazifyConfig.INSTANCE.isDebug()) return true;
        return !bordicKey().isEmpty() && (status >= 2 || inBwPregame);
    }

    private void scanNickWoodPlacements(Minecraft mc) {
        World world = mc.theWorld;
        if (world == null) return;

        boolean debugWood = LazifyConfig.INSTANCE.isDebug();

        for (EntityPlayer player : world.playerEntities) {
            if (!debugWood && player == mc.thePlayer) continue;

            String uuid = player.getUniqueID().toString().replace("-", "");
            boolean nicked = isNickedPlayer(uuid);
            if (!debugWood && (!nicked || !isInOverlay(uuid))) continue;

            if (nicked && isInOverlay(uuid)) {
                Map<String, Object> data = overlayPlayers.get(uuid);
                if (data != null && data.containsKey(NICK_WOOD_KEY)) continue;
            }

            if (!WoodSkinUtil.isHoldingWood(player)) continue;

            String woodId = WoodSkinUtil.fromItemStack(player.getHeldItem());
            if (woodId == null) continue;

            observeWoodHeld(uuid, player.getName(), woodId);
        }
    }

    private void observeWoodHeld(String uuid, String name, String woodId) {
        boolean nicked = isNickedPlayer(uuid);
        if (LazifyConfig.INSTANCE.isDebug()) {
            debug("Wood detect [held]: " + name + " nicked=" + nicked + " wood=" + woodId);
        }
        if (!nicked) return;
        recordNickWood(uuid, name, woodId);
    }

    private void recordNickWood(String uuid, String name, String woodId) {
        Map<String, Object> existing = overlayPlayers.get(uuid);
        if (existing != null && existing.containsKey(NICK_WOOD_KEY)) return;

        Map<String, Object> data = new ConcurrentHashMap<>();
        data.put(NICK_WOOD_KEY, woodId);
        addToOverlay(uuid, data);
        debug("Nick wood stored: " + name + " wood=" + woodId);
        trySuperstarDenick(uuid, name, currentLobby);
    }

    private boolean isNickedPlayer(String uuid) {
        if (isNickedKey(uuid)) return true;
        Map<String, Object> data = overlayPlayers.get(uuid);
        return data != null && Boolean.TRUE.equals(data.get("nicked"));
    }

    private void trySuperstarDenick(String nickUuid, String nickName, String lobby) {
        if (bordicKey().isEmpty()) return;

        Map<String, Object> data = overlayPlayers.get(nickUuid);
        if (data == null) return;
        if (data.containsKey("superstarDenicked")) return;

        Object pkgObj = data.get(KILLMSG_ID_KEY);
        Object finalsObj = data.get(NICK_FINALS_KEY);
        Object bedsObj = data.get(NICK_BEDS_KEY);
        if (!(pkgObj instanceof String) || !(finalsObj instanceof Number) || !(bedsObj instanceof Number)) {
            return;
        }

        final String packageId = (String) pkgObj;
        final int finals = ((Number) finalsObj).intValue();
        final int beds = ((Number) bedsObj).intValue();
        Object woodObj = data.get(NICK_WOOD_KEY);
        final String woodType = woodObj instanceof String ? (String) woodObj : null;
        Object starObj = data.get(NICK_STAR_KEY);
        final Integer star = starObj instanceof Number ? ((Number) starObj).intValue() : null;
        final String fUuid = nickUuid, fName = nickName, fLobby = lobby;

        new Thread(() -> handleSuperstarDenick(fUuid, fName, fLobby, packageId, finals, beds, woodType, star)).start();
    }

    private void handleSuperstarDenick(String nickUuid, String nickName, String lobby,
                                       String packageId, int finals, int beds, String woodType,
                                       Integer star) {
        try {
            List<BordicSuperstar.Entry> list = BordicSuperstar.fetch(bordicKey());
            debugFromThread("Superstar denick: searching " + list.size() + " MVP++ players for "
                    + nickName + " pkg=" + packageId + " finals=" + finals + " beds=" + beds
                    + " star=" + star + " wood=" + woodType);
            BordicSuperstar.MatchResult match = BordicSuperstar.findBestMatch(
                    list, packageId, finals, beds, woodType, star);
            if (match == null) {
                debugFromThread("Superstar denick: no match for " + nickName);
                return;
            }

            BordicSuperstar.Entry entry = match.entry;
            debugFromThread("Superstar denick: " + nickName + " -> " + entry.name
                    + " (score=" + match.score + " killMsg=" + entry.killMessage
                    + " wood=" + entry.activeWoodType + ")");

            if (!isInOverlay(nickUuid) || !currentLobby.equals(lobby)) return;

            applyDenick(nickUuid, entry.uuid, nickName, entry.name, lobby, "superstar");
            Map<String, Object> mark = new ConcurrentHashMap<>();
            mark.put("superstarDenicked", true);
            addToOverlay(nickUuid, mark);
        } catch (Exception e) {
            debugFromThread("Superstar denick exception for " + nickName + ": " + e.getMessage());
        }
    }

    private String findUuidForUsername(String name) {
        for (Map.Entry<String, String> entry : uuidToName.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(name)) return entry.getKey();
        }
        synchronized (currentPlayers) {
            for (Map.Entry<String, Map<String, Object>> entry : overlayPlayers.entrySet()) {
                if (matchesPlayerName(entry.getValue(), name)) return entry.getKey();
            }
        }
        NetworkPlayerInfo npi = findTabPlayer(name);
        if (npi != null) return npi.getGameProfile().getId().toString().replace("-", "");
        return null;
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
        dodgeWarned = false;
        teamFkdrSent = false;
        teamThreatSent = false;
        partySent = false;
        parsingPartyList = false;
        currentBwMode = -1;
        lobbyMaxPlayers = -1;
        overlayTicks = 0;
        clearMaps();
    }

    // ==========================================================================
    // Stats fetching (async)
    // ==========================================================================

    private void handlePlayerStats(String overlayUuid, String lobby) {
        String fetchUuid = statsUuidFor(overlayUuid);

        Map<String, Object> cached = statsCache.get(fetchUuid);
        if (cached != null) {
            long cacheTime = cached.containsKey("cachetime") ? (long)(Object)cached.get("cachetime") : 0L;
            if (System.currentTimeMillis() < cacheTime) {
                debugFromThread("Stats cache hit for " + fetchUuid + " (overlay " + overlayUuid + ")");
                if (isInOverlay(overlayUuid) && currentLobby.equals(lobby)) {
                    Map<String, Object> copy = new ConcurrentHashMap<>(cached);
                    preserveDenickDisplay(overlayUuid, copy);
                    addToOverlay(overlayUuid, copy);
                }
                return;
            }
            debugFromThread("Stats cache expired for " + fetchUuid);
            statsCache.remove(fetchUuid);
        }

        if (isNickedKey(overlayUuid) && !nickRealUuid.containsKey(overlayUuid)) {
            String username = uuidToName.get(overlayUuid);
            if (username == null || username.isEmpty()) {
                debugFromThread("No username mapped for " + overlayUuid + ", skipping stats fetch");
                return;
            }
            debugFromThread("Offline UUID nick for " + username);
            Map<String, Object> nickedStats = buildApiNickedStats(username);
            if (isInOverlay(overlayUuid) && currentLobby.equals(lobby)) addToOverlay(overlayUuid, nickedStats);
            return;
        }

        String username = uuidToName.getOrDefault(overlayUuid, uuidToName.get(fetchUuid));
        if (username == null || username.isEmpty()) {
            debugFromThread("No username mapped for " + overlayUuid + ", skipping stats fetch");
            return;
        }

        Map<String, Object> playerStats = new ConcurrentHashMap<>();
        try {
            debugFromThread("Fetching stats for " + username + " (fetch " + fetchUuid + ", overlay " + overlayUuid + ")");
            StatsProvider.Result result = StatsProvider.fetch(fetchUuid, hypixelKey(), bordicKey());
            debugFromThread("Stats response: provider=" + result.provider
                    + " HTTP " + result.code + " for " + username
                    + (result.nickDebug != null ? " nick=" + result.nickDebug : ""));
            if (result.nicked && !nickRealUuid.containsKey(overlayUuid)) {
                debugFromThread("API confirmed nick for " + username);
                playerStats = buildApiNickedStats(username);
            } else if (result.data != null && result.code == 200) {
                playerStats = parseStats(result.data, fetchUuid);
                preserveDenickDisplay(overlayUuid, playerStats);
            } else if (result.code == 502) {
                printFromThread(PREFIX + "\u00a7eAll stat providers failed for \u00a73" + username + "\u00a7e.");
                playerStats.put("error", true);
            } else {
                printFromThread(PREFIX + "\u00a7eHTTP Error \u00a73" + result.code
                        + " \u00a7ewhile getting stats.");
                playerStats.put("error", true);
            }
        } catch (Exception e) {
            debugFromThread("Stats API exception for " + username + ": " + e.getMessage());
            printFromThread(PREFIX + "\u00a7eRuntime error while getting stats.");
            playerStats.put("error", true);
        }

        if (isInOverlay(overlayUuid) && currentLobby.equals(lobby)) addToOverlay(overlayUuid, playerStats);
    }

    private Map<String, Object> buildApiNickedStats(String username) {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("nicked", true);
        stats.put("apinicked", "\u00a7eN");
        stats.put(URCHIN_KEY, "\u00a7bN");
        stats.put("username", username);
        stats.put(PLAYER_KEY, username);
        stats.put(RANK_KEY, ColorUtil.formatRankColumn(true, ""));
        return stats;
    }

    private Map<String, Object> parseStats(JsonWrapper jsonData, String uuid) {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        try {
            JsonWrapper data = jsonData.object();

            JsonWrapper network = data.object("network");
            JsonWrapper bw      = data.object("bedwars");
            JsonWrapper overall  = bw.object("overall");

            String username = data.get("name", data.get("username", ""));
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
            stats.put("rankStr",    rankStr);
            boolean nickedRow = Boolean.TRUE.equals(
                    overlayPlayers.getOrDefault(uuid, Collections.<String, Object>emptyMap()).get("nicked"));
            stats.put(RANK_KEY, ColorUtil.formatRankColumn(nickedRow, rankStr));

            if (teams.containsKey(uuid) && showTeamColors) {
                String existing = (String) overlayPlayers.getOrDefault(uuid, Collections.<String, Object>emptyMap()).get(PLAYER_KEY);
                if (existing != null) {
                    if (showRanks && !rankPrefix.equals("\u00a77") && !rankPrefix.isEmpty()) {
                        stats.put(PLAYER_KEY, rankPrefix + " " + existing);
                    } else {
                        stats.put(PLAYER_KEY, existing);
                    }
                }
            } else {
                String coloredUsername = rankColor + username;
                if (showRanks && !rankPrefix.equals("\u00a77") && !rankPrefix.isEmpty()) {
                    stats.put(PLAYER_KEY, rankPrefix + " " + coloredUsername);
                } else {
                    stats.put(PLAYER_KEY, coloredUsername);
                }
            }

            // Language
            String language = network.exists() ? network.get("language", "ENGLISH") : "ENGLISH";
            if (!language.equals("ENGLISH")) stats.put("language", "\u00a73L");

            // Star
            int star = (int) Double.parseDouble(overall.get("stars", overall.get("level", "0")));
            stats.put(STAR_KEY,   ColorUtil.getPrestigeColor(star));
            stats.put(STAR_VALUE, (double) star);

            // Network level
            int networkLevel = network.exists() ? (int) Double.parseDouble(network.get("level", "0")) : 0;
            stats.put(LEVEL_KEY, networkLevel > 0 ? "§7" + networkLevel : "§7-");

            // FKDR
            double fkdr = Double.parseDouble(overall.get("fkdr", "0"));
            double finalKills  = Double.parseDouble(overall.get("final_kills", "0"));
            double finalDeaths = Double.parseDouble(overall.get("final_deaths", "0"));
            if (finalDeaths == 0 && fkdr == 0 && finalKills == 0) stats.put("nofinaldeaths", "\u00a75Z");
            if (fkdr == 0 && finalDeaths > 0) fkdr = finalKills / finalDeaths;
            fkdr = fkdr < 10 ? ColorUtil.round(fkdr, 2) : ColorUtil.round(fkdr, 1);
            double index = star * Math.pow(fkdr, 2);
            String fkdrStr = ColorUtil.formatDoubleStr(fkdr);
            stats.put(FKDR_KEY,   LazifyConfig.INSTANCE.isFkdrColors()
                    ? ColorUtil.getFkdrColor(fkdrStr, LazifyConfig.INSTANCE.getFkdrColors())
                    : "\u00a7f" + fkdrStr);
            stats.put(FKDR_VALUE, fkdr);
            stats.put(INDEX_VALUE, index);

            // Session
            long lastLogin  = parseEpochMs(network.exists() ? network.get("last_login",  network.get("lastLogin",  "0")) : "0");
            long lastLogout = parseEpochMs(network.exists() ? network.get("last_logout", network.get("lastLogout", "0")) : "0");
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

            // Winstreak — fall back to Bordic when Hypixel winstreak API is disabled
            JsonWrapper modes = bw.object("modes");
            int overallWs = 0;
            boolean hypixelWs = overall.get("winstreak").exists();
            if (hypixelWs) {
                overallWs = (int) Double.parseDouble(overall.get("winstreak", "0"));
            }

            int soloWs = modeWinstreak(modes, "solo");
            int doublesWs = modeWinstreak(modes, "doubles");
            int threesWs = modeWinstreak(modes, "threes");
            int foursWs = modeWinstreak(modes, "fours");
            int fourV4Ws = modeWinstreak(modes, "4v4");

            if (!hypixelWs || overallWs == 0) {
                debugFromThread("Winstreak API " + (hypixelWs ? "zero" : "off") + " for " + uuid + ", fetching from Bordic");
                BordicWinstreaks bordic = fetchBordicWinstreaks(uuid);
                if (bordic != null) {
                    if (!hypixelWs || overallWs == 0) overallWs = bordic.overall;
                    if (!hypixelWs || soloWs == 0) soloWs = bordic.solo;
                    if (!hypixelWs || doublesWs == 0) doublesWs = bordic.doubles;
                    if (!hypixelWs || threesWs == 0) threesWs = bordic.threes;
                    if (!hypixelWs || foursWs == 0) foursWs = bordic.fours;
                    if (!hypixelWs || fourV4Ws == 0) fourV4Ws = bordic.fourV4;
                }
            }

            storeWinstreakValues(stats, overallWs, soloWs, doublesWs, threesWs, foursWs, fourV4Ws);
            int displayWs = (int) getDoubleStat(stats, WINSTREAK_VALUE);
            boolean highWS = displayWs > 50 || overallWs > 50;
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

    private static long parseEpochMs(String raw) {
        if (raw == null || raw.isEmpty()) return 0L;
        try {
            return (long) Double.parseDouble(raw);
        } catch (Exception e) {
            return 0L;
        }
    }

    private static int modeWinstreak(JsonWrapper modes, String mode) {
        if (!modes.exists()) return 0;
        JsonWrapper m = modes.object(mode);
        if (!m.exists()) return 0;
        JsonWrapper ws = m.get("winstreak");
        if (!ws.exists()) return 0;
        return ws.asInt(0);
    }

    private void storeWinstreakValues(Map<String, Object> stats, int overall, int solo, int doubles,
                                      int threes, int fours, int fourV4) {
        stats.put(WS_OVERALL_VALUE, (double) overall);
        stats.put(WS_MODE1_VALUE, (double) solo);
        stats.put(WS_MODE2_VALUE, (double) doubles);
        stats.put(WS_MODE3_VALUE, (double) threes);
        stats.put(WS_MODE4_VALUE, (double) fours);
        stats.put(WS_MODE5_VALUE, (double) fourV4);
        applyWinstreakDisplay(stats);
    }

    private void refreshWinstreakDisplays() {
        for (Map<String, Object> data : overlayPlayers.values()) {
            if (data.containsKey(WS_OVERALL_VALUE)) {
                applyWinstreakDisplay(data);
            }
        }
    }

    private void applyWinstreakDisplay(Map<String, Object> stats) {
        int overall = (int) getDoubleStat(stats, WS_OVERALL_VALUE);
        int displayValue = overall;
        String suffix = "";

        if (currentBwMode >= 1 && currentBwMode <= 5) {
            int modeWs = winstreakForMode(stats, currentBwMode);
            if (modeWs > overall) {
                displayValue = modeWs;
                suffix = winstreakSuffix(currentBwMode);
            }
        }

        String plain = suffix.isEmpty() ? String.valueOf(displayValue) : displayValue + " " + suffix;
        stats.put(WINSTREAK_KEY, ColorUtil.getWinstreakColor(plain));
        stats.put(WINSTREAK_VALUE, (double) displayValue);
    }

    private int winstreakForMode(Map<String, Object> stats, int mode) {
        switch (mode) {
            case 1: return (int) getDoubleStat(stats, WS_MODE1_VALUE);
            case 2: return (int) getDoubleStat(stats, WS_MODE2_VALUE);
            case 3: return (int) getDoubleStat(stats, WS_MODE3_VALUE);
            case 4: return (int) getDoubleStat(stats, WS_MODE4_VALUE);
            case 5: return (int) getDoubleStat(stats, WS_MODE5_VALUE);
            default: return (int) getDoubleStat(stats, WS_OVERALL_VALUE);
        }
    }

    private static String winstreakSuffix(int mode) {
        switch (mode) {
            case 1: return "1s";
            case 2: return "2s";
            case 3: return "3s";
            case 4: return "4s";
            case 5: return "4v4";
            default: return "";
        }
    }

    private static String parseWinstreakMode(int i) {
        switch (i) {
            case 1: return "solo";
            case 2: return "doubles";
            case 3: return "threes";
            case 4: return "fours";
            case 5: return "4v4";
            default: return "";
        }
    }

    private static final class BordicWinstreaks {
        final int overall;
        final int solo;
        final int doubles;
        final int threes;
        final int fours;
        final int fourV4;

        BordicWinstreaks(int overall, int solo, int doubles, int threes, int fours, int fourV4) {
            this.overall = overall;
            this.solo = solo;
            this.doubles = doubles;
            this.threes = threes;
            this.fours = fours;
            this.fourV4 = fourV4;
        }
    }

    private static BordicWinstreaks parseBordicWinstreaks(JsonWrapper data) {
        return new BordicWinstreaks(
                data.get("winstreak").asInt(0),
                data.get("eight_one_winstreak").asInt(0),
                data.get("eight_two_winstreak").asInt(0),
                data.get("four_three_winstreak").asInt(0),
                data.get("four_four_winstreak").asInt(0),
                data.get("two_four_winstreak").asInt(0));
    }

    private static final String BORDIC_WINSTREAK_URL = "https://bordic.xyz/api/v2/resources/winstreak";

    private BordicWinstreaks fetchBordicWinstreaks(String uuid) {
        String undashed = uuid == null ? null : uuid.replace("-", "");
        if (undashed == null || undashed.length() != 32) return null;
        try {
            String url = BORDIC_WINSTREAK_URL + "?uuid=" + undashed;
            debugFromThread("Fetching Bordic winstreak for " + undashed);
            Object[] res = HttpUtil.get(url, 3000);
            int code = (int) res[1];
            debugFromThread("Bordic winstreak response: HTTP " + code + " for " + uuid);
            if (code != 200) return null;
            JsonWrapper root = (JsonWrapper) res[0];
            if (!root.get("success").asBoolean(false)) return null;
            JsonWrapper data = root.object("data");
            if (!data.exists()) return null;
            BordicWinstreaks ws = parseBordicWinstreaks(data);
            debugFromThread("Bordic winstreak parsed: overall=" + ws.overall
                    + " 1s=" + ws.solo + " 2s=" + ws.doubles + " 3s=" + ws.threes
                    + " 4s=" + ws.fours + " 4v4=" + ws.fourV4);
            return ws;
        } catch (Exception e) {
            debugFromThread("Bordic winstreak exception for " + uuid + ": " + e.getMessage());
            return null;
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
    // Urchin + Seraph tag fetching (async)
    // ==========================================================================

    private static final String SERAPH_BLACKLIST_URL = "https://api.seraph.si/";
    private final ConcurrentHashMap<String, Object> tagFetchLocks = new ConcurrentHashMap<>();

    private void handlePlayerTags(String overlayUuid, String lobby) {
        String tagUuid = statsUuidFor(overlayUuid);
        Object lock = tagFetchLocks.computeIfAbsent(tagUuid, k -> new Object());
        synchronized (lock) {
            if (tagCache.containsKey(tagUuid)) {
                debugFromThread("Tag cache hit for " + tagUuid);
                TagInfo cached = tagCache.get(tagUuid);
                if (cached != null && cached.hasAnyTag() && isInOverlay(overlayUuid) && currentLobby.equals(lobby)) {
                    Map<String, Object> tagData = new ConcurrentHashMap<>();
                    tagData.put(URCHIN_KEY, cached.overlayDisplay());
                    addToOverlay(overlayUuid, tagData);
                }
                return;
            }
            boolean hasUrchinKey = urchinKey() != null && !urchinKey().isEmpty();
            boolean hasSeraphKey = seraphKey() != null && !seraphKey().isEmpty();
            if (!hasUrchinKey && !hasSeraphKey) {
                debugFromThread("Tags skipped for " + tagUuid + " (no Urchin or Seraph key set)");
                return;
            }

            TagInfo info = new TagInfo();

            if (hasUrchinKey) {
                fetchUrchinTag(tagUuid, info);
            }
            if (hasSeraphKey) {
                fetchSeraphTag(tagUuid, info);
            }

            tagCache.put(tagUuid, info);

            if (!info.hasAnyTag()) return;

            debugFromThread("Tags for " + tagUuid + " (overlay " + overlayUuid + "): urchin="
                    + info.hasUrchin + " seraph=" + info.hasSeraph);

            String username = uuidToName.getOrDefault(overlayUuid, uuidToName.getOrDefault(tagUuid, tagUuid));
            Map<String, Object> tagData = new ConcurrentHashMap<>();
            tagData.put(URCHIN_KEY, info.overlayDisplay());
            if (isInOverlay(overlayUuid) && currentLobby.equals(lobby)) addToOverlay(overlayUuid, tagData);

            if (LazifyConfig.INSTANCE.isSendUrchinReasonToChat()) {
                if (info.hasUrchin) {
                    notifyTagInChat(username, "Urchin", info.urchinType, info.urchinReason);
                }
                if (info.hasSeraph) {
                    notifyTagInChat(username, "Seraph", info.seraphType, info.seraphReason);
                }
            }
        }
    }

    private void fetchUrchinTag(String uuid, TagInfo info) {
        try {
            String url = "https://urchin.ws/player/" + uuid + "?key=" + urchinKey() + "&sources=GAME";
            debugFromThread("Fetching Urchin tag for " + uuid);
            Object[] res = HttpUtil.get(url, 3000);
            debugFromThread("Urchin API response: HTTP " + (int) res[1] + " for " + uuid);
            if ((int) res[1] != 200) return;

            JsonWrapper json = (JsonWrapper) res[0];
            List<JsonWrapper> tagsArray = json.object().array("tags");
            if (tagsArray == null || tagsArray.isEmpty()) return;

            JsonWrapper firstTag = tagsArray.get(0);
            String tagType = firstTag.object().get("type", "");
            String reason = firstTag.object().get("reason", "");
            if (tagType.isEmpty()) return;

            info.hasUrchin = true;
            info.urchinType = tagType;
            info.urchinReason = reason;
            debugFromThread("Urchin tag for " + uuid + ": type=" + tagType + " reason=" + reason);
        } catch (Exception ignored) {}
    }

    private void fetchSeraphTag(String uuid, TagInfo info) {
        try {
            String url = SERAPH_BLACKLIST_URL + uuid + "/blacklist";
            Map<String, String> headers = new HashMap<>();
            headers.put("seraph-api-key", seraphKey());
            debugFromThread("Fetching Seraph tag for " + uuid + " -> " + url);
            Object[] res = HttpUtil.get(url, 3000, headers);
            debugFromThread("Seraph API response: HTTP " + (int) res[1] + " for " + uuid);
            if ((int) res[1] != 200) return;

            JsonWrapper root = (JsonWrapper) res[0];
            if (!root.get("success").asBoolean(false)) return;

            JsonWrapper blacklist = root.object("data").object("blacklist");
            if (!blacklist.get("tagged").asBoolean(false)) return;

            String reportType = blacklist.get("report_type", "");
            String reason = blacklist.get("reason", "");
            if (reason.isEmpty()) reason = blacklist.get("tooltip", "");
            if (reportType.isEmpty() && reason.isEmpty()) return;

            info.hasSeraph = true;
            info.seraphType = reportType.isEmpty() ? "blacklisted" : reportType;
            info.seraphReason = reason;
            debugFromThread("Seraph tag for " + uuid + ": type=" + info.seraphType + " reason=" + reason);
        } catch (Exception ignored) {}
    }

    private void notifyTagInChat(String username, String source, String tagType, String reason) {
        String formattedType = TagInfo.formatSourceType(tagType);
        StringBuilder msg = new StringBuilder(PREFIX)
                .append("\u00a7c").append(username)
                .append(" \u00a7eis tagged as \u00a73").append(formattedType)
                .append(" \u00a7e(").append(source).append(")");
        if (reason != null && !reason.isEmpty()) {
            msg.append(" for: \u00a73").append(reason);
        }
        printFromThread(msg.toString());
    }

    private static final String BORDIC_PING_URL = "https://bordic.xyz/api/v2/resources/ping";

    private void handleBordicPing(String overlayUuid, String lobby) {
        if (!LazifyConfig.INSTANCE.isColPing()) return;
        String fetchUuid = statsUuidFor(overlayUuid);
        if (!isV4UndashedUuid(fetchUuid)) {
            debugFromThread("Bordic ping skipped for " + fetchUuid + " (not v4 UUID)");
            return;
        }
        if (pingCache.containsKey(fetchUuid)) {
            debugFromThread("Ping cache hit for " + fetchUuid);
            Map<String, Object> cached = new ConcurrentHashMap<>();
            cached.put(PING_KEY, ColorUtil.getPingColor(pingCache.get(fetchUuid)));
            if (isInOverlay(overlayUuid) && currentLobby.equals(lobby)) addToOverlay(overlayUuid, cached);
            return;
        }
        try {
            String url = BORDIC_PING_URL + "?uuid=" + fetchUuid;
            debugFromThread("Fetching Bordic ping for " + fetchUuid);
            Object[] res = HttpUtil.get(url, 3000);
            int code = (int) res[1];
            debugFromThread("Bordic ping response: HTTP " + code + " for " + fetchUuid);
            if (code != 200) return;
            JsonWrapper root = (JsonWrapper) res[0];
            if (!root.get("success").asBoolean(false)) return;
            List<JsonWrapper> entries = root.array("data");
            if (entries.isEmpty()) return;
            int avg = entries.get(0).object().get("avg").asInt(-1);
            if (avg < 0) return;
            pingCache.put(fetchUuid, avg);
            Map<String, Object> pingData = new ConcurrentHashMap<>();
            pingData.put(PING_KEY, ColorUtil.getPingColor(avg));
            if (isInOverlay(overlayUuid) && currentLobby.equals(lobby)) addToOverlay(overlayUuid, pingData);
        } catch (Exception e) {
            debugFromThread("Bordic ping exception for " + fetchUuid + ": " + e.getMessage());
        }
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
        "teams","teamprefix","showyourself","showranks","removefinalkill","autotablist","clearonwho","middleclickshop","skindenick",
        "fkdrcolors","autowho","whodelay","hidewho","dodgewarning","dodgethreshold","teamthreatchat","teamthreatthreshold",
        "threatfkdrweight","threatstarweight","threatwinstreakweight","threaturchinweight","threatteamsizeweight",
        "threatencounterweight","threatnickweight","nohurtcam","antidebuff","teamfkdrchat",
        "sendnicked","sendurchinreason","keybindhold","showontab","overlayovertab","keybind",
        "debug","col","sortby","sortmode","winstreak","enctimeout",
        "x","y","colgap","rowgap","scale","bgopacity","bghue","headerhue","borderhue","overlaytheme"
    };
    public static final String[] ALL_COLUMNS = {
        "encounters","username","rank","star","fkdr","winstreaks","urchin","session","level","ping"
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
                            m.put(RANK_KEY, ColorUtil.formatRankColumn(true, ""));
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
                            statsCache.remove(fu); tagCache.remove(fu); pingCache.remove(fu);
                            new Thread(() -> {
                                handlePlayerStats(fu, fl);
                                handlePlayerTags(fu, fl);
                                handleBordicPing(fu, fl);
                            }).start();
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
                    new Thread(() -> {
                        handlePlayerStats(uuid, fl);
                        handlePlayerTags(uuid, fl);
                        handleBordicPing(uuid, fl);
                    }).start();
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
                print(PREFIX + "\u00a7dTag legend");
                print(PREFIX + "\u00a7e[N] \u00a77Nicked player");
                print(PREFIX + "\u00a7e[NICK] \u00a77Rank column \u00a77| \u00a77[NON] \u00a77No rank");
                print(PREFIX + "\u00a77\u2014 \u00a7dUrchin \u00a77\u2014");
                print(PREFIX + "\u00a7c[C] \u00a77Confirmed Cheater");
                print(PREFIX + "\u00a7e[CC] \u00a77Closet Cheater");
                print(PREFIX + "\u00a7c[BC] \u00a77Blatant Cheater");
                print(PREFIX + "\u00a74[S] \u00a77Sniper");
                print(PREFIX + "\u00a77\u2014 \u00a7dSeraph \u00a77\u2014");
                print(PREFIX + "\u00a7c[BL] \u00a77Blacklisted / unknown type");
                print(PREFIX + "\u00a77Uses same \u00a7c[C]\u00a77/\u00a7e[CC]\u00a77/\u00a7c[BC]\u00a77/\u00a74[S] \u00a77when applicable");
                print(PREFIX + "\u00a77\u2014 \u00a7dBoth \u00a77\u2014");
                print(PREFIX + "\u00a77+ \u00a77Shown when tagged on Urchin and Seraph");
                return;

            case "tag":
                if (args.length < 2) { print(PREFIX + "\u00a7eUsage: \u00a73/ov tag <username>"); return; }
                if ((urchinKey() == null || urchinKey().isEmpty())
                        && (seraphKey() == null || seraphKey().isEmpty())) {
                    print(PREFIX + "\u00a7cNo tag keys set. Use \u00a73/ov key urchin <key>\u00a7c or \u00a73/ov key seraph <key>"); return;
                }
                final String tagPlayer = args[1];
                new Thread(() -> {
                    String uuid = null;
                    for (Map.Entry<String, String> entry : uuidToName.entrySet()) {
                        if (entry.getValue().equalsIgnoreCase(tagPlayer)) {
                            uuid = entry.getKey(); break;
                        }
                    }
                    if (uuid == null) {
                        String[] conv = convertPlayer(tagPlayer);
                        uuid = conv[0];
                        if (uuid == null || uuid.isEmpty()) {
                            String[] conv2 = convertPlayerPlayerdb(tagPlayer);
                            uuid = conv2[0];
                        }
                    }
                    if (uuid == null || uuid.isEmpty()) {
                        printFromThread(PREFIX + "\u00a7cCould not resolve UUID for \u00a73" + tagPlayer + "\u00a7c.");
                        return;
                    }

                    TagInfo info = new TagInfo();
                    if (urchinKey() != null && !urchinKey().isEmpty()) fetchUrchinTag(uuid, info);
                    if (seraphKey() != null && !seraphKey().isEmpty()) fetchSeraphTag(uuid, info);

                    if (!info.hasAnyTag()) {
                        printFromThread(PREFIX + "\u00a73" + tagPlayer + "\u00a7e has no Urchin or Seraph tags.");
                        return;
                    }

                    if (info.hasUrchin) {
                        printFromThread(PREFIX + "\u00a77\u2500\u2500\u2500 \u00a7dUrchin: \u00a73" + tagPlayer + " \u00a77\u2500\u2500\u2500");
                        String formatted = TagInfo.formatSourceType(info.urchinType);
                        String color = info.urchinType.contains("cheater") ? "\u00a7c" : "\u00a7e";
                        if (info.urchinReason.isEmpty()) {
                            printFromThread(PREFIX + color + formatted);
                        } else {
                            printFromThread(PREFIX + color + formatted + " \u00a77- \u00a7f" + info.urchinReason);
                        }
                    }
                    if (info.hasSeraph) {
                        printFromThread(PREFIX + "\u00a77\u2500\u2500\u2500 \u00a7dSeraph: \u00a73" + tagPlayer + " \u00a77\u2500\u2500\u2500");
                        String formatted = TagInfo.formatSourceType(info.seraphType);
                        String color = info.seraphType.toLowerCase().contains("cheater") ? "\u00a7c" : "\u00a7e";
                        if (info.seraphReason.isEmpty()) {
                            printFromThread(PREFIX + color + formatted);
                        } else {
                            printFromThread(PREFIX + color + formatted + " \u00a77- \u00a7f" + info.seraphReason);
                        }
                    }
                }).start();
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
                if (args.length < 3) { print(PREFIX + "\u00a7eUsage: \u00a73/ov key <urchin|seraph|bordic|hypixel> <key>"); return; }
                String keyType = args[1].toLowerCase();
                if (keyType.equals("urchin")) {
                    LazifyConfig.INSTANCE.setUrchinKey(args[2]); LazifyConfig.INSTANCE.save();
                    refreshOverlayTags();
                    print(PREFIX + "\u00a7eUrchin API key saved.");
                } else if (keyType.equals("seraph")) {
                    LazifyConfig.INSTANCE.setSeraphKey(args[2]); LazifyConfig.INSTANCE.save();
                    refreshOverlayTags();
                    print(PREFIX + "\u00a7eSeraph API key saved.");
                } else if (keyType.equals("bordic")) {
                    LazifyConfig.INSTANCE.setBordicKey(args[2]); LazifyConfig.INSTANCE.save();
                    BordicSuperstar.clearCache();
                    print(PREFIX + "\u00a7eBordic API key saved.");
                } else if (keyType.equals("hypixel")) {
                    LazifyConfig.INSTANCE.setHypixelKey(args[2]); LazifyConfig.INSTANCE.save();
                    print(PREFIX + "\u00a7eHypixel API key saved.");
                } else {
                    print(PREFIX + "\u00a7eUnknown key type: \u00a73" + args[1]
                            + "\u00a7e. Use \u00a73urchin\u00a7e, \u00a73seraph\u00a7e, \u00a73bordic\u00a7e, or \u00a73hypixel\u00a7e.");
                }
                return;

            case "fkdrcolor":
                handleFkdrColor(args);
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
                case "middleclickshop":
                    cfg.setMiddleClickShop(args.length > 1 ? parseBool(args[1]) : !cfg.isMiddleClickShop()); break;
                case "skindenick":
                    cfg.setSkinDenick(args.length > 1 ? parseBool(args[1]) : !cfg.isSkinDenick()); break;
                case "fkdrcolors":
                    cfg.setFkdrColors(args.length > 1 ? parseBool(args[1]) : !cfg.isFkdrColors()); break;
                case "autowho":
                    cfg.setAutoWho(args.length > 1 ? parseBool(args[1]) : !cfg.isAutoWho()); break;
                case "hidewho":
                    cfg.setHideWho(args.length > 1 ? parseBool(args[1]) : !cfg.isHideWho()); break;
                case "dodgewarning":
                    cfg.setDodgeWarning(args.length > 1 ? parseBool(args[1]) : !cfg.isDodgeWarning()); break;
                case "teamthreatchat":
                    cfg.setTeamThreatChat(args.length > 1 ? parseBool(args[1]) : !cfg.isTeamThreatChat()); break;
                case "teamfkdrchat":
                    cfg.setTeamFkdrChat(args.length > 1 ? parseBool(args[1]) : !cfg.isTeamFkdrChat()); break;
                case "nohurtcam":
                    cfg.setNoHurtCam(args.length > 1 ? parseBool(args[1]) : !cfg.isNoHurtCam()); break;
                case "antidebuff":
                    cfg.setAntiDebuff(args.length > 1 ? parseBool(args[1]) : !cfg.isAntiDebuff()); break;
                case "sendnicked":
                    cfg.setSendNickedToChat(args.length > 1 ? parseBool(args[1]) : !cfg.isSendNickedToChat()); break;
                case "sendurchinreason":
                    cfg.setSendUrchinReasonToChat(args.length > 1 ? parseBool(args[1]) : !cfg.isSendUrchinReasonToChat()); break;
                case "keybindhold":
                    cfg.setKeybindHold(args.length > 1 ? parseBool(args[1]) : !cfg.isKeybindHold()); break;
                case "showontab":
                    cfg.setShowOnTab(args.length > 1 ? parseBool(args[1]) : !cfg.isShowOnTab()); break;
                case "overlayovertab":
                    cfg.setOverlayOverTab(args.length > 1 ? parseBool(args[1]) : !cfg.isOverlayOverTab()); break;
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
                case "whodelay":
                    if (args.length < 2) { print(PREFIX + "\u00a7ewhodelay: \u00a73" + cfg.getWhoDelay() + "s \u00a7e(0-10)"); return; }
                    cfg.setWhoDelay(Math.max(0, Math.min(10, Double.parseDouble(args[1])))); break;
                case "dodgethreshold":
                    if (args.length < 2) { print(PREFIX + "\u00a7edodgethreshold: \u00a73" + cfg.getDodgeThreshold()); return; }
                    cfg.setDodgeThreshold(Math.max(0.1, Double.parseDouble(args[1]))); break;
                case "teamthreatthreshold":
                    if (args.length < 2) { print(PREFIX + "\u00a7eteamthreatthreshold: \u00a73" + cfg.getTeamThreatThreshold()); return; }
                    cfg.setTeamThreatThreshold(Math.max(0.1, Double.parseDouble(args[1]))); break;
                case "threatfkdrweight":
                    if (args.length < 2) { print(PREFIX + "\u00a7ethreatfkdrweight: \u00a73" + cfg.getThreatFkdrWeight()); return; }
                    cfg.setThreatFkdrWeight(Math.max(0.0, Double.parseDouble(args[1]))); break;
                case "threatstarweight":
                    if (args.length < 2) { print(PREFIX + "\u00a7ethreatstarweight: \u00a73" + cfg.getThreatStarWeight()); return; }
                    cfg.setThreatStarWeight(Math.max(0.0, Double.parseDouble(args[1]))); break;
                case "threatwinstreakweight":
                    if (args.length < 2) { print(PREFIX + "\u00a7ethreatwinstreakweight: \u00a73" + cfg.getThreatWinstreakWeight()); return; }
                    cfg.setThreatWinstreakWeight(Math.max(0.0, Double.parseDouble(args[1]))); break;
                case "threaturchinweight":
                    if (args.length < 2) { print(PREFIX + "\u00a7ethreaturchinweight: \u00a73" + cfg.getThreatUrchinWeight()); return; }
                    cfg.setThreatUrchinWeight(Math.max(0.0, Double.parseDouble(args[1]))); break;
                case "threatteamsizeweight":
                    if (args.length < 2) { print(PREFIX + "\u00a7ethreatteamsizeweight: \u00a73" + cfg.getThreatTeamSizeWeight()); return; }
                    cfg.setThreatTeamSizeWeight(Math.max(0.0, Double.parseDouble(args[1]))); break;
                case "threatencounterweight":
                    if (args.length < 2) { print(PREFIX + "\u00a7ethreatencounterweight: \u00a73" + cfg.getThreatEncounterWeight()); return; }
                    cfg.setThreatEncounterWeight(Math.max(0.0, Double.parseDouble(args[1]))); break;
                case "threatnickweight":
                    if (args.length < 2) { print(PREFIX + "\u00a7ethreatnickweight: \u00a73" + cfg.getThreatNickWeight()); return; }
                    cfg.setThreatNickWeight(Math.max(0.0, Double.parseDouble(args[1]))); break;
                case "x":
                    if (args.length < 2) { print(PREFIX + "\u00a7ex: \u00a73" + cfg.getOverlayX()); return; }
                    cfg.setOverlayX(Math.max(0, Integer.parseInt(args[1]))); break;
                case "y":
                    if (args.length < 2) { print(PREFIX + "\u00a7ey: \u00a73" + cfg.getOverlayY()); return; }
                    cfg.setOverlayY(Math.max(0, Integer.parseInt(args[1]))); break;
                case "colgap":
                    if (args.length < 2) { print(PREFIX + "\u00a7ecolgap: \u00a73" + cfg.getOverlayColGap() + " \u00a7e(0-40)"); return; }
                    cfg.setOverlayColGap(clamp(Integer.parseInt(args[1]), 0, 40)); break;
                case "rowgap":
                    if (args.length < 2) { print(PREFIX + "\u00a7erowgap: \u00a73" + cfg.getOverlayRowGap() + " \u00a7e(0-20)"); return; }
                    cfg.setOverlayRowGap(clamp(Integer.parseInt(args[1]), 0, 20)); break;
                case "scale":
                    if (args.length < 2) {
                        print(PREFIX + "\u00a7escale: \u00a73" + cfg.getOverlayScalePercent()
                                + "% \u00a7e(50-200, 100 = normal)");
                        return;
                    }
                    cfg.setOverlayScalePercent(clamp(Integer.parseInt(args[1]), 50, 200)); break;
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
                case "overlaytheme":
                    if (args.length < 2) {
                        print(PREFIX + "\u00a7eoverlaytheme: \u00a73" + cfg.getOverlayTheme()
                                + " \u00a7e(" + OverlayTheme.themeName(cfg.getOverlayTheme()) + "). 0=Default 1=Nerdify");
                        return;
                    }
                    cfg.setOverlayTheme(clamp(Integer.parseInt(args[1]), 0, 1)); break;

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
                    if (LazifyMod.overlayKeybind != null) LazifyMod.overlayKeybind.setKeyCode(code);
                    print(PREFIX + "\u00a7ekeybind \u00a72\u2192\u00a7e " + Keyboard.getKeyName(code)
                            + " (" + code + "). Also rebindable in Controls > Lazify."); return;
                }

                // ── Column visibility ──────────────────────────────────────────
                case "col":
                    if (args.length < 2) { printColStatus(); return; }
                    String colName = args[1].toLowerCase();
                    boolean curVal;
                    switch (colName) {
                        case "encounters": curVal = cfg.isColEncounters(); break;
                        case "username":   curVal = cfg.isColUsername();   break;
                        case "rank":       curVal = cfg.isColRank();       break;
                        case "star":       curVal = cfg.isColStar();       break;
                        case "fkdr":       curVal = cfg.isColFkdr();       break;
                        case "winstreaks": curVal = cfg.isColWinstreaks(); break;
                        case "urchin":     curVal = cfg.isColUrchin();     break;
                        case "session":    curVal = cfg.isColSession();    break;
                        case "level":      curVal = cfg.isColLevel();      break;
                        case "ping":       curVal = cfg.isColPing();       break;
                        default: print(PREFIX + "\u00a7eUnknown column: \u00a73" + args[1]
                            + "\u00a7e. Options: encounters username rank star fkdr winstreaks urchin session level ping"); return;
                    }
                    // If arg[2] is a number, move column to that 1-indexed position
                    if (args.length > 2) {
                        try {
                            int pos = Integer.parseInt(args[2]);
                            String newOrder = moveColInOrder(cfg.getColOrder(), colName, pos);
                            cfg.setColOrder(newOrder);
                            cfg.save(); defaultSettings();
                            print(PREFIX + "u00a7eColumn u00a73" + colName + "u00a7e moved to position u00a73" + pos);
                            return;
                        } catch (NumberFormatException ignored) {}
                    }
                    boolean newVal = args.length > 2 ? parseBool(args[2]) : !curVal;
                    switch (colName) {
                        case "encounters": cfg.setColEncounters(newVal); break;
                        case "username":   cfg.setColUsername(newVal);   break;
                        case "rank":       cfg.setColRank(newVal);       break;
                        case "star":       cfg.setColStar(newVal);       break;
                        case "fkdr":       cfg.setColFkdr(newVal);       break;
                        case "winstreaks": cfg.setColWinstreaks(newVal); break;
                        case "urchin":     cfg.setColUrchin(newVal);     break;
                        case "session":    cfg.setColSession(newVal);    break;
                        case "level":      cfg.setColLevel(newVal);      break;
                        case "ping":       cfg.setColPing(newVal);       break;
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
        print(PREFIX + "\u00a77key \u00a7e<urchin|seraph|bordic|hypixel> <key>\u00a77 \u00a7e\u2013 set API key");
        print(PREFIX + "\u00a77tags\u00a77 \u00a7e\u2013 show overlay tag definitions");
        print(PREFIX + "\u00a77tag \u00a7e<user>\u00a77 \u00a7e\u2013 show player's Urchin + Seraph tags");
        print(PREFIX + "\u00a77fkdrcolor \u00a7e<1-7> <0-f>\u00a77 \u00a7e\u2013 set FKDR tier color");
        if (LazifyConfig.INSTANCE.isDebug()) {
            print(PREFIX + "\u00a78debugsb\u00a77 \u00a7e\u2013 dump scoreboard data to chat");
            print(PREFIX + "\u00a78debugtab\u00a77 \u00a7e\u2013 dump tab list data to chat");
        }
    }

    private void printStatus() {
        LazifyConfig c = LazifyConfig.INSTANCE;
        print(PREFIX + "\u00a77\u2500\u2500\u2500 \u00a7dLazify Settings \u00a77\u2500\u2500\u2500  \u00a77/ov <setting> [value]");

        // ── Keybind ──
        print(PREFIX + "\u00a7d Keybind");
        print(PREFIX + settingLine("keybind", Keyboard.getKeyName(c.getKeybind()))
            + settingLine("keybindhold", c.isKeybindHold())
            + settingLine("showontab", c.isShowOnTab())
            + settingLine("overlayovertab", c.isOverlayOverTab()));

        // ── Overlay ──
        print(PREFIX + "\u00a7d Overlay");
        print(PREFIX + settingLine("teams", c.isTeams())
            + settingLine("teamprefix", c.isTeamPrefix())
            + settingLine("showranks", c.isShowRanks())
            + settingLine("showyourself", c.isShowYourself()));
        print(PREFIX + settingLine("removefinalkill", c.isRemoveFinalKill())
            + settingLine("autotablist", c.isAutoTablist())
            + settingLine("clearonwho", c.isClearOnWho()));

        // ── Features ──
        print(PREFIX + "\u00a7d Features");
        print(PREFIX + settingLine("skindenick", c.isSkinDenick())
            + settingLine("middleclickshop", c.isMiddleClickShop())
            + settingLine("fkdrcolors", c.isFkdrColors()));
        print(PREFIX + settingLine("autowho", c.isAutoWho())
            + settingLine("whodelay", c.getWhoDelay() + "s")
            + settingLine("hidewho", c.isHideWho())
            + settingLine("dodgewarning", c.isDodgeWarning())
            + settingLine("dodgethreshold", String.valueOf(c.getDodgeThreshold())));
        print(PREFIX + settingLine("teamthreatchat", c.isTeamThreatChat())
            + settingLine("teamthreatthreshold", ColorUtil.formatDoubleStr(ColorUtil.round(c.getTeamThreatThreshold(), 2))));
        print(PREFIX + settingLine("threatfkdrweight", ColorUtil.formatDoubleStr(ColorUtil.round(c.getThreatFkdrWeight(), 2)))
            + settingLine("threatstarweight", ColorUtil.formatDoubleStr(ColorUtil.round(c.getThreatStarWeight(), 2)))
            + settingLine("threatwinstreakweight", ColorUtil.formatDoubleStr(ColorUtil.round(c.getThreatWinstreakWeight(), 2))));
        print(PREFIX + settingLine("threaturchinweight", ColorUtil.formatDoubleStr(ColorUtil.round(c.getThreatUrchinWeight(), 2)))
            + settingLine("threatteamsizeweight", ColorUtil.formatDoubleStr(ColorUtil.round(c.getThreatTeamSizeWeight(), 2)))
            + settingLine("threatencounterweight", ColorUtil.formatDoubleStr(ColorUtil.round(c.getThreatEncounterWeight(), 2)))
            + settingLine("threatnickweight", ColorUtil.formatDoubleStr(ColorUtil.round(c.getThreatNickWeight(), 2))));
        print(PREFIX + settingLine("nohurtcam", c.isNoHurtCam())
            + settingLine("antidebuff", c.isAntiDebuff())
            + settingLine("teamfkdrchat", c.isTeamFkdrChat()));

        // ── Chat ──
        print(PREFIX + "\u00a7d Chat");
        print(PREFIX + settingLine("sendnicked", c.isSendNickedToChat())
            + settingLine("sendurchinreason", c.isSendUrchinReasonToChat()));

        // ── Sorting ──
        print(PREFIX + "\u00a7d Sorting");
        print(PREFIX + settingLine("sortby", c.getSortByIndex() + " \u00a77(" + sortByName(c.getSortByIndex()) + ")")
            + settingLine("sortmode", c.getSortMode() + " \u00a77(" + (c.getSortMode() == 0 ? "asc" : "desc") + ")")
            + settingLine("winstreak", c.getWinstreakMode() + " \u00a77(" + winstreakName(c.getWinstreakMode()) + ")"));
        print(PREFIX + settingLine("enctimeout", c.getEncountersTimeoutMins() + "m"));

        // ── Appearance ──
        print(PREFIX + "\u00a7d Appearance");
        print(PREFIX + settingLine("x", String.valueOf(c.getOverlayX()))
            + settingLine("y", String.valueOf(c.getOverlayY()))
            + settingLine("colgap", String.valueOf(c.getOverlayColGap()))
            + settingLine("rowgap", String.valueOf(c.getOverlayRowGap()))
            + settingLine("scale", c.getOverlayScalePercent() + "%")
            + settingLine("bgopacity", String.valueOf(c.getBgOpacity())));
        print(PREFIX + settingLine("bghue", String.valueOf(c.getBgHue()))
            + settingLine("headerhue", String.valueOf(c.getHeaderHue()))
            + settingLine("borderhue", String.valueOf(c.getBorderHue()))
            + settingLine("overlaytheme", c.getOverlayTheme() + " \u00a77(" + OverlayTheme.themeName(c.getOverlayTheme()) + ")"));
        String[] fc = c.getFkdrColors();
        StringBuilder fcLine = new StringBuilder(" \u00a77fkdrcolor ");
        for (int i = 0; i < 7; i++) fcLine.append("\u00a7").append(fc[i]).append("\u2588");
        print(PREFIX + fcLine.toString() + " \u00a77(/ov fkdrcolor)");

        // ── Columns ──
        print(PREFIX + "\u00a7d Columns \u00a77(/ov col <name>)");
        print(PREFIX
            + colLine("encounters", c.isColEncounters()) + colLine("username", c.isColUsername())
            + colLine("rank", c.isColRank()) + colLine("star", c.isColStar()) + colLine("fkdr", c.isColFkdr())
            + colLine("winstreaks", c.isColWinstreaks()) + colLine("urchin", c.isColUrchin())
            + colLine("session", c.isColSession()) + colLine("level", c.isColLevel())
            + colLine("ping", c.isColPing()));

        // ── Status ──
        print(PREFIX + "\u00a77urchin key: " + (c.getUrchinKey().isEmpty() ? "\u00a7cnot set" : "\u00a7aset")
            + "  \u00a77seraph key: " + (c.getSeraphKey().isEmpty() ? "\u00a7cnot set" : "\u00a7aset") + "  "
            + "\u00a77overlay: " + (visible ? "\u00a7avisible" : "\u00a7chidden") + "  "
            + "\u00a77debug: \u00a7" + (c.isDebug() ? "a" : "c") + c.isDebug());
    }

    private static String settingLine(String name, boolean val) {
        return " \u00a77" + name + " \u00a7" + (val ? "a" : "c") + val + " ";
    }

    private static String settingLine(String name, String val) {
        return " \u00a77" + name + " \u00a7e" + val + " ";
    }

    private static String colLine(String name, boolean val) {
        return " \u00a7" + (val ? "a" : "c") + name + " ";
    }

    private static final String VALID_COLOR_CODES = "0123456789abcdef";
    private static final String[] FKDR_TIER_NAMES = {
        "< 1.4", "1.4 - 2.4", "2.4 - 5", "5 - 10", "10 - 100", "100 - 1000", "1000+"
    };

    private void handleFkdrColor(String[] args) {
        LazifyConfig cfg = LazifyConfig.INSTANCE;
        String[] colors = cfg.getFkdrColors();
        if (args.length < 2) {
            print(PREFIX + "\u00a77\u2500\u2500\u2500 \u00a7dFKDR Colors \u00a77\u2500\u2500\u2500  \u00a77/ov fkdrcolor <1-7> <0-f>");
            for (int i = 0; i < 7; i++) {
                print(PREFIX + " \u00a73" + (i + 1) + " \u00a77(" + FKDR_TIER_NAMES[i] + ") \u00a7" + colors[i] + "\u2588\u2588 " + colors[i]);
            }
            return;
        }
        int tier;
        try { tier = Integer.parseInt(args[1]); } catch (NumberFormatException e) {
            print(PREFIX + "\u00a7cTier must be 1-7."); return;
        }
        if (tier < 1 || tier > 7) { print(PREFIX + "\u00a7cTier must be 1-7."); return; }
        if (args.length < 3) {
            print(PREFIX + "\u00a7eTier " + tier + " (" + FKDR_TIER_NAMES[tier - 1] + "): \u00a7" + colors[tier - 1] + "\u2588\u2588 " + colors[tier - 1]);
            return;
        }
        String code = args[2].toLowerCase();
        if (code.length() != 1 || VALID_COLOR_CODES.indexOf(code.charAt(0)) == -1) {
            print(PREFIX + "\u00a7cColor must be a single hex char: 0-9, a-f."); return;
        }
        switch (tier) {
            case 1: cfg.setFkdrColor1(code); break;
            case 2: cfg.setFkdrColor2(code); break;
            case 3: cfg.setFkdrColor3(code); break;
            case 4: cfg.setFkdrColor4(code); break;
            case 5: cfg.setFkdrColor5(code); break;
            case 6: cfg.setFkdrColor6(code); break;
            case 7: cfg.setFkdrColor7(code); break;
        }
        cfg.save(); defaultSettings();
        print(PREFIX + "\u00a7eFKDR tier " + tier + " (" + FKDR_TIER_NAMES[tier - 1] + ") \u00a72\u2192\u00a7 " + code + "\u2588\u2588 " + code);
    }

    private void printColStatus() {
        LazifyConfig c = LazifyConfig.INSTANCE;
        print(PREFIX + "\u00a77Columns \u00a77(use /ov col <name> to toggle):");
        print(PREFIX
            + "  encounters " + boolStr(c.isColEncounters()) + "  username " + boolStr(c.isColUsername())
            + "  rank " + boolStr(c.isColRank()) + "  star " + boolStr(c.isColStar()) + "  fkdr " + boolStr(c.isColFkdr())
            + "  winstreaks " + boolStr(c.isColWinstreaks()) + "  urchin " + boolStr(c.isColUrchin())
            + "  session " + boolStr(c.isColSession()) + "  level " + boolStr(c.isColLevel())
            + "  ping " + boolStr(c.isColPing()));
        String[] order = c.getColOrder().split(",");
        StringBuilder osb = new StringBuilder(PREFIX + "u00a77Order: ");
        for (int i = 0; i < order.length; i++) osb.append("u00a7e").append(i + 1).append("u00a77.").append(order[i].trim()).append(" ");
        print(osb.toString());
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
            case "middleclickshop": return boolStr(c.isMiddleClickShop());
            case "skindenick":      return boolStr(c.isSkinDenick());
            case "fkdrcolors":      return boolStr(c.isFkdrColors());
            case "autowho":         return boolStr(c.isAutoWho());
            case "whodelay":        return "\u00a7e" + c.getWhoDelay() + "s";
            case "hidewho":         return boolStr(c.isHideWho());
            case "dodgewarning":    return boolStr(c.isDodgeWarning());
            case "dodgethreshold":  return "\u00a7e" + c.getDodgeThreshold();
            case "teamthreatchat":  return boolStr(c.isTeamThreatChat());
            case "teamthreatthreshold": return "\u00a7e" + c.getTeamThreatThreshold();
            case "threatfkdrweight": return "\u00a7e" + c.getThreatFkdrWeight();
            case "threatstarweight": return "\u00a7e" + c.getThreatStarWeight();
            case "threatwinstreakweight": return "\u00a7e" + c.getThreatWinstreakWeight();
            case "threaturchinweight": return "\u00a7e" + c.getThreatUrchinWeight();
            case "threatteamsizeweight": return "\u00a7e" + c.getThreatTeamSizeWeight();
            case "threatencounterweight": return "\u00a7e" + c.getThreatEncounterWeight();
            case "threatnickweight": return "\u00a7e" + c.getThreatNickWeight();
            case "nohurtcam":       return boolStr(c.isNoHurtCam());
            case "antidebuff":      return boolStr(c.isAntiDebuff());
            case "teamfkdrchat":    return boolStr(c.isTeamFkdrChat());
            case "sendnicked":       return boolStr(c.isSendNickedToChat());
            case "sendurchinreason": return boolStr(c.isSendUrchinReasonToChat());
            case "keybindhold":      return boolStr(c.isKeybindHold());
            case "showontab":        return boolStr(c.isShowOnTab());
            case "overlayovertab":   return boolStr(c.isOverlayOverTab());
            case "debug":            return boolStr(c.isDebug());
            case "keybind":          return "\u00a7e" + Keyboard.getKeyName(c.getKeybind()) + " (" + c.getKeybind() + ")";
            case "sortby":     return "\u00a7e" + c.getSortByIndex() + "\u00a7e (" + sortByName(c.getSortByIndex()) + ")";
            case "sortmode":   return "\u00a7e" + c.getSortMode() + "\u00a7e (" + (c.getSortMode() == 0 ? "asc" : "desc") + ")";
            case "winstreak":  return "\u00a7e" + c.getWinstreakMode() + "\u00a7e (" + winstreakName(c.getWinstreakMode()) + ")";
            case "enctimeout": return "\u00a7e" + c.getEncountersTimeoutMins() + "\u00a7em";
            case "x":          return "\u00a7e" + c.getOverlayX();
            case "y":          return "\u00a7e" + c.getOverlayY();
            case "colgap":     return "\u00a7e" + c.getOverlayColGap();
            case "rowgap":     return "\u00a7e" + c.getOverlayRowGap();
            case "scale":      return "\u00a7e" + c.getOverlayScalePercent() + "%";
            case "bgopacity":  return "\u00a7e" + c.getBgOpacity();
            case "bghue":      return "\u00a7e" + c.getBgHue();
            case "headerhue":  return "\u00a7e" + c.getHeaderHue();
            case "borderhue":  return "\u00a7e" + c.getBorderHue();
            case "overlaytheme": return "\u00a7e" + c.getOverlayTheme() + "\u00a7e (" + OverlayTheme.themeName(c.getOverlayTheme()) + ")";
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
