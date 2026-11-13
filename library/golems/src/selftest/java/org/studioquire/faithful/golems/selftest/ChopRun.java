package org.studioquire.faithful.golems.selftest;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import javax.imageio.ImageIO;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.studioquire.faithful.golems.GolemEntity;
import org.studioquire.faithful.golems.Golems;
import org.studioquire.faithful.golems.Tree;
import org.studioquire.faithful.harness.test.server.ServerMove;
import org.studioquire.oldfaithful.core.api.Chunks;

/**
 * An axe golem in a real forest, on its own terms: only when a {@code golems/chop.properties} is found in a folder above the game's, never in an ordinary self-test. Boundary poles round a
 * square of woodland, a chest of spare axes by the post, and the golem ticked {@code speed} times a game tick, with the area's time kept in step with it: its leaves' and saplings' random
 * ticks come {@code speed} times as often, and what lies on the ground ages as fast, so leaves fall, saplings grow and drops despawn at the pace the golem sees. Beside the file: the land from
 * above before and after ({@code <name>-<side>-before.png}, {@code -after.png}) and what it did ({@code .txt}): trees felled and left, logs and saplings put away, stumps planted, logs left
 * hanging.
 * <p>
 * Keys: {@code name}; {@code half}, the area's half width (12); {@code speed} (80); {@code maxTicks} in game ticks (3000); {@code dx}, {@code dz} from the server's plot and {@code singleDx},
 * {@code singleDz} from single player's to start looking for woods; {@code axe}, an item id (iron's); {@code noChest}.
 */
public final class ChopRun {
	private static final int HALF = 32;
	/** Beta picks 80 cells of a chunk's 32768 each tick for a random tick. */
	private static final double RANDOM_TICK = 80.0 / 32768.0;

	private final File dir;
	private final Properties p;
	private World world;
	private GolemEntity golem;
	private int siteX, siteY, siteZ, half, ticks, chestX, chestY, chestZ;
	private long golemTicks;
	private final Random random = new Random(1);
	private final List<int[]> treesBefore = new ArrayList<>();
	private int logsBefore, hangingBefore;
	private final Map<String, Integer> goods = new HashMap<>();
	private int axesUsed, looks;
	private long slowestLook;
	/** Logs cut from within 2 of their column along the ground, from 2 to 3, and further: those above its eye, and the rest. */
	private final int[] stoodHigh = new int[3], stoodLow = new int[3];

	private ChopRun(File dir, Properties p) {
		this.dir = dir;
		this.p = p;
	}

	public static ChopRun asked() {
		for (File f = new File(System.getProperty("user.dir")).getAbsoluteFile(); f != null; f = f.getParentFile()) {
			File file = new File(f, "golems/chop.properties");
			if (!file.isFile()) continue;
			Properties p = new Properties();
			try (java.io.Reader r = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
				p.load(r);
			} catch (IOException e) {
				return null;
			}
			return new ChopRun(file.getParentFile(), p);
		}
		return null;
	}

	private int prop(String key, int fallback) {
		return Integer.parseInt(p.getProperty(key, String.valueOf(fallback)).trim());
	}

	private static boolean onServer() {
		return FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER;
	}

	private String name() {
		return p.getProperty("name", "chop").trim() + (onServer() ? "-server" : "-single");
	}

	void start(GolemsTest test, World world, PlayerEntity player, int x, int y, int z) {
		this.world = world;
		half = prop("half", 12);
		siteX = x + prop(onServer() ? "dx" : "singleDx", 60);
		siteZ = z + prop(onServer() ? "dz" : "singleDz", 0);
		searchWoods();
		if (onServer()) ServerMove.load(world, siteX, siteZ, HALF + 8);
		world.setBlock(siteX, 120, siteZ, Block.GLASS.id);
		move(player, siteX + 0.5, 121.01, siteZ + 0.5);
		int waitX = siteX, waitZ = siteZ;
		test.levelLand(() -> {
			for (int cx = (siteX - HALF) >> 4; cx <= (siteX + HALF) >> 4; cx++) {
				for (int cz = (siteZ - HALF) >> 4; cz <= (siteZ + HALF) >> 4; cz++) {
					if (!Chunks.isLoaded(world, cx, cz) || !world.getChunkFromPos(cx << 4, cz << 4).terrainPopulated) return false;
				}
			}
			return true;
		}, () -> {
			world.setBlock(waitX, 120, waitZ, 0);
			begin(test, player);
		});
	}

	/** Out in rings from where it was asked for, for land mostly under leaves and out of the water. */
	private void searchWoods() {
		int startX = siteX, startZ = siteZ;
		for (int ring = 0; ring <= 14; ring++) {
			for (int i = -ring; i <= ring; i++) {
				for (int j = -ring; j <= ring; j++) {
					if (Math.max(Math.abs(i), Math.abs(j)) != ring) continue;
					int cx = startX + i * 48, cz = startZ + j * 48;
					if (onServer()) ServerMove.load(world, cx, cz, 24);
					int leaves = 0, wet = 0, samples = 0;
					for (int a = -12; a <= 12; a += 3) {
						for (int b = -12; b <= 12; b += 3) {
							int top = world.getTopY(cx + a, cz + b) - 1;
							int id = top < 1 ? 0 : world.getBlockId(cx + a, top, cz + b);
							samples++;
							if (id == Block.LEAVES.id) leaves++;
							if (id != 0 && (Block.BLOCKS[id].material.isFluid() || id == Block.ICE.id)) wet++;
						}
					}
					if (leaves * 10 >= samples * 4 && wet * 10 <= samples) {
						siteX = cx;
						siteZ = cz;
						return;
					}
				}
			}
		}
	}

	private void begin(GolemsTest test, PlayerEntity player) {
		siteY = ground(siteX, siteZ);
		// The post on open ground near the middle.
		for (int r = 0; r <= 4 && world.getBlockId(siteX, siteY, siteZ) != 0; r++) {
			siteX++;
			siteY = ground(siteX, siteZ);
		}
		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) world.setBlock(siteX + i, 125, siteZ + j, Block.GLASS.id);
		}
		move(player, siteX + 0.5, 126.01, siteZ + 0.5);
		// Poles at the corners, a step further out where a trunk stands on one; whatever grows there is cleared for it.
		for (int tries = 0; tries < 4; tries++, half++) {
			int c = half + 1;
			boolean clear = true;
			for (int[] d : new int[][] {{-c, -c}, {c, -c}, {-c, c}, {c, c}}) {
				int sx = siteX + d[0], sz = siteZ + d[1];
				clear &= world.getBlockId(sx, ground(sx, sz), sz) != Block.LOG.id;
			}
			if (!clear) continue;
			for (int[] d : new int[][] {{-c, -c}, {c, -c}, {-c, c}, {c, c}}) {
				int sx = siteX + d[0], sz = siteZ + d[1], sy = ground(sx, sz);
				world.setBlock(sx, sy, sz, 0);
				world.setBlock(sx, sy, sz, Golems.pole.get().id);
			}
			break;
		}
		Item axe = Item.ITEMS[prop("axe", Item.IRON_AXE.id)];
		boolean hasChest = prop("noChest", 0) == 0;
		chestX = siteX - 2;
		chestZ = siteZ - 2;
		chestY = ground(chestX, chestZ);
		if (hasChest) {
			world.setBlock(chestX, chestY, chestZ, Block.CHEST.id);
			Inventory chest = (Inventory) world.getBlockEntity(chestX, chestY, chestZ);
			for (int i = 18; i < 27; i++) chest.setStack(i, new ItemStack(axe));
		}
		for (int i = -half; i <= half; i++) {
			for (int j = -half; j <= half; j++) {
				int x = siteX + i, z = siteZ + j;
				int top = Math.min(world.getTopY(x, z), 126);
				for (int y = top; y > 1; y--) {
					if (world.getBlockId(x, y, z) == Block.LOG.id && Tree.at(world, x, y, z)) treesBefore.add(new int[] {x, y, z, world.getBlockMeta(x, y, z) & 3});
				}
			}
		}
		int[] logs = logs();
		logsBefore = logs[0];
		hangingBefore = logs[1];
		System.out.println("[golems] woods at " + siteX + " " + siteY + " " + siteZ + ": " + treesBefore.size() + " trees, " + logsBefore + " logs round them, " + hangingBefore + " hanging already");
		try {
			map("before");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		GolemEntity g = new GolemEntity(world);
		golem = g;
		g.setPositionAndAngles(siteX + 0.5, siteY, siteZ + 0.5, 0.0F, 0.0F);
		world.spawnEntity(g);
		ItemStack hand = player.inventory.main[player.inventory.selectedSlot];
		player.inventory.main[player.inventory.selectedSlot] = new ItemStack(axe);
		g.interact(player);
		player.inventory.main[player.inventory.selectedSlot] = hand;
		System.out.println("[golems] its area: " + g.area());
		int speed = prop("speed", 80);
		test.levelSteps(prop("maxTicks", 3000) + 100, () -> {
			ticks++;
			if (onServer() && ticks % 20 == 0) ServerMove.load(world, (int) g.x, (int) g.z, 40);
			for (int i = 0; i < speed && !g.dead; i++) {
				String before = g.status();
				int done = g.worked;
				double gx = Math.floor(g.x) + 0.5, gz = Math.floor(g.z) + 0.5, eye = g.feetY() + GolemEntity.EYE;
				int[] on = Felling.workingOn(before);
				world.updateEntity(g);
				golemTicks++;
				if (golemTicks % 10 == 0) Lying.hurry(world, g, 6, 10);
				long look = g.lastLookMicros();
				if (look > 0) {
					looks++;
					slowestLook = Math.max(slowestLook, look);
				}
				// A log just cut: how far along the ground from its column it stood, for those above its eye and the rest.
				if (g.worked > done && on != null && world.getBlockId(on[0], on[1], on[2]) != Block.LOG.id && world.getBlockId(on[0], on[1], on[2]) != Block.SAPLING.id) {
					double along = Felling.along(gx, gz, on[0], on[2]);
					int[] counts = on[1] > eye ? stoodHigh : stoodLow;
					counts[along <= GolemEntity.CLOSE ? 0 : along <= 3.0 ? 1 : 2]++;
				}
			}
			keepTime(speed);
			Inventory chest = hasChest ? (Inventory) world.getBlockEntity(chestX, chestY, chestZ) : null;
			for (int i = 0; chest != null && i < chest.size(); i++) {
				ItemStack s = chest.getStack(i);
				if (s == null) continue;
				if (s.getItem() == axe) {
					if (i < 18) axesUsed++;
					continue;
				}
				goods.merge(s.getItem().getTranslationKey() + (s.itemId == Block.SAPLING.id ? "/" + s.getDamage() : ""), s.count, Integer::sum);
				chest.setStack(i, null);
			}
			if (ticks % 100 == 0) System.out.println("[golems] " + g.worked + " done after " + ticks + " game ticks, carrying " + g.carriedCount() + ": " + g.status());
			return g.dead || ticks >= prop("maxTicks", 3000);
		}, () -> finish(test));
	}

	/** The area's own time at the golem's pace: random ticks for leaves and saplings, and age for what lies about. */
	private void keepTime(int speed) {
		double chance = Math.min(1.0, RANDOM_TICK * speed);
		int r = half + 6;
		for (int i = -r; i <= r; i++) {
			for (int j = -r; j <= r; j++) {
				int x = siteX + i, z = siteZ + j;
				int top = Math.min(world.getTopY(x, z) + 1, 127);
				for (int y = top; y > siteY - 8 && y > 1; y--) {
					int id = world.getBlockId(x, y, z);
					if (id == Block.LEAVES.id && (world.getBlockMeta(x, y, z) & 8) != 0 || id == Block.SAPLING.id) {
						if (random.nextDouble() < chance) Block.BLOCKS[id].onTick(world, x, y, z, world.random);
					}
				}
			}
		}
		for (Object o : world.collectEntitiesByClass(ItemEntity.class, Box.createCached(siteX - r, 0, siteZ - r, siteX + r + 1, 128, siteZ + r + 1))) ((ItemEntity) o).itemAge += speed - 1;
	}

	/** The first cell above the ground, under any tree. */
	private int ground(int x, int z) {
		int y = Math.min(world.getTopY(x, z), 126);
		while (y > 1) {
			int below = world.getBlockId(x, y - 1, z);
			if (below != 0 && below != Block.LEAVES.id && below != Block.LOG.id && Block.BLOCKS[below].material.blocksMovement()) return y;
			y--;
		}
		return y;
	}

	/** Logs round the area, and how many of those hang by nothing to a log on the ground. */
	private int[] logs() {
		int r = half + 9;
		Set<Long> all = new HashSet<>(), held = new HashSet<>();
		ArrayDeque<int[]> open = new ArrayDeque<>();
		for (int i = -r; i <= r; i++) {
			for (int j = -r; j <= r; j++) {
				int x = siteX + i, z = siteZ + j;
				for (int y = 1; y < 127; y++) {
					if (world.getBlockId(x, y, z) != Block.LOG.id) continue;
					all.add(key(x, y, z));
					int below = world.getBlockId(x, y - 1, z);
					if (below != 0 && below != Block.LEAVES.id && below != Block.LOG.id && held.add(key(x, y, z))) open.add(new int[] {x, y, z});
				}
			}
		}
		while (!open.isEmpty()) {
			int[] c = open.poll();
			for (int i = -1; i <= 1; i++) {
				for (int j = -1; j <= 1; j++) {
					for (int k = -1; k <= 1; k++) {
						long n = key(c[0] + i, c[1] + j, c[2] + k);
						if (all.contains(n) && held.add(n)) open.add(new int[] {c[0] + i, c[1] + j, c[2] + k});
					}
				}
			}
		}
		return new int[] {all.size(), all.size() - held.size()};
	}

	private static long key(int x, int y, int z) {
		return ((long) x & 0xFFFFFFL) << 32 | ((long) z & 0xFFFFFFL) << 8 | (y & 0xFF);
	}

	private void finish(GolemsTest test) {
		try {
			map("after");
			StringWriter out = new StringWriter();
			PrintWriter w = new PrintWriter(out);
			GolemEntity g = golem;
			w.println("run " + name() + ": " + g.worked + " logs and plantings in " + golemTicks + " golem ticks (" + golemTicks / 1200 + " minutes at its pace), dead " + g.dead + ", axes taken from the chest " + axesUsed);
			w.println(g.status());
			w.println("site " + siteX + " " + siteY + " " + siteZ + ", area " + g.area() + ", chest " + chestX + " " + chestY + " " + chestZ);
			w.println("goods put away " + goods + ", carrying " + g.carriedCount());
			w.println("logs cut above its eye, standing within 2 of the log's column along the ground / 2 to 3 / further: " + stoodHigh[0] + " / " + stoodHigh[1] + " / " + stoodHigh[2] + "; the rest " + stoodLow[0] + " / " + stoodLow[1] + " / " + stoodLow[2]);
			w.printf("looking for work: %d looks, the slowest %.2f ms%n", looks, slowestLook / 1000.0);
			int felled = 0, planted = 0, ownKind = 0;
			List<String> standing = new ArrayList<>();
			for (int[] t : treesBefore) {
				int id = world.getBlockId(t[0], t[1], t[2]);
				if (id == Block.LOG.id) {
					standing.add(t[0] + " " + t[1] + " " + t[2] + (Tree.at(world, t[0], t[1], t[2]) ? "" : " (cut partway)"));
					continue;
				}
				felled++;
				if (id == Block.SAPLING.id || id == Block.LOG.id) planted++;
				if (id == Block.SAPLING.id && (world.getBlockMeta(t[0], t[1], t[2]) & 3) == t[3]) ownKind++;
			}
			w.println("trees in its area " + treesBefore.size() + ", felled " + felled + ", their stumps planted " + planted + " (" + ownKind + " of their own kind), still standing " + standing.size() + ": " + standing);
			// For each still standing, what's round its foot: logs and saplings by place, to see why.
			for (int[] t : treesBefore) {
				if (world.getBlockId(t[0], t[1], t[2]) != Block.LOG.id) continue;
				StringBuilder round = new StringBuilder("  round " + t[0] + " " + t[1] + " " + t[2] + ":");
				for (int i = -4; i <= 4; i++) {
					for (int j = -4; j <= 4; j++) {
						for (int h = -2; h <= 16; h++) {
							int id = world.getBlockId(t[0] + i, t[1] + h, t[2] + j);
							if (id == Block.LOG.id || id == Block.SAPLING.id) round.append(' ').append(id == Block.LOG.id ? "log " : "sapling ").append(i).append(',').append(h).append(',').append(j);
						}
					}
				}
				w.println(round);
				w.println("  " + reachable(g, t));
			}
			int[] logs = logs();
			w.println("logs round the area " + logsBefore + " -> " + logs[0] + ", hanging by nothing " + hangingBefore + " -> " + logs[1]);
			int lying = 0;
			Map<String, Integer> left = new HashMap<>();
			int r = half + 6;
			for (Object o : world.collectEntitiesByClass(ItemEntity.class, Box.createCached(siteX - r, 0, siteZ - r, siteX + r + 1, 128, siteZ + r + 1))) {
				ItemStack s = ((ItemEntity) o).stack;
				left.merge(s.getItem().getTranslationKey(), s.count, Integer::sum);
				lying += s.count;
			}
			w.println("lying about " + lying + ": " + left);
			Lying.report(w, world, g, 6);
			w.flush();
			System.out.print("[golems] " + out);
			Files.write(new File(dir, name() + ".txt").toPath(), out.toString().getBytes(StandardCharsets.UTF_8));
			test.report("it felled trees", felled > 0);
			test.report("no log was left hanging by nothing", logs[1] <= hangingBefore);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * For a tree left standing, from the golem's last walk search: each tip of it, how many places it could walk to within its reach along the ground, and from how many of those its job
	 * says it reaches the tip. Through reflection, the job and the walk search being the mod's own business.
	 */
	private String reachable(GolemEntity g, int[] foot) {
		try {
			Class<?> treeClass = Tree.class, mapClass = Class.forName("org.studioquire.faithful.golems.WalkMap"), targetClass = Class.forName("org.studioquire.faithful.golems.Target");
			java.lang.reflect.Method of = treeClass.getDeclaredMethod("of", World.class, int.class, int.class, int.class, boolean.class);
			of.setAccessible(true);
			Object tree = of.invoke(null, world, foot[0], foot[1], foot[2], true);
			if (tree == null) return "not a tree to it now";
			java.lang.reflect.Field logsField = treeClass.getDeclaredField("logs");
			logsField.setAccessible(true);
			java.lang.reflect.Method get = mapClass.getDeclaredMethod("get"), node = mapClass.getDeclaredMethod("node", int.class, int.class, int.class);
			get.setAccessible(true);
			node.setAccessible(true);
			Object map = get.invoke(null);
			Object job = g.job();
			java.lang.reflect.Method reaches = Class.forName("org.studioquire.faithful.golems.Job").getDeclaredMethod("reaches", double.class, double.class, double.class, targetClass);
			reaches.setAccessible(true);
			java.lang.reflect.Constructor<?> target = targetClass.getConstructor(int.class, int.class, int.class, int.class);
			StringBuilder out = new StringBuilder("tips (place, depth: places to stand within 4 / from which it reaches):");
			for (Object o : (List<?>) logsField.get(tree)) {
				int[] l = (int[]) o;
				if (l[4] == 0) continue;
				Object t = target.newInstance(l[0], l[1], l[2], 1000);
				int stands = 0, from = 0;
				for (int x = l[0] - 4; x <= l[0] + 4; x++) {
					for (int z = l[2] - 4; z <= l[2] + 4; z++) {
						for (int y = l[1] - 34; y <= l[1] + 3; y++) {
							if ((int) node.invoke(map, x, y, z) < 0) continue;
							stands++;
							if ((boolean) reaches.invoke(job, x + 0.5, y + GolemEntity.EYE, z + 0.5, t)) from++;
						}
					}
				}
				out.append(' ').append(l[0] - foot[0]).append(',').append(l[1] - foot[1]).append(',').append(l[2] - foot[2]).append(" d").append(l[3]).append(": ").append(stands).append('/').append(from);
			}
			return out.toString();
		} catch (ReflectiveOperationException e) {
			return "couldn't look: " + e;
		}
	}

	static void move(PlayerEntity player, double x, double y, double z) {
		if (onServer()) ServerMove.to(player, x, y, z);
		else player.setPosition(x, y + player.standingEyeHeight, z);
	}

	private static int colour(int id, int meta) {
		if (id == Block.GRASS_BLOCK.id) return 0x5c8f3c;
		if (id == Block.DIRT.id) return 0x866043;
		if (id == Block.STONE.id) return 0x8a8a8a;
		if (id == Block.SAND.id) return 0xdbd3a0;
		if (id == Block.GRAVEL.id) return 0x7e7c78;
		if (id == Block.LOG.id) return 0x503818;
		if (id == Block.LEAVES.id) return (meta & 3) == 1 ? 0x2f4f2f : (meta & 3) == 2 ? 0x6a9a40 : 0x2c6a20;
		if (id == Block.SAPLING.id) return 0xe0ff60;
		if (id == Block.CHEST.id) return 0xff00ff;
		if (id == Block.SNOW.id) return 0xf0f0f0;
		if (org.studioquire.faithful.golems.BoundaryPole.is(id)) return 0xffff00;
		if (id != 0 && Block.BLOCKS[id].material == Material.WATER) return 0x3060d0;
		return 0x402040;
	}

	/** From above: what's on top, shaded by height; saplings and stumps bright. */
	private void map(String when) throws IOException {
		int side = 2 * HALF + 1, scale = 6;
		BufferedImage image = new BufferedImage(side * scale, side * scale, BufferedImage.TYPE_INT_RGB);
		for (int i = 0; i < side; i++) {
			for (int j = 0; j < side; j++) {
				int x = siteX - HALF + i, z = siteZ - HALF + j, y = 127;
				while (y > 0) {
					int id = world.getBlockId(x, y, z);
					if (id != 0 && id != Block.GLASS.id && (Block.BLOCKS[id].material.blocksMovement() || Block.BLOCKS[id].material.isFluid() || id == Block.SAPLING.id)) break;
					y--;
				}
				int c = colour(world.getBlockId(x, y, z), world.getBlockMeta(x, y, z));
				float shade = Math.max(0.4F, Math.min(1.5F, 1.0F + (y - siteY) * 0.04F));
				int rgb = Math.min(255, (int) ((c >> 16 & 255) * shade)) << 16 | Math.min(255, (int) ((c >> 8 & 255) * shade)) << 8 | Math.min(255, (int) ((c & 255) * shade));
				// Just outside its area: the poles' lines, or the square round its post.
				int x0 = golem != null ? golem.area().minX - 1 : siteX - half - 1, x1 = golem != null ? golem.area().maxX + 1 : siteX + half + 1;
				int z0 = golem != null ? golem.area().minZ - 1 : siteZ - half - 1, z1 = golem != null ? golem.area().maxZ + 1 : siteZ + half + 1;
				boolean edge = (x == x0 || x == x1) && z >= z0 && z <= z1 || (z == z0 || z == z1) && x >= x0 && x <= x1;
				if (edge) rgb = (rgb >> 1 & 0x7f7f7f) | 0x800000;
				for (int u = 0; u < scale; u++) {
					for (int v = 0; v < scale; v++) image.setRGB(i * scale + u, j * scale + v, rgb);
				}
			}
		}
		ImageIO.write(image, "png", new File(dir, name() + "-" + when + ".png"));
	}
}
