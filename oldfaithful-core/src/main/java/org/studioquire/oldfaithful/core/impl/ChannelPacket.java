package org.studioquire.oldfaithful.core.impl;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.NetworkHandler;
import net.minecraft.network.packet.Packet;

/**
 * Everything the core and its mods send: packet 250, an id vanilla doesn't have and disconnects on, so it goes only to a peer
 * that has declared itself. The reader only copies bytes, and the length is checked before anything's allocated. The game's
 * own overflow check disconnects at 1 MB queued, so the most a message may hold stays well under that.
 */
public final class ChannelPacket extends Packet {
	public static final int ID = 250;
	/** "FQL1": OSL uses 250 too. */
	static final int PREFIX = 0x46514C31;
	public static final int MAX_TO_SERVER = 32 * 1024;
	public static final int MAX_TO_CLIENT = 256 * 1024;
	private static final boolean READS_FOR_SERVER = FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER;

	public int channel;
	public byte[] data;

	public ChannelPacket() {
	}

	public ChannelPacket(int channel, byte[] data, boolean world) {
		this.channel = channel;
		this.data = data;
		this.worldPacket = world;
	}

	@Override
	public void read(DataInputStream stream) {
		try {
			if (stream.readInt() != PREFIX) throw new IOException("Not one of ours: a 250 packet with another prefix");
			channel = stream.readUnsignedShort();
			int length = stream.readInt();
			int max = READS_FOR_SERVER ? MAX_TO_SERVER : MAX_TO_CLIENT;
			if (length < 0 || length > max) throw new IOException("A channel message of " + length + " bytes");
			byte[] bytes = new byte[length];
			stream.readFully(bytes);
			data = bytes;
		} catch (IOException e) {
			throw CoreNet.<RuntimeException>sneak(e);
		}
	}

	@Override
	public void write(DataOutputStream stream) {
		try {
			stream.writeInt(PREFIX);
			stream.writeShort(channel);
			stream.writeInt(data.length);
			stream.write(data);
		} catch (IOException e) {
			throw CoreNet.<RuntimeException>sneak(e);
		}
	}

	@Override
	public void apply(NetworkHandler networkHandler) {
		CoreNet.side().receive(networkHandler, this);
	}

	@Override
	public int size() {
		return 10 + (data == null ? 0 : data.length);
	}
}
