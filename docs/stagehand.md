# Stagehand

Claude Opus 5.5, 2026-11-06. A mod a session drives from outside the game, to set up a play-test in the game someone is playing: teleport them to a floor of a tower, give them weapons and armour, make it night. It runs where the world runs, single player or a server. No network port: a script goes in a folder, a result comes out. A public jar in the library's release, in no pack or server package, since something taking outside commands shouldn't be on by default. Anything that can write to the game's folder can command the game with it.

```
python tools/stagehand.py tp spawn
python tools/stagehand.py "tp 120 40 -300; kit iron; heal; time noon; say Try the way down."
python tools/stagehand.py -f setup.txt
python tools/stagehand.py --json status
```

It finds the game by itself: the Prism instance with Stagehand in it, the latest played if several. `--dir <.minecraft or server folder>` or `STAGEHAND_DIR` to say. It waits for the script to finish, up to `--timeout` (30 s; the script goes on in the game, and `result <id>` reads it later), prints each line's outcome and exits 0 if all ran, 1 if the script failed, 2 if nothing answered or it's still going. `--world-wait <secs>` keeps trying while no world is open or nobody's in it. `--help` has the rest.

## Installing

The game must be closed. In an instance from More Faithful, copy `stagehand-<v>.jar` to `.minecraft/mods-library/stagehand.jar` and add the line `mods-library/stagehand.jar` to `.minecraft/mods-library/selection.txt`; it then has a switch in the Mods menu like the rest. In any other instance, it goes in `.minecraft/mods` with `faithful-core` and `oldfaithful-settings`. On a server, in `mods/` with those two. Clients need nothing: it adds no content and says nothing on the network. Its core must be of the same library version or later.

`./gw :library:stagehand:remapJar` builds it in `library/stagehand/build/libs/`; `./gw prismMoreStagehandInstance` builds a whole test pack with it on, More Faithful's dev pack as `local/prism/MoreFaithful-dev-stagehand.zip`, to import in Prism.

## Scripts

A text file, UTF-8, a command a line, `#` starting a comment line. Numbers are the game's own. `~` is an offset from the player's cell (`~3`, `~-2`), `@me` is `~ ~ ~`.

| Command | |
|---|---|
| `tp <x> <y> <z> [dim=overworld\|nether] [yaw=] [pitch=] [exact]` | Moves `@me`. |
| `tp <x> <z>` | The same, to the top of that column. |
| `tp <kind> [words] [key=value...]` | To a named place: `tp spawn`, `tp player Alice`, `tp tower nearest floor=3` with Buried Towers there (floors count from the top, 0 being the top one; no floor is just outside the gate; the result says the floor's kind and its `way` down). |
| `locate <kind> [words] [key=value...]` | Where that is, and how far. |
| `give <item>[@damage] [count] [wear]` | Into the inventory; `wear` puts armour on. Beta's by name, `iron_sword`, `wool@14`; a mod's whole, `water-bottles:water_bottle`. `redstone_torch` is the one a player crafts (beta's lit block); `unlit_redstone_torch` is beta's dark one, which stays dark where it's placed until something beside it changes. |
| `kit <name>` | `wood`, `stone`, `iron`, `diamond`, `builder`; `list kits` says what's in them. |
| `heal [n]` / `health <n>` | Half-hearts, and the first also puts out fire and fills the air. |
| `clear [all\|items\|armor]` | The inventory. |
| `time [dawn\|day\|noon\|dusk\|night\|midnight\|<0-23999>]` | Forward to the next such hour; alone it says the hour. |
| `weather [clear\|rain\|thunder] [how long]` | Alone it says the weather. |
| `spawn <entity> [count] [at <x> <y> <z>] [exact] [dim=]` | In front of `@me` unless told. Beta's mobs, or a mod's whole name. |
| `setblock <x> <y> <z> <block>[@meta] [dim=]` | |
| `fill <x> <y> <z> <x> <y> <z> <block>[@meta] [dim=]` | Up to 65536 cells, a slice a tick. |
| `say <text>` | A chat line to `@me`, split as chat needs. |
| `wait <40\|5s\|2m>` | World ticks: none go by while single player is paused. |
| `wait until near <x> <y> <z> [radius=4] [timeout=10m] [dim=]` | Until `@me` is there. `wait until <kind> ...` for a named place. A timeout ends the script as a failure. |
| `status [radius=32]` | Position, health, inventory and armour by name, what they stand on, the biome, mobs nearby, hour, weather, seed. |
| `as <name>` | Who `@me` is from here on. |
| `cancel [id]` | Ends another running script, or all others. |
| `list items\|blocks\|kits\|places\|entities` | What can be named. |

There's no command for world rules, locked or not. `weather` under World Rules' weather rule doesn't take, and its result says so.

**`@me`** is the only player here: in single player that's always so. On a server with several, `as <name>` at the top of the script, or the helper's `--as`, and a script without one fails saying who's there.

**Landing.** Without `exact`, a teleport lands on the nearest safe cell within 16 blocks of what was asked (something to stand on, shallow water included, room for the head, no lava or fire at hand), or failing that the top of a nearby column, treading water only if there's no ground at all. `moved_for_safety` in the result says it had to. With nowhere to stand it fails and they stay put. A vehicle is left, a sleeper woken, a screen closed if it's changing dimension. On a server the step ends once their client has the ground under it and has answered the move, as a second move sent before that can get them kicked for moving too quickly, and the chunks of a long move go out from the middle, as a change of dimension sends them, not in the game's rows from a far corner.

**Paused.** Alt-tabbing to talk to a session pauses single player, so scripts run from its menu too, the Nether included. What needs time to pass (`wait`) holds until the game goes on. So a session can move someone while they're away from the window, and they arrive when they come back.

**Bad input.** A script is read whole first: any line that can't be understood fails it with nothing run, and `errors` lists every such line. What can only be found out running (nobody by that name, nowhere safe) stops the script there, and `not_run` counts the rest. A script doesn't run on a game at its menus (`no_world`), or on a client joined to a server (`on_a_server`: it belongs in the server's folder), and one that lay unread over ten minutes `expired`, so it can't move someone out of the blue. What crashes beta when it's made by hand is refused: dye and slab damage past their tables, a piston's meta past its six facings, a piston's head and moving block by name.

## By hand

`stagehand/in/<id>.txt` in the game's folder; write it elsewhere and move it in, or finish within 250 ms. The folders may need making; the mod looks once a second. `stagehand/out/<id>.json` appears when it's taken, replaced whole as each line finishes:

```json
{
  "id": "20261106-101500-a1b2c3",
  "state": "done",
  "ok": true,
  "where": "single player",
  "started_ms": 1793000000000,
  "finished_ms": 1793000000050,
  "steps": [
    {
      "line": 1,
      "command": "tp 100 70 100",
      "ok": true,
      "data": {"asked": {"x": 100.0, "y": 70.0, "z": 100.0}, "x": 100.5, "y": 71.0, "z": 100.5, "dim": "overworld", "standing_on": "grass_block", "moved_for_safety": true}
    }
  ]
}
```

`state`: `running` (with `waiting_on`, and `paused` if so), `done`, `failed`, `cancelled`, `stopped` (the world closed), `refused`, `expired`, `lost` (the game ended without a word: it's marked when a later game next sees a script). `error` has `code`, `message` and `line`. Results over three days old are removed. Idle the mod looks at one folder once a second on the tick, and writes nothing until a script comes.

## A mod's named places

A mod answers for a kind of place through an entrypoint, without depending on Stagehand: a game without Stagehand never loads the class.

```json
"entrypoints": { "stagehand:places": ["org.studioquire.faithful.mymod.MyPlaces"] }
```
```groovy
compileOnly project(':library:stagehand')
```
```java
public final class MyPlaces implements Places {
	@Override public String kind() { return "tower"; }

	@Override public Place find(Query query) {   // tp tower nearest floor=3: words ["nearest"], options {floor: 3}
		...
		return new Place(Worldgen.OVERWORLD, x, y, z).about("floor", 3);
	}
}
```

`find` runs on the world's thread, from `query.world` and the asker's cell, and returns the cell for the feet, or null if there's none. Nothing needs to exist yet: Stagehand loads the chunks around the answer before moving anyone. An `IllegalArgumentException`'s message goes back to the script.

## Tests

`./gw :harness:librarySelftest -Plibrary.mods=stagehand,fixture-machine`: scripts through the real folder, on single player and a real server with a hidden client, including new land 3000 blocks out and the Nether, both ways, with no portal built.
