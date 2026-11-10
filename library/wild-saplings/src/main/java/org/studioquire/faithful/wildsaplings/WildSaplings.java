package org.studioquire.faithful.wildsaplings;

import java.util.Random;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.LargeOakTreeFeature;
import net.minecraft.world.gen.feature.OakTreeFeature;
import net.minecraft.world.gen.feature.PineTreeFeature;
import net.minecraft.world.gen.feature.SpruceTreeFeature;
import org.studioquire.oldfaithful.core.api.Worldgen;

/** What a sapling grows: the trees of its kind, in the shares they grow wild, made as the world makes them. */
public final class WildSaplings {
	private WildSaplings() {
	}

	/**
	 * The tree to grow in place of the one beta chose. Shares from each biome's {@code getRandomTreeFeature}: a third of Taiga's trees are pines, and only
	 * Taiga has spruces, so a third anywhere; Forest's and Rainforest's oaks are a third big, every other biome's a tenth, which is the draw beta's
	 * sapling already made, so there it stands. Birches are birches.
	 */
	public static Feature wild(Feature chosen, World world, Random random, int x, int z) {
		Feature tree = chosen;
		Class<?> kind = chosen.getClass();
		if (kind == SpruceTreeFeature.class) {
			if (random.nextInt(3) == 0) tree = new PineTreeFeature();
		} else if (kind == OakTreeFeature.class || kind == LargeOakTreeFeature.class) {
			Biome biome = Worldgen.biome(world, x, z);
			if (biome == Biome.FOREST || biome == Biome.RAINFOREST) tree = random.nextInt(3) == 0 ? new LargeOakTreeFeature() : new OakTreeFeature();
		}
		// The world's generator prepares every tree it grows this way and beta's sapling never did, so a sapling's big oak had leaf clusters a layer short.
		tree.prepare(1.0, 1.0, 1.0);
		return tree;
	}
}
