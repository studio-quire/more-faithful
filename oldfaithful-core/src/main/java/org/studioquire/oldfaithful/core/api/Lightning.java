package org.studioquire.oldfaithful.core.api;

import java.util.function.Consumer;

import net.minecraft.world.World;

/** Natural lightning, in the world's weather. Strikes a mod makes itself aren't heard. */
public final class Lightning {
	public static final class Strike extends Cancellable {
		public final World world;
		/** Where the weather chose. */
		public final int x, y, z;
		private int toX, toY, toZ;
		private long nearest = Long.MAX_VALUE;
		private String nearestMod;

		Strike(World world, int x, int y, int z) {
			this.world = world;
			this.x = x;
			this.y = y;
			this.z = z;
		}

		/** A proposal: strike here instead. */
		public void proposeMoveTo(int x, int y, int z) {
			long dx = x - this.x, dy = y - this.y, dz = z - this.z;
			long distance = dx * dx + dy * dy + dz * dz;
			String mod = STRIKE.speaking();
			if (distance < nearest || distance == nearest && mod.compareTo(nearestMod) < 0) {
				nearest = distance;
				nearestMod = mod;
				toX = x;
				toY = y;
				toZ = z;
			}
		}

		boolean moved() {
			return nearestMod != null;
		}
	}

	/** Where a strike landed. */
	public static final class Struck {
		public final World world;
		public final int x, y, z;

		Struck(World world, int x, int y, int z) {
			this.world = world;
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}

	private static final Listeners<Consumer<Strike>> STRIKE = new Listeners<>("Lightning.onStrike");
	private static final Listeners<Consumer<Struck>> STRUCK = new Listeners<>("Lightning.onStruck");

	private Lightning() {
	}

	/**
	 * Choice, before the strike exists, where the world runs: every listener hears it and may propose a place. The proposal
	 * nearest the original strike wins, and between equals the lower mod id. {@link Cancellable#cancel} beats any proposal: no
	 * strike at all, and no fire or thunder from it.
	 */
	public static void onStrike(Consumer<Strike> listener) {
		STRIKE.add(listener);
	}

	/** Notice, where the world runs: a strike has landed, here, after every proposal and with its fires lit. A rod that wants to know it was the one hit asks this. */
	public static void onStruck(Consumer<Struck> listener) {
		STRUCK.add(listener);
	}

	/** The core's. */
	public static void struck(World world, int x, int y, int z) {
		if (STRUCK.isEmpty() || world.isRemote) return;
		Struck event = new Struck(world, x, y, z);
		STRUCK.each(l -> l.accept(event));
	}

	/** The core's: where it strikes, or null for nowhere. */
	public static int[] strike(World world, int x, int y, int z) {
		if (STRIKE.isEmpty() || world.isRemote) return new int[] {x, y, z};
		Strike event = new Strike(world, x, y, z);
		STRIKE.each(l -> l.accept(event));
		if (event.cancelled()) return null;
		return event.moved() ? new int[] {event.toX, event.toY, event.toZ} : new int[] {x, y, z};
	}
}
