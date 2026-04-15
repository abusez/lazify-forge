package com.lazify.config;

import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import java.io.File;

public class LazifyConfig {

    public static final LazifyConfig INSTANCE = new LazifyConfig();

    private Configuration config;

    // API keys
    private String urchinKey  = "";

    // keybind behaviour
    private boolean keybindHold = false;
    private boolean showOnTab   = true;
    private int     keybind     = 41;    // LWJGL KEY_GRAVE (`)

    // debug
    private boolean debug = false;

    // boolean settings
    private boolean teams                  = true;
    private boolean teamPrefix             = false;
    private boolean showYourself           = false;
    private boolean sendNickedToChat       = true;
    private boolean sendUrchinReasonToChat = false;
    private boolean showRanks              = false;
    private boolean removeFinalKill        = false;
    private boolean autoTablist            = true;
    private boolean clearOnWho             = false;
    private boolean middleClickShop        = false;
    private boolean skinDenick             = true;
    private boolean fkdrColors             = true;
    private boolean autoWho                = false;
    private double  whoDelay               = 0.0;
    private boolean hideWho                = false;
    private boolean dodgeWarning           = false;
    private double  dodgeThreshold         = 3.0;
    private boolean noHurtCam              = false;
    private boolean antiDebuff             = false;

    // column visibility
    private boolean colEncounters = true;
    private boolean colUsername   = true;
    private boolean colStar       = true;
    private boolean colFkdr       = true;
    private boolean colWinstreaks = true;
    private boolean colUrchin     = true;
    private boolean colSession    = true;

    // int settings
    private int encountersTimeoutMins = 30;
    private int sortByIndex           = 2;
    private int sortMode              = 0;
    private int winstreakMode         = 0;

    // overlay position
    private int overlayX = 2;
    private int overlayY = 2;

    // colors
    private int bgOpacity  = 170;
    private int bgHue      = 0;
    private int headerHue  = 290;
    private int borderHue  = 360;

    // fkdr colors (single Minecraft color code char per tier)
    private String fkdrColor1 = "7"; // < 1.4
    private String fkdrColor2 = "f"; // 1.4 - 2.4
    private String fkdrColor3 = "e"; // 2.4 - 5
    private String fkdrColor4 = "6"; // 5 - 10
    private String fkdrColor5 = "c"; // 10 - 100
    private String fkdrColor6 = "4"; // 100 - 1000
    private String fkdrColor7 = "5"; // 1000+

    private LazifyConfig() {}

    public void load(File configDir) {
        File cfgFile = new File(configDir, "lazify.cfg");
        config = new Configuration(cfgFile);
        config.load();
        syncFromFile();
        if (config.hasChanged()) config.save();
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
        p.comment = "Your Urchin API key from urchin.ws. Enables cheater detection tags in the overlay.";
        urchinKey = p.getString();

        // ── General: Keybind ──────────────────────────────────────────────────
        p = config.get("general", "keybind", 41);
        p.comment = "LWJGL key code for the overlay toggle. Default is grave/backtick. Use /ov keybind to change by name.";
        keybind = p.getInt(41);

        p = config.get("general", "keybindHold", false);
        p.comment = "Hold mode: overlay is only visible while you hold the keybind. Off = toggle on/off.";
        keybindHold = p.getBoolean(false);

        p = config.get("general", "showOnTab", true);
        p.comment = "Also show the overlay while you hold the Tab key.";
        showOnTab = p.getBoolean(true);

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
        p.comment = "Show the player's Hypixel rank (VIP, MVP+, etc.) next to their name.";
        showRanks = p.getBoolean(false);

        // ── General: Detection ────────────────────────────────────────────────
        p = config.get("general", "autoTablist", true);
        p.comment = "Automatically scan the tab list to detect and add players. Disable for manual /ov sc only.";
        autoTablist = p.getBoolean(true);

        p = config.get("general", "clearOnWho", false);
        p.comment = "Clear the overlay when a /who response is received, then re-add players from the response.";
        clearOnWho = p.getBoolean(false);

        p = config.get("general", "skinDenick", true);
        p.comment = "Detect nicked players by matching their skin against known Hypixel nick skins.";
        skinDenick = p.getBoolean(true);

        p = config.get("general", "removeFinalKill", false);
        p.comment = "Automatically remove a player from the overlay when they get final killed.";
        removeFinalKill = p.getBoolean(false);

        // ── General: Chat Notifications ───────────────────────────────────────
        p = config.get("general", "sendNickedToChat", true);
        p.comment = "Print a chat message when a nicked player is detected.";
        sendNickedToChat = p.getBoolean(true);

        p = config.get("general", "sendUrchinReasonToChat", false);
        p.comment = "Print the full Urchin tag reason in chat when a tagged player is found.";
        sendUrchinReasonToChat = p.getBoolean(false);

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

        p = config.get("general", "dodgeWarning", false);
        p.comment = "Print a chat warning when the average lobby FKDR exceeds the dodge threshold.";
        dodgeWarning = p.getBoolean(false);

        p = config.get("general", "dodgeThreshold", 3.0);
        p.comment = "Average lobby FKDR threshold for the dodge warning. Only used when dodgeWarning is enabled.";
        dodgeThreshold = p.getDouble(3.0);

        p = config.get("general", "noHurtCam", false);
        p.comment = "Disable the camera tilt effect when taking damage.";
        noHurtCam = p.getBoolean(false);

        p = config.get("general", "antiDebuff", false);
        p.comment = "Remove visual debuff effects: blindness, nausea, and slowness FOV change.";
        antiDebuff = p.getBoolean(false);

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

        p = config.get("columns", "colStar", true);
        p.comment = "Show the Star (level) column.";
        colStar = p.getBoolean(true);

        p = config.get("columns", "colFkdr", true);
        p.comment = "Show the FKDR (Final Kill/Death Ratio) column.";
        colFkdr = p.getBoolean(true);

        p = config.get("columns", "colWinstreaks", true);
        p.comment = "Show the Winstreak column.";
        colWinstreaks = p.getBoolean(true);

        p = config.get("columns", "colUrchin", true);
        p.comment = "Show the Urchin tag column (cheater detection).";
        colUrchin = p.getBoolean(true);

        p = config.get("columns", "colSession", true);
        p.comment = "Show the Session column (how long the player has been online).";
        colSession = p.getBoolean(true);

        // ── Position ──────────────────────────────────────────────────────────
        p = config.get("position", "overlayX", 2);
        p.comment = "Horizontal position in pixels from the left edge.";
        overlayX = p.getInt(2);

        p = config.get("position", "overlayY", 2);
        p.comment = "Vertical position in pixels from the top edge.";
        overlayY = p.getInt(2);

        // ── Colors ────────────────────────────────────────────────────────────
        p = config.get("colors", "bgOpacity", 170);
        p.comment = "Background transparency. 0 = fully transparent, 255 = fully opaque.";
        bgOpacity = p.getInt(170);

        p = config.get("colors", "bgHue", 0);
        p.comment = "Background color. 0 = black, 1-359 = color wheel, 360 = rainbow.";
        bgHue = p.getInt(0);

        p = config.get("colors", "headerHue", 290);
        p.comment = "Column header text color. 0-359 = color wheel, 360 = rainbow.";
        headerHue = p.getInt(290);

        p = config.get("colors", "borderHue", 360);
        p.comment = "Overlay border color. 0-359 = color wheel, 360 = rainbow.";
        borderHue = p.getInt(360);

        p = config.get("colors", "fkdrColor1", "7");
        p.comment = "FKDR color for < 1.4. Minecraft color code: 0-9, a-f. Default: 7 (gray).";
        fkdrColor1 = p.getString();

        p = config.get("colors", "fkdrColor2", "f");
        p.comment = "FKDR color for 1.4 - 2.4. Minecraft color code: 0-9, a-f. Default: f (white).";
        fkdrColor2 = p.getString();

        p = config.get("colors", "fkdrColor3", "e");
        p.comment = "FKDR color for 2.4 - 5. Minecraft color code: 0-9, a-f. Default: e (yellow).";
        fkdrColor3 = p.getString();

        p = config.get("colors", "fkdrColor4", "6");
        p.comment = "FKDR color for 5 - 10. Minecraft color code: 0-9, a-f. Default: 6 (gold).";
        fkdrColor4 = p.getString();

        p = config.get("colors", "fkdrColor5", "c");
        p.comment = "FKDR color for 10 - 100. Minecraft color code: 0-9, a-f. Default: c (red).";
        fkdrColor5 = p.getString();

        p = config.get("colors", "fkdrColor6", "4");
        p.comment = "FKDR color for 100 - 1000. Minecraft color code: 0-9, a-f. Default: 4 (dark red).";
        fkdrColor6 = p.getString();

        p = config.get("colors", "fkdrColor7", "5");
        p.comment = "FKDR color for 1000+. Minecraft color code: 0-9, a-f. Default: 5 (dark purple).";
        fkdrColor7 = p.getString();

        // ── Clean up stale properties from old versions ───────────────────────
        cleanStaleProperties();
    }

    private void cleanStaleProperties() {
        // Properties removed during API/settings revamp
        String[] staleApi = {"hypixelKey", "mellowKey", "mellowUrl", "usePrism"};
        for (String key : staleApi) {
            if (config.getCategory("api").containsKey(key))
                config.getCategory("api").remove(key);
        }

        String[] staleGeneral = {"addTaggedToEnemy", "useprism", "autoRequeue"};
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
        gen.get("debug").set(debug);
        gen.get("keybindHold").set(keybindHold);
        gen.get("showOnTab").set(showOnTab);
        gen.get("keybind").set(keybind);
        gen.get("teams").set(teams);
        gen.get("teamPrefix").set(teamPrefix);
        gen.get("showYourself").set(showYourself);
        gen.get("sendNickedToChat").set(sendNickedToChat);
        gen.get("sendUrchinReasonToChat").set(sendUrchinReasonToChat);
        gen.get("showRanks").set(showRanks);
        gen.get("removeFinalKill").set(removeFinalKill);
        gen.get("autoTablist").set(autoTablist);
        gen.get("clearOnWho").set(clearOnWho);
        gen.get("middleClickShop").set(middleClickShop);
        gen.get("skinDenick").set(skinDenick);
        gen.get("fkdrColors").set(fkdrColors);
        gen.get("autoWho").set(autoWho);
        gen.get("whoDelay").set(whoDelay);
        gen.get("hideWho").set(hideWho);
        gen.get("dodgeWarning").set(dodgeWarning);
        gen.get("dodgeThreshold").set(dodgeThreshold);
        gen.get("noHurtCam").set(noHurtCam);
        gen.get("antiDebuff").set(antiDebuff);
        gen.get("encountersTimeoutMins").set(encountersTimeoutMins);
        gen.get("sortByIndex").set(sortByIndex);
        gen.get("sortMode").set(sortMode);
        gen.get("winstreakMode").set(winstreakMode);
        col.get("colEncounters").set(colEncounters);
        col.get("colUsername").set(colUsername);
        col.get("colStar").set(colStar);
        col.get("colFkdr").set(colFkdr);
        col.get("colWinstreaks").set(colWinstreaks);
        col.get("colUrchin").set(colUrchin);
        col.get("colSession").set(colSession);
        pos.get("overlayX").set(overlayX);
        pos.get("overlayY").set(overlayY);
        clr.get("bgOpacity").set(bgOpacity);
        clr.get("bgHue").set(bgHue);
        clr.get("headerHue").set(headerHue);
        clr.get("borderHue").set(borderHue);
        clr.get("fkdrColor1").set(fkdrColor1);
        clr.get("fkdrColor2").set(fkdrColor2);
        clr.get("fkdrColor3").set(fkdrColor3);
        clr.get("fkdrColor4").set(fkdrColor4);
        clr.get("fkdrColor5").set(fkdrColor5);
        clr.get("fkdrColor6").set(fkdrColor6);
        clr.get("fkdrColor7").set(fkdrColor7);
        config.save();
    }

    public Configuration getConfiguration() { return config; }

    // ── Getters ────────────────────────────────────────────────────────────────
    public String  getUrchinKey()              { return urchinKey; }
    public boolean isDebug()                   { return debug; }
    public boolean isKeybindHold()             { return keybindHold; }
    public boolean isShowOnTab()              { return showOnTab; }
    public int     getKeybind()                { return keybind; }
    public boolean isTeams()                   { return teams; }
    public boolean isTeamPrefix()              { return teamPrefix; }
    public boolean isShowYourself()            { return showYourself; }
    public boolean isSendNickedToChat()        { return sendNickedToChat; }
    public boolean isSendUrchinReasonToChat()  { return sendUrchinReasonToChat; }
    public boolean isShowRanks()               { return showRanks; }
    public boolean isRemoveFinalKill()         { return removeFinalKill; }
    public boolean isAutoTablist()             { return autoTablist; }
    public boolean isClearOnWho()              { return clearOnWho; }
    public boolean isMiddleClickShop()         { return middleClickShop; }
    public boolean isSkinDenick()              { return skinDenick; }
    public boolean isFkdrColors()              { return fkdrColors; }
    public boolean isAutoWho()                 { return autoWho; }
    public double  getWhoDelay()               { return whoDelay; }
    public boolean isHideWho()                 { return hideWho; }
    public boolean isDodgeWarning()            { return dodgeWarning; }
    public double  getDodgeThreshold()         { return dodgeThreshold; }
    public boolean isNoHurtCam()               { return noHurtCam; }
    public boolean isAntiDebuff()              { return antiDebuff; }
    public boolean isColEncounters()           { return colEncounters; }
    public boolean isColUsername()             { return colUsername; }
    public boolean isColStar()                 { return colStar; }
    public boolean isColFkdr()                 { return colFkdr; }
    public boolean isColWinstreaks()           { return colWinstreaks; }
    public boolean isColUrchin()               { return colUrchin; }
    public boolean isColSession()              { return colSession; }
    public int     getEncountersTimeoutMins()  { return encountersTimeoutMins; }
    public int     getSortByIndex()            { return sortByIndex; }
    public int     getSortMode()               { return sortMode; }
    public int     getWinstreakMode()          { return winstreakMode; }
    public int     getOverlayX()               { return overlayX; }
    public int     getOverlayY()               { return overlayY; }
    public int     getBgOpacity()              { return bgOpacity; }
    public int     getBgHue()                  { return bgHue; }
    public int     getHeaderHue()              { return headerHue; }
    public int     getBorderHue()              { return borderHue; }
    public String  getFkdrColor1()             { return fkdrColor1; }
    public String  getFkdrColor2()             { return fkdrColor2; }
    public String  getFkdrColor3()             { return fkdrColor3; }
    public String  getFkdrColor4()             { return fkdrColor4; }
    public String  getFkdrColor5()             { return fkdrColor5; }
    public String  getFkdrColor6()             { return fkdrColor6; }
    public String  getFkdrColor7()             { return fkdrColor7; }
    public String[] getFkdrColors()            { return new String[]{fkdrColor1, fkdrColor2, fkdrColor3, fkdrColor4, fkdrColor5, fkdrColor6, fkdrColor7}; }

    // ── Setters ────────��────────────────────────────────────���──────────────────
    public void setUrchinKey(String v)             { urchinKey = v; }
    public void setDebug(boolean v)                { debug = v; }
    public void setKeybindHold(boolean v)          { keybindHold = v; }
    public void setShowOnTab(boolean v)            { showOnTab = v; }
    public void setKeybind(int v)                  { keybind = v; }
    public void setTeams(boolean v)                { teams = v; }
    public void setTeamPrefix(boolean v)           { teamPrefix = v; }
    public void setShowYourself(boolean v)         { showYourself = v; }
    public void setSendNickedToChat(boolean v)     { sendNickedToChat = v; }
    public void setSendUrchinReasonToChat(boolean v) { sendUrchinReasonToChat = v; }
    public void setShowRanks(boolean v)            { showRanks = v; }
    public void setRemoveFinalKill(boolean v)      { removeFinalKill = v; }
    public void setAutoTablist(boolean v)          { autoTablist = v; }
    public void setClearOnWho(boolean v)           { clearOnWho = v; }
    public void setMiddleClickShop(boolean v)      { middleClickShop = v; }
    public void setSkinDenick(boolean v)           { skinDenick = v; }
    public void setFkdrColors(boolean v)          { fkdrColors = v; }
    public void setAutoWho(boolean v)             { autoWho = v; }
    public void setWhoDelay(double v)             { whoDelay = v; }
    public void setHideWho(boolean v)             { hideWho = v; }
    public void setDodgeWarning(boolean v)        { dodgeWarning = v; }
    public void setDodgeThreshold(double v)       { dodgeThreshold = v; }
    public void setNoHurtCam(boolean v)           { noHurtCam = v; }
    public void setAntiDebuff(boolean v)          { antiDebuff = v; }
    public void setColEncounters(boolean v)        { colEncounters = v; }
    public void setColUsername(boolean v)           { colUsername = v; }
    public void setColStar(boolean v)              { colStar = v; }
    public void setColFkdr(boolean v)              { colFkdr = v; }
    public void setColWinstreaks(boolean v)        { colWinstreaks = v; }
    public void setColUrchin(boolean v)            { colUrchin = v; }
    public void setColSession(boolean v)           { colSession = v; }
    public void setEncountersTimeoutMins(int v)    { encountersTimeoutMins = v; }
    public void setSortByIndex(int v)              { sortByIndex = v; }
    public void setSortMode(int v)                 { sortMode = v; }
    public void setWinstreakMode(int v)            { winstreakMode = v; }
    public void setOverlayX(int v)                 { overlayX = v; }
    public void setOverlayY(int v)                 { overlayY = v; }
    public void setBgOpacity(int v)                { bgOpacity = v; }
    public void setBgHue(int v)                    { bgHue = v; }
    public void setHeaderHue(int v)                { headerHue = v; }
    public void setBorderHue(int v)                { borderHue = v; }
    public void setFkdrColor1(String v)            { fkdrColor1 = v; }
    public void setFkdrColor2(String v)            { fkdrColor2 = v; }
    public void setFkdrColor3(String v)            { fkdrColor3 = v; }
    public void setFkdrColor4(String v)            { fkdrColor4 = v; }
    public void setFkdrColor5(String v)            { fkdrColor5 = v; }
    public void setFkdrColor6(String v)            { fkdrColor6 = v; }
    public void setFkdrColor7(String v)            { fkdrColor7 = v; }
}
