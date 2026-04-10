package com.lazify.config;

import net.minecraftforge.common.config.Configuration;

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

    private LazifyConfig() {}

    public void load(File configDir) {
        File cfgFile = new File(configDir, "lazify.cfg");
        config = new Configuration(cfgFile);
        config.load();
        syncFromFile();
        if (config.hasChanged()) config.save();
    }

    private void syncFromFile() {
        urchinKey  = config.getString("urchinKey",  "api", "", "Your Urchin API key (https://urchin.ws)");

        debug       = config.getBoolean("debug",       "general", false, "Print debug messages to chat");
        keybindHold = config.getBoolean("keybindHold", "general", false, "true=show overlay while key held, false=toggle");
        showOnTab   = config.getBoolean("showOnTab",   "general", true,  "Show overlay while holding Tab");
        keybind     = config.getInt("keybind", "general", 41, -1, Integer.MAX_VALUE, "Overlay toggle key code (LWJGL)");

        teams                  = config.getBoolean("teams",                  "general", true,  "Show team colors in overlay");
        teamPrefix             = config.getBoolean("teamPrefix",             "general", false, "Show team prefix letters");
        showYourself           = config.getBoolean("showYourself",           "general", false, "Show yourself in the overlay");
        sendNickedToChat       = config.getBoolean("sendNickedToChat",       "general", true,  "Print chat notice for nicked players");
        sendUrchinReasonToChat = config.getBoolean("sendUrchinReasonToChat", "general", false, "Print Urchin tag reason to chat");
        showRanks              = config.getBoolean("showRanks",              "general", false, "Show formatted rank prefix next to name");
        removeFinalKill        = config.getBoolean("removeFinalKill",        "general", false, "Remove players from overlay on final kill");
        autoTablist            = config.getBoolean("autoTablist",            "general", true,  "Auto-detect players from tab list");
        clearOnWho             = config.getBoolean("clearOnWho",             "general", false, "Clear overlay when /who response is received");
        middleClickShop        = config.getBoolean("middleClickShop",        "general", false, "Convert left/right clicks to middle clicks in BW shops");
        skinDenick             = config.getBoolean("skinDenick",             "general", true,  "Auto-detect nicked players by their skin");

        colEncounters = config.getBoolean("colEncounters", "columns", true, "Show Encounters column");
        colUsername   = config.getBoolean("colUsername",   "columns", true, "Show Username column");
        colStar       = config.getBoolean("colStar",       "columns", true, "Show Star column");
        colFkdr       = config.getBoolean("colFkdr",       "columns", true, "Show FKDR column");
        colWinstreaks = config.getBoolean("colWinstreaks", "columns", true, "Show Winstreaks column");
        colUrchin     = config.getBoolean("colUrchin",     "columns", true, "Show Urchin column");
        colSession    = config.getBoolean("colSession",    "columns", true, "Show Session column");

        encountersTimeoutMins = config.getInt("encountersTimeoutMins", "general", 30, 1,  1440, "Minutes before encounters entry expires");
        sortByIndex           = config.getInt("sortByIndex",           "general",  2, 0,  5,    "Sort column: 0=Encounters 1=Star 2=FKDR 3=Index 4=Winstreak 5=JoinTime");
        sortMode              = config.getInt("sortMode",              "general",  0, 0,  1,    "0=ascending (highest on top), 1=descending");
        winstreakMode         = config.getInt("winstreakMode",         "general",  0, 0,  5,    "0=Overall 1=Solos 2=Doubles 3=Threes 4=Fours 5=4v4");

        overlayX = config.getInt("overlayX", "position", 2, 0, 10000, "Overlay X position");
        overlayY = config.getInt("overlayY", "position", 2, 0, 10000, "Overlay Y position");

        bgOpacity = config.getInt("bgOpacity",  "colors", 170, 0,  255, "Background opacity (0-255)");
        bgHue     = config.getInt("bgHue",      "colors",   0, 0,  360, "Background hue (0=black, 360=chroma)");
        headerHue = config.getInt("headerHue",  "colors", 290, 0,  360, "Column header hue");
        borderHue = config.getInt("borderHue",  "colors", 360, 0,  360, "Border hue");
    }

    public void save() {
        if (config == null) return;
        config.get("api",      "urchinKey",              "").set(urchinKey);
        config.get("general",  "debug",                  false).set(debug);
        config.get("general",  "keybindHold",            false).set(keybindHold);
        config.get("general",  "showOnTab",              true).set(showOnTab);
        config.get("general",  "keybind",                41).set(keybind);
        config.get("general",  "teams",                  true).set(teams);
        config.get("general",  "teamPrefix",             false).set(teamPrefix);
        config.get("general",  "showYourself",           false).set(showYourself);
        config.get("general",  "sendNickedToChat",       true).set(sendNickedToChat);
        config.get("general",  "sendUrchinReasonToChat", false).set(sendUrchinReasonToChat);
        config.get("general",  "showRanks",              false).set(showRanks);
        config.get("general",  "removeFinalKill",        false).set(removeFinalKill);
        config.get("general",  "autoTablist",            true).set(autoTablist);
        config.get("general",  "clearOnWho",             false).set(clearOnWho);
        config.get("general",  "middleClickShop",        false).set(middleClickShop);
        config.get("general",  "skinDenick",             true).set(skinDenick);
        config.get("columns",  "colEncounters",          true).set(colEncounters);
        config.get("columns",  "colUsername",            true).set(colUsername);
        config.get("columns",  "colStar",                true).set(colStar);
        config.get("columns",  "colFkdr",                true).set(colFkdr);
        config.get("columns",  "colWinstreaks",          true).set(colWinstreaks);
        config.get("columns",  "colUrchin",              true).set(colUrchin);
        config.get("columns",  "colSession",             true).set(colSession);
        config.get("general",  "encountersTimeoutMins",  30).set(encountersTimeoutMins);
        config.get("general",  "sortByIndex",            2).set(sortByIndex);
        config.get("general",  "sortMode",               0).set(sortMode);
        config.get("general",  "winstreakMode",          0).set(winstreakMode);
        config.get("position", "overlayX",               2).set(overlayX);
        config.get("position", "overlayY",               2).set(overlayY);
        config.get("colors",   "bgOpacity",              170).set(bgOpacity);
        config.get("colors",   "bgHue",                  0).set(bgHue);
        config.get("colors",   "headerHue",              290).set(headerHue);
        config.get("colors",   "borderHue",              360).set(borderHue);
        config.save();
    }

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
}
