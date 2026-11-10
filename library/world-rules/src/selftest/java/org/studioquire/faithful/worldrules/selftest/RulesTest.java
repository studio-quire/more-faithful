package org.studioquire.faithful.worldrules.selftest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.Monster;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipeManager;
import net.minecraft.screen.ScreenHandler;
import org.studioquire.faithful.harness.test.WorldTest;
import org.studioquire.faithful.worldrules.WorldRules;
import org.studioquire.oldfaithful.core.api.Days;
import org.studioquire.oldfaithful.core.api.Rule;
import org.studioquire.oldfaithful.core.impl.Rules;

/**
 * Day length, weather, natural monsters, apples and bows where the world runs. It leaves a double day and rain for the restart to find; the client's half is {@code RulesClientTest}.
 * After the restart it locks the rules and checks they hold, then unlocks them as a player would, by the line in the world's file, read again as a world opening reads it, so
 * the world is as it was for any test after this one.
 */
public final class RulesTest extends WorldTest {
	private static final int LEAVES = 4000;

	private long clock, ticks;

	private void pace(String what, int n, int divisor) {
		then(() -> {
			clock = Days.time(world);
			ticks = world.getTime();
		});
		wait(n);
		then(() -> {
			long passed = world.getTime() - ticks, moved = Days.time(world) - clock;
			check(what + ": ticks keep their pace, the sun takes " + divisor + " of them: " + passed + ", " + moved, Math.abs(passed - n) <= 1 && Math.abs(moved - (double) passed / divisor) <= 1);
		});
	}

	/** The open world's file, found by its lock: the one world here whose rules are locked. */
	private static Path lockedFile() {
		try (Stream<Path> files = Files.find(FabricLoader.getInstance().getGameDir(), 3, (p, a) -> p.getFileName().toString().equals("faithful-world.properties"))) {
			for (Path p : (Iterable<Path>) files::iterator) {
				if (new String(Files.readAllBytes(p), StandardCharsets.UTF_8).contains(Rules.LOCK_KEY + "=true")) return p;
			}
			return null;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/** The line out, and the file read again as a world opening reads it; the clients told. */
	static void unlock() {
		Path file = lockedFile();
		if (file == null) return;
		try {
			List<String> kept = new ArrayList<>(Files.readAllLines(file));
			kept.remove(Rules.LOCK_KEY + "=true");
			Files.write(file, kept);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		Rules.open(file.getParent());
		Rules.sendTo(null);
	}

	private void lock() {
		then(() -> {
			check("lock: the rules lock, and the world's file says so", Rules.lock() && Rules.locked() && lockedFile() != null);
			check("lock: a change is refused, the rule as it was", !Rules.change(WorldRules.weather, "rain") && !Rules.change(WorldRules.keepInventory, "true")
					&& WorldRules.weather.get().equals("normal") && !WorldRules.keepInventory.get());
			unlock();
			check("unlocked by the line: a change is taken again", !Rules.locked() && Rules.change(WorldRules.keepInventory, "true") && WorldRules.keepInventory.get());
			Rules.change(WorldRules.keepInventory, "false");
		});
	}

	private boolean raining() {
		return world.getProperties().getRaining();
	}

	@SuppressWarnings("unchecked")
	private List<Entity> monsters() {
		List<Entity> found = new ArrayList<>();
		for (Entity e : (List<Entity>) world.entities) {
			if (e instanceof Monster && !e.dead) found.add(e);
		}
		return found;
	}

	private void day() {
		check("a new world: beta's day, one clock for the two", Days.length() == Days.NORMAL && Days.time(world) == world.getTime());
		then(() -> Rules.change(WorldRules.dayLength, "double"));
		pace("double", 60, 2);
		then(() -> Rules.change(WorldRules.dayLength, "triple"));
		pace("triple", 60, 3);
		// A new server's land leaves a queue of block ticks, a thousand run a tick and the oldest first, which a timing has to come after.
		then(() -> set(2, 2, 0, Block.SAND));
		until("the new land's block ticks are done", 1000, () -> block(2, 2, 0) == 0);
		wait(30);
		then(() -> {
			set(2, 0, 0, null);
			world.setTime(18000);
			check("setting the time sets the hour", Days.time(world) == 18000 && world.getTime() == 18000);
			set(2, 0, 0, Block.FLOWING_WATER);
		});
		until("water runs at beta's pace in a long day", 12, () -> block(3, 0, 0) == Block.FLOWING_WATER.id);
		then(() -> {
			for (int dx = 0; dx <= 7; dx++) {
				for (int dz = -7; dz <= 7; dz++) set(dx, 0, dz, null);
			}
			check("and night falls by the sun's clock: " + world.ambientDarkness, world.ambientDarkness >= 10);
		});
	}

	/** The same night's sleep is the weather's test: beta clears the weather when it ends. */
	private void sleep() {
		then(() -> {
			Rules.change(WorldRules.naturalMonsters, "false");
			Rules.change(WorldRules.weather, "rain");
			set(2, -1, 0, Block.STONE);
			set(2, -1, 1, Block.STONE);
			set(2, 0, 0, Block.BED, 0);
			set(2, 0, 1, Block.BED, 8);
		});
		until("it rains", 150, () -> world.isRaining());
		then(() -> {
			Block.BED.onUse(world, x + 2, y, z, player);
			check("a bed works at night in a long day", player.isSleeping());
		});
		until("everyone slept", 300, () -> !player.isSleeping());
		then(() -> {
			long hour = Days.time(world) % Days.NORMAL;
			check("it's sunrise by the sun's clock: " + hour, hour < 100);
			check("and it hasn't stopped raining", raining());
			set(2, 0, 0, null);
			set(2, 0, 1, null);
		});
		wait(5);
		then(() -> check("nor does it stop after: " + world.getRainGradient(1.0F), raining() && world.isRaining()));
	}

	private void weather() {
		then(() -> {
			Rules.change(WorldRules.weather, "clear");
			world.getProperties().setRainTime(1);
			world.getProperties().setThundering(true);
		});
		until("clear: the sky clears", 150, () -> !world.isRaining());
		wait(5);
		then(() -> {
			check("clear: and the rain's timer can't bring it back", !raining());
			check("clear: thunder is nothing without rain", !world.isThundering());
			Rules.change(WorldRules.weather, "rain");
			world.getProperties().setRainTime(1);
			world.getProperties().setThunderTime(100);
		});
		wait(10);
		then(() -> {
			check("rain: the rain's timer can't end it", raining());
			check("rain: thunder keeps its own cycle: " + world.getProperties().getThunderTime(), world.getProperties().getThundering() && world.getProperties().getThunderTime() <= 90);
			world.getProperties().setThundering(false);
		});
	}

	/** A test server has no monsters of its own, so it's given them for this. Only what a dark night sends: nothing here spawns one by hand. */
	private void monsters(boolean on) {
		world.difficulty = 1;
		world.allowSpawning(true, true);
		for (Entity e : monsters()) e.markDead();
		world.setTime(18000);
		Rules.change(WorldRules.naturalMonsters, String.valueOf(on));
	}

	private void naturalSpawning() {
		then(() -> monsters(true));
		until("monsters on: one comes by itself", 1500, () -> !monsters().isEmpty());
		then(() -> monsters(false));
		wait(400);
		then(() -> {
			check("monsters off: none in twenty seconds of night: " + monsters(), monsters().isEmpty());
			Rules.change(WorldRules.naturalMonsters, "true");
			if (!singlePlayer()) {
				world.difficulty = 0;
				world.allowSpawning(false, true);
			}
			world.setTime(6000);
		});
	}

	/** What this many leaves left, apples counted and everything taken away. */
	@SuppressWarnings("unchecked")
	private int applesFrom(Runnable leaf) {
		for (int i = 0; i < LEAVES; i++) leaf.run();
		int apples = 0;
		for (Entity e : (List<Entity>) world.entities) {
			if (!(e instanceof ItemEntity) || e.dead) continue;
			if (((ItemEntity) e).stack.itemId == Item.APPLE.id) apples++;
			e.markDead();
		}
		return apples;
	}

	private void decay(int meta) {
		set(3, 1, 3, Block.LEAVES, meta | 8);
		Block.LEAVES.onTick(world, x + 3, y + 1, z + 3, world.random);
	}

	private void broken(int meta) {
		Block.LEAVES.afterBreak(world, player, x + 3, y + 1, z + 3, meta);
	}

	/** At 1 in 200, four thousand leaves leave twenty apples, and outside these bounds by chance about once in fifty thousand runs. */
	private void apples() {
		then(() -> {
			check("off, as beta: no apple from oak", applesFrom(() -> decay(0)) == 0 && applesFrom(() -> broken(0)) == 0);
			Rules.change(WorldRules.appleDrops, "true");
			int decayed = applesFrom(() -> decay(0)), broken = applesFrom(() -> broken(0));
			check("on: oak leaves that decay drop apples, about 20: " + decayed, decayed >= 5 && decayed <= 45);
			check("on: oak leaves broken by hand drop them too, about 20: " + broken, broken >= 5 && broken <= 45);
			check("on: not spruce or birch", applesFrom(() -> decay(1)) == 0 && applesFrom(() -> decay(2)) == 0 && applesFrom(() -> broken(1)) == 0 && applesFrom(() -> broken(2)) == 0);
			player.inventory.main[player.inventory.selectedSlot] = new ItemStack(Item.SHEARS);
			check("on: not leaves taken with shears", applesFrom(() -> broken(0)) == 0);
			player.inventory.main[player.inventory.selectedSlot] = null;
			Rules.change(WorldRules.appleDrops, "false");
			set(3, 1, 3, null);
		});
	}

	/** What a bare-handed hit takes from a fresh pig, falling or not. */
	private int hit(double velocityY, boolean onGround) {
		boolean wasOnGround = player.onGround;
		player.onGround = onGround;
		PigEntity pig = new PigEntity(world);
		pig.setPosition(x + 2, y, z + 2);
		world.spawnEntity(pig);
		int before = pig.health;
		player.velocityY = velocityY;
		player.attack(pig);
		player.velocityY = 0.0;
		player.onGround = wasOnGround;
		int taken = before - pig.health;
		pig.markDead();
		return taken;
	}

	private boolean stoppedAt(net.minecraft.entity.LivingEntity entity, int dx, int dz) {
		entity.setPosition(x + dx + 0.5, y, z + dz + 0.5);
		return WorldRules.lightStops(entity);
	}

	private void monsterLight() {
		then(() -> set(4, 0, 4, Block.TORCH));
		// Beta spreads light in batches, and a new server has a queue of them.
		until("the torch's light reaches ten cells off", 400, () -> world.getBrightness(net.minecraft.world.LightType.BLOCK, x + 4, y, z + 4 - 10) > 0);
		then(() -> {
			ZombieEntity zombie = new ZombieEntity(world);
			PigEntity pig = new PigEntity(world);
			check("beta: light stops nothing outright", !stoppedAt(zombie, 4, 3));
			Rules.change(WorldRules.monsterLight, "none");
			check("none: a monster in a torch's light is stopped", stoppedAt(zombie, 4, 3));
			check("none: a monster in its dim edge is too, where beta would let some through", stoppedAt(zombie, 4, 4 - 10));
			check("none: and where no block's light reaches, beta's rolls decide", !stoppedAt(zombie, 4, 4 - 16));
			check("none: an animal isn't", !stoppedAt(pig, 4, 3));
			Rules.change(WorldRules.monsterLight, "beta");
			set(4, 0, 4, null);
		});
	}

	private void fallingBonus() {
		then(() -> {
			check("beta: one more while moving down, standing on the ground included", hit(-0.0784, true) == 2 && hit(-0.5, false) == 2 && hit(0.2, false) == 1);
			Rules.change(WorldRules.fallingBonus, "fixed");
			check("fixed: one more only in the air", hit(-0.0784, true) == 1 && hit(-0.5, false) == 2 && hit(0.2, false) == 1);
			Rules.change(WorldRules.fallingBonus, "off");
			check("off: never", hit(-0.0784, true) == 1 && hit(-0.5, false) == 1 && hit(0.2, false) == 1);
			Rules.change(WorldRules.fallingBonus, "beta");
		});
	}

	private int count(Item item) {
		int n = 0;
		for (ItemStack s : player.inventory.main) {
			if (s != null && s.itemId == item.id) n += s.count;
		}
		return n;
	}

	/** Arrows in flight or lying about, gone, so none is picked up later, here or on another test's plot. */
	@SuppressWarnings("unchecked")
	private int clearArrows() {
		int n = 0;
		for (Entity e : (List<Entity>) world.entities) {
			if (e instanceof ArrowEntity && !e.dead) {
				e.markDead();
				n++;
			}
		}
		return n;
	}

	/** One shot through beta's own use, as a right click makes it, and its arrow taken away: whether one flew. */
	private boolean shoot(ItemStack bow) {
		clearArrows();
		player.inventory.selectedSlot = 0;
		bow.use(world, player);
		return clearArrows() == 1;
	}

	private static ItemStack crafted(ItemStack... cells) {
		CraftingInventory grid = new CraftingInventory(new ScreenHandler() {
			@Override
			public boolean canUse(PlayerEntity player) {
				return true;
			}
		}, 3, 3);
		for (int i = 0; i < cells.length; i++) grid.setStack(i, cells[i]);
		return CraftingRecipeManager.getInstance().craft(grid);
	}

	private static ItemStack dispenserFrom(ItemStack bow) {
		Block c = Block.COBBLESTONE;
		return crafted(new ItemStack(c), new ItemStack(c), new ItemStack(c), new ItemStack(c), bow, new ItemStack(c), new ItemStack(c), new ItemStack(Item.REDSTONE), new ItemStack(c));
	}

	private static String said(ItemStack stack) {
		return stack == null ? "nothing" : stack.count + " of " + stack.getItem().getTranslationKey() + " at " + stack.getDamage();
	}

	/** Bows Wear Out where the world runs: a bow's most damage following the rule, the wear a shot makes, the last shot, and what makes things of bows. */
	private void bows() {
		then(() -> {
			Arrays.fill(player.inventory.main, null);
			player.inventory.main[1] = new ItemStack(Item.ARROW, 64);
			ItemStack bow = new ItemStack(Item.BOW), kept = new ItemStack(Item.BOW, 1, 100);
			check("bows off, as beta: a bow has no most damage, so nothing to wear or show", !WorldRules.bowsWearOut.get() && Item.BOW.getMaxDamage() == 0 && !bow.isDamageable());
			check("bows off: a shot takes an arrow and leaves the bow new", shoot(bow) && count(Item.ARROW) == 63 && bow.getDamage() == 0);
			check("bows off: one worn while they were on keeps its wear, unshown, and wears no more", shoot(kept) && kept.getDamage() == 100 && !kept.isDamaged());
			check("bows off: a worn one still makes a dispenser: " + said(dispenserFrom(kept)), is(dispenserFrom(kept), Block.DISPENSER.id));
			Rules.change(WorldRules.bowsWearOut, "true");
			check("bows on: later versions' most damage, so the worn one shows its wear again", Item.BOW.getMaxDamage() == WorldRules.BOW_MOST_DAMAGE && kept.isDamaged());
			boolean shot = shoot(bow);
			check("bows on: a shot wears it by one: " + said(bow), shot && bow.getDamage() == 1 && count(Item.ARROW) == 61);
			player.inventory.main[1] = null;
			check("bows on: with no arrow, no shot and no wear", !shoot(bow) && bow.getDamage() == 1);
			player.inventory.main[1] = new ItemStack(Item.ARROW, 64);
			ItemStack last = new ItemStack(Item.BOW, 1, WorldRules.BOW_MOST_DAMAGE);
			shot = shoot(last);
			check("bows on: the 385th shot still flies, and uses the bow up, as a fishing rod's last cast is: " + said(last), shot && last.count == 0);
			Item explosive = FabricLoader.getInstance().isModLoaded("explosive-arrows") ? org.studioquire.oldfaithful.core.api.Names.item("explosive-arrows:explosive_arrow") : null;
			if (explosive != null) {
				player.inventory.main[1] = new ItemStack(explosive, 1);
				player.inventory.main[2] = new ItemStack(Item.ARROW, 64);
				shot = shoot(bow);
				check("bows on: an explosive arrow wears it as a plain one does: " + said(bow), shot && count(explosive) == 0 && count(Item.ARROW) == 64 && bow.getDamage() == 2);
			}
			check("bows on: a worn bow makes a dispenser, and so does a new one", is(dispenserFrom(new ItemStack(Item.BOW, 1, 300)), Block.DISPENSER.id) && is(dispenserFrom(new ItemStack(Item.BOW)), Block.DISPENSER.id));
			boolean repair = FabricLoader.getInstance().isModLoaded("item-repair");
			if (repair) {
				ItemStack mended = crafted(new ItemStack(Item.BOW, 1, 100), new ItemStack(Item.BOW, 1, 300));
				check("bows on: Item Repair makes one of two worn bows, 285 and 85 shots left coming to 370: " + said(mended), mended != null && mended.itemId == Item.BOW.id && mended.getDamage() == 15);
			}
			Rules.change(WorldRules.bowsWearOut, "false");
			if (repair) check("bows off: nor does it mend them", crafted(new ItemStack(Item.BOW, 1, 100), new ItemStack(Item.BOW, 1, 300)) == null);
			Arrays.fill(player.inventory.main, null);
			player.currentScreenHandler.sendContentUpdates();
		});
	}

	private static boolean is(ItemStack stack, int id) {
		return stack != null && stack.itemId == id && stack.count == 1;
	}

	/** For the client's half: a bow worn by the first number in the hotbar's first slot, eight arrows in the second. */
	private void bowScenes() {
		scene("bow kit", a -> {
			clearArrows();
			Arrays.fill(player.inventory.main, null);
			player.inventory.main[0] = new ItemStack(Item.BOW, 1, Integer.parseInt(a[0]));
			player.inventory.main[1] = new ItemStack(Item.ARROW, 8);
			player.currentScreenHandler.sendContentUpdates();
		});
		// The bow where the world runs, at a wear or "none", and the arrows left; the client's shot cleared away.
		scene("bow held", a -> {
			ItemStack held = player.inventory.main[0];
			boolean bow = a[0].equals("none") ? held == null : held != null && held.itemId == Item.BOW.id && held.getDamage() == Integer.parseInt(a[0]);
			check("where the world runs, the bow " + (a[0].equals("none") ? "used up" : "at " + a[0]) + " and " + a[1] + " arrows: " + said(held) + ", " + count(Item.ARROW),
					bow && count(Item.ARROW) == Integer.parseInt(a[1]));
			clearArrows();
		});
		scene("bows clear", a -> {
			clearArrows();
			Arrays.fill(player.inventory.main, null);
			player.currentScreenHandler.sendContentUpdates();
		});
	}

	@Override
	protected void script() {
		scene("rule", args -> {
			for (Rule r : Rules.all()) {
				if (r.id().endsWith(":" + args[0])) Rules.change(r, args[1]);
			}
		});
		scene("lock", args -> Rules.lock());
		scene("unlock", args -> unlock());
		bowScenes();
		if (restarted()) {
			check("the rules came back", WorldRules.dayLength.get().equals("double") && WorldRules.weather.get().equals("rain"));
			check("the sun's clock came back: " + Days.time(world) + " at tick " + world.getTime(), Days.time(world) != world.getTime());
			until("it rains", 150, () -> world.isRaining());
			pace("double, restarted", 40, 2);
			then(() -> {
				Rules.change(WorldRules.dayLength, "normal");
				Rules.change(WorldRules.weather, "normal");
			});
			pace("normal again", 40, 1);
			then(() -> check("and the sun stays where it was", Days.time(world) != world.getTime()));
			lock();
			return;
		}
		day();
		sleep();
		weather();
		// A long tick, and a server catches up on one with quick ticks of its own: this comes well before anything timed on the client.
		apples();
		fallingBonus();
		bows();
		monsterLight();
		naturalSpawning();
		then(() -> Rules.change(WorldRules.dayLength, "double"));
		wait(40);
	}
}
