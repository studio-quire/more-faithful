package org.studioquire.faithful.harness.test.server;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.ServerWorld;
import net.minecraft.world.World;

/** Moving a player on a server has to tell their client. */
public final class ServerMove {
	private ServerMove() {
	}

	/** A server's world reads an unloaded chunk as empty. */
	public static void load(World world, int x, int z, int radius) {
		for (int cx = (x - radius) >> 4; cx <= (x + radius) >> 4; cx++) {
			for (int cz = (z - radius) >> 4; cz <= (z + radius) >> 4; cz++) ((ServerWorld) world).chunkCache.loadChunk(cx, cz);
		}
	}

	public static void to(PlayerEntity player, double x, double y, double z) {
		((ServerPlayerEntity) player).networkHandler.teleport(x, y, z, player.yaw, player.pitch);
	}
}
