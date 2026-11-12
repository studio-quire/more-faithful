package org.studioquire.faithful.pumpkins;

import java.util.Random;
import java.util.function.Consumer;

import net.minecraft.block.Block;
import net.minecraft.world.World;

/**
 * Pumpkins that spread, as beta's mushrooms do, from beta's own random ticks. A pumpkin on grass, or on the dirt its own shade turns the grass under it into, now and then
 * sets a new one where beta's pumpkin patch would put one round it: up to {@link #REACH} cells sideways by the patch's own draw, in that column's open cell over grass within
 * {@link #RISE} up or down, facing any way. Open is empty or under a snow layer, which the pumpkin takes; never tall grass, a flower, or anything else.
 * <p>
 * A patch is full at {@link #FULL} pumpkins within twice that reach of a pumpkin, so a grown patch is a wild one's size and a patch left with one pumpkin fills again.
 * Counted within the reach alone, a patch never quite closed: its edge pumpkins see half of it, so it crept on, and one in ten from a lone pumpkin ended past 38 pumpkins
 * 45 cells across (simulated on open grass, Claude Opus 5.5, 2026-11-11). Twice the reach holds a patch to 6, rarely to 16.
 * <p>
 * A pumpkin snipped with shears (the Snipping mod) gets no random ticks, so it never spreads; it still counts here, as it's still a pumpkin.
 */
public final class Spreading {
	/**
	 * One in this many of a pumpkin's random ticks tries to set another. Beta gives a cell a random tick at 80 in 32768 a game tick, so a pumpkin tries once in
	 * 409.6 x 75 = 30,720 ticks; on open grass a lone pumpkin's patch is then full in about 70,000 ticks, three in-game days of a player within beta's 9 chunks
	 * of it (the middle of a simulation; one in ten took past five and a half). Tall grass and flowers take the cells they stand on, so a meadow fills slower.
	 */
	public static final int CHANCE = 75;
	/** Pumpkins within twice the reach of one, itself included, at which it stops. */
	public static final int FULL = 6;
	/** Beta's {@code PumpkinPatchFeature}: {@code nextInt(8) - nextInt(8)} sideways, {@code nextInt(4) - nextInt(4)} up or down. */
	public static final int REACH = 7, RISE = 3;

	/** Told of each pumpkin set, with what its cell held before ({@code x, y, z, id}), for the self-test. Null in play. */
	public static volatile Consumer<int[]> grew;

	private Spreading() {
	}

	/** A random tick of beta's pumpkin block, which is the jack-o'-lantern's too. */
	public static void tick(World world, int x, int y, int z, int id, Random random) {
		// The rule before the roll, so with it off the world's random goes as in beta.
		if (world.isRemote || id != Block.PUMPKIN.id || !Pumpkins.spreading.get() || random.nextInt(CHANCE) != 0) return;
		int ground = world.getBlockId(x, y - 1, z);
		if (ground != Block.GRASS_BLOCK.id && ground != Block.DIRT.id) return;
		// Everything this reads, so a pumpkin near the edge of what's loaded doesn't load or make land.
		if (!world.isRegionLoaded(x - 2 * REACH, y - 2 * RISE, z - 2 * REACH, x + 2 * REACH, y + 2 * RISE, z + 2 * REACH)) return;
		int tx = x + random.nextInt(8) - random.nextInt(8), tz = z + random.nextInt(8) - random.nextInt(8);
		int ty = landing(world, tx, y, tz);
		if (ty < 0 || full(world, x, y, z)) return;
		int was = world.getBlockId(tx, ty, tz);
		world.setBlock(tx, ty, tz, Block.PUMPKIN.id, random.nextInt(4));
		Consumer<int[]> watch = grew;
		if (watch != null) watch.accept(new int[] {tx, ty, tz, was});
	}

	/** The open cell over grass in this column, nearest {@code y} within {@link #RISE}; -1 if there's none. */
	public static int landing(World world, int x, int y, int z) {
		for (int i = 0; i <= 2 * RISE; i++) {
			int ty = y + ((i & 1) == 1 ? (i + 1) / 2 : -(i / 2));
			if (ty < 1 || ty > 127) continue;
			int here = world.getBlockId(x, ty, z);
			if ((here == 0 || here == Block.SNOW.id) && world.getBlockId(x, ty - 1, z) == Block.GRASS_BLOCK.id) return ty;
		}
		return -1;
	}

	/** Whether there are {@link #FULL} pumpkins, this one included, within twice the reach of it. */
	static boolean full(World world, int x, int y, int z) {
		int pumpkin = Block.PUMPKIN.id, n = 0;
		for (int dx = -2 * REACH; dx <= 2 * REACH; dx++) {
			for (int dz = -2 * REACH; dz <= 2 * REACH; dz++) {
				for (int dy = -2 * RISE; dy <= 2 * RISE; dy++) {
					if (world.getBlockId(x + dx, y + dy, z + dz) == pumpkin && ++n >= FULL) return true;
				}
			}
		}
		return false;
	}
}
