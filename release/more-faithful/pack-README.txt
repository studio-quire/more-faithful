@NAME@ @VERSION@
@TAGLINE@

This folder is a Prism Launcher instance. Prism downloads Minecraft beta 1.7.3 itself, from Mojang, the first time
you launch it; everything else is already here. You need a Minecraft account that owns the game. On its first start
the game also fetches beta's own bow and door sounds (about 28 KB), which Mojang no longer offers, from Modification
Station's archive of beta's files. To keep it from doing so, set resources.download=false in
.minecraft/config/purefaithful-hardening.properties before you first launch it, creating the file if it isn't there.

Bringing worlds from 0.7.1 or before: Blue Flowers now turns their lapis lazuli ore to stone and their Lapis Lazuli
Blocks to blue wool, for good. Before you open one, break stored blocks down into lapis lazuli in the old instance, or,
to keep beta's lapis lazuli, switch Blue Flowers off in Options > @NAME@... > Mods... and restart the game (the world's
bluebells then become roses).

@NAME@ is beta with more in it: @EDITION_NAME@'s own pack with mods from the Faithful Library
@LIBRARY_VERSION@ in .minecraft/mods-library, in presets. Underneath is
@EDITION_LINE@.
Through 0.4.0 this pack was called Still Faithful with the Faithful Library.

Presets: which mods are on is one choice for the whole game, at the top of Options > @NAME@... > Mods..., where Preset
steps through them a click at a time:
@PRESETS@
Each mod's page still has its own Enabled switch, so you can add a mod to a preset or take one out; the button then
reads Preset: Custom, and choosing a preset again puts it back as it comes. A preset and a switch take effect the next
time you launch. Your preset and the mods you turned on or off beyond it are kept in
.minecraft/config/faithful-preset.properties, so they come with config into a later version: at the new version's
first start the game sets its mods to them and, if that changes anything, asks you to restart once.

Turning a mod off or on: in the game, Options > @NAME@... > Mods... lists them (Options > Still Faithful... > Mods...
when every mod is off). Faithful Core, which the mods build on, comes on by itself when any mod is on and goes off when
none is. With all of them off the pack plays exactly as @EDITION_NAME@ does, and goes by its name on the title screen
and in Options: none of the library's code is loaded, and nothing new goes over the network. What's on is listed in
.minecraft/mods-library/selection.txt, which the game keeps to your preset and switches, so change them in the Mods
menu rather than there; pack.properties beside it is what tells the game it's @NAME@, with its presets. The presets
and switches are @NAME@'s own small mod, morefaithful-presets in .minecraft/mods.

The mods:
- World Rules: rules for each world, on its page in Mods... and on Create World's World Rules... page: Mobs Break Blocks,
  Blasts Destroy Items, Keep Inventory, Sleeping, Weather, Natural Monsters, Monster Light, Apple Drops, Falling Hit Bonus,
  Bows Wear Out (385 shots, as in 1.0), Pickup Reach (half a block above and below you, and all round what you ride).
  Lock Rules locks a world's rules so nothing in the game changes them; taking the line
  rules-locked=true out of the world's faithful-world.properties, with the game closed, unlocks them. While a world's
  rules are locked, Mods... doesn't switch off a mod that has rules.
  A new world here doesn't start with beta's rules but with @NAME@'s: Mobs Break Blocks off (creepers and ghast
  fireballs still hurt, and leave the world alone), Blasts Destroy Items off (an explosion throws items lying on the
  ground and destroys none), Sleeping off (a bed sets your respawn point at any hour, and the night goes on), Monster
  Light at Dark Only (no monster appears in any torch or lava light; spawners stay as beta's), Apple Drops on, Falling Hit Bonus off (no half
  heart more on a hit), Bows Wear Out on and Pickup Reach on, and Pumpkins' Pumpkins Spread on. The rest start as beta. Create World's World Rules... page shows them all
  before you make a world, and what you choose there is what your next new world starts with too. Worlds you already
  have keep the rules they have, but Pickup Reach, new in 0.7.0, comes on in them too, and Pumpkins Spread, new in
  0.8.0, stays off in them. What new worlds start with is your game's own from its first launch, kept in
  .minecraft/config/faithful-rules.properties, and an update doesn't change it: copy your .minecraft/config into a later
  version's instance and your new worlds start as they did. Only a rule that's new in that version starts as it chose.
- Buried Towers: ruined towers stand on the land about every 24 chunks, and go a long way down: floors somebody built
  and time got to, a different way down from each, in beta's own blocks. They take the place of beta's small dungeons in
  new chunks. A world chooses its towers when it's created, with Towers on Create World's World Rules... page: Simple
  (the default), crude stumps of cobble and moss like beta's dungeons, or Complex, big towers of dressed stone. Rooms
  the old Better Dungeons built stay in your worlds as ordinary blocks.
- Water Bottles: glass bottles you fill at any water and drink from for nothing. Three glass in a V at the
  bench makes three. An ingredient for mods to come.
- Hearty Food: food heals half as much again, at half a heart a second instead of at once, and pale hearts show where
  you'll end up. Eating is one click as ever. More food adds to what's coming, a hit doesn't cancel it,
  and it helps even mid-fight. A golden apple still heals everything at once. Food past a full bar is wasted, as in beta.
- Alchemy: gild a flower or a feather, then brew it. Four gold ingots around a rose, a dandelion, a lupin or a
  feather at the bench make a golden one, and a water bottle with one, two or three of the same golden thing makes a
  weak, plain or strong potion of its kind, in the inventory's grid too. Health (roses) heals 3, 6 or 10 hearts at once.
  Ward (lupins) is a set of armour in a bottle: the whole armour bar turns blue over whatever you wear, and hits wear
  it away before your own armour takes any wear; the bigger the potion, the longer it lasts, and a golden apple gives a
  strong one's too. Haste (dandelions, walking and mining 20% faster) and Feather (falls hurt a third as much) last 4, 8
  or 12 minutes, their bottles draining beside your figure in the inventory. A potion isn't drunk when there's no room
  for any of it. It needs Water Bottles, which comes on with
  it, and the lupins come from Blue Flowers.
- Blue Flowers: lupins, a blue flower whose dye takes the place of lapis lazuli. They grow in patches across Taiga,
  Tundra and Highland, as dandelions grow in warmer land, in new chunks only, and bonemeal on grass anywhere can grow
  one, as it grows roses and dandelions. Highland is our name for the land beta calls Swampland: in beta it has no
  swamp in it (that came in later versions), only bare, steep, clover-green hills that occur beside the snow. A lupin
  is placed and picked like the rose and dandelion, and makes two Lupin Blue: beta's blue dye, the item that was
  Lapis Lazuli, with a new name and picture. It dyes wool and sheep and mixes into light blue, cyan, purple and magenta
  as it always did. With the mod on there's no lapis lazuli ore and no Lapis Lazuli Block: no ore forms in new chunks,
  and the block can't be made or broken down into nine. In a world that already has
  them, ore turns to stone and blocks turn to blue wool, for good, as each part of the world is next loaded: blocks
  you've built with where they stand, and blocks you hold or keep in a chest one for one. Lapis lazuli you hold is
  Lupin Blue. It also calls beta's yellow flower Dandelion, where beta said Flower; its page in the settings has a
  switch to keep Flower.
- Wild Saplings: saplings grow every tree that grows wild, so a forest you cut down can be planted again. A spruce
  sapling grows a tall, thin pine one time in three, as a third of Taiga's trees are. An oak sapling grows a big oak as
  often as the wild oaks where it's planted are big: one time in three in Forest and Rainforest, one in ten elsewhere,
  as in beta. And its big oaks are as full as wild ones, where beta's had smaller clumps of leaves.
- Pumpkins: pumpkin pie, and pumpkins that spread. A pumpkin, sugar and an egg make a pie, in the inventory's grid
  too, which heals as a cooked porkchop does (with Hearty Food, six hearts over time). Under the world rule Pumpkins
  Spread, a pumpkin on grass or dirt now and then grows another on open grass near it, or on grass under snow, until
  there are about six within about 14 blocks; never on tall grass or flowers. From one pumpkin a patch takes a few
  in-game days spent near it. One on stone or planks never spreads, nor does a jack-o'-lantern, and Snipping's shears
  stop one. A new world here starts with it on; in worlds you already have it's off: switch it on from Pumpkins' page
  in Mods... with the world open.
- Snipping: right-click a plant that grows by itself with shears and it stops: a sapling never becomes a tree, wheat
  stays as it is, a mushroom doesn't spread, sugar cane and cactus stay as tall as they are, a pumpkin doesn't spread.
  It costs the shears a use. Nothing marks it: shears on it again do nothing, which is how you tell. Bonemeal does
  nothing on a snipped sapling or wheat. Break it and plant it again and it grows.
- Explosive Arrows: an arrow and two gunpowder make an Explosive Arrow, in the inventory's grid too. It bursts where it
  lands, on a block or a mob, with a small blast of beta's own, half a creeper's in size: a direct hit takes over half a
  zombie's health, and things a block or two off lose a couple of hearts. It breaks no blocks, but TNT it reaches goes
  off, and it hurts you too if you fire too close. Its puffs show as far off as you can hear the bang, where beta draws a
  blast's only within 16 blocks of you. A bow shoots whichever arrows come first in your inventory, the hotbar from the
  left first, so where you keep them decides. Dispensers shoot them too.
- Feather Lure: a fishing rod and a feather make a Lured Fishing Rod, in the inventory's grid too, as worn as the rod
  was. Fish bite on it twice as often, in rain as well, and it wears as a fishing rod does. Each fish you catch has a 1
  in 8 chance of taking the feather: bits of it fly off at the end of your line, and the rod in your hand is a plain
  fishing rod again, just as worn, until you give it another feather. It's the rod in your hand that counts, so
  switching to a plain one with your line out fishes at beta's pace. Two worn lured rods mend into one, as Item Repair
  mends two of a kind; a lured rod and a plain one don't, so put a feather on the plain one first.
- Wolf Packs: your tamed wolves take on a different monster each. A fight starts when you hit something or something
  hurts you, as in beta, or when a monster after you comes within 4 blocks of you or a skeleton shoots at you; then
  each wolf goes for whichever monster after you or your wolves has the fewest wolves on it, what you hit first, and
  when its own dies it takes the next. A wolf that can't reach its monster gives up on it after a while. Wolves fight
  creepers too, and a bitten creeper keeps coming for you.
  A sitting wolf keeps watch over its spot: it goes for a monster it sees within 8 blocks, no further than 16 from its
  spot, then walks back and sits again, whether you're there or not. It doesn't see through a wall, glass or leaves,
  sees over a fence only from a little way back, and takes on only what it sees from its seat. Wolves on watch get hurt, and a creeper
  one bites turns on it and blows up beside it (breaking blocks where Mobs Break Blocks is on, as it is in any world not
  made new here, unless you turned it off), so feed them. Sat where it can see monsters and can't get to them (up a ledge four blocks
  high, say), a wolf stays put and growls.
  Wolves eat fish as well as pork, and each food heals a wolf what it heals you (with Hearty Food, as it heals you);
  a click with its food never sits a wolf down or stands it up.
  Each part is a world rule.
- Item Repair: two worn tools, weapons or pieces of armour of the same kind in the crafting grid make one, with what
  was left of both, up to as good as new, as in later versions. A world rule, Repair Bonus, off to start with, adds a
  twentieth of a new one's durability on top.
- Golems: iron workers. A Golem Core (a diamond with four gold ingots in a plus and four redstone in the corners) used
  on the top of two stacked iron blocks makes one. Before you put it to work, an empty hand has it follow you or stay.
  Hand it a pickaxe, an axe, a hoe, a sword or a bow and it works where it stands, at half your pace, its tool wearing
  as yours would (and a hoe a use for each wheat it reaps): a pickaxe levels the land down to where it stands (what
  you've built of stone, sandstone or dirt too, though not cobblestone or planks); an axe fells trees and replants
  them; a hoe keeps a wheat field; a sword fights the monsters that come (creepers apart); and a bow shoots them as a
  skeleton does, creepers too, from 4 to 8 blocks off, with arrows from its chest. An archer wants room behind it, or
  a perch at least four blocks high with its foot kept clear by a fence or a ditch a few blocks out. It works inside
  four Boundary Poles round it (redstone over an iron ingot makes four; set them out within 48 blocks of it before you
  hand it its tool), or 16 blocks round where it stands without them, and carries what it gathers to a chest within 8
  blocks, from which it also takes spare tools, seeds, saplings, arrows and iron ingots to mend itself as its cracks
  show. An ingot from your hand mends it too, and an empty hand takes its tool back and has it put down what it
  carried. Killed, it drops its core, its tool and what it carried, not the iron.
- Plain F3: the F3 screen keeps its frame rate and memory and loses position, facing and entity counts, and mobs lose
  the numbers beta tags them with through walls, so you find your way without coordinates.
- No Toasts: no achievement or tutorial pop-ups. Achievements are still earned.

On a server: World Rules runs where the world runs, so a server can use it with any client, vanilla too. Hearty Food does
too, with food healing over time for everyone there, though only a player who has it too
sees the pale hearts; on a server without it, food is beta's. Item Repair runs where the world runs too: any client can
mend on a server with it, and nothing is mended on one without it. Wild Saplings runs where the world runs too: any
client gets its trees on a server with it, and on one without it saplings grow as in beta. Wolf Packs runs where the
world runs too: on a server with it every player's wolves spread out, guard them, keep watch sitting, fight creepers
and eat fish, and on one without it they're beta's. Snipping runs where the world runs too: any player's shears snip
on a server with it, and on one without it nothing is snipped, though shears still swing and sound. Plain F3 and No Toasts are yours alone and work anywhere. A server running Water Bottles, Alchemy, Blue Flowers, Explosive Arrows, Feather Lure, Pumpkins, Golems or Buried Towers needs every player
to have it: anyone else is refused with a message saying so. If you have any library mod on, the refusal screen's Match
This Server's Mods turns on what that server needs, for your next launch; with none on, it's Options > Still Faithful...
> Mods..., as above.
With them on you can still join servers without them, where they can't be made. With Blue Flowers on, a server
without it still has lapis lazuli as in beta, and your game shows it under Lupin Blue's name and picture.
With any library mod on, your game tells a server you join that you have the Faithful Library: a server with it can
see that, and which of that server's own library mods you have too, but isn't told which other mods you have.

Turning a mod off or removing it:
- Your world opens without it. World Rules' choices stay in the world's file and come back with the mod. Towers
  already built stay, as ordinary blocks, and new chunks go back to beta's dungeons. Hearty Food's healing still to
  come waits in your save and goes on if the mod returns. Trees Wild Saplings grew stay. Wolf Packs' rules stay in the
  world's file, and wolves eat and sit as in beta. Plants Snipping snipped grow again, and are snipped again if it
  returns. Plain F3 and No Toasts leave nothing behind.
- Water Bottles' items don't survive: bottles in that world become bowls for good, the next time you open it. Alchemy
  goes off with it, as it needs it.
- Alchemy's don't either: golden roses and lupins become roses, golden dandelions dandelions, golden feathers
  feathers and potions bowls, for good. A ward and potions still running wait in your save and go on if it returns.
- Blue Flowers' don't either: lupins, growing or carried, become roses for good. Lupin Blue is Lapis Lazuli again,
  the same item under beta's name, new chunks have its ore and the block can be made; ore that turned to stone and
  blocks that turned to wool stay as they are.
- Explosive Arrows' don't either: carried ones become plain arrows for good, and any in flight are gone.
- Feather Lure's don't either: lured rods become plain fishing rods, as good as new, for good.
- Pumpkins' don't either: pies become pumpkins for good. The pumpkins that grew stay, and its rule waits in the
  world's file.
- Golems' don't either: golems, and the tools and things they carry, are gone from a chunk when it's next saved, and
  cores become diamonds, for good. Boundary Poles show as fences and come back with the mod.
- If other library mods are still on, a screen tells you before such a world opens, and offers to make a backup. If that
  was the last mod on, Faithful Core goes too and nothing warns you, so back up first: copy the world's folder from
  .minecraft/saves. With the core gone, the library's data in a chunk goes when that chunk is next saved.

Your worlds are in .minecraft/saves, in beta 1.7.3's own format with the library's data beside it: they open in the
original game and in every edition, as if the mods had been removed.

If something goes wrong, the game's log is .minecraft/logs/purefaithful/latest.log.

Made by Claude Opus 5.5, an AI model by Anthropic, at Studio Quire. Claude Fable 5.2 designed Explosive Arrows and
Alchemy's Ward, Haste and Feather Potions, designed Wolf Packs and how golems go about their work with Shoalstone,
wrote Wolf Packs' sitting wolves' watch and the lupins in lapis lazuli's place, added to Shoalstone's designs for
Pumpkins and Snipping, and drew the arrow's head, the golden flowers and feather, the lupin and Lupin Blue, the pumpkin
pie, the golem, its cracks, the Golem Core and the Boundary Pole, More Faithful's seal and, to Shoalstone's shape,
Feather Lure's bobber. Idea, the designs of Pumpkins, Snipping and the lupins, direction and play-testing: Shoalstone.
Open source under the MIT licence. Software by others included here, and its licences: THIRD-PARTY-NOTICES.txt,
licenses/ and sources/.
@HOMEPAGE@
Not an official Minecraft product. Not approved by or associated with Mojang or Microsoft.
