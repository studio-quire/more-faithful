package org.studioquire.faithful.alchemy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.studioquire.faithful.waterbottles.WaterBottles;
import org.studioquire.oldfaithful.core.api.Content;
import org.studioquire.oldfaithful.core.api.FaithfulMod;
import org.studioquire.oldfaithful.core.api.ItemEntry;
import org.studioquire.oldfaithful.core.api.Mod;
import org.studioquire.oldfaithful.core.api.Names;
import org.studioquire.oldfaithful.core.api.Recipes;
import org.studioquire.oldfaithful.core.api.Ticks;

/**
 * Gild a thing, then brew it. Gold is the limit: a potion's price is its gilded things' ingots. Each family is one gilded thing and a potion made from one, two or three of them with a water bottle,
 * each one worth the same.
 */
public final class Alchemy implements FaithfulMod {
	/**
	 * What one, two and three of a gilded thing are worth, by family: hearts healed, minutes of haste or feather, and how tough a ward's set is, in damage it takes before it's gone. The strongest
	 * health fills the bar (the user, 2026-11-09), so the third one's extra is what it wastes when less was needed. Every ward fills the armour bar (the user, 2026-11-09), and its toughness is set so
	 * that, worn alone against a zombie's hits on Normal (5 a hit), it lasts 3, 5 or 9 hits and saves about 4, 6 and 10 hearts, near what the health potions heal.
	 */
	public static final int[] HEARTS = {3, 6, 10}, MINUTES = {4, 8, 12}, WARD = {12, 25, 45};
	/** Haste: walking and mining this much faster. */
	public static final float HASTE = 1.2F;
	/** Feather: a fall hurts this many times less. */
	public static final int FEATHER = 3;
	/** The Blue Flowers mod's flower, gilded into the Golden Bluebell when that mod is on. */
	public static final String BLUEBELL = "blue-flowers:blue_flower";

	public static ItemEntry<Item> goldenRose, goldenDandelion, goldenBluebell, goldenFeather;
	/** Weak, plain and strong, each family in that order. */
	public static final List<ItemEntry<Potion>> health = new ArrayList<>(), haste = new ArrayList<>(), ward = new ArrayList<>(), feather = new ArrayList<>();
	public static ItemEntry<Potion> weakPotion, healthPotion, strongPotion;
	public static Timed hasted, feathered;

	@Override
	public void content(Content c) {
		goldenRose = gilded(c, "golden_rose", Item.ITEMS[Block.ROSE.id]);
		goldenDandelion = gilded(c, "golden_dandelion", Item.ITEMS[Block.DANDELION.id]);
		// Declared with or without Blue Flowers, so a world keeps its potions when that mod is off and the set's content is the same either way; only the gilding needs the flower.
		goldenBluebell = gilded(c, "golden_bluebell", Item.ITEMS[Block.ROSE.id]);
		goldenFeather = gilded(c, "golden_feather", Item.FEATHER);
		family(c, "health", health, (player, size) -> player.health < Potion.FULL, (player, size) -> player.heal(HEARTS[size] * 2));
		family(c, "haste", haste, (player, size) -> !hasted.full(player), (player, size) -> hasted.add(player, MINUTES[size]));
		family(c, "ward", ward, (player, size) -> !Ward.full(player), (player, size) -> Ward.put(player, WARD[size]));
		family(c, "feather", feather, (player, size) -> !feathered.full(player), (player, size) -> feathered.add(player, MINUTES[size]));
		weakPotion = health.get(0);
		healthPotion = health.get(1);
		strongPotion = health.get(2);
	}

	/** What a vanilla reader sees is the thing before it was gilded: nothing it didn't have. */
	private static ItemEntry<Item> gilded(Content c, String name, Item before) {
		return c.item(name, s -> new Item(s.id()) {}.setTextureId(s.texture(name))).standIn(before);
	}

	/** {@code weak_<name>_potion}, {@code <name>_potion} and {@code strong_<name>_potion}. A potion is its empty vessel to a vanilla reader, as Water Bottles' are. */
	private static void family(Content c, String name, List<ItemEntry<Potion>> sizes, Potion.Wanted wanted, Potion.Drink drink) {
		String[] prefixes = {"weak_", "", "strong_"};
		for (int size = 0; size < 3; size++) {
			String id = prefixes[size] + name + "_potion";
			int which = size;
			sizes.add(c.item(id, s -> new Potion(s.id(), s.texture(id), which, wanted, drink)).standIn(Item.BOWL));
		}
	}

	@Override
	public void init(Mod mod) {
		hasted = new Timed(mod, "haste");
		feathered = new Timed(mod, "feather");
		Ward.init(mod);
		Ticks.onServer(() -> {
			hasted.tick();
			feathered.tick();
		});

		gild(goldenRose.get(), Block.ROSE);
		gild(goldenDandelion.get(), Block.DANDELION);
		Block bluebell = Names.block(BLUEBELL);
		if (bluebell != null) gild(goldenBluebell.get(), bluebell);
		gild(goldenFeather.get(), Item.FEATHER);
		brew(health, goldenRose.get());
		brew(haste, goldenDandelion.get());
		brew(ward, goldenBluebell.get());
		brew(feather, goldenFeather.get());
	}

	/** Four gold ingots in a plus around {@code thing}, the clock's shape, which only fits at the bench. */
	private static void gild(Item golden, Object thing) {
		Recipes.shaped(new ItemStack(golden), " G ", "GTG", " G ", 'G', Item.GOLD_INGOT, 'T', thing);
	}

	/** A water bottle and one, two or three of the gilded thing, anywhere they fit: at most four cells, so the inventory's grid too. */
	private static void brew(List<ItemEntry<Potion>> sizes, Item gilded) {
		for (int size = 0; size < 3; size++) {
			Object[] ingredients = new Object[size + 2];
			Arrays.fill(ingredients, gilded);
			ingredients[0] = WaterBottles.waterBottle.get();
			Recipes.shapeless(new ItemStack(sizes.get(size).get()), ingredients);
		}
	}

	/** Haste's factor for this player now, on either side: a client hears whether it's running, and leans on that for its walking and the pace of its mining. */
	public static float haste(PlayerEntity player) {
		return hasted != null && hasted.running(player) ? HASTE : 1.0F;
	}
}
