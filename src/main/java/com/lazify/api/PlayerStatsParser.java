package com.lazify.api;

import com.google.gson.JsonObject;

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
        root.add("network", buildNetwork(player, achievements));

        if (bedwarsRaw.exists()) {
            root.add("bedwars", buildBedwars(bedwarsRaw, achievements));
        }

        return new JsonWrapper(root);
    }

    private static JsonObject buildNetwork(JsonWrapper player, JsonWrapper achievements) {
        JsonObject network = new JsonObject();
        double networkExp = num(player, "networkExp");
        String rank = player.get("newPackageRank", player.get("packageRank", ""));
        String monthlyRank = player.get("monthlyPackageRank", "");
        if (monthlyRank != null && !monthlyRank.isEmpty() && !"NONE".equals(monthlyRank)) {
            rank = monthlyRank;
        }

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

    private static JsonObject buildBedwars(JsonWrapper s, JsonWrapper ach) {
        int fk = intVal(s, "final_kills_bedwars");
        int fd = intVal(s, "final_deaths_bedwars");
        int w = intVal(s, "wins_bedwars");
        int l = intVal(s, "losses_bedwars");
        int bb = intVal(s, "beds_broken_bedwars");
        int bl = intVal(s, "beds_lost_bedwars");
        int k = intVal(s, "kills_bedwars");
        int d = intVal(s, "deaths_bedwars");

        JsonObject overall = new JsonObject();
        overall.addProperty("stars", intVal(ach, "bedwars_level"));
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
