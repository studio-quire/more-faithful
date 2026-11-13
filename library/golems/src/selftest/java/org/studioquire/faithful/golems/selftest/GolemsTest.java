package org.studioquire.faithful.golems.selftest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.PigZombieEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.LightType;
import net.minecraft.world.gen.feature.OakTreeFeature;
import net.minecraft.world.gen.feature.SpruceTreeFeature;
import org.studioquire.faithful.golems.Area;
import org.studioquire.faithful.golems.BoundaryPole;
import org.studioquire.faithful.golems.GolemEntity;
import org.studioquire.faithful.golems.Golems;
import org.studioquire.faithful.golems.Tree;
import org.studioquire.faithful.harness.test.WorldTest;
import org.studioquire.oldfaithful.core.api.Names;
import org.studioquire.oldfaithful.core.api.Tools;

/**
 * What isn't plain from the code: making one; following and staying; what hurts it and what mends it; which poles are its own; the leveller on a little hill with everything it must
 * leave beside it; the exchange measured against a player's numbers; a spare from the chest; and what survives a restart. A {@link LevelRun} (real terrain, hours of work) or a
 * {@link Look} run takes the place of all this when its file asks for it.
 */
public final class GolemsTest extends WorldTest {
	/** Extra golem ticks a game tick while it levels the little hill: the hill takes a few minutes at its own pace. */
	private static final int HURRY = 3;

	private GolemEntity golem, made, sentry;
	private CreeperEntity mark;
	private int sceneDifficulty;
	private int lastAt, stackX, stackY, stackZ;

	/** For {@link LevelRun}: its steps, in this test. */
	void levelSteps(int limit, java.util.function.BooleanSupplier step, Runnable finished) {
		await("the land to be levelled", limit, step);
		then(finished);
	}

	void levelLand(java.util.function.BooleanSupplier ready, Runnable begin) {
		await("the land to be there", 1200, ready);
		wait(100);
		then(begin);
	}

	void report(String what, boolean ok) {
		check(what, ok);
	}

	private List<?> golems() {
		return world.collectEntitiesByClass(GolemEntity.class, Box.createCached(x - 40, y - 60, z - 40, x + 40, y + 40, z + 40));
	}

	private Inventory chest() {
		BlockEntity be = world.getBlockEntity(x, y, z - 6);
		return be instanceof Inventory ? (Inventory) be : null;
	}

	private int inChest(Item item, int blockId) {
		Inventory chest = chest();
		int n = 0;
		for (int i = 0; chest != null && i < chest.size(); i++) {
			ItemStack s = chest.getStack(i);
			if (s != null && (item != null ? s.getItem() == item : s.itemId == blockId)) n += s.count;
		}
		return n;
	}

	@Override
	protected void script() {
		if (Look.asked()) {
			check("a look run asks nothing of the world", true);
			return;
		}
		LevelRun run = LevelRun.asked();
		if (run != null) {
			scene("level_to_golem", args -> LevelRun.toGolem(world, player));
			scene("level_time", args -> world.setTime(6000L));
			scene("level_move", args -> LevelRun.moveTo(world, player, Double.parseDouble(args[0]), Double.parseDouble(args[1]), Double.parseDouble(args[2])));
			if (restarted()) check("the second life of a level run asks nothing", true);
			else run.start(this, world, player, x, y, z);
			return;
		}
		FollowRun follow = FollowRun.asked();
		if (follow != null) {
			if (restarted()) check("the second life of a follow run asks nothing", true);
			else follow.start(this, world, player, x, y, z);
			return;
		}
		ChopRun chop = ChopRun.asked();
		if (chop != null) {
			if (restarted()) check("the second life of a chop run asks nothing", true);
			else chop.start(this, world, player, x, y, z);
			return;
		}
		ArcherRun archerRun = ArcherRun.asked();
		if (archerRun != null) {
			if (restarted()) check("the second life of an archer survey asks nothing", true);
			else archerRun.start(this, world, player, x, y, z);
			return;
		}
		if (restarted()) {
			afterRestart();
			return;
		}
		clientScenes();
		making();
		farming();
		following();
		harm();
		poles();
		glow();
		chopping();
		levelling();
		exchange();
		mending();
		fullAndSpares();
		guarding();
		archery();
		takingBack();
	}

	/** For the client test: a golem by a real click on the iron, a tool by a real click on the golem. */
	private void clientScenes() {
		// Eight blocks of stone in a row at the player's feet, for the client test to measure a player mining them.
		scene("row", args -> {
			int fx = Integer.parseInt(args[0]), fy = Integer.parseInt(args[1]), fz = Integer.parseInt(args[2]);
			for (int k = 0; k < 8; k++) {
				world.setBlock(fx + 2 + k % 2, fy, fz - 2 + k / 2, Block.STONE.id);
				world.setBlock(fx + 2 + k % 2, fy + 1, fz - 2 + k / 2, 0);
			}
			hold(new ItemStack(Item.DIAMOND_PICKAXE));
		});
		scene("stack", args -> {
			stackX = Integer.parseInt(args[0]);
			stackY = Integer.parseInt(args[1]);
			stackZ = Integer.parseInt(args[2]);
			world.setBlock(stackX, stackY, stackZ, Block.IRON_BLOCK.id);
			world.setBlock(stackX, stackY + 1, stackZ, Block.IRON_BLOCK.id);
			// Poles round it, so it has a little flat area with nothing to do, rather than the land round the plot.
			for (int[] c : new int[][] {{-3, -3}, {3, -3}, {-3, 3}, {3, 3}}) world.setBlock(stackX + c[0], stackY, stackZ + c[1], Golems.pole.get().id);
			hold(new ItemStack(Golems.core.get()));
		});
		scene("stack_done", args -> {
			check("the stack became a golem, by a client's click", world.getBlockId(stackX, stackY, stackZ) == 0 && world.getBlockId(stackX, stackY + 1, stackZ) == 0);
			check("and the core went", player.getHand() == null || player.getHand().count == 0);
			made = null;
			for (Object o : golems()) {
				GolemEntity g = (GolemEntity) o;
				if (Math.abs(g.x - (stackX + 0.5)) < 1.0 && Math.abs(g.z - (stackZ + 0.5)) < 1.0) made = g;
			}
			hold(new ItemStack(Item.IRON_PICKAXE));
		});
		// Its health as a client is told it: cracks for a hurt golem, and an ingot taking a stage off.
		scene("hurt", args -> {
			if (made != null) made.health = 19;
		});
		scene("mended", args -> {
			hold(new ItemStack(Item.IRON_INGOT));
			if (made != null) made.interact(player);
			hold(null);
		});
		// A sentry drawing on a creeper caught in cobwebs, where it can hardly move and an arrow goes through: for the client to see it aim.
		scene("archer", args -> {
			sceneDifficulty = world.difficulty;
			// Peaceful takes a monster away on its first tick.
			world.difficulty = 1;
			for (int i = 6; i <= 8; i++) {
				for (int k = 6; k <= 8; k++) {
					for (int h = 0; h <= 1; h++) set(i, h, k, Block.COBWEB);
				}
			}
			sentry = placed(new GolemEntity(world), 7.5, 1.5);
			sentry.setSlot(0, new ItemStack(Item.ARROW, 16));
			hold(new ItemStack(Item.BOW));
			sentry.interact(player);
			hold(null);
			mark = placed(new CreeperEntity(world), 7.5, 7.5);
		});
		scene("archer_done", args -> {
			check("the sentry shot at the creeper", sentry != null && sentry.status().matches(".*\\b[1-9][0-9]* shots.*"));
			if (sentry != null) sentry.markDead();
			if (mark != null) mark.markDead();
			for (int i = 6; i <= 8; i++) {
				for (int k = 6; k <= 8; k++) {
					for (int h = 0; h <= 1; h++) set(i, h, k, null);
				}
			}
			for (Object o : world.collectEntitiesByClass(Entity.class, Box.createCached(x + 4, y - 1, z - 2, x + 9, y + 4, z + 9))) {
				if (o instanceof ItemEntity || o instanceof ArrowEntity) ((Entity) o).markDead();
			}
			world.difficulty = sceneDifficulty;
		});
		scene("given_done", args -> {
			check("a client's click hands it the pick, and it takes up the job", made != null && "pickaxe".equals(made.jobName()) && made.tool() != null && player.getHand() == null);
			check("inside the poles round it", made != null && made.area() != null && made.area().marked && made.area().minX == stackX - 2);
		});
		// For the client's held clicks: a bow in its hand, arrows in the player's pack and nothing in the player's hand.
		scene("held_bow", args -> {
			hold(new ItemStack(Item.BOW));
			if (made != null) made.interact(player);
			hold(null);
			player.inventory.main[20] = new ItemStack(Item.ARROW, 16);
			player.currentScreenHandler.sendContentUpdates();
		});
		scene("held_taken", args -> {
			check("where the world runs, the held click took the bow back, its job gone", made != null && made.tool() == null && made.jobName() == null
					&& player.getHand() != null && player.getHand().getItem() == Item.BOW);
			check("and no arrow left the player's pack", player.inventory.main[20] != null && player.inventory.main[20].count == 16);
		});
		scene("held_given", args -> {
			check("where the world runs, the held click handed it the bow and it kept it", made != null && made.tool() != null && "bow".equals(made.jobName()) && player.getHand() == null);
			// Its bow back, out of the player's hand, for a held click with an empty hand on a golem with no tool.
			if (made != null) made.interact(player);
			hold(null);
		});
		scene("held_done", args -> {
			check("where the world runs, a held click with an empty hand told it to follow, once", made != null && player.name.equals(made.following()));
			if (made != null) made.markDead();
			player.inventory.main[20] = null;
			player.currentScreenHandler.sendContentUpdates();
		});
	}

	/** Two iron blocks and a core, through the item's own use. */
	private void making() {
		set(3, 0, 0, Block.IRON_BLOCK);
		set(3, 1, 0, Block.IRON_BLOCK);
		ItemStack core = new ItemStack(Golems.core.get());
		check("one iron block isn't enough", !Golems.core.get().useOnBlock(core, player, world, x + 3, y, z, 1));
		check("the core makes a golem of the top of two", Golems.core.get().useOnBlock(core, player, world, x + 3, y + 1, z, 1));
		check("the core is used", core.count == 0);
		check("the iron is gone", block(3, 0, 0) == 0 && block(3, 1, 0) == 0);
		List<?> found = golems();
		check("there's a golem", found.size() == 1);
		golem = (GolemEntity) found.get(0);
		check("with no job, staying", golem.jobName() == null && golem.following() == null);
	}

	/**
	 * The hoe, with a golem of its own on a little farm between four poles, south-east of the player: eight farmland round a water source, five of them ripe, one growing, two bare,
	 * beside dirt that was never tilled, and a chest with seeds in it. It reaps the ripe and sows every gap, never reaping twice running with a seed on it; from one strike to the
	 * next is a player's gap, doubled; the hoe wears once a crop. Then, carried over by its NBT as a restart would, a cell trampled back to dirt and one grassed over since are tilled
	 * and sown again with seeds it fetches from the chest for them, and the dirt never tilled is left. The plot is left as it came.
	 */
	private void farming() {
		GolemEntity[] farmer = new GolemEntity[1];
		// Per cell of the 3 by 3 round the water: 0 bare, 1 growing, 2 ripe.
		int[] cells = new int[9], reaps = {0}, ticks = {0}, lastAt = {0}, last = {-1}, afterReap = {Integer.MAX_VALUE}, afterSow = {Integer.MAX_VALUE}, twice = {0}, before = {0};
		boolean[] seedSince = {false};
		int[][] ripe = {{3, 4}, {4, 4}, {5, 4}, {3, 5}, {5, 6}};
		then(() -> {
			for (int[] c : new int[][] {{1, 2}, {7, 2}, {1, 8}, {7, 8}}) set(c[0], 0, c[1], Golems.pole.get());
			for (int n = 0; n < 9; n++) set(3 + n % 3, -1, 4 + n / 3, n == 4 ? Block.WATER : Block.FARMLAND, n == 4 ? 0 : 7);
			// Glass over the water, which waters the farmland all the same: a reaped crop's drop can roll into an open hole and sink, where a golem, which never wades, can't
			// reach it (a player standing beside it can't either), and the wheat count came up one short so (2026-11-10).
			set(4, 0, 5, Block.GLASS);
			for (int[] c : ripe) set(c[0], 0, c[1], Block.WHEAT, 7);
			set(5, 0, 5, Block.WHEAT, 3);
			set(6, -1, 6, Block.DIRT);
			set(6, 0, 2, Block.CHEST);
			((Inventory) world.getBlockEntity(x + 6, y, z + 2)).setStack(0, new ItemStack(Item.SEEDS, 16));
			for (int n = 0; n < 9; n++) cells[n] = crop(3 + n % 3, 4 + n / 3);
			GolemEntity g = new GolemEntity(world);
			g.setPositionAndAngles(x + 2.5, y, z + 5.5, 0.0F, 0.0F);
			world.spawnEntity(g);
			farmer[0] = g;
			hold(new ItemStack(Item.IRON_HOE));
			g.interact(player);
			check("a hoe sets it to farming", "hoe".equals(g.jobName()) && player.getHand() == null);
			check("inside its poles", g.area().marked && g.area().minX == x + 2 && g.area().maxX == x + 6 && g.area().minZ == z + 3 && g.area().maxZ == z + 7);
		});
		// Watched after every golem tick: the world's own, then HURRY more.
		Runnable watch = () -> {
			ticks[0]++;
			for (int n = 0; n < 9; n++) {
				if (n == 4) continue;
				int now = crop(3 + n % 3, 4 + n / 3), was = cells[n];
				cells[n] = now;
				boolean reaped = was == 2 && now == 0, sown = was == 0 && now > 0;
				if (!reaped && !sown) continue;
				if (last[0] == 0) afterReap[0] = Math.min(afterReap[0], ticks[0] - lastAt[0]);
				if (last[0] == 1) afterSow[0] = Math.min(afterSow[0], ticks[0] - lastAt[0]);
				if (reaped && last[0] == 0 && seedSince[0]) twice[0]++;
				if (reaped) {
					reaps[0]++;
					seedSince[0] = false;
				}
				last[0] = reaped ? 0 : 1;
				lastAt[0] = ticks[0];
			}
			if (seeds(farmer[0]) > 0) seedSince[0] = true;
		};
		until("it reaps the ripe, sows every gap, and puts what it gathered away", 2400, () -> {
			watch.run();
			for (int i = 0; i < HURRY; i++) {
				world.updateEntity(farmer[0]);
				watch.run();
			}
			boolean sown = true;
			for (int n = 0; n < 9; n++) sown &= n == 4 || cells[n] > 0;
			return reaps[0] == 5 && sown && !farmer[0].carriesAnything() && farmer[0].status().startsWith("WAIT");
		});
		then(() -> {
			GolemEntity g = farmer[0];
			System.out.println("[golems] farming: " + g.status());
			boolean farmland = true;
			for (int n = 0; n < 9; n++) farmland &= n == 4 ? world.getMaterial(x + 4, y - 1, z + 5) == Material.WATER : block(3 + n % 3, -1, 4 + n / 3) == Block.FARMLAND.id;
			check("every cell is sown again, the farmland whole (never trampled) and the water there", farmland);
			check("the growing one is left to grow", block(5, 0, 5) == Block.WHEAT.id && meta(5, 0, 5) >= 3 && meta(5, 0, 5) < 7);
			check("it never reaps twice running with a seed on it: one at a time", twice[0] == 0);
			note("from one strike to the next, its work in reach: " + afterReap[0] + " ticks after a reap, " + afterSow[0] + " after a sowing");
			check("a player's gaps, doubled: 12 after breaking a crop, 10 after a right-click", afterReap[0] == 12 && afterSow[0] == 10);
			check("its hoe wore once a crop", g.tool() != null && g.tool().getDamage() == 5);
			check("it walked up to every cell, never reaching from further than " + GolemEntity.CLOSE, g.stretched == 0 && g.furthest <= GolemEntity.CLOSE);
			StringBuilder lying = new StringBuilder();
			for (Object o : world.collectEntitiesByClass(ItemEntity.class, Box.createCached(x, y - 2, z + 1, x + 9, y + 4, z + 10))) {
				ItemEntity item = (ItemEntity) o;
				lying.append(String.format(" %s at %.2f %.2f %.2f;", item.stack.getItem().getTranslationKey(), item.x - x, item.boundingBox.minY - y, item.z - z));
			}
			note("lying on the farm:" + (lying.length() == 0 ? " nothing" : lying.toString()) + "; its gathers left " + java.util.Arrays.toString(g.leftLying));
			check("the wheat is in the chest", inFarmChest(Item.WHEAT) == 5);
			note(inFarmChest(Item.SEEDS) + " seeds in the chest, of 16 and what five crops gave");
			// Grass may spread onto it, as beta's grass does onto any dirt beside it: that's not the golem's.
			check("dirt that was never farmland isn't tilled", block(6, -1, 6) == Block.DIRT.id || block(6, -1, 6) == Block.GRASS_BLOCK.id);
			check("and it's back at its post", Math.abs(g.x - (x + 2.5)) < 1.6 && Math.abs(g.z - (z + 5.5)) < 1.6);
			// The whole entity, place and all, as a chunk saves it.
			NbtCompound nbt = new NbtCompound();
			g.write(nbt);
			check("its NBT keeps the field it has seen: eight cells", nbt.getCompound("JobState").getByteArray("Field").length == 8 * 3);
			g.markDead();
			farmer[0] = new GolemEntity(world);
			farmer[0].read(nbt);
		});
		wait(2);
		then(() -> {
			world.spawnEntity(farmer[0]);
			// Trampled back to dirt, and another grassed over since, their wheat gone with them.
			set(4, 0, 6, null);
			set(4, -1, 6, Block.DIRT);
			set(5, 0, 4, null);
			set(5, -1, 4, Block.GRASS_BLOCK);
			before[0] = inFarmChest(Item.SEEDS);
		});
		until("from its NBT, it tills its field again where it went back to dirt, and sows it", 2400, () -> {
			for (int i = 0; i < HURRY; i++) world.updateEntity(farmer[0]);
			GolemEntity g = farmer[0];
			return block(4, -1, 6) == Block.FARMLAND.id && block(4, 0, 6) == Block.WHEAT.id && block(5, -1, 4) == Block.FARMLAND.id && block(5, 0, 4) == Block.WHEAT.id && !g.carriesAnything()
					&& g.status().startsWith("WAIT");
		});
		then(() -> {
			GolemEntity g = farmer[0];
			check("tilling wears the hoe once a cell, as a player's", g.tool() != null && g.tool().getDamage() == 5 + 2);
			check("it took from the chest just the two seeds it needed", inFarmChest(Item.SEEDS) == before[0] - 2);
			check("and still leaves the dirt that was never its field", block(6, -1, 6) == Block.DIRT.id || block(6, -1, 6) == Block.GRASS_BLOCK.id);
			// The plot as it came.
			hold(null);
			g.interact(player);
			check("an empty hand takes its hoe back", player.getHand() != null && player.getHand().getItem() == Item.IRON_HOE);
			hold(null);
			g.markDead();
			Inventory chest = (Inventory) world.getBlockEntity(x + 6, y, z + 2);
			for (int i = 0; chest != null && i < chest.size(); i++) chest.setStack(i, null);
			for (int i = 1; i <= 7; i++) {
				for (int k = 2; k <= 8; k++) {
					for (int h = 2; h >= 0; h--) set(i, h, k, null);
					set(i, -1, k, Block.STONE);
				}
			}
			for (Object o : world.collectEntitiesByClass(ItemEntity.class, Box.createCached(x, y - 2, z + 1, x + 9, y + 4, z + 10))) ((ItemEntity) o).markDead();
		});
	}

	/** A cell of the farm: 0 bare, 1 growing, 2 ripe. */
	private int crop(int dx, int dz) {
		return block(dx, 0, dz) != Block.WHEAT.id ? 0 : meta(dx, 0, dz) >= 7 ? 2 : 1;
	}

	private static int seeds(GolemEntity g) {
		int n = 0;
		for (int i = 0; g != null && i < GolemEntity.SLOTS; i++) if (g.slot(i) != null && g.slot(i).getItem() == Item.SEEDS) n += g.slot(i).count;
		return n;
	}

	private int inFarmChest(Item item) {
		BlockEntity be = world.getBlockEntity(x + 6, y, z + 2);
		int n = 0;
		for (int i = 0; be instanceof Inventory && i < ((Inventory) be).size(); i++) {
			ItemStack s = ((Inventory) be).getStack(i);
			if (s != null && s.getItem() == item) n += s.count;
		}
		return n;
	}

	/** It stays until told; an empty hand has it follow, at a player's walk, and another has it stay. */
	private void following() {
		double[] at = new double[2];
		then(() -> {
			golem.setPositionAndAngles(x + 6.5, y, z + 0.5, 0.0F, 0.0F);
			at[0] = golem.x;
			at[1] = golem.z;
		});
		wait(40);
		then(() -> {
			check("made, it stays put", Math.abs(golem.x - at[0]) < 0.3 && Math.abs(golem.z - at[1]) < 0.3);
			hold(null);
			golem.interact(player);
			check("an empty hand has it follow", player.name.equals(golem.following()));
		});
		await("it sets off", 40, golem::hasPath);
		then(() -> at[0] = golem.x);
		wait(10);
		// Its pace is a number in the code (a player's walk); this only says what it came to.
		then(() -> note("following, it walks " + (at[0] - golem.x) / 10.0 + " blocks a tick in its first ten"));
		until("it comes to the player", 200, () -> golem.getSquaredDistance(player) < 9.0);
		then(() -> {
			golem.interact(player);
			check("another empty hand has it stay", golem.following() == null);
			golem.setPositionAndAngles(x + 6.5, y, z + 0.5, 0.0F, 0.0F);
			at[0] = golem.x;
		});
		wait(40);
		then(() -> check("and it stays", Math.abs(golem.x - at[0]) < 0.3));
	}

	/** Hurts from nothing iron minds don't; a fall, lava and a blow do. Mended with iron. */
	private void harm() {
		then(() -> {
			int was = golem.health;
			check("fire, cactus, a wall or water don't hurt it", !golem.damage(null, 6) && golem.health == was);
			check("it starts at 20 hearts", was == GolemEntity.MAX_HEALTH);
			golem.setPositionAndAngles(x + 6.5, y + 6.5, z + 6.5, 0.0F, 0.0F);
		});
		wait(3);
		await("the fall", 60, () -> golem.onGround);
		int[] fell = {0};
		then(() -> {
			// Beta leaves the landing tick's step out of a fall, so six and a half blocks count as five and a half to six and a half by how the ticks fall: 3 or 4, for a player too.
			fell[0] = golem.health;
			check("a fall of six and a half blocks hurts it half what it would a player (3 or 4): 1 or 2, the odd half carried", golem.health == GolemEntity.MAX_HEALTH - 1 || golem.health == GolemEntity.MAX_HEALTH - 2);
			set(6, 0, 6, Block.FIRE);
		});
		wait(60);
		then(() -> {
			check("standing in fire doesn't hurt it, or set it burning", golem.health == fell[0] && golem.fireTicks == 0);
			set(6, 0, 6, null);
			set(6, -2, 6, Block.STONE);
			set(6, -1, 6, Block.LAVA);
		});
		wait(5);
		then(() -> {
			check("lava does", golem.health < fell[0]);
			golem.setPositionAndAngles(x + 5.5, y, z + 5.5, 0.0F, 0.0F);
			set(6, -1, 6, Block.STONE);
		});
		wait(30);
		then(() -> {
			int hurt = golem.health;
			check("and once out of it, it isn't burning", golem.fireTicks == 0);
			hold(new ItemStack(Item.IRON_INGOT, 5));
			golem.interact(player);
			check("an iron ingot mends a quarter", golem.health == Math.min(GolemEntity.MAX_HEALTH, hurt + GolemEntity.MEND) && player.getHand().count == 4);
			for (int i = 0; i < 4; i++) golem.interact(player);
			check("up to 20 hearts and no more, and the ingots left over are kept", golem.health == GolemEntity.MAX_HEALTH && player.getHand() != null && player.getHand().count >= 1);
			hold(null);
		});
	}

	/** Which poles are its own: the smallest rectangle round its post, the poles' rows left out; and as poles go, the next one out, then the square round its post. */
	private void poles() {
		then(() -> {
			Block stone = Golems.pole.get();
			int[][] outer = {{-8, -8}, {8, -8}, {-8, 8}, {8, 8}}, inner = {{-6, -6}, {6, -6}, {-6, 6}, {6, 6}};
			for (int[] c : outer) set(c[0], 0, c[1], stone);
			Area a = Area.find(world, x - 3, z - 3, null);
			check("four poles mark out what's inside them", a.marked && a.minX == x - 7 && a.maxX == x + 7 && a.minZ == z - 7 && a.maxZ == z + 7);
			for (int[] c : inner) set(c[0], 0, c[1], stone);
			// One a block higher: a rectangle's poles needn't be level.
			set(6, 0, 6, Block.STONE);
			set(6, 1, 6, stone);
			a = Area.find(world, x - 3, z - 3, null);
			check("a smaller set round the post is the one it takes, at any height", a.marked && a.minX == x - 5 && a.maxX == x + 5 && a.minZ == z - 5 && a.maxZ == z + 5);
			check("a post outside the smaller set takes the larger", Area.find(world, x + 7, z, null).minX == x - 7);
			set(-6, 0, -6, null);
			a = Area.find(world, x - 3, z - 3, null);
			check("a stone gone, and the larger set it's in is its area", a.minX == x - 7);
			set(-8, -1, 8, null);
			check("a pole falls off with the ground from under it", block(-8, 0, 8) == 0);
			a = Area.find(world, x - 3, z - 3, null);
			check("and with no whole set, it's the square of 16 round its post", !a.marked && a.minX == x - 3 - Area.AROUND_POST && a.maxZ == z - 3 + Area.AROUND_POST);
			set(-8, -1, 8, Block.STONE);
			set(-8, 0, 8, stone);
			set(-6, 0, -6, stone);
			set(6, 1, 6, null);
			set(6, 0, 6, stone);
		});
	}

	/**
	 * A pole glows, lit as a redstone torch is and giving its light, while some golem's rectangle has it for a corner: two golems in the inner set, one letting go and the other
	 * keeping them lit, then that one handed a hoe between the sets so the outer set lights and the inner goes plain, a pole of it broken and put back, and its death.
	 */
	private void glow() {
		GolemEntity[] two = new GolemEntity[2];
		int[][] outer = {{-8, -8}, {8, -8}, {-8, 8}, {8, 8}}, inner = {{-6, -6}, {6, -6}, {-6, 6}, {6, 6}};
		then(() -> {
			two[0] = placed(new GolemEntity(world), -2.5, -2.5);
			two[1] = placed(new GolemEntity(world), -2.5, 3.5);
			for (GolemEntity g : two) {
				hold(new ItemStack(Item.IRON_HOE));
				g.interact(player);
			}
			hold(null);
		});
		wait(2);
		then(() -> check("the poles two golems work within glow", lit(inner, true) && lit(outer, false)));
		// Beta spreads light a few hundred cells a tick, so it's given a moment.
		await("a lit redstone torch's light, 7, at a lit pole", 40, () -> world.getBrightness(LightType.BLOCK, x + 6, y, z + 6) == 7);
		then(() -> {
			two[0].interact(player);
			hold(null);
		});
		wait(2);
		then(() -> {
			check("one letting go leaves them lit for the other", lit(inner, true));
			two[1].setPositionAndAngles(x - 6.5, y, z - 6.5, 0.0F, 0.0F);
			hold(new ItemStack(Item.IRON_HOE));
			two[1].interact(player);
			hold(null);
		});
		wait(2);
		then(() -> {
			check("handed a hoe between the sets, the outer set it now works glows and the inner goes plain", lit(outer, true) && lit(inner, false));
		});
		// Its own light gone: what's left there is the outer pole's two blocks off each way, 7 less 4.
		await("a plain pole's light to be none of its own, the outer pole's 3", 40, () -> world.getBrightness(LightType.BLOCK, x + 6, y, z + 6) == 3);
		then(() -> {
			set(8, 0, 8, null);
		});
		wait(2);
		then(() -> {
			check("a pole of it broken, the rest still glow: the golem still works the rectangle it was handed its tool in", lit(new int[][] {{-8, -8}, {8, -8}, {-8, 8}}, true));
			set(8, 0, 8, Golems.pole.get());
		});
		wait(2);
		then(() -> {
			check("one put back at the corner glows", lit(outer, true));
			two[1].damage(player, 1000);
		});
		wait(25);
		then(() -> {
			check("the golem dead, its poles go plain", two[1].dead && lit(outer, false) && lit(inner, false));
			two[0].markDead();
			for (Object o : world.collectEntitiesByClass(ItemEntity.class, Box.createCached(x - 9, y - 2, z - 9, x + 10, y + 8, z + 10))) ((ItemEntity) o).markDead();
		});
	}

	/** Whether every one of these poles, at the floor, is lit (or every one plain). */
	private boolean lit(int[][] poles, boolean lit) {
		for (int[] c : poles) {
			int id = block(c[0], 0, c[1]);
			if (id != (lit ? Golems.poleLit.get().id : Golems.pole.get().id)) return false;
		}
		return true;
	}

	/**
	 * The axe, inside the inner poles: an oak and a spruce as beta grows them, both with leaves a block off the ground that keep it from the trunk, and a tree with branches joined at
	 * edges and corners on open ground, among things built of logs it must leave: a post with leaves and a torch on it, two logs side by side, a log in a hedge, logs in the air. It fells
	 * the three from their farthest logs down, nothing ever left hanging, from within 2 of the logs where it can stand that near; is handed another axe partway and goes on knowing its
	 * stumps; puts every log in the chest, its axe worn once a log; and plants a sapling of each tree's kind where it stood, from two put down for it and the spruce's from its chest,
	 * where a player left it. Leaves decay by beta's own tick, called more often than beta would, and their saplings are taken away, so the test's are the only ones.
	 */
	private void chopping() {
		GolemEntity[] axe = new GolemEntity[1];
		// Each tree's foot and how many logs it had; how far from a log's column it stood to chop one of each.
		int[][] trees = {{-4, 4, 0}, {4, 4, 0}, {4, -4, 0}};
		int[] kinds = {0, 1, 0}, total = {0}, hanging = {0};
		List<Integer> ours = new ArrayList<>();
		double[] stood = new double[3];
		then(() -> {
			golem.setPositionAndAngles(x - 7.5, y, z + 0.5, 0.0F, 0.0F);
			// The player in the middle with glass on four sides: the golem walks all round the plot, and a mob walking past shoves a player, who then picks up what lies near.
			ChopRun.move(player, x + 0.5, y + 0.01, z + 0.5);
			for (int[] s : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
				set(s[0], 0, s[1], Block.GLASS);
				set(s[0], 1, s[1], Block.GLASS);
			}
			for (int[] t : trees) set(t[0], -1, t[1], Block.GRASS_BLOCK);
			// Beta's draws for these: an oak four logs high, its leaves from the second block up, two out; a spruce's from the second, three out.
			check("an oak grows for the axe", new OakTreeFeature().generate(world, new Random(10), x - 4, y, z + 4) && block(-3, 1, 4) == Block.LEAVES.id);
			check("and a spruce", new SpruceTreeFeature().generate(world, new Random(2), x + 4, y, z + 4) && block(3, 1, 4) == Block.LEAVES.id);
			Felling.branchy(world, x + 4, y, z - 4);
			// Built of logs: a post with a torch, two logs side by side, a log in a hedge, logs in the air; each with leaves on it.
			for (int h = 0; h <= 4; h++) set(-4, h, -4, Block.LOG);
			Felling.leavesAround(world, x - 4, y + 4, z - 4);
			set(-3, 1, -4, Block.TORCH, 1);
			for (int h = 0; h <= 3; h++) {
				set(-2, h, -4, Block.LOG);
				set(-1, h, -4, Block.LOG);
			}
			Felling.leavesAround(world, x - 2, y + 3, z - 4);
			Felling.leavesAround(world, x - 1, y + 3, z - 4);
			set(-4, 0, 0, Block.LOG);
			Felling.leavesAround(world, x - 4, y, z);
			Felling.leavesAround(world, x - 4, y + 1, z);
			set(-1, 4, -1, Block.LOG);
			set(-1, 5, -1, Block.LOG);
			Felling.leavesAround(world, x - 1, y + 5, z - 1);
			for (int[] c : new int[][] {{-4, -4}, {-2, -4}, {-1, -4}, {-4, 0}}) set(c[0], -1, c[1], Block.GRASS_BLOCK);
			for (int[] t : trees) {
				t[2] = Felling.standing(world, x + t[0], y, z + t[1])[0];
				total[0] += t[2];
			}
			check("what beta grows, and a tree with branches, are trees to an axe", Tree.at(world, x - 4, y, z + 4) && Tree.at(world, x + 4, y, z + 4) && Tree.at(world, x + 4, y, z - 4));
			check("a post with a torch on it isn't", !Tree.at(world, x - 4, y, z - 4));
			check("nor two logs side by side", !Tree.at(world, x - 2, y, z - 4) && !Tree.at(world, x - 1, y, z - 4));
			check("nor a log in a hedge", !Tree.at(world, x - 4, y, z));
			note("the trees have " + trees[0][2] + ", " + trees[1][2] + " and " + trees[2][2] + " logs");
			set(0, 0, -6, Block.CHEST);
			GolemEntity g = new GolemEntity(world);
			g.setPositionAndAngles(x + 0.5, y, z - 2.5, 0.0F, 0.0F);
			world.spawnEntity(g);
			axe[0] = g;
			hold(new ItemStack(Item.IRON_AXE));
			g.interact(player);
			hold(null);
			check("an axe sets it to work, inside the inner poles", "axe".equals(g.jobName()) && g.area().marked && g.area().minX == x - 5 && g.area().maxZ == z + 5);
		});
		int[] ticks = {0};
		until("it fells the three trees", 3000, () -> {
			GolemEntity g = axe[0];
			for (int i = 0; i < HURRY; i++) {
				// Where it stands as its next tick of work judges its reach, before the tick's movement.
				int[] on = Felling.workingOn(g.status());
				for (int k = 0; on != null && k < 3; k++) {
					int[] t = trees[k];
					if (Math.abs(on[0] - x - t[0]) > 3 || Math.abs(on[2] - z - t[1]) > 3 || world.getBlockId(on[0], on[1], on[2]) != Block.LOG.id) continue;
					stood[k] = Math.max(stood[k], Felling.along(Math.floor(g.x) + 0.5, Math.floor(g.z) + 0.5, on[0], on[2]));
				}
				world.updateEntity(g);
			}
			ticks[0]++;
			int left = 0;
			for (int[] t : trees) {
				int[] n = Felling.standing(world, x + t[0], y, z + t[1]);
				left += n[0];
				if (n[1] > 0 && hanging[0]++ == 0) note("logs left hanging by the tree at " + t[0] + " " + t[1] + ": " + n[1]);
			}
			// Saplings from leaves already decaying are taken away until the last tree is down, so its three stumps wait for theirs.
			for (Object o : world.collectEntitiesByClass(ItemEntity.class, Box.createCached(x - 12, y - 2, z - 12, x + 13, y + 24, z + 13))) {
				if (((ItemEntity) o).stack.itemId == Block.SAPLING.id) ((ItemEntity) o).markDead();
			}
			if (ticks[0] % 200 == 0) System.out.println("[golems] felling: " + left + " logs standing, " + g.status());
			return left == 0;
		});
		then(() -> {
			GolemEntity g = axe[0];
			check("never a log left hanging as it went", hanging[0] == 0);
			note(String.format("chopping, it stood at most %.2f from a log's column by the oak, %.2f by the spruce, %.2f by the tree with branches", stood[0], stood[1], stood[2]));
			check("with ground beside the trunk and under the branches, it chopped from within 2 of each log, however high", stood[2] > 0 && stood[2] <= GolemEntity.CLOSE);
			check("with leaves a block off the ground keeping it off, it stretched further, as far as its reach", stood[0] > GolemEntity.CLOSE && stood[0] <= GolemEntity.REACH && stood[1] > GolemEntity.CLOSE && stood[1] <= GolemEntity.REACH);
			check("three stumps wait for saplings", "3 stumps to plant".equals(Felling.stumps(g.status())));
			NbtCompound nbt = new NbtCompound();
			g.writeNbt(nbt);
			GolemEntity copy = new GolemEntity(world);
			copy.readNbt(nbt);
			String stumps = Felling.stumps(g.status());
			check("a save keeps the stumps it has to plant", stumps != null && stumps.equals(Felling.stumps(copy.status())));
			// Another axe, at its post so its post stays where it was: the old one comes back to the hand, and the job goes on with what it knew.
			g.setPositionAndAngles(x + 0.5, y, z - 2.5, 0.0F, 0.0F);
			hold(new ItemStack(Item.STONE_AXE));
			g.interact(player);
			ItemStack iron = player.getHand();
			hold(null);
			check("its axe wore once a log, as a player's does", iron != null && iron.getItem() == Item.IRON_AXE && iron.getDamage() == total[0]);
			check("another axe keeps the stumps it has to plant", stumps != null && stumps.equals(Felling.stumps(g.status())));
			// Saplings enough for the three: two oak lying about, and a spruce in its chest, as a player might leave it one.
			for (int k = 0; k < 2; k++) {
				ItemEntity item = new ItemEntity(world, x - 2.5, y + 0.3, z + 2.5, new ItemStack(Block.SAPLING.id, 1, 0));
				item.velocityX = item.velocityY = item.velocityZ = 0.0;
				world.spawnEntity(item);
				ours.add(item.id);
			}
			chest().setStack(13, new ItemStack(Block.SAPLING.id, 1, 1));
		});
		until("it plants the three stumps and puts every log away", 3000, () -> {
			GolemEntity g = axe[0];
			for (int i = 0; i < HURRY; i++) world.updateEntity(g);
			if (++ticks[0] % 20 == 0) Felling.decay(world, x, y, z);
			for (Object o : world.collectEntitiesByClass(ItemEntity.class, Box.createCached(x - 12, y - 2, z - 12, x + 13, y + 24, z + 13))) {
				ItemEntity item = (ItemEntity) o;
				if (item.stack.itemId == Block.SAPLING.id && !ours.contains(item.id)) item.markDead();
			}
			boolean planted = true;
			for (int i = 0; i < 3; i++) planted &= block(trees[i][0], 0, trees[i][1]) == Block.SAPLING.id;
			boolean done = planted && inChest(null, Block.LOG.id) >= total[0] && g.status().startsWith("WAIT");
			if (ticks[0] % 200 == 0 || done) {
				StringBuilder lying = new StringBuilder();
				for (Object o : world.collectEntitiesByClass(ItemEntity.class, Box.createCached(x - 12, y - 2, z - 12, x + 13, y + 24, z + 13))) {
					ItemEntity item = (ItemEntity) o;
					lying.append(String.format(" %s at %.1f %.1f %.1f;", item.stack.getItem().getTranslationKey(), item.x - x, item.y - y, item.z - z));
				}
				System.out.println("[golems] planting: " + g.status() + ", carrying " + g.carriedCount() + ", logs in the chest " + inChest(null, Block.LOG.id) + ", lying:" + lying);
			}
			return done;
		});
		then(() -> {
			boolean ownKinds = true;
			for (int i = 0; i < 3; i++) ownKinds &= block(trees[i][0], 0, trees[i][1]) == Block.SAPLING.id && (meta(trees[i][0], 0, trees[i][1]) & 3) == kinds[i];
			check("a sapling of each tree's own kind where it stood, the spruce's fetched from the chest", ownKinds && inChest(null, Block.SAPLING.id) == 0);
			check("every log is in the chest", inChest(null, Block.LOG.id) == total[0]);
			boolean built = true;
			for (int h = 0; h <= 4; h++) built &= block(-4, h, -4) == Block.LOG.id;
			for (int h = 0; h <= 3; h++) built &= block(-2, h, -4) == Block.LOG.id && block(-1, h, -4) == Block.LOG.id;
			built &= block(-3, 1, -4) == Block.TORCH.id && block(-4, 0, 0) == Block.LOG.id && block(-1, 4, -1) == Block.LOG.id && block(-1, 5, -1) == Block.LOG.id;
			check("what was built of logs is as it was", built);
			// The plot as it came for the leveller: its poles (lit or not), a stone floor, nothing else.
			axe[0].markDead();
			Inventory chest = chest();
			for (int i = 0; chest != null && i < chest.size(); i++) chest.setStack(i, null);
			for (int i = -8; i <= 8; i++) {
				for (int k = -8; k <= 8; k++) {
					for (int h = 20; h >= 0; h--) {
						if (!BoundaryPole.is(block(i, h, k))) set(i, h, k, null);
					}
					set(i, -1, k, Block.STONE);
				}
			}
			for (Object o : world.collectEntitiesByClass(ItemEntity.class, Box.createCached(x - 12, y - 2, z - 12, x + 13, y + 24, z + 13))) ((ItemEntity) o).markDead();
			// The glass went with the rest; the player is put back in the middle all the same.
			check("the player stayed where they were", Math.abs(player.x - x - 0.5) < 0.5 && Math.abs(player.z - z - 0.5) < 0.5);
			ChopRun.move(player, x + 0.5, y + 0.01, z + 0.5);
		});
	}

	/**
	 * A little hill to level, inside the inner poles, with what it must leave about it: a puddle walled in by ground, ground over lava, a block a floor torch stands by, a tree's
	 * earth, cobble, a spire too tall to reach the top of, and ground on the poles' line and outside it.
	 */
	private void levelling() {
		int[] hill = {0};
		then(() -> {
			// The work east of the player and two blocks off, past where a player picks things up; what's to be left, to the west. A ring of glass round the player, two high, so
			// nothing a block drops can roll to them: they'd pick it up before the golem could (a cobblestone once did, 2026-11-09).
			for (int i = -1; i <= 1; i++) {
				for (int k = -1; k <= 1; k++) {
					if (i == 0 && k == 0) continue;
					set(i, 0, k, Block.GLASS);
					set(i, 1, k, Block.GLASS);
				}
			}
			Block stone = Block.STONE;
			for (int i = 2; i <= 5; i++) {
				for (int k = 2; k <= 5; k++) {
					set(i, 0, k, stone);
					hill[0]++;
					if (i >= 3 && i <= 4 && k >= 3 && k <= 4) {
						set(i, 1, k, Block.DIRT);
						hill[0]++;
					}
				}
			}
			set(3, 2, 3, Block.GRASS_BLOCK);
			hill[0]++;
			set(-4, 0, 2, Block.WATER);
			// The plot is wherever the spawn was, snow included, where beta's chunk tick freezes still water open to the sky; frozen, the puddle's walls were
			// ordinary ground and taken (2026-11-10). A block at the top of the world over it keeps it water.
			world.setBlock(x - 4, 127, z + 2, Block.STONE.id);
			for (int[] s : new int[][] {{-5, 2}, {-3, 2}, {-4, 1}, {-4, 3}}) set(s[0], 0, s[1], stone);
			set(-4, -2, -1, stone);
			set(-4, -1, -1, Block.LAVA);
			set(-4, 0, -1, stone);
			set(-2, 0, -4, stone);
			set(-2, 0, -5, Block.TORCH, 5);
			set(-4, 0, -4, Block.DIRT);
			for (int h = 1; h <= 3; h++) set(-4, h, -4, Block.LOG);
			set(-2, 0, -2, Block.COBBLESTONE);
			// Seven high: its top six over the floor, past a player's reach of 4 from an eye 1.62 up beside it.
			for (int h = 0; h < 7; h++) set(-2, h, 3, stone);
			set(6, 0, 0, stone);
			set(7, 0, 0, stone);
			set(0, 0, -6, Block.CHEST);
			((Inventory) world.getBlockEntity(x, y, z - 6)).setStack(26, new ItemStack(Item.STONE_PICKAXE));
			golem.setPositionAndAngles(x - 2.5, y, z - 2.5, 0.0F, 0.0F);
			hold(new ItemStack(Item.IRON_PICKAXE));
			golem.interact(player);
			check("a pick sets it to work", "pickaxe".equals(golem.jobName()) && golem.hasPost() && player.getHand() == null);
			check("inside the inner poles (" + golem.area() + "; the inner poles " + block(-6, 0, -6) + " " + block(6, 0, -6) + " " + block(-6, 0, 6) + " " + block(6, 0, 6) + ")",
					golem.area().marked && golem.area().minX == x - 5 && golem.area().maxX == x + 5);
		});
		int[] ticks = {0};
		int[] notGot = {0}, turned = {0}, idleTicks = {0};
		float[] lastYaw = {Float.NaN};
		String[] turnedAt = {""};
		until("it levels the hill and puts all it took away", 2400, () -> {
			// Compared only across its own ticks here, one straight after another: the world's tick between two of these goes unseen.
			lastYaw[0] = Float.NaN;
			for (int i = 0; i < HURRY; i++) {
				world.updateEntity(golem);
				// Between two pieces (resting, its phase still DECIDE) and waiting, standing still, its head holds: beta's idle look would turn it to the player now and then, two blocks off.
				String s = golem.status();
				boolean idle = (s.startsWith("DECIDE") || s.startsWith("WAIT")) && !golem.hasPath();
				if (idle && !Float.isNaN(lastYaw[0])) {
					idleTicks[0]++;
					if (Math.abs(golem.yaw - lastYaw[0]) > 0.5F && turned[0]++ == 0) turnedAt[0] = String.format("from %.1f to %.1f, %s", lastYaw[0], golem.yaw, s);
				}
				lastYaw[0] = idle ? golem.yaw : Float.NaN;
				// Whenever a gather leaves something, where it was and what lay round it, to see why.
				if (golem.leftLying[2] + golem.leftLying[3] != notGot[0]) {
					notGot[0] = golem.leftLying[2] + golem.leftLying[3];
					StringBuilder near = new StringBuilder();
					for (Object o : world.collectEntitiesByClass(ItemEntity.class, golem.boundingBox.expand(4.0, 2.0, 4.0))) {
						ItemEntity item = (ItemEntity) o;
						near.append(String.format(" %.2f %.2f %.2f delay %d;", item.x - x, item.boundingBox.minY - y, item.z - z, item.pickupDelay));
					}
					System.out.printf("[golems] a gather left one, the golem at %.2f %.2f %.2f, %s; lying near:%s%n", golem.x - x, golem.boundingBox.minY - y, golem.z - z, golem.status(), near);
				}
			}
			if (++ticks[0] % 200 == 0) System.out.println("[golems] levelling: " + golem.worked + " blocks, " + golem.status());
			return golem.worked >= hill[0] && !golem.carriesAnything() && golem.status().startsWith("WAIT");
		});
		then(() -> {
			System.out.println("[golems] the hill took " + ticks[0] + " game ticks at " + (HURRY + 1) + " golem ticks each: " + golem.status());
			boolean level = true;
			for (int i = 2; i <= 5; i++) {
				for (int k = 2; k <= 5; k++) level &= block(i, 0, k) == 0 && block(i, 1, k) == 0 && block(i, 2, k) == 0;
			}
			check("the hill is gone, down to the floor", level);
			boolean floor = true;
			for (int i = -5; i <= 5; i++) {
				for (int k = -5; k <= 5; k++) if (!(i == -4 && k == -1)) floor &= block(i, -1, k) == Block.STONE.id;
			}
			check("and the floor is whole", floor);
			check("the ground walling in a puddle stays, and so does the water", block(-5, 0, 2) == Block.STONE.id && block(-3, 0, 2) == Block.STONE.id && block(-4, 0, 1) == Block.STONE.id && block(-4, 0, 3) == Block.STONE.id && block(-4, 0, 2) == Block.WATER.id);
			world.setBlock(x - 4, 127, z + 2, 0);
			check("so does the ground over the lava", block(-4, 0, -1) == Block.STONE.id);
			check("and the block a torch stands by, and the torch", block(-2, 0, -4) == Block.STONE.id && block(-2, 0, -5) == Block.TORCH.id);
			check("the tree keeps its earth", block(-4, 0, -4) == Block.DIRT.id && block(-4, 1, -4) == Block.LOG.id);
			check("cobble isn't the land", block(-2, 0, -2) == Block.COBBLESTONE.id);
			check("a spire too tall to reach the top of stays", block(-2, 6, 3) == Block.STONE.id && block(-2, 0, 3) == Block.STONE.id);
			check("ground on the poles' line and outside it stays", block(6, 0, 0) == Block.STONE.id && block(7, 0, 0) == Block.STONE.id);
			check("its pick wore once a block, as a player's does", golem.tool() != null && golem.tool().getDamage() == golem.worked);
			// Every block of the hill has somewhere within 2 to stand: it walks up to its work, though its reach is a player's 4.
			note("the furthest it worked from: " + golem.furthest);
			check("its head held still between pieces and waiting, never turning to the player, over " + idleTicks[0] + " ticks" + (turned[0] > 0 ? ": turned " + turned[0] + " times, first " + turnedAt[0] : ""), turned[0] == 0 && idleTicks[0] > 100);
			check("it walked up to every block of the hill, never reaching from further than " + GolemEntity.CLOSE + (golem.stretched > 0 ? ": " + golem.stretched + " times, the last " + golem.lastStretch : ""), golem.stretched == 0 && golem.furthest <= GolemEntity.CLOSE);
			int cobble = inChest(null, Block.COBBLESTONE.id), dirt = inChest(null, Block.DIRT.id);
			note("in the chest: " + cobble + " cobblestone, " + dirt + " dirt, of " + hill[0] + " blocks");
			StringBuilder lying = new StringBuilder();
			for (Object o : world.collectEntitiesByClass(ItemEntity.class, Box.createCached(x - 9, y - 2, z - 9, x + 10, y + 8, z + 10))) {
				ItemEntity item = (ItemEntity) o;
				lying.append(String.format(" %s at %.2f %.2f %.2f;", item.stack.getItem().getTranslationKey(), item.x - x, item.y - y, item.z - z));
			}
			note("lying on the plot:" + (lying.length() == 0 ? " nothing" : lying.toString()) + "; its gathers left " + java.util.Arrays.toString(golem.leftLying)
					+ " (nowhere in touch to stand, no way there, gone out of touch, not got into in its steps, out of time)");
			check("all it took is in the chest", cobble == 16 && dirt == 5);
			check("and it's back at its post", Math.abs(golem.x - (x - 2.5)) < 1.6 && Math.abs(golem.z - (z - 2.5)) < 1.6);
			for (int i = -1; i <= 1; i++) {
				for (int k = -1; k <= 1; k++) {
					set(i, 0, k, null);
					set(i, 1, k, null);
				}
			}
		});
	}

	/** The sums against what a player's own {@code getHardness} says, then a golem with a stopwatch, its work all round it in reach. */
	private void exchange() {
		then(() -> {
			hold(new ItemStack(Item.IRON_PICKAXE));
			check("breakProgress is getHardness for stone", Tools.breakProgress(Block.STONE, player.getHand(), player.onGround, false) == Block.STONE.getHardness(player));
			check("canHarvest asks what a player's does", Tools.canHarvest(player.getHand(), Block.DIAMOND_ORE) == player.canHarvest(Block.DIAMOND_ORE));
			hold(null);
		});
		int[] started = {0}, ticks = {0}, shortest = {Integer.MAX_VALUE}, last = {0};
		then(() -> {
			for (int i = 2; i <= 4; i++) {
				for (int k = -4; k <= -2; k++) if (i != 3 || k != -3) set(i, 0, k, Block.STONE);
			}
			golem.setPositionAndAngles(x + 3.5, y, z - 2.5, 0.0F, 0.0F);
			hold(new ItemStack(Item.DIAMOND_PICKAXE));
			golem.interact(player);
			started[0] = golem.worked;
		});
		await("the first block", 400, () -> golem.worked > started[0]);
		then(() -> {
			started[0] = golem.worked;
			last[0] = golem.worked;
			lastAt = 0;
		});
		until("six more blocks", 800, () -> {
			ticks[0]++;
			if (golem.worked > last[0]) {
				shortest[0] = Math.min(shortest[0], ticks[0] - lastAt);
				last[0] = golem.worked;
				lastAt = ticks[0];
			}
			return golem.worked >= started[0] + 6;
		});
		then(() -> {
			note("6 blocks of stone with a diamond pick in " + ticks[0] + " ticks, the shortest gap " + shortest[0]);
			// A player holding the button: 6 ticks a block at 8 / 1.5 / 30 a tick, 5 after it, 1 to start the next. Doubled; GolemsClientTest measures the player's on a real one.
			check("a golem takes a block in twice a player's 12 ticks, its work in reach", shortest[0] == 24);
		});
	}

	/**
	 * Cracked, with iron in its chest, it goes and mends itself between two blocks, an ingot at a time while a whole ingot's worth is gone and no more: from 15 of its 40, two ingots
	 * to 35, the cracks going with them.
	 */
	private void mending() {
		then(() -> {
			for (int i = 2; i <= 4; i++) set(i, 0, -4, Block.STONE);
			chest().setStack(0, new ItemStack(Item.IRON_INGOT, 5));
			golem.health = 15;
			check("15 of 40 is drawn cracked past half", golem.cracks() == 2);
		});
		until("it goes to its chest and mends itself", 600, () -> golem.health >= 35);
		wait(100);
		then(() -> {
			check("with two ingots, and no third for the 5 still missing", golem.health == 35 && inChest(Item.IRON_INGOT, 0) == 3);
			check("its cracks gone with them", golem.cracks() == 0);
			chest().setStack(0, null);
		});
	}

	/** Full, it goes home; its tool broken, it takes a spare of the same kind from the chest; with none, it waits there, still on its job. */
	private void fullAndSpares() {
		then(() -> {
			for (int i = 2; i <= 4; i++) set(i, 0, -5, Block.STONE);
			for (int i = 0; i < GolemEntity.SLOTS; i++) golem.setSlot(i, new ItemStack(Item.FLINT, 1));
		});
		until("full, it takes it home", 600, () -> inChest(Item.FLINT, 0) == GolemEntity.SLOTS);
		then(() -> {
			ItemStack worn = golem.tool();
			if (worn != null) worn.setDamage(worn.getMaxDamage());
		});
		until("its pick breaks, and it takes the stone one from the chest", 600, () -> golem.tool() != null && golem.tool().getItem() == Item.STONE_PICKAXE);
		then(() -> {
			check("the chest gave it up", inChest(Item.STONE_PICKAXE, 0) == 0);
			golem.tool().setDamage(golem.tool().getMaxDamage());
		});
		until("that breaks too, and there's no other", 600, () -> golem.tool() == null);
		until("so it waits at the chest for one, on its job", 600, () -> golem.tool() == null && "pickaxe".equals(golem.jobName()) && golem.status().contains("waiting for one"));
	}

	/**
	 * The sword, in a pen east of the player: walled so nothing in it sees them (a monster that did would go for them rather than wait to be fought) and roofed so nothing burns in
	 * the day. A zombie, then a skeleton: it walks up to each and puts it down at a zombie's rate with the sword's own damage, a point of wear a blow, takes what they dropped to its
	 * chest and goes back to its post. Then a creeper, a pig, a wild wolf and a zombie pigman, none of them angry, all of which it leaves alone.
	 */
	private void guarding() {
		GolemEntity[] guard = {null};
		int[] difficulty = {0};
		Map<Integer, Integer> dropped = new HashMap<>();
		List<Integer> seen = new ArrayList<>();
		List<LivingEntity> calm = new ArrayList<>();
		LivingEntity[] crowd = new LivingEntity[3];
		then(() -> {
			difficulty[0] = world.difficulty;
			// A server that spawns no monsters is peaceful, where a monster is gone on its first tick.
			world.difficulty = 1;
			for (int i = 2; i <= 8; i++) {
				for (int k = -5; k <= 5; k++) {
					set(i, -1, k, Block.STONE);
					for (int h = 0; h <= 3; h++) set(i, h, k, h == 3 || i == 2 || i == 8 || k == -5 || k == 5 ? Block.STONE : null);
				}
			}
			for (int[] c : new int[][] {{3, -4}, {7, -4}, {3, 4}, {7, 4}}) set(c[0], 0, c[1], Golems.pole.get());
			set(7, 0, 0, Block.CHEST);
			guard[0] = placed(new GolemEntity(world), 5.5, 0.5);
			hold(new ItemStack(Item.IRON_SWORD));
			guard[0].interact(player);
			check("a sword sets it to guard", "sword".equals(guard[0].jobName()) && player.getHand() == null);
			check("inside its poles", guard[0].area().marked && guard[0].area().minX == x + 4 && guard[0].area().maxX == x + 6 && guard[0].area().minZ == z - 3 && guard[0].area().maxZ == z + 3);
		});
		fight(guard, "zombie", () -> new ZombieEntity(world), 5.5, -2.5, dropped, seen);
		fight(guard, "skeleton", () -> new SkeletonEntity(world), 5.5, 2.5, dropped, seen);
		then(() -> {
			Inventory chest = (Inventory) world.getBlockEntity(x + 7, y, z);
			boolean all = true;
			StringBuilder list = new StringBuilder();
			for (Map.Entry<Integer, Integer> e : dropped.entrySet()) {
				int n = 0;
				for (int i = 0; i < chest.size(); i++) {
					ItemStack s = chest.getStack(i);
					if (s != null && s.itemId == e.getKey()) n += s.count;
				}
				all &= n == e.getValue();
				list.append(' ').append(e.getValue()).append(' ').append(Item.ITEMS[e.getKey()].getTranslationKey()).append(" (").append(n).append(" in the chest)");
			}
			note("the two dropped:" + (dropped.isEmpty() ? " nothing, this time" : list.toString()));
			check("whatever they dropped is in its chest", all);
			guard[0].health = GolemEntity.MAX_HEALTH;
			calm.add(placed(new CreeperEntity(world), 4.5, -2.5));
			calm.add(placed(new PigEntity(world), 6.5, 2.5));
			calm.add(placed(new WolfEntity(world), 4.5, 2.5));
			calm.add(placed(new PigZombieEntity(world), 6.5, -2.5));
		});
		int[] worn = {0}, healths = new int[4];
		then(() -> {
			worn[0] = guard[0].tool().getDamage();
			for (int i = 0; i < calm.size(); i++) healths[i] = calm.get(i).health;
		});
		// Six of its looks about, watching that it never goes for any of them (a pig or wolf may nudge it off its post, and it walks back).
		boolean[] chased = {false};
		String[] chasing = {""};
		int[] watched = {0};
		await("six of its looks about", 200, () -> {
			String s = guard[0].status();
			if (s.startsWith("TO_WORK") || s.startsWith("WORKING")) {
				chased[0] = true;
				chasing[0] = s;
			}
			return ++watched[0] >= 120;
		});
		then(() -> {
			check("it never goes for them" + (chased[0] ? ": " + chasing[0] : ""), !chased[0]);
			boolean untouched = true;
			StringBuilder hurt = new StringBuilder();
			for (int i = 0; i < calm.size(); i++) {
				LivingEntity e = calm.get(i);
				if (e.isAlive() && e.health == healths[i]) continue;
				untouched = false;
				hurt.append(' ').append(e.getClass().getSimpleName()).append(' ').append(e.health);
			}
			check("it leaves a creeper, a pig, a wild wolf and a calm zombie pigman alone" + hurt, untouched && guard[0].tool().getDamage() == worn[0]);
			check("and nothing blew up", block(5, 3, 0) == Block.STONE.id && block(2, 1, 0) == Block.STONE.id && guard[0].health == GolemEntity.MAX_HEALTH);
			note("then: " + guard[0].status());
			for (LivingEntity e : calm) e.markDead();
			// Three zombies at once, all after it from the start, for what a crowd costs it: a measure, not a rule.
			guard[0].health = GolemEntity.MAX_HEALTH;
			for (int k = 0; k < 3; k++) {
				crowd[k] = placed(new ZombieEntity(world), 4.5 + k, k == 1 ? -3.5 : -2.5);
				((MobEntity) crowd[k]).setTarget(guard[0]);
			}
		});
		until("it puts down three zombies at once", 900, () -> {
			if (guard[0].dead || guard[0].health <= 0) return true;
			for (LivingEntity c : crowd) if (c.isAlive()) return false;
			return true;
		});
		then(() -> {
			note("three zombies at once: the golem lost " + (GolemEntity.MAX_HEALTH - Math.max(0, guard[0].health)) + " of its " + GolemEntity.MAX_HEALTH + (guard[0].health <= 0 ? ", and was killed" : ""));
			if (guard[0].health > 0) {
				hold(null);
				guard[0].interact(player);
				check("an empty hand takes its sword back", player.getHand() != null && player.getHand().getItem() == Item.IRON_SWORD && guard[0].jobName() == null);
				hold(null);
			}
			// The plot as it came.
			guard[0].markDead();
			for (LivingEntity c : crowd) if (c != null) c.markDead();
			Inventory chest = (Inventory) world.getBlockEntity(x + 7, y, z);
			for (int i = 0; i < chest.size(); i++) chest.setStack(i, null);
			for (int i = 2; i <= 8; i++) {
				for (int k = -5; k <= 5; k++) {
					for (int h = 3; h >= 0; h--) set(i, h, k, null);
				}
			}
			for (Object o : world.collectEntitiesByClass(Entity.class, pen())) {
				if (o instanceof ItemEntity || o instanceof ArrowEntity) ((Entity) o).markDead();
			}
			world.difficulty = difficulty[0];
		});
	}

	/**
	 * One foe let into the pen: walked up to and put down with an iron sword, 8 a blow (a player's with the same sword) and never two blows within a zombie's 20 ticks; the sword worn
	 * a point a blow; then what it dropped gathered, put away at home, and the guard out of work on its rounds. Where it stands then is chance, a guard strolling since it
	 * walks its rounds: the check asked for it back at its post until 2026-11-10, and passed only when a pause still running kept it by its chest or a stroll happened to end there.
	 */
	private void fight(GolemEntity[] guard, String what, java.util.function.Supplier<LivingEntity> foe, double dx, double dz, Map<Integer, Integer> dropped, List<Integer> seen) {
		LivingEntity[] it = {null};
		int[] ticks = {0}, last = {0}, lastAt = {0}, wear = {0};
		List<String> blows = new ArrayList<>();
		boolean[] steady = {true};
		then(() -> {
			guard[0].health = GolemEntity.MAX_HEALTH;
			wear[0] = guard[0].tool().getDamage();
			it[0] = placed(foe.get(), dx, dz);
			last[0] = it[0].health;
		});
		until("it walks up to a " + what + " and puts it down", 600, () -> {
			ticks[0]++;
			watchDrops(dropped, seen);
			if (it[0].health < last[0]) {
				int took = last[0] - it[0].health;
				if (took != 8 || (!blows.isEmpty() && ticks[0] - lastAt[0] < 20)) steady[0] = false;
				blows.add(took + (blows.isEmpty() ? " at " + ticks[0] : " after " + (ticks[0] - lastAt[0])));
				last[0] = it[0].health;
				lastAt[0] = ticks[0];
			}
			if (ticks[0] % 100 == 0) System.out.println("[golems] guarding: " + guard[0].status());
			return !it[0].isAlive();
		});
		then(() -> {
			note("the " + what + "'s blows: " + blows + "; the golem lost " + (GolemEntity.MAX_HEALTH - guard[0].health) + " of its " + GolemEntity.MAX_HEALTH + " to it");
			check("8 a blow with an iron sword, none within 20 ticks of the last", steady[0] && blows.size() == 3);
			check("the sword worn a point a blow", guard[0].tool() != null && guard[0].tool().getDamage() - wear[0] == blows.size());
		});
		until("what the " + what + " dropped is gathered and put away, and it's on its rounds again", 600, () -> {
			watchDrops(dropped, seen);
			boolean lying = false;
			for (Object o : world.collectEntitiesByClass(ItemEntity.class, pen())) lying |= !((ItemEntity) o).dead;
			return !lying && !guard[0].carriesAnything() && guard[0].status().contains("its rounds");
		});
	}

	private Box pen() {
		return Box.createCached(x + 3, y, z - 4, x + 8, y + 3, z + 5);
	}

	/** What's come to lie in the pen, by item, each counted once. */
	private void watchDrops(Map<Integer, Integer> dropped, List<Integer> seen) {
		for (Object o : world.collectEntitiesByClass(ItemEntity.class, pen())) {
			ItemEntity item = (ItemEntity) o;
			if (item.dead || seen.contains(item.id)) continue;
			seen.add(item.id);
			dropped.merge(item.stack.itemId, item.stack.count, Integer::sum);
		}
	}

	private <T extends Entity> T placed(T entity, double dx, double dz) {
		entity.setPositionAndAngles(x + dx, y, z + dz, 0.0F, 0.0F);
		world.spawnEntity(entity);
		return entity;
	}

	/** An empty hand: the tool back, the job over, what it carried at its feet; it stays where it is. Then it's left for the restart. */
	private void takingBack() {
		then(() -> {
			golem.setTool(new ItemStack(Item.IRON_PICKAXE));
			golem.setSlot(0, new ItemStack(Item.COAL, 3));
			hold(null);
			golem.interact(player);
			check("an empty hand takes its tool back", player.getHand() != null && player.getHand().getItem() == Item.IRON_PICKAXE && golem.tool() == null);
			check("and ends its job", golem.jobName() == null && !golem.hasPost() && golem.following() == null);
			int coal = 0;
			for (Object o : world.collectEntitiesByClass(ItemEntity.class, golem.boundingBox.expand(2.0, 2.0, 2.0))) {
				ItemStack s = ((ItemEntity) o).stack;
				if (s.getItem() == Item.COAL) coal += s.count;
			}
			check("and puts down what it carried", coal == 3 && !golem.carriesAnything());
			// The plot as it came, for the client test; then for the restart, at work in a little area of its own away from where the client test works, with nothing in it to do.
			for (int i = -8; i <= 8; i++) {
				for (int k = -8; k <= 8; k++) {
					for (int h = 7; h >= 0; h--) set(i, h, k, null);
					set(i, -1, k, Block.STONE);
				}
			}
			for (Object o : world.collectEntitiesByClass(ItemEntity.class, Box.createCached(x - 9, y - 2, z - 9, x + 10, y + 8, z + 10))) ((ItemEntity) o).markDead();
			for (int[] c : new int[][] {{-7, -7}, {-1, -7}, {-7, -1}, {-1, -1}}) set(c[0], 0, c[1], Golems.pole.get());
			golem.setPositionAndAngles(x - 2.5, y, z - 2.5, 0.0F, 0.0F);
			hold(new ItemStack(Item.DIAMOND_PICKAXE));
			golem.interact(player);
			hold(null);
			saved();
		});
	}

	/** What a save keeps, through the golem's own NBT: what it carries, and whose heels it's at. The restart itself shows the rest. */
	private void saved() {
		golem.setSlot(4, new ItemStack(Item.COAL, 5));
		NbtCompound nbt = new NbtCompound();
		golem.writeNbt(nbt);
		GolemEntity copy = new GolemEntity(world);
		copy.readNbt(nbt);
		check("its NBT keeps its job, post, area, tool and what it carries", "pickaxe".equals(copy.jobName()) && copy.postX() == golem.postX() && copy.area().minX == x - 6 && copy.area().marked && copy.tool() != null && copy.slot(4) != null && copy.slot(4).count == 5);
		golem.setSlot(4, null);
		GolemEntity follower = new GolemEntity(world);
		follower.interact(player);
		nbt = new NbtCompound();
		follower.writeNbt(nbt);
		copy = new GolemEntity(world);
		copy.readNbt(nbt);
		check("and a follower's whose heels it's at", player.name.equals(copy.following()));
	}

	private void afterRestart() {
		List<?> found = golems();
		check("the golem came back", found.size() == 1);
		if (found.isEmpty()) return;
		GolemEntity g = (GolemEntity) found.get(0);
		check("with its job, its post, its area and its tool", "pickaxe".equals(g.jobName()) && g.hasPost() && g.area() != null && g.area().marked && g.area().minX == x - 6 && g.tool() != null && g.tool().getItem() == Item.DIAMOND_PICKAXE);
		check("and its poles still glow", lit(new int[][] {{-7, -7}, {-1, -7}, {-7, -1}, {-1, -1}}, true));
		then(() -> {
			hold(null);
			g.interact(player);
			hold(new ItemStack(Item.IRON_PICKAXE));
			g.interact(player);
			check("its poles are found again after the restart", g.area().marked && g.area().minX == x - 6);
			int carried = 0;
			g.damage(player, 1000);
			check("it can be killed", g.dead || g.health <= 0);
			int core = 0, tools = 0, iron = 0;
			for (Object o : world.collectEntitiesByClass(ItemEntity.class, g.boundingBox.expand(3.0, 3.0, 3.0))) {
				ItemStack s = ((ItemEntity) o).stack;
				if (s.getItem() == Golems.core.get()) core += s.count;
				if (s.getItem() == Item.IRON_PICKAXE) tools += s.count;
				if (s.itemId == Block.IRON_BLOCK.id || s.getItem() == Item.IRON_INGOT) iron += s.count;
			}
			check("and drops its core and its tool, and not the iron", core == 1 && tools == 1 && iron == 0 && carried == 0);
		});
	}

	// The bow.

	/** Its pen, east of the player: walls of stone at these x and z, a roof of glowstone (lit, so nothing spawns in it), inside x 3 to 7, z -7 to 7, 6 high. */
	private static final int PEN_MIN_X = 2, PEN_MAX_X = 8, PEN_Z = 8, PEN_ROOF = 6;

	/**
	 * The bow, in a long pen east of the player (walled so nothing in it sees them), with a chest of arrows beside its post. It takes a quiver from the chest; at a zombie 11 blocks
	 * off it walks up into its reach and shoots, and at one 9 off it shoots at a skeleton's rate, an arrow every 30 ticks; with Explosive Arrows in the chest too it shoots its plain
	 * arrows first, at a creeper and a zombie, then the Explosive ones, and nothing is broken. Then from a tower 4 blocks high, its foot fenced off, it kills a creeper that comes for it
	 * and is never hurt; and on the ground it steps back from a creeper coming for it, shooting.
	 */
	private void archery() {
		GolemEntity[] archer = {null};
		LivingEntity[] foe = {null};
		ArrowEntity[] first = {null};
		int[] difficulty = {0}, ticks = {0}, lowest = {0};
		boolean[] strayed = {false};
		List<Integer> shots = new ArrayList<>();
		List<ArrowEntity> loosed = new ArrayList<>();
		Item explosive = Names.item("explosive-arrows:explosive_arrow");
		int[][][] under = new int[PEN_MAX_X - PEN_MIN_X + 1][4][2 * PEN_Z + 1];
		then(() -> {
			difficulty[0] = world.difficulty;
			world.difficulty = 1;
			// The ground under the pen as it came, put back after the creeper's blast.
			for (int i = PEN_MIN_X; i <= PEN_MAX_X; i++) {
				for (int h = -4; h <= -1; h++) {
					for (int k = -PEN_Z; k <= PEN_Z; k++) under[i - PEN_MIN_X][h + 4][k + PEN_Z] = block(i, h, k);
				}
			}
			for (int i = PEN_MIN_X; i <= PEN_MAX_X; i++) {
				for (int k = -PEN_Z; k <= PEN_Z; k++) {
					boolean wall = i == PEN_MIN_X || i == PEN_MAX_X || k == -PEN_Z || k == PEN_Z;
					for (int h = 0; h <= PEN_ROOF; h++) set(i, h, k, h == PEN_ROOF ? Block.GLOWSTONE : wall ? Block.STONE : null);
				}
			}
			set(6, 0, -7, Block.CHEST);
			penChest(6, 0, -7).setStack(0, new ItemStack(Item.ARROW, 32));
			archer[0] = placed(new GolemEntity(world), 5.5, -5.5);
			hold(new ItemStack(Item.BOW));
			archer[0].interact(player);
			check("a bow sets it to stand sentry", "bow".equals(archer[0].jobName()) && player.getHand() == null);
			check("Explosive Arrows are here for it", explosive != null);
		});
		until("it takes a quiver from its chest, 16 of the 32 arrows", 100, () -> carried(archer[0], Item.ARROW) == 16 && inPenChest(6, 0, -7, Item.ARROW) == 16);

		// A zombie held 11 blocks off, past a skeleton's reach, and then 9, feet to feet as a skeleton measures, and kept whole so it lasts. At 11 it walks up until it can shoot, as a
		// skeleton does, and stands there.
		then(() -> foe[0] = placed(new ZombieEntity(world), 5.5, 5.5));
		watchShots(archer, foe, 5.5, 5.5, 100, ticks, shots, loosed, strayed);
		then(() -> {
			double d = Math.sqrt((archer[0].x - foe[0].x) * (archer[0].x - foe[0].x) + (archer[0].z - foe[0].z) * (archer[0].z - foe[0].z));
			check(String.format("at a zombie 11 blocks off it walks up into its reach and shoots, stopping %.1f off", d), strayed[0] && !shots.isEmpty() && d >= 4.0 && d < 10.0);
		});
		watchShots(archer, foe, 5.5, 3.5, 200, ticks, shots, loosed, strayed);
		then(() -> {
			List<Integer> gaps = new ArrayList<>();
			for (int i = 1; i < shots.size(); i++) gaps.add(shots.get(i) - shots.get(i - 1));
			note("at a zombie 9 blocks off: " + (shots.isEmpty() ? "no arrows" : "the first arrow at tick " + shots.get(0) + ", the rest after " + gaps) + "; " + archer[0].status());
			// Never sooner than a skeleton; and at its rate while nothing stands in the line. One run of eight had a single gap of 33, and the four after it, every held shot logged, none.
			int exact = 0;
			boolean never = true;
			for (int g : gaps) {
				never &= g >= BOW_RATE;
				if (g == BOW_RATE) exact++;
			}
			check("9 blocks off it shoots, an arrow every 30 ticks, a skeleton's rate", shots.size() >= 6 && never && exact >= gaps.size() - 1);
			check("its bow unworn, as beta's bow is", archer[0].tool() != null && archer[0].tool().getDamage() == 0);
			foe[0].markDead();
			penChest(6, 0, -7).setStack(1, new ItemStack(explosive, 8));
		});

		// With both kinds: the first in its quiver at anything, as a player's bow takes the first arrows it finds. The plain ones came first from its chest, so a creeper and then
		// a zombie, each held 7 blocks off, both get plain arrows; with the plain ones gone, the zombie gets Explosive Arrows.
		until("with Explosive Arrows in the chest as well, it takes those too", 100, () -> carried(archer[0], explosive) == 8);
		then(() -> {
			seen(archer[0], loosed);
			foe[0] = placed(new CreeperEntity(world), 5.5, 1.5);
		});
		until("it shoots at a creeper", 100, () -> {
			keep(foe[0], 5.5, 1.5);
			first[0] = loose(archer[0], loosed);
			return first[0] != null;
		});
		then(() -> {
			// A plain arrow that misses stays stuck where it lands, so nothing waits for this one to be gone.
			check("a plain arrow, the first in its quiver, at a creeper too", first[0] != null && first[0].getClass() == ArrowEntity.class);
			foe[0].markDead();
			seen(archer[0], loosed);
			foe[0] = placed(new ZombieEntity(world), 5.5, 1.5);
		});
		until("then it shoots at a zombie", 100, () -> {
			keep(foe[0], 5.5, 1.5);
			first[0] = loose(archer[0], loosed);
			return first[0] != null;
		});
		then(() -> {
			check("and at a zombie", first[0] != null && first[0].getClass() == ArrowEntity.class);
			for (int i = 0; i < GolemEntity.SLOTS; i++) {
				ItemStack s = archer[0].slot(i);
				if (s != null && s.getItem() == Item.ARROW) archer[0].setSlot(i, null);
			}
			seen(archer[0], loosed);
		});
		until("its plain arrows gone, it shoots its Explosive Arrows", 100, () -> {
			keep(foe[0], 5.5, 1.5);
			first[0] = loose(archer[0], loosed);
			return first[0] != null;
		});
		await("the arrow to land", 40, () -> {
			keep(foe[0], 5.5, 1.5);
			return first[0].dead;
		});
		then(() -> {
			check("an Explosive Arrow, at whatever it shoots", first[0] != null && exploding(first[0]));
			check("and nothing in the pen is broken", penIntact());
			foe[0].markDead();
		});

		// On a perch: a tower 4 blocks high, the post on it and a chest of arrows beside it, plain arrows only. Its foot is fenced off 4 blocks out, as a player keeps creepers where it
		// can see them: one pressed to the wall right under it would be out of its sight. A creeper 7 out that has seen it.
		List<Integer> healths = new ArrayList<>();
		then(() -> {
			emptyChest(6, 0, -7);
			for (int i = 0; i < GolemEntity.SLOTS; i++) archer[0].setSlot(i, null);
			archer[0].setSlot(0, new ItemStack(Item.ARROW, 16));
			for (int h = 0; h < PERCH; h++) {
				set(5, h, -6, Block.STONE);
				set(6, h, -6, Block.STONE);
			}
			for (int i = PEN_MIN_X + 1; i < PEN_MAX_X; i++) set(i, 0, -2, Block.FENCE);
			set(6, PERCH, -6, Block.CHEST);
			penChest(6, PERCH, -6).setStack(0, new ItemStack(Item.ARROW, 16));
			archer[0].setPositionAndAngles(x + 5.5, y + PERCH, z - 5.5, 0.0F, 0.0F);
			hold(new ItemStack(Item.BOW));
			archer[0].interact(player);
			hold(null);
			check("handed a bow on top of the tower, its post is up there", archer[0].postY() == y + PERCH && archer[0].postX() == x + 5);
			seen(archer[0], loosed);
			foe[0] = placed(new CreeperEntity(world), 5.5, 1.5);
			((MobEntity) foe[0]).setTarget(archer[0]);
			lowest[0] = archer[0].health;
			shots.clear();
		});
		until("from its perch it kills a creeper that comes for it", 600, () -> {
			lowest[0] = Math.min(lowest[0], archer[0].health);
			if (healths.isEmpty() || healths.get(healths.size() - 1) != foe[0].health) healths.add(foe[0].health);
			if (loose(archer[0], loosed) != null) shots.add(0);
			return !foe[0].isAlive();
		});
		then(() -> {
			note("on the perch: " + shots.size() + " arrows, the creeper's health going " + healths + ", last at " + String.format("%.1f %.1f %.1f", foe[0].x - x, foe[0].y - y, foe[0].z - z));
			check("killed, not gone off", foe[0].health <= 0);
			check("and the golem never hurt", lowest[0] == GolemEntity.MAX_HEALTH && penIntact());
		});

		// On the ground, with room behind it: a creeper 7 1/2 blocks off that has seen it, and the length of the pen to step back along. Whether it kills it first is a measure, not a rule
		// (ArcherRun has the numbers): what's checked is that it steps back, shooting.
		int[] creeperHealth = {0};
		double[] startZ = {0.0};
		then(() -> {
			emptyChest(6, PERCH, -6);
			for (int h = 0; h < PERCH; h++) {
				set(5, h, -6, null);
				set(6, h, -6, null);
			}
			for (int i = PEN_MIN_X + 1; i < PEN_MAX_X; i++) set(i, 0, -2, null);
			for (int i = 0; i < GolemEntity.SLOTS; i++) archer[0].setSlot(i, null);
			archer[0].setSlot(0, new ItemStack(Item.ARROW, 16));
			archer[0].setPositionAndAngles(x + 5.5, y, z - 0.5, 0.0F, 0.0F);
			hold(new ItemStack(Item.BOW));
			archer[0].interact(player);
			hold(null);
			startZ[0] = archer[0].z;
			seen(archer[0], loosed);
			foe[0] = placed(new CreeperEntity(world), 5.5, 7.0);
			((MobEntity) foe[0]).setTarget(archer[0]);
			shots.clear();
		});
		until("on the ground, a creeper it shoots at is killed or goes off", 400, () -> {
			if (loose(archer[0], loosed) != null) shots.add(0);
			if (foe[0].isAlive()) creeperHealth[0] = foe[0].health;
			return !foe[0].isAlive();
		});
		then(() -> {
			note(String.format("on the ground: %d arrows, the creeper %s; the golem stepped back %d times, %.1f blocks, and was left with %d of %d", shots.size(),
					foe[0].health <= 0 ? "killed" : "gone off at " + creeperHealth[0] + " of 20", archer[0].backedOff(), startZ[0] - archer[0].z, archer[0].health, GolemEntity.MAX_HEALTH));
			check("it steps back from a creeper coming for it, shooting", archer[0].backedOff() > 0 && startZ[0] - archer[0].z >= 2.0 && shots.size() >= 2);
			// The plot as it came.
			archer[0].markDead();
			for (int i = PEN_MIN_X; i <= PEN_MAX_X; i++) {
				for (int k = -PEN_Z; k <= PEN_Z; k++) {
					for (int h = PEN_ROOF; h >= 0; h--) set(i, h, k, null);
					for (int h = -1; h >= -4; h--) set(i, h, k, Block.BLOCKS[under[i - PEN_MIN_X][h + 4][k + PEN_Z]]);
				}
			}
			for (Object o : world.collectEntitiesByClass(Entity.class, Box.createCached(x + PEN_MIN_X - 3, y - 5, z - PEN_Z - 3, x + PEN_MAX_X + 4, y + PEN_ROOF + 4, z + PEN_Z + 4))) {
				if (o instanceof ItemEntity || o instanceof ArrowEntity) ((Entity) o).markDead();
			}
			world.difficulty = difficulty[0];
		});
	}

	/**
	 * The perch's height. A creeper's fuse lights under 3 blocks from its target, feet to feet; at 4 one at the foot is past that unless it jumps right against the wall under the golem
	 * (its feet rise 1.25), and at 3 one did that in four tries of a survey (2026-11-10).
	 */
	private static final int PERCH = 4;

	/** The skeleton's rate, which the bow's is. */
	private static final int BOW_RATE = 30;

	/** {@code n} ticks with the foe held at a place, noting the tick of each arrow the archer looses, and whether it ever stands anywhere but its post. */
	private void watchShots(GolemEntity[] archer, LivingEntity[] foe, double dx, double dz, int n, int[] ticks, List<Integer> shots, List<ArrowEntity> loosed, boolean[] strayed) {
		then(() -> {
			seen(archer[0], loosed);
			ticks[0] = 0;
			shots.clear();
		});
		await("a zombie held there", n + 5, () -> {
			keep(foe[0], dx, dz);
			ticks[0]++;
			if (loose(archer[0], loosed) != null) shots.add(ticks[0]);
			GolemEntity g = archer[0];
			strayed[0] |= MathHelper.floor(g.x) != g.postX() || MathHelper.floor(g.z) != g.postZ() || g.feetY() != g.postY();
			return ticks[0] >= n;
		});
	}

	/** Where it's put, whole, standing still. */
	private void keep(LivingEntity e, double dx, double dz) {
		e.setPosition(x + dx, y, z + dz);
		e.velocityX = e.velocityY = e.velocityZ = 0.0;
		e.health = 20;
	}

	/** The next arrow the archer has loosed that hasn't been seen, or null. */
	private ArrowEntity loose(GolemEntity archer, List<ArrowEntity> loosed) {
		for (Object o : world.collectEntitiesByClass(ArrowEntity.class, Box.createCached(x + PEN_MIN_X, y - 2, z - PEN_Z, x + PEN_MAX_X + 1, y + PEN_ROOF + 1, z + PEN_Z + 1))) {
			ArrowEntity a = (ArrowEntity) o;
			if (a.owner != archer || loosed.contains(a)) continue;
			loosed.add(a);
			return a;
		}
		return null;
	}

	/** Every arrow of the archer's about now, counted as seen: one loosed in a tick between two steps that look isn't the next phase's. */
	private void seen(GolemEntity archer, List<ArrowEntity> loosed) {
		while (loose(archer, loosed) != null) {
		}
	}

	/** Explosive Arrows' own arrow, known by its class's name: the test needs nothing of that mod but its being there. */
	private static boolean exploding(ArrowEntity a) {
		return a.getClass().getSimpleName().equals("ExplosiveArrow");
	}

	/** The pen's floor, walls and roof as they were built. */
	private boolean penIntact() {
		for (int i = PEN_MIN_X; i <= PEN_MAX_X; i++) {
			for (int k = -PEN_Z; k <= PEN_Z; k++) {
				if (block(i, -1, k) != Block.STONE.id || block(i, PEN_ROOF, k) != Block.GLOWSTONE.id) return false;
				boolean wall = i == PEN_MIN_X || i == PEN_MAX_X || k == -PEN_Z || k == PEN_Z;
				for (int h = 0; wall && h < PEN_ROOF; h++) if (block(i, h, k) != Block.STONE.id) return false;
			}
		}
		return true;
	}

	private Inventory penChest(int dx, int dy, int dz) {
		return (Inventory) world.getBlockEntity(x + dx, y + dy, z + dz);
	}

	private int inPenChest(int dx, int dy, int dz, Item item) {
		Inventory chest = penChest(dx, dy, dz);
		int n = 0;
		for (int i = 0; chest != null && i < chest.size(); i++) {
			ItemStack s = chest.getStack(i);
			if (s != null && s.getItem() == item) n += s.count;
		}
		return n;
	}

	/** Emptied, so nothing spills, and gone. */
	private void emptyChest(int dx, int dy, int dz) {
		Inventory chest = penChest(dx, dy, dz);
		for (int i = 0; chest != null && i < chest.size(); i++) chest.setStack(i, null);
		set(dx, dy, dz, null);
	}

	private static int carried(GolemEntity g, Item item) {
		int n = 0;
		for (int i = 0; i < GolemEntity.SLOTS; i++) {
			ItemStack s = g.slot(i);
			if (s != null && s.getItem() == item) n += s.count;
		}
		return n;
	}
}
