package org.studioquire.faithful.buriedtowers;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.world.World;
import org.studioquire.faithful.buriedtowers.plan.Blocks;
import org.studioquire.faithful.buriedtowers.plan.Floor;
import org.studioquire.faithful.buriedtowers.plan.PlanDump;
import org.studioquire.faithful.buriedtowers.plan.Tower;

/** For other mods: where towers are and what's in them. It works out from the seed and the land, and generates nothing, so a tower it names may not exist yet: it will when the chunks are made. */
public final class Towers {
	private Towers() {
	}

	/** A tower, as far as anyone outside needs to know. */
	public static final class Found {
		public final int x, z, groundY;
		public final String climate, purpose;
		public final List<FoundFloor> floors;
		/** Just outside the gate, on the land: stand at {@code groundY} or a little above. */
		public final int gateX, gateZ;

		Found(Tower t) {
			x = t.cx;
			z = t.cz;
			groundY = t.groundY;
			climate = t.climate.name().toLowerCase();
			purpose = t.purpose.name().toLowerCase();
			gateX = t.gateX;
			gateZ = t.gateZ;
			List<FoundFloor> list = new ArrayList<>();
			for (Floor f : t.floors) list.add(new FoundFloor(f));
			floors = Collections.unmodifiableList(list);
		}
	}

	/** One floor: a place to stand on it, what it is, and how you get from it to the next one down. */
	public static final class FoundFloor {
		public final int index;
		public final String kind;
		public final int safeX, safeY, safeZ;
		/** How you go down from here; null for the last. */
		public final String wayDown;
		/** Where that starts, at the height of {@link #safeY}. */
		public final int wayX, wayZ;

		FoundFloor(Floor f) {
			index = f.index;
			kind = f.kindName();
			safeX = f.safeX;
			safeY = f.safeY;
			safeZ = f.safeZ;
			wayDown = f.out == null ? null : f.out.kind;
			wayX = f.out == null ? f.safeX : f.out.ux;
			wayZ = f.out == null ? f.safeZ : f.out.uz;
		}
	}

	/** The towers whose squares lie in a square of chunks. */
	public static List<Found> within(World world, int chunkX, int chunkZ, int size) {
		Keeper keeper = Keeper.of(world);
		List<Found> found = new ArrayList<>();
		int r0 = Math.floorDiv(chunkX, Keeper.REGION), r1 = Math.floorDiv(chunkX + size - 1, Keeper.REGION);
		int q0 = Math.floorDiv(chunkZ, Keeper.REGION), q1 = Math.floorDiv(chunkZ + size - 1, Keeper.REGION);
		for (int rx = r0; rx <= r1; rx++) {
			for (int rz = q0; rz <= q1; rz++) {
				Keeper.Site site = keeper.site(rx * Keeper.REGION, rz * Keeper.REGION);
				if (site != null) found.add(new Found(keeper.tower(site)));
			}
		}
		return found;
	}

	/** For the test driver: where the sites of a square of squares would stand with towers kept {@code margin} from their squares' edges, as x and z, without working out their towers. */
	public static List<int[]> places(World world, int rx0, int rz0, int squares, int margin) {
		Keeper keeper = Keeper.of(world);
		List<int[]> found = new ArrayList<>();
		for (int rx = rx0; rx < rx0 + squares; rx++) {
			for (int rz = rz0; rz < rz0 + squares; rz++) {
				Keeper.Site site = keeper.find(rx, rz, margin);
				if (site != null) found.add(new int[] {site.x, site.z});
			}
		}
		return found;
	}

	/** For the test driver: a tower's square as it stands in the world, in {@link PlanDump}'s form. */
	public static String dump(World world, int x, int z, OutputStream out) throws IOException {
		Keeper keeper = Keeper.of(world);
		Keeper.Site site = keeper.site(x >> 4, z >> 4);
		if (site == null) return "none";
		Tower t = keeper.tower(site);
		PlanDump.write(t, (cx, cy, cz) -> Blocks.cell(world.getBlockId(cx, cy, cz), world.getBlockMeta(cx, cy, cz)), out);
		return t.cx + " " + t.cz;
	}

	/** For the test driver: a tower's square as drawn, from its plan alone, in {@link PlanDump}'s form, so two builds can be shown to draw the same towers whatever else differs in their worlds. */
	public static String planDump(World world, int x, int z, OutputStream out) throws IOException {
		Keeper keeper = Keeper.of(world);
		Keeper.Site site = keeper.site(x >> 4, z >> 4);
		if (site == null) return "none";
		Tower t = keeper.tower(site);
		PlanDump.write(t, t.plan::get, out);
		return t.cx + " " + t.cz;
	}

	/** For the test driver: each site in a square of chunks against what stands there, a line each. */
	public static List<String> report(World world, int chunkX, int chunkZ, int size) {
		Keeper keeper = Keeper.of(world);
		List<String> lines = new ArrayList<>();
		for (int rx = Math.floorDiv(chunkX, Keeper.REGION); rx <= Math.floorDiv(chunkX + size - 1, Keeper.REGION); rx++) {
			for (int rz = Math.floorDiv(chunkZ, Keeper.REGION); rz <= Math.floorDiv(chunkZ + size - 1, Keeper.REGION); rz++) {
				Keeper.Site site = keeper.site(rx * Keeper.REGION, rz * Keeper.REGION);
				if (site != null) lines.add(keeper.report(site));
			}
		}
		return lines;
	}

	/** The nearest tower in this overworld to a place, looking some way out; null if there's none in reach. */
	public static Found nearest(World world, int x, int z) {
		Keeper keeper = Keeper.of(world);
		int rx = Math.floorDiv(x >> 4, Keeper.REGION), rz = Math.floorDiv(z >> 4, Keeper.REGION);
		Keeper.Site best = null;
		double bestFar = Double.MAX_VALUE;
		for (int ring = 0; ring <= 8; ring++) {
			// Anything further out than the best so far can't win once a whole ring beyond it has been looked at.
			if (best != null && (ring - 1) * Keeper.REGION * 16 > bestFar) break;
			for (int ax = -ring; ax <= ring; ax++) {
				for (int az = -ring; az <= ring; az++) {
					if (Math.max(Math.abs(ax), Math.abs(az)) != ring) continue;
					Keeper.Site site = keeper.site((rx + ax) * Keeper.REGION, (rz + az) * Keeper.REGION);
					if (site == null) continue;
					double far = Math.hypot(site.x - x, site.z - z);
					if (far < bestFar) {
						bestFar = far;
						best = site;
					}
				}
			}
		}
		return best == null ? null : new Found(keeper.tower(best));
	}
}
