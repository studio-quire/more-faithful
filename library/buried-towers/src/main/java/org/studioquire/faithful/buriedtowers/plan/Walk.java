package org.studioquire.faithful.buriedtowers.plan;

import static org.studioquire.faithful.buriedtowers.plan.Blocks.*;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Makes sure a floor can be walked from where you come in to where you leave. Floors are drawn without looking at where their ways landed, so sometimes a wall stands in the way: then the wall has a
 * breach in it, and a gap has a few boards across it, which is also how a place that's been lived in and fallen apart looks.
 */
final class Walk {
	private static final int PLAIN = 1, SWIM = 3, BOARD = 5, BREAK = 7;

	private Walk() {
	}

	/** What it costs to stand in a column at this level of the floor, or 0 for never. */
	private static int cost(Floor f, int level, boolean swim, int x, int z) {
		if (!f.in(x, z)) return 0;
		int feet = f.p.id(x, level, z), head = f.p.id(x, level + 1, z), under = f.p.id(x, level - 1, z), deeper = f.p.id(x, level - 2, z);
		if (feet == LAVA || head == LAVA) return 0;
		if (fixed(f, x, level, z, feet) || fixed(f, x, level + 1, z, head)) return 0;
		if (!passable(feet) || !passable(head)) return BREAK;
		if (carries(under)) return PLAIN;
		if (swim && (under == WATER || feet == WATER || deeper == WATER)) return SWIM;
		// Boards laid against a chest would wall it in.
		for (int d = 0; d < 4; d++) {
			if (f.p.id(x + DX[d], level - 1, z + DZ[d]) == CHEST) return 0;
		}
		// Nor over a way's own well below the walking level, as boards across a stair's well take the headroom of whoever climbs it.
		return f.p.held(x, level - 1, z) && (level == f.stand || under == AIR && deeper != WATER) ? 0 : BOARD;
	}

	/** Part of a way, which a walk mustn't cut. */
	private static boolean fixed(Floor f, int x, int y, int z, int id) {
		return !passable(id) && f.p.held(x, y, z) && !f.p.isRock(x, y, z);
	}

	/** Moves a floor's place to stand to the nearest cell that's really good to stand in. */
	static void safeSpot(Floor f) {
		for (int reach = 0; reach < f.t.radius * 2; reach++) {
			for (int dx = -reach; dx <= reach; dx++) {
				for (int dz = -reach; dz <= reach; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != reach) continue;
					int x = f.safeX + dx, z = f.safeZ + dz;
					if (f.in(x, z) && f.p.id(x, f.stand, z) == AIR && f.p.id(x, f.stand + 1, z) == AIR && carries(f.p.id(x, f.stand - 1, z))) {
						f.safeX = x;
						f.safeZ = z;
						f.safeY = f.stand;
						return;
					}
				}
			}
		}
	}

	/** The way on starts below the floor's walking level: from the nearest of what's dry round the pit's foot to it, with boards to be walked over if it's out in the water. */
	static void toMouth(Floor f) {
		int ux = f.out.ux, uz = f.out.uz;
		double far = Math.hypot(ux - f.t.cx, uz - f.t.cz), toward = Math.max(far, 1);
		for (double at = f.rim() - 2; at > 1; at -= 0.5) {
			int x = f.t.cx + (int) Math.round((ux - f.t.cx) / toward * at), z = f.t.cz + (int) Math.round((uz - f.t.cz) / toward * at);
			if (f.in(x, z) && carries(f.p.id(x, f.leave - 1, z)) && !f.p.held(x, f.leave - 1, z)) {
				join(f, f.leave, false, x, z, ux, uz);
				return;
			}
		}
	}

	/** Joins two places on a floor, cutting and boarding only what the cheapest way needs. */
	static void join(Floor f, int fromX, int fromZ, int toX, int toZ) {
		join(f, f.stand, true, fromX, fromZ, toX, toZ);
	}

	private static void join(Floor f, int level, boolean swim, int fromX, int fromZ, int toX, int toZ) {
		int span = f.t.radius * 2 + 1, ox = f.t.cx - f.t.radius, oz = f.t.cz - f.t.radius;
		int[] best = new int[span * span];
		int[] from = new int[span * span];
		java.util.Arrays.fill(best, Integer.MAX_VALUE);
		java.util.Arrays.fill(from, -1);
		PriorityQueue<int[]> queue = new PriorityQueue<>((a, b) -> a[0] != b[0] ? Integer.compare(a[0], b[0]) : Integer.compare(a[1], b[1]));
		int start = (fromX - ox) * span + fromZ - oz, goal = (toX - ox) * span + toZ - oz;
		if (start < 0 || goal < 0 || start >= best.length || goal >= best.length) return;
		best[start] = 0;
		queue.add(new int[] {0, start});
		while (!queue.isEmpty()) {
			int[] top = queue.remove();
			if (top[0] > best[top[1]]) continue;
			if (top[1] == goal) break;
			int x = top[1] / span + ox, z = top[1] % span + oz;
			for (int d = 0; d < 4; d++) {
				int nx = x + DX[d], nz = z + DZ[d];
				if (nx < ox || nz < oz || nx >= ox + span || nz >= oz + span) continue;
				int c = cost(f, level, swim, nx, nz);
				if (c == 0) continue;
				int n = (nx - ox) * span + nz - oz;
				if (top[0] + c < best[n]) {
					best[n] = top[0] + c;
					from[n] = top[1];
					queue.add(new int[] {best[n], n});
				}
			}
		}
		if (best[goal] == Integer.MAX_VALUE) return;
		List<Integer> path = new ArrayList<>();
		for (int at = goal; at != -1 && at != start; at = from[at]) path.add(at);
		for (int at : path) {
			int x = at / span + ox, z = at % span + oz;
			int c = cost(f, level, swim, x, z);
			if (c == BREAK) {
				for (int y = level; y <= level + 1; y++) {
					if (!passable(f.p.id(x, y, z))) f.p.force(x, y, z, AIR);
				}
				if (!carries(f.p.id(x, level - 1, z))) f.p.force(x, level - 1, z, cell(PLANKS, 0));
				f.notes.add("breach");
			} else if (c == BOARD) {
				// Boards would catch, over lava. Over water, stones built up from the bottom instead, as boards on it would be a place a swimmer can't come up.
				if (f.p.id(x, level - 2, z) == WATER) {
					for (int y = level - 1; y >= f.y && (y == level - 1 || f.p.id(x, y, z) == WATER); y--) f.p.force(x, y, z, f.pal.wall.at(x, y, z));
				} else f.p.force(x, level - 1, z, cell(f.p.id(x, level - 1, z) == LAVA ? COBBLE : PLANKS, 0));
				f.notes.add("boards");
			}
		}
	}
}
