# Writing a mod for the Faithful Library

For whoever is adding a mod to `library/`: the build is this repository's, so a mod is a folder there. Claude Opus 5.5, 2026-11-04. The Java API is 0.x and can still change between releases. What's permanent since 0.1.0: names (a mod's id and everything it declares), the save formats, the login's `MAGIC` and the `faithful` metadata key. If something here is missing or wrong, say so rather than working round it quietly.

**Read this, the javadoc in `oldfaithful-core/src/main/java/.../core/api/` and `api/client/`, and the decompiled game in
`local/decomp/b1.7.3-named` (`./gw decompileMinecraft`).** You shouldn't need `impl/` or `mixin/`. Biny names: `Block`, `Item`, `ItemStack`, `World`, `ScreenHandler`.

## What you're writing for

Beta 1.7.3 on Fabric. Mods compile to Java 8. A player may run your mod with any other library mods, on any edition (Pure, Old or Still Faithful),
in single player or on a server. A few facts follow, and they decide most of what can go wrong:

- **The world runs in one place.** In single player that's the client process; on a server, the server. Game logic goes behind `if (!world.isRemote)`. Beta's
  client on a server only predicts. A block's `onUse` runs on both: return `true` early on a client after anything visual.
- **Two jars.** Client and server are different classes. Anything marked `@Environment(EnvType.CLIENT)` or in `net.minecraft.client` doesn't exist on the server, and
  `ServerPlayerEntity` and `net.minecraft.server` don't exist on the client. Common code that touches one crashes on the other. Put client code in a package named `client` and
  a `faithful:client` entrypoint.
- **Numbers aren't yours.** A block's or item's id changes when mods are added. Never store one: not in NBT, a file or a `static final`. Ask `entry.get()` each time, or hold the `Block`; a number handed to a constructor in a factory is fine, as it dies with the process.
  An item stack saves itself by name; so do blocks in the world.
- **Mod blocks sit over a stand-in.** Read blocks through `world.getBlockId`, `getBlockMeta` or `chunk.getBlockId`, never `chunk.blocks` or `chunk.meta`.
- **Meta is 4 bits.** Sixteen values. Need more? A second block, a family or a block entity.

## A mod

```
library/my-mod/
  build.gradle
  gradle.properties                     oldfaithful_edition=library
  src/main/resources/fabric.mod.json
  src/main/resources/assets/my-mod/
    lang/en_US.lang
    textures/block/*.png                16x16; 16 wide and a multiple of 16 tall is an animation strip, two ticks a frame
    textures/item/*.png
    textures/gui/*.png                  screen backgrounds, 256x256; textures/mob/*.png for models, 64x32
    textures/armor/<set>_1.png, _2.png  as vanilla's two sheets
    sounds/**/*.ogg
  src/main/java/org/studioquire/faithful/mymod/MyMod.java
```

`build.gradle`:
```groovy
plugins { id 'oldfaithful.mod' }
dependencies {
    compileOnly project(':oldfaithful-core')
    // Only for client code that names LWJGL (OpenGL, Keyboard):
    compileOnly("org.studioquire.oldfaithful.natives:lwjgl:${lwjgl_version}") { transitive = false }
}
```

`fabric.mod.json`, as `library/world-rules` has it (`library/fixture-ore` is the smallest whole mod with content to copy from, its test included):
```json
{
  "schemaVersion": 1, "id": "my-mod", "version": "${version}", "name": "My Mod",
  "description": "One or two plain sentences; the first is the hint on the mod's page in the settings, cut at about 90 letters.",
  "authors": ["Whoever made it"],
  "license": "MIT", "environment": "*",
  "entrypoints": {
    "faithful:mod": ["org.studioquire.faithful.mymod.MyMod"],
    "faithful:client": ["org.studioquire.faithful.mymod.client.MyModClient"]
  },
  "depends": { "faithful-core": "*" },
  "custom": { "faithful": { "content": true, "network": 1 } }
}
```

Say `"content": true` if you declare blocks, items, entities, block entities or screens. A server with content refuses clients that lack it, so a mod that needn't, shouldn't.
Bump `"network"` when what crosses the wire changes (a slot count, spawn data): peers on different numbers are refused. `"match": true` is for a mod with no content whose two sides must agree
anyway, like one changing `maxCount`. A mod with neither is one-sided or shared, never refused, and has to cope with a peer without it (`channel.available()`).

**A client-only mod** (a map, a HUD): `"environment": "client"`, `faithful:client` as its only entrypoint, `"faithful": {}`, everything in a `client` package. It runs on any server, with the core or not, vanilla's too. A server with the core can see that a client has the core, by its login and its answer to the hello, and which of the server's channels it shares; it isn't told about client-only mods. There `channel.available()` is false and nothing answers, so use only what the client already has. In the same way a server-only mod with no content asks nothing of clients. `initClient` comes at the very start of the game's own start-up, so `Client.minecraft()` is there but its `options`, textures and the rest aren't: use those from a tick or a screen.

Your mod's id is permanent, because it's the namespace in players' saves. So is every name you declare, once shipped; `c.alias(oldName, name)` covers renames, in any namespace.

```java
public final class MyMod implements FaithfulMod {
	public static BlockEntry<MyOre> ore;
	public static ItemEntry<MyGem> gem;

	@Override public void content(Content c) {
		ore = c.block("my_ore", s -> new MyOre(s.id(), s.texture("my_ore"))).standIn(Block.STONE);
		gem = c.item("my_gem", s -> new MyGem(s.id(), s.texture("my_gem")));   // MyGem extends Item: its constructor is protected
	}

	@Override public void init(Mod mod) {
		Tools.requires(ore.get(), "pickaxe", Tools.LEVEL_IRON);
		Recipes.shaped(new ItemStack(Block.GOLD_BLOCK), "RR", "RR", 'R', gem.get());
		Worldgen.ore("my-mod:my_ore", ore.get(), 6, 2, 0, 32);
	}
}
```

Three phases, in order: `content` for every mod (names and factories; nothing exists yet), `lateContent` (every name is known: `c.blockNames()`, for a slab of every stone), then the core numbers and builds everything, then
`init` (hooks, recipes, rules, channels, attachments). The client's `initClient` follows. Declared things resolve with `.get()` from `init` on. Vanilla's constructors and setters are often protected, so a block is usually a small subclass of your own.

**A block**: extend vanilla's classes. The factory's `Spec` gives the id to pass and textures: `s.texture("name")` for your PNG, or `s.vanillaTile(n)` for one of `terrain.png`'s (a block's tiles live in one atlas, so reuse a vanilla tile this way, never by its bare number). Your tiles aren't next to each other, so a class that works out a tile from `textureId` (`CropBlock`'s `textureId + meta`, `SaplingBlock`'s 63 and 79) needs `getTexture` overridden. Hardness, sounds and the rest are vanilla's setters. Names come from lang: `tile.my-mod.my_ore.name=My Ore`, `item.my-mod.my_gem.name=My Gem`.

**One of beta's own things under another name or picture** (`library/blue-flowers`, whose Lupin Blue is beta's blue dye): a line in your lang file for beta's own key renames it (`item.dyePowder.blue.name=Lupin Blue`), as every mod's file is read over beta's. For its picture, `c.itemTexture("name")` in `content` gives the texture index of `textures/item/<name>.png` without an item of yours behind it, and a mixin on that item's `getTextureId` answers it (a client mixin: the method is the client's). Slots, the hand and dropped stacks then draw it as they draw your own items'. Both are the client's own, so on a server without your mod a player still sees your name and picture. A mod that asks for a sprite this way needs `"content": true` even with no block or item of its own, as the sprite is drawn from the content layer's atlases (so, as with any content mod, a server with it refuses players without it).

Trees and crops have base classes, because vanilla's only know vanilla's: `Leaves`, `Log`, `Crop`. They bring decay, fancy graphics, shears, fire, axes, bonemeal, farmland and drops by vanilla's rules. A sapling or anything else that bonemeal grows implements `Growth.Bonemealable`.

A `stand-in` is what a vanilla reader, or this game without your mod, sees there: pick what reads best, a vanilla block without a block entity. It's all or nothing: that reader sees the stand-in's declared meta or damage whatever yours is, and your meta, damage and data wait beside it until the mod is back. So a stand-in needs no mapping from your metas, and a worn pick of yours reads as a fresh stone one. `.itemStandIn(item)` for what its item is in that reader's eyes. An item of your own takes `.standIn(item)`, and defaults to a stick. `.noItem()` for crops and for lit variants, which need no lang name as nothing shows them; swap those with `BlockEntities.swap`, never a copy of vanilla's `updateLitState`, which leaves a stray block entity ticking. `c.family(a, b)` for blocks needing consecutive ids (liquids: vanilla's use `id ± 1`).

## Prefer a hook to a mixin

Every mixin is a future conflict, and forty mods reach for the same places. Before writing one, look in `api/`:

| | |
|---|---|
| Life | `Lifecycle`, `Ticks`, `Chunks`, `Worldgen`, `Server` (the running worlds and players, either side) |
| Players | `Players` (join, leave, drops, respawn, dimension change, keepInventory), `Beds`, `Attachment` |
| Blocks | `Blocks` (placed, broken, used), `Drops`, `Crop`, `Leaves`, `Log`, `Growth` (bonemeal, farmland), `Fire`, `Explosions`, `Lightning` |
| Entities | `Entities` (spawning, monsters appearing on their own, damage, death, natural spawn lists), `Creepers` (what frightens one; how it flees is the core's), `Content.entity`, `EntitySpawnData` |
| Items | `Stacks`, `Tools`, `Arrows` (what an arrow item flies as, for a shooter of your own), `Recipes`, `Tags`, `Armor`, `Names` (anyone's blocks and items by name) |
| Block entities | `Lookups`, `ItemStorage`, `SyncedBlockEntity`, `BlockEntities`, `Containers` |
| World rules, talk | `mod.rules()`, `Commands`, `mod.channel()`, `Sounds` |
| Client (`api/client`) | `Client` (the game, a world entered or left), `Hud`, `WorldRender`, `Screens`, `Keys`, `Input`, `PlayerRender`, `Camera`, `Renderers` (drawing, reading a tile), `ContainerScreens` |

How listeners combine is stated on each hook: **veto** (cancel is sticky, and everyone still hears), **filter** (chained; each sees the last result), **choice** (each proposes; the hook's rule picks), **notice**. Order between mods isn't yours to lean on. A listener that throws is logged and skipped, and turned off after five throws, so write as if yours might be next to one that does.

Need a mixin anyway? Fine, and say why in its comment: it's how we find what the core lacks. Rules: handler names `mymod$...`, `@WrapOperation`/`@ModifyExpressionValue` over `@Redirect`, never `@Overwrite` or a pasted vanilla body, `"defaultRequire": 1`, client-only targets in the config's `client` list, server-only in `server`. A mixin package holds only mixins. Local variable slots differ between the two jars. `@Environment` members exist on one side: `harness` will tell you.

## Things that bite

- **Stacks**: compare with `Stacks.canMerge`, copy with `Stacks.copy`; a hand-rolled `itemId ==` drops stack data. For a mod's data on a stack, `Stacks.data(stack)` (an NBT compound). Past `Stacks.MAX_WIRE_BYTES` (16 KB; measure with `Stacks.wireBytes`) the data stays with the server: it's safe and saved, a client gets the item without it, and `Stacks.withheld(stack)` tells a client that's why, so don't show such a stack as empty. For a lot of data, keep it per world and put an id in the damage, as maps do.
- **Per-player data**: an `Attachment` with a persist, sync and on-death policy, not a field on the player, which is a new object at every respawn. Not `DataTracker`: 32 ids and a crash on a duplicate.
- **Sounds**: `world.playSound` is silent on a server. `Sounds.play`.
- **Block entity data** a client must see: `SyncedBlockEntity` and `BlockEntities.sync`. Vanilla sends none but signs.
- **An inventory block**: `Containers.Slots` is the plain inventory for common code (`SimpleInventory` is client-only), and `BlockEntities.spill` in `onBreak` throws out what it held; copying vanilla's `new ItemStack(id, n, damage)` loses stack data. `ItemStorage` finds any `Inventory` by itself; implement `ItemStorage` to limit it, or register a provider (`Lookups`) to answer by side or hide it.
- **A screen**: on a server the client's handler must have the same slot count (`Containers`), or the client disconnects. Numbers on a screen go through `Containers.Tracked`, which is single player and a remote client in one, and ints where vanilla's packet has a short. `c.screen("my_screen", ...)` is the short name; `Containers.open` and `ContainerScreens.register` take `my-mod:my_screen`. GUI text: lang keys of your own, `gui.my-mod.progress`, through `TranslationStorage`.
- **Power**: the core has no energy system, on purpose. If your mod has machines, how they're powered is part of its design.
- **Channels**: what arrives came from the other side: check it. Keep messages small, and anything about a block or chunk `.world()` or it overtakes its chunk.
- **Commands** exist on servers only: beta's chat opens only there.
- **Sharing a material.** Names are namespaced, so two mods can each declare `my_ingot`. A recipe that should take either asks for a tag; with nobody providing it the recipe just never matches. Tag names are in `library/tags.txt`, which holds only vanilla's: a new material's tag goes in it with the mod that designs the material, `c:ingots/name` and the like.
- **Fire**: `Fire.flammable(block, burn, spread)` for wood-like blocks.
- **Worldgen**: decorations have their own `Random` from seed, chunk and feature name, so don't use `world.random`. Luminous blocks go in `decorate`, not `chunkBuilt`. Yours run after vanilla's, so tall grass is already there: find ground with `Worldgen.surface`, and let a tree replace plants. Vanilla's lake and dungeon features read what they find to decide, and then draw differently, so taking a solid cell out during decoration can move every ore after them in a neighbouring chunk. A cell is past all that once its chunk and the three to the north and west have `terrainPopulated`. `library/buried-towers` is the worked example of something big: a pure function of seed and site written in `chunkBuilt`, with block entities and lights put in by the decoration pass, and the decoration pass putting back what vanilla's features wrote into it. `Worldgen.onDungeonPlacing` stops a dungeon and keeps vanilla's draws; `Attachment.saved` and `Chunks.isSaved` read the disk with nothing loaded, for a mod that has to tell a chunk it made from one that was there before it.
- **Entities**: a mod's own entity may use `DataTracker` from id 16 (the warning above is about players). A tracker change that lasts under about 3 ticks never reaches clients. A server's players have no velocity to read, only positions, so measure over time.
- **Finding your own blocks**: don't scan. Keep positions in a persisted `Attachment.chunk`, added in `onPlaced` and removed in `onBreak`, and read the few chunks in range with `Chunks.isLoaded`.
- **Statistics**: mod content has none, on purpose.
- **Time**: `world.getTime()` is beta's one clock: a count of ticks, right for "every 100 ticks", whose remainder by 24000 is the hour (0 sunrise, 6000 noon, 18000 midnight). It jumps when a night is slept through and when `/time` is used. On a client on a server it's the server's, put right every second.
- **Animals despawn**, as beta's do: `LivingEntity.tryDespawn` takes any mob over 128 blocks from every player at once, and past 32 by chance, and an `AnimalEntity` is no exception. A test or a mod that leaves one somewhere has to allow for that, or override `canDespawn`.
- **An item's `use`** runs on both, the client's first, as a guess. A server only corrects the hand, and only if the hand ended different from what the client said it held *before* guessing, so a guess of a change the server didn't make stays on the client. Where the two can disagree (`use` that looks along the player's angles: a server's are a tick old), leave it to the server: `if (world.isRemote) return stack;`, and the answer arrives as a slot update. Anything else a server's `use` changes in the inventory never reaches the client unless you call `user.currentScreenHandler.sendContentUpdates()` there and then: the packet handler's own call right after is muted, and it marks those slots as sent. `library/water-bottles` does both.
- **`world.raycast`** walks its `start` along the ray, so that vector ends up beside the hit: make a new one for anything after.
- **A screen on a held item**: a server replaces the held stack with a clone after every right-click, so follow it by slot and a mark in its data, not by object. `insertItem` doesn't ask `Slot.canInsert`, so a `quickMove` must. A click merging two stacks changes a count without `markDirty`.
- **Models**: a `ModelPart` is one box with no children, and a model's texture is always 64x32.
- **GUIs**: a background is a 256x256 PNG, drawn from its top left: `minecraft.textureManager.bindTexture(minecraft.textureManager.getTextureId("/assets/my-mod/textures/gui/x.png"))`, then `drawTexture`. Its words are lang keys of your own, `gui.my-mod.x=...`, read with `TranslationStorage.getInstance().get(key)`.
- **Reading pixels**: a tile's colours come from `Renderers.tilePixels` and `averageColor`, with no drawing. If you do call `glReadPixels`, read RGBA, or set `GL_PACK_ALIGNMENT` to 1: rows are padded to 4 bytes, the size check doesn't count that, and RGB at an odd width into `w * h * 3` bytes overruns the buffer and takes the game down with no exception.
- **Keys**: two keys on one code both fire, and Controls shows them in red for the player to move one. Pick a default nobody has.

## Settings

Gameplay rules belong to the world: `mod.rules().bool("my_rule", false)` or `.choice("my_rule", "normal", "other")`, whose first value is beta's, in `init`, with `rule.my-mod.my_rule.name` and `.hint` in the lang file and `rule.my-mod.my_rule.other` for each word of a choice. They get a place on your mod's page in the settings, on Create World's World Rules... page, in `/rule` and in the world's file from that alone. A rule is read on a client on a server too, as the server's value, so a client can act on one. A world's rules can be locked by whoever owns it (Lock Rules on your page, `/rule lock` on a server), and then nothing changes them: there's nothing for you to do about it, and a rule named `lock` is reached by its whole name. A rule for what a world is made of rather than how it plays, where changing it partway would leave the world half one thing and half another (how its generated buildings look, say), is declared through `mod.rules().atCreation()`: it's set on Create World's page or in a server's world file before its first start, shown inside the world but not changed there or by `/rule`, and a world made before the rule existed has its default. Personal choices belong to the install: your mod's page in the settings' Mods menu, from `oldfaithful-settings`, which comes with the core.

In `build.gradle`, `compileOnly project(':oldfaithful-settings')`. In `fabric.mod.json`, beside your other entrypoints, `"oldfaithful-options:settings": ["org.studioquire.faithful.mymod.client.MyModSettings"]`. It's only touched when a screen opens.
```java
public final class MyModSettings implements SettingsProvider {
	@Override public void register(SettingsRegistry settings) {
		settings.add(Setting.onOff("my-mod.properties", "sparkle", "Sparkle", "Whether it sparkles.", true)
				.applyNow(v -> MyModClient.sparkle = Boolean.parseBoolean(v)));
	}
}
```
The values live in `config/my-mod.properties`, your own file, which the screen edits a line at a time. So at start-up you read it yourself, with the same defaults: `Properties p = ConfigFile.parse(ConfigFile.read(path))`, where `path` is `FabricLoader.getInstance().getConfigDir().resolve("my-mod.properties")`. A missing file reads as empty. From code (a key that toggles something), `ConfigFile.write(path, ConfigFile.set(ConfigFile.read(path), key, value))`, which leaves the rest as it was.

`add` puts it on your mod's page: a wide button in the Mods menu by your mod's name, leading to Enabled, if the mod is in the library's folder, then your rules and settings, in pairs of short buttons. There's nothing to declare or number, and paging takes care of itself. Without `applyNow` a change waits for a restart, and the screen says so. Besides `onOff` there's `choice(file, key, label, hint, default, value, word, value, word...)`. A label with its value and an asterisk has to fit 144 pixels, about 24 letters. Labels and hints are plain strings, not lang keys: beta has one language, and the screen is English in every edition.

## Testing

`./gw :library:my-mod:remapJar` to build; `./gw :harness:libraryStatic -Plibrary.mods=my-mod` in seconds; `./gw :harness:librarySelftest -Plibrary.mods=my-mod` for your own test; `./gw :harness:libraryTest -Plibrary.mods=my-mod` for everything (`docs/library-harness.md`). A mod comes with what it `depends` on. Each failure names your mod and the rule. `local/run/...` holds logs and pictures. Hidden clients only, never a visible window.

**Your own test** is a folder, `src/selftest`, and nothing in `build.gradle`: the plugin builds it into a mod of its own that's never shipped.

```
src/selftest/resources/fabric.mod.json
src/selftest/java/org/studioquire/faithful/mymod/selftest/MyModTest.java
```
```json
{
  "schemaVersion": 1, "id": "my-mod-selftest", "version": "1", "name": "My Mod self-test", "environment": "*",
  "entrypoints": { "faithful:test": ["org.studioquire.faithful.mymod.selftest.MyModTest"] },
  "depends": { "my-mod": "*" }
}
```
```java
public final class MyModTest extends WorldTest {
	@Override protected void script() {
		if (restarted()) {
			check("the ore came back", block(1, 0, 0) == MyMod.ore.get().id);
			return;
		}
		set(1, 0, 0, MyMod.ore.get());
		check("the ore reads back", block(1, 0, 0) == MyMod.ore.get().id);
		wait(40);
		then(() -> check("and it's still there two seconds on", block(1, 0, 0) == MyMod.ore.get().id));
		until("the crop grows", 600, () -> meta(2, 0, 0) > 0);
	}
}
```

That's a whole test. `librarySelftest` runs it four times: single player, single player reopened, a real server with a hidden client joined, and that server restarted. A `WorldTest` runs where the world runs, so one class covers both, with `world`, `player` and `x, y, z`, where the player stands on a stone floor 8 cells each way with clear air above: your plot, and your inventory, empty, with the hotbar's first slot held. `restarted()` says this is the same world after a save and restart, with your plot and inventory as you left them. The player goes through every mod's test, so they're whole at the start of yours, even if the last test left them dead. Plain code in `script()` runs at once; `then`, `wait`, `until` and `await` are steps, run in order and back to back in one tick until one holds (a `wait`, or an `until` or `await` not met yet), so the game moves on between two steps only where one of those stands between them. Your test's first steps run in the tick the player was moved to your plot. `check` is the one reporting call: a FAIL fails the run under your mod's name on either side, and a test that reports no checks fails too. Anything else you print is just output. It's common code, so `libraryStatic` holds it to the same rules as your mod.

**The plot is wherever the spawn was, snow included**, and the test server is on Peaceful (no monsters). On snow, beta's chunk tick turns still water open to the sky to ice as your test runs, unless the cell over the water has a block light of 10 or more: give your water glowstone or a torch beside it, or a block at y 127 over it (`library/buried-towers`' `WaysTest`, `library/water-bottles`' `BottlesTest`). On Peaceful, a player under full health heals a half-heart every second, so read health in the step that set it. And a player your test lets die stays the size of a dead one (0.2 high, eyes 0.1 up) even with their health set back, so nothing in the same test after a death can be read.

**What only a client can see** goes in a `ClientTest`, under `"faithful:test-client"`, in the same `script()` form. It can stand alone: a client-only mod's test has that one entrypoint and `"environment": "client"`. It runs after your world test, on your plot, with `mc`, `remote()` and `restarted()`. `scene("name", args)` runs a `scene("name", args -> ...)` your `WorldTest` declared, wherever the world runs, and waits for it to finish; on a server that's a real round trip. `picture("name")` saves what's in the window, world or screen, as `shots/<mod>/<name>.png` in the game's folder, so look at it. Real clicks go through `mc.interactionManager`.

**On a server without your mod**: a `ClientTest` under `"faithful:test-without"`. If the set has content, `librarySelftest` ends by joining a server with the core alone and then the real server, with the client as an op, so `/give` through `sendChatMessage` is how it gets things; a gift is thrown where the player looks, so look down first or it's never picked up. Nothing answers there, so no `scene`, and no plot: it's wherever the server spawned it, which can be in water, beside it or under trees, so a test that needs ground calls `ownGround(headroom)` first: the player led to open sky near by and lifted, a floor from `/give` placed under it, with `headroom` cells of air above, and the test goes on from `groundX`, `groundY`, `groundZ`. `library/water-bottles`' `WithoutTest` is one.

`-Plibrary.serverOnly` runs just the `WorldTest`s, on a real server with a bare socket for a player: no game client, about a minute, and light enough for a PC someone's using. A `ClientTest` and a test's own server commands don't run there, so it's a first check and not the whole test. `-Plibrary.singlePlayerOnly` is the other half: single player, fresh and reopened, with your `ClientTest`, and no server; about half the time. Either is for iterating; run the whole test before you're done.

Beside other mods: the stage in `libraryTest` runs every mod's test one after another in one world, each on its own plot, so stay on yours. `-Plibrary.seed=N` picks a server's world; single player's is fixed.

A few things that have cost people a run:
- The game's test stop skips beta's last save; the harness saves for you between a run and its restart, so don't.
- `Commands` run on the server's queue, after whatever packets are already in, and need the client to be an op (the test client is). `scene` has neither problem, so prefer it.
- A server reads an unloaded chunk as empty, writes to one are lost, and it only keeps chunks near players: stay near your plot. Beta moves an entity only while every chunk within 32 blocks of it is loaded, and the selftest server's view (3 chunks) doesn't reach that far past your plot unless it's by the spawn, so a test with something in flight well off its plot loads the chunks round it first (`world.getChunkSource().loadChunk`). A test that builds in the ground waits until the chunks it touches and the ones west and north of them are loaded and `terrainPopulated` (`Chunks.isLoaded`), or a decoration that comes later can put a lake in what it built.
- A new server's land leaves a long queue of block ticks (gravel's, over a hundred thousand), run a thousand a tick and the oldest first, so a block tick you schedule early on can wait far past its time. A test timing one waits for the queue first: `RulesTest` drops a sand block and sees it go.
- `spawn-animals=false` kills any animal on its first tick. The selftest server has it on.
- Real keys: `StartupCheck.pushKey(scancode, 0, down)` on a hidden client, SDL's scancodes (F3 is 60, E 8), from a step, which is an event the game takes on its next tick.
- A test that needs what a test server hasn't got can take it for a moment: the selftest server has `spawn-monsters=false` and `allow-nether=false`, so `world.difficulty` and `world.allowSpawning` for a moment of monsters, put back after, and a test of the Nether belongs in `libraryRulesTest`'s kind of scene.
- That makes the selftest server Peaceful, so a player on it heals half a heart a second by themselves: a test timing health sets `world.difficulty` to 1 for itself. In single player the game puts `world.difficulty` back from the options every tick.
- A client's `clickSlot` shows its guess at once, so wait a few ticks before a `scene` counts what the server has.
- A shift-click on a crafting result makes one lot, as beta's does: it only repeats when the next result is smaller than the last. Closing a bench with anything left in its grid throws it on the ground, where the player picks it up.
- A hidden client has no real mouse: `mc.interactionManager` and `clickSlot`, not input events. The game cancels mining each tick with no button held, so a test mines with `tryBreakBlock` on the world's side or sends the packets.
- Server-only code in a selftest (`ServerPlayerEntity`) goes in a class marked `@Environment(EnvType.SERVER)`, or a `server` package, like the mod's own client code in `client`.
