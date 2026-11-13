package org.studioquire.faithful.golems;

import java.util.BitSet;

import net.minecraft.block.Block;
import net.minecraft.block.CropBlock;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.SaplingBlock;
import net.minecraft.item.ItemStack;
import org.studioquire.oldfaithful.core.api.Tags;

/**
 * What the land is made of, to a pickaxe golem: stone, dirt, grass, sand, sandstone, gravel, clay, netherrack, soul sand, and ores, a mod's too through {@code c:ores/}. Not cobble, which
 * is what mining leaves and building uses, nor anything else built, cut or growing. And what lies on the land and comes away with it: plants and snow.
 */
public final class Ground {
	private static BitSet ground, cover;

	private Ground() {
	}

	/**
	 * Beta's own plants by their class, so a mod's flower built on {@code PlantBlock} (Blue Flowers' lupin) is one with no dependency on it; cactus, cane and snow, which aren't. Saplings
	 * and crops are left out: someone planted those (the axe's stumps, the hoe's field). Built lazily, as mods add their blocks in their {@code init}.
	 */
	private static BitSet cover() {
		if (cover != null) return cover;
		BitSet set = new BitSet();
		for (Block b : Block.BLOCKS) {
			if (b instanceof PlantBlock && !(b instanceof SaplingBlock) && !(b instanceof CropBlock)) set.set(b.id);
		}
		for (Block b : new Block[] {Block.SNOW, Block.CACTUS, Block.SUGAR_CANE}) set.set(b.id);
		cover = set;
		return set;
	}

	/** Built lazily: other mods add to the ore tags in their {@code init}, in no set order. */
	private static BitSet ground() {
		if (ground != null) return ground;
		BitSet set = new BitSet();
		for (Block b : new Block[] {Block.STONE, Block.DIRT, Block.GRASS_BLOCK, Block.SAND, Block.SANDSTONE, Block.GRAVEL, Block.CLAY, Block.NETHERRACK, Block.SOUL_SAND, Block.LIT_REDSTONE_ORE}) set.set(b.id);
		for (String tag : Tags.names()) {
			if (!tag.startsWith("c:ores/") && !tag.equals("c:stone") && !tag.equals("c:dirt") && !tag.equals("c:sand") && !tag.equals("c:gravel")) continue;
			for (ItemStack s : Tags.items(tag)) {
				if (s.itemId < Block.BLOCKS.length && Block.BLOCKS[s.itemId] != null) set.set(s.itemId);
			}
		}
		ground = set;
		return set;
	}

	public static boolean isGround(int id) {
		return ground().get(id);
	}

	/** Lies on the ground and comes away with it: grass, flowers (a mod's too), mushrooms, snow, cactus, cane. A tree doesn't, nor a sapling or a crop. */
	public static boolean isCover(int id) {
		return cover().get(id);
	}

	static boolean isLiquid(int id) {
		return id != 0 && Block.BLOCKS[id] != null && Block.BLOCKS[id].material.isFluid();
	}
}
