@NAME@ @VERSION@
@TAGLINE@

This folder is a Prism Launcher instance. Prism downloads Minecraft beta 1.7.3 itself, from Mojang, the first time
you launch it; everything else is already here. You need a Minecraft account that owns the game.

@NAME@ is beta with more in it, where you still do everything yourself: @EDITION_NAME@'s own pack with the
Faithful Library @LIBRARY_VERSION@'s mods in .minecraft/mods-library, all on. Underneath is
@EDITION_LINE@.
Through 0.4.0 this pack was called Still Faithful with the Faithful Library.

Turning a mod off or on: in the game, Options > @NAME@... > Mods... lists them (Options > Still Faithful... > Mods...
when every mod is off). Each has a page with an Enabled
switch, which takes effect the next time you launch. Faithful Core, which the mods build on, comes on by itself when any
mod is on and goes off when none is. With all of them off the pack plays exactly as @EDITION_NAME@ does, and goes by
its name on the title screen and in Options: none of the library's code is loaded, and nothing new goes over the
network. The switches are kept in .minecraft/mods-library/selection.txt, and pack.properties beside it is what tells
the game it's @NAME@.

The mods:
- World Rules: rules for each world, on its page in Mods... and on Create World's World Rules... page: Mobs Break Blocks,
  Blasts Destroy Items, Keep Inventory, Sleeping, Day Length, Weather, Natural Monsters, Monster Light, Apple Drops, Falling Hit Bonus,
  Bows Wear Out (385 shots, as in 1.0). A world
  nobody changed plays as beta. Lock Rules locks a world's rules so nothing in the game changes them; taking the line
  rules-locked=true out of the world's faithful-world.properties, with the game closed, unlocks them. While a world's
  rules are locked, Mods... doesn't switch off a mod that has rules.
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
- Alchemy: gild a flower or a feather, then brew it. Four gold ingots around a rose, a dandelion, a bluebell or a
  feather at the bench make a golden one, and a water bottle with one, two or three of the same golden thing makes a
  weak, plain or strong potion of its kind, in the inventory's grid too. Health (roses) heals 3, 6 or 10 hearts at once.
  Ward (bluebells) is a set of armour in a bottle: the whole armour bar turns blue over whatever you wear, and hits wear
  it away before your own armour takes any wear; the bigger the potion, the longer it lasts, and a golden apple gives a
  strong one's too. Haste (dandelions, walking and mining 20% faster) and Feather (falls hurt a third as much) last 4, 8
  or 12 minutes, their bottles draining beside your figure in the inventory. A potion isn't drunk when there's no room
  for any of it. It needs Water Bottles, which comes on with
  it, and the bluebells come from Blue Flowers.
- Blue Flowers: bluebells in patches across Taiga and Tundra, as dandelions grow in warmer land. New chunks only.
  A bluebell is placed and picked like the rose and dandelion, and makes two light blue dye. Bonemeal on grass in those
  biomes can grow one too. It also calls beta's yellow flower Dandelion, where beta said Flower; its page in the
  settings has a switch to keep Flower.
- Wild Saplings: saplings grow every tree that grows wild, so a forest you cut down can be planted again. A spruce
  sapling grows a tall, thin pine one time in three, as a third of Taiga's trees are. An oak sapling grows a big oak as
  often as the wild oaks where it's planted are big: one time in three in Forest and Rainforest, one in ten elsewhere,
  as in beta. And its big oaks are as full as wild ones, where beta's had smaller clumps of leaves.
- Explosive Arrows: an arrow and two gunpowder make an Explosive Arrow, in the inventory's grid too. It bursts where it
  lands, on a block or a mob, with a small blast of beta's own, half a creeper's in size: a direct hit takes over half a
  zombie's health, and things a block or two off lose a couple of hearts. It breaks no blocks, but TNT it reaches goes
  off, and it hurts you too if you fire too close. Its puffs show as far off as you can hear the bang, where beta draws a
  blast's only within 16 blocks of you. A bow shoots whichever arrows come first in your inventory, the hotbar from the
  left first, so where you keep them decides. Dispensers shoot them too.
- Wary Creepers: creepers keep away from a jack-o'-lantern whose lit face they can see. A creeper within 8 blocks, on
  the side the face looks out on and with nothing in the way, turns and walks off, and comes back once it no longer
  sees a lit face in reach. A creeper coming from behind a lantern isn't scared at all, so mind which way each one
  faces: it faces you as you place it. Glass, leaves and walls hide it, as they hide you from a creeper. One already at
  your side still swells. Carrying or wearing a pumpkin does nothing.
- Wolf Packs: your tamed wolves take on a different monster each. A fight starts when you hit something or something
  hurts you, as in beta, or when a monster after you comes within 4 blocks of you or a skeleton shoots at you; then
  each wolf goes for whichever monster after you or your wolves has the fewest wolves on it, what you hit first, and
  when its own dies it takes the next. A wolf that can't reach its monster gives up on it after a while. Wolves fight
  creepers too, and a bitten creeper keeps coming for you.
  Wolves eat fish as well as pork, and each food heals a wolf what it heals you (with Hearty Food, as it heals you);
  a click with its food never sits a wolf down or stands it up.
  Each part is a world rule.
- Item Repair: two worn tools, weapons or pieces of armour of the same kind in the crafting grid make one, with what
  was left of both, up to as good as new, as in later versions. A world rule, Repair Bonus, off to start with, adds a
  twentieth of a new one's durability on top.
- Plain F3: the F3 screen keeps its frame rate and memory and loses position, facing and entity counts, and mobs lose
  the numbers beta tags them with through walls, so you find your way without coordinates.
- No Toasts: no achievement or tutorial pop-ups. Achievements are still earned.

On a server: World Rules runs where the world runs, so a server can use it with any client, vanilla too. Hearty Food does
too, with food healing over time for everyone there, though only a player who has it too
sees the pale hearts; on a server without it, food is beta's. Item Repair runs where the world runs too: any client can
mend on a server with it, and nothing is mended on one without it. Wild Saplings runs where the world runs too: any
client gets its trees on a server with it, and on one without it saplings grow as in beta. Wary Creepers runs where the
world runs too: on a server with it creepers keep away from lanterns for everyone there, and on one without it they're
beta's. Wolf Packs runs where the world runs too: on a server with it every player's wolves spread out, guard them,
fight creepers and eat fish, and on one without it they're beta's. Plain F3 and No Toasts are yours alone and work anywhere. A server running Water Bottles, Alchemy, Blue Flowers, Explosive Arrows or Buried Towers needs every player
to have it: anyone else is refused with a message saying so. If you have any library mod on, the refusal screen's Match
This Server's Mods turns on what that server needs, for your next launch; with none on, it's Options > Still Faithful...
> Mods..., as above.
With them on you can still join servers without them, where they can't be made.
With any library mod on, your game tells a server you join that you have the Faithful Library: a server with it can
see that, and which of that server's own library mods you have too, but isn't told which other mods you have.

Turning a mod off or removing it:
- Your world opens without it. World Rules' choices stay in the world's file and come back with the mod; the hour may
  jump if you'd changed the day's length. Towers already built stay, as ordinary blocks, and new chunks go back to beta's
  dungeons. Hearty Food's healing still to come waits in your save and goes on if the mod returns. Trees Wild Saplings
  grew stay. Wolf Packs' rules stay in the world's file, and wolves eat as in beta. Wary Creepers, Plain F3 and No Toasts leave
  nothing behind.
- Water Bottles' items don't survive: bottles in that world become bowls for good, the next time you open it. Alchemy
  goes off with it, as it needs it.
- Alchemy's don't either: golden roses and bluebells become roses, golden dandelions dandelions, golden feathers
  feathers and potions bowls, for good. A ward and potions still running wait in your save and go on if it returns.
- Blue Flowers' don't either: bluebells, growing or carried, become roses for good.
- Explosive Arrows' don't either: carried ones become plain arrows for good, and any in flight are gone.
- If other library mods are still on, a screen tells you before such a world opens, and offers to make a backup. If that
  was the last mod on, Faithful Core goes too and nothing warns you, so back up first: copy the world's folder from
  .minecraft/saves. With the core gone, the library's data in a chunk goes when that chunk is next saved.

Your worlds are in .minecraft/saves, in beta 1.7.3's own format with the library's data beside it: they open in the
original game and in every edition, as if the mods had been removed.

If something goes wrong, the game's log is .minecraft/logs/purefaithful/latest.log.

Made by Claude Opus 5.5, an AI model by Anthropic, at Studio Quire. Claude Fable 5.2 designed Explosive Arrows and
Alchemy's Ward, Haste and Feather Potions, designed Wary Creepers and Wolf Packs with Shoalstone, and drew the
arrow's head, the golden flowers and feather, and More Faithful's seal. Idea, direction and play-testing: Shoalstone.
Open source under the MIT licence. Software by others included here, and its licences: THIRD-PARTY-NOTICES.txt,
licenses/ and sources/.
@HOMEPAGE@
Not an official Minecraft product. Not approved by or associated with Mojang or Microsoft.
