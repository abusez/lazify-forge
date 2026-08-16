package com.lazify.util;

import com.lazify.api.HttpUtil;
import com.lazify.api.JsonWrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Fetches and searches Bordic's MVP++ superstar list for nick denicking.
 */
public final class BordicSuperstar {

    private static final String SUPERSTAR_URL = "https://bordic.xyz/api/v2/resources/superstar";
    private static final long CACHE_MS = 15 * 60 * 1000L;

    public static final class Entry {
        public final String uuid;
        public final String name;
        public final int star;
        public final int finals;
        public final int beds;
        public final String killMessage;
        public final String activeWoodType;

        Entry(String uuid, String name, int star, int finals, int beds,
              String killMessage, String activeWoodType) {
            this.uuid = uuid;
            this.name = name;
            this.star = star;
            this.finals = finals;
            this.beds = beds;
            this.killMessage = killMessage;
            this.activeWoodType = activeWoodType;
        }
    }

    public static final class MatchResult {
        public final Entry entry;
        public final long score;
        /** Candidates matching filters with finals+beds both within {@link #NEARBY_STAT_RADIUS}. */
        public final int nearbyCount;

        MatchResult(Entry entry, long score, int nearbyCount) {
            this.entry = entry;
            this.score = score;
            this.nearbyCount = nearbyCount;
        }

        /** Too many people near the observed finals/beds — chat denick would be unreliable. */
        public boolean isAmbiguous() {
            return nearbyCount >= AMBIGUOUS_NEARBY_MIN;
        }
    }

    /** Max |Δfinals| and |Δbeds| to count as "nearby" for ambiguity. */
    public static final int NEARBY_STAT_RADIUS = 200;
    /** If this many nearby candidates, suppress chat denick. */
    public static final int AMBIGUOUS_NEARBY_MIN = 7;

    private static List<Entry> cache = Collections.emptyList();
    private static long cacheTime = 0L;

    private BordicSuperstar() {}

    public static synchronized void clearCache() {
        cache = Collections.emptyList();
        cacheTime = 0L;
    }

    public static synchronized List<Entry> fetch(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) return Collections.emptyList();
        long now = System.currentTimeMillis();
        if (!cache.isEmpty() && now - cacheTime < CACHE_MS) return cache;

        Object[] res = HttpUtil.get(SUPERSTAR_URL + "?key=" + apiKey, 30000);
        if ((int) res[1] != 200) return cache.isEmpty() ? Collections.<Entry>emptyList() : cache;

        JsonWrapper root = (JsonWrapper) res[0];
        if (!root.get("success").asBoolean(false)) return cache.isEmpty() ? Collections.<Entry>emptyList() : cache;

        List<Entry> parsed = new ArrayList<>();
        for (JsonWrapper item : root.array("data")) {
            JsonWrapper obj = item.object();
            if (!obj.exists()) continue;
            String uuid = obj.get("uuid", "");
            String name = obj.get("name", "");
            if (uuid.isEmpty() || name.isEmpty()) continue;
            parsed.add(new Entry(
                uuid,
                name,
                obj.get("star").asInt(0),
                obj.get("finals").asInt(0),
                obj.get("beds").asInt(0),
                obj.get("killMessage", ""),
                obj.get("activeWoodType", "")
            ));
        }

        cache = parsed;
        cacheTime = now;
        return cache;
    }

    /** Maps internal package id to Bordic killMessage field value(s). */
    public static List<String> bordicKillMessagesForPackage(String packageId) {
        if (packageId == null || packageId.isEmpty()) return Collections.emptyList();
        switch (packageId) {
            case "stat":
                return Arrays.asList(
                    "killmessages_counter",
                    "killmessages_noble",
                    "killmessages_glorious");
            case "oxd":
                return Collections.singletonList("killmessages_oxed");
            case "literally_spooky":
                return Collections.singletonList("killmessages_spooky");
            case "santas_workshop":
                return Collections.singletonList("killmessages_santa_workshop");
            case "social_distance":
                return Collections.singletonList("killmessages_social_distancing");
            case "triumph":
                // Not exposed separately on Bordic; counter-style packages share stats lines.
                return Collections.singletonList("killmessages_counter");
            default:
                return Collections.singletonList("killmessages_" + packageId);
        }
    }

    public static MatchResult findBestMatch(List<Entry> list, String packageId, int finals, int beds,
                                            String woodType) {
        return findBestMatch(list, packageId, finals, beds, woodType, null);
    }

    public static MatchResult findBestMatch(List<Entry> list, String packageId, int finals, int beds,
                                            String woodType, Integer star) {
        if (list == null || list.isEmpty()) return null;
        List<String> killMsgs = bordicKillMessagesForPackage(packageId);
        if (killMsgs.isEmpty()) return null;

        Entry best = null;
        long bestScore = Long.MAX_VALUE;
        int nearbyCount = 0;
        for (Entry e : list) {
            if (e.killMessage == null || e.killMessage.isEmpty()) continue;
            if (!killMsgs.contains(e.killMessage)) continue;
            if (!woodTypeMatches(woodType, e.activeWoodType)) continue;
            if (star != null && star > 0 && e.star != star) continue;

            long dFinals = Math.abs((long) e.finals - finals);
            long dBeds = Math.abs((long) e.beds - beds);
            if (dFinals <= NEARBY_STAT_RADIUS && dBeds <= NEARBY_STAT_RADIUS) {
                nearbyCount++;
            }
            long score = dFinals + dBeds;
            if (score < bestScore) {
                bestScore = score;
                best = e;
            }
        }
        return best != null ? new MatchResult(best, bestScore, nearbyCount) : null;
    }

    /** Skip only when both sides have a concrete wood id and they disagree. */
    private static boolean woodTypeMatches(String observed, String entryWood) {
        if (observed == null || observed.isEmpty()) return true;
        if (entryWood == null || entryWood.isEmpty()) return true;
        if (entryWood.startsWith("random_")) return true;
        return observed.equals(entryWood);
    }
}
