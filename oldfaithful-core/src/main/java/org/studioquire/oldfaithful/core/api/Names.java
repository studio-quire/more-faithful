package org.studioquire.oldfaithful.core.api;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import org.studioquire.oldfaithful.core.impl.content.ContentRegistry;

/**
 * Mods' blocks and items by the names they declared, {@code <mod id>:<name>}, for whatever takes a name from outside: a command, a config, a tool. Old names through {@code alias} still answer.
 * Vanilla's have no names here. From {@code init} on; a mod that needs one of its own has its entry, and this is for anyone else's.
 */
public final class Names {
	private Names() {
	}

	/** The block of this name, or null. */
	public static Block block(String name) {
		int id = ContentRegistry.blockId(name);
		return id < 0 ? null : Block.BLOCKS[id];
	}

	/** The item of this name, a block's own item included, or null. A block declared with {@code noItem} has none. */
	public static Item item(String name) {
		int id = ContentRegistry.itemId(name);
		return id < 0 ? null : Item.ITEMS[id];
	}

	/** The name of a mod's block, or null for vanilla's. */
	public static String of(Block block) {
		return ContentRegistry.blockName(block.id);
	}

	/** The name of a mod's item, or null for vanilla's. */
	public static String of(Item item) {
		return ContentRegistry.itemName(item.id);
	}

	/** Every mod's blocks, sorted. */
	public static List<String> blocks() {
		return ContentRegistry.blockNames();
	}

	/** Every mod's items, block items included, sorted. */
	public static List<String> items() {
		return ContentRegistry.itemNames();
	}
}
