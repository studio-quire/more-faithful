package org.studioquire.faithful.buriedtowers.plan;

import static org.studioquire.faithful.buriedtowers.plan.Blocks.*;

/** Where the garrison kept its gear: racks along the wall, some gone, and the chests of arms beside them. No traps (the user, 2026-11-08). */
final class Armoury {
	static final FloorKind SIMPLE = new Simple(), COMPLEX = new Complex();

	private Armoury() {
	}

	static double weight(Tower t) {
		double w = t.purpose == Tower.Purpose.GARRISON ? 3 : t.purpose == Tower.Purpose.MINE ? 0.6 : 0.5;
		return t.is(Climate.OPEN) ? w * 1.4 : w;
	}

	/** A rack {@code wide} along the wall at an angle: fence {@code tall} high, and in a complex tower a board over it. Only where it's all free. */
	static boolean rack(Floor f, double degrees, int wide, int tall, boolean shelf) {
		int[] c = f.against(degrees);
		int d = Floor.outward(c[0], c[1]), side = d + 1 & 3;
		int[][] cells = new int[wide][];
		for (int k = 0; k < wide; k++) cells[k] = f.toWall(c[0] + DX[side] * (k - wide / 2) - DX[d] * 3, c[1] + DZ[side] * (k - wide / 2) - DZ[d] * 3, d);
		for (int[] a : cells) {
			int x = f.frame.x(a[0], a[1]), z = f.frame.z(a[0], a[1]);
			for (int yy = f.y; yy <= f.y + Math.max(tall, 2) - 1; yy++) {
				if (f.idAt(a[0], yy, a[1]) != AIR || f.p.held(x, yy, z)) return false;
			}
		}
		for (int[] a : cells) {
			for (int k = 0; k < tall; k++) f.put(a[0], f.y + k, a[1], cell(FENCE, 0));
			if (shelf) f.put(a[0], f.y + tall, a[1], cell(SLAB, WOOD_SLAB));
		}
		return true;
	}

	private static final class Simple extends FloorKind {
		Simple() {
			super("armoury", Size.MEDIUM, 5, 6, 0.1, 0.85);
		}

		@Override
		double weight(Tower t) {
			return Armoury.weight(t);
		}

		/** Long racks two high, plain fence, so they read as racks in a room this small (the user, 2026-11-08, after short low ones). */
		@Override
		void draw(Floor f) {
			int turn = f.r.nextInt(360);
			for (int k = 0, made = 0; k < 24 && made < 3; k++) {
				if (rack(f, turn + 15 * k, 4, 2, false) || rack(f, turn + 15 * k, 3, 2, false)) made++;
			}
			SimpleFloors.onWall(f, turn + 45, "arms");
			if (f.r.nextBoolean()) SimpleFloors.onWall(f, turn + 225, "arms");
			int[] s = Floor.polar(turn + 135, 1.5);
			f.spawnerIfFree(s[0], f.y, s[1], null);
			SimpleFloors.decay(f, 1, 10);
		}
	}

	/** Racks with a shelf over them between pilasters of the trim all round, the chests between them, a forge of brick at one end and a table in the middle. */
	private static final class Complex extends FloorKind {
		Complex() {
			super("armoury", Size.MEDIUM, 6, 8, 0.1, 0.85);
		}

		@Override
		double weight(Tower t) {
			return Armoury.weight(t);
		}

		@Override
		Mat underfoot(Floor f) {
			return f.pal.flags(f.t);
		}

		@Override
		void draw(Floor f) {
			int turn = f.r.nextInt(30), bays = 10;
			for (int i = 0; i < bays; i++) {
				int[] c = f.against(turn + 360.0 * i / bays);
				f.pier(c[0], c[1], c[0], c[1], f.top(), f.pal.trim);
			}
			int forge = f.r.nextInt(bays);
			for (int i = 0; i < bays; i++) {
				double a = turn + 360.0 * (i + 0.5) / bays;
				if (i == forge) {
					int[] c = f.against(a);
					if (f.idAt(c[0], f.y, c[1]) == AIR) {
						f.put(c[0], f.y, c[1], cell(FURNACE, backTo(f.frame.dir(Floor.outward(c[0], c[1])))));
						f.box(c[0], f.y + 1, c[1], c[0], f.top(), c[1], Mat.of(BRICK));
					}
				} else if (i % 3 == 1) {
					SimpleFloors.onWall(f, a, "arms");
				} else if (f.r.nextInt(5) != 0) {
					rack(f, a, 2, 1, true);
				}
			}
			for (int k = -1; k <= 1; k += 2) {
				int u = k * 2, v = 0;
				if (f.idAt(u, f.y, v) != AIR || f.idAt(u, f.y + 1, v) != AIR || f.p.held(f.frame.x(u, v), f.y, f.frame.z(u, v))) continue;
				f.put(u, f.y, v, cell(FENCE, 0));
				f.put(u, f.y + 1, v, cell(SLAB, WOOD_SLAB));
			}
			for (int i = 0; i < 2; i++) {
				int[] s = Floor.polar(turn + 90 + 180 * i, f.rim() - 3);
				f.spawnerIfFree(s[0], f.y, s[1], null);
			}
			f.cobwebCorners(12);
		}
	}
}
