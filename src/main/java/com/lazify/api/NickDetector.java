package com.lazify.api;

/**
 * Multi-provider nick confirmation: Abyss + Prism + optional Bordic + optional Hypixel.
 * All active signals must agree before a player is marked nicked.
 */
public final class NickDetector {

    private static final String BORDIC_CACHE_URL =
            "https://bordic.xyz/api/v2/resources/cache/hypixel?uuid=";
    private static final String BORDIC_CACHE_KEY_URL =
            "https://bordic.xyz/api/v2/resources/cache/hypixel?key=";

    private NickDetector() {}

    /** Abyss / Hypixel v2: HTTP 200, success true, player null. Offline UUID + invalid/malformed also counts. */
    public static boolean isNullPlayerNick(JsonWrapper body, int code) {
        return isNullPlayerNick(body, code, null);
    }

    public static boolean isNullPlayerNick(JsonWrapper body, int code, String dashlessUuid) {
        if (code == 200 && body.exists() && body.get("success").asBoolean(false)) {
            return !body.get("player").exists();
        }
        if (dashlessUuid != null && isOfflineUuid(dashlessUuid) && body.exists()) {
            String err = (body.get("error", "") + " " + body.get("cause", "")).toLowerCase();
            return err.contains("invalid") || err.contains("malformed");
        }
        return false;
    }

    /**
     * Prism: success true + null player on 200, or well-known / not-stored error (often HTTP 500).
     */
    public static boolean isPrismNick(JsonWrapper body, int code) {
        if (!body.exists()) return false;
        if (body.get("success").asBoolean(false)) {
            return code == 200 && !body.get("player").exists();
        }
        String cause = body.get("cause", "").toLowerCase();
        return cause.contains("well-known") || cause.contains("player not stored");
    }

    /** Bordic hypixel cache: success false with no-data cause, or malformed offline UUID. */
    public static boolean isBordicNick(JsonWrapper body) {
        return isBordicNick(body, null);
    }

    public static boolean isBordicNick(JsonWrapper body, String dashlessUuid) {
        if (!body.exists()) return false;
        if (body.get("success").asBoolean(true)) return false;
        String cause = body.get("cause", "");
        if (cause.contains("No data available for that request")) return true;
        if (dashlessUuid != null && isOfflineUuid(dashlessUuid)) {
            String msg = (cause + " " + body.get("error", "")).toLowerCase();
            return msg.contains("invalid") || msg.contains("malformed");
        }
        return false;
    }

    public static boolean probeBordicNick(String dashlessUuid, String bordicKey) {
        if (bordicKey == null || bordicKey.isEmpty()) return true;
        Object[] res = HttpUtil.get(BORDIC_CACHE_KEY_URL + bordicKey + "&uuid=" + dashlessUuid, 10000);
        return isBordicNick((JsonWrapper) res[0], dashlessUuid);
    }

    /** Bordic Hypixel cache (uuid-only; optional key when configured). */
    public static Object[] fetchBordicCache(String dashlessUuid, String bordicKey) {
        String url = (bordicKey != null && !bordicKey.isEmpty())
                ? BORDIC_CACHE_KEY_URL + bordicKey + "&uuid=" + dashlessUuid
                : BORDIC_CACHE_URL + dashlessUuid;
        return HttpUtil.get(url, 10000);
    }

    /** Hypixel offline-mode UUID assigned to nicked players (version nibble != 4). */
    public static boolean isOfflineUuid(String dashlessUuid) {
        return dashlessUuid != null && dashlessUuid.length() == 32 && dashlessUuid.charAt(12) != '4';
    }

    public static boolean isConfirmedNick(
            boolean abyssNick,
            boolean prismNick,
            boolean bordicNick,
            boolean hypixelNick,
            String bordicKey,
            String hypixelKey) {
        if (!abyssNick || !prismNick) return false;
        if (bordicKey != null && !bordicKey.isEmpty() && !bordicNick) return false;
        if (hypixelKey != null && !hypixelKey.isEmpty() && !hypixelNick) return false;
        return true;
    }
}
