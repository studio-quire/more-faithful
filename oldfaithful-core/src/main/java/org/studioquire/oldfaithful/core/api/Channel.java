package org.studioquire.oldfaithful.core.api;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.entity.player.PlayerEntity;

/**
 * A mod's way to talk between client and server. Nothing crosses the wire to a peer without it. Handlers run on the game
 * thread; what they read came from the other side, so they check it. A handler's exception disconnects the client or
 * kicks the player. In single player a send arrives at the next tick, so code has one path.
 */
public interface Channel {
	interface Writer {
		void write(DataOutputStream out) throws IOException;
	}

	interface Receiver {
		/** {@code player}: who sent it, on a server; this game's player on a client. */
		void receive(PlayerEntity player, DataInputStream in) throws IOException;
	}

	String name();

	/** For anything about a block or chunk: sent in order with chunk data, or it overtakes its chunk and is lost. */
	Channel world();

	/** The most one message may hold. Defaults to the most allowed: 32 KB to a server, 256 KB to a client. */
	Channel maxBytes(int bytes);

	/** Called on the side that runs the world, with what a client sent. */
	Channel onServer(Receiver receiver);

	/** Called on the client, with what the side running the world sent. */
	Channel onClient(Receiver receiver);

	/** On a client: whether the server has this channel. */
	boolean available();

	/** On the side running the world: whether this player's client has it. */
	boolean available(PlayerEntity player);

	void toServer(Writer writer);

	void toPlayer(PlayerEntity player, Writer writer);

	void toAll(Writer writer);
}
