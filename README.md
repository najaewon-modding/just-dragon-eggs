# Just Dragon Eggs

**Just Dragon Eggs** is a lightweight NeoForge mod that turns every Ender Dragon egg into a unique trophy containing the battle record.

## Compatibility

- Minecraft: `26.1.2`
- NeoForge: `26.1.2.97` or newer for Minecraft 26.1.2
- Mod version: `1.0.0-mc26.1.2`

For multiplayer, install the mod on both the server and connecting clients.

## Features

### A recorded egg for every Ender Dragon

Every Ender Dragon kill produces a recorded Dragon Egg. The first vanilla Dragon Egg is replaced with the recorded version, and later kills create additional recorded eggs.

Each egg stores the record of its own battle, and the record is preserved when the egg becomes an item and is placed again.

### Egg information

When a recorded egg is an item, its tooltip shows the Dragon number and killer, for example:

```text
#1 PlayerName
```

When aiming at a placed recorded egg, the Dragon number and killer are displayed above the egg.

### Vanilla-style interaction

- Left click: teleport the Dragon Egg.
- Right click: teleport the Dragon Egg.
- Shift + right click: keep the egg in place and open its Battle Record screen.

### Battle Record screen

The Battle Record screen shows:

- players ranked by damage dealt to the Ender Dragon;
- each player's share of total battle damage;
- unowned or unattributed damage as `Other`, outside the player ranking;
- the player who dealt the final blow;
- per-player damage breakdowns by weapon or damage method.

Click a player name, or `Other`, to open the detailed damage breakdown.

## Damage attribution

The mod records the Ender Dragon's actual health loss after Minecraft has applied damage handling. Player attribution is intentionally conservative: damage is assigned to a player only when Minecraft provides a reliable causal connection. Damage that cannot be attributed reliably is recorded as `Other` instead of being guessed from proximity or redstone activity.

## Installation

1. Install NeoForge for Minecraft 26.1.2.
2. Put the Just Dragon Eggs JAR in the `mods` folder.
3. Launch Minecraft with the matching NeoForge profile.

## License

Just Dragon Eggs is licensed under the [MIT License](LICENSE).
