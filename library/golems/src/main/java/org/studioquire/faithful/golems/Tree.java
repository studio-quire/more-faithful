package org.studioquire.faithful.golems;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.LogBlock;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;

/**
 * A tree, as an axe golem tells one from something built of logs. Every tree beta grows (oak, birch, spruce, pine and the big oak) stands on one log on dirt or grass, its trunk at least four
 * logs straight up from there, with nothing joined to it but more of its logs (the big oak's branches join at edges and corners), its leaves, and what grows wild round it. So a tree here is:
 * <ul>
 * <li>logs joined at faces, edges or corners, the lowest of them alone at its height, on dirt or grass, and none of the others resting on anything but a log, leaves or air;</li>
 * <li>within {@link #SPREAD} of that foot each way and {@link #TALL} above it, no more than {@link #MOST} logs;</li>
 * <li>touching nothing else: no plank, cobble, torch, fence, glass, sign or anything else a player puts down, so a log in a wall, a post by a door or a tree someone hung a torch on is left;</li>
 * <li>and, to be started, a trunk four logs high and leaves on it that grew there (Still Faithful marks those a player places), so a stump or a lone post, or a log hidden in a hedge, isn't one.</li>
 * </ul>
 * A tree it has started is felled to the ground without asking about trunk or leaves again, since cutting it from the top takes them away.
 * <p>
 * Its logs are ordered from the foot: each log's depth is its steps from the foot through the logs, and a tip is one no other log hangs from on the way down. A tip can always go without
 * leaving anything floating.
 */
public final class Tree {
	public static final int SPREAD = 8, TALL = 32, MOST = 256;
	private static final int[][] FACES = {{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}, {0, 1, 0}, {0, -1, 0}};

	/** Its foot: the lowest log, on the ground. */
	public final int x, y, z;
	/** Each log: x, y, z, its depth from the foot, and whether it's a tip. The foot is first. */
	final List<int[]> logs;
	/** What it grows from, by its foot's log: beta's sapling of that kind. */
	final int saplingId, saplingMeta;

	private Tree(int x, int y, int z, List<int[]> logs, int saplingId, int saplingMeta) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.logs = logs;
		this.saplingId = saplingId;
		this.saplingMeta = saplingMeta;
	}

	/** Whether a tree stands on this foot, as an axe golem would start on one. */
	public static boolean at(World world, int x, int y, int z) {
		return of(world, x, y, z, false) != null;
	}

	/** Its logs now, with this one's foot as given, or null if what stands there isn't a tree; {@code started} for one it's felling. */
	static Tree of(World world, int bx, int by, int bz, boolean started) {
		if (!world.isPosLoaded(bx, 64, bz) || !isLog(world.getBlockId(bx, by, bz)) || !rootsIn(world.getBlockId(bx, by - 1, bz))) return null;
		List<int[]> logs = new ArrayList<>();
		Map<Long, Integer> index = new HashMap<>();
		logs.add(new int[] {bx, by, bz, 0, 1});
		index.put(key(bx, by, bz), 0);
		// Who each was reached from: one that nothing was reached from is a tip.
		List<Integer> from = new ArrayList<>();
		from.add(-1);
		for (int i = 0; i < logs.size(); i++) {
			int[] l = logs.get(i);
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					for (int dz = -1; dz <= 1; dz++) {
						int nx = l[0] + dx, ny = l[1] + dy, nz = l[2] + dz;
						if (ny < 0 || ny > 127 || !world.isPosLoaded(nx, 64, nz) || !isLog(world.getBlockId(nx, ny, nz))) continue;
						long k = key(nx, ny, nz);
						if (index.containsKey(k)) continue;
						// Another log as low as its foot, or past how far a tree spreads: part of something else.
						if (ny <= by || Math.abs(nx - bx) > SPREAD || Math.abs(nz - bz) > SPREAD || ny - by > TALL || logs.size() >= MOST) return null;
						index.put(k, logs.size());
						logs.add(new int[] {nx, ny, nz, l[3] + 1, 1});
						from.add(i);
					}
				}
			}
		}
		int leaves = 0, leafId = 0, leafMeta = 0;
		for (int i = 0; i < logs.size(); i++) {
			int[] l = logs.get(i);
			if (from.get(i) >= 0) logs.get(from.get(i))[4] = 0;
			for (int[] f : FACES) {
				int nx = l[0] + f[0], ny = l[1] + f[1], nz = l[2] + f[2];
				// Land not loaded can't be seen to be wild, and in single player reading it would load it.
				if (!world.isPosLoaded(nx, 64, nz)) return null;
				int id = world.getBlockId(nx, ny, nz);
				if (isLog(id)) continue;
				if (isLeaves(id)) {
					int meta = world.getBlockMeta(nx, ny, nz);
					if (!grown(meta)) continue;
					leaves++;
					if (leafId == 0) {
						leafId = id;
						leafMeta = meta;
					}
					continue;
				}
				// Only its foot stands on the ground.
				if (i > 0 && f[1] < 0 && id != 0 && Block.BLOCKS[id] != null && Block.BLOCKS[id].material.blocksMovement()) return null;
				if (!wild(id)) return null;
			}
		}
		if (!started) {
			for (int h = 1; h <= 3; h++) {
				if (!index.containsKey(key(bx, by + h, bz))) return null;
			}
			if (leaves < 4) return null;
		}
		// Beta's log says its kind, as its sapling does; a mod's tree goes by what its leaves drop.
		int foot = world.getBlockId(bx, by, bz), saplingId = Block.SAPLING.id, saplingMeta = world.getBlockMeta(bx, by, bz) & 3;
		if (foot != Block.LOG.id && leafId != 0) {
			int dropped = Block.BLOCKS[leafId].getDroppedItemId(leafMeta, world.random);
			if (dropped > 0 && dropped < Block.BLOCKS.length && Block.BLOCKS[dropped] != null) {
				saplingId = dropped;
				saplingMeta = leafMeta & 3;
			}
		}
		return new Tree(bx, by, bz, logs, saplingId, saplingMeta);
	}

	/** Only a foot, to be looked at again with {@link #of}. */
	static Tree foot(int x, int y, int z) {
		return new Tree(x, y, z, new ArrayList<>(), Block.SAPLING.id, 0);
	}

	/** Its deepest tip: the farthest log from the foot, the highest of those. */
	int[] farthest() {
		int[] best = null;
		for (int[] l : logs) {
			if (l[4] == 0) continue;
			if (best == null || l[3] > best[3] || l[3] == best[3] && l[1] > best[1]) best = l;
		}
		return best;
	}

	boolean isFoot(int x, int y, int z) {
		return x == this.x && y == this.y && z == this.z;
	}

	boolean sameFoot(Tree other) {
		return other != null && isFoot(other.x, other.y, other.z);
	}

	static boolean isLog(int id) {
		return id > 0 && Block.BLOCKS[id] instanceof LogBlock;
	}

	static boolean isLeaves(int id) {
		return id > 0 && Block.BLOCKS[id] instanceof LeavesBlock;
	}

	/** Leaves that grew: Still Faithful marks those a player places with bit 4, which beta leaves unused. */
	static boolean grown(int meta) {
		return (meta & 4) == 0;
	}

	/** What a tree grows from in beta. */
	static boolean rootsIn(int id) {
		return id == Block.GRASS_BLOCK.id || id == Block.DIRT.id;
	}

	/** What can be beside a tree without anybody having built it in: the land, what grows on it, snow, ice and water; and a boundary pole, which marks land rather than building on it. */
	static boolean wild(int id) {
		if (id == 0 || Ground.isGround(id) || Ground.isCover(id) || BoundaryPole.is(id)) return true;
		Block block = Block.BLOCKS[id];
		if (block == null) return true;
		Material m = block.material;
		return m == Material.PLANT || m == Material.SNOW_LAYER || m == Material.SNOW_BLOCK || m == Material.ICE || m == Material.PUMPKIN || m.isFluid();
	}

	static long key(int x, int y, int z) {
		return ((long) (x + 0x2000000) << 34) | ((long) (z + 0x2000000) << 8) | (y & 0xFF);
	}

	@Override
	public String toString() {
		return "tree at " + x + " " + y + " " + z + ", " + logs.size() + " logs";
	}
}
