package com.lazify.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lazify.util.BedwarsExpCalculator;

/**
 * Normalizes Hypixel-shaped player JSON into the structure expected by OverlayManager.parseStats().
 * Bedwars + network only (all the overlay uses). Mirrors keyless-hypixel-api/api/parsers.py.
 */
public final class PlayerStatsParser {

    private PlayerStatsParser() {}

    public static JsonWrapper parse(JsonWrapper data, String provider) {
        if (!data.exists()) return null;

        JsonWrapper player = data.object("player");
        if (!player.exists()) player = data.object();
        if (!player.exists()) return null;

        JsonWrapper stats = player.object("stats");
        JsonWrapper achievements = player.object("achievements");
        JsonWrapper bedwarsRaw = stats.object("Bedwars");

        JsonObject root = new JsonObject();
        root.addProperty("provider", provider);
        root.addProperty("name", player.get("displayname", "Unknown"));
        root.add("network", buildNetwork(player));

        if (bedwarsRaw.exists()) {
            root.add("bedwars", buildBedwars(bedwarsRaw, achievements));
        }

        return new JsonWrapper(root);
    }

    /**
     * Build overlay stats JSON from a Bordic session {@code current.value} Bedwars blob
     * (lifetime stats piggybacked on daily/weekly/monthly bulk responses).
     */
    public static JsonWrapper fromBedwarsStats(String name, JsonWrapper bedwarsRaw) {
        if (bedwarsRaw == null || !bedwarsRaw.exists()) return null;
        JsonObject root = new JsonObject();
        root.addProperty("provider", "bordic-sessions");
        root.addProperty("name", name == null || name.isEmpty() ? "Unknown" : name);
        JsonObject network = new JsonObject();
        network.addProperty("level", 0);
        network.addProperty("exp", 0);
        network.addProperty("rank", "");
        network.addProperty("prefix", "");
        network.addProperty("language", "ENGLISH");
        root.add("network", network);
        root.add("bedwars", buildBedwars(bedwarsRaw, new JsonWrapper(null)));
        return new JsonWrapper(root);
    }

    /** Fill gaps in {@code primary} from {@code secondary} (rank, stars, etc.). */
    public static JsonWrapper merge(JsonWrapper primary, JsonWrapper secondary) {
        if (primary == null || !primary.exists()) return secondary;
        if (secondary == null || !secondary.exists()) return primary;

        JsonObject root = new JsonParser().parse(primary.getRaw().toString()).getAsJsonObject();
        mergeNetwork(root, primary.object("network"), secondary.object("network"));
        mergeBedwars(root, primary.object("bedwars"), secondary.object("bedwars"));
        return new JsonWrapper(root);
    }

    public static boolean needsRank(JsonWrapper parsed) {
        if (parsed == null || !parsed.exists()) return true;
        JsonWrapper network = parsed.object("network");
        if (!network.exists()) return true;
        return isEmptyRank(network.get("rank", ""));
    }

    private static void mergeNetwork(JsonObject root, JsonWrapper primary, JsonWrapper secondary) {
        if (!secondary.exists()) return;

        JsonObject network;
        if (root.has("network") && root.get("network").isJsonObject()) {
            network = root.getAsJsonObject("network");
        } else {
            network = new JsonParser().parse(secondary.getRaw().toString()).getAsJsonObject();
            root.add("network", network);
            return;
        }

        String primaryRank = primary.exists() ? primary.get("rank", "") : "";
        String secondaryRank = secondary.get("rank", "");
        if (isEmptyRank(primaryRank) && !isEmptyRank(secondaryRank)) {
            network.addProperty("rank", secondaryRank);
        }
    }

    private static void mergeBedwars(JsonObject root, JsonWrapper primary, JsonWrapper secondary) {
        if (!secondary.exists()) return;

        JsonObject bedwars;
        if (root.has("bedwars") && root.get("bedwars").isJsonObject()) {
            bedwars = root.getAsJsonObject("bedwars");
        } else {
            bedwars = new JsonParser().parse(secondary.getRaw().toString()).getAsJsonObject();
            root.add("bedwars", bedwars);
            return;
        }

        JsonWrapper primaryOverall = primary.exists() ? primary.object("overall") : new JsonWrapper(null);
        JsonWrapper secondaryOverall = secondary.object("overall");
        if (!secondaryOverall.exists()) return;

        JsonObject overall;
        if (bedwars.has("overall") && bedwars.get("overall").isJsonObject()) {
            overall = bedwars.getAsJsonObject("overall");
        } else {
            overall = new JsonParser().parse(secondaryOverall.getRaw().toString()).getAsJsonObject();
            bedwars.add("overall", overall);
            return;
        }

        int primaryStars = primaryOverall.exists() ? intVal(primaryOverall, "stars") : 0;
        int secondaryStars = intVal(secondaryOverall, "stars");
        if (secondaryStars > primaryStars) {
            overall.addProperty("stars", secondaryStars);
        }
    }

    private static JsonObject buildNetwork(JsonWrapper player) {
        JsonObject network = new JsonObject();
        double networkExp = num(player, "networkExp");
        String rank = resolveApiRank(player);

        network.addProperty("level", networkLevel(networkExp));
        network.addProperty("exp", networkExp);
        network.addProperty("karma", num(player, "karma"));
        network.addProperty("achievement_points", num(player, "achievementPoints"));
        network.addProperty("rank", rank);
        network.addProperty("prefix", player.get("prefix", ""));
        network.addProperty("first_login", num(player, "firstLogin"));
        network.addProperty("last_login", num(player, "lastLogin"));
        network.addProperty("last_logout", num(player, "lastLogout"));
        network.addProperty("most_recent_game", player.get("mostRecentGameType", ""));
        network.addProperty("language", player.get("userLanguage", "ENGLISH"));
        return network;
    }

    /** Rank string for {@code formatRankColumn}; prefers paid ranks over staff {@code rank}. */
    private static String resolveApiRank(JsonWrapper player) {
        String rank = player.get("newPackageRank", "");
        if (rank.isEmpty() || "NONE".equals(rank)) {
            rank = player.get("packageRank", "");
        }
        if ("NONE".equals(rank)) rank = "";

        String monthlyRank = player.get("monthlyPackageRank", "");
        if (!monthlyRank.isEmpty() && !"NONE".equals(monthlyRank)) {
            rank = monthlyRank;
        }

        if (isEmptyRank(rank)) {
            String staffRank = player.get("rank", "");
            if (!staffRank.isEmpty() && !"NORMAL".equalsIgnoreCase(staffRank)) {
                rank = staffRank;
            }
        }
        return rank == null ? "" : rank;
    }

    private static boolean isEmptyRank(String rank) {
        return rank == null || rank.isEmpty() || "NONE".equalsIgnoreCase(rank) || "NORMAL".equalsIgnoreCase(rank);
    }

    private static JsonObject buildBedwars(JsonWrapper s, JsonWrapper ach) {
        int fk = intVal(s, "final_kills_bedwars");
        int fd = intVal(s, "final_deaths_bedwars");
        int w = intVal(s, "wins_bedwars");
        int l = intVal(s, "losses_bedwars");
        int bb = intVal(s, "beds_broken_bedwars");
        int bl = intVal(s, "beds_lost_bedwars");
        int k = intVal(s, "kills_bedwars");
        int d = intVal(s, "deaths_bedwars");

        int stars = intVal(ach, "bedwars_level");
        if (stars <= 0) {
            int experience = intVal(s, "Experience");
            if (experience > 0) {
                stars = BedwarsExpCalculator.levelFromExp(experience);
            }
        }

        JsonObject overall = new JsonObject();
        overall.addProperty("stars", stars);
        overall.addProperty("coins", intVal(s, "coins"));
        if (s.get("winstreak").exists()) {
            overall.addProperty("winstreak", intVal(s, "winstreak"));
        }
        overall.addProperty("games_played", intVal(s, "games_played_bedwars"));
        overall.addProperty("kills", k);
        overall.addProperty("deaths", d);
        overall.addProperty("kdr", safeRatio(k, d));
        overall.addProperty("final_kills", fk);
        overall.addProperty("final_deaths", fd);
        overall.addProperty("fkdr", safeRatio(fk, fd));
        overall.addProperty("wins", w);
        overall.addProperty("losses", l);
        overall.addProperty("wlr", safeRatio(w, l));
        overall.addProperty("beds_broken", bb);
        overall.addProperty("beds_lost", bl);
        overall.addProperty("bblr", safeRatio(bb, bl));

        JsonObject modes = new JsonObject();
        addBedwarsMode(modes, "solo", "eight_one_", s);
        addBedwarsMode(modes, "doubles", "eight_two_", s);
        addBedwarsMode(modes, "threes", "four_three_", s);
        addBedwarsMode(modes, "fours", "four_four_", s);
        addBedwarsMode(modes, "4v4", "two_four_", s);

        JsonObject bedwars = new JsonObject();
        bedwars.add("overall", overall);
        bedwars.add("modes", modes);
        return bedwars;
    }

    private static void addBedwarsMode(JsonObject modes, String modeName, String prefix, JsonWrapper s) {
        int mfk = intVal(s, prefix + "final_kills_bedwars");
        int mfd = intVal(s, prefix + "final_deaths_bedwars");
        int mw = intVal(s, prefix + "wins_bedwars");
        int ml = intVal(s, prefix + "losses_bedwars");
        int mbb = intVal(s, prefix + "beds_broken_bedwars");
        int mbl = intVal(s, prefix + "beds_lost_bedwars");
        int mk = intVal(s, prefix + "kills_bedwars");
        int md = intVal(s, prefix + "deaths_bedwars");
        if (mfk == 0 && mfd == 0 && mw == 0 && ml == 0 && mk == 0 && md == 0) return;

        JsonObject mode = new JsonObject();
        if (s.get(prefix + "winstreak").exists()) {
            mode.addProperty("winstreak", intVal(s, prefix + "winstreak"));
        }
        mode.addProperty("games_played", intVal(s, prefix + "games_played_bedwars"));
        mode.addProperty("kills", mk);
        mode.addProperty("deaths", md);
        mode.addProperty("kdr", safeRatio(mk, md));
        mode.addProperty("final_kills", mfk);
        mode.addProperty("final_deaths", mfd);
        mode.addProperty("fkdr", safeRatio(mfk, mfd));
        mode.addProperty("wins", mw);
        mode.addProperty("losses", ml);
        mode.addProperty("wlr", safeRatio(mw, ml));
        mode.addProperty("beds_broken", mbb);
        mode.addProperty("beds_lost", mbl);
        mode.addProperty("bblr", safeRatio(mbb, mbl));
        modes.add(modeName, mode);
    }

    private static double networkLevel(double exp) {
        return round((Math.sqrt(exp + 15312.5D) - 125.0D / Math.sqrt(2.0D)) / (25.0D * Math.sqrt(2.0D)), 2);
    }

    private static double safeRatio(int a, int b) {
        if (b == 0) return a;
        return round((double) a / (double) b, 2);
    }

    private static double round(double value, int places) {
        double scale = Math.pow(10, places);
        return Math.round(value * scale) / scale;
    }

    private static int intVal(JsonWrapper obj, String key) {
        return (int) num(obj, key);
    }

    private static double num(JsonWrapper obj, String key) {
        JsonWrapper val = obj.get(key);
        if (!val.exists()) return 0;
        return val.asDouble(0);
    }
}
