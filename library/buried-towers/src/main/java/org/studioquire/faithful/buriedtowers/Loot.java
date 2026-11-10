package org.studioquire.faithful.buriedtowers;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.studioquire.oldfaithful.core.api.Worldgen;

/**
 * What's in a chest, by what the chest is for and how deep it lies. Beta's items only, nothing from the Nether, and nothing a player couldn't have put there. Near the top, plenty of everyday things:
 * food, coal, string, seeds, bones, flint, arrows, paper, now and then a saddle or a few ingots, so going in a little way pays. Gear and the rare valuables lie deeper, the chests getting fuller with
 * depth; the vault is the best of it without being a hoard: no blocks of metal, a handful of diamonds, and only rarely a diamond tool or piece of armour (user, 2026-11-07).
 */
public final class Loot {
	private Loot() {
	}

	private interface Draw {
		ItemStack from(Random r);
	}

	/** A draw and how often it comes up beside the others in its list. */
	private static final class Entry {
		final double weight;
		final Draw draw;

		Entry(double weight, Draw draw) {
			this.weight = weight;
			this.draw = draw;
		}
	}

	private static Entry e(double weight, Draw draw) {
		return new Entry(weight, draw);
	}

	private static Draw item(Item item, int low, int high) {
		return r -> new ItemStack(item, low + r.nextInt(high - low + 1));
	}

	private static Draw block(Block block, int low, int high) {
		return r -> new ItemStack(block, low + r.nextInt(high - low + 1));
	}

	private static Draw one(Item item) {
		return item(item, 1, 1);
	}

	private static Draw ink(int low, int high) {
		return r -> new ItemStack(Item.DYE, low + r.nextInt(high - low + 1), 0);
	}

	/** A worn tool or a bit of armour: it's been lying here a long time. */
	private static Draw worn(Item item) {
		return r -> {
			ItemStack stack = new ItemStack(item);
			int most = item.getMaxDamage();
			if (most > 0) stack.setDamage(most / 4 + r.nextInt(most / 2));
			return stack;
		};
	}

	private static void fill(ChestBlockEntity chest, Random r, int draws, Entry... list) {
		double total = 0;
		for (Entry entry : list) total += entry.weight;
		for (int i = 0; i < draws; i++) {
			double roll = r.nextDouble() * total;
			Entry pick = list[list.length - 1];
			for (Entry entry : list) {
				roll -= entry.weight;
				if (roll < 0) {
					pick = entry;
					break;
				}
			}
			ItemStack stack = pick.draw.from(r);
			if (stack == null) continue;
			// Food and the like stack to one in beta: three loaves are three slots, the ones past the first in empty ones.
			int most = stack.getMaxCount(), extra = stack.count - most;
			if (extra > 0) stack.count = most;
			chest.setStack(r.nextInt(chest.size()), stack);
			for (; extra > 0; extra -= most) sure(chest, r, new ItemStack(stack.itemId, Math.min(most, extra), stack.getDamage()));
		}
	}

	/** Into an empty slot, so nothing drawn covers it or is covered. */
	private static void sure(ChestBlockEntity chest, Random r, ItemStack stack) {
		int empty = 0;
		for (int i = 0; i < chest.size(); i++) if (chest.getStack(i) == null) empty++;
		if (empty == 0) return;
		int pick = r.nextInt(empty);
		for (int i = 0; i < chest.size(); i++) {
			if (chest.getStack(i) == null && pick-- == 0) {
				chest.setStack(i, stack);
				return;
			}
		}
	}

	/** Every kind of chest the floors ask for, for the checks. */
	public static final String[] KINDS = {"lookout", "scraps", "kitchen", "bunk", "stores", "cell", "prisoner", "books", "maps", "hall", "arms", "crypt", "sunken", "pit", "cart", "granary", "farm", "nest", "hidden", "vault", "vault-best"};

	/** {@code depth} is the floor's, 0 at the land and 1 at the vault. */
	public static void fill(ChestBlockEntity chest, String kind, double depth, Random r) {
		// Deeper chests hold more, and the deep-only things come in by degrees.
		int more = (int) Math.round(Math.max(0, depth) * 3);
		double deep = Math.max(0, depth - 0.5) * 2;
		switch (kind) {
			case "lookout":
				fill(chest, r, 5, e(3, item(Item.ARROW, 3, 10)), e(2, item(Item.STRING, 1, 3)), e(2, item(Item.FEATHER, 1, 4)), e(2, item(Item.FLINT, 1, 3)), e(2, item(Item.BREAD, 1, 3)),
						e(1, item(Item.APPLE, 1, 2)), e(1, item(Item.COAL, 1, 4)), e(1, worn(Item.BOW)), e(0.3, one(Item.SADDLE)));
				break;
			case "scraps":
				fill(chest, r, 5, e(2, item(Item.STICK, 2, 6)), e(2, item(Item.COAL, 2, 6)), e(2, item(Item.SEEDS, 2, 8)), e(1.5, item(Item.WHEAT, 1, 4)), e(1.5, item(Item.STRING, 1, 3)),
						e(1.5, item(Item.BONE, 1, 4)), e(1, item(Item.FLINT, 1, 2)), e(1, item(Item.APPLE, 1, 3)), e(1, item(Item.BREAD, 1, 2)), e(0.6, item(Item.IRON_INGOT, 1, 2)),
						e(0.3, one(Item.SADDLE)), e(0.5, worn(Item.WOODEN_PICKAXE)));
				break;
			case "kitchen":
				fill(chest, r, 6 + more, e(2, item(Item.BREAD, 1, 4)), e(1.5, item(Item.COOKED_PORKCHOP, 1, 3)), e(1.5, item(Item.COOKED_FISH, 1, 3)), e(1.5, item(Item.WHEAT, 2, 8)),
						e(1, item(Item.SUGAR, 1, 4)), e(1, item(Item.APPLE, 1, 3)), e(1, item(Item.EGG, 1, 4)), e(1.5, item(Item.COAL, 2, 6)), e(0.7, item(Item.BOWL, 1, 2)), e(0.5, one(Item.BUCKET)),
						e(0.4, one(Item.MILK_BUCKET)));
				break;
			case "bunk":
				fill(chest, r, 4 + more, e(2, item(Item.ARROW, 4, 12)), e(1.5, item(Item.BREAD, 1, 3)), e(1.5, item(Item.COAL, 1, 4)), e(1.5, item(Item.STRING, 1, 3)), e(1, item(Item.LEATHER, 1, 3)),
						e(1, item(Item.FLINT, 1, 2)), e(0.7, item(Item.IRON_INGOT, 1, 2)), e(0.5, worn(Item.LEATHER_BOOTS)), e(0.5, worn(Item.LEATHER_HELMET)), e(0.4, worn(Item.STONE_SWORD)),
						e(0.3, one(Item.SADDLE)));
				break;
			case "stores":
				fill(chest, r, 7 + more, e(2, item(Item.WHEAT, 4, 12)), e(1.5, item(Item.SEEDS, 3, 10)), e(1.5, item(Item.BREAD, 1, 4)), e(1, item(Item.APPLE, 1, 4)), e(1, item(Item.SUGAR_CANE, 1, 6)),
						e(2, item(Item.COAL, 3, 10)), e(1, block(Block.TORCH, 2, 8)), e(1, item(Item.STRING, 1, 4)), e(1, item(Item.BONE, 2, 6)), e(1, item(Item.LEATHER, 1, 4)), e(0.7, item(Item.CLAY, 2, 6)),
						e(0.8, item(Item.IRON_INGOT, 1, 3)), e(0.4, one(Item.MUSHROOM_STEW)), e(0.4, one(Item.SADDLE)));
				break;
			case "cell":
				fill(chest, r, 3, e(2, item(Item.BONE, 1, 5)), e(1.5, item(Item.STRING, 1, 3)), e(1.5, item(Item.FLINT, 1, 3)), e(1, item(Item.COAL, 1, 3)), e(1, item(Item.BREAD, 1, 2)),
						e(0.5, worn(Item.WOODEN_SHOVEL)));
				break;
			case "prisoner":
				fill(chest, r, 5 + more, e(1, worn(Item.STONE_PICKAXE)), e(0.5, worn(Item.IRON_SHOVEL)), e(2, item(Item.BONE, 2, 6)), e(1.5, item(Item.COAL, 2, 5)), e(1.5, item(Item.FLINT, 2, 4)),
						e(1, item(Item.IRON_INGOT, 1, 3)), e(0.4, one(Item.CLOCK)), e(1, item(Item.STRING, 1, 3)));
				break;
			case "books":
				fill(chest, r, 6, e(2.5, item(Item.PAPER, 2, 8)), e(1, item(Item.BOOK, 1, 2)), e(1.5, item(Item.FEATHER, 2, 5)), e(1.5, ink(1, 4)), e(1, item(Item.REDSTONE, 1, 4)),
						e(0.4, one(Item.COMPASS)), e(0.3, one(Item.CLOCK)), e(1, item(Item.SUGAR_CANE, 1, 4)));
				break;
			case "maps":
				fill(chest, r, 4, e(1, one(Item.COMPASS)), e(2, item(Item.PAPER, 2, 6)), e(1, item(Item.FEATHER, 1, 3)), e(0.6, item(Item.GOLD_INGOT, 1, 2)));
				break;
			case "hall":
				fill(chest, r, 5 + more, e(2, item(Item.IRON_INGOT, 1, 4)), e(1, item(Item.GOLD_INGOT, 1, 3)), e(1.5, item(Item.REDSTONE, 2, 8)), e(1.5, item(Item.COAL, 3, 8)), e(1.5, item(Item.ARROW, 4, 12)),
						e(1, item(Item.GUNPOWDER, 1, 4)), e(1, item(Item.BREAD, 1, 3)), e(0.5, one(Item.BUCKET)), e(0.3 + deep * 0.3, worn(Item.IRON_SWORD)), e(0.3 + deep * 0.3, worn(Item.IRON_HELMET)),
						e(0.2 + deep * 0.3, worn(Item.IRON_LEGGINGS)), e(deep * 0.3, one(Item.DIAMOND)));
				break;
			case "arms":
				fill(chest, r, 4 + more, e(1, worn(Item.IRON_SWORD)), e(0.8, worn(Item.IRON_HELMET)), e(0.4, worn(Item.IRON_CHESTPLATE)), e(0.8, worn(Item.CHAIN_LEGGINGS)), e(0.8, worn(Item.CHAIN_BOOTS)),
						e(1, worn(Item.BOW)), e(2, item(Item.ARROW, 8, 24)), e(1.5, item(Item.IRON_INGOT, 1, 4)), e(1, item(Item.GUNPOWDER, 1, 4)), e(1, item(Item.FLINT, 2, 5)));
				break;
			case "crypt":
				fill(chest, r, 5 + more, e(2, item(Item.GOLD_INGOT, 1, 4)), e(2, item(Item.BONE, 3, 9)), e(1.5, item(Item.REDSTONE, 2, 6)), e(0.6, worn(Item.GOLDEN_HELMET)), e(0.6, worn(Item.GOLDEN_SWORD)),
						e(0.3, one(Item.GOLDEN_APPLE)), e(0.2 + deep * 0.3, one(Item.DIAMOND)), e(0.3, one(Item.RECORD_CAT)), e(1, item(Item.STRING, 1, 4)));
				break;
			case "sunken":
				fill(chest, r, 5 + more, e(2, item(Item.IRON_INGOT, 2, 5)), e(1, item(Item.GOLD_INGOT, 1, 3)), e(0.5, worn(Item.IRON_PICKAXE)), e(1, one(Item.FISHING_ROD)), e(1.5, item(Item.RAW_FISH, 1, 4)),
						e(1, ink(1, 3)), e(1, item(Item.CLAY, 2, 6)), e(0.5, one(Item.SADDLE)), e(0.3, one(Item.RECORD_THIRTEEN)));
				break;
			case "pit":
				fill(chest, r, 5 + more, e(2, item(Item.IRON_INGOT, 2, 6)), e(1, item(Item.GOLD_INGOT, 1, 4)), e(1.5, item(Item.GUNPOWDER, 2, 6)), e(1.5, item(Item.REDSTONE, 2, 6)),
						e(1, item(Item.COAL, 4, 10)), e(0.6, worn(Item.IRON_PICKAXE)), e(0.4, one(Item.WATER_BUCKET)), e(0.4, block(Block.TNT, 1, 2)), e(0.3, one(Item.MINECART)),
						e(0.2 + deep * 0.3, one(Item.DIAMOND)));
				break;
			case "cart":
				// By the top of a cart run: the cart to ride it, and what was left beside it.
				fill(chest, r, 2, e(2, item(Item.COAL, 1, 4)), e(1, item(Item.STRING, 1, 2)), e(1, item(Item.BREAD, 1, 2)), e(0.5, item(Item.IRON_INGOT, 1, 2)));
				sure(chest, r, new ItemStack(Item.MINECART, 1));
				break;
			case "granary":
				fill(chest, r, 6 + more, e(3, item(Item.WHEAT, 4, 16)), e(2.5, item(Item.SEEDS, 4, 16)), e(1.5, item(Item.BREAD, 1, 4)), e(1, item(Item.SUGAR, 1, 4)), e(0.8, item(Item.STRING, 1, 3)),
						e(0.6, item(Item.BONE, 1, 3)), e(0.4, worn(Item.WOODEN_HOE)), e(0.3, worn(Item.STONE_HOE)), e(0.3, one(Item.BUCKET)));
				break;
			case "farm":
				fill(chest, r, 5 + more, e(2.5, item(Item.BOWL, 1, 4)), e(1.5, one(Item.MUSHROOM_STEW)), e(2, item(Item.BONE, 2, 6)), e(1.5, item(Item.SEEDS, 2, 8)), e(1, item(Item.COAL, 1, 4)),
						e(0.5, worn(Item.STONE_HOE)), e(0.3, worn(Item.IRON_HOE)), e(0.4, one(Item.BUCKET)));
				break;
			case "nest":
				// What the spiders left of what they caught.
				fill(chest, r, 5 + more, e(3, item(Item.STRING, 2, 8)), e(2.5, item(Item.BONE, 2, 6)), e(1, item(Item.ARROW, 2, 10)), e(1, item(Item.FEATHER, 1, 4)), e(0.8, item(Item.GUNPOWDER, 1, 3)),
						e(0.5, worn(Item.BOW)), e(0.4, worn(Item.LEATHER_HELMET)), e(0.3, worn(Item.IRON_BOOTS)), e(0.6, item(Item.IRON_INGOT, 1, 2)), e(0.2 + deep * 0.2, item(Item.GOLD_INGOT, 1, 2)));
				break;
			case "hidden":
				// Someone's stash in the rock beside a way down, for whoever noticed the way ran long: more than the floors round it hold, no filler, short of the vault.
				fill(chest, r, 6 + more, e(2, item(Item.IRON_INGOT, 2, 5)), e(1.5, item(Item.GOLD_INGOT, 1, 4)), e(1.2, item(Item.REDSTONE, 3, 8)), e(1, item(Item.GUNPOWDER, 2, 5)),
						e(1, item(Item.ARROW, 6, 16)), e(0.5, one(Item.BUCKET)), e(0.4, one(Item.SADDLE)), e(0.3, one(Item.COMPASS)), e(0.3, one(Item.CLOCK)), e(0.5, worn(Item.IRON_PICKAXE)),
						e(0.4, worn(Item.IRON_SWORD)), e(0.3, worn(Item.IRON_HELMET)), e(0.2, one(Item.GOLDEN_APPLE)), e(0.15, one(Item.RECORD_THIRTEEN)), e(0.15, one(Item.RECORD_CAT)),
						e(0.2 + deep * 0.6, one(Item.DIAMOND)));
				// And always something worth the digging: a diamond one time in three, or a few gold.
				sure(chest, r, r.nextInt(3) == 0 ? new ItemStack(Item.DIAMOND, 1) : new ItemStack(Item.GOLD_INGOT, 2 + r.nextInt(3)));
				break;
			case "vault":
				fill(chest, r, 7, e(2, item(Item.GOLD_INGOT, 1, 4)), e(2, item(Item.IRON_INGOT, 2, 5)), e(1.5, item(Item.REDSTONE, 4, 12)), e(1, item(Item.GUNPOWDER, 2, 6)), e(1, item(Item.BREAD, 1, 3)),
						e(0.5, item(Item.DIAMOND, 1, 2)), e(0.3, one(Item.GOLDEN_APPLE)), e(0.5, one(Item.SADDLE)), e(0.5, worn(Item.IRON_CHESTPLATE)), e(0.4, worn(Item.IRON_PICKAXE)),
						e(0.08, worn(Item.DIAMOND_SWORD)), e(0.2, one(Item.RECORD_CAT)));
				break;
			case "vault-best":
				fill(chest, r, 8, e(0.8, item(Item.DIAMOND, 1, 2)), e(2, item(Item.GOLD_INGOT, 2, 5)), e(2, item(Item.IRON_INGOT, 2, 6)), e(1.5, item(Item.REDSTONE, 4, 12)), e(0.5, one(Item.GOLDEN_APPLE)),
						e(0.3, one(Item.RECORD_THIRTEEN)), e(0.3, one(Item.RECORD_CAT)), e(0.5, one(Item.SADDLE)), e(0.12, worn(Item.DIAMOND_PICKAXE)), e(0.08, worn(Item.DIAMOND_CHESTPLATE)),
						e(0.6, worn(Item.IRON_SWORD)));
				// And always a few more, so no vault comes out lean (user, 2026-11-07): about 4.9 a vault in all, never fewer than 2.
				sure(chest, r, new ItemStack(Item.DIAMOND, 2 + r.nextInt(2)));
				break;
			default:
				for (int i = 0; i < 8; i++) {
					ItemStack stack = Worldgen.dungeonChestItem(r);
					if (stack != null) chest.setStack(r.nextInt(chest.size()), stack);
				}
		}
	}
}
