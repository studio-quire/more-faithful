package org.studioquire.oldfaithful.core.api;

import java.util.Arrays;
import java.util.Random;
import java.util.function.Consumer;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNode;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.studioquire.oldfaithful.core.impl.Flight;
import org.studioquire.oldfaithful.core.mixin.CreeperEntityAccessor;
import org.studioquire.oldfaithful.core.mixin.PathAccessor;

/**
 * Creepers keeping away from things, where the world runs. A mod says what frightens a creeper ({@link #onFright}); how it turns away, where it goes,
 * when it comes back and what its fuse does are here, the same for every mod, so creepers frightened by different mods behave as one and two such mods
 * never steer one creeper two ways. With no listener, creepers are beta's.
 *
 * <p>Beta's monsters have no goals to fit a fear into: each tick {@code MobEntity.tickLiving} paths to the target, or now and then wanders, and walks
 * whatever path it has. So a frightened creeper is given a path of its own, away, and while it has one beta's asking for a path is answered with it, and it
 * doesn't turn to face its target, which in beta would steer it back. Its target stays its target, so once a flight is over and nothing frightens it, it
 * comes back; at the edge of what frightens it that makes it pace. With nowhere to run (every way away blocked, or leading nearer first) it holds still
 * and tries again a second later. Once its fuse is lit, beside a target it can see, it's beta's creeper until the fuse goes out: fleeing changes where it
 * goes, not whether it swells at arm's length, as with cats in later versions.
 *
 * <p>The way of fleeing is Wary Creepers' (Claude Opus 5.5, 2026-11-10), moved here so that any mod frightening creepers shares it.
 */
public final class Creepers {
	/** One creeper asking what frightens it, a few times a second. */
	public static final class Fright {
		public final CreeperEntity creeper;
		private double x, y, z, best = Double.MAX_VALUE;

		Fright(CreeperEntity creeper) {
			this.creeper = creeper;
		}

		/** Something frightening it, at this point: the middle of what it runs from. Of everything proposed, it runs from the nearest. */
		public void at(double x, double y, double z) {
			double d = creeper.getSquaredDistance(x, y, z);
			if (d < best) {
				best = d;
				this.x = x;
				this.y = y;
				this.z = z;
			}
		}

		/** A thing frightening it, at its position (its feet). */
		public void at(Entity thing) {
			at(thing.x, thing.y, thing.z);
		}

		boolean frightened() {
			return best != Double.MAX_VALUE;
		}
	}

	private static final Listeners<Consumer<Fright>> FRIGHT = new Listeners<>("Creepers.onFright");

	/** How often a calm creeper looks round, in its own ticks. */
	public static final int LOOK_EVERY = 4;
	/** The longest a flight goes on before it looks again, if it hasn't reached the end of its path: a 16-block run takes about 10 seconds. */
	public static final int FLIGHT_TICKS = 200;
	/** How long a creeper with nowhere to run holds still before it tries again. */
	public static final int CORNERED_TICKS = 20;
	/** How far off, in blocks, the places it might run to are picked: later versions' cats send a creeper up to 16. */
	private static final double RUN_MIN = 8, RUN_MAX = 16;
	/** Places picked; the furthest from the fright tried first, this many with beta's pathfinder. */
	private static final int SPOTS = 10, TRIES = 3;
	/** A flight ends at least this much further from the fright than the creeper is, in blocks, and comes no nearer than this less, for a step round something. */
	private static final double GAIN = 2.0, SLACK = 1.0;

	private Creepers() {
	}

	/**
	 * Choice, asked of each creeper every {@link #LOOK_EVERY} ticks where the world runs while it isn't already running or its fuse lit: what frightens it
	 * just now. A listener proposes what it frightens it with ({@link Fright#at}), deciding reach and sight for itself ({@code creeper.canSee} is beta's
	 * monster sight for a thing); of everything proposed the creeper runs from the nearest. It runs 8 to 16 blocks, by a way that never takes it nearer,
	 * at its usual pace, then looks again.
	 */
	public static void onFright(Consumer<Fright> listener) {
		FRIGHT.add(listener);
	}

	/** Whether this mob is a creeper running from something, or holding still with nowhere to run. Where the world runs. */
	public static boolean fleeing(Object mob) {
		return flight(mob) != null;
	}

	/** The core's: a creeper's flight, or null. Never set on a client's copy of a world on a server. */
	public static Flight flight(Object mob) {
		return mob instanceof Flight.Holder ? ((Flight.Holder) mob).faithful$flight() : null;
	}

	/** The core's: at the start of the creeper's own thinking, each tick, where the world runs. */
	public static void tick(CreeperEntity creeper) {
		if (FRIGHT.isEmpty()) return;
		Flight.Holder holder = (Flight.Holder) creeper;
		Flight flight = holder.faithful$flight();
		if (((CreeperEntityAccessor) creeper).faithful$fuseSpeed() > 0) {
			if (flight != null) {
				holder.faithful$setFlight(null);
				// What beta's creeper would be walking while it swells.
				Entity target = creeper.getTarget();
				creeper.setPath(target == null ? null : creeper.world.findPath(creeper, target, 16.0F));
			}
			return;
		}
		if (flight != null) {
			Path path = flight.walking();
			if (--flight.ticks > 0 && (path != null || flight.cornered())) {
				// Beta drops a path one tick in a hundred, idling a tick: this puts it back on.
				if (path != null) creeper.setPath(path);
				return;
			}
		} else if (creeper.age % LOOK_EVERY != 0) {
			return;
		}
		Fright fright = new Fright(creeper);
		FRIGHT.each(l -> l.accept(fright));
		if (!fright.frightened()) {
			if (flight != null) {
				holder.faithful$setFlight(null);
				creeper.setPath(null);
			}
			return;
		}
		Path path = escape(creeper, fright.x, fright.y, fright.z);
		holder.faithful$setFlight(new Flight(path, path == null ? CORNERED_TICKS : FLIGHT_TICKS));
		creeper.setPath(path);
	}

	/**
	 * A path away from the fright at {@code fx, fy, fz}, or null with nowhere to go. Places are picked as later versions' fleeing picks them: 8 to 16 blocks
	 * off, on the far side of the creeper from the fright (within a right angle of straight away), a few blocks up or down, and beta's pathfinder walks to the
	 * furthest it can.
	 */
	static Path escape(CreeperEntity creeper, double fx, double fy, double fz) {
		World world = creeper.world;
		Random random = world.random;
		double ax = creeper.x - fx, az = creeper.z - fz;
		double away = ax * ax + az * az < 1.0E-4 ? random.nextDouble() * 2 * Math.PI : Math.atan2(az, ax);
		double now = distance(fx, fy, fz, creeper.x, creeper.y, creeper.z);
		int[][] spots = new int[SPOTS][];
		for (int i = 0; i < SPOTS; i++) {
			double angle = away + (random.nextDouble() - 0.5) * Math.PI, run = RUN_MIN + random.nextDouble() * (RUN_MAX - RUN_MIN);
			spots[i] = new int[] {MathHelper.floor(creeper.x + Math.cos(angle) * run), MathHelper.floor(creeper.y) + random.nextInt(7) - 3,
					MathHelper.floor(creeper.z + Math.sin(angle) * run)};
		}
		Arrays.sort(spots, (a, b) -> Double.compare(distance(fx, fy, fz, b[0] + 0.5, b[1], b[2] + 0.5), distance(fx, fy, fz, a[0] + 0.5, a[1], a[2] + 0.5)));
		for (int i = 0; i < TRIES; i++) {
			// Beta's pathfinder keeps to cells within its range of the place, and the creeper is up to 16 (and 3 down) from it.
			Path path = world.findPath(creeper, spots[i][0], spots[i][1], spots[i][2], (float) RUN_MAX + 4.0F);
			if (path != null && leadsAway(path, fx, fy, fz, now)) return path;
		}
		return null;
	}

	/** Beta's pathfinder gives the way to the nearest cell it could reach, so where it ends and how it gets there are checked. */
	private static boolean leadsAway(Path path, double fx, double fy, double fz, double now) {
		PathNode[] nodes = ((PathAccessor) path).faithful$nodes();
		if (nodes.length == 0) return false;
		for (PathNode node : nodes) {
			if (distance(fx, fy, fz, node.x + 0.5, node.y, node.z + 0.5) < now - SLACK) return false;
		}
		PathNode end = nodes[nodes.length - 1];
		return distance(fx, fy, fz, end.x + 0.5, end.y, end.z + 0.5) >= now + GAIN;
	}

	private static double distance(double fx, double fy, double fz, double x, double y, double z) {
		double dx = x - fx, dy = y - fy, dz = z - fz;
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}
}
