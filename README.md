# Sovereignty & Steel: Island Realm (1.21.4)
**The Ultimate Island Nation, Geopolitical & Civilization Plugin for Modern Minecraft Servers**  
**Official Author & Lead Developer**: `filipGFF1` ˘ **Version**: `1.0.0` ˘ **Platform**: Paper / Purpur 1.21.4

![Sovereignty & Steel Banner](./sovereignty_thumbnail.jpg)

---

## đźŚź Overview

**Sovereignty & Steel: Island Realm** is a next-generation geopolitical, kingdom management, and territorial warfare engine engineered specifically for Minecraft 1.21.4 island survival and civilization servers.

Unlike traditional town or claim plugins that treat the world as a flat, boring grid, **Sovereignty & Steel** turns your server into a living, breathing oceanic archipelago of rival empires, trade networks, automated fortress walls, and maritime warfare. Players don't just "claim land" â€” they found dynasties, choose from 8 distinct political doctrines, commission legendary generals, defend against pirate flotillas, and build self-repairing battlements.

---

##  Multi-Language Localization Engine

Sovereignty & Steel comes with complete out-of-the-box internationalization. On a clean installation, the plugin greets players and server owners in **English (`en`)** by default.

### Language Configuration (`config.yml`):
```yaml
# -------------------------------------------------------------------------
# Multi-Language Localization Engine
# -------------------------------------------------------------------------
# Choose the plugin interface & messaging language:
#   "en" - English (Default / Recommended for international servers)
#   "pl" - Polish (Polski)
#   "es" - Spanish (EspaĂ±ol)
#
# Note: Every message can be fully customized in the respective messages_*.yml file.
language: "en"
```

### Supported Language Packs:
- **`messages_en.yml`**: English (Default international standard)
- **`messages_pl.yml`**: Polish (Complete native translation)
- **`messages_es.yml`**: Spanish (Complete native translation)

If any custom key is omitted or missing in a translated file, the engine automatically falls back to English without crashing or throwing errors.

---

## Localized Commands & Native Aliases

Players on international servers can comfortably use commands in their native language:

### English Commands (Default):
- `/country` (or `/nation`, `/kingdom`, `/sovereignty`) â€” Opens the main management dashboard and command guide.
- `/country create <name> <tag>` â€” Founds a new island realm and awards the mystical **Bastion Core**.
- `/country bank` (or `/bank`, `/treasury`) â€” Inspects the National Treasury, Vault balance, and daily Upkeep status.
- `/country deposit <amount>` / `/country withdraw <amount>` â€” Financial transactions with the National Treasury.
- `/country vault [1-5|upgrade|autosell]` â€” Accesses multi-page secure storage and configures Auto-Sell liquidation.
- `/country claim` (or `/claim`) â€” Assimilates the current chunk into the nation's borders with anti-abuse cooldowns.
- `/country unclaim` (or `/unclaim`) â€” Relinquishes the current chunk back to the untamed wilderness.
- `/country wall` â€” Opens the interactive border fortification and automated wall management GUI.
- `/country regime` â€” Opens the Political Doctrine selection menu (8 unique doctrines).
- `/country tech` (or `/country upgrades`) â€” Opens the national technology research tree (Tier I to V).
- `/country color <#HEX>` â€” Customizes nation territory border colors on web maps (Squaremap, Dynmap, BlueMap, Pl3xMap).
- `/country territory` (or `/teren`, `/land`) â€” Inspects the allegiance of the chunk you are currently standing in.
- `/country recipes` (or `/crafting`) â€” Interactive 3x3 recipe book showcasing all custom items and siege engines.
- `/country invite <player>` / `/country join <name>` / `/country leave` â€” Citizen recruitment and membership.
- `/country war <nation>` / `/country relation <ally|nap|neutral|rival> <nation>` â€” International diplomacy and warfare.
- `/country surrender` â€” Formally surrenders a war (cedes 25% territory, 70% treasury, and establishes tribute).
- `/country siegewindow <from> <to>` â€” Configures your 3-to-4 hour daily bastion defense window to prevent offline raids.
- `/country general` (or `/country drumroll`) â€” Activates General Paolo Or'e's War March combat aura.
- `/country tribute` â€” Negotiates and pays tributes to pirate fleets.
- `/country trash` (or `/trash`, `/kosz`) â€” Portable item disposal bin.
- `/country sit` (or `/sit`) â€” Sit down on stairs, slabs, chairs, or flat ground.
- `/country author` (or `/country about`, `/country version`) â€” Displays official developer credentials for `filipGFF1`.
- `/tc <message>` â€” Encrypted town / nation internal chat channel.
- `/ac <message>` â€” Allied cross-nation communication channel.

### Polish Commands (`pl`):
- `/panstwo` (or `/kraj`) â€” GĹ‚Ăłwne menu zarzÄ…dzania paĹ„stwem wyspiarskim.
- `/panstwo stworz <nazwa> <tag>` / `/panstwo zapros <gracz>` / `/panstwo dolacz <nazwa>`.
- `/panstwo bank` / `/panstwo wplac` / `/panstwo wyplac` / `/panstwo skarbiec`.
- `/panstwo claim` / `/panstwo unclaim` / `/panstwo mur` / `/panstwo ustroj` / `/panstwo upgrady`.
- `/panstwo autor` â€” Oficjalny certyfikat autorstwa twĂłrcy `filipGFF1`.

### Spanish Commands (`es`):
- `/pais` (or `/reino`) â€” MenĂş principal de gestiĂłn de la naciĂłn isleĂ±a.
- `/pais crear <nombre> <tag>` / `/pais invitar <jugador>` / `/pais unirse <nombre>`.
- `/pais banco` / `/pais depositar` / `/pais retirar` / `/pais tesoro`.
- `/pais reclamar` / `/pais renunciar` / `/pais muro` / `/pais regimen` / `/pais tecnologia`.
- `/pais autor` â€” Credenciales oficiales del desarrollador `filipGFF1`.

---

## ° Deep Gameplay Mechanics

### 1. Bastion Cores & Capital Founding
When founding a nation (`/country create`), the sovereign receives an ancient **Bastion Core**. Placing this artifact on an island anchors the capital city:
- Projects a defensive influence sphere (65-block radius).
- Activates an immediate **48-Hour Peace Shield** protecting new realms from aggression.
- Enforces a player-configured daily **Siege Defense Window** (e.g. 18:00â€“22:00) so players never get raided while asleep or offline.

### 2. The 8 Political Regimes (Doctrines)
Choose a path that defines your nation's society, economy, and bonuses:
1. **Feudalism**: Lowers military unit upkeep by 30% and multiplies grain/agricultural production.
2. **Republic**: Elevates citizen welfare, speeds up construction, and boosts tax yields.
3. **Merchant Oligarchy**: Lowers market fees, gives discounts on server trade, and collects customs tariffs from visiting ships.
4. **Imperial Monarchy**: Increases claim capacity by 50% and dramatically hardens fortress walls.
5. **Theocracy**: Grants defensive damage resistance within holy ground and passive health regeneration.
6. **Totalitarian State**: Boosts army movement speed, war mobilization, and doubles war casualty points.
7. **Tribal Federation**: Increases island exploration speed, fishing yields, and wood harvest output.
8. **Technocracy**: Accelerates Super Furnace smelting rates and slashes research upgrade costs.

### 3. Five-Tier National Technology Tree
Invest national wealth to unlock higher tiers of development:
- **Tier I (Wood / Starter)**: Basic claims (up to 15 chunks), 1 vault chest (54 slots).
- **Tier II (Stone)**: Up to 35 chunks, 2 vault chests (108 slots), Ballistas and Catapults.
- **Tier III (Iron)**: Up to 65 chunks, 3 vault chests (162 slots), Iron Perimeter Walls.
- **Tier IV (Diamond)**: Up to 100 chunks, 4 vault chests (216 slots), Battering Rams, Customs Outposts.
- **Tier V (Netherite)**: Unlimited island claims, 5 vault chests (270 slots), **Automated Auto-Sell Engine**, and Trebuchets.

### 4. Self-Assembling & Auto-Repairing Walls
Border claims can be outfitted with defensive perimeter walls via `/country wall`. When walls sustain damage during siege warfare, the built-in **Restoration Engine** slowly and automatically reconstructs fallen blocks once the battle concludes.

### 5. Multi-Page National Vault with Auto-Sell
Access shared national storage with `/country vault`. On Tier V, toggle the **Auto-Sell Module** (`/country vault autosell`): any designated excess ores, wood, or crops deposited into vault chests are automatically sold to the economy at peak market rates, funneling revenue directly into the National Treasury.

### 6. General Paolo Or'e & The War March
Summon the legendary tactician General Paolo Or'e! When marching into battle, his drumbeat aura grants nearby soldiers speed and damage resistance, shifting the tide in pivotal island clashes.

### 7. Maritime Pirate Raids
As your realm's wealth and borders expand, rogue pirate galleons spawn offshore. The crown must decide: pay tribute in gold and rations before the negotiation timer expires, or rally the garrison to fight the pirates for legendary custom drops.

### 8. Live Web Map Rendering
Seamless, zero-lag integration with modern web mapping plugins:
- **Squaremap** â€˘ **Dynmap** â€˘ **BlueMap** â€˘ **Pl3xMap**
- Highlights claims, capital bastions, and custom HEX colors chosen by the sovereign.

### 9. Tactical Short Shield (Buckler) â€˘ CustomModelData: 1001
Engineered for agile island skirmishers and duelists:
- **Mobility**: Grants +10% sprint speed while equipped in hand or off-hand.
- **Fast Parry Recovery**: When struck by an axe, the shield-disable cooldown is reduced by 50% (recovers in 2.5s instead of 5.0s).
- **Custom Sound & Spark Effects**: Plays high-pitch deflect audio (`ITEM_SHIELD_BLOCK` at 1.6f) and spawns golden critical deflection particles.
- **Crafting Recipe**: Crafted in a 3x3 table with `Planks | Iron Ingot | Planks` over `Planks`. Inspect live in `/country recipes`!
- **Resource Pack Ready**: Tagged with `CustomModelData: 1001` for custom 3D round buckler models.

---

## Installation & Requirements

1. Download **`sovereignty-steel.jar`** from your Desktop (`C:\Users\pimpe\OneDrive\Pulpit\sovereignty-steel.jar`).
2. Drop it into your server's `/plugins/` folder.
3. Ensure **Paper** or **Purpur 1.21.4** (Java 21) is installed.
4. Ensure **Vault** (and a Vault-compatible economy plugin like EssentialsX) is installed.
5. *(Optional)* Install a web map plugin (**Squaremap**, **Dynmap**, **BlueMap**, or **Pl3xMap**) for live territory maps.
6. Start or restart your server. The plugin will automatically generate all configuration files and language packs.

---

## Permissions Summary

| Permission Node | Default | Description |
| :--- | :--- | :--- |
| `sovereignty.use` | `true` (All Players) | Basic access to create, join, and manage island nations |
| `sovereignty.admin` | `op` (Administrators) | Full access to administrative bypass, force disband, and debugging tools |

---
