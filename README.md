# Vortex Plus

An addon for [Vortex Client](https://github.com/Marcinator31/Vortex-Client)
that adds seven combat modules. They appear in the client's own menu under
**Cheats**, with their own settings, key bindings and presets — the same as the
built-in ones.

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

---

## A word before you install this

Every one of these automates something the game expects you to do yourself.
Anti-cheat systems detect that reliably, and using them on a server that
forbids them will very likely get you banned. That is not a warning about an
edge case — it is the normal outcome.

The client on its own only displays information the game has already sent you.
This addon is a different thing, which is why it is a separate download and why
its modules sit under Cheats.

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
