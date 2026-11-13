package org.studioquire.faithful.blueflowers;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.studioquire.oldfaithful.core.api.Worldgen;

/**
 * Patches as beta makes its dandelions, in a decoration's own stream: a point at any height, then 64 tries within 7 cells sideways and 3 up or down, each taking a cell the flower can live
 * in. Most points are far from the ground and make nothing, as most of beta's do. By the square's biome, where beta looks for its own flowers: in Taiga, Tundra and Swampland, none elsewhere.
 * <p>
 * We come after vanilla's snow, so where it has settled, the flower takes the snow's cell, as though it had stood there first and the snow had passed it by, as it passes beta's.
 * <p>
 * Flowers stay {@link #MARGIN} cells inside the square this chunk decorates. A neighbour's tree that's still to come looks for free cells out to its leaves, and gives up on anything but air and
 * leaves there, which moves the rest of that chunk's draws; and its leaves can shade what's under them too deeply to live, so a flower there would drop at its first tick. Beta's own flowers can
 * meet both. A spruce or an oak can't reach these, nor can a pine unless its foot is well below them on a slope. Swampland's one tree in ten is a big oak, whose crown can: it draws from a
 * stream of its own, so nothing else moves, and a flower it comes to stand over lives by day and drops in the dark, as beta's flowers under trees do.
 */
final class Patches {
	private static final int TRIES = 64, MARGIN = 3, SPAN = 16 - 2 * MARGIN;

	private Patches() {
	}

	/**
	 * Patches a chunk. Beta gives Taiga 2 of dandelions, but they spill out of the square where these stay in its middle, so 2 of these made about 0.6 of the dandelions in the same land (the test
	 * counts both); 3 comes to about as many. Tundra is open flat ground with nothing else on it, where every patch shows, so half that looks about as flowered.
	 * Swampland (bare, steep, green hills beside the snow in beta, nothing like a swamp: Highland to us) is open ground too and where lupins look most at home, so a little more than Tundra, to
	 * be tuned in play.
	 */
	private static float patches(Biome biome) {
		if (biome == Biome.TAIGA) return 3.0F;
		if (biome == Biome.TUNDRA) return 1.5F;
		if (biome == Biome.SWAMPLAND) return 2.0F;
		return 0.0F;
	}

	static void grow(World world, Random random, int x, int z) {
		float rate = patches(Worldgen.biome(world, x + 8, z + 8));
		if (rate == 0.0F) return;
		int count = (int) rate;
		if (random.nextFloat() < rate - count) count++;
		for (int i = 0; i < count; i++) {
			int cx = x + MARGIN + random.nextInt(SPAN), cy = random.nextInt(128), cz = z + MARGIN + random.nextInt(SPAN);
			for (int j = 0; j < TRIES; j++) {
				int fx = cx + random.nextInt(8) - random.nextInt(8), fy = cy + random.nextInt(4) - random.nextInt(4), fz = cz + random.nextInt(8) - random.nextInt(8);
				if (fx < x + MARGIN || fz < z + MARGIN || fx >= x + MARGIN + SPAN || fz >= z + MARGIN + SPAN) continue;
				if (suits(world, fx, fy, fz)) world.setBlockWithoutNotifyingNeighbors(fx, fy, fz, BlueFlowers.flower.get().id);
			}
		}
	}

	private static boolean suits(World world, int x, int y, int z) {
		if (y <= 0 || y >= 128) return false;
		int id = world.getBlockId(x, y, z);
		if (id != 0 && id != Block.SNOW.id) return false;
		// Beta's own patches ask this. Decorating, a column's light is its own, from the sky straight down, and light from the side only ever adds to it, so what's standing now stays.
		return BlueFlowers.flower.get().canGrow(world, x, y, z);
	}
}
