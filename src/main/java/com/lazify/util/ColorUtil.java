package com.lazify.util;

import java.awt.Color;

public class ColorUtil {

    public static String colorize(String text) {
        if (text == null) return "";
        return text.replace("&0","\u00a70").replace("&1","\u00a71").replace("&2","\u00a72")
                   .replace("&3","\u00a73").replace("&4","\u00a74").replace("&5","\u00a75")
                   .replace("&6","\u00a76").replace("&7","\u00a77").replace("&8","\u00a78")
                   .replace("&9","\u00a79").replace("&a","\u00a7a").replace("&b","\u00a7b")
                   .replace("&c","\u00a7c").replace("&d","\u00a7d").replace("&e","\u00a7e")
                   .replace("&f","\u00a7f").replace("&k","\u00a7k").replace("&l","\u00a7l")
                   .replace("&m","\u00a7m").replace("&n","\u00a7n").replace("&o","\u00a7o")
                   .replace("&r","\u00a7r").replace("&A","\u00a7a").replace("&B","\u00a7b")
                   .replace("&C","\u00a7c").replace("&D","\u00a7d").replace("&E","\u00a7e")
                   .replace("&F","\u00a7f");
    }

    public static String strip(String text) {
        if (text == null) return "";
        return text.replaceAll("\u00a7[0-9a-fk-orA-FK-OR]", "");
    }

    public static String colorSymbol() { return "\u00a7"; }

    /** hue=0 → black, hue=360 → chroma, else HSB full-sat/val */
    public static int getHueRGB(float hue, int alpha) {
        if (hue == 0) return new Color(0, 0, 0, alpha).getRGB();
        if (hue == 360) return getChroma(1L, alpha);
        Color c = Color.getHSBColor(hue / 360.0f, 1.0f, 1.0f);
        return (alpha << 24) | (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue();
    }

    public static int getChroma(long speed, int alpha) {
        float hue = System.currentTimeMillis() % (15000L / speed) / (15000.0f / speed);
        Color c = Color.getHSBColor(hue, 1.0f, 1.0f);
        return (alpha << 24) | (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue();
    }

    // ── Prestige color: exact port of the original switch ──────────────────────

    public static String getPrestigeColor(int number) {
        int prestige = number - (number % 100);
        String nums = String.format("%04d", number);
        char d0 = nums.charAt(0), d1 = nums.charAt(1), d2 = nums.charAt(2), d3 = nums.charAt(3);

        switch (prestige) {
            case 5000: return "\u00a74"+d0+"\u00a75"+d1+"\u00a79"+d2+d3+"\u00a7";
            case 4900: return "\u00a7a"+d0+"\u00a7f"+d1+d2+"\u00a7a"+d3;
            case 4800: return "\u00a75"+d0+"\u00a7c"+d1+"\u00a76"+d2+"\u00a7e"+d3;
            case 4700: return "\u00a7f"+d0+"\u00a74"+d1+d2+"\u00a79"+d3;
            case 4600: return "\u00a72"+d0+"\u00a7b"+d1+d2+"\u00a76"+d3;
            case 4500: return "\u00a7f"+d0+"\u00a7b"+d1+d2+"\u00a72"+d3;
            case 4400: return "\u00a72"+d0+"\u00a7a"+d1+"\u00a7e"+d2+"\u00a76"+d3;
            case 4300: return "\u00a70"+d0+"\u00a75"+d1+d2+"\u00a75"+d3;
            case 4200: return "\u00a71"+d0+"\u00a79"+d1+"\u00a73"+d2+"\u00a7f"+d3;
            case 4100: return "\u00a7e"+d0+"\u00a76"+d1+"\u00a7c"+d2+"\u00a7d"+d3;
            case 4000: return "\u00a75"+d0+"\u00a7c"+d1+d2+"\u00a76"+d3;
            case 3900: return "\u00a7c"+d0+"\u00a7a"+d1+d2+"\u00a73"+d3;
            case 3800: return "\u00a71"+d0+"\u00a79"+d1+d2+"\u00a75"+d3;
            case 3700: return "\u00a74"+d0+"\u00a7c"+d1+d2+"\u00a7b"+d3;
            case 3600: return "\u00a7a"+d0+d1+"\u00a7b"+d2+"\u00a79"+d3;
            case 3500: return "\u00a7c"+d0+"\u00a74"+d1+d2+"\u00a72"+d3;
            case 3400: return "\u00a72"+d0+"\u00a7a"+d1+d2+"\u00a75"+d3;
            case 3300: return "\u00a79"+d0+d1+"\u00a7d"+d2+"\u00a7c"+d3;
            case 3200: return "\u00a7c"+d0+"\u00a77"+d1+d2+"\u00a74"+d3;
            case 3100: return "\u00a79"+d0+"\u00a72"+d1+d2+"\u00a76"+d3;
            case 3000: return "\u00a7e"+d0+"\u00a76"+d1+"\u00a7c"+d2+"\u00a74"+d3;
            case 2900: return "\u00a7b"+d0+"\u00a73"+d1+d2+"\u00a79"+d3;
            case 2800: return "\u00a7a"+d0+"\u00a72"+d1+d2+"\u00a76"+d3;
            case 2700: return "\u00a7e"+d0+"\u00a7f"+d1+d2+"\u00a78"+d3;
            case 2600: return "\u00a74"+d0+"\u00a7c"+d1+d2+"\u00a7d"+d3;
            case 2500: return "\u00a7f"+d0+"\u00a7a"+d1+d2+"\u00a72"+d3;
            case 2400: return "\u00a7b"+d0+"\u00a7f"+d1+d2+"\u00a77"+d3;
            case 2300: return "\u00a75"+d0+"\u00a7d"+d1+d2+"\u00a76"+d3;
            case 2200: return "\u00a76"+d0+"\u00a7f"+d1+d2+"\u00a7b"+d3;
            case 2100: return "\u00a7f"+d0+"\u00a7e"+d1+d2+"\u00a76"+d3;
            case 2000: return "\u00a77"+d0+"\u00a7f"+d1+d2+"\u00a77"+d3;
            case 1000: return "\u00a7c"+d0+"\u00a76"+d1+"\u00a7e"+d2+"\u00a7a"+d3;
            case 1900: case 900:  return "\u00a75"+number;
            case 1800: case 800:  return "\u00a79"+number;
            case 1700: case 700:  return "\u00a7d"+number;
            case 1600:            return "\u00a7c"+number;
            case 600:             return "\u00a74"+number;
            case 1500: case 500:  return "\u00a73"+number;
            case 1400: case 400:  return "\u00a72"+number;
            case 1300: case 300:  return "\u00a7b"+number;
            case 1200: case 200:  return "\u00a76"+number;
            case 1100: case 100:  return "\u00a7f"+number;
            default:              return "\u00a77"+number;
        }
    }

    // ── FKDR color: exact original thresholds ──────────────────────────────────

    public static String getFkdrColor(String fkdr) {
        try {
            double v = Double.parseDouble(fkdr);
            if (v > 1000) return "\u00a75" + fkdr;
            if (v > 100)  return "\u00a74" + fkdr;
            if (v > 10)   return "\u00a7c" + fkdr;
            if (v > 5)    return "\u00a76" + fkdr;
            if (v > 2.4)  return "\u00a7e" + fkdr;
            if (v > 1.4)  return "\u00a7f" + fkdr;
            return "\u00a77" + fkdr;
        } catch (NumberFormatException e) {
            return "\u00a77" + fkdr;
        }
    }

    // ── Winstreak color: exact original thresholds ─────────────────────────────

    public static String getWinstreakColor(String winstreak) {
        if (winstreak == null || winstreak.isEmpty()) return "";
        try {
            int v = Integer.parseInt(winstreak);
            if (v == 0) return "";
            if (v >= 1000) return "\u00a75" + v;
            if (v >= 500)  return "\u00a7d" + v;
            if (v >= 300)  return "\u00a74" + v;
            if (v >= 150)  return "\u00a7c" + v;
            if (v >= 100)  return "\u00a76" + v;
            if (v >= 75)   return "\u00a7e" + v;
            if (v >= 50)   return "\u00a72" + v;
            if (v >= 25)   return "\u00a7a" + v;
            return "\u00a77" + v;
        } catch (NumberFormatException e) {
            return "\u00a77" + winstreak;
        }
    }

    // ── Session color: based on session duration (lastLogout - lastLogin) ───────

    public static String getSessionColor(long lastLogin, long lastLogout, String sessionFormatted) {
        long session = lastLogout - lastLogin;
        if (session > 21600000) return "\u00a74" + sessionFormatted;
        if (session > 14400000) return "\u00a7c" + sessionFormatted;
        if (session > 9000000)  return "\u00a76" + sessionFormatted;
        if (session > 7200000)  return "\u00a7e" + sessionFormatted;
        if (session > 1200000)  return "\u00a7a" + sessionFormatted;
        if (session > 600000)   return "\u00a7e" + sessionFormatted;
        if (session > 300000)   return "\u00a7e" + sessionFormatted;
        if (session > 150000)   return "\u00a7c" + sessionFormatted;
        return "\u00a74" + sessionFormatted;
    }

    // ── Seen/encounter color: exact original ────────────────────────────────────

    public static String getSeenColor(int encounters) {
        if (encounters > 5) return "\u00a7c" + encounters;
        if (encounters > 3) return "\u00a76" + encounters;
        if (encounters > 1) return "\u00a7e" + encounters;
        return "\u00a7a" + encounters;
    }

    // ── Urchin tag color: exact original tag types ─────────────────────────────

    public static String getUrchinTagColor(String tagType) {
        switch (tagType) {
            case "blatant_cheater":   return "\u00a7cBC";
            case "confirmed_cheater": return "\u00a7cC";
            case "closet_cheater":    return "\u00a7eCC";
            case "sniper":            return "\u00a74S";
            default:                  return "";
        }
    }

    // ── Rank helpers (take full Hypixel API response JsonWrapper) ───────────────

    public static String getRank(com.lazify.api.JsonWrapper playerData) {
        if (playerData == null || !playerData.exists()) return "\u00a77";
        if (!playerData.object("player").exists()) return "\u00a77";
        com.lazify.api.JsonWrapper player = playerData.object("player");

        String prefix = player.get("prefix", "");
        if (!prefix.isEmpty()) {
            try { return strip(prefix.substring(4, prefix.length() - 1)); }
            catch (Exception ignored) {}
        }

        String rank = player.get("rank", "");
        if (!rank.isEmpty()) {
            if (rank.equals("GAME_MASTER")) return "GM";
            if (rank.equals("YOUTUBER"))   return "YOUTUBE";
            if (!rank.equals("NORMAL"))    return rank;
        }

        String packageRank = player.get("newPackageRank", "");
        if (packageRank.isEmpty()) packageRank = player.get("packageRank", "");
        if (packageRank.isEmpty() || packageRank.equals("NONE")) return "\u00a77";

        if (packageRank.startsWith("MVP")) {
            String monthly = player.get("monthlyPackageRank", "");
            if (monthly.equals("SUPERSTAR")) return "MVP++";
            return packageRank.length() == 3 ? packageRank : "MVP+";
        }
        if (packageRank.startsWith("VIP")) {
            return packageRank.length() == 3 ? packageRank : "VIP+";
        }
        return "\u00a77";
    }

    public static String getRankColor(String rank) {
        switch (rank) {
            case "VIP": case "VIP+":             return "\u00a7a";
            case "MVP": case "MVP+":             return "\u00a7b";
            case "MVP++":                        return "\u00a76";
            case "YOUTUBE": case "ADMIN":
            case "OWNER":                        return "\u00a7c";
            case "GM":                           return "\u00a72";
            case "PIG+++":                       return "\u00a7d";
            default:                             return "\u00a77";
        }
    }

    public static String getFormattedRank(com.lazify.api.JsonWrapper playerData) {
        String rank = getRank(playerData);
        if (rank.equals("\u00a77") || rank.isEmpty()) return "\u00a77";

        com.lazify.api.JsonWrapper player = playerData.object("player");
        String plusColor = player.get("rankPlusColor", "RED");
        String colorCode;
        switch (plusColor) {
            case "BLACK":        colorCode = "\u00a70"; break;
            case "DARK_BLUE":    colorCode = "\u00a71"; break;
            case "DARK_GREEN":   colorCode = "\u00a72"; break;
            case "DARK_AQUA":    colorCode = "\u00a73"; break;
            case "DARK_RED":     colorCode = "\u00a74"; break;
            case "DARK_PURPLE":  colorCode = "\u00a75"; break;
            case "GOLD":         colorCode = "\u00a76"; break;
            case "GRAY":         colorCode = "\u00a77"; break;
            case "DARK_GRAY":    colorCode = "\u00a78"; break;
            case "BLUE":         colorCode = "\u00a79"; break;
            case "GREEN":        colorCode = "\u00a7a"; break;
            case "AQUA":         colorCode = "\u00a7b"; break;
            case "LIGHT_PURPLE": colorCode = "\u00a7d"; break;
            case "YELLOW":       colorCode = "\u00a7e"; break;
            case "WHITE":        colorCode = "\u00a7f"; break;
            default:             colorCode = "\u00a7c"; break; // RED
        }

        switch (rank) {
            case "VIP":    return "\u00a7a[VIP]";
            case "VIP+":   return "\u00a7a[VIP\u00a76+\u00a7a]";
            case "MVP":    return "\u00a7b[MVP]";
            case "MVP+":   return "\u00a7b[MVP"+colorCode+"+\u00a7b]";
            case "MVP++":  return "\u00a76[MVP"+colorCode+"++\u00a76]";
            case "GM":     return "\u00a72[GM]";
            case "YOUTUBE":return "\u00a7c[\u00a7fYOUTUBE\u00a7c]";
            case "ADMIN":  return "\u00a7c[ADMIN]";
            case "OWNER":  return "\u00a7c[OWNER]";
            case "PIG+++": return "\u00a7d[PIG\u00a7b+++\u00a7d]";
            default:       return "\u00a77";
        }
    }

    // ── Relative timestamp (session duration, up to 2 components) ──────────────

    public static String calculateRelativeTimestamp(long lastLogin, long lastLogout) {
        long timeSince = (lastLogout - lastLogin) / 1000L;
        long remaining = timeSince;

        long years   = remaining / 31557600L; remaining %= 31557600L;
        long months  = remaining / 2629800L;  remaining %= 2629800L;
        long days    = remaining / 86400L;    remaining %= 86400L;
        long hours   = remaining / 3600L;     remaining %= 3600L;
        long minutes = remaining / 60L;
        long seconds = remaining % 60L;

        StringBuilder msg = new StringBuilder();
        int n = 0;
        if (years   > 0 && n < 2) { msg.append(years).append("y");   n++; }
        if (months  > 0 && n < 2) { msg.append(months).append("mo"); n++; }
        if (days    > 0 && n < 2) { msg.append(days).append("d");    n++; }
        if (hours   > 0 && n < 2) { msg.append(hours).append("h");   n++; }
        if (minutes > 0 && n < 2) { msg.append(minutes).append("m"); n++; }
        if ((seconds > 0 && n < 2) || timeSince == 0) msg.append(seconds).append("s");

        return msg.toString();
    }

    // ── Misc helpers ───────────────────────────────────────────────────────────

    public static String formatDoubleStr(double val) {
        if (val % 1 == 0) return String.valueOf((int) val);
        return String.valueOf(val);
    }

    public static double round(double val, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(val * factor) / factor;
    }

    public static String generatePadding(char character, int pixelWidth, int charWidth) {
        if (charWidth <= 0) charWidth = 4;
        StringBuilder sb = new StringBuilder();
        int count = pixelWidth / charWidth;
        for (int i = 0; i < count; i++) sb.append(character);
        return sb.toString();
    }
}
