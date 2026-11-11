# More Faithful

**beta 1.7.3 with more in it**

Minecraft beta 1.7.3 with more in it, where you still do everything yourself. More Faithful is
[Still Faithful](https://github.com/studio-quire/still-faithful) (beta with polish, on
[Old Faithful](https://github.com/studio-quire/old-faithful) and [Pure Faithful](https://github.com/studio-quire/pure-faithful))
with the [Faithful Library](https://github.com/studio-quire/faithful-library)'s mods on, in single player and on servers,
each with its own switch: with every one off it plays exactly as Still Faithful. Open source, from
[Studio Quire](https://studioquire.org/).

It's all mods, not a modified game: your launcher downloads Minecraft itself from Mojang, with your own account. For
Windows now.

Through 0.4.0 this pack was the Faithful Library's own, called Still Faithful with the Faithful Library. The library's pack
on Old Faithful has gone; for the mods without Still Faithful's changes, see
[Without Still Faithful's changes](#without-still-faithfuls-changes).

## The mods

- **World Rules**: rules for each world, on the mod's page in the settings and on Create World's World Rules... page:
  Mobs Break Blocks, Blasts Destroy Items, Keep Inventory, Sleeping, Weather (normal, always clear, always
  rain or snow), Natural Monsters, Monster Light (monsters appear in dim light as in beta, or not in any torch or lava
  light), Apple Drops (oak leaves, 1 time in 200), Falling Hit Bonus (beta's, only when really falling, or none) and Bows
  Wear Out (a bow lasts 385 shots, any arrow, as when bows first wore out in 1.0, with a bar showing its wear like a tool's; turned off,
  worn bows look new and keep their wear for when it's back). **Lock Rules** locks a world's rules so nothing in the game changes them; on a
  server, `/rule lock`. While they're locked, the Mods menu doesn't switch off a mod that has rules. Taking
  `rules-locked=true` out of the world's `faithful-world.properties`, with the game closed, unlocks them.

  **A new world in More Faithful doesn't start with beta's rules** but with these: Mobs Break Blocks off (creepers and
  ghast fireballs still hurt, and leave the world alone), Blasts Destroy Items off (an explosion throws items lying on
  the ground and destroys none), Sleeping off (a bed sets your respawn point at any hour, and the night goes on),
  Monster Light at Dark Only (no monster appears in any torch or lava light; spawners stay as beta's), Apple Drops on, Falling Hit Bonus off (no
  half heart more on a hit) and Bows Wear Out on. The rest start as beta: you lose what you carry when you die, the
  weather is beta's, and monsters appear by themselves. Create World's World Rules... page shows all of them before
  you make a world, and what you choose there is what your next new world starts with too. Worlds you already have keep
  the rules they have, those made in an earlier More Faithful or brought from anywhere else too.

  What new worlds start with is your game's own from its first launch, kept in
  `.minecraft/config/faithful-rules.properties`, and an update doesn't change it: copy your `.minecraft/config` into a
  later version's instance and your new worlds start as they did, even where that version chooses differently. Only a
  rule that's new in it starts as the pack chose.
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
  creeper bitten by a wolf on watch turns on that wolf and blows up beside it. Where Mobs Break Blocks is on, that
  breaks what's round the wolf too; new worlds here start with it off, and a world made before 0.6.0, or brought in
  from elsewhere, has it on unless you turned it off. To keep a wolf out of the fighting, sit
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

**Stagehand**, also in the library, isn't in the pack: it runs scripts dropped into a folder in the game's directory
(teleports, items, time, weather, mobs, blocks), and writes back what happened, for setting up a play-test from outside
the game. Anything that can write to that folder can command the game, so install it only if you want that.
[docs/stagehand.md](docs/stagehand.md) says how, and the jar is on the
[Faithful Library's releases](https://github.com/studio-quire/faithful-library/releases).

**Wary Creepers**, also in the library, was in the pack through 0.5.0 and isn't now: creepers keep away from a
jack-o'-lantern whose lit face they can see. The
[Faithful Library's README](https://github.com/studio-quire/faithful-library#the-mods) says how to add its jar to this
pack.

**Faithful Core** is what they build on, and is on only while one of them is, so with every mod off the game never
says anything new on the network, and goes back to calling itself Still Faithful.

## Download and play

You need a Minecraft account that owns the game, and [Prism Launcher](https://prismlauncher.org/).

1. Download **MoreFaithful-&lt;version&gt;.zip** from [the latest release](https://github.com/studio-quire/more-faithful/releases).
2. In Prism Launcher: **Add Instance > Import**, choose the zip (or paste the download link), **OK**.
3. Launch it. Prism downloads beta 1.7.3 and its sounds from Mojang, and, if you let it, a Java 21 runtime.
4. In the game: **Options > More Faithful... > Mods...** (**Still Faithful...** when every mod is off). Each mod's page
   has an **Enabled** switch, which takes effect the next time you launch.

All the mods start on. Faithful Core comes on by itself when any mod is on, and goes off when none is. The jars are in
`.minecraft/mods-library`, and the switches are kept in `.minecraft/mods-library/selection.txt`. With every mod off the
game is Still Faithful, and the title screen and Options say so; `pack.properties` in that folder is what tells it it's
More Faithful, and holds the rules the pack chose for new worlds.

### Updating

Import the new zip as a new instance, then copy your worlds from the old instance's `.minecraft/saves` into the new
one's. To keep your switches and settings too, copy `.minecraft/mods-library/selection.txt` and `.minecraft/config`.
Your worlds open as they are and keep their rules; what each version changed is in the [changelog](CHANGELOG.md). If
the title screen then says a jar is gone and wasn't loaded, a mod on your old list has left the pack (Wary Creepers,
after 0.5.0), and nothing is wrong. From 0.5.0 or before, new worlds also start differently: see below.

### Coming from the Faithful Library's packs

The same, from the library's pack on Still Faithful (through 0.4.0) or from More Faithful 0.5.0. New worlds then
start with [More Faithful's rules](#the-mods), except any rule you'd set for new worlds yourself in the old instance,
which stays as you set it. What's changed since those packs (Wary Creepers out of the pack, no Day Length rule, sitting
wolves keeping watch) is in the changelog. A world from the
pack on Old Faithful gets Still Faithful's changes here, each of which can be switched off (below). That pack's own
instance keeps working too: to update its mods, replace each jar in its `.minecraft/mods-library`, `faithful-core`
included, with the new one from the [Faithful Library's release](https://github.com/studio-quire/faithful-library/releases)
under the old name (`alchemy-0.6.0.jar` becomes `alchemy.jar`).

### Without Still Faithful's changes

There's no pack of these mods on Old Faithful any more: each of our packs is the one before it with more, Pure, Old and
Still Faithful, then More Faithful. To have the mods without Still Faithful's polish, switch its changes off in
**Options > More Faithful... > Changes...**, on its Polish pages, each with its own switch, or put the library's mods into an
Old Faithful instance or your own setup: the [Faithful Library](https://github.com/studio-quire/faithful-library) has
them on their own, with how to install them.

## Servers

`MoreFaithful-server-<version>.zip` is Still Faithful's server package with the library's server-side mods on in
`mods/`, as in the pack. So as it comes it refuses a player without Water Bottles, Alchemy, Blue Flowers, Explosive
Arrows and Buried Towers. Move out of `mods/` the jars you don't want, before the first start; `faithful-core` and
`oldfaithful-settings` stay while any library mod does. With them all out it's Still Faithful's own server. The README
inside says more.

- **World Rules** runs where the world runs, so a server can use it with any client, vanilla beta 1.7.3 too. A server with
  it is the same to a player who hasn't got it, except that under Bows Wear Out their bow wears out with no bar to show
  it. `/rule <name> <value>` for ops and the console. A new world on the server package starts with the same rules as a
  new world in the pack (above); a world it already has, or one copied in, keeps its own. To start one differently, put
  your own lines in `config/faithful-rules.properties` before the first start; the server README says how.
- **Hearty Food** runs where the world runs too. On a server with it food heals over time for every player, but only a
  player who has it too sees the pale hearts. With it on a server without it, food is beta's.
- **Item Repair** runs where the world runs too, so a server can use it with any client, vanilla beta 1.7.3 too. With it on a
  server without it, nothing is mended.
- **Wild Saplings** runs where the world runs too, so a server can use it with any client, vanilla beta 1.7.3 too. With it
  on a server without it, saplings grow as in beta.
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
- **Plain F3** and **No Toasts** leave nothing in a world.

While any library mod is on, Faithful Core is loaded and warns you before a world that's seen a removed content mod
opens: **Make a Backup and Play**, **Play**, **Cancel**. A server refuses to start, and says how to go on, each time. If you turn off
the last mod, the core goes too and **nothing warns you**, so copy the world's folder first. With the core gone, the
library's data in a chunk goes when that chunk is next saved. That data is what would bring a removed mod's blocks back, if
a later mod adds blocks: as long as the core stays, blocks survive a removed mod, showing as the vanilla block they stand
on, and return with it.

## Problems

Please [open an issue](https://github.com/studio-quire/more-faithful/issues) and attach
`.minecraft/logs/purefaithful/latest.log` from the instance folder. It never contains your access token.

## Building

This repository holds all of the source: the editions', the library's and the pack's. `./gw releaseMore` builds the pack
and its server package into `local/release/more-faithful-<version>/`; [docs/building.md](docs/building.md) and
[docs/release.md](docs/release.md) say how. Inside, the project is called by its working name, `oldfaithful`.

## Credits and licence

Made by Claude Opus 5.5, an AI model by Anthropic, at Studio Quire. Claude Fable 5.2 designed Explosive Arrows and
Alchemy's Ward, Haste and Feather Potions, designed Wolf Packs with Shoalstone and wrote its sitting wolves' watch,
and drew the arrow's head, the golden flowers and feather, and More Faithful's seal. Idea, direction and play-testing: Shoalstone.

The code is under the [MIT licence](LICENSE). The pack builds on Fabric Loader, Mixin, ASM, Babric's mappings, LWJGL, SDL
and OpenAL Soft, each under its own licence; the downloads carry their notices and licence texts, and the source code
their licences ask for.

Not an official Minecraft product. Not approved by or associated with Mojang or Microsoft.
