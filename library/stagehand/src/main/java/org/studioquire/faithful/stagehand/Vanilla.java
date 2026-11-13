package org.studioquire.faithful.stagehand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.block.Block;
import net.minecraft.item.Item;

/**
 * Beta's blocks and items by name: Biny's field names in lower case, so {@code iron_sword} and {@code grass_block}. Written out, as the fields' own names don't exist in a running game (it's remapped).
 * A piston's head and its moving block aren't here: alone in a cell they're a broken piston, and nothing a test needs. Only touched once a script names something.
 */
final class Vanilla {
	private static final Map<String, Block> BLOCKS = new TreeMap<>();
	private static final Map<String, Item> ITEMS = new TreeMap<>();
	private static final Map<Integer, String> ITEM_NAMES = new HashMap<>();
	private static final Map<Integer, String> BLOCK_NAMES = new HashMap<>();

	static {
		block("stone", Block.STONE);
		block("grass_block", Block.GRASS_BLOCK);
		block("dirt", Block.DIRT);
		block("cobblestone", Block.COBBLESTONE);
		block("planks", Block.PLANKS);
		block("sapling", Block.SAPLING);
		block("bedrock", Block.BEDROCK);
		block("flowing_water", Block.FLOWING_WATER);
		block("water", Block.WATER);
		block("flowing_lava", Block.FLOWING_LAVA);
		block("lava", Block.LAVA);
		block("sand", Block.SAND);
		block("gravel", Block.GRAVEL);
		block("gold_ore", Block.GOLD_ORE);
		block("iron_ore", Block.IRON_ORE);
		block("coal_ore", Block.COAL_ORE);
		block("log", Block.LOG);
		block("leaves", Block.LEAVES);
		block("sponge", Block.SPONGE);
		block("glass", Block.GLASS);
		block("lapis_ore", Block.LAPIS_ORE);
		block("lapis_block", Block.LAPIS_BLOCK);
		block("dispenser", Block.DISPENSER);
		block("sandstone", Block.SANDSTONE);
		block("note_block", Block.NOTE_BLOCK);
		block("bed", Block.BED);
		block("powered_rail", Block.POWERED_RAIL);
		block("detector_rail", Block.DETECTOR_RAIL);
		block("sticky_piston", Block.STICKY_PISTON);
		block("cobweb", Block.COBWEB);
		block("grass", Block.GRASS);
		block("dead_bush", Block.DEAD_BUSH);
		block("piston", Block.PISTON);
		block("wool", Block.WOOL);
		block("dandelion", Block.DANDELION);
		block("rose", Block.ROSE);
		block("brown_mushroom", Block.BROWN_MUSHROOM);
		block("red_mushroom", Block.RED_MUSHROOM);
		block("gold_block", Block.GOLD_BLOCK);
		block("iron_block", Block.IRON_BLOCK);
		block("double_slab", Block.DOUBLE_SLAB);
		block("slab", Block.SLAB);
		block("bricks", Block.BRICKS);
		block("tnt", Block.TNT);
		block("bookshelf", Block.BOOKSHELF);
		block("mossy_cobblestone", Block.MOSSY_COBBLESTONE);
		block("obsidian", Block.OBSIDIAN);
		block("torch", Block.TORCH);
		block("fire", Block.FIRE);
		block("spawner", Block.SPAWNER);
		block("wooden_stairs", Block.WOODEN_STAIRS);
		block("chest", Block.CHEST);
		block("redstone_wire", Block.REDSTONE_WIRE);
		block("diamond_ore", Block.DIAMOND_ORE);
		block("diamond_block", Block.DIAMOND_BLOCK);
		block("crafting_table", Block.CRAFTING_TABLE);
		block("wheat", Block.WHEAT);
		block("farmland", Block.FARMLAND);
		block("furnace", Block.FURNACE);
		block("lit_furnace", Block.LIT_FURNACE);
		block("sign", Block.SIGN);
		block("door", Block.DOOR);
		block("ladder", Block.LADDER);
		block("rail", Block.RAIL);
		block("cobblestone_stairs", Block.COBBLESTONE_STAIRS);
		block("wall_sign", Block.WALL_SIGN);
		block("lever", Block.LEVER);
		block("iron_door", Block.IRON_DOOR);
		block("redstone_ore", Block.REDSTONE_ORE);
		block("lit_redstone_ore", Block.LIT_REDSTONE_ORE);
		// Beta's own names are the other way round from what a player holds: the torch that's crafted and placed is the lit block, and the plain-named one is the unlit
		// block no player ever has. Given as "redstone_torch", that one sat dark where it was placed until something beside it changed.
		block("unlit_redstone_torch", Block.REDSTONE_TORCH);
		block("redstone_torch", Block.LIT_REDSTONE_TORCH);
		BLOCKS.put("lit_redstone_torch", Block.LIT_REDSTONE_TORCH);
		block("snow", Block.SNOW);
		block("ice", Block.ICE);
		block("snow_block", Block.SNOW_BLOCK);
		block("cactus", Block.CACTUS);
		block("clay", Block.CLAY);
		block("sugar_cane", Block.SUGAR_CANE);
		block("jukebox", Block.JUKEBOX);
		block("fence", Block.FENCE);
		block("pumpkin", Block.PUMPKIN);
		block("netherrack", Block.NETHERRACK);
		block("soul_sand", Block.SOUL_SAND);
		block("glowstone", Block.GLOWSTONE);
		block("nether_portal", Block.NETHER_PORTAL);
		block("jack_o_lantern", Block.JACK_O_LANTERN);
		block("cake", Block.CAKE);
		block("repeater", Block.REPEATER);
		block("powered_repeater", Block.POWERED_REPEATER);
		block("locked_chest", Block.LOCKED_CHEST);
		block("trapdoor", Block.TRAPDOOR);
		item("iron_shovel", Item.IRON_SHOVEL);
		item("iron_pickaxe", Item.IRON_PICKAXE);
		item("iron_axe", Item.IRON_AXE);
		item("flint_and_steel", Item.FLINT_AND_STEEL);
		item("apple", Item.APPLE);
		item("bow", Item.BOW);
		item("arrow", Item.ARROW);
		item("coal", Item.COAL);
		item("diamond", Item.DIAMOND);
		item("iron_ingot", Item.IRON_INGOT);
		item("gold_ingot", Item.GOLD_INGOT);
		item("iron_sword", Item.IRON_SWORD);
		item("wooden_sword", Item.WOODEN_SWORD);
		item("wooden_shovel", Item.WOODEN_SHOVEL);
		item("wooden_pickaxe", Item.WOODEN_PICKAXE);
		item("wooden_axe", Item.WOODEN_AXE);
		item("stone_sword", Item.STONE_SWORD);
		item("stone_shovel", Item.STONE_SHOVEL);
		item("stone_pickaxe", Item.STONE_PICKAXE);
		item("stone_axe", Item.STONE_AXE);
		item("diamond_sword", Item.DIAMOND_SWORD);
		item("diamond_shovel", Item.DIAMOND_SHOVEL);
		item("diamond_pickaxe", Item.DIAMOND_PICKAXE);
		item("diamond_axe", Item.DIAMOND_AXE);
		item("stick", Item.STICK);
		item("bowl", Item.BOWL);
		item("mushroom_stew", Item.MUSHROOM_STEW);
		item("golden_sword", Item.GOLDEN_SWORD);
		item("golden_shovel", Item.GOLDEN_SHOVEL);
		item("golden_pickaxe", Item.GOLDEN_PICKAXE);
		item("golden_axe", Item.GOLDEN_AXE);
		item("string", Item.STRING);
		item("feather", Item.FEATHER);
		item("gunpowder", Item.GUNPOWDER);
		item("wooden_hoe", Item.WOODEN_HOE);
		item("stone_hoe", Item.STONE_HOE);
		item("iron_hoe", Item.IRON_HOE);
		item("diamond_hoe", Item.DIAMOND_HOE);
		item("golden_hoe", Item.GOLDEN_HOE);
		item("seeds", Item.SEEDS);
		item("wheat", Item.WHEAT);
		item("bread", Item.BREAD);
		item("leather_helmet", Item.LEATHER_HELMET);
		item("leather_chestplate", Item.LEATHER_CHESTPLATE);
		item("leather_leggings", Item.LEATHER_LEGGINGS);
		item("leather_boots", Item.LEATHER_BOOTS);
		item("chain_helmet", Item.CHAIN_HELMET);
		item("chain_chestplate", Item.CHAIN_CHESTPLATE);
		item("chain_leggings", Item.CHAIN_LEGGINGS);
		item("chain_boots", Item.CHAIN_BOOTS);
		item("iron_helmet", Item.IRON_HELMET);
		item("iron_chestplate", Item.IRON_CHESTPLATE);
		item("iron_leggings", Item.IRON_LEGGINGS);
		item("iron_boots", Item.IRON_BOOTS);
		item("diamond_helmet", Item.DIAMOND_HELMET);
		item("diamond_chestplate", Item.DIAMOND_CHESTPLATE);
		item("diamond_leggings", Item.DIAMOND_LEGGINGS);
		item("diamond_boots", Item.DIAMOND_BOOTS);
		item("golden_helmet", Item.GOLDEN_HELMET);
		item("golden_chestplate", Item.GOLDEN_CHESTPLATE);
		item("golden_leggings", Item.GOLDEN_LEGGINGS);
		item("golden_boots", Item.GOLDEN_BOOTS);
		item("flint", Item.FLINT);
		item("raw_porkchop", Item.RAW_PORKCHOP);
		item("cooked_porkchop", Item.COOKED_PORKCHOP);
		item("painting", Item.PAINTING);
		item("golden_apple", Item.GOLDEN_APPLE);
		item("sign", Item.SIGN);
		item("wooden_door", Item.WOODEN_DOOR);
		item("bucket", Item.BUCKET);
		item("water_bucket", Item.WATER_BUCKET);
		item("lava_bucket", Item.LAVA_BUCKET);
		item("minecart", Item.MINECART);
		item("saddle", Item.SADDLE);
		item("iron_door", Item.IRON_DOOR);
		item("redstone", Item.REDSTONE);
		item("snowball", Item.SNOWBALL);
		item("boat", Item.BOAT);
		item("leather", Item.LEATHER);
		item("milk_bucket", Item.MILK_BUCKET);
		item("brick", Item.BRICK);
		item("clay", Item.CLAY);
		item("sugar_cane", Item.SUGAR_CANE);
		item("paper", Item.PAPER);
		item("book", Item.BOOK);
		item("slimeball", Item.SLIMEBALL);
		item("chest_minecart", Item.CHEST_MINECART);
		item("furnace_minecart", Item.FURNACE_MINECART);
		item("egg", Item.EGG);
		item("compass", Item.COMPASS);
		item("fishing_rod", Item.FISHING_ROD);
		item("clock", Item.CLOCK);
		item("glowstone_dust", Item.GLOWSTONE_DUST);
		item("raw_fish", Item.RAW_FISH);
		item("cooked_fish", Item.COOKED_FISH);
		item("dye", Item.DYE);
		item("bone", Item.BONE);
		item("sugar", Item.SUGAR);
		item("cake", Item.CAKE);
		item("bed", Item.BED);
		item("repeater", Item.REPEATER);
		item("cookie", Item.COOKIE);
		item("map", Item.MAP);
		item("shears", Item.SHEARS);
		item("record_thirteen", Item.RECORD_THIRTEEN);
		item("record_cat", Item.RECORD_CAT);
	}

	private Vanilla() {
	}

	private static void block(String name, Block block) {
		BLOCKS.put(name, block);
		BLOCK_NAMES.put(block.id, name);
	}

	private static void item(String name, Item item) {
		ITEMS.put(name, item);
		ITEM_NAMES.put(item.id, name);
	}

	static Block block(String name) {
		return BLOCKS.get(name);
	}

	/** An item of its own, or a block's: {@code bed} is the item you carry, as {@code setblock bed} is the block. */
	static Item item(String name) {
		Item item = ITEMS.get(name);
		if (item != null) return item;
		Block block = BLOCKS.get(name);
		return block == null ? null : Item.ITEMS[block.id];
	}

	static String name(Block block) {
		return BLOCK_NAMES.get(block.id);
	}

	static String name(Item item) {
		String name = ITEM_NAMES.get(item.id);
		return name != null ? name : BLOCK_NAMES.get(item.id);
	}

	static List<String> blockNames() {
		return new ArrayList<>(BLOCKS.keySet());
	}

	static List<String> itemNames() {
		java.util.Set<String> all = new java.util.TreeSet<>(ITEMS.keySet());
		all.addAll(BLOCKS.keySet());
		return Collections.unmodifiableList(new ArrayList<>(all));
	}
}
