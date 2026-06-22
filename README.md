# WynnDialogueSkip

A Fabric client mod for Wynncraft that automatically skips NPC dialogue using your configured dialogue skip keybind.

## Features

- Automatically presses your dialogue skip key when NPC dialogue can be advanced
- Configurable skip delay and animation behavior
- Toggle keybind to enable or disable the mod in-game
- Toast notification when toggling enabled/disabled
- Mod Menu integration for easy configuration

## Requirements

- Minecraft **1.21.11**
- [Fabric Loader](https://fabricmc.net/) **0.18.4+**
- [Fabric API](https://modrinth.com/mod/fabric-api)
- [Wynntils](https://modrinth.com/mod/wynntils) (required at runtime)
- [Cloth Config](https://modrinth.com/mod/cloth-config)
- [Mod Menu](https://modrinth.com/mod/modmenu) (optional, for in-game config)

## Installation

1. Install Fabric, Fabric API, Wynntils, and Cloth Config for Minecraft 1.21.11.
2. Download the latest `WynnDialogueSkip` jar from [GitHub Releases](https://github.com/Mengilly/wynndialogueskip/releases).
3. Place the jar in your Minecraft `mods` folder.
4. Launch the game.

## Configuration

Open Mod Menu and select **Auto Skip Dialogue**, or edit the config file directly.

| Option | Description | Default |
|--------|-------------|---------|
| Enable Mod | Turn automatic dialogue skipping on or off | `true` |
| Skip Delay (ms) | Delay before skipping dialogue | `400` |
| Skip During Animation | Skip while text is still animating | `false` |

### Toggle Keybind

Assign **Toggle Auto Skip Dialogue** under **Options → Controls → Auto Skip Dialogue**. Pressing it toggles the mod and shows a toast notification.

## Building from Source

You need **Java 21** and a local Wynntils jar for compilation.

```bash
git clone https://github.com/Mengilly/wynndialogueskip.git
cd wynndialogueskip
```

Build Wynntils and place the remapped Fabric jar where this project expects it, or clone Wynntils as a sibling directory:

```bash
# From the parent directory of this repo
git clone https://github.com/Wynntils/Wynntils.git Wynntils-4.1.20
cd Wynntils-4.1.20
./gradlew :fabric:remapJar
```

Then build this mod:

```bash
cd wynndialogueskip
./gradlew build
```

The built jar is at `build/libs/WynnDialogueSkip-<version>.jar`.

## License

This project is licensed under the [GNU Lesser General Public License v3.0](LICENSE).
