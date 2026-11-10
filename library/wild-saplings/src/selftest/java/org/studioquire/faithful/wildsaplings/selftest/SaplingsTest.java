package org.studioquire.faithful.wildsaplings.selftest;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.SaplingBlock;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.LargeOakTreeFeature;
import net.minecraft.world.gen.feature.OakTreeFeature;
import net.minecraft.world.gen.feature.PineTreeFeature;
import net.minecraft.world.gen.feature.SpruceTreeFeature;
import org.studioquire.faithful.harness.test.WorldTest;
import org.studioquire.oldfaithful.core.api.Worldgen;

/**
 * Saplings grown through beta's own growth call, each held against the trees the world's generator makes from the same draws, cell by cell: spruce
 * saplings and oak saplings on the plot, and oak saplings over a Forest far off. All of it within a tick, and every cell put back after each tree.
 */
public final class SaplingsTest extends WorldTest {
	private static final int SEEDS = 30, REACH = 10, TOP = 18, SKY = 96;
	private static final int OAK = 0, SPRUCE = 1;

	/** Where trees grow, and the box round it as it was before. */
	private int tx, ty, tz;
	private int[] ids, metas;

	/**
	 * The box emptied first, all but its floor, and any leaves or logs in that too: on the plot it reaches past the plot's cleared 8 cells, where the world's
	 * own trees can stand, and beta's leaves and logs mark the leaves near them for decay as they're taken out, notified or not, so putting a tree's cells
	 * back would leave neighbouring leaves changed, read as part of the next tree.
	 */
	private void site(int sx, int sy, int sz) {
		tx = sx;
		ty = sy;
		tz = sz;
		for (int dx = -REACH; dx <= REACH; dx++) {
			for (int dz = -REACH; dz <= REACH; dz++) {
				for (int dy = -1; dy <= TOP; dy++) {
					int id = world.getBlockId(tx + dx, ty + dy, tz + dz);
					if (id != 0 && (dy >= 0 || id == Block.LEAVES.id || id == Block.LOG.id)) world.setBlockWithoutNotifyingNeighbors(tx + dx, ty + dy, tz + dz, 0);
				}
			}
		}
		int n = (2 * REACH + 1) * (2 * REACH + 1) * (TOP + 2);
		ids = new int[n];
		metas = new int[n];
		int i = 0;
		for (int dx = -REACH; dx <= REACH; dx++) {
			for (int dz = -REACH; dz <= REACH; dz++) {
				for (int dy = -1; dy <= TOP; dy++, i++) {
					ids[i] = world.getBlockId(tx + dx, ty + dy, tz + dz);
					metas[i] = world.getBlockMeta(tx + dx, ty + dy, tz + dz);
				}
			}
		}
	}

	/** What changed in the box since {@link #site}, then put back. */
	private String grown() {
		StringBuilder changed = new StringBuilder();
		int i = 0;
		for (int dx = -REACH; dx <= REACH; dx++) {
			for (int dz = -REACH; dz <= REACH; dz++) {
				for (int dy = -1; dy <= TOP; dy++, i++) {
					int id = world.getBlockId(tx + dx, ty + dy, tz + dz), meta = world.getBlockMeta(tx + dx, ty + dy, tz + dz);
					if (id == ids[i] && meta == metas[i]) continue;
					changed.append(dx).append(',').append(dy).append(',').append(dz).append('=').append(id).append(':').append(meta).append(' ');
					world.setBlockWithoutNotifyingNeighbors(tx + dx, ty + dy, tz + dz, ids[i], metas[i]);
				}
			}
		}
		return changed.toString();
	}

	private String sapling(int kind, long seed) {
		world.setBlockWithoutNotifyingNeighbors(tx, ty - 1, tz, Block.DIRT.id);
		world.setBlockWithoutNotifyingNeighbors(tx, ty, tz, Block.SAPLING.id, kind);
		((SaplingBlock) Block.SAPLING).generate(world, tx, ty, tz, new Random(seed));
		return grown();
	}

	/** The world's way: the tree prepared as its generator prepares them, after the draws a sapling makes before it. */
	private String wild(Feature tree, boolean prepared, long seed, int... draws) {
		world.setBlockWithoutNotifyingNeighbors(tx, ty - 1, tz, Block.DIRT.id);
		Random random = new Random(seed);
		for (int bound : draws) random.nextInt(bound);
		if (prepared) tree.prepare(1.0, 1.0, 1.0);
		tree.generate(world, random, tx, ty, tz);
		return grown();
	}

	private void spruces(String where) {
		int pines = 0, spruces = 0, neither = 0;
		for (long seed = 0; seed < SEEDS; seed++) {
			String tree = sapling(SPRUCE, seed);
			if (tree.equals(wild(new PineTreeFeature(), true, seed, 3))) pines++;
			else if (tree.equals(wild(new SpruceTreeFeature(), true, seed, 3))) spruces++;
			else neither++;
		}
		note(where + ": " + SEEDS + " spruce saplings grew " + pines + " pines and " + spruces + " spruces");
		check(where + ": each spruce sapling grew a wild pine or spruce, " + neither + " neither", neither == 0);
		check(where + ": about a third pines", pines >= SEEDS / 6 && pines <= SEEDS / 2);
	}

	private void oaks(String where) {
		Biome biome = Worldgen.biome(world, tx, tz);
		boolean forest = biome == Biome.FOREST || biome == Biome.RAINFOREST;
		int[] draws = forest ? new int[] {10, 3} : new int[] {10};
		int big = 0, small = 0, neither = 0, betas = 0;
		long firstBig = -1;
		for (long seed = 0; seed < SEEDS; seed++) {
			if (new Random(seed).nextInt(10) == 0) betas++;
			String tree = sapling(OAK, seed);
			if (tree.equals(wild(new LargeOakTreeFeature(), true, seed, draws))) {
				big++;
				if (firstBig < 0) firstBig = seed;
			} else if (tree.equals(wild(new OakTreeFeature(), true, seed, draws))) {
				small++;
			} else {
				neither++;
			}
		}
		note(where + " (" + biome.name + "): " + SEEDS + " oak saplings grew " + big + " big oaks and " + small + " small; beta's sapling would have grown " + betas + " big");
		check(where + ": each oak sapling grew a wild oak, big or small, " + neither + " neither", neither == 0);
		if (forest) check(where + ": about a third big, as this biome's own", big >= SEEDS / 6 && big <= SEEDS / 2);
		else check(where + ": big as often as beta's sapling, as this biome's own", big == betas);
		if (firstBig >= 0) {
			String full = wild(new LargeOakTreeFeature(), true, firstBig, draws), unprepared = wild(new LargeOakTreeFeature(), false, firstBig, draws);
			check(where + ": a big oak as beta's sapling made them would have been different", !full.equals(unprepared));
		}
	}

	/** Somewhere well away from the plot in this biome. */
	private int[] find(Biome biome) {
		for (int r = 512; r < 12000; r += 64) {
			for (int a = -r; a <= r; a += 64) {
				for (int[] p : new int[][] {{a, -r}, {a, r}, {-r, a}, {r, a}}) {
					if (Worldgen.biome(world, x + p[0], z + p[1]) == biome) return new int[] {x + p[0], z + p[1]};
				}
			}
		}
		return null;
	}

	@Override
	protected void script() {
		then(() -> {
			site(x + 4, y, z + 4);
			spruces("on the plot");
			oaks("on the plot");
			int[] forest = find(Biome.FOREST);
			if (!check("a Forest to plant in", forest != null)) return;
			for (int cx = (forest[0] - REACH >> 4) - 1; cx <= (forest[0] + REACH >> 4) + 1; cx++) {
				for (int cz = (forest[1] - REACH >> 4) - 1; cz <= (forest[1] + REACH >> 4) + 1; cz++) world.getChunkSource().loadChunk(cx, cz);
			}
			site(forest[0], SKY, forest[1]);
			oaks("over a Forest");
		});
	}
}
