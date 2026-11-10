package org.studioquire.oldfaithful.core.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.NetworkHandler;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.studioquire.oldfaithful.core.api.Channel;
import org.studioquire.oldfaithful.core.impl.client.ClientNet;
import org.studioquire.oldfaithful.core.impl.server.ServerNet;

/** Channels, and what's common to the client's and the server's ends of them. */
public final class CoreNet {
	/** In the client's login packet's seed, which vanilla's server never reads. Matched exactly. "FAITH". */
	public static final long MAGIC = 0x4641495448000000L;

	/** One end of the wire: {@link ClientNet} or {@link ServerNet}, so neither's classes load on the other side. */
	public interface Side {
		void receive(NetworkHandler handler, ChannelPacket packet);

		boolean available(ChannelImpl channel, PlayerEntity player);

		void toServer(ChannelImpl channel, byte[] data);

		void toPlayer(ChannelImpl channel, PlayerEntity player, byte[] data);

		void toAll(ChannelImpl channel, byte[] data);

		/** Whether this is a dedicated server, where what's set must be sent. */
		default boolean runsServer() {
			return false;
		}

		/** The worlds running in this process, for {@code Server}. */
		List<World> worlds();

		/** The players in them, for {@code Server}. */
		List<PlayerEntity> players();

		/** On a server, who can see this entity. */
		default List<PlayerEntity> watching(Entity entity) {
			return Collections.emptyList();
		}

		/** On a server, who has this chunk. */
		default List<PlayerEntity> watching(Chunk chunk) {
			return Collections.emptyList();
		}
	}

	private static final Map<String, ChannelImpl> CHANNELS = new TreeMap<>();
	private static final List<ChannelImpl> BY_NUMBER = new ArrayList<>();
	private static Side side;

	private CoreNet() {
	}

	@SuppressWarnings("unchecked")
	static <T extends Throwable> T sneak(Throwable t) throws T {
		throw (T) t;
	}

	static void startSide(boolean client) {
		side = client ? new ClientNet() : new ServerNet();
	}

	public static Side side() {
		return side;
	}

	public static synchronized ChannelImpl channel(String name, int protocol) {
		if (BY_NUMBER.size() > 0) throw new IllegalStateException("The channel " + name + " came after the mods started");
		ChannelImpl c = new ChannelImpl(name, protocol);
		if (CHANNELS.putIfAbsent(name, c) != null) throw new IllegalStateException("The channel " + name + " is registered twice");
		return c;
	}

	/** Numbers go by name, from 1: 0 is the hello. */
	static synchronized void seal() {
		int n = 1;
		for (ChannelImpl c : CHANNELS.values()) {
			c.number = n++;
			BY_NUMBER.add(c);
		}
	}

	public static List<ChannelImpl> channels() {
		return BY_NUMBER;
	}

	public static ChannelImpl byNumber(int number) {
		return number >= 1 && number <= BY_NUMBER.size() ? BY_NUMBER.get(number - 1) : null;
	}

	public static ChannelImpl byName(String name) {
		return CHANNELS.get(name);
	}

	public static final class ChannelImpl implements Channel {
		private final String name;
		public final int protocol;
		public int number = -1;
		public boolean world;
		public int maxBytes = ChannelPacket.MAX_TO_CLIENT;
		public Receiver onServer;
		public Receiver onClient;

		ChannelImpl(String name, int protocol) {
			this.name = name;
			this.protocol = protocol;
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public Channel world() {
			world = true;
			return this;
		}

		@Override
		public Channel maxBytes(int bytes) {
			if (bytes <= 0 || bytes > ChannelPacket.MAX_TO_CLIENT) throw new IllegalArgumentException("The most a channel may carry is " + ChannelPacket.MAX_TO_CLIENT);
			maxBytes = bytes;
			return this;
		}

		@Override
		public Channel onServer(Receiver receiver) {
			onServer = receiver;
			return this;
		}

		@Override
		public Channel onClient(Receiver receiver) {
			onClient = receiver;
			return this;
		}

		@Override
		public boolean available() {
			return side.available(this, null);
		}

		@Override
		public boolean available(PlayerEntity player) {
			return side.available(this, player);
		}

		private byte[] bytes(Writer writer, int limit) {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			try {
				writer.write(new DataOutputStream(bytes));
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
			if (bytes.size() > Math.min(maxBytes, limit)) {
				throw new IllegalArgumentException("A message of " + bytes.size() + " bytes on " + name + ": more than it may carry");
			}
			return bytes.toByteArray();
		}

		@Override
		public void toServer(Writer writer) {
			side.toServer(this, bytes(writer, ChannelPacket.MAX_TO_SERVER));
		}

		@Override
		public void toPlayer(PlayerEntity player, Writer writer) {
			side.toPlayer(this, player, bytes(writer, ChannelPacket.MAX_TO_CLIENT));
		}

		@Override
		public void toAll(Writer writer) {
			side.toAll(this, bytes(writer, ChannelPacket.MAX_TO_CLIENT));
		}

		public void deliver(Receiver receiver, PlayerEntity player, byte[] data) {
			if (receiver == null) return;
			try {
				receiver.receive(player, new DataInputStream(new ByteArrayInputStream(data)));
			} catch (IOException e) {
				throw new IllegalStateException("A bad message on " + name + ": " + e, e);
			} catch (OutOfMemoryError | StackOverflowError e) {
				// A length believed, or a list that nests: what the peer sent is the fault, and one peer can't be let take down a server.
				System.out.println("[faithful-core] WARN The handler on " + name + " overran on a message: " + e);
				throw new IllegalStateException("A bad message on " + name + ": " + e);
			}
		}
	}
}
