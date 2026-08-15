package com.lazify.overlay;

import com.lazify.config.LazifyConfig;
import com.lazify.util.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.EnumChatFormatting;

import java.util.Locale;
import java.util.Map;

/** Visual overlay themes (layout unchanged; styling only). */
public final class OverlayTheme {

    public static final int DEFAULT  = 0;
    public static final int NERDIFY  = 1;
    public static final int MELLOW   = 2;

    /** Semi-transparent neutral black base (RGB only; alpha from config). */
    public static final int NERDIFY_BG_RGB = 0x00101010;

    public static int backgroundColor(int opacity) {
        int alpha = Math.max(0, Math.min(255, opacity));
        return (alpha << 24) | NERDIFY_BG_RGB;
    }

    public static int colGap()               { return LazifyConfig.INSTANCE.getOverlayColGap(); }
    public static int edgePad()              { return Math.max(4, colGap() / 2); }
    public static int cellPad()              { return Math.max(3, colGap() / 2); }
    public static int headerPadTop()         { return Math.max(3, rowGap()); }
    public static int headerSeparatorGap()   { return Math.max(2, rowGap() / 2); }
    public static int rowGapAfterSeparator() { return Math.max(3, rowGap()); }
    public static int bottomPad()            { return Math.max(4, rowGap()); }
    public static int rowGap()               { return LazifyConfig.INSTANCE.getOverlayRowGap(); }
    public static int lineExtra()            { return rowGap(); }

    /** Bright terminal lime (#00FF00). */
    public static final int NERDIFY_GREEN  = 0xFF00FF00;

    public static final String GREEN = "\u00a7a";
    public static final String GRAY  = "\u00a77";
    public static final String WHITE = "\u00a7f";
    public static final String GOLD  = "\u00a76";
    public static final String RED   = "\u00a7c";

    /** Minimum column slot width so headers and values aren't cramped. */
    public static int minColWidth(String colKey) {
        if (colKey == null) return 0;
        switch (colKey) {
            case OverlayManager.ENCOUNTERS_KEY: return 20;
            case OverlayManager.RANK_KEY:       return 36;
            case OverlayManager.STAR_KEY:       return 30;
            case OverlayManager.FKDR_KEY:       return 34;
            case OverlayManager.WLR_KEY:
            case OverlayManager.BBLR_KEY:
            case OverlayManager.KDR_KEY:       return 34;
            case OverlayManager.KILLS_KEY:
            case OverlayManager.FINALS_KEY:
            case OverlayManager.BEDS_KEY:
            case OverlayManager.WINS_KEY:      return 36;
            case OverlayManager.DAILY_FKDR_KEY:
            case OverlayManager.WEEKLY_FKDR_KEY:
            case OverlayManager.MONTHLY_FKDR_KEY:
            case OverlayManager.DAILY_WLR_KEY:
            case OverlayManager.WEEKLY_WLR_KEY:
            case OverlayManager.MONTHLY_WLR_KEY:
            case OverlayManager.DAILY_BBLR_KEY:
            case OverlayManager.WEEKLY_BBLR_KEY:
            case OverlayManager.MONTHLY_BBLR_KEY:
            case OverlayManager.DAILY_KDR_KEY:
            case OverlayManager.WEEKLY_KDR_KEY:
            case OverlayManager.MONTHLY_KDR_KEY: return 40;
            case OverlayManager.DAILY_STARS_KEY:
            case OverlayManager.WEEKLY_STARS_KEY:
            case OverlayManager.MONTHLY_STARS_KEY: return 34;
            case OverlayManager.WINSTREAK_KEY:  return 24;
            case OverlayManager.URCHIN_KEY:     return 36;
            case OverlayManager.PING_KEY:       return 28;
            case OverlayManager.SESSION_KEY:    return 32;
            case OverlayManager.LEVEL_KEY:      return 24;
            default: return 0;
        }
    }

    public static boolean isNumericCol(String colKey) {
        return !OverlayManager.PLAYER_KEY.equals(colKey)
                && !OverlayManager.RANK_KEY.equals(colKey)
                && !OverlayManager.ENCOUNTERS_KEY.equals(colKey);
    }

    private OverlayTheme() {}

    public static boolean isNerdify(int theme) {
        return theme == NERDIFY;
    }

    public static boolean isMellow(int theme) {
        return theme == MELLOW;
    }

    public static String themeName(int theme) {
        switch (theme) {
            case NERDIFY: return "Nerdify";
            case MELLOW:  return "Mellow";
            default:      return "Lazify";
        }
    }

    /** Lowercase nerdify header label for a column key. */
    public static String headerFor(String colKey) {
        if (colKey == null) return "";
        switch (colKey) {
            case OverlayManager.ENCOUNTERS_KEY: return "enc";
            case OverlayManager.PLAYER_KEY:     return "ign";
            case OverlayManager.RANK_KEY:       return "rank";
            case OverlayManager.STAR_KEY:       return "star";
            case OverlayManager.FKDR_KEY:     return "fkdr";
            case OverlayManager.WLR_KEY:      return "wlr";
            case OverlayManager.BBLR_KEY:     return "bblr";
            case OverlayManager.KDR_KEY:      return "kdr";
            case OverlayManager.KILLS_KEY:    return "kills";
            case OverlayManager.FINALS_KEY:   return "finals";
            case OverlayManager.BEDS_KEY:     return "beds";
            case OverlayManager.WINS_KEY:     return "wins";
            case OverlayManager.DAILY_FKDR_KEY: return "dfkdr";
            case OverlayManager.DAILY_WLR_KEY: return "dwlr";
            case OverlayManager.DAILY_STARS_KEY: return "dstar";
            case OverlayManager.DAILY_BBLR_KEY: return "dbblr";
            case OverlayManager.DAILY_KDR_KEY: return "dkdr";
            case OverlayManager.WEEKLY_FKDR_KEY: return "wfkdr";
            case OverlayManager.WEEKLY_WLR_KEY: return "wwlr";
            case OverlayManager.WEEKLY_STARS_KEY: return "wstar";
            case OverlayManager.WEEKLY_BBLR_KEY: return "wbblr";
            case OverlayManager.WEEKLY_KDR_KEY: return "wkdr";
            case OverlayManager.MONTHLY_FKDR_KEY: return "mfkdr";
            case OverlayManager.MONTHLY_WLR_KEY: return "mwlr";
            case OverlayManager.MONTHLY_STARS_KEY: return "mstar";
            case OverlayManager.MONTHLY_BBLR_KEY: return "mbblr";
            case OverlayManager.MONTHLY_KDR_KEY: return "mkdr";
            case OverlayManager.WINSTREAK_KEY:  return "ws";
            case OverlayManager.URCHIN_KEY:     return "tags";
            case OverlayManager.SESSION_KEY:    return "sess";
            case OverlayManager.LEVEL_KEY:      return "lvl";
            case OverlayManager.PING_KEY:       return "ping";
            default: return colKey.toLowerCase(Locale.ROOT);
        }
    }

    /**
     * Re-style a cell value for nerdify mode. Tags and special markers keep their colors.
     */
    public static String styleCell(String colKey, String text, Map<String, Object> ps, String uuid) {
        if (text == null || text.isEmpty()) return text;

        switch (colKey) {
            case OverlayManager.PLAYER_KEY:
                return stylePlayer(uuid, text, ps);
            case OverlayManager.RANK_KEY:
                return styleRank(text);
            case OverlayManager.STAR_KEY:
                return styleStar(text);
            case OverlayManager.FKDR_KEY:
            case OverlayManager.WLR_KEY:
            case OverlayManager.BBLR_KEY:
            case OverlayManager.KDR_KEY:
            case OverlayManager.DAILY_FKDR_KEY:
            case OverlayManager.DAILY_WLR_KEY:
            case OverlayManager.DAILY_BBLR_KEY:
            case OverlayManager.DAILY_KDR_KEY:
            case OverlayManager.WEEKLY_FKDR_KEY:
            case OverlayManager.WEEKLY_WLR_KEY:
            case OverlayManager.WEEKLY_BBLR_KEY:
            case OverlayManager.WEEKLY_KDR_KEY:
            case OverlayManager.MONTHLY_FKDR_KEY:
            case OverlayManager.MONTHLY_WLR_KEY:
            case OverlayManager.MONTHLY_BBLR_KEY:
            case OverlayManager.MONTHLY_KDR_KEY:
                return styleFkdr(text);
            case OverlayManager.DAILY_STARS_KEY:
            case OverlayManager.WEEKLY_STARS_KEY:
            case OverlayManager.MONTHLY_STARS_KEY:
                return plainGray(text);
            case OverlayManager.KILLS_KEY:
            case OverlayManager.FINALS_KEY:
            case OverlayManager.BEDS_KEY:
            case OverlayManager.WINS_KEY:
                return plainGray(text);
            case OverlayManager.PING_KEY:
                return stylePing(text);
            case OverlayManager.URCHIN_KEY:
                return styleTag(text);
            case OverlayManager.WINSTREAK_KEY:
            case OverlayManager.SESSION_KEY:
            case OverlayManager.LEVEL_KEY:
                return plainGray(text);
            case OverlayManager.ENCOUNTERS_KEY:
                return GREEN + ColorUtil.strip(text);
            default:
                return plainWhite(text);
        }
    }

    private static String stylePlayer(String uuid, String text, Map<String, Object> ps) {
        if (LazifyConfig.INSTANCE.isShowRanks() && text != null && !text.isEmpty()) {
            String rankPre = rankPrefixFor(ps);
            if (rankPre.isEmpty()) {
                String plain = ColorUtil.strip(text);
                if (plain.startsWith("[") && plain.contains("]")) {
                    return text;
                }
            }
        }

        String stripped = ColorUtil.strip(text);
        boolean nicked = ps != null && Boolean.TRUE.equals(ps.get("nicked"));
        boolean showTeams = OverlayManager.INSTANCE.showTeamColors;
        boolean showPrefix = OverlayManager.INSTANCE.showTeamPrefix;
        String rankPre = rankPrefixFor(ps);

        if (stripped.contains(" > ")) {
            int idx = stripped.indexOf(" > ");
            String nick = stripped.substring(0, idx);
            String real = stripped.substring(idx + 3);
            TeamStyle team = resolveTeam(uuid, ps, text);
            if (showTeams && !team.letter.isEmpty()) {
                String nickFmt = showPrefix ? team.letter + "_" + nick : nick;
                return withRankPrefix(rankPre, team.color + nickFmt + " \u00a77> \u00a7a" + real);
            }
            return withRankPrefix(rankPre, "\u00a7e" + nick + " \u00a77> \u00a7a" + real);
        }

        TeamStyle team = resolveTeam(uuid, ps, text);
        if (showTeams && !team.letter.isEmpty()) {
            String name = playerNameOnly(stripped);
            String body = showPrefix
                    ? team.color + team.letter + "_" + name
                    : team.color + name;
            return withRankPrefix(rankPre, body);
        }

        if (nicked) {
            return "\u00a7e" + playerNameOnly(stripped);
        }

        String name = playerNameOnly(stripped);
        String rankColor = rankColorFor(ps, text);
        String body = (rankColor != null ? rankColor : WHITE) + name;
        return withRankPrefix(rankPre, body);
    }

    private static String rankPrefixFor(Map<String, Object> ps) {
        if (!LazifyConfig.INSTANCE.isShowRanks() || ps == null) return "";
        Object rp = ps.get("rankPrefix");
        if (!(rp instanceof String)) return "";
        String s = (String) rp;
        if (s.isEmpty() || "\u00a77".equals(s)) return "";
        return s;
    }

    private static String withRankPrefix(String rankPre, String body) {
        if (rankPre == null || rankPre.isEmpty()) return body;
        return rankPre + " " + body;
    }

    private static String styleRank(String text) {
        if (text == null || text.isEmpty()) return GRAY + "-";
        String plain = ColorUtil.strip(text);
        if ("[NICK]".equals(plain)) return "\u00a7e[NICK]";
        if ("[NON]".equals(plain)) return GRAY + "[NON]";
        return text;
    }

    private static String playerNameOnly(String stripped) {
        if (stripped == null) return "";
        String s = stripped.trim();
        while (s.startsWith("[") && s.contains("]")) {
            int end = s.indexOf(']');
            s = s.substring(end + 1).trim();
        }
        return s;
    }

    private static String rankColorFor(Map<String, Object> ps, String text) {
        if (ps != null) {
            Object rc = ps.get("rankColor");
            if (rc instanceof String && !((String) rc).isEmpty()) {
                return (String) rc;
            }
        }
        if (text != null && text.length() >= 2 && text.charAt(0) == '\u00a7') {
            return text.substring(0, 2);
        }
        return null;
    }

    /** Width basis for nerdify column sizing (plain text, no rank bloat). */
    public static String measureText(String colKey, String raw) {
        if (raw == null) return "";
        String plain = ColorUtil.strip(raw);
        if (OverlayManager.PLAYER_KEY.equals(colKey)) {
            if (plain.contains(" > ")) {
                plain = plain.substring(0, plain.indexOf(" > "));
            }
            return playerNameOnly(plain);
        }
        if (OverlayManager.STAR_KEY.equals(colKey)) {
            String num = plain.replaceAll("[^0-9]", "");
            return num.isEmpty() ? plain : num;
        }
        if (OverlayManager.RANK_KEY.equals(colKey)) {
            return plain;
        }
        return plain;
    }

    /** Plain display width sample for a styled player cell (includes team prefix). */
    public static String measurePlayer(String uuid, String raw, Map<String, Object> ps) {
        String styled = styleCell(OverlayManager.PLAYER_KEY, raw, ps, uuid);
        return ColorUtil.strip(styled);
    }

    private static String styleTag(String text) {
        String plain = ColorUtil.strip(text);
        if (plain.isEmpty()) return GRAY + "-";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < plain.length(); i++) {
            char c = plain.charAt(i);
            if (c == '+') out.append(GRAY).append('+');
            else out.append(RED).append(c);
        }
        return out.toString();
    }

    private static String styleStar(String text) {
        String num = ColorUtil.strip(text).replaceAll("[^0-9]", "");
        if (num.isEmpty()) return "\u00a7f" + ColorUtil.strip(text);
        try {
            int star = Integer.parseInt(num);
            if (star >= 200) return GOLD + num;
            return WHITE + num;
        } catch (NumberFormatException e) {
            return WHITE + ColorUtil.strip(text);
        }
    }

    private static String styleFkdr(String text) {
        String stripped = ColorUtil.strip(text);
        try {
            double v = Double.parseDouble(stripped);
            if (v >= 1.0) return GREEN + stripped;
            return GRAY + stripped;
        } catch (NumberFormatException e) {
            return GRAY + stripped;
        }
    }

    private static String stylePing(String text) {
        String stripped = ColorUtil.strip(text);
        if (stripped.equals("-")) return GRAY + "-";
        try {
            int ping = Integer.parseInt(stripped);
            if (ping >= 200) return RED + ping;
            if (ping >= 150) return GOLD + ping;
            return GREEN + ping;
        } catch (NumberFormatException e) {
            return GRAY + stripped;
        }
    }

    private static String plainGray(String text) {
        if (text == null || text.isEmpty()) return GRAY + "-";
        return GRAY + ColorUtil.strip(text);
    }

    private static String plainWhite(String text) {
        if (text == null || text.isEmpty()) return WHITE + "-";
        return WHITE + ColorUtil.strip(text);
    }

    private static final class TeamStyle {
        final String letter;
        final String color;
        TeamStyle(String letter, String color) {
            this.letter = letter;
            this.color = color;
        }
    }

    private static TeamStyle resolveTeam(String uuid, Map<String, Object> ps, String text) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) return new TeamStyle("", "");

        String username = OverlayManager.INSTANCE.uuidToNameForRender(uuid);
        if (username == null || username.isEmpty()) {
            if (ps != null) {
                Object u = ps.get("username");
                if (u instanceof String) username = (String) u;
            }
        }
        if (username == null || username.isEmpty()) {
            username = playerNameOnly(ColorUtil.strip(text));
        }
        if (username.isEmpty()) return new TeamStyle("", "");

        ScorePlayerTeam team = mc.theWorld.getScoreboard().getPlayersTeam(username);
        if (team == null) return new TeamStyle("", "");

        String letter = teamMarker(team.getRegisteredName());
        String color = teamColorCode(team);
        return new TeamStyle(letter, color);
    }

    /** Team letter prefix for column width (empty when no team). */
    public static String resolveTeamLetter(String uuid) {
        return resolveTeam(uuid, null, "").letter;
    }

    private static String teamMarker(String teamName) {
        if (teamName == null) return "";
        String n = teamName.toLowerCase(Locale.ROOT);
        if (n.contains("red"))    return "R";
        if (n.contains("blue"))   return "B";
        if (n.contains("green"))  return "G";
        if (n.contains("yellow")) return "Y";
        if (n.contains("aqua"))   return "A";
        if (n.contains("white"))  return "W";
        if (n.contains("pink"))   return "P";
        if (n.contains("gray") || n.contains("grey")) return "G";
        return "";
    }

    private static String teamColorCode(ScorePlayerTeam team) {
        if (team.getChatFormat() != null) {
            return team.getChatFormat().toString();
        }
        String n = team.getRegisteredName() == null ? "" : team.getRegisteredName().toLowerCase(Locale.ROOT);
        if (n.contains("red"))    return EnumChatFormatting.RED.toString();
        if (n.contains("blue"))   return EnumChatFormatting.BLUE.toString();
        if (n.contains("green"))  return EnumChatFormatting.GREEN.toString();
        if (n.contains("yellow")) return EnumChatFormatting.YELLOW.toString();
        if (n.contains("aqua"))   return EnumChatFormatting.AQUA.toString();
        if (n.contains("white"))  return EnumChatFormatting.WHITE.toString();
        if (n.contains("pink"))   return EnumChatFormatting.LIGHT_PURPLE.toString();
        if (n.contains("gray") || n.contains("grey")) return EnumChatFormatting.GRAY.toString();
        return EnumChatFormatting.WHITE.toString();
    }
}
