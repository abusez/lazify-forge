package com.lazify.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects Hypixel Bedwars kill-message packages from chat lines.
 * Returns the killer username and package id/display name.
 */
public final class KillMessageDetector {

    public static final class Match {
        public final String packageId;
        public final String displayName;
        public final String killer;
        public final Integer observedFinals;
        public final Integer observedBeds;

        Match(String packageId, String displayName, String killer,
              Integer observedFinals, Integer observedBeds) {
            this.packageId = packageId;
            this.displayName = displayName;
            this.killer = killer;
            this.observedFinals = observedFinals;
            this.observedBeds = observedBeds;
        }
    }

    private static final class Entry {
        final String id;
        final String display;
        final Pattern pattern;

        Entry(String id, String display, String regex) {
            this.id = id;
            this.display = display;
            this.pattern = Pattern.compile(regex);
        }
    }

    private static final Pattern FINALS_IN_MSG = Pattern.compile("'s final #(\\d+)");
    private static final Pattern BEDS_IN_MSG   = Pattern.compile("Bed was bed #(\\d+) destroyed");
    private static final String V = "[\\w+]{1,16}";
    private static final String K = "([\\w+]{1,16})";
    private static final List<Entry> ENTRIES = new ArrayList<>();

    static {
        // 0 — default
        pkg("default", "Default",
            V + " was killed by " + K + "\\.",
            V + " was knocked into the void by " + K + "\\.",
            ".+ Bed was destroyed by " + K + "!");

        // 1 — fire
        pkg("fire", "Fire",
            V + " was struck down by " + K + "\\.",
            V + " was turned to dust by " + K + "\\.",
            V + " was melted by " + K + "\\.",
            V + " was turned to ash by " + K + "\\.",
            V + " was fried by " + K + "'s Golem\\.",
            ".+ Bed was incinerated by " + K + "!");

        // 2 — western
        pkg("western", "Western",
            V + " was filled full of lead by " + K + "\\.",
            V + " met their end by " + K + "\\.",
            V + " was killed with dynamite by " + K + "\\.",
            V + " lost a drinking contest with " + K + "\\.",
            V + " lost the draw to " + K + "'s Golem\\.",
            ".+ Bed was iced by " + K + "!");

        // 3 — honourable
        pkg("honourable", "Honourable",
            V + " died in close combat to " + K + "\\.",
            V + " fought to the edge with " + K + "\\.",
            V + " fell to the great marksmanship of " + K + "\\.",
            V + " stumbled off a ledge with help by " + K + "\\.",
            V + " tangoed with " + K + "'s Golem\\.",
            ".+ Bed had to raise the white flag to " + K + "!");

        // 4 — multiverse
        pkg("multiverse", "Multiverse",
            V + " was distorted by " + K + "\\.",
            V + " was thrown into the singularity by " + K + "\\.",
            V + " was shot into another dimension by " + K + "\\.",
            V + " was thrown into a black hole by " + K + "\\.",
            V + " was launched into a wormhole by " + K + "'s Golem\\.",
            ".+ Bed was sucked into a black hole by " + K + "\\.");

        // 5 — limbo
        pkg("limbo", "Limbo",
            V + " was sent to limbo by " + K + "\\.",
            V + " was pushed into limbo by " + K + "\\.",
            V + " was shot into limbo by " + K + "\\.",
            V + " was launched into limbo by " + K + "'s Golem\\.",
            ".+ Bed was sent to limbo by " + K + "\\.");

        // 6 — love
        pkg("love", "Love",
            V + " was given the cold shoulder by " + K + "\\.",
            V + " was hit off by a love bomb from " + K + "\\.",
            V + " was struck with Cupid's arrow by " + K + "\\.",
            V + " was out of the league of " + K + "\\.",
            V + " was no match for " + K + "'s Golem\\.",
            ".+ Bed was dismantled by " + K + "!");

        // 7 — bbq
        pkg("bbq", "BBQ",
            V + " was glazed in BBQ sauce by " + K + "\\.",
            V + " slipped in BBQ sauce off the edge spilled by " + K + "\\.",
            V + " was thrown chili powder at by " + K + "\\.",
            V + " was not spicy enough for " + K + "\\.",
            V + " was sliced up by " + K + "'s Golem\\.",
            ".+ Bed was deep fried by " + K + "!");

        // 8 — woof woof
        pkg("woof_woof", "Woof Woof",
            V + " was bitten by " + K + "\\.",
            V + " howled into the void for " + K + "\\.",
            V + " caught the ball thrown by " + K + "\\.",
            V + " was distracted by a puppy placed by " + K + "\\.",
            V + " played too rough with " + K + "'s Golem\\.",
            ".+ Bed was ripped apart by " + K + "!");

        // 9 — santa's workshop
        pkg("santas_workshop", "Santa's Workshop",
            V + " was wrapped into a gift by " + K + "\\.",
            V + " hit the hard-wood floor because of " + K + "\\.",
            V + " was put on the naughty list by " + K + "\\.",
            V + " was pushed down a slope by " + K + "\\.",
            V + " was turned to gingerbread by " + K + "'s Golem\\.",
            ".+ Bed was traded in for milk and cookies by " + K + "!");

        // 10 — primal
        pkg("primal", "Primal",
            V + " was hunted down by " + K + "\\.",
            V + " stumbled on a trap set by " + K + "\\.",
            V + " got skewered by " + K + "\\.",
            V + " was thrown into a volcano by " + K + "\\.",
            V + " was mauled by " + K + "'s Golem\\.",
            ".+ Bed was sacrificed by " + K + "!");

        // 11 — oink
        pkg("oink", "Oink",
            V + " was oinked by " + K + "\\.",
            V + " slipped into void for " + K + "\\.",
            V + " got attacked by a carrot from " + K + "\\.",
            V + " was distracted by a piglet from " + K + "\\.",
            V + " was oinked by " + K + "'s Golem\\.",
            ".+ Bed was gulped by " + K + "!");

        // 12 — squeak
        pkg("squeak", "Squeak",
            V + " was chewed up by " + K + "\\.",
            V + " was scared into the void by " + K + "\\.",
            V + " stepped in a mouse trap placed by " + K + "\\.",
            V + " was distracted by a rat dragging pizza from " + K + "\\.",
            V + " squeaked around with " + K + "'s Golem\\.",
            ".+ Bed was squeaked apart by " + K + "!");

        // 13 — buzz
        pkg("buzz", "Buzz",
            V + " was buzzed to death by " + K + "\\.",
            V + " was bzzz'd into the void by " + K + "\\.",
            V + " was startled by " + K + "\\.",
            V + " was stung off the edge by " + K + "\\.",
            V + " was bee'd by " + K + "'s Golem\\.",
            ".+ Bed was stung by " + K + "!");

        // 14 — ox'd
        pkg("oxd", "Ox'd",
            V + " was trampled by " + K + "\\.",
            V + " was back kicked into the void by " + K + "\\.",
            V + " was impaled from a distance by " + K + "\\.",
            V + " was headbutted off a cliff by " + K + "\\.",
            V + " was trampled by " + K + "'s Golem\\.",
            ".+ Bed was impaled by " + K + "!");

        // 15 — pirate
        pkg("pirate", "Pirate",
            V + " be sent to Davy Jones' locker by " + K + "\\.",
            V + " be cannonballed to death by " + K + "\\.",
            V + " be shot and killed by " + K + "\\.",
            V + " be killed with magic by " + K + "\\.",
            V + " be killed with metal by " + K + "'s Golem\\.",
            ".+ Bed be shot with cannon by " + K + "!");

        // 16 — literally spooky
        pkg("literally_spooky", "Literally Spooky",
            V + " was spooked by " + K + "\\.",
            V + " was spooked off the map by " + K + "\\.",
            V + " was remotely spooked by " + K + "\\.",
            V + " was totally spooked by " + K + "\\.",
            V + " was spooked by " + K + "'s Golem\\.",
            ".+ Bed was spooked by " + K + "!");

        // 17 — memed
        pkg("memed", "Memed",
            V + " got rekt by " + K + "\\.",
            V + " took the L to " + K + "\\.",
            V + " got smacked by " + K + "\\.",
            V + " got roasted by " + K + "\\.",
            V + " got bamboozled by " + K + "'s Golem\\.",
            ".+ Bed got memed by " + K + "!");

        // 18 — dramatic
        pkg("dramatic", "Dramatic",
            V + " was tragically backstabbed by " + K + "\\.",
            V + " was heartlessly let go by " + K + "\\.",
            V + "'s heart was pierced by " + K + "\\.",
            V + " was delivered into nothingness by " + K + "\\.",
            V + " was dismembered by " + K + "'s Golem\\.",
            ".+ Bed was dreadfully corrupted by " + K + "!");

        // 19 — noble (unique lines only; final # / bed # shared with triumph/glorious)
        pkg("noble", "Noble",
            V + " was crushed by " + K + "\\.",
            V + " was dominated by " + K + "\\.",
            V + " was assassinated by " + K + "\\.",
            V + " was thrown off their high horse by " + K + "\\.",
            V + " was degraded by " + K + "'s Golem\\.");

        // 20 — snow storm
        pkg("snow_storm", "Snow Storm",
            V + " was locked outside during a snow storm by " + K + "\\.",
            V + " was pushed into a snowbank by " + K + "\\.",
            V + " was hit with a snowball from " + K + "\\.",
            V + " was shoved down an icy slope by " + K + "\\.",
            V + " got snowed in by " + K + "'s Golem\\.",
            ".+ Bed was made into a snowman by " + K + "!");

        // 21 — eggy
        pkg("eggy", "Eggy",
            V + " was painted pretty by " + K + "\\.",
            V + " was deviled into the void by " + K + "\\.",
            V + " slipped into a pan placed by " + K + "\\.",
            V + " was flipped off the edge by " + K + "\\.",
            V + " was made sunny side up by " + K + "'s Golem\\.",
            ".+ Bed was scrambled by " + K + "!");

        // 22 — celebratory
        pkg("celebratory", "Celebratory",
            V + " was whacked with a party balloon by " + K + "\\.",
            V + " was popped into the void by " + K + "\\.",
            V + " was shot with a roman candle by " + K + "\\.",
            V + " was launched like a firework by " + K + "\\.",
            V + " was lit up by " + K + "'s Golem\\.",
            ".+ Bed exploded from a firework by " + K + "!");

        // 23 — wrapped up
        pkg("wrapped_up", "Wrapped Up",
            V + " was wrapped up by " + K + "\\.",
            V + " was tied into a bow by " + K + "\\.",
            V + " was glued up by " + K + "\\.",
            V + " tripped over a present placed by " + K + "\\.",
            V + " was taped together by " + K + "'s Golem\\.",
            ".+ Bed was stuffed with tissue paper by " + K + "!");

        // 24 — to the moon
        pkg("to_the_moon", "To The Moon",
            V + " was crushed into moon dust by " + K + "\\.",
            V + " was sent the wrong way by " + K + "\\.",
            V + " was hit by an asteroid from " + K + "\\.",
            V + " was blasted to the moon by " + K + "\\.",
            V + " was blown up by " + K + "'s Golem\\.",
            ".+ Bed was blasted to dust by " + K + "!");

        // 25 — festive
        pkg("festive", "Festive",
            V + " was smothered in holiday cheer by " + K + "\\.",
            V + " was banished into the ether by " + K + "'s holiday spirit\\.",
            V + " was sniped by a missile of festivity by " + K + "\\.",
            V + " was pushed by " + K + "'s holiday spirit\\.",
            V + " was sung holiday tunes to by " + K + "'s Golem\\.",
            ".+ Bed was melted by " + K + "'s holiday spirit!");

        // 26 — roar
        pkg("roar", "Roar",
            V + " was ripped to shreds by " + K + "\\.",
            V + " was charged by " + K + "\\.",
            V + " was pounced on by " + K + "\\.",
            V + " was ripped and thrown by " + K + "\\.",
            V + " was ripped to shreds by " + K + "'s Golem\\.",
            ".+ Bed was ripped to shreds by " + K + "\\.");

        // 27 — triumph
        pkg("triumph", "Triumph",
            V + " was bested by " + K + "\\.",
            V + " was knocked into the void by " + K + "\\.",
            V + " was shot by " + K + "\\.",
            V + " was knocked off an edge by " + K + "\\.",
            V + " was bested by " + K + "'s Golem\\.");

        // Ambiguous stat-tracking counters (noble/triumph/glorious share these lines)
        pkg("stat", "Stat",
            V + " was " + K + "'s final #\\d+\\.",
            ".+ Bed was bed #\\d+ destroyed by " + K + "!");

        // 28 — bridging for dummies
        pkg("bridging_for_dummies", "Bridging For Dummies",
            V + " had a small brain moment while fighting " + K + "\\.",
            V + " was not able to block clutch against " + K + "\\.",
            V + " got 360 no-scoped by " + K + "\\.",
            V + " forgot how many blocks they had left while fighting " + K + "\\.",
            V + " got absolutely destroyed by " + K + "'s Golem\\.",
            ".+ Bed has left the game after seeing " + K + "!");

        // 29 — social distance
        pkg("social_distance", "Social Distance",
            V + " was too shy to meet " + K + "\\.",
            V + " didn't distance themselves properly from " + K + "\\.",
            V + " was coughed at by " + K + "\\.",
            V + " tripped while trying to run away from " + K + "\\.",
            V + " got too close to " + K + "'s Golem\\.",
            ".+ Bed was contaminated by " + K + "!");

        // 30 — old man
        pkg("old_man", "Old Man",
            V + " was yelled at by " + K + "\\.",
            V + " was thrown off the lawn by " + K + "\\.",
            V + " was accidentally spit on by " + K + "\\.",
            V + " slipped on the fake teeth of " + K + "\\.",
            V + " was chased away by " + K + "'s Golem\\.",
            ".+ Bed was sold in a garage sale by " + K + "!");

        // 31 — glorious
        pkg("glorious", "Glorious",
            V + " was stomped by " + K + "\\.",
            V + " was thrown down a pit by " + K + "\\.",
            V + " was thrown to the ground by " + K + "\\.",
            V + " was outclassed by " + K + "'s Golem\\.");

        // 32 — lucid
        pkg("lucid", "Lucid",
            V + " was put to bed by " + K + "\\.",
            V + " fell asleep and was then knocked into the void by " + K + "\\.",
            V + " was knocked unconscious by " + K + "'s arrow\\.",
            V + " wishes they hit the hay instead of the ground after being pushed by " + K + "\\.",
            V + " fell asleep to " + K + "'s Golem\\.",
            ".+ Bed was destroyed by a half-awake " + K + "\\.");
    }

    private KillMessageDetector() {}

    private static void pkg(String id, String display, String... regexes) {
        for (String regex : regexes) {
            ENTRIES.add(new Entry(id, display, regex));
        }
    }

    public static Match detect(String plainMessage) {
        if (plainMessage == null || plainMessage.isEmpty()) return null;
        Integer observedFinals = extractFinals(plainMessage);
        Integer observedBeds = extractBeds(plainMessage);
        for (Entry entry : ENTRIES) {
            Matcher matcher = entry.pattern.matcher(plainMessage);
            if (matcher.matches()) {
                return new Match(entry.id, entry.display, matcher.group(1),
                        observedFinals, observedBeds);
            }
        }
        return null;
    }

    private static Integer extractFinals(String msg) {
        Matcher m = FINALS_IN_MSG.matcher(msg);
        if (m.find()) {
            try { return Integer.parseInt(m.group(1)); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private static Integer extractBeds(String msg) {
        Matcher m = BEDS_IN_MSG.matcher(msg);
        if (m.find()) {
            try { return Integer.parseInt(m.group(1)); } catch (NumberFormatException ignored) {}
        }
        return null;
    }
}
