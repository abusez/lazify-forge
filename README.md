# Lazify

A Minecraft Forge mod for **1.8.9** that displays a real-time HUD overlay showing player statistics during Hypixel Bedwars games. Ported from the Raven B4 Lazify3 Lua script.

## Features

- Live player stats table overlay during Bedwars matches
- Shows star level, FKDR, winstreak, session duration, and encounter count
- Urchin API integration for cheater detection tags
- Hypixel rank display with proper formatting and colors
- Team color coding
- Prestige-based star colors (50+ prestige levels)
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

Set your Urchin API key (optional): `/ov key <your-key>`

## Commands

All commands use the `/ov` prefix (aliases: `/overlay`, `/lazify`).

| Command | Description |
|---|---|
| `/ov key <key>` | Set your Urchin API key |
| `/ov sc` | Sneak-click toggle |
| `/ov hide <player>` | Hide a player from the overlay |
| `/ov clearhidden` | Clear the hidden players list |
| `/ov col <column> <true/false>` | Toggle column visibility (encounters, username, star, fkdr, winstreaks, urchin, session) |
| `/ov sortby <0-5>` | Sort by column (0=Encounters, 1=Star, 2=FKDR, 3=Index, 4=Winstreak, 5=JoinTime) |
| `/ov sortmode <0/1>` | 0=ascending (highest on top), 1=descending |
| `/ov winstreak <0-5>` | Winstreak mode (0=Overall, 1=Solos, 2=Doubles, 3=Threes, 4=Fours, 5=4v4) |
| `/ov set <setting> <value>` | Change a boolean setting (teams, teamprefix, showyourself, sendnickedtochat, sendurchinreasontochat, showranks, removefinalkill, showontab, keybindhold) |
| `/ov color <bg/header/border> <0-360>` | Set hue for overlay colors |
| `/ov opacity <0-255>` | Set background opacity |
| `/ov reload` | Reload config from disk |
| `/ov clear` | Clear cached player data |

## Keybind

Default keybind is the grave/backtick key (`` ` ``). Change it with `/ov keybind <LWJGL key code>`.

Modes:
- **Toggle** (default): press to show/hide
- **Hold** (`/ov set keybindhold true`): overlay visible only while key is held
- **Tab** (`/ov set showontab true`): overlay also shows while holding Tab

## Credits

Ported from the Raven B4 Lazify3 script.
