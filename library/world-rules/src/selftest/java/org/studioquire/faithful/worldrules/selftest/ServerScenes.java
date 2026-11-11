package org.studioquire.faithful.worldrules.selftest;

import java.util.List;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.Monster;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.GhastEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.ServerWorld;
import net.minecraft.world.World;
import org.studioquire.faithful.worldrules.WorldRules;
import org.studioquire.oldfaithful.core.api.Commands;
import org.studioquire.oldfaithful.core.api.Rule;
import org.studioquire.oldfaithful.core.impl.Rules;
import org.studioquire.oldfaithful.core.api.Server;

/** {@code ruletest <scene> [player]} on the real server: each scene prints PASS or FAIL for the task to read. */
@Environment(EnvType.SERVER)
final class ServerScenes {
	private ServerScenes() {
	}

	private static void check(String what, boolean ok) {
		System.out.println("[faithful-test] " + (ok ? "PASS " : "FAIL ") + what);
	}

	static void register() {
		// Sorts first and only the server has it, so the server's numbers aren't the client's: a client must go by the hello.
		org.studioquire.oldfaithful.core.impl.CoreNet.channel("aaa:server-only", 1);
		Commands.register("ruletest", "ruletest <scene>: test only", (args, reply) -> {
			ServerWorld world = (ServerWorld) Server.world(0);
			switch (args[0]) {
				case "explosions": explosions(world); blastedItems(world); break;
				case "bed": bed(world, player(args[1])); break;
				case "bednight": bedAtNight(world, player(args[1])); break;
				case "night": world.setTime(18000); break;
				case "kill": kill(world, player(args[1]), Boolean.parseBoolean(args[2])); break;
				case "kept": kept(player(args[1]), Boolean.parseBoolean(args[2])); break;
				case "set": set(args[1], args[2]); break;
				case "nether": player(args[1]).server.playerManager.changePlayerDimension(player(args[1])); break;
				case "spawning": spawning((ServerWorld) Server.world(Integer.parseInt(args[1]))); break;
				case "spawned": spawned((ServerWorld) Server.world(Integer.parseInt(args[1])), Boolean.parseBoolean(args[2])); break;
				case "peaceful": peaceful(); break;
				default: reply.say("no such scene");
			}
			System.out.println("[faithful-test] DONE " + args[0]);
		});
	}

	private static void set(String name, String value) {
		for (Rule r : Rules.all()) {
			if (r.id().endsWith(":" + name)) Rules.change(r, value);
		}
	}

	/** A test server has no monsters of its own, so it's given them for this, at night. */
	private static void spawning(ServerWorld w) {
		w.difficulty = 1;
		w.allowSpawning(true, false);
		w.setTime(18000);
		for (Object e : w.entities) {
			if (e instanceof Monster) ((Entity) e).markDead();
		}
	}

	private static void spawned(ServerWorld w, boolean on) {
		int monsters = 0;
		for (Object e : w.entities) {
			if (e instanceof Monster && !((Entity) e).dead) monsters++;
		}
		check("monsters " + (on ? "on" : "off") + " in " + (w.dimension.id == 0 ? "the overworld" : "the Nether") + ": " + monsters + " came by themselves", on ? monsters > 0 : monsters == 0);
	}

	private static void peaceful() {
		for (World w : Server.worlds()) {
			w.difficulty = 0;
			w.allowSpawning(false, false);
			w.setTime(6000);
		}
	}

	private static ServerPlayerEntity player(String name) {
		ServerPlayerEntity p = (ServerPlayerEntity) Server.player(name);
		if (p == null) throw new IllegalArgumentException("no player " + name);
		return p;
	}

	private static final int Y = 100;

	/** A slab of stone with air around it. */
	private static void slab(ServerWorld w, int x, int z) {
		slab(w, x, z, Block.STONE.id);
	}

	private static void slab(ServerWorld w, int x, int z, int block) {
		for (int dx = -8; dx <= 8; dx++) {
			for (int dz = -8; dz <= 8; dz++) {
				for (int y = Y - 8; y <= Y + 10; y++) w.setBlock(x + dx, y, z + dz, y >= Y - 2 && y <= Y ? block : 0);
			}
		}
	}

	private static int[] count(ServerWorld w, int x, int z) {
		int[] solidFire = new int[2];
		for (int dx = -8; dx <= 8; dx++) {
			for (int dz = -8; dz <= 8; dz++) {
				for (int y = Y - 8; y <= Y + 10; y++) {
					int id = w.getBlockId(x + dx, y, z + dz);
					if (id == Block.STONE.id || id == Block.DIRT.id) solidFire[0]++;
					else if (id == Block.FIRE.id) solidFire[1]++;
				}
			}
		}
		return solidFire;
	}

	private static void creeper(ServerWorld w, int x, int z) {
		CreeperEntity c = new CreeperEntity(w);
		c.setPosition(x + 0.5, Y + 1, z + 0.5);
		w.createExplosion(c, x + 0.5, Y + 1, z + 0.5, 3.0F);
	}

	private static void fireball(ServerWorld w, int x, int z) {
		GhastEntity g = new GhastEntity(w);
		g.setPosition(x + 0.5, Y + 3, z + 0.5);
		FireballEntity f = new FireballEntity(w, g, 0, -2, 0);
		f.setPosition(x + 0.5, Y + 2, z + 0.5);
		for (int i = 0; i < 100 && !f.dead; i++) f.tick();
	}

	private static void tnt(ServerWorld w, int x, int z) {
		TntEntity t = new TntEntity(w, x + 0.5, Y + 1, z + 0.5);
		t.fuse = 0;
		t.tick();
	}

	private static void blastedItems(ServerWorld w) {
		Vec3i spawn = w.getSpawnPos();
		double x = spawn.x + 0.5, y = Y + 16, z = spawn.z + 0.5; // high in the air, where the blasts break nothing
		for (boolean on : new boolean[] {false, true}) {
			set("blasts_destroy_items", String.valueOf(on));
			ItemEntity item = new ItemEntity(w, x + 1, y, z, new ItemStack(Item.DIAMOND));
			w.spawnEntity(item);
			w.createExplosion(null, x, y, z, 3.0F);
			check("blasts destroy items " + (on ? "on: the item is gone" : "off: the item is there, and thrown"), on ? item.dead : !item.dead && item.velocityX > 0);
			item.markDead();
		}
	}

	private static void explosions(ServerWorld w) {
		Vec3i spawn = w.getSpawnPos();
		int x = spawn.x, z = spawn.z;
		for (boolean on : new boolean[] {false, true}) {
			set("mobs_break_blocks", String.valueOf(on));
			String rule = on ? " (rule on)" : " (rule off)";
			slab(w, x, z);
			int stone = count(w, x, z)[0];
			creeper(w, x, z);
			int[] after = count(w, x, z);
			check("creeper breaks blocks: " + (stone - after[0]) + rule, on ? after[0] < stone : after[0] == stone);

			// A few, since a blast sets fires by chance. Beta then breaks every cell of its list, fires included, so none stay.
			int fires = 0, lost = 0;
			for (int i = 0; i < 6; i++) {
				slab(w, x, z, Block.DIRT.id); // a fireball's blast can't break stone
				fireball(w, x, z);
				after = count(w, x, z);
				lost += stone - after[0];
				fires += after[1];
			}
			check("ghast fireballs: " + lost + " blocks, " + fires + " fires" + rule, on ? lost > 0 : lost == 0 && fires == 0);

			slab(w, x, z);
			tnt(w, x, z);
			check("TNT breaks blocks either way" + rule, count(w, x, z)[0] < stone);
		}
		set("mobs_break_blocks", "true");
		slab(w, x, z);
		for (int dx = -8; dx <= 8; dx++) {
			for (int dz = -8; dz <= 8; dz++) {
				for (int y = Y - 2; y <= Y; y++) w.setBlock(x + dx, y, z + dz, 0);
			}
		}
	}

	private static void bed(ServerWorld w, ServerPlayerEntity player) {
		Vec3i spawn = w.getSpawnPos();
		int x = spawn.x + 3, z = spawn.z + 3, y = Y;
		w.setBlock(x, y - 1, z, Block.STONE.id);
		w.setBlock(x, y - 1, z + 1, Block.STONE.id);
		w.setBlock(x, y, z, Block.BED.id, 0);
		w.setBlock(x, y, z + 1, Block.BED.id, 8);
		set("sleeping", "false");
		w.setTime(18000);
		player.setPosition(x + 0.5, y + 1, z + 1.5);
		Block.BED.onUse(w, x, y, z, player);
		Vec3i at = player.getSpawnPos();
		check("sleeping off: a bed at night sets the respawn point at its head", at != null && at.x == x && at.y == y && at.z == z + 1);
		check("sleeping off: nobody sleeps", !player.isSleeping());
		player.setSpawnPos(null);
		w.setTime(6000);
		Block.BED.onUse(w, x, y, z, player);
		at = player.getSpawnPos();
		check("sleeping off: and by day", at != null && at.z == z + 1);
		set("sleeping", "true");
		player.setSpawnPos(null);
		Block.BED.onUse(w, x, y, z, player);
		check("sleeping on: by day beta says no and sets nothing", player.getSpawnPos() == null && !player.isSleeping());
	}

	/** The game takes in the time of day on its tick, so this comes a moment after {@code night}. */
	private static void bedAtNight(ServerWorld w, ServerPlayerEntity player) {
		Vec3i spawn = w.getSpawnPos();
		int x = spawn.x + 3, z = spawn.z + 3, y = Y;
		Block.BED.onUse(w, x, y, z, player);
		check("sleeping on: at night the player sleeps", player.isSleeping());
		player.wakeUp(true, true, false);
		w.setTime(6000);
		w.setBlock(x, y, z, 0);
		w.setBlock(x, y, z + 1, 0);
	}

	@SuppressWarnings("unchecked")
	private static List<Entity> near(ServerWorld w, ServerPlayerEntity p) {
		return (List<Entity>) w.getEntities(p, Box.createCached(p.x - 4, p.y - 4, p.z - 4, p.x + 4, p.y + 4, p.z + 4));
	}

	/** Dead, as the game's own damage ends: the join's invulnerability and the armor a big hit breaks don't come into it. */
	private static void kill(ServerWorld w, ServerPlayerEntity player, boolean kept) {
		for (Entity e : near(w, player)) {
			if (e instanceof ItemEntity) e.markDead();
		}
		player.inventory.addStack(new ItemStack(Item.DIAMOND, 7));
		player.inventory.armor[3] = new ItemStack(Item.IRON_HELMET, 1);
		player.health = 0;
		player.onKilledBy(null);
		int items = 0;
		for (Entity e : near(w, player)) {
			if (e instanceof ItemEntity && !e.dead) {
				items++;
				e.markDead(); // or the respawning player picks them up again
			}
		}
		check("death with the rule " + (kept ? "on drops nothing" : "off drops what was carried") + ": " + items + " items", kept ? items == 0 : items > 0);
	}

	private static void kept(ServerPlayerEntity player, boolean kept) {
		int diamonds = 0;
		for (ItemStack s : player.inventory.main) {
			if (s != null && s.itemId == Item.DIAMOND.id) diamonds += s.count;
		}
		boolean helmet = player.inventory.armor[3] != null;
		check("after the respawn: " + diamonds + " diamonds, helmet " + helmet, kept ? diamonds >= 7 && helmet : diamonds == 0 && !helmet);
		if (kept) {
			java.util.Arrays.fill(player.inventory.main, null);
			java.util.Arrays.fill(player.inventory.armor, null);
		}
		player.currentScreenHandler.sendContentUpdates();
		check("the world has a player at full health", WorldRules.keepInventory != null && player.health > 0);
	}
}
