package com.lazify.api;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Player stats waterfall: Abyss → Prism → Hypixel API v2.
 * When no provider returns stats, multi-API nick confirmation runs (Abyss + Prism + optional Bordic/Hypixel).
 */
public final class StatsProvider {

    private static final String ABYSS_URL = "http://api.abyssoverlay.com/player?uuid=";
    private static final String PRISM_URL = "https://flashlight.prismoverlay.com/v1/playerdata?uuid=";
    private static final String HYPIXEL_URL = "https://api.hypixel.net/v2/player?uuid=";
    private static final int TIMEOUT_MS = 10000;

    private static final String PRISM_USER_ID = UUID.randomUUID().toString().replace("-", "");

    public static final class Result {
        public final JsonWrapper data;
        public final int code;
        public final String provider;
        public final boolean nicked;
        public final String nickDebug;

        Result(JsonWrapper data, int code, String provider, boolean nicked, String nickDebug) {
            this.data = data;
            this.code = code;
            this.provider = provider;
            this.nicked = nicked;
            this.nickDebug = nickDebug;
        }
    }

    private StatsProvider() {}

    public static Result fetch(String uuid, String hypixelKey, String bordicKey) {
        String dashless = uuid == null ? "" : uuid.replace("-", "");
        if (dashless.length() != 32) {
            return new Result(null, 400, null, false, null);
        }

        Object[] abyssRes = HttpUtil.get(ABYSS_URL + dashless, TIMEOUT_MS, abyssHeaders());
        JsonWrapper abyssBody = (JsonWrapper) abyssRes[0];
        int abyssCode = (int) abyssRes[1];
        JsonWrapper abyssStats = tryParseStats(abyssBody, abyssCode, "abyss");
        if (abyssStats != null) {
            return new Result(abyssStats, 200, "abyss", false, null);
        }

        Object[] prismRes = HttpUtil.get(PRISM_URL + dashless, TIMEOUT_MS, prismHeaders());
        JsonWrapper prismBody = (JsonWrapper) prismRes[0];
        int prismCode = (int) prismRes[1];
        JsonWrapper prismStats = tryParseStats(prismBody, prismCode, "prism");
        if (prismStats != null) {
            return new Result(prismStats, 200, "prism", false, null);
        }

        JsonWrapper hypixelBody = null;
        int hypixelCode = 0;
        boolean hypixelNick = true;
        boolean hypixelChecked = hypixelKey != null && !hypixelKey.isEmpty();
        if (hypixelChecked) {
            Object[] hypixelRes = HttpUtil.get(HYPIXEL_URL + dashless + "&key=" + hypixelKey, TIMEOUT_MS);
            hypixelBody = (JsonWrapper) hypixelRes[0];
            hypixelCode = (int) hypixelRes[1];
            JsonWrapper hypixelStats = tryParseStats(hypixelBody, hypixelCode, "hypixel");
            if (hypixelStats != null) {
                return new Result(hypixelStats, 200, "hypixel", false, null);
            }
            hypixelNick = NickDetector.isNullPlayerNick(hypixelBody, hypixelCode, dashless);
        }

        boolean abyssNick = NickDetector.isNullPlayerNick(abyssBody, abyssCode, dashless);
        boolean prismNick = NickDetector.isPrismNick(prismBody, prismCode);
        boolean bordicChecked = bordicKey != null && !bordicKey.isEmpty();
        boolean bordicNick = NickDetector.probeBordicNick(dashless, bordicKey);

        String nickDebug = "abyss=" + yn(abyssNick)
                + " prism=" + yn(prismNick)
                + " bordic=" + (bordicChecked ? yn(bordicNick) : "skip")
                + " hypixel=" + (hypixelChecked ? yn(hypixelNick) : "skip");

        if (NickDetector.isOfflineUuid(dashless)) {
            return new Result(null, 200, null, true, nickDebug + " offline=Y");
        }

        if (NickDetector.isConfirmedNick(abyssNick, prismNick, bordicNick, hypixelNick, bordicKey, hypixelKey)) {
            return new Result(null, 200, null, true, nickDebug);
        }

        return new Result(null, 502, null, false, nickDebug);
    }

    private static Map<String, String> abyssHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "node-ao/2.0.3");
        return headers;
    }

    private static Map<String, String> prismHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-User-Id", PRISM_USER_ID);
        headers.put("X-Prism-Version", "v1.11.0");
        return headers;
    }

    private static JsonWrapper tryParseStats(JsonWrapper body, int code, String provider) {
        if (code != 200 || !body.exists()) return null;
        if (!body.get("success").asBoolean(false)) return null;
        if (!body.object("player").exists()) return null;
        return PlayerStatsParser.parse(body, provider);
    }

    private static String yn(boolean value) {
        return value ? "Y" : "N";
    }
}
