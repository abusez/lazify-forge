package com.lazify.util;

/**
 * Bedwars star level from {@code stats.Bedwars.Experience}.
 * Port of Plancke's ExpCalculator (used when {@code achievements.bedwars_level} is missing).
 */
public final class BedwarsExpCalculator {

    private static final int EASY_LEVELS = 4;
    private static final int EASY_LEVELS_XP = 7000;
    private static final int XP_PER_PRESTIGE = 96 * 5000 + EASY_LEVELS_XP;
    private static final int LEVELS_PER_PRESTIGE = 100;
    private static final int HIGHEST_PRESTIGE = 10;

    private BedwarsExpCalculator() {}

    public static int levelFromExp(int exp) {
        if (exp <= 0) return 0;

        int prestiges = exp / XP_PER_PRESTIGE;
        int level = prestiges * LEVELS_PER_PRESTIGE;
        int expWithoutPrestiges = exp - (prestiges * XP_PER_PRESTIGE);

        for (int i = 1; i <= EASY_LEVELS; i++) {
            int expForEasyLevel = expForLevel(i);
            if (expWithoutPrestiges < expForEasyLevel) break;
            level++;
            expWithoutPrestiges -= expForEasyLevel;
        }
        return level + (expWithoutPrestiges / 5000);
    }

    private static int expForLevel(int level) {
        if (level == 0) return 0;
        int respectedLevel = levelRespectingPrestige(level);
        if (respectedLevel > EASY_LEVELS) return 5000;
        switch (respectedLevel) {
            case 1: return 500;
            case 2: return 1000;
            case 3: return 2000;
            case 4: return 3500;
            default: return 5000;
        }
    }

    private static int levelRespectingPrestige(int level) {
        if (level > HIGHEST_PRESTIGE * LEVELS_PER_PRESTIGE) {
            return level - HIGHEST_PRESTIGE * LEVELS_PER_PRESTIGE;
        }
        return level % LEVELS_PER_PRESTIGE;
    }
}
