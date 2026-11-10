package org.studioquire.faithful.hookcheck.server;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.ServerWorld;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.EntitySpawnGroup;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.AlphaChunkStorage;
import net.minecraft.world.chunk.storage.RegionIo;
import net.minecraft.server.world.PlayerSaveHandler;
import net.minecraft.world.WorldProperties;
import net.minecraft.world.chunk.storage.ChunkStorage;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.storage.WorldStorage;
import org.studioquire.faithful.hookcheck.HookCheck;
import org.studioquire.oldfaithful.core.api.Commands;
import org.studioquire.oldfaithful.core.api.Fire;
import org.studioquire.oldfaithful.core.api.Lightning;
import org.studioquire.oldfaithful.core.impl.Core;
import org.studioquire.oldfaithful.core.api.Server;

/** {@code hooktest <scene> [player]} on the real server: each scene prints PASS or FAIL for the task to read. */
public final class ServerScenes {
	private static final int Y = 100;

	private ServerScenes() {
	}

	private static void check(String what, boolean ok) {
		System.out.println("[faithful-test] " + (ok ? "PASS " : "FAIL ") + what);
	}

	public static void register() {
		Commands.register("hookhi", "hookhi: test only, for anyone", true, (args, reply) -> reply.say("hello " + (reply.player() == null ? "console" : reply.player().name)));
		Commands.register("hookop", "hookop: test only, for ops", (args, reply) -> reply.say("hello op"));
		Commands.register("hookthrow", "hookthrow: test only", true, (args, reply) -> {
			System.out.println("[faithful-test] THROWS BEGIN");
			throw new IllegalStateException("hook-check throws on purpose");
		});
		Commands.register("hooktest", "hooktest <scene>: test only", (args, reply) -> {
			ServerWorld world = (ServerWorld) Server.world(0);
			switch (args[0]) {
				case "start": start(world); break;
				case "chunks": chunks(world); break;
				case "beingbuilt": beingBuilt(world); break;
				case "players": players(world, player(args[1])); break;
				case "blocks": blocks(world, player(args[1])); break;
				case "drops": drops(world, player(args[1])); break;
				case "growth": growth(world, player(args[1])); break;
				case "entities": entities(world, player(args[1])); break;
				case "fire": fire(world, player(args[1])); break;
				case "lightning": lightning(world); break;
				case "order": order(world, player(args[1])); break;
				case "throws": throwing(world, player(args[1])); break;
				case "endthrows": System.out.println("[faithful-test] THROWS END"); break;
				case "attach": attach(world, player(args[1])); break;
				case "kill": kill(player(args[1]), Boolean.parseBoolean(args[2])); break;
				case "respawned": respawned(world, player(args[1]), Boolean.parseBoolean(args[2])); break;
				case "dimension": dimension(player(args[1])); break;
				case "relogged": relogged(player(args[1]), args[2]); break;
				case "plain": plain(player(args[1])); break;
				case "chunkdata": chunkData(world); break;
				case "rewrite": rewrite(world); break;
				case "restarted": restarted(world); break;
				default: reply.say("no such scene");
			}
			System.out.println("[faithful-test] DONE " + args[0]);
		});
	}

	private static ServerPlayerEntity player(String name) {
		ServerPlayerEntity p = (ServerPlayerEntity) Server.player(name);
		if (p == null) throw new IllegalArgumentException("no player " + name);
		return p;
	}

	private static int spawnX(ServerWorld w) {
		return w.getSpawnPos().x;
	}

	private static int spawnZ(ServerWorld w) {
		return w.getSpawnPos().z;
	}

	/** An empty box with a stone floor at {@link #Y}. */
	private static void room(ServerWorld w, int x, int z) {
		for (int dx = -6; dx <= 6; dx++) {
			for (int dz = -6; dz <= 6; dz++) {
				for (int y = Y - 2; y <= Y + 8; y++) w.setBlock(x + dx, y, z + dz, y <= Y ? Block.STONE.id : 0);
			}
		}
		for (Object e : w.getEntities(null, Box.createCached(x - 8, Y - 3, z - 8, x + 8, Y + 9, z + 8))) {
			if (!(e instanceof ServerPlayerEntity)) ((Entity) e).markDead();
		}
	}

	private static void start(ServerWorld w) {
		check("the server's start was heard once, after both worlds' loads: " + HookCheck.summary(), HookCheck.count("serverStart") == 1 && HookCheck.count("worldLoad") == 2);
		check("the server ticks, and its worlds", HookCheck.count("serverTick") > 0 && HookCheck.count("worldTick") >= HookCheck.count("serverTick"));
		check("a dedicated server has no client ticks", HookCheck.count("clientTick") == 0);
		check("chunks were heard loading", HookCheck.count("chunkLoaded") > 0);
		int saves = HookCheck.count("worldSave");
		w.saveWithLoadingDisplay(true, null);
		check("a save was heard", HookCheck.count("worldSave") == saves + 1);
		@SuppressWarnings("unchecked")
		List<EntitySpawnGroup> list = (List<EntitySpawnGroup>) Biome.PLAINS.getSpawnableEntities(SpawnGroup.MONSTER);
		long zombies = list.stream().filter(e -> e.clazz == ZombieEntity.class).count();
		check("a natural spawn was added beside vanilla's: " + zombies, zombies == 2);
	}

	/** A world with nothing behind it: new each time, and nothing saved. */
	private static final class NoStorage implements WorldStorage {
		@Override public WorldProperties loadProperties() { return null; }
		@Override public void checkSessionLock() { }
		@Override public ChunkStorage getChunkStorage(Dimension dimension) { return null; }
		@Override public void save(WorldProperties properties, List players) { }
		@Override public void save(WorldProperties properties) { }
		@Override public File getWorldPropertiesFile(String name) { return null; }
		@Override public PlayerSaveHandler getPlayerSaveHandler() { return null; }
		@Override public void forceSave() { }
	}

	/** A new world's spawn search comes inside {@code World}'s constructor, before {@code ServerWorld} has its fields, and can decorate chunks. In place of its chance of about 1 in 900 of a spring's water reaching a plant: that, certainly. */
	private static final class BeingBuilt extends ServerWorld {
		static final int X = 8, Y = 120, Z = 8;

		BeingBuilt(long seed) {
			super(null, new NoStorage(), "being-built", 0, seed);
		}

		@Override
		protected void initializeSpawnPoint() {
			eventProcessingEnabled = true;
			getChunk(X >> 4, Z >> 4);
			setBlock(X, Y - 1, Z, Block.DIRT.id);
			setBlock(X + 1, Y - 1, Z, Block.STONE.id);
			setBlock(X + 2, Y, Z, Block.STONE.id);
			setBlock(X + 1, Y, Z - 1, Block.STONE.id);
			setBlock(X + 1, Y, Z + 1, Block.STONE.id);
			setBlock(X, Y, Z, Block.ROSE.id);
			setBlock(X + 1, Y, Z, Block.FLOWING_WATER.id);
			instantBlockUpdateEnabled = true;
			Block.FLOWING_WATER.onTick(this, X + 1, Y, Z, random);
			instantBlockUpdateEnabled = false;
			eventProcessingEnabled = false;
		}
	}

	private static void beingBuilt(ServerWorld real) {
		BeingBuilt built = new BeingBuilt(real.getSeed());
		long roses = 0, alive = 0;
		for (Object o : built.entities) {
			if (o instanceof ItemEntity && ((ItemEntity) o).stack.itemId == Block.ROSE.id) {
				roses++;
				if (!((ItemEntity) o).dead) alive++;
			}
		}
		check("a spring's water breaks a plant while a new server world is being built, and the server comes through it: " + roses + " rose", roses > 0);
		check("and that drop dies, as nothing could be tracking it", alive == 0);
		check("and water stands where the rose was", built.getBlockId(BeingBuilt.X, BeingBuilt.Y, BeingBuilt.Z) == Block.FLOWING_WATER.id);
	}

	private static void chunks(ServerWorld w) {
		int cx = 700, cz = 700;
		int loaded = HookCheck.count("chunkLoaded");
		w.chunkCache.loadChunk(cx, cz);
		check("a far chunk loading was heard", HookCheck.count("chunkLoaded") == loaded + 1);
		int unloaded = HookCheck.count("chunkUnloaded");
		w.chunkCache.isLoaded(cx, cz);
		for (int i = 0; i < 4 && HookCheck.count("chunkUnloaded") == unloaded; i++) w.chunkCache.tick();
		check("its unloading was heard", HookCheck.count("chunkUnloaded") > unloaded);
	}

	private static void players(ServerWorld w, ServerPlayerEntity p) {
		check("a join was heard, with the player in the world", HookCheck.count("joined") >= 1);
	}

	private static void blocks(ServerWorld w, ServerPlayerEntity p) {
		int x = spawnX(w), z = spawnZ(w);
		room(w, x, z);
		p.setPosition(x + 0.5, Y + 1, z + 0.5);

		HookCheck.reset();
		w.setBlock(x + 2, Y + 1, z, Block.DIRT.id);
		HookCheck.vetoBreak = true;
		boolean broke = p.interactionManager.tryBreakBlock(x + 2, Y + 1, z);
		HookCheck.vetoBreak = false;
		check("a vetoed break leaves the block, with every listener still heard", !broke && w.getBlockId(x + 2, Y + 1, z) == Block.DIRT.id && HookCheck.count("broken") == 1 && HookCheck.count("broken2") == 1);
		broke = p.interactionManager.tryBreakBlock(x + 2, Y + 1, z);
		check("the same break goes through without the veto", broke && w.getBlockId(x + 2, Y + 1, z) == 0);

		ItemStack stone = new ItemStack(Block.STONE, 5);
		HookCheck.vetoUse = true;
		boolean used = p.interactionManager.interactBlock(p, w, stone, x + 2, Y, z, 1);
		HookCheck.vetoUse = false;
		check("a vetoed use places nothing and uses nothing", !used && w.getBlockId(x + 2, Y + 1, z) == 0 && stone.count == 5);
		used = p.interactionManager.interactBlock(p, w, stone, x + 2, Y, z, 1);
		check("placing was heard with where it went", used && HookCheck.count("placed") == 1 && HookCheck.lastPlaced != null && HookCheck.lastPlaced.x == x + 2 && HookCheck.lastPlaced.y == Y + 1 && HookCheck.lastPlaced.block == Block.STONE.id);
		ItemStack door = new ItemStack(Item.WOODEN_DOOR, 1);
		p.interactionManager.interactBlock(p, w, door, x + 3, Y, z, 1);
		check("a door is heard by its lower half", HookCheck.count("placed") == 2 && HookCheck.lastPlaced.block == Block.DOOR.id && HookCheck.lastPlaced.y == Y + 1);
		w.setBlock(x + 4, Y + 1, z, Block.LEVER.id, 5);
		int before = w.getBlockMeta(x + 4, Y + 1, z);
		HookCheck.vetoUse = true;
		p.interactionManager.interactBlock(p, w, null, x + 4, Y + 1, z, 1);
		HookCheck.vetoUse = false;
		check("a vetoed use leaves a lever as it was", w.getBlockMeta(x + 4, Y + 1, z) == before);
		p.interactionManager.interactBlock(p, w, null, x + 4, Y + 1, z, 1);
		check("without the veto it flips", w.getBlockMeta(x + 4, Y + 1, z) != before);
	}

	private static long stacks(ServerWorld w, int x, int z, int item) {
		long n = 0;
		for (Object e : w.getEntities(null, Box.createCached(x - 8, Y - 3, z - 8, x + 8, Y + 9, z + 8))) {
			if (e instanceof ItemEntity && !((ItemEntity) e).dead && ((ItemEntity) e).stack.itemId == item) n += ((ItemEntity) e).stack.count;
		}
		return n;
	}

	private static void drops(ServerWorld w, ServerPlayerEntity p) {
		int x = spawnX(w), z = spawnZ(w);
		room(w, x, z);
		HookCheck.reset();
		HookCheck.addDrop = true;
		w.setBlock(x + 1, Y + 1, z, Block.DIRT.id);
		p.interactionManager.tryBreakBlock(x + 1, Y + 1, z);
		check("a player's break: the filter sees the player and the stack, and its extra falls", HookCheck.lastBlockDrops != null && HookCheck.lastBlockDrops.player == p && stacks(w, x, z, Item.APPLE.id) == 1 && stacks(w, x, z, Block.DIRT.id) == 1);

		// Each of wheat's three seeds is a roll, and all three can miss.
		boolean seeds = false;
		for (int i = 0; i < 40 && !seeds; i++) {
			room(w, x, z);
			w.setBlock(x + 1, Y + 1, z, Block.WHEAT.id, 7);
			Block.WHEAT.dropStacks(w, x + 1, Y + 1, z, 7);
			for (ItemStack s : HookCheck.lastBlockDrops.stacks) seeds |= s.itemId == Item.SEEDS.id;
		}
		check("wheat's hand-spawned seeds are in the list too: " + HookCheck.lastBlockDrops.stacks, seeds && HookCheck.lastBlockDrops.player == null);
		check("what fell matches the list", stacks(w, x, z, Item.SEEDS.id) > 0 && stacks(w, x, z, Item.WHEAT.id) == 1);

		room(w, x, z);
		int blockDrops = HookCheck.count("blockDrops");
		for (int i = 0; i < 40; i++) w.setBlock(x + 1 + i % 3, Y + 1 + i / 12, z + i % 4, Block.DIRT.id);
		w.createExplosion(null, x + 2, Y + 2, z + 1, 3.0F);
		check("an explosion's drops are heard, by the same filter", HookCheck.count("blockDrops") > blockDrops);

		room(w, x, z);
		CowEntity cow = new CowEntity(w);
		cow.setPosition(x + 1.5, Y + 1, z + 1.5);
		w.spawnEntity(cow);
		cow.health = 0;
		cow.onKilledBy(p);
		check("a mob's drop is heard, with what killed it, and its extra falls", HookCheck.lastMobDrops != null && HookCheck.lastMobDrops.entity == cow && HookCheck.lastMobDrops.adversary == p && stacks(w, x, z, Item.APPLE.id) >= 1);
		HookCheck.addDrop = false;
		room(w, x, z);
	}

	private static void growth(ServerWorld w, ServerPlayerEntity p) {
		int x = spawnX(w), z = spawnZ(w);
		room(w, x, z);
		w.setBlock(x + 1, Y, z, Block.FARMLAND.id, 0);
		w.setBlock(x + 1, Y + 1, z, Block.WHEAT.id, 1);
		ItemStack meal = new ItemStack(Item.DYE, 3, 15);
		HookCheck.reset();
		HookCheck.bonemealCancelled = true;
		boolean r = meal.useOnBlock(p, w, x + 1, Y + 1, z, 1);
		HookCheck.bonemealCancelled = false;
		check("cancelled bonemeal: nothing grows and none is used", !r && w.getBlockMeta(x + 1, Y + 1, z) == 1 && meal.count == 3 && HookCheck.count("bonemeal") == 1);
		HookCheck.bonemealHandled = true;
		r = meal.useOnBlock(p, w, x + 3, Y, z, 1);
		HookCheck.bonemealHandled = false;
		check("handled bonemeal is used once, on what vanilla would refuse", r && meal.count == 2);
		r = meal.useOnBlock(p, w, x + 1, Y + 1, z, 1);
		check("with nobody speaking, vanilla's wheat grows", r && w.getBlockMeta(x + 1, Y + 1, z) == 7 && meal.count == 1);

		w.setBlock(x + 1, Y + 1, z, 0);
		w.setBlock(x + 2, Y, z, Block.FARMLAND.id, 0);
		w.setBlock(x + 2, Y + 1, z, Block.DEAD_BUSH.id);
		Random rolls = new Random(1);
		for (int i = 0; i < 200; i++) {
			if (w.getBlockId(x + 1, Y, z) == Block.FARMLAND.id) Block.FARMLAND.onTick(w, x + 1, Y, z, rolls);
			Block.FARMLAND.onTick(w, x + 2, Y, z, rolls);
		}
		check("dry farmland under a declared crop stays, and bare farmland decays", w.getBlockId(x + 2, Y, z) == Block.FARMLAND.id && w.getBlockId(x + 1, Y, z) == Block.DIRT.id);
		room(w, x, z);
	}

	private static void entities(ServerWorld w, ServerPlayerEntity p) {
		int x = spawnX(w), z = spawnZ(w);
		room(w, x, z);
		HookCheck.reset();
		CowEntity cow = new CowEntity(w);
		cow.setPosition(x + 1.5, Y + 1, z + 1.5);
		HookCheck.vetoSpawn = true;
		boolean spawned = w.spawnEntity(cow);
		HookCheck.vetoSpawn = false;
		check("a vetoed spawn never joins the world", !spawned && !w.entities.contains(cow));
		spawned = w.spawnEntity(cow);
		check("the same spawn goes through", spawned && w.entities.contains(cow));

		cow.health = 10;
		HookCheck.vetoDamage = true;
		cow.damage(null, 4);
		HookCheck.vetoDamage = false;
		check("vetoed damage does nothing", cow.health == 10);
		HookCheck.doubleDamage = true;
		cow.damage(null, 2);
		HookCheck.doubleDamage = false;
		check("a filter's damage is what lands: " + cow.health, cow.health == 6);
		int died = HookCheck.count("died");
		cow.hearts = 0;
		cow.damage(p, 1000);
		check("a mob's death is heard, with what killed it", HookCheck.count("died") == died + 1 && HookCheck.lastDeath.entity == cow && HookCheck.lastDeath.adversary == p);
		room(w, x, z);
	}

	private static void fire(ServerWorld w, ServerPlayerEntity p) {
		int x = spawnX(w), z = spawnZ(w);
		room(w, x, z);
		HookCheck.reset();
		check("Fire.flammable reaches fire's own tables", Block.FIRE.isFlammable(placeSponge(w, x, z), x + 1, Y + 1, z));
		check("vanilla's stone stays out of them", !Block.FIRE.isFlammable(w, x, Y, z));
		w.setBlock(x + 1, Y + 1, z, 0);
		Random rolls = new Random(2);

		// Flint and steel, on the floor.
		ItemStack flint = new ItemStack(Item.FLINT_AND_STEEL, 1);
		HookCheck.vetoIgnite = true;
		flint.useOnBlock(p, w, x + 1, Y, z, 1);
		HookCheck.vetoIgnite = false;
		check("a refused flint and steel lights nothing and wears nothing", w.getBlockId(x + 1, Y + 1, z) == 0 && flint.getDamage() == 0 && HookCheck.IGNITIONS.contains(Fire.Cause.PLAYER));
		flint.useOnBlock(p, w, x + 1, Y, z, 1);
		check("without the veto it lights and wears", w.getBlockId(x + 1, Y + 1, z) == Block.FIRE.id && flint.getDamage() == 1);
		w.setBlock(x + 1, Y + 1, z, 0);

		// A fire eating planks, its ticks driven by hand.
		HookCheck.IGNITIONS.clear();
		for (int dx = 0; dx < 3; dx++) w.setBlock(x + 2 + dx, Y + 1, z, Block.PLANKS.id);
		w.setBlock(x + 1, Y + 1, z, Block.FIRE.id, 0);
		HookCheck.vetoBurn = true;
		for (int i = 0; i < 400 && HookCheck.count("burn") == 0; i++) {
			w.setBlock(x + 1, Y + 1, z, Block.FIRE.id, 0);
			Block.FIRE.onTick(w, x + 1, Y + 1, z, rolls);
		}
		HookCheck.vetoBurn = false;
		check("a vetoed burn leaves the block", HookCheck.count("burn") > 0 && w.getBlockId(x + 2, Y + 1, z) == Block.PLANKS.id);
		HookCheck.vetoIgnite = true;
		for (int i = 0; i < 2000 && !HookCheck.IGNITIONS.contains(Fire.Cause.SPREAD); i++) {
			w.setBlock(x + 1, Y + 1, z, Block.FIRE.id, 0);
			Block.FIRE.onTick(w, x + 1, Y + 1, z, rolls);
		}
		HookCheck.vetoIgnite = false;
		check("spreading fire is heard as SPREAD", HookCheck.IGNITIONS.contains(Fire.Cause.SPREAD));
		room(w, x, z);

		// TNT beside a fire: a vetoed burn must not set it off.
		w.setBlock(x + 2, Y + 1, z, Block.TNT.id);
		HookCheck.vetoBurn = true;
		int burns = HookCheck.count("burn");
		for (int i = 0; i < 400 && HookCheck.count("burn") == burns; i++) {
			w.setBlock(x + 1, Y + 1, z, Block.FIRE.id, 0);
			Block.FIRE.onTick(w, x + 1, Y + 1, z, rolls);
		}
		HookCheck.vetoBurn = false;
		long primed = w.entities.stream().filter(e -> e instanceof net.minecraft.entity.TntEntity).count();
		check("a vetoed burn doesn't set off TNT", HookCheck.count("burn") > burns && primed == 0 && w.getBlockId(x + 2, Y + 1, z) == Block.TNT.id);
		room(w, x, z);

		// Lava lighting a fire: a still lava block with planks above it.
		HookCheck.IGNITIONS.clear();
		w.setBlock(x, Y, z, Block.LAVA.id);
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				w.setBlock(x + dx, Y + 3, z + dz, Block.PLANKS.id);
			}
		}
		HookCheck.vetoIgnite = true;
		for (int i = 0; i < 4000 && !HookCheck.IGNITIONS.contains(Fire.Cause.LAVA); i++) Block.LAVA.onTick(w, x, Y, z, rolls);
		HookCheck.vetoIgnite = false;
		check("lava's fire is heard as LAVA, and a veto leaves the air", HookCheck.IGNITIONS.contains(Fire.Cause.LAVA) && noFire(w, x, z));
		room(w, x, z);

		// A strike's own fires need difficulty 2.
		HookCheck.IGNITIONS.clear();
		int difficulty = w.difficulty;
		w.difficulty = 2;
		HookCheck.vetoIgnite = true;
		new LightningEntity(w, x + 1, Y + 1, z + 1);
		HookCheck.vetoIgnite = false;
		check("a strike's fire is heard as LIGHTNING, and a veto leaves no fire", HookCheck.IGNITIONS.contains(Fire.Cause.LIGHTNING) && noFire(w, x, z));
		new LightningEntity(w, x + 1, Y + 1, z + 1);
		check("without the veto a strike lights", !noFire(w, x, z));
		w.difficulty = difficulty;
		room(w, x, z);

		HookCheck.IGNITIONS.clear();
		w.createExplosion(null, x + 0.5, Y + 2, z + 0.5, 3.0F, true);
		check("an explosion's fires are heard as EXPLOSION", HookCheck.IGNITIONS.contains(Fire.Cause.EXPLOSION));
		room(w, x, z);
	}

	private static ServerWorld placeSponge(ServerWorld w, int x, int z) {
		w.setBlock(x + 1, Y + 1, z, Block.SPONGE.id);
		return w;
	}

	private static boolean noFire(ServerWorld w, int x, int z) {
		for (int dx = -6; dx <= 6; dx++) {
			for (int dz = -6; dz <= 6; dz++) {
				for (int y = Y - 2; y <= Y + 8; y++) {
					if (w.getBlockId(x + dx, y, z + dz) == Block.FIRE.id) return false;
				}
			}
		}
		return true;
	}

	/** The real site in the world's tick, with its one-in-100,000 roll rigged and the weather made to thunder. */
	private static void lightning(ServerWorld w) {
		int strikes = HookCheck.count("strike");
		Random real = w.random;
		w.random = new Random() {
			@Override
			public int nextInt(int bound) {
				return bound == 100000 ? 0 : real.nextInt(bound);
			}
		};
		w.getProperties().setRaining(true);
		w.getProperties().setThundering(true);
		w.getProperties().setRainTime(100000);
		w.getProperties().setThunderTime(100000);
		HookCheck.cancelStrike = true;
		w.globalEntities.clear();
		for (int i = 0; i < 400 && HookCheck.count("strike") == strikes; i++) w.tick();
		HookCheck.cancelStrike = false;
		check("the weather's strike is heard at its site, and a cancelled one never exists", HookCheck.count("strike") > strikes && w.globalEntities.isEmpty());

		HookCheck.strikeTo = new int[] {spawnX(w) + 40, 90, spawnZ(w) + 40};
		strikes = HookCheck.count("strike");
		for (int i = 0; i < 40 && HookCheck.count("strike") == strikes; i++) w.tick();
		boolean moved = !w.globalEntities.isEmpty();
		for (Object e : w.globalEntities) moved &= ((Entity) e).x == HookCheck.strikeTo[0] && ((Entity) e).z == HookCheck.strikeTo[2];
		HookCheck.strikeTo = null;
		check("a proposal moves the real strike", moved);
		w.random = real;
		w.getProperties().setRaining(false);
		w.getProperties().setThundering(false);
		w.globalEntities.clear();
	}

	/** What's between two listeners, whichever order they run in. Under a salt the order may be the other way. */
	private static void order(ServerWorld w, ServerPlayerEntity p) {
		int x = spawnX(w), z = spawnZ(w);
		room(w, x, z);
		boolean natural = Core.orderKey("hook-check").compareTo(Core.orderKey("hook-check-second")) <= 0;
		System.out.println("[faithful-test] ORDER " + (natural ? "natural" : "salted"));
		CowEntity cow = new CowEntity(w);
		cow.setPosition(x + 1.5, Y + 1, z + 1.5);
		w.spawnEntity(cow);
		cow.health = 100;
		cow.maxHealth = 100;
		HookCheck.reset();
		HookCheck.doubleDamage = true;
		HookCheck.secondAddOne = true;
		cow.damage(null, 3);
		HookCheck.doubleDamage = false;
		HookCheck.secondAddOne = false;
		check("filters chain in order: " + HookCheck.SPOKEN + ", health " + cow.health, cow.health == 100 - (natural ? 3 * 2 + 1 : (3 + 1) * 2));
		check("the order is the salted one", HookCheck.SPOKEN.equals(natural ? Arrays.asList("hook-check", "hook-check-second") : Arrays.asList("hook-check-second", "hook-check")));

		HookCheck.secondSawCancelledBreak = false;
		w.setBlock(x + 2, Y + 1, z, Block.DIRT.id);
		HookCheck.vetoBreak = true;
		p.interactionManager.tryBreakBlock(x + 2, Y + 1, z);
		HookCheck.vetoBreak = false;
		check("a veto is sticky: the block stands, whichever spoke first", w.getBlockId(x + 2, Y + 1, z) == Block.DIRT.id);
		check("and a listener after it can read that", HookCheck.secondSawCancelledBreak == natural);

		int bx = x + 100, by = 80, bz = z;
		HookCheck.strikeTo = new int[] {bx + 5, by, bz};
		HookCheck.secondStrikeTo = new int[] {bx + 3, by, bz};
		int[] at = Lightning.strike(w, bx, by, bz);
		check("the nearest proposal wins", at != null && at[0] == bx + 3);
		HookCheck.strikeTo = new int[] {bx + 4, by, bz};
		HookCheck.secondStrikeTo = new int[] {bx - 4, by, bz};
		at = Lightning.strike(w, bx, by, bz);
		check("between equals, the lower mod id, whatever the order", at != null && at[0] == bx + 4);
		HookCheck.cancelStrike = true;
		check("a cancel beats a proposal", Lightning.strike(w, bx, by, bz) == null);
		HookCheck.cancelStrike = false;
		HookCheck.strikeTo = null;
		HookCheck.secondStrikeTo = null;
		cow.markDead();
		room(w, x, z);
	}

	/** Between these two lines of output the listener's stack traces are expected. */
	private static void throwing(ServerWorld w, ServerPlayerEntity p) {
		int x = spawnX(w), z = spawnZ(w);
		room(w, x, z);
		HookCheck.reset();
		HookCheck.throwing = true;
		HookCheck.thrown = 0;
		System.out.println("[faithful-test] THROWS BEGIN");
		ItemStack stone = new ItemStack(Block.STONE, 12);
		boolean all = true;
		for (int i = 0; i < 7; i++) all &= p.interactionManager.interactBlock(p, w, stone, x + 1 + i % 3, Y, z - 3 + i, 1);
		System.out.println("[faithful-test] THROWS END");
		HookCheck.throwing = false;
		check("a listener that throws stops neither the placing nor the game", all && stone.count == 5);
		check("nor the other listener on the hook, heard each time: " + HookCheck.count("placed2"), HookCheck.count("placed2") == 7);
		check("after five throws it's turned off: " + HookCheck.thrown, HookCheck.thrown == 5);
		room(w, x, z);
	}

	private static void attach(ServerWorld w, ServerPlayerEntity p) {
		HookCheck.mana.set(p, 7);
		HookCheck.rage.set(p, 9);
		HookCheck.pouch.set(p, 3);
		HookCheck.scratch.set(p, 5);
		check("a player's values read back", HookCheck.mana.get(p) == 7 && HookCheck.rage.get(p) == 9 && HookCheck.pouch.get(p) == 3 && HookCheck.scratch.get(p) == 5);
		NbtCompound nbt = new NbtCompound();
		p.write(nbt);
		NbtCompound attachments = nbt.getCompound("Faithful").getCompound("Attachments");
		check("the persisted ones are in the player's NBT under Faithful, and the rest aren't", attachments.contains("hook-check:mana") && attachments.contains("hook-check:pouch") && !attachments.contains("hook-check:scratch"));

		int x = spawnX(w), z = spawnZ(w);
		CowEntity cow = new CowEntity(w);
		cow.setPosition(x + 1.5, Y + 1, z + 1.5);
		w.spawnEntity(cow);
		HookCheck.tag.set(cow, "kept");
		Chunk chunk = w.getChunk(x >> 4, z >> 4);
		HookCheck.note.set(chunk, "spawn chunk");
		check("setting a chunk's value marks it for saving", chunk.dirty);
		NbtCompound level = new NbtCompound();
		AlphaChunkStorage.saveChunkToNbt(chunk, w, level);
		check("a chunk's value is under Level.Faithful.Attachments", level.getCompound("Faithful").getCompound("Attachments").contains("hook-check:note"));
	}

	private static void kill(ServerPlayerEntity p, boolean keep) {
		HookCheck.keepAll = keep;
		p.health = 0;
		p.onKilledBy(null);
		HookCheck.keepAll = false;
		check("a player's death is heard by Entities.onDied", HookCheck.lastDeath != null && HookCheck.lastDeath.entity == p);
		// Taken away now, or the respawning player picks them up again.
		long gold = 0;
		for (Object e : p.world.getEntities(p, Box.createCached(p.x - 8, p.y - 8, p.z - 8, p.x + 8, p.y + 8, p.z + 8))) {
			if (!(e instanceof ItemEntity)) continue;
			if (((ItemEntity) e).stack.itemId == Item.GOLD_INGOT.id) gold += ((ItemEntity) e).stack.count;
			((Entity) e).markDead();
		}
		check(keep ? "a kept inventory lets nothing fall from an attachment" : "DROP's stacks fell: " + gold, gold == (keep ? 0 : 3));
	}

	private static void respawned(ServerWorld w, ServerPlayerEntity p, boolean kept) {
		check("the respawn hook gave the old and new objects", HookCheck.reborn == p && HookCheck.dead != p && HookCheck.dead.name.equals(p.name));
		check("KEEP carried over, persisted or not", HookCheck.mana.get(p) == 7 && HookCheck.scratch.get(p) == 5);
		check("RESET is back at the default: " + HookCheck.rage.get(p), HookCheck.rage.get(p) == 0 && !HookCheck.rage.has(p));
		check(kept ? "DROP stays when the inventory is kept" : "DROP is gone", kept ? HookCheck.pouch.get(p) == 3 : HookCheck.pouch.get(p) == null);
		HookCheck.rage.set(p, 9);
		HookCheck.pouch.set(p, 3);
	}

	private static void dimension(ServerPlayerEntity p) {
		int heard = HookCheck.count("dimension");
		p.server.playerManager.changePlayerDimension(p);
		boolean there = HookCheck.count("dimension") == heard + 1 && HookCheck.lastDimension.from == 0 && HookCheck.lastDimension.to == -1;
		check("a dimension change is heard, from and to", there);
		check("attachments are still there in the Nether", HookCheck.mana.get(p) == 7 && HookCheck.rage.get(p) == 9 && HookCheck.pouch.get(p) == 3 && HookCheck.scratch.get(p) == 5);
		p.server.playerManager.changePlayerDimension(p);
		check("and on the way back", HookCheck.lastDimension.from == -1 && HookCheck.lastDimension.to == 0 && HookCheck.mana.get(p) == 7);
	}

	private static void relogged(ServerPlayerEntity p, String after) {
		check("a persisted value came back after " + after, HookCheck.mana.get(p) == 7 && HookCheck.rage.get(p) == 9 && HookCheck.pouch.get(p) == 3);
		check("one not persisted didn't", !HookCheck.scratch.has(p));
	}

	private static void plain(ServerPlayerEntity p) {
		NbtCompound nbt = new NbtCompound();
		p.write(nbt);
		check("a player with no values writes no Faithful key", !nbt.contains("Faithful"));
	}

	private static byte[] bytes(NbtCompound level) throws IOException {
		NbtCompound root = new NbtCompound();
		root.put("Level", (net.minecraft.nbt.NbtElement) level);
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(bytes);
		NbtIo.write(root, out);
		out.close();
		return bytes.toByteArray();
	}

	private static void chunkData(ServerWorld w) {
		int cx = (spawnX(w) >> 4) + 3, cz = spawnZ(w) >> 4;
		Chunk chunk = w.getChunk(cx, cz);
		int x = cx * 16 + 4, z = cz * 16 + 4;
		w.setBlock(x, Y, z, Block.SIGN.id);
		SignBlockEntity sign = (SignBlockEntity) w.getBlockEntity(x, Y, z);
		check("a sign to test with", sign != null);

		NbtCompound level = new NbtCompound();
		AlphaChunkStorage.saveChunkToNbt(chunk, w, level);
		check("a chunk with nothing to say has no Faithful key", !level.contains("Faithful"));
		NbtCompound faithful = new NbtCompound();
		faithful.putByte("V", (byte) 1);
		faithful.putByteArray("Cells", new byte[] {1, 2, 3});
		NbtCompound attachments = new NbtCompound();
		NbtCompound gone = new NbtCompound();
		gone.putInt("v", 42);
		attachments.put("gone-mod:thing", gone);
		NbtCompound note = new NbtCompound();
		note.putString("v", "from disk");
		attachments.put("hook-check:note", note);
		faithful.put("Attachments", attachments);
		level.put("Faithful", faithful);

		int loads = HookCheck.sectionLoads;
		HookCheck.sectionRanBeforeBlockEntities = true;
		Chunk read = AlphaChunkStorage.loadChunkFromNbt(w, level);
		check("sections run in the load, before block entities are added", HookCheck.sectionLoads == loads + 1 && HookCheck.sectionRanBeforeBlockEntities && !read.blockEntities.isEmpty());
		check("a chunk's value is read", "from disk".equals(HookCheck.note.get(read)));

		NbtCompound again = new NbtCompound();
		AlphaChunkStorage.saveChunkToNbt(read, w, again);
		NbtCompound out = again.getCompound("Faithful");
		check("unknown keys stay as they were", out.contains("V") && Arrays.equals(out.getByteArray("Cells"), new byte[] {1, 2, 3}));
		check("so does an unknown attachment, beside a known one", out.getCompound("Attachments").getCompound("gone-mod:thing").getInt("v") == 42 && out.getCompound("Attachments").contains("hook-check:note"));

		NbtCompound damaged = new NbtCompound();
		AlphaChunkStorage.saveChunkToNbt(chunk, w, damaged);
		damaged.putInt("Faithful", 3);
		Chunk survivor = AlphaChunkStorage.loadChunkFromNbt(w, damaged);
		NbtCompound after = new NbtCompound();
		AlphaChunkStorage.saveChunkToNbt(survivor, w, after);
		check("a Faithful entry that isn't a compound loses nothing: the chunk loads, and it's written back", survivor != null && after.contains("Faithful") && after.getInt("Faithful") == 3);
		w.setBlock(x, Y, z, 0);
	}

	/**
	 * Chunks written by a server without the core, read and written back here. With nothing to say, what comes out must be what went
	 * in, byte for byte, but for the time of the save.
	 */
	private static void rewrite(ServerWorld w) {
		File dir = new File("world");
		int compared = 0, same = 0;
		try {
			for (int cx = -64; cx < 64 && compared < 200; cx++) {
				for (int cz = -64; cz < 64 && compared < 200; cz++) {
					DataInputStream in = RegionIo.getChunkInputStream(dir, cx, cz);
					if (in == null) continue;
					NbtCompound level = NbtIo.read(in).getCompound("Level");
					in.close();
					byte[] before = bytes(level);
					Chunk chunk = AlphaChunkStorage.loadChunkFromNbt(w, level);
					NbtCompound out = new NbtCompound();
					AlphaChunkStorage.saveChunkToNbt(chunk, w, out);
					out.putLong("LastUpdate", level.getLong("LastUpdate"));
					compared++;
					if (Arrays.equals(before, bytes(out))) same++;
				}
			}
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		check("a chunk with nothing to say is byte for byte what a server without the core wrote: " + same + " of " + compared, compared == 200 && same == compared);
	}

	private static void restarted(ServerWorld w) {
		int x = spawnX(w), z = spawnZ(w);
		check("a chunk's value survives a restart", "spawn chunk".equals(HookCheck.note.get(w.getChunk(x >> 4, z >> 4))));
		boolean cow = false;
		for (Object e : w.entities) cow |= e instanceof CowEntity && "kept".equals(HookCheck.tag.get((Entity) e));
		check("an entity's value survives a restart", cow);
	}
}
