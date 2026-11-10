package org.studioquire.faithful.buriedtowers.plan;

import static org.studioquire.faithful.buriedtowers.plan.Blocks.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The simple style's kinds of floor: the same ideas as {@link Floors}, crudely built, as beta's dungeons are. Plain walls and floors of one stuff, openings that are only gaps, only what a floor
 * needs for its idea to read, and decay throughout. Names as the complex kinds', since a floor's kind is what it was for, not how well. Every room is small, nine or eleven across inside a wall
 * one thick, so each is drawn round the ways down rather than the other way about: whatever stands in the room gives way to them.
 */
final class SimpleFloors {
	static final FloorKind GATEHALL = new Stump(), VAULT = new Vault();
	static final List<FloorKind> ALL = Arrays.asList(new Barracks(), new Hall(), new CellBlock(), new Cellar(), new Crypt(), new Library(), new Cistern(), new Bridges(), new Gallery(), new Foundry(), new Collapse(),
			Granary.SIMPLE, RootCellar.SIMPLE, MushroomFarm.SIMPLE, SpiderNest.SIMPLE, Armoury.SIMPLE, Maze.SIMPLE, FloodedHall.SIMPLE,
			SandFloor.SIMPLE, SpiralGallery.SIMPLE, Aquarium.SIMPLE, LavaLake.SIMPLE, JukeboxHall.SIMPLE, PortalRoom.SIMPLE);

	private SimpleFloors() {
	}

	/** Heaps where some of the ceiling or wall came down, somewhere on the floor and clear of the middle, and webs in the corners. */
	static void decay(Floor f, int heaps, int webs) {
		int half = f.half();
		for (int tries = 0; tries < heaps * 10 && heaps > 0; tries++) {
			int u = f.r.nextInt(half * 2 + 1) - half, v = f.r.nextInt(half * 2 + 1) - half;
			if (!f.inFrame(u, v) || Math.hypot(u, v) < 2 || f.idAt(u, f.y, v) != AIR) continue;
			f.rubble(f.frame.x(u, v), f.frame.z(u, v), 1, f.rubbleMat());
			heaps--;
		}
		f.cobwebCorners(webs);
	}

	/**
	 * A chest against the wall at about this angle, or the nearest place round from it that's free: in a small room the ways down take a good part of the wall. Never where lava could light a
	 * fire beside it. Where it went, or null if nowhere was.
	 */
	static int[] onWall(Floor f, double degrees, String loot) {
		for (int k = 0; k < 12; k++) {
			int[] c = f.against(degrees + (k % 2 == 0 ? 1 : -1) * 30 * ((k + 1) / 2));
			int x = f.frame.x(c[0], c[1]), z = f.frame.z(c[0], c[1]);
			if (f.idAt(c[0], f.y, c[1]) != AIR || f.idAt(c[0], f.y + 1, c[1]) != AIR || f.p.held(x, f.y, z) || f.p.held(x, f.y + 1, z) || f.lavaReaches(c[0], f.y, c[1])) continue;
			f.chestAt(c[0], f.y, c[1], Floor.outward(c[0], c[1]), loot);
			return c;
		}
		return null;
	}

	/** Where a run of a floor's own stuff is broken up, boards gone or flags lifted, so it isn't laid whole. */
	static Mat worn(Floor f, Mat laid) {
		return Mat.patched(laid, Mat.of(COBBLE), f.t.noise, 0.25, 3);
	}

	/** The stump above the land, open to the sky: the ground coming back over its floor, a heap or two where the wall came in, and what was left lying against the wall. */
	private static final class Stump extends FloorKind {
		Stump() {
			super("gate hall", Size.MEDIUM, 5, 5, -1, 0);
		}

		@Override
		double weight(Tower t) {
			return 0;
		}

		@Override
		void draw(Floor f) {
			int half = f.half();
			for (int x = f.t.cx - half; x <= f.t.cx + half; x++) {
				for (int z = f.t.cz - half; z <= f.t.cz + half; z++) {
					if (f.in(x, z) && f.p.id(x, f.y, z) == AIR && f.t.noise.at(x, 31, z, 3) > 0.42) growth(f, x, z);
				}
			}
			for (int falls = 1 + f.r.nextInt(2); falls > 0; falls--) {
				int[] c = f.against(f.r.nextInt(360));
				f.rubble(f.frame.x(c[0], c[1]), f.frame.z(c[0], c[1]), 2, f.rubbleMat());
			}
			onWall(f, f.r.nextInt(360), "scraps");
			if (f.t.is(Climate.WOODED, Climate.WET) && f.r.nextInt(3) == 0) tree(f);
		}

		private void growth(Floor f, int x, int z) {
			if (f.t.climate == Climate.DESERT) {
				f.set(x, f.y - 1, z, cell(SAND, 0));
				if (f.r.nextInt(9) == 0) f.set(x, f.y, z, cell(DEAD_BUSH, 0));
				return;
			}
			f.set(x, f.y - 1, z, cell(GRASS, 0));
			if (f.t.climate == Climate.COLD) return;
			int roll = f.r.nextInt(10);
			if (roll < 4) f.set(x, f.y, z, cell(TALL_GRASS, 1));
			else if (roll == 4) f.set(x, f.y, z, cell(f.r.nextBoolean() ? ROSE : DANDELION, 0));
		}

		/** A tree that's grown up in it since the roof went. */
		private void tree(Floor f) {
			int half = f.half() - 3;
			for (int tries = 0; tries < 30; tries++) {
				int x = f.t.cx - half + f.r.nextInt(half * 2 + 1), z = f.t.cz - half + f.r.nextInt(half * 2 + 1);
				if (!f.in(x, z) || f.p.held(x, f.y, z) || f.p.id(x, f.y, z) != AIR) continue;
				int wood = f.t.climate == Climate.WOODED && f.r.nextBoolean() ? 2 : 0;
				int top = f.y + 5 + f.r.nextInt(3);
				f.p.force(x, f.y - 1, z, cell(DIRT, 0));
				for (int yy = f.y; yy <= top; yy++) f.p.force(x, yy, z, cell(LOG, wood));
				for (int yy = top - 3; yy <= top + 1; yy++) {
					int reach = yy > top - 1 ? 1 : 2;
					for (int dx = -reach; dx <= reach; dx++) {
						for (int dz = -reach; dz <= reach; dz++) {
							if (Math.abs(dx) + Math.abs(dz) > reach + 1 || dx == 0 && dz == 0 && yy <= top) continue;
							if (f.p.id(x + dx, yy, z + dz) == AIR) f.p.force(x + dx, yy, z + dz, cell(LEAVES, wood));
						}
					}
				}
				return;
			}
		}
	}

	/** Where the garrison slept: a few beds along the wall, heads to it, some gone; a cold furnace and the mess's chest. */
	private static final class Barracks extends FloorKind {
		Barracks() {
			super("barracks", Size.MEDIUM, 5, 6, 0, 0.75);
		}

		@Override
		double weight(Tower t) {
			return t.purpose == Tower.Purpose.GARRISON ? 4 : 0.7;
		}

		@Override
		void draw(Floor f) {
			int beds = f.half() >= 5 ? 7 : 6, skip = f.r.nextInt(beds), turn = f.r.nextInt(360);
			for (int i = 0; i < beds; i++) {
				// Not all the way round: the stretch past the last bed is for the furnace and the mess.
				if (i == skip || i == beds - 1 || f.r.nextInt(4) == 0) continue;
				int[] head = f.against(turn + 360.0 * i / beds);
				int d = Floor.outward(head[0], head[1]), fu = head[0] - DX[d], fv = head[1] - DZ[d];
				if (f.idAt(head[0], f.y, head[1]) != AIR || f.idAt(fu, f.y, fv) != AIR) continue;
				int dir = f.frame.dir(d);
				f.put(fu, f.y, fv, cell(BED, bed(dir)));
				f.put(head[0], f.y, head[1], cell(BED, bed(dir) | 8));
			}
			double gap = turn + 360.0 * (beds - 1) / beds;
			int[] oven = f.against(gap - 12), mess = f.against(gap + 12);
			if (f.idAt(oven[0], f.y, oven[1]) == AIR) f.put(oven[0], f.y, oven[1], cell(FURNACE, backTo(f.frame.dir(Floor.outward(oven[0], oven[1])))));
			onWall(f, gap + 12, "kitchen");
			int[] bunk = f.against(gap + 180);
			if (f.idAt(bunk[0], f.y, bunk[1]) == AIR && f.r.nextBoolean()) f.chestAt(bunk[0], f.y, bunk[1], Floor.outward(bunk[0], bunk[1]), "bunk");
			int[] s = Floor.polar(f.r.nextInt(360), 1.5);
			f.spawnerAt(s[0], f.y, s[1]);
			decay(f, f.r.nextInt(2), 10);
		}
	}

	/**
	 * Cut out of the rock: a passage straight across, and either side of it cells two wide behind a wall of the rock, most doorways only gaps, a few with a wooden door left, one behind iron with
	 * the lever on the passage's side. One prisoner dug through to the next.
	 */
	private static final class CellBlock extends FloorKind {
		CellBlock() {
			super("cell block", Size.SHORT, 4, 5, 0.1, 0.92);
		}

		@Override
		double weight(Tower t) {
			return t.purpose == Tower.Purpose.GARRISON ? 3 : t.purpose == Tower.Purpose.ORDER ? 1.2 : 0.6;
		}

		@Override
		Mat overhead(Floor f) {
			return f.pal.wall;
		}

		@Override
		void draw(Floor f) {
			int half = f.half();
			f.rock(-half, f.y, -half, half, f.top(), half, f.pal.wall);
			f.cut(-half, f.y, -1, half, f.top() - 1, 1);
			// The cells, from one end of the passage to the other, either side.
			List<int[]> cells = new ArrayList<>();
			for (int s = -1; s <= 1; s += 2) {
				for (int a = -half + 1; a < half; a += 3) {
					if (f.inFrame(a, 3 * s) || f.inFrame(a + 1, 3 * s)) cells.add(new int[] {a, s});
				}
			}
			int strong = f.r.nextInt(cells.size()), dug = (strong + 1 + f.r.nextInt(cells.size() - 1)) % cells.size();
			for (int i = 0; i < cells.size(); i++) prison(f, cells.get(i)[0], cells.get(i)[1], i == strong, i == dug);
			int end = f.r.nextBoolean() ? EAST : WEST;
			int[] arms = f.toWall(0, 0, end);
			f.chestAt(arms[0], f.y, arms[1], end, "arms");
			decay(f, 0, 8);
		}

		/** A cell two wide from the passage's wall back to the tower's, two high, a gap through into it, and what was left in it. */
		private void prison(Floor f, int a, int s, boolean strong, boolean dug) {
			int half = f.half(), toRoom = s > 0 ? SOUTH : NORTH, side = f.frame.dir(opposite(toRoom));
			int[] back = null;
			for (int u = a; u <= a + 1; u++) {
				for (int k = 3; k <= half; k++) {
					if (!f.inFrame(u, k * s)) break;
					f.cut(u, f.y, k * s, u, f.y + 1, k * s);
					if (back == null || Math.abs(k) > Math.abs(back[1])) back = new int[] {u, k * s};
				}
			}
			if (back == null) return;
			int du = f.inFrame(a, 3 * s) ? a : a + 1;
			f.cut(du, f.y, 2 * s, du, f.y + 1, 2 * s);
			int out = s > 0 ? SOUTH : NORTH;
			if (strong) {
				f.put(du, f.y, 2 * s, cell(IRON_DOOR, doorOn(side)));
				f.put(du, f.y + 1, 2 * s, cell(IRON_DOOR, doorOn(side) | 8));
				// On the passage's side, on the rock beside the doorway.
				for (int lu : new int[] {du - 1, du + 1}) {
					if (!f.inFrame(lu, s) || f.idAt(lu, f.y + 1, s) != AIR || !solid(f.idAt(lu, f.y + 1, 2 * s))) continue;
					f.put(lu, f.y + 1, s, cell(LEVER, leverOn(f.frame.dir(toRoom))));
					break;
				}
				f.chestAt(back[0], f.y, back[1], out, "cell");
				return;
			}
			if (f.r.nextInt(3) == 0) {
				f.put(du, f.y, 2 * s, cell(WOOD_DOOR, doorOn(side)));
				f.put(du, f.y + 1, 2 * s, cell(WOOD_DOOR, doorOn(side) | 8));
			}
			if (dug) {
				// Through the rock between this cell and the next along.
				f.chestAt(back[0], f.y, back[1], out, "prisoner");
				for (int u : new int[] {a - 1, a + 2}) {
					if (!f.inFrame(u, 3 * s) || f.idAt(u, f.y, 3 * s) == AIR) continue;
					f.cut(u, f.y, 3 * s, u, f.y + 1, 3 * s);
					if (f.r.nextBoolean()) f.put(u, f.y, 3 * s, cell(GRAVEL, 0));
					break;
				}
				return;
			}
			int what = f.r.nextInt(5);
			if (what == 0) f.chestAt(back[0], f.y, back[1], out, "cell");
			else if (what == 1) f.spawnerAt(back[0] == a ? a + 1 : a, f.y, back[1], f.r.nextBoolean() ? "Skeleton" : "Zombie");
			else if (what == 2) f.web(f.frame.x(back[0], back[1]), f.y + 1, f.frame.z(back[0], back[1]));
		}
	}

	/** A low room for stores: barrels on end round the wall, a pumpkin here and there, chests among them, most of it long gone, and part of the ceiling down. */
	private static final class Cellar extends FloorKind {
		Cellar() {
			super("cellar", Size.SHORT, 4, 5, 0, 0.7);
		}

		@Override
		double weight(Tower t) {
			return t.purpose == Tower.Purpose.STORES ? 4 : 0.9;
		}

		@Override
		Mat overhead(Floor f) {
			return f.pal.wall;
		}

		@Override
		void draw(Floor f) {
			double far = f.rim() - 0.6;
			int count = (int) (2 * Math.PI * far / 1.6);
			for (int i = 0; i < count; i++) {
				int[] c = f.against(360.0 * i / count);
				if (f.idAt(c[0], f.y, c[1]) != AIR) continue;
				int roll = f.r.nextInt(12);
				if (roll == 0) f.chestAt(c[0], f.y, c[1], Floor.outward(c[0], c[1]), "stores");
				else if (roll == 1) f.put(c[0], f.y, c[1], cell(PUMPKIN, f.r.nextInt(4)));
				else if (roll < 8) {
					f.put(c[0], f.y, c[1], cell(LOG, 0));
					if (f.r.nextInt(3) == 0) f.put(c[0], f.y + 1, c[1], cell(LOG, 0));
				}
			}
			onWall(f, f.r.nextInt(360), "stores");
			int[] s = Floor.polar(f.r.nextInt(360), 1.5);
			f.spawnerAt(s[0], f.y, s[1]);
			decay(f, 2, 12);
		}
	}

	/** One plain room: cases of books on the wall, a few, most of what was there gone, and a corner the fire had. */
	private static final class Library extends FloorKind {
		Library() {
			super("library", Size.MEDIUM, 5, 6, 0.1, 0.8);
		}

		@Override
		double weight(Tower t) {
			return t.purpose == Tower.Purpose.ORDER ? 5 : 0.6;
		}

		@Override
		Mat underfoot(Floor f) {
			return worn(f, Mat.of(PLANKS));
		}

		@Override
		void draw(Floor f) {
			int turn = 90 * f.r.nextInt(4);
			// Four places on the wall: three cases of books and one of bare boards where there were more.
			for (int i = 0; i < 4; i++) bookcase(f, turn + 90 * i, i == 3 ? cell(PLANKS, 0) : cell(BOOKSHELF, 0));
			onWall(f, turn + 45, "books");
			if (f.r.nextBoolean()) onWall(f, turn + 225, "maps");
			int[] s = Floor.polar(turn + 135, f.rim() - 2.5);
			f.spawnerAt(s[0], f.y, s[1]);
			fire(f, turn + 315);
			decay(f, 1, 12);
		}

		/** Three wide and two high on the wall. */
		private void bookcase(Floor f, int degrees, int what) {
			int[] c = f.against(degrees);
			int d = Floor.outward(c[0], c[1]), side = d + 1 & 3;
			for (int w = -1; w <= 1; w++) {
				int[] at = f.toWall(c[0] + DX[side] * w, c[1] + DZ[side] * w, d);
				f.box(at[0], f.y, at[1], at[0], f.y + 1, at[1], what);
			}
		}

		/** A stretch the fire had: shelves and boards gone, down to what's under them. */
		private void fire(Floor f, int degrees) {
			int[] c = Floor.polar(degrees, f.rim() - 2);
			int size = 2 + f.r.nextInt(2);
			for (int u = c[0] - size; u <= c[0] + size; u++) {
				for (int v = c[1] - size; v <= c[1] + size; v++) {
					if (Math.hypot(u - c[0], v - c[1]) > size || !f.inFrame(u, v)) continue;
					for (int yy = f.y; yy <= f.top(); yy++) {
						int id = f.idAt(u, yy, v);
						if (id == BOOKSHELF || id == PLANKS || id == CHEST || id == SPAWNER) f.put(u, yy, v, Mat.of(AIR));
					}
					f.put(u, f.y - 1, v, f.burnt());
				}
			}
		}
	}

	/**
	 * A shaft nearly the floor's width down to black water, a plank bridge straight across it from rim to rim, rotten through now and then, and the stub of a second that's fallen. A ledge round the
	 * water to climb out on, and ladders up the shaft, which is where the way on starts; a way coming in lands on a stage of its own at the rim.
	 */
	private static final class Bridges extends FloorKind {
		Bridges() {
			super("bridges over a pit", Size.TALL, 13, 18, 0.15, 0.9);
		}

		@Override
		double weight(Tower t) {
			return t.purpose == Tower.Purpose.MINE ? 2.5 : 1.1;
		}

		@Override
		int standOffset(int h) {
			return h - 4;
		}

		@Override
		int waterOffset(int h) {
			return 1;
		}

		@Override
		int leaveOffset(int h) {
			return waterOffset(h) + 2;
		}

		@Override
		boolean landsOnStage() {
			return true;
		}

		@Override
		boolean openAt(Floor f, int x, int z) {
			return Math.hypot(x - f.t.cx, z - f.t.cz) < pit(f);
		}

		private static double pit(Floor f) {
			return f.rim() - 1.2;
		}

		@Override
		void draw(Floor f) {
			int half = f.half(), rim = f.stand - 1;
			double pit = pit(f);
			Mat wall = f.pal.wall;
			for (int u = -half - 1; u <= half + 1; u++) {
				for (int v = -half - 1; v <= half + 1; v++) {
					if (!f.inFrame(u, v)) continue;
					double far = Math.hypot(u, v);
					if (far >= pit) f.box(u, f.y, v, u, rim, v, wall);
					// The ledge, standing on the bottom: water under it would be a place a swimmer can't come up.
					else if (far >= pit - 1) f.box(u, f.y, v, u, f.waterTop + 1, v, wall);
					else f.box(u, f.y, v, u, f.waterTop, v, Mat.of(WATER));
				}
			}
			int across = f.r.nextInt(2), stub = across + 1 + 2 * f.r.nextInt(2) & 3;
			bridge(f, across, rim, -half - 1, half + 1, f.r.nextInt(3) == 0);
			bridge(f, stub, rim, 1, (int) pit / 2 + 1, false);
			// What went over with it, on the ledge at the foot.
			for (int tries = 0; tries < 12; tries++) {
				int[] c = Floor.polar(f.r.nextInt(360), pit - 0.6);
				if (f.idAt(c[0], f.waterTop + 1, c[1]) != wall.at(0, 0, 0) && !solid(f.idAt(c[0], f.waterTop + 1, c[1]))) continue;
				boolean under = false;
				for (int yy = f.waterTop + 2; yy <= rim + 1; yy++) under |= f.p.held(f.frame.x(c[0], c[1]), yy, f.frame.z(c[0], c[1]));
				if (under) continue;
				f.chestAt(c[0], f.waterTop + 2, c[1], Floor.outward(c[0], c[1]), "pit");
				break;
			}
			int[] s = Floor.polar(f.r.nextInt(360), pit + 0.4);
			f.spawnerAt(s[0], f.stand, s[1]);
			ladders(f, rim, pit);
			f.cobwebCorners(8);
		}

		/** Two planks wide along a quarter's line, from {@code from} to {@code to} out from the middle; a rotten one has lost its middle. Not where a way's stage stands. */
		private void bridge(Floor f, int d, int rim, int from, int to, boolean rotten) {
			int du = DX[d], dv = DZ[d];
			for (int k = from; k <= to; k++) {
				if (rotten && Math.abs(k) <= 1) continue;
				for (int w = 0; w <= 1; w++) {
					int u = du * k + dv * w, v = dv * k + du * w;
					if (f.inFrame(u, v) && f.p.held(f.frame.x(u, v), rim, f.frame.z(u, v))) continue;
					f.put(u, rim, v, cell(PLANKS, 0));
				}
			}
		}

		/** A long ladder up the shaft on each side, on the first bit of wall that's free. */
		private void ladders(Floor f, int rim, double pit) {
			for (int k = 0; k < 2; k++) {
				for (int a : new int[] {0, 30, -30, 60, -60, 90, -90}) {
					int[] c = Floor.polar((k == 0 ? 60 : 240) + a, pit - 0.6);
					int d = Floor.outward(c[0], c[1]);
					int x = f.frame.x(c[0], c[1]), z = f.frame.z(c[0], c[1]);
					if (f.p.held(x, f.waterTop + 1, z) || f.p.held(x, rim, z)) continue;
					if (!solid(f.idAt(c[0] + DX[d], rim, c[1] + DZ[d]))) continue;
					for (int yy = f.waterTop + 1; yy <= rim; yy++) {
						f.put(c[0] + DX[d], yy, c[1] + DZ[d], f.pal.wall);
						f.put(c[0], yy, c[1], cell(LADDER, backTo(f.frame.dir(d))));
					}
					break;
				}
			}
		}
	}

	/** Two floors that have become one: what's left of the upper floor clinging to the wall in a ragged shelf, and the rest of it in heaps on the lower one. */
	private static final class Collapse extends FloorKind {
		Collapse() {
			super("collapsed hall", Size.TALL, 13, 18, 0.1, 0.95);
		}

		@Override
		double weight(Tower t) {
			return 1.3;
		}

		@Override
		boolean openWalls() {
			return true;
		}

		@Override
		Mat underfoot(Floor f) {
			return worn(f, f.pal.floor);
		}

		@Override
		void draw(Floor f) {
			int half = f.half(), upper = f.y + f.h / 2, reach = half + 1;
			double rim = f.rim();
			Noise n = new Noise(f.t.seed ^ f.index * 0xFA11L);
			for (int u = -reach; u <= reach; u++) {
				for (int v = -reach; v <= reach; v++) {
					if (!f.inFrame(u, v)) continue;
					if (rim - Math.hypot(u, v) <= 0.5 + n.at(u, 9, v, 4) * 3) f.put(u, upper, v, f.pal.floor);
				}
			}
			int[] chest = onWall(f, f.r.nextInt(360), "hall");
			if (chest == null) chest = new int[] {99, 99};
			int heaps = 1 + f.r.nextInt(2);
			for (int i = 0; i < heaps; i++) {
				double a = f.r.nextDouble() * Math.PI * 2, far = (0.15 + f.r.nextDouble() * 0.5) * half;
				heap(f, (int) (Math.cos(a) * far), (int) (Math.sin(a) * far), i == 0 ? 3 : 2, i == 0 ? upper - f.y : 2 + f.r.nextInt(2), chest);
			}
			if (f.idAt(-half / 2, f.y, half / 2) == AIR) f.spawnerAt(-half / 2, f.y, half / 2);
			int[] shelf = f.toWall(0, 0, EAST);
			f.spawner(f.frame.x(shelf[0], shelf[1]), upper + 1, f.frame.z(shelf[0], shelf[1]));
			f.cobwebCorners(18);
		}

		/** A cone of what came down, gravel with stone through it, packed so it doesn't shift, tall enough to climb back up to the shelf from. */
		private void heap(Floor f, int cu, int cv, int radius, int tall, int[] keep) {
			Mat rubble = f.rubbleMat();
			for (int u = cu - radius; u <= cu + radius; u++) {
				for (int v = cv - radius; v <= cv + radius; v++) {
					double far = Math.hypot(u - cu, v - cv);
					if (far > radius || !f.inFrame(u, v) || Math.max(Math.abs(u - keep[0]), Math.abs(v - keep[1])) <= 1) continue;
					int rise = (int) Math.round((radius - far) / radius * tall);
					for (int k = 0; k < rise; k++) {
						if (f.idAt(u, f.y + k, v) == AIR) f.put(u, f.y + k, v, rubble);
					}
				}
			}
		}
	}

	/**
	 * A low hall on a ring of plain pillars of the wall's own stuff, joined under the ceiling by lintels of it, so the ring reads as holding the roof up; one now and then snapped off with what
	 * came down round it, and a chest left at the foot of one.
	 */
	private static final class Hall extends FloorKind {
		Hall() {
			super("pillared hall", Size.MEDIUM, 5, 6, 0.05, 1);
		}

		@Override
		double weight(Tower t) {
			return t.purpose == Tower.Purpose.ORDER ? 3 : 1.2;
		}

		@Override
		Mat overhead(Floor f) {
			return f.pal.wall;
		}

		@Override
		void draw(Floor f) {
			int count = f.half() >= 5 ? 6 : 4, turn = f.r.nextInt(360);
			double ring = f.rim() - 2;
			// The ways take much of a small room: the ring turned, and a little nearer the wall if need be, to where the most pillars stand clear of them.
			int most = -1, bestTurn = turn;
			double bestRing = ring;
			for (double rr : new double[] {f.rim() - 2, f.rim() - 1.6}) {
				for (int k = 0; k < 12; k++) {
					int clear = 0;
					for (int i = 0; i < count; i++) {
						int[] c = Floor.polar(turn + 360.0 * k / count / 12 + 360.0 * i / count, rr);
						if (standing(f, c[0], c[1])) clear++;
					}
					if (clear > most) {
						most = clear;
						bestTurn = turn + (int) Math.round(360.0 * k / count / 12);
						bestRing = rr;
					}
				}
			}
			turn = bestTurn;
			ring = bestRing;
			boolean chest = false;
			int[][] at = new int[count][];
			boolean[] whole = new boolean[count];
			for (int i = 0; i < count; i++) {
				at[i] = Floor.polar(turn + 360.0 * i / count, ring);
				if (f.r.nextInt(5) == 0) {
					if (f.pier(at[i][0], at[i][1], at[i][0], at[i][1], f.y + f.r.nextInt(2), f.pal.wall)) f.rubble(f.frame.x(at[i][0], at[i][1]), f.frame.z(at[i][0], at[i][1]), 1, f.rubbleMat());
				} else if (whole[i] = f.pier(at[i][0], at[i][1], at[i][0], at[i][1], f.top(), f.pal.wall)) {
					if (!chest) {
						int d = Floor.outward(at[i][0], at[i][1]);
						chest = true;
						f.chestAt(at[i][0] - DX[d], f.y, at[i][1] - DZ[d], d, "hall");
					}
				}
			}
			for (int i = 0; i < count; i++) {
				int j = (i + 1) % count;
				if (whole[i] && whole[j]) f.beam(at[i][0], at[i][1], at[j][0], at[j][1], f.top(), f.pal.wall);
			}
			int[] s = Floor.polar(turn + 180.0 / count, ring);
			f.spawnerAt(s[0], f.y, s[1]);
			f.spawnerAt(-s[0], f.y, -s[1]);
			decay(f, 1, 10);
		}

		/** Whether a pillar could stand here floor to ceiling: nothing held by a way, nothing there. */
		private static boolean standing(Floor f, int u, int v) {
			int x = f.frame.x(u, v), z = f.frame.z(u, v);
			for (int yy = f.y; yy <= f.top(); yy++) if (f.p.held(x, yy, z) || f.idAt(u, yy, v) != AIR) return false;
			return true;
		}
	}

	/**
	 * A low room of the dead: niches cut into the wall all round, where they lay, in two rows staggered where the room's high enough, a few fallen in or webbed over; a tomb or two of the plain
	 * stuff on the floor, two long, and a chest by the wall where the altar was.
	 */
	private static final class Crypt extends FloorKind {
		Crypt() {
			super("crypt", Size.SHORT, 4, 5, 0.2, 1);
		}

		@Override
		double weight(Tower t) {
			return t.purpose == Tower.Purpose.ORDER ? 3.5 : 0.7;
		}

		@Override
		Mat overhead(Floor f) {
			return f.pal.wall;
		}

		@Override
		void draw(Floor f) {
			int turn = 45 * f.r.nextInt(8);
			niches(f);
			onWall(f, turn, "crypt");
			for (int k = 0; k < 2; k++) {
				int[] c = Floor.polar(turn + 120 + 120 * k + f.r.nextInt(30) - 15, 1.6);
				int d = Floor.outward(c[0], c[1]), across = d + 1 & 3;
				if (f.idAt(c[0], f.y, c[1]) != AIR || f.idAt(c[0] + DX[across], f.y, c[1] + DZ[across]) != AIR) continue;
				f.put(c[0], f.y, c[1], f.pal.wall);
				f.put(c[0] + DX[across], f.y, c[1] + DZ[across], f.pal.wall);
			}
			int[] s = Floor.polar(turn + 180, 1.5);
			f.spawnerAt(s[0], f.y, s[1], f.depth > 0.6 ? "Skeleton" : "Zombie");
			decay(f, 0, 12);
		}

		/**
		 * Every other cell of the wall, a block up from the floor, and in a room five high the ones between two higher. Cut through the wall, which is one thick, and a cell on into the rock,
		 * with a slab to lie on at the back, so it reads as a place for the dead and not a chip out of the wall; the rock round it made good in the wall's stuff wherever it shows, so no cave
		 * or water comes in. Not where a way or anything else stands in front.
		 */
		private static void niches(Floor f) {
			Tower t = f.t;
			List<int[]> ring = new ArrayList<>();
			for (int x = t.cx - t.radius; x <= t.cx + t.radius; x++) {
				for (int z = t.cz - t.radius; z <= t.cz + t.radius; z++) if (t.depth(x, z) == t.wall) ring.add(new int[] {x, z});
			}
			ring.sort((a, b) -> Double.compare(Math.atan2(a[1] - t.cz, a[0] - t.cx), Math.atan2(b[1] - t.cz, b[0] - t.cx)));
			boolean two = f.top() >= f.y + 4;
			for (int i = 0; i < ring.size(); i++) {
				if (i % 2 == 1 && !two) continue;
				int x = ring.get(i)[0], z = ring.get(i)[1], yy = i % 2 == 0 ? f.y + 1 : f.y + 3;
				int in = -1;
				for (int d = 0; d < 4; d++) {
					int ix = x + DX[d], iz = z + DZ[d];
					if (t.depth(ix, iz) > t.wall && f.p.id(ix, yy, iz) == AIR && !f.p.held(ix, yy, iz) && !f.p.held(ix, yy - 1, iz)) in = d;
				}
				if (in < 0) continue;
				int bx = x - DX[in], bz = z - DZ[in];
				if (t.depth(bx, bz) != 0 || f.p.held(x, yy, z) || f.p.held(bx, yy, bz) || f.r.nextInt(6) == 0) continue;
				f.p.force(x, yy, z, f.r.nextInt(5) == 0 ? cell(WEB, 0) : AIR);
				f.p.force(bx, yy, bz, cell(SLAB, f.pal.slab));
				for (int[] c : new int[][] {{x, z}, {bx, bz}}) {
					for (int d = 0; d < 4; d++) {
						int ox = c[0] + DX[d], oz = c[1] + DZ[d];
						if (t.depth(ox, oz) == 0 && (ox != bx || oz != bz)) f.p.force(ox, yy, oz, f.pal.wall.at(ox, yy, oz));
					}
				}
				f.p.force(bx, yy - 1, bz, f.pal.wall.at(bx, yy - 1, bz));
				f.p.force(bx, yy + 1, bz, f.pal.wall.at(bx, yy + 1, bz));
			}
		}
	}

	/** Black water three deep over the whole floor, a ledge round the wall above it, a few plain columns up out of it, and a causeway across, built up from the bottom, with a gap where it's fallen in. */
	private static final class Cistern extends FloorKind {
		Cistern() {
			super("cistern", Size.MEDIUM, 8, 10, 0.2, 0.95);
		}

		@Override
		double weight(Tower t) {
			return t.is(Climate.DESERT) ? 0.5 : t.is(Climate.WET) ? 2.5 : 1.4;
		}

		@Override
		int waterOffset(int h) {
			return 2;
		}

		@Override
		int standOffset(int h) {
			return waterOffset(h) + 2;
		}

		@Override
		Mat overhead(Floor f) {
			return f.pal.wall;
		}

		@Override
		void draw(Floor f) {
			int half = f.half(), walk = f.stand - 1;
			double rim = f.rim();
			f.fill(f.t.cx - half, f.y, f.t.cz - half, f.t.cx + half, f.waterTop, f.t.cz + half, Mat.of(WATER));
			for (int u = -half; u <= half; u++) {
				for (int v = -half; v <= half; v++) {
					if (f.inFrame(u, v) && Math.hypot(u, v) >= rim - 1) f.box(u, f.y, v, u, walk, v, f.pal.wall);
				}
			}
			int turn = 45 * f.r.nextInt(2);
			for (int i = 0; i < 4; i++) {
				int[] at = Floor.polar(turn + 45 + 90 * i, 2.2);
				f.pier(at[0], at[1], at[0], at[1], f.top(), f.pal.wall);
			}
			int gap = f.r.nextBoolean() ? 2 : -2;
			// Solid from the bottom, as water under it would be a place a swimmer can't come up.
			for (int u = -half; u <= half; u++) {
				if (u == gap || Math.abs(u) >= rim - 1) continue;
				f.box(u, f.y, 0, u, walk, 0, f.pal.wall);
			}
			for (int tries = 0; tries < 10; tries++) {
				int u = f.r.nextInt(half * 2 + 1) - half, v = (f.r.nextBoolean() ? 1 : -1) * (2 + f.r.nextInt(2));
				if (f.idAt(u, f.y, v) == WATER && f.idAt(u, f.y + 1, v) == WATER) {
					f.chestAt(u, f.y, v, SOUTH, "sunken");
					break;
				}
			}
			int[] s = f.against(f.r.nextInt(360));
			f.spawnerAt(s[0], f.stand, s[1]);
			f.cobwebCorners(10);
		}
	}

	/** Solid rock with ore in it as beta's lies, a little chamber in the middle under a frame of timber, and drifts out from it to the wall, one with its rail still down. */
	private static final class Gallery extends FloorKind {
		Gallery() {
			super("mine gallery", Size.MEDIUM, 5, 7, 0.3, 1);
		}

		@Override
		double weight(Tower t) {
			return t.purpose == Tower.Purpose.MINE ? 5 : 0.7;
		}

		@Override
		Mat underfoot(Floor f) {
			return Mat.of(STONE);
		}

		@Override
		void draw(Floor f) {
			int half = f.half(), high = Math.min(f.top() - 1, f.y + 3);
			f.rock(-half, f.y, -half, half, f.top(), half, Mat.of(STONE));
			veins(f);
			for (int u = -2; u <= 2; u++) {
				for (int v = -2; v <= 2; v++) {
					if (Math.hypot(u, v) < 2.3) f.cut(u, f.y, v, u, high, v);
				}
			}
			for (int su = -1; su <= 1; su += 2) {
				for (int sv = -1; sv <= 1; sv += 2) f.pier(su, sv, su, sv, high - 1, Mat.of(FENCE));
			}
			f.box(-1, high, -1, 1, high, 1, cell(PLANKS, 0));
			int rail = f.r.nextInt(4), skip = f.r.nextInt(5);
			for (int d = 0; d < 4; d++) {
				if (d == skip) continue;
				int[] end = {0, 0};
				for (int k = 2; k <= half; k++) {
					int u = DX[d] * k, v = DZ[d] * k;
					if (!f.inFrame(u, v)) break;
					f.cut(u, f.y, v, u, f.y + 1, v);
					if (d == rail && f.r.nextInt(6) != 0) f.put(u, f.y, v, cell(RAIL, rail(f.frame.dir(DX[d] != 0 ? EAST : SOUTH))));
					end = new int[] {u, v};
				}
				if (d == (rail + 2 & 3)) f.chestAt(end[0], f.y, end[1], d, f.depth > 0.75 ? "pit" : "stores");
			}
			int[] pile = Floor.polar(f.r.nextInt(360), 1);
			f.rubble(f.frame.x(pile[0], pile[1]), f.frame.z(pile[0], pile[1]), 1, Mat.of(GRAVEL));
			int[] s = Floor.polar(45 + 90 * f.r.nextInt(4), 1);
			f.spawnerAt(s[0], f.y, s[1]);
			f.cobwebCorners(14);
		}

		/** Beta's own ores for this depth, a few times as thick as the ground outside, the mine being here for them, and gravel, as beta's rock has. */
		private void veins(Floor f) {
			for (int i = 2 + f.r.nextInt(2); i > 0; i--) vein(f, COAL_ORE, 8 + f.r.nextInt(6));
			for (int i = 1 + f.r.nextInt(2); i > 0; i--) vein(f, IRON_ORE, 4 + f.r.nextInt(4));
			if (f.y < 32 && f.r.nextBoolean()) vein(f, GOLD_ORE, 4 + f.r.nextInt(4));
			if (f.y < 32 && f.r.nextInt(5) == 0) vein(f, LAPIS_ORE, 4 + f.r.nextInt(3));
			vein(f, GRAVEL, 6 + f.r.nextInt(6));
		}

		/** Beta's {@code OreFeature}, written into the rock: blobs along a short line. */
		private void vein(Floor f, int id, int size) {
			int half = f.half();
			double mx = f.t.cx + f.r.nextInt(half * 2 + 1) - half, my = f.y + f.r.nextInt(Math.min(f.h, 3)), mz = f.t.cz + f.r.nextInt(half * 2 + 1) - half;
			float angle = f.r.nextFloat() * (float) Math.PI;
			double x0 = mx + Math.sin(angle) * size / 8, x1 = mx - Math.sin(angle) * size / 8, z0 = mz + Math.cos(angle) * size / 8, z1 = mz - Math.cos(angle) * size / 8;
			double y0 = my + f.r.nextInt(3) - 1, y1 = my + f.r.nextInt(3) - 1;
			for (int i = 0; i <= size; i++) {
				double cx = x0 + (x1 - x0) * i / size, cy = y0 + (y1 - y0) * i / size, cz = z0 + (z1 - z0) * i / size;
				double spread = (Math.sin(i * Math.PI / size) + 1) * (f.r.nextDouble() * size / 16) + 1;
				for (int x = (int) Math.floor(cx - spread / 2); x <= (int) Math.floor(cx + spread / 2); x++) {
					for (int y = (int) Math.floor(cy - spread / 2); y <= (int) Math.floor(cy + spread / 2); y++) {
						for (int z = (int) Math.floor(cz - spread / 2); z <= (int) Math.floor(cz + spread / 2); z++) {
							double dx = (x + 0.5 - cx) / (spread / 2), dy = (y + 0.5 - cy) / (spread / 2), dz = (z + 0.5 - cz) / (spread / 2);
							if (dx * dx + dy * dy + dz * dz < 1 && y >= f.y && y <= f.top() && f.in(x, z) && f.p.isRock(x, y, z)) f.p.rock(x, y, z, cell(id, 0));
						}
					}
				}
			}
		}
	}

	/** A pit of lava let into the middle of the floor, cold furnaces along the wall with a brick stack up from them, and stone underfoot: nothing here to catch but the chests, which stand on the wall out of the lava's reach. Lit right through. */
	private static final class Foundry extends FloorKind {
		Foundry() {
			super("foundry", Size.MEDIUM, 6, 8, 0.45, 1);
		}

		@Override
		double weight(Tower t) {
			return t.purpose == Tower.Purpose.MINE ? 4 : t.purpose == Tower.Purpose.GARRISON ? 1.2 : 0.6;
		}

		@Override
		Mat underfoot(Floor f) {
			return Mat.of(STONE);
		}

		@Override
		Mat overhead(Floor f) {
			return f.pal.wall;
		}

		@Override
		boolean hotUnderfoot() {
			return true;
		}

		@Override
		double heart(Floor f) {
			return 2;
		}

		/** Nothing comes down into the lava. */
		@Override
		boolean allows(Floor f, int x, int z) {
			return Math.hypot(x - f.t.cx, z - f.t.cz) >= 1.5;
		}

		@Override
		void draw(Floor f) {
			for (int u = -1; u <= 1; u++) {
				for (int v = -1; v <= 1; v++) {
					if (Math.hypot(u, v) < 1.5) f.put(u, f.y - 1, v, cell(LAVA, 0));
				}
			}
			int turn = f.r.nextInt(360), stack = f.r.nextInt(3);
			for (int i = 0; i < 3; i++) {
				int[] at = f.against(turn + 40 * i);
				if (f.idAt(at[0], f.y, at[1]) != AIR) continue;
				f.put(at[0], f.y, at[1], cell(FURNACE, backTo(f.frame.dir(Floor.outward(at[0], at[1])))));
				if (i == stack) f.box(at[0], f.y + 1, at[1], at[0], f.top(), at[1], Mat.of(BRICK));
			}
			onWall(f, turn + 180, "arms");
			if (f.r.nextBoolean()) onWall(f, turn + 240, "arms");
			int[] s = f.against(turn + 120);
			f.spawnerAt(s[0], f.y, s[1]);
			decay(f, 1, 8);
		}
	}

	/**
	 * The bottom: the strongroom's a black box cut into the rock behind the tower's wall, with one iron door through it and the lever beside it, a ditch across the floor in front of the door with
	 * one plain block over it, and the rest of the room bare and dark. Out in the rock, so a small room still has space for a way down into it. No lava: in a room this small it would light every
	 * corner, the strongroom too through its door, and nothing could spawn at the bottom of the tower.
	 */
	private static final class Vault extends FloorKind {
		Vault() {
			super("vault", Size.MEDIUM, 6, 7, 0.9, 1);
		}

		@Override
		double weight(Tower t) {
			return 0;
		}

		/** The tower's wall along {@code u}, where it runs flat: the strongroom's front. */
		private static int front(Floor f) {
			return -f.t.radius;
		}

		private static int moat(Floor f) {
			return f.t.radius >= 6 ? 2 : 1;
		}

		/** Ways come down in the room, not in the ditch or in the way to the door. */
		@Override
		boolean allows(Floor f, int x, int z) {
			int u = f.frame.u(x, z), v = f.frame.v(x, z);
			return u > front(f) + moat(f) + 2 || Math.abs(v) > 2;
		}

		@Override
		void draw(Floor f) {
			int wall = front(f), back = wall - 4, rt = f.y + 3, ditch = moat(f);
			Mat lid = f.pal.wall;
			// The strongroom: obsidian all round, floor and ceiling too (user, 2026-11-07), the one place in the tower digging takes real effort, three across inside; the lid's edge over its walls plain.
			for (int u = back; u <= wall; u++) {
				for (int v = -2; v <= 2; v++) {
					boolean inside = u > back && u < wall && Math.abs(v) <= 1;
					for (int yy = f.y; yy < rt; yy++) beyond(f, u, yy, v, inside ? AIR : cell(OBSIDIAN, 0));
					beyond(f, u, rt, v, inside ? cell(OBSIDIAN, 0) : lid.at(f.frame.x(u, v), rt, f.frame.z(u, v)));
					beyond(f, u, f.y - 1, v, inside ? cell(OBSIDIAN, 0) : lid.at(f.frame.x(u, v), f.y - 1, f.frame.z(u, v)));
				}
			}
			int side = f.frame.dir(EAST);
			beyond(f, wall, f.y, 0, cell(IRON_DOOR, doorOn(opposite(side))));
			beyond(f, wall, f.y + 1, 0, cell(IRON_DOOR, doorOn(opposite(side)) | 8));
			f.put(wall + 1, f.y + 1, 1, cell(LEVER, leverOn(opposite(side))));
			// The best in the middle, the others in three corners, none touching, and the guard at the back.
			strongbox(f, wall - 2, 0, "vault-best");
			strongbox(f, wall - 3, -1, "vault");
			strongbox(f, wall - 3, 1, "vault");
			strongbox(f, wall - 1, 1, "vault");
			int gx = f.frame.x(wall - 3, 0), gz = f.frame.z(wall - 3, 0);
			f.p.force(gx, f.y, gz, cell(SPAWNER, 0));
			f.p.fixture(gx, f.y, gz, "spawner:Zombie");
			f.t.spawners++;
			// The ditch, two deep, with a block left across it to the door and what's fallen in at its foot.
			for (int u = wall + 2; u < wall + 2 + ditch; u++) {
				for (int v = -2; v <= 2; v++) {
					if (v == 0) continue;
					f.put(u, f.y - 1, v, Mat.of(AIR));
					f.put(u, f.y - 2, v, f.r.nextInt(3) == 0 ? cell(GRAVEL, 0) : AIR);
				}
			}
			f.goalX = f.frame.x(wall + 2 + ditch, 0);
			f.goalZ = f.frame.z(wall + 2 + ditch, 0);
			f.hasGoal = true;
			int s = f.r.nextBoolean() ? 1 : -1, near = wall + 3 + ditch;
			f.spawnerAt(near + f.r.nextInt(2), f.y, s * 3, "Creeper");
			f.spawnerAt(f.half() - 1, f.y, -s * (1 + f.r.nextInt(2)), f.t.monster(f.r, 1));
			f.cobwebCorners(10);
		}

		/** A cell of the strongroom, out past the wall in the rock, where a floor doesn't draw. */
		private static void beyond(Floor f, int u, int yy, int v, int cell) {
			f.p.force(f.frame.x(u, v), yy, f.frame.z(u, v), cell);
		}

		private static void strongbox(Floor f, int u, int v, String loot) {
			int x = f.frame.x(u, v), z = f.frame.z(u, v);
			f.p.force(x, f.y, z, cell(CHEST, backTo(f.frame.dir(WEST))));
			f.p.fixture(x, f.y, z, "chest:" + loot);
			f.t.chests++;
		}
	}
}
