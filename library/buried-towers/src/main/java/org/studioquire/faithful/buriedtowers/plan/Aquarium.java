package org.studioquire.faithful.buriedtowers.plan;

import static org.studioquire.faithful.buriedtowers.plan.Blocks.*;

/**
 * Tanks built into the rock behind the wall, each seen through a window of glass in it: water over a bed of sand, a cell of air over the water under the tank's lid, so nothing in it is ever under a
 * solid block and squid can come (beta's water creatures appear in any water with no solid block over it). Glass sparingly, as a built thing: a few small windows, not a wall of it. Every face of a
 * tank that isn't the tower's wall is lined in the wall's stuff, so no cave, lava or open air meets its water.
 */
final class Aquarium {
	static final FloorKind SIMPLE = new Simple(), COMPLEX = new Complex();

	private Aquarium() {
	}

	static double weight(Tower t) {
		return t.is(Climate.WET) ? 1.1 : t.is(Climate.DESERT) ? 0.3 : t.purpose == Tower.Purpose.ORDER ? 0.9 : 0.5;
	}

	/**
	 * A tank behind the wall at an angle, {@code across} wide (odd) and {@code deep} back into the rock, its water from the floor's level to {@code high} cells up with a cell of air over it, and a window
	 * {@code tall} high from a cell over the floor (two thick walls: in the inner cell, the outer one water). The window's sides and sill in {@code frame} if not null. Whether it went in: only where the
	 * window's cells are plain wall, nothing's held there, and the tank meets nothing of the tower's but its wall.
	 */
	static boolean tank(Floor f, double degrees, int across, int deep, int high, int tall, Mat frame) {
		Tower t = f.t;
		Plan p = f.p;
		int[] c = f.against(degrees);
		int d = Floor.outward(c[0], c[1]), side = d + 1 & 3, half = across / 2;
		int x0 = f.frame.x(c[0], c[1]), z0 = f.frame.z(c[0], c[1]), dx = DX[f.frame.dir(d)], dz = DZ[f.frame.dir(d)], sx = DX[f.frame.dir(side)], sz = DZ[f.frame.dir(side)];
		int bottom = f.y, lid = f.y + high + 1;
		// The window's column: the wall cells straight out from the room cell, then the rock.
		int out = 1;
		while (t.depth(x0 + dx * out, z0 + dz * out) > 0) out++;
		if (out - 1 != t.wall) return false;
		for (int k = 1; k < out; k++) {
			int x = x0 + dx * k, z = z0 + dz * k;
			for (int yy = f.y; yy <= f.y + tall + 1; yy++) {
				if (p.held(x, yy, z) || !solid(p.id(x, yy, z))) return false;
			}
		}
		// Every cell of the tank, and of its lining, out in the rock and touching nothing the tower's built but its wall.
		for (int k = out - 1; k <= out + deep; k++) {
			for (int j = -half - 1; j <= half + 1; j++) {
				int x = x0 + dx * k + sx * j, z = z0 + dz * k + sz * j;
				if (t.depth(x, z) > 0) continue;
				for (int yy = bottom - 2; yy <= lid; yy++) {
					if (p.held(x, yy, z)) return false;
					int id = p.id(x, yy, z);
					if (p.get(x, yy, z) != Plan.KEEP && id != AIR && !solid(id)) return false;
				}
			}
		}
		for (int k = out; k < out + deep; k++) {
			for (int j = -half; j <= half; j++) {
				int x = x0 + dx * k + sx * j, z = z0 + dz * k + sz * j;
				if (t.depth(x, z) > 0) continue;
				for (int yy = bottom - 2; yy <= lid; yy++) {
					int cell;
					if (yy == bottom - 2 || yy == lid) cell = f.pal.wall.at(x, yy, z);
					else if (yy == bottom - 1) cell = (int) (t.noise.at(x * 3.1, yy, z * 2.7, 1) * 10) < 7 ? cell(SAND, 0) : cell(GRAVEL, 0);
					else if (yy < lid - 1) cell = cell(WATER, 0);
					else cell = AIR;
					p.force(x, yy, z, cell);
				}
			}
		}
		// The lining round its sides, wherever it isn't the tower's wall.
		for (int k = out - 1; k <= out + deep; k++) {
			for (int j = -half - 1; j <= half + 1; j++) {
				boolean inner = k >= out && k < out + deep && Math.abs(j) <= half;
				int x = x0 + dx * k + sx * j, z = z0 + dz * k + sz * j;
				if (inner || t.depth(x, z) > 0) continue;
				for (int yy = bottom - 2; yy <= lid; yy++) p.force(x, yy, z, f.pal.wall.at(x, yy, z));
			}
		}
		// The window: glass in the inner wall cell, water in any wall cell beyond it.
		for (int k = 1; k < out; k++) {
			int x = x0 + dx * k, z = z0 + dz * k;
			for (int yy = f.y + 1; yy <= f.y + tall; yy++) p.force(x, yy, z, k == 1 ? cell(GLASS, 0) : cell(WATER, 0));
			if (k > 1) {
				p.force(x, f.y, z, cell(WATER, 0));
				for (int yy = f.y + tall + 1; yy < lid - 1; yy++) p.force(x, yy, z, cell(WATER, 0));
				p.force(x, lid - 1, z, AIR);
			}
		}
		if (frame != null) {
			for (int j = -1; j <= 1; j += 2) {
				int x = x0 + sx * j, z = z0 + sz * j;
				if (!f.in(x, z)) continue;
				for (int yy = f.y; yy <= f.y + tall + 1; yy++) {
					if (p.id(x, yy, z) == AIR && !p.held(x, yy, z) && solid(p.id(x + dx, yy, z + dz))) p.set(x, yy, z, frame.at(x, yy, z));
				}
			}
			if (p.id(x0, f.y, z0) == AIR && !p.held(x0, f.y, z0)) p.set(x0, f.y, z0, frame.at(x0, f.y, z0));
		}
		return true;
	}

	/** Two or three small windows in the plain wall, two high, each a tank three across behind it; a chest and a spawner. */
	private static final class Simple extends FloorKind {
		Simple() {
			super("aquarium", Size.MEDIUM, 5, 7, 0.2, 0.95);
		}

		@Override
		double weight(Tower t) {
			return Aquarium.weight(t);
		}

		@Override
		void draw(Floor f) {
			int turn = f.r.nextInt(360), want = 2 + f.r.nextInt(2), made = 0;
			for (int k = 0; k < 12 && made < want; k++) {
				if (tank(f, turn + 360.0 * made / want + 15 * (k - made), 3, 3, 3, 2, null)) made++;
			}
			SimpleFloors.onWall(f, turn + 180.0 / want, "sunken");
			int[] s = Floor.polar(turn + 180.0 / want + 180, 2);
			f.spawnerIfFree(s[0], f.y, s[1], null);
			SimpleFloors.decay(f, 1, 8);
		}
	}

	/** Tall windows three high between pilasters of the trim all round, each with its tank, a sill of the trim under each; flags underfoot. */
	private static final class Complex extends FloorKind {
		Complex() {
			super("aquarium", Size.MEDIUM, 7, 10, 0.2, 0.95);
		}

		@Override
		double weight(Tower t) {
			return Aquarium.weight(t);
		}

		@Override
		Mat underfoot(Floor f) {
			return f.pal.flags(f.t);
		}

		@Override
		void draw(Floor f) {
			int turn = f.r.nextInt(360), bays = 5, made = 0;
			for (int i = 0; i < bays; i++) {
				if (tank(f, turn + 360.0 * i / bays, 3, 4, 5, 3, f.pal.trim)) made++;
			}
			if (made == 0) tank(f, turn + 36, 3, 4, 5, 3, f.pal.trim);
			SimpleFloors.onWall(f, turn + 180.0 / bays, "sunken");
			SimpleFloors.onWall(f, turn + 180.0 / bays + 144, "sunken");
			for (int i = 0; i < 2; i++) {
				int[] s = Floor.polar(turn + 108 + 180 * i, f.rim() - 4);
				f.spawnerIfFree(s[0], f.y, s[1], null);
			}
			f.cobwebCorners(12);
		}
	}
}
