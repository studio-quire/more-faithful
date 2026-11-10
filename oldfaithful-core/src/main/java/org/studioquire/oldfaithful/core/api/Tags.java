package org.studioquire.oldfaithful.core.api;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.studioquire.oldfaithful.core.impl.Core;

/**
 * Shared names for sets of items, so two mods' planks meet without knowing each other: {@code Tags.add("c:planks", block)}, and a
 * recipe that asks for {@code "c:planks"} takes any of them. Names come from {@code library/tags.txt}, which the harness checks against,
 * so two mods don't invent two spellings. Vanilla's own materials are in from the start. Add from {@code init}.
 * <p>
 * A tag with nothing in it is no error: a recipe asking for one never matches, and {@link #items} is empty. So a mod can accept a material without knowing whether anyone provides it, and the harness
 * notes it (it doesn't fail). A tag shares what exists; it doesn't make a material exist. A material has one owner mod (the authoring guide says what to do about that).
 */
public final class Tags {
	/** Any damage. */
	private static final int ANY = -1;
	private static final Map<String, List<int[]>> TAGS = new TreeMap<>();
	private static final Map<String, Set<String>> WHO = new TreeMap<>();

	static {
		add("c:ingots/iron", Item.IRON_INGOT);
		add("c:ingots/gold", Item.GOLD_INGOT);
		add("c:gems/diamond", Item.DIAMOND);
		add("c:gems/lapis", new ItemStack(Item.DYE, 1, 4));
		add("c:dusts/redstone", Item.REDSTONE);
		add("c:dusts/glowstone", Item.GLOWSTONE_DUST);
		add("c:ores/iron", Block.IRON_ORE);
		add("c:ores/gold", Block.GOLD_ORE);
		add("c:ores/diamond", Block.DIAMOND_ORE);
		add("c:ores/coal", Block.COAL_ORE);
		add("c:ores/redstone", Block.REDSTONE_ORE);
		add("c:ores/lapis", Block.LAPIS_ORE);
		add("c:coal", new ItemStack(Item.COAL, 1, 0));
		add("c:charcoal", new ItemStack(Item.COAL, 1, 1));
		add("c:planks", Block.PLANKS);
		add("c:logs", Block.LOG);
		add("c:stone", Block.STONE);
		add("c:cobblestone", Block.COBBLESTONE);
		add("c:sand", Block.SAND);
		add("c:gravel", Block.GRAVEL);
		add("c:dirt", Block.DIRT);
		add("c:glass", Block.GLASS);
		add("c:wool", Block.WOOL);
		add("c:sticks", Item.STICK);
		add("c:string", Item.STRING);
		add("c:leather", Item.LEATHER);
		add("c:feathers", Item.FEATHER);
		add("c:storage_blocks/iron", Block.IRON_BLOCK);
		add("c:storage_blocks/gold", Block.GOLD_BLOCK);
		add("c:storage_blocks/diamond", Block.DIAMOND_BLOCK);
		add("c:storage_blocks/lapis", Block.LAPIS_BLOCK);
		add("c:meats/raw", Item.RAW_PORKCHOP);
		add("c:meats/cooked", Item.COOKED_PORKCHOP);
		add("c:seeds/wheat", Item.SEEDS);
		add("c:crops/wheat", Item.WHEAT);
		add("c:fruits/apple", Item.APPLE);
	}

	private Tags() {
	}

	private static List<int[]> entries(String tag) {
		if (tag.indexOf(':') <= 0) throw new IllegalArgumentException("A tag has a namespace: " + tag);
		WHO.computeIfAbsent(tag, t -> new TreeSet<>()).add(Core.currentMod());
		return TAGS.computeIfAbsent(tag, t -> new ArrayList<>());
	}

	/** A tag used before anything's in it: a recipe that asks for one does this, so the vocabulary check sees the name. */
	public static void declare(String tag) {
		entries(tag);
	}

	/** The item, at any damage. */
	public static void add(String tag, Item item) {
		entries(tag).add(new int[] {item.id, ANY});
	}

	/** The block's item, at any damage. */
	public static void add(String tag, Block block) {
		entries(tag).add(new int[] {block.id, ANY});
	}

	/** The stack's item at its damage. */
	public static void add(String tag, ItemStack stack) {
		entries(tag).add(new int[] {stack.itemId, stack.getDamage()});
	}

	/** Takes an item out of a tag, at every damage: for vanilla's or another mod's. */
	public static void remove(String tag, Item item) {
		List<int[]> entries = TAGS.get(tag);
		if (entries == null) return;
		for (Iterator<int[]> i = entries.iterator(); i.hasNext(); ) {
			if (i.next()[0] == item.id) i.remove();
		}
	}

	public static boolean has(String tag, ItemStack stack) {
		List<int[]> entries = TAGS.get(tag);
		if (entries == null || stack == null) return false;
		for (int[] e : entries) {
			if (e[0] == stack.itemId && (e[1] == ANY || e[1] == stack.getDamage())) return true;
		}
		return false;
	}

	/** What's in a tag, one stack each; a damage of -1 means any. */
	public static List<ItemStack> items(String tag) {
		List<ItemStack> stacks = new ArrayList<>();
		List<int[]> entries = TAGS.get(tag);
		if (entries != null) {
			for (int[] e : entries) stacks.add(new ItemStack(e[0], 1, e[1]));
		}
		return stacks;
	}

	/** The mods that named a tag. For the harness, to say whose spelling it is. */
	public static Set<String> who(String tag) {
		return WHO.get(tag);
	}

	/** Every tag anything has been added to or asked for. For the harness's check against the vocabulary. */
	public static Set<String> names() {
		return TAGS.keySet();
	}
}
