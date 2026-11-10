package org.studioquire.faithful.buriedtowers.plan;

import static org.studioquire.faithful.buriedtowers.plan.Blocks.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * A haulage shaft cut in the rock beside the tower, outside its wall, with a track down it skipping two or three floors (the user's way down, agreed 2026-11-08). At the top a doorway through the
 * wall onto a short station: a brake (a powered rail left unpowered) holds a cart put on it, and a lever on the landing beside it lets it go, pushing it off the buffer behind; the cart itself is in a
 * chest by the doorway. The track runs in two lanes along the wall, one against it and one beyond, down in switchbacks: slopes along each lane and two flat corners at each end, as beta's carts take
 * corners only flat. At the foot a brake against a buffer stops it beside a doorway into the room below; from there the ordinary stairs and ladders lead back up to the floors skipped. Each of those
 * has a chute through the wall into the shaft, where a spawner beside it can drop what it makes onto the track: the price of the shortcut.
 *
 * <p>Everything outside the wall is lined in the wall's stuff wherever the shaft's air meets the rock, so no cave, water or lava comes in.
 */
final class CartRun extends Way {
	/**
	 * How often a tower that has room for one has a cart run: never, since the user turned it off (2026-11-08), as holes already carry the price it was for (a player who drops past floors without
	 * clearing their spawners leaves them working overhead). Was 0.35; the rest is kept for when it comes back.
	 */
	static final double ODDS = 0;
	/** Where the top station's brake and the bottom's stand along the wall, in the run's frame. */
	private static final int START = -1, END = 0;
	/** What a track cell is. */
	private static final int SLOPE = 0, FLAT = 1, CURVE = 2, BRAKE = 3;

	final Frame frame;
	private final int radius, half, laneA, laneB;
	final int top, bottom;
	/** The track from the top station's brake to the bottom's, each {x, y, z, id, meta}. */
	final List<int[]> track = new ArrayList<>();
	/** What carries the track and closes the shaft's ends: {x, y, z}. */
	private final List<int[]> solids = new ArrayList<>();
	/** Room cells inside each doorway and chute that stay clear, each {floor index, x, z}. */
	final List<int[]> keep = new ArrayList<>();
	/** The chutes from the skipped floors, each {floor index, x, y, z} of the room cell in front of it, and the way into the room from it. */
	final List<int[]> chutes = new ArrayList<>();
	int chestX, chestZ, leverX, leverY, leverZ;
	/** The world direction the cart sets off in. */
	int setOff;

	private CartRun(Floor up, Floor low, Frame frame) {
		super("cart run", up, low);
		this.frame = frame;
		Tower t = up.t;
		radius = t.radius;
		half = t.radius;
		laneA = -radius - 1;
		laneB = -radius - 2;
		top = up.stand;
		bottom = low.stand;
	}

	/** A cart run for the tower, or null: the floors it joins and the side it runs down, the first that fits. Draws only from {@code r}. */
	static CartRun choose(Tower t, Random r) {
		if (r.nextDouble() >= ODDS) return null;
		int hall = t.floors.get(0).kind == Floors.RUIN ? 1 : 0;
		List<int[]> pairs = new ArrayList<>();
		for (int a = hall + 1; a < t.floors.size(); a++) {
			for (int skip = 2; skip <= 3; skip++) {
				int b = a + skip + 1;
				if (b > t.floors.size() - 2 || a == t.gapUnder) continue;
				Floor up = t.floors.get(a);
				if (up.leave != up.stand) continue;
				pairs.add(new int[] {a, b});
			}
		}
		Collections.shuffle(pairs, r);
		int first = r.nextInt(4);
		for (int[] pair : pairs) {
			for (int k = 0; k < 4; k++) {
				CartRun run = new CartRun(t.floors.get(pair[0]), t.floors.get(pair[1]), new Frame(t.cx, t.cz, first + k));
				if (run.lay()) return run;
			}
		}
		return null;
	}

	private int x(int u, int v) {
		return frame.x(u, v);
	}

	private int z(int u, int v) {
		return frame.z(u, v);
	}

	/** Works the run out; false if it doesn't fit this side of the tower. */
	private boolean lay() {
		Tower t = up.t;
		int wall = t.wall, room = -radius + wall;
		// The doorways at each end: through the wall where it runs flat, into the room.
		if (!doorway(up, START + 1) || !doorway(up, START) || !doorway(low, END)) return false;
		int drop = top - bottom;
		for (int loops = 1; loops <= 8; loops++) {
			List<int[]> path = path(loops);
			int slopeable = 0;
			for (int[] n : path) if (n[2] == SLOPE) slopeable++;
			if (slopeable < drop) continue;
			if (build(path, drop, slopeable)) break;
			track.clear();
			solids.clear();
		}
		if (track.isEmpty()) return false;
		// The land must lie over all of it, with rock to spare.
		for (int u = -half - 1; u <= half + 1; u++) {
			for (int v = laneB - 1; v <= laneA + 1; v++) {
				if (t.depth(x(u, v), z(u, v)) == 0 && t.ground.top(x(u, v), z(u, v)) - 3 < top + 4) return false;
			}
		}
		ux = x(START + 1, room);
		uz = z(START + 1, room);
		lx = x(END, room);
		lz = z(END, room);
		x = ux;
		z = uz;
		chestX = x(START, room);
		chestZ = z(START, room);
		keep.add(new int[] {up.index, ux, uz});
		keep.add(new int[] {up.index, chestX, chestZ});
		keep.add(new int[] {low.index, lx, lz});
		setOff = frame.dir(EAST);
		chutes();
		return true;
	}

	/** Whether a doorway can go through the wall at {@code u} into the floor's room: the wall's cells there, and the room's in front of them. */
	private boolean doorway(Floor f, int u) {
		Tower t = f.t;
		for (int v = -radius; v < -radius + t.wall; v++) {
			int d = t.depth(x(u, v), z(u, v));
			if (d < 1 || d > t.wall) return false;
		}
		int rx = x(u, -radius + t.wall), rz = z(u, -radius + t.wall);
		return f.in(rx, rz) && f.kind.allows(f, rx, rz);
	}

	/**
	 * The track's cells in order, each {u, lane (0 against the wall, 1 beyond), what}: out along the wall from the top station to the end, back along the outer lane, and so on, ending on the inner
	 * lane at the bottom station.
	 */
	private List<int[]> path(int loops) {
		List<int[]> path = new ArrayList<>();
		path.add(new int[] {START, 0, BRAKE});
		path.add(new int[] {START + 1, 0, FLAT});
		for (int u = START + 2; u < half; u++) path.add(new int[] {u, 0, SLOPE});
		path.add(new int[] {half, 0, CURVE});
		for (int j = 0; j < loops; j++) {
			path.add(new int[] {half, 1, CURVE});
			for (int u = half - 1; u > -half; u--) path.add(new int[] {u, 1, SLOPE});
			path.add(new int[] {-half, 1, CURVE});
			path.add(new int[] {-half, 0, CURVE});
			if (j < loops - 1) {
				for (int u = -half + 1; u < half; u++) path.add(new int[] {u, 0, SLOPE});
				path.add(new int[] {half, 0, CURVE});
			}
		}
		for (int u = -half + 1; u < END - 1; u++) path.add(new int[] {u, 0, SLOPE});
		path.add(new int[] {END - 1, 0, FLAT});
		path.add(new int[] {END, 0, BRAKE});
		return path;
	}

	/** Spreads the drop evenly over the cells that can slope, and checks every rail has the headroom of a rider over it. */
	private boolean build(List<int[]> path, int drop, int slopeable) {
		int level = top, k = 0;
		int[] ys = new int[path.size()];
		boolean[] slope = new boolean[path.size()];
		for (int i = 0; i < path.size(); i++) {
			int[] n = path.get(i);
			if (n[2] == SLOPE) {
				slope[i] = (long) (k + 1) * drop / slopeable > (long) k * drop / slopeable;
				k++;
			}
			if (slope[i]) level--;
			ys[i] = level;
		}
		if (level != bottom) return false;
		Map<Long, Integer> taken = new HashMap<>();
		for (int i = 0; i < path.size(); i++) {
			int[] n = path.get(i);
			int v = lane(n[1]), cx = x(n[0], v), cz = z(n[0], v);
			int prev = i == 0 ? toward(n, new int[] {n[0] - 1, n[1]}) : toward(n, path.get(i - 1));
			int next = i == path.size() - 1 ? toward(n, new int[] {n[0] + 1, n[1]}) : toward(n, path.get(i + 1));
			int id = n[2] == BRAKE ? POWERED_RAIL : RAIL, meta;
			if (slope[i]) meta = ascending(prev);
			else if (n[2] == CURVE) meta = curve(prev, next);
			else meta = prev == EAST || prev == WEST ? 1 : 0;
			track.add(new int[] {cx, ys[i], cz, id, meta});
			solids.add(new int[] {cx, ys[i] - 1, cz});
		}
		// The buffers at each end, and at the top a landing beside the station on the outer lane, where the lever stands.
		solids.add(new int[] {x(START - 1, laneA), top, z(START - 1, laneA)});
		solids.add(new int[] {x(END + 1, laneA), bottom, z(END + 1, laneA)});
		for (int u = START - 1; u <= START + 1; u++) solids.add(new int[] {x(u, laneB), top - 1, z(u, laneB)});
		leverX = x(START, laneB);
		leverY = top;
		leverZ = z(START, laneB);
		for (int[] s : solids) taken.put(Plan.key(s[0], s[1], s[2]), 1);
		taken.put(Plan.key(leverX, leverY, leverZ), 2);
		for (int[] c : track) {
			for (int y = c[1] + 1; y <= c[1] + 3; y++) {
				if (taken.containsKey(Plan.key(c[0], y, c[2]))) return false;
			}
		}
		return true;
	}

	private int lane(int which) {
		return which == 0 ? laneA : laneB;
	}

	/** The world direction from a path cell to a neighbouring one. */
	private int toward(int[] from, int[] to) {
		int du = Integer.signum(to[0] - from[0]), dv = Integer.signum(lane(to[1]) - lane(from[1]));
		int d = du > 0 ? EAST : du < 0 ? WEST : dv > 0 ? SOUTH : NORTH;
		return frame.dir(d);
	}

	/** A slope rail rising toward {@code dir}. */
	private static int ascending(int dir) {
		return new int[] {2, 5, 3, 4}[dir];
	}

	/** A flat corner joining two directions. */
	private static int curve(int a, int b) {
		boolean south = a == SOUTH || b == SOUTH, east = a == EAST || b == EAST;
		return south ? east ? 6 : 7 : east ? 9 : 8;
	}

	/** A chute through the wall from each floor skipped, where the inner lane's open in front of it. */
	private void chutes() {
		Tower t = up.t;
		Map<Long, Boolean> filled = new HashMap<>();
		for (int[] s : solids) filled.put(Plan.key(s[0], s[1], s[2]), true);
		for (int i = up.index + 1; i < low.index; i++) {
			Floor f = t.floors.get(i);
			for (int u : new int[] {1, -1, 2, -2, 0}) {
				if (!doorway(f, u)) continue;
				int sx = x(u, laneA), sz = z(u, laneA);
				if (filled.containsKey(Plan.key(sx, f.stand, sz)) || filled.containsKey(Plan.key(sx, f.stand + 1, sz))) continue;
				int rx = x(u, -radius + t.wall), rz = z(u, -radius + t.wall);
				chutes.add(new int[] {i, rx, f.stand, rz, frame.dir(SOUTH)});
				keep.add(new int[] {i, rx, rz});
				break;
			}
		}
	}

	/** Whether another way placed at {@code cx, cz} would stand on a cell this one keeps clear. */
	boolean blocks(Way way, int cx, int cz) {
		for (int[] k : keep) {
			boolean onUp = k[0] == way.up.index, onLow = k[0] == way.low.index;
			if (!onUp && !onLow) continue;
			if (simple()) {
				for (int[] a : way.foot(onUp, cx, cz)) {
					if ((a[4] == CORE || a[4] == KERB) && k[1] >= a[0] && k[1] <= a[2] && k[2] >= a[1] && k[2] <= a[3]) return true;
				}
			} else if (Math.abs(k[1] - cx) <= way.reachX() && Math.abs(k[2] - cz) <= way.reachZ()) return true;
		}
		return false;
	}

	@Override
	int reachX() {
		return 0;
	}

	@Override
	int reachZ() {
		return 0;
	}

	@Override
	void placed() {
	}

	@Override
	void hold() {
		Plan p = up.p;
		for (int u = -half - 1; u <= half + 1; u++) {
			for (int v = laneB - 1; v <= laneA + 1; v++) {
				if (up.t.depth(x(u, v), z(u, v)) != 0) continue;
				for (int y = bottom - 1; y <= top + 4; y++) p.hold(x(u, v), y, z(u, v));
			}
		}
		for (int[] k : keep) {
			Floor f = up.t.floors.get(k[0]);
			for (int y = f.stand; y <= f.stand + 2; y++) p.hold(k[1], y, k[2]);
		}
	}

	@Override
	void draw() {
		Tower t = up.t;
		Plan p = up.p;
		// The shaft: lined where its air meets anything outside the tower, air through the rest.
		for (int u = -half - 1; u <= half + 1; u++) {
			for (int v = laneB - 1; v <= laneA + 1; v++) {
				int cx = x(u, v), cz = z(u, v);
				boolean inside = Math.abs(u) <= half && (v == laneA || v == laneB);
				for (int y = bottom - 1; y <= top + 4; y++) {
					boolean open = inside && y >= bottom && y <= top + 3;
					if (open) p.force(cx, y, cz, AIR);
					else if (t.depth(cx, cz) == 0) p.force(cx, y, cz, t.paletteAt(y).wall.at(cx, y, cz));
				}
			}
		}
		for (int[] s : solids) p.force(s[0], s[1], s[2], (t.style == Style.COMPLEX && s[1] != top - 1 ? Mat.of(PLANKS) : t.paletteAt(s[1]).wall).at(s[0], s[1], s[2]));
		for (int[] c : track) p.force(c[0], c[1], c[2], cell(c[3], c[4]));
		p.force(leverX, leverY, leverZ, cell(LEVER, 5));
		// Doorways at each end, and the chutes.
		door(up, START + 1, top);
		door(low, END, bottom);
		for (int[] c : chutes) {
			Floor f = t.floors.get(c[0]);
			door(f, frame.u(c[1], c[3]), f.stand);
			clearRock(c[1], c[3], c[1], c[3], f.stand, f.stand + 1);
		}
		// The cart, in a chest by the doorway at the top.
		p.force(chestX, top, chestZ, cell(CHEST, backTo(frame.dir(NORTH))));
		p.fixture(chestX, top, chestZ, "chest:cart");
		t.chests++;
		p.force(chestX, top + 1, chestZ, AIR);
	}

	private void door(Floor f, int u, int y) {
		Tower t = f.t;
		for (int v = -radius; v <= -radius + t.wall; v++) {
			for (int yy = y; yy <= y + 1; yy++) f.p.force(x(u, v), yy, z(u, v), AIR);
		}
	}

	/** {x, y, z} of the top station's brake and the bottom's, for the ride check. */
	int[] start() {
		return track.get(0);
	}

	int[] end() {
		return track.get(track.size() - 1);
	}
}
