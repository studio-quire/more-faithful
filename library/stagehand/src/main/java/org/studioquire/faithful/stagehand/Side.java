package org.studioquire.faithful.stagehand;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

/** What differs between single player's game and a server: beta runs them through separate code. */
public interface Side {
	/** Why a script can't run here now. */
	final class Refusal {
		public final String code, message;

		public Refusal(String code, String message) {
			this.code = code;
			this.message = message;
		}
	}

	/** "single player" or "server". */
	String where();

	/** Why no script can run here now, or null if one can. */
	Refusal refusal();

	/** Single player's menu is up: the world stands still, though scripts still run. A step that needs ticks waits. */
	boolean paused();

	/** Whether scripts may send someone to the Nether. */
	boolean netherAllowed(PlayerEntity player);

	/**
	 * Through the game's own change of dimension, with no portal, and whatever's open closed. {@code x} and {@code z} say where in the world they're going, so the chunks that load are the ones there;
	 * {@link #place} settles it.
	 */
	void changeDimension(PlayerEntity player, int dimension, double x, double y, double z);

	/** To this place in the world they're in, whose chunks are loaded. {@code y} is their feet. */
	void place(PlayerEntity player, double x, double y, double z, float yaw, float pitch);

	/**
	 * Whether this player's client has answered the last move it was sent. Another mustn't follow until it has: a server takes the client's answer to an earlier move for a real step, once one has
	 * matched where it expects them, and a long enough one gets them kicked for moving too quickly.
	 */
	default boolean answered(PlayerEntity player) {
		return true;
	}

	/** Whether this player's client has been sent the chunk they're in. */
	default boolean arrived(PlayerEntity player, int chunkX, int chunkZ) {
		return true;
	}

	/** Their client now has the ground under them: says the position again, in case it dropped a little while it had none. */
	default void correct(PlayerEntity player, double x, double y, double z, float yaw, float pitch) {
	}

	/** The world's clock, to a later time. */
	void setTime(World world, long time);
}
