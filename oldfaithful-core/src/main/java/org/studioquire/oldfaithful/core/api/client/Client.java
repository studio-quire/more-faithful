package org.studioquire.oldfaithful.core.api.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import org.studioquire.oldfaithful.core.api.Listeners;
import org.studioquire.oldfaithful.core.impl.client.ClientNet;

/** The client as a whole. {@code Lifecycle} is about the world running; this is about what this client is showing, on a server too. */
public final class Client {
	public interface WorldChange {
		void changed(World from, World to);
	}

	private static final Listeners<WorldChange> WORLD_CHANGED = new Listeners<>("Client.onWorldChanged");

	private Client() {
	}

	/** The game. There from {@code initClient} on; its world and player are null until one's entered. */
	public static Minecraft minecraft() {
		return ClientNet.minecraft;
	}

	/**
	 * Notice: this client's world is a different one. {@code from} null: entered, by opening a save or joining a server. {@code to} null: left.
	 * Neither: a change of dimension, which in beta is a new world object. A respawn in the same dimension isn't a change. After the switch, with
	 * {@code minecraft().world} and {@code .player} set.
	 */
	public static void onWorldChanged(WorldChange listener) {
		WORLD_CHANGED.add(listener);
	}

	/** The core's. */
	public static void worldChanged(World from, World to) {
		WORLD_CHANGED.each(l -> l.changed(from, to));
	}
}
