package org.studioquire.oldfaithful.core.api;

/** A declared entity: how a server tracks it. Set these in {@code content}. */
public interface EntityEntry {
	String name();

	/** How near, in blocks, a player must be to be told about it, and the ticks between position updates. Default 160 and 3: a mob's. */
	EntityEntry tracking(int range, int rate);

	/** Velocity changes go out too, as for boats, carts and thrown things. */
	EntityEntry sendVelocity();
}
