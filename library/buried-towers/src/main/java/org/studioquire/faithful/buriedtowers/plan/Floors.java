package org.studioquire.faithful.buriedtowers.plan;

import static org.studioquire.faithful.buriedtowers.plan.Blocks.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * The kinds of floor. Each is what its builders meant it to be, then a few things time has done to it. Each has one clear idea, in a round room. What's built may be as regular as
 * builders are; what's damaged needn't be. A floor's own frame has {@code u} along its long axis.
 */
final class Floors {
	static final FloorKind RUIN = new Ruin(), GATEHALL = new Gatehall(), VAULT = new Vault();
	static final List<FloorKind> ALL = Arrays.asList(new Barracks(), new Hall(), new CellBlock(), new Cellar(), new Crypt(), new Library(), new Cistern(), new Bridges(), new Gallery(), new Foundry(), new Collapse(),
			Granary.COMPLEX, RootCellar.COMPLEX, MushroomFarm.COMPLEX, SpiderNest.COMPLEX, Armoury.COMPLEX, Maze.COMPLEX, FloodedHall.COMPLEX,
			SandFloor.COMPLEX, SpiralGallery.COMPLEX, Aquarium.COMPLEX, LavaLake.COMPLEX, JukeboxHall.COMPLEX, PortalRoom.COMPLEX);
	/** Where the noise is below this, the top of the tower has gone and the sky comes through. */
	static final double ROOF_GONE = 0.38;
	/** For a mine's vein that is drawn and lays nothing: where lapis lazuli was, in both styles' galleries. */
	static final int NO_ORE = -1;

	private Floors() {
	}

	/** Whether the roof over the hall has gone at a column: mostly over the middle, where nothing held it up but itself, and here and there elsewhere. */
	static boolean roofGone(Tower t, int x, int z) {
		double far = Math.hypot(x - t.cx, z - t.cz) / t.radius;
		return t.noise.at(x, 77, z, 6) < ROOF_GONE - 0.1 + 0.42 * (1 - far);
	}

	/** Whether the sky comes down this column into a floor. */
	private static boolean underSky(Floor f, int x, int z) {
		return f.p.id(x, f.top() + 1, z) == AIR;
	}

	/** The top of the tower with its roof gone: what's left of the boards, open to the sky over the hall, and a lookout's chest left against the wall. */
	private static final class Ruin extends FloorKind {
		Ruin() {
			super("ruin", Size.MEDIUM, 5, 8, -1, 0);
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
					if (f.in(x, z) && roofGone(f.t, x, z)) f.set(x, f.y - 1, z, AIR);
				}
			}
			int d = f.r.nextInt(4);
			for (int i = 0; i < 4; i++, d = d + 1 & 3) {
				int[] c = f.toWall(0, 0, d);
				if (f.idAt(c[0], f.y - 1, c[1]) == AIR) continue;
				f.chestAt(c[0], f.y, c[1], d, "lookout");
				break;
			}
		}
	}

	/** The way in: a ring of posts round a flagged court, with a wheel of beams over it and the sky through the wheel, a bench round the wall, and the ground coming back where the rain gets in. */
	private static final class Gatehall extends FloorKind {
		private static final int[] POSTS = {22, 67, 112, 157, 202, 247, 292, 337};

		Gatehall() {
			super("gate hall", Size.MEDIUM, 6, 7, -1, 0);
		}

		@Override
		double weight(Tower t) {
			return 0;
		}

		private static double court(Floor f) {
			return f.rim() - 3.5;
		}

		/** Flags in the court with a ring of the trim round them, and the floor's own stuff in the aisle. */
		@Override
		Mat underfoot(Floor f) {
			double court = court(f);
			Mat flags = f.t.is(Climate.DESERT) ? Mat.of(DOUBLE_SLAB, SAND_SLAB) : Mat.of(DOUBLE_SLAB, STONE_SLAB), edge = f.t.is(Climate.DESERT) ? f.pal.trim : Mat.of(COBBLE);
			return (x, y, z) -> {
				double far = Math.hypot(x - f.t.cx, z - f.t.cz);
				return far < court - 1 ? flags.at(x, y, z) : far < court ? edge.at(x, y, z) : f.pal.floorAt(x, y, z);
			};
		}

		@Override
		void draw(Floor f) {
			int half = f.half(), top = f.top();
			double court = court(f);
			// The bench goes in first, so the posts and whatever else stands at the wall replace it.
			for (int x = f.t.cx - f.t.radius; x <= f.t.cx + f.t.radius; x++) {
				for (int z = f.t.cz - f.t.radius; z <= f.t.cz + f.t.radius; z++) {
					if (f.t.depth(x, z) == 3) f.set(x, f.y, z, cell(SLAB, f.pal.slab));
				}
			}
			int broken = f.r.nextInt(POSTS.length * 2);
			for (int i = 0; i < POSTS.length; i++) {
				int[] post = Floor.polar(POSTS[i], court), wall = f.against(POSTS[i]);
				f.pier(wall[0], wall[1], wall[0], wall[1], top, f.pal.beam);
				f.beam(post[0], post[1], wall[0], wall[1], top, f.pal.beam);
				if (i == broken) {
					f.pier(post[0], post[1], post[0], post[1], f.y + 1, f.pal.beam);
					f.rubble(f.frame.x(post[0], post[1]), f.frame.z(post[0], post[1]), 2, f.rubbleMat());
				} else f.pier(post[0], post[1], post[0], post[1], top, f.pal.beam);
			}
			f.ring(court, top, f.pal.beam);
			// The ground comes back only where the rain and the light get in.
			for (int x = f.t.cx - half; x <= f.t.cx + half; x++) {
				for (int z = f.t.cz - half; z <= f.t.cz + half; z++) {
					if (f.in(x, z) && f.p.id(x, f.y, z) == AIR && underSky(f, x, z) && f.t.noise.at(x, 31, z, 3) > 0.45) growth(f, x, z);
				}
			}
			// Where the roof has come down.
			for (int tries = 0, falls = 1 + f.r.nextInt(2); tries < 30 && falls > 0; tries++) {
				int x = f.t.cx - half + f.r.nextInt(half * 2 + 1), z = f.t.cz - half + f.r.nextInt(half * 2 + 1);
				if (!underSky(f, x, z) || Math.hypot(x - f.t.cx, z - f.t.cz) < 2) continue;
				f.rubble(x, z, 2, f.rubbleMat());
				falls--;
			}
			f.chestOnWall(f.r.nextInt(5) - 2, f.y, 0, SOUTH, "scraps");
			if (f.t.is(Climate.WOODED, Climate.WET) && f.r.nextBoolean()) tree(f);
			f.cobwebCorners(6);
		}

		private void growth(Floor f, int x, int z) {
			if (f.t.climate == Climate.DESERT) {
				f.set(x, f.y - 1, z, cell(SAND, 0));
				if (f.r.nextInt(9) == 0) f.set(x, f.y, z, cell(DEAD_BUSH, 0));
				return;
			}
			f.set(x, f.y - 1, z, cell(GRASS, 0));
			if (f.t.climate == Climate.COLD) return;
			int roll = f.r.nextInt(12);
			if (roll < 4) f.set(x, f.y, z, cell(TALL_GRASS, 1));
			else if (roll == 4) f.set(x, f.y, z, cell(f.r.nextBoolean() ? ROSE : DANDELION, 0));
		}

		/** A tree that's grown up through a hole in the roof. */
		private void tree(Floor f) {
			int half = f.half() - 4;
			for (int tries = 0; tries < 30; tries++) {
				int x = f.t.cx - half + f.r.nextInt(half * 2 + 1), z = f.t.cz - half + f.r.nextInt(half * 2 + 1);
				if (!f.in(x, z) || f.p.held(x, f.y, z) || f.p.id(x, f.y, z) != AIR || !underSky(f, x, z)) continue;
				int wood = f.t.climate == Climate.WOODED && f.r.nextBoolean() ? 2 : 0;
				int top = f.top() + 7 + f.r.nextInt(4);
				f.p.force(x, f.y - 1, z, cell(DIRT, 0));
				for (int yy = f.y; yy <= top; yy++) f.p.force(x, yy, z, cell(LOG, wood));
				for (int yy = top - 3; yy <= top + 1; yy++) {
					int reach = yy > top - 1 ? 1 : 2;
					for (int dx = -reach; dx <= reach; dx++) {
						for (int dz = -reach; dz <= reach; dz++) {
							if (Math.abs(dx) + Math.abs(dz) > reach + 1 || dx == 0 && dz == 0 && yy <= top) continue;
							if (f.p.id(x + dx, yy, z + dz) == AIR && f.t.depth(x + dx, z + dz) >= 3) f.p.force(x + dx, yy, z + dz, cell(LEAVES, wood));
						}
					}
				}
				return;
			}
		}
	}

	/** Where the garrison slept: a round room on a ring of timber posts, with a wheel of beams overhead, board partitions from the posts to the wall making a bay for each pair of beds, and the hearth in the middle, a brick stack through the ceiling with what the mess used round it. */
	private static final class Barracks extends FloorKind {
		private static final int[] POSTS = {22, 67, 112, 157, 202, 247, 292, 337};

		Barracks() {
			super("barracks", Size.MEDIUM, 6, 8, 0, 0.75);
		}

		@Override
		double weight(Tower t) {
			return t.purpose == Tower.Purpose.GARRISON ? 4 : 0.7;
		}

		private static double aisle(Floor f) {
			return f.rim() - 4;
		}

		@Override
		double heart(Floor f) {
			return 2.5;
		}

		/** Flags round the hearth, with a kerb where the posts stand, and boards in the bays. */
		@Override
		Mat underfoot(Floor f) {
			double aisle = aisle(f);
			Mat flags = f.pal.flags(f.t), kerb = f.pal.kerb(), boards = Mat.of(PLANKS);
			return (x, y, z) -> {
				double far = Math.hypot(x - f.t.cx, z - f.t.cz);
				return far < aisle - 0.5 ? flags.at(x, y, z) : far < aisle + 0.5 ? kerb.at(x, y, z) : boards.at(x, y, z);
			};
		}

		@Override
		void draw(Floor f) {
			int top = f.top(), walls = f.y + 2;
			double aisle = aisle(f);
			Mat post = f.pal.beam;
			for (int i = 0; i < POSTS.length; i++) {
				int[] at = Floor.polar(POSTS[i], aisle), wall = f.against(POSTS[i]);
				for (int yy = f.y; yy <= walls; yy++) f.beam(at[0], at[1], wall[0], wall[1], yy, Mat.of(PLANKS));
				f.pier(wall[0], wall[1], wall[0], wall[1], top, post);
				f.pier(at[0], at[1], at[0], at[1], top, post);
				f.beam(at[0], at[1], wall[0], wall[1], top, post);
			}
			f.ring(aisle, top, post);
			for (int a = 0; a < 360; a += 45) bay(f, a, aisle);
			hearth(f);
			int[] s = Floor.polar(45 + 90 * f.r.nextInt(4), aisle - 1.5);
			f.spawnerAt(s[0], f.y, s[1]);
			if (f.depth > 0.4) f.spawnerAt(-s[0], f.y, -s[1]);
			f.cobwebCorners(10);
		}

		/** A flat run of wall takes two beds with perhaps a chest between them, and the stepped run on a diagonal one. Heads to the wall. */
		private void bay(Floor f, int a, double aisle) {
			int[] c = f.against(a);
			int d = Floor.outward(c[0], c[1]), side = d + 1 & 3, dir = f.frame.dir(d);
			for (int w : a % 90 == 0 ? new int[] {-2, 2} : new int[] {0}) {
				if (f.r.nextInt(5) == 0) continue;
				int[] head = f.toWall(c[0] + DX[side] * w, c[1] + DZ[side] * w, d);
				int fu = head[0] - DX[d], fv = head[1] - DZ[d];
				if (Math.hypot(fu, fv) < aisle + 1 || f.idAt(head[0], f.y, head[1]) != AIR || f.idAt(fu, f.y, fv) != AIR) continue;
				f.put(fu, f.y, fv, cell(BED, bed(dir)));
				f.put(head[0], f.y, head[1], cell(BED, bed(dir) | 8));
			}
			if (a % 90 != 0) return;
			if (f.r.nextInt(3) == 0) f.chestAt(c[0], f.y, c[1], d, "bunk");
		}

		/** A stack of brick floor to ceiling, its mouth long cold, a furnace and a bench to either side of it and the mess's chest at its back. */
		private void hearth(Floor f) {
			for (int u = -1; u <= 1; u++) {
				for (int v = -1; v <= 1; v++) {
					for (int yy = f.y; yy <= f.top(); yy++) {
						if (f.p.held(f.frame.x(u, v), yy, f.frame.z(u, v))) return;
					}
				}
			}
			f.box(-1, f.y, -1, 1, f.top(), 1, Mat.of(BRICK));
			int d = f.r.nextInt(4), side = d + 1 & 3, back = opposite(d);
			f.put(DX[side] * 2, f.y, DZ[side] * 2, cell(FURNACE, backTo(f.frame.dir(opposite(side)))));
			f.put(-DX[side] * 2, f.y, -DZ[side] * 2, cell(WORKBENCH, 0));
			f.chestAt(DX[back] * 2, f.y, DZ[back] * 2, d, "kitchen");
		}
	}

	/** A place to be overawed in. Three arrangements, all round: a colonnade with a ring beam round a dais, one great pier in the middle with ribs fanning from it to the wall, or a thick forest of thin columns in rings about a clearing. */
	private static final class Hall extends FloorKind {
		Hall() {
			super("pillared hall", Size.MEDIUM, 7, 11, 0.05, 1);
		}

		@Override
		double weight(Tower t) {
			return t.purpose == Tower.Purpose.ORDER ? 3 : 1.2;
		}

		private static double band(Floor f) {
			return f.rim() - 3;
		}

		@Override
		double heart(Floor f) {
			return 3.5;
		}

		/** Flags in the middle, a kerb round them, and the floor's own stuff outside it. */
		@Override
		Mat underfoot(Floor f) {
			double band = band(f);
			Mat flags = f.pal.flags(f.t), kerb = f.pal.kerb();
			return (x, y, z) -> {
				double far = Math.hypot(x - f.t.cx, z - f.t.cz);
				return far < band - 1 ? flags.at(x, y, z) : far < band ? kerb.at(x, y, z) : f.pal.floorAt(x, y, z);
			};
		}

		@Override
		void draw(Floor f) {
			int style = f.r.nextInt(3);
			double round = f.clearOf(pierRadius(f)) ? pierRadius(f) : f.clearOf(2.5) ? 2.5 : 0;
			if (style == 2) forest(f);
			else if (style == 1 && round > 0) greatPier(f, round);
			else colonnade(f);
			f.cobwebCorners(10);
		}

		private void colonnade(Floor f) {
			int top = f.top(), count = f.half() >= 10 ? 12 : 8;
			double ring = band(f) - 0.5;
			Mat pillar = f.pal.pillar, trim = f.pal.trim;
			for (int i = 0; i < count; i++) {
				int a = 360 * i / count + 180 / count;
				int[] at = Floor.polar(a, ring), wall = f.against(a);
				f.pier(wall[0], wall[1], wall[0], wall[1], top, trim);
				f.beam(at[0], at[1], wall[0], wall[1], top, trim);
				if (f.r.nextInt(6) == 0) {
					f.pier(at[0], at[1], at[0], at[1], f.y + 1 + f.r.nextInt(2), pillar);
					f.rubble(f.frame.x(at[0], at[1]), f.frame.z(at[0], at[1]), 2, f.rubbleMat());
				} else if (f.pier(at[0], at[1], at[0], at[1], top, pillar)) {
					f.put(at[0], f.y, at[1], trim);
				}
			}
			f.ring(ring, top, trim);
			// A step up to a dais in the middle, and something on it.
			f.disc(0, 0, 3.5, f.y, Mat.of(SLAB, f.pal.slab));
			f.disc(0, 0, 2.5, f.y, trim);
			f.chestAt(0, f.y + 1, 0, NORTH, "hall");
			int s = f.r.nextBoolean() ? 1 : -1;
			f.spawnerAt(s * (int) (ring - 2), f.y, 0);
			f.spawnerAt(-s * (int) (ring - 2), f.y, 0);
		}

		private static double pierRadius(Floor f) {
			return f.half() >= 10 ? 3.5 : 2.5;
		}

		/** One pier the whole height of the room, banded, with a chest in a niche at its foot and ribs fanning from it to pilasters round the wall. */
		private void greatPier(Floor f, double round) {
			int top = f.top();
			Mat pillar = f.pal.pillar, trim = f.pal.trim;
			f.column(0, 0, round, f.y, top, (x, yy, z) -> (yy - f.y) % 3 == 0 ? trim.at(x, yy, z) : pillar.at(x, yy, z));
			int step = f.half() >= 10 ? 30 : 45;
			for (int a = step / 2; a < 360; a += step) {
				int[] wall = f.against(a), foot = Floor.polar(a, round - 0.5);
				f.pier(wall[0], wall[1], wall[0], wall[1], top, trim);
				f.beam(foot[0], foot[1], wall[0], wall[1], top, trim);
			}
			int d = f.r.nextInt(4), reach = (int) round;
			f.box(DX[d] * reach, f.y, DZ[d] * reach, DX[d] * reach, f.y + 1, DZ[d] * reach, Mat.of(AIR));
			f.chestAt(DX[d] * reach, f.y, DZ[d] * reach, opposite(d), "hall");
			for (int a = step / 2; a < 360; a += step * 2) {
				int[] wall = f.against(a);
			}
			int[] s0 = Floor.polar(45 + f.r.nextInt(4) * 90, round + 2.5);
			f.spawnerAt(s0[0], f.y, s0[1]);
			if (f.depth > 0.5) f.spawnerAt(-s0[0], f.y, -s0[1]);
		}

		/** Rings of thin columns about a clearing with a chest, and a second clearing out among them where something still walks. */
		private void forest(Floor f) {
			int top = f.top(), far = 30 + 60 * f.r.nextInt(6);
			int[] glade = Floor.polar(far, f.rim() * 0.6);
			for (int ring = 1; ring <= 3; ring++) {
				double round = ring * 3 + 0.5;
				if (round > f.rim() - 0.5) break;
				int count = ring * 6;
				for (int i = 0; i < count; i++) {
					int[] at = Floor.polar(360.0 * i / count + 180.0 / count, round);
					if (Math.hypot(at[0] - glade[0], at[1] - glade[1]) < 2.5) continue;
					if (f.r.nextInt(9) == 0) {
						f.pier(at[0], at[1], at[0], at[1], f.y + f.r.nextInt(2), f.pal.pillar);
						continue;
					}
					f.pier(at[0], at[1], at[0], at[1], top, f.pal.pillar);
				}
			}
			f.put(0, f.y, 0, cell(DOUBLE_SLAB, STONE_SLAB));
			f.chestAt(0, f.y + 1, 0, NORTH, "hall");
			f.spawnerAt(glade[0], f.y, glade[1]);
			if (f.depth > 0.5) f.spawnerAt(-glade[0], f.y, -glade[1]);
		}
	}

	/** A round guard court with a post in the middle, and the cells in a ring round it, cut out of solid rock back to the tower's wall: wooden doors or none left, and one strong cell behind iron with the lever on the court's side. One prisoner dug through to the next. */
	private static final class CellBlock extends FloorKind {
		private static final int[] CELLS = {0, 90, 180, 270, 45, 135, 225, 315};

		CellBlock() {
			super("cell block", Size.SHORT, 4, 5, 0.1, 0.92);
		}

		@Override
		double weight(Tower t) {
			return t.purpose == Tower.Purpose.GARRISON ? 3 : t.purpose == Tower.Purpose.ORDER ? 1.2 : 0.6;
		}

		private static double court(Floor f) {
			return f.rim() - 4;
		}

		@Override
		double heart(Floor f) {
			return 2;
		}

		/** Flags in the court, a kerb at its edge, and cobble in the cells. */
		@Override
		Mat underfoot(Floor f) {
			double court = court(f);
			Mat flags = f.pal.flags(f.t), kerb = f.pal.kerb(), cobble = Mat.of(COBBLE);
			return (x, y, z) -> {
				double far = Math.hypot(x - f.t.cx, z - f.t.cz);
				return far < court - 1 ? flags.at(x, y, z) : far < court ? kerb.at(x, y, z) : cobble.at(x, y, z);
			};
		}

		@Override
		void draw(Floor f) {
			int half = f.half();
			double court = court(f);
			f.rock(-half, f.y, -half, half, f.top(), half, f.pal.wall);
			for (int u = -half; u <= half; u++) {
				for (int v = -half; v <= half; v++) {
					if (Math.hypot(u, v) < court) f.cut(u, f.y, v, u, f.top(), v);
				}
			}
			// The guard's post: a column, a bench and the arms chest by it.
			f.pier(0, 0, 0, 0, f.top(), f.pal.trim);
			int d = f.r.nextInt(4), side = d + 1 & 3;
			f.put(DX[d] * 2, f.y, DZ[d] * 2, cell(WORKBENCH, 0));
			f.chestAt(DX[d] * 2 + DX[side], f.y, DZ[d] * 2 + DZ[side], d, "arms");
			int[] s = Floor.polar(f.r.nextInt(4) * 90 + 45, court - 1.5);
			f.spawnerAt(s[0], f.y, s[1]);

			int strong = f.r.nextInt(4), dug = 4 + f.r.nextInt(4);
			for (int i = 0; i < CELLS.length; i++) prison(f, CELLS[i], court, i == strong, i == dug);
		}

		private static double width(Floor f) {
			return f.half() >= 10 ? 12 : 15;
		}

		/** A wedge of room from a wall's thickness outside the court back to the tower's wall, and its doorway. */
		private void prison(Floor f, int a, double court, boolean strong, boolean dug) {
			int half = f.half();
			double wedge = width(f);
			for (int u = -half; u <= half; u++) {
				for (int v = -half; v <= half; v++) {
					double far = Math.hypot(u, v);
					if (far < court + 1.5 || !f.inFrame(u, v)) continue;
					double off = Math.abs(Math.IEEEremainder(Math.toDegrees(Math.atan2(v, u)) - a, 360));
					if (off <= wedge) f.cut(u, f.y, v, u, f.y + 2, v);
				}
			}
			// From the court out, one cell at a time, as far as the room: the first cell of rock on the way is the door, and any more are a short passage.
			int[] from = Floor.polar(a, court - 1), to = Floor.polar(a, court + 2.5);
			List<int[]> path = f.line(from[0], from[1], to[0], to[1]);
			int[] door = null, step = null;
			for (int i = 1; i < path.size(); i++) {
				int[] c = path.get(i);
				if (f.idAt(c[0], f.y, c[1]) == AIR) continue;
				f.cut(c[0], f.y, c[1], c[0], f.y + 1, c[1]);
				if (door == null) {
					door = c;
					int[] was = path.get(i - 1);
					step = new int[] {c[0] - was[0], c[1] - was[1]};
				}
			}
			int[] back = f.against(a);
			int d = Floor.outward(back[0], back[1]);
			if (door != null) doorway(f, door, step, strong);
			if (dug) {
				prisoner(f, a, back);
				return;
			}
			if (strong) {
				f.chestAt(back[0], f.y, back[1], d, "cell");
				int[] guard = f.nextTo(back, d);
				f.spawnerAt(guard[0], f.y, guard[1], f.r.nextBoolean() ? "Skeleton" : "Zombie");
				return;
			}
			int what = f.r.nextInt(6);
			if (what == 0) {
				int[] guard = f.nextTo(back, d);
				f.spawnerAt(guard[0], f.y, guard[1], f.r.nextBoolean() ? "Skeleton" : "Zombie");
			} else if (what == 1) f.chestAt(back[0], f.y, back[1], d, "cell");
			else if (what <= 3) {
				int dir = f.frame.dir(d);
				if (f.idAt(back[0] - DX[d], f.y, back[1] - DZ[d]) == AIR) {
					f.put(back[0] - DX[d], f.y, back[1] - DZ[d], cell(BED, bed(dir)));
					f.put(back[0], f.y, back[1], cell(BED, bed(dir) | 8));
				}
			} else if (what == 4) f.web(f.frame.x(back[0], back[1]), f.y + 2, f.frame.z(back[0], back[1]));
		}

		/** Jambs and a lintel of the trim, so a door reads across the court, and in it wood, iron or nothing left. */
		private void doorway(Floor f, int[] door, int[] step, boolean strong) {
			int alongU = step[0] != 0 ? 1 : 0, u = door[0], v = door[1];
			int toRoom = step[0] > 0 ? EAST : step[0] < 0 ? WEST : step[1] > 0 ? SOUTH : NORTH;
			int ju = alongU == 1 ? 0 : 1, jv = alongU == 1 ? 1 : 0;
			Mat trim = f.pal.trim;
			for (int s = -1; s <= 1; s += 2) f.box(u + ju * s, f.y, v + jv * s, u + ju * s, f.y + 2, v + jv * s, trim);
			f.put(u, f.y + 2, v, trim);
			int side = f.frame.dir(opposite(toRoom));
			if (strong) {
				for (int s = -1; s <= 1; s += 2) {
					int lu = u + ju * s - step[0], lv = v + jv * s - step[1];
					if (f.idAt(lu, f.y + 1, lv) != AIR || !f.inFrame(lu, lv)) continue;
					f.put(u, f.y, v, cell(IRON_DOOR, doorOn(side)));
					f.put(u, f.y + 1, v, cell(IRON_DOOR, doorOn(side) | 8));
					f.put(lu, f.y + 1, lv, cell(LEVER, leverOn(f.frame.dir(toRoom))));
					// A barred slit in the other jamb, so you can look in.
					f.put(u - ju * s, f.y + 1, v - jv * s, cell(FENCE, 0));
					return;
				}
			}
			if (f.r.nextBoolean()) {
				f.put(u, f.y, v, cell(WOOD_DOOR, doorOn(side)));
				f.put(u, f.y + 1, v, cell(WOOD_DOOR, doorOn(side) | 8));
			}
		}

		/** The one who got out: a rough hole through the side of the cell into the next, full of what they dug, and what they hid. */
		private void prisoner(Floor f, int a, int[] back) {
			int d = Floor.outward(back[0], back[1]);
			f.chestAt(back[0], f.y, back[1], d, "prisoner");
			double far = (court(f) + 1.5 + f.rim()) / 2;
			for (double at = a; at <= a + 45; at += 3) {
				int[] c = Floor.polar(at, far);
				if (!f.inFrame(c[0], c[1]) || f.idAt(c[0], f.y, c[1]) == AIR) continue;
				f.cut(c[0], f.y, c[1], c[0], f.y + 1, c[1]);
				if (f.r.nextInt(3) == 0) f.put(c[0], f.y, c[1], cell(GRAVEL, 0));
			}
		}
	}

	/** One long barrel vault bent into a ring round a solid core, stepped up to its crown with ribs across it at regular bays, barrels on end along both walls, a chest here and there, most of the stores long gone; and in a wide tower, a store-room in the core behind a door. */
	private static final class Cellar extends FloorKind {
		Cellar() {
			super("cellar", Size.SHORT, 5, 6, 0, 0.7);
		}

		@Override
		double weight(Tower t) {
			return t.purpose == Tower.Purpose.STORES ? 4 : 0.9;
		}

		private static double middle(Floor f) {
			return f.rim() - 2.5;
		}

		/** A paved way down the middle of the vault, and cobble under the barrels. */
		@Override
		Mat underfoot(Floor f) {
			double mid = middle(f);
			Mat flags = f.pal.flags(f.t), cobble = Mat.of(COBBLE);
			return (x, y, z) -> Math.abs(Math.hypot(x - f.t.cx, z - f.t.cz) - mid) < 1.3 ? flags.at(x, y, z) : cobble.at(x, y, z);
		}

		@Override
		Mat overhead(Floor f) {
			return f.pal.wall;
		}

		@Override
		void draw(Floor f) {
			int half = f.half(), top = f.top();
			double mid = middle(f);
			f.rock(-half, f.y, -half, half, top, half, f.pal.wall);
			for (int u = -half - 1; u <= half + 1; u++) {
				for (int v = -half - 1; v <= half + 1; v++) {
					if (!f.inFrame(u, v)) continue;
					double far = Math.hypot(u, v), off = Math.abs(far - mid);
					if (off >= 2.5) continue;
					int rise = off < 1.2 ? top : off < 2 ? top - 1 : top - 2;
					double a = Math.toDegrees(Math.atan2(v, u));
					boolean rib = Math.abs(Math.IEEEremainder(a, 30)) * far * Math.PI / 180 < 0.5;
					f.cut(u, f.y, v, u, rib ? rise - 1 : rise, v);
					if (rib) f.put(u, rise, v, f.pal.trim);
				}
			}
			core(f, mid);
			for (int s = -1; s <= 1; s += 2) barrels(f, mid + s * 2, s);
			int[] at = Floor.polar(f.r.nextInt(360), mid + 1);
			if (f.idAt(at[0], f.y, at[1]) == AIR) f.spawnerAt(at[0], f.y, at[1]);
			int[] fall = Floor.polar(f.r.nextInt(360), mid);
			f.rubble(f.frame.x(fall[0], fall[1]), f.frame.z(fall[0], fall[1]), 2, f.rubbleMat());
			f.cobwebCorners(12);
		}

		/** Barrels: logs on their ends, one or two high, a block apart along a wall; here and there a chest in the row, a pumpkin, or a gap. */
		private void barrels(Floor f, double far, int s) {
			int count = (int) (2 * Math.PI * far / 2);
			for (int i = 0; i < count; i++) {
				int[] c = Floor.polar(360.0 * i / count, far);
				if (!f.inFrame(c[0], c[1]) || f.idAt(c[0], f.y, c[1]) != AIR || f.idAt(c[0], f.y + 1, c[1]) != AIR) continue;
				int roll = f.r.nextInt(14);
				if (roll == 0) {
					int d = Floor.outward(c[0], c[1]);
					f.chestAt(c[0], f.y, c[1], s > 0 ? d : opposite(d), "stores");
				} else if (roll == 1) f.put(c[0], f.y, c[1], cell(PUMPKIN, f.r.nextInt(4)));
				else if (roll < 10) {
					f.put(c[0], f.y, c[1], cell(LOG, 0));
					if (f.r.nextBoolean()) f.put(c[0], f.y + 1, c[1], cell(LOG, 0));
				}
			}
		}

		/** Where the core's wide enough a small round room, and a door to it from the vault; where it isn't, a niche at its foot with a chest in it. */
		private void core(Floor f, double mid) {
			int d = f.r.nextInt(4), reach = (int) Math.ceil(mid - 2.5) - 1;
			if (reach < 3) {
				f.box(DX[d] * reach, f.y, DZ[d] * reach, DX[d] * reach, f.y + 1, DZ[d] * reach, Mat.of(AIR));
				f.chestAt(DX[d] * reach, f.y, DZ[d] * reach, opposite(d), "stores");
				return;
			}
			for (int u = -reach; u <= reach; u++) {
				for (int v = -reach; v <= reach; v++) {
					if (Math.hypot(u, v) < reach - 0.9) f.cut(u, f.y, v, u, f.y + 2, v);
				}
			}
			int side = f.frame.dir(opposite(d));
			f.cut(DX[d] * reach, f.y, DZ[d] * reach, DX[d] * reach, f.y + 1, DZ[d] * reach);
			f.put(DX[d] * reach, f.y, DZ[d] * reach, cell(WOOD_DOOR, doorOn(side)));
			f.put(DX[d] * reach, f.y + 1, DZ[d] * reach, cell(WOOD_DOOR, doorOn(side) | 8));
			f.chestAt(0, f.y, 0, opposite(d), "stores");
			f.spawnerAt(-DX[d], f.y, -DZ[d]);
		}
	}

	/** A round chapel with walls two thick in the middle of a low round room, one door in, and niches where the dead lay, each with a slab, all round the chapel's outside and the tower's wall. A few tombs on the floor of the walk between. */
	private static final class Crypt extends FloorKind {
		Crypt() {
			super("crypt", Size.SHORT, 4, 5, 0.2, 1);
		}

		@Override
		double weight(Tower t) {
			return t.purpose == Tower.Purpose.ORDER ? 3.5 : 0.7;
		}

		private static double chapel(Floor f) {
			return f.half() >= 10 ? 3.5 : 2.5;
		}

		@Override
		double heart(Floor f) {
			return chapel(f) + 2;
		}

		/** Flags in the chapel, and the walk round it paved with a kerb. */
		@Override
		Mat underfoot(Floor f) {
			double outside = chapel(f) + 2;
			Mat flags = Mat.of(DOUBLE_SLAB, STONE_SLAB), cobble = Mat.of(COBBLE);
			return (x, y, z) -> {
				double far = Math.hypot(x - f.t.cx, z - f.t.cz);
				return far < outside || Math.abs(far - (outside + f.rim()) / 2) < 1 ? flags.at(x, y, z) : cobble.at(x, y, z);
			};
		}

		@Override
		Mat overhead(Floor f) {
			return Mat.of(DOUBLE_SLAB, STONE_SLAB);
		}

		@Override
		void draw(Floor f) {
			double inner = chapel(f), outer = inner + 2, walk = (outer + f.rim()) / 2;
			int reach = (int) outer + 1, top = f.top();
			for (int u = -reach; u <= reach; u++) {
				for (int v = -reach; v <= reach; v++) {
					double far = Math.hypot(u, v);
					if (far >= inner && far < outer) f.pier(u, v, u, v, top, f.pal.wall);
				}
			}
			// The chapel's outside gets a course of the trim at the head of its niches, wherever a way has left it standing; and niches in it and the tower's wall, the one skin there that's not its outside.
			List<int[]> onChapel = new ArrayList<>(), onWall = new ArrayList<>();
			for (int x = f.t.cx - f.t.radius; x <= f.t.cx + f.t.radius; x++) {
				for (int z = f.t.cz - f.t.radius; z <= f.t.cz + f.t.radius; z++) {
					double far = Math.hypot(x - f.t.cx, z - f.t.cz);
					if (far >= outer - 1 && far < outer && f.p.id(x, f.y, z) != AIR) {
						onChapel.add(new int[] {x, z});
						f.set(x, top - 1 > f.y + 2 ? top - 1 : top, z, f.pal.trim);
					} else if (f.t.depth(x, z) == 2 && (f.in(x + 1, z) || f.in(x - 1, z) || f.in(x, z + 1) || f.in(x, z - 1))) onWall.add(new int[] {x, z});
				}
			}
			niches(f, onChapel);
			niches(f, onWall);
			// The door, framed.
			int d = f.r.nextInt(4), du = DX[d], dv = DZ[d];
			f.box(du * (int) inner, f.y, dv * (int) inner, du * (int) outer, f.y + 1, dv * (int) outer, Mat.of(AIR));
			int across = d + 1 & 3;
			for (int s = -1; s <= 1; s += 2) f.box(du * (int) outer + DX[across] * s, f.y, dv * (int) outer + DZ[across] * s, du * (int) outer + DX[across] * s, f.y + 2, dv * (int) outer + DZ[across] * s, f.pal.trim);
			f.put(du * (int) outer, f.y + 2, dv * (int) outer, f.pal.trim);
			// Inside: a block for an altar, what's left on it, and whoever's watching.
			f.box(-1, f.y, -1, 1, f.y, 1, Mat.of(DOUBLE_SLAB, STONE_SLAB));
			f.chestAt(0, f.y + 1, 0, NORTH, "crypt");
			f.spawnerAt(-du * 2, f.y, -dv * 2, "Skeleton");
			// Outside: tombs on the walk, and something walking the round.
			for (int k = 0; k < 4; k++) tomb(f, 90 * k + 45 + (f.r.nextBoolean() ? 22 : -22), walk);
			int[] round = Floor.polar(90 * (d + 2) + 45, walk);
			f.spawnerAt(round[0], f.y, round[1], f.depth > 0.6 ? "Skeleton" : "Zombie");
			f.cobwebCorners(18);
		}

		/** A stone box two long on the walk's floor. */
		private void tomb(Floor f, int a, double walk) {
			if (f.r.nextInt(4) == 0) return;
			int[] c = Floor.polar(a, walk);
			int d = Floor.outward(c[0], c[1]), side = d + 1 & 3;
			int ou = c[0] + DX[side], ov = c[1] + DZ[side];
			if (f.idAt(c[0], f.y, c[1]) != AIR || f.idAt(ou, f.y, ov) != AIR) return;
			f.put(c[0], f.y, c[1], Mat.of(DOUBLE_SLAB, STONE_SLAB));
			f.put(ou, f.y, ov, Mat.of(DOUBLE_SLAB, STONE_SLAB));
		}

		/** Every other place going round, three in four of them still open. */
		private void niches(Floor f, List<int[]> places) {
			places.sort((a, b) -> Double.compare(Math.atan2(a[1] - f.t.cz, a[0] - f.t.cx), Math.atan2(b[1] - f.t.cz, b[0] - f.t.cx)));
			for (int i = 0; i < places.size(); i += 2) {
				if (f.r.nextInt(4) != 0) niche(f, places.get(i)[0], places.get(i)[1]);
			}
		}

		/** A shelf in the wall one up from the floor, with a slab to lie on; now and then a web across it. */
		private void niche(Floor f, int x, int z) {
			if (f.p.held(x, f.y + 1, z) || f.p.held(x, f.y + 2, z)) return;
			f.p.force(x, f.y + 1, z, cell(SLAB, f.pal.slab));
			f.p.force(x, f.y + 2, z, f.r.nextInt(5) == 0 ? cell(WEB, 0) : AIR);
		}
	}

	/**
	 * A round reading room two storeys high: a gallery round an open well, on timber posts, spokes from the posts to the wall making bays under it, each bay a case of books, a set of bare shelves or a place to sit and read. A desk in the well. The books are few: most went long ago, or in the fire. In the narrowest towers the gallery stands on half as many posts with thin ones between, and nothing but the rail above it.
	 */
	private static final class Library extends FloorKind {
		private static final int[] POSTS = {22, 67, 112, 157, 202, 247, 292, 337};

		Library() {
			super("library", Size.MEDIUM, 9, 11, 0.1, 0.8);
		}

		@Override
		double weight(Tower t) {
			return t.purpose == Tower.Purpose.ORDER ? 5 : 0.6;
		}

		private static boolean narrow(Floor f) {
			return f.half() < 9;
		}

		@Override
		double heart(Floor f) {
			return 3;
		}

		private static double well(Floor f) {
			return f.rim() - (narrow(f) ? 3 : 4);
		}

		/** Boards, with a disc of stone flags under the desk. */
		@Override
		Mat underfoot(Floor f) {
			Mat planks = Mat.of(PLANKS), flags = Mat.of(DOUBLE_SLAB, STONE_SLAB);
			return (x, y, z) -> Math.hypot(x - f.t.cx, z - f.t.cz) < 3 ? flags.at(x, y, z) : planks.at(x, y, z);
		}

		@Override
		Mat overhead(Floor f) {
			return Mat.of(PLANKS);
		}

		@Override
		void draw(Floor f) {
			int half = f.half(), gallery = f.y + 4, top = f.top();
			double well = well(f);
			boolean narrow = narrow(f);
			Mat post = f.pal.beam, planks = Mat.of(PLANKS);
			// The gallery's boards round the well, and panelling on the wall beneath them, in bays.
			for (int u = -half - 1; u <= half + 1; u++) {
				for (int v = -half - 1; v <= half + 1; v++) {
					if (!f.inFrame(u, v) || Math.hypot(u, v) < well) continue;
					f.put(u, gallery, v, planks);
					int x = f.frame.x(u, v), z = f.frame.z(u, v);
					if (f.t.depth(x, z) == 3) f.box(u, f.y, v, u, gallery - 1, v, planks);
				}
			}
			// The rail, standing on the gallery's edge.
			for (int u = -half; u <= half; u++) {
				for (int v = -half; v <= half; v++) {
					double far = Math.hypot(u, v);
					if (far >= well && far < well + 1 && f.idAt(u, gallery, v) == PLANKS) f.put(u, gallery + 1, v, cell(FENCE, 0));
				}
			}
			// The posts at the well's edge, with their fellows on the wall and spokes joining them under the gallery and at the ceiling. Where it's narrow, every other one's a thin post up to the gallery and no more.
			for (int i = 0; i < POSTS.length; i++) {
				int a = POSTS[i];
				int[] in = Floor.polar(a, well + 0.5), wall = f.against(a);
				if (narrow && i % 2 == 0) {
					f.box(in[0], f.y, in[1], in[0], gallery - 1, in[1], cell(FENCE, 0));
					continue;
				}
				f.pier(wall[0], wall[1], wall[0], wall[1], top, post);
				f.beam(in[0], in[1], wall[0], wall[1], gallery - 1, post);
				f.beam(in[0], in[1], wall[0], wall[1], top, post);
				f.pier(in[0], in[1], in[0], in[1], top, post);
			}
			if (!narrow) f.ring(well + 0.5, top, post);
			bays(f, gallery);
			ladder(f, POSTS[1], gallery);
			ladder(f, POSTS[5], gallery);
			desk(f);
			// Up on the gallery, chests against the wall; in the well, something patrolling.
			int[] a = f.against(45), b = f.against(225), c = f.against(135);
			f.chestAt(a[0], gallery + 1, a[1], Floor.outward(a[0], a[1]), "books");
			f.chestAt(b[0], gallery + 1, b[1], Floor.outward(b[0], b[1]), "books");
			if (f.r.nextBoolean()) f.chestAt(c[0], gallery + 1, c[1], Floor.outward(c[0], c[1]), "maps");
			int[] s0 = Floor.polar(135, well - 2), s1 = Floor.polar(315, well + (narrow ? 1.5 : 2));
			f.spawnerAt(s0[0], f.y, s0[1]);
			f.spawnerAt(s1[0], gallery + 1, s1[1]);
			fire(f);
			f.cobwebCorners(12);
		}

		/** Under the gallery, what each bay holds: on the flat runs of the wall, cases and places to read by turns; on the diagonals, bare shelves. */
		private void bays(Floor f, int gallery) {
			int turn = f.r.nextInt(2);
			for (int i = 0; i < 4; i++) {
				if ((i + turn) % 2 == 0) bookcase(f, i * 90, f.y, 2);
				else reading(f, i);
				bareShelves(f, 45 + i * 90);
			}
			// And up on the gallery two taller ones, facing each other across the well, where you see them from anywhere.
			int side = f.r.nextInt(2);
			bookcase(f, side * 90, gallery + 1, 3);
			bookcase(f, side * 90 + 180, gallery + 1, 3);
		}

		/** Three wide on the wall, {@code high} high. */
		private void bookcase(Floor f, int degrees, int from, int high) {
			int[] c = f.against(degrees);
			int d = Floor.outward(c[0], c[1]), side = d + 1 & 3;
			for (int w = -1; w <= 1; w++) {
				int[] at = f.toWall(c[0] + DX[side] * w, c[1] + DZ[side] * w, d);
				f.box(at[0], from, at[1], at[0], from + high - 1, at[1], Mat.of(BOOKSHELF));
			}
		}

		/** Where there were more: planks at the back and three slabs of shelf with nothing on them. */
		private void bareShelves(Floor f, int degrees) {
			int[] c = f.against(degrees);
			int d = Floor.outward(c[0], c[1]);
			int other = Math.abs(c[0]) >= Math.abs(c[1]) ? c[1] >= 0 ? SOUTH : NORTH : c[0] >= 0 ? EAST : WEST;
			for (int k = -1; k <= 1; k++) {
				int u = c[0] + (k == -1 ? DX[d] * -1 : k == 1 ? DX[other] * -1 : 0), v = c[1] + (k == -1 ? DZ[d] * -1 : k == 1 ? DZ[other] * -1 : 0);
				int[] at = f.toWall(u, v, d);
				if (!f.inFrame(at[0], at[1])) continue;
				for (int yy = f.y; yy <= f.y + 2; yy++) f.put(at[0], yy, at[1], cell(SLAB, WOOD_SLAB));
				f.p.set(f.frame.x(at[0] + DX[d], at[1] + DZ[d]), f.y + 1, f.frame.z(at[0] + DX[d], at[1] + DZ[d]), cell(PLANKS, 0));
			}
		}

		/** A low bench of a slab along the wall, and a table before it. The table's a fence post and a slab, as a plate won't stay on a fence in beta. */
		private void reading(Floor f, int i) {
			int degrees = i * 90;
			int[] c = f.against(degrees);
			int d = Floor.outward(c[0], c[1]), side = d + 1 & 3;
			for (int w = -1; w <= 1; w++) {
				int[] at = f.toWall(c[0] + DX[side] * w, c[1] + DZ[side] * w, d);
				f.box(at[0], f.y, at[1], at[0], f.y + 2, at[1], Mat.of(AIR));
				f.put(at[0], f.y, at[1], cell(SLAB, WOOD_SLAB));
				int tu = at[0] - DX[d] * 2, tv = at[1] - DZ[d] * 2;
				f.put(tu, f.y, tv, cell(FENCE, 0));
				f.put(tu, f.y + 1, tv, cell(SLAB, WOOD_SLAB));
			}
		}

		/** A table three square in the middle of the well on four thin legs and a post through it, and a slab to sit on at each side. */
		private void desk(Floor f) {
			for (int u = -1; u <= 1; u++) {
				for (int v = -1; v <= 1; v++) {
					f.put(u, f.y + 1, v, cell(SLAB, WOOD_SLAB));
					if (u != 0 && v != 0) f.put(u, f.y, v, cell(FENCE, 0));
				}
			}
			f.box(0, f.y, 0, 0, f.y + 1, 0, cell(FENCE, 0));
			for (int d = 0; d < 4; d++) f.put(DX[d] * 3, f.y, DZ[d] * 3, cell(SLAB, WOOD_SLAB));
		}

		/** Up the side of a post that looks at the wall, and through the gallery beside it. */
		private void ladder(Floor f, int degrees, int gallery) {
			int[] in = Floor.polar(degrees, well(f) + 0.5);
			int d = Floor.outward(in[0], in[1]), u = in[0] + DX[d], v = in[1] + DZ[d];
			int back = f.frame.dir(opposite(d));
			f.put(u, gallery, v, Mat.of(AIR));
			for (int yy = f.y; yy <= gallery; yy++) f.put(u, yy, v, cell(LADDER, backTo(back)));
			f.put(u, gallery + 1, v, Mat.of(AIR));
		}

		/** A bay the fire had: boards, posts and shelves gone, down to what's under them. */
		private void fire(Floor f) {
			int[] c = Floor.polar(45 + 90 * f.r.nextInt(4), f.rim() - 2);
			int size = 2 + f.r.nextInt(2);
			for (int u = c[0] - size; u <= c[0] + size; u++) {
				for (int v = c[1] - size; v <= c[1] + size; v++) {
					double far = Math.hypot(u - c[0], v - c[1]);
					if (far > size || !f.inFrame(u, v)) continue;
					for (int yy = f.y; yy <= f.top(); yy++) {
						int id = f.idAt(u, yy, v);
						if (id == BOOKSHELF || id == PLANKS || id == FENCE || id == LOG || id == SLAB || id == CHEST || id == SPAWNER) f.put(u, yy, v, Mat.of(AIR));
					}
					f.put(u, f.y - 1, v, f.burnt());
				}
			}
		}
	}

	/** A drowned forest of columns in rings, standing out of black water to a wheel of stone beams: a walk round the wall and a causeway across with a round landing halfway, both built up from the bottom, something at the bottom for whoever dives. */
	private static final class Cistern extends FloorKind {
		Cistern() {
			super("cistern", Size.TALL, 13, 18, 0.2, 0.95);
		}

		@Override
		double weight(Tower t) {
			return t.is(Climate.DESERT) ? 0.5 : t.is(Climate.WET) ? 2.5 : 1.4;
		}

		@Override
		int waterOffset(int h) {
			return 3 + (h - 13) / 3;
		}

		@Override
		int standOffset(int h) {
			return waterOffset(h) + 2;
		}

		@Override
		Mat overhead(Floor f) {
			return Mat.of(DOUBLE_SLAB, STONE_SLAB);
		}

		@Override
		void draw(Floor f) {
			int half = f.half(), water = f.waterTop, walk = f.stand - 1, top = f.top();
			double rim = f.rim();
			Mat trim = f.pal.trim, flags = f.pal.flags(f.t);
			f.fill(f.t.cx - half, f.y, f.t.cz - half, f.t.cx + half, water, f.t.cz + half, Mat.of(WATER));
			// The columns, out of the water to the rings of beam.
			boolean two = half >= 10;
			double outer = rim - 3.5, inner = 4;
			if (two) ringOfColumns(f, inner, 6, 30, walk, top);
			ringOfColumns(f, outer, two ? 12 : 8, two ? 15 : 22, walk, top);
			// The walk round the wall.
			for (int u = -half - 1; u <= half + 1; u++) {
				for (int v = -half - 1; v <= half + 1; v++) {
					if (!f.inFrame(u, v) || Math.hypot(u, v) < rim - 1.5) continue;
					f.put(u, walk, v, flags);
					// Built up from the bottom: water under it would be a place a swimmer can't come up (air there, the seal closes).
					f.box(u, f.y, v, u, walk - 1, v, f.pal.wall);
				}
			}
			// Pilasters up the wall above it, carrying the beams, and courses round the wall between them.
			int step = two ? 30 : 45;
			for (int a = two ? 15 : 22, i = 0; a < 360; a += step, i++) {
				int[] wall = f.against(a), at = Floor.polar(a, outer);
				for (int yy = f.stand; yy <= top; yy++) f.put(wall[0], yy, wall[1], trim);
				f.beam(at[0], at[1], wall[0], wall[1], top, trim);
			}
			f.wallCourse(f.stand + 4, trim);
			f.wallCourse(f.stand + 8, trim);
			// The spine across, with a round landing halfway and a gap to jump where it's fallen in.
			int gap = f.r.nextInt(half) - half / 2;
			if (Math.abs(gap) <= 4) gap = gap >= 0 ? 5 : -5;
			// Built up from the bottom too, as the walk round the wall is.
			for (int u = -half; u <= half; u++) {
				if (Math.abs(u - gap) <= 1) continue;
				f.box(u, f.y, 0, u, walk - 1, 1, f.pal.wall);
				f.box(u, walk, 0, u, walk, 1, flags);
			}
			f.column(0.5, 0.5, 2.5, f.y, walk - 1, f.pal.wall);
			f.disc(0.5, 0.5, 2.5, walk, flags);
			f.spawnerAt(1, f.stand, 0);
			sunken(f, half / 2);
			sunken(f, -half / 2);
		}

		private void ringOfColumns(Floor f, double far, int count, int from, int walk, int top) {
			for (int i = 0; i < count; i++) {
				int[] at = Floor.polar(from + 360.0 * i / count, far);
				if (Math.abs(at[1]) <= 1 || !f.pier(at[0], at[1], at[0], at[1], top, f.pal.pillar)) continue;
				f.put(at[0], f.waterTop + 1, at[1], f.pal.trim);
				f.put(at[0], walk + 1, at[1], f.pal.trim);
			}
			f.ring(far, top, f.pal.trim);
		}
	}

	/** A chest on the bottom in open water, clear of the columns and what's over them. */
	private static void sunken(Floor f, int v) {
		for (int u = -f.half(); u <= f.half(); u++) {
			if (f.idAt(u, f.y, v) == WATER && f.idAt(u, f.y + 1, v) == WATER && f.r.nextInt(3) == 0) {
				f.chestAt(u, f.y, v, SOUTH, "sunken");
				return;
			}
		}
	}

	/**
	 * A round shaft nearly the floor's whole width and a long way down: a broad rim, three plank bridges with rails over to a platform on a pier in the middle, some with their middles gone, and the fourth side left open. The shaft's wall is ribs and bands with the stone set back between them, and a ramp winds down it to a ledge at the water, which is where the way on starts: so nothing but timber stands in the drop, and a way coming in lands on a stage of its own at the rim.
	 */
	private static final class Bridges extends FloorKind {
		private static final int[] SPOKES = {45, 135, 225, 315};

		Bridges() {
			super("bridges over a pit", Size.TALL, 14, 20, 0.15, 0.9);
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
			return f.rim() - 1.5;
		}

		private static int pierR(Floor f) {
			return pit(f) >= 8 ? 2 : 1;
		}

		/** The pier in the middle stands whole. */
		@Override
		boolean allows(Floor f, int x, int z) {
			return Math.hypot(x - f.t.cx, z - f.t.cz) >= pierR(f) + 0.5;
		}

		@Override
		void draw(Floor f) {
			int half = f.half(), rim = f.stand - 1, top = f.top();
			double pit = pit(f), deck = pit >= 9 ? 3.5 : 2.5;
			int pierR = pierR(f);
			Mat wall = f.pal.wall, dressed = f.pal.trim;
			// The rock left round the shaft, with a coping at the rim and bands down it.
			for (int u = -half - 1; u <= half + 1; u++) {
				for (int v = -half - 1; v <= half + 1; v++) {
					if (!f.inFrame(u, v)) continue;
					double far = Math.hypot(u, v);
					if (far >= pit) {
						f.box(u, f.y, v, u, rim, v, wall);
						if (far < pit + 1) f.put(u, rim, v, dressed);
						// Set back between the ribs and the bands, down the whole of the shaft.
						if (far < pit + 1) {
							for (int yy = f.waterTop + 1; yy < rim; yy++) {
								if ((rim - yy) % 4 == 0) f.put(u, yy, v, dressed);
								else if (!onRib(u, v)) f.put(u, yy, v, Mat.of(AIR));
								else f.put(u, yy, v, f.pal.pillar);
							}
						}
					} else if (far >= pit - 1.5) {
						// Under the ledge and the ramp's foot, solid from the bottom: water under them would be a place a swimmer can't come up.
						f.box(u, f.y, v, u, f.waterTop, v, wall);
					} else f.box(u, f.y, v, u, f.waterTop, v, Mat.of(WATER));
				}
			}
			// The pier in the middle and the platform on it, with a chest let into its foot for whoever falls and looks.
			f.column(0, 0, pierR + 0.5, f.y, rim - 2, (x, yy, z) -> (rim - yy) % 4 == 0 ? dressed.at(x, yy, z) : wall.at(x, yy, z));
			int reach = (int) deck + 1;
			for (int u = -reach; u <= reach; u++) {
				for (int v = -reach; v <= reach; v++) {
					if (Math.hypot(u, v) < deck) {
						f.put(u, rim - 1, v, dressed);
						f.put(u, rim, v, Mat.of(PLANKS));
					}
				}
			}
			for (int d = 0; d < 4; d++) {
				int nu = DX[d] * (pierR + 1), nv = DZ[d] * (pierR + 1);
				if (f.p.held(f.frame.x(nu, nv), f.waterTop + 1, f.frame.z(nu, nv))) continue;
				f.box(DX[d] * pierR, f.waterTop + 1, DZ[d] * pierR, DX[d] * pierR, f.waterTop + 2, DZ[d] * pierR, Mat.of(AIR));
				f.chestAt(DX[d] * pierR, f.waterTop + 1, DZ[d] * pierR, opposite(d), "pit");
				break;
			}
			f.spawnerAt(1, f.stand, 1);
			int none = f.r.nextInt(4);
			for (int d = 0; d < 4; d++) {
				if (d != none) bridge(f, d, rim, deck, pit);
			}
			ledge(f, pit);
			ramp(f, rim, pit);
			// Above: pilasters on the chamber's wall and a wheel of beams overhead.
			for (int a : SPOKES) {
				int[] wallCell = f.against(a);
				f.pier(wallCell[0], wallCell[1], wallCell[0], wallCell[1], top, f.pal.beam);
				f.box(wallCell[0], f.y, wallCell[1], wallCell[0], rim, wallCell[1], wall);
			}
			f.beam(-(int) f.rim(), 0, (int) f.rim(), 0, top, f.pal.beam);
			f.beam(0, -(int) f.rim(), 0, (int) f.rim(), top, f.pal.beam);
			// Ladders, for those who fell, wherever a way has left the ramp no room.
			ladders(f, rim, pit);
			f.cobwebCorners(8);
		}

		private static boolean onRib(int u, int v) {
			double a = Math.toDegrees(Math.atan2(v, u));
			double off = Math.abs(Math.floorMod((int) Math.round(a) - 15, 30));
			return off <= 4 || off >= 26;
		}

		/** Three wide with a rail each side, from the platform to the rim, carried on a log under its middle. A rotten one has lost its middle, and what's left holds to one end or the other. */
		private void bridge(Floor f, int d, int rim, double deck, double pit) {
			int du = DX[d], dv = DZ[d], from = (int) Math.ceil(deck), to = (int) pit;
			// A stage where it would meet the rim is the way across on that side.
			if (f.in != null) {
				int mx = f.frame.x(du * to, dv * to), mz = f.frame.z(du * to, dv * to);
				if (Math.abs(mx - f.in.x) <= f.in.reachX() && Math.abs(mz - f.in.z) <= f.in.reachZ()) return;
			}
			boolean rotten = to - from >= 4 && f.r.nextInt(3) == 0;
			int mid = (from + to) / 2;
			for (int k = from; k <= to; k++) {
				if (rotten && (k == mid || k == mid + 1)) continue;
				for (int w = -1; w <= 1; w++) f.put(du * k + dv * w, rim, dv * k + du * w, cell(PLANKS, 0));
				f.put(du * k, rim - 1, dv * k, f.pal.beam);
				for (int w = -1; w <= 1; w += 2) f.put(du * k + dv * w, rim + 1, dv * k + du * w, cell(FENCE, 0));
			}
		}

		/** A step's width round the shaft just over the water, to climb out on. */
		private void ledge(Floor f, double pit) {
			int reach = (int) pit + 1, y = f.waterTop + 1;
			for (int u = -reach; u <= reach; u++) {
				for (int v = -reach; v <= reach; v++) {
					double far = Math.hypot(u, v);
					if (far >= pit - 1 && far < pit + 1 && f.inFrame(u, v)) f.put(u, y, v, far < pit ? cell(SLAB, f.pal.slab) : f.pal.pillar.at(f.frame.x(u, v), y, f.frame.z(u, v)));
				}
			}
		}

		/** A ramp two wide winding down the shaft's wall, half a block a step, from the rim to the ledge, on whichever stretch of wall no way has taken; none at all if that's too short. */
		private void ramp(Floor f, int rim, double pit) {
			double perDegree = 0.5 * pit * Math.PI / 180;
			double drop = rim - (f.waterTop + 1);
			int sweep = (int) Math.ceil(drop / perDegree);
			if (sweep >= 350) sweep = 350;
			int start = -1, dir = 1;
			// Clear of the bridges at the top: it has to have dropped three by the time it passes under one.
			int[] starts = {30, 120, 210, 300};
			for (int i = 0, first = f.r.nextInt(4); i < 8 && start < 0; i++) {
				int s = starts[(i + first) % 4] + (i < 4 ? 0 : 30);
				int d = i < 4 ? 1 : -1;
				if (rampClear(f, s, d, sweep, pit)) {
					start = s;
					dir = d;
				}
			}
			if (start < 0) return;
			int reach = (int) pit + 2;
			for (int u = -reach; u <= reach; u++) {
				for (int v = -reach; v <= reach; v++) {
					double far = Math.hypot(u, v);
					if (far < pit - 1.5 || far >= pit + 1 || !f.inFrame(u, v)) continue;
					double a = Math.floorMod((int) Math.round(Math.toDegrees(Math.atan2(v, u))) - start, 360);
					if (dir < 0) a = Math.floorMod(-(int) a, 360);
					if (a > sweep) continue;
					double level = rim - Math.floor(a * perDegree * 2) / 2;
					int y = (int) Math.floor(level);
					int at = level == y ? f.pal.wall.at(f.frame.x(u, v), y, f.frame.z(u, v)) : cell(SLAB, f.pal.slab);
					f.put(u, level == y ? y : y + 1, v, at);
					for (int yy = (level == y ? y : y + 1) + 1; yy <= (level == y ? y : y + 1) + 2 && yy < rim; yy++) {
						if (far < pit) f.put(u, yy, v, Mat.of(AIR));
					}
				}
			}
		}

		/** Whether it can wind down there: clear of what's held at each height it passes, and of the posts of any stage over it. */
		private boolean rampClear(Floor f, int start, int dir, int sweep, double pit) {
			double perDegree = 0.5 * pit * Math.PI / 180;
			for (int a = 0; a <= sweep; a += 3) {
				int[] c = Floor.polar(start + dir * a, pit - 1);
				int x = f.frame.x(c[0], c[1]), z = f.frame.z(c[0], c[1]), level = f.stand - 1 - (int) (a * perDegree);
				for (int y = level; y <= level + 3; y++) {
					if (f.p.held(x, y, z)) return false;
				}
				if (f.in != null && Math.abs(x - f.in.x) <= f.in.reachX() + 1 && Math.abs(z - f.in.z) <= f.in.reachZ() + 1) return false;
			}
			return true;
		}

		/** A long ladder up the shaft each side of the middle, on the first bit of wall that's free. */
		private void ladders(Floor f, int rim, double pit) {
			for (int k = 0; k < 2; k++) {
				for (int a : new int[] {0, 30, -30, 60, -60}) {
					int[] c = Floor.polar((k == 0 ? 60 : 240) + a, pit - 0.6);
					int d = Floor.outward(c[0], c[1]);
					int x = f.frame.x(c[0], c[1]), z = f.frame.z(c[0], c[1]);
					if (f.p.held(x, f.waterTop + 1, z) || f.p.held(x, rim, z)) continue;
					if (!solid(f.idAt(c[0] + DX[d], rim, c[1] + DZ[d]))) continue;
					for (int yy = f.waterTop + 1; yy <= rim; yy++) {
						f.put(c[0] + DX[d], yy, c[1] + DZ[d], f.pal.pillar);
						f.put(c[0], yy, c[1], cell(LADDER, backTo(f.frame.dir(d))));
					}
					break;
				}
			}
		}
	}

	/** Where they stopped building and started digging: a round chamber at the bottom of the workings on timber posts, four straight drifts out to the wall with a rail that doesn't go anywhere any more, timbered at regular sets, rough branches and a rough ring joining some of them, with what the rock really holds showing in the walls: scattered veins, as beta's own ground has them. */
	private static final class Gallery extends FloorKind {
		private static final int[] POSTS = {45, 135, 225, 315};

		Gallery() {
			super("mine gallery", Size.MEDIUM, 6, 8, 0.3, 1);
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
		double heart(Floor f) {
			return 3.5;
		}

		@Override
		void draw(Floor f) {
			int half = f.half();
			Noise n = new Noise(f.t.seed ^ f.index * 0xC0FFEEL);
			f.rock(-half, f.y, -half, half, f.top(), half, Mat.of(STONE));
			// The wall's inner skin is the rock too, so a drift ends in rock and not in the tower's brick.
			for (int x = f.t.cx - f.t.radius; x <= f.t.cx + f.t.radius; x++) {
				for (int z = f.t.cz - f.t.radius; z <= f.t.cz + f.t.radius; z++) {
					if (f.t.depth(x, z) != 2) continue;
					for (int yy = f.y; yy <= f.top(); yy++) f.p.force(x, yy, z, cell(STONE, 0));
				}
			}
			veins(f);
			List<int[]> sets = new ArrayList<>();
			double hub = 3.5;
			for (int d = 0; d < 4; d++) {
				int[] end = drift(f, n, DX[d] * 3, DZ[d] * 3, DX[d] * half, DZ[d] * half, true, sets);
				if (f.r.nextInt(2) == 0 && f.idAt(end[0], f.y, end[1]) != SPAWNER) f.chestAt(end[0], f.y, end[1], d, f.depth > 0.75 ? "pit" : "stores");
				// A branch off it, following a seam.
				if (f.r.nextInt(3) != 0) {
					int side = d + (f.r.nextBoolean() ? 1 : 3) & 3, at = 5 + f.r.nextInt(Math.max(1, half - 6));
					int reach = 3 + f.r.nextInt(3);
					int[] tip = drift(f, n, DX[d] * at + DX[side] * 2, DZ[d] * at + DZ[side] * 2, DX[d] * at + DX[side] * (2 + reach), DZ[d] * at + DZ[side] * (2 + reach), false, sets);
					if (f.idAt(tip[0], f.y, tip[1]) == AIR && f.r.nextInt(2) == 0) f.chestAt(tip[0], f.y, tip[1], side, "stores");
					if (f.r.nextInt(3) == 0) f.spawnerAt(tip[0] - DX[side], f.y, tip[1] - DZ[side]);
					if (f.r.nextInt(3) == 0) stope(f, tip[0], tip[1]);
				}
			}
			ringDrift(f, n, half);
			chamber(f, hub);
			for (int[] set : sets) timber(f, set[0], set[1], set[2] == 1);
			int[] s = Floor.polar(45 + 90 * f.r.nextInt(4), hub + 3);
			if (f.idAt(s[0], f.y, s[1]) == AIR) f.spawnerAt(s[0], f.y, s[1]);
			f.cobwebCorners(24);
		}

		/** A round chamber taller than the drifts, a post and a cap of timber at each quarter, joined overhead. */
		private void chamber(Floor f, double hub) {
			int high = Math.min(f.top() - 1, f.y + 4);
			int reach = (int) hub + 1;
			for (int u = -reach; u <= reach; u++) {
				for (int v = -reach; v <= reach; v++) {
					if (Math.hypot(u, v) < hub) f.cut(u, f.y, v, u, high, v);
				}
			}
			for (int a : POSTS) {
				int[] at = Floor.polar(a, hub - 0.7);
				f.pier(at[0], at[1], at[0], at[1], high, f.pal.beam);
			}
			f.ring(hub - 0.7, high, f.pal.beam);
			int[] pile = Floor.polar(f.r.nextInt(360), 1);
			f.rubble(f.frame.x(pile[0], pile[1]), f.frame.z(pile[0], pile[1]), 1, Mat.of(GRAVEL));
		}

		/** Some stretches of a rough drift round near the wall, joining one drift to the next, wherever it's been worth it. */
		private void ringDrift(Floor f, Noise n, int half) {
			double round = f.rim() - 2;
			for (int q = 0; q < 4; q++) {
				if (f.r.nextBoolean()) continue;
				for (int a = q * 90; a <= q * 90 + 90; a += 2) {
					double wander = (n.at(a * 0.2, 5, q, 7) - 0.5) * 1.6;
					int[] c = Floor.polar(a, round + wander);
					for (int du = -1; du <= 1; du++) {
						for (int dv = -1; dv <= 1; dv++) {
							if (Math.abs(du) + Math.abs(dv) > 1 || !f.inFrame(c[0] + du, c[1] + dv)) continue;
							f.cut(c[0] + du, f.y, c[1] + dv, c[0] + du, f.y + 2, c[1] + dv);
						}
					}
				}
				if (f.r.nextInt(3) == 0) {
					int[] c = Floor.polar(q * 90 + 45, round);
					stope(f, c[0], c[1]);
				}
			}
		}

		/**
		 * Veins by beta's own rule for what lies at this depth, a few times as thick as the ground outside, the mine being here for a reason; and gravel, as beta's rock has.
		 * <p>
		 * No lapis lazuli (the user, 2026-11-12). Towers drawn until then had a vein of it in one deep floor in five, and a tower is drawn again from its seed for every chunk of it, some perhaps
		 * made before and some after: so the vein's place in the drawing stays. {@link #NO_ORE} makes every draw that vein made and lays nothing, and the cells it would have taken are kept from
		 * the gravel after it, which only takes stone and never took those. Every other cell of every tower then comes out as it always did.
		 */
		private void veins(Floor f) {
			Random r = f.r;
			Set<Long> spared = new HashSet<>();
			for (int i = 3 + r.nextInt(3); i > 0; i--) vein(f, COAL_ORE, 8 + r.nextInt(9), spared);
			for (int i = 2 + r.nextInt(3); i > 0; i--) vein(f, IRON_ORE, 4 + r.nextInt(5), spared);
			if (f.y < 32) {
				if (r.nextInt(2) == 0) vein(f, GOLD_ORE, 4 + r.nextInt(5), spared);
				if (r.nextInt(5) == 0) vein(f, NO_ORE, 4 + r.nextInt(3), spared);
			}
			vein(f, GRAVEL, 8 + r.nextInt(8), spared);
		}

		/** Beta's {@code OreFeature}, written into stone: blobs along a short line, no two alike. Half lie where a drift's walls will show them, the drift being there for them; the rest anywhere on the floor. */
		private void vein(Floor f, int id, int size, Set<Long> spared) {
			Random r = f.r;
			int half = f.half();
			int along = r.nextInt(half * 2 + 1) - half, off = (r.nextBoolean() ? 1 : -1) * (2 + r.nextInt(2));
			int vu, vv;
			if (r.nextBoolean()) {
				boolean uWays = r.nextBoolean();
				vu = uWays ? along : off;
				vv = uWays ? off : along;
			} else {
				vu = along;
				vv = r.nextInt(half * 2 + 1) - half;
			}
			double mx = f.frame.x(vu, vv), my = f.y + r.nextInt(Math.min(f.h, 4)), mz = f.frame.z(vu, vv);
			float angle = r.nextFloat() * (float) Math.PI;
			double x0 = mx + Math.sin(angle) * size / 8, x1 = mx - Math.sin(angle) * size / 8, z0 = mz + Math.cos(angle) * size / 8, z1 = mz - Math.cos(angle) * size / 8;
			double y0 = my + r.nextInt(3) - 1, y1 = my + r.nextInt(3) - 1;
			for (int i = 0; i <= size; i++) {
				double cx = x0 + (x1 - x0) * i / size, cy = y0 + (y1 - y0) * i / size, cz = z0 + (z1 - z0) * i / size;
				double spread = (Math.sin(i * Math.PI / size) + 1) * (r.nextDouble() * size / 16) + 1;
				for (int x = (int) Math.floor(cx - spread / 2); x <= (int) Math.floor(cx + spread / 2); x++) {
					for (int y = (int) Math.floor(cy - spread / 2); y <= (int) Math.floor(cy + spread / 2); y++) {
						for (int z = (int) Math.floor(cz - spread / 2); z <= (int) Math.floor(cz + spread / 2); z++) {
							double dx = (x + 0.5 - cx) / (spread / 2), dy = (y + 0.5 - cy) / (spread / 2), dz = (z + 0.5 - cz) / (spread / 2);
							if (dx * dx + dy * dy + dz * dz < 1 && y >= f.y && y <= f.top() && f.t.depth(x, z) >= 2 && f.p.id(x, y, z) == STONE && !spared.contains(Plan.key(x, y, z))) {
								if (id == NO_ORE) spared.add(Plan.key(x, y, z));
								else if (f.p.isRock(x, y, z)) f.p.rock(x, y, z, cell(id, 0));
								else f.p.force(x, y, z, cell(id, 0));
							}
						}
					}
				}
			}
		}

		/** Three wide and three high. A main drift runs straight and has the rail; a branch wanders a cell or so, as a seam leads it. Returns the cell it ended at. */
		private int[] drift(Floor f, Noise n, int u0, int v0, int u1, int v1, boolean main, List<int[]> sets) {
			int steps = Math.max(Math.abs(u1 - u0), Math.abs(v1 - v0));
			boolean alongU = Math.abs(u1 - u0) > Math.abs(v1 - v0);
			int[] end = {u0, v0};
			for (int i = 0; i <= steps; i++) {
				int u = u0 + (u1 - u0) * i / Math.max(1, steps), v = v0 + (v1 - v0) * i / Math.max(1, steps);
				if (!main) {
					int wander = (int) Math.round((n.at(i, 3, u0 + v0, 7) - 0.5) * 3);
					if (alongU) v += wander;
					else u += wander;
				}
				if (!f.inFrame(u, v)) break;
				end = new int[] {u, v};
				for (int w = -1; w <= 1; w++) f.cut(u + (alongU ? 0 : w), f.y, v + (alongU ? w : 0), u + (alongU ? 0 : w), f.y + 2, v + (alongU ? w : 0));
				if (main && f.r.nextInt(9) != 0) f.put(u, f.y, v, cell(RAIL, rail(f.frame.dir(alongU ? EAST : SOUTH))));
				if (i > 0 && i % 3 == 0) sets.add(new int[] {u, v, alongU ? 1 : 0});
			}
			return end;
		}

		/** A timber set, regular as whoever put them in: two posts and a cap across the drift, whole or not at all, and only under rock, so it's holding something up. */
		private void timber(Floor f, int u, int v, boolean alongU) {
			int du = alongU ? 0 : 1, dv = alongU ? 1 : 0;
			for (int w = -1; w <= 1; w++) {
				int cu = u + du * w, cv = v + dv * w;
				for (int yy = f.y; yy <= f.y + 2; yy++) {
					if (!f.inFrame(cu, cv) || f.p.held(f.frame.x(cu, cv), yy, f.frame.z(cu, cv))) return;
					int id = f.idAt(cu, yy, cv);
					if (id != AIR && !(id == RAIL && w == 0)) return;
				}
				if (!solid(f.idAt(cu, f.y + 3, cv))) return;
			}
			for (int w = -1; w <= 1; w += 2) f.box(u + du * w, f.y, v + dv * w, u + du * w, f.y + 1, v + dv * w, Mat.of(FENCE));
			for (int w = -1; w <= 1; w++) f.put(u + du * w, f.y + 2, v + dv * w, cell(PLANKS, 0));
		}

		private void stope(Floor f, int u, int v) {
			int size = 2;
			for (int a = -size; a <= size; a++) {
				for (int b = -size; b <= size; b++) {
					if (a * a + b * b > size * size) continue;
					f.cut(u + a, f.y, v + b, u + a, f.top() - 1 - f.r.nextInt(2), v + b);
				}
			}
			f.rubble(f.frame.x(u, v), f.frame.z(u, v), 2, f.rubbleMat());
		}
	}

	/** A ring of lava let into the floor round an island, stone bridges over it at the quarters, and all round the wall a brick bench with cold furnaces set in it, under stacks going up to the ceiling, and a chest or two standing on the bench, out of the lava's reach. Stone overhead, nothing else here to catch. On one side the ring has met water, and turned. */
	private static final class Foundry extends FloorKind {
		Foundry() {
			super("foundry", Size.MEDIUM, 7, 10, 0.45, 1);
		}

		@Override
		double weight(Tower t) {
			return t.purpose == Tower.Purpose.MINE ? 4 : t.purpose == Tower.Purpose.GARRISON ? 1.2 : 0.6;
		}

		private static double channel(Floor f) {
			return f.rim() - 5;
		}

		@Override
		double heart(Floor f) {
			return 2;
		}

		/** Boards would catch from the lava. */
		@Override
		Mat underfoot(Floor f) {
			double channel = channel(f);
			Mat flags = Mat.of(DOUBLE_SLAB, STONE_SLAB), stone = Mat.of(STONE);
			return (x, y, z) -> Math.hypot(x - f.t.cx, z - f.t.cz) < channel ? flags.at(x, y, z) : stone.at(x, y, z);
		}

		@Override
		Mat overhead(Floor f) {
			return Mat.of(DOUBLE_SLAB, STONE_SLAB);
		}

		@Override
		boolean hotUnderfoot() {
			return true;
		}

		@Override
		void draw(Floor f) {
			int half = f.half(), top = f.top();
			double channel = channel(f);
			Mat trim = f.pal.trim, brick = Mat.of(BRICK);
			int cold = 45 + 90 * f.r.nextInt(4), reach = half + 1;
			double span = Math.toDegrees(2.2 / channel);
			for (int u = -reach; u <= reach; u++) {
				for (int v = -reach; v <= reach; v++) {
					if (!f.inFrame(u, v)) continue;
					double far = Math.hypot(u, v), off = Math.abs(Math.IEEEremainder(Math.toDegrees(Math.atan2(v, u)) - cold, 360));
					if (far >= channel - 1 && far < channel + 1) f.put(u, f.y - 1, v, off <= span ? cell(OBSIDIAN, 0) : cell(LAVA, 0));
					else if (far >= channel + 1 && far < channel + 2 && off <= span * 0.45) f.put(u, f.y - 1, v, cell(WATER, 0));
				}
			}
			// A plain slab of stone across it at each quarter.
			for (int d = 0; d < 4; d++) {
				int across = d + 1 & 3;
				for (int k = (int) channel - 2; k <= (int) channel + 2; k++) {
					for (int w = -1; w <= 1; w++) f.put(DX[d] * k + DX[across] * w, f.y - 1, DZ[d] * k + DZ[across] * w, Mat.of(DOUBLE_SLAB, STONE_SLAB));
				}
			}
			// The bench round the wall, the furnaces in it, a slab along its top.
			for (int x = f.t.cx - f.t.radius; x <= f.t.cx + f.t.radius; x++) {
				for (int z = f.t.cz - f.t.radius; z <= f.t.cz + f.t.radius; z++) {
					if (f.t.depth(x, z) != 3) continue;
					f.set(x, f.y, z, brick);
					f.set(x, f.y + 1, z, brick);
					f.set(x, f.y + 2, z, cell(SLAB, f.pal.slab));
				}
			}
			int step = half >= 10 ? 20 : 30;
			for (int a = step / 2; a < 360; a += step) {
				int[] at = f.against(a);
				f.put(at[0], f.y, at[1], cell(FURNACE, backTo(f.frame.dir(Floor.outward(at[0], at[1])))));
			}
			// Stacks up the wall from the bench, and ribs from them to the post in the middle.
			for (int a = 0; a < 360; a += 60) {
				int[] at = f.against(a), foot = Floor.polar(a, 1);
				f.box(at[0], f.y + 2, at[1], at[0], top, at[1], brick);
				f.beam(foot[0], foot[1], at[0], at[1], top, trim);
			}
			f.column(0, 0, 1.5, f.y, top, (x, yy, z) -> (yy - f.y) % 3 == 0 ? trim.at(x, yy, z) : f.pal.pillar.at(x, yy, z));
			if (channel >= 4) onBench(f, cold);
			onBench(f, cold + 90);
			int[] s0 = Floor.polar(cold + 180, channel + 1.5), s1 = Floor.polar(cold - 90, channel + 1.5);
			f.spawnerAt(s0[0], f.y, s0[1]);
			f.spawnerAt(s1[0], f.y, s1[1]);
			f.cobwebCorners(8);
		}

		/**
		 * A chest standing on the bench at about this angle, in place of its slab, the nearest place round from it that's free and out of the lava's reach. Anywhere on the floor near the
		 * channel, lava would light fire beside it; up on the bench it's always out of reach.
		 */
		private static void onBench(Floor f, double degrees) {
			for (int k = 0; k < 24; k++) {
				int[] c = f.against(degrees + (k % 2 == 0 ? 1 : -1) * 8 * ((k + 1) / 2));
				int x = f.frame.x(c[0], c[1]), z = f.frame.z(c[0], c[1]), seat = f.y + 2, back = Floor.outward(c[0], c[1]);
				// Where the bench steps round the curve, a cell can have bench either side and wall behind: its front has to be open to the room.
				if (f.idAt(c[0], seat - 1, c[1]) != BRICK || f.idAt(c[0], seat, c[1]) != SLAB || f.idAt(c[0], seat + 1, c[1]) != AIR || f.idAt(c[0] - DX[back], seat, c[1] - DZ[back]) != AIR
						|| f.p.held(x, seat, z) || f.p.held(x, seat + 1, z) || f.lavaReaches(c[0], seat, c[1])) continue;
				f.chestAt(c[0], seat, c[1], back, "arms");
				return;
			}
		}
	}

	/** Two floors that have become one: a ring of columns that held the upper floor, some whole, some snapped, one lying in the wreck; pilasters round the wall with the stumps of its joists; what's left of its boards clinging to them in a ragged shelf; and the rest in great heaps on the lower floor. */
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

		/** The floor's own, broken up where things came down on it. */
		@Override
		Mat underfoot(Floor f) {
			return Mat.patched(f.pal.floor, Mat.of(COBBLE), f.t.noise, 0.35, 3);
		}

		@Override
		void draw(Floor f) {
			int half = f.half(), upper = f.y + f.h / 2, top = f.top(), reach = half + 1;
			double rim = f.rim();
			Noise n = new Noise(f.t.seed ^ f.index * 0xFA11L);
			Mat trim = f.pal.trim, pillar = f.pal.pillar, beam = f.pal.beam;
			// The upper floor, where it held: a ragged shelf round the walls, wider here and there.
			for (int u = -reach; u <= reach; u++) {
				for (int v = -reach; v <= reach; v++) {
					if (!f.inFrame(u, v)) continue;
					double fromWall = rim - Math.hypot(u, v);
					if (fromWall <= 1 + n.at(u, 9, v, 4) * 3.5) f.put(u, upper, v, Mat.of(PLANKS));
				}
			}
			// Pilasters the whole height, with a joist's stump out from each under the boards, and a course at the shelf's height.
			int step = half >= 10 ? 30 : 45;
			for (int a = step / 2, i = 0; a < 360; a += step, i++) {
				int[] wall = f.against(a), stub = Floor.polar(a, rim - 3 - f.r.nextInt(2));
				f.pier(wall[0], wall[1], wall[0], wall[1], top, trim);
				f.beam(wall[0], wall[1], stub[0], stub[1], upper - 1, beam);
			}
			f.wallCourse(upper - 1, trim);
			// The ring of columns: whole, snapped, or gone.
			double ring = rim - 4.5;
			int count = 6, fallen = f.r.nextInt(count);
			List<int[]> stumps = new ArrayList<>();
			for (int i = 0; i < count; i++) {
				int[] at = Floor.polar(30 + 60 * i, ring);
				if (i == fallen) continue;
				int roll = f.r.nextInt(3);
				int stop = roll == 0 ? f.y + 1 + f.r.nextInt(upper - f.y) : top;
				f.pier(at[0], at[1], at[0], at[1], stop, pillar);
			}
			// The fallen one, lying across the floor as it broke: a few drums of it.
			int[] lie = Floor.polar(30 + 60 * fallen, ring);
			int along = f.r.nextBoolean() ? 1 : 3;
			for (int k = -2; k <= 2; k++) {
				if (k == 1) continue;
				int lu = lie[0] + DX[along] * k, lv = lie[1] + DZ[along] * k;
				if (f.inFrame(lu, lv) && f.idAt(lu, f.y, lv) == AIR) f.put(lu, f.y, lv, pillar);
			}
			int[] chest = f.against(f.r.nextInt(360 / step) * step);
			f.chestAt(chest[0], f.y, chest[1], Floor.outward(chest[0], chest[1]), "hall");
			// The heaps: cones of what came down, gravel with stone through it, tall enough to climb back up to the shelf from.
			int heaps = 3 + f.r.nextInt(2);
			for (int i = 0; i < heaps; i++) {
				double a = f.r.nextDouble() * Math.PI * 2, far = (0.2 + f.r.nextDouble() * 0.6) * half;
				heap(f, (int) (Math.cos(a) * far), (int) (Math.sin(a) * far), 3 + f.r.nextInt(3), i == 0 ? upper - f.y : 2 + f.r.nextInt(3), chest);
			}
			if (f.idAt(-half / 2, f.y, half / 2) == AIR) f.spawnerAt(-half / 2, f.y, half / 2);
			int[] shelf = f.toWall(0, 0, EAST);
			f.spawner(f.frame.x(shelf[0], shelf[1]), upper + 1, f.frame.z(shelf[0], shelf[1]));
			f.cobwebCorners(18);
		}

		private void heap(Floor f, int cu, int cv, int radius, int tall, int[] keep) {
			Mat rubble = f.rubbleMat();
			for (int u = cu - radius; u <= cu + radius; u++) {
				for (int v = cv - radius; v <= cv + radius; v++) {
					double far = Math.hypot(u - cu, v - cv);
					if (far > radius || !f.inFrame(u, v) || Math.max(Math.abs(u - keep[0]), Math.abs(v - keep[1])) <= 1) continue;
					int rise = (int) Math.round((radius - far) / radius * tall);
					for (int k = 0; k < rise; k++) {
						// Packed underneath so a heap doesn't shift if something beside it is disturbed.
						if (f.idAt(u, f.y + k, v) == AIR) f.put(u, f.y + k, v, rubble);
					}
				}
			}
		}
	}

	/** The bottom: a low black drum of a strongroom with its back to the tower's wall under a domed lid, a ring of lava round it, one bridge to its one iron door, and the rest of the round hall on pilasters, with ribs fanning overhead from them to a post on the lid. */
	private static final class Vault extends FloorKind {
		private static final int[] PILASTERS = {0, 30, 60, 90, 120, 150, 180, 210, 240, 270, 300, 330};

		Vault() {
			super("vault", Size.MEDIUM, 7, 9, 0.9, 1);
		}

		@Override
		double weight(Tower t) {
			return 0;
		}

		/** The strongroom's inside as a circle. */
		private static int room(Floor f) {
			return f.half() >= 10 ? 3 : 2;
		}

		/**
		 * Whether a place, measured from the drum's middle, is the drum's wall: out to its circle, and every cell touching the inside even at a corner. Lava a cell up and one across from the moat
		 * would otherwise land inside where the pixel circle's wall meets itself only at a corner, and set fire by the chests.
		 */
		private static boolean drumWall(int u, int v, int room) {
			if (Math.hypot(u, v) <= room + 0.5) return false;
			if (Math.hypot(u, v) <= room + 1.5) return true;
			for (int a = -1; a <= 1; a++) {
				for (int b = -1; b <= 1; b++) {
					if (Math.hypot(u + a, v + b) <= room + 0.5) return true;
				}
			}
			return false;
		}

		/** Lava's let into the floor, so a ladder's post into the hall is stone. */
		@Override
		boolean hotUnderfoot() {
			return true;
		}

		/** The drum's middle along {@code u}, touching the tower's wall behind it. Its door looks along {@code u} to the rest of the floor. */
		private static int middle(Floor f) {
			return -f.half() + room(f) + 1;
		}

		/** Ways come down in the hall, or at worst over the moat, never on the strongroom or in the way to its door. */
		@Override
		boolean allows(Floor f, int x, int z) {
			int u = f.frame.u(x, z) - middle(f), v = f.frame.v(x, z), room = room(f);
			if (Math.hypot(u, v) <= room + 2.5) return false;
			return u <= 0 || u > room + 4 || Math.abs(v) > 1;
		}

		/** Flags, with a band of cobble round the wall. */
		@Override
		Mat underfoot(Floor f) {
			Mat flags = Mat.of(DOUBLE_SLAB, STONE_SLAB), edge = Mat.of(COBBLE);
			double band = f.rim() - 1;
			return (x, y, z) -> Math.hypot(x - f.t.cx, z - f.t.cz) >= band ? edge.at(x, y, z) : flags.at(x, y, z);
		}

		@Override
		void draw(Floor f) {
			int half = f.half(), room = room(f), mid = middle(f), top = f.top();
			double moat = room + 3.5, drum = room + 1.5;
			Random r = f.r;
			Mat dressed = Mat.of(DOUBLE_SLAB, STONE_SLAB);
			// The moat, one cell down so it can't spill.
			for (int u = -half - 1; u <= half + 1; u++) {
				for (int v = -half - 1; v <= half + 1; v++) {
					double far = Math.hypot(u - mid, v);
					if (far > drum && far <= moat && !drumWall(u - mid, v, room)) f.put(u, f.y - 1, v, cell(LAVA, 0));
				}
			}
			f.goalX = f.frame.x(mid + room + 4, 0);
			f.goalZ = f.frame.z(mid + room + 4, 0);
			f.hasGoal = true;
			// The bridge over it, a single block wide.
			for (int k = room + 2; k <= room + 3; k++) f.put(mid + k, f.y - 1, 0, cell(DOUBLE_SLAB, STONE_SLAB));
			// Pilasters round the wall, wherever the moat isn't, ribs from them toward the drum, and a row of columns partway in.
			for (int a : PILASTERS) {
				int[] w = f.against(a);
				if (Math.hypot(w[0] - mid, w[1]) <= moat + 0.5) continue;
				f.pier(w[0], w[1], w[0], w[1], top, dressed);
				// A rib to the post on the drum's lid.
				f.beam(w[0], w[1], mid, 0, top, dressed);
				// Partway along the rib, a column where the floor's clear.
				int cu = (w[0] + mid + (w[0] - mid) / 2) / 2 + (w[0] - mid) / 4, cv = w[1] * 3 / 4;
				boolean clear = Math.hypot(cu - mid, cv) > moat + 1.5 && (Math.abs(cv) > 1 || cu < mid);
				if (clear) f.pier(cu, cv, cu, cv, top, f.pal.pillar);
			}
			// The strongroom: obsidian all round, floor and ceiling too (user, 2026-11-07), the one place in the tower digging takes real effort, under a stone lid stepping in to a dome with the post on it,
			// and an iron door you need the lever for.
			int rt = f.y + 3;
			for (int u = -room - 2; u <= room + 2; u++) {
				for (int v = -room - 2; v <= room + 2; v++) {
					double far = Math.hypot(u, v);
					if (far > drum && !drumWall(u, v, room)) continue;
					f.box(mid + u, f.y, v, mid + u, rt - 1, v, Mat.of(OBSIDIAN));
					f.put(mid + u, rt, v, far > drum - 1 ? dressed : Mat.of(STONE));
					if (rt + 1 <= top) f.put(mid + u, rt + 1, v, far > drum - 1 ? Mat.of(SLAB, f.pal.slab) : far > drum - 2 ? dressed : Mat.of(STONE));
				}
			}
			f.column(mid, 0, 1.2, rt + 1, top, dressed);
			for (int u = -room - 1; u <= room + 1; u++) {
				for (int v = -room - 1; v <= room + 1; v++) {
					if (Math.hypot(u, v) > room + 0.5) continue;
					f.box(mid + u, f.y, v, mid + u, rt - 1, v, Mat.of(AIR));
					f.put(mid + u, f.y - 1, v, cell(OBSIDIAN, 0));
					f.put(mid + u, rt, v, cell(OBSIDIAN, 0));
				}
			}
			int side = f.frame.dir(EAST), doorU = mid + room + 1;
			f.put(doorU, f.y, 0, cell(IRON_DOOR, doorOn(opposite(side))));
			f.put(doorU, f.y + 1, 0, cell(IRON_DOOR, doorOn(opposite(side)) | 8));
			f.put(doorU + 1, f.y + 1, 1, cell(LEVER, leverOn(opposite(side))));
			// In the middle, the best of it on a plinth; the others against the wall, either side.
			f.put(mid, f.y, 0, cell(DOUBLE_SLAB, STONE_SLAB));
			f.chestAt(mid, f.y + 1, 0, NORTH, "vault-best");
			int[][] spots = {{-room, 1}, {-room, -1}, {1, room}};
			for (int i = 0; i < spots.length; i++) f.chestAt(mid + spots[i][0], f.y, spots[i][1], spots[i][0] == -room ? WEST : SOUTH, "vault");
			// The guards: creepers in the hall, standing past the moat, to either side of the way to the bridge, and zombies behind the door (the user's call, 2026-11-09).
			int s = r.nextBoolean() ? 1 : -1, front = mid + room + 5;
			f.spawnerAt(front, f.y, s * (2 + r.nextInt(3)), "Creeper");
			f.spawnerAt(front + 1 + r.nextInt(Math.max(1, half - front - 1)), f.y, -s * (2 + r.nextInt(3)), f.t.monster(r, 1));
			f.spawnerAt(mid - room, f.y, 0, "Zombie");
			f.cobwebCorners(10);
		}
	}
}
