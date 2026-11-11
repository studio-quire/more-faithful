# Changelog

More Faithful's releases, newest first. Each has its own version number, carried by its pack and server package, and
names the Faithful Library release whose mods are inside and the Still Faithful, Old Faithful and Pure Faithful releases
it's built on, each as those releases have them;
[the Faithful Library's changelog](https://github.com/studio-quire/faithful-library/blob/main/CHANGELOG.md) and
[Still Faithful's](https://github.com/studio-quire/still-faithful/blob/main/CHANGELOG.md) cover those. Entries say what a
player or server owner notices.

## 0.7.0 (2026-11-11)

On the Faithful Library 0.7.0 and Still Faithful 0.7.0, on Old Faithful 0.2.5 and Pure Faithful 0.1.5.

- **Pickup Reach**, a new rule of World Rules, on in new worlds and in worlds you already have: you pick up items from
  half a block above and below you as well, so something on top of a two-block step beside you comes to you without a
  jump, and riding a boat, a minecart or a pig, from all round what you ride, as in later versions. Turn it off on
  World Rules' page for beta's reach.
- From Still Faithful, **Boat Steering**: boats steer as in later versions, A and D turning the boat and your view with
  it, W paddling it the way it faces, up to 6.5 blocks a second where beta's reached about 5.8; a boat you place faces the
  way you look. With it, from Old Faithful, **Riding Look**: on a server, others see your head turn while you ride, and
  a server with Boat Steering steers your boat. Players joining with a game without Riding Look paddle as in beta. Still
  Faithful's changelog has the detail.
- From Still Faithful, **Boat Handling**: boats no longer bob up and down at the surface. A boat settles after a bob or
  two and stays put, about as high in the water as before, and your view with it.
- From Still Faithful, **Held Attack**: holding the button to attack or mine no longer hits whatever comes under the
  crosshair, such as the animal behind a block you've just mined: a hold goes on hitting only what you clicked. Still
  Faithful's changelog has the detail.
- From Old Faithful 0.2.5, through Still Faithful: mobs at the edges of the screen stay drawn while any part of
  them is in view, where beta dropped them while a head, arms or tentacles still showed; and a correction to the fix
  that sends a server's chunks nearest first. Old Faithful's changelog has the detail.
- **Feather Lure**, a new mod, on: a fishing rod and a feather make a Lured Fishing Rod, as worn as the rod was, and
  fish bite on it twice as often; its bobber shows a small feather over the hook. Each fish you catch has a 1 in 8 chance
  of taking the feather, leaving you a plain fishing rod, just as worn. Two lured rods mend into one, but a lured rod
  and a plain one don't. The server package has it on as it comes, so it now refuses players without it; the pack has
  it.
- **Updating**, from any earlier version: copy your worlds and `.minecraft/config` from the old instance, but no longer
  your old `.minecraft/mods-library/selection.txt`, which earlier entries told you to copy. The new one has every mod on, Feather
  Lure included. If you'd switched mods off, switch them off again in Options > More Faithful... > Mods..., then restart
  the game before you open a world. If you'd added a mod yourself (Wary Creepers, say), copy its jar into the new
  `.minecraft/mods-library` and add its line to the new `selection.txt` again.

## 0.6.1 (2026-11-10)

On the Faithful Library 0.6.0 and Still Faithful 0.6.1, on Old Faithful 0.2.4 and Pure Faithful 0.1.5. The pack now
carries Still Faithful 0.6.1; the library's mods are unchanged. From Still Faithful 0.6.1: a skeleton riding a spider no
longer shoots its own spider when it shoots down at you. Since Still Faithful 0.6.0, the steeper the shot, the more of
its arrows hit the spider, nearly all of them when it was well above you and close by. Still Faithful's changelog has
the detail.

## 0.6.0 (2026-11-10)

On the Faithful Library 0.6.0 and Still Faithful 0.6.0, on Old Faithful 0.2.4 and Pure Faithful 0.1.5.

**Sitting wolves keep watch**, and **Wary Creepers is out of the pack**. Creepers no longer keep away from
jack-o'-lanterns: what keeps a place safe is a wolf you've sat there, and it deals with whatever monster it sees, at a price.

- **Sitting Wolves Watch**, a new rule of Wolf Packs, on in worlds you already have too. A sitting wolf goes for a
  monster it sees within 8 blocks, follows it no further than 16 from its spot, then walks back to where it sat and
  sits down again, whether you're there or not (if it can't get back, it sits where it ends up). It doesn't see
  through a wall, glass or leaves, and sees over a fence only from a little way back; it takes on only what it sees
  from its seat, so where you sit it decides what it guards. It leaves a spider alone by day and a calm pig zombie.
- **It costs wolves, or food.** Wolves on watch get hurt and don't heal by themselves. A creeper bitten by a wolf on
  watch turns on that wolf and blows up beside it; with Mobs Break Blocks off, as new worlds here start, that hurts the
  wolf and not your walls. One bitten by a wolf at your side still keeps coming for you.
- **A wolf that can't get to a monster it sees stays sitting and growls**, so one sat up a ledge four blocks high (a
  wolf will drop three) is out of the fighting and still an alarm.
- A sitting wolf hit by a monster or another player, or that gets up because something hurt you, as in beta, goes back
  to its spot when that fight is over, where it used to stay up and follow you. Your click stands it up for good, as
  ever.
- **Wary Creepers** is gone from the pack and its server package. Nothing in a world changes: it added no blocks or
  items, and lanterns are lanterns. The mod itself is unchanged in the
  [Faithful Library's release](https://github.com/studio-quire/faithful-library/releases), and the library's README
  says how to put the jar back into this pack if you want both.

**World Rules' Day Length rule is gone.** It made a day two or three times as long, and it had faults that weren't
worth what mending them would take. A world that had a longer day opens as before and has beta's day again: the sun
goes to where the world's own time puts it, so the hour jumps once, and nothing else in the world changes.

**New worlds start with More Faithful's own rules**, where they started with beta's. In a world made from this version
on, unless you choose otherwise on Create World's **World Rules...** page:

- **Mobs Break Blocks** is off: creepers and ghast fireballs still hurt, and leave the world alone.
- **Blasts Destroy Items** is off: an explosion throws items lying on the ground and destroys none.
- **Sleeping** is off: a bed sets your respawn point, at any hour, and the night goes on.
- **Monster Light** is Dark Only: no monster appears in any torch or lava light, where beta's appear in dim light.
  Spawners stay as beta's.
- **Apple Drops** is on: oak leaves sometimes drop an apple.
- **Falling Hit Bonus** is off: no half heart more on a hit.
- **Bows Wear Out** is on: a bow lasts 385 shots.

The other rules start as before: Keep Inventory off, Weather normal, Natural Monsters on. Every one can
still be changed, for a world you're about to make on Create World's page and for the one you're in on World Rules'
page in the settings.

**Worlds you already have keep their rules.** One made in 0.5.0 or before, or brought from anywhere else, keeps the
rules it has; to give it the new ones, set them on World Rules' page in the settings while you're in it. Sitting Wolves
Watch is new, so it's on there too, and in such a world Mobs Break Blocks is still on unless you turned it off: a
creeper that turns on a watching wolf breaks what's round it. If you'd rather it didn't, turn one of the two off on its
page in the settings (where a world's rules are locked, unlock them first: the README says how, under World Rules).

**What new worlds start with is your game's own, and an update doesn't change it.** The first time your game meets a
rule, it takes the pack's choice for it and keeps it in `.minecraft/config/faithful-rules.properties`, a line a rule.
After that only you change it, on Create World's page. So when a later version of More Faithful comes and you copy your
`.minecraft/config` into its instance, your new worlds go on starting as they did, even where that version chooses
differently, and only a rule that's new in it starts as the pack chose. A fresh instance, without your old config,
starts with that version's choices.

Updating from 0.5.0: import the new zip as a new instance and copy your worlds from the old one's `.minecraft/saves`,
and `.minecraft/mods-library/selection.txt` and `.minecraft/config` to keep your switches and settings. With the old
`selection.txt`, the title screen says once that `wary-creepers.jar` is gone and wasn't loaded: that's Wary Creepers
having left the pack, and nothing is wrong. 0.5.0 kept a
rule for new worlds only once you had changed it on Create World's page, so those stay as you set them (a tower style
too), and every rule you hadn't touched there starts with the choices above.

The server package starts a new world the same way. The pack's choices are in `pack.properties`, beside the start
scripts, and the server writes them to `config/faithful-rules.properties` at its first start. To start a new world
differently, put your own lines in that file before the first start (`rule.world-rules.sleeping=true`), or use `/rule`
after it. A world the server already has keeps its rules. This is the pack's choice, not the World Rules mod's: on its
own, in another setup, its rules still start as beta's.

## 0.5.0 (2026-11-10)

On the Faithful Library 0.5.0 and Still Faithful 0.6.0, on Old Faithful 0.2.4 and Pure Faithful 0.1.5.

**The Faithful Library's pack on Still Faithful is now More Faithful**, with its own releases here. Its numbers go on
from that pack's, so this is the one after 0.4.0. The mods are the same, all on, each with its switch, and so is the
server package. What's new is the name: the title screen says **More Faithful 0.5.0**, the settings in Options are
**More Faithful...**, and the pack has a seal of its own, crimson with the rim and β in silver. With every mod switched
off it's Still Faithful, and says so.

Worlds carry over as they are, since no mod's name or way of saving changed: import the new zip as a new instance and
copy your worlds from the old one's `.minecraft/saves` (and, to keep your switches,
`.minecraft/mods-library/selection.txt` and `.minecraft/config`). The files are now `MoreFaithful-<version>.zip` and
`MoreFaithful-server-<version>.zip`, where they were `StillFaithful-Library-...`.

**The Faithful Library's pack on Old Faithful is gone.** Each of our packs is now the one before it with more: Pure, Old
and Still Faithful, then More Faithful. A world from it opens in More Faithful as it was, with Still Faithful's changes
on, each of which has a switch under **Options > More Faithful... > Changes...**, on its Polish pages. The pack's own
instance keeps working; to update its mods, replace each jar in its `.minecraft/mods-library` with the new one from the
Faithful Library's release, under the old name (`alchemy-0.5.0.jar` becomes `alchemy.jar`). For the mods on Old Faithful
without those changes, put them into an Old Faithful instance yourself: the [Faithful
Library](https://github.com/studio-quire/faithful-library) has every mod on its own.

The Faithful Library itself goes on as the mods and Faithful Core as jars on their own, for people's own setups and for
writing mods.

## 0.4.0 and before

Released as the Faithful Library's packs, `StillFaithful-Library-<version>.zip` and `OldFaithful-Library-<version>.zip`:
[their changes](https://github.com/studio-quire/faithful-library/blob/main/CHANGELOG.md) are in the Faithful Library's
changelog, and the downloads on [its releases](https://github.com/studio-quire/faithful-library/releases).
