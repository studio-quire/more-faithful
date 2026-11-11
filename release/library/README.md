# Faithful Library

**optional mods for beta 1.7.3**

Mods for Minecraft beta 1.7.3 on Fabric, and Faithful Core, the API they're built on: for your own setup, and for writing
mods of your own. Any combination works, in single player and on servers. Open source, from
[Studio Quire](https://studioquire.org/).

To play them together, each with its own switch, take [More Faithful](https://github.com/studio-quire/more-faithful):
[Still Faithful](https://github.com/studio-quire/still-faithful) with the mods here on, all but Wary Creepers and
Stagehand, one download to import into Prism Launcher. Through 0.4.0 this repository's releases held the packs: the one on Still Faithful is More Faithful now,
and the one on Old Faithful has gone, as each pack now builds on the one before. For the mods without Still Faithful's
changes, install them on their own in an [Old Faithful](https://github.com/studio-quire/old-faithful) instance (below).

They're mods, not a modified game: your launcher downloads Minecraft itself from Mojang, with your own account.

## The mods

- **World Rules**: rules for each world, on the mod's page in the settings and on Create World's World Rules... page:
  Mobs Break Blocks, Blasts Destroy Items, Keep Inventory, Sleeping, Weather (normal, always clear, always
  rain or snow), Natural Monsters, Monster Light (monsters appear in dim light as in beta, or not in any torch or lava
  light), Apple Drops (oak leaves, 1 time in 200), Falling Hit Bonus (beta's, only when really falling, or none) and Bows
  Wear Out (a bow lasts 385 shots, any arrow, as when bows first wore out in 1.0, with a bar showing its wear like a tool's; turned off,
  worn bows look new and keep their wear for when it's back). With the mod on its own, a world nobody changed plays as beta; [More Faithful](https://github.com/studio-quire/more-faithful) starts its new worlds with rules of its own. **Lock Rules** locks a world's rules so nothing in the game changes them; on a
  server, `/rule lock`. While they're locked, the Mods menu doesn't switch off a mod that has rules. Taking
  `rules-locked=true` out of the world's `faithful-world.properties`, with the game closed, unlocks them.
- **Buried Towers**: ruined towers stand on the land about every 24 chunks, and go a long way down, to about bedrock:
  floors somebody built and time got to, a different way down from each, in beta's own blocks. They take the place of
  beta's small dungeons in new chunks, and don't cut into land already made. A world chooses its towers when it's
  created, with **Towers** on Create World's World Rules... page, and keeps them: **Simple** (the default), crude stumps
  of cobble and moss like beta's dungeons, about 12 blocks across, or **Complex**, big towers of dressed stone, about
  23 across. (Better Dungeons, which this replaces, is gone: rooms it built stay in your worlds as ordinary blocks.)
- **Water Bottles**: glass bottles you fill at any water and drink from for nothing. Three glass in a V at
  the bench makes three. An ingredient for mods to come.
- **Hearty Food**: food heals half as much again, at half a heart a second instead of at once, and pale hearts show where
  you'll end up. Eating is one click as ever. More food adds to what's coming, a hit doesn't cancel it,
  and it helps even mid-fight. A golden apple still heals everything at once. Food past a full bar is wasted, as in beta.
- **Alchemy**: gild a flower or a feather, then brew it. Four gold ingots around a rose, a dandelion, a bluebell or a
  feather at the bench make a Golden Rose, Golden Dandelion, Golden Bluebell or Golden Feather. A water bottle with one,
  two or three of the same golden thing makes a weak, plain or strong potion of its kind, in the inventory's grid too, and
  drinking one leaves the bottle. Health Potions (roses) heal 3, 6 or 10 hearts at once. A Ward Potion (bluebells) is a
  set of armour in a bottle: it turns the whole armour bar blue, over whatever armour you wear, and protects as a full
  set does. Hits wear it away as they wear a set, the blue draining from the bar, and your own armour takes no wear
  until the blue is gone, then shows again as it was. The bigger the potion, the tougher the set: a weak one lasts about
  three zombie hits, a plain one five and a strong one nine. Another over a worn one adds what was left to it, up to a
  strong one's, and a golden apple gives a strong one's ward as well as its full heal. Haste Potions (dandelions) make
  you walk and mine 20% faster, and Feather Potions (feathers) make a fall hurt a third as much, for 4, 8 or 12
  minutes; another adds its time, up to 12 minutes. While one runs, its bottle stands beside your figure in the
  inventory, draining as the time goes, in the order you drank them. A potion isn't drunk when there's no room for any
  of it. Bluebells
  come from Blue Flowers: without it there are no Ward Potions. Alchemy needs Water Bottles, which comes on with it.
- **Blue Flowers**: bluebells in patches across Taiga and Tundra, as dandelions grow in warmer land. New chunks only.
  A bluebell is placed and picked like the rose and dandelion, and makes two light blue dye. Bonemeal on grass in those
  biomes can grow one too. It also calls beta's yellow flower Dandelion, where beta said Flower; its page in the
  settings has a switch to keep Flower.
- **Wild Saplings**: saplings grow every tree that grows wild, so a forest you cut down can be planted again. A spruce
  sapling grows a tall, thin pine one time in three, as a third of Taiga's trees are. An oak sapling grows a big oak as
  often as the wild oaks where it's planted are big: one time in three in Forest and Rainforest, one in ten elsewhere,
  as in beta. And its big oaks are as full as wild ones, where beta's had smaller clumps of leaves.
- **Explosive Arrows**: an arrow and two gunpowder make an Explosive Arrow, in the inventory's grid too. It bursts where it
  lands, on a block or a mob, with a small blast of beta's own, half a creeper's in size: a direct hit takes over half a
  zombie's health, and things a block or two off lose a couple of hearts. It breaks no blocks, but TNT it reaches goes
  off, and it hurts you too if you fire too close. Its puffs show as far off as you can hear the bang, where beta draws a
  blast's only within 16 blocks of you. A bow shoots whichever arrows come first in your inventory, the hotbar from the
  left first, so where you keep them decides. Dispensers shoot them too.
- **Wolf Packs**: your tamed wolves take on a different monster each. A fight starts when you hit something or
  something hurts you, as in beta, or when a monster after you comes within 4 blocks of you or a skeleton shoots at you;
  then each wolf goes for whichever monster after you or your wolves has the fewest wolves on it, what you hit first,
  and when its own dies it takes the next, so a pack works through a group. A wolf that can't reach its monster gives up
  on it after a while and comes back. Wolves fight creepers too, where beta's left them alone, and a bitten creeper
  keeps coming for you, so keep your wolves between you and it.
  A sitting wolf keeps watch over the spot where it sits. It goes for a monster it sees within 8 blocks, follows it no
  further than 16 blocks from its spot, then walks back and sits down again, whether you're there or not; if it can't
  get back, it sits where it ends up. It doesn't see
  through a wall, glass or leaves, and it sees over a fence only from a little way back; grass and flowers hide
  nothing from it. It takes on only what it sees from its seat, so where you sit it decides what it guards. It leaves
  a spider alone by day and a pig zombie that's calm, as they'd leave you. Wolves on watch get hurt and don't heal by themselves, so a watch costs pork and fish, or wolves: a
  creeper bitten by a wolf on watch turns on that wolf and blows up beside it, which breaks what's round the wolf too
  unless World Rules' Mobs Break Blocks is off. To keep a wolf out of the fighting, sit
  it where it can see the monsters and can't get to them, up a ledge four blocks high, say (a wolf will drop three):
  there it stays sitting and growls at what it sees. A sitting wolf hit by a monster or another player, or that gets up
  because something hurt you, as in beta, goes back to its spot after that fight too. Your click stands it up for good, as ever.
  Wolves eat fish as well as pork, raw or cooked, and each food heals a wolf what it heals you (beta's healed a
  raw porkchop's worth, 1.5 hearts, whatever it was); with Hearty Food, as Hearty Food heals you, over time. A wolf at full health eats
  nothing, as in beta, and a click with its food never sits it down or stands it up.
  Each part is a world rule on its page.
- **Item Repair**: two worn tools, weapons or pieces of armour of the same kind in the crafting grid make one, with what
  was left of both, up to as good as new, as in later versions. Shears, fishing rods and flint and steel too, and bows
  in a world where World Rules' Bows Wear Out is on. A
  world rule, Repair Bonus, adds a twentieth of a new one's durability on top, as later versions did; it's off to start
  with, so mending only puts together what you had, and there's nothing to gain by saving worn things for it.
- **Plain F3**: the F3 screen keeps its frame rate and memory and loses position, facing and entity counts, and mobs lose
  the numbers beta tags them with through walls, so you find your way without coordinates.
- **No Toasts**: no achievement or tutorial pop-ups. Achievements are still earned.

**Wary Creepers** is in the release as a jar of its own and in no pack: More Faithful carried it through 0.5.0, and
has sitting wolves keep a place instead (Wolf Packs). Creepers keep away from a jack-o'-lantern whose lit face they can
see. A creeper within 8 blocks, on the side the face looks out on and with nothing in the way, turns and walks off, and
comes back once it no longer sees a lit face in reach, so creepers keep to the edge of a lantern's reach. **A creeper
coming from behind a lantern isn't scared at all**, so mind which way each one faces: it faces you as you place it. A
creeper sees it as it would see you, so not through glass, leaves or a wall. One already at your side still swells.
It's the lantern standing there that keeps a place, not you: carrying or wearing a pumpkin does nothing. To have it in
a More Faithful instance, with the game closed copy `wary-creepers-<version>.jar` to
`.minecraft/mods-library/wary-creepers.jar` and add the line `mods-library/wary-creepers.jar` to `selection.txt`
there; it then has a switch in the Mods menu like the rest.

**Stagehand** is in the release as a jar of its own and in no pack: it runs scripts dropped into a folder in the game's
directory (teleports, items, time, weather, mobs, blocks), and writes back what happened, for setting up a play-test from
outside the game. Anything that can write to that folder can command the game, so install it only if you want that.
[docs/stagehand.md](docs/stagehand.md) says how.

**Faithful Core** is what they build on, and arrives only with them: none of the editions carries it, so a game with no
library mod on never says anything new on the network. **Faithful Settings** is the settings screen they share, which
Old and Still Faithful already have.

## Installing

The release has each mod's jar, `faithful-core` and `oldfaithful-settings` on their own. In a b1.7.3 Fabric (Babric)
instance, put the mods you want in `.minecraft/mods`, with those two; Old and Still Faithful already have
`oldfaithful-settings`. We test them on the three editions' mods; other setups are untested. Faithful Core declares that it
doesn't run with StationAPI, so that combination stops at launch with a message.

In an instance from More Faithful, or from the library's old packs, they're in `.minecraft/mods-library` instead, each
with a switch in the game's settings, and named without their versions: to update one, replace its jar there with the
new one under the old name (`alchemy-0.6.0.jar` becomes `alchemy.jar`), `faithful-core` included.

## Servers

A server takes the server-side mods it wants in `mods/`, with `faithful-core` and `oldfaithful-settings`.
[More Faithful](https://github.com/studio-quire/more-faithful)'s server package is Still Faithful's with them on, all but
Wary Creepers.

- **World Rules** runs where the world runs, so a server can use it with any client, vanilla beta 1.7.3 too. A server with
  it is the same to a player who hasn't got it, except that under Bows Wear Out their bow wears out with no bar to show
  it. `/rule <name> <value>` for ops and the console.
- **Hearty Food** runs where the world runs too. On a server with it food heals over time for every player, but only a
  player who has it too sees the pale hearts. With it on a server without it, food is beta's.
- **Item Repair** runs where the world runs too, so a server can use it with any client, vanilla beta 1.7.3 too. With it on a
  server without it, nothing is mended.
- **Wild Saplings** runs where the world runs too, so a server can use it with any client, vanilla beta 1.7.3 too. With it
  on a server without it, saplings grow as in beta.
- **Wary Creepers** runs where the world runs too, so a server can use it with any client, vanilla beta 1.7.3 too. With it
  on a server without it, creepers are beta's.
- **Wolf Packs** runs where the world runs too, so a server can use it with any client, vanilla beta 1.7.3 too. With it
  on a server without it, wolves and creepers are beta's. A player without it feeds fish on a server with it all the same.
  Sitting wolves keep watch while their owner is away too, wherever players keep the land loaded. A wolf's growl from
  its seat is heard by players with any library mod on, and not by a vanilla client.
- **Water Bottles**, **Alchemy**, **Blue Flowers**, **Explosive Arrows** and **Buried Towers** need every player to have them: the first four add blocks or items, and Buried Towers builds on the library's world generation, which needs it too. A server running one
  refuses anyone without it, vanilla clients included, with a message saying so. A player on More Faithful with any mod
  on is offered **Match This Server's Mods**, which turns on what that server needs for their next launch. A client with
  them can join a server without them, where they can't be made and no towers are built.
- **Plain F3** and **No Toasts** are the player's alone, on any server.

With any library mod on, your game tells a server you join that you have the Faithful Library. A server with the library
sees that, and which of that server's own library mods you have too. It isn't told which other mods you have, Plain F3
and No Toasts included. A vanilla server ignores it.

Servers running other software (Project Poseidon and other Bukkit-family servers) can't load these mods. A client's mods
behave there as on a vanilla server.

## Turning a mod off or removing it

Worlds are kept in beta 1.7.3's own format, with the library's data beside it, so they open without the mods, in the
original game too. What you lose, plainly:

- **World Rules**: nothing. Its choices stay in the world's `faithful-world.properties` and come back with the mod.
  Worn bows look new and wear no more without it, and show their wear again when it's back.
- **Buried Towers**: towers it's built stay, as ordinary blocks, and new chunks go back to beta's own dungeons. A tower half
  made stays half made. Rooms the old Better Dungeons built stay too.
- **Water Bottles**: its items don't survive. Bottles in that world become bowls for good. Alchemy goes off with it, as it needs it.
- **Alchemy**: its items don't survive. Golden roses and bluebells in that world become roses, golden dandelions
  dandelions, golden feathers feathers and potions bowls, for good. A ward and potions still running wait in the
  player's save and go on if the mod returns.
- **Blue Flowers**: its bluebells don't survive. Growing or carried, in that world they become roses, for good. The
  yellow flower is called Flower again.
- **Explosive Arrows**: its arrows don't survive. Carried, in that world they become plain arrows, for good, and any in
  flight are gone.
- **Hearty Food**: food goes back to beta's. Healing still to come waits in the player's save and goes on if the mod returns.
- **Item Repair**: nothing. What it mended stays mended, and Repair Bonus, if you turned it on, stays in the world's file.
- **Wild Saplings**: nothing. Trees it grew stay, and saplings grow as in beta again.
- **Wolf Packs**: wolves eat as in beta again, and sitting ones only sit. Its rules stay in the world's file, and a wolf's
  healing still to come waits in the save, and both come back with the mod.
- **Wary Creepers**, **Plain F3** and **No Toasts** leave nothing in a world.

While any library mod is on, Faithful Core is loaded and warns you before a world that's seen a removed content mod
opens: **Make a Backup and Play**, **Play**, **Cancel**. A server refuses to start, and says how to go on, each time. If you turn off
the last mod, the core goes too and **nothing warns you**, so copy the world's folder first. With the core gone, the
library's data in a chunk goes when that chunk is next saved. That data is what would bring a removed mod's blocks back, if
a later mod adds blocks: as long as the core stays, blocks survive a removed mod, showing as the vanilla block they stand
on, and return with it.

## Writing a mod

[docs/library-authoring.md](docs/library-authoring.md) is the guide, and
[docs/library-harness.md](docs/library-harness.md) how mods are tested against each other. The Java API is 0.x and can
still change between releases. Names, save formats and what goes on the wire at login are permanent.

## Problems

Please [open an issue](https://github.com/studio-quire/faithful-library/issues) and attach
`.minecraft/logs/purefaithful/latest.log` from the instance folder. It never contains your access token.

## Building

This repository holds all of the library's source, and the editions' too. `./gw releaseLibrary` builds the jars a release
contains into `local/release/faithful-library-<version>/`, and `./gw releaseMore` More Faithful's pack;
[docs/building.md](docs/building.md) and [docs/release.md](docs/release.md) say how. Inside, the project is called by its
working name, `oldfaithful`.

## Credits and licence

Made by Claude Opus 5.5, an AI model by Anthropic, at Studio Quire. Claude Fable 5.2 designed Explosive Arrows and
Alchemy's Ward, Haste and Feather Potions, designed Wary Creepers and Wolf Packs with Shoalstone, wrote Wolf Packs'
sitting wolves' watch, and drew the arrow's head and the golden flowers and feather. Idea, direction and play-testing: Shoalstone.

The library's code is under the [MIT licence](LICENSE). It builds on Fabric Loader, Mixin and Babric's mappings, each
under its own licence.

Not an official Minecraft product. Not approved by or associated with Mojang or Microsoft.
