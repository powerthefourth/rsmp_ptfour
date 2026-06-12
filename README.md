# RankedSMP v3.0 — Source Project

> **Inspired by SEA4's Ranked SMP series**  
> A complete Minecraft plugin with all mechanics from the ranked SMP format.

---

## ✅ What's New in v3.0 (vs the original v2.2)

| Feature | v2.2 (original) | v3.0 (this build) |
|---|---|---|
| Rank stealing on kill | ✅ | ✅ Improved (unranked→ranked also works) |
| Health scaling | ✅ | ✅ Config-driven, linear interpolation |
| XP multiplier | ✅ | ✅ |
| Potion duration scaling | ✅ | ✅ Negative effects now scale inversely |
| Extra inventory (top 10) | ✅ | ✅ Persistent across restarts |
| Hierarchy Hammer dash | ✅ | ✅ With particle trail |
| Hierarchy Hammer Verdict | ✅ (5 hits) | ✅ Configurable hits, AoE shockwave |
| Combo action bar HUD | ❌ | ✅ Visual bar showing combo progress |
| Dragon Egg buffs + glow | ✅ | ✅ Speed II + Strength I, persistent check |
| Dragon Egg locator bar | ✅ | ❌ Removed (as requested) |
| Rank in chat | ❌ | ✅ [#1] Name: message format |
| Tab list rank prefix | ✅ | ✅ Color-coded by rank tier |
| Rank management GUI | ✅ | ✅ Improved with swap + right-click remove |
| `/rsmp stop` command | ❌ | ✅ |
| `/rsmp list` command | ❌ | ✅ |
| `/rsmp reset` command | ❌ | ✅ |
| `/rsmp give egg` | ❌ | ✅ |
| Config-driven all values | partial | ✅ Fully configurable |
| Extra inv persists restarts | partial | ✅ YAML-based save |

---

## 🔨 How to Build

**Requirements:** Java 21+, Maven 3.6+

```bash
# 1. Open a terminal in this folder (where pom.xml is)
cd RankedSMP

# 2. Run Maven build
mvn package

# 3. Your plugin JAR is at:
#    target/RankedSMP-3.0.jar
```

Then drop `RankedSMP-3.0.jar` into your server's `plugins/` folder and restart.

---

## 🚀 Quick Start

1. Put the JAR in `plugins/`
2. Start the server — it will generate `plugins/RankedSMP/config.yml`
3. Get players online, then run: `/rsmp start`
4. Ranks are automatically assigned to up to 20 online players

---

## 📋 Commands

| Command | Permission | Description |
|---|---|---|
| `/rsmp start` | op | Randomly assign ranks to online players |
| `/rsmp stop` | op | Stop system, clear all ranks |
| `/rsmp reset` | op | Same as stop |
| `/rsmp manage` | op | Open the rank management GUI |
| `/rsmp list` | op | Show all ranked players in chat |
| `/rsmp rank <player> <1-20\|remove>` | op | Manually set or remove a rank |
| `/rsmp give hammer [player]` | op | Give a Hierarchy Hammer |
| `/rsmp give egg [player]` | op | Give a Dragon Egg |
| `/rsmp reload` | op | Reload config.yml |
| `/rsmp version` | op | Show plugin version |
| `/einv` | all players | Open your extra inventory (top 10 only) |

**Aliases:** `/rsmp` = `/rankedsmp`, `/einv` = `/extrainventory`

---

## ⚙️ Config Overview (`config.yml`)

```yaml
gameplay:
  keep-ranks: false        # true = ranks can't be stolen
  max-ranked-players: 20   # how many ranked slots exist
  announce-rank-swaps: true

health:
  rank-1-hearts: 40        # 20 full hearts (HP)
  rank-20-hearts: 21       # ~10.5 hearts

xp:
  rank-1-multiplier: 3.0
  rank-20-multiplier: 1.1

potions:
  rank-1-multiplier: 2.0   # Rank 1 gets 2x duration on all buffs
  rank-20-multiplier: 1.05
  scale-negative-effects: true  # Higher ranks suffer less from debuffs

extra-inventory:
  rank-1-slots: 54    # Full double chest
  rank-2-slots: 54
  rank-3-slots: 45
  # ... down to rank 10

hammer:
  hits-to-charge: 4         # Hits needed to activate Verdict
  verdict-damage-multiplier: 2.0
  verdict-aoe-radius: 4.0
  verdict-knockback: 2.5
  dash-cooldown: 3           # Seconds between dashes

dragon-egg:
  holder-glows: true
  announce-egg-pickup: true
```

---

## ⚔️ Mechanic Details

### Rank Stealing
- Kill a **higher-ranked** player (lower number) → you get their rank, they get yours
- Kill a **lower-ranked** player → no rank change (you already outrank them)
- Kill a ranked player while **unranked** → you take their rank

### Hierarchy Hammer
- **Right-click** → Dash forward with particle trail (configurable cooldown)
- **Land N consecutive hits** (default 4) without missing → VERDICT activates on next hit:
  - 2× damage on primary target
  - AoE shockwave knocks back all nearby players
  - Shockwave deals half damage to those hit
  - Visual explosion + sound effects
- Missing an attack (no target hit within the combo window) → combo resets

### Dragon Egg
- Holding the egg gives Speed II + Strength I
- You glow visibly (others can see you through walls at close range)
- Announced to the server on pickup
- Buffs automatically removed when egg is dropped/lost

### Extra Inventory
- Only ranks **#1–#10** have access via `/einv`
- Inventory sizes scale: Rank 1–2 = 54 slots, down to Rank 10 = 9 slots
- **Saved to disk** when you close the inventory or log out
- If you drop below rank 10, your inventory is saved until you return

---

## 🖼️ Rank Management GUI (`/rsmp manage`)
- Shows all 20 rank slots with player heads
- **Left-click** a player → select for swap → left-click another → swap ranks
- **Right-click** a player → remove their rank
- Bottom row: Start | Stop | Reload | View List | Close

---

## 🎨 Rank Display Colors
| Rank | Color |
|---|---|
| #1 | **Gold (bold)** |
| #2–#5 | Yellow |
| #6–#10 | Green |
| #11–#20 | Aqua |
| UNRANKED | Gray |

---

## 📁 File Structure

```
RankedSMP/
├── pom.xml                          ← Maven build file
├── README.md
└── src/main/
    ├── resources/
    │   ├── plugin.yml
    │   └── config.yml
    └── java/me/rankedsmp/
        ├── RankedSMP.java           ← Main plugin class
        ├── commands/
        │   ├── RankedSMPCommand.java
        │   └── ExtraInventoryCommand.java
        ├── gui/
        │   └── RankManagementGUI.java
        ├── items/
        │   └── HierarchyHammer.java
        ├── listeners/
        │   ├── PlayerListener.java
        │   ├── PvPListener.java       ← Rank stealing
        │   ├── ChatListener.java      ← Rank in chat
        │   ├── HierarchyHammerListener.java
        │   ├── ExtraInventoryListener.java
        │   ├── PotionListener.java
        │   ├── XPListener.java
        │   ├── DragonEggListener.java
        │   └── RankManagementGUIListener.java
        └── managers/
            ├── ConfigManager.java
            ├── RankManager.java       ← Core rank logic
            └── ExtraInventoryManager.java
```

---

## ℹ️ Notes
- Requires **Spigot or Paper 1.21.x** (uses Mace material + 1.21 API)
- No external dependencies needed
- All data saved in `plugins/RankedSMP/config.yml` (ranks) and `extra_inventories.yml` (inventories)
- Compatible with vanilla survival — no economy/permissions plugins required
