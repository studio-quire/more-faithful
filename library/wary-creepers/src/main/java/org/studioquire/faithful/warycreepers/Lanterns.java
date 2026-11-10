package org.studioquire.faithful.warycreepers;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResultType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.studioquire.oldfaithful.core.api.Chunks;

/**
 * A jack-o'-lantern frightens a creeper that can see its lit face (the user's rule, 2026-11-10): the creeper's eyes on the face's side of the block, in front
 * of the face's plane, within {@link #REACH} of the lantern's middle, with nothing between its eyes and the middle of the face by beta's own sight, the
 * ray a monster uses to see a player. So glass, leaves and fences hide it as they hide a player, and water doesn't.
 *
 * <p>Lanterns are found by reading the blocks round the creeper rather than keeping a list: beta's lanterns are placed by players and moved by pistons,
 * and worlds have them from before the mod. A ball of 8 is about 2,100 cells, read straight from the chunks each time the core asks (five times a second
 * per creeper not already running): about 15 microseconds with none near.
 */
public final class Lanterns {
	public static final double REACH = 8.0;
	/** How far inside the face the sight line ends, so it enters the block through the face, however slanting. */
	private static final double INSIDE = 0.001;

	private Lanterns() {
	}

	/** The middle of the nearest lantern whose lit face this creeper sees within reach, or null. */
	public static Vec3d nearest(CreeperEntity creeper) {
		World world = creeper.world;
		double ex = creeper.x, ey = creeper.y + creeper.getEyeHeight(), ez = creeper.z;
		int lantern = Block.JACK_O_LANTERN.id;
		int x0 = MathHelper.floor(ex - REACH), x1 = MathHelper.floor(ex + REACH);
		int z0 = MathHelper.floor(ez - REACH), z1 = MathHelper.floor(ez + REACH);
		List<int[]> found = null;
		for (int cx = x0 >> 4; cx <= x1 >> 4; cx++) {
			for (int cz = z0 >> 4; cz <= z1 >> 4; cz++) {
				if (!Chunks.isLoaded(world, cx, cz)) continue;
				Chunk chunk = world.getChunk(cx, cz);
				for (int x = Math.max(x0, cx << 4); x <= Math.min(x1, (cx << 4) + 15); x++) {
					double dx = x + 0.5 - ex;
					for (int z = Math.max(z0, cz << 4); z <= Math.min(z1, (cz << 4) + 15); z++) {
						double dz = z + 0.5 - ez, left = REACH * REACH - dx * dx - dz * dz;
						if (left < 0) continue;
						double half = Math.sqrt(left);
						int y0 = Math.max(0, (int) Math.ceil(ey - 0.5 - half)), y1 = Math.min(127, MathHelper.floor(ey - 0.5 + half));
						for (int y = y0; y <= y1; y++) {
							if (chunk.getBlockId(x & 15, y, z & 15) != lantern) continue;
							if (found == null) found = new ArrayList<>(4);
							found.add(new int[] {x, y, z});
						}
					}
				}
			}
		}
		if (found == null) return null;
		found.sort((a, b) -> Double.compare(squared(a, ex, ey, ez), squared(b, ex, ey, ez)));
		for (int[] l : found) {
			if (seesFace(world, ex, ey, ez, l[0], l[1], l[2])) return Vec3d.create(l[0] + 0.5, l[1] + 0.5, l[2] + 0.5);
		}
		return null;
	}

	private static double squared(int[] cell, double x, double y, double z) {
		double dx = cell[0] + 0.5 - x, dy = cell[1] + 0.5 - y, dz = cell[2] + 0.5 - z;
		return dx * dx + dy * dy + dz * dz;
	}

	/**
	 * Whether eyes at {@code ex, ey, ez} see the lit face of the lantern at {@code x, y, z}. Its meta is which way it looks, as beta's {@code PumpkinBlock}
	 * draws it: 0 south (+z), 1 west (-x), 2 north (-z), 3 east (+x), toward whoever placed it.
	 */
	public static boolean seesFace(World world, double ex, double ey, double ez, int x, int y, int z) {
		double fx = x + 0.5, fz = z + 0.5;
		switch (world.getBlockMeta(x, y, z) & 3) {
			case 0:
				if (ez <= z + 1) return false;
				fz = z + 1 - INSIDE;
				break;
			case 1:
				if (ex >= x) return false;
				fx = x + INSIDE;
				break;
			case 2:
				if (ez >= z) return false;
				fz = z + INSIDE;
				break;
			default:
				if (ex <= x + 1) return false;
				fx = x + 1 - INSIDE;
				break;
		}
		HitResult hit = world.raycast(Vec3d.createCached(ex, ey, ez), Vec3d.createCached(fx, y + 0.5, fz));
		return hit != null && hit.type == HitResultType.BLOCK && hit.blockX == x && hit.blockY == y && hit.blockZ == z;
	}
}
