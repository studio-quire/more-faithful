# Changelog

The Faithful Library's releases, newest first. One version number for Faithful Core and the library's mods; Faithful
Settings, beside them, is Old Faithful's. Through 0.4.0 it was the library packs' and their server packages' number too,
and each entry named the edition releases the packs were built on. The pack with every mod on is now
[More Faithful](https://github.com/studio-quire/more-faithful), with its own changelog. Entries say what a player or
server owner notices.

## 0.5.0 (2026-11-10)

Faithful Settings is Old Faithful 0.2.4's, as in 0.4.0.

- **The packs have moved.** The library's pack on Still Faithful is now
  [More Faithful](https://github.com/studio-quire/more-faithful), with releases of its own, and its pack on Old
  Faithful is gone: each of our packs is now the one before it with more (Pure, Old and Still Faithful, then More
  Faithful). This repository's releases hold the mods, Faithful Core and Faithful Settings as jars on their own. For the
  mods on Old Faithful without Still Faithful's changes, put them into an Old Faithful instance. An instance from the
  old pack on Old Faithful keeps working: to update its mods, replace each jar in its `.minecraft/mods-library`,
  `faithful-core` included, with the new one under the old name (`alchemy-0.5.0.jar` becomes `alchemy.jar`). Nothing
  about the mods changed: worlds carry over as they are.
- **Faithful Core** names the pack it came in: in More Faithful, while any of its mods is on, the title screen and the
  settings in Options say More Faithful. The pack carries `mods-library/pack.properties` saying so; mods put into an
  instance by hand bring none, so the game goes on naming its edition there.

## 0.4.0 (2026-11-10)

Library packs for Old Faithful 0.2.4 and Still Faithful 0.6.0, on Pure Faithful 0.1.5. From Still Faithful 0.6.0 (in
its pack): skeletons shoot from their eyes, so they can now hit you under a ceiling two blocks high; squid swim on just
under the surface instead of bouncing against it; and blasts and small things show from further away. Its changelog
has the detail. Two new mods, on in the packs and server packages, and a new rule in World Rules:

- **World Rules**: a new rule, **Bows Wear Out**, off to start with. On, a bow lasts 385 shots, as when bows first wore
  out in 1.0, whichever arrows it shoots, with a bar under it showing its wear like a tool's, and Item Repair makes one of two worn
  bows. Turned off, worn bows look new and wear no more, and show their wear again when it's back on. A worn bow makes a
  dispenser as a new one does.
- **Wary Creepers**, a new mod: creepers keep away from a jack-o'-lantern whose lit face they can see, within 8 blocks,
  and come back once they no longer see one in reach. A creeper coming from behind a lantern isn't scared, nor one with a
  wall or glass in the way, and one already at your side still swells. It runs where the world runs, so a server can
  use it with any client.
- **Wolf Packs**, a new mod: your tamed wolves take on a different monster each, and the next when theirs dies, where
  beta sent them all at one; they go for a monster after you within 4 blocks, or a skeleton shooting at you, before it
  hurts you; they fight creepers, which beta's never did; and they eat fish as well as pork, each food healing a wolf
  what it heals you, over time with Hearty Food. Each is a world rule. It runs where the world runs, so a server can
  use it with any client.
- **Stagehand**: `give redstone_torch` gives the redstone torch a player crafts, lit. It gave beta's unlit one, which
  stays dark where it's placed until something beside it changes; that one is `unlit_redstone_torch` now.
- **Faithful Core**, for writing mods: `Creepers.onFright`, for a mod to say what frightens a creeper. Every such mod's
  creepers flee one way, the core's, so two mods never pull a creeper two ways
  ([docs/library-authoring.md](docs/library-authoring.md)).

## 0.3.0 (2026-11-09)

Library packs for Old Faithful 0.2.4 and Still Faithful 0.5.1, on Pure Faithful 0.1.5. From Old Faithful 0.2.4 (in
both packs): saving is faster in a world with many blocks waiting to update. Their changelogs have the detail. One new
mod, on in the packs and server packages, and more in three others:

- **Blue Flowers**: its flower is called Bluebell, and beta's yellow flower Dandelion where beta said Flower. A switch on
  Blue Flowers' page in the settings keeps Flower. Only the names you see change: worlds and servers are as they were.
- **Explosive Arrows**, a new mod: an arrow and two gunpowder make an Explosive Arrow, which bursts where it lands, on a
  block or a mob, with a small blast of beta's own, half a creeper's in size. A direct hit takes over half a zombie's
  health, and things a block or two off lose a couple of hearts. It breaks no blocks, but TNT it reaches goes off, and it
  hurts you too if you fire too close. Its puffs show as far off as you can hear the bang, where beta draws a blast's
  puffs only within 16 blocks of you; other blasts stay as beta draws them. A bow shoots whichever arrows come first in
  your inventory, the hotbar from the left first, so where you keep them decides; dispensers shoot them too. Every player
  on a server needs it, as with Water Bottles.
- **Alchemy** brews three more kinds of potion, each in three sizes as health has, from three more golden things made
  as the Golden Rose is: a Golden Dandelion, a Golden Bluebell (from Blue Flowers' bluebell) and a Golden Feather.
  **Ward** is a set of armour in a bottle: it turns the whole armour bar blue over whatever you wear, and hits wear it
  away, the blue draining, before your own armour takes any wear; the bigger the potion, the longer it lasts. **Haste**
  makes you walk and mine 20% faster, and **Feather** makes a fall hurt a third as much, for 4, 8 or 12 minutes,
  their bottles draining beside your figure in the inventory while they run. A
  golden apple now gives a strong Ward Potion's ward as well as its full heal. **Health potions now heal 3, 6 or 10
  hearts**, where they healed 4, 6 or 8. The Golden Rose is redrawn to match the new golden things.
- **Buried Towers**: the spawner guarding the vault at the bottom of a tower makes zombies, where it made skeletons, in
  towers made from now on. Towers already in your worlds keep theirs.

## 0.2.4 (2026-11-09)

Library packs for Old Faithful 0.2.3 and Still Faithful 0.5.0, on Pure Faithful 0.1.5. The packs now carry those
releases; the library's own mods are unchanged. From Still Faithful 0.5.0 (in its pack): wooden tools, bows, fishing
rods, bowls, signs, wooden doors, ladders, boats and wooden slabs burn in a furnace, as in later versions. Its changelog has
the detail.

## 0.2.3 (2026-11-09)

Library packs for Old Faithful 0.2.3 and Still Faithful 0.4.1, on Pure Faithful 0.1.5, as in 0.2.2. One new mod, on
in the packs and server packages.

- **Wild Saplings**, a new mod: saplings grow every tree that grows wild, so a forest you cut down can be planted again.
  A spruce sapling grows a tall, thin pine one time in three, as a third of Taiga's trees are. An oak sapling grows a
  big oak as often as the wild oaks where it's planted are big: one time in three in Forest and Rainforest, one in ten
  elsewhere, as in beta. And its big oaks are as full as wild ones, where beta's had smaller clumps of leaves. It runs
  where the world runs, so a server can use it with any client, vanilla too.

## 0.2.2 (2026-11-09)

Library packs for Old Faithful 0.2.3 and Still Faithful 0.4.1, on Pure Faithful 0.1.5. The packs now carry those
releases; the library's own mods are unchanged. From Still Faithful 0.4.1 (in its pack): its Skeleton Arrows switch,
off unless you turned it on, is gone. Its changelog has the detail.

## 0.2.1 (2026-11-09)

Library packs for Old Faithful 0.2.3 and Still Faithful 0.4.0, on Pure Faithful 0.1.5. The packs carry those
releases: from Pure Faithful, servers that make new land faster; from Old Faithful, mobs that stop being solid as they
die; and from Still Faithful (in its pack), chests that pair as in later versions with one lid, shift-click that fills
dispensers and furnaces and puts armour on, and skeletons that can shoot from their eyes (off unless turned on).
Their changelogs have the detail.

- **Buried Towers** works out its towers in less time as new land is made, so a new tower holds the game up for less
  of a moment, the first of each game most of all. Every tower comes out exactly as before: towers in your worlds
  carry on as they were.

## 0.2.0 (2026-11-08)

Library packs for Old Faithful 0.2.2 and Still Faithful 0.3.0, on Pure Faithful 0.1.4. Six new mods, and the packs
carry those releases: squid that swim and a fix for a phantom extra item after closing a chest on a server from Old Faithful, fences
shaped as in later versions and short chat bars from Still Faithful (in its pack), and a fix for a rare crash making a
new server world from Pure Faithful. Their changelogs have the detail.

- **Buried Towers**, a new mod: ruined towers stand on the land about every 24 chunks, and go a long way down: floors
  somebody built and time got to, and ways down worth finding. They replace beta's small dungeons in new chunks. A
  world chooses its towers when it's created, with the rule Towers on Create World's World Rules... page: Simple (the
  default), crude stumps of cobble and moss like beta's dungeons, or Complex, big towers of dressed stone. A server
  running it needs every player to have it.
- **Better Dungeons is gone.** Buried Towers replaces it. Rooms it built stay in your worlds as ordinary blocks, and worlds
  carry on as they were. Its id is retired; a pack's list that still names it just drops the line.
- **Hearty Food**, a new mod: food heals half as much again, at half a heart a second instead of at once, and pale hearts
  show where you'll end up. Eating is one click as ever. More food adds to what's coming, a hit doesn't
  cancel it, and it helps even mid-fight. A golden apple still heals everything at once. Cake isn't eaten once a full
  bar is on its way. It runs where the world runs: any client can join a server with it, and only a client with it too
  sees the pale hearts.
- **Stagehand**, a new mod as a jar of its own, in no pack or server package: scripts dropped into a folder in the game's
  directory set up a play-test from outside it (teleports, items, time, weather, mobs, blocks).
- **Alchemy**, a new mod: gild a rose, then brew it. Four gold ingots around a rose at the bench make a Golden Rose. A
  water bottle with one, two or three of them makes a Weak Health Potion, a Health Potion or a Strong Health Potion, in
  the inventory's grid too. Drinking heals 4, 6 or 8 hearts at once and leaves the bottle; at full health it isn't
  drunk. A server running it needs every player to have it. It needs Water Bottles.
- **Blue Flowers**, a new mod: a blue flower in patches across Taiga and Tundra, as dandelions grow in warmer land. New
  chunks only. It's placed and picked like the rose and dandelion, and makes two light blue dye. Bonemeal on grass in
  those biomes can grow one too. A server running it needs every player to have it.
- **Item Repair**, a new mod: two worn tools, weapons or pieces of armour of the same kind in the crafting grid make
  one, with what was left of both, up to as good as new, as in later versions. A world rule, Repair Bonus, off to
  start with, adds a twentieth of a new one's durability on top. It runs where the world runs: any client can join a
  server with it and mend there.
- **World Rules**: Monster Light's second choice is called Dark Only (it was None): any torch or lava light stops
  monsters appearing.
- Locked rules: while a world's rules are locked (or the server's you're on), the Mods menu doesn't switch off a mod
  that has rules (World Rules, Buried Towers, Item Repair); from the title screen it does. The game no longer says how
  to unlock them; the README still does.
- When a server refuses you over its mods, the screen names the mods as you know them, says which the server needs and
  which to turn off to join, and where the log is. Its button is now **Match This Server's Mods**.
- Faithful Core: fixed a rare crash when a server makes a new world, about once in 900 (stopped at "Preparing level"),
  on any server running the library, with or without Pure Faithful's mods.

## 0.1.4 (2026-11-06)

Library packs for Old Faithful 0.2.1 and Still Faithful 0.2.2, on Pure Faithful 0.1.3. The packs now carry those releases;
the library's own mods are unchanged.

From Old Faithful 0.2.1, each on its own switch, under Fixes > Players and Vehicles:

- With a block low overhead (a roof 2 above the floor, say), a step could sink you about a third of a block into the
  floor or the block ahead, and you were shoved back out. A step now comes down only as far as it went up. Mobs sank
  the same way and no longer do (Steps Under Roofs).
- Rejoining a world right against a wall, a rounding slip could put you a hair inside it. You then walked into the wall,
  suffocating if it reached your eyes, until you were pushed out. The sides of your box now stop at the wall (Rejoining
  by Walls).

The Rejoining Worlds switch (`world.rejoinTerrain`) is gone: in our tests it made no difference a player would notice,
and a config file that still names it loads as before.
[Old Faithful's changelog](https://github.com/studio-quire/old-faithful/blob/main/CHANGELOG.md) has the correction that
goes with this.

We can't show that these two fixes cover every time you've been moved into a block and pushed out. If it happens again,
please tell us in an issue where you were and whether anything was overhead.

## 0.1.3 (2026-11-06)

Library packs for Old Faithful 0.2.0 and Still Faithful 0.2.1, on Pure Faithful 0.1.3.

- **World Rules**: a new rule, Blasts Destroy Items. On, as in beta, explosions of any kind destroy items lying on the
  ground. Off, they only throw them.

## 0.1.2 (2026-11-06)

Library packs for Old Faithful 0.2.0 and Still Faithful 0.2.1, on Pure Faithful 0.1.3.

- The server packages now start with every server-side mod on in `mods/`, to match the packs. With Water Bottles on, a new
  server refuses players without it, vanilla clients and the plain editions' packs included. Take it out of `mods/` before
  the first start to keep such a server open to them.

## 0.1.1 (2026-11-06)

Library packs for Old Faithful 0.2.0 and Still Faithful 0.2.1, on Pure Faithful 0.1.3.

- The packs now start with every mod on. Each still has its Enabled switch in the settings' Mods menu. The server packages
  are as they were: a server turns on what its owner moves into `mods/`.
- The packs' icons are now the editions' own seals.

## 0.1.0 (2026-11-05)

The first release, a pre-release. Library packs for Old Faithful 0.2.0 and Still Faithful 0.2.1, on Pure Faithful 0.1.3,
with every mod off until you turn it on.

- **World Rules**: rules for each world: Mobs Break Blocks, Keep Inventory, Sleeping, Day Length, Weather, Natural
  Monsters, Monster Light, Apple Drops, Falling Hit Bonus. A world's rules can be locked. Runs where the world runs, so
  a server can use it with any client.
- **Better Dungeons**: beta's dungeons can grow, with corridors to more rooms, in new chunks. Runs where the world runs.
- **Water Bottles**: glass bottles to fill at any water and drink from for nothing. A server running it needs every
  player to have it.
- **Plain F3**: the F3 screen without position, facing and entity counts. Client only.
- **No Toasts**: no achievement or tutorial pop-ups. Client only.
- **Faithful Core**, what the mods build on, and an API for writing more ([docs/library-authoring.md](docs/library-authoring.md)).
