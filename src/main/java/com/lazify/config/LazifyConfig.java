package com.lazify.config;

import com.lazify.LazifyMod;
import com.lazify.util.ThresholdColorScale;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class LazifyConfig {

    public static final LazifyConfig INSTANCE = new LazifyConfig();

    private Configuration config;
    private File configDir;

    // API keys
    private String urchinKey  = "";
    private String seraphKey  = "";
    private String bordicKey  = "";
    private String hypixelKey = "";

    // keybind behaviour
    private boolean keybindHold = false;
    private boolean showOnTab   = true;
    private boolean overlayOverTab = false;
    private int     keybind     = 41;    // LWJGL KEY_GRAVE (`)

    // debug
    private boolean debug = false;

    // boolean settings
    private boolean teams                  = true;
    private boolean teamPrefix             = false;
    private boolean showYourself           = false;
    private boolean sendNickedToChat       = true;
    private boolean sendUrchinReasonToChat = false;
    private boolean disableInLobby         = true;
    private boolean showRanks              = false;
    private boolean removeFinalKill        = false;
    private boolean autoTablist            = true;
    private boolean clearOnWho             = false;
    private boolean middleClickShop        = false;
    private boolean denick                   = true;
    private boolean fkdrColors             = true;
    private boolean autoWho                = false;
    private double  whoDelay               = 0.0;
    private boolean hideWho                = false;
    private boolean autoPl                 = true;
    private boolean hidePl                 = true;
    private boolean teamFkdrChat           = false;
    private boolean teamThreatChat         = false;
    private boolean dodgeWarning           = false;
    private double  dodgeThreshold         = 3.0;
    private double  teamThreatThreshold    = 6.5;
    private double  threatFkdrWeight       = 0.7;
    private double  threatStarWeight       = 0.35;
    private double  threatWinstreakWeight  = 0.3;
    private double  threatUrchinWeight     = 1.4;
    private double  threatTeamSizeWeight   = 0.9;
    private double  threatEncounterWeight  = 0.25;
    private double  threatNickWeight       = 0.75;
    private boolean noHurtCam              = false;
    private boolean antiDebuff             = false;

    // PartyDetector (pregame group-join alerts)
    private boolean partyDetector          = true;
    private boolean partyDetectorPing      = false;
    private boolean partyDetectorShowMissed = true;
    private boolean partyDetectorBw2s      = false;
    private boolean partyDetectorBw3s      = true;
    private boolean partyDetectorBw4s      = true;
    private boolean partyDetectorBw4v4     = false;

    /** Print teammate kills/finals/beds in chat when a Bedwars game ends. */
    private boolean gameResultChat         = true;

    /** Overlay/chat filter: keep players who meet min FKDR OR min stars. */
    private boolean statFilter             = false;
    private double  statFilterMinFkdr      = 0.0;
    private int     statFilterMinStars     = 0;
    private boolean statFilterChat         = false;

    // column visibility
    private boolean colEncounters = true;
    private boolean colUsername   = true;
    private boolean colRank       = true;
    private boolean colStar       = true;
    private boolean colFkdr       = true;
    private boolean colWinstreaks = true;
    private boolean colUrchin     = true;
    private boolean colSession    = true;
    private boolean colLevel      = false;
    private boolean colPing       = false;
    private boolean colWlr        = false;
    private boolean colBblr       = false;
    private boolean colKdr        = false;
    private boolean colKills      = false;
    private boolean colFinals     = false;
    private boolean colBeds       = false;
    private boolean colWins       = false;
    private boolean colDailyFkdr   = false;
    private boolean colDailyWlr    = false;
    private boolean colDailyStars  = false;
    private boolean colDailyBblr   = false;
    private boolean colDailyKdr    = false;
    private boolean colWeeklyFkdr  = false;
    private boolean colWeeklyWlr   = false;
    private boolean colWeeklyStars = false;
    private boolean colWeeklyBblr  = false;
    private boolean colWeeklyKdr   = false;
    private boolean colMonthlyFkdr = false;
    private boolean colMonthlyWlr  = false;
    private boolean colMonthlyStars = false;
    private boolean colMonthlyBblr = false;
    private boolean colMonthlyKdr  = false;
    private String  colOrder      = "encounters,username,rank,star,fkdr,wlr,bblr,kdr,kills,finals,beds,wins,dailyfkdr,dailywlr,dailystars,dailybblr,dailykdr,weeklyfkdr,weeklywlr,weeklystars,weeklybblr,weeklykdr,monthlyfkdr,monthlywlr,monthlystars,monthlybblr,monthlykdr,winstreaks,urchin,session,ping,level";

    // int settings
    private int encountersTimeoutMins = 30;
    private int sortByIndex           = 2;
    private int sortMode              = 0;
    private int winstreakMode         = 0;

    private int overlayTheme         = 0;   // 0=Lazify 1=Nerdify 2=Mellow (tab list)

    // overlay position
    private int overlayX = 2;
    private int overlayY = 2;
    private int overlayColGap = 12;
    private int overlayRowGap = 5;
    private int overlayScalePercent = 100;

    // colors — background
    private int bgOpacity = 170;
    private int bgR = 0, bgG = 0, bgB = 0;
    private int bgHue = 0;       // legacy migration only
    private int headerHue = 290; // legacy migration only
    private int borderHue = 360; // legacy migration only

    // outline (border)
    private boolean outlineEnabled = true;
    private boolean outlineChroma  = true;
    private int outlineR = 255;
    private int outlineG = 255;
    private int outlineB = 255;
    private float outlineWidth = 2.5f;
    private int borderRadius = 0; // 0 = sharp corners
    private int overlayPad = 0;   // extra inner padding (px)

    private boolean textShadow = true;
    private boolean headerBold = true;

    private boolean stripeEnabled = false;
    private int stripeR = 255, stripeG = 255, stripeB = 255, stripeA = 18;

    private boolean highlightSelf = false;
    private int highlightSelfR = 80, highlightSelfG = 180, highlightSelfB = 255, highlightSelfA = 40;
    private boolean highlightParty = false;
    private int highlightPartyR = 80, highlightPartyG = 255, highlightPartyB = 120, highlightPartyA = 40;
    private boolean highlightNicked = false;
    private int highlightNickedR = 255, highlightNickedG = 220, highlightNickedB = 60, highlightNickedA = 45;
    private boolean highlightTagged = false;
    private int highlightTaggedR = 255, highlightTaggedG = 60, highlightTaggedB = 60, highlightTaggedA = 50;

    private int fkdrDecimals = 2;       // 0–3
    private boolean abbreviateNumbers = false;
    private int pingStyle = 0;          // 0 = number, 1 = with ms

    // per-column header RGB keyed by OverlayManager column key (player, star, …)
    private final Map<String, int[]> headerColors = new LinkedHashMap<>();
    private int headerAllR = 170, headerAllG = 0, headerAllB = 255;

    // Mellow tab panel colors (ARGB channels)
    private int mellowOuterR = 0, mellowOuterG = 0, mellowOuterB = 0, mellowOuterA = 128;
    private int mellowHeaderR = 255, mellowHeaderG = 255, mellowHeaderB = 255, mellowHeaderA = 32;
    private int mellowRowR = 255, mellowRowG = 255, mellowRowB = 255, mellowRowA = 32;
    private int mellowTaggedR = 0, mellowTaggedG = 0, mellowTaggedB = 0, mellowTaggedA = 153;

    // legacy FKDR § codes (migrated into fkdrScale)
    private String fkdrColor1 = "7";
    private String fkdrColor2 = "f";
    private String fkdrColor3 = "e";
    private String fkdrColor4 = "6";
    private String fkdrColor5 = "c";
    private String fkdrColor6 = "4";
    private String fkdrColor7 = "5";

    // Custom threshold → RGB scales (also used for WLR/BBLR/KDR via fkdrScale)
    private ThresholdColorScale fkdrScale = ThresholdColorScale.defaultFkdr();
    private ThresholdColorScale wsScale = ThresholdColorScale.defaultWinstreak();
    private ThresholdColorScale pingScale = ThresholdColorScale.defaultPing();
    private ThresholdColorScale sessionScale = ThresholdColorScale.defaultSessionMinutes();
    private ThresholdColorScale encountersScale = ThresholdColorScale.defaultEncounters();
    private ThresholdColorScale countsScale = ThresholdColorScale.defaultCounts();
    private ThresholdColorScale periodStarsScale = ThresholdColorScale.defaultPeriodStars();
    private boolean wsColors = true;
    private boolean pingColors = true;
    private boolean sessionColors = true;
    private boolean encountersColors = true;
    private boolean countColors = true;
    private boolean periodStarsColors = true;

    /** Overlay column keys that have a header tag color. */
    public static final String[] HEADER_COL_KEYS = {
        "player", "rank", "seen", "star", "fkdr", "wlr", "bblr", "kdr",
        "kills", "finals", "beds", "wins",
        "winstreaks", "session", "urchin", "netlevel", "ping",
        "dailyfkdr", "dailywlr", "dailystars", "dailybblr", "dailykdr",
        "weeklyfkdr", "weeklywlr", "weeklystars", "weeklybblr", "weeklykdr",
        "monthlyfkdr", "monthlywlr", "monthlystars", "monthlybblr", "monthlykdr"
    };

    private LazifyConfig() {}

    public void load(File configDir) {
        this.configDir = configDir;
        File cfgFile = resolveConfigFile(configDir);
        config = new Configuration(cfgFile);
        config.load();
        syncFromFile();
        if (config.hasChanged()) config.save();
    }

    /**
     * Prefer {@code config/lazify/lazify.cfg}. If only the legacy
     * {@code config/lazify.cfg} exists, copy it into the lazify folder.
     */
    private static File resolveConfigFile(File forgeConfigDir) {
        File modDir = new File(forgeConfigDir, "lazify");
        if (!modDir.exists()) modDir.mkdirs();

        File newCfg = new File(modDir, "lazify.cfg");
        File oldCfg = new File(forgeConfigDir, "lazify.cfg");

        if (!newCfg.exists() && oldCfg.isFile()) {
            try {
                copyFile(oldCfg, newCfg);
                LazifyMod.LOGGER.info("Migrated lazify.cfg to {}", newCfg.getAbsolutePath());
            } catch (IOException e) {
                LazifyMod.LOGGER.warn("Failed to migrate lazify.cfg into lazify/: {}", e.getMessage());
                return oldCfg;
            }
        }
        return newCfg;
    }

    private static void copyFile(File from, File to) throws IOException {
        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            in = new FileInputStream(from);
            out = new FileOutputStream(to);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) >= 0) {
                out.write(buf, 0, n);
            }
            out.flush();
        } finally {
            if (in != null) try { in.close(); } catch (IOException ignored) {}
            if (out != null) try { out.close(); } catch (IOException ignored) {}
        }
    }

    public File getConfigDir() { return configDir; }

    /** Mod-owned folder under Forge config: {@code config/lazify/}. */
    public File getModConfigDir() {
        File dir = new File(configDir, "lazify");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public void syncFromFile() {
        // ── Category descriptions ─────────────────────────────────────────────
        config.getCategory("general").setComment("General overlay and gameplay settings.");
        config.getCategory("columns").setComment("Toggle which columns are shown in the overlay.");
        config.getCategory("position").setComment("Overlay position on screen. Use the Drag Position button for visual positioning.");
        config.getCategory("colors").setComment("Overlay color and opacity settings. Hue values: 0 = black, 1-359 = color wheel, 360 = rainbow.");
        config.getCategory("api").setComment("API key configuration.");

        // ── API ───────────────────────────────────────────────────────────────
        Property p;

        p = config.get("api", "urchinKey", "");
        p.comment = "Your Urchin/Coral API key from api.urchin.gg. Enables blacklist tags in the overlay.";
        urchinKey = p.getString();

        p = config.get("api", "seraphKey", "");
        p.comment = "Your Seraph API key from seraph.si. Enables blacklist tags alongside Urchin/Coral.";
        seraphKey = p.getString();

        p = config.get("api", "bordicKey", "");
        p.comment = "Your Bordic API key from bordic.xyz. Enables session period columns and MVP++ stat nick denicking.";
        bordicKey = p.getString();

        p = config.get("api", "hypixelKey", "");
        p.comment = "Optional Hypixel API key. Used as last-resort stats fallback after Abyss and Prism.";
        hypixelKey = p.getString();

        // ── General: Keybind ──────────────────────────────────────────────────
        p = config.get("general", "keybind", 41);
        p.comment = "Overlay toggle key (LWJGL code). Rebind in Controls > Lazify or /ov keybind.";
        keybind = p.getInt(41);

        p = config.get("general", "keybindHold", false);
        p.comment = "Hold mode: overlay is only visible while you hold the keybind. Off = toggle on/off.";
        keybindHold = p.getBoolean(false);

        p = config.get("general", "showOnTab", true);
        p.comment = "Also show the overlay while you hold the Tab key.";
        showOnTab = p.getBoolean(true);

        p = config.get("general", "overlayOverTab", false);
        p.comment = "Render the overlay above the tab list instead of behind it.";
        overlayOverTab = p.getBoolean(false);

        // Legacy: statsDisplayMode 1/2 migrated to overlayTheme Mellow (2)
        p = config.get("general", "statsDisplayMode", 0);
        int legacyStatsMode = p.getInt(0);

        // ── General: Display ──────────────────────────────────────────────────
        p = config.get("general", "teams", true);
        p.comment = "Color-code player names by their Bedwars team color.";
        teams = p.getBoolean(true);

        p = config.get("general", "teamPrefix", false);
        p.comment = "Show a team letter prefix (R, B, G, Y...) before each player name.";
        teamPrefix = p.getBoolean(false);

        p = config.get("general", "showYourself", false);
        p.comment = "Include your own stats in the overlay.";
        showYourself = p.getBoolean(false);

        p = config.get("general", "showRanks", false);
        p.comment = "Also show the rank prefix in the Username column (Rank column is separate).";
        showRanks = p.getBoolean(false);

        // ── General: Detection ────────────────────────────────────────────────
        p = config.get("general", "autoTablist", true);
        p.comment = "Automatically scan the tab list to detect and add players. Disable for manual /ov sc only.";
        autoTablist = p.getBoolean(true);

        p = config.get("general", "clearOnWho", false);
        p.comment = "Clear the overlay when a /who response is received, then re-add players from the response.";
        clearOnWho = p.getBoolean(false);

        // Migrate skinDenick → denick (master toggle for tab/skin/stat denick)
        boolean denickDefault = true;
        ConfigCategory genCat = config.getCategory("general");
        if (!genCat.containsKey("denick") && genCat.containsKey("skinDenick")) {
            denickDefault = genCat.get("skinDenick").getBoolean(true);
        }
        p = config.get("general", "denick", denickDefault);
        p.comment = "Master toggle for nick denicking (tab leak, skin match, and Bordic stat fingerprint).";
        denick = p.getBoolean(denickDefault);

        p = config.get("general", "removeFinalKill", false);
        p.comment = "Automatically remove a player from the overlay when they get final killed.";
        removeFinalKill = p.getBoolean(false);

        // ── General: Chat Notifications ───────────────────────────────────────
        p = config.get("general", "sendNickedToChat", true);
        p.comment = "Print a chat message when a nicked player is detected.";
        sendNickedToChat = p.getBoolean(true);

        p = config.get("general", "sendUrchinReasonToChat", false);
        p.comment = "Print tag reasons in chat when a tagged player is found (includes Urchin/Coral and Seraph source).";
        sendUrchinReasonToChat = p.getBoolean(false);

        p = config.get("general", "disableInLobby", true);
        p.comment = "In the main Bedwars lobby (not pregame), skip auto-adding players and nick/tag chat alerts.";
        disableInLobby = p.getBoolean(true);

        // ── General: Features ─────────────────────────────────────────────────
        p = config.get("general", "middleClickShop", false);
        p.comment = "Convert clicks to middle-click in Bedwars shop GUIs for instant buying. Shift-click still works for sorting.";
        middleClickShop = p.getBoolean(false);

        p = config.get("general", "fkdrColors", true);
        p.comment = "Color-code FKDR values by threat level (gray < 1.4, white < 2.4, yellow < 5, gold < 10, red < 100, dark red 100+).";
        fkdrColors = p.getBoolean(true);

        p = config.get("general", "autoWho", false);
        p.comment = "Automatically send /who when joining a Bedwars lobby to populate the overlay without typing it.";
        autoWho = p.getBoolean(false);

        p = config.get("general", "whoDelay", 0.0);
        p.comment = "Delay in seconds before sending /who (0-10). Useful to avoid rate limits or let the lobby fill.";
        whoDelay = p.getDouble(0.0);

        p = config.get("general", "hideWho", false);
        p.comment = "Hide the ONLINE: message from /who in chat.";
        hideWho = p.getBoolean(false);

        p = config.get("general", "autoPl", true);
        p.comment = "Automatically send /pl once per lobby to detect party members for dodge warning.";
        autoPl = p.getBoolean(true);

        p = config.get("general", "hidePl", true);
        p.comment = "Hide the party list response from auto /pl in chat.";
        hidePl = p.getBoolean(true);

        p = config.get("general", "teamFkdrChat", false);
        p.comment = "After game starts, send avg FKDR per team to party chat.";
        teamFkdrChat = p.getBoolean(false);

        p = config.get("general", "teamThreatChat", false);
        p.comment = "After game starts, send Bedwars team threat ratings to party chat.";
        teamThreatChat = p.getBoolean(false);

        p = config.get("general", "dodgeWarning", false);
        p.comment = "Print a chat warning when the average lobby FKDR exceeds the dodge threshold.";
        dodgeWarning = p.getBoolean(false);

        p = config.get("general", "dodgeThreshold", 3.0);
        p.comment = "Average lobby FKDR threshold for the dodge warning. Only used when dodgeWarning is enabled.";
        dodgeThreshold = p.getDouble(3.0);

        p = config.get("general", "teamThreatThreshold", 6.5);
        p.comment = "Minimum team threat score needed before the party threat notifier sends.";
        teamThreatThreshold = p.getDouble(6.5);

        p = config.get("general", "threatFkdrWeight", 0.7);
        p.comment = "Weight applied to per-player FKDR contribution in team threat scoring.";
        threatFkdrWeight = p.getDouble(0.7);

        p = config.get("general", "threatStarWeight", 0.35);
        p.comment = "Weight applied to Bedwars star contribution in team threat scoring.";
        threatStarWeight = p.getDouble(0.35);

        p = config.get("general", "threatWinstreakWeight", 0.3);
        p.comment = "Weight applied to winstreak contribution in team threat scoring.";
        threatWinstreakWeight = p.getDouble(0.3);

        p = config.get("general", "threatUrchinWeight", 1.4);
        p.comment = "Weight applied to Urchin/Coral tag severity in team threat scoring.";
        threatUrchinWeight = p.getDouble(1.4);

        p = config.get("general", "threatTeamSizeWeight", 0.9);
        p.comment = "Bonus added per extra teammate when scoring Bedwars team threat.";
        threatTeamSizeWeight = p.getDouble(0.9);

        p = config.get("general", "threatEncounterWeight", 0.25);
        p.comment = "Weight applied to encounter count contribution in team threat scoring.";
        threatEncounterWeight = p.getDouble(0.25);

        p = config.get("general", "threatNickWeight", 0.75);
        p.comment = "Bonus added for nicked players in team threat scoring.";
        threatNickWeight = p.getDouble(0.75);

        p = config.get("general", "noHurtCam", false);
        p.comment = "Disable the camera tilt effect when taking damage.";
        noHurtCam = p.getBoolean(false);

        p = config.get("general", "antiDebuff", false);
        p.comment = "Remove visual debuff effects: blindness, nausea, and slowness FOV change.";
        antiDebuff = p.getBoolean(false);

        p = config.get("general", "partyDetector", true);
        p.comment = "Detect parties joining the pregame lobby by clustered join messages.";
        partyDetector = p.getBoolean(true);
        p = config.get("general", "partyDetectorPing", false);
        p.comment = "Play a sound when PartyDetector alerts.";
        partyDetectorPing = p.getBoolean(false);
        p = config.get("general", "partyDetectorShowMissed", true);
        p.comment = "Show how many players were already in the lobby before you.";
        partyDetectorShowMissed = p.getBoolean(true);
        p = config.get("general", "partyDetectorBw2s", false);
        p.comment = "Alert for parties of 2 in Bedwars doubles.";
        partyDetectorBw2s = p.getBoolean(false);
        p = config.get("general", "partyDetectorBw3s", true);
        p.comment = "Alert for parties of 3 in Bedwars threes.";
        partyDetectorBw3s = p.getBoolean(true);
        p = config.get("general", "partyDetectorBw4s", true);
        p.comment = "Alert for parties of 4 in Bedwars fours.";
        partyDetectorBw4s = p.getBoolean(true);
        p = config.get("general", "partyDetectorBw4v4", false);
        p.comment = "Alert for parties of 4 in Bedwars 4v4.";
        partyDetectorBw4v4 = p.getBoolean(false);

        p = config.get("general", "gameResultChat", true);
        p.comment = "After a Bedwars game ends, print your teammates' kills, final kills, and beds broken.";
        gameResultChat = p.getBoolean(true);

        p = config.get("general", "statFilter", false);
        p.comment = "Only show players who meet min FKDR or min stars (OR).";
        statFilter = p.getBoolean(false);
        p = config.get("general", "statFilterMinFkdr", 0.0);
        p.comment = "Min FKDR to keep a player when Stat Filter is on (0 = ignore FKDR).";
        statFilterMinFkdr = Math.max(0.0, p.getDouble(0.0));
        p = config.get("general", "statFilterMinStars", 0);
        p.comment = "Min stars to keep a player when Stat Filter is on (0 = ignore stars).";
        statFilterMinStars = Math.max(0, p.getInt(0));
        p = config.get("general", "statFilterChat", false);
        p.comment = "Also print a chat line when a player matches the Stat Filter.";
        statFilterChat = p.getBoolean(false);

        p = config.get("general", "debug", false);
        p.comment = "Show debug messages in chat for API calls, player detection, and status changes.";
        debug = p.getBoolean(false);

        // ── General: Sorting ──────────────────────────────────────────────────
        p = config.get("general", "sortByIndex", 2);
        p.comment = "Sort overlay by: 0 = Encounters, 1 = Star, 2 = FKDR, 3 = Join Order, 4 = Winstreak, 5 = Join Time";
        sortByIndex = p.getInt(2);

        p = config.get("general", "sortMode", 0);
        p.comment = "Sort direction: 0 = Ascending (highest on top), 1 = Descending (lowest on top)";
        sortMode = p.getInt(0);

        p = config.get("general", "winstreakMode", 0);
        p.comment = "Winstreak mode: 0 = Overall, 1 = Solos, 2 = Doubles, 3 = Threes, 4 = Fours, 5 = 4v4";
        winstreakMode = p.getInt(0);

        p = config.get("general", "encountersTimeoutMins", 30);
        p.comment = "Minutes before an encounter entry expires and the count resets.";
        encountersTimeoutMins = p.getInt(30);

        // ── Columns ───────────────────────────────────────────────────────────
        p = config.get("columns", "colEncounters", true);
        p.comment = "Show the Encounters column (how many times you've seen this player).";
        colEncounters = p.getBoolean(true);

        p = config.get("columns", "colUsername", true);
        p.comment = "Show the Username column.";
        colUsername = p.getBoolean(true);

        p = config.get("columns", "colRank", true);
        p.comment = "Show the Rank column ([MVP+], [NICK], [NON], etc.).";
        colRank = p.getBoolean(true);

        p = config.get("columns", "colStar", true);
        p.comment = "Show the Star (level) column.";
        colStar = p.getBoolean(true);

        p = config.get("columns", "colFkdr", true);
        p.comment = "Show the FKDR (Final Kill/Death Ratio) column.";
        colFkdr = p.getBoolean(true);

        p = config.get("columns", "colWlr", false);
        p.comment = "Show overall WLR (Wins/Losses).";
        colWlr = p.getBoolean(false);

        p = config.get("columns", "colBblr", false);
        p.comment = "Show overall BBLR (Beds Broken/Lost).";
        colBblr = p.getBoolean(false);

        p = config.get("columns", "colKdr", false);
        p.comment = "Show overall KDR (Kills/Deaths).";
        colKdr = p.getBoolean(false);

        p = config.get("columns", "colKills", false);
        p.comment = "Show overall Bedwars kills.";
        colKills = p.getBoolean(false);

        p = config.get("columns", "colFinals", false);
        p.comment = "Show overall Bedwars final kills.";
        colFinals = p.getBoolean(false);

        p = config.get("columns", "colBeds", false);
        p.comment = "Show overall beds broken.";
        colBeds = p.getBoolean(false);

        p = config.get("columns", "colWins", false);
        p.comment = "Show overall Bedwars wins.";
        colWins = p.getBoolean(false);

        p = config.get("columns", "colDailyFkdr", false);
        p.comment = "Show daily FKDR from Bordic sessions.";
        colDailyFkdr = p.getBoolean(false);

        p = config.get("columns", "colDailyWlr", false);
        p.comment = "Show daily WLR from Bordic sessions.";
        colDailyWlr = p.getBoolean(false);

        p = config.get("columns", "colDailyStars", false);
        p.comment = "Show daily Stars from Bordic sessions.";
        colDailyStars = p.getBoolean(false);

        p = config.get("columns", "colDailyBblr", false);
        p.comment = "Show daily BBLR from Bordic sessions.";
        colDailyBblr = p.getBoolean(false);

        p = config.get("columns", "colDailyKdr", false);
        p.comment = "Show daily KDR from Bordic sessions.";
        colDailyKdr = p.getBoolean(false);

        p = config.get("columns", "colWeeklyFkdr", false);
        p.comment = "Show weekly FKDR from Bordic sessions.";
        colWeeklyFkdr = p.getBoolean(false);

        p = config.get("columns", "colWeeklyWlr", false);
        p.comment = "Show weekly WLR from Bordic sessions.";
        colWeeklyWlr = p.getBoolean(false);

        p = config.get("columns", "colWeeklyStars", false);
        p.comment = "Show weekly Stars from Bordic sessions.";
        colWeeklyStars = p.getBoolean(false);

        p = config.get("columns", "colWeeklyBblr", false);
        p.comment = "Show weekly BBLR from Bordic sessions.";
        colWeeklyBblr = p.getBoolean(false);

        p = config.get("columns", "colWeeklyKdr", false);
        p.comment = "Show weekly KDR from Bordic sessions.";
        colWeeklyKdr = p.getBoolean(false);

        p = config.get("columns", "colMonthlyFkdr", false);
        p.comment = "Show monthly FKDR from Bordic sessions.";
        colMonthlyFkdr = p.getBoolean(false);

        p = config.get("columns", "colMonthlyWlr", false);
        p.comment = "Show monthly WLR from Bordic sessions.";
        colMonthlyWlr = p.getBoolean(false);

        p = config.get("columns", "colMonthlyStars", false);
        p.comment = "Show monthly Stars from Bordic sessions.";
        colMonthlyStars = p.getBoolean(false);

        p = config.get("columns", "colMonthlyBblr", false);
        p.comment = "Show monthly BBLR from Bordic sessions.";
        colMonthlyBblr = p.getBoolean(false);

        p = config.get("columns", "colMonthlyKdr", false);
        p.comment = "Show monthly KDR from Bordic sessions.";
        colMonthlyKdr = p.getBoolean(false);

        p = config.get("columns", "colWinstreaks", true);
        p.comment = "Show the Winstreak column.";
        colWinstreaks = p.getBoolean(true);

        p = config.get("columns", "colUrchin", true);
        p.comment = "Show the Tags column (Urchin/Coral + Seraph cheater detection).";
        colUrchin = p.getBoolean(true);

        p = config.get("columns", "colSession", true);
        p.comment = "Show the Session column (how long the player has been online).";
        colSession = p.getBoolean(true);

        p = config.get("columns", "colLevel", false);
        p.comment = "Show the Hypixel network level column.";
        colLevel = p.getBoolean(false);

        p = config.get("columns", "colPing", false);
        p.comment = "Show average ping from Bordic (historical daily avg, not live tab ping).";
        colPing = p.getBoolean(false);

        p = config.get("columns", "colOrder", "encounters,username,rank,star,fkdr,wlr,bblr,kdr,winstreaks,urchin,session,ping,level");
        p.comment = "Comma-separated column display order.";
        colOrder = p.getString();

        // ── Position ──────────────────────────────────────────────────────────
        p = config.get("position", "overlayX", 2);
        p.comment = "Horizontal position in pixels from the left edge.";
        overlayX = p.getInt(2);

        p = config.get("position", "overlayY", 2);
        p.comment = "Vertical position in pixels from the top edge.";
        overlayY = p.getInt(2);

        p = config.get("position", "overlayColGap", 12);
        p.comment = "Horizontal spacing between overlay columns (pixels).";
        overlayColGap = clamp(p.getInt(12), 0, 40);

        p = config.get("position", "overlayRowGap", 5);
        p.comment = "Vertical spacing between overlay rows (pixels).";
        overlayRowGap = clamp(p.getInt(5), 0, 20);

        p = config.get("position", "overlayScalePercent", 100);
        p.comment = "Overlay scale as a percentage. 100 = normal size, 150 = 150%, 50 = half size.";
        overlayScalePercent = clamp(p.getInt(100), 50, 200);

        // ── Colors ────────────────────────────────────────────────────────────
        p = config.get("colors", "bgOpacity", 170);
        p.comment = "Background transparency. 0 = fully transparent, 255 = fully opaque.";
        bgOpacity = clamp(p.getInt(170), 0, 255);

        p = config.get("colors", "bgHue", 0);
        p.comment = "Legacy background hue (migrated to bg RGB).";
        bgHue = p.getInt(0);

        boolean hasBgRgb = config.getCategory("colors").containsKey("bgR");
        int[] migratedBg = unpackHueToRgb(bgHue);
        p = config.get("colors", "bgR", hasBgRgb ? 0 : migratedBg[0]);
        p.comment = "Background red (0-255).";
        bgR = clamp(p.getInt(hasBgRgb ? 0 : migratedBg[0]), 0, 255);
        p = config.get("colors", "bgG", hasBgRgb ? 0 : migratedBg[1]);
        p.comment = "Background green (0-255).";
        bgG = clamp(p.getInt(hasBgRgb ? 0 : migratedBg[1]), 0, 255);
        p = config.get("colors", "bgB", hasBgRgb ? 0 : migratedBg[2]);
        p.comment = "Background blue (0-255).";
        bgB = clamp(p.getInt(hasBgRgb ? 0 : migratedBg[2]), 0, 255);

        p = config.get("colors", "headerHue", 290);
        p.comment = "Legacy header hue (migrated to per-column header RGB).";
        headerHue = p.getInt(290);

        p = config.get("colors", "borderHue", 360);
        p.comment = "Legacy border hue (migrated to outline settings).";
        borderHue = p.getInt(360);

        boolean hasOutline = config.getCategory("colors").containsKey("outlineEnabled");
        p = config.get("colors", "outlineEnabled", true);
        p.comment = "Draw the overlay outline/border.";
        outlineEnabled = p.getBoolean(true);

        p = config.get("colors", "outlineChroma", !hasOutline && borderHue == 360);
        p.comment = "Animate outline color through the rainbow (overrides outline RGB).";
        outlineChroma = p.getBoolean(!hasOutline && borderHue == 360);

        int[] migratedBorder = unpackHueToRgb(borderHue);
        p = config.get("colors", "outlineR", hasOutline ? 255 : migratedBorder[0]);
        p.comment = "Outline red (0-255). Used when outline chroma is off.";
        outlineR = clamp(p.getInt(hasOutline ? 255 : migratedBorder[0]), 0, 255);
        p = config.get("colors", "outlineG", hasOutline ? 255 : migratedBorder[1]);
        p.comment = "Outline green (0-255). Used when outline chroma is off.";
        outlineG = clamp(p.getInt(hasOutline ? 255 : migratedBorder[1]), 0, 255);
        p = config.get("colors", "outlineB", hasOutline ? 255 : migratedBorder[2]);
        p.comment = "Outline blue (0-255). Used when outline chroma is off.";
        outlineB = clamp(p.getInt(hasOutline ? 255 : migratedBorder[2]), 0, 255);

        p = config.get("colors", "borderRadius", 0);
        p.comment = "Overlay corner radius in pixels. 0 = sharp corners.";
        borderRadius = clamp(p.getInt(0), 0, 16);

        p = config.get("colors", "outlineWidth", 2.5);
        p.comment = "Outline stroke width in pixels.";
        outlineWidth = (float) clampDbl(p.getDouble(2.5), 0.5, 8.0);

        p = config.get("colors", "overlayPad", 0);
        p.comment = "Extra inner padding around overlay content (pixels).";
        overlayPad = clamp(p.getInt(0), 0, 24);

        p = config.get("colors", "textShadow", true);
        p.comment = "Draw text with a drop shadow.";
        textShadow = p.getBoolean(true);

        p = config.get("colors", "headerBold", true);
        p.comment = "Draw column headers in bold.";
        headerBold = p.getBoolean(true);

        p = config.get("colors", "stripeEnabled", false);
        p.comment = "Alternating row stripe tint.";
        stripeEnabled = p.getBoolean(false);
        p = config.get("colors", "stripeR", 255);
        p.comment = "Stripe red (0-255).";
        stripeR = clamp(p.getInt(255), 0, 255);
        p = config.get("colors", "stripeG", 255);
        p.comment = "Stripe green (0-255).";
        stripeG = clamp(p.getInt(255), 0, 255);
        p = config.get("colors", "stripeB", 255);
        p.comment = "Stripe blue (0-255).";
        stripeB = clamp(p.getInt(255), 0, 255);
        p = config.get("colors", "stripeA", 18);
        p.comment = "Stripe alpha (0-255).";
        stripeA = clamp(p.getInt(18), 0, 255);

        p = config.get("colors", "highlightSelf", false);
        p.comment = "Tint your own row.";
        highlightSelf = p.getBoolean(false);
        p = config.get("colors", "highlightSelfR", 80);
        highlightSelfR = clamp(p.getInt(80), 0, 255);
        p = config.get("colors", "highlightSelfG", 180);
        highlightSelfG = clamp(p.getInt(180), 0, 255);
        p = config.get("colors", "highlightSelfB", 255);
        highlightSelfB = clamp(p.getInt(255), 0, 255);
        p = config.get("colors", "highlightSelfA", 40);
        highlightSelfA = clamp(p.getInt(40), 0, 255);

        p = config.get("colors", "highlightParty", false);
        p.comment = "Tint party members' rows.";
        highlightParty = p.getBoolean(false);
        p = config.get("colors", "highlightPartyR", 80);
        highlightPartyR = clamp(p.getInt(80), 0, 255);
        p = config.get("colors", "highlightPartyG", 255);
        highlightPartyG = clamp(p.getInt(255), 0, 255);
        p = config.get("colors", "highlightPartyB", 120);
        highlightPartyB = clamp(p.getInt(120), 0, 255);
        p = config.get("colors", "highlightPartyA", 40);
        highlightPartyA = clamp(p.getInt(40), 0, 255);

        p = config.get("colors", "highlightNicked", false);
        p.comment = "Tint unresolved nicked players' rows.";
        highlightNicked = p.getBoolean(false);
        p = config.get("colors", "highlightNickedR", 255);
        highlightNickedR = clamp(p.getInt(255), 0, 255);
        p = config.get("colors", "highlightNickedG", 220);
        highlightNickedG = clamp(p.getInt(220), 0, 255);
        p = config.get("colors", "highlightNickedB", 60);
        highlightNickedB = clamp(p.getInt(60), 0, 255);
        p = config.get("colors", "highlightNickedA", 45);
        highlightNickedA = clamp(p.getInt(45), 0, 255);

        p = config.get("colors", "highlightTagged", false);
        p.comment = "Tint rows with cheater tags.";
        highlightTagged = p.getBoolean(false);
        p = config.get("colors", "highlightTaggedR", 255);
        highlightTaggedR = clamp(p.getInt(255), 0, 255);
        p = config.get("colors", "highlightTaggedG", 60);
        highlightTaggedG = clamp(p.getInt(60), 0, 255);
        p = config.get("colors", "highlightTaggedB", 60);
        highlightTaggedB = clamp(p.getInt(60), 0, 255);
        p = config.get("colors", "highlightTaggedA", 50);
        highlightTaggedA = clamp(p.getInt(50), 0, 255);

        p = config.get("colors", "fkdrDecimals", 2);
        p.comment = "Decimal places for FKDR / WLR / BBLR / KDR (0-3).";
        fkdrDecimals = clamp(p.getInt(2), 0, 3);

        p = config.get("colors", "abbreviateNumbers", false);
        p.comment = "Abbreviate large counts (1200 → 1.2k).";
        abbreviateNumbers = p.getBoolean(false);

        p = config.get("colors", "pingStyle", 0);
        p.comment = "Ping display. 0 = number, 1 = with ms suffix.";
        pingStyle = clamp(p.getInt(0), 0, 1);

        int[] headerRgb = unpackHueToRgb(headerHue);
        headerAllR = headerRgb[0];
        headerAllG = headerRgb[1];
        headerAllB = headerRgb[2];
        initDefaultHeaderColors(headerRgb);
        // Migrate old per-column fields if present
        migrateLegacyHeaderField("headerPlayer", "player", headerRgb);
        migrateLegacyHeaderField("headerStar", "star", headerRgb);
        migrateLegacyHeaderField("headerPing", "ping", headerRgb);
        p = config.get("colors", "headerColors", serializeHeaderColors());
        p.comment = "Per-column header RGB. Format: key:r,g,b;key:r,g,b";
        parseHeaderColors(p.getString());

        p = config.get("colors", "headerAllR", headerAllR);
        p.comment = "Bulk header red — used by All Headers in click GUI.";
        headerAllR = clamp(p.getInt(headerAllR), 0, 255);
        p = config.get("colors", "headerAllG", headerAllG);
        p.comment = "Bulk header green — used by All Headers in click GUI.";
        headerAllG = clamp(p.getInt(headerAllG), 0, 255);
        p = config.get("colors", "headerAllB", headerAllB);
        p.comment = "Bulk header blue — used by All Headers in click GUI.";
        headerAllB = clamp(p.getInt(headerAllB), 0, 255);
        // New columns added after a config was saved may be missing from headerColors;
        // seed them from All so they stay in sync with bulk edits.
        ensureAllHeaderKeysPresent();

        p = config.get("colors", "mellowOuterR", 0);
        p.comment = "Mellow outer panel red.";
        mellowOuterR = clamp(p.getInt(0), 0, 255);
        p = config.get("colors", "mellowOuterG", 0);
        p.comment = "Mellow outer panel green.";
        mellowOuterG = clamp(p.getInt(0), 0, 255);
        p = config.get("colors", "mellowOuterB", 0);
        p.comment = "Mellow outer panel blue.";
        mellowOuterB = clamp(p.getInt(0), 0, 255);
        p = config.get("colors", "mellowOuterA", 128);
        p.comment = "Mellow outer panel alpha.";
        mellowOuterA = clamp(p.getInt(128), 0, 255);

        p = config.get("colors", "mellowHeaderR", 255);
        p.comment = "Mellow header row red.";
        mellowHeaderR = clamp(p.getInt(255), 0, 255);
        p = config.get("colors", "mellowHeaderG", 255);
        p.comment = "Mellow header row green.";
        mellowHeaderG = clamp(p.getInt(255), 0, 255);
        p = config.get("colors", "mellowHeaderB", 255);
        p.comment = "Mellow header row blue.";
        mellowHeaderB = clamp(p.getInt(255), 0, 255);
        p = config.get("colors", "mellowHeaderA", 32);
        p.comment = "Mellow header row alpha.";
        mellowHeaderA = clamp(p.getInt(32), 0, 255);

        p = config.get("colors", "mellowRowR", 255);
        p.comment = "Mellow player row red.";
        mellowRowR = clamp(p.getInt(255), 0, 255);
        p = config.get("colors", "mellowRowG", 255);
        p.comment = "Mellow player row green.";
        mellowRowG = clamp(p.getInt(255), 0, 255);
        p = config.get("colors", "mellowRowB", 255);
        p.comment = "Mellow player row blue.";
        mellowRowB = clamp(p.getInt(255), 0, 255);
        p = config.get("colors", "mellowRowA", 32);
        p.comment = "Mellow player row alpha.";
        mellowRowA = clamp(p.getInt(32), 0, 255);

        p = config.get("colors", "mellowTaggedR", 0);
        p.comment = "Mellow tagged-row red.";
        mellowTaggedR = clamp(p.getInt(0), 0, 255);
        p = config.get("colors", "mellowTaggedG", 0);
        p.comment = "Mellow tagged-row green.";
        mellowTaggedG = clamp(p.getInt(0), 0, 255);
        p = config.get("colors", "mellowTaggedB", 0);
        p.comment = "Mellow tagged-row blue.";
        mellowTaggedB = clamp(p.getInt(0), 0, 255);
        p = config.get("colors", "mellowTaggedA", 153);
        p.comment = "Mellow tagged-row alpha.";
        mellowTaggedA = clamp(p.getInt(153), 0, 255);

        p = config.get("colors", "overlayTheme", 0);
        p.comment = "Overlay theme. 0 = Lazify HUD, 1 = Nerdify HUD, 2 = Mellow (tab list stats).";
        overlayTheme = p.getInt(0);
        if (legacyStatsMode >= 1 && overlayTheme < 2) {
            overlayTheme = 2;
        }
        overlayTheme = Math.max(0, Math.min(2, overlayTheme));

        p = config.get("colors", "fkdrColor1", "7");
        p.comment = "Legacy FKDR § color (migrated to fkdrScale).";
        fkdrColor1 = p.getString();
        p = config.get("colors", "fkdrColor2", "f");
        fkdrColor2 = p.getString();
        p = config.get("colors", "fkdrColor3", "e");
        fkdrColor3 = p.getString();
        p = config.get("colors", "fkdrColor4", "6");
        fkdrColor4 = p.getString();
        p = config.get("colors", "fkdrColor5", "c");
        fkdrColor5 = p.getString();
        p = config.get("colors", "fkdrColor6", "4");
        fkdrColor6 = p.getString();
        p = config.get("colors", "fkdrColor7", "5");
        fkdrColor7 = p.getString();

        ThresholdColorScale legacyFkdr = ThresholdColorScale.fromLegacyFkdrCodes(
                new String[]{fkdrColor1, fkdrColor2, fkdrColor3, fkdrColor4, fkdrColor5, fkdrColor6, fkdrColor7});
        boolean hasFkdrScale = config.getCategory("colors").containsKey("fkdrScale");
        p = config.get("colors", "fkdrScale", legacyFkdr.serialize());
        p.comment = "FKDR/WLR/BBLR/KDR color tiers: min:r,g,b;min:r,g,b";
        fkdrScale = ThresholdColorScale.parse(p.getString(), hasFkdrScale ? ThresholdColorScale.defaultFkdr() : legacyFkdr);

        p = config.get("colors", "wsScale", ThresholdColorScale.defaultWinstreak().serialize());
        p.comment = "Winstreak color tiers: min:r,g,b;...";
        wsScale = ThresholdColorScale.parse(p.getString(), ThresholdColorScale.defaultWinstreak());

        p = config.get("colors", "pingScale", ThresholdColorScale.defaultPing().serialize());
        p.comment = "Ping color tiers (ms): min:r,g,b;...";
        pingScale = ThresholdColorScale.parse(p.getString(), ThresholdColorScale.defaultPing());

        p = config.get("colors", "sessionScale", ThresholdColorScale.defaultSessionMinutes().serialize());
        p.comment = "Session color tiers (minutes): min:r,g,b;...";
        sessionScale = ThresholdColorScale.parse(p.getString(), ThresholdColorScale.defaultSessionMinutes());

        p = config.get("colors", "encountersScale", ThresholdColorScale.defaultEncounters().serialize());
        p.comment = "Encounters color tiers: min:r,g,b;...";
        encountersScale = ThresholdColorScale.parse(p.getString(), ThresholdColorScale.defaultEncounters());

        p = config.get("colors", "countsScale", ThresholdColorScale.defaultCounts().serialize());
        p.comment = "Kills/Finals/Beds/Wins color tiers: min:r,g,b;...";
        countsScale = ThresholdColorScale.parse(p.getString(), ThresholdColorScale.defaultCounts());

        p = config.get("colors", "periodStarsScale", ThresholdColorScale.defaultPeriodStars().serialize());
        p.comment = "Daily/weekly/monthly stars-gained color tiers: min:r,g,b;...";
        periodStarsScale = ThresholdColorScale.parse(p.getString(), ThresholdColorScale.defaultPeriodStars());

        p = config.get("general", "wsColors", true);
        p.comment = "Color-code winstreak values.";
        wsColors = p.getBoolean(true);
        p = config.get("general", "pingColors", true);
        p.comment = "Color-code ping values.";
        pingColors = p.getBoolean(true);
        p = config.get("general", "sessionColors", true);
        p.comment = "Color-code session duration.";
        sessionColors = p.getBoolean(true);
        p = config.get("general", "encountersColors", true);
        p.comment = "Color-code encounter counts.";
        encountersColors = p.getBoolean(true);
        p = config.get("general", "countColors", true);
        p.comment = "Color-code kills/finals/beds/wins.";
        countColors = p.getBoolean(true);
        p = config.get("general", "periodStarsColors", true);
        p.comment = "Color-code daily/weekly/monthly stars gained.";
        periodStarsColors = p.getBoolean(true);

        // ── Clean up stale properties from old versions ───────────────────────
        cleanStaleProperties();
    }

    private void cleanStaleProperties() {
        // Properties removed during API/settings revamp
        String[] staleApi = {"mellowKey", "mellowUrl", "usePrism"};
        for (String key : staleApi) {
            if (config.getCategory("api").containsKey(key))
                config.getCategory("api").remove(key);
        }

        String[] staleColumns = {"colKillMsg"};
        for (String key : staleColumns) {
            if (config.getCategory("columns").containsKey(key))
                config.getCategory("columns").remove(key);
        }

        String[] staleGeneral = {
            "addTaggedToEnemy", "useprism", "autoRequeue", "statsDisplayMode",
            "anticheatEnabled", "acScaffold", "acEagle", "acAutoBlock", "acNoSlow",
            "acVerbose", "acVl", "acCooldown", "skinDenick"
        };
        for (String key : staleGeneral) {
            if (config.getCategory("general").containsKey(key))
                config.getCategory("general").remove(key);
        }
    }

    public void save() {
        if (config == null) return;
        // Use category.get() to avoid config.get() which wipes comments
        ConfigCategory api = config.getCategory("api");
        ConfigCategory gen = config.getCategory("general");
        ConfigCategory col = config.getCategory("columns");
        ConfigCategory pos = config.getCategory("position");
        ConfigCategory clr = config.getCategory("colors");

        api.get("urchinKey").set(urchinKey);
        api.get("seraphKey").set(seraphKey);
        api.get("bordicKey").set(bordicKey);
        api.get("hypixelKey").set(hypixelKey);
        gen.get("debug").set(debug);
        gen.get("keybindHold").set(keybindHold);
        gen.get("showOnTab").set(showOnTab);
        gen.get("overlayOverTab").set(overlayOverTab);
        gen.get("keybind").set(keybind);
        gen.get("teams").set(teams);
        gen.get("teamPrefix").set(teamPrefix);
        gen.get("showYourself").set(showYourself);
        gen.get("sendNickedToChat").set(sendNickedToChat);
        gen.get("sendUrchinReasonToChat").set(sendUrchinReasonToChat);
        gen.get("disableInLobby").set(disableInLobby);
        gen.get("showRanks").set(showRanks);
        gen.get("removeFinalKill").set(removeFinalKill);
        gen.get("autoTablist").set(autoTablist);
        gen.get("clearOnWho").set(clearOnWho);
        gen.get("middleClickShop").set(middleClickShop);
        gen.get("denick").set(denick);
        gen.get("fkdrColors").set(fkdrColors);
        gen.get("autoWho").set(autoWho);
        gen.get("whoDelay").set(whoDelay);
        gen.get("hideWho").set(hideWho);
        gen.get("autoPl").set(autoPl);
        gen.get("hidePl").set(hidePl);
        gen.get("teamFkdrChat").set(teamFkdrChat);
        gen.get("teamThreatChat").set(teamThreatChat);
        gen.get("dodgeWarning").set(dodgeWarning);
        gen.get("dodgeThreshold").set(dodgeThreshold);
        gen.get("teamThreatThreshold").set(teamThreatThreshold);
        gen.get("threatFkdrWeight").set(threatFkdrWeight);
        gen.get("threatStarWeight").set(threatStarWeight);
        gen.get("threatWinstreakWeight").set(threatWinstreakWeight);
        gen.get("threatUrchinWeight").set(threatUrchinWeight);
        gen.get("threatTeamSizeWeight").set(threatTeamSizeWeight);
        gen.get("threatEncounterWeight").set(threatEncounterWeight);
        gen.get("threatNickWeight").set(threatNickWeight);
        gen.get("noHurtCam").set(noHurtCam);
        gen.get("antiDebuff").set(antiDebuff);
        gen.get("partyDetector").set(partyDetector);
        gen.get("partyDetectorPing").set(partyDetectorPing);
        gen.get("partyDetectorShowMissed").set(partyDetectorShowMissed);
        gen.get("partyDetectorBw2s").set(partyDetectorBw2s);
        gen.get("partyDetectorBw3s").set(partyDetectorBw3s);
        gen.get("partyDetectorBw4s").set(partyDetectorBw4s);
        gen.get("partyDetectorBw4v4").set(partyDetectorBw4v4);
        gen.get("gameResultChat").set(gameResultChat);
        gen.get("statFilter").set(statFilter);
        gen.get("statFilterMinFkdr").set(statFilterMinFkdr);
        gen.get("statFilterMinStars").set(statFilterMinStars);
        gen.get("statFilterChat").set(statFilterChat);
        gen.get("encountersTimeoutMins").set(encountersTimeoutMins);
        gen.get("sortByIndex").set(sortByIndex);
        gen.get("sortMode").set(sortMode);
        gen.get("winstreakMode").set(winstreakMode);
        col.get("colEncounters").set(colEncounters);
        col.get("colUsername").set(colUsername);
        col.get("colRank").set(colRank);
        col.get("colStar").set(colStar);
        col.get("colFkdr").set(colFkdr);
        col.get("colWlr").set(colWlr);
        col.get("colBblr").set(colBblr);
        col.get("colKdr").set(colKdr);
        col.get("colKills").set(colKills);
        col.get("colFinals").set(colFinals);
        col.get("colBeds").set(colBeds);
        col.get("colWins").set(colWins);
        col.get("colDailyFkdr").set(colDailyFkdr);
        col.get("colDailyWlr").set(colDailyWlr);
        col.get("colDailyStars").set(colDailyStars);
        col.get("colDailyBblr").set(colDailyBblr);
        col.get("colDailyKdr").set(colDailyKdr);
        col.get("colWeeklyFkdr").set(colWeeklyFkdr);
        col.get("colWeeklyWlr").set(colWeeklyWlr);
        col.get("colWeeklyStars").set(colWeeklyStars);
        col.get("colWeeklyBblr").set(colWeeklyBblr);
        col.get("colWeeklyKdr").set(colWeeklyKdr);
        col.get("colMonthlyFkdr").set(colMonthlyFkdr);
        col.get("colMonthlyWlr").set(colMonthlyWlr);
        col.get("colMonthlyStars").set(colMonthlyStars);
        col.get("colMonthlyBblr").set(colMonthlyBblr);
        col.get("colMonthlyKdr").set(colMonthlyKdr);
        col.get("colWinstreaks").set(colWinstreaks);
        col.get("colUrchin").set(colUrchin);
        col.get("colSession").set(colSession);
        col.get("colLevel").set(colLevel);
        col.get("colPing").set(colPing);
        col.get("colOrder").set(colOrder);
        pos.get("overlayX").set(overlayX);
        pos.get("overlayY").set(overlayY);
        pos.get("overlayColGap").set(overlayColGap);
        pos.get("overlayRowGap").set(overlayRowGap);
        pos.get("overlayScalePercent").set(overlayScalePercent);
        clr.get("bgOpacity").set(bgOpacity);
        clr.get("bgHue").set(bgHue);
        clr.get("bgR").set(bgR);
        clr.get("bgG").set(bgG);
        clr.get("bgB").set(bgB);
        clr.get("headerHue").set(headerHue);
        clr.get("borderHue").set(borderHue);
        clr.get("outlineEnabled").set(outlineEnabled);
        clr.get("outlineChroma").set(outlineChroma);
        clr.get("outlineR").set(outlineR);
        clr.get("outlineG").set(outlineG);
        clr.get("outlineB").set(outlineB);
        clr.get("outlineWidth").set(outlineWidth);
        clr.get("borderRadius").set(borderRadius);
        clr.get("overlayPad").set(overlayPad);
        clr.get("textShadow").set(textShadow);
        clr.get("headerBold").set(headerBold);
        clr.get("stripeEnabled").set(stripeEnabled);
        clr.get("stripeR").set(stripeR);
        clr.get("stripeG").set(stripeG);
        clr.get("stripeB").set(stripeB);
        clr.get("stripeA").set(stripeA);
        clr.get("highlightSelf").set(highlightSelf);
        clr.get("highlightSelfR").set(highlightSelfR);
        clr.get("highlightSelfG").set(highlightSelfG);
        clr.get("highlightSelfB").set(highlightSelfB);
        clr.get("highlightSelfA").set(highlightSelfA);
        clr.get("highlightParty").set(highlightParty);
        clr.get("highlightPartyR").set(highlightPartyR);
        clr.get("highlightPartyG").set(highlightPartyG);
        clr.get("highlightPartyB").set(highlightPartyB);
        clr.get("highlightPartyA").set(highlightPartyA);
        clr.get("highlightNicked").set(highlightNicked);
        clr.get("highlightNickedR").set(highlightNickedR);
        clr.get("highlightNickedG").set(highlightNickedG);
        clr.get("highlightNickedB").set(highlightNickedB);
        clr.get("highlightNickedA").set(highlightNickedA);
        clr.get("highlightTagged").set(highlightTagged);
        clr.get("highlightTaggedR").set(highlightTaggedR);
        clr.get("highlightTaggedG").set(highlightTaggedG);
        clr.get("highlightTaggedB").set(highlightTaggedB);
        clr.get("highlightTaggedA").set(highlightTaggedA);
        clr.get("fkdrDecimals").set(fkdrDecimals);
        clr.get("abbreviateNumbers").set(abbreviateNumbers);
        clr.get("pingStyle").set(pingStyle);
        clr.get("headerColors").set(serializeHeaderColors());
        clr.get("headerAllR").set(headerAllR);
        clr.get("headerAllG").set(headerAllG);
        clr.get("headerAllB").set(headerAllB);
        clr.get("mellowOuterR").set(mellowOuterR);
        clr.get("mellowOuterG").set(mellowOuterG);
        clr.get("mellowOuterB").set(mellowOuterB);
        clr.get("mellowOuterA").set(mellowOuterA);
        clr.get("mellowHeaderR").set(mellowHeaderR);
        clr.get("mellowHeaderG").set(mellowHeaderG);
        clr.get("mellowHeaderB").set(mellowHeaderB);
        clr.get("mellowHeaderA").set(mellowHeaderA);
        clr.get("mellowRowR").set(mellowRowR);
        clr.get("mellowRowG").set(mellowRowG);
        clr.get("mellowRowB").set(mellowRowB);
        clr.get("mellowRowA").set(mellowRowA);
        clr.get("mellowTaggedR").set(mellowTaggedR);
        clr.get("mellowTaggedG").set(mellowTaggedG);
        clr.get("mellowTaggedB").set(mellowTaggedB);
        clr.get("mellowTaggedA").set(mellowTaggedA);
        clr.get("overlayTheme").set(overlayTheme);
        clr.get("fkdrColor1").set(fkdrColor1);
        clr.get("fkdrColor2").set(fkdrColor2);
        clr.get("fkdrColor3").set(fkdrColor3);
        clr.get("fkdrColor4").set(fkdrColor4);
        clr.get("fkdrColor5").set(fkdrColor5);
        clr.get("fkdrColor6").set(fkdrColor6);
        clr.get("fkdrColor7").set(fkdrColor7);
        clr.get("fkdrScale").set(fkdrScale.serialize());
        clr.get("wsScale").set(wsScale.serialize());
        clr.get("pingScale").set(pingScale.serialize());
        clr.get("sessionScale").set(sessionScale.serialize());
        clr.get("encountersScale").set(encountersScale.serialize());
        clr.get("countsScale").set(countsScale.serialize());
        clr.get("periodStarsScale").set(periodStarsScale.serialize());
        gen.get("wsColors").set(wsColors);
        gen.get("pingColors").set(pingColors);
        gen.get("sessionColors").set(sessionColors);
        gen.get("encountersColors").set(encountersColors);
        gen.get("countColors").set(countColors);
        gen.get("periodStarsColors").set(periodStarsColors);
        config.save();
    }

    public Configuration getConfiguration() { return config; }

    // ── Getters ────────────────────────────────────────────────────────────────
    public String  getUrchinKey()              { return urchinKey; }
    public String  getSeraphKey()              { return seraphKey; }
    public String  getBordicKey()              { return bordicKey; }
    public String  getHypixelKey()             { return hypixelKey; }
    public boolean isDebug()                   { return debug; }
    public boolean isKeybindHold()             { return keybindHold; }
    public boolean isShowOnTab()              { return showOnTab; }
    public boolean isOverlayOverTab()         { return overlayOverTab; }
    public int     getKeybind()                { return keybind; }
    public boolean isTeams()                   { return teams; }
    public boolean isTeamPrefix()              { return teamPrefix; }
    public boolean isShowYourself()            { return showYourself; }
    public boolean isSendNickedToChat()        { return sendNickedToChat; }
    public boolean isSendUrchinReasonToChat()  { return sendUrchinReasonToChat; }
    public boolean isDisableInLobby()          { return disableInLobby; }
    public boolean isShowRanks()               { return showRanks; }
    public boolean isRemoveFinalKill()         { return removeFinalKill; }
    public boolean isAutoTablist()             { return autoTablist; }
    public boolean isClearOnWho()              { return clearOnWho; }
    public boolean isMiddleClickShop()         { return middleClickShop; }
    public boolean isDenick()                  { return denick; }
    /** @deprecated use {@link #isDenick()} */
    public boolean isSkinDenick()              { return denick; }
    public boolean isFkdrColors()              { return fkdrColors; }
    public boolean isAutoWho()                 { return autoWho; }
    public double  getWhoDelay()               { return whoDelay; }
    public boolean isHideWho()                 { return hideWho; }
    public boolean isAutoPl()                  { return autoPl; }
    public boolean isHidePl()                  { return hidePl; }
    public boolean isTeamFkdrChat()            { return teamFkdrChat; }
    public boolean isTeamThreatChat()          { return teamThreatChat; }
    public boolean isDodgeWarning()            { return dodgeWarning; }
    public double  getDodgeThreshold()         { return dodgeThreshold; }
    public double  getTeamThreatThreshold()    { return teamThreatThreshold; }
    public double  getThreatFkdrWeight()       { return threatFkdrWeight; }
    public double  getThreatStarWeight()       { return threatStarWeight; }
    public double  getThreatWinstreakWeight()  { return threatWinstreakWeight; }
    public double  getThreatUrchinWeight()     { return threatUrchinWeight; }
    public double  getThreatTeamSizeWeight()   { return threatTeamSizeWeight; }
    public double  getThreatEncounterWeight()  { return threatEncounterWeight; }
    public double  getThreatNickWeight()       { return threatNickWeight; }
    public boolean isNoHurtCam()               { return noHurtCam; }
    public boolean isAntiDebuff()              { return antiDebuff; }
    public boolean isPartyDetector()           { return partyDetector; }
    public boolean isPartyDetectorPing()       { return partyDetectorPing; }
    public boolean isPartyDetectorShowMissed() { return partyDetectorShowMissed; }
    public boolean isPartyDetectorBw2s()       { return partyDetectorBw2s; }
    public boolean isPartyDetectorBw3s()       { return partyDetectorBw3s; }
    public boolean isPartyDetectorBw4s()       { return partyDetectorBw4s; }
    public boolean isPartyDetectorBw4v4()      { return partyDetectorBw4v4; }
    public boolean isGameResultChat()          { return gameResultChat; }
    public boolean isStatFilter()              { return statFilter; }
    public double  getStatFilterMinFkdr()      { return statFilterMinFkdr; }
    public int     getStatFilterMinStars()     { return statFilterMinStars; }
    public boolean isStatFilterChat()          { return statFilterChat; }
    public boolean isColEncounters()           { return colEncounters; }
    public boolean isColUsername()             { return colUsername; }
    public boolean isColRank()                 { return colRank; }
    public boolean isColStar()                 { return colStar; }
    public boolean isColFkdr()                 { return colFkdr; }
    public boolean isColWlr()                  { return colWlr; }
    public boolean isColBblr()                 { return colBblr; }
    public boolean isColKdr()                  { return colKdr; }
    public boolean isColKills()                { return colKills; }
    public boolean isColFinals()               { return colFinals; }
    public boolean isColBeds()                 { return colBeds; }
    public boolean isColWins()                 { return colWins; }
    public boolean isColDailyFkdr()            { return colDailyFkdr; }
    public boolean isColDailyWlr()             { return colDailyWlr; }
    public boolean isColDailyStars()           { return colDailyStars; }
    public boolean isColDailyBblr()            { return colDailyBblr; }
    public boolean isColDailyKdr()             { return colDailyKdr; }
    public boolean isColWeeklyFkdr()           { return colWeeklyFkdr; }
    public boolean isColWeeklyWlr()            { return colWeeklyWlr; }
    public boolean isColWeeklyStars()          { return colWeeklyStars; }
    public boolean isColWeeklyBblr()           { return colWeeklyBblr; }
    public boolean isColWeeklyKdr()            { return colWeeklyKdr; }
    public boolean isColMonthlyFkdr()          { return colMonthlyFkdr; }
    public boolean isColMonthlyWlr()           { return colMonthlyWlr; }
    public boolean isColMonthlyStars()         { return colMonthlyStars; }
    public boolean isColMonthlyBblr()          { return colMonthlyBblr; }
    public boolean isColMonthlyKdr()           { return colMonthlyKdr; }
    public boolean isColWinstreaks()           { return colWinstreaks; }
    public boolean isColUrchin()               { return colUrchin; }
    public boolean isColSession()              { return colSession; }
    public boolean isColLevel()                { return colLevel; }
    public boolean isColPing()                 { return colPing; }
    public String  getColOrder()               { return colOrder; }
    public int     getEncountersTimeoutMins()  { return encountersTimeoutMins; }
    public int     getSortByIndex()            { return sortByIndex; }
    public int     getSortMode()               { return sortMode; }
    public int     getWinstreakMode()          { return winstreakMode; }
    public int     getOverlayX()               { return overlayX; }
    public int     getOverlayY()               { return overlayY; }
    public int     getOverlayColGap()          { return overlayColGap; }
    public int     getOverlayRowGap()          { return overlayRowGap; }
    public int     getOverlayScalePercent()    { return overlayScalePercent; }
    public float   getOverlayScale()           { return overlayScalePercent / 100.0f; }
    public int     getBgOpacity()              { return bgOpacity; }
    public int     getBgR()                    { return bgR; }
    public int     getBgG()                    { return bgG; }
    public int     getBgB()                    { return bgB; }
    public int     getBgHue()                  { return bgHue; }
    public int     getHeaderHue()              { return headerHue; }
    public int     getBorderHue()              { return borderHue; }
    public boolean isOutlineEnabled()          { return outlineEnabled; }
    public boolean isOutlineChroma()           { return outlineChroma; }
    public int     getOutlineR()               { return outlineR; }
    public int     getOutlineG()               { return outlineG; }
    public int     getOutlineB()               { return outlineB; }
    public float   getOutlineWidth()           { return outlineWidth; }
    public int     getBorderRadius()           { return borderRadius; }
    public int     getOverlayPad()             { return overlayPad; }
    public boolean isTextShadow()              { return textShadow; }
    public boolean isHeaderBold()              { return headerBold; }
    public boolean isStripeEnabled()           { return stripeEnabled; }
    public int     getStripeR()                { return stripeR; }
    public int     getStripeG()                { return stripeG; }
    public int     getStripeB()                { return stripeB; }
    public int     getStripeA()                { return stripeA; }
    public int getStripeColor() {
        return com.lazify.util.ColorUtil.rgb(stripeR, stripeG, stripeB, stripeA);
    }
    public boolean isHighlightSelf()           { return highlightSelf; }
    public int getHighlightSelfR() { return highlightSelfR; }
    public int getHighlightSelfG() { return highlightSelfG; }
    public int getHighlightSelfB() { return highlightSelfB; }
    public int getHighlightSelfA() { return highlightSelfA; }
    public int getHighlightSelfColor() {
        return com.lazify.util.ColorUtil.rgb(highlightSelfR, highlightSelfG, highlightSelfB, highlightSelfA);
    }
    public boolean isHighlightParty()          { return highlightParty; }
    public int getHighlightPartyR() { return highlightPartyR; }
    public int getHighlightPartyG() { return highlightPartyG; }
    public int getHighlightPartyB() { return highlightPartyB; }
    public int getHighlightPartyA() { return highlightPartyA; }
    public int getHighlightPartyColor() {
        return com.lazify.util.ColorUtil.rgb(highlightPartyR, highlightPartyG, highlightPartyB, highlightPartyA);
    }
    public boolean isHighlightNicked()         { return highlightNicked; }
    public int getHighlightNickedR() { return highlightNickedR; }
    public int getHighlightNickedG() { return highlightNickedG; }
    public int getHighlightNickedB() { return highlightNickedB; }
    public int getHighlightNickedA() { return highlightNickedA; }
    public int getHighlightNickedColor() {
        return com.lazify.util.ColorUtil.rgb(highlightNickedR, highlightNickedG, highlightNickedB, highlightNickedA);
    }
    public boolean isHighlightTagged()         { return highlightTagged; }
    public int getHighlightTaggedR() { return highlightTaggedR; }
    public int getHighlightTaggedG() { return highlightTaggedG; }
    public int getHighlightTaggedB() { return highlightTaggedB; }
    public int getHighlightTaggedA() { return highlightTaggedA; }
    public int getHighlightTaggedColor() {
        return com.lazify.util.ColorUtil.rgb(highlightTaggedR, highlightTaggedG, highlightTaggedB, highlightTaggedA);
    }
    public int getFkdrDecimals()               { return fkdrDecimals; }
    public boolean isAbbreviateNumbers()       { return abbreviateNumbers; }
    public int getPingStyle()                  { return pingStyle; }
    public int     getHeaderAllR()             { return headerAllR; }
    public int     getHeaderAllG()             { return headerAllG; }
    public int     getHeaderAllB()             { return headerAllB; }
    public int     getOverlayTheme()           { return overlayTheme; }

    public int getMellowOuterR()  { return mellowOuterR; }
    public int getMellowOuterG()  { return mellowOuterG; }
    public int getMellowOuterB()  { return mellowOuterB; }
    public int getMellowOuterA()  { return mellowOuterA; }
    public int getMellowHeaderR() { return mellowHeaderR; }
    public int getMellowHeaderG() { return mellowHeaderG; }
    public int getMellowHeaderB() { return mellowHeaderB; }
    public int getMellowHeaderA() { return mellowHeaderA; }
    public int getMellowRowR()    { return mellowRowR; }
    public int getMellowRowG()    { return mellowRowG; }
    public int getMellowRowB()    { return mellowRowB; }
    public int getMellowRowA()    { return mellowRowA; }
    public int getMellowTaggedR() { return mellowTaggedR; }
    public int getMellowTaggedG() { return mellowTaggedG; }
    public int getMellowTaggedB() { return mellowTaggedB; }
    public int getMellowTaggedA() { return mellowTaggedA; }

    /** Packed ARGB background (RGB + opacity). */
    public int getBackgroundColor() {
        return com.lazify.util.ColorUtil.rgb(bgR, bgG, bgB, bgOpacity);
    }

    /** Packed ARGB for outline (chroma or solid RGB). */
    public int getOutlineColor() {
        if (outlineChroma) return com.lazify.util.ColorUtil.getChroma(1L, 255);
        return com.lazify.util.ColorUtil.rgb(outlineR, outlineG, outlineB);
    }

    public int getHeaderR(String colKey) { return ensureHeader(colKey)[0]; }
    public int getHeaderG(String colKey) { return ensureHeader(colKey)[1]; }
    public int getHeaderB(String colKey) { return ensureHeader(colKey)[2]; }

    public int getHeaderColor(String colKey) {
        int[] rgb = ensureHeader(colKey);
        return com.lazify.util.ColorUtil.rgb(rgb[0], rgb[1], rgb[2]);
    }

    public int getMellowOuterColor() {
        return com.lazify.util.ColorUtil.rgb(mellowOuterR, mellowOuterG, mellowOuterB, mellowOuterA);
    }
    public int getMellowHeaderColor() {
        return com.lazify.util.ColorUtil.rgb(mellowHeaderR, mellowHeaderG, mellowHeaderB, mellowHeaderA);
    }
    public int getMellowRowColor() {
        return com.lazify.util.ColorUtil.rgb(mellowRowR, mellowRowG, mellowRowB, mellowRowA);
    }
    public int getMellowTaggedColor() {
        return com.lazify.util.ColorUtil.rgb(mellowTaggedR, mellowTaggedG, mellowTaggedB, mellowTaggedA);
    }

    /** Config column name (username, star, …) → overlay column key. */
    public static String colNameToKey(String colName) {
        if (colName == null) return "";
        switch (colName.trim().toLowerCase()) {
            case "encounters": return "seen";
            case "username":   return "player";
            case "level":      return "netlevel";
            default:           return colName.trim().toLowerCase();
        }
    }

    public static String headerLabelForKey(String colKey) {
        if (colKey == null) return "?";
        switch (colKey) {
            case "player":     return "Player";
            case "rank":       return "Rank";
            case "seen":       return "Encounters";
            case "star":       return "Star";
            case "fkdr":       return "FKDR";
            case "wlr":        return "WLR";
            case "bblr":       return "BBLR";
            case "kdr":        return "KDR";
            case "kills":      return "Kills";
            case "finals":     return "Finals";
            case "beds":       return "Beds";
            case "wins":       return "Wins";
            case "dailyfkdr":   return "dfkdr";
            case "dailywlr":    return "dwlr";
            case "dailystars":  return "dstar";
            case "dailybblr":   return "dbblr";
            case "dailykdr":    return "dkdr";
            case "weeklyfkdr":  return "wfkdr";
            case "weeklywlr":   return "wwlr";
            case "weeklystars": return "wstar";
            case "weeklybblr":  return "wbblr";
            case "weeklykdr":   return "wkdr";
            case "monthlyfkdr": return "mfkdr";
            case "monthlywlr":  return "mwlr";
            case "monthlystars": return "mstar";
            case "monthlybblr": return "mbblr";
            case "monthlykdr":  return "mkdr";
            case "winstreaks": return "WS";
            case "session":    return "Session";
            case "urchin":     return "Tags";
            case "netlevel":   return "Level";
            case "ping":       return "Ping";
            default:           return colKey;
        }
    }

    public boolean isColumnEnabledByName(String colName) {
        if (colName == null) return false;
        switch (colName.trim().toLowerCase()) {
            case "encounters": return colEncounters;
            case "username":   return colUsername;
            case "rank":       return colRank;
            case "star":       return colStar;
            case "fkdr":       return colFkdr;
            case "wlr":        return colWlr;
            case "bblr":       return colBblr;
            case "kdr":        return colKdr;
            case "kills":      return colKills;
            case "finals":     return colFinals;
            case "beds":       return colBeds;
            case "wins":       return colWins;
            case "dailyfkdr":   return colDailyFkdr;
            case "dailywlr":    return colDailyWlr;
            case "dailystars":  return colDailyStars;
            case "dailybblr":   return colDailyBblr;
            case "dailykdr":    return colDailyKdr;
            case "weeklyfkdr":  return colWeeklyFkdr;
            case "weeklywlr":   return colWeeklyWlr;
            case "weeklystars": return colWeeklyStars;
            case "weeklybblr":  return colWeeklyBblr;
            case "weeklykdr":   return colWeeklyKdr;
            case "monthlyfkdr": return colMonthlyFkdr;
            case "monthlywlr":  return colMonthlyWlr;
            case "monthlystars": return colMonthlyStars;
            case "monthlybblr": return colMonthlyBblr;
            case "monthlykdr":  return colMonthlyKdr;
            case "winstreaks": return colWinstreaks;
            case "urchin":     return colUrchin;
            case "session":    return colSession;
            case "level":      return colLevel;
            case "ping":       return colPing;
            default:           return false;
        }
    }
    public String  getFkdrColor1()             { return fkdrColor1; }
    public String  getFkdrColor2()             { return fkdrColor2; }
    public String  getFkdrColor3()             { return fkdrColor3; }
    public String  getFkdrColor4()             { return fkdrColor4; }
    public String  getFkdrColor5()             { return fkdrColor5; }
    public String  getFkdrColor6()             { return fkdrColor6; }
    public String  getFkdrColor7()             { return fkdrColor7; }
    public String[] getFkdrColors()            { return new String[]{fkdrColor1, fkdrColor2, fkdrColor3, fkdrColor4, fkdrColor5, fkdrColor6, fkdrColor7}; }

    public ThresholdColorScale getFkdrScale()       { return fkdrScale; }
    public ThresholdColorScale getWsScale()         { return wsScale; }
    public ThresholdColorScale getPingScale()       { return pingScale; }
    public ThresholdColorScale getSessionScale()    { return sessionScale; }
    public ThresholdColorScale getEncountersScale() { return encountersScale; }
    public ThresholdColorScale getCountsScale()     { return countsScale; }
    public ThresholdColorScale getPeriodStarsScale(){ return periodStarsScale; }
    public boolean isWsColors()                     { return wsColors; }
    public boolean isPingColors()                   { return pingColors; }
    public boolean isSessionColors()                { return sessionColors; }
    public boolean isEncountersColors()             { return encountersColors; }
    public boolean isCountColors()                  { return countColors; }
    public boolean isPeriodStarsColors()            { return periodStarsColors; }

    public void setFkdrScale(ThresholdColorScale v) {
        fkdrScale = v != null ? v : ThresholdColorScale.defaultFkdr();
    }
    public void setWsScale(ThresholdColorScale v) {
        wsScale = v != null ? v : ThresholdColorScale.defaultWinstreak();
    }
    public void setPingScale(ThresholdColorScale v) {
        pingScale = v != null ? v : ThresholdColorScale.defaultPing();
    }
    public void setSessionScale(ThresholdColorScale v) {
        sessionScale = v != null ? v : ThresholdColorScale.defaultSessionMinutes();
    }
    public void setEncountersScale(ThresholdColorScale v) {
        encountersScale = v != null ? v : ThresholdColorScale.defaultEncounters();
    }
    public void setCountsScale(ThresholdColorScale v) {
        countsScale = v != null ? v : ThresholdColorScale.defaultCounts();
    }
    public void setPeriodStarsScale(ThresholdColorScale v) {
        periodStarsScale = v != null ? v : ThresholdColorScale.defaultPeriodStars();
    }
    public void setWsColors(boolean v)          { wsColors = v; }
    public void setPingColors(boolean v)        { pingColors = v; }
    public void setSessionColors(boolean v)     { sessionColors = v; }
    public void setEncountersColors(boolean v)  { encountersColors = v; }
    public void setCountColors(boolean v)       { countColors = v; }
    public void setPeriodStarsColors(boolean v) { periodStarsColors = v; }

    /** Any lifetime count column enabled (kills/finals/beds/wins). */
    public boolean anyCountColumnEnabled() {
        return colKills || colFinals || colBeds || colWins;
    }

    /** Any daily/weekly/monthly ratio column (uses FKDR scale). */
    public boolean anyPeriodRatioColumnEnabled() {
        return colDailyFkdr || colDailyWlr || colDailyBblr || colDailyKdr
                || colWeeklyFkdr || colWeeklyWlr || colWeeklyBblr || colWeeklyKdr
                || colMonthlyFkdr || colMonthlyWlr || colMonthlyBblr || colMonthlyKdr;
    }

    /** Any daily/weekly/monthly stars-gained column. */
    public boolean anyPeriodStarsColumnEnabled() {
        return colDailyStars || colWeeklyStars || colMonthlyStars;
    }

    // ── Setters ────────��────────────────────────────────────���──────────────────
    public void setUrchinKey(String v)             { urchinKey = v; }
    public void setSeraphKey(String v)             { seraphKey = v; }
    public void setBordicKey(String v)             { bordicKey = v; }
    public void setHypixelKey(String v)            { hypixelKey = v; }
    public void setDebug(boolean v)                { debug = v; }
    public void setKeybindHold(boolean v)          { keybindHold = v; }
    public void setShowOnTab(boolean v)            { showOnTab = v; }
    public void setOverlayOverTab(boolean v)       { overlayOverTab = v; }
    public void setKeybind(int v)                  { keybind = v; }
    public void setTeams(boolean v)                { teams = v; }
    public void setTeamPrefix(boolean v)           { teamPrefix = v; }
    public void setShowYourself(boolean v)         { showYourself = v; }
    public void setSendNickedToChat(boolean v)     { sendNickedToChat = v; }
    public void setSendUrchinReasonToChat(boolean v) { sendUrchinReasonToChat = v; }
    public void setDisableInLobby(boolean v)       { disableInLobby = v; }
    public void setShowRanks(boolean v)            { showRanks = v; }
    public void setRemoveFinalKill(boolean v)      { removeFinalKill = v; }
    public void setAutoTablist(boolean v)          { autoTablist = v; }
    public void setClearOnWho(boolean v)           { clearOnWho = v; }
    public void setMiddleClickShop(boolean v)      { middleClickShop = v; }
    public void setDenick(boolean v)               { denick = v; }
    /** @deprecated use {@link #setDenick(boolean)} */
    public void setSkinDenick(boolean v)           { denick = v; }
    public void setFkdrColors(boolean v)          { fkdrColors = v; }
    public void setAutoWho(boolean v)             { autoWho = v; }
    public void setWhoDelay(double v)             { whoDelay = v; }
    public void setHideWho(boolean v)             { hideWho = v; }
    public void setAutoPl(boolean v)              { autoPl = v; }
    public void setHidePl(boolean v)              { hidePl = v; }
    public void setTeamFkdrChat(boolean v)        { teamFkdrChat = v; }
    public void setTeamThreatChat(boolean v)      { teamThreatChat = v; }
    public void setDodgeWarning(boolean v)        { dodgeWarning = v; }
    public void setDodgeThreshold(double v)       { dodgeThreshold = v; }
    public void setTeamThreatThreshold(double v)  { teamThreatThreshold = v; }
    public void setThreatFkdrWeight(double v)     { threatFkdrWeight = v; }
    public void setThreatStarWeight(double v)     { threatStarWeight = v; }
    public void setThreatWinstreakWeight(double v){ threatWinstreakWeight = v; }
    public void setThreatUrchinWeight(double v)   { threatUrchinWeight = v; }
    public void setThreatTeamSizeWeight(double v) { threatTeamSizeWeight = v; }
    public void setThreatEncounterWeight(double v){ threatEncounterWeight = v; }
    public void setThreatNickWeight(double v)     { threatNickWeight = v; }
    public void setNoHurtCam(boolean v)           { noHurtCam = v; }
    public void setAntiDebuff(boolean v)          { antiDebuff = v; }
    public void setPartyDetector(boolean v)       { partyDetector = v; }
    public void setPartyDetectorPing(boolean v)   { partyDetectorPing = v; }
    public void setPartyDetectorShowMissed(boolean v) { partyDetectorShowMissed = v; }
    public void setPartyDetectorBw2s(boolean v)   { partyDetectorBw2s = v; }
    public void setPartyDetectorBw3s(boolean v)   { partyDetectorBw3s = v; }
    public void setPartyDetectorBw4s(boolean v)   { partyDetectorBw4s = v; }
    public void setPartyDetectorBw4v4(boolean v)  { partyDetectorBw4v4 = v; }
    public void setGameResultChat(boolean v)      { gameResultChat = v; }
    public void setStatFilter(boolean v)          { statFilter = v; }
    public void setStatFilterMinFkdr(double v)    { statFilterMinFkdr = Math.max(0.0, v); }
    public void setStatFilterMinStars(int v)      { statFilterMinStars = Math.max(0, v); }
    public void setStatFilterChat(boolean v)      { statFilterChat = v; }
    public void setColEncounters(boolean v)        { colEncounters = v; }
    public void setColUsername(boolean v)           { colUsername = v; }
    public void setColRank(boolean v)               { colRank = v; }
    public void setColStar(boolean v)              { colStar = v; }
    public void setColFkdr(boolean v)              { colFkdr = v; }
    public void setColWlr(boolean v)               { colWlr = v; }
    public void setColBblr(boolean v)              { colBblr = v; }
    public void setColKdr(boolean v)               { colKdr = v; }
    public void setColKills(boolean v)             { colKills = v; }
    public void setColFinals(boolean v)            { colFinals = v; }
    public void setColBeds(boolean v)              { colBeds = v; }
    public void setColWins(boolean v)              { colWins = v; }
    public void setColDailyFkdr(boolean v)          { colDailyFkdr = v; }
    public void setColDailyWlr(boolean v)           { colDailyWlr = v; }
    public void setColDailyStars(boolean v)         { colDailyStars = v; }
    public void setColDailyBblr(boolean v)          { colDailyBblr = v; }
    public void setColDailyKdr(boolean v)           { colDailyKdr = v; }
    public void setColWeeklyFkdr(boolean v)         { colWeeklyFkdr = v; }
    public void setColWeeklyWlr(boolean v)          { colWeeklyWlr = v; }
    public void setColWeeklyStars(boolean v)        { colWeeklyStars = v; }
    public void setColWeeklyBblr(boolean v)         { colWeeklyBblr = v; }
    public void setColWeeklyKdr(boolean v)          { colWeeklyKdr = v; }
    public void setColMonthlyFkdr(boolean v)        { colMonthlyFkdr = v; }
    public void setColMonthlyWlr(boolean v)         { colMonthlyWlr = v; }
    public void setColMonthlyStars(boolean v)       { colMonthlyStars = v; }
    public void setColMonthlyBblr(boolean v)        { colMonthlyBblr = v; }
    public void setColMonthlyKdr(boolean v)         { colMonthlyKdr = v; }
    public void setColWinstreaks(boolean v)        { colWinstreaks = v; }
    public void setColUrchin(boolean v)            { colUrchin = v; }
    public void setColSession(boolean v)           { colSession = v; }
    public void setColLevel(boolean v)             { colLevel = v; }
    public void setColPing(boolean v)              { colPing = v; }
    public void setColOrder(String v)              { colOrder = v; }
    public void setEncountersTimeoutMins(int v)    { encountersTimeoutMins = v; }
    public void setSortByIndex(int v)              { sortByIndex = v; }
    public void setSortMode(int v)                 { sortMode = v; }
    public void setWinstreakMode(int v)            { winstreakMode = v; }
    public void setOverlayX(int v)                 { overlayX = v; }
    public void setOverlayY(int v)                 { overlayY = v; }
    public void setOverlayColGap(int v)            { overlayColGap = clamp(v, 0, 40); }
    public void setOverlayRowGap(int v)            { overlayRowGap = clamp(v, 0, 20); }
    public void setOverlayScalePercent(int v)      { overlayScalePercent = clamp(v, 50, 200); }
    public void setBgOpacity(int v)                { bgOpacity = clamp(v, 0, 255); }
    public void setBgR(int v)                      { bgR = clamp(v, 0, 255); }
    public void setBgG(int v)                      { bgG = clamp(v, 0, 255); }
    public void setBgB(int v)                      { bgB = clamp(v, 0, 255); }
    /** Legacy: converts hue into bg RGB. */
    public void setBgHue(int v) {
        bgHue = v;
        int[] rgb = unpackHueToRgb(v);
        bgR = rgb[0]; bgG = rgb[1]; bgB = rgb[2];
    }
    /** Legacy: applies hue RGB to every header tag. */
    public void setHeaderHue(int v) {
        headerHue = v;
        int[] rgb = unpackHueToRgb(v);
        setAllHeaderColors(rgb[0], rgb[1], rgb[2]);
    }
    /** Legacy: converts hue into outline RGB. */
    public void setBorderHue(int v) {
        borderHue = v;
        int[] rgb = unpackHueToRgb(v);
        outlineR = rgb[0]; outlineG = rgb[1]; outlineB = rgb[2];
    }
    public void setOutlineEnabled(boolean v)       { outlineEnabled = v; }
    public void setOutlineChroma(boolean v)        { outlineChroma = v; }
    public void setOutlineR(int v)                 { outlineR = clamp(v, 0, 255); }
    public void setOutlineG(int v)                 { outlineG = clamp(v, 0, 255); }
    public void setOutlineB(int v)                 { outlineB = clamp(v, 0, 255); }
    public void setOutlineWidth(double v)          { outlineWidth = (float) clampDbl(v, 0.5, 8.0); }
    public void setBorderRadius(int v)             { borderRadius = clamp(v, 0, 16); }
    public void setOverlayPad(int v)               { overlayPad = clamp(v, 0, 24); }
    public void setTextShadow(boolean v)           { textShadow = v; }
    public void setHeaderBold(boolean v)           { headerBold = v; }
    public void setStripeEnabled(boolean v)        { stripeEnabled = v; }
    public void setStripeR(int v)                  { stripeR = clamp(v, 0, 255); }
    public void setStripeG(int v)                  { stripeG = clamp(v, 0, 255); }
    public void setStripeB(int v)                  { stripeB = clamp(v, 0, 255); }
    public void setStripeA(int v)                  { stripeA = clamp(v, 0, 255); }
    public void setHighlightSelf(boolean v)        { highlightSelf = v; }
    public void setHighlightSelfR(int v)           { highlightSelfR = clamp(v, 0, 255); }
    public void setHighlightSelfG(int v)           { highlightSelfG = clamp(v, 0, 255); }
    public void setHighlightSelfB(int v)           { highlightSelfB = clamp(v, 0, 255); }
    public void setHighlightSelfA(int v)           { highlightSelfA = clamp(v, 0, 255); }
    public void setHighlightParty(boolean v)       { highlightParty = v; }
    public void setHighlightPartyR(int v)          { highlightPartyR = clamp(v, 0, 255); }
    public void setHighlightPartyG(int v)          { highlightPartyG = clamp(v, 0, 255); }
    public void setHighlightPartyB(int v)          { highlightPartyB = clamp(v, 0, 255); }
    public void setHighlightPartyA(int v)          { highlightPartyA = clamp(v, 0, 255); }
    public void setHighlightNicked(boolean v)      { highlightNicked = v; }
    public void setHighlightNickedR(int v)         { highlightNickedR = clamp(v, 0, 255); }
    public void setHighlightNickedG(int v)         { highlightNickedG = clamp(v, 0, 255); }
    public void setHighlightNickedB(int v)         { highlightNickedB = clamp(v, 0, 255); }
    public void setHighlightNickedA(int v)         { highlightNickedA = clamp(v, 0, 255); }
    public void setHighlightTagged(boolean v)      { highlightTagged = v; }
    public void setHighlightTaggedR(int v)         { highlightTaggedR = clamp(v, 0, 255); }
    public void setHighlightTaggedG(int v)         { highlightTaggedG = clamp(v, 0, 255); }
    public void setHighlightTaggedB(int v)         { highlightTaggedB = clamp(v, 0, 255); }
    public void setHighlightTaggedA(int v)         { highlightTaggedA = clamp(v, 0, 255); }
    public void setFkdrDecimals(int v)             { fkdrDecimals = clamp(v, 0, 3); }
    public void setAbbreviateNumbers(boolean v)    { abbreviateNumbers = v; }
    public void setPingStyle(int v)                { pingStyle = clamp(v, 0, 1); }
    public void setHeaderAllR(int v) {
        headerAllR = clamp(v, 0, 255);
        applyHeaderAllChannel(0, headerAllR);
    }
    public void setHeaderAllG(int v) {
        headerAllG = clamp(v, 0, 255);
        applyHeaderAllChannel(1, headerAllG);
    }
    public void setHeaderAllB(int v) {
        headerAllB = clamp(v, 0, 255);
        applyHeaderAllChannel(2, headerAllB);
    }
    public void setHeaderR(String colKey, int v) { ensureHeader(colKey)[0] = clamp(v, 0, 255); }
    public void setHeaderG(String colKey, int v) { ensureHeader(colKey)[1] = clamp(v, 0, 255); }
    public void setHeaderB(String colKey, int v) { ensureHeader(colKey)[2] = clamp(v, 0, 255); }
    public void setAllHeaderColors(int r, int g, int b) {
        headerAllR = clamp(r, 0, 255);
        headerAllG = clamp(g, 0, 255);
        headerAllB = clamp(b, 0, 255);
        ensureAllHeaderKeysPresent();
        for (String key : HEADER_COL_KEYS) {
            int[] rgb = ensureHeader(key);
            rgb[0] = headerAllR; rgb[1] = headerAllG; rgb[2] = headerAllB;
        }
        for (int[] rgb : headerColors.values()) {
            if (rgb == null || rgb.length < 3) continue;
            rgb[0] = headerAllR; rgb[1] = headerAllG; rgb[2] = headerAllB;
        }
    }

    /** Push one RGB channel onto every known header column (including newly added keys). */
    private void applyHeaderAllChannel(int channel, int value) {
        ensureAllHeaderKeysPresent();
        for (String key : HEADER_COL_KEYS) {
            ensureHeader(key)[channel] = value;
        }
        for (int[] rgb : headerColors.values()) {
            if (rgb == null || rgb.length <= channel) continue;
            rgb[channel] = value;
        }
    }
    public void setMellowOuterR(int v)  { mellowOuterR = clamp(v, 0, 255); }
    public void setMellowOuterG(int v)  { mellowOuterG = clamp(v, 0, 255); }
    public void setMellowOuterB(int v)  { mellowOuterB = clamp(v, 0, 255); }
    public void setMellowOuterA(int v)  { mellowOuterA = clamp(v, 0, 255); }
    public void setMellowHeaderR(int v) { mellowHeaderR = clamp(v, 0, 255); }
    public void setMellowHeaderG(int v) { mellowHeaderG = clamp(v, 0, 255); }
    public void setMellowHeaderB(int v) { mellowHeaderB = clamp(v, 0, 255); }
    public void setMellowHeaderA(int v) { mellowHeaderA = clamp(v, 0, 255); }
    public void setMellowRowR(int v)    { mellowRowR = clamp(v, 0, 255); }
    public void setMellowRowG(int v)    { mellowRowG = clamp(v, 0, 255); }
    public void setMellowRowB(int v)    { mellowRowB = clamp(v, 0, 255); }
    public void setMellowRowA(int v)    { mellowRowA = clamp(v, 0, 255); }
    public void setMellowTaggedR(int v) { mellowTaggedR = clamp(v, 0, 255); }
    public void setMellowTaggedG(int v) { mellowTaggedG = clamp(v, 0, 255); }
    public void setMellowTaggedB(int v) { mellowTaggedB = clamp(v, 0, 255); }
    public void setMellowTaggedA(int v) { mellowTaggedA = clamp(v, 0, 255); }
    public void setOverlayTheme(int v)             { overlayTheme = clamp(v, 0, 2); }
    public void setFkdrColor1(String v)            { fkdrColor1 = v; }
    public void setFkdrColor2(String v)            { fkdrColor2 = v; }
    public void setFkdrColor3(String v)            { fkdrColor3 = v; }
    public void setFkdrColor4(String v)            { fkdrColor4 = v; }
    public void setFkdrColor5(String v)            { fkdrColor5 = v; }
    public void setFkdrColor6(String v)            { fkdrColor6 = v; }
    public void setFkdrColor7(String v)            { fkdrColor7 = v; }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static double clampDbl(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    /** Reset all appearance-related settings to defaults (keeps columns/API/keys). */
    public void resetAppearance() {
        overlayTheme = 0;
        overlayColGap = 12;
        overlayRowGap = 5;
        overlayScalePercent = 100;
        bgOpacity = 170;
        bgR = 0; bgG = 0; bgB = 0;
        outlineEnabled = true;
        outlineChroma = true;
        outlineR = 255; outlineG = 255; outlineB = 255;
        outlineWidth = 2.5f;
        borderRadius = 0;
        overlayPad = 0;
        textShadow = true;
        headerBold = true;
        stripeEnabled = false;
        stripeR = 255; stripeG = 255; stripeB = 255; stripeA = 18;
        highlightSelf = false;
        highlightSelfR = 80; highlightSelfG = 180; highlightSelfB = 255; highlightSelfA = 40;
        highlightParty = false;
        highlightPartyR = 80; highlightPartyG = 255; highlightPartyB = 120; highlightPartyA = 40;
        highlightNicked = false;
        highlightNickedR = 255; highlightNickedG = 220; highlightNickedB = 60; highlightNickedA = 45;
        highlightTagged = false;
        highlightTaggedR = 255; highlightTaggedG = 60; highlightTaggedB = 60; highlightTaggedA = 50;
        fkdrDecimals = 2;
        abbreviateNumbers = false;
        pingStyle = 0;
        headerAllR = 170; headerAllG = 0; headerAllB = 255;
        initDefaultHeaderColors(new int[]{headerAllR, headerAllG, headerAllB});
        mellowOuterR = 0; mellowOuterG = 0; mellowOuterB = 0; mellowOuterA = 128;
        mellowHeaderR = 255; mellowHeaderG = 255; mellowHeaderB = 255; mellowHeaderA = 32;
        mellowRowR = 255; mellowRowG = 255; mellowRowB = 255; mellowRowA = 32;
        mellowTaggedR = 0; mellowTaggedG = 0; mellowTaggedB = 0; mellowTaggedA = 153;
        fkdrColor1 = "7"; fkdrColor2 = "f"; fkdrColor3 = "e"; fkdrColor4 = "6";
        fkdrColor5 = "c"; fkdrColor6 = "4"; fkdrColor7 = "5";
        fkdrScale = ThresholdColorScale.defaultFkdr();
        wsScale = ThresholdColorScale.defaultWinstreak();
        pingScale = ThresholdColorScale.defaultPing();
        sessionScale = ThresholdColorScale.defaultSessionMinutes();
        encountersScale = ThresholdColorScale.defaultEncounters();
        countsScale = ThresholdColorScale.defaultCounts();
        periodStarsScale = ThresholdColorScale.defaultPeriodStars();
        fkdrColors = true;
        wsColors = true;
        pingColors = true;
        sessionColors = true;
        encountersColors = true;
        countColors = true;
        periodStarsColors = true;
    }

    /** Snapshot appearance settings for a preset file. */
    public Properties exportAppearance() {
        Properties p = new Properties();
        put(p, "overlayTheme", overlayTheme);
        put(p, "overlayColGap", overlayColGap);
        put(p, "overlayRowGap", overlayRowGap);
        put(p, "overlayScalePercent", overlayScalePercent);
        put(p, "bgOpacity", bgOpacity);
        put(p, "bgR", bgR); put(p, "bgG", bgG); put(p, "bgB", bgB);
        put(p, "outlineEnabled", outlineEnabled);
        put(p, "outlineChroma", outlineChroma);
        put(p, "outlineR", outlineR); put(p, "outlineG", outlineG); put(p, "outlineB", outlineB);
        put(p, "outlineWidth", outlineWidth);
        put(p, "borderRadius", borderRadius);
        put(p, "overlayPad", overlayPad);
        put(p, "textShadow", textShadow);
        put(p, "headerBold", headerBold);
        put(p, "stripeEnabled", stripeEnabled);
        put(p, "stripeR", stripeR); put(p, "stripeG", stripeG); put(p, "stripeB", stripeB); put(p, "stripeA", stripeA);
        put(p, "highlightSelf", highlightSelf);
        put(p, "highlightSelfR", highlightSelfR); put(p, "highlightSelfG", highlightSelfG);
        put(p, "highlightSelfB", highlightSelfB); put(p, "highlightSelfA", highlightSelfA);
        put(p, "highlightParty", highlightParty);
        put(p, "highlightPartyR", highlightPartyR); put(p, "highlightPartyG", highlightPartyG);
        put(p, "highlightPartyB", highlightPartyB); put(p, "highlightPartyA", highlightPartyA);
        put(p, "highlightNicked", highlightNicked);
        put(p, "highlightNickedR", highlightNickedR); put(p, "highlightNickedG", highlightNickedG);
        put(p, "highlightNickedB", highlightNickedB); put(p, "highlightNickedA", highlightNickedA);
        put(p, "highlightTagged", highlightTagged);
        put(p, "highlightTaggedR", highlightTaggedR); put(p, "highlightTaggedG", highlightTaggedG);
        put(p, "highlightTaggedB", highlightTaggedB); put(p, "highlightTaggedA", highlightTaggedA);
        put(p, "fkdrDecimals", fkdrDecimals);
        put(p, "abbreviateNumbers", abbreviateNumbers);
        put(p, "pingStyle", pingStyle);
        put(p, "headerColors", serializeHeaderColors());
        put(p, "headerAllR", headerAllR); put(p, "headerAllG", headerAllG); put(p, "headerAllB", headerAllB);
        put(p, "mellowOuterR", mellowOuterR); put(p, "mellowOuterG", mellowOuterG);
        put(p, "mellowOuterB", mellowOuterB); put(p, "mellowOuterA", mellowOuterA);
        put(p, "mellowHeaderR", mellowHeaderR); put(p, "mellowHeaderG", mellowHeaderG);
        put(p, "mellowHeaderB", mellowHeaderB); put(p, "mellowHeaderA", mellowHeaderA);
        put(p, "mellowRowR", mellowRowR); put(p, "mellowRowG", mellowRowG);
        put(p, "mellowRowB", mellowRowB); put(p, "mellowRowA", mellowRowA);
        put(p, "mellowTaggedR", mellowTaggedR); put(p, "mellowTaggedG", mellowTaggedG);
        put(p, "mellowTaggedB", mellowTaggedB); put(p, "mellowTaggedA", mellowTaggedA);
        put(p, "fkdrScale", fkdrScale.serialize());
        put(p, "wsScale", wsScale.serialize());
        put(p, "pingScale", pingScale.serialize());
        put(p, "sessionScale", sessionScale.serialize());
        put(p, "encountersScale", encountersScale.serialize());
        put(p, "countsScale", countsScale.serialize());
        put(p, "periodStarsScale", periodStarsScale.serialize());
        put(p, "fkdrColors", fkdrColors);
        put(p, "wsColors", wsColors);
        put(p, "pingColors", pingColors);
        put(p, "sessionColors", sessionColors);
        put(p, "encountersColors", encountersColors);
        put(p, "countColors", countColors);
        put(p, "periodStarsColors", periodStarsColors);
        return p;
    }

    /** Apply a preset snapshot (does not save to disk). */
    public void importAppearance(Properties p) {
        if (p == null) return;
        overlayTheme = clamp(getInt(p, "overlayTheme", overlayTheme), 0, 2);
        overlayColGap = clamp(getInt(p, "overlayColGap", overlayColGap), 0, 40);
        overlayRowGap = clamp(getInt(p, "overlayRowGap", overlayRowGap), 0, 20);
        overlayScalePercent = clamp(getInt(p, "overlayScalePercent", overlayScalePercent), 50, 200);
        bgOpacity = clamp(getInt(p, "bgOpacity", bgOpacity), 0, 255);
        bgR = clamp(getInt(p, "bgR", bgR), 0, 255);
        bgG = clamp(getInt(p, "bgG", bgG), 0, 255);
        bgB = clamp(getInt(p, "bgB", bgB), 0, 255);
        outlineEnabled = getBool(p, "outlineEnabled", outlineEnabled);
        outlineChroma = getBool(p, "outlineChroma", outlineChroma);
        outlineR = clamp(getInt(p, "outlineR", outlineR), 0, 255);
        outlineG = clamp(getInt(p, "outlineG", outlineG), 0, 255);
        outlineB = clamp(getInt(p, "outlineB", outlineB), 0, 255);
        outlineWidth = (float) clampDbl(getDbl(p, "outlineWidth", outlineWidth), 0.5, 8.0);
        borderRadius = clamp(getInt(p, "borderRadius", borderRadius), 0, 16);
        overlayPad = clamp(getInt(p, "overlayPad", overlayPad), 0, 24);
        textShadow = getBool(p, "textShadow", textShadow);
        headerBold = getBool(p, "headerBold", headerBold);
        stripeEnabled = getBool(p, "stripeEnabled", stripeEnabled);
        stripeR = clamp(getInt(p, "stripeR", stripeR), 0, 255);
        stripeG = clamp(getInt(p, "stripeG", stripeG), 0, 255);
        stripeB = clamp(getInt(p, "stripeB", stripeB), 0, 255);
        stripeA = clamp(getInt(p, "stripeA", stripeA), 0, 255);
        highlightSelf = getBool(p, "highlightSelf", highlightSelf);
        highlightSelfR = clamp(getInt(p, "highlightSelfR", highlightSelfR), 0, 255);
        highlightSelfG = clamp(getInt(p, "highlightSelfG", highlightSelfG), 0, 255);
        highlightSelfB = clamp(getInt(p, "highlightSelfB", highlightSelfB), 0, 255);
        highlightSelfA = clamp(getInt(p, "highlightSelfA", highlightSelfA), 0, 255);
        highlightParty = getBool(p, "highlightParty", highlightParty);
        highlightPartyR = clamp(getInt(p, "highlightPartyR", highlightPartyR), 0, 255);
        highlightPartyG = clamp(getInt(p, "highlightPartyG", highlightPartyG), 0, 255);
        highlightPartyB = clamp(getInt(p, "highlightPartyB", highlightPartyB), 0, 255);
        highlightPartyA = clamp(getInt(p, "highlightPartyA", highlightPartyA), 0, 255);
        highlightNicked = getBool(p, "highlightNicked", highlightNicked);
        highlightNickedR = clamp(getInt(p, "highlightNickedR", highlightNickedR), 0, 255);
        highlightNickedG = clamp(getInt(p, "highlightNickedG", highlightNickedG), 0, 255);
        highlightNickedB = clamp(getInt(p, "highlightNickedB", highlightNickedB), 0, 255);
        highlightNickedA = clamp(getInt(p, "highlightNickedA", highlightNickedA), 0, 255);
        highlightTagged = getBool(p, "highlightTagged", highlightTagged);
        highlightTaggedR = clamp(getInt(p, "highlightTaggedR", highlightTaggedR), 0, 255);
        highlightTaggedG = clamp(getInt(p, "highlightTaggedG", highlightTaggedG), 0, 255);
        highlightTaggedB = clamp(getInt(p, "highlightTaggedB", highlightTaggedB), 0, 255);
        highlightTaggedA = clamp(getInt(p, "highlightTaggedA", highlightTaggedA), 0, 255);
        fkdrDecimals = clamp(getInt(p, "fkdrDecimals", fkdrDecimals), 0, 3);
        abbreviateNumbers = getBool(p, "abbreviateNumbers", abbreviateNumbers);
        pingStyle = clamp(getInt(p, "pingStyle", pingStyle), 0, 1);
        if (p.containsKey("headerColors")) {
            headerColors.clear();
            parseHeaderColors(p.getProperty("headerColors"));
        }
        headerAllR = clamp(getInt(p, "headerAllR", headerAllR), 0, 255);
        headerAllG = clamp(getInt(p, "headerAllG", headerAllG), 0, 255);
        headerAllB = clamp(getInt(p, "headerAllB", headerAllB), 0, 255);
        ensureAllHeaderKeysPresent();
        mellowOuterR = clamp(getInt(p, "mellowOuterR", mellowOuterR), 0, 255);
        mellowOuterG = clamp(getInt(p, "mellowOuterG", mellowOuterG), 0, 255);
        mellowOuterB = clamp(getInt(p, "mellowOuterB", mellowOuterB), 0, 255);
        mellowOuterA = clamp(getInt(p, "mellowOuterA", mellowOuterA), 0, 255);
        mellowHeaderR = clamp(getInt(p, "mellowHeaderR", mellowHeaderR), 0, 255);
        mellowHeaderG = clamp(getInt(p, "mellowHeaderG", mellowHeaderG), 0, 255);
        mellowHeaderB = clamp(getInt(p, "mellowHeaderB", mellowHeaderB), 0, 255);
        mellowHeaderA = clamp(getInt(p, "mellowHeaderA", mellowHeaderA), 0, 255);
        mellowRowR = clamp(getInt(p, "mellowRowR", mellowRowR), 0, 255);
        mellowRowG = clamp(getInt(p, "mellowRowG", mellowRowG), 0, 255);
        mellowRowB = clamp(getInt(p, "mellowRowB", mellowRowB), 0, 255);
        mellowRowA = clamp(getInt(p, "mellowRowA", mellowRowA), 0, 255);
        mellowTaggedR = clamp(getInt(p, "mellowTaggedR", mellowTaggedR), 0, 255);
        mellowTaggedG = clamp(getInt(p, "mellowTaggedG", mellowTaggedG), 0, 255);
        mellowTaggedB = clamp(getInt(p, "mellowTaggedB", mellowTaggedB), 0, 255);
        mellowTaggedA = clamp(getInt(p, "mellowTaggedA", mellowTaggedA), 0, 255);
        if (p.containsKey("fkdrScale"))
            fkdrScale = ThresholdColorScale.parse(p.getProperty("fkdrScale"), ThresholdColorScale.defaultFkdr());
        if (p.containsKey("wsScale"))
            wsScale = ThresholdColorScale.parse(p.getProperty("wsScale"), ThresholdColorScale.defaultWinstreak());
        if (p.containsKey("pingScale"))
            pingScale = ThresholdColorScale.parse(p.getProperty("pingScale"), ThresholdColorScale.defaultPing());
        if (p.containsKey("sessionScale"))
            sessionScale = ThresholdColorScale.parse(p.getProperty("sessionScale"), ThresholdColorScale.defaultSessionMinutes());
        if (p.containsKey("encountersScale"))
            encountersScale = ThresholdColorScale.parse(p.getProperty("encountersScale"), ThresholdColorScale.defaultEncounters());
        // Migration: older presets lack these — keep defaults when absent
        if (p.containsKey("countsScale"))
            countsScale = ThresholdColorScale.parse(p.getProperty("countsScale"), ThresholdColorScale.defaultCounts());
        if (p.containsKey("periodStarsScale"))
            periodStarsScale = ThresholdColorScale.parse(p.getProperty("periodStarsScale"), ThresholdColorScale.defaultPeriodStars());
        fkdrColors = getBool(p, "fkdrColors", fkdrColors);
        wsColors = getBool(p, "wsColors", wsColors);
        pingColors = getBool(p, "pingColors", pingColors);
        sessionColors = getBool(p, "sessionColors", sessionColors);
        encountersColors = getBool(p, "encountersColors", encountersColors);
        countColors = getBool(p, "countColors", countColors);
        periodStarsColors = getBool(p, "periodStarsColors", periodStarsColors);
    }

    private static void put(Properties p, String key, Object val) {
        p.setProperty(key, String.valueOf(val));
    }

    private static int getInt(Properties p, String key, int def) {
        String v = p.getProperty(key);
        if (v == null) return def;
        try { return Integer.parseInt(v.trim()); } catch (Exception e) { return def; }
    }

    private static double getDbl(Properties p, String key, double def) {
        String v = p.getProperty(key);
        if (v == null) return def;
        try { return Double.parseDouble(v.trim()); } catch (Exception e) { return def; }
    }

    private static boolean getBool(Properties p, String key, boolean def) {
        String v = p.getProperty(key);
        if (v == null) return def;
        return "true".equalsIgnoreCase(v.trim()) || "1".equals(v.trim()) || "yes".equalsIgnoreCase(v.trim());
    }

    /** Convert legacy hue (0/1-359/360) to RGB for migration. Chroma → white. */
    private static int[] unpackHueToRgb(int hue) {
        if (hue <= 0) return new int[]{0, 0, 0};
        if (hue >= 360) return new int[]{255, 255, 255};
        int argb = com.lazify.util.ColorUtil.getHueRGB(hue, 255);
        return com.lazify.util.ColorUtil.unpackRgb(argb);
    }

    private void initDefaultHeaderColors(int[] def) {
        headerColors.clear();
        for (String key : HEADER_COL_KEYS) {
            headerColors.put(key, new int[]{def[0], def[1], def[2]});
        }
    }

    /** Make sure every HEADER_COL_KEYS entry exists (e.g. after upgrading the mod). */
    private void ensureAllHeaderKeysPresent() {
        for (String key : HEADER_COL_KEYS) {
            if (!headerColors.containsKey(key)) {
                headerColors.put(key, new int[]{headerAllR, headerAllG, headerAllB});
            }
        }
    }

    private int[] ensureHeader(String colKey) {
        if (colKey == null) colKey = "";
        int[] rgb = headerColors.get(colKey);
        if (rgb == null) {
            rgb = new int[]{headerAllR, headerAllG, headerAllB};
            headerColors.put(colKey, rgb);
        }
        return rgb;
    }

    private void migrateLegacyHeaderField(String prefix, String colKey, int[] fallback) {
        ConfigCategory cat = config.getCategory("colors");
        if (!cat.containsKey(prefix + "R")) return;
        int r = clamp(config.get("colors", prefix + "R", fallback[0]).getInt(fallback[0]), 0, 255);
        int g = clamp(config.get("colors", prefix + "G", fallback[1]).getInt(fallback[1]), 0, 255);
        int b = clamp(config.get("colors", prefix + "B", fallback[2]).getInt(fallback[2]), 0, 255);
        headerColors.put(colKey, new int[]{r, g, b});
    }

    private String serializeHeaderColors() {
        StringBuilder sb = new StringBuilder();
        for (String key : HEADER_COL_KEYS) {
            int[] rgb = ensureHeader(key);
            if (sb.length() > 0) sb.append(';');
            sb.append(key).append(':').append(rgb[0]).append(',').append(rgb[1]).append(',').append(rgb[2]);
        }
        return sb.toString();
    }

    private void parseHeaderColors(String raw) {
        if (raw == null || raw.trim().isEmpty()) return;
        String[] parts = raw.split(";");
        for (String part : parts) {
            String[] kv = part.split(":");
            if (kv.length != 2) continue;
            String key = kv[0].trim();
            String[] comps = kv[1].split(",");
            if (comps.length != 3) continue;
            try {
                int r = clamp(Integer.parseInt(comps[0].trim()), 0, 255);
                int g = clamp(Integer.parseInt(comps[1].trim()), 0, 255);
                int b = clamp(Integer.parseInt(comps[2].trim()), 0, 255);
                headerColors.put(key, new int[]{r, g, b});
            } catch (NumberFormatException ignored) { }
        }
    }
}
