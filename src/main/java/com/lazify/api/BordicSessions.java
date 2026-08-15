package com.lazify.api;

import com.lazify.util.BedwarsExpCalculator;
import com.lazify.util.ColorUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Bordic v4 Bedwars session deltas (daily / weekly / monthly) via bulk POST.
 * Auth: {@code ?key=} query param. Body: {@code {"uuids":["..."]}} (max 10).
 */
public final class BordicSessions {

    public enum Period {
        DAILY("daily"),
        WEEKLY("weekly"),
        MONTHLY("monthly");

        public final String path;
        Period(String path) { this.path = path; }
    }

    /** Parsed period deltas for one player. {@code ok=false} means no usable history. */
    public static final class Snapshot {
        public final boolean ok;
        public final String cause;
        public final double fkdr;
        public final double wlr;
        public final double bblr;
        public final double kdr;
        public final int starsGained;
        public final int finalKills;
        public final int finalDeaths;
        public final int wins;
        public final int losses;
        public final int bedsBroken;
        public final int bedsLost;
        public final int kills;
        public final int deaths;
        /** Lifetime Bedwars stats blob from {@code current.value}, when present. */
        public final JsonWrapper currentBedwars;

        private Snapshot(boolean ok, String cause,
                         double fkdr, double wlr, double bblr, double kdr, int starsGained,
                         int finalKills, int finalDeaths, int wins, int losses,
                         int bedsBroken, int bedsLost, int kills, int deaths,
                         JsonWrapper currentBedwars) {
            this.ok = ok;
            this.cause = cause;
            this.fkdr = fkdr;
            this.wlr = wlr;
            this.bblr = bblr;
            this.kdr = kdr;
            this.starsGained = starsGained;
            this.finalKills = finalKills;
            this.finalDeaths = finalDeaths;
            this.wins = wins;
            this.losses = losses;
            this.bedsBroken = bedsBroken;
            this.bedsLost = bedsLost;
            this.kills = kills;
            this.deaths = deaths;
            this.currentBedwars = currentBedwars != null ? currentBedwars : new JsonWrapper(null);
        }

        public static Snapshot missing(String cause) {
            return new Snapshot(false, cause == null ? "" : cause,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, new JsonWrapper(null));
        }

        public boolean hasLifetime() {
            return currentBedwars != null && currentBedwars.exists();
        }
    }

    private static final String BASE = "https://api.bordic.xyz/v4/sessions/";
    private static final int MAX_BULK = 10;

    private BordicSessions() {}

    public static Map<String, Snapshot> fetchBulk(Period period, String apiKey, List<String> uuids, int timeoutMs) {
        if (period == null || apiKey == null || apiKey.isEmpty() || uuids == null || uuids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<String> cleaned = new ArrayList<>();
        for (String u : uuids) {
            if (u == null) continue;
            String id = u.replace("-", "").toLowerCase(Locale.ROOT);
            if (id.length() == 32 && !cleaned.contains(id)) cleaned.add(id);
            if (cleaned.size() >= MAX_BULK) break;
        }
        if (cleaned.isEmpty()) return Collections.emptyMap();

        StringBuilder json = new StringBuilder(64 + cleaned.size() * 40);
        json.append("{\"uuids\":[");
        for (int i = 0; i < cleaned.size(); i++) {
            if (i > 0) json.append(',');
            json.append('"').append(cleaned.get(i)).append('"');
        }
        json.append("]}");

        String url = BASE + period.path + "?key=" + apiKey;
        Object[] res = HttpUtil.post(url, timeoutMs, null, json.toString());
        JsonWrapper root = (JsonWrapper) res[0];
        int code = (int) res[1];
        if (code != 200 || !root.exists() || !root.get("success").asBoolean(false)) {
            return Collections.emptyMap();
        }

        Map<String, Snapshot> out = new LinkedHashMap<>();
        for (JsonWrapper session : root.array("sessions")) {
            String uuid = session.get("uuid", "").replace("-", "").toLowerCase(Locale.ROOT);
            if (uuid.isEmpty()) continue;
            out.put(uuid, parseSession(session));
        }
        return out;
    }

    public static Snapshot parseSession(JsonWrapper session) {
        if (session == null || !session.exists()) return Snapshot.missing("empty");
        if (!session.get("success").asBoolean(false)) {
            return Snapshot.missing(session.get("cause", "failed"));
        }
        JsonWrapper delta = session.object("delta");
        int finals = deltaInt(delta, "final_kills_bedwars");
        int finalDeaths = deltaInt(delta, "final_deaths_bedwars");
        int wins = deltaInt(delta, "wins_bedwars");
        int losses = deltaInt(delta, "losses_bedwars");
        int bedsBroken = deltaInt(delta, "beds_broken_bedwars");
        int bedsLost = deltaInt(delta, "beds_lost_bedwars");
        int kills = deltaInt(delta, "kills_bedwars");
        int deaths = deltaInt(delta, "deaths_bedwars");

        double fkdr = ratio(finals, finalDeaths);
        double wlr = ratio(wins, losses);
        double bblr = ratio(bedsBroken, bedsLost);
        double kdr = ratio(kills, deaths);

        int stars = 0;
        JsonWrapper historical = session.object("historical").object("value");
        JsonWrapper current = session.object("current").object("value");
        if (historical.exists() && current.exists()) {
            int expFrom = expValue(historical);
            int expTo = expValue(current);
            stars = BedwarsExpCalculator.levelFromExp(expTo) - BedwarsExpCalculator.levelFromExp(expFrom);
        } else {
            // Fallback: Experience delta alone is not star count; leave 0 if snapshots missing
            stars = 0;
        }

        return new Snapshot(true, "", fkdr, wlr, bblr, kdr, stars,
                finals, finalDeaths, wins, losses, bedsBroken, bedsLost, kills, deaths,
                current.exists() ? current : new JsonWrapper(null));
    }

    private static int deltaInt(JsonWrapper delta, String key) {
        if (delta == null || !delta.exists()) return 0;
        JsonWrapper v = delta.get(key);
        if (!v.exists()) return 0;
        // Integers are raw deltas; string/array fields are {old,new} objects — ignore those
        if (v.getRaw() != null && v.getRaw().isJsonObject()) return 0;
        return v.asInt(0);
    }

    private static int expValue(JsonWrapper bedwarsStats) {
        if (!bedwarsStats.exists()) return 0;
        JsonWrapper exp = bedwarsStats.get("Experience");
        if (!exp.exists()) exp = bedwarsStats.get("experience");
        return exp.asInt(0);
    }

    private static double ratio(int num, int den) {
        if (den > 0) return (double) num / (double) den;
        if (num > 0) return num;
        return 0.0;
    }

    /** Format a gained-star cell (+N / 0 / -). Color applied at draw time. */
    public static String formatStarsCell(Snapshot snap) {
        if (snap == null || !snap.ok) return "-";
        int n = snap.starsGained;
        if (n > 0) return "+" + n;
        return String.valueOf(n);
    }

    public static String formatRatioCell(double ratio, int decimals) {
        return ColorUtil.formatRatio(ColorUtil.round(ratio, decimals), decimals);
    }
}
