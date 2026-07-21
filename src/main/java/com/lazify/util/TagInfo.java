package com.lazify.util;

/**
 * Combined Urchin + Seraph tag state for one player.
 */
public final class TagInfo {

    public String urchinType = "";
    public String urchinReason = "";
    public String seraphType = "";
    public String seraphReason = "";
    public boolean hasUrchin;
    public boolean hasSeraph;

    public boolean hasAnyTag() {
        return hasUrchin || hasSeraph;
    }

    public String overlayDisplay() {
        if (!hasAnyTag()) return "";
        String urchin = hasUrchin ? ColorUtil.getUrchinTagColor(urchinType) : "";
        String seraph = hasSeraph ? ColorUtil.getSeraphTagColor(seraphType) : "";
        if (hasUrchin && hasSeraph) {
            String s = seraph.isEmpty() ? "\u00a7cBL" : seraph;
            String u = urchin.isEmpty() ? "\u00a7cT" : urchin;
            return s + "\u00a77+" + u;
        }
        if (!urchin.isEmpty()) return urchin;
        if (!seraph.isEmpty()) return seraph;
        return "\u00a7cT";
    }

    public double threatValue() {
        double best = 0.0;
        if (hasUrchin) best = Math.max(best, threatForType(urchinType));
        if (hasSeraph) best = Math.max(best, threatForType(seraphType));
        return best;
    }

    private static double threatForType(String type) {
        if (type == null || type.isEmpty()) return 0.0;
        String norm = type.toLowerCase().replace(' ', '_');
        switch (norm) {
            case "blatant_cheater":   return 4.0;
            case "confirmed_cheater": return 3.5;
            case "closet_cheater":    return 2.25;
            case "sniper":            return 1.5;
            default:
                if (norm.contains("blatant")) return 4.0;
                if (norm.contains("confirmed")) return 3.5;
                if (norm.contains("closet")) return 2.25;
                if (norm.contains("sniper")) return 1.5;
                return 2.0;
        }
    }

    public static String formatSourceType(String type) {
        if (type == null || type.isEmpty()) return "Tagged";
        return ColorUtil.formatTagType(type.replace(' ', '_'));
    }
}
