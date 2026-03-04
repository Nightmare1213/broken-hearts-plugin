# Broken Hearts Plugin

A Paper plugin (updated for **Minecraft/Paper 1.21.x**) that adds **equippable hearts**.

## Heart Abilities

- `flame` → **Inferno Brand**: melee hits burn enemies for 8 seconds and deal bonus damage.
- `swift` → **Tempest Dash**: press swap-hands (`F` by default) to dash far and gain Speed II (4s cooldown).
- `stone` → **Titan Guard**: reduces incoming damage by **45%**.
- `night` → **Abyss Pulse**: right-click to reveal every player/mob in your current chunk (stronger at night) (10s cooldown).
- `vampiric` → **Blood Feast**: drains hearts from enemies on hit and transfers them to you.
- `thunder` → **Stormcaller**: melee hits start a lightning storm that keeps shocking targets.
- `frost` → **Glacial Prison**: right-click to freeze monsters in place so they cannot act for 3 seconds.

### Boss Hearts (Level Up by Kills)

Boss hearts start in base form, then get stronger as you get kills while that boss heart is equipped.

- Tier system: **+1 tier per 5 kills, up to Tier 5**.
- `dragon` → **Dragon Roar**: stronger buffs, bigger roar radius and stronger blast each tier.
- `warden` → **Deep Silence**: higher bonus damage and stronger control each tier.
- `wither` → **Wither Reign**: stronger wither curse and drain scaling each tier.

## Crafting Hearts

All hearts are craftable with this shaped recipe:

```text
G D G
D C D
G D G
```

- `G` = Gold Block
- `D` = Diamond Block (4 total in the recipe)
- `C` = Heart catalyst item (varies by heart type)

Example catalysts:
- Flame: Blaze Rod
- Frost: Blue Ice
- Dragon: Dragon Breath
- Warden: Sculk Catalyst
- Wither: Wither Rose

## Command

- `/heart give <flame|swift|stone|night|vampiric|thunder|frost|dragon|warden|wither>`

## Build

```bash
mvn package
```

Use the generated JAR from `target/` in your server's `plugins/` folder.
