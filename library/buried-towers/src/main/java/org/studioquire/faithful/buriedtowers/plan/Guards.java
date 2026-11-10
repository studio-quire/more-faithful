package org.studioquire.faithful.buriedtowers.plan;

import static org.studioquire.faithful.buriedtowers.plan.Blocks.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The monsters, once a tower's drawn. Nothing in it gives light but lava and the sky: the player brings their own (user, 2026-11-07). Beta lets a monster appear, from a spawner or by itself, only
 * where the light is at most a random 0 to 7 ({@code MonsterEntity.canSpawn}), and a spawner tries cells up to four across and one up or down from itself. So the light is worked out by beta's rule,
 * each spawner a floor drew stays if enough of the cells round it are dark, is moved to the best dark place on its floor if not, or goes if the floor has none (a foundry is all lit by its lava);
 * then deeper floors get more, of harder kinds, and now and then one stands by a hole down, pouring into the pool below. On a floor something's taken over (the spider nest) they're all of its kind,
 * as many as any floor at the depth gets (the user, 2026-11-08, after two more there was too many).
 */
final class Guards {
	/** A spawner that can use this much of the cells it tries, weighted as it picks them, spawns about as often as it can: when no try works it tries again next tick. */
	private static final double WORKS = 0.03;
	/** Where a spawner's put new, it should work well. */
	private static final double GOOD = 0.1;

	private final Tower t;
	private final Plan p;
	private final Random r;
	private final int x0, z0, side;
	private final byte[] light;
	/** What {@link #at} finds in each cell of the box, worked out once, as the light and the spawners' places read most cells many times over; kept in step as spawners move. */
	private final byte[] cells;
	private static final byte[] OPACITY = new byte[256], LUMINANCE = new byte[256];

	static {
		for (int id = 0; id < 256; id++) {
			OPACITY[id] = (byte) opacity(id);
			LUMINANCE[id] = (byte) luminance(id);
		}
	}

	private Guards(Tower t) {
		this.t = t;
		this.p = t.plan;
		this.r = new Random(t.seed ^ 0x6A4D5L);
		// A simple tower's vault keeps its strongroom in the rock outside the wall, and a cart run its shaft in either style.
		int margin = t.style == Style.COMPLEX && t.cart == null ? 1 : 5;
		x0 = t.cx - t.radius - margin;
		z0 = t.cz - t.radius - margin;
		side = t.radius * 2 + 1 + margin * 2;
		light = new byte[side * side * Plan.HEIGHT];
		cells = new byte[light.length];
		for (int x = x0; x < x0 + side; x++) {
			for (int z = z0; z < z0 + side; z++) {
				int top = Integer.MIN_VALUE;
				for (int y = 0; y < Plan.HEIGHT; y++) {
					int c = p.get(x, y, z);
					if (c == Plan.KEEP && top == Integer.MIN_VALUE) top = t.ground.top(x, z);
					cells[index(x, y, z)] = (byte) (c != Plan.KEEP ? id(c) : y < top ? STONE : AIR);
				}
			}
		}
	}

	static void post(Tower t) {
		Guards g = new Guards(t);
		g.light();
		for (int i = 0; i < t.floors.size(); i++) g.floor(t.floors.get(i));
	}

	// The light, by beta's rule (docs/lighting.md): a source holds its own level, a cell open to the sky above holds 15, and everything else takes its brightest neighbour less its own opacity, at
	// least 1. Daylight is taken, the worst case for spawning; sky and block light are one number here, as the spawning check takes the larger.

	private int index(int x, int y, int z) {
		return ((x - x0) * side + z - z0) * Plan.HEIGHT + y;
	}

	private boolean inBox(int x, int y, int z) {
		return x >= x0 && x < x0 + side && z >= z0 && z < z0 + side && y >= 0 && y < Plan.HEIGHT;
	}

	/** What a cell holds, with the land standing in for what the plan leaves alone. */
	private int at(int x, int y, int z) {
		if (inBox(x, y, z)) return cells[index(x, y, z)] & 0xFF;
		int c = p.get(x, y, z);
		if (c != Plan.KEEP) return id(c);
		return y < t.ground.top(x, z) ? STONE : AIR;
	}

	static int opacity(int id) {
		switch (id) {
			case WATER: case FLOWING_WATER: case ICE:
				return 3;
			case LEAVES: case WEB:
				return 1;
			case FENCE: case SPAWNER: case GLASS: case BED: case CACTUS: case CAKE: case PORTAL:
				return 0;
			default:
				return passable(id) ? 0 : 15;
		}
	}

	static int luminance(int id) {
		switch (id) {
			case LAVA: case FLOWING_LAVA: case FIRE: case GLOWSTONE: case JACK_O_LANTERN:
				return 15;
			case TORCH:
				return 14;
			case LIT_FURNACE:
				return 13;
			case PORTAL:
				return 11;
			case REDSTONE_TORCH:
				return 7;
			default:
				return 0;
		}
	}

	/** A cell's light by the rule above, which comes out the same whatever order cells are taken in: each ends as the most any path from a source leaves it. */
	private void light() {
		IntQueue queue = new IntQueue();
		for (int x = x0; x < x0 + side; x++) {
			for (int z = z0; z < z0 + side; z++) {
				boolean sky = true;
				for (int y = Plan.HEIGHT - 1; y >= 0; y--) {
					int i = index(x, y, z), id = cells[i] & 0xFF, level = LUMINANCE[id];
					if (sky && OPACITY[id] != 0) sky = false;
					if (sky) level = 15;
					if (level > 0) {
						light[i] = (byte) level;
						queue.add(i);
					}
				}
			}
		}
		int alongZ = Plan.HEIGHT, alongX = side * Plan.HEIGHT;
		while (!queue.isEmpty()) {
			int i = queue.remove(), level = light[i];
			int y = i % Plan.HEIGHT, column = i / Plan.HEIGHT, dx = column / side, dz = column % side;
			if (dx + 1 < side) spread(queue, i + alongX, level);
			if (dx > 0) spread(queue, i - alongX, level);
			if (y + 1 < Plan.HEIGHT) spread(queue, i + 1, level);
			if (y > 0) spread(queue, i - 1, level);
			if (dz + 1 < side) spread(queue, i + alongZ, level);
			if (dz > 0) spread(queue, i - alongZ, level);
		}
	}

	private void spread(IntQueue queue, int i, int level) {
		int op = OPACITY[cells[i] & 0xFF];
		if (op >= 15) return;
		int next = level - Math.max(1, op);
		if (next > light[i]) {
			light[i] = (byte) next;
			queue.add(i);
		}
	}

	private int lightAt(int x, int y, int z) {
		return inBox(x, y, z) ? light[index(x, y, z)] : 15;
	}

	/** Room for a monster to appear: two cells it can stand in, not under water. */
	private boolean room(int x, int y, int z) {
		int a = at(x, y, z), b = at(x, y + 1, z);
		return passable(a) && passable(b) && a != WATER && a != FLOWING_WATER && b != WATER && b != FLOWING_WATER;
	}

	/** How much of what a spawner at this cell tries would work, weighted as it chooses: the offsets of {@code (random - random) * 4} lean to the middle, and the light passes with (8 - level) in 8. */
	double works(int sx, int sy, int sz) {
		double all = 0, good = 0;
		for (int dx = -4; dx <= 4; dx++) {
			for (int dz = -4; dz <= 4; dz++) {
				double w = (4.5 - Math.abs(dx)) * (4.5 - Math.abs(dz));
				for (int dy = -1; dy <= 1; dy++) {
					all += w;
					int x = sx + dx, y = sy + dy, z = sz + dz;
					if (!room(x, y, z)) continue;
					int level = lightAt(x, y, z);
					if (level < 8) good += w * (8 - level) / 8.0;
				}
			}
		}
		return good / all;
	}

	private static final class Spot {
		final int x, y, z;
		final double works;

		Spot(int x, int y, int z, double works) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.works = works;
		}
	}

	private void floor(Floor f) {
		boolean above = f.y >= t.groundY;
		List<int[]> mine = new ArrayList<>();
		for (Map.Entry<Long, String> e : p.fixtures.entrySet()) {
			if (!e.getValue().startsWith("spawner:")) continue;
			int x = Plan.keyX(e.getKey()), y = Plan.keyY(e.getKey()), z = Plan.keyZ(e.getKey());
			if (t.floorAt(y) == f && y >= f.y - 1) mine.add(new int[] {x, y, z});
		}
		List<int[]> kept = new ArrayList<>();
		for (int[] s : mine) {
			String mob = p.fixture(s[0], s[1], s[2]).substring(8);
			if (works(s[0], s[1], s[2]) >= WORKS) {
				kept.add(s);
				continue;
			}
			remove(s);
			Spot to = best(f, kept, s[0], s[2], false);
			if (to != null) kept.add(put(to, mob));
		}
		if (above) return;
		// Going deeper: more of them, and harder.
		double d = f.depth;
		String taken = f.kind.takenBy();
		int want = d < 0.25 ? 1 : d < 0.5 ? 2 : d < 0.75 ? 2 + r.nextInt(2) : 3;
		if (f.out != null && f.out.kind.equals("hole") && d > 0.25 && r.nextBoolean() && kept.size() < want + 1) {
			int[] s = overHole(f, kept, taken);
			if (s != null) kept.add(s);
		}
		if (t.cart != null) {
			for (int[] c : t.cart.chutes) {
				if (c[0] != f.index) continue;
				int[] s = byChute(f, kept, c);
				if (s != null) kept.add(s);
			}
		}
		for (int tries = 0; kept.size() < want && tries < 4; tries++) {
			Spot to = best(f, kept, t.cx, t.cz, true);
			if (to == null) break;
			String mob = t.monster(r, d);
			kept.add(put(to, taken != null ? taken : mob));
		}
	}

	private void remove(int[] s) {
		p.force(s[0], s[1], s[2], AIR);
		if (inBox(s[0], s[1], s[2])) cells[index(s[0], s[1], s[2])] = AIR;
		p.fixtures.remove(Plan.key(s[0], s[1], s[2]));
		t.spawners--;
	}

	private int[] put(Spot s, String mob) {
		p.force(s.x, s.y, s.z, cell(SPAWNER, 0));
		if (inBox(s.x, s.y, s.z)) cells[index(s.x, s.y, s.z)] = SPAWNER;
		p.fixture(s.x, s.y, s.z, "spawner:" + mob);
		t.spawners++;
		return new int[] {s.x, s.y, s.z};
	}

	/** Beside the hole's rough edge, on the side away from where you'd stand to drop: what appears over it falls to the pool, out of the spawner's count, so it keeps coming while you're near. */
	private int[] overHole(Floor f, List<int[]> kept, String taken) {
		Way w = f.out;
		int[][] sides = {{w.x + 3, w.z}, {w.x, w.z + 3}, {w.x, w.z - 3}};
		int start = r.nextInt(sides.length);
		for (int i = 0; i < sides.length; i++) {
			int[] c = sides[(start + i) % sides.length];
			int x = c[0], z = c[1], y = f.y;
			if (!f.in(x, z) || p.held(x, y, z) || at(x, y, z) != AIR || at(x, y + 1, z) != AIR || !solid(at(x, y - 1, z)) || near(kept, x, z, 4)) continue;
			if (works(x, y, z) < GOOD) continue;
			String mob = t.monster(r, f.depth);
			return put(new Spot(x, y, z, 0), taken != null ? taken : mob);
		}
		return null;
	}

	/**
	 * Beside a floor's chute into a cart run's shaft, against the wall: about half of what it tries is in the shaft beyond, in the dark, and what appears there falls onto the track or beside it
	 * while a rider goes by (the user, 2026-11-08: the price of the shortcut). Always, where there's room.
	 */
	private int[] byChute(Floor f, List<int[]> kept, int[] chute) {
		int x0 = chute[1], y = chute[2], z0 = chute[3], in = chute[4], along = in + 1 & 3;
		for (int[] o : new int[][] {{1, 0}, {-1, 0}, {2, 0}, {-2, 0}, {1, 1}, {-1, 1}}) {
			int x = x0 + DX[along] * o[0] + DX[in] * o[1], z = z0 + DZ[along] * o[0] + DZ[in] * o[1];
			if (!f.in(x, z) || p.held(x, y, z) || at(x, y, z) != AIR || at(x, y + 1, z) != AIR || !solid(at(x, y - 1, z)) || near(kept, x, z, 2)) continue;
			if (works(x, y, z) < WORKS) continue;
			return put(new Spot(x, y, z, 0), t.monster(r, f.depth));
		}
		return null;
	}

	private static boolean near(List<int[]> spawners, int x, int z, int far) {
		for (int[] s : spawners) {
			if (Math.abs(s[0] - x) <= far && Math.abs(s[2] - z) <= far) return true;
		}
		return false;
	}

	/**
	 * The best place on a floor for a spawner: standing free on solid ground with open floor all round, so it blocks nothing, clear of where you land and leave and of the chests, and apart from the
	 * others. Moving one, the nearest good place to where it was; adding one, the darkest, by chance among the good ones.
	 */
	private Spot best(Floor f, List<int[]> kept, int fromX, int fromZ, boolean adding) {
		List<int[]> clear = new ArrayList<>();
		clear.add(new int[] {f.safeX, f.safeZ});
		if (f.in != null) clear.add(new int[] {f.in.lx, f.in.lz});
		if (f.arrive != null) clear.add(new int[] {f.arrive.lx, f.arrive.lz});
		if (f.out != null) clear.add(new int[] {f.out.ux, f.out.uz});
		if (f.hasGoal) clear.add(new int[] {f.goalX, f.goalZ});
		List<int[]> chests = new ArrayList<>();
		for (Map.Entry<Long, String> e : p.fixtures.entrySet()) {
			if (e.getValue().startsWith("chest:") && t.floorAt(Plan.keyY(e.getKey())) == f) chests.add(new int[] {Plan.keyX(e.getKey()), 0, Plan.keyZ(e.getKey())});
		}
		List<Spot> good = new ArrayList<>();
		int[] levels = f.stand == f.y ? new int[] {f.y} : f.kind.hollowUnder() ? new int[] {f.stand} : new int[] {f.y, f.stand};
		for (int y : levels) {
			for (int x = t.cx - t.radius; x <= t.cx + t.radius; x++) {
				for (int z = t.cz - t.radius; z <= t.cz + t.radius; z++) {
					if (!f.in(x, z) || !standsFree(x, y, z) || near(kept, x, z, 5) || near(chests, x, z, 1)) continue;
					boolean blocked = false;
					for (int[] c : clear) blocked |= Math.abs(c[0] - x) <= 3 && Math.abs(c[1] - z) <= 3;
					if (blocked) continue;
					double works = works(x, y, z);
					if (works >= GOOD) good.add(new Spot(x, y, z, works));
				}
			}
		}
		if (good.isEmpty()) return null;
		if (adding) {
			good.sort((a, b) -> Double.compare(b.works, a.works));
			return good.get(r.nextInt(Math.min(good.size(), 6)));
		}
		Spot best = null;
		double nearest = Double.MAX_VALUE;
		for (Spot s : good) {
			double far = Math.hypot(s.x - fromX, s.z - fromZ);
			if (far < nearest) {
				nearest = far;
				best = s;
			}
		}
		return best;
	}

	/**
	 * Empty with headroom, on solid ground, nothing a way holds, and room round it: most of the eight cells round it open, and never shut in on two opposite sides, so it can't stop up a passage.
	 * A mine's drifts are two wide in places, and a spawner there leaves one to pass.
	 */
	private boolean standsFree(int x, int y, int z) {
		if (at(x, y, z) != AIR || at(x, y + 1, z) != AIR || !solid(at(x, y - 1, z))) return false;
		int open = 0;
		boolean[] side = new boolean[4];
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				int ax = x + dx, az = z + dz;
				if (p.held(ax, y, az) || p.held(ax, y - 1, az)) return false;
				if (dx == 0 && dz == 0 || !room(ax, y, az)) continue;
				open++;
				for (int d = 0; d < 4; d++) side[d] |= DX[d] == dx && DZ[d] == dz;
			}
		}
		return open >= 5 && (side[EAST] || side[WEST]) && (side[NORTH] || side[SOUTH]);
	}

}
