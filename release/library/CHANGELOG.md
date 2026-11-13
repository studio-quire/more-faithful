# Changelog

The Faithful Library's releases, newest first. One version number for Faithful Core and the library's mods; Faithful
Settings, beside them, is Old Faithful's. Through 0.4.0 it was the library packs' and their server packages' number too,
and each entry named the edition releases the packs were built on. The pack is now
[More Faithful](https://github.com/studio-quire/more-faithful), with its own changelog. Entries say what a player or
server owner notices.

## 0.9.0 (2026-11-13)

Faithful Settings is Old Faithful 0.2.7's.

- **Golems**, a new mod: iron workers you make from two iron blocks and a Golem Core. Hand one a pickaxe, an axe, a hoe,
  a sword or a bow and it levels the land (what you've built of stone, sandstone or dirt too), fells and replants trees,
  keeps a wheat field, fights monsters with a sword or shoots them with a bow, creepers too, at half your pace, inside
  four Boundary Poles or 16 blocks round where it stands, carrying what it gathers to a chest nearby. A guard walks its
  rounds when there's nothing to fight. Boundary Poles glow while a golem works inside them. Its chest also gives it
  spare tools, seeds, saplings and arrows, and iron ingots to mend itself with when its cracks show. It adds an item, a
  block and a creature, so a server running it needs every player to have it.
- **Faithful Settings** (Old Faithful's, beside the library's mods) no longer has the switches for the jars in
  `.minecraft/mods-library`: they're [More Faithful](https://github.com/studio-quire/more-faithful)'s own now, in its
  pack, with its new presets. With the jars on their own nothing else changes, but take `faithful-core` 0.9.0 with it:
  an older core beside this Settings stops the game when a server refuses you over mods. In a pack from before More
  Faithful 0.9.0, keep the pack's own Settings, which has the switches (the README's Installing).
- A jar you add to a More Faithful instance by hand (Wary Creepers, say) is switched on in the Mods menu, where it shows
  once it's in `.minecraft/mods-library`, rather than by a line added to `selection.txt`, which the game now keeps to the
  preset and switches you chose.

## 0.8.0 (2026-11-12)

Faithful Settings is Old Faithful 0.2.6's, as in 0.7.1.

- **Blue Flowers**: its flower is the Lupin now, redrawn, and its dye takes the place of lapis lazuli. A lupin makes two
  **Lupin Blue**, where the bluebell made light blue dye. Lupin Blue is beta's blue dye, the item that was Lapis
  Lazuli, with a new name and picture, as Rose Red and Dandelion Yellow are named for their flowers: it dyes wool and
  sheep and mixes as it always did, and light blue is Lupin Blue and bone meal, beta's own recipe. Lupins grow wild in
  Highland too, in new chunks: our name for the land beta calls Swampland, which in beta has no swamp in it (that came
  in later versions), only bare, steep, clover-green hills that occur beside the snow. Bonemeal on grass grows a lupin
  in any biome, as it grows roses and dandelions, where it was Taiga and Tundra only. With the mod on there's no lapis
  lazuli ore and no Lapis Lazuli Block: no ore forms in new chunks, and the block can't
  be made or broken down into nine. **In a world you already have, lapis lazuli ore turns to stone and Lapis Lazuli
  Blocks turn to blue wool, for good**, as each part of the world is next loaded with the mod on; nothing else there
  changes. Blocks you've built with become blue wool where they stand, which keeps their colour but burns, as wool
  does. Blocks you hold or keep in a chest become blue wool too, one for one, so the nine lapis lazuli in each is
  gone: break stored blocks down into lapis lazuli before you update, as it can't be done after. Your bluebells are
  lupins, and lapis lazuli you hold is Lupin Blue. To keep beta's lapis lazuli, leave Blue Flowers out, from before you
  first open the world with this version. In a world that had Blue Flowers before, the bluebells then become roses,
  and no new Golden Lupins can be made for Ward Potions (the README's Turning a mod off or removing it).
- **Buried Towers**: no tower has lapis lazuli in it. The only lapis lazuli a tower ever held was a little ore in the
  rock of a mine deep down, in about one tower in a hundred, and a new tower has stone there, or the ore that was under
  it. Towers are otherwise the same, block for block, and one already standing keeps what it has, unless Blue Flowers
  is on, which turns that ore to stone like any other lapis lazuli ore.
- **Alchemy**: the Golden Bluebell is the Golden Lupin, redrawn over the lupin. The same item, and Ward Potions are
  as they were.
- **Faithful Core**, for writing mods: `Content.itemTexture`, a sprite of a mod's own for an item that isn't its own,
  as Blue Flowers draws Lupin Blue. Blue Flowers 0.8.0 needs this core, so update the core with it.
- **Pumpkins**, a new mod: pumpkin pie, and pumpkins you can grow. A pumpkin, sugar and an egg make a pie, anywhere
  they fit, which heals as a cooked porkchop does. Under a new world rule, **Pumpkins Spread**, a pumpkin on grass or
  dirt now and then grows another on open grass near it, until there are about six round it, so a wild patch, or a
  pumpkin you place, becomes a patch you can harvest and leave one of to grow back; from one pumpkin it takes a few
  in-game days spent near it. Beta's pumpkins never grew. The rule is off in worlds you already have, and with the mod
  on its own in new worlds too: switch it on from the mod's page with the world open, or on Create World's World
  Rules... page. It adds an item, so a server running it needs every player to have it.
- **Snipping**, a new mod: shears snip a plant that grows by itself so it stays as it is: a sapling that should never
  become a tree, wheat kept at its stage, a mushroom that shouldn't spread, sugar cane and cactus at the height you
  want them, and with Pumpkins a pumpkin that shouldn't. A snip costs the shears a use; nothing shows it, and shears on
  a snipped plant do nothing, which is how you tell. Break and plant it again and it grows. It runs where the world
  runs, so a server can use it with any client.

## 0.7.1 (2026-11-11)

Faithful Settings is Old Faithful 0.2.6's, unchanged since 0.2.4's but for its version.

- **Buried Towers**: cocoa beans turn up in the towers' chests, among the food and supplies of the upper floors, a bean
  or a few at a time, in about two towers in three. Beta's only cocoa beans were in its dungeons' chests, and the towers
  take the dungeons' place, so land made with the mod had none, and with that no cookies and no brown dye. Towers in
  land already made keep the chests they have: the beans are in towers made from now on.

## 0.7.0 (2026-11-11)

Faithful Settings is Old Faithful 0.2.5's, unchanged since 0.2.4's but for its version.

- **World Rules**: a new rule, **Pickup Reach**, on to start with, and in worlds you already have too. You pick up
  items, and arrows lying about, from half a block above and below you as well, so something on top of a two-block
  step beside you comes to you without a jump, and riding a boat, a minecart or a pig, from all round what you ride,
  as in later versions. Beta picked up only what was within a block of you across and between your feet and the top
  of your head. Turn it off on World Rules' page for beta's reach. It's the one World Rules rule that doesn't start as
  beta. On a server it works for players with any client.
- **Feather Lure**, a new mod: a fishing rod and a feather make a Lured Fishing Rod, anywhere they fit, as worn as the
  rod was, and fish bite on it twice as often, in rain as well; its bobber shows a small feather over the hook. It wears
  as a fishing rod does, and each fish you catch has a 1 in 8 chance of taking the feather: bits of it fly off at the
  end of your line, and you're left holding a plain fishing rod, just as worn. With Item Repair, two lured rods mend
  into one, but a lured rod and a plain one don't. It adds an item, so a server running it needs every player to have
  it.
- **Faithful Core**, for writing mods: the hand draws an item from the sheet of the tile its holder picks for it
  (`getItemStackTextureId`), where it took the item's own, so a mod's item can show one of beta's tiles in the hand, as
  Feather Lure's rod shows beta's cast rod while its line is out. On an older core that rod shows the wrong picture,
  or none, while it's cast, so update the core with it.

## 0.6.0 (2026-11-10)

Faithful Settings is Old Faithful 0.2.4's, as in 0.5.0.

- **Faithful Core**: a pack can choose what its new worlds start with, and More Faithful now does. The pack's choices
  are `rule.<mod>.<rule>` lines in its `pack.properties` (in `mods-library/` in a pack, beside the start scripts in a
  server package). The first time a game or server meets a rule, it takes the pack's choice for it, or with no pack the
  rule's own start, and keeps it in `config/faithful-rules.properties`; from then on that line is yours, changed on
  Create World's World Rules... page, and no update changes it. That file now has a line for every rule of the mods you
  have on, where it had lines only for the rules you'd changed. With the mods on their own nothing changes: there's no
  pack file, so every rule starts as its mod has it.
- **World Rules**: its description no longer says a world nobody changed plays as beta, since More Faithful now
  starts its new worlds with rules of its own, and its list of the rules has Blasts Destroy Items, which it had left
  out. On its own, every rule still starts as beta's.
- **World Rules**: the **Day Length** rule is gone. It made a day two or three times as long, and it had faults that
  weren't worth what mending them would take: on a server with a longer day, a player without the mods saw the light
  step back and forth at dusk and dawn. A world that had a longer day opens as before and has beta's day again: the
  sun goes to where the world's own time puts it, so the hour jumps once, and nothing else in the world changes. The
  rule's line in the world's `faithful-world.properties` is no longer read.
- **Faithful Core**: if you update jars one at a time, update Faithful Core with whichever mod you update, and World
  Rules and Stagehand together with the core: with Explosive Arrows 0.6.0 on an older core the game doesn't start; nor
  does it with this core and a World Rules of 0.5.0 or before; and an older Stagehand's `time` and `status` fail.
  For mods written against the core, `Days` (the day's length and the sun's hour) is gone with the
  Day Length rule; the hour is `world.getTime() % 24000`, as in beta.
- **Faithful Core**, for writing mods: `Arrows`, where a mod's arrow says what it flies as, for anything of a mod's own
  that shoots one (Explosive Arrows' arrow is there), and `Tools.canHarvest` and `Tools.breakProgress`, a player's
  digging worked out for a miner that isn't a player ([docs/library-authoring.md](docs/library-authoring.md)).
- **Wolf Packs**: sitting wolves keep watch, a new rule, **Sitting Wolves Watch**, on to start with, in worlds you
  already have too. A sitting wolf goes for a monster it sees within 8 blocks, follows it no further than 16 from its
  spot, then walks back to where it sat and sits down again, whether its owner is there or not (if it can't get back,
  it sits where it ends up). It doesn't see through a wall, glass or leaves, sees over a fence only from a little way
  back, and takes on only what it sees from its seat. It leaves a spider alone by day and a calm pig zombie. A creeper
  bitten by a wolf on watch turns on that wolf and blows up beside it, which breaks what's round the wolf too unless
  World Rules' Mobs Break Blocks is off (one bitten by a wolf at your side still keeps coming for you). A wolf with no
  way to a monster it sees stays sitting and growls. A sitting wolf hit by a monster or another player, or that gets
  up because something hurt you, now goes back to its spot when that fight is over, where beta's stayed up and
  followed you. Turned off, a sitting wolf is beta's.
- **Wary Creepers** is no longer in More Faithful, where sitting wolves keep a place instead. The mod is unchanged and
  still here as a jar; the README says how to put it in a More Faithful instance.

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
