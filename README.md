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

## Requirements

- Minecraft 1.8.9
- Minecraft Forge 11.15.1.2318
- Java 8
- Hypixel API key (`/api new` in-game on Hypixel)
- Urchin API key (optional, for cheater tags)

## Building

```bash
./gradlew build
```

The output JAR will be in `build/libs/Lazify-3.0.jar`.

To run in a development environment:

```bash
./gradlew runClient
```

## Installation

1. Install [Minecraft Forge 1.8.9](https://files.minecraftforge.net/)
2. Drop `Lazify-3.0.jar` into your `.minecraft/mods/` folder
3. Launch Minecraft and set your API keys (see Configuration below)

## Configuration

Run `/ov key hypixel <your-key>` and `/ov key urchin <your-key>` in-game to set API keys.

The config file is stored at `.minecraft/config/Lazify/lazify.cfg`.

## Commands

All commands use the `/ov` prefix.

| Command | Description |
|---|---|
| `/ov key hypixel <key>` | Set your Hypixel API key |
| `/ov key urchin <key>` | Set your Urchin API key |
| `/ov sc` | Sneak-click toggle (show overlay only while sneaking) |
| `/ov hide <player>` | Hide a player from the overlay |
| `/ov clearhidden` | Clear the hidden players list |
| `/ov col <column> <true/false>` | Toggle column visibility (e.g. `fkdr`, `ws`, `star`) |
| `/ov sortby <n>` | Sort by column number |
| `/ov set <setting> <value>` | Change a config setting |
| `/ov reload` | Reload config from disk |
| `/ov clear` | Clear cached player data |

## Keybind

Default keybind is the grave/backtick key (`` ` ``). Change it in Minecraft's controls menu.

## Credits

Ported from the Raven B4 Lazify3 script.
