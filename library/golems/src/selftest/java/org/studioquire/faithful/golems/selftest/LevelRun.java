package org.studioquire.faithful.golems.selftest;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.imageio.ImageIO;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.studioquire.faithful.golems.GolemEntity;
import org.studioquire.faithful.golems.Golems;
import org.studioquire.faithful.golems.Ground;
import org.studioquire.faithful.harness.test.server.ServerMove;
import org.studioquire.oldfaithful.core.api.Chunks;

/**
 * A pickaxe golem levelling real land, on its own terms: only when a {@code golems/level.properties} is found in a folder above the game's, never in an ordinary self-test. Boundary
 * poles go at the corners of a square round a hilly site, a chest of spare picks by the post, and the golem is ticked {@code speed} times a game tick until it's out of work, so hours
 * pass in minutes. Beside the file: the land before and after from above with two slices through it ({@code <name>-<side>-before.png}, {@code -after.png}), and what it did
 * ({@code .txt}), with checks for what it must never do, where it could: take anything outside its area or under its floor, leave anything floating, let water or lava in.
 * <p>
 * Keys: {@code name}; {@code half}, the area's half width (12); {@code speed} (80); {@code maxTicks} in game ticks (3000: a server-only self-test gives a world test five minutes, which is 6000 ticks at best); {@code dx}, {@code dz} from the server's plot and
 * {@code singleDx}, {@code singleDz} from single player's to start looking for land; {@code pick}, an item id (iron's); {@code noChest}; {@code relief}, how much the land must rise and fall round the site (5; 14 finds hills);
 * {@code atX}, {@code atZ}, a site of its own with no looking: the self-test's world has a fixed seed, so the same land each run, for comparing two builds on the same hill.
 */
public final class LevelRun {
	public static int siteX, siteY, siteZ;
	public static GolemEntity golem;
	/** Half the side of the square looked at and pictured. */
	private static final int HALF = 40;

	private final File dir;
	private final Properties p;
	private World world;
	private int ticks, chestX, chestY, chestZ, half, floor;
	private long golemTicks;
	private long slowestLook;
	private int looks;
	private int[] liquidBefore;
	private BitSet floatingBefore, solidBefore;
	private final BitSet groundBefore = new BitSet();
	private final Map<String, Integer> kinds = new HashMap<>();

	private LevelRun(File dir, Properties p) {
		this.dir = dir;
		this.p = p;
	}

	public static LevelRun asked() {
		for (File f = new File(System.getProperty("user.dir")).getAbsoluteFile(); f != null; f = f.getParentFile()) {
			File file = new File(f, "golems/level.properties");
			if (!file.isFile()) continue;
			Properties p = new Properties();
			try (java.io.Reader r = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
				p.load(r);
			} catch (IOException e) {
				return null;
			}
			return new LevelRun(file.getParentFile(), p);
		}
		return null;
	}

	public String property(String key, String fallback) {
		return p.getProperty(key, fallback).trim();
	}

	private int prop(String key, int fallback) {
		return Integer.parseInt(property(key, String.valueOf(fallback)));
	}

	private static boolean onServer() {
		return FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER;
	}

	private String name() {
		return p.getProperty("name", "run") + (onServer() ? "-server" : "-single");
	}

	/** Moves over and waits for the land: a chunk's lakes and trees come with its neighbours, and what's built or measured before that doesn't last. */
	void start(GolemsTest test, World world, PlayerEntity player, int x, int y, int z) {
		this.world = world;
		half = prop("half", 12);
		siteX = x + prop(onServer() ? "dx" : "singleDx", 60);
		siteZ = z + prop(onServer() ? "dz" : "singleDz", 0);
		boolean exact = p.getProperty("atX") != null;
		if (exact) {
			siteX = prop("atX", 0);
			siteZ = prop("atZ", 0);
		} else {
			searchLand();
		}
		if (onServer()) ServerMove.load(world, siteX, siteZ, HALF + 8);
		// Somewhere to wait: a server kicks a player left in the air.
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

	private void begin(GolemsTest test, PlayerEntity player) {
		if (p.getProperty("atX") == null) findSite();
		siteY = ground(siteX, siteZ);
		floor = siteY - 1;
		// A stand over the site for whoever watches, on glass so it hides nothing.
		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) world.setBlock(siteX + i, siteY + 40, siteZ + j, Block.GLASS.id);
		}
		move(player, siteX + 0.5, siteY + 41.01, siteZ + 0.5);
		// The corners, on the ground there, each a step outside the area.
		int c = half + 1;
		for (int[] d : new int[][] {{-c, -c}, {c, -c}, {-c, c}, {c, c}}) {
			int sx = siteX + d[0], sz = siteZ + d[1];
			world.setBlock(sx, ground(sx, sz), sz, Golems.pole.get().id);
		}
		chestX = siteX - 2;
		chestZ = siteZ - 2;
		chestY = ground(chestX, chestZ);
		System.out.println("[golems] site " + siteX + " " + siteY + " " + siteZ + ", area " + (2 * half + 1) + " across, chest at height " + chestY);
		Item pick = Item.ITEMS[prop("pick", Item.IRON_PICKAXE.id)];
		boolean hasChest = prop("noChest", 0) == 0;
		if (hasChest) world.setBlock(chestX, chestY, chestZ, Block.CHEST.id);
		Inventory chest = hasChest ? (Inventory) world.getBlockEntity(chestX, chestY, chestZ) : null;
		for (int i = 18; i < 27 && hasChest; i++) chest.setStack(i, new ItemStack(pick));
		liquidBefore = countLiquids();
		floatingBefore = floating();
		solidBefore = new BitSet();
		for (int i = 0; i <= 2 * HALF; i++) {
			for (int j = 0; j <= 2 * HALF; j++) {
				for (int h = 1; h < 127; h++) {
					if (solid(i, h, j)) solidBefore.set(index(i, h, j));
					if (Ground.isGround(world.getBlockId(siteX - HALF + i, h, siteZ - HALF + j))) groundBefore.set(index(i, h, j));
				}
			}
		}
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
		player.inventory.main[player.inventory.selectedSlot] = new ItemStack(pick);
		g.interact(player);
		player.inventory.main[player.inventory.selectedSlot] = hand;
		System.out.println("[golems] its area: " + g.area());
		int speed = prop("speed", 80);
		test.levelSteps(prop("maxTicks", 3000) + 100, () -> {
			ticks++;
			// A test server keeps only three chunks round a player, and an entity needs 32 blocks loaded round it to tick at all.
			if (onServer() && ticks % 20 == 0) ServerMove.load(world, (int) g.x, (int) g.z, 40);
			for (int i = 0; i < speed && !g.dead; i++) {
				world.updateEntity(g);
				golemTicks++;
				if (golemTicks % 10 == 0) Lying.hurry(world, g, 6, 10);
				long look = g.lastLookMicros();
				if (look > 0) {
					looks++;
					slowestLook = Math.max(slowestLook, look);
				}
			}
			for (int i = 0; chest != null && i < chest.size(); i++) {
				ItemStack s = chest.getStack(i);
				if (s == null || s.getItem() == pick) continue;
				kinds.merge(s.getItem().getTranslationKey(), s.count, Integer::sum);
				chest.setStack(i, null);
			}
			if (ticks % 100 == 0) System.out.println("[golems] " + g.worked + " blocks after " + ticks + " game ticks, tool " + (g.tool() == null ? "none" : g.tool().getDamage()) + ", carrying " + g.carriedCount() + ", at " + (int) Math.floor(g.x) + " " + g.feetY() + " " + (int) Math.floor(g.z) + ": " + g.status());
			return g.dead || ticks >= prop("maxTicks", 3000) || g.worked > 0 && g.status().startsWith("WAIT") && g.status().contains("out of work");
		}, () -> finish(test));
	}

	/** The first cell above the ground, under any tree. */
	private int ground(int x, int z) {
		int y = world.getTopSolidBlockY(x, z);
		while (y > 1) {
			int below = world.getBlockId(x, y - 1, z);
			if (below == Block.LOG.id) return y - 1;
			if (below != 0 && below != Block.LEAVES.id && Block.BLOCKS[below].material.blocksMovement()) return y;
			y--;
		}
		return y;
	}

	/** A single player's spawn moves between runs, into the sea as like as not: this walks out in rings for dry land with hills in it. */
	private void searchLand() {
		int startX = siteX, startZ = siteZ;
		for (int ring = 0; ring <= 12; ring++) {
			for (int i = -ring; i <= ring; i++) {
				for (int j = -ring; j <= ring; j++) {
					if (Math.max(Math.abs(i), Math.abs(j)) != ring) continue;
					int cx = startX + i * 48, cz = startZ + j * 48;
					if (!dryLand(cx, cz)) continue;
					siteX = cx;
					siteZ = cz;
					return;
				}
			}
		}
	}

	private boolean dryLand(int cx, int cz) {
		if (onServer()) ServerMove.load(world, cx, cz, 32);
		int low = 127, high = 0, grass = 0, samples = 0;
		for (int a = -24; a <= 24; a += 6) {
			for (int b = -24; b <= 24; b += 6) {
				int y = world.getTopSolidBlockY(cx + a, cz + b);
				if (y < 1) return false;
				int id = world.getBlockId(cx + a, y - 1, cz + b);
				if (id == Block.ICE.id || world.getBlockId(cx + a, y, cz + b) == Block.SNOW.id || (id != 0 && Block.BLOCKS[id].material.isFluid())) return false;
				samples++;
				if (id == Block.GRASS_BLOCK.id) grass++;
				if (id != Block.LEAVES.id && id != Block.LOG.id && id != Block.CACTUS.id) {
					low = Math.min(low, y);
					high = Math.max(high, y);
				}
			}
		}
		return low >= 64 && high - low >= prop("relief", 5) && high - low <= Math.max(20, prop("relief", 5) + 8) && grass * 10 >= samples * 6;
	}

	/** A low spot with rising land round it, near where it was asked for: where somebody would want a yard levelled. */
	private void findSite() {
		int bestX = siteX, bestZ = siteZ;
		float best = -Float.MAX_VALUE;
		for (int i = -24; i <= 24; i += 3) {
			for (int j = -24; j <= 24; j += 3) {
				int y = world.getTopSolidBlockY(siteX + i, siteZ + j);
				if (y < 1 || world.getBlockId(siteX + i, y - 1, siteZ + j) != Block.GRASS_BLOCK.id) continue;
				// Land above it round about, and flat under its own feet.
				float score = -(i * i + j * j) * 0.01F;
				for (int a = -half; a <= half; a += 3) {
					for (int b = -half; b <= half; b += 3) score += Math.max(0, Math.min(8, world.getTopSolidBlockY(siteX + i + a, siteZ + j + b) - y));
				}
				for (int a = -1; a <= 1; a++) {
					for (int b = -1; b <= 1; b++) score -= 20 * Math.abs(world.getTopSolidBlockY(siteX + i + a, siteZ + j + b) - y);
				}
				if (score > best) {
					best = score;
					bestX = siteX + i;
					bestZ = siteZ + j;
				}
			}
		}
		siteX = bestX;
		siteZ = bestZ;
	}

	private boolean insideArea(int i, int j) {
		return Math.abs(i - HALF) <= half && Math.abs(j - HALF) <= half;
	}

	private void finish(GolemsTest test) {
		try {
			map("after");
			GolemEntity g = golem;
			StringWriter out = new StringWriter();
			PrintWriter w = new PrintWriter(out);
			w.println("run " + name() + ": mined " + g.worked + " in " + golemTicks + " golem ticks (" + golemTicks / 1200 + " minutes at its pace), golem at " + g.x + " " + g.y + " " + g.z + ", dead " + g.dead);
			w.println(g.status());
			w.println("site " + siteX + " " + siteY + " " + siteZ + ", floor " + floor + ", area " + g.area() + ", chest " + chestX + " " + chestY + " " + chestZ);
			w.println("goods " + kinds);
			w.printf("looking for work: %d looks, the slowest %.2f ms%n", looks, slowestLook / 1000.0);
			w.printf("blocks taken from further than %.0f, with nowhere nearer to stand: %d, the furthest from %.2f%n", GolemEntity.CLOSE, g.stretched, g.furthest);
			int outside = 0, under = 0, flooded = 0, left = 0, higher = 0;
			int side = 2 * HALF + 1;
			for (int k = solidBefore.nextSetBit(0); k >= 0; k = solidBefore.nextSetBit(k + 1)) {
				int h = k % 128, c = k / 128, i = c / side, j = c % side;
				// Only where a golem could touch: from a little under its floor up, and no further out than it can stand and reach. Deeper down, sand drops into caves and lava
				// runs whatever it does.
				if (h < floor - 3 || Math.abs(i - HALF) > half + 6 || Math.abs(j - HALF) > half + 6) continue;
				int id = world.getBlockId(siteX - HALF + i, h, siteZ - HALF + j);
				boolean gone = !solid(i, h, j);
				if (gone && id != 0 && Block.BLOCKS[id].material.isFluid() && flooded++ < 8) w.println("  flooded " + (siteX - HALF + i) + " " + h + " " + (siteZ - HALF + j));
				// Leaves decay, and plants and snow come away with the ground under them: only the land itself counts.
				if (!gone || !groundBefore.get(k)) continue;
				if (!insideArea(i, j) && outside++ < 8) w.println("  taken outside its area " + (siteX - HALF + i) + " " + h + " " + (siteZ - HALF + j));
				if (insideArea(i, j) && h <= floor && under++ < 8) w.println("  taken from under its floor " + (siteX - HALF + i) + " " + h + " " + (siteZ - HALF + j));
			}
			for (int i = 0; i < side; i++) {
				for (int j = 0; j < side; j++) {
					if (!insideArea(i, j)) continue;
					for (int h = floor + 1; h < 127; h++) {
						if (!Ground.isGround(world.getBlockId(siteX - HALF + i, h, siteZ - HALF + j))) continue;
						left++;
						if (h > floor + 1) higher++;
					}
				}
			}
			w.println("ground left above its floor in its area: " + left + " cells, " + higher + " of them above the first layer");
			int gx = (int) Math.floor(g.x), gz = (int) Math.floor(g.z);
			for (int j = -half - 1; j <= half + 1; j++) {
				StringBuilder row = new StringBuilder("  ");
				for (int i = -half - 1; i <= half + 1; i++) {
					int t = world.getTopSolidBlockY(siteX + i, siteZ + j) - 1 - floor;
					row.append(String.format("%3s", (siteX + i == gx && siteZ + j == gz ? "*" : "") + t));
				}
				w.println(row);
			}
			int[] liquids = countLiquids();
			w.println("water cells " + liquidBefore[0] + " -> " + liquids[0] + ", lava cells " + liquidBefore[1] + " -> " + liquids[1] + ", ground that became liquid " + flooded);
			BitSet after = floating();
			after.andNot(floatingBefore);
			// Where a golem could have done it, as above: a cave's sand far below hangs or falls by itself.
			for (int k = after.nextSetBit(0); k >= 0; k = after.nextSetBit(k + 1)) {
				int h = k % 128, c = k / 128, i = c / side, j = c % side;
				if (h < floor - 3 || Math.abs(i - HALF) > half + 6 || Math.abs(j - HALF) > half + 6) after.clear(k);
			}
			w.println("new floating cells: " + after.cardinality() + ", ground taken outside its area: " + outside + ", from under its floor: " + under);
			int listed = 0;
			for (int k = after.nextSetBit(0); k >= 0 && listed < 8; k = after.nextSetBit(k + 1), listed++) {
				int h = k % 128, c = k / 128;
				w.println("  floating " + (siteX - HALF + c / side) + " " + h + " " + (siteZ - HALF + c % side));
			}
			Lying.report(w, world, g, 6);
			w.flush();
			System.out.print("[golems] " + out);
			Files.write(new File(dir, name() + ".txt").toPath(), out.toString().getBytes(StandardCharsets.UTF_8));
			test.report("it levelled something", g.worked > 0);
			test.report("nothing was taken outside its area", outside == 0);
			test.report("nothing was taken from under its floor", under == 0);
			test.report("no floating cells were left", after.isEmpty());
			test.report("no water or lava came into what it dug", flooded == 0);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	static void moveTo(World world, PlayerEntity player, double x, double y, double z) {
		world.setTime(6000L);
		move(player, x, y, z);
	}

	/** Beside it, on the ground: a server only tells a client about what's near. */
	static void toGolem(World world, PlayerEntity player) {
		int x = (int) Math.floor(golem.x) + 3, z = (int) Math.floor(golem.z) + 2;
		move(player, x + 0.5, world.getTopSolidBlockY(x, z) + 0.01, z + 0.5);
	}

	private static void move(PlayerEntity player, double x, double y, double z) {
		if (onServer()) ServerMove.to(player, x, y, z);
		else player.setPosition(x, y + player.standingEyeHeight, z);
	}

	private int[] countLiquids() {
		int[] n = new int[2];
		for (int x = siteX - HALF; x <= siteX + HALF; x++) {
			for (int z = siteZ - HALF; z <= siteZ + HALF; z++) {
				for (int y = 1; y < 127; y++) {
					int id = world.getBlockId(x, y, z);
					if (id == 0) continue;
					Material m = Block.BLOCKS[id].material;
					if (m == Material.WATER) n[0]++;
					if (m == Material.LAVA) n[1]++;
				}
			}
		}
		return n;
	}

	/** Solid cells joined to nothing that reaches the edge of the box or its floor. */
	private BitSet floating() {
		int side = 2 * HALF + 1, height = 128;
		BitSet anchored = new BitSet(side * side * height);
		ArrayDeque<int[]> open = new ArrayDeque<>();
		int[][] dirs = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
		for (int i = 0; i < side; i++) {
			for (int j = 0; j < side; j++) {
				for (int y = 0; y < height; y++) {
					boolean edge = i == 0 || j == 0 || i == side - 1 || j == side - 1 || y == 0;
					if (!edge || !solid(i, y, j)) continue;
					anchored.set(index(i, y, j));
					open.add(new int[] {i, y, j});
				}
			}
		}
		while (!open.isEmpty()) {
			int[] c = open.poll();
			for (int[] d : dirs) {
				int i = c[0] + d[0], y = c[1] + d[1], j = c[2] + d[2];
				if (i < 0 || j < 0 || y < 0 || i >= side || j >= side || y >= height) continue;
				int k = index(i, y, j);
				if (anchored.get(k) || !solid(i, y, j)) continue;
				anchored.set(k);
				open.add(new int[] {i, y, j});
			}
		}
		BitSet loose = new BitSet();
		for (int i = 0; i < side; i++) {
			for (int j = 0; j < side; j++) {
				for (int y = 0; y < height; y++) {
					if (!solid(i, y, j) || anchored.get(index(i, y, j))) continue;
					// Leaves decay away from their trees, and water freezes, whatever the golem does.
					int id = world.getBlockId(siteX - HALF + i, y, siteZ - HALF + j);
					if (id != Block.LEAVES.id && id != Block.ICE.id) loose.set(index(i, y, j));
				}
			}
		}
		return loose;
	}

	private int index(int i, int y, int j) {
		return (i * (2 * HALF + 1) + j) * 128 + y;
	}

	private boolean solid(int i, int y, int j) {
		int id = world.getBlockId(siteX - HALF + i, y, siteZ - HALF + j);
		return id != 0 && id != Block.GLASS.id && Block.BLOCKS[id].material.blocksMovement();
	}

	private static int colour(int id) {
		if (id == Block.GRASS_BLOCK.id) return 0x5c8f3c;
		if (id == Block.DIRT.id) return 0x866043;
		if (id == Block.STONE.id) return 0x8a8a8a;
		if (id == Block.SAND.id) return 0xdbd3a0;
		if (id == Block.SANDSTONE.id) return 0xc9bf90;
		if (id == Block.GRAVEL.id) return 0x7e7c78;
		if (id == Block.CLAY.id) return 0x9ea4b0;
		if (id == Block.COAL_ORE.id) return 0x303030;
		if (id == Block.IRON_ORE.id) return 0xc8a080;
		if (id == Block.GOLD_ORE.id) return 0xf0d040;
		if (id == Block.DIAMOND_ORE.id) return 0x50e0e0;
		if (id == Block.REDSTONE_ORE.id || id == Block.LIT_REDSTONE_ORE.id) return 0xd02020;
		if (id == Block.LAPIS_ORE.id) return 0x2040c0;
		if (id == Block.LOG.id) return 0x503818;
		if (id == Block.LEAVES.id) return 0x2c6a20;
		if (id == Block.CHEST.id) return 0xff00ff;
		if (id == Block.GLASS.id) return 0xffffff;
		if (org.studioquire.faithful.golems.BoundaryPole.is(id)) return 0xffff00;
		if (id != 0 && Block.BLOCKS[id].material == Material.WATER) return 0x3060d0;
		if (id != 0 && Block.BLOCKS[id].material == Material.LAVA) return 0xff6000;
		return 0x402040;
	}

	/** The land from above: colour by what's on top, shade by the slope to the north-west, darker with depth; and two slices through the site. */
	private void map(String when) throws IOException {
		int side = 2 * HALF + 1, scale = 6, slice = 64;
		BufferedImage image = new BufferedImage(side * scale, side * scale + 2 * slice * scale / 2, BufferedImage.TYPE_INT_RGB);
		int[][] top = new int[side][side];
		for (int i = 0; i < side; i++) {
			for (int j = 0; j < side; j++) {
				int xw = siteX - HALF + i, zw = siteZ - HALF + j;
				int y = 127;
				while (y > 0) {
					int id = world.getBlockId(xw, y, zw);
					if (id != 0 && id != Block.GLASS.id && (Block.BLOCKS[id].material.blocksMovement() || Block.BLOCKS[id].material.isFluid())) break;
					y--;
				}
				top[i][j] = y;
			}
		}
		for (int i = 0; i < side; i++) {
			for (int j = 0; j < side; j++) {
				int c = colour(world.getBlockId(siteX - HALF + i, top[i][j], siteZ - HALF + j));
				int west = i > 0 ? top[i - 1][j] : top[i][j], north = j > 0 ? top[i][j - 1] : top[i][j];
				float shade = 1.0F + 0.14F * (top[i][j] - west) + 0.14F * (top[i][j] - north);
				shade *= Math.max(0.35F, 1.0F - (siteY - top[i][j]) * 0.022F);
				shade = Math.max(0.2F, Math.min(1.6F, shade));
				int rgb = Math.min(255, (int) ((c >> 16 & 255) * shade)) << 16 | Math.min(255, (int) ((c >> 8 & 255) * shade)) << 8 | Math.min(255, (int) ((c & 255) * shade));
				// The area's edge, a line just outside it.
				boolean edge = (Math.abs(i - HALF) == half + 1 && Math.abs(j - HALF) <= half + 1) || (Math.abs(j - HALF) == half + 1 && Math.abs(i - HALF) <= half + 1);
				if (edge) rgb = (rgb >> 1 & 0x7f7f7f) | 0x800000;
				for (int u = 0; u < scale; u++) {
					for (int v = 0; v < scale; v++) image.setRGB(i * scale + u, j * scale + v, rgb);
				}
			}
		}
		int base = side * scale;
		for (int k = 0; k < 2; k++) {
			for (int i = 0; i < side; i++) {
				for (int h = 0; h < slice; h++) {
					int y = siteY + 24 - h;
					int id = y < 0 ? 0 : k == 0 ? world.getBlockId(siteX - HALF + i, y, siteZ) : world.getBlockId(siteX, y, siteZ - HALF + i);
					int c = id == 0 ? (y == floor ? 0xc0a0a0 : 0xa0c0f0) : colour(id);
					for (int u = 0; u < scale; u++) {
						for (int v = 0; v < scale / 2; v++) image.setRGB(i * scale + u, base + k * slice * scale / 2 + h * (scale / 2) + v, c);
					}
				}
			}
		}
		ImageIO.write(image, "png", new File(dir, name() + "-" + when + ".png"));
	}
}
