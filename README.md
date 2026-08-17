# Vortex Plus

An addon for [Vortex Client](https://github.com/Marcinator31/Vortex-Client)
that adds the existing combat modules plus Armor Organizer and Inventory Tweak. The modules are sorted into the client's standard **Cheats** and **Misc** categories.

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
| **Inventory Tweak** | Moves an item with the normal Minecraft quick-move action when you hover over it while holding Sneak/Shift; no click is required |
| **Local PvP Features** | Tracks wins, losses, kills, deaths and playtime locally; monitors ping changes, sends PvP notifications, stores server profiles and supports PvP profiles and hit sounds |
| **Dynamic Crosshair** | Changes the crosshair gap with sprinting, attack cooldown and item use |
| **Replay Highlights** | Starts a real local FFmpeg recording on detected kill, win or PvP-event messages and writes MP4 files |
| **Manual Replay Recorder** | Uses the module's configurable toggle key to start recording and the same key to stop and finalize a real MP4 file |

---

## A word before you install this

Every one of these automates something the game expects you to do yourself.
Anti-cheat systems detect that reliably, and using them on a server that
forbids them will very likely get you banned. That is not a warning about an
edge case — it is the normal outcome.

The client on its own only displays information the game has already sent you.
Armor Organizer and Inventory Tweak use normal client inventory interactions. The existing combat modules remain functionally unchanged and are shown in the standard Cheats category.

---

## Requirements

- **Vortex Client 2.20.0** or newer — earlier versions lack the interfaces this
  uses, and the addon will not load
- Minecraft **1.21.11**, Fabric Loader **0.18.1** or newer, Fabric API
- Java **21**
- `ffmpeg` in the system PATH for real MP4 Replay Highlights; clips are written to `.minecraft/vortex-plus/replays/`

To record manually, open the Vortex menu, find **Manual Replay Recorder** under **Misc**, set its **Toggle Key**, then press that key once to start and again to stop. The file is finalized as an MP4 when recording stops.

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
