package org.studioquire.faithful.stagehand;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;

/** Where a player can stand. Nothing here moves anyone. Every cell it reads is in the chunks {@link Move#load} loads around the place it's asked about. */
final class Landing {
	/** How far a landing may be from what was asked: with two chunks loaded each way, it can't read past what's loaded. */
	static final int REACH = 16;

	private Landing() {
	}

	/** The nearest safe cell to this one, itself if it'll do; failing that, what stands on top nearby. Null if there's nothing. */
	static int[] near(World world, int x, int y, int z) {
		for (int r = 0; r <= REACH; r++) {
			int[] found = shell(world, x, y, z, r);
			if (found != null) return found;
		}
		return top(world, x, z);
	}

	/** The highest safe cell in this column or those around it: the surface, or under a roof the first open place. Treading water only if there's no ground at all. */
	static int[] top(World world, int x, int z) {
		for (int water = 0; water < 2; water++) {
			for (int r = 0; r <= REACH; r++) {
				for (int dx = -r; dx <= r; dx++) {
					for (int dz = -r; dz <= r; dz++) {
						if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
						int y = highest(world, x + dx, z + dz, water == 1);
						if (y >= 0) return new int[] {x + dx, y, z + dz};
					}
				}
			}
		}
		return null;
	}

	/** Under a roof, the first open place; under the sky, the top of what's there or nothing, as a cave below the sea or a tree is no surface. -1 if there's none in this column. */
	private static int highest(World world, int x, int z, boolean water) {
		boolean roofed = world.dimension.hasCeiling;
		for (int y = 125; y >= 1; y--) {
			if (roofed) {
				if (safe(world, x, y, z, water)) return y;
				continue;
			}
			Material here = world.getMaterial(x, y, z);
			if (here == Material.LEAVES || !(here.isFluid() || here.blocksMovement())) continue;
			if (here.isFluid()) return safe(world, x, y, z, water) ? y : -1;
			return safe(world, x, y + 1, z, false) ? y + 1 : -1;
		}
		return -1;
	}

	/** The cube's skin at this radius, nearest by plain distance, so a landing stays as close as it can. */
	private static int[] shell(World world, int x, int y, int z, int r) {
		int[] best = null;
		int bestDistance = Integer.MAX_VALUE;
		for (int dy = -r; dy <= r; dy++) {
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (Math.max(Math.abs(dy), Math.max(Math.abs(dx), Math.abs(dz))) != r) continue;
					int distance = dx * dx + dy * dy + dz * dz;
					if (distance < bestDistance && safe(world, x + dx, y + dy, z + dz, false)) {
						bestDistance = distance;
						best = new int[] {x + dx, y + dy, z + dz};
					}
				}
			}
		}
		return best;
	}

	/** Whether feet in this cell stand on something, shallow water or not, with room for a head and nothing burning at hand. With {@code water}, treading deep water with the head clear also counts. */
	static boolean safe(World world, int x, int y, int z, boolean water) {
		if (y < 1 || y > 125) return false;
		Material feet = world.getMaterial(x, y, z), head = world.getMaterial(x, y + 1, z);
		if (!clear(feet) || !clear(head) || head.isFluid()) return false;
		Material under = world.getMaterial(x, y - 1, z);
		boolean stands = under.blocksMovement() && under != Material.CACTUS;
		if (!stands && !(water && feet.isFluid())) return false;
		for (int dy = -1; dy <= 2; dy++) {
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					Material near = world.getMaterial(x + dx, y + dy, z + dz);
					if (near == Material.LAVA || near == Material.FIRE) return false;
				}
			}
		}
		return true;
	}

	private static boolean clear(Material material) {
		return !material.blocksMovement() && material != Material.LAVA && material != Material.FIRE && material != Material.NETHER_PORTAL;
	}

	/** What a cell holds, by name. */
	static String blockAt(World world, int x, int y, int z) {
		int id = world.getBlockId(x, y, z);
		Block block = id > 0 && id < Block.BLOCKS.length ? Block.BLOCKS[id] : null;
		return block == null ? "air" : Named.block(block);
	}
}
