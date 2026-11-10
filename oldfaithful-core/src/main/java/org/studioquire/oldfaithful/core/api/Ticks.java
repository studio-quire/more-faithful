package org.studioquire.oldfaithful.core.api;

import java.util.function.Consumer;

import net.minecraft.world.World;

/** Notices, at the end of a tick. Each listener runs twenty times a second, so it keeps to what it must. */
public final class Ticks {
	private static final Listeners<Consumer<World>> WORLD = new Listeners<>("Ticks.onWorld");
	private static final Listeners<Runnable> SERVER = new Listeners<>("Ticks.onServer");
	private static final Listeners<Runnable> CLIENT = new Listeners<>("Ticks.onClient");

	private Ticks() {
	}

	/**
	 * Each tick of each running world, on the side that runs it. Never on a client on a server, and not while single player is paused. A server has two worlds and
	 * ticks both, so this runs twice a tick there, and the Nether has no players most of the time: don't keep "the world" from this, ask {@link Server}.
	 */
	public static void onWorld(Consumer<World> listener) {
		WORLD.add(listener);
	}

	/** Each tick of whatever runs the world: a server's, or single player's while a world is open and not paused. */
	public static void onServer(Runnable listener) {
		SERVER.add(listener);
	}

	/** Each tick of a client, in a world or not, on a server or not. Never on a dedicated server. {@code Client.minecraft()} has the game. */
	public static void onClient(Runnable listener) {
		CLIENT.add(listener);
	}

	/** The core's. */
	public static void world(World world) {
		if (!world.isRemote) WORLD.each(l -> l.accept(world));
	}

	/** The core's. */
	public static void server() {
		SERVER.each(Runnable::run);
	}

	/** The core's. */
	public static void client() {
		CLIENT.each(Runnable::run);
	}
}
