package org.studioquire.faithful.stagehand.server;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.PlayerManager;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ServerWorld;
import net.minecraft.world.World;
import org.studioquire.faithful.stagehand.Move;
import org.studioquire.faithful.stagehand.Side;
import org.studioquire.faithful.stagehand.mixin.EntityAccessor;
import org.studioquire.faithful.stagehand.mixin.server.NetworkHandlerAccessor;
import org.studioquire.oldfaithful.core.api.Worldgen;

/**
 * A dedicated server's side. What a move has to get right here: a client has no notion of an unloaded chunk and falls through one, and the game's own way of following a player sends the new chunks in
 * rows from a far corner, which at a chunk a tick leaves them falling for ten seconds after a long move. A change of dimension instead subscribes them from the middle outward. So a long move does
 * that too, and the position goes out once more when the ground has been sent.
 */
@Environment(EnvType.SERVER)
public final class ServerSide implements Side {
	private static ServerPlayerEntity of(PlayerEntity player) {
		return (ServerPlayerEntity) player;
	}

	@Override
	public String where() {
		return "server";
	}

	@Override
	public Refusal refusal() {
		return null;
	}

	@Override
	public boolean paused() {
		return false;
	}

	@Override
	public boolean netherAllowed(PlayerEntity player) {
		return of(player).server.properties.getProperty("allow-nether", true);
	}

	@Override
	public void changeDimension(PlayerEntity player, int dimension, double x, double y, double z) {
		ServerPlayerEntity p = of(player);
		if (p.currentScreenHandler != p.playerScreenHandler) p.closeHandledScreen();
		// The game scales the position by 8 on the way, and subscribes them to the chunks around the result.
		double scale = dimension == Worldgen.NETHER ? 8.0 : 1.0 / 8.0;
		p.setPositionAndAnglesKeepPrevAngles(x * scale, y, z * scale, p.yaw, p.pitch);
		Move.crossing = true;
		try {
			p.server.playerManager.changePlayerDimension(p);
		} finally {
			Move.crossing = false;
		}
	}

	@Override
	public void place(PlayerEntity player, double x, double y, double z, float yaw, float pitch) {
		ServerPlayerEntity p = of(player);
		PlayerManager players = p.server.playerManager;
		// Within what they've been sent, all of it stays and the game's following adds the new rows, with no flicker. A chunk short of the edge, as the last rows may not have gone yet.
		int reach = players.getBlockViewDistance();
		boolean beyond = Math.abs(x - p.lastX) > reach || Math.abs(z - p.lastZ) > reach;
		p.setPositionAndAngles(x, y, z, yaw, pitch);
		((ServerWorld) p.world).updateEntity(p, false);
		if (beyond) players.updatePlayerAfterDimensionChange(p);
		p.networkHandler.teleport(x, y, z, yaw, pitch);
	}

	@Override
	public boolean answered(PlayerEntity player) {
		return ((NetworkHandlerAccessor) of(player).networkHandler).stagehand$teleported();
	}

	@Override
	public boolean arrived(PlayerEntity player, int chunkX, int chunkZ) {
		return !of(player).pendingChunkUpdates.contains(new ChunkPos(chunkX, chunkZ));
	}

	@Override
	public void correct(PlayerEntity player, double x, double y, double z, float yaw, float pitch) {
		ServerPlayerEntity p = of(player);
		double dx = x - p.x, dz = z - p.z;
		if (dx * dx + dz * dz > 4.0) return; // They've walked off: it's theirs now.
		p.networkHandler.teleport(x, y, z, p.yaw, p.pitch);
		((EntityAccessor) p).stagehand$fallDistance(0.0F);
	}

	@Override
	public void setTime(World world, long time) {
		((ServerWorld) world).synchronizeTimeAndUpdates(time);
	}
}
