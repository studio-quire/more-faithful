package org.studioquire.faithful.buriedtowers.plan;

import static org.studioquire.faithful.buriedtowers.plan.Blocks.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A tall room you come into high up, on a stage at the top, and leave from the floor: a walkway along the wall winds down round it from the stage to the floor, a turn and a bit, a step of stairs
 * every few cells. Nothing's under the walkway but the room, so a fall off it is a long one. Only stairs or a ladder come in, landing on the stage, and leave from the floor.
 */
final class SpiralGallery {
	static final FloorKind SIMPLE = new Simple(), COMPLEX = new Complex();

	private SpiralGallery() {
	}

	static double weight(Tower t) {
		return t.purpose == Tower.Purpose.ORDER ? 1.3 : t.purpose == Tower.Purpose.STORES ? 1 : 0.7;
	}

	/** The cells along the wall at the given depth, in order round the room, joined side to side: where two meet only at a corner, the cell inside them between. */
	static List<int[]> ring(Floor f, int depth) {
		Tower t = f.t;
		List<int[]> cells = new ArrayList<>();
		for (int x = t.cx - t.radius; x <= t.cx + t.radius; x++) {
			for (int z = t.cz - t.radius; z <= t.cz + t.radius; z++) {
				if (t.depth(x, z) == depth) cells.add(new int[] {x, z});
			}
		}
		cells.sort((a, b) -> Double.compare(Math.atan2(a[1] - t.cz, a[0] - t.cx), Math.atan2(b[1] - t.cz, b[0] - t.cx)));
		List<int[]> joined = new ArrayList<>();
		for (int i = 0; i < cells.size(); i++) {
			int[] a = cells.get(i), b = cells.get((i + 1) % cells.size());
			joined.add(a);
			if (Math.abs(a[0] - b[0]) == 1 && Math.abs(a[1] - b[1]) == 1) {
				int[] p = {a[0], b[1]}, q = {b[0], a[1]};
				joined.add(t.depth(p[0], p[1]) > depth ? p : q);
			}
		}
		return joined;
	}

	/** The world direction from one cell to the next one beside it. */
	private static int toward(int[] a, int[] b) {
		return b[0] > a[0] ? EAST : b[0] < a[0] ? WEST : b[1] > a[1] ? SOUTH : NORTH;
	}

	/**
	 * The walkway: from the cell along the wall nearest where the way in lands, round the room in whichever direction runs into nothing a way holds, dropping a cell every {@code rate} cells until it
	 * meets the floor; the last cell of each run before a drop is a stair down. {@code lanes} wide, the inner lane following the outer. Returns its first cell, where it meets the stage, or null.
	 */
	static int[] walkway(Floor f, int lanes, Mat stuff, int stairs) {
		Tower t = f.t;
		Plan p = f.p;
		List<int[]> ring = ring(f, t.wall + 1);
		int n = ring.size(), top = f.stand - 1, drop = f.stand - f.y;
		if (n < 8) return null;
		int rate = Math.max(1, Math.min(n / 4, (int) Math.round(n * 1.3 / drop)));
		int lx = f.in != null ? f.in.lx : t.cx, lz = f.in != null ? f.in.lz : t.cz;
		// Each start near the landing, each way round, and the one that meets fewest held cells above where a drop off it would hurt.
		Integer[] order = new Integer[n];
		for (int i = 0; i < n; i++) order[i] = i;
		java.util.Arrays.sort(order, (a, b) -> Double.compare(Math.hypot(ring.get(a)[0] - lx, ring.get(a)[1] - lz), Math.hypot(ring.get(b)[0] - lx, ring.get(b)[1] - lz)));
		int bestStart = -1, bestDir = 1, bestBad = Integer.MAX_VALUE;
		for (int k = 0; k < Math.min(n, 8) && bestBad > 0; k++) {
			int s = order[k];
			int[] c = ring.get(s);
			if (blocked(p, c[0], top, c[1])) continue;
			for (int dir = -1; dir <= 1; dir += 2) {
				int bad = 0;
				for (int i = 0; ; i++) {
					int level = top - i / rate;
					if (level <= f.y - 1) break;
					int[] a = ring.get(Math.floorMod(s + dir * i, n));
					if (level > f.y + 2 && blocked(p, a[0], level, a[1])) bad++;
				}
				if (bad < bestBad) {
					bestBad = bad;
					bestStart = s;
					bestDir = dir;
				}
			}
		}
		if (bestStart < 0) return null;
		List<int[]> laid = new ArrayList<>();
		Map<Long, Boolean> outer = new HashMap<>();
		for (int i = 0; ; i++) {
			int level = top - i / rate;
			if (level <= f.y - 1) break;
			int[] a = ring.get(Math.floorMod(bestStart + bestDir * i, n));
			boolean last = (i + 1) % rate == 0 && top - (i + 1) / rate > f.y - 1;
			int cell = stuff.at(a[0], level, a[1]);
			if (last) {
				int[] back = ring.get(Math.floorMod(bestStart + bestDir * (i - 1), n));
				cell = cell(stairs, stairMeta(i == 0 ? opposite(toward(a, ring.get(Math.floorMod(bestStart + bestDir, n)))) : toward(a, back)));
			}
			lay(f, a[0], level, a[1], cell);
			laid.add(new int[] {a[0], level, a[1], cell});
			outer.put(Plan.key(a[0], 0, a[1]), true);
		}
		if (lanes > 1) {
			// The inner lane beside each cell of the outer one, at its level, each turn of it.
			for (int[] a : laid) {
				for (int d = 0; d < 4; d++) {
					int x = a[0] + DX[d], z = a[2] + DZ[d];
					if (t.depth(x, z) != t.wall + 2 || outer.containsKey(Plan.key(x, 0, z))) continue;
					lay(f, x, a[1], z, a[3]);
				}
			}
		}
		int[] first = ring.get(bestStart);
		return new int[] {first[0], first[1]};
	}

	/** Whether a way holds the cell or the two over it. */
	private static boolean blocked(Plan p, int x, int level, int z) {
		return p.held(x, level, z) || p.held(x, level + 1, z) || p.held(x, level + 2, z);
	}

	private static void lay(Floor f, int x, int level, int z, int cell) {
		if (blocked(f.p, x, level, z) || f.p.id(x, level, z) != AIR) return;
		f.p.set(x, level, z, cell);
	}

	/** Its first cell's where the way in has to be joined to, over the stage: a walk does it once the ways are drawn. */
	static void meet(Floor f, int[] first) {
		if (first == null) return;
		f.goalX = first[0];
		f.goalZ = first[1];
		f.hasGoal = true;
	}

	/** Plain: a walkway of boards one wide, wooden stairs at its steps, so it reads against the stone, and the floor bare but for a chest and a spawner. */
	private static final class Simple extends FloorKind {
		Simple() {
			super("spiral gallery", Size.TALL, 12, 15, 0.1, 0.9);
		}

		@Override
		double weight(Tower t) {
			return SpiralGallery.weight(t);
		}

		@Override
		int standOffset(int h) {
			return h - 4;
		}

		@Override
		int leaveOffset(int h) {
			return 0;
		}

		@Override
		boolean landsOnStage() {
			return true;
		}

		@Override
		boolean openAt(Floor f, int x, int z) {
			return f.t.depth(x, z) > f.t.wall + 1;
		}

		@Override
		void draw(Floor f) {
			meet(f, walkway(f, 1, Mat.of(PLANKS), WOOD_STAIRS));
			int turn = f.r.nextInt(360);
			SimpleFloors.onWall(f, turn, "hall");
			int[] s = Floor.polar(turn + 180, 1.5);
			f.spawnerIfFree(s[0], f.y, s[1], null);
			f.cobwebCorners(10);
		}
	}

	/** A walkway two wide of the floor's flags winding down a taller room, a kerb round the floor below, two chests. */
	private static final class Complex extends FloorKind {
		Complex() {
			super("spiral gallery", Size.TALL, 13, 17, 0.1, 0.9);
		}

		@Override
		double weight(Tower t) {
			return SpiralGallery.weight(t);
		}

		@Override
		int standOffset(int h) {
			return h - 4;
		}

		@Override
		int leaveOffset(int h) {
			return 0;
		}

		@Override
		boolean landsOnStage() {
			return true;
		}

		@Override
		boolean openAt(Floor f, int x, int z) {
			return f.t.depth(x, z) > f.t.wall + 2;
		}

		@Override
		Mat underfoot(Floor f) {
			return f.pal.flags(f.t);
		}

		@Override
		void draw(Floor f) {
			meet(f, walkway(f, 2, f.pal.trim, f.pal.stairs));
			f.ring(f.rim() - 4, f.y - 1, f.pal.kerb());
			int turn = f.r.nextInt(360);
			SimpleFloors.onWall(f, turn, "hall");
			SimpleFloors.onWall(f, turn + 150, "books");
			for (int i = 0; i < 2; i++) {
				int[] s = Floor.polar(turn + 75 + 180 * i, f.rim() - 5);
				f.spawnerIfFree(s[0], f.y, s[1], null);
			}
			f.cobwebCorners(16);
		}
	}
}
