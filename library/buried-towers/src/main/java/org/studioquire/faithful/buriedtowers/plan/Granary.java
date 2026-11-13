package org.studioquire.faithful.buriedtowers.plan;

import static org.studioquire.faithful.buriedtowers.plan.Blocks.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Where the grain was kept: bins against the wall, boxes of boards a block high and open on top, empty, and chests of seed and wheat beside them. Stores' towers have them most.
 * Open, as bins are, but each one whole (a board beside a bin reads as a broken one) and one block high.
 */
final class Granary {
	static final FloorKind SIMPLE = new Simple(), COMPLEX = new Complex();

	private Granary() {
	}

	static double weight(Tower t) {
		double w = t.purpose == Tower.Purpose.STORES ? 3.5 : t.purpose == Tower.Purpose.GARRISON ? 1 : 0.5;
		return t.is(Climate.WOODED, Climate.OPEN) ? w * 1.3 : t.is(Climate.DESERT) ? w * 0.6 : w;
	}

	/**
	 * A bin {@code wide} inside, along the wall at an angle: a row of boards a cell out from the wall with an end at each side, against a stretch of wall that runs straight its whole length, so
	 * the wall closes it behind. No other bin's boards touch it, and once in it's held, so nothing later cuts it, walks through it or puts
	 * anything in it. Only where all of it's free. Whether it went in.
	 */
	static boolean bin(Floor f, double degrees, int wide) {
		int[] c = f.against(degrees);
		int d = Floor.outward(c[0], c[1]), side = d + 1 & 3, n = wide + 2;
		// Slid along the wall from where the angle meets it, nearest first, to where the wall runs straight.
		for (int k = 0; k < n; k++) {
			int shift = -((n - 1) / 2) + (k % 2 == 0 ? 1 : -1) * ((k + 1) / 2);
			if (binAt(f, c[0] + DX[side] * shift, c[1] + DZ[side] * shift, d, n)) return true;
		}
		return false;
	}

	private static boolean binAt(Floor f, int u, int v, int d, int n) {
		int side = d + 1 & 3;
		int[][] back = new int[n][], front = new int[n][];
		for (int k = 0; k < n; k++) {
			int[] a = f.toWall(u + DX[side] * k - DX[d] * 3, v + DZ[side] * k - DZ[d] * 3, d);
			if (k > 0 && a[0] * DX[d] + a[1] * DZ[d] != back[0][0] * DX[d] + back[0][1] * DZ[d]) return false;
			back[k] = a;
			front[k] = new int[] {a[0] - DX[d], a[1] - DZ[d]};
		}
		for (int k = 0; k < n; k++) {
			if (!free(f, back[k]) || !free(f, front[k])) return false;
			int bu = back[k][0] + DX[d], bv = back[k][1] + DZ[d];
			if (!solid(f.idAt(bu, f.y, bv)) || f.p.held(f.frame.x(bu, bv), f.y, f.frame.z(bu, bv))) return false;
		}
		// What's round it: the ends' neighbours along the wall and the row in front, corners too.
		List<int[]> round = new ArrayList<>();
		for (int k = -1; k <= n; k++) round.add(new int[] {front[0][0] + DX[side] * k - DX[d], front[0][1] + DZ[side] * k - DZ[d]});
		for (int[][] row : new int[][][] {back, front}) {
			round.add(new int[] {row[0][0] - DX[side], row[0][1] - DZ[side]});
			round.add(new int[] {row[n - 1][0] + DX[side], row[n - 1][1] + DZ[side]});
		}
		for (int[] a : round) {
			if (f.inFrame(a[0], a[1]) && f.idAt(a[0], f.y, a[1]) == PLANKS) return false;
		}
		int board = cell(PLANKS, 0);
		for (int k = 0; k < n; k++) {
			if (k == 0 || k == n - 1) f.put(back[k][0], f.y, back[k][1], board);
			f.put(front[k][0], f.y, front[k][1], board);
			for (int[] a : new int[][] {back[k], front[k]}) {
				for (int yy = f.y; yy <= f.y + 1; yy++) f.p.hold(f.frame.x(a[0], a[1]), yy, f.frame.z(a[0], a[1]));
			}
		}
		return true;
	}

	private static boolean free(Floor f, int[] a) {
		if (!f.inFrame(a[0], a[1])) return false;
		int x = f.frame.x(a[0], a[1]), z = f.frame.z(a[0], a[1]);
		return f.idAt(a[0], f.y, a[1]) == AIR && f.idAt(a[0], f.y + 1, a[1]) == AIR && !f.p.held(x, f.y, z) && !f.p.held(x, f.y + 1, z);
	}

	private static final class Simple extends FloorKind {
		Simple() {
			super("granary", Size.SHORT, 4, 6, 0, 0.6);
		}

		@Override
		double weight(Tower t) {
			return Granary.weight(t);
		}

		@Override
		Mat underfoot(Floor f) {
			return SimpleFloors.worn(f, Mat.of(PLANKS));
		}

		@Override
		void draw(Floor f) {
			int bins = f.half() >= 5 ? 4 : 3, turn = f.r.nextInt(360), made = 0;
			// Round the wall from a start, as the ways take much of a small room's wall; a narrower bin where a wide one won't go.
			for (int k = 0; k < 24 && made < bins; k++) {
				if (bin(f, turn + 15 * k, 2) || bin(f, turn + 15 * k, 1)) made++;
			}
			SimpleFloors.onWall(f, turn + 180.0 / bins, "granary");
			if (made < 3) SimpleFloors.onWall(f, turn + 180.0 / bins + 180, "granary");
			int[] s = Floor.polar(f.r.nextInt(360), 1.5);
			f.spawnerIfFree(s[0], f.y, s[1], null);
			SimpleFloors.decay(f, 1, 10);
		}
	}

	/** Bins all round the wall between timber posts, a threshing floor of flags in the middle, and the chests on it. */
	private static final class Complex extends FloorKind {
		Complex() {
			super("granary", Size.MEDIUM, 6, 8, 0, 0.6);
		}

		@Override
		double weight(Tower t) {
			return Granary.weight(t);
		}

		@Override
		Mat underfoot(Floor f) {
			Mat flags = f.pal.flags(f.t), planks = Mat.of(PLANKS);
			double court = f.rim() - 4;
			return (x, y, z) -> Math.hypot(x - f.t.cx, z - f.t.cz) < court ? flags.at(x, y, z) : planks.at(x, y, z);
		}

		@Override
		void draw(Floor f) {
			int posts = 12, turn = f.r.nextInt(30);
			for (int i = 0; i < posts; i++) {
				int[] c = f.against(turn + 360.0 * i / posts);
				f.pier(c[0], c[1], c[0], c[1], f.top(), f.pal.beam);
			}
			for (int i = 0; i < posts; i++) {
				if (f.r.nextInt(6) == 0) continue;
				double a = turn + 360.0 * (i + 0.5) / posts;
				if (!bin(f, a, 2)) bin(f, a, 1);
			}
			f.ring(f.rim() - 4, f.y - 1, f.pal.kerb());
			for (int i = 0; i < 2; i++) {
				int[] c = Floor.polar(turn + 180 * i + 90, 2);
				if (f.idAt(c[0], f.y, c[1]) == AIR) f.chestAt(c[0], f.y, c[1], Floor.outward(-c[0], -c[1]), "granary");
			}
			for (int i = 0; i < 3; i++) {
				int[] c = Floor.polar(f.r.nextInt(360), f.rim() - 2.5);
				if (f.idAt(c[0], f.y, c[1]) == AIR) f.put(c[0], f.y, c[1], cell(PUMPKIN, f.r.nextInt(4)));
			}
			for (int i = 0; i < 2; i++) {
				int[] s = Floor.polar(turn + 180 * i, f.rim() - 4.5);
				f.spawnerIfFree(s[0], f.y, s[1], null);
			}
			f.cobwebCorners(14);
		}
	}
}
