# VaultFinder Pro

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1%20--%2026.3-informational)](https://fabricmc.net/)
[![Fabric Loader](https://img.shields.io/badge/Fabric%20Loader-%3E%3D0.16.0-blue)](https://fabricmc.net/)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![GitHub Actions](https://github.com/slopgobblercobbler/vault-finder-pro/actions/workflows/build.yml/badge.svg)](https://github.com/slopgobblercobbler/vault-finder-pro/actions)

**VaultFinder Pro** is a high-performance, 100% client-side Fabric Minecraft mod that indexes all containers and shulker boxes you interact with and provides instant in-game item searches with 3D in-world glowing radar highlights.

Never lose items in massive storage rooms or nested shulkers again. Works seamlessly on any multiplayer server, Realm, or singleplayer world without requiring any server-side companion mod.

---

## 🌟 Key Features

- **Universal Container Memory**: Automatically records contents, stack counts, enchantments, and custom names of every Chest, Double Chest, Shulker Box, Ender Chest, Barrel, Hopper, Dispenser, Dropper, and Crafter you open.
- **In-World 3D Hologram Radar**: Containers containing your searched item glow through walls with customizable colored bounding boxes (green for complete stacks, yellow/cyan for partial matches).
- **Instant Search HUD (`Ctrl + F`)**: Fast in-game search bar with real-time fuzzy matching across your entire base inventory.
- **Shulker Peek Overlays**: Hover over any shulker box in your inventory or chests to inspect its 3x9 grid contents without placing or opening it.
- **Smart Quick-Deposit & Restock**:
  - One-click **Quick Deposit** moves matching items from your inventory into open containers that already store them.
  - One-click **Restock** replenishes depleted hotbar building blocks, rockets, and consumables from open containers.
- **100% Client-Side**: Strictly client-side networking. Generates zero custom packets, preventing kicks on vanilla, Paper, Purpur, or Spigot servers.

---

## 🔍 Powerful Search Syntax

| Query | Matches |
| :--- | :--- |
| `iron` | Any item containing "iron" (Iron Ingot, Iron Pickaxe, Iron Block). |
| `#mending` | Any weapon, armor, or enchanted book with the Mending enchantment. |
| `@create` | Any item belonging to the `create` mod namespace. |
| `>64` | Any container with more than 64 items matching the search query. |
| `durability < 20%` | Damaged tools or Elytra below 20% durability. |
| `color:cyan` | Cyan-dyed shulker boxes or leather armor. |

---

## 🎮 Controls

| Key | Default | Function |
| :--- | :---: | :--- |
| **Open Vault Search** | `Ctrl + F` | Opens the search HUD and container index. |
| **Toggle 3D Radar** | `H` | Toggles in-world bounding box highlight rendering. |
| **Quick Deposit** | `X` | Deposits matching inventory items into open container. |
| **Quick Restock** | `R` | Restocks depleted hotbar stacks from open container. |
| **Open Settings** | `M + C` / ModMenu | Opens in-game configuration screen. |

Keybindings are fully remappable in **Options > Controls > Key Binds > VaultFinder Pro**.

---

## ⚙️ Configuration

Settings can be customized in-game or via `.minecraft/config/vaultfinder.json`:

| Option | Type | Default | Description |
| :--- | :---: | :---: | :--- |
| `radarEnabled` | boolean | `true` | Enables 3D bounding box rendering in the world. |
| `maxRadarDistance` | integer | `64` | Maximum distance in blocks to render container highlights (8 - 256). |
| `xrayHighlights` | boolean | `true` | Allows highlight bounding boxes to render through walls. |
| `shulkerPeekEnabled` | boolean | `true` | Shows 3x9 tooltip preview when hovering over shulker boxes. |
| `autoRestockHotbar` | boolean | `false` | Automatically refills depleted hotbar slots when opening containers. |
| `showDistanceTags` | boolean | `true` | Displays floating distance billboards above highlighted containers. |

---

## 📦 Building from Source

```bash
git clone https://github.com/slopgobblercobbler/vault-finder-pro.git
cd vault-finder-pro
python build_native_mod.py
```

Packaged release jars are generated in `build/libs/`.

---

## 📄 License

This mod is licensed under the [MIT License](LICENSE).
