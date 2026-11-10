package org.studioquire.oldfaithful.core.impl;

import java.io.DataInputStream;
import java.io.IOException;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.studioquire.oldfaithful.core.api.Channel;

/** Sounds from a server. In the base layer: a mod needn't have content to make a noise. */
public final class SoundNet {
	private static final int MAX_NAME = 128;
	private static final float HEARD_AT = 16.0F;
	private static final boolean SERVER = FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER;
	private static Channel channel;

	private SoundNet() {
	}

	static void start() {
		channel = CoreNet.channel(Core.ID + ":sound", 1).onClient(SoundNet::heard);
	}

	public static void play(World world, double x, double y, double z, String name, float volume, float pitch) {
		if (name.length() > MAX_NAME) throw new IllegalArgumentException("A sound's name is at most " + MAX_NAME + " long: " + name);
		world.playSound(x, y, z, name, volume, pitch);
		if (!SERVER) return;
		double reach = HEARD_AT * Math.max(1.0F, volume);
		for (Object o : world.players) {
			PlayerEntity player = (PlayerEntity) o;
			if (player.getSquaredDistance(x, y, z) > reach * reach) continue;
			channel.toPlayer(player, out -> {
				out.writeUTF(name);
				out.writeDouble(x);
				out.writeDouble(y);
				out.writeDouble(z);
				out.writeFloat(volume);
				out.writeFloat(pitch);
			});
		}
	}

	private static void heard(PlayerEntity player, DataInputStream in) throws IOException {
		String name = in.readUTF();
		double x = in.readDouble(), y = in.readDouble(), z = in.readDouble();
		float volume = in.readFloat(), pitch = in.readFloat();
		if (name.length() > MAX_NAME) throw new IOException("A sound's name of " + name.length());
		// From a server, so nothing here is trusted to be a number OpenAL can take.
		if (Double.isNaN(x + y + z) || Double.isInfinite(x + y + z) || !(volume >= 0.0F && volume <= 64.0F) || !(pitch > 0.0F && pitch <= 8.0F)) throw new IOException("A sound out of range");
		if (player != null && player.world != null) player.world.playSound(x, y, z, name, volume, pitch);
	}
}
