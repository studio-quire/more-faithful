package org.studioquire.faithful.stagehand;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSource;
import org.studioquire.faithful.stagehand.mixin.EntityAccessor;
import org.studioquire.oldfaithful.core.api.Server;
import org.studioquire.oldfaithful.core.api.Worldgen;

/**
 * Moving a player, the same for either side but for what {@link Side} does. Beta's traps: a server reads an unloaded chunk as empty and a client falls through one, so the place is loaded and checked
 * before anyone goes; a change of dimension ends by building a portal ({@code PortalForcerMixin}); and in single player the other dimension isn't running, so the landing can only be found on arrival,
 * and if there's none they go back where they were.
 */
public final class Move {
	/** What the mixin reads: Stagehand is moving someone between dimensions, on the world's thread, right now. */
	public static boolean crossing;

	/** Enough that what's at the place has been decorated: a chunk's decoration needs its neighbours in. */
	private static final int LOAD_RADIUS = 2;

	/** What's asked for: {@code surface} goes by the columns' tops and ignores {@code y}, {@code exact} takes the place as it is. */
	static final class Target {
		final int dimension;
		final double x, y, z;
		final boolean surface, exact;
		final Float yaw, pitch;

		Target(int dimension, double x, double y, double z, boolean surface, boolean exact, Float yaw, Float pitch) {
			this.dimension = dimension;
			this.x = x;
			this.y = y;
			this.z = z;
			this.surface = surface;
			this.exact = exact;
			this.yaw = yaw;
			this.pitch = pitch;
		}
	}

	/** Where someone ended up. */
	static final class Landed {
		final double x, y, z;
		final float yaw, pitch;
		final Map<String, Object> report = new LinkedHashMap<>();

		Landed(double x, double y, double z, float yaw, float pitch) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.yaw = yaw;
			this.pitch = pitch;
		}
	}

	private Move() {
	}

	static void load(World world, int x, int z, int radius) {
		ChunkSource source = world.getChunkSource();
		int chunkX = x >> 4, chunkZ = z >> 4;
		for (int a = chunkX - radius; a <= chunkX + radius; a++) {
			for (int b = chunkZ - radius; b <= chunkZ + radius; b++) source.loadChunk(a, b);
		}
	}

	static Landed go(Side side, PlayerEntity player, Target target) {
		if (player.health <= 0 || player.dead) throw new Failure("dead", player.name + " is dead");
		int from = player.dimensionId;
		if (target.dimension != from && target.dimension == Worldgen.NETHER && !side.netherAllowed(player)) throw new Failure("nether_off", "this server has allow-nether=false");
		double fromX = player.x, fromFeet = player.boundingBox.minY, fromZ = player.z;
		float yaw = target.yaw != null ? target.yaw : player.yaw, pitch = target.pitch != null ? target.pitch : player.pitch;

		World there = Server.world(target.dimension);
		int[] cell = null;
		if (there != null) {
			cell = landing(there, target);
			if (cell == null) throw nowhere(target);
		}
		leaveWhatTheyAreIn(player);
		if (target.dimension != from) side.changeDimension(player, target.dimension, target.x, target.y, target.z);
		if (there == null) {
			there = player.world;
			cell = landing(there, target);
			if (cell == null) {
				side.changeDimension(player, from, fromX, fromFeet, fromZ);
				side.place(player, fromX, fromFeet, fromZ, yaw, pitch);
				throw nowhere(target);
			}
		}

		double x = target.exact ? target.x : cell[0] + 0.5, y = target.exact ? target.y : cell[1], z = target.exact ? target.z : cell[2] + 0.5;
		side.place(player, x, y, z, yaw, pitch);
		player.velocityX = player.velocityY = player.velocityZ = 0.0;
		((EntityAccessor) player).stagehand$fallDistance(0.0F);

		Landed landed = new Landed(x, y, z, yaw, pitch);
		landed.report.put("x", round(x));
		landed.report.put("y", round(y));
		landed.report.put("z", round(z));
		landed.report.put("dim", dimension(target.dimension));
		landed.report.put("standing_on", Landing.blockAt(there, cell[0], cell[1] - 1, cell[2]));
		landed.report.put("moved_for_safety", !target.exact && !target.surface && (cell[0] != MathHelper.floor(target.x) || cell[1] != MathHelper.floor(target.y) || cell[2] != MathHelper.floor(target.z)));
		return landed;
	}

	private static int[] landing(World world, Target target) {
		int x = MathHelper.floor(target.x), y = MathHelper.floor(target.y), z = MathHelper.floor(target.z);
		load(world, x, z, LOAD_RADIUS);
		if (target.exact) return new int[] {x, y, z};
		return target.surface ? Landing.top(world, x, z) : Landing.near(world, x, y, z);
	}

	private static Failure nowhere(Target target) {
		return new Failure("no_safe_place", "nowhere safe to stand within " + Landing.REACH + " blocks of " + MathHelper.floor(target.x) + " " + MathHelper.floor(target.y) + " " + MathHelper.floor(target.z)
				+ "; \"exact\" goes there anyway");
	}

	/** A vehicle follows no teleport, and a sleeper is held at the bed. */
	private static void leaveWhatTheyAreIn(PlayerEntity player) {
		if (player.vehicle != null) player.setVehicle(player.vehicle);
		if (player.isSleeping()) player.wakeUp(true, true, false);
	}

	static Map<String, Object> coordinates(double x, double y, double z) {
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("x", round(x));
		out.put("y", round(y));
		out.put("z", round(z));
		return out;
	}

	static double round(double value) {
		return Math.round(value * 1000.0) / 1000.0;
	}

	static String dimension(int id) {
		return id == Worldgen.NETHER ? "nether" : id == Worldgen.OVERWORLD ? "overworld" : String.valueOf(id);
	}

	static int dimension(String word) {
		switch (word.toLowerCase(java.util.Locale.ROOT)) {
			case "overworld": return Worldgen.OVERWORLD;
			case "nether": return Worldgen.NETHER;
			default: throw new Failure("bad_dimension", "dim is overworld or nether, not " + word);
		}
	}
}
