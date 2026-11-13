package org.studioquire.oldfaithful.core.impl.client;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.ClientNetworkHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.NetworkHandler;
import net.minecraft.world.World;
import org.studioquire.oldfaithful.core.impl.ChannelPacket;
import org.studioquire.oldfaithful.core.impl.Core;
import org.studioquire.oldfaithful.core.impl.CoreNet;
import org.studioquire.oldfaithful.core.impl.CoreNet.ChannelImpl;
import org.studioquire.oldfaithful.core.impl.Hello;
import org.studioquire.oldfaithful.core.impl.ModInfo;
import org.studioquire.oldfaithful.core.impl.Peer;
import org.studioquire.oldfaithful.core.impl.PeerHolder;
import org.studioquire.oldfaithful.core.impl.content.ContentRegistry;
import org.studioquire.oldfaithful.core.impl.content.Wire;
import org.studioquire.oldfaithful.core.mixin.client.ClientNetworkHandlerAccessor;

/**
 * The client's end. No 250 goes out until a server's hello has come in: a login reply arriving first means a vanilla server,
 * and the game goes on as if the core weren't there. In single player, messages go round through the next tick.
 */
public final class ClientNet implements CoreNet.Side {
	public static Minecraft minecraft;
	private static ClientNetworkHandler handler;
	/** The channels both ends have, by the number the server's hello gave them, once it has said hello. */
	private static final Map<Integer, ChannelImpl> SERVER_NUMBERS = new HashMap<>();
	private static final Map<ChannelImpl, Integer> NUMBERS = new HashMap<>();

	private static final class Loop {
		final ChannelImpl channel;
		final byte[] data;
		final boolean toServer;

		Loop(ChannelImpl channel, byte[] data, boolean toServer) {
			this.channel = channel;
			this.data = data;
			this.toServer = toServer;
		}
	}

	private static final ArrayDeque<Loop> LOOP = new ArrayDeque<>();

	/** Each tick: single player's messages. */
	public static void tick() {
		for (int i = LOOP.size(); i > 0; i--) {
			Loop m = LOOP.poll();
			if (minecraft.player != null) m.channel.deliver(m.toServer ? m.channel.onServer : m.channel.onClient, minecraft.player, m.data);
		}
	}

	/** The hello of this connection has been heard. */
	private static volatile boolean helloHeard;
	/**
	 * The connection this game turned down, whose server answers the refusal by disconnecting it. That answer can come in
	 * while the hello's tick is still reading packets, and beta's handler would put its own screen over ours.
	 */
	private static ClientNetworkHandler refused;

	public static void connected(ClientNetworkHandler newHandler) {
		handler = newHandler;
		refused = null;
		helloHeard = false;
		Wire.serverContent = false;
		SERVER_NUMBERS.clear();
		NUMBERS.clear();
	}

	/** Whether this connection is one the game turned down, so its end leaves the refusal on the screen. */
	public static boolean refused(ClientNetworkHandler h) {
		return h != null && h == refused;
	}

	public static void disconnected() {
		handler = null;
		helloHeard = false;
		Wire.serverContent = false;
		SERVER_NUMBERS.clear();
		NUMBERS.clear();
		LOOP.clear();
	}

	@Override
	public void receive(NetworkHandler networkHandler, ChannelPacket packet) {
		ClientNetworkHandler client = (ClientNetworkHandler) networkHandler;
		try {
			if (packet.channel == 0) {
				// One per connection: a later one could change what a server has already been told.
				if (helloHeard) return;
				helloHeard = true;
				hello(client, packet.data);
				return;
			}
			ChannelImpl channel = SERVER_NUMBERS.get(packet.channel);
			if (channel == null || packet.data.length > channel.maxBytes) return;
			channel.deliver(channel.onClient, minecraft.player, packet.data);
		} catch (IOException | RuntimeException e) {
			// With or without the hardening mod's own catch: a bad message is a disconnect, never a crash.
			// What the exception says may be the server's own words: one line of them, so they can't pose as more of the log.
			System.out.println("[faithful-core] WARN A bad message from the server: " + e.toString().replaceAll("\\p{Cntrl}+", " "));
			((ClientNetworkHandlerAccessor) client).faithful$connection().disconnect("disconnect.genericReason", "Internal exception: " + e);
		}
	}

	private void hello(ClientNetworkHandler client, byte[] data) throws IOException {
		Hello hello = Hello.decode(data);
		byte[] mine = Hello.matchHash(Core.mods(), ContentRegistry.hashLines());
		boolean sameProtocol = hello.protocol == Hello.PROTOCOL;
		boolean matches = !hello.hasMatch || (sameProtocol && mine != null && Arrays.equals(mine, hello.matchHash));

		Hello reply = new Hello();
		reply.kind = 2;
		reply.accepted = matches;
		// The section stays and says nothing: no server reads it, and a client-only mod is no business of a server's.
		if (!matches) {
			refused = client;
			client.sendPacketAndDisconnect(new ChannelPacket(0, reply.encode(), false));
			List<String> log = new ArrayList<>();
			List<String> lines = sameProtocol ? Hello.differences(hello.mods, Core.mods(), MismatchScreen::modName, log)
					: new ArrayList<>(Arrays.asList("This server has another version of the Faithful Library, and yours needs to match it."));
			if (!sameProtocol) log.add("Faithful Core protocol " + hello.protocol + " on the server, " + Hello.PROTOCOL + " here");
			for (String line : log) System.out.println("[faithful-core] Not joining, the mods don't match: " + line);
			List<String> theirSet = new ArrayList<>();
			for (ModInfo m : hello.mods) {
				if (m.mustMatch()) theirSet.add(m.id);
			}
			minecraft.setScreen(new MismatchScreen(lines, sameProtocol ? theirSet : null));
			return;
		}
		Peer p = ((PeerHolder) ((ClientNetworkHandlerAccessor) client).faithful$connection()).faithful$peer();
		p.core = true;
		p.content = hello.hasContent;
		Wire.serverContent = hello.hasContent;
		if (sameProtocol) {
			for (java.util.Map.Entry<Integer, String> e : hello.channels.entrySet()) {
				ChannelImpl mineChannel = CoreNet.byName(e.getValue());
				if (mineChannel != null && mineChannel.protocol == hello.channelProtocols.get(e.getKey())) {
					SERVER_NUMBERS.put(e.getKey(), mineChannel);
					NUMBERS.put(mineChannel, e.getKey());
					reply.acceptedChannels.add(e.getKey());
				}
			}
		}
		client.sendPacket(new ChannelPacket(0, reply.encode(), false));
	}

	@Override
	public List<World> worlds() {
		return singlePlayer() ? Collections.singletonList(minecraft.world) : Collections.emptyList();
	}

	@Override
	public List<PlayerEntity> players() {
		return singlePlayer() && minecraft.player != null ? Collections.singletonList(minecraft.player) : Collections.emptyList();
	}

	private static boolean singlePlayer() {
		return minecraft.world != null && !minecraft.world.isRemote;
	}

	@Override
	public boolean available(ChannelImpl channel, PlayerEntity player) {
		return singlePlayer() || NUMBERS.containsKey(channel);
	}

	@Override
	public void toServer(ChannelImpl channel, byte[] data) {
		if (singlePlayer()) LOOP.add(new Loop(channel, data, true));
		else if (NUMBERS.containsKey(channel)) handler.sendPacket(new ChannelPacket(NUMBERS.get(channel), data, channel.world));
	}

	@Override
	public void toPlayer(ChannelImpl channel, PlayerEntity player, byte[] data) {
		if (singlePlayer()) LOOP.add(new Loop(channel, data, false));
	}

	@Override
	public void toAll(ChannelImpl channel, byte[] data) {
		toPlayer(channel, null, data);
	}
}
