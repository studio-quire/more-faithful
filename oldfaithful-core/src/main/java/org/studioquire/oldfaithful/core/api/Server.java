package org.studioquire.oldfaithful.core.api;

import java.util.List;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.studioquire.oldfaithful.core.impl.CoreNet;

/**
 * What's running here, the same in single player and on a dedicated server: the worlds this process runs, and the players in them. Common code that needs "the world" or "everyone" asks here.
 * <p>
 * <b>On a client on a server, nothing runs here</b>: {@link #running} is false, {@link #worlds} and {@link #players} are empty and {@link #world} is null, because that client's world
 * is only a view. It has {@code minecraft.world} for what it sees, and the server has the truth. The same holds on a client at the title screen, and on a server before its worlds are made.
 * <p>
 * A dedicated server always runs the overworld and the Nether. Single player runs one world at a time, the one the player is in: {@code world(Worldgen.NETHER)} is null while they're in the
 * overworld, and each trip through a portal changes what's here ({@link Lifecycle}). So look each time rather than keeping a world, and have a null ready. The lists are copies, safe to change or to
 * walk while players leave. Call from the thread the world runs on: hooks, ticks and commands all are.
 */
public final class Server {
	private Server() {
	}

	/** Whether this process runs a world now. */
	public static boolean running() {
		return !worlds().isEmpty();
	}

	/** The worlds running here: on a server the overworld, then the Nether. */
	public static List<World> worlds() {
		return CoreNet.side().worlds();
	}

	/** The world of this dimension ({@link Worldgen#OVERWORLD}, {@link Worldgen#NETHER}) if it's running here, or null. */
	public static World world(int dimension) {
		for (World w : worlds()) {
			if (w.dimension.id == dimension) return w;
		}
		return null;
	}

	/** Everyone in the worlds running here: on a server all who've joined, in any dimension; in single player the one player. */
	public static List<PlayerEntity> players() {
		return CoreNet.side().players();
	}

	/** The player of this name, as vanilla writes it, or null. */
	public static PlayerEntity player(String name) {
		for (PlayerEntity p : players()) {
			if (p.name.equals(name)) return p;
		}
		return null;
	}
}
