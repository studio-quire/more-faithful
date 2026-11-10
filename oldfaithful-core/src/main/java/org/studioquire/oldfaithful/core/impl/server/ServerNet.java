package org.studioquire.oldfaithful.core.impl.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.Connection;
import net.minecraft.network.NetworkHandler;
import net.minecraft.network.packet.login.LoginHelloPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.entity.EntityTrackerEntry;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.studioquire.oldfaithful.core.impl.Attachments;
import org.studioquire.oldfaithful.core.impl.ChannelPacket;
import org.studioquire.oldfaithful.core.impl.Core;
import org.studioquire.oldfaithful.core.impl.CoreNet;
import org.studioquire.oldfaithful.core.impl.CoreNet.ChannelImpl;
import org.studioquire.oldfaithful.core.impl.Hello;
import org.studioquire.oldfaithful.core.impl.Peer;
import org.studioquire.oldfaithful.core.impl.PeerHolder;
import org.studioquire.oldfaithful.core.impl.Rules;
import org.studioquire.oldfaithful.core.mixin.server.EntityTrackerAccessor;
import org.studioquire.oldfaithful.core.impl.content.ContentRegistry;
import org.studioquire.oldfaithful.core.mixin.server.ServerPlayNetworkHandlerAccessor;

/** The server's end. Nothing new reaches a peer until its login packet has declared it with {@link CoreNet#MAGIC}. */
public final class ServerNet implements CoreNet.Side {
	/** A vanilla client's disconnect reason: at most 100 characters. */
	static final String REFUSED = "This server needs the Faithful Library: studioquire.org/faithful-library";

	public static MinecraftServer server;

	private static Peer peer(Connection connection) {
		return ((PeerHolder) connection).faithful$peer();
	}

	private static byte[] matchHash() {
		return Hello.matchHash(Core.mods(), ContentRegistry.hashLines());
	}

	/**
	 * Before a login is accepted: true to go ahead. A core client is held here until it has answered the hello; a vanilla one
	 * goes ahead unless this server has something that must match.
	 */
	public static boolean admit(ServerLoginNetworkHandler login, LoginHelloPacket packet) {
		Peer peer = peer(login.connection);
		if (packet.worldSeed != CoreNet.MAGIC) {
			if (matchHash() == null) return true;
			login.disconnect(REFUSED);
			return false;
		}
		if (peer.replied) return true;
		if (!peer.helloSent) {
			peer.core = true;
			peer.helloSent = true;
			peer.heldLogin = packet;
			login.connection.sendPacket(new ChannelPacket(0, hello().encode(), false));
		}
		return false;
	}

	private static Hello hello() {
		Hello h = new Hello();
		h.kind = 1;
		h.accepted = true;
		h.mods = Core.mods();
		for (ChannelImpl c : CoreNet.channels()) {
			h.channels.put(c.number, c.name());
			h.channelProtocols.put(c.number, c.protocol);
		}
		byte[] hash = matchHash();
		h.hasMatch = hash != null;
		h.hasContent = ContentRegistry.any();
		h.matchHash = hash != null ? hash : new byte[0];
		return h;
	}

	private void reply(ServerLoginNetworkHandler login, byte[] data) throws IOException {
		Peer peer = peer(login.connection);
		if (!peer.helloSent || peer.replied) {
			login.disconnect("Protocol error");
			return;
		}
		Hello reply = Hello.decode(data);
		if (!reply.accepted) {
			login.disconnect("Your mods don't match this server's");
			return;
		}
		// A client that says it agrees is taken at its word: this is compatibility, not policing, and a false reply only
		// hurts the one who sent it.
		peer.replied = true;
		peer.content = ContentRegistry.any();
		for (int n : reply.acceptedChannels) {
			if (CoreNet.byNumber(n) != null) peer.channels.add(n);
		}
		login.accept((LoginHelloPacket) peer.heldLogin);
		peer.heldLogin = null;
	}

	@Override
	public void receive(NetworkHandler handler, ChannelPacket packet) {
		if (handler instanceof ServerLoginNetworkHandler) {
			ServerLoginNetworkHandler login = (ServerLoginNetworkHandler) handler;
			if (packet.channel != 0) {
				login.disconnect("Protocol error");
				return;
			}
			try {
				reply(login, packet.data);
			} catch (IOException e) {
				login.disconnect("Protocol error");
			}
		} else if (handler instanceof ServerPlayNetworkHandler) {
			ServerPlayNetworkHandler play = (ServerPlayNetworkHandler) handler;
			ChannelImpl channel = CoreNet.byNumber(packet.channel);
			if (channel == null || !peer(play.connection).channels.contains(packet.channel) || packet.data.length > channel.maxBytes) return;
			channel.deliver(channel.onServer, ((ServerPlayNetworkHandlerAccessor) play).faithful$player(), packet.data);
		}
	}

	/** A player has joined: what the core tells them first. */
	public static void joined(ServerPlayerEntity player) {
		if (CoreNet.byName(Core.ID + ":rules").available(player)) Rules.sendTo(player);
		Attachments.sendAllToSelf(player);
	}

	@Override
	public List<World> worlds() {
		List<World> running = new ArrayList<>();
		if (server != null && server.worlds != null) {
			for (World w : server.worlds) {
				if (w != null) running.add(w);
			}
		}
		return running;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<PlayerEntity> players() {
		return server == null || server.playerManager == null ? new ArrayList<>() : new ArrayList<>((List<PlayerEntity>) server.playerManager.players);
	}

	@Override
	public boolean runsServer() {
		return true;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<PlayerEntity> watching(Entity entity) {
		List<PlayerEntity> players = new ArrayList<>();
		for (int dimension : new int[] {0, -1}) {
			EntityTrackerEntry entry = (EntityTrackerEntry) ((EntityTrackerAccessor) server.getEntityTracker(dimension)).faithful$entriesById().get(entity.id);
			if (entry != null && entry.currentTrackedEntity == entity) players.addAll((Set<PlayerEntity>) entry.listeners);
		}
		return players;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<PlayerEntity> watching(Chunk chunk) {
		ChunkPos pos = new ChunkPos(chunk.x, chunk.z);
		List<PlayerEntity> players = new ArrayList<>();
		for (ServerPlayerEntity p : (List<ServerPlayerEntity>) server.playerManager.players) {
			if (p.world == chunk.world && p.activeChunks.contains(pos)) players.add(p);
		}
		return players;
	}

	@Override
	public boolean available(ChannelImpl channel, PlayerEntity player) {
		if (!(player instanceof ServerPlayerEntity)) return false;
		ServerPlayNetworkHandler handler = ((ServerPlayerEntity) player).networkHandler;
		return handler != null && peer(handler.connection).channels.contains(channel.number);
	}

	@Override
	public void toServer(ChannelImpl channel, byte[] data) {
		throw new IllegalStateException("toServer on the server, on " + channel.name());
	}

	@Override
	public void toPlayer(ChannelImpl channel, PlayerEntity player, byte[] data) {
		if (available(channel, player)) ((ServerPlayerEntity) player).networkHandler.sendPacket(new ChannelPacket(channel.number, data, channel.world));
	}

	@Override
	@SuppressWarnings("unchecked")
	public void toAll(ChannelImpl channel, byte[] data) {
		if (server == null) return;
		for (ServerPlayerEntity player : (List<ServerPlayerEntity>) server.playerManager.players) toPlayer(channel, player, data);
	}
}
