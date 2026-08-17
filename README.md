# Vortex Plus

An addon for [Vortex Client](https://github.com/Marcinator31/Vortex-Client)
that adds the existing combat modules plus Armor Organizer, Inventory Tweak and an add-on-owned Vortex+ HUD. Because the main client is not modified, the add-on modules use the client's existing **HUD** category and the visible name prefix **Vortex +**.

---

## Modules

| Module | What it does |
| --- | --- |
| **Kill Aura** | Attacks whatever is in range, with a line-of-sight check |
| **Crystal Aura** | Places end crystals under opponents and breaks them, without catching you |
| **Fast Anchor** | Charges and triggers respawn anchors in one go |
| **Fast Use** | Throws bottles, pearls and potions far faster than the game allows |
| **Chest Stealer** | Empties an open container in a single tick |
| **Auto Tool** | Switches to the best tool for the block you are breaking |
| **Spawner Safer** | Collects spawners nearby and waits when a player hits you |
| **Armor Organizer** | Equips the best available armor from the player's own inventory, even without opening the inventory screen |
| **Inventory Tweak** | Uses the normal Minecraft quick-move action once while the Sneak/Shift key is held over an item |
| **Vortex + HUD** | Shows local session time/deaths, server marker, ping and passive event notifications |

---

## A word before you install this

Every one of these automates something the game expects you to do yourself.
Anti-cheat systems detect that reliably, and using them on a server that
forbids them will very likely get you banned. That is not a warning about an
edge case — it is the normal outcome.

The client on its own only displays information the game has already sent you.
Armor Organizer and Inventory Tweak use normal client inventory interactions. The Vortex + HUD only reads local client state and already visible chat messages. The existing combat modules remain functionally unchanged; only their displayed category/name is adapted to the read-only main-client API.

---

## Requirements

- **Vortex Client 2.20.0** or newer — earlier versions lack the interfaces this
  uses, and the addon will not load
- Minecraft **1.21.11**, Fabric Loader **0.18.1** or newer, Fabric API
- Java **21**

Install both jars in your mods folder. The client's mark turns red once the
addon is present, so you can see which of the two you are running.

---

## Building

### Via GitHub Actions
Every push builds automatically; the jar is an artifact in the Actions tab. The
workflow fetches and builds the client itself, so nothing needs to be prepared.

### Locally
The addon compiles against the client, so build that first and put its jar in
place:

```
# in the client repository
./gradlew build
cp build/libs/vortexclient-*.jar ../vortex-plus/libs/vortexclient-api.jar

# in this repository
./gradlew build
```

The finished addon lands in `build/libs/` — the file **without** `-sources`.

---

## Licence

MIT
