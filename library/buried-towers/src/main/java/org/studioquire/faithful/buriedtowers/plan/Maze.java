package org.studioquire.faithful.buriedtowers.plan;

import static org.studioquire.faithful.buriedtowers.plan.Blocks.*;

import java.util.ArrayList;
import java.util.List;

/**
 * A floor of narrow passages, one wide, cut in the rock or walled in it, turning and branching: a few turns in a small simple room, a real maze in a complex one. A chest left at one dead end and a
 * spawner at another. The ways down stand in it as they fall, the passages meeting them, and where a wall shuts one off a breach is made, as in any floor.
 */
final class Maze {
	static final FloorKind SIMPLE = new Simple(), COMPLEX = new Complex();

	private Maze() {
	}

	static double weight(Tower t) {
		return t.purpose == Tower.Purpose.ORDER ? 1.6 : t.purpose == Tower.Purpose.GARRISON ? 0.8 : 0.5;
	}

	/**
	 * Passages on a grid of every other cell, the middle one of them, carved from it as a maze (each cell joined to one before it, so it's all one), then {@code loops} of the walls left knocked
	 * through so there's more than one way round. Cells beside the wall where the round room cuts the grid join in too. The dead ends, for what's left in them.
	 */
	static List<int[]> carve(Floor f, Mat wall, double loops, int high) {
		int half = f.half(), n = half / 2 * 2;
		f.rock(-half, f.y, -half, half, f.top(), half, wall);
		List<int[]> open = new ArrayList<>();
		boolean[][] seen = new boolean[n + 1][n + 1];
		int[] start = {0, 0};
		List<int[]> stack = new ArrayList<>();
		stack.add(start);
		seen[n / 2][n / 2] = true;
		cut(f, 0, 0, high);
		List<int[]> ends = new ArrayList<>();
		while (!stack.isEmpty()) {
			int[] c = stack.get(stack.size() - 1);
			List<Integer> next = new ArrayList<>();
			for (int d = 0; d < 4; d++) {
				int u = c[0] + DX[d] * 2, v = c[1] + DZ[d] * 2;
				if (Math.abs(u) > n || Math.abs(v) > n || !f.inFrame(u, v) || !f.inFrame(c[0] + DX[d], c[1] + DZ[d])) continue;
				int iu = (u + n) / 2, iv = (v + n) / 2;
				if (iu < 0 || iv < 0 || iu > n || iv > n || seen[iu][iv]) continue;
				next.add(d);
			}
			if (next.isEmpty()) {
				int ways = 0;
				for (int d = 0; d < 4; d++) if (f.idAt(c[0] + DX[d], f.y, c[1] + DZ[d]) == AIR) ways++;
				if (ways == 1) ends.add(c);
				stack.remove(stack.size() - 1);
				continue;
			}
			int d = next.get(f.r.nextInt(next.size()));
			int u = c[0] + DX[d] * 2, v = c[1] + DZ[d] * 2;
			cut(f, c[0] + DX[d], c[1] + DZ[d], high);
			cut(f, u, v, high);
			seen[(u + n) / 2][(v + n) / 2] = true;
			stack.add(new int[] {u, v});
		}
		// Knock through some of what's left between two passages.
		for (int u = -half; u <= half; u++) {
			for (int v = -half; v <= half; v++) {
				if ((u + v & 1) == 0 || !f.inFrame(u, v) || f.idAt(u, f.y, v) == AIR || f.r.nextDouble() >= loops) continue;
				boolean across = f.idAt(u - 1, f.y, v) == AIR && f.idAt(u + 1, f.y, v) == AIR, along = f.idAt(u, f.y, v - 1) == AIR && f.idAt(u, f.y, v + 1) == AIR;
				if (across ^ along) cut(f, u, v, high);
			}
		}
		ends.removeIf(e -> {
			int ways = 0;
			for (int d = 0; d < 4; d++) if (f.idAt(e[0] + DX[d], f.y, e[1] + DZ[d]) == AIR) ways++;
			return ways != 1;
		});
		return ends;
	}

	private static void cut(Floor f, int u, int v, int high) {
		f.cut(u, f.y, v, u, high, v);
	}

	/** A chest at one dead end and a spawner at another, or near the middle if there's only the one. */
	static void furnish(Floor f, List<int[]> ends, String loot) {
		if (!ends.isEmpty()) {
			int[] e = ends.remove(f.r.nextInt(ends.size()));
			int open = 0;
			for (int d = 0; d < 4; d++) if (f.idAt(e[0] + DX[d], f.y, e[1] + DZ[d]) == AIR) open = d;
			f.chestAt(e[0], f.y, e[1], opposite(open), loot);
		}
		int[] s = ends.isEmpty() ? new int[] {0, 0} : ends.get(f.r.nextInt(ends.size()));
		if (f.idAt(s[0], f.y, s[1]) == AIR) f.spawnerIfFree(s[0], f.y, s[1], null);
	}

	private static final class Simple extends FloorKind {
		Simple() {
			super("maze", Size.SHORT, 4, 5, 0.15, 0.9);
		}

		@Override
		double weight(Tower t) {
			return Maze.weight(t);
		}

		@Override
		Mat overhead(Floor f) {
			return f.pal.wall;
		}

		@Override
		void draw(Floor f) {
			List<int[]> ends = carve(f, f.pal.wall, 0.25, f.y + 2);
			furnish(f, ends, "hall");
			f.cobwebCorners(10);
		}
	}

	/** The same, walled in the trim from floor to ceiling, with more of it to get lost in. */
	private static final class Complex extends FloorKind {
		Complex() {
			super("maze", Size.SHORT, 4, 6, 0.15, 0.9);
		}

		@Override
		double weight(Tower t) {
			return Maze.weight(t);
		}

		@Override
		Mat underfoot(Floor f) {
			return f.pal.flags(f.t);
		}

		@Override
		void draw(Floor f) {
			List<int[]> ends = carve(f, f.pal.trim, 0.12, f.top());
			furnish(f, ends, "hall");
			furnish(f, ends, "hall");
			f.cobwebCorners(16);
		}
	}
}
