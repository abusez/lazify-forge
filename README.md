# Lazify

A Minecraft Forge mod for **1.8.9** that displays a real-time HUD overlay showing player statistics during Hypixel Bedwars games. Ported from the Raven B4 Lazify3 Lua script.

## Features

- Live player stats table overlay during Bedwars matches
- Shows star level, FKDR, winstreak, session duration, and encounter count
- Urchin API integration for cheater detection tags
- Hypixel rank display with proper formatting and colors
- Team color coding
- Prestige-based star colors (50+ prestige levels)
- Skin-based nick detection — reveals nicked players' real names
- Middle-click shop — instant buying in Bedwars shop GUIs
- Auto-detect players from tab list or `/who`
- Customizable columns, sorting, position, and colors
- Toggle or hold-to-show keybind modes
- Show overlay while holding Tab
- Auto-remove players on final kill

## Requirements

- Minecraft 1.8.9
- Minecraft Forge 11.15.1.2318
- Java 8
- Urchin API key (optional, for cheater tags)

## Building

```bash
./gradlew build
```

The output JAR will be in `build/retromapping/retromappedReplacedMain.jar`.

## Installation

1. Install [Minecraft Forge 1.8.9](https://files.minecraftforge.net/)
2. Drop the JAR into your `.minecraft/mods/` folder
3. Launch Minecraft

## Configuration

The config file is stored at `.minecraft/config/lazify.cfg`.

Run `/ov 2` to view all settings grouped by category. Toggle any boolean setting with `/ov <setting>` or set a value with `/ov <setting> <value>`.

Set your Urchin API key (optional): `/ov key urchin <your-key>`

## Commands

All commands use the `/ov` prefix (aliases: `/overlay`, `/lazify`).

| Command | Description |
|---|---|
| `/ov` | Show command help |
| `/ov 2` | Show all settings |
| `/ov sc <player>` | Add a player to the overlay |
| `/ov hide <player>` | Hide a player from the overlay |
| `/ov clearhidden` | Clear the hidden players list |
| `/ov reload` | Re-fetch stats for all players |
| `/ov clear` | Clear all players from overlay |
| `/ov key urchin <key>` | Set your Urchin API key |
| `/ov tags` | Show overlay tag definitions |
| `/ov tag <player>` | Look up a player's full Urchin tags |
| `/ov col <column>` | Toggle column visibility |
| `/ov sortby <0-5>` | Sort by column (0=Encounters, 1=Star, 2=FKDR, 3=Index, 4=Winstreak, 5=JoinTime) |
| `/ov sortmode <0/1>` | 0=ascending (highest on top), 1=descending |
| `/ov winstreak <0-5>` | Winstreak mode (0=Overall, 1=Solos, 2=Doubles, 3=Threes, 4=Fours, 5=4v4) |

## Settings

Toggle any boolean setting with `/ov <name>`, or set explicitly with `/ov <name> true/false`.

| Setting | Default | Description |
|---|---|---|
| `teams` | on | Show team colors in overlay |
| `teamprefix` | off | Show team prefix letters |
| `showranks` | off | Show Hypixel rank next to name |
| `showyourself` | off | Include yourself in the overlay |
| `removefinalkill` | off | Remove players on final kill |
| `autotablist` | on | Auto-detect players from tab list |
| `clearonwho` | off | Clear overlay when `/who` is used |
| `skindenick` | on | Detect nicked players by their skin |
| `middleclickshop` | off | Convert clicks to middle-click in BW shops (shift-click still works normally) |
| `sendnicked` | on | Print chat notice for nicked players |
| `sendurchinreason` | off | Print Urchin tag reason to chat |
| `keybindhold` | off | Hold key to show overlay instead of toggle |
| `showontab` | on | Show overlay while holding Tab |
| `debug` | off | Print debug messages to chat |

## Keybind

Default keybind is the grave/backtick key (`` ` ``). Change it with `/ov keybind <LWJGL key code>`.

Modes:
- **Toggle** (default): press to show/hide
- **Hold** (`/ov keybindhold`): overlay visible only while key is held
- **Tab** (`/ov showontab`): overlay also shows while holding Tab

## Credits

Ported from the Raven B4 Lazify3 script.
