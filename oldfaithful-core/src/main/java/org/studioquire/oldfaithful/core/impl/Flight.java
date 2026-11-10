package org.studioquire.oldfaithful.core.impl;

import net.minecraft.entity.ai.pathing.Path;

/** A creeper running from a fright (`Creepers`): the path it runs, or none while it has nowhere to run and holds still, and how long until it looks again. Never saved. */
public final class Flight {
	/** What every creeper has with the core (its mixin). */
	public interface Holder {
		Flight faithful$flight();

		void faithful$setFlight(Flight flight);
	}

	final Path path;
	public int ticks;

	public Flight(Path path, int ticks) {
		this.path = path;
		this.ticks = ticks;
	}

	/** The path it's walking, while there's any of it left. */
	public Path walking() {
		return path != null && !path.isFinished() ? path : null;
	}

	/** Holding still, with nowhere to run. */
	public boolean cornered() {
		return path == null;
	}
}
