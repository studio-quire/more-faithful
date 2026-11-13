package org.studioquire.faithful.buriedtowers.plan;

import static org.studioquire.faithful.buriedtowers.plan.Blocks.*;

import java.util.Random;

/**
 * A simple tower's outside: one stuff from the foot up, and above the land only a stump of it, broken to a ragged top a storey high at most, with stretches come down to the ground and lying round
 * it. A gap for a door. No rings, coping, windows or roof: less built than a modest house beside it. Where the land falls away from it, earth has banked up against the wall over the years, so it
 * doesn't stand out of a slope taller than a house.
 */
final class SimpleBody extends Exterior {
	/** The highest the stump stands over the hall's floor. */
	private static final int TALLEST = 5;
	/** The highest it stands over the land outside, but where it has to reach down to the hall's floor. */
	private static final int LOW_SIDE = 4;
	/** The deepest a cave's mouth under the bank is filled. */
	private static final int FILLS = 4;

	private final Tower t;
	private final Plan p;
	private final Random r;
	private final Noise n;
	private final Palette outer;
	private final Mat moss;

	SimpleBody(Tower t) {
		this.t = t;
		this.p = t.plan;
		this.r = new Random(t.seed ^ 0x2F0B6E1L);
		this.n = new Noise(t.seed ^ 0x57A3FL);
		this.outer = hall().pal;
		this.moss = Mat.of(t.is(Climate.DESERT) ? SANDSTONE : MOSSY);
	}

	private Floor hall() {
		return t.floors.get(0);
	}

	private boolean chance(int x, int y, int z, int oneIn) {
		return Math.floorMod(Tower.mix(t.seed ^ y * 0x2545F491L, x, z), oneIn) == 0;
	}

	/** How far the wall's come down here, 0 to 1: the stump's height follows it, and so does the stone lying outside. */
	private double fallen(int x, int z) {
		return 1 - n.at(x, 0, z, 7);
	}

	/**
	 * The top of the wall at a column: a ragged stump, a block or two lower here and there, down to two courses over the floor where it's fallen, so the hall still reads as the room it was, and never
	 * below the land it holds back. Where the land falls away it stands at most a few blocks over it, as that's where a wall comes down first.
	 */
	private int wallTop(int x, int z) {
		int floor = hall().y, land = t.ground.top(x, z);
		int top = floor - 1 + (int) Math.floor((0.78 - fallen(x, z)) * 11);
		if (chance(x, 0, z, 3)) top--;
		top = Math.min(top, floor + TALLEST - 1);
		top = Math.min(top, Math.max(floor, land + LOW_SIDE));
		return Math.max(top, Math.max(floor + 1, land));
	}

	/**
	 * The top block of the earth banked against the wall here, or lower than anything if there's none: not out past the fallen stones, or in water. It falls away about a block for each out
	 * from the wall, steeper and gentler by turns and its edge wandering, as ground does, rather than in even rings (which read as a stepped terrace).
	 */
	private int bank(int x, int z) {
		if (t.depth(x, z) != 0 || t.ground.top(x, z) < Ground.SEA) return Integer.MIN_VALUE;
		double far = Math.hypot(x - t.cx, z - t.cz) - t.radius - 0.5, lumps = n.at(x, 40, z, 3.5), slope = 0.7 + 0.6 * n.at(x, 80, z, 9);
		if (far > Tower.APRON - lumps * 2) return Integer.MIN_VALUE;
		return hall().y - 1 - (int) Math.ceil(Math.max(far, 0.5) * slope + lumps * 2.4 - 0.9);
	}

	/**
	 * The bank's top block where it's drawn, or lower than anything: only where it stands on the land before caves, and not over a cave's mouth deeper than {@link #FILLS} under that land, which is
	 * left open. Over a shallower one the bank's earth goes down into it to the land the cave left, so it never stands on air.
	 */
	private int banked(int x, int z) {
		int bank = bank(x, z), land = t.ground.top(x, z);
		return bank >= land && land - t.ground.surface(x, z) <= FILLS ? bank : Integer.MIN_VALUE;
	}

	/** The lowest cell above the land outside, the bank included. */
	private int landAt(int x, int z) {
		return Math.max(t.ground.top(x, z), bank(x, z) + 1);
	}

	/**
	 * Where whatever's laid on the land rests: on the bank where it's drawn ({@link #banked}), and otherwise on the land with beta's caves cut, lower than {@link #landAt} where a cave has opened the
	 * land from above.
	 */
	private int restAt(int x, int z) {
		int bank = banked(x, z);
		return bank != Integer.MIN_VALUE ? bank + 1 : t.ground.surface(x, z);
	}

	@Override
	void shell() {
		int foot = t.foot();
		for (int x = t.cx - t.radius - Tower.APRON - 1; x <= t.cx + t.radius + Tower.APRON + 1; x++) {
			for (int z = t.cz - t.radius - Tower.APRON - 1; z <= t.cz + t.radius + Tower.APRON + 1; z++) {
				int top = banked(x, z), land = t.ground.surface(x, z);
				if (top == Integer.MIN_VALUE) continue;
				boolean sand = t.is(Climate.DESERT);
				for (int y = land; y <= top; y++) p.force(x, y, z, y == top ? cell(sand ? SAND : GRASS, 0) : y >= top - 2 ? cell(sand ? SAND : DIRT, 0) : cell(sand ? SANDSTONE : DIRT, 0));
				// Snow lies on it as on the land round it, which had its snow before the bank was made.
				if (t.is(Climate.COLD)) p.force(x, top + 1, z, cell(SNOW, 0));
			}
		}
		for (int x = t.cx - t.radius; x <= t.cx + t.radius; x++) {
			for (int z = t.cz - t.radius; z <= t.cz + t.radius; z++) {
				int d = t.depth(x, z);
				if (d == 0) continue;
				for (int y = foot; y < Plan.HEIGHT; y++) p.force(x, y, z, AIR);
				if (d > t.wall) continue;
				int top = wallTop(x, z), land = landAt(x, z);
				for (int y = foot; y <= top; y++) {
					// Stones out of the stump, which is one thick: never beside a room below it, where the land's fallen away.
					if (d == 1 && y > land + 1 && y > hall().y && y < top && chance(x, y, z, 12)) continue;
					p.force(x, y, z, d == 1 && y >= land - 1 ? face(x, y, z, land) : t.paletteAt(y).wall.at(x, y, z));
				}
			}
		}
		floors(t);
	}

	/** Damp and mossy near the land, plain above it. */
	private int face(int x, int y, int z, int land) {
		if (y <= land || y <= land + 2 && n.at(x, y, z, 4) > 0.4 + (y - land) * 0.05) return moss.at(x, y, z);
		return outer.wall.at(x, y, z);
	}

	/** A gap through the wall three wide on the side where the land comes nearest the floor, and the land cut or built up a little so you walk in. */
	@Override
	void entrance() {
		Floor hall = hall();
		int best = 0, bestScore = Integer.MAX_VALUE;
		for (int d = 0; d < 4; d++) {
			int score = 0;
			for (int k = 0; k <= Tower.APRON; k++) score += Math.abs(landAt(t.cx + DX[d] * (t.radius + k), t.cz + DZ[d] * (t.radius + k)) - hall.y);
			score = score * 3 + r.nextInt(5);
			if (score < bestScore) {
				bestScore = score;
				best = d;
			}
		}
		int across = best + 1 & 3;
		Random path = new Random(t.seed ^ 0x70617468L);
		int length = 3 + path.nextInt(3);
		for (int w = -1; w <= 1; w++) {
			int ox = DX[across] * w, oz = DZ[across] * w;
			int[] levels = pathLevels(t, hall.y, best, ox, oz, this::restAt);
			for (int k = t.radius + Tower.APRON; k >= 0; k--) {
				int x = t.cx + DX[best] * k + ox, z = t.cz + DZ[best] * k + oz;
				int d = t.depth(x, z);
				if (d > t.wall) {
					if (!p.held(x, hall.y, z)) for (int y = hall.y; y < hall.y + 3; y++) p.force(x, y, z, AIR);
					break;
				}
				if (d >= 1) {
					for (int y = hall.y; y < hall.y + 3; y++) p.force(x, y, z, AIR);
					p.force(x, hall.y - 1, z, hall.kind.underfoot(hall).at(x, hall.y - 1, z));
				} else {
					int land = restAt(x, z), out = k - t.radius, level = levels[k];
					for (int y = level; y <= land + 2; y++) p.force(x, y, z, AIR);
					int top = pathTop(t, path, length, out, r.nextInt(3) == 0 ? cell(MOSSY, 0) : cell(COBBLE, 0));
					for (int y = land; y < level; y++) p.force(x, y, z, pathFill(t, id(top) != GRASS && id(top) != SAND));
					p.force(x, level - 1, z, top);
				}
			}
		}
		hall.safeX = t.cx + DX[best] * (t.radius - 4);
		hall.safeZ = t.cz + DZ[best] * (t.radius - 4);
		hall.safeY = hall.stand;
		t.gateX = t.cx + DX[best] * (t.radius + 2);
		t.gateZ = t.cz + DZ[best] * (t.radius + 2);
	}

	@Override
	void dress() {
	}

	/** What came off the wall, on the land under it: thickest where the wall's lowest, a stone or two high, as far as a fallen stretch would reach. */
	@Override
	void apron() {
		for (int x = p.ox; x < p.ox + p.size; x++) {
			for (int z = p.oz; z < p.oz + p.size; z++) {
				if (t.depth(x, z) != 0) continue;
				double far = Math.hypot(x - t.cx, z - t.cz) - t.radius;
				if (far < 0 || far > 5) continue;
				double a = Math.atan2(z - t.cz, x - t.cx);
				int wx = t.cx + (int) Math.round(Math.cos(a) * t.radius), wz = t.cz + (int) Math.round(Math.sin(a) * t.radius);
				double odds = (fallen(wx, wz) - 0.2) * 0.9 - far * 0.09;
				if (r.nextDouble() > odds) continue;
				int land = restAt(x, z);
				if (land < Ground.SEA || p.get(x, land, z) != Plan.KEEP) continue;
				p.loose(x, land, z, stone(x, land, z));
				if (far < 2.5 && r.nextInt(3) == 0 && p.get(x, land + 1, z) == Plan.KEEP) p.loose(x, land + 1, z, stone(x, land + 1, z));
			}
		}
	}

	private int stone(int x, int y, int z) {
		return r.nextInt(4) == 0 ? moss.at(x, y, z) : outer.wall.at(x, y, z);
	}

	@Override
	void dropLoose() {
	}
}
