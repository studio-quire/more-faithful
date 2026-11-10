package org.studioquire.faithful.blueflowers;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.studioquire.oldfaithful.core.api.BlockEntry;
import org.studioquire.oldfaithful.core.api.Content;
import org.studioquire.oldfaithful.core.api.FaithfulMod;
import org.studioquire.oldfaithful.core.api.Growth;
import org.studioquire.oldfaithful.core.api.Mod;
import org.studioquire.oldfaithful.core.api.Recipes;
import org.studioquire.oldfaithful.core.api.Worldgen;

/** A third flower for the cold biomes: in patches in new terrain, as beta's dandelions grow, and from bonemeal on their grass. */
public final class BlueFlowers implements FaithfulMod {
	/** {@code DyeItem.names}. */
	private static final int LIGHT_BLUE = 12;

	public static BlockEntry<BlueFlower> flower;

	@Override
	public void content(Content c) {
		flower = c.block("blue_flower", s -> new BlueFlower(s.id(), s.texture("blue_flower"))).standIn(Block.ROSE);
	}

	@Override
	public void init(Mod mod) {
		// Two, as a rose or a dandelion makes.
		Recipes.shapeless(new ItemStack(Item.DYE, 2, LIGHT_BLUE), flower.get());
		Worldgen.decorate("blue-flowers:patches", Patches::grow);
		Growth.onBonemeal(BlueFlowers::bonemeal);
	}

	static boolean cold(Biome biome) {
		return biome == Biome.TAIGA || biome == Biome.TUNDRA;
	}

	/**
	 * Beta's own walk from the grass clicked, and a rose's odds at each cell the first time it's reached, since a cell beta has filled takes nothing more. Before beta's walk, which comes
	 * along to fill the rest: not {@code handled}, so the bonemeal goes on to do all it always does.
	 */
	private static void bonemeal(Growth.Bonemeal event) {
		if (event.block != Block.GRASS_BLOCK.id || event.cancelled() || !cold(Worldgen.biome(event.world, event.x, event.z))) return;
		World world = event.world;
		Random random = world.random;
		Set<Vec3i> reached = new HashSet<>();
		walks:
		for (int i = 0; i < 128; i++) {
			int x = event.x, y = event.y + 1, z = event.z;
			for (int step = 0; step < i / 16; step++) {
				x += random.nextInt(3) - 1;
				y += (random.nextInt(3) - 1) * random.nextInt(3) / 2;
				z += random.nextInt(3) - 1;
				if (world.getBlockId(x, y - 1, z) != Block.GRASS_BLOCK.id || world.shouldSuffocate(x, y, z)) continue walks;
			}
			if (reached.add(new Vec3i(x, y, z)) && world.isAir(x, y, z) && random.nextInt(30) == 0) world.setBlock(x, y, z, flower.get().id);
		}
	}
}
