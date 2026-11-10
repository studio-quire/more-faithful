package org.studioquire.faithful.buriedtowers.plan;

import static org.studioquire.faithful.buriedtowers.plan.Blocks.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** The outside of a tower: a round drum from the foot up, a finished top standing out a block, rings going down the body to a heavy base, rows of windows, the door, and a few stones fallen off it. */
final class Body extends Exterior {
	/** Round the walls, every this many degrees. */
	private static final int WINDOW_STEP = 30;

	private final Tower t;
	private final Plan p;
	private final Random r;
	private final Noise n;
	private final Palette outer;
	private final Mat moss;
	/** The rings round the body, flush or standing out: which, of what, and how many is this tower's own. */
	private final List<Course> courses = new ArrayList<>();
	/** The top of the base, which stands a block out from the wall from the land up. */
	private final int baseTop;
	/** What's been put on outside the wall's own columns, which {@link #dropLoose} looks over at the end. */
	private final Set<Long> outward = new HashSet<>();
	private int gateDir;

	/** A ring round the tower from {@code y0} to {@code y1}. */
	private static final class Course {
		final int y0, y1;
		final Mat mat;
		final boolean proud;

		Course(int y0, int y1, Mat mat, boolean proud) {
			this.y0 = y0;
			this.y1 = y1;
			this.mat = mat;
			this.proud = proud;
		}
	}

	Body(Tower t) {
		this.t = t;
		this.p = t.plan;
		this.r = new Random(t.seed ^ 0x5DEECE66DL);
		this.n = new Noise(t.seed ^ 0xB0D1E5L);
		this.outer = hall().pal;
		this.moss = Mat.of(t.is(Climate.DESERT) ? COBBLE : MOSSY);
		Random b = new Random(t.seed ^ 0xBA2D5L);
		this.baseTop = hall().y - 1 + b.nextInt(2);
		rings(b);
	}

	/** The second stuff a tower's rings are made of, beside its trim, by where it stands: timber lacing in the woods and the cold, plain cobble against sandstone or moss, brick or bare stone in the open. */
	private Mat accent(Random b) {
		switch (t.climate) {
			case DESERT:
				return b.nextBoolean() ? Mat.of(COBBLE) : Mat.of(DOUBLE_SLAB, STONE_SLAB);
			case COLD:
				return b.nextBoolean() ? Mat.of(LOG, 1) : Mat.of(COBBLE);
			case WOODED:
				return b.nextBoolean() ? Mat.of(LOG, 0) : Mat.of(STONE);
			case WET:
				return b.nextBoolean() ? Mat.of(COBBLE) : Mat.of(STONE);
			default:
				return b.nextBoolean() ? Mat.of(BRICK) : Mat.of(STONE);
		}
	}

	/** One of a few ways of ringing the body, all of them heavier going down, so the crown isn't the only weight on it. The hall's floor line is always the strongest. */
	private void rings(Random b) {
		Floor hall = hall(), top = t.floors.get(0);
		Mat trim = outer.trim, accent = accent(b);
		int waist = hall.top() + 1;
		// With no ruin over the hall the crown's right there, and there's less body to ring.
		if (top == hall) {
			courses.add(new Course(baseTop + 3, baseTop + 3, accent, false));
			return;
		}
		switch (b.nextInt(4)) {
			case 0: // A strong string course at the floor line and a thin one under each row of windows.
				courses.add(new Course(waist, waist, trim, true));
				courses.add(new Course(top.y + 1, top.y + 1, trim, false));
				courses.add(new Course(hall.y + 1, hall.y + 1, accent, false));
				break;
			case 1: // Twin rings about the floor line, the lower one a course deeper.
				courses.add(new Course(waist, waist, trim, true));
				courses.add(new Course(waist + 2, waist + 2, accent, false));
				courses.add(new Course(waist - 3, waist - 2, accent, false));
				break;
			case 2: // Widening as it goes down: one course high up, two at the floor line, three over the base.
				courses.add(new Course(top.y + 1, top.y + 1, accent, false));
				courses.add(new Course(waist - 1, waist, trim, false));
				courses.add(new Course(baseTop + 1, baseTop + 3, t.is(Climate.DESERT) ? accent : trim, false));
				break;
			default: // The floor line standing out with a band of the second stuff under it, and the upper storey in paler stone over the rubble below.
				courses.add(new Course(waist, waist, trim, true));
				courses.add(new Course(waist - 2, waist - 1, accent, false));
				if (!t.is(Climate.DESERT)) courses.add(new Course(waist + 1, top.top() + 2, Mat.patched(Mat.of(STONE), Mat.of(COBBLE), n, 0.3, 5), false));
				break;
		}
	}

	private Floor hall() {
		return t.floors.get(0).kind == Floors.RUIN ? t.floors.get(1) : t.floors.get(0);
	}

	private boolean chance(int x, int y, int z, int oneIn) {
		return Math.floorMod(Tower.mix(t.seed ^ y * 0x2545F491L, x, z), oneIn) == 0;
	}

	/** The top of the masonry, unbroken. */
	private int high() {
		Floor top = t.floors.get(0);
		return top.top() + (top.kind == Floors.GATEHALL ? 2 : 1);
	}

	/** The wall's height at a column above ground: level, with a block gone here and there and a stretch or two where it's come down. */
	private int wallTop(int x, int z) {
		int high = high();
		double slump = n.at(x, 0, z, 9);
		if (slump < 0.36) return Math.max(t.groundY + 4, high - Math.min(5, 1 + (int) ((0.36 - slump) * 28)));
		return chance(x, 0, z, 9) ? high - 1 : high;
	}

	void shell() {
		int foot = t.foot();
		for (int x = t.cx - t.radius; x <= t.cx + t.radius; x++) {
			for (int z = t.cz - t.radius; z <= t.cz + t.radius; z++) {
				int d = t.depth(x, z);
				if (d == 0) continue;
				for (int y = foot; y < Plan.HEIGHT; y++) p.force(x, y, z, AIR);
				int top = wallTop(x, z), land = t.ground.top(x, z);
				if (d > 2) continue;
				for (int y = foot; y <= top; y++) {
					// The outer face has lost the odd block, above the land, where weather gets at it.
					if (d == 1 && y > land + 1 && y < top && chance(x, y, z, 45)) continue;
					p.force(x, y, z, d == 1 && y >= land - 1 ? face(x, y, z, land) : inner(x, y, z));
				}
			}
		}
		floors(t);
		if (t.floors.get(0).kind == Floors.GATEHALL) roof(t.floors.get(0));
		windows();
		crown();
	}

	/** What the weather sees: the walls' stone, damp and mossy near the land, with this tower's rings round it. */
	private int face(int x, int y, int z, int land) {
		if (y <= land) return moss.at(x, y, z);
		for (Course c : courses) {
			if (y >= c.y0 && y <= c.y1) return c.mat.at(x, y, z);
		}
		if (y == baseTop) return capping();
		if (y <= land + 3 && n.at(x, y, z, 4) > 0.4 + (y - land) * 0.05) return moss.at(x, y, z);
		return outer.wall.at(x, y, z);
	}

	/** Inside: the floor's wall, with its trim at the foot and the head of the room. */
	private int inner(int x, int y, int z) {
		Floor f = t.floorAt(y);
		Palette pal = t.paletteAt(y);
		return y == f.y || y == f.top() ? pal.trim.at(x, y, z) : pal.wall.at(x, y, z);
	}

	/** A hall with nothing over it keeps most of a flat roof, with the sky through the rest; where there's a ruin above, its floor is this and it draws its own holes. */
	private void roof(Floor hall) {
		int y = hall.top() + 1;
		for (int x = t.cx - t.radius; x <= t.cx + t.radius; x++) {
			for (int z = t.cz - t.radius; z <= t.cz + t.radius; z++) {
				if (t.depth(x, z) >= 3 && !Floors.roofGone(t, x, z)) p.force(x, y, z, hall.pal.ceiling.at(x, y, z));
			}
		}
	}

	/** A row of windows at each floor above the land, evenly round: the top floor's every place, the hall's every other. */
	private void windows() {
		for (Floor f : t.floors) {
			if (f.y < t.groundY) continue;
			int step = f.kind == Floors.RUIN ? WINDOW_STEP : WINDOW_STEP * 2, tall = f.kind == Floors.RUIN ? 3 : 2;
			for (int a = f.kind == Floors.RUIN ? 0 : WINDOW_STEP; a < 360; a += step) window(a, f.y + 2, tall);
		}
	}

	/** Straight through the wall along whichever axis faces out most there: as wide as the flat run of wall it's in lets it be, three on the long runs facing the axes and two elsewhere, with a head of the trim. */
	private void window(int degrees, int y, int tall) {
		int[] c = Floor.polar(degrees, t.radius);
		boolean alongX = Math.abs(c[0]) >= Math.abs(c[1]);
		int side = alongX ? Integer.signum(c[0]) : Integer.signum(c[1]), across = alongX ? c[1] : c[0];
		if (across == 0) {
			for (int w = -1; w <= 1; w++) slit(alongX, side, w, y, tall);
			return;
		}
		slit(alongX, side, across, y, tall);
		for (int s = -1; s <= 1; s += 2) {
			if (faceAt(alongX, side, across + s) == faceAt(alongX, side, across)) {
				slit(alongX, side, across + s, y, tall);
				return;
			}
		}
	}

	/** How far out the wall's face stands in one line across the tower. */
	private int faceAt(boolean alongX, int side, int across) {
		for (int k = t.radius; k >= 0; k--) {
			if (t.depth(t.cx + (alongX ? side * k : across), t.cz + (alongX ? across : side * k)) != 0) return k;
		}
		return -1;
	}

	private void slit(boolean alongX, int side, int across, int y, int tall) {
		for (int k = t.radius; k >= 0; k--) {
			int x = t.cx + (alongX ? side * k : across), z = t.cz + (alongX ? across : side * k), depth = t.depth(x, z);
			if (depth == 0) continue;
			if (depth > 2) break;
			for (int yy = y; yy < y + tall; yy++) p.force(x, yy, z, AIR);
			if (depth == 1) p.force(x, y + tall, z, outer.trim.at(x, y + tall, z));
		}
	}

	/** The top stands a block out from the walls, two courses of the trim and a coping, wherever the wall under it has kept its height. */
	private void crown() {
		int high = high();
		for (int x = t.cx - t.radius - 1; x <= t.cx + t.radius + 1; x++) {
			for (int z = t.cz - t.radius - 1; z <= t.cz + t.radius + 1; z++) {
				int d = t.depth(x, z);
				if (d == 1 || d == 2) {
					if (wallTop(x, z) == high && !chance(x, 3, z, 7)) p.force(x, high + 1, z, cell(SLAB, outer.slab));
					continue;
				}
				if (d != 0) continue;
				int under = 0;
				for (int k = 0; k < 4; k++) {
					if (t.depth(x + DX[k], z + DZ[k]) == 1 && wallTop(x + DX[k], z + DZ[k]) >= high - 1) under++;
				}
				if (under == 0) continue;
				// Brick all the way up is a heavy hat on sandstone: only the lower course of it there.
				for (int y = high - 1; y <= high; y++) {
					if (!chance(x, y, z, 12)) put(x, y, z, y == high && t.is(Climate.DESERT) ? outer.wall.at(x, y, z) : outer.trim.at(x, y, z));
				}
			}
		}
	}

	/** A gate with a frame of the trim on the side where the land comes nearest the hall's floor, cut or stepped so a player walks in. */
	void entrance() {
		Floor hall = hall();
		int best = 0;
		int bestScore = Integer.MAX_VALUE;
		for (int d = 0; d < 4; d++) {
			int score = 0;
			for (int k = 0; k <= Tower.APRON; k++) score += Math.abs(t.ground.top(t.cx + DX[d] * (t.radius + k), t.cz + DZ[d] * (t.radius + k)) - hall.y);
			score = score * 3 + r.nextInt(5);
			if (score < bestScore) {
				bestScore = score;
				best = d;
			}
		}
		gateDir = best;
		int across = best + 1 & 3;
		Random path = new Random(t.seed ^ 0x70617468L);
		int length = 3 + path.nextInt(3);
		// In from the inside until the wall ends, out along the path.
		for (int w = -1; w <= 1; w++) {
			int ox = DX[across] * w, oz = DZ[across] * w;
			int[] levels = pathLevels(t, hall.y, best, ox, oz, t.ground::surface);
			for (int k = t.radius + Tower.APRON; k >= 0; k--) {
				int x = t.cx + DX[best] * k + ox, z = t.cz + DZ[best] * k + oz;
				int d = t.depth(x, z);
				if (d >= 3) {
					// Whatever the hall has against its wall stops short of the gate.
					if (!p.held(x, hall.y, z)) for (int y = hall.y; y < hall.y + 3; y++) p.force(x, y, z, AIR);
					break;
				}
				if (d >= 1) {
					for (int y = hall.y; y < hall.y + 3; y++) p.force(x, y, z, AIR);
					p.force(x, hall.y - 1, z, hall.kind.underfoot(hall).at(x, hall.y - 1, z));
				} else {
					int land = t.ground.surface(x, z), out = k - t.radius, level = levels[k];
					for (int y = level; y <= land + 2; y++) p.force(x, y, z, AIR);
					int top = pathTop(t, path, length, out, r.nextInt(4) == 0 ? cell(MOSSY, 0) : cell(COBBLE, 0));
					for (int y = land; y < level; y++) p.force(x, y, z, pathFill(t, id(top) != GRASS && id(top) != SAND));
					p.force(x, level - 1, z, top);
				}
			}
		}
		// The frame: jambs and a head of the trim on the outer face, and the head through the wall's thickness.
		for (int w = -2; w <= 2; w++) {
			int ox = DX[across] * w, oz = DZ[across] * w;
			for (int k = t.radius; k >= t.radius - 3; k--) {
				int x = t.cx + DX[best] * k + ox, z = t.cz + DZ[best] * k + oz, d = t.depth(x, z);
				if (d != 1 && d != 2) continue;
				if (Math.abs(w) == 2 && d == 1) for (int y = hall.y; y <= hall.y + 3; y++) p.force(x, y, z, outer.trim.at(x, y, z));
				if (Math.abs(w) < 2 || d == 1) p.force(x, hall.y + 3, z, outer.trim.at(x, hall.y + 3, z));
				if (d == 1) p.force(x, hall.y + 4, z, outer.trim.at(x, hall.y + 4, z));
			}
		}
		hall.safeX = t.cx + DX[best] * (t.radius - 4);
		hall.safeZ = t.cz + DZ[best] * (t.radius - 4);
		hall.safeY = hall.stand;
		t.gateX = t.cx + DX[best] * (t.radius + 2);
		t.gateZ = t.cz + DZ[best] * (t.radius + 2);
	}

	/** What stands out from the wall below the crown: the proud rings, carried on the wall behind them with a chamfer of slabs on top, and the base, level along its top whatever the land does and stopping at the gate's frame. */
	void dress() {
		int reach = t.radius + 1, slab = cell(SLAB, outer.slab);
		for (int x = t.cx - reach; x <= t.cx + reach; x++) {
			for (int z = t.cz - reach; z <= t.cz + reach; z++) {
				if (t.depth(x, z) != 0 || !besideWall(x, z)) continue;
				int land = t.ground.top(x, z);
				for (Course c : courses) {
					if (!c.proud) continue;
					for (int y = c.y0; y <= c.y1; y++) {
						if (y >= land && backed(x, y, z)) put(x, y, z, c.mat.at(x, y, z));
					}
					if (c.y1 >= land && backed(x, c.y1, z) && backed(x, c.y1 + 1, z)) put(x, c.y1 + 1, z, slab);
				}
				if (atGate(x, z)) continue;
				// From the land as beta's caves leave it, so over a cave's mouth it goes down into it rather than standing on air.
				for (int y = Math.min(land, t.ground.surface(x, z)); y <= baseTop; y++) put(x, y, z, y == baseTop ? capping() : moss.at(x, y, z));
				if (land <= baseTop + 1) put(x, baseTop + 1, z, slab);
			}
		}
	}

	private void put(int x, int y, int z, int cell) {
		p.force(x, y, z, cell);
		outward.add(Plan.key(x, y, z));
	}

	private boolean besideWall(int x, int z) {
		for (int k = 0; k < 4; k++) {
			if (t.depth(x + DX[k], z + DZ[k]) == 1) return true;
		}
		return false;
	}

	/** The top course of the base: the trim, or not brick where the walls are pale, which would make three stripes of it. */
	private int capping() {
		return t.is(Climate.DESERT) ? cell(DOUBLE_SLAB, STONE_SLAB) : outer.trim.at(0, 0, 0);
	}

	/** Whether the wall beside a place outside it is there at this height. */
	private boolean backed(int x, int y, int z) {
		for (int k = 0; k < 4; k++) {
			int ax = x + DX[k], az = z + DZ[k];
			if (t.depth(ax, az) == 1 && solid(p.id(ax, y, az))) return true;
		}
		return false;
	}

	private boolean atGate(int x, int z) {
		int along = (x - t.cx) * DX[gateDir] + (z - t.cz) * DZ[gateDir], across = Math.abs((x - t.cx) * DZ[gateDir] - (z - t.cz) * DX[gateDir]);
		return along > 0 && across <= 2;
	}

	/** Takes off anything outside the wall's columns with nothing left holding it, as when a piece of the crown is over a stretch of wall that's gone. Last of all, after {@link Tower#settle}. */
	void dropLoose() {
		Set<Long> held = new HashSet<>();
		Deque<Long> queue = new ArrayDeque<>();
		for (long key : outward) {
			int x = Plan.keyX(key), y = Plan.keyY(key), z = Plan.keyZ(key);
			if (!bears(x, y, z)) continue;
			boolean anchored = y < t.ground.top(x, z) + 1;
			for (int k = 0; k < 4 && !anchored; k++) anchored = t.depth(x + DX[k], z + DZ[k]) != 0 && bears(x + DX[k], y, z + DZ[k]);
			if (anchored && held.add(key)) queue.add(key);
		}
		int[][] steps = {{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}, {0, 1, 0}, {0, -1, 0}};
		while (!queue.isEmpty()) {
			long key = queue.remove();
			for (int[] s : steps) {
				long next = Plan.key(Plan.keyX(key) + s[0], Plan.keyY(key) + s[1], Plan.keyZ(key) + s[2]);
				if (outward.contains(next) && bears(Plan.keyX(next), Plan.keyY(next), Plan.keyZ(next)) && held.add(next)) queue.add(next);
			}
		}
		for (long key : outward) {
			if (!held.contains(key) && bears(Plan.keyX(key), Plan.keyY(key), Plan.keyZ(key))) p.force(Plan.keyX(key), Plan.keyY(key), Plan.keyZ(key), AIR);
		}
	}

	private boolean bears(int x, int y, int z) {
		int c = p.get(x, y, z);
		return c != Plan.KEEP && !passable(id(c));
	}

	/** A few fallen stones on the land right under the walls, where a wall has come down. */
	void apron() {
		for (int x = p.ox; x < p.ox + p.size; x++) {
			for (int z = p.oz; z < p.oz + p.size; z++) {
				if (t.depth(x, z) != 0) continue;
				double far = Math.hypot(x - t.cx, z - t.cz) - t.radius;
				if (far < 0 || far > 4) continue;
				if (r.nextDouble() > (0.42 - n.at(x, 0, z, 9)) * 0.6) continue;
				int land = t.ground.surface(x, z);
				if (land < Ground.SEA || p.get(x, land, z) != Plan.KEEP) continue;
				int roll = r.nextInt(6);
				p.loose(x, land, z, roll == 0 ? cell(SLAB, outer.slab) : roll == 1 ? moss.at(x, land, z) : outer.wall.at(x, land, z));
			}
		}
	}
}
