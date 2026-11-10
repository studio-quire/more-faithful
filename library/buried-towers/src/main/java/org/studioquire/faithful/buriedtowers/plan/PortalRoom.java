package org.studioquire.faithful.buriedtowers.plan;

import static org.studioquire.faithful.buriedtowers.plan.Blocks.*;

/**
 * A rare prize: a frame of obsidian someone built down here and never lit, whole, so flint and steel would open it. Nothing of the Nether comes with it (the user, 2026-11-06), and its obsidian is
 * fine to take (2026-11-08). The order's towers have it most, and the deeper half.
 */
final class PortalRoom {
	static final FloorKind SIMPLE = new Simple(), COMPLEX = new Complex();

	private PortalRoom() {
	}

	static double weight(Tower t) {
		return t.purpose == Tower.Purpose.ORDER ? 0.6 : 0.25;
	}

	/**
	 * A frame four across and five high in the floor's frame, along {@code side} from {@code u, v} (its first column), its bottom row at {@code base}: the opening two across and three high above it.
	 * Whether it went in, which it does only where every cell of it and of the opening above its bottom row is empty and none is held.
	 */
	static boolean frame(Floor f, int u, int v, int side, int base) {
		for (int k = 0; k < 4; k++) {
			int cu = u + DX[side] * k, cv = v + DZ[side] * k;
			if (!f.inFrame(cu, cv)) return false;
			for (int yy = base; yy <= base + 4; yy++) {
				if (f.p.held(f.frame.x(cu, cv), yy, f.frame.z(cu, cv))) return false;
				if (yy > base && f.idAt(cu, yy, cv) != AIR) return false;
			}
		}
		for (int k = 0; k < 4; k++) {
			int cu = u + DX[side] * k, cv = v + DZ[side] * k;
			for (int yy = base; yy <= base + 4; yy++) {
				boolean post = k == 0 || k == 3, rail = yy == base || yy == base + 4;
				if (post || rail) f.p.force(f.frame.x(cu, cv), yy, f.frame.z(cu, cv), cell(OBSIDIAN, 0));
			}
		}
		return true;
	}

	/** The frame against the wall at the first quarter from a random start where it fits, its sill let into the floor so you walk straight in, the gap behind it made good. Its quarter, or -1. */
	static int againstWall(Floor f) {
		int first = f.r.nextInt(4);
		for (int k = 0; k < 4; k++) {
			int d = first + k & 3, side = d + 1 & 3;
			int[] c = f.toWall(0, 0, d);
			// As far toward the wall as the four columns are all inside it, the frame centred on the middle line.
			for (int back = 0; back <= 2; back++) {
				int u = c[0] - DX[d] * back - DX[side] * 2, v = c[1] - DZ[d] * back - DZ[side] * 2;
				if (!frame(f, u, v, side, f.y - 1)) continue;
				for (int kk = 0; kk < 4; kk++) {
					int bu = u + DX[side] * kk + DX[d], bv = v + DZ[side] * kk + DZ[d];
					for (int yy = f.y; yy <= f.y + 3; yy++) if (f.inFrame(bu, bv) && f.idAt(bu, yy, bv) == AIR) f.box(bu, yy, bv, bu, yy, bv, f.pal.wall);
				}
				return d;
			}
		}
		return -1;
	}

	/** Plain: the frame against the wall, a chest and what time's done. Corners too: a built room's frame is whole, and without them it looked unfinished (user, 2026-11-08). */
	private static final class Simple extends FloorKind {
		Simple() {
			super("portal frame", Size.MEDIUM, 6, 7, 0.45, 0.95);
		}

		@Override
		double weight(Tower t) {
			return PortalRoom.weight(t);
		}

		@Override
		void draw(Floor f) {
			int placed = againstWall(f);
			double turn = placed < 0 ? f.r.nextInt(360) : Math.toDegrees(Math.atan2(DZ[placed], DX[placed]));
			SimpleFloors.onWall(f, turn + 120, "crypt");
			int[] s = Floor.polar(turn + 200, 2);
			f.spawnerIfFree(s[0], f.y, s[1], null);
			SimpleFloors.decay(f, 1, 10);
		}
	}

	/**
	 * The frame with its corners on a round dais of the trim in the middle, a ring of pillars round it, flags underfoot, two chests on the wall. Ways keep off the dais; if one had to stand in the
	 * middle anyway (the last resort of a ladder there), the frame stands against the wall instead.
	 */
	private static final class Complex extends FloorKind {
		private static final double DAIS = 3.2;

		Complex() {
			super("portal frame", Size.MEDIUM, 8, 10, 0.45, 0.95);
		}

		@Override
		double weight(Tower t) {
			return PortalRoom.weight(t);
		}

		@Override
		Mat underfoot(Floor f) {
			return f.pal.flags(f.t);
		}

		@Override
		double heart(Floor f) {
			return DAIS + 1;
		}

		@Override
		boolean allows(Floor f, int x, int z) {
			return Math.hypot(x - f.t.cx, z - f.t.cz) >= DAIS + 0.5;
		}

		@Override
		void draw(Floor f) {
			int along = f.r.nextBoolean() ? EAST : SOUTH;
			if (f.clearOf(DAIS + 0.5)) {
				f.disc(0, 0, DAIS, f.y, f.pal.trim);
				f.ring(DAIS + 0.5, f.y - 1, f.pal.kerb());
				frame(f, -DX[along] * 2, -DZ[along] * 2, along, f.y);
			} else againstWall(f);
			int turn = f.r.nextInt(45);
			double ring = f.rim() - 2.5;
			for (int i = 0; i < 8; i++) {
				int[] c = Floor.polar(turn + 45 * i, ring);
				f.pier(c[0], c[1], c[0], c[1], f.top(), f.pal.pillar);
			}
			SimpleFloors.onWall(f, turn + 22, "crypt");
			SimpleFloors.onWall(f, turn + 202, "crypt");
			for (int i = 0; i < 2; i++) {
				int[] s = Floor.polar(turn + 112 + 180 * i, ring - 1.5);
				f.spawnerIfFree(s[0], f.y, s[1], null);
			}
			f.cobwebCorners(14);
		}
	}
}
