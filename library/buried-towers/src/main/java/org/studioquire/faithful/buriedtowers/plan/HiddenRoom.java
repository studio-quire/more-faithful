package org.studioquire.faithful.buriedtowers.plan;

import static org.studioquire.faithful.buriedtowers.plan.Blocks.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * A small sealed room in the rock between two floors, holding a chest and nothing else. The way between those floors, always a ladder or stairs, runs {@link Tower#GAP} longer than
 * any other, past plain wall: that length is the only clue, and the room lies beside it, one block in, at the height you pass it. Two high on a floor of its style's stone, the chest on that floor
 * against the far wall. Monsters may appear in it, as in any dark pocket of beta's; what it never has is a spawner. Cut last of all, after the guards, into rock nothing else draws on, and only where everything
 * round it is plain building stuff inside the tower: so no air, water, light or cave reaches it, and in a simple tower it never backs onto the one-thick outer wall.
 */
final class HiddenRoom {
	private HiddenRoom() {
	}

	/** The ways that may run past a hidden room: a ladder, in a shaft of its own, or stairs, a few more turns cut in the rock. */
	static List<Way> ways(Random hiding, Floor up, Floor low, boolean ladderAllowed) {
		List<Way> list = new ArrayList<>();
		boolean ladder = ladderAllowed && hiding.nextBoolean();
		if (ladder) list.add(new Ways.Ladder(up, low));
		list.add(new Ways.Stairs(up, low, "stairs"));
		if (ladderAllowed && !ladder) list.add(new Ways.Ladder(up, low));
		return list;
	}

	/** One way a room could lie: {@code along} away from the way, from the cell two out from it, {@code across} from {@code a0} on, its floor at {@code r0}. */
	private static final class Lie {
		final int nx, nz, along, a0, wide, deep, r0;

		Lie(int nx, int nz, int along, int a0, int wide, int deep, int r0) {
			this.nx = nx;
			this.nz = nz;
			this.along = along;
			this.a0 = a0;
			this.wide = wide;
			this.deep = deep;
			this.r0 = r0;
		}

		int across() {
			return along + 1 & 3;
		}

		int x(int k, int a) {
			return nx + DX[along] * k + DX[across()] * a;
		}

		int z(int k, int a) {
			return nz + DZ[along] * k + DZ[across()] * a;
		}

		/** Where a complex room's niche is, in its far wall. */
		int nicheA() {
			return a0 + (wide - 1) / 2;
		}
	}

	static void cut(Tower t, Random hiding) {
		Way way = null;
		for (Way w : t.ways) {
			if (w.up.index == t.gapUnder) way = w;
		}
		if (way == null) return;
		boolean simple = t.style == Style.SIMPLE;
		// Across the way, then away from it: a rough cell two by three in a simple tower, a dressed one three square in a complex one, smaller where that won't go.
		int[][] sizes = simple ? new int[][] {{2, 3}, {3, 2}, {2, 2}} : new int[][] {{3, 3}, {3, 2}, {2, 2}};
		for (int[] size : sizes) {
			List<Lie> found = new ArrayList<>();
			// Its floor a cell over the ceiling of the floor below, or two where nothing's sunk into the floor above, as stairs pass different sides at different heights.
			for (int r0 = way.low.top() + 2; r0 <= way.low.top() + 3; r0++) {
				for (int sx = way.x - way.reachX(); sx <= way.x + way.reachX(); sx++) {
					for (int sz = way.z - way.reachZ(); sz <= way.z + way.reachZ(); sz++) {
						for (int y = r0 + 1; y <= r0 + 2; y++) {
							if (!passable(t.plan.id(sx, y, sz))) continue;
							for (int d = 0; d < 4; d++) {
								// Never behind a ladder: digging there takes the ladder down.
								if (way instanceof Ways.Ladder && d == WEST) continue;
								if (!building(t, sx + DX[d], y, sz + DZ[d])) continue;
								for (int o = 0; o < size[0]; o++) {
									Lie lie = new Lie(sx + 2 * DX[d], sz + 2 * DZ[d], d, -o, size[0], size[1], r0);
									if (fits(t, lie, r0, simple)) found.add(lie);
								}
							}
						}
					}
				}
			}
			if (!found.isEmpty()) {
				Lie lie = found.get(hiding.nextInt(found.size()));
				draw(t, way, lie, lie.r0, simple, hiding);
				return;
			}
		}
	}

	/** What's cut for the room: its floor's row, which is drawn again, and the air over it. */
	private static Set<Long> air(Lie lie, int r0, boolean simple) {
		Set<Long> air = new HashSet<>();
		for (int k = 0; k < lie.deep; k++) {
			for (int a = lie.a0; a < lie.a0 + lie.wide; a++) {
				for (int y = r0; y <= r0 + 2; y++) air.add(Plan.key(lie.x(k, a), y, lie.z(k, a)));
			}
		}
		if (!simple) {
			air.add(Plan.key(lie.x(lie.deep, lie.nicheA()), r0 + 1, lie.z(lie.deep, lie.nicheA())));
			air.add(Plan.key(lie.x(lie.deep, lie.nicheA()), r0 + 2, lie.z(lie.deep, lie.nicheA())));
		}
		return air;
	}

	private static final int[][] FACES = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

	/**
	 * All of it in solid stuff nothing holds, a cell in from the wall at least, with solid stuff all round it, a cell in from the wall too. A sunk pool's floor may be its ceiling, as one block keeps
	 * water out, and so may a foundry's floor under its lava, unless what's round the room is wood, which lava sets alight a little way off.
	 */
	private static boolean fits(Tower t, Lie lie, int r0, boolean simple) {
		Set<Long> air = air(lie, r0, simple);
		for (long c : air) {
			int x = Plan.keyX(c), y = Plan.keyY(c), z = Plan.keyZ(c);
			if (!building(t, x, y, z) || t.plan.held(x, y, z) || t.depth(x, z) < t.wall + 2) return false;
		}
		for (long c : air) {
			int x = Plan.keyX(c), y = Plan.keyY(c), z = Plan.keyZ(c);
			for (int[] f : FACES) {
				int ax = x + f[0], ay = y + f[1], az = z + f[2];
				if (air.contains(Plan.key(ax, ay, az))) continue;
				if (!building(t, ax, ay, az) || t.depth(ax, az) < t.wall + 1) return false;
				int id = t.plan.id(ax, ay, az);
				if ((id == PLANKS || id == LOG) && lavaNear(t, ax, ay, az)) return false;
			}
		}
		return true;
	}

	private static boolean lavaNear(Tower t, int x, int y, int z) {
		for (int dx = -3; dx <= 3; dx++) {
			for (int dy = -3; dy <= 3; dy++) {
				for (int dz = -3; dz <= 3; dz++) {
					int id = t.plan.id(x + dx, y + dy, z + dz);
					if (id == LAVA || id == FLOWING_LAVA) return true;
				}
			}
		}
		return false;
	}

	/** Plain building stuff, whole and opaque, that doesn't fall or melt and isn't anything a floor put there for itself. */
	private static boolean building(Tower t, int x, int y, int z) {
		if (t.plan.get(x, y, z) == Plan.KEEP || t.plan.fixture(x, y, z) != null) return false;
		switch (t.plan.id(x, y, z)) {
			case STONE: case COBBLE: case MOSSY: case BRICK: case SANDSTONE: case DOUBLE_SLAB: case PLANKS: case LOG: case DIRT: case OBSIDIAN: case CLAY:
			case COAL_ORE: case IRON_ORE: case GOLD_ORE: case DIAMOND_ORE: case REDSTONE_ORE:
				return true;
			default:
				return false;
		}
	}

	private static void draw(Tower t, Way way, Lie lie, int r0, boolean simple, Random hiding) {
		Plan p = t.plan;
		Palette pal = way.up.pal;
		Set<Long> air = air(lie, r0, simple);
		air.removeIf(c -> Plan.keyY(c) == r0);
		// A complex room is lined in the floor's trim, where nothing but the room sees it; the side the way passes stays as the way's wall.
		if (!simple) {
			List<long[]> lining = new ArrayList<>();
			for (long c : air) {
				for (int[] f : FACES) {
					int x = Plan.keyX(c) + f[0], y = Plan.keyY(c) + f[1], z = Plan.keyZ(c) + f[2];
					if (air.contains(Plan.key(x, y, z)) || !unseen(t, air, x, y, z)) continue;
					lining.add(new long[] {x, y, z});
				}
			}
			for (long[] c : lining) p.force((int) c[0], (int) c[1], (int) c[2], pal.trim.at((int) c[0], (int) c[1], (int) c[2]));
		}
		int x0 = Integer.MAX_VALUE, z0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, z1 = Integer.MIN_VALUE;
		for (int k = 0; k < lie.deep; k++) {
			for (int a = lie.a0; a < lie.a0 + lie.wide; a++) {
				int x = lie.x(k, a), z = lie.z(k, a);
				p.force(x, r0, z, simple ? pal.wall.at(x, r0, z) : pal.trim.at(x, r0, z));
				p.force(x, r0 + 1, z, AIR);
				p.force(x, r0 + 2, z, AIR);
				x0 = Math.min(x0, x);
				z0 = Math.min(z0, z);
				x1 = Math.max(x1, x);
				z1 = Math.max(z1, z);
			}
		}
		int cx, cz;
		if (simple) {
			// Against the far wall, and the odd corner of the ceiling left rough.
			int a = lie.a0 + hiding.nextInt(lie.wide);
			cx = lie.x(lie.deep - 1, a);
			cz = lie.z(lie.deep - 1, a);
			for (int k : new int[] {0, lie.deep - 1}) {
				for (int b : new int[] {lie.a0, lie.a0 + lie.wide - 1}) {
					int x = lie.x(k, b), z = lie.z(k, b);
					if (hiding.nextInt(3) == 0 && (x != cx || z != cz)) p.force(x, r0 + 2, z, pal.wall.at(x, r0 + 2, z));
				}
			}
		} else {
			// In a niche in the far wall at the floor's height, its head the trim.
			cx = lie.x(lie.deep, lie.nicheA());
			cz = lie.z(lie.deep, lie.nicheA());
			p.force(cx, r0 + 2, cz, AIR);
		}
		p.force(cx, r0 + 1, cz, cell(CHEST, backTo(lie.along)));
		p.fixture(cx, r0 + 1, cz, "chest:hidden");
		t.chests++;
		t.hidden = new int[] {x0, r0, z0, x1, r0 + 2, z1};
		t.hiddenChest = new int[] {cx, r0 + 1, cz};
	}

	/** Whether a cell next to the room has solid all round it but the room, so nothing else would see it changed. */
	private static boolean unseen(Tower t, Set<Long> air, int x, int y, int z) {
		for (int[] f : FACES) {
			int ax = x + f[0], ay = y + f[1], az = z + f[2];
			if (air.contains(Plan.key(ax, ay, az))) continue;
			if (t.plan.get(ax, ay, az) == Plan.KEEP || !solid(t.plan.id(ax, ay, az))) return false;
		}
		return true;
	}
}
