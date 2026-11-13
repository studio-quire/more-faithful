package org.studioquire.faithful.golems;

import java.util.BitSet;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import org.studioquire.oldfaithful.core.api.Tags;

/**
 * What the land is made of, to a pickaxe golem: stone, dirt, grass, sand, sandstone, gravel, clay, netherrack, soul sand, and ores, a mod's too through {@code c:ores/}. Not cobble, which
 * is what mining leaves and building uses, nor anything else built, cut or growing. And what lies on the land and comes away with it: plants and snow.
 */
public final class Ground {
	private static BitSet ground;
	private static final BitSet COVER = new BitSet();

	static {
		for (Block b : new Block[] {Block.GRASS, Block.DEAD_BUSH, Block.DANDELION, Block.ROSE, Block.BROWN_MUSHROOM, Block.RED_MUSHROOM, Block.SNOW, Block.CACTUS, Block.SUGAR_CANE}) COVER.set(b.id);
	}

	private Ground() {
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

	/** Lies on the ground and comes away with it: grass, flowers, mushrooms, snow, cactus, cane. A tree doesn't. */
	public static boolean isCover(int id) {
		return COVER.get(id);
	}

	static boolean isLiquid(int id) {
		return id != 0 && Block.BLOCKS[id] != null && Block.BLOCKS[id].material.isFluid();
	}
}
