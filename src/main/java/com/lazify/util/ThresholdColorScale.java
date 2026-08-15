package com.lazify.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Sorted list of (minValue → RGB) tiers. {@link #colorFor(double)} picks the
 * highest tier whose min is &lt;= value.
 */
public final class ThresholdColorScale {

    public static final class Tier {
        public double min;
        public int r, g, b;

        public Tier(double min, int r, int g, int b) {
            this.min = min;
            this.r = clamp(r);
            this.g = clamp(g);
            this.b = clamp(b);
        }

        public Tier copy() {
            return new Tier(min, r, g, b);
        }

        public int argb() {
            return ColorUtil.rgb(r, g, b);
        }
    }

    private final List<Tier> tiers = new ArrayList<>();

    public ThresholdColorScale() {}

    public ThresholdColorScale(List<Tier> initial) {
        if (initial != null) {
            for (Tier t : initial) tiers.add(t.copy());
        }
    }

    public List<Tier> getTiers() {
        return Collections.unmodifiableList(tiers);
    }

    public int size() {
        return tiers.size();
    }

    public Tier get(int i) {
        return tiers.get(i);
    }

    public void set(int i, Tier t) {
        tiers.set(i, t);
    }

    public void add(Tier t) {
        tiers.add(t);
    }

    public void remove(int i) {
        if (i >= 0 && i < tiers.size() && tiers.size() > 1) {
            tiers.remove(i);
        }
    }

    /** Move a tier in list order (editor reorder). Lookup still sorts by min. */
    public void move(int from, int to) {
        if (from < 0 || from >= tiers.size()) return;
        if (to < 0) to = 0;
        if (to > tiers.size()) to = tiers.size();
        if (from == to || from + 1 == to) return;
        Tier t = tiers.remove(from);
        if (to > from) to--;
        tiers.add(to, t);
    }

    public void clearAndSet(List<Tier> next) {
        tiers.clear();
        if (next != null) {
            for (Tier t : next) tiers.add(t.copy());
        }
        if (tiers.isEmpty()) {
            tiers.add(new Tier(0, 255, 255, 255));
        }
    }

    private List<Tier> sortedCopy() {
        List<Tier> sorted = new ArrayList<>(tiers);
        Collections.sort(sorted, new Comparator<Tier>() {
            @Override
            public int compare(Tier a, Tier b) {
                return Double.compare(a.min, b.min);
            }
        });
        return sorted;
    }

    /** ARGB for value (fully opaque). */
    public int colorFor(double value) {
        List<Tier> sorted = sortedCopy();
        if (sorted.isEmpty()) return 0xFFFFFFFF;
        Tier chosen = sorted.get(0);
        for (int i = 0; i < sorted.size(); i++) {
            if (value + 1e-9 >= sorted.get(i).min) chosen = sorted.get(i);
            else break;
        }
        return chosen.argb();
    }

    /** Serialize in list order: {@code min:r,g,b;min:r,g,b} */
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tiers.size(); i++) {
            Tier t = tiers.get(i);
            if (i > 0) sb.append(';');
            sb.append(formatMin(t.min)).append(':')
                    .append(t.r).append(',').append(t.g).append(',').append(t.b);
        }
        return sb.toString();
    }

    public static ThresholdColorScale parse(String raw, ThresholdColorScale fallback) {
        if (raw == null || raw.trim().isEmpty()) {
            return fallback != null ? fallback.copy() : defaultFkdr();
        }
        List<Tier> list = new ArrayList<>();
        String[] parts = raw.split(";");
        for (String part : parts) {
            String[] kv = part.split(":");
            if (kv.length != 2) continue;
            String[] rgb = kv[1].split(",");
            if (rgb.length != 3) continue;
            try {
                double min = Double.parseDouble(kv[0].trim());
                int r = Integer.parseInt(rgb[0].trim());
                int g = Integer.parseInt(rgb[1].trim());
                int b = Integer.parseInt(rgb[2].trim());
                list.add(new Tier(min, r, g, b));
            } catch (NumberFormatException ignored) { }
        }
        if (list.isEmpty()) {
            return fallback != null ? fallback.copy() : defaultFkdr();
        }
        return new ThresholdColorScale(list);
    }

    public ThresholdColorScale copy() {
        return new ThresholdColorScale(tiers);
    }

    private static String formatMin(double v) {
        if (Math.abs(v - Math.rint(v)) < 1e-6) return String.valueOf((long) Math.rint(v));
        return String.format(Locale.US, "%.2f", v);
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    /** Minecraft § code → approximate RGB. */
    public static int[] mcCodeToRgb(char code) {
        switch (Character.toLowerCase(code)) {
            case '0': return new int[]{0, 0, 0};
            case '1': return new int[]{0, 0, 170};
            case '2': return new int[]{0, 170, 0};
            case '3': return new int[]{0, 170, 170};
            case '4': return new int[]{170, 0, 0};
            case '5': return new int[]{170, 0, 170};
            case '6': return new int[]{255, 170, 0};
            case '7': return new int[]{170, 170, 170};
            case '8': return new int[]{85, 85, 85};
            case '9': return new int[]{85, 85, 255};
            case 'a': return new int[]{85, 255, 85};
            case 'b': return new int[]{85, 255, 255};
            case 'c': return new int[]{255, 85, 85};
            case 'd': return new int[]{255, 85, 255};
            case 'e': return new int[]{255, 255, 85};
            case 'f': return new int[]{255, 255, 255};
            default:  return new int[]{255, 255, 255};
        }
    }

    public static ThresholdColorScale fromLegacyFkdrCodes(String[] codes) {
        double[] mins = {0, 1.4, 2.4, 5, 10, 100, 1000};
        List<Tier> list = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            char c = (codes != null && i < codes.length && codes[i] != null && !codes[i].isEmpty())
                    ? codes[i].charAt(0) : "7fe6c45".charAt(i);
            int[] rgb = mcCodeToRgb(c);
            list.add(new Tier(mins[i], rgb[0], rgb[1], rgb[2]));
        }
        return new ThresholdColorScale(list);
    }

    public static ThresholdColorScale defaultFkdr() {
        return fromLegacyFkdrCodes(new String[]{"7", "f", "e", "6", "c", "4", "5"});
    }

    public static ThresholdColorScale defaultWinstreak() {
        List<Tier> list = new ArrayList<>();
        list.add(new Tier(0, 170, 170, 170));
        list.add(new Tier(25, 85, 255, 85));
        list.add(new Tier(50, 0, 170, 0));
        list.add(new Tier(75, 255, 255, 85));
        list.add(new Tier(100, 255, 170, 0));
        list.add(new Tier(150, 255, 85, 85));
        list.add(new Tier(300, 170, 0, 0));
        list.add(new Tier(500, 255, 85, 255));
        list.add(new Tier(1000, 170, 0, 170));
        return new ThresholdColorScale(list);
    }

    public static ThresholdColorScale defaultPing() {
        // Lower ping = better; tiers are max-latency buckets (colorFor uses >= min)
        List<Tier> list = new ArrayList<>();
        list.add(new Tier(0, 85, 255, 85));      // green
        list.add(new Tier(100, 255, 255, 85));   // yellow
        list.add(new Tier(150, 255, 170, 0));    // gold
        list.add(new Tier(200, 255, 85, 85));    // red
        return new ThresholdColorScale(list);
    }

    public static ThresholdColorScale defaultSessionMinutes() {
        List<Tier> list = new ArrayList<>();
        list.add(new Tier(0, 170, 0, 0));
        list.add(new Tier(2.5, 255, 85, 85));
        list.add(new Tier(5, 255, 255, 85));
        list.add(new Tier(10, 255, 255, 85));
        list.add(new Tier(20, 85, 255, 85));
        list.add(new Tier(120, 255, 255, 85));
        list.add(new Tier(150, 255, 170, 0));
        list.add(new Tier(240, 255, 85, 85));
        list.add(new Tier(360, 170, 0, 0));
        return new ThresholdColorScale(list);
    }

    public static ThresholdColorScale defaultEncounters() {
        List<Tier> list = new ArrayList<>();
        list.add(new Tier(0, 85, 255, 85));
        list.add(new Tier(2, 255, 255, 85));
        list.add(new Tier(4, 255, 170, 0));
        list.add(new Tier(6, 255, 85, 85));
        return new ThresholdColorScale(list);
    }

    /** Lifetime kills / finals / beds / wins. */
    public static ThresholdColorScale defaultCounts() {
        List<Tier> list = new ArrayList<>();
        list.add(new Tier(0, 170, 170, 170));
        list.add(new Tier(500, 255, 255, 255));
        list.add(new Tier(2000, 255, 255, 85));
        list.add(new Tier(5000, 255, 170, 0));
        list.add(new Tier(10000, 255, 85, 85));
        list.add(new Tier(25000, 170, 0, 0));
        list.add(new Tier(50000, 255, 85, 255));
        return new ThresholdColorScale(list);
    }

    /** Daily / weekly / monthly stars gained. */
    public static ThresholdColorScale defaultPeriodStars() {
        List<Tier> list = new ArrayList<>();
        list.add(new Tier(0, 170, 170, 170));
        list.add(new Tier(1, 85, 255, 85));
        list.add(new Tier(5, 255, 255, 85));
        list.add(new Tier(10, 255, 170, 0));
        list.add(new Tier(25, 255, 85, 85));
        list.add(new Tier(50, 255, 85, 255));
        return new ThresholdColorScale(list);
    }
}
