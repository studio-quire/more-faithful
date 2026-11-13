package org.studioquire.faithful.blueflowers;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.WoolBlock;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.studioquire.oldfaithful.core.api.BlockEntry;
import org.studioquire.oldfaithful.core.api.Chunks;
import org.studioquire.oldfaithful.core.api.Content;
import org.studioquire.oldfaithful.core.api.FaithfulMod;
import org.studioquire.oldfaithful.core.api.Growth;
import org.studioquire.oldfaithful.core.api.Mod;
import org.studioquire.oldfaithful.core.api.Recipes;
import org.studioquire.oldfaithful.core.api.Worldgen;

/**
 * The lupin, a third flower, and the blue dye in lapis lazuli's place: in patches in new terrain, as beta's dandelions grow, and from bonemeal on grass anywhere, as beta's two
 * flowers come. With this mod on there's no lapis lazuli in a world at all, ore or block: no ore is laid in new land ({@code mixin.LapisVeinMixin}), the block has no recipe either way, and what
 * a world holds from before goes as it's read: ore to stone and blocks to blue wool where they stand ({@link #lapisGoes}), and the same for the stacks of them in anything saved
 * ({@code mixin.ItemStackMixin}). The dye itself is untouched: it's the lupin's now.
 */
public final class BlueFlowers implements FaithfulMod {
	/** {@code DyeItem.names}' blue: beta's Lapis Lazuli, shown as Lupin Blue. The same item as ever, so wool, sheep and every mix beta makes of it are beta's own recipes. */
	public static final int BLUE = 4;
	/** Blue wool's meta, and its item's damage: what a lapis lazuli block becomes, having been built with for its colour. */
	public static final int BLUE_WOOL = WoolBlock.getBlockMeta(BLUE);

	public static BlockEntry<BlueFlower> flower;
	/** The dye's own sprite, for {@code mixin.DyeItemMixin}. */
	public static int dyeSprite;

	@Override
	public void content(Content c) {
		flower = c.block("blue_flower", s -> new BlueFlower(s.id(), s.texture("blue_flower"))).standIn(Block.ROSE);
		dyeSprite = c.itemTexture("lupin_blue");
	}

	@Override
	public void init(Mod mod) {
		// Two, as a rose or a dandelion makes.
		Recipes.shapeless(new ItemStack(Item.DYE, 2, BLUE), flower.get());
		// Neither of the block's recipes: nine of the dye into it, and it into nine. The second by its count, as a lupin's two are the same dye.
		Recipes.modify(r -> {
			r.removeOutput(Block.LAPIS_BLOCK);
			r.crafting().removeIf(recipe -> {
				ItemStack made = recipe.getOutput();
				return made != null && made.getItem() == Item.DYE && made.getDamage() == BLUE && made.count == 9;
			});
		});
		Worldgen.decorate("blue-flowers:patches", Patches::grow);
		Growth.onBonemeal(BlueFlowers::bonemeal);
		Chunks.onLoaded(BlueFlowers::lapisGoes);
	}

	/**
	 * Lapis lazuli in a chunk that has just joined the world: ore becomes the stone it was laid in (there is no flower ore) and a block becomes blue wool (lapis goes
	 * entirely; wool, as a placed block was built with for its colour), for good: the chunk is saved so. Heard where the world runs, before the chunk has gone to anyone, at every
	 * height and in every dimension, so none is left standing wherever it came from. Every chunk is looked through at every load, a new one too, since a mark for "done" would be mod data in every
	 * chunk of a world, which otherwise holds none where no lupin grows; it's a read of each cell, which doesn't show in the time new land takes to make (5,184 chunks on the real server with and
	 * without it, 2026-11-12). All four blocks weigh the same to light, so the game's own write changes nothing else: the light it asks to be looked at again comes out as it was.
	 */
	public static void lapisGoes(Chunk chunk) {
		int ore = Block.LAPIS_ORE.id, block = Block.LAPIS_BLOCK.id;
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				for (int y = 0; y < 128; y++) {
					int id = chunk.getBlockId(x, y, z);
					if (id == ore) chunk.setBlock(x, y, z, Block.STONE.id);
					else if (id == block) chunk.setBlock(x, y, z, Block.WOOL.id, BLUE_WOOL);
				}
			}
		}
	}

	/**
	 * Beta's own walk from the grass clicked, and a rose's odds at each cell the first time it's reached, since a cell beta has filled takes nothing more. Before beta's walk, which comes
	 * along to fill the rest: not {@code handled}, so the bonemeal goes on to do all it always does. In any biome, as beta's two flowers come.
	 */
	private static void bonemeal(Growth.Bonemeal event) {
		if (event.block != Block.GRASS_BLOCK.id || event.cancelled()) return;
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
