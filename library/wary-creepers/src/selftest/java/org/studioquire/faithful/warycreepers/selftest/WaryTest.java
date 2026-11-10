package org.studioquire.faithful.warycreepers.selftest;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.passive.PigEntity;
import org.studioquire.faithful.harness.test.WorldTest;
import org.studioquire.faithful.warycreepers.Lanterns;
import org.studioquire.oldfaithful.core.api.Creepers;

/**
 * Where the world runs, all within one tick. First what a creeper sees: a lantern's face from each side for each way it can face (the face found from
 * what beta draws), its reach, glass, a plain pumpkin, and a lantern set in a wall seen along it. Then creepers walking: a rig of seven corridors over the
 * player's head, one wide and two high, each creeper heading for a pig penned behind glass at the far end, which it can't see and so never swells at. The
 * creepers and pigs are never put in the world: each creeper is ticked by hand as the world ticks one, so nothing else moves them, the test server's
 * Peaceful doesn't take them (the difficulty is raised for the moment) and no fuse can run its course.
 *
 * <p>Corridors run along x at even z from -6 to 6, walls at odd z. Lanterns: in the wall at z -5 facing +z (seen from the corridor at -4, from behind at
 * -6); in the wall at -3 facing +z (the swelling case at -2); in the corridor at 2 facing -z, at the wall (hidden from the corridor at 0); in the wall at 5
 * facing -z (the reach case at 4); in the outer wall at 7 facing -z, near the corridor's closed end (the cornered case at 6).
 */
public final class WaryTest extends WorldTest {
	/** The rig's floor, over the player's head; its corridors are the two cells above, its roof the one above those. */
	private static final int RIG = 3, WALK = RIG + 1;
	/** Beta's sides 2 to 5: north (-z), south (+z), west (-x), east (+x). */
	private static final int[] SIDE_X = {0, 0, 0, 0, -1, 1}, SIDE_Z = {0, 0, -1, 1, 0, 0};

	@Override
	protected void script() {
		then(() -> {
			int was = world.difficulty;
			world.difficulty = 2;
			try {
				sight();
				build();
				front();
				behind();
				walled();
				reach();
				cornered();
				swelling();
			} finally {
				world.difficulty = was;
				clear();
			}
		});
	}

	private CreeperEntity creeper(double cx, int dy, double cz, float yaw, Entity target) {
		CreeperEntity c = new CreeperEntity(world);
		c.setPositionAndAngles(x + cx, y + dy, z + cz, yaw, 0.0F);
		c.setTarget(target);
		return c;
	}

	/** In its pen at the far end of a corridor, behind the glass. */
	private PigEntity penned(int dz) {
		PigEntity pig = new PigEntity(world);
		pig.setPositionAndAngles(x + 7.5, y + WALK, z + dz + 0.5, 0.0F, 0.0F);
		return pig;
	}

	/** The side beta draws the lit face on for this meta: the one of the four whose texture is unlike the other three's. */
	private static int faceSide(int meta) {
		for (int s = 2; s <= 5; s++) {
			int same = 0;
			for (int o = 2; o <= 5; o++) if (Block.JACK_O_LANTERN.getTexture(o, meta) == Block.JACK_O_LANTERN.getTexture(s, meta)) same++;
			if (same == 1) return s;
		}
		return -1;
	}

	private void sight() {
		CreeperEntity c = creeper(0.5, 0, 0.5, 0.0F, null);
		double eye = c.getEyeHeight();
		StringBuilder wrong = new StringBuilder();
		for (int meta = 0; meta < 4; meta++) {
			set(-5, 0, 0, Block.JACK_O_LANTERN, meta);
			int face = faceSide(meta);
			for (int side = 2; side <= 5; side++) {
				boolean seen = Lanterns.seesFace(world, x - 4.5 + 3 * SIDE_X[side], y + eye, z + 0.5 + 3 * SIDE_Z[side], x - 5, y, z);
				if (seen != (side == face)) wrong.append(" meta ").append(meta).append(" side ").append(side).append(seen ? " seen" : " unseen");
			}
		}
		check("a creeper sees a lantern's face from in front, not from behind or beside, whichever way it faces, by the side beta draws it on" + wrong, wrong.length() == 0);

		// Facing east (+x), seen from in front at eye height.
		set(-5, 0, 0, Block.JACK_O_LANTERN, 3);
		c.setPositionAndAngles(x - 4.5 + 3, y, z + 0.5, 0.0F, 0.0F);
		check("three blocks in front of it, it frightens", Lanterns.nearest(c) != null);
		set(-5, 0, 0, Block.PUMPKIN, 3);
		check("a plain pumpkin there doesn't", Lanterns.nearest(c) == null);
		set(-5, 0, 0, Block.JACK_O_LANTERN, 3);
		set(-3, 0, 0, Block.GLASS);
		set(-3, 1, 0, Block.GLASS);
		check("nor a lantern behind glass, which hides a player from a monster in beta too", Lanterns.nearest(c) == null);
		set(-3, 0, 0, null);
		set(-3, 1, 0, null);
		set(-5, 0, 0, null);

		set(-7, 0, 0, Block.JACK_O_LANTERN, 3);
		c.setPositionAndAngles(x - 6.5 + 7.4, y, z + 0.5, 0.0F, 0.0F);
		double at = Math.sqrt(c.getSquaredDistance(x - 6.5, y + 0.5 - eye, z + 0.5));
		check("about 7.5 blocks from its middle, it frightens (" + String.format("%.2f", at) + " from the eyes)", Lanterns.nearest(c) != null);
		c.setPositionAndAngles(x - 6.5 + 8.6, y, z + 0.5, 0.0F, 0.0F);
		check("8.6 blocks off, it doesn't", Lanterns.nearest(c) == null);

		// A lantern set in a wall facing -z, seen from a block out and five along: the face shows at a slant, and the wall beside it mustn't hide it.
		for (int dx = -8; dx <= 1; dx++) for (int dy = 0; dy <= 1; dy++) set(dx, dy, 3, Block.STONE);
		set(-4, 0, 3, Block.JACK_O_LANTERN, 2);
		boolean slant = Lanterns.seesFace(world, x + 1.5, y + eye, z + 2.3, x - 4, y, z + 3);
		c.setPositionAndAngles(x + 1.5, y, z + 2.3, 0.0F, 0.0F);
		check("a lantern set in a wall is seen at a slant along it, from 0.7 blocks out and 5 along", slant && Lanterns.nearest(c) != null);
		for (int dx = -8; dx <= 1; dx++) for (int dy = 0; dy <= 1; dy++) set(dx, dy, 3, null);

		c.setPositionAndAngles(x + 0.5, y, z + 0.5, 0.0F, 0.0F);
		long t0 = System.nanoTime();
		for (int i = 0; i < 2000; i++) Lanterns.nearest(c);
		note("a look round with no lantern near: " + String.format("%.1f", (System.nanoTime() - t0) / 2000 / 1000.0) + " us");
	}

	private void build() {
		for (int dx = -8; dx <= 8; dx++) {
			for (int dz = -7; dz <= 7; dz++) {
				set(dx, RIG, dz, Block.STONE);
				set(dx, RIG + 3, dz, Block.STONE);
				boolean wall = dz % 2 != 0 || Math.abs(dx) == 8;
				for (int dy = WALK; dy <= WALK + 1; dy++) set(dx, dy, dz, wall ? Block.STONE : null);
			}
		}
		for (int dz = -6; dz <= 6; dz += 2) for (int dy = WALK; dy <= WALK + 1; dy++) set(6, dy, dz, Block.GLASS);
		set(3, WALK, -5, Block.JACK_O_LANTERN, 0);
		set(3, WALK, -3, Block.JACK_O_LANTERN, 0);
		set(3, WALK, 2, Block.JACK_O_LANTERN, 2);
		set(5, WALK, 5, Block.JACK_O_LANTERN, 2);
		set(-4, WALK, 7, Block.JACK_O_LANTERN, 2);
	}

	private void clear() {
		for (int dx = -8; dx <= 8; dx++) for (int dz = -8; dz <= 8; dz++) for (int dy = 0; dy <= RIG + 3; dy++) set(dx, dy, dz, null);
	}

	/** A creeper's walk, tick by tick, against a lantern's middle. */
	private final class Walk {
		final CreeperEntity creeper;
		final double lx, ly, lz;
		final double start, startX;
		final List<Double> distance = new ArrayList<>();
		final List<Double> xs = new ArrayList<>();
		int firstFlight = -1, litTicks;
		double firstFlightAt;
		boolean fled;

		Walk(int dz, int startDx, int ldx, int ldz) {
			creeper = creeper(startDx + 0.5, WALK, dz + 0.5, -90.0F, penned(dz));
			lx = x + ldx + 0.5;
			ly = y + WALK + 0.5;
			lz = z + ldz + 0.5;
			start = distance();
			startX = creeper.x;
		}

		double distance() {
			double dx = creeper.x - lx, dy = creeper.y + creeper.getEyeHeight() - ly, dz = creeper.z - lz;
			return Math.sqrt(dx * dx + dy * dy + dz * dz);
		}

		/** Its fuse burning: beta's creeper keeps that in its data tracker's 16, 1 lit and -1 going out. */
		boolean lit() {
			return creeper.getDataTracker().getByte(16) > 0;
		}

		/** The world's tick for it; stops at once if its fuse burns on, so nothing here ever goes off. */
		Walk tick(int ticks) {
			for (int t = 0; t < ticks; t++) {
				creeper.tick();
				distance.add(distance());
				xs.add(creeper.x);
				if (Creepers.fleeing(creeper)) {
					fled = true;
					if (firstFlight < 0) {
						firstFlight = distance.size();
						firstFlightAt = distance();
					}
				}
				litTicks = lit() ? litTicks + 1 : 0;
				if (litTicks > 5) break;
			}
			return this;
		}

		double nearest(int from) {
			double n = Double.MAX_VALUE;
			for (int i = from; i < distance.size(); i++) n = Math.min(n, distance.get(i));
			return n;
		}

		double furthestX() {
			double f = -Double.MAX_VALUE;
			for (double v : xs) f = Math.max(f, v);
			return f;
		}

		String about() {
			return String.format("from %.1f blocks, nearest %.1f, furthest x %+.1f", start, nearest(0), furthestX() - x);
		}
	}

	private void front() {
		Walk w = new Walk(-4, -1, 3, -5).tick(400);
		double gone = 0;
		for (int i = 0; i < Math.min(100, w.distance.size()); i++) gone = Math.max(gone, w.distance.get(i));
		check("a creeper in front of a lantern's face turns away at once: running " + w.firstFlight + " ticks in", w.firstFlight > 0 && w.firstFlight <= 8);
		check(String.format("and gets away: as far as %.1f blocks from it in 5 seconds, from %.1f", gone, w.start), gone >= w.start + 3.0);
		check("never nearer it than it began, but for turning round (" + w.about() + ")", w.nearest(0) >= w.start - 1.0 && w.furthestX() < x + 3.0);
		int out = -1;
		for (int i = 0; i < w.distance.size(); i++) {
			if (w.distance.get(i) > Lanterns.REACH) {
				out = i;
				break;
			}
		}
		boolean back = false;
		double far = 0;
		if (out >= 0) {
			for (int i = out; i < w.distance.size(); i++) {
				far = Math.max(far, w.distance.get(i));
				if (w.distance.get(i) <= far - 1.5) back = true;
			}
		}
		check(String.format("once out of reach it comes back toward its pig, and keeps off: past 8 blocks at tick %d, nearest after %.1f", out, out < 0 ? 0 : w.nearest(out)),
				out >= 0 && back && w.nearest(out) >= 6.0);
		check("it never swells at a pig it can't see", w.litTicks == 0);
	}

	private void behind() {
		Walk w = new Walk(-6, -1, 3, -5).tick(250);
		check("a creeper behind a lantern isn't frightened and walks past it to its pig (" + w.about() + ")", w.firstFlight < 0 && w.furthestX() > x + 4.5);
	}

	private void walled() {
		Walk w = new Walk(0, -1, 3, 2).tick(250);
		check("nor one with a wall between it and the face (" + w.about() + ")", w.firstFlight < 0 && w.furthestX() > x + 4.5);
	}

	private void reach() {
		Walk w = new Walk(4, -7, 5, 5).tick(300);
		check(String.format("a creeper walking toward a lantern's face turns at its reach: at %.2f blocks, and never nearer than %.1f", w.firstFlightAt, w.nearest(0)),
				w.firstFlight > 0 && w.firstFlightAt <= Lanterns.REACH && w.firstFlightAt >= Lanterns.REACH - 1.0 && w.nearest(0) >= 6.0);
	}

	private void cornered() {
		Walk w = new Walk(6, -7, -4, 7).tick(120);
		double strayed = 0;
		for (double v : w.xs) strayed = Math.max(strayed, Math.abs(v - w.startX));
		check(String.format("a creeper with nowhere to run holds still rather than pass the lantern: %.2f blocks at most from where it was", strayed),
				w.fled && strayed <= 1.0);
	}

	private void swelling() {
		Walk w = new Walk(-2, -1, 3, -3).tick(10);
		boolean running = Creepers.fleeing(w.creeper);
		PigEntity pig = (PigEntity) w.creeper.getTarget();
		pig.setPositionAndAngles(w.creeper.x - 2.0, w.creeper.y, w.creeper.z, 0.0F, 0.0F);
		int lit = -1;
		for (int t = 1; t <= 5 && lit < 0; t++) {
			w.tick(1);
			if (w.lit()) lit = t;
		}
		w.tick(1);
		check("a running creeper whose target comes within 3 blocks still swells, " + lit + (lit == 1 ? " tick" : " ticks") + " on, and stops running", running && lit > 0 && w.lit() && !Creepers.fleeing(w.creeper));
		pig.setPositionAndAngles(x + 7.5, y + WALK, z - 2 + 0.5, 0.0F, 0.0F);
		int again = -1;
		for (int t = 1; t <= 40 && again < 0; t++) {
			w.tick(1);
			if (!w.lit() && Creepers.fleeing(w.creeper)) again = t;
		}
		check("and once the target is out of reach its fuse goes out and the lantern frightens it again, " + again + " ticks on", again > 0);
	}
}
