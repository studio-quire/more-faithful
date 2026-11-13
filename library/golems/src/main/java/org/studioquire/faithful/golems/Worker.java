package org.studioquire.faithful.golems;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import net.minecraft.block.Block;
import net.minecraft.block.ChestBlock;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.studioquire.oldfaithful.core.api.ItemStorage;
import org.studioquire.oldfaithful.core.api.Lookups;
import org.studioquire.oldfaithful.core.api.Sounds;
import org.studioquire.oldfaithful.core.api.Stacks;

/**
 * The loop every job shares. It finds the nearest work of its job's kind in its area that it can safely walk to and reach (the highest rank first), walks there, does one, rests as a
 * player would between two, and picks up what dropped. When it's full, or out of work with anything on it, it carries everything to its chest but what its work waiting could use; when
 * its tool breaks it takes another of the same kind there. Cracked, it goes to the chest between two pieces to mend itself with the iron there; out of work and short of what its work
 * waiting could use, it fetches that. Out of work, it waits at its post and looks again now and then, or a guard walks its rounds of its area ({@link Job#patrols}). With no chest, home is
 * its post, and what it carries is put down there.
 */
final class Worker {
	private enum Phase { DECIDE, TO_WORK, WORKING, GATHER, HOME, TO_POST, ROUNDS, WAIT }

	/** How far from its post its chest may be, each way. */
	static final int CHEST_RANGE = 8;
	/** How far past its area it may walk to get round to things. */
	static final int MARGIN = 4;
	/**
	 * How long work with nowhere to stand for it is passed over before it's tried again: digging nearby may give it somewhere. Something that moves only two seconds: a hostile out of
	 * an archer's reach now may walk into it.
	 */
	private static final int NO_STAND_TICKS = 400, NO_STAND_MOVING = 40;
	/** How far round where it stands it goes for what its work dropped before moving on, and for how long: its reach of 2, and a little for what rolls. */
	private static final int GATHER_RANGE = 4, GATHER_TICKS = 300;
	/** Steps straight at something lying about, once where it stands would touch it, before it's left: two seconds of them, not counting time it's already against it. */
	private static final int PICKUP_STEPS = 40;
	/** A trip to the chest for something in it (iron to mend, what its work is short of) that got nothing waits this long before the next such trip. */
	private static final int CHEST_TRIP_AGAIN = 1200;
	/**
	 * Walking its rounds: how far it strolls at a time, at most, each way, as beta's mobs wander up to 6 blocks off; and how long it stands between, 3 to 8 seconds, round the 4 a beta
	 * mob idles on average (a one in 80 chance a tick of setting off).
	 */
	private static final int STROLL = 6, PAUSE = 60, PAUSE_SPREAD = 100;
	/** Stepping back from a target too near (a {@link Job#standOff} job's): how many steps it looks at, at most, for somewhere further off, and how often it looks again on the way. */
	private static final int BACK_STEPS = 6, BACK_AGAIN = 20;

	private final GolemEntity golem;
	private final Job job;
	private final World world;
	private final Walk walk;
	private Phase phase = Phase.DECIDE;
	private Target target;
	private int rest, waitFor, phaseTicks, clock;
	private int gatherX, gatherY, gatherZ, gatherItem;
	/** It's done work here since it last gathered: what dropped is to be picked up before it leaves. */
	private boolean dropsAround;
	private final Set<Integer> passedItems = new HashSet<>();
	private final Map<Long, Integer> refused = new HashMap<>();
	private boolean chestKnown;
	private int chestX, chestY, chestZ, chestLooked = -10000, homeFails;
	/** It's on its way to the chest for something in it; and when it may next make such a trip. */
	private boolean chestTrip;
	private int chestTripAt;
	/** The cell it began its piece in: how far it reached is measured from there, not from wherever a last slide has moved it by the piece's end. */
	private int fromX, fromY, fromZ;
	/** What it's stepping into to pick up, for how many steps, and when it last did; and whether the last it gave up on had moved off. */
	private int steppingFor = -1, steps, steppedAt;
	private boolean movedOff;
	/** The thing the gather last looked again for a place to stand for, and how many times. */
	private int replanFor = -1, replans;
	/** When its pause on its rounds ends; when it last set off stepping back, and until when it stands its ground with nowhere to step back to. */
	private int pauseEnds, backedAt, corneredUntil = -1;
	/** Steps it took back from what came too near, since it was loaded: a test's to read. */
	int backedOff;
	/** For a survey comparing ({@code ArcherRun}): never stepping back, as the first build's archer stood its ground. */
	static boolean standGround;
	/** What it last decided and why, for a test to print. */
	String status = "";
	/** How long its last look for work took, in microseconds, for a test to print. */
	long lastLookMicros;

	Worker(GolemEntity golem, Job job) {
		this.golem = golem;
		this.job = job;
		this.world = golem.world;
		this.walk = new Walk(golem);
	}

	/** Loaded with the world: two seconds before it looks about, as the land round it may still be coming in. */
	Worker loaded() {
		waitFor(40, "just loaded");
		return this;
	}

	Job job() {
		return job;
	}

	/** Waiting with nothing to do: out of work, or for a tool or room in its chest. */
	boolean waiting() {
		return phase == Phase.WAIT;
	}

	/** Called away: its tool was taken, or it was given another. */
	void stop() {
		if (phase == Phase.WORKING && target != null) job.abandon(target);
		walk.stop();
		phase = Phase.DECIDE;
		golem.setWorking(false);
	}

	void tick() {
		clock++;
		if (rest > 0) {
			rest--;
			golem.setWorking(false);
			return;
		}
		// A phase that ends without using its tick hands on to the next at once, so nothing waits a tick for nothing.
		for (int i = 0; i < 4 && step(); i++) {
		}
		golem.setWorking(job.swinging(phase == Phase.WORKING && !isItem(target)));
	}

	private static boolean isItem(Target t) {
		return t != null && t.entity instanceof ItemEntity;
	}

	/** One tick of the phase it's in. True if it moved on to another without using the tick. */
	private boolean step() {
		phaseTicks++;
		switch (phase) {
			case DECIDE:
				return decide();
			case TO_WORK:
				return toWork();
			case WORKING:
				return working();
			case GATHER:
				return gather();
			case HOME:
				return home();
			case TO_POST:
				return toPost();
			case ROUNDS:
				return rounds();
			case WAIT:
				if (--waitFor > 0) return false;
				return to(Phase.DECIDE);
			default:
				return false;
		}
	}

	private boolean to(Phase next) {
		phase = next;
		phaseTicks = 0;
		return true;
	}

	private boolean waitFor(int ticks, String why) {
		status = why;
		waitFor = ticks;
		phase = Phase.WAIT;
		phaseTicks = 0;
		return false;
	}

	// Deciding.

	/**
	 * What next. Work in reach from where it stands comes first, as a player works through what's round them; what that dropped is gathered before it walks off anywhere, as a
	 * player sweeps up before moving on, rather than stepping down into every hole for every block.
	 */
	private boolean decide() {
		target = null;
		boolean toolless = golem.tool() == null && !golem.equipSpare(job.kind());
		// Mended before the next piece, never halfway through one (a guard's fight is one): its owner's ingots, from its chest, as its owner would use them.
		// Its tool gone and none to be had, it waits by the chest all the same: it mends there too.
		if (golem.wantsMending() && mayTrip() && chestHolds(s -> s.itemId == Item.IRON_INGOT.id)) {
			if (dropsAround && !golem.isFull()) return gatherHere();
			return toChestFor("cracked: to its chest to mend");
		}
		if (!toolless && !golem.isFull()) {
			long t0 = System.nanoTime();
			Choice choice = choose();
			lastLookMicros = (System.nanoTime() - t0) / 1000;
			if (choice != null) {
				target = choice.target;
				targetClose = choice.close;
			}
			if (choice != null && WalkMap.get().steps(choice.node) == 0 && nearEnough()) {
				status = "working " + target;
				workingFrom();
				to(Phase.WORKING);
				return false;
			}
			if (dropsAround) return gatherHere();
			if (choice != null) {
				status = "walking to " + target;
				startWalk(choice.node);
				to(Phase.TO_WORK);
				return false;
			}
		} else if (dropsAround && !golem.isFull()) {
			return gatherHere();
		}
		if (toolless) {
			if (chest()) return goHome("its tool broke: to the chest for another");
			if (hasLoad()) return goHome("its tool broke: what it has goes down at its post");
			return goToPost("no tool, and no chest to take one from");
		}
		if (golem.isFull()) return goHome("full");
		if (mayTrip() && chestHolds(s -> shortOf(s) > 0)) return toChestFor("out of work: to its chest for what its work is short of");
		if (hasLoad()) return goHome("out of work: taking it all home");
		return job.patrols() ? walkRounds() : goToPost("out of work");
	}

	/** What its work here dropped, round where it stands, before it leaves. */
	private boolean gatherHere() {
		dropsAround = false;
		gatherX = MathHelper.floor(golem.x);
		gatherY = golem.feetY();
		gatherZ = MathHelper.floor(golem.z);
		passedItems.clear();
		gatherItem = -1;
		status = "gathering what dropped";
		return to(Phase.GATHER);
	}

	private static final class Choice {
		final Target target;
		final int node;
		final boolean close;

		Choice(Target target, int node, boolean close) {
			this.target = target;
			this.node = node;
			this.close = close;
		}
	}

	/** The work to do next: of the highest rank it can reach, the one nearest to walk to. */
	private Choice choose() {
		List<Target> found = new ArrayList<>();
		job.findWork(golem.area(), found);
		if (found.isEmpty()) return null;
		search();
		WalkMap map = WalkMap.get();
		double gx = golem.x, gz = golem.z;
		found.sort((a, b) -> a.rank != b.rank ? Integer.compare(b.rank, a.rank) : Double.compare(flat(a, gx, gz), flat(b, gx, gz)));
		// Steps to a place to stand are no fewer than the distance to the work, less how far from it it may stand.
		double slack = job.standAcross() * 1.415 + 1.0;
		for (int i = 0; i < found.size(); ) {
			int rank = found.get(i).rank, end = i;
			while (end < found.size() && found.get(end).rank == rank) end++;
			Target best = null;
			int bestNode = -1, bestSteps = Integer.MAX_VALUE;
			boolean bestClose = false;
			for (int j = i; j < end; j++) {
				Target t = found.get(j);
				if (best != null && Math.sqrt(flat(t, gx, gz)) - slack > bestSteps) break;
				if (refused(t.key())) continue;
				int node = standFor(map, t, bestSteps);
				if (node < 0) {
					// Nowhere at all to stand for it, near or far (not just further than the best so far): passed over for a while, rather than searched round at every look.
					if (bestSteps == Integer.MAX_VALUE) refuse(t, t.entity != null ? NO_STAND_MOVING : NO_STAND_TICKS);
					continue;
				}
				best = t;
				bestNode = node;
				bestSteps = map.steps(node);
				bestClose = standClose;
			}
			if (best != null) return new Choice(best, bestNode, bestClose);
			i = end;
		}
		return null;
	}

	private static double flat(Target t, double x, double z) {
		double dx = t.centerX() - x, dz = t.centerZ() - z;
		return dx * dx + dz * dz;
	}

	/**
	 * Where to stand to work a target, as a person walks up to their work: the nearest place within {@link GolemEntity#CLOSE} of it, and only if it can get to none, the nearest
	 * within its reach. The node, or -1 if that place is {@code under} steps away or more; {@link #standClose} says which kind it was.
	 */
	private int standFor(WalkMap map, Target t, int under) {
		double[] off = isItem(t) ? null : job.standOff(t);
		if (off != null) {
			standClose = false;
			return standOff(map, t, off, under);
		}
		int close = nearestStand(map, t, CLOSE_ACROSS, CLOSE_BELOW, CLOSE_ABOVE, true, Integer.MAX_VALUE);
		standClose = close >= 0;
		if (standClose) return map.steps(close) < under ? close : -1;
		return nearestStand(map, t, job.standAcross(), job.standBelow(), job.standAbove(), false, under);
	}

	/** Where {@link GolemEntity#CLOSE} reaches from its eye, 1.62 up: two across, three below a target (a block over its head) and one above (the floor beside its feet). */
	private static final int CLOSE_ACROSS = 2, CLOSE_BELOW = 3, CLOSE_ABOVE = 1;
	/** Whether the last {@link #standFor} was within {@link GolemEntity#CLOSE}; and for the target it's working towards. */
	private boolean standClose, targetClose;

	/** The places round a target it can walk to, nearest first, and the first it can work the target from: so the rays for sight are only cast until one is found. */
	private int nearestStand(WalkMap map, Target t, int across, int below, int above, boolean close, int under) {
		int n = 0;
		for (int y = t.y - below; y <= t.y + above; y++) {
			for (int x = t.x - across; x <= t.x + across; x++) {
				for (int z = t.z - across; z <= t.z + across; z++) {
					int node = map.node(x, y, z);
					if (node < 0 || map.steps(node) >= under || !mayStandAt(x, y, z, t)) continue;
					if (close && job.distance(x + 0.5, y + GolemEntity.EYE, z + 0.5, t) > GolemEntity.CLOSE) continue;
					if (n == stands.length) stands = java.util.Arrays.copyOf(stands, n * 2);
					stands[n++] = (long) map.steps(node) << 32 | node;
				}
			}
		}
		java.util.Arrays.sort(stands, 0, n);
		for (int i = 0; i < n; i++) {
			int node = (int) stands[i];
			if (reaches(map.x(node) + 0.5, map.y(node) + GolemEntity.EYE, map.z(node) + 0.5, t)) return node;
		}
		return -1;
	}

	/** Places to stand being weighed, as steps and node together. */
	private long[] stands = new long[1024];

	/**
	 * For a job that stands off: the nearest place it can walk to in the ring round the target, at any height within 6 of it (a perch), that it can work the target from; and with none
	 * there, the nearest nearer in that it can (a tower top with what it shoots at its foot), from where it steps back as it works if it can.
	 */
	private int standOff(WalkMap map, Target t, double[] off, int under) {
		int node = standOff(map, t, off[0], off[1], under);
		return node >= 0 || off[0] <= 0.0 ? node : standOff(map, t, 0.0, off[0], under);
	}

	private int standOff(WalkMap map, Target t, double nearest, double furthest, int under) {
		double[] off = {nearest, furthest};
		int n = 0, r = (int) Math.ceil(off[1]);
		double tx = t.centerX(), tz = t.centerZ();
		for (int y = t.y - 6; y <= t.y + 6; y++) {
			for (int x = t.x - r; x <= t.x + r; x++) {
				for (int z = t.z - r; z <= t.z + r; z++) {
					int node = map.node(x, y, z);
					if (node < 0 || map.steps(node) >= under) continue;
					double dx = x + 0.5 - tx, dz = z + 0.5 - tz, d = Math.sqrt(dx * dx + dz * dz);
					if (d < off[0] || d > off[1]) continue;
					if (n == stands.length) stands = java.util.Arrays.copyOf(stands, n * 2);
					stands[n++] = (long) map.steps(node) << 32 | node;
				}
			}
		}
		java.util.Arrays.sort(stands, 0, n);
		for (int i = 0; i < n; i++) {
			int node = (int) stands[i];
			if (reaches(map.x(node) + 0.5, map.y(node) + GolemEntity.EYE, map.z(node) + 0.5, t)) return node;
		}
		return -1;
	}

	/** Near enough to start on the target from where it stands: within reach, and within {@link GolemEntity#CLOSE} if that's where it was walking to. */
	private boolean nearEnough() {
		double ex = golem.reachX(), ey = golem.reachY(), ez = golem.reachZ();
		return (!targetClose || job.distance(ex, ey, ez, target) <= GolemEntity.CLOSE) && reaches(ex, ey, ez, target);
	}

	/** The job's reach, but for something lying about, which is the loop's: in touch. */
	private boolean reaches(double ex, double ey, double ez, Target t) {
		return isItem(t) ? touches(ex, ey, ez, t.entity.boundingBox) : job.reaches(ex, ey, ez, t);
	}

	private boolean mayStandAt(int x, int y, int z, Target t) {
		return isItem(t) || job.mayStandAt(x, y, z, t);
	}

	/** Whether, with its eye here over a cell's middle, what it picks up from would touch this box: its own box a block wider each way, as a player's. */
	private static boolean touches(double ex, double ey, double ez, Box b) {
		double half = GolemEntity.WIDTH / 2.0 + 1.0, feet = ey - GolemEntity.EYE;
		return b.maxX > ex - half && b.minX < ex + half && b.maxZ > ez - half && b.minZ < ez + half && b.maxY > feet && b.minY < feet + GolemEntity.HEIGHT;
	}

	/**
	 * Picking something up, as a player does, by walking into it: a step straight at it each tick it isn't against it yet, and it's taken as soon as it may be. False once it's had
	 * {@link #PICKUP_STEPS} steps and still isn't, or once the thing has gone out of touch of the cell it stands in: what rolls or drops away (off a ledge, into a ravine) isn't
	 * followed, as steps straight at it would follow it over the edge (a golem gathering in real woods once fell twelve blocks so).
	 */
	private boolean stepInto(ItemEntity item) {
		// Its steps are counted afresh for each thing, and each time it comes back to one.
		if (item.id != steppingFor || clock - steppedAt > 1) {
			steppingFor = item.id;
			steps = 0;
		}
		steppedAt = clock;
		if (golem.pickupBox().intersects(item.boundingBox)) return true;
		movedOff = !touches(golem.reachX(), golem.reachY(), golem.reachZ(), item.boundingBox);
		if (movedOff) return false;
		golem.stepToward((float) item.x, (float) item.boundingBox.minY, (float) item.z);
		return ++steps <= PICKUP_STEPS;
	}

	/** Everywhere it can walk from where it stands, over its area and a little round it, its post and its chest. */
	private void search() {
		int[] r = region();
		WalkMap.get().search(world, MathHelper.floor(golem.x), golem.feetY(), MathHelper.floor(golem.z), r[0], r[1], r[2], r[3]);
		reached = WalkMap.get().count();
	}

	/** Places the last search reached, for a test to print. */
	private int reached;

	private int[] region() {
		Area a = golem.area();
		int minX = Math.min(a.minX, golem.postX()), minZ = Math.min(a.minZ, golem.postZ()), maxX = Math.max(a.maxX, golem.postX()), maxZ = Math.max(a.maxZ, golem.postZ());
		if (chestKnown) {
			minX = Math.min(minX, chestX);
			minZ = Math.min(minZ, chestZ);
			maxX = Math.max(maxX, chestX);
			maxZ = Math.max(maxZ, chestZ);
		}
		// And wherever it is now, should it have been pushed out.
		int gx = MathHelper.floor(golem.x), gz = MathHelper.floor(golem.z);
		return new int[] {Math.min(minX - MARGIN, gx - 2), Math.min(minZ - MARGIN, gz - 2), Math.max(maxX + MARGIN, gx + 2), Math.max(maxZ + MARGIN, gz + 2)};
	}

	private void startWalk(int node) {
		int[] r = region();
		walk.start(WalkMap.get(), node, r[0], r[1], r[2], r[3]);
	}

	// Doing it.

	private boolean toWork() {
		if (phaseTicks % 20 == 0 && !job.stillWork(target)) {
			walk.stop();
			return to(Phase.DECIDE);
		}
		// Near enough on the way: no need to go further.
		if (golem.onGround && nearEnough() && mayStandAt(MathHelper.floor(golem.x), golem.feetY(), MathHelper.floor(golem.z), target)) {
			walk.stop();
			status = "working " + target;
				workingFrom();
			return to(Phase.WORKING);
		}
		// What moves is followed: a new place to stand when it's gone more than a couple of blocks from where it was.
		if (target.entity != null && phaseTicks % 10 == 0) {
			Target now = new Target(target.entity, target.rank);
			if (Math.abs(now.x - target.x) + Math.abs(now.z - target.z) + Math.abs(now.y - target.y) > 2) {
				walk.stop();
				target = now;
				search();
				int node = standFor(WalkMap.get(), target, Integer.MAX_VALUE);
				if (node < 0) {
					refuse(target, 200);
					return to(Phase.DECIDE);
				}
				targetClose = standClose;
				startWalk(node);
			}
		}
		Walk.Status s = walk.tick();
		if (s == Walk.Status.FAILED) {
			// Something that moves may be got to another way soon, and may be at it: a guard mustn't ignore a zombie for a minute.
			refuse(target, target.entity != null ? 200 : 1200);
			status = "couldn't get to " + target;
			return to(Phase.DECIDE);
		}
		if (s == Walk.Status.ARRIVED) {
			if (job.stillWork(target) && reaches(golem.reachX(), golem.reachY(), golem.reachZ(), target)) {
				status = "working " + target;
				workingFrom();
				return to(Phase.WORKING);
			}
			// Something that moves has moved on since the place was chosen: it looks again. A block it can't work from there is passed over for a while.
			if (target.entity == null) refuse(target, 200);
			return to(Phase.DECIDE);
		}
		return false;
	}

	private void workingFrom() {
		fromX = MathHelper.floor(golem.x);
		fromY = golem.feetY();
		fromZ = MathHelper.floor(golem.z);
	}

	private boolean working() {
		if (isItem(target)) {
			// Taken (or gone): the piece is over, with no rest, as picking something up is no stroke of work. Not got in time: left a minute.
			if (target.entity.dead || !job.stillWork(target)) return to(Phase.DECIDE);
			// Moved out of touch (rolled, fell): looked at again where it lies now.
			if (!touches(golem.reachX(), golem.reachY(), golem.reachZ(), target.entity.boundingBox)) return to(Phase.DECIDE);
			if (stepInto((ItemEntity) target.entity)) return false;
			refuse(target, 1200);
			return to(Phase.DECIDE);
		}
		golem.aimAt((float) target.centerX(), (float) target.centerY(), (float) target.centerZ());
		if (golem.tool() == null || !job.stillWork(target) || !job.reaches(golem.reachX(), golem.reachY(), golem.reachZ(), target)) {
			job.abandon(target);
			walk.stop();
			return to(Phase.DECIDE);
		}
		double[] off = job.standOff(target);
		if (off != null) keepOff(off[0]);
		if (!job.work(target)) return false;
		walk.stop();
		// From the cell it began in: it can drift a few hundredths over a cell's edge as it works (a slide left from its last step), and reach goes by the cell, so measured at the
		// end, a piece begun 1.7 off read as 2.62 in one leveller's test (2026-11-10).
		double from = job.distance(fromX + 0.5, fromY + GolemEntity.EYE, fromZ + 0.5, target);
		golem.furthest = Math.max(golem.furthest, from);
		if (from > GolemEntity.CLOSE) {
			golem.stretched++;
			StringBuilder by = new StringBuilder();
			for (Object o : world.collectEntitiesByClass(net.minecraft.entity.LivingEntity.class, golem.boundingBox.expand(2.5, 1.0, 2.5))) {
				if (o != golem) by.append(' ').append(o.getClass().getSimpleName()).append(String.format(" at %.1f %.1f %.1f", ((net.minecraft.entity.Entity) o).x, ((net.minecraft.entity.Entity) o).y, ((net.minecraft.entity.Entity) o).z));
			}
			golem.lastStretch = String.format("%s from %.2f, standing at %.1f %.1f %.1f, its stand %d %d %d (close %b), beside it:%s; it began in %d %d %d", target, from, golem.x, golem.boundingBox.minY, golem.z, walk.toX(), walk.toY(),
					walk.toZ(), targetClose, by.length() == 0 ? " nothing" : by, fromX, fromY, fromZ);
		}
		dropsAround = true;
		// The pause a player has between two, less the tick it takes to start the next.
		rest = job.restAfter() - 1;
		golem.setWorking(false);
		to(Phase.DECIDE);
		return false;
	}

	/**
	 * What its work dropped round where it stands, and anything else lying there, nearest first: from where it stands if that's in touch, else from the nearest place it can walk to that
	 * is, as the cell a thing lies in often can't be stood in (beside a pole, at a wall's foot); then stepped into. What it can't get to, or into, is left.
	 */
	private boolean gather() {
		if (golem.isFull() || phaseTicks > GATHER_TICKS) {
			if (!golem.isFull() && nearestItem() != null) golem.leftLying[4]++;
			walk.stop();
			return to(Phase.DECIDE);
		}
		ItemEntity item = nearestItem();
		if (item == null) {
			walk.stop();
			return to(Phase.DECIDE);
		}
		if (item.id != gatherItem) {
			walk.stop();
			gatherItem = item.id;
			if (!touches(golem.reachX(), golem.reachY(), golem.reachZ(), item.boundingBox)) {
				search();
				// Its body has to come level with the thing, from its cell or one beside it. A thing at rest on farmland or a slab sits low in the cell under the one it's in, as a
				// target's height reads it, so its feet may be a cell above that too; the touch says which will do.
				int node = nearestStand(WalkMap.get(), new Target(item, 0), 1, 1, 1, false, Integer.MAX_VALUE);
				if (node < 0) {
					passedItems.add(item.id);
					golem.leftLying[0]++;
					return false;
				}
				startWalk(node);
			}
		}
		if (walk.active()) {
			Walk.Status s = walk.tick();
			if (s == Walk.Status.GOING) return false;
			if (s == Walk.Status.FAILED) {
				passedItems.add(item.id);
				golem.leftLying[1]++;
				return false;
			}
		}
		if (!stepInto(item)) {
			// Out of touch of where it ended up (a walk's end a cell off or a level up, or the thing rolled): another place to stand for it, twice at most, before it's left. One that
			// fell away has none in touch and is left then. Left at once, a thing out of touch from the top of the hill it was taking was never come back for (2026-11-10).
			if (item.id != replanFor) {
				replanFor = item.id;
				replans = 0;
			}
			if (movedOff && replans++ < 2) {
				gatherItem = -1;
				return false;
			}
			passedItems.add(item.id);
			golem.leftLying[movedOff ? 2 : 3]++;
		}
		return false;
	}

	private ItemEntity nearestItem() {
		Box box = Box.createCached(gatherX - GATHER_RANGE, gatherY - GATHER_RANGE, gatherZ - GATHER_RANGE, gatherX + 1 + GATHER_RANGE, gatherY + 1 + GATHER_RANGE, gatherZ + 1 + GATHER_RANGE);
		ItemEntity best = null;
		double bestD = Double.MAX_VALUE;
		for (Object o : world.collectEntitiesByClass(ItemEntity.class, box)) {
			ItemEntity item = (ItemEntity) o;
			// Wherever it rolled: what lies by the work is the work's.
			if (!golem.wouldTake(item) || passedItems.contains(item.id) || !golem.hasRoomFor(item.stack)) continue;
			double d = golem.getSquaredDistance(item);
			if (d < bestD) {
				bestD = d;
				best = item;
			}
		}
		return best;
	}

	// Home: the chest, or with none its post.

	/** To the chest for something in it: home all the same (what it carries goes in while it's there), but a trip that gets nothing isn't made again for a while. */
	private boolean toChestFor(String why) {
		chestTrip = true;
		return goHome(why);
	}

	private boolean mayTrip() {
		return clock >= chestTripAt;
	}

	private boolean goHome(String why) {
		status = why;
		if (chest()) {
			if (!withinReachOfChest(GolemEntity.CLOSE + 0.5)) {
				search();
				int node = chestStand(WalkMap.get());
				if (node < 0) return cantGetHome();
				startWalk(node);
			}
		} else if (!atPost()) {
			search();
			int node = nodeNear(WalkMap.get(), golem.postX(), golem.postY(), golem.postZ());
			if (node < 0) return cantGetHome();
			startWalk(node);
		}
		to(Phase.HOME);
		return !walk.active();
	}

	private boolean home() {
		if (walk.active()) {
			Walk.Status s = walk.tick();
			if (s == Walk.Status.GOING) return false;
			if (s == Walk.Status.FAILED) return cantGetHome();
		}
		if (chest()) {
			if (!withinReachOfChest(GolemEntity.REACH)) return cantGetHome();
			ItemStorage storage = storage();
			boolean moved = deposit(storage);
			if (golem.tool() == null) takeSpare(storage);
			boolean got = mend(storage) | fetch(storage);
			if (got || moved) Sounds.play(world, chestX + 0.5, chestY + 0.5, chestZ + 0.5, "random.chestclosed", 0.3F, 0.9F);
			if (chestTrip && !got) chestTripAt = clock + CHEST_TRIP_AGAIN;
		} else {
			putDownLoad();
		}
		chestTrip = false;
		homeFails = 0;
		if (golem.tool() == null && !golem.equipSpare(job.kind())) return chest() ? waitFor(200, "no tool in the chest: waiting for one") : goToPost("no tool, and no chest");
		if (golem.isFull()) return waitFor(100, "the chest is full: waiting");
		return to(Phase.DECIDE);
	}

	/** Back to its post to wait there, looking again for work now and then. */
	private boolean goToPost(String why) {
		status = why;
		if (atPost()) return waitFor(job.lookEvery(), why);
		search();
		int node = nodeNear(WalkMap.get(), golem.postX(), golem.postY(), golem.postZ());
		if (node < 0) return waitFor(job.lookEvery(), why + ", and it can't get to its post");
		startWalk(node);
		to(Phase.TO_POST);
		return false;
	}

	/** On its way it looks about as it would at its post: a guard coming back from the edge of its area mustn't walk past what turns up. */
	private boolean toPost() {
		if (phaseTicks % job.lookEvery() == 0 && golem.tool() != null && !golem.isFull() && choose() != null) {
			walk.stop();
			return to(Phase.DECIDE);
		}
		if (walk.tick() == Walk.Status.GOING) return false;
		return waitFor(job.lookEvery(), status);
	}

	/**
	 * Out of work, a guard walks its rounds: a stroll to somewhere it can safely walk in its area, up to {@link #STROLL} off as beta's mobs wander, then a pause, looking for work
	 * as often as it would at its post, on the way and standing. Its chest calls it as at any other time (to mend, for arrows, to put things down), and with nowhere to stroll it
	 * stands where it is. Where it can walk bounds it, so one made on a tower top stays up there.
	 */
	private boolean walkRounds() {
		if (clock < pauseEnds) return waitFor(Math.min(job.lookEvery(), pauseEnds - clock), "out of work: on its rounds, standing a while");
		search();
		int node = strollTo(WalkMap.get());
		if (node < 0) {
			pauseEnds = clock + PAUSE;
			return waitFor(job.lookEvery(), "out of work: nowhere on its rounds to walk to");
		}
		status = "out of work: walking its rounds";
		startWalk(node);
		return to(Phase.ROUNDS);
	}

	private boolean rounds() {
		if (phaseTicks % job.lookEvery() == 0 && golem.tool() != null && !golem.isFull() && choose() != null) {
			walk.stop();
			return to(Phase.DECIDE);
		}
		if (walk.tick() == Walk.Status.GOING) return false;
		pauseEnds = clock + PAUSE + world.random.nextInt(PAUSE_SPREAD + 1);
		return waitFor(job.lookEvery(), "out of work: on its rounds, standing a while");
	}

	/** Somewhere to stroll to: a place it can walk to in its area, up to {@link #STROLL} off each way, at random, as beta's mobs pick theirs; else anywhere in its area. -1 if none. */
	private int strollTo(WalkMap map) {
		Area a = golem.area();
		int gx = MathHelper.floor(golem.x), gz = MathHelper.floor(golem.z);
		java.util.Random random = world.random;
		for (int i = 0; i < 10; i++) {
			int x = gx + random.nextInt(2 * STROLL + 1) - STROLL, z = gz + random.nextInt(2 * STROLL + 1) - STROLL;
			int node = strollNode(map, a, x, z);
			if (node >= 0) return node;
		}
		for (int i = 0; i < 10; i++) {
			int node = strollNode(map, a, a.minX + random.nextInt(a.maxX - a.minX + 1), a.minZ + random.nextInt(a.maxZ - a.minZ + 1));
			if (node >= 0) return node;
		}
		return -1;
	}

	/** A place in its area at this x and z it can walk to and isn't standing in, at the height nearest its own. */
	private int strollNode(WalkMap map, Area a, int x, int z) {
		if (!a.contains(x, z)) return -1;
		int feet = golem.feetY(), best = -1;
		for (int dy = 0; dy <= 6 && best < 0; dy++) {
			int node = map.node(x, feet + dy, z);
			if (node < 0) node = map.node(x, feet - dy, z);
			if (node >= 0 && map.steps(node) > 0) best = node;
		}
		return best;
	}

	/**
	 * At work on something nearer than it likes ({@link Job#standOff}): it steps back, to the place within {@link #BACK_STEPS} steps that's furthest from it, if that gains at least a block
	 * and a half, and looks again each second on the way; working on all the while, as the loop runs {@code work} after this. Nowhere to step back to (a wall behind it, a drop, a tower top):
	 * it stands its ground and works, and looks again in a second.
	 */
	private void keepOff(double nearest) {
		if (standGround) return;
		if (walk.active()) {
			if (walk.tick() == Walk.Status.GOING && clock - backedAt < BACK_AGAIN) return;
			walk.stop();
		}
		double tx = target.centerX(), tz = target.centerZ();
		double now = Math.sqrt((golem.x - tx) * (golem.x - tx) + (golem.z - tz) * (golem.z - tz));
		if (now >= nearest || clock < corneredUntil) return;
		search();
		WalkMap map = WalkMap.get();
		int best = -1;
		double furthest = now + 1.5;
		for (int node = 0; node < map.count(); node++) {
			int steps = map.steps(node);
			if (steps == 0 || steps > BACK_STEPS) continue;
			double dx = map.x(node) + 0.5 - tx, dz = map.z(node) + 0.5 - tz, d = Math.sqrt(dx * dx + dz * dz);
			if (d > furthest) {
				furthest = d;
				best = node;
			}
		}
		if (best < 0) {
			corneredUntil = clock + BACK_AGAIN;
			return;
		}
		backedAt = clock;
		backedOff++;
		startWalk(best);
	}

	/** The node nearest a cell, within two blocks of it. */
	private static int nodeNear(WalkMap map, int x, int y, int z) {
		int best = -1;
		double bestD = Double.MAX_VALUE;
		for (int dy = -2; dy <= 2; dy++) {
			for (int dx = -2; dx <= 2; dx++) {
				for (int dz = -2; dz <= 2; dz++) {
					int node = map.node(x + dx, y + dy, z + dz);
					double d = dx * dx + dy * dy + dz * dz;
					if (node < 0 || d >= bestD) continue;
					bestD = d;
					best = node;
				}
			}
		}
		return best;
	}

	/** No way to its chest. A few tries, then what it carries goes down where it is, so it can get on. */
	private boolean cantGetHome() {
		if (chestTrip) {
			// Only for something in the chest: it gets on with its work, and tries again in a while.
			chestTrip = false;
			chestTripAt = clock + CHEST_TRIP_AGAIN;
			walk.stop();
			return to(Phase.DECIDE);
		}
		if (++homeFails >= 3) {
			putDownLoad();
			homeFails = 0;
		}
		return waitFor(100, "can't get to its chest");
	}

	private boolean atPost() {
		double dx = golem.postX() + 0.5 - golem.x, dz = golem.postZ() + 0.5 - golem.z;
		return dx * dx + dz * dz < 2.25 && Math.abs(golem.feetY() - golem.postY()) <= 1;
	}

	// The chest.

	/** A chest within range of its post, looked for now and then, nearest the post first. */
	boolean chest() {
		if (chestKnown && world.isPosLoaded(chestX, chestY, chestZ) && Block.BLOCKS[world.getBlockId(chestX, chestY, chestZ)] instanceof ChestBlock) return true;
		chestKnown = false;
		if (clock - chestLooked < 200 && chestLooked > -10000) return false;
		chestLooked = clock;
		int px = golem.postX(), py = golem.postY(), pz = golem.postZ();
		double best = Double.MAX_VALUE;
		for (int x = px - CHEST_RANGE; x <= px + CHEST_RANGE; x++) {
			for (int z = pz - CHEST_RANGE; z <= pz + CHEST_RANGE; z++) {
				if (!world.isPosLoaded(x, 64, z)) continue;
				for (int y = Math.max(1, py - CHEST_RANGE); y <= Math.min(126, py + CHEST_RANGE); y++) {
					if (!(Block.BLOCKS[world.getBlockId(x, y, z)] instanceof ChestBlock)) continue;
					double dx = x - px, dy = y - py, dz = z - pz, d = dx * dx + dy * dy + dz * dz;
					if (d >= best) continue;
					best = d;
					chestX = x;
					chestY = y;
					chestZ = z;
					chestKnown = true;
				}
			}
		}
		return chestKnown;
	}

	/** Somewhere to stand at the chest: as for work, the nearest place it can walk up close to it (a chest is a big target, so half a block more), else the nearest in reach. */
	private int chestStand(WalkMap map) {
		int close = chestStand(map, CLOSE_ACROSS, CLOSE_BELOW, CLOSE_ABOVE, GolemEntity.CLOSE + 0.5);
		return close >= 0 ? close : chestStand(map, CLOSE_ACROSS + 2, CLOSE_BELOW + 2, CLOSE_ABOVE + 2, GolemEntity.REACH);
	}

	private int chestStand(WalkMap map, int across, int below, int above, double reach) {
		int best = -1, under = Integer.MAX_VALUE;
		for (int y = chestY - below; y <= chestY + above; y++) {
			for (int x = chestX - across; x <= chestX + across; x++) {
				for (int z = chestZ - across; z <= chestZ + across; z++) {
					int node = map.node(x, y, z);
					if (node < 0 || map.steps(node) >= under || !reachesChest(x + 0.5, y + GolemEntity.EYE, z + 0.5, reach)) continue;
					best = node;
					under = map.steps(node);
				}
			}
		}
		return best;
	}

	private boolean withinReachOfChest(double reach) {
		return chestKnown && reachesChest(golem.reachX(), golem.reachY(), golem.reachZ(), reach);
	}

	private boolean reachesChest(double ex, double ey, double ez, double reach) {
		double nx = Math.max(chestX, Math.min(chestX + 1, ex)), ny = Math.max(chestY, Math.min(chestY + 1, ey)), nz = Math.max(chestZ, Math.min(chestZ + 1, ez));
		double dx = nx - ex, dy = ny - ey, dz = nz - ez;
		return dx * dx + dy * dy + dz * dz <= reach * reach && job.seesBlock(ex, ey, ez, chestX, chestY, chestZ);
	}

	private ItemStorage storage() {
		return Lookups.find(ItemStorage.TYPE, world, chestX, chestY, chestZ, Lookups.NO_SIDE);
	}

	/** Whether its chest, wherever it is now, holds anything like this: read from afar, as anyone knows what's in their own chest. */
	private boolean chestHolds(Predicate<ItemStack> like) {
		if (!chest()) return false;
		ItemStorage storage = storage();
		for (int slot = 0; storage != null && slot < storage.size(); slot++) {
			ItemStack there = storage.peek(slot);
			if (there != null && there.count > 0 && like.test(there)) return true;
		}
		return false;
	}

	/** Everything into the chest but what its work waiting could use. True if anything went in. */
	private boolean deposit(ItemStorage storage) {
		if (storage == null) return false;
		boolean any = false;
		Map<Long, Integer> kept = new HashMap<>();
		for (int i = 0; i < GolemEntity.SLOTS; i++) {
			ItemStack stack = golem.slot(i);
			if (stack == null) continue;
			int keep = keep(stack, kept);
			if (keep >= stack.count) continue;
			ItemStack put = Stacks.copy(stack);
			put.count = stack.count - keep;
			ItemStack left = storage.insert(put, false);
			int back = keep + (left == null ? 0 : left.count);
			if (back != stack.count) any = true;
			stack.count = back;
			golem.setSlot(i, stack);
		}
		return any;
	}

	/** An ingot at a time from the chest while a whole ingot's worth is gone, each mending it as its owner's would. True if it took any. */
	private boolean mend(ItemStorage storage) {
		boolean any = false;
		for (int slot = 0; storage != null && slot < storage.size() && golem.wantsMending(); ) {
			ItemStack there = storage.peek(slot);
			if (there == null || there.itemId != Item.IRON_INGOT.id || storage.extract(slot, 1, false) == null) {
				slot++;
				continue;
			}
			golem.mend();
			any = true;
		}
		return any;
	}

	/** What its work waiting could use and it's short of, from the chest: its own put away earlier, or what a player left there for it. True if it took any. */
	private boolean fetch(ItemStorage storage) {
		boolean any = false;
		for (int slot = 0; storage != null && slot < storage.size(); slot++) {
			ItemStack there = storage.peek(slot);
			if (there == null || there.count <= 0) continue;
			int want = Math.min(shortOf(there), there.count);
			if (want <= 0 || !golem.hasRoomFor(there)) continue;
			ItemStack taken = storage.extract(slot, want, false);
			if (taken == null) continue;
			int count = taken.count;
			golem.store(taken);
			if (taken.count > 0) storage.insert(taken, false);
			any |= taken.count < count;
		}
		return any;
	}

	/** How many more like this its work waiting could use than it carries. */
	private int shortOf(ItemStack stack) {
		int wanted = job.wants(stack);
		if (wanted <= 0) return 0;
		for (int i = 0; i < GolemEntity.SLOTS; i++) {
			ItemStack s = golem.slot(i);
			if (s != null && Stacks.canMerge(s, stack)) wanted -= s.count;
		}
		return wanted;
	}

	/** With no chest, or no way to it: everything but what its work waiting could use, down where it stands. */
	private void putDownLoad() {
		Map<Long, Integer> kept = new HashMap<>();
		for (int i = 0; i < GolemEntity.SLOTS; i++) {
			ItemStack stack = golem.slot(i);
			if (stack == null) continue;
			int keep = keep(stack, kept);
			if (keep >= stack.count) continue;
			ItemStack put = Stacks.copy(stack);
			put.count = stack.count - keep;
			golem.putDown(put);
			stack.count = keep;
			golem.setSlot(i, stack);
		}
	}

	/** Carries something it would put away: anything past what its work waiting could use. */
	private boolean hasLoad() {
		Map<Long, Integer> kept = new HashMap<>();
		for (int i = 0; i < GolemEntity.SLOTS; i++) {
			ItemStack stack = golem.slot(i);
			if (stack != null && keep(stack, kept) < stack.count) return true;
		}
		return false;
	}

	/** How many of this stack it keeps, counting what earlier slots kept of the same item. */
	private int keep(ItemStack stack, Map<Long, Integer> kept) {
		long key = (long) stack.itemId << 32 | stack.getDamage() & 0xFFFFFFFFL;
		int had = kept.getOrDefault(key, 0), n = Math.max(0, Math.min(stack.count, job.wants(stack) - had));
		kept.put(key, had + n);
		return n;
	}

	/** A tool of its job's kind from the chest, any tier. */
	private void takeSpare(ItemStorage storage) {
		if (storage == null) return;
		for (int slot = 0; slot < storage.size(); slot++) {
			ItemStack there = storage.peek(slot);
			if (there == null || !job.kind().equals(GolemEntity.toolClass(there))) continue;
			ItemStack taken = storage.extract(slot, 1, false);
			if (taken == null) continue;
			golem.setTool(taken);
			Sounds.play(world, golem.x, golem.y, golem.z, GolemEntity.CLANK, 0.3F, 1.3F);
			return;
		}
	}

	// Giving up on things for a while.

	private boolean refused(long key) {
		Integer until = refused.get(key);
		if (until == null) return false;
		if (until > clock) return true;
		refused.remove(key);
		return false;
	}

	private void refuse(Target t, int ticks) {
		if (refused.size() > 512) {
			for (Iterator<Integer> i = refused.values().iterator(); i.hasNext(); ) if (i.next() <= clock) i.remove();
		}
		refused.put(t.key(), clock + ticks);
	}

	String describe() {
		return phase + " " + status + (walk.active() ? " (walking to " + walk.toX() + " " + walk.toY() + " " + walk.toZ() + ")" : "") + " " + job.describe() + " [at " + MathHelper.floor(golem.x) + " " + golem.feetY() + " "
				+ MathHelper.floor(golem.z) + ", its last search reached " + reached + " places]";
	}
}
