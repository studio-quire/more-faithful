package org.studioquire.faithful.golems.selftest;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.block.Block;
import net.minecraft.world.World;

/** For the axe's part of {@link GolemsTest}: a tree with branches, leaves, counting what's standing and what hangs, and beta's leaf decay sooner than its random ticks would come. */
final class Felling {
	/** How far round a tree's foot its logs are counted. */
	private static final int AROUND = 3, UP = 14;
	private static final Pattern STUMPS = Pattern.compile("(\\d+) stumps to plant"), WORKING = Pattern.compile("^WORKING working (-?\\d+) (-?\\d+) (-?\\d+) rank");

	private Felling() {
	}

	/** A trunk of six with three branches: one rising across a corner, one straight out along the level, one off a corner at the top; leaves round the top and each branch's end. */
	static void branchy(World world, int x, int y, int z) {
		for (int h = 0; h <= 5; h++) world.setBlock(x, y + h, z, Block.LOG.id);
		world.setBlock(x + 1, y + 3, z, Block.LOG.id);
		world.setBlock(x + 2, y + 4, z, Block.LOG.id);
		world.setBlock(x, y + 4, z - 1, Block.LOG.id);
		world.setBlock(x, y + 4, z - 2, Block.LOG.id);
		world.setBlock(x - 1, y + 5, z + 1, Block.LOG.id);
		leavesAround(world, x, y + 6, z);
		leavesAround(world, x + 2, y + 5, z);
		leavesAround(world, x, y + 5, z - 2);
		leavesAround(world, x - 1, y + 6, z + 1);
	}

	/** Leaves, as a tree grows them, in the air of the cells round this one and in it. */
	static void leavesAround(World world, int x, int y, int z) {
		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				for (int k = -1; k <= 1; k++) {
					if (world.getBlockId(x + i, y + j, z + k) == 0) world.setBlock(x + i, y + j, z + k, Block.LEAVES.id, 0);
				}
			}
		}
	}

	/** Round a tree's foot: how many logs there are, and how many of them hang by nothing to it through other logs. */
	static int[] standing(World world, int x, int y, int z) {
		int all = 0;
		for (int i = -AROUND; i <= AROUND; i++) {
			for (int k = -AROUND; k <= AROUND; k++) {
				for (int h = 0; h <= UP; h++) if (world.getBlockId(x + i, y + h, z + k) == Block.LOG.id) all++;
			}
		}
		Set<Long> joined = new HashSet<>();
		ArrayDeque<int[]> open = new ArrayDeque<>();
		if (world.getBlockId(x, y, z) == Block.LOG.id) {
			joined.add(key(x, y, z));
			open.add(new int[] {x, y, z});
		}
		while (!open.isEmpty()) {
			int[] c = open.poll();
			for (int i = -1; i <= 1; i++) {
				for (int j = -1; j <= 1; j++) {
					for (int k = -1; k <= 1; k++) {
						int nx = c[0] + i, ny = c[1] + j, nz = c[2] + k;
						if (Math.abs(nx - x) > AROUND || Math.abs(nz - z) > AROUND || ny < y || ny > y + UP) continue;
						if (world.getBlockId(nx, ny, nz) != Block.LOG.id || !joined.add(key(nx, ny, nz))) continue;
						open.add(new int[] {nx, ny, nz});
					}
				}
			}
		}
		return new int[] {all, all - joined.size()};
	}

	/** Beta's own tick for every leaf on the plot marked to look for its log: what its random ticks would do over a minute or so. */
	static void decay(World world, int x, int y, int z) {
		for (int i = -8; i <= 8; i++) {
			for (int k = -8; k <= 8; k++) {
				for (int h = 0; h <= 16; h++) {
					if (world.getBlockId(x + i, y + h, z + k) == Block.LEAVES.id && (world.getBlockMeta(x + i, y + h, z + k) & 8) != 0) {
						Block.LEAVES.onTick(world, x + i, y + h, z + k, world.random);
					}
				}
			}
		}
	}

	/** The block a golem's status says it's working on, or null. */
	static int[] workingOn(String status) {
		Matcher m = WORKING.matcher(status);
		return m.find() ? new int[] {Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3))} : null;
	}

	/** Along the ground, from a point to the nearest part of a block's column. */
	static double along(double fromX, double fromZ, int x, int z) {
		double dx = Math.max(0.0, Math.abs(fromX - (x + 0.5)) - 0.5), dz = Math.max(0.0, Math.abs(fromZ - (z + 0.5)) - 0.5);
		return Math.sqrt(dx * dx + dz * dz);
	}

	/** "N stumps to plant" out of a golem's status, or null. */
	static String stumps(String status) {
		Matcher m = STUMPS.matcher(status);
		return m.find() ? m.group() : null;
	}

	private static long key(int x, int y, int z) {
		return ((long) x & 0xFFFFFFL) << 32 | ((long) z & 0xFFFFFFL) << 8 | (y & 0xFF);
	}
}
