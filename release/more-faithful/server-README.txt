More Faithful @VERSION@ server
==========================

More Faithful's server package: @EDITION_NAME@'s, with the Faithful Library @LIBRARY_VERSION@'s server-side mods in mods/,
all on, as they are in the More Faithful pack. Minecraft beta 1.7.3's own server, run through Fabric Loader with our
@SERVER_MODS@ mods, from
@EDITION_LINE@, and the library's. Water Bottles, Alchemy, Blue Flowers,
Explosive Arrows and Buried Towers are on, so as it comes this server refuses anyone without them, vanilla clients and
the editions' own packs included; the More Faithful pack has them. Below is how to take them out.

From the editions: it's hardened against malicious clients, a crash or power cut can't damage the world, and beta's clear
mistakes are fixed, each on a switch in config/. @EDITION_NAME@'s own README says more: https://github.com/@EDITION_REPO@

What it can't change, because it's how beta 1.7.3's online mode works: a server a player joins could pass that
player's login on to another server, so online mode isn't proof against a hostile server operator. Players should
only join servers they trust with their name, as with vanilla.

Setting up
1. Install Java 21 or newer.
2. Put Minecraft beta 1.7.3's server jar in this folder, named minecraft_server.jar. Mojang no longer offers it
   for download, so use a copy you have; the start script checks it's the genuine one (SHA-1 @SERVER_SHA1@).
   It isn't included: it's Mojang's.
3. Run start.bat (Windows) or start.sh (macOS, Linux). The first start writes server.properties and the world, as
   vanilla does. JAVA picks the java executable and JAVA_OPTS its options (default -Xmx1G).

Turning mods off
With the server stopped, move the jar out of mods/ into any other folder. faithful-core-@VERSION@.jar and
oldfaithful-settings-@FIXED_VERSION@.jar stay as long as any library mod does. Do this before the first start for any mod
you don't want: a world that has seen Water Bottles, Alchemy, Blue Flowers, Explosive Arrows or Buried Towers is asked about below. With all of them out, the core and settings too,
it's the plain @EDITION_NAME@ server: the library's code isn't loaded and nothing new goes over the network.

- world-rules: rules for each world, the same for everyone on the server. /rule <name> <value> for ops and the console,
  or the world's faithful-world.properties. /rule lock locks them until the line rules-locked=true comes out of that
  file, with the server stopped. Any client can join, vanilla too; a client with World Rules shows the server's values.
  With /rule bows_wear_out true, bows last 385 shots; a player without World Rules sees no bar for the wear.
- buried-towers: ruined towers on the land, going a long way down, in new chunks, and no more of beta's small dungeons
  there. Every player needs it, like water-bottles below. A new world's towers are Simple, crude stumps like beta's
  dungeons, or Complex, big towers of dressed stone, with the line rule.buried-towers.style=complex in
  config/faithful-rules.properties before the world's first start. A world keeps the style it started with.
- item-repair: two worn tools, weapons or pieces of armour of the same kind in the crafting grid make one, with what was
  left of both. Its rule repair_bonus (/rule repair_bonus true) adds a twentieth of a new one's durability;
  off as it comes. Any client
  can join, vanilla too, and mend.
- hearty-food: food heals half as much again, at half a heart a second. Any client can join; only one with the mod too
  sees the pale hearts showing where a player will end up.
- water-bottles: every player needs it. A server running it refuses anyone without it, vanilla clients included, with a
  message saying so. A player on the More Faithful pack with any mod on gets a button to turn on what your server needs.
- alchemy: potions of health, ward, haste and feather from gilded flowers and feathers. Every player needs it, in the
  same way. It needs water-bottles, so that goes out only with it.
- blue-flowers: bluebells in patches across Taiga and Tundra, in new chunks. Every player needs it, in the same way.
- explosive-arrows: arrows with gunpowder that burst where they land, hurting what's near (the shooter too) and breaking
  no blocks, though TNT they reach goes off. One a player shoots hurts other players only where the server's pvp
  is on; one from a dispenser hurts players whatever the pvp setting, as its plain arrows do. Every player needs it,
  in the same way.
- wild-saplings: saplings grow every tree that grows wild, as often as it grows wild: spruce saplings grow pines too, and
  oak saplings grow big oaks a third of the time in Forest and Rainforest, as full as wild ones. Any client can join,
  vanilla too.
- wary-creepers: creepers keep away from a jack-o'-lantern whose lit face they can see, within 8 blocks: not from
  behind it, through a wall or through glass, and one already beside a player still swells. Any client can join,
  vanilla too.
- wolf-packs: tamed wolves take on a different monster each, and the next when theirs dies, where beta sent them all at
  one; they go for a monster after their owner within 4 blocks and a skeleton shooting at them, and fight creepers,
  which beta's never did; wolves eat fish as well as pork, each food healing its own worth (with hearty-food, as
  hearty-food heals players). Four world rules, spread_out, fight_creepers, feeding and guarding, all on. Any client
  can join, vanilla too, and feed fish.

Plain F3 and No Toasts are client-only, so they aren't here, and players can use them on any server.

Taking a mod out again
A server refuses to start when a world has seen Water Bottles, Alchemy, Blue Flowers, Explosive Arrows or Buried Towers and it's gone, and says how to go on: make a
backup, then add accept-missing-content=true to the world's faithful-world.properties. Its bottles and potions then
become bowls, its golden roses and bluebells roses, and its explosive arrows plain arrows, for good. That
line is good for one start: the server sets it back to false, so a later removal is refused again. World Rules' values
stay in that file and come back with the mod. Towers built stay, as ordinary blocks, as do rooms the old Better Dungeons
built. Hearty Food's healing still to come
waits in each player's save and goes on if the mod returns. If you take out
faithful-core too, the library's data in a chunk goes when that chunk is next saved, and nothing checks first.

A vanilla beta 1.7.3 server's world and server.properties work as they are: copy them in. The world stays in beta's
format, with the library's data beside it, which vanilla ignores.

@HOMEPAGE@
Open source under the MIT licence, made by Claude Opus 5.5 at Studio Quire. Claude Fable 5.2 designed Explosive
Arrows and Alchemy's Ward, Haste and Feather Potions, and Wary Creepers and Wolf Packs with Shoalstone. Opus and
Fable are Anthropic's AI models.
Not an official Minecraft product. Not approved by or associated with Mojang or Microsoft.
