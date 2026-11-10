package org.studioquire.faithful.buriedtowers.plan;

import java.util.ArrayList;
import java.util.List;

/** How a player gets from one floor to the next. It holds its cells before the floors are drawn, and draws over them after. */
public abstract class Way {
	public final String kind;
	public final Floor up, low;
	/** Its middle. */
	int x, z;
	/** Where you step in on the floor above, and where you end up on the floor below. */
	public int ux, uz, lx, lz;

	Way(String kind, Floor up, Floor low) {
		this.kind = kind;
		this.up = up;
		this.low = low;
	}

	/** Half its size on the plan, each way. */
	abstract int reachX();

	abstract int reachZ();

	/** What a cell of a simple tower's way is, for fitting ways into small rooms: what it builds, a kerb of it that may be the tower's wall, floor kept clear round it, and where you step on or off. */
	static final int CORE = 0, KERB = 1, AREA = 2, NEED = 3;

	/**
	 * A simple tower's ways fit by what they really take on the floor above ({@code onUp}) and on the floor below, so two can share a small room: rectangles {x0, z0, x1, z1, kind} about a middle at
	 * {@code cx, cz}. What it builds and where you step on or off have to be in the room, where the floor allows; its kerbs may be the tower's wall, and the floor round it the wall or anywhere in the
	 * room, whatever the floor has there giving way to it. Another way may share the floor round it and where
	 * you step, but nothing of it may stand on what this one builds. By default all of its reach is built on.
	 */
	List<int[]> foot(boolean onUp, int cx, int cz) {
		List<int[]> foot = new ArrayList<>();
		foot.add(new int[] {cx - reachX(), cz - reachZ(), cx + reachX(), cz + reachZ(), CORE});
		return foot;
	}

	static int[] rect(int x0, int z0, int x1, int z1, int kind) {
		return new int[] {x0, z0, x1, z1, kind};
	}

	/** A square's ring, its corners off, as four rectangles. */
	static void ring(List<int[]> foot, int cx, int cz, int r, int kind) {
		foot.add(rect(cx - r + 1, cz - r, cx + r - 1, cz - r, kind));
		foot.add(rect(cx - r + 1, cz + r, cx + r - 1, cz + r, kind));
		foot.add(rect(cx - r, cz - r + 1, cx - r, cz + r - 1, kind));
		foot.add(rect(cx + r, cz - r + 1, cx + r, cz + r - 1, kind));
	}

	final boolean simple() {
		return up.t.style == Style.SIMPLE;
	}

	boolean fits(int cx, int cz) {
		if (simple()) return fitsOn(up, foot(true, cx, cz)) && fitsOn(low, foot(false, cx, cz));
		for (int ax = cx - reachX(); ax <= cx + reachX(); ax++) {
			for (int az = cz - reachZ(); az <= cz + reachZ(); az++) {
				if (!up.in(ax, az) || !low.in(ax, az) || !up.kind.allows(up, ax, az) || !low.kind.allows(low, ax, az)) return false;
			}
		}
		return true;
	}

	private static boolean fitsOn(Floor f, List<int[]> foot) {
		for (int[] a : foot) {
			boolean wallOk = a[4] == KERB || a[4] == AREA;
			for (int ax = a[0]; ax <= a[2]; ax++) {
				for (int az = a[1]; az <= a[3]; az++) {
					if (f.in(ax, az) ? a[4] != AREA && !f.kind.allows(f, ax, az) : !wallOk || f.t.depth(ax, az) == 0) return false;
				}
			}
		}
		return true;
	}

	/** Whether two ways' feet on one floor clash: anything of either on what the other builds. */
	static boolean clash(List<int[]> a, List<int[]> b) {
		for (int[] p : a) {
			for (int[] q : b) {
				boolean built = p[4] == CORE || p[4] == KERB || q[4] == CORE || q[4] == KERB;
				if (built && p[0] <= q[2] && q[0] <= p[2] && p[1] <= q[3] && q[1] <= p[3]) return true;
			}
		}
		return false;
	}

	/** Holds every rectangle of a foot. */
	final void hold(List<int[]> foot, int y0, int y1) {
		for (int[] a : foot) hold(a[0], a[1], a[2], a[3], y0, y1);
	}

	/** How many of its cells would stand on what the two floors have in their middles. */
	final int onHearts(int cx, int cz) {
		double a = up.kind.heart(up), b = low.kind.heart(low);
		int count = 0;
		for (int ax = cx - reachX(); ax <= cx + reachX(); ax++) {
			for (int az = cz - reachZ(); az <= cz + reachZ(); az++) {
				double far = Math.hypot(ax - up.t.cx, az - up.t.cz);
				if (far < a) count++;
				if (far < b) count++;
			}
		}
		return count;
	}

	final void at(int cx, int cz) {
		x = cx;
		z = cz;
		placed();
	}

	abstract void placed();

	abstract void hold();

	abstract void draw();

	/** Holds a rectangle of columns from {@code y0} to {@code y1}. */
	final void hold(int x0, int z0, int x1, int z1, int y0, int y1) {
		for (int ax = x0; ax <= x1; ax++) {
			for (int az = z0; az <= z1; az++) {
				for (int y = y0; y <= y1; y++) up.p.hold(ax, y, az);
			}
		}
	}

	/** What's left in a way's cells of a floor that was solid: gone from a rectangle. */
	final void clearRock(int x0, int z0, int x1, int z1, int y0, int y1) {
		for (int ax = x0; ax <= x1; ax++) {
			for (int az = z0; az <= z1; az++) {
				for (int y = y0; y <= y1; y++) {
					if (up.p.isRock(ax, y, az)) up.p.force(ax, y, az, Blocks.AIR);
				}
			}
		}
	}

	/** Somewhere to stand at each end, on a floor that was solid. */
	final void opened() {
		clearRock(ux, uz, ux, uz, up.stand, up.stand + 1);
		clearRock(lx, lz, lx, lz, low.stand, low.stand + 1);
	}

	/** A kerb round a rectangle at a raised level of a floor, so what's held for a shaft is still somewhere to walk: its corners off, so it isn't a square in a round room. */
	final void walkway(Floor f, int level, int x0, int z0, int x1, int z1) {
		if (level == f.y - 1) return;
		for (int ax = x0; ax <= x1; ax++) {
			for (int az = z0; az <= z1; az++) {
				if (ax > x0 && ax < x1 && az > z0 && az < z1) continue;
				if ((ax == x0 || ax == x1) && (az == z0 || az == z1) && x1 - x0 > 3 && z1 - z0 > 3) continue;
				f.p.force(ax, level, az, f.pal.trim.at(ax, level, az));
			}
		}
	}

	/**
	 * A floor of planks at a raised level over the open part of a floor, its corners off, with a rail round the open side and posts under its corners where they'd come down in the open. A simple
	 * tower's is plain: no rail, and posts of the wall's own stuff.
	 */
	final void stage(Floor f, int level, int x0, int z0, int x1, int z1, int ix0, int iz0, int ix1, int iz1) {
		Plan p = f.p;
		boolean plain = f.t.style == Style.SIMPLE;
		for (int ax = x0; ax <= x1; ax++) {
			for (int az = z0; az <= z1; az++) {
				boolean corner = (ax == x0 || ax == x1) && (az == z0 || az == z1) && x1 - x0 > 3 && z1 - z0 > 3;
				boolean open = f.kind.openAt(f, ax, az);
				if (!corner || !open) p.force(ax, level, az, corner ? f.pal.trim.at(ax, level, az) : Blocks.cell(Blocks.PLANKS, 0));
				if (corner) continue;
				boolean edge = ax == x0 || ax == x1 || az == z0 || az == z1;
				if (edge && open && !plain && Blocks.id(p.get(ax, level + 1, az)) == Blocks.AIR) p.force(ax, level + 1, az, Blocks.cell(Blocks.FENCE, 0));
			}
		}
		for (int ax : new int[] {ix0, ix1}) {
			for (int az : new int[] {iz0, iz1}) {
				if (!f.kind.openAt(f, ax, az)) continue;
				for (int y = level - 1; y >= f.y; y--) {
					int id = p.id(ax, y, az);
					if (id != Blocks.AIR && id != Blocks.WATER) break;
					p.force(ax, y, az, (plain ? f.pal.wall : f.pal.beam).at(ax, y, az));
				}
			}
		}
	}
}
