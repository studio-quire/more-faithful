package org.studioquire.faithful.buriedtowers.plan;

import static org.studioquire.faithful.buriedtowers.plan.Blocks.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** One level of a tower while it's drawn: its place in the column, what it's made of, and the calls every floor needs. Cells it's asked to write outside its walls, or held for a way down, stay as they are. */
public final class Floor {
	final Tower t;
	final Plan p;
	final Random r;
	final Palette pal;
	final FloorKind kind;
	public final int index;
	/** The lowest air cell and how many are clear above it. */
	public final int y, h;
	/** The feet's level where you come and go: {@link #y}, unless the floor stands on a raised walk. */
	public int stand;
	/** The feet's level where the way on starts: {@link #stand}, unless it starts from the bottom of a pit. */
	public int leave;
	/** The top of the water it stands in, or -1. A fall into it needs no pool. */
	int waterTop = -1;
	/** Set in from the walls by this much, above ground. */
	int inset;
	public Way in, out;
	/** A cart run coming down to this floor past the ones over it, besides the way in from the floor above. */
	public Way arrive;
	/** Fraction of the way down the tower, 0 at the land. */
	public final double depth;
	/** The middle of the floor is what a floor is drawn around. */
	final Frame frame;
	/** A place to stand: where you land, once ways are drawn. */
	public int safeX, safeY, safeZ;
	/** What has to be reachable from where you land, besides the way on, if anything: a door, say. */
	int goalX, goalZ;
	boolean hasGoal;
	final List<String> notes = new ArrayList<>();

	Floor(Tower t, FloorKind kind, int index, int y, int h, double depth) {
		this.t = t;
		this.p = t.plan;
		this.kind = kind;
		this.index = index;
		this.y = y;
		this.h = h;
		this.stand = y;
		this.leave = y;
		this.depth = depth;
		this.r = new Random(t.seed * 31 + index * 0x9E3779B97F4A7C15L);
		this.pal = t.style == Style.COMPLEX ? Palette.at(depth, t.climate, t.noise) : Palette.simple(depth, t.climate, t.noise);
		this.frame = new Frame(t.cx, t.cz, r.nextInt(4));
	}

	public String kindName() {
		return kind.name;
	}

	public int top() {
		return y + h - 1;
	}

	boolean in(int x, int z) {
		return t.depth(x, z) > t.wall + inset;
	}

	/** The half width of what's inside, along either axis, before the walls' shape cuts it. */
	int half() {
		return t.radius - t.wall - inset;
	}

	/** How far the floor's inside reaches from the tower's middle, as a circle. */
	double rim() {
		return t.radius + 0.5 - t.wall - inset;
	}

	/** A place in the floor's frame at an angle and a distance from the middle, rounding the same way in every quarter so a ring of things comes out even. */
	static int[] polar(double degrees, double far) {
		double a = Math.toRadians(degrees);
		return new int[] {even(Math.cos(a) * far), even(Math.sin(a) * far)};
	}

	private static int even(double v) {
		return (int) (Math.signum(v) * Math.round(Math.abs(v) - 1e-9));
	}

	/** The last cell inside the wall at an angle. */
	int[] against(double degrees) {
		for (double far = rim() + 2; far > 0; far -= 0.25) {
			int[] c = polar(degrees, far);
			if (inFrame(c[0], c[1])) return c;
		}
		return new int[] {0, 0};
	}

	/** The frame's direction from a place toward the nearer wall. */
	static int outward(int u, int v) {
		return Math.abs(u) >= Math.abs(v) ? u >= 0 ? EAST : WEST : v >= 0 ? SOUTH : NORTH;
	}

	/** A ring one cell thick on a circle, passing round whatever a way holds. */
	void ring(double far, int yy, Mat m) {
		int reach = (int) far + 2;
		for (int u = -reach; u <= reach; u++) {
			for (int v = -reach; v <= reach; v++) {
				double at = Math.hypot(u, v);
				if (at >= far - 0.5 && at < far + 0.5) put(u, yy, v, m);
			}
		}
	}

	/** A beam from one place to another, joined face to face at every step so it holds together. Whole or not at all: half a beam left by a way, hanging in the air, is the one thing it mustn't be. */
	boolean beam(int u0, int v0, int u1, int v1, int yy, Mat m) {
		List<int[]> cells = line(u0, v0, u1, v1);
		for (int[] c : cells) {
			if (!inFrame(c[0], c[1]) || p.held(frame.x(c[0], c[1]), yy, frame.z(c[0], c[1]))) return false;
		}
		for (int[] c : cells) put(c[0], yy, c[1], m);
		return true;
	}

	void set(int x, int yy, int z, int cell) {
		if (in(x, z)) p.set(x, yy, z, cell);
	}

	void set(int x, int yy, int z, Mat m) {
		if (in(x, z)) p.set(x, yy, z, m.at(x, yy, z));
	}

	void fill(int x0, int y0, int z0, int x1, int y1, int z1, Mat m) {
		for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
			for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
				for (int yy = Math.min(y0, y1); yy <= Math.max(y0, y1); yy++) set(x, yy, z, m);
			}
		}
	}

	/** In the floor's own frame. */
	void put(int u, int yy, int v, int cell) {
		set(frame.x(u, v), yy, frame.z(u, v), cell);
	}

	void put(int u, int yy, int v, Mat m) {
		set(frame.x(u, v), yy, frame.z(u, v), m);
	}

	void box(int u0, int y0, int v0, int u1, int y1, int v1, Mat m) {
		for (int u = Math.min(u0, u1); u <= Math.max(u0, u1); u++) {
			for (int v = Math.min(v0, v1); v <= Math.max(v0, v1); v++) {
				for (int yy = Math.min(y0, y1); yy <= Math.max(y0, y1); yy++) put(u, yy, v, m);
			}
		}
	}

	void box(int u0, int y0, int v0, int u1, int y1, int v1, int cell) {
		box(u0, y0, v0, u1, y1, v1, Mat.of(id(cell), meta(cell)));
	}

	/** A column or pier, floor to {@code topY}, unless a way holds any of its cells: half a pier hanging over a stair would make no sense. Says whether it went in. */
	boolean pier(int u0, int v0, int u1, int v1, int topY, Mat m) {
		for (int u = Math.min(u0, u1); u <= Math.max(u0, u1); u++) {
			for (int v = Math.min(v0, v1); v <= Math.max(v0, v1); v++) {
				for (int yy = y; yy <= topY; yy++) {
					if (p.held(frame.x(u, v), yy, frame.z(u, v))) return false;
				}
			}
		}
		box(u0, y, v0, u1, topY, v1, m);
		return true;
	}

	/** Whether nothing's held for a way within this far of the floor's middle, floor to ceiling. */
	boolean clearOf(double far) {
		int reach = (int) far + 1;
		for (int u = -reach; u <= reach; u++) {
			for (int v = -reach; v <= reach; v++) {
				if (Math.hypot(u, v) >= far) continue;
				for (int yy = y; yy <= top(); yy++) {
					if (p.held(frame.x(u, v), yy, frame.z(u, v))) return false;
				}
			}
		}
		return true;
	}

	/** A flat disc about a place in the floor's frame. */
	void disc(double cu, double cv, double far, int yy, Mat m) {
		int reach = (int) far + 2;
		for (int u = (int) cu - reach; u <= (int) cu + reach; u++) {
			for (int v = (int) cv - reach; v <= (int) cv + reach; v++) {
				if (Math.hypot(u - cu, v - cv) < far) put(u, yy, v, m);
			}
		}
	}

	/** A round column about a place in the floor's frame. */
	void column(double cu, double cv, double far, int y0, int y1, Mat m) {
		for (int yy = y0; yy <= y1; yy++) disc(cu, cv, far, yy, m);
	}

	/** The cells of a line from one place to another, joined face to face. */
	List<int[]> line(int u0, int v0, int u1, int v1) {
		List<int[]> cells = new ArrayList<>();
		int su = Integer.signum(u1 - u0), sv = Integer.signum(v1 - v0), u = u0, v = v0;
		cells.add(new int[] {u, v});
		while (u != u1 || v != v1) {
			double alongU = u == u1 ? Double.MAX_VALUE : Math.abs((u + su - u0) * (double) (v1 - v0) - (v - v0) * (double) (u1 - u0));
			double alongV = v == v1 ? Double.MAX_VALUE : Math.abs((u - u0) * (double) (v1 - v0) - (v + sv - v0) * (double) (u1 - u0));
			if (alongU <= alongV) u += su;
			else v += sv;
			cells.add(new int[] {u, v});
		}
		return cells;
	}

	/** The place one in from a place at the wall that lies {@code dir} of it. */
	int[] nextTo(int[] at, int dir) {
		return new int[] {at[0] - DX[dir], at[1] - DZ[dir]};
	}

	/** A course of something round the room's wall, on the skin of it, at a height. */
	void wallCourse(int yy, Mat m) {
		for (int x = t.cx - t.radius; x <= t.cx + t.radius; x++) {
			for (int z = t.cz - t.radius; z <= t.cz + t.radius; z++) {
				if (t.depth(x, z) == t.wall) p.force(x, yy, z, m.at(x, yy, z));
			}
		}
	}

	/** Solid to begin with, ways' cells and all: a way draws what it needs over it. */
	void rock(int u0, int y0, int v0, int u1, int y1, int v1, Mat m) {
		for (int u = Math.min(u0, u1); u <= Math.max(u0, u1); u++) {
			for (int v = Math.min(v0, v1); v <= Math.max(v0, v1); v++) {
				int x = frame.x(u, v), z = frame.z(u, v);
				if (!in(x, z)) continue;
				for (int yy = Math.min(y0, y1); yy <= Math.max(y0, y1); yy++) p.rock(x, yy, z, m.at(x, yy, z));
			}
		}
	}

	/** Cuts a room or a passage out of what {@link #rock} left, ways' cells and all. */
	void cut(int u0, int y0, int v0, int u1, int y1, int v1) {
		for (int u = Math.min(u0, u1); u <= Math.max(u0, u1); u++) {
			for (int v = Math.min(v0, v1); v <= Math.max(v0, v1); v++) {
				int x = frame.x(u, v), z = frame.z(u, v);
				if (!in(x, z)) continue;
				for (int yy = Math.min(y0, y1); yy <= Math.max(y0, y1); yy++) p.force(x, yy, z, AIR);
			}
		}
	}

	boolean inFrame(int u, int v) {
		return in(frame.x(u, v), frame.z(u, v));
	}

	int idAt(int u, int yy, int v) {
		return p.id(frame.x(u, v), yy, frame.z(u, v));
	}

	/** Every cell of the floor, from the boards up to the ceiling's underside, taken back to empty air over the shell's floor. */
	void clear() {
		for (int x = t.cx - t.radius; x <= t.cx + t.radius; x++) {
			for (int z = t.cz - t.radius; z <= t.cz + t.radius; z++) {
				for (int yy = y; yy <= top(); yy++) set(x, yy, z, AIR);
			}
		}
	}

	void chest(int x, int yy, int z, int back, String loot) {
		if (!in(x, z) || p.held(x, yy, z)) return;
		p.set(x, yy, z, cell(CHEST, backTo(back)));
		p.fixture(x, yy, z, "chest:" + loot);
		t.chests++;
	}

	void chestAt(int u, int yy, int v, int back, String loot) {
		chest(frame.x(u, v), yy, frame.z(u, v), frame.dir(back), loot);
	}

	void spawner(int x, int yy, int z) {
		spawner(x, yy, z, t.monster(r, depth));
	}

	void spawner(int x, int yy, int z, String mob) {
		if (!in(x, z) || p.held(x, yy, z)) return;
		p.set(x, yy, z, cell(SPAWNER, 0));
		p.fixture(x, yy, z, "spawner:" + mob);
		t.spawners++;
	}

	void spawnerAt(int u, int yy, int v) {
		spawner(frame.x(u, v), yy, frame.z(u, v));
	}

	void spawnerAt(int u, int yy, int v, String mob) {
		spawner(frame.x(u, v), yy, frame.z(u, v), mob);
	}

	/** Only on an empty cell with room over it, so it never takes the place of something the floor's put there. */
	void spawnerIfFree(int u, int yy, int v, String mob) {
		if (idAt(u, yy, v) == AIR && idAt(u, yy + 1, v) == AIR) spawner(frame.x(u, v), yy, frame.z(u, v), mob == null ? t.monster(r, depth) : mob);
	}

	/** A heap of fallen stuff, sloping away from a middle, resting on whatever's there. */
	void rubble(int x, int z, int radius, Mat m) {
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				int rise = radius - (Math.abs(dx) + Math.abs(dz));
				if (rise < 0 || rise == 0 && r.nextInt(3) != 0) continue;
				int tall = Math.min(rise, 1 + r.nextInt(radius + 1));
				for (int k = 0, top = stackBase(x + dx, z + dz); k < tall; k++) {
					if (top + k > top() || p.id(x + dx, top + k, z + dz) != AIR) break;
					set(x + dx, top + k, z + dz, m);
				}
			}
		}
	}

	/** The first empty cell up from the floor in a column. */
	int stackBase(int x, int z) {
		int yy = y;
		while (yy <= top() && p.id(x, yy, z) != AIR) yy++;
		return yy;
	}

	/** Gravel, with cobble mixed through it and the odd fallen flag. */
	Mat rubbleMat() {
		return (x, yy, z) -> {
			int roll = (int) (t.noise.at(x * 7.3, yy * 5.1, z * 6.7, 1) * 10);
			if (roll < 5) return cell(GRAVEL, 0);
			return roll < 8 ? cell(COBBLE, 0) : cell(MOSSY, 0);
		};
	}

	/** What's under boards a fire's had: bare cobble and gravel. */
	Mat burnt() {
		return (x, yy, z) -> (int) (t.noise.at(x * 5.7, yy * 3.3, z * 6.1, 1) * 10) < 7 ? cell(COBBLE, 0) : cell(GRAVEL, 0);
	}

	/** The last cell inside the walls, going {@code dir} from a place on the floor, in the floor's frame. */
	int[] toWall(int u, int v, int dir) {
		while (inFrame(u + DX[dir], v + DZ[dir])) {
			u += DX[dir];
			v += DZ[dir];
		}
		return new int[] {u, v};
	}

	/**
	 * Whether lava already drawn could set fire beside a cell, in the floor's frame. Beta's still lava, on a random tick, takes up to two steps, each a cell up and at most one across either
	 * way, and lights the first empty cell it lands in that has anything burning beside it (anything of wood, a chest too). This ignores whatever might stop the steps, as ways drawn later can
	 * open a room up; {@code tools/tower_audit.py} checks the finished tower with them.
	 */
	boolean lavaReaches(int u, int yy, int v) {
		int x = frame.x(u, v), z = frame.z(u, v);
		for (int side = 0; side < 6; side++) {
			int nx = x + (side < 4 ? DX[side] : 0), ny = yy + (side == 4 ? 1 : side == 5 ? -1 : 0), nz = z + (side < 4 ? DZ[side] : 0);
			for (int k = 1; k <= 2; k++) {
				for (int a = -k; a <= k; a++) {
					for (int b = -k; b <= k; b++) {
						int id = p.id(nx - a, ny - k, nz - b);
						if (id == LAVA || id == FLOWING_LAVA) return true;
					}
				}
			}
		}
		return false;
	}

	/** A chest with its back to the wall that lies {@code dir} of a place on the floor. */
	void chestOnWall(int u, int yy, int v, int dir, String loot) {
		int[] c = toWall(u, v, dir);
		chestAt(c[0], yy, c[1], dir, loot);
	}

	/** A web in a high corner, or wherever there's room. */
	void web(int x, int yy, int z) {
		if (in(x, z) && p.id(x, yy, z) == AIR) p.set(x, yy, z, cell(WEB, 0));
	}

	/** Webs and hanging dirt where the walls meet the ceiling, a little more with depth. */
	void cobwebCorners(int tries) {
		int half = half();
		for (int i = 0; i < tries; i++) {
			int x = t.cx - half + r.nextInt(half * 2 + 1), z = t.cz - half + r.nextInt(half * 2 + 1), yy = top() - r.nextInt(2);
			if (p.id(x, yy, z) != AIR || !in(x, z)) continue;
			int around = 0;
			for (int d = 0; d < 4; d++) if (solid(p.id(x + DX[d], yy, z + DZ[d]))) around++;
			if (around >= 1 && solid(p.id(x, yy + 1, z))) web(x, yy, z);
		}
	}
}
