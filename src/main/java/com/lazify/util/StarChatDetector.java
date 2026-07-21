package com.lazify.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts Bedwars star level from Hypixel chat lines.
 * Stars appear in brackets before team/rank when a player chats, e.g. {@code [523?] [RED] Name: hi}.
 */
public final class StarChatDetector {

    /** {@code [523]} or {@code [523?]} — numeric star bracket (team tags like {@code [RED]} are skipped). */
    private static final Pattern STAR_BRACKET = Pattern.compile("\\[(\\d+)\\??\\]");

    private StarChatDetector() {}

    /**
     * @param stripped  Plain chat line (color codes removed)
     * @param username  Sender username from chat parsing
     * @return Bedwars star level, or null if not present
     */
    public static Integer extract(String stripped, String username) {
        if (stripped == null || username == null || username.isEmpty()) return null;

        int colonIdx = stripped.lastIndexOf(':');
        if (colonIdx <= 0) return null;

        String beforeColon = stripped.substring(0, colonIdx).trim();
        if (beforeColon.isEmpty()) return null;

        int nameIdx = indexOfUsername(beforeColon, username);
        if (nameIdx < 0) return null;

        String prefix = beforeColon.substring(0, nameIdx).trim();
        if (prefix.isEmpty()) return null;

        Integer star = null;
        Matcher m = STAR_BRACKET.matcher(prefix);
        while (m.find()) {
            star = m.group(1).isEmpty() ? star : parseInt(m.group(1));
        }
        return star;
    }

    private static int indexOfUsername(String beforeColon, String username) {
        if (beforeColon.endsWith(username)) {
            return beforeColon.length() - username.length();
        }
        String lower = beforeColon.toLowerCase();
        String target = username.toLowerCase();
        if (lower.endsWith(target)) {
            return beforeColon.length() - username.length();
        }
        int idx = beforeColon.lastIndexOf(' ');
        if (idx >= 0 && idx + 1 < beforeColon.length()) {
            String last = beforeColon.substring(idx + 1);
            if (last.equalsIgnoreCase(username)) return idx + 1;
        }
        return -1;
    }

    private static Integer parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
