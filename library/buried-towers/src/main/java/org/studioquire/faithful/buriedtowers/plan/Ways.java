package org.studioquire.faithful.buriedtowers.plan;

import static org.studioquire.faithful.buriedtowers.plan.Blocks.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** The ways down. Each is one thing a person would really build or leave behind, sized to the drop it has to cross. */
final class Ways {
	private Ways() {
	}

	/**
	 * The ways that could join these two floors, best first, for the first that fits to take it. About half are plain ways, stairs or now and then a ladder, and half one of the others, each kind
	 * once in a tower, in any order (the user, 2026-11-07: a tower with a good few ladders and stairs is the one to make typical, the others the occasional surprise). Stairs can always be built
	 * again, in a different stone; a ladder never straight after another, and at most two. If nothing finds room, a ladder is the smallest thing there is.
	 */
	static List<Way> options(Random r, Floor up, Floor low, List<String> used, boolean ladderAllowed) {
		List<String> kinds = new ArrayList<>();
		for (String k : new String[] {"hole", "well", "gravel plug", "wall climb", "crumbling stairs"}) {
			if (!used.contains(k) && suits(k, up, low)) kinds.add(k);
		}
		Collections.shuffle(kinds, r);
		boolean ladder = ladderAllowed && r.nextInt(100) < LADDER_ODDS, plain = r.nextInt(100) >= SPECIAL_ODDS;
		List<Way> ways = new ArrayList<>();
		if (plain) ways.add(ladder ? new Ladder(up, low) : new Stairs(up, low, "stairs"));
		for (String k : kinds) ways.add(make(k, up, low));
		if (!plain) ways.add(ladder ? new Ladder(up, low) : new Stairs(up, low, "stairs"));
		if (ladder) ways.add(new Stairs(up, low, "stairs"));
		else if (ladderAllowed) ways.add(new Ladder(up, low));
		ways.add(new Ladder(up, low));
		return ways;
	}

	/** Percent of ways that try one of the special kinds first, and of plain ways that are ladders where one may go. */
	private static final int SPECIAL_ODDS = 65, LADDER_ODDS = 35;

	private static boolean suits(String kind, Floor up, Floor low) {
		// Where the way starts is the floor's own lowest level: a spiral gallery's floor, though it's come into higher up, but not a pit's foot or a walk raised over water or sand.
		boolean flatAbove = up.leave == up.y;
		int drop = up.leave - low.stand;
		switch (kind) {
			case "crumbling stairs":
				// Short drops only, which keeps it to about the share the old straight flight had (4.5% of a simple tower's ways, 6% of a complex one's; now 5.7% and 6.4%).
				return flatAbove && drop <= (up.t.style == Style.SIMPLE ? up.half() * 2 - 2 : up.half() - 1) && low.waterTop < 0 && low.stand == low.y;
			case "hole":
			case "gravel plug":
			case "well":
				return flatAbove && drop <= 24 && (low.waterTop < 0 ? low.stand == low.y : low.stand - low.waterTop <= 3);
			case "wall climb":
				return low.h >= 12 && low.kind.openWalls() && flatAbove && up.stand == up.y;
			default:
				return true;
		}
	}

	private static Way make(String kind, Floor up, Floor low) {
		switch (kind) {
			case "hole": return new Hole(up, low);
			case "gravel plug": return new GravelPlug(up, low);
			case "well": return new Well(up, low);
			case "wall climb": return new WallClimb(up, low);
			case "crumbling stairs": return new CrumblingStairs(up, low);
			default: return new Stairs(up, low, kind);
		}
	}

	/** Something to catch a fall: the floor's own water, or a walled pool two deep sunk through its boards. */
	private abstract static class Falling extends Way {
		final int radius;

		Falling(String kind, Floor up, Floor low, int radius) {
			super(kind, up, low);
			this.radius = radius;
		}

		@Override
		int reachX() {
			return radius + 2;
		}

		/** A simple tower's narrower: across the way you step off and land, it keeps only the pool's own rim. */
		@Override
		int reachZ() {
			return simple() ? radius + 1 : radius + 2;
		}

		boolean wet() {
			return low.waterTop >= 0;
		}

		/** In a simple tower, a pool sunk into the floor with no kerb, so it takes no more of a small room than the hole over it: the room below has a lump in its ceiling for it. */
		boolean sunk() {
			return simple() && !wet();
		}

		@Override
		List<int[]> foot(boolean onUp, int cx, int cz) {
			List<int[]> foot = new ArrayList<>();
			int r = radius;
			if (onUp) {
				foot.add(rect(cx - r, cz - r, cx + r, cz + r, CORE));
				foot.add(rect(cx - r - 1, cz - r - 1, cx + r + 1, cz + r + 1, AREA));
				foot.add(rect(cx - r - 1, cz, cx - r - 1, cz, NEED));
			} else if (wet()) {
				foot.add(rect(cx - r, cz - r, cx + r, cz + r, CORE));
			} else if (sunk()) {
				foot.add(rect(cx - r, cz - r, cx + r, cz + r, CORE));
				foot.add(rect(cx - r - 1, cz - r - 1, cx + r + 1, cz + r + 1, AREA));
				foot.add(rect(cx + r + 1, cz, cx + r + 1, cz, NEED));
			} else {
				foot.add(rect(cx - r, cz - r, cx + r, cz + r, CORE));
				ring(foot, cx, cz, r + 1, KERB);
				foot.add(rect(cx + r + 2, cz, cx + r + 2, cz, NEED));
			}
			return foot;
		}

		@Override
		void placed() {
			// Beside the pool's rim, on the floor above and on the floor below.
			ux = x - radius - 1;
			uz = z;
			lx = x + radius + (sunk() ? 1 : 2);
			lz = z;
			if (wet()) {
				lx = x;
				lz = z;
			}
		}

		@Override
		void hold() {
			hold(x - radius - 1, z - radius - 1, x + radius + 1, z + radius + 1, up.y, up.y + 2);
			if (wet()) return;
			if (simple()) hold(foot(false, x, z), low.y - 1, low.top());
			else hold(x - radius - 2, z - radius - 2, x + radius + 2, z + radius + 2, low.y - 1, low.top());
		}

		/** A shaft and somewhere to stand round it, if the floors were solid. */
		void clearRoom() {
			clearRock(x - radius - 1, z - radius - 1, x + radius + 1, z + radius + 1, up.y, up.y + 2);
			if (wet()) return;
			clearRock(x - radius - 1, z - radius - 1, x + radius + 1, z + radius + 1, low.y, low.top());
			// Headroom to climb out over the rim and step down where you end up.
			clearRock(lx, lz, lx, lz, low.y, low.top());
		}

		/** {@code deep} two, or three where gravel will come down into it too: a fall into one of water still hurts, measured, and gravel on the bottom takes a cell of it. */
		void pool(int deep) {
			Plan p = low.p;
			int r = radius + 1;
			if (wet()) {
				for (int ax = x - radius; ax <= x + radius; ax++) {
					for (int az = z - radius; az <= z + radius; az++) {
						for (int y = low.y; y <= low.waterTop; y++) p.force(ax, y, az, cell(WATER, 0));
						for (int y = low.waterTop + 1; y <= low.top(); y++) p.force(ax, y, az, AIR);
					}
				}
				return;
			}
			if (sunk()) {
				// Two deep, both of them in the floor, and under it a lump in the ceiling of the room below, wide enough to hang from it.
				for (int ax = x - r; ax <= x + r; ax++) {
					for (int az = z - r; az <= z + r; az++) {
						boolean edge = Math.max(Math.abs(ax - x), Math.abs(az - z)) == r;
						if (edge && Math.abs(ax - x) == r && Math.abs(az - z) == r) continue;
						if (!edge) for (int y = low.y - 2; y <= low.y - 1; y++) p.force(ax, y, az, cell(WATER, 0));
						if (p.id(ax, low.y - 3, az) == AIR || p.id(ax, low.y - 3, az) == WEB) p.force(ax, low.y - 3, az, low.t.paletteAt(low.y - 3).wall.at(ax, low.y - 3, az));
					}
				}
				return;
			}
			int bottom = low.y - 1;
			for (int ax = x - r; ax <= x + r; ax++) {
				for (int az = z - r; az <= z + r; az++) {
					boolean rim = Math.max(Math.abs(ax - x), Math.abs(az - z)) == r;
					// Its kerb has its corners off.
					if (rim && Math.abs(ax - x) == r && Math.abs(az - z) == r) continue;
					for (int y = low.y; y <= bottom + deep - 1; y++) p.force(ax, y, az, rim ? low.pal.trim.at(ax, y, az) : cell(WATER, 0));
					if (!rim) for (int y = bottom; y < low.y; y++) p.force(ax, y, az, cell(WATER, 0));
				}
			}
		}

		/** Cuts the floor above through to the room below, in a shape like a floor that gave way. */
		void breakThrough(int radius, int fill, boolean rough) {
			Plan p = up.p;
			Noise n = up.t.noise;
			for (int ax = x - radius - 1; ax <= x + radius + 1; ax++) {
				for (int az = z - radius - 1; az <= z + radius + 1; az++) {
					int far = Math.max(Math.abs(ax - x), Math.abs(az - z));
					// A rough edge stays over the pool's water, as a fall past it would land on the rim.
					boolean open = far <= radius - (rough ? 1 : 0) || rough && far <= radius && n.at(ax, 9, az, 2) > 0.3;
					if (!open) continue;
					for (int y = low.top() + 1; y < up.y; y++) p.force(ax, y, az, fill);
					for (int y = up.y; y <= up.y + 2; y++) p.force(ax, y, az, AIR);
				}
			}
		}
	}

	/** A floor fallen through. You look over the edge, and the water's a long way down. */
	static final class Hole extends Falling {
		Hole(Floor up, Floor low) {
			super("hole", up, low, 1);
		}

		@Override
		void draw() {
			clearRoom();
			pool(2);
			breakThrough(radius, AIR, true);
		}
	}

	/** A patch of gravel in the floor over nothing: dig any of it and the lot goes, and you with it. */
	static final class GravelPlug extends Falling {
		GravelPlug(Floor up, Floor low) {
			super("gravel plug", up, low, 1);
		}

		/** Its pool's three deep, as the gravel takes a cell of it, so even in a simple tower it stands kerbed on the floor rather than sinking into the room below. */
		@Override
		boolean sunk() {
			return false;
		}

		@Override
		void draw() {
			clearRoom();
			pool(3);
			Plan p = up.p;
			for (int[] c : patch()) {
				int ax = x + c[0], az = z + c[1];
				// One thick: all of it lands in the pool, and leaves it a cell of water to land in.
				p.force(ax, up.y - 1, az, cell(GRAVEL, 0));
				for (int y = low.top() + 1; y < up.y - 1; y++) p.force(ax, y, az, AIR);
			}
		}

		/**
		 * Where the floor was patched with gravel or gave way to it, rough rather than square (the user, 2026-11-07): three to seven cells of the pool's own three square, so all of it comes down
		 * into the water, and joined side to side, so digging any of it brings the lot.
		 */
		private List<int[]> patch() {
			Random r = new Random(Tower.mix(up.t.seed ^ 0x67726176L, x, z));
			List<int[]> cells = new ArrayList<>();
			if (r.nextInt(4) == 0) {
				for (int[] c : new int[][] {{-1, -1}, {-1, 0}, {0, -1}, {0, 0}}) cells.add(c);
				return cells;
			}
			int size = 3 + r.nextInt(5);
			cells.add(new int[] {r.nextInt(3) - 1, r.nextInt(3) - 1});
			while (cells.size() < size) {
				int[] from = cells.get(r.nextInt(cells.size()));
				int d = r.nextInt(4), cu = from[0] + DX[d], cv = from[1] + DZ[d];
				if (Math.abs(cu) > 1 || Math.abs(cv) > 1) continue;
				boolean have = false;
				for (int[] c : cells) have |= c[0] == cu && c[1] == cv;
				if (!have) cells.add(new int[] {cu, cv});
			}
			return cells;
		}
	}

	/** A curbed well with a shaft below it, running out through the ceiling of the room below into a basin. */
	static final class Well extends Falling {
		Well(Floor up, Floor low) {
			super("well", up, low, 1);
		}

		@Override
		void draw() {
			clearRoom();
			pool(2);
			Plan p = up.p;
			Palette pal = up.pal;
			for (int ax = x - 1; ax <= x + 1; ax++) {
				for (int az = z - 1; az <= z + 1; az++) {
					p.force(ax, up.y, az, pal.trim.at(ax, up.y, az));
				}
			}
			p.force(x, up.y, z, cell(WATER, 0));
			int foot = low.waterTop >= 0 ? low.waterTop + 1 : sunk() ? low.y : low.y + 1;
			for (int y = up.y - 1; y >= foot; y--) p.force(x, y, z, cell(WATER, FALLING));
		}
	}

	/** A newel stair: steps one wide round a stone core, turning at each corner. Going by itself it reaches any drop: it just turns more. A simple tower's is a tight one round a single post. */
	static final class Stairs extends Way {
		private final int size, half;

		Stairs(Floor up, Floor low, String kind) {
			super(kind, up, low);
			size = up.t.style == Style.SIMPLE ? 3 : 5;
			half = size / 2;
		}

		@Override
		int reachX() {
			return half + 1;
		}

		@Override
		int reachZ() {
			return half + 1;
		}

		/** The steps and the post; you land on them, at a corner, and step on from the floor above at the first corner. */
		@Override
		List<int[]> foot(boolean onUp, int cx, int cz) {
			List<int[]> foot = new ArrayList<>();
			foot.add(rect(cx - half, cz - half, cx + half, cz + half, CORE));
			foot.add(rect(cx - half - 1, cz - half - 1, cx + half + 1, cz + half + 1, AREA));
			if (onUp) foot.add(rect(cx - half - 1, cz - half, cx - half - 1, cz - half, NEED));
			return foot;
		}

		@Override
		void placed() {
			ux = x - half - 1;
			uz = z - half;
			lx = x + half + 1;
			lz = z + half;
		}

		@Override
		void hold() {
			hold(x - half - 1, z - half - 1, x + half + 1, z + half + 1, low.kind.landsOnStage() ? low.stand - 1 : low.y - 1, up.leave + 2);
		}

		@Override
		void draw() {
			Plan p = up.p;
			int x0 = x - half, z0 = z - half, x1 = x + half, z1 = z + half;
			int level = up.leave - 1, target = low.stand - 1, run = size - 2;
			Palette pal = low.pal;
			// In water, a solid block stands in the room, its walk round it too, as water under them would be a place a swimmer can't come up; on a raised walk with nothing under it, a plinth.
			// Over a pit it's a stage, and the pit goes on under it.
			boolean stage = low.kind.landsOnStage(), wet = low.waterTop >= 0 && !stage;
			if (wet) {
				for (int ax = x0 - 1; ax <= x1 + 1; ax++) {
					for (int az = z0 - 1; az <= z1 + 1; az++) {
						for (int y = low.y; y < target; y++) p.force(ax, y, az, pal.pillar.at(ax, y, az));
					}
				}
			}
			for (int ax = x0; ax <= x1; ax++) {
				for (int az = z0; az <= z1; az++) {
					for (int y = low.y; y <= target && !stage; y++) p.force(ax, y, az, pal.pillar.at(ax, y, az));
					for (int y = target + 1; y <= up.leave + 2; y++) {
						if (y <= low.top() || y >= up.y) p.force(ax, y, az, AIR);
					}
				}
			}
			for (int ax = x0 + 1; ax < x1; ax++) {
				for (int az = z0 + 1; az < z1; az++) {
					for (int y = target + 1; y <= up.leave - 2; y++) p.force(ax, y, az, pal.pillar.at(ax, y, az));
				}
			}
			int[][] corners = {{x0, z0}, {x1, z0}, {x1, z1}, {x0, z1}};
			Mat edge = pal.trim;
			for (int side = 0; level > target; side++) {
				int d = side & 3;
				p.force(corners[d][0], level, corners[d][1], edge.at(corners[d][0], level, corners[d][1]));
				clearOver(p, corners[d][0], level, corners[d][1]);
				for (int i = 0; i < run && level > target; i++, level--) {
					int sx = corners[d][0] + DX[d] * (i + 1), sz = corners[d][1] + DZ[d] * (i + 1);
					p.force(sx, level, sz, cell(pal.stairs, stairMeta(opposite(d))));
					clearOver(p, sx, level, sz);
					// You end up one on from the last step, wherever the turns have brought that.
					lx = sx + DX[d];
					lz = sz + DZ[d];
				}
			}
			walkway(up, up.leave - 1, x0 - 1, z0 - 1, x1 + 1, z1 + 1);
			if (stage) stage(low, target, x0 - 1, z0 - 1, x1 + 1, z1 + 1, x0, z0, x1, z1);
			else walkway(low, target, x0 - 1, z0 - 1, x1 + 1, z1 + 1);
		}

		private static void clearOver(Plan p, int x, int level, int z) {
			for (int y = level + 1; y <= level + 3; y++) p.force(x, y, z, AIR);
		}
	}

	/** A ladder up a post, through an open hole in the floor above (no trapdoor, the user's call, 2026-11-07). */
	static final class Ladder extends Way {
		Ladder(Floor up, Floor low) {
			super("ladder", up, low);
		}

		@Override
		int reachX() {
			return 2;
		}

		@Override
		int reachZ() {
			return 2;
		}

		/** The post and the ladder below, and only the hole above. */
		@Override
		List<int[]> foot(boolean onUp, int cx, int cz) {
			List<int[]> foot = new ArrayList<>();
			foot.add(rect(onUp ? cx + 1 : cx, cz, cx + 1, cz, CORE));
			foot.add(rect(cx - 1, cz - 1, cx + 2, cz + 1, AREA));
			foot.add(rect(cx + 2, cz, cx + 2, cz, NEED));
			return foot;
		}

		@Override
		void placed() {
			ux = x + 2;
			uz = z;
			lx = x + 2;
			lz = z;
		}

		@Override
		void hold() {
			hold(x - 1, z - 1, x + 2, z + 1, low.kind.landsOnStage() ? low.stand - 1 : low.y - 1, up.leave + 1);
		}

		@Override
		void draw() {
			Plan p = up.p;
			Palette pal = low.pal;
			int lad = x + 1;
			boolean stage = low.kind.landsOnStage();
			for (int y = low.y; y <= low.stand - 1 && !stage; y++) {
				for (int ax = x - 1; ax <= x + 2; ax++) {
					for (int az = z - 1; az <= z + 1; az++) p.force(ax, y, az, pal.pillar.at(ax, y, az));
				}
			}
			// Up through the floor above, so you climb out onto it. Where there's lava in the room it stands in, a post of stone, as lava sets a log alight from three cells off; so too at the
			// bottom, where beta's caves run with lava below y 10 (CaveCarver), right up to the wall.
			int post = low.kind.hotUnderfoot() || low.stand <= 12 ? pal.pillar.at(x, low.stand, z) : cell(LOG, 0);
			for (int y = low.stand; y <= up.leave - 1; y++) {
				if (y < up.leave - 1 && (y <= low.top() || y >= up.y)) p.force(x, y, z, post);
				p.force(lad, y, z, cell(LADDER, backTo(WEST)));
			}
			p.force(lad, up.leave, z, AIR);
			p.force(lad, up.leave + 1, z, AIR);
			p.force(ux, up.leave, uz, AIR);
			p.force(ux, up.leave + 1, uz, AIR);
			walkway(up, up.leave - 1, x - 1, z - 1, x + 2, z + 1);
			if (stage) stage(low, low.stand - 1, x - 1, z - 1, x + 2, z + 1, x, z, x, z);
			else walkway(low, low.stand - 1, x - 1, z - 1, x + 2, z + 1);
		}
	}

	/** Ledges up the walls of a tall room, each a short drop and a short step from the last. */
	static final class WallClimb extends Way {
		private static final int DROP = 2, OPEN = 3;

		WallClimb(Floor up, Floor low) {
			super("wall climb", up, low);
		}

		@Override
		int reachX() {
			return 2;
		}

		@Override
		int reachZ() {
			return 2;
		}

		@Override
		boolean fits(int cx, int cz) {
			// Right up against the wall of the room below, so it's a real climb from the first ledge.
			if (!super.fits(cx, cz)) return false;
			double a = Math.atan2(cz - low.t.cz, cx - low.t.cx);
			return !low.in(cx + (int) Math.round(Math.cos(a) * 4), cz + (int) Math.round(Math.sin(a) * 4));
		}

		/** How far round the first ledge the floor above has fallen in: less in a simple tower's small room. */
		private int open() {
			return simple() ? 2 : OPEN;
		}

		/** Where the first ledge goes for a way placed here: hard against the wall. */
		private int[] first(int cx, int cz) {
			return wallCell(Math.atan2(cz - low.t.cz, cx - low.t.cx));
		}

		/** Where you step off the floor above: in from the first ledge, toward the middle, past what's fallen in. */
		private int[] mouth(int[] c) {
			int inward = Math.abs(c[0] - low.t.cx) > Math.abs(c[1] - low.t.cz) ? (c[0] > low.t.cx ? WEST : EAST) : c[1] > low.t.cz ? NORTH : SOUTH;
			int far = simple() ? open() + 1 : 4;
			return new int[] {c[0] + DX[inward] * far, c[1] + DZ[inward] * far};
		}

		/** The ledges, top down, from the first: each a middle and a level. */
		private List<int[]> ledges(int[] c0) {
			List<int[]> out = new ArrayList<>();
			double a = Math.atan2(c0[1] - low.t.cz, c0[0] - low.t.cx), step = 3.2 / low.t.radius;
			for (int level = up.stand - 4; level >= low.stand - 1; a += step) {
				int[] c = out.isEmpty() ? c0 : wallCell(a);
				out.add(new int[] {c[0], c[1], level});
				if (level == low.stand - 1) break;
				level = Math.max(low.stand - 1, level - DROP);
			}
			return out;
		}

		/** In a simple tower: the hole in the floor above and where you step off, and every ledge below and where you end up. */
		@Override
		List<int[]> foot(boolean onUp, int cx, int cz) {
			if (!simple()) return super.foot(onUp, cx, cz);
			int[] c0 = placed && cx == x && cz == z ? new int[] {x, z} : first(cx, cz);
			List<int[]> foot = new ArrayList<>();
			if (onUp) {
				for (int ax = c0[0] - open(); ax <= c0[0] + open(); ax++) {
					for (int az = c0[1] - open(); az <= c0[1] + open(); az++) {
						if (up.in(ax, az) && low.in(ax, az)) foot.add(rect(ax, az, ax, az, CORE));
					}
				}
				int[] m = mouth(c0);
				foot.add(rect(m[0], m[1], m[0], m[1], NEED));
				return foot;
			}
			List<int[]> path = ledges(c0);
			for (int i = 0; i < path.size(); i++) {
				int[] c = path.get(i);
				for (int ax = c[0] - 1; ax <= c[0] + 1; ax++) {
					for (int az = c[1] - 1; az <= c[1] + 1; az++) {
						if ((i > 0 && Math.abs(ax - c[0]) + Math.abs(az - c[1]) > 1) || !low.in(ax, az)) continue;
						foot.add(rect(ax, az, ax, az, CORE));
					}
				}
			}
			return foot;
		}

		private boolean placed;

		/** Moved hard against the wall, so the first ledge is the first of a run. The mouth's in from it, toward the middle. */
		@Override
		void placed() {
			int[] c = first(x, z);
			x = c[0];
			z = c[1];
			placed = true;
			int[] m = mouth(c);
			ux = m[0];
			uz = m[1];
			lx = x;
			lz = z;
		}

		/** The floor above has fallen in over the first ledges, as there's no walking out from under a roof one cell above it. */
		@Override
		void hold() {
			hold(x - open(), z - open(), x + open(), z + open(), up.y, up.y + 2);
		}

		/** The cell against the wall at this angle, and one in from it. */
		private int[] wallCell(double a) {
			Tower t = low.t;
			double rr = t.radius;
			int fx = t.cx, fz = t.cz;
			while (rr > 1) {
				int cx = t.cx + (int) Math.round(Math.cos(a) * rr), cz = t.cz + (int) Math.round(Math.sin(a) * rr);
				if (low.in(cx, cz)) {
					fx = cx;
					fz = cz;
					break;
				}
				rr -= 0.5;
			}
			return new int[] {fx, fz};
		}

		@Override
		void draw() {
			Plan p = up.p;
			Mat ledge = low.pal.trim;
			double a = Math.atan2(z - low.t.cz, x - low.t.cx);
			int level = up.stand - 4;
			double step = 3.2 / low.t.radius;
			boolean first = true;
			lx = x;
			lz = z;
			while (level >= low.stand - 1) {
				int[] c = first ? new int[] {x, z} : wallCell(a);
				for (int ax = c[0] - 1; ax <= c[0] + 1; ax++) {
					for (int az = c[1] - 1; az <= c[1] + 1; az++) {
						if (!first && Math.abs(ax - c[0]) + Math.abs(az - c[1]) > 1) continue;
						if (!low.in(ax, az)) continue;
						p.force(ax, level, az, ledge.at(ax, level, az));
						for (int y = level + 1; y <= level + 3; y++) {
							if (y <= low.top() || first) p.force(ax, y, az, AIR);
						}
					}
				}
				lx = c[0];
				lz = c[1];
				if (level == low.stand - 1) break;
				level = Math.max(low.stand - 1, level - DROP);
				a += step;
				first = false;
			}
			// Through the floor above onto the first ledge.
			for (int ax = x - open(); ax <= x + open(); ax++) {
				for (int az = z - open(); az <= z + open(); az++) {
					if (!up.in(ax, az) || !low.in(ax, az)) continue;
					for (int y = low.top() + 1; y < up.y; y++) p.force(ax, y, az, AIR);
					for (int y = up.y; y <= up.y + 2; y++) p.force(ax, y, az, AIR);
				}
			}
		}
	}

	/**
	 * Loose gravel clinging round a post in a stairwell, a block a step, over a pool (the user, 2026-11-08: gravel stuck to a wall or a pillar makes sense as a place, a pressure plate that drops it
	 * doesn't). Walking down it changes nothing, but by beta's own rules a step that's told of a change beside it falls: a torch put on it, a block broken next to it. Every step has the post or the
	 * well's wall beside it, the steps a turn apart are four apart, so dropping with one costs at most a half-heart, and the last drop is into the pool, with a way out of the well over it.
	 */
	static final class CrumblingStairs extends Way {
		/** The well's inside each way from the post, and its wall one further. */
		private static final int IN = 1, WALL = 2;

		CrumblingStairs(Floor up, Floor low) {
			super("crumbling stairs", up, low);
		}

		/** Out to where you step off below, past the well's wall. */
		@Override
		int reachX() {
			return WALL + 1;
		}

		@Override
		int reachZ() {
			return WALL + 1;
		}

		/** The steps from the top, each {dx, y, dz} from the post: round it as {@link Stairs} goes round its post, a corner and then a side at each level, down to the last over the pool. */
		private List<int[]> steps() {
			List<int[]> steps = new ArrayList<>();
			int[][] corners = {{-IN, -IN}, {IN, -IN}, {IN, IN}, {-IN, IN}};
			for (int side = 0, level = up.leave - 1; level > low.y + 2; side++, level--) {
				int d = side & 3;
				steps.add(new int[] {corners[d][0], level, corners[d][1]});
				steps.add(new int[] {corners[d][0] + DX[d], level, corners[d][1] + DZ[d]});
			}
			return steps;
		}

		/** The side the way out of the well is cut in: where no step stands over the water there, and you come out into the room with a step more of it before you. -1 if there's none. */
		private int exit(int cx, int cz) {
			List<int[]> steps = steps();
			for (int k = 0; k < 4; k++) {
				int d = k + 1 & 3;
				boolean free = true;
				for (int[] s : steps) {
					if (s[0] == DX[d] && s[2] == DZ[d] && s[1] <= low.y + 3) free = false;
				}
				int ox = cx + DX[d] * (WALL + 1), oz = cz + DZ[d] * (WALL + 1), fx = ox + DX[d], fz = oz + DZ[d];
				if (free && low.in(ox, oz) && low.kind.allows(low, ox, oz) && low.in(fx, fz) && low.kind.allows(low, fx, fz)) return d;
			}
			return -1;
		}

		/** The hole in the floor above and where you step on; the well, whose wall may be the tower's, and where you come out. */
		@Override
		List<int[]> foot(boolean onUp, int cx, int cz) {
			List<int[]> foot = new ArrayList<>();
			if (onUp) {
				foot.add(rect(cx - IN, cz - IN, cx + IN, cz + IN, CORE));
				foot.add(rect(cx - WALL, cz - WALL, cx + WALL, cz + WALL, AREA));
				foot.add(rect(cx - IN - 1, cz - IN, cx - IN - 1, cz - IN, NEED));
			} else {
				foot.add(rect(cx - IN, cz - IN, cx + IN, cz + IN, CORE));
				foot.add(rect(cx - WALL, cz - WALL, cx + WALL, cz - WALL, KERB));
				foot.add(rect(cx - WALL, cz + WALL, cx + WALL, cz + WALL, KERB));
				foot.add(rect(cx - WALL, cz - IN, cx - WALL, cz + IN, KERB));
				foot.add(rect(cx + WALL, cz - IN, cx + WALL, cz + IN, KERB));
				int d = exit(cx, cz);
				if (d >= 0) {
					int ax = cx + DX[d] * (WALL + 1), az = cz + DZ[d] * (WALL + 1), bx = ax + DX[d], bz = az + DZ[d];
					foot.add(rect(Math.min(ax, bx), Math.min(az, bz), Math.max(ax, bx), Math.max(az, bz), NEED));
				}
			}
			return foot;
		}

		@Override
		boolean fits(int cx, int cz) {
			return exit(cx, cz) >= 0 && super.fits(cx, cz);
		}

		@Override
		void placed() {
			ux = x - IN - 1;
			uz = z - IN;
			int d = Math.max(exit(x, z), 0);
			lx = x + DX[d] * (WALL + 1);
			lz = z + DZ[d] * (WALL + 1);
		}

		@Override
		void hold() {
			hold(x - WALL, z - WALL, x + WALL, z + WALL, low.y - 1, up.leave + 2);
			hold(lx, lz, lx, lz, low.y, low.y + 3);
		}

		@Override
		void draw() {
			Plan p = up.p;
			Palette pal = low.pal;
			// The well, from the pool's floor to the room's ceiling and open on up through the floor above; its post from the pool's floor to just under the floor above.
			for (int ax = x - WALL; ax <= x + WALL; ax++) {
				for (int az = z - WALL; az <= z + WALL; az++) {
					boolean wall = Math.abs(ax - x) == WALL || Math.abs(az - z) == WALL;
					for (int y = low.y - 1; y <= low.top(); y++) p.force(ax, y, az, wall ? pal.wall.at(ax, y, az) : y <= low.y + 1 ? cell(WATER, 0) : AIR);
					if (!wall) for (int y = low.top() + 1; y <= up.leave + 2; y++) p.force(ax, y, az, AIR);
				}
			}
			for (int y = low.y - 1; y <= up.leave - 2; y++) p.force(x, y, z, pal.pillar.at(x, y, z));
			// The way out: through the wall over the water, and down into the room.
			int d = Math.max(exit(x, z), 0);
			for (int y = low.y + 2; y <= low.y + 3; y++) p.force(x + DX[d] * WALL, y, z + DZ[d] * WALL, AIR);
			for (int y = low.y; y <= low.y + 3; y++) p.force(lx, y, lz, AIR);
			for (int[] s : steps()) {
				p.force(x + s[0], s[1], z + s[2], cell(GRAVEL, 0));
				for (int y = s[1] + 1; y <= s[1] + 3; y++) p.force(x + s[0], y, z + s[2], AIR);
			}
		}
	}
}
