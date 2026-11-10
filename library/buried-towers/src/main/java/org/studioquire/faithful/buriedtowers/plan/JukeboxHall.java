package org.studioquire.faithful.buriedtowers.plan;

import static org.studioquire.faithful.buriedtowers.plan.Blocks.*;

/**
 * A rare prize: the room where the garrison kept its jukebox, and the last record still in it (right-clicked, it comes out). Benches of wooden stairs face it across a floor of boards. The jukebox is
 * worth a diamond to whoever carries it off, which is the point.
 */
final class JukeboxHall {
	static final FloorKind SIMPLE = new Simple(), COMPLEX = new Complex();

	private JukeboxHall() {
	}

	static double weight(Tower t) {
		return t.purpose == Tower.Purpose.GARRISON ? 0.3 : 0.15;
	}

	/** The jukebox at a place, with one of beta's two records in it. */
	static void jukebox(Floor f, int u, int yy, int v) {
		int x = f.frame.x(u, v), z = f.frame.z(u, v);
		if (!f.in(x, z) || f.p.held(x, yy, z)) return;
		f.p.set(x, yy, z, cell(JUKEBOX, 1));
		f.p.fixture(x, yy, z, "jukebox:" + (f.r.nextBoolean() ? "13" : "cat"));
	}

	/** The first quarter, from a random start, whose wall has room for the jukebox and a free cell in front of it: the jukebox's place, or null. */
	static int[] spot(Floor f, int level, int clear) {
		int first = f.r.nextInt(4);
		for (int k = 0; k < 4; k++) {
			int d = first + k & 3;
			int[] c = f.toWall(0, 0, d);
			boolean free = true;
			for (int back = 0; back <= clear && free; back++) {
				int u = c[0] - DX[d] * back, v = c[1] - DZ[d] * back;
				for (int yy = level; yy <= level + 1 && free; yy++) free = f.idAt(u, yy, v) == AIR && !f.p.held(f.frame.x(u, v), yy, f.frame.z(u, v));
			}
			if (free) return new int[] {c[0], c[1], d};
		}
		return null;
	}

	/** A bench of wooden stairs {@code wide} across, its backs away from the jukebox, which lies {@code d} of it, at a place. Only the seats that are free go in. */
	static void bench(Floor f, int u, int v, int d, int wide, int level) {
		int side = d + 1 & 3;
		for (int k = -(wide / 2); k < wide - wide / 2; k++) {
			int bu = u + DX[side] * k, bv = v + DZ[side] * k;
			if (!f.inFrame(bu, bv) || f.idAt(bu, level, bv) != AIR || f.idAt(bu, level + 1, bv) != AIR || f.p.held(f.frame.x(bu, bv), level, f.frame.z(bu, bv))) continue;
			f.put(bu, level, bv, cell(WOOD_STAIRS, stairMeta(f.frame.dir(opposite(d)))));
		}
	}

	/** Plain: the jukebox against the wall, two benches facing it, a chest, a floor of boards worn through. */
	private static final class Simple extends FloorKind {
		Simple() {
			super("jukebox hall", Size.MEDIUM, 5, 6, 0.1, 0.85);
		}

		@Override
		double weight(Tower t) {
			return JukeboxHall.weight(t);
		}

		@Override
		Mat underfoot(Floor f) {
			return SimpleFloors.worn(f, Mat.of(PLANKS));
		}

		@Override
		void draw(Floor f) {
			int[] j = spot(f, f.y, 1);
			int turn = f.r.nextInt(360);
			if (j != null) {
				int d = j[2];
				jukebox(f, j[0], f.y, j[1]);
				// Two benches, a row apart, the nearer two clear of it.
				for (int row = 0; row < 2; row++) {
					int back = 3 + row * 2;
					bench(f, j[0] - DX[d] * back, j[1] - DZ[d] * back, d, 3, f.y);
				}
				turn = (int) Math.toDegrees(Math.atan2(DZ[d], DX[d])) + 90;
			}
			SimpleFloors.onWall(f, turn, "hall");
			int[] s = Floor.polar(turn + 180, 2);
			f.spawnerIfFree(s[0], f.y, s[1], null);
			SimpleFloors.decay(f, 1, 8);
		}
	}

	/** A dais of the trim against the wall with the jukebox on it between two pilasters, rows of benches across a floor of boards, flags round the edge. */
	private static final class Complex extends FloorKind {
		Complex() {
			super("jukebox hall", Size.MEDIUM, 6, 8, 0.1, 0.85);
		}

		@Override
		double weight(Tower t) {
			return JukeboxHall.weight(t);
		}

		@Override
		Mat underfoot(Floor f) {
			Mat flags = f.pal.flags(f.t), planks = Mat.of(PLANKS);
			double court = f.rim() - 2.5;
			return (x, y, z) -> Math.hypot(x - f.t.cx, z - f.t.cz) < court ? planks.at(x, y, z) : flags.at(x, y, z);
		}

		@Override
		void draw(Floor f) {
			int[] j = spot(f, f.y, 3);
			int turn = f.r.nextInt(360);
			if (j != null) {
				int d = j[2], side = d + 1 & 3;
				// The dais: three across, two out from the wall, a step up; the jukebox at its back, pilasters either side of it.
				for (int out = 0; out < 2; out++) {
					for (int k = -1; k <= 1; k++) {
						int u = j[0] - DX[d] * out + DX[side] * k, v = j[1] - DZ[d] * out + DZ[side] * k;
						int[] w = f.toWall(u, v, d);
						if (out == 0) {
							u = w[0];
							v = w[1];
						}
						if (f.inFrame(u, v) && f.idAt(u, f.y, v) == AIR && !f.p.held(f.frame.x(u, v), f.y, f.frame.z(u, v))) f.put(u, f.y, v, f.pal.trim);
					}
				}
				jukebox(f, j[0], f.y + 1, j[1]);
				for (int k = -2; k <= 2; k += 4) {
					int[] w = f.toWall(j[0] + DX[side] * k, j[1] + DZ[side] * k, d);
					f.pier(w[0], w[1], w[0], w[1], f.top(), f.pal.trim);
				}
				for (int row = 0; row < 3; row++) {
					int back = 4 + row * 2;
					bench(f, j[0] - DX[d] * back - DX[side] * 2, j[1] - DZ[d] * back - DZ[side] * 2, d, 3, f.y);
					bench(f, j[0] - DX[d] * back + DX[side] * 2, j[1] - DZ[d] * back + DZ[side] * 2, d, 3, f.y);
				}
				turn = (int) Math.toDegrees(Math.atan2(DZ[d], DX[d]));
			}
			SimpleFloors.onWall(f, turn + 60, "hall");
			SimpleFloors.onWall(f, turn + 180, "hall");
			for (int i = 0; i < 2; i++) {
				int[] s = Floor.polar(turn + 120 + 120 * i, f.rim() - 2.5);
				f.spawnerIfFree(s[0], f.y, s[1], null);
			}
			f.cobwebCorners(12);
		}
	}
}
