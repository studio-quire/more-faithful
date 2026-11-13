package org.studioquire.faithful.golems;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNode;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/**
 * Where a golem can safely walk, from where it stands, over a rectangle of the land. A node is a cell it can stand in: a floor under it as wide as the cell (not a fence, a post or the
 * leaves of a tree), room for its head, nothing liquid, no lava beside it. Nodes join the four beside them a step up, level, or one or two down: a drop of two doesn't hurt, three
 * can. Searched breadth first, so every node knows its steps from the start and the way back; beta's own {@code Path} is what's handed to its walking.
 * <p>
 * One search at a time, on the one thread the world runs on, so the tables are shared by every golem and made on first use. A search holds until the next.
 */
final class WalkMap {
	static final int MAX_NODES = 40000;
	private static final int[] DX = {1, -1, 0, 0}, DZ = {0, 0, 1, -1};
	private static final int VALID = 1, INVALID = 2;

	private static WalkMap shared;

	private final IntMap cells = new IntMap(1 << 18), nodes = new IntMap(1 << 17);
	private final int[] key = new int[MAX_NODES], from = new int[MAX_NODES], steps = new int[MAX_NODES];
	private int count;
	private World world;
	private int minX, minZ, maxX, maxZ;

	private WalkMap() {
	}

	static WalkMap get() {
		if (shared == null) shared = new WalkMap();
		return shared;
	}

	/** Everywhere it can walk to from this cell, inside the rectangle given (at most 1024 across). The start counts as a node wherever it is: in water, mid-jump. */
	void search(World world, int x, int y, int z, int minX, int minZ, int maxX, int maxZ) {
		this.world = world;
		this.minX = minX;
		this.minZ = minZ;
		this.maxX = Math.min(maxX, minX + 1023);
		this.maxZ = Math.min(maxZ, minZ + 1023);
		cells.clear();
		nodes.clear();
		count = 0;
		if (!inside(x, z) || y < 0 || y > 127) return;
		add(key(x, y, z), -1, 0);
		for (int head = 0; head < count && count < MAX_NODES; head++) {
			int k = key[head];
			int ax = keyX(k), ay = keyY(k), az = keyZ(k);
			for (int d = 0; d < 4; d++) {
				int nx = ax + DX[d], nz = az + DZ[d];
				// Only loaded land: in single player, reading a chunk that isn't would load or make it there and then.
				if (!inside(nx, nz) || !world.isPosLoaded(nx, 64, nz)) continue;
				int ny = stepTo(ax, ay, az, nx, nz);
				if (ny < 0) continue;
				int nk = key(nx, ny, nz);
				if (nodes.get(nk) >= 0) continue;
				add(nk, head, steps[head] + 1);
				if (count >= MAX_NODES) break;
			}
		}
	}

	/** Where a step from a node to the column beside it lands, or -1: up one, level, down one or down two, whichever stands there, with room to get there. */
	private int stepTo(int x, int y, int z, int nx, int nz) {
		if (standable(nx, y + 1, nz)) return clear(x, y + 2, z) ? y + 1 : -1;
		if (standable(nx, y, nz)) return y;
		if (!clear(nx, y + 1, nz)) return -1;
		if (standable(nx, y - 1, nz)) return y - 1;
		if (!clear(nx, y, nz)) return -1;
		return standable(nx, y - 2, nz) ? y - 2 : -1;
	}

	private void add(int k, int parent, int n) {
		key[count] = k;
		from[count] = parent;
		steps[count] = n;
		nodes.put(k, count);
		count++;
	}

	private boolean inside(int x, int z) {
		return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
	}

	private int key(int x, int y, int z) {
		return (x - minX) << 17 | (z - minZ) << 7 | y;
	}

	private int keyX(int k) {
		return (k >>> 17) + minX;
	}

	private int keyZ(int k) {
		return (k >>> 7 & 1023) + minZ;
	}

	private static int keyY(int k) {
		return k & 127;
	}

	/** The node at this cell, or -1 if the last search didn't reach it. */
	int node(int x, int y, int z) {
		if (!inside(x, z) || y < 0 || y > 127) return -1;
		return nodes.get(key(x, y, z));
	}

	int steps(int node) {
		return steps[node];
	}

	int x(int node) {
		return keyX(key[node]);
	}

	int y(int node) {
		return keyY(key[node]);
	}

	int z(int node) {
		return keyZ(key[node]);
	}

	int count() {
		return count;
	}

	/**
	 * Beta's own walking, from the start of the last search to this node of it. The cell it starts in is left off, or beta's walking would first turn it back to that cell's middle;
	 * a path to where it stands is that cell alone, as beta's can't be empty.
	 */
	Path pathTo(int node) {
		PathNode[] list = new PathNode[Math.max(1, steps[node])];
		for (int k = list.length - 1, i = node; k >= 0 && i >= 0; k--, i = from[i]) list[k] = new PathNode(x(i), y(i), z(i));
		return new Path(list);
	}

	/**
	 * As {@link #pathTo}, with the grid's corners taken out wherever it can walk straight: from each place on the way, on to the furthest one further on, at the same height, that a
	 * body as wide as a golem's can walk to in a straight line over nodes of the last search, and the first leg from where it stands. So a following golem goes across open ground as
	 * a wolf does, rather than along the grid in an L or a staircase of quarter turns (which made its following look odd). A step up or down stays a place of its own.
	 */
	Path straightPathTo(int node, double fromX, double fromZ) {
		int n = Math.max(1, steps[node]) + 1;
		int[] way = new int[n];
		for (int k = n - 1, i = node; k >= 0 && i >= 0; k--, i = from[i]) way[k] = i;
		List<PathNode> kept = new ArrayList<>();
		int at = 0;
		double ax = fromX, az = fromZ;
		while (at < n - 1) {
			int next = at + 1;
			for (int j = n - 1; j > at + 1; j--) {
				if (level(way, at, j) && straight(ax, az, x(way[j]) + 0.5, z(way[j]) + 0.5, y(way[at]))) {
					next = j;
					break;
				}
			}
			kept.add(new PathNode(x(way[next]), y(way[next]), z(way[next])));
			at = next;
			ax = x(way[at]) + 0.5;
			az = z(way[at]) + 0.5;
		}
		return new Path(kept.toArray(new PathNode[0]));
	}

	/** Every place of the way from one to another at the same height. */
	private boolean level(int[] way, int from, int to) {
		for (int k = from + 1; k <= to; k++) if (y(way[k]) != y(way[from])) return false;
		return true;
	}

	/** A straight walk at this height over nothing but nodes, its body's width either side, checked every quarter block. */
	private boolean straight(double x0, double z0, double x1, double z1, int y) {
		double dx = x1 - x0, dz = z1 - z0, half = GolemEntity.WIDTH / 2.0;
		int steps = (int) Math.ceil(Math.sqrt(dx * dx + dz * dz) * 4.0);
		for (int s = 0; s <= steps; s++) {
			double f = steps == 0 ? 1.0 : s / (double) steps, x = x0 + dx * f, z = z0 + dz * f;
			for (int c = 0; c < 4; c++) {
				int cx = MathHelper.floor(x + (c % 2 == 0 ? -half : half)), cz = MathHelper.floor(z + (c < 2 ? -half : half));
				if (node(cx, y, cz) < 0) return false;
			}
		}
		return true;
	}

	// What a cell is, to a golem.

	/** Whether it can stand here, its feet in this cell. */
	boolean standable(int x, int y, int z) {
		if (y < 1 || y > 126) return false;
		int k = key(x, y, z);
		int known = cells.get(k);
		if (known > 0) return known == VALID;
		boolean ok = footing(world, x, y - 1, z) && clear(x, y, z) && clear(x, y + 1, z) && !nearLava(x, y, z);
		cells.put(k, ok ? VALID : INVALID);
		return ok;
	}

	/** A floor to stand on: its top in the upper half of the cell and as wide as the cell. Not a fence, a post, a cactus, or leaves. */
	static boolean footing(World world, int x, int y, int z) {
		int id = world.getBlockId(x, y, z);
		if (id == 0) return false;
		Block block = Block.BLOCKS[id];
		if (block == null || block.material == Material.LEAVES) return false;
		Box box = block.getCollisionShape(world, x, y, z);
		return box != null && box.maxY >= y + 0.5 && box.maxY <= y + 1.0 && box.maxX - box.minX >= 0.9 && box.maxZ - box.minZ >= 0.9;
	}

	/** Room for a body: nothing it would bump into, no liquid, and no portal. */
	private boolean clear(int x, int y, int z) {
		return clear(world, x, y, z);
	}

	static boolean clear(World world, int x, int y, int z) {
		if (y > 127) return true;
		int id = world.getBlockId(x, y, z);
		if (id == 0) return true;
		Block block = Block.BLOCKS[id];
		if (block == null) return true;
		if (block.material.isFluid() || block.material == Material.NETHER_PORTAL) return false;
		return block.getCollisionShape(world, x, y, z) == null;
	}

	/** Lava beside its feet, its head, or the floor under it: a slip, or the lava's spread, and it's in. */
	private boolean nearLava(int x, int y, int z) {
		for (int d = 0; d < 4; d++) {
			for (int dy = -1; dy <= 1; dy++) {
				if (isLava(world.getBlockId(x + DX[d], y + dy, z + DZ[d]))) return true;
			}
		}
		return false;
	}

	static boolean isLava(int id) {
		return id != 0 && Block.BLOCKS[id] != null && Block.BLOCKS[id].material == Material.LAVA;
	}

	/** A map of ints to ints for one search at a time: cleared by moving to a new stamp, not by wiping. Keys are never negative; a missing one reads as -1. */
	static final class IntMap {
		private final int[] keys, values, stamps;
		private final int mask, limit;
		private int stamp = 1, size;

		IntMap(int capacity) {
			keys = new int[capacity];
			values = new int[capacity];
			stamps = new int[capacity];
			mask = capacity - 1;
			limit = capacity * 3 / 4;
		}

		void clear() {
			size = 0;
			if (++stamp == 0) {
				java.util.Arrays.fill(stamps, 0);
				stamp = 1;
			}
		}

		int get(int key) {
			for (int i = mix(key) & mask; stamps[i] == stamp; i = (i + 1) & mask) {
				if (keys[i] == key) return values[i];
			}
			return -1;
		}

		/** Past three quarters full it takes nothing more; a later get just misses. */
		void put(int key, int value) {
			int i = mix(key) & mask;
			for (; stamps[i] == stamp; i = (i + 1) & mask) {
				if (keys[i] == key) {
					values[i] = value;
					return;
				}
			}
			if (size >= limit) return;
			stamps[i] = stamp;
			keys[i] = key;
			values[i] = value;
			size++;
		}

		private static int mix(int key) {
			int h = key * 0x9E3779B9;
			return h ^ h >>> 15;
		}
	}
}
