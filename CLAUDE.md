# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Command

Gradle wrapper JAR is missing; invoke Gradle directly using Java 8:

```bash
JAVA_HOME="/c/Program Files/Java/jdk1.8.0_291"
GRADLE_HOME="/c/Users/sheepie/.gradle/wrapper/dists/gradle-4.4.1-bin/46gopw3g8i1v3zqqx4q949t2x/gradle-4.4.1"
cd "C:\Users\sheepie\Documents\.coding\java\overlay\forge" && "$JAVA_HOME/bin/java" -cp "$GRADLE_HOME/lib/gradle-launcher-4.4.1.jar" org.gradle.launcher.GradleMain build 2>&1 | tail -15
```

Must use Java 8 (`jdk1.8.0_291`) — Gradle 4.4.1 does not support Java 17. Output JAR: `build/retromapping/retromappedReplacedMain.jar`.

**Always run a build after making code changes.**

There are no tests.

## Project Overview

Minecraft Forge 1.8.9 client-side mod (mod ID: `lazify`). Displays a Bedwars player stats overlay using the Prism (Flashlight) and Urchin APIs. No API key needed for stats; Urchin key optional for cheater tags. Activated via keybind (default: backtick) or `/ov` command (aliases: `/overlay`, `/lazify`).

## Architecture

### Entry Points
- **`LazifyMod.java`** — `@Mod` class; initializes `OverlayManager` singleton, loads config, registers event handler and command.
- **`EventHandler.java`** — All Forge `@SubscribeEvent` handlers:
  - `ClientTickEvent`: direct LWJGL keybind polling (no Forge KeyBinding), `OverlayManager.onTick()` (every 5 ticks), world-change detection via `EntityJoinWorldEvent`
  - `RenderGameOverlayEvent.Post`: renders overlay at EXP_BAR layer
  - `ClientChatReceivedEvent`: forwards to `OverlayManager.onChat()`

### Core: OverlayManager
The bulk of the logic lives here. Key state:

- `currentPlayers` (synchronized List) — ordered UUIDs for rendering; controls row order
- `overlayPlayers` (ConcurrentHashMap UUID→data) — per-player stat maps
- `status` — Bedwars game state: `-1` none, `0` end, `1` lobby, `2` pregame, `3` ingame; set by scoreboard sidebar parsing in `getBedwarsStatus()`
- `inBwPregame` — boolean flag set when a `"has joined (N/M)!"` message is seen; used to enable chat-based player detection when scoreboard is broken (common on Hypixel)
- `currentLobby` — lobby/server ID string parsed from scoreboard; used as a cache key to prevent cross-game stat leakage

**Player add pipeline** (for `/ov add` and auto-detection):
1. `addPlaceholderStats(uuid, name, true)` — immediately shows `§7-` placeholders in overlay
2. `addToPlayers(uuid)` — inserts into `currentPlayers` (nicked players sorted separately)
3. `new Thread(() -> handlePlayerStats(uuid, lobby)).start()` — fetches stats from Prism API async (no key required)
4. `new Thread(() -> handleUrchinTag(uuid, lobby)).start()` — fetches cheater tag async
5. Both threads call `addToOverlay(uuid, data)` if `isInOverlay(uuid) && currentLobby.equals(lobby)` (prevents stale updates after lobby change)

**Auto-detection in `onChat()`** uses two patterns:
- `LOBBY_JOIN` — `"^(\w+) has joined \(\d+/\d+\)!$"` — no status guard; fires `addChatPlayer()` with 1-second delayed tab-list lookup retry before falling back to Mojang UUID API
- `CHAT_SENDER` — `"^(?:\[[\w+]+\] )?(\w+) ?: .+"` — guarded by `inBwPregame || status >= 1`

**Nick detection**: `uuid.charAt(12) != '4'` — UUID v4 always has `4` at position 12; Hypixel assigns offline-mode UUIDs to nicked players.

### Config
`LazifyConfig` is a singleton (`LazifyConfig.INSTANCE`) backed by `lazify.cfg` in the mod config directory. Settings are read/written through `CommandOv` subcommands (`/ov key`, `/ov sort`, `/ov col`, `/ov color`, etc.).

### Rendering
`OverlayRenderer` handles raw OpenGL/Tessellator drawing. `OverlayManager.doColumns()` recalculates column widths and layout whenever the player list changes. `OverlayManager.sortOverlay()` reorders `currentPlayers`.

### Known Non-Issues
`Scoreboard.func_96511_d` / `S3EPacketTeams` NPEs in logs are a pre-existing vanilla 1.8.9 bug on Hypixel, unrelated to this mod. The `status` field may be `-1` throughout a game when the scoreboard fails; `inBwPregame` exists specifically to compensate for this.
