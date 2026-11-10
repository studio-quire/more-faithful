package org.studioquire.oldfaithful.core.api;

import java.util.function.Consumer;

import net.minecraft.world.World;

/**
 * Notices, on the side that runs the world. In single player the "server" is the open save: it starts when a world is opened
 * and stops when it's closed, so one listener holds for both. Never on a client on a server.
 * <p>
 * A world's first chunks are loaded while it's being made, so {@link Chunks#onLoaded} can come before {@link #onWorldLoad}. The
 * Nether is a world of its own: on a server both are loaded together; in single player each trip through a portal unloads one and
 * loads the other, and the server doesn't stop.
 * <p>
 * Order at the start: world load (each world), then server start, then players join. At the end: players leave, server stop, the
 * game's last save, then world unload.
 */
public final class Lifecycle {
	private static final Listeners<Runnable> START = new Listeners<>("Lifecycle.onServerStart");
	private static final Listeners<Runnable> STOP = new Listeners<>("Lifecycle.onServerStop");
	private static final Listeners<Consumer<World>> LOAD = new Listeners<>("Lifecycle.onWorldLoad");
	private static final Listeners<Consumer<World>> SAVE = new Listeners<>("Lifecycle.onWorldSave");
	private static final Listeners<Consumer<World>> UNLOAD = new Listeners<>("Lifecycle.onWorldUnload");

	private Lifecycle() {
	}

	/** Notice. */
	public static void onServerStart(Runnable listener) {
		START.add(listener);
	}

	/**
	 * Notice. Before the last save, so what's written here is saved. Except that closing the game's window skips beta's last save, in
	 * single player, and that stays as it was.
	 */
	public static void onServerStop(Runnable listener) {
		STOP.add(listener);
	}

	/** Notice. */
	public static void onWorldLoad(Consumer<World> listener) {
		LOAD.add(listener);
	}

	/** Notice: each save of a world, autosaves too, before its chunks are written. */
	public static void onWorldSave(Consumer<World> listener) {
		SAVE.add(listener);
	}

	/** Notice. After the last save. */
	public static void onWorldUnload(Consumer<World> listener) {
		UNLOAD.add(listener);
	}

	/** The core's. */
	public static void serverStarted() {
		START.each(Runnable::run);
	}

	/** The core's. */
	public static void serverStopping() {
		STOP.each(Runnable::run);
	}

	/** The core's. */
	public static void worldLoaded(World world) {
		LOAD.each(l -> l.accept(world));
	}

	/** The core's. */
	public static void worldSaving(World world) {
		if (!world.isRemote) SAVE.each(l -> l.accept(world));
	}

	/** The core's. */
	public static void worldUnloaded(World world) {
		UNLOAD.each(l -> l.accept(world));
	}
}
