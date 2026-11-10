package org.studioquire.faithful.stagehand;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Small named sets, to save a script some lines. Each entry is a name and a count, as {@code give} takes them, in the order they fill the inventory; armour goes on if that slot's free. */
final class Kits {
	static final Map<String, List<String>> ALL;

	static {
		Map<String, List<String>> all = new LinkedHashMap<>();
		all.put("wood", Arrays.asList("wooden_sword", "wooden_pickaxe", "wooden_axe", "wooden_shovel", "bread 8", "torch 16"));
		all.put("stone", Arrays.asList("leather_helmet", "leather_chestplate", "leather_leggings", "leather_boots", "stone_sword", "stone_pickaxe", "stone_axe", "stone_shovel", "bread 8", "torch 32"));
		all.put("iron", Arrays.asList("iron_helmet", "iron_chestplate", "iron_leggings", "iron_boots", "iron_sword", "iron_pickaxe", "iron_axe", "iron_shovel", "bow", "arrow 64", "cooked_porkchop 8", "torch 64"));
		all.put("diamond", Arrays.asList("diamond_helmet", "diamond_chestplate", "diamond_leggings", "diamond_boots", "diamond_sword", "diamond_pickaxe", "diamond_axe", "diamond_shovel", "bow", "arrow 64", "golden_apple 4", "cooked_porkchop 16", "torch 64"));
		all.put("builder", Arrays.asList("cobblestone 64", "planks 64", "glass 64", "torch 64", "ladder 64", "iron_pickaxe", "iron_shovel", "iron_axe", "water_bucket", "lava_bucket"));
		ALL = Collections.unmodifiableMap(all);
	}

	private Kits() {
	}
}
